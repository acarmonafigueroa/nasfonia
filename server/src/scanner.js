import fs from 'node:fs/promises';
import path from 'node:path';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { parseFile } from 'music-metadata';
import { cleanTrackQuery, fetchOnlineCover, fetchOnlineLyrics } from './metadata.js';

const execFileAsync = promisify(execFile);
let hasFfmpeg = null;
export async function checkFfmpeg() {
  if (hasFfmpeg != null) return hasFfmpeg;
  try {
    await execFileAsync('ffmpeg', ['-version'], { timeout: 3000 });
    hasFfmpeg = true;
  } catch {
    hasFfmpeg = false;
  }
  return hasFfmpeg;
}

export const AUDIO = new Set(['.mp3', '.flac', '.m4a', '.m4b', '.aac', '.ogg', '.oga', '.opus', '.wav', '.wma', '.aif', '.aiff', '.ape', '.wv']);
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
    this.coverLocks = new Map();
    this.enriching = false;
    this.status = { running: false, lastRun: null, scanned: 0, added: 0, updated: 0, removed: 0, coversFetched: 0, lyricsFetched: 0, error: null };
    this.q = {
      artist: db.prepare('INSERT INTO artists(name) VALUES (?) ON CONFLICT(name) DO UPDATE SET name = name RETURNING id'),
      album: db.prepare(`INSERT INTO albums(title, artist_id, year, added_at) VALUES (?, ?, ?, ?)
        ON CONFLICT(title, artist_id) DO UPDATE SET year = COALESCE(albums.year, excluded.year)
        RETURNING id, cover`),
      setCover: db.prepare('UPDATE albums SET cover = ?, cover_checked_at = ? WHERE id = ?'),
      markCoverChecked: db.prepare('UPDATE albums SET cover_checked_at = ? WHERE id = ?'),
      albumById: db.prepare(`SELECT al.id, al.title, al.cover, al.cover_checked_at AS checkedAt, ar.name AS artist
        FROM albums al JOIN artists ar ON ar.id = al.artist_id WHERE al.id = ?`),
      albumSamples: db.prepare(`SELECT t.title, ar.name AS artist
        FROM tracks t JOIN artists ar ON ar.id = t.artist_id WHERE t.album_id = ? ORDER BY COALESCE(t.track_no, 9999), t.id LIMIT 4`),
      trackForLyrics: db.prepare(`SELECT t.id, t.path, t.title, t.duration, ar.name AS artist, al.title AS album
        FROM tracks t JOIN artists ar ON ar.id = t.artist_id JOIN albums al ON al.id = t.album_id WHERE t.id = ?`),
      getLyricsCache: db.prepare('SELECT synced, lines_json AS linesJson, fetched_at AS fetchedAt FROM track_lyrics WHERE track_id = ?'),
      setLyricsCache: db.prepare(`INSERT INTO track_lyrics(track_id, synced, lines_json, source, fetched_at)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(track_id) DO UPDATE SET synced = excluded.synced, lines_json = excluded.lines_json, source = excluded.source, fetched_at = excluded.fetched_at`),
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
    this.status = {
      running: true,
      startedAt: Date.now(),
      lastRun: this.status.lastRun,
      scanned: 0,
      added: 0,
      updated: 0,
      removed: 0,
      coversFetched: this.status.coversFetched || 0,
      lyricsFetched: this.status.lyricsFetched || 0,
      error: null,
    };
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
    // Kick off automatic online cover & lyrics enrichment in the background (like Plex).
    this.enrichOnline({ full }).catch((e) => console.warn('[meta] error en descarga automática:', e.message));
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
        await this.#index(file, st, mtime, folderCover, firstScan, full);
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

  async #index(file, st, mtime, folderCover, firstScan, full = false) {
    let meta = null;
    try {
      meta = await parseFile(file, { skipPostHeaders: true });
    } catch (e) {
      console.warn(`[scan] etiquetas ilegibles en ${file}: ${e.message}`);
    }
    const c = meta?.common ?? {};
    const f = meta?.format ?? {};

    let artistName = clean(c.artist) || clean(c.albumartist);
    let title = clean(c.title) || path.basename(file, path.extname(file));
    if (!artistName) {
      const inferred = cleanTrackQuery({ title });
      if (inferred.artist) {
        artistName = inferred.artist;
        if (!clean(c.title)) title = inferred.title;
      } else {
        artistName = 'Artista desconocido';
      }
    }
    const albumArtistName = clean(c.albumartist) || artistName;
    const parentName = path.basename(path.dirname(file));
    let albumTitle = clean(c.album);
    if (!albumTitle) {
      const isGenericFolder = /^(music|m[uú]sica|downloads?|descargas|mp3|audio|carpeta\s+personal|volumen\s*\d*)$/i.test(parentName);
      albumTitle = isGenericFolder ? title : parentName;
    }
    const year = Number(c.year) || null;
    // On the first scan the file date gives a sensible "recently added" order.
    const addedAt = firstScan ? Math.min(mtime, Date.now()) : Date.now();

    const artistId = this.#artistId(artistName);
    const albumArtistId = this.#artistId(albumArtistName);
    const album = this.q.album.get(albumTitle, albumArtistId, year, addedAt);
    if (!album.cover || full) await this.#saveCover(album.id, folderCover, c.picture?.[0]);

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
        const dest = path.join(this.config.coversDir, name);
        await fs.copyFile(folderCover, dest);
        name = await this.#resizeCoverIfNeeded(albumId, dest, name);
      } else if (picture?.data?.length) {
        const ext = /png/i.test(picture.format) ? '.png' : /webp/i.test(picture.format) ? '.webp' : '.jpg';
        name = `${albumId}${ext}`;
        const dest = path.join(this.config.coversDir, name);
        await fs.writeFile(dest, picture.data);
        name = await this.#resizeCoverIfNeeded(albumId, dest, name);
      }
      if (name) this.q.setCover.run(name, Date.now(), albumId);
    } catch (e) {
      console.warn(`[scan] no se pudo guardar la portada del álbum ${albumId}: ${e.message}`);
    }
  }

  /** Fetches an album's cover from Deezer / iTunes if missing locally. */
  async ensureAlbumCover(albumId, { force = false } = {}) {
    const id = Number(albumId);
    if (!Number.isFinite(id) || id <= 0) return null;
    if (this.coverLocks.has(id)) return this.coverLocks.get(id);

    const task = (async () => {
      try {
        const al = this.q.albumById.get(id);
        if (!al) return null;
        if (al.cover && !force) return al.cover;
        if (!force && al.checkedAt && Date.now() - al.checkedAt < 7 * 86_400_000) return al.cover || null;

        const samples = this.q.albumSamples.all(id);
        const buf = await fetchOnlineCover({ title: al.title, artist: al.artist, samples });
        const now = Date.now();
        if (!buf) {
          this.q.markCoverChecked.run(now, id);
          return al.cover || null;
        }
        let name = `${id}.jpg`;
        const dest = path.join(this.config.coversDir, name);
        await fs.writeFile(dest, buf);
        name = await this.#resizeCoverIfNeeded(id, dest, name);
        this.q.setCover.run(name, now, id);
        this.status.coversFetched = (this.status.coversFetched || 0) + 1;
        return name;
      } catch (e) {
        console.warn(`[meta] error buscando portada para álbum ${id}: ${e.message}`);
        return null;
      } finally {
        this.coverLocks.delete(id);
      }
    })();

    this.coverLocks.set(id, task);
    return task;
  }

  /** Reads local (.lrc / embedded) lyrics or fetches and caches them from LRCLIB / Lyrics.ovh. */
  async getLyrics(trackId, { force = false } = {}) {
    const t = this.q.trackForLyrics.get(Number(trackId));
    if (!t) return null;

    // 1. Local sidecar (.lrc / .txt) or embedded audio tags
    const local = await readLyrics(t.path);
    if (local.lines.length) return local;

    // 2. Cached online lyrics in SQLite
    const cached = this.q.getLyricsCache.get(t.id);
    if (cached && !force) {
      try {
        const lines = JSON.parse(cached.linesJson);
        if (lines.length || Date.now() - cached.fetchedAt < 3 * 86_400_000) {
          return { synced: Boolean(cached.synced), lines };
        }
      } catch {
        /* corrupt cache row */
      }
    }

    // 3. Online lookup (LRCLIB + Lyrics.ovh fallback)
    const online = await fetchOnlineLyrics({
      title: t.title,
      artist: t.artist,
      album: t.album,
      duration: t.duration,
    });
    this.q.setLyricsCache.run(t.id, online.synced ? 1 : 0, JSON.stringify(online.lines), online.source, Date.now());
    if (online.lines.length) {
      this.status.lyricsFetched = (this.status.lyricsFetched || 0) + 1;
    }
    return { synced: online.synced, lines: online.lines };
  }

  /** Background pass that downloads missing covers and lyrics (like Plex). */
  async enrichOnline({ full = false } = {}) {
    if (this.enriching) return;
    this.enriching = true;
    const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
    try {
      const retryBefore = Date.now() - 7 * 86_400_000;
      const missingAlbums = full
        ? this.db.prepare('SELECT id FROM albums WHERE cover IS NULL ORDER BY id').all()
        : this.db.prepare('SELECT id FROM albums WHERE cover IS NULL AND (cover_checked_at IS NULL OR cover_checked_at < ?) ORDER BY id').all(retryBefore);

      let newCovers = 0;
      for (const a of missingAlbums) {
        if (this.status.running) break;
        const c = await this.ensureAlbumCover(a.id, { force: full });
        if (c) newCovers++;
        await sleep(250);
      }

      const missingLyrics = full
        ? this.db.prepare(`SELECT t.id FROM tracks t LEFT JOIN track_lyrics tl ON tl.track_id = t.id WHERE tl.track_id IS NULL OR tl.lines_json = '[]' ORDER BY t.id`).all()
        : this.db.prepare('SELECT t.id FROM tracks t LEFT JOIN track_lyrics tl ON tl.track_id = t.id WHERE tl.track_id IS NULL ORDER BY t.id').all();

      let newLyrics = 0;
      for (const tr of missingLyrics) {
        if (this.status.running) break;
        const lyr = await this.getLyrics(tr.id, { force: full });
        if (lyr?.lines?.length) newLyrics++;
        await sleep(300);
      }

      if (missingAlbums.length || missingLyrics.length) {
        console.log(`[meta] Descarga automática: +${newCovers}/${missingAlbums.length} portadas, +${newLyrics}/${missingLyrics.length} letras`);
      }
    } finally {
      this.enriching = false;
    }
  }

  async #resizeCoverIfNeeded(albumId, filePath, currentName) {
    try {
      const st = await fs.stat(filePath);
      // Only resize covers larger than 250 KB when ffmpeg is available.
      if (st.size < 250_000 || !(await checkFfmpeg())) return currentName;
      const outName = `${albumId}.jpg`;
      const tmpPath = path.join(this.config.coversDir, `${albumId}.tmp.jpg`);
      const outPath = path.join(this.config.coversDir, outName);
      await execFileAsync(
        'ffmpeg',
        ['-y', '-loglevel', 'error', '-i', filePath, '-vf', "scale='min(600,iw)':'min(600,ih)':force_original_aspect_ratio=decrease", '-frames:v', '1', '-q:v', '3', tmpPath],
        { timeout: 10_000 },
      );
      const outStat = await fs.stat(tmpPath);
      if (outStat.size > 0 && outStat.size < st.size) {
        if (filePath !== outPath) await fs.rm(filePath, { force: true });
        await fs.rename(tmpPath, outPath);
        return outName;
      }
      await fs.rm(tmpPath, { force: true });
    } catch {
      await fs.rm(path.join(this.config.coversDir, `${albumId}.tmp.jpg`), { force: true }).catch(() => {});
    }
    return currentName;
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

