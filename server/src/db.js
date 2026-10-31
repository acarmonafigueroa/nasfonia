import { DatabaseSync } from 'node:sqlite';

// Lowercase without accents, so "camaron" finds "Camarón".
export const normalize = (s) =>
  s == null ? '' : String(s).normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

export function openDb(file) {
  const db = new DatabaseSync(file);
  db.function('norm', { deterministic: true }, normalize);
  db.exec(`
    PRAGMA journal_mode = WAL;
    PRAGMA synchronous = NORMAL;
    PRAGMA foreign_keys = ON;

    CREATE TABLE IF NOT EXISTS artists (
      id INTEGER PRIMARY KEY,
      name TEXT NOT NULL UNIQUE COLLATE NOCASE
    );
    CREATE TABLE IF NOT EXISTS albums (
      id INTEGER PRIMARY KEY,
      title TEXT NOT NULL,
      artist_id INTEGER NOT NULL REFERENCES artists(id),
      year INTEGER,
      cover TEXT,
      added_at INTEGER NOT NULL,
      UNIQUE (title, artist_id)
    );
    CREATE TABLE IF NOT EXISTS tracks (
      id INTEGER PRIMARY KEY,
      path TEXT NOT NULL UNIQUE,
      title TEXT NOT NULL,
      artist_id INTEGER NOT NULL REFERENCES artists(id),
      album_id INTEGER NOT NULL REFERENCES albums(id),
      track_no INTEGER,
      disc_no INTEGER,
      duration REAL,
      genre TEXT,
      year INTEGER,
      bitrate INTEGER,
      format TEXT,
      size INTEGER,
      mtime INTEGER,
      added_at INTEGER NOT NULL
    );
    CREATE INDEX IF NOT EXISTS tracks_album ON tracks(album_id);
    CREATE INDEX IF NOT EXISTS tracks_artist ON tracks(artist_id);

    CREATE TABLE IF NOT EXISTS playlists (
      id INTEGER PRIMARY KEY,
      name TEXT NOT NULL,
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL
    );
    CREATE TABLE IF NOT EXISTS playlist_tracks (
      playlist_id INTEGER NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
      position INTEGER NOT NULL,
      track_id INTEGER NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
      PRIMARY KEY (playlist_id, position)
    );
    CREATE TABLE IF NOT EXISTS favorites (
      track_id INTEGER PRIMARY KEY REFERENCES tracks(id) ON DELETE CASCADE,
      added_at INTEGER NOT NULL
    );
    CREATE TABLE IF NOT EXISTS plays (
      track_id INTEGER NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
      played_at INTEGER NOT NULL
    );
    CREATE INDEX IF NOT EXISTS plays_time ON plays(played_at);
    CREATE INDEX IF NOT EXISTS plays_track ON plays(track_id);

    CREATE TABLE IF NOT EXISTS settings (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL
    );
  `);
  return db;
}
