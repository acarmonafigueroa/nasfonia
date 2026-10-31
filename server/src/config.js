import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';

const dataDir = path.resolve(process.env.DATA_DIR || './data');
fs.mkdirSync(path.join(dataDir, 'covers'), { recursive: true });

// Reads a value kept in the data volume, creating it on first start.
function persisted(file, make) {
  const p = path.join(dataDir, file);
  if (fs.existsSync(p)) return fs.readFileSync(p, 'utf8').trim();
  const value = make();
  fs.writeFileSync(p, value);
  return value;
}

export const config = {
  port: Number(process.env.PORT || 8090),
  serverName: process.env.SERVER_NAME || 'Nasfonía',
  // Several libraries can be given separated by commas.
  musicDirs: (process.env.MUSIC_DIR || './music')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => path.resolve(s)),
  // Where the NAS volumes are mounted; the folder picker can browse anything below it.
  browseRoot: path.resolve(process.env.BROWSE_ROOT || '/nas'),
  dataDir,
  coversDir: path.join(dataDir, 'covers'),
  scanIntervalMin: Number(process.env.SCAN_INTERVAL_MINUTES ?? 60),
  passwordFromEnv: Boolean(process.env.MUSIC_PASSWORD),
  password: process.env.MUSIC_PASSWORD || persisted('password.txt', () => crypto.randomBytes(6).toString('hex')),
  secret: persisted('secret.key', () => crypto.randomBytes(32).toString('hex')),
};