/** Reads synced (.lrc / SYLT) or plain lyrics from a sidecar file or embedded audio tags. */
export async function readLyrics(filePath) {
  const dir = path.dirname(filePath);
  const base = path.basename(filePath, path.extname(filePath));
  let raw = null;
  for (const ext of ['.lrc', '.LRC', '.txt']) {
    try {
      raw = await fs.readFile(path.join(dir, base + ext), 'utf8');
      if (raw && raw.trim()) break;
    } catch {
      /* no sidecar */
    }
  }
  if (!raw || !raw.trim()) {
    try {
      const meta = await parseFile(filePath, { skipCovers: true });
      const lyr = meta?.common?.lyrics;
      if (Array.isArray(lyr) && lyr.length) {
        const first = lyr[0];
        if (typeof first === 'string') raw = first;
        else if (typeof first?.text === 'string') raw = first.text;
        else if (Array.isArray(first?.syncText)) {
          const lines = first.syncText
            .map((s) => ({ timeMs: s.timestamp ?? 0, text: String(s.text ?? '').trim() }))
            .filter((s) => s.text);
          if (lines.length) return { synced: true, lines };
        }
      }
      if ((!raw || !raw.trim()) && meta?.native) {
        for (const tags of Object.values(meta.native)) {
          for (const tag of tags) {
            if (/^(USLT|SYLT|LYRICS|©lyr|WM\/Lyrics)$/i.test(tag.id)) {
              if (typeof tag.value === 'string' && tag.value.trim()) {
                raw = tag.value;
                break;
              } else if (typeof tag.value?.text === 'string' && tag.value.text.trim()) {
                raw = tag.value.text;
                break;
              }
            }
          }
          if (raw) break;
        }
      }
    } catch {
      /* unreadable tags */
    }
  }
  if (!raw || !raw.trim()) return { synced: false, lines: [] };
  return parseLrc(raw);
}

export function parseLrc(raw) {
  const lines = [];
  let synced = false;
  const tagRe = /\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]/g;
  for (const rawLine of raw.replace(/\uFEFF/g, '').split(/\r?\n/)) {
    const matches = [...rawLine.matchAll(tagRe)];
    if (matches.length) {
      const text = rawLine.replace(tagRe, '').trim();
      for (const m of matches) {
        const min = Number(m[1]);
        const sec = Number(m[2]);
        const frac = m[3] ? Number(m[3].padEnd(3, '0').slice(0, 3)) : 0;
        lines.push({ timeMs: (min * 60 + sec) * 1000 + frac, text });
      }
      synced = true;
    } else {
      if (/^\[[a-z]{2,8}:.*\]$/i.test(rawLine.trim())) continue;
      lines.push({ timeMs: null, text: rawLine.trim() });
    }
  }
  if (synced) {
    const syncedLines = lines.filter((l) => l.timeMs != null).sort((a, b) => a.timeMs - b.timeMs);
    return { synced: true, lines: syncedLines };
  }
  while (lines.length && !lines[0].text) lines.shift();
  while (lines.length && !lines[lines.length - 1].text) lines.pop();
  return { synced: false, lines };
}
