import fs from 'node:fs/promises';
import path from 'node:path';
import { parseFile } from 'music-metadata';

export const AUDIO = new Set(['.mp3', '.flac', '.m4a', '.aac', '.ogg', '.oga', '.opus', '.wav', '.wma', '.aif', '.aiff', '.ape', '.wv']);
const IMAGES = new Set(['.jpg', '.jpeg', '.png', '.webp']);
const COVER_NAMES = new Set(['cover', 'folder', 'front', 'album', 'albumart', 'artwork']);

// NAS system folders (@eaDir, #recycle, .Trash...) are not music.
export const skipName = (name) => name.startsWith('.') || name.startsWith('@') || name.startsWith('#') || name === 'lost+found';

const clean = (v) => {
  if (Array.isArray(v)) v = v[0];
  if (v == null) return '';
  return String(v).replace(/\u0000/g, '').trim();
};

export class Scanner {
  constructor(db, config) {
    this.db = db;
    this.config = config;
    this.status = { running: false, lastRun: null, scanned: 0, added: 0, updated: 0, removed: 0, error: null };
    this.q = {
      artist: db.prepare('INSERT INTO artists(name) VALUES (?) ON CONFLICT(name) DO UPDATE SET name = name RETURNING id'),
      album: db.prepare(`INSERT INTO albums(title, artist_id, year, added_at) VALUES (?, ?, ?, ?)
        ON CONFLICT(title, artist_id) DO UPDATE SET year = COALESCE(albums.year, excluded.year)
        RETURNING id, cover`),
      setCover: db.prepare('UPDATE albums SET cover = ? WHERE id = ?'),
      track: db.prepare(`INSERT INTO tracks(path, title, artist_id, album_id, track_no, disc_no, duration, genre, year, bitrate, format, size, mtime, added_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(path) DO UPDATE SET title = excluded.title, artist_id = excluded.artist_id, album_id = excluded.album_id,
          track_no = excluded.track_no, disc_no = excluded.disc_no, duration = excluded.duration, genre = excluded.genre,
          year = excluded.year, bitrate = excluded.bitrate, format = excluded.format, size = excluded.size, mtime = excluded.mtime`),
    };
  }

  /** Starts a scan unless one is running. `full` re-reads the tags of every file. */
  async scan({ full = false } = {}) {
    if (this.status.running) {
      // Folders may have changed mid-scan: go again when this one ends.
      this.again = true;
      return this.status;
    }
    this.status = { running: true, startedAt: Date.now(), lastRun: this.status.lastRun, scanned: 0, added: 0, updated: 0, removed: 0, error: null };
    const t0 = Date.now();
    try {
      await this.#run(full);
      console.log(`[scan] ${this.status.scanned} archivos, +${this.status.added} nuevos, ${this.status.updated} actualizados, -${this.status.removed} borrados (${((Date.now() - t0) / 1000).toFixed(1)} s)`);
    } catch (e) {
      this.status.error = e.message;
      console.error('[scan] error', e);
    } finally {
      this.status.running = false;
      this.status.lastRun = Date.now();
    }
    if (this.again) {
      this.again = false;
      return this.scan();
    }
    return this.status;
  }

  async #run(full) {
    const { db } = this;
    const existing = new Map(db.prepare('SELECT path, mtime, size FROM tracks').all().map((r) => [r.path, r]));
    const firstScan = existing.size === 0;
    const seen = new Set();
    let allRootsReachable = true;
    this.artistCache = new Map();

    for (const root of this.config.musicDirs) {
      try {
        await fs.access(root);
      } catch {
        // An unmounted volume must not wipe the library.
        allRootsReachable = false;
        console.warn(`[scan] carpeta no accesible: ${root}`);
        continue;
      }
      for await (const { file, folderCover } of this.#walk(root)) {
        seen.add(file);
        this.status.scanned++;
        let st;
        try {
          st = await fs.stat(file);
        } catch {
          continue;
        }
        const mtime = Math.floor(st.mtimeMs);
        const prev = existing.get(file);
        if (prev && !full && prev.mtime === mtime && prev.size === st.size) continue;
        await this.#index(file, st, mtime, folderCover, firstScan);
        if (prev) this.status.updated++;
        else this.status.added++;
      }
    }

