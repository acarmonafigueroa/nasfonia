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
  migrate(db);
  return db;
}

// Schema changes after 1.2.0, tracked with PRAGMA user_version.
function migrate(db) {
  const version = db.prepare('PRAGMA user_version').get().user_version;
  if (version < 1) {
    // 1.3.0 — users. Everything that existed belongs to the first admin (id 1).
    // password_hash NULL on the admin means "still on the default password" (see auth.js).
    db.exec(`
      BEGIN;
      CREATE TABLE users (
        id INTEGER PRIMARY KEY,
        username TEXT NOT NULL UNIQUE COLLATE NOCASE,
        password_hash TEXT,
        is_admin INTEGER NOT NULL DEFAULT 0,
        created_at INTEGER NOT NULL
      );
      INSERT INTO users(id, username, password_hash, is_admin, created_at)
        VALUES (1, 'admin', NULL, 1, CAST(strftime('%s', 'now') AS INTEGER) * 1000);

      CREATE TABLE favorites_v1 (
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        track_id INTEGER NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
        added_at INTEGER NOT NULL,
        PRIMARY KEY (user_id, track_id)
      );
      INSERT INTO favorites_v1 SELECT 1, track_id, added_at FROM favorites;
      DROP TABLE favorites;
      ALTER TABLE favorites_v1 RENAME TO favorites;

      -- SQLite cannot add a REFERENCES column with a non-null default; deleting a user cleans these by hand.
      ALTER TABLE plays ADD COLUMN user_id INTEGER NOT NULL DEFAULT 1;
      ALTER TABLE playlists ADD COLUMN user_id INTEGER NOT NULL DEFAULT 1;
      CREATE INDEX plays_user ON plays(user_id, played_at);
      CREATE INDEX playlists_user ON playlists(user_id);

      PRAGMA user_version = 1;
      COMMIT;
    `);
  }
  if (version < 2) {
    // 1.4.0 — shared playlists and extra indexes for genres & recents.
    db.exec(`
      BEGIN;
      ALTER TABLE playlists ADD COLUMN is_public INTEGER NOT NULL DEFAULT 0;
      CREATE INDEX IF NOT EXISTS tracks_genre ON tracks(genre);
      CREATE INDEX IF NOT EXISTS tracks_added ON tracks(added_at);
      PRAGMA user_version = 2;
      COMMIT;
    `);
  }
  if (version < 3) {
    // 1.4.1 — automatic online metadata cache (cover checks + synced lyrics).
    db.exec(`
      BEGIN;
      ALTER TABLE albums ADD COLUMN cover_checked_at INTEGER;
      CREATE TABLE IF NOT EXISTS track_lyrics (
        track_id   INTEGER PRIMARY KEY REFERENCES tracks(id) ON DELETE CASCADE,
        synced     INTEGER NOT NULL DEFAULT 0,
        lines_json TEXT NOT NULL,
        source     TEXT,
        fetched_at INTEGER NOT NULL
      );
      PRAGMA user_version = 3;
      COMMIT;
    `);
  }
}
