import fs from 'node:fs/promises';
import path from 'node:path';
import { AUDIO, skipName } from './scanner.js';

// The music folders the user picked, stored in the database. Until a choice is
// saved, MUSIC_DIR from the environment is used.
export function createFolders(db, config) {
  const envDirs = [...config.musicDirs];
  const read = db.prepare("SELECT value FROM settings WHERE key = 'musicDirs'");
  const write = db.prepare("INSERT INTO settings(key, value) VALUES ('musicDirs', ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value");

  const saved = read.get();
  if (saved) {
    try {
      config.musicDirs = JSON.parse(saved.value);
    } catch {
      /* keep the environment's folders */
    }
  }

  const inside = (p, root) => p === root || p.startsWith(root.endsWith(path.sep) ? root : root + path.sep);
  // Folders from the environment stay allowed even when outside the browse root.
  const allowed = (p) => inside(p, config.browseRoot) || envDirs.some((d) => inside(p, d));

  async function isDir(p) {
    try {
      return (await fs.stat(p)).isDirectory();
    } catch {
      return false;
    }
  }

  return {
    async info() {
      return {
        dirs: await Promise.all(config.musicDirs.map(async (p) => ({ path: p, name: label(p), exists: await isDir(p) }))),
        browseRoot: config.browseRoot,
        canBrowse: await isDir(config.browseRoot),
      };
    },

    async browse(requested) {
      const p = path.resolve(requested || config.browseRoot);
      if (!allowed(p)) throw Object.assign(new Error('Carpeta fuera de las permitidas'), { status: 403 });
      let entries;
      try {
        entries = await fs.readdir(p, { withFileTypes: true });
      } catch {
        throw Object.assign(new Error('No se puede abrir la carpeta'), { status: 404 });
      }
      const folders = entries
        .filter((e) => e.isDirectory() && !skipName(e.name))
        .map((e) => ({ name: e.name, path: path.join(p, e.name) }))
        .sort((a, b) => a.name.localeCompare(b.name, 'es', { sensitivity: 'base' }));
      const audioFiles = entries.filter((e) => e.isFile() && AUDIO.has(path.extname(e.name).toLowerCase())).length;
      const parent = p !== config.browseRoot && allowed(path.dirname(p)) ? path.dirname(p) : null;
      return { path: p, name: label(p), parent, folders, audioFiles, selected: config.musicDirs.includes(p) };
    },

    async setDirs(dirs) {
      const clean = [...new Set(dirs.map((d) => path.resolve(String(d))))];
      for (const d of clean) {
        if (!allowed(d)) throw Object.assign(new Error(`No permitido: ${d}`), { status: 400 });
        if (!(await isDir(d))) throw Object.assign(new Error(`No existe: ${d}`), { status: 400 });
      }
      config.musicDirs = clean;
      write.run(JSON.stringify(clean));
    },
  };

  // "/nas/Carpeta personal/Music" → "Carpeta personal › Music"
  function label(p) {
    const rel = inside(p, config.browseRoot) ? path.relative(config.browseRoot, p) : p;
    return rel ? rel.split(path.sep).join(' › ') : 'NAS';
  }
}