    if (allRootsReachable) {
      const del = db.prepare('DELETE FROM tracks WHERE path = ?');
      for (const p of existing.keys()) {
        if (!seen.has(p)) {
          del.run(p);
          this.status.removed++;
        }
      }
    }
    await this.#cleanup();
  }

  async *#walk(dir) {
    let entries;
    try {
      entries = await fs.readdir(dir, { withFileTypes: true });
    } catch (e) {
      console.warn(`[scan] no se puede leer ${dir}: ${e.message}`);
      return;
    }
    const cover = entries.find((e) => {
      if (!e.isFile()) return false;
      const ext = path.extname(e.name).toLowerCase();
      return IMAGES.has(ext) && COVER_NAMES.has(path.basename(e.name, ext).toLowerCase());
    });
    const folderCover = cover ? path.join(dir, cover.name) : null;
    entries.sort((a, b) => a.name.localeCompare(b.name));
    for (const e of entries) {
      if (skipName(e.name)) continue;
      const full = path.join(dir, e.name);
      if (e.isDirectory()) yield* this.#walk(full);
      else if (e.isFile() && AUDIO.has(path.extname(e.name).toLowerCase())) yield { file: full, folderCover };
    }
  }

  #artistId(name) {
    const key = name.toLowerCase();
    let id = this.artistCache.get(key);
    if (id == null) {
      id = this.q.artist.get(name).id;
      this.artistCache.set(key, id);
    }
    return id;
  }

  async #index(file, st, mtime, folderCover, firstScan) {
    let meta = null;
    try {
      meta = await parseFile(file, { skipPostHeaders: true });
    } catch (e) {
      console.warn(`[scan] etiquetas ilegibles en ${file}: ${e.message}`);
    }
    const c = meta?.common ?? {};
    const f = meta?.format ?? {};

    const artistName = clean(c.artist) || clean(c.albumartist) || 'Artista desconocido';
    const albumArtistName = clean(c.albumartist) || artistName;
    const albumTitle = clean(c.album) || path.basename(path.dirname(file));
    const title = clean(c.title) || path.basename(file, path.extname(file));
    const year = Number(c.year) || null;
    // On the first scan the file date gives a sensible "recently added" order.
    const addedAt = firstScan ? Math.min(mtime, Date.now()) : Date.now();

    const artistId = this.#artistId(artistName);
    const albumArtistId = this.#artistId(albumArtistName);
    const album = this.q.album.get(albumTitle, albumArtistId, year, addedAt);
    if (!album.cover) await this.#saveCover(album.id, folderCover, c.picture?.[0]);

    this.q.track.run(
      file,
      title,
      artistId,
      album.id,
      c.track?.no ?? null,
      c.disk?.no ?? null,
      f.duration ?? null,
      clean(c.genre) || null,
      year,
      f.bitrate ? Math.round(f.bitrate / 1000) : null,
      (f.codec || f.container || path.extname(file).slice(1)).toString().slice(0, 20),
      st.size,
      mtime,
      addedAt,
    );
  }

  async #saveCover(albumId, folderCover, picture) {
    try {
      let name = null;
      if (folderCover) {
        name = `${albumId}${path.extname(folderCover).toLowerCase()}`;
        await fs.copyFile(folderCover, path.join(this.config.coversDir, name));
      } else if (picture?.data?.length) {
        const ext = /png/i.test(picture.format) ? '.png' : /webp/i.test(picture.format) ? '.webp' : '.jpg';
        name = `${albumId}${ext}`;
        await fs.writeFile(path.join(this.config.coversDir, name), picture.data);
      }
      if (name) this.q.setCover.run(name, albumId);
    } catch (e) {
      console.warn(`[scan] no se pudo guardar la portada del álbum ${albumId}: ${e.message}`);
    }
  }

  async #cleanup() {
    const { db } = this;
    const orphans = db.prepare('SELECT id, cover FROM albums WHERE id NOT IN (SELECT DISTINCT album_id FROM tracks)').all();
    const delAlbum = db.prepare('DELETE FROM albums WHERE id = ?');
    for (const a of orphans) {
      delAlbum.run(a.id);
      if (a.cover) await fs.rm(path.join(this.config.coversDir, a.cover), { force: true });
    }
    db.exec(`DELETE FROM artists WHERE id NOT IN (SELECT artist_id FROM tracks) AND id NOT IN (SELECT artist_id FROM albums)`);
  }
}
