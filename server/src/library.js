import { normalize } from './db.js';

// Favorites are per user, so the track query is built for one user id (always an integer).
const trackSelect = (uid) => `
  SELECT t.id, t.title, t.artist_id AS artistId, ar.name AS artist, t.album_id AS albumId, al.title AS album,
         t.track_no AS trackNo, t.disc_no AS discNo, t.duration, t.genre, t.year, t.format, t.bitrate,
         (al.cover IS NOT NULL) AS hasCover, (f.track_id IS NOT NULL) AS favorite
  FROM tracks t
  JOIN artists ar ON ar.id = t.artist_id
  JOIN albums al ON al.id = t.album_id
  LEFT JOIN favorites f ON f.track_id = t.id AND f.user_id = ${Number(uid)}`;

const ALBUM_SELECT = `
  SELECT al.id, al.title, al.artist_id AS artistId, ar.name AS artist, al.year, (al.cover IS NOT NULL) AS hasCover,
         al.added_at AS addedAt, COUNT(t.id) AS trackCount, COALESCE(SUM(t.duration), 0) AS duration
  FROM albums al
  JOIN artists ar ON ar.id = al.artist_id
  LEFT JOIN tracks t ON t.album_id = al.id`;

const ARTIST_SELECT = `
  SELECT ar.id, ar.name,
    (SELECT COUNT(*) FROM albums WHERE artist_id = ar.id) AS albumCount,
    (SELECT COUNT(*) FROM tracks WHERE artist_id = ar.id) AS trackCount,
    COALESCE(
      (SELECT id FROM albums WHERE artist_id = ar.id AND cover IS NOT NULL ORDER BY year DESC LIMIT 1),
      (SELECT a.id FROM tracks t JOIN albums a ON a.id = t.album_id WHERE t.artist_id = ar.id AND a.cover IS NOT NULL LIMIT 1)
    ) AS coverAlbumId
  FROM artists ar`;

const PLAYLIST_SELECT = `
  SELECT p.id, p.name, p.user_id AS ownerId, u.username AS owner, p.is_public AS isPublic,
    p.created_at AS createdAt, p.updated_at AS updatedAt,
    (SELECT COUNT(*) FROM playlist_tracks WHERE playlist_id = p.id) AS trackCount,
    (SELECT COALESCE(SUM(t.duration), 0) FROM playlist_tracks pt JOIN tracks t ON t.id = pt.track_id WHERE pt.playlist_id = p.id) AS duration,
    (SELECT t.album_id FROM playlist_tracks pt JOIN tracks t ON t.id = pt.track_id JOIN albums a ON a.id = t.album_id
       WHERE pt.playlist_id = p.id AND a.cover IS NOT NULL ORDER BY pt.position LIMIT 1) AS coverAlbumId
  FROM playlists p
  LEFT JOIN users u ON u.id = p.user_id`;

const ALBUM_SORT = {
  title: 'al.title COLLATE NOCASE',
  artist: 'ar.name COLLATE NOCASE, al.year',
  year: 'al.year DESC, al.title COLLATE NOCASE',
  recent: 'al.added_at DESC, al.id DESC',
  random: 'RANDOM()',
};

const TRACK_SORT = {
  title: 't.title COLLATE NOCASE',
  artist: 'ar.name COLLATE NOCASE, al.title COLLATE NOCASE, t.disc_no, t.track_no',
  recent: 't.added_at DESC, t.id DESC',
  random: 'RANDOM()',
};

const ALBUM_TRACK_ORDER = 'ORDER BY COALESCE(t.disc_no, 1), COALESCE(t.track_no, 9999), t.title COLLATE NOCASE';

const track = (r) => r && { ...r, hasCover: Boolean(r.hasCover), favorite: Boolean(r.favorite) };
const album = (r) => r && { ...r, hasCover: Boolean(r.hasCover) };
const playlistRow = (r, uid) => r && { ...r, isPublic: Boolean(r.isPublic), isMine: r.ownerId === uid };
const int = (v, def, max) => Math.min(Math.max(Number.parseInt(v, 10) || def, 0), max);

/** orUser(id) gives the library as that user sees it: their favorites, playlists and history. */
export function createLibrary(db) {
  const cache = new Map();
  return {
    forUser(uid) {
      if (!cache.has(uid)) cache.set(uid, libraryFor(db, Number(uid)));
      return cache.get(uid);
    },
  };
}

function libraryFor(db, uid) {
  const all = (sql, ...p) => db.prepare(sql).all(...p);
  const get = (sql, ...p) => db.prepare(sql).get(...p);
  const now = () => Date.now();
  const TRACK_SELECT = trackSelect(uid);
  const MY_PLAYLISTS = `${PLAYLIST_SELECT} WHERE (p.user_id = ${uid} OR p.is_public = 1)`;
  const MY_PLAYS = `(SELECT * FROM plays WHERE user_id = ${uid})`;
  const mapPl = (r) => playlistRow(r, uid);

  const lib = {
    stats() {
      return get(`SELECT (SELECT COUNT(*) FROM tracks) AS tracks, (SELECT COUNT(*) FROM albums) AS albums,
        (SELECT COUNT(*) FROM artists) AS artists, (SELECT COALESCE(SUM(duration), 0) FROM tracks) AS duration,
        (SELECT COALESCE(SUM(size), 0) FROM tracks) AS size`);
    },

    tracks({ sort = 'title', limit, offset } = {}) {
      const order = TRACK_SORT[sort] || TRACK_SORT.title;
      return all(`${TRACK_SELECT} ORDER BY ${order} LIMIT ? OFFSET ?`, int(limit, 500, 5000), int(offset, 0, 1e9)).map(track);
    },

    track(id) {
      return track(get(`${TRACK_SELECT} WHERE t.id = ?`, id));
    },

    trackFile(id) {
      return get('SELECT path, format FROM tracks WHERE id = ?', id);
    },

    albums({ sort = 'title', limit, offset } = {}) {
      const order = ALBUM_SORT[sort] || ALBUM_SORT.title;
      return all(`${ALBUM_SELECT} GROUP BY al.id ORDER BY ${order} LIMIT ? OFFSET ?`, int(limit, 5000, 20000), int(offset, 0, 1e9)).map(album);
    },

    album(id) {
      const a = album(get(`${ALBUM_SELECT} WHERE al.id = ? GROUP BY al.id`, id));
      if (!a) return null;
      a.tracks = all(`${TRACK_SELECT} WHERE t.album_id = ? ${ALBUM_TRACK_ORDER}`, id).map(track);
      a.genre = a.tracks.find((t) => t.genre)?.genre ?? null;
      return a;
    },

    albumCover(id) {
      return get('SELECT cover FROM albums WHERE id = ?', id)?.cover ?? null;
    },

    artists() {
      return all(`${ARTIST_SELECT} ORDER BY ar.name COLLATE NOCASE`);
    },

    artist(id) {
      const a = get(`${ARTIST_SELECT} WHERE ar.id = ?`, id);
      if (!a) return null;
      a.albums = all(`${ALBUM_SELECT} WHERE al.artist_id = ? GROUP BY al.id ORDER BY al.year DESC, al.title`, id).map(album);
      a.appearsOn = all(
        `${ALBUM_SELECT} WHERE al.artist_id != ? AND al.id IN (SELECT album_id FROM tracks WHERE artist_id = ?) GROUP BY al.id ORDER BY al.year DESC`,
        id,
        id,
      ).map(album);
      a.topTracks = all(
        `${TRACK_SELECT} LEFT JOIN (SELECT track_id, COUNT(*) AS n FROM plays GROUP BY track_id) pc ON pc.track_id = t.id
         WHERE t.artist_id = ? ORDER BY COALESCE(pc.n, 0) DESC, (f.track_id IS NOT NULL) DESC, al.year DESC, t.track_no LIMIT 10`,
        id,
      ).map(track);
      return a;
    },

    artistTracks(id) {
      return all(`${TRACK_SELECT} WHERE t.artist_id = ? ORDER BY al.year, al.title, COALESCE(t.disc_no, 1), t.track_no`, id).map(track);
    },

    genres() {
      return all(`SELECT t.genre AS name, COUNT(*) AS trackCount,
          (SELECT t2.album_id FROM tracks t2 JOIN albums a ON a.id = t2.album_id WHERE t2.genre = t.genre AND a.cover IS NOT NULL LIMIT 1) AS coverAlbumId
        FROM tracks t WHERE t.genre IS NOT NULL GROUP BY t.genre ORDER BY trackCount DESC`);
    },

    genreTracks(name) {
      return all(`${TRACK_SELECT} WHERE t.genre = ? ORDER BY ar.name COLLATE NOCASE, al.year, t.disc_no, t.track_no LIMIT 2000`, name).map(track);
    },

    search(q, limit = 30) {
      const words = normalize(q).split(/\s+/).filter(Boolean);
      if (!words.length) return { tracks: [], albums: [], artists: [], playlists: [] };
      const like = (expr) => words.map(() => `${expr} LIKE ?`).join(' AND ');
      const params = words.map((w) => `%${w}%`);
      return {
        artists: all(`${ARTIST_SELECT} WHERE ${like('norm(ar.name)')} AND ar.id IN (SELECT artist_id FROM tracks) LIMIT ?`, ...params, limit),
        albums: all(`${ALBUM_SELECT} WHERE ${like("norm(al.title || ' ' || ar.name)")} GROUP BY al.id LIMIT ?`, ...params, limit).map(album),
        tracks: all(`${TRACK_SELECT} WHERE ${like("norm(t.title || ' ' || ar.name || ' ' || al.title)")} LIMIT ?`, ...params, limit * 2).map(track),
        playlists: all(`${MY_PLAYLISTS} AND ${like('norm(p.name)')} LIMIT ?`, ...params, limit).map(mapPl),
      };
    },

    randomTracks(limit) {
      return all(`${TRACK_SELECT} ORDER BY RANDOM() LIMIT ?`, int(limit, 100, 1000)).map(track);
    },

    /** Builds a "Track Radio" queue starting with the seed track, followed by similar tracks (artist, genre, era). */
    radio(trackId, limit = 50) {
      const max = int(limit, 50, 200);
      const seed = lib.track(trackId);
      if (!seed) return lib.randomTracks(max);
      const similar = all(
        `${TRACK_SELECT}
         WHERE t.id != ? AND (
           t.artist_id = ?
           OR (t.genre IS NOT NULL AND t.genre = ?)
           OR (t.year IS NOT NULL AND ? IS NOT NULL AND ABS(t.year - ?) <= 5)
         )
         ORDER BY
           ((t.artist_id = ?) * 2 + (t.genre IS NOT NULL AND t.genre = ?) * 2 + (f.track_id IS NOT NULL)) DESC,
           RANDOM()
         LIMIT ?`,
        seed.id,
        seed.artistId,
        seed.genre ?? '',
        seed.year,
        seed.year ?? 0,
        seed.artistId,
        seed.genre ?? '',
        max - 1,
      ).map(track);
      const seen = new Set([seed.id, ...similar.map((t) => t.id)]);
      if (similar.length < max - 1) {
        const extra = all(`${TRACK_SELECT} ORDER BY RANDOM() LIMIT ?`, max).map(track);
        for (const t of extra) {
          if (similar.length >= max - 1) break;
          if (!seen.has(t.id)) {
            seen.add(t.id);
            similar.push(t);
          }
        }
      }
      return [seed, ...similar];
    },

    // --- favorites -------------------------------------------------------
    favorites() {
      return all(`${TRACK_SELECT} WHERE f.track_id IS NOT NULL ORDER BY f.added_at DESC`).map(track);
    },

    setFavorite(trackId, on) {
      if (on) db.prepare('INSERT OR IGNORE INTO favorites(user_id, track_id, added_at) SELECT ?, id, ? FROM tracks WHERE id = ?').run(uid, now(), trackId);
      else db.prepare('DELETE FROM favorites WHERE user_id = ? AND track_id = ?').run(uid, trackId);
    },

    // --- plays -----------------------------------------------------------
    recordPlay(trackId) {
      db.prepare('INSERT INTO plays(user_id, track_id, played_at) SELECT ?, id, ? FROM tracks WHERE id = ?').run(uid, now(), trackId);
    },

    home() {
      const recentTracks = all(
        `${TRACK_SELECT} JOIN (SELECT track_id, MAX(played_at) AS last FROM ${MY_PLAYS} GROUP BY track_id) p ON p.track_id = t.id
         ORDER BY p.last DESC LIMIT 20`,
      ).map(track);
      const recentAlbums = all(
        `${ALBUM_SELECT} JOIN (SELECT tr.album_id AS aid, MAX(p.played_at) AS last FROM ${MY_PLAYS} p JOIN tracks tr ON tr.id = p.track_id GROUP BY tr.album_id) rp ON rp.aid = al.id
         GROUP BY al.id ORDER BY rp.last DESC LIMIT 12`,
      ).map(album);
      const mostPlayed = all(
        `${TRACK_SELECT} JOIN (SELECT track_id, COUNT(*) AS n FROM ${MY_PLAYS} GROUP BY track_id) p ON p.track_id = t.id
         ORDER BY p.n DESC LIMIT 10`,
      ).map(track);
      return {
        recentAlbums,
        recentTracks,
        mostPlayed,
        newAlbums: lib.albums({ sort: 'recent', limit: 14 }),
        randomAlbums: lib.albums({ sort: 'random', limit: 14 }),
        artists: all(`${ARTIST_SELECT} WHERE ar.id IN (SELECT artist_id FROM albums) ORDER BY RANDOM() LIMIT 14`),
        playlists: lib.playlists().slice(0, 8),
        stats: lib.stats(),
      };
    },

    // --- playlists -------------------------------------------------------
    playlists() {
      return all(`${MY_PLAYLISTS} ORDER BY (p.user_id = ${uid}) DESC, p.updated_at DESC`).map(mapPl);
    },

    playlist(id) {
      const p = mapPl(get(`${MY_PLAYLISTS} AND p.id = ?`, id));
      if (!p) return null;
      p.tracks = all(
        `${TRACK_SELECT.replace('SELECT t.id', 'SELECT pt.position, t.id')} JOIN playlist_tracks pt ON pt.track_id = t.id
         WHERE pt.playlist_id = ? ORDER BY pt.position`,
        id,
      ).map(track);
      return p;
    },

    createPlaylist(name, trackIds = [], isPublic = false) {
      const { lastInsertRowid } = db
        .prepare('INSERT INTO playlists(user_id, name, is_public, created_at, updated_at) VALUES (?, ?, ?, ?, ?)')
        .run(uid, name, isPublic ? 1 : 0, now(), now());
      const id = Number(lastInsertRowid);
      if (trackIds.length) lib.addToPlaylist(id, trackIds);
      return lib.playlist(id);
    },

    updatePlaylist(id, { name, isPublic }) {
      const cur = get('SELECT id, name, is_public FROM playlists WHERE id = ? AND user_id = ?', id, uid);
      if (!cur) return false;
      const nextName = name ? name : cur.name;
      const nextPub = isPublic == null ? cur.is_public : isPublic ? 1 : 0;
      return db.prepare('UPDATE playlists SET name = ?, is_public = ?, updated_at = ? WHERE id = ? AND user_id = ?').run(nextName, nextPub, now(), id, uid).changes > 0;
    },

    renamePlaylist(id, name) {
      return lib.updatePlaylist(id, { name });
    },

    deletePlaylist(id) {
      return db.prepare('DELETE FROM playlists WHERE id = ? AND user_id = ?').run(id, uid).changes > 0;
    },

    addToPlaylist(id, trackIds) {
      let pos = get('SELECT COALESCE(MAX(position), -1) AS m FROM playlist_tracks WHERE playlist_id = ?', id).m;
      const ins = db.prepare('INSERT INTO playlist_tracks(playlist_id, position, track_id) SELECT ?, ?, id FROM tracks WHERE id = ?');
      db.exec('BEGIN');
      try {
        for (const t of trackIds) ins.run(id, ++pos, Number(t));
        db.prepare('UPDATE playlists SET updated_at = ? WHERE id = ?').run(now(), id);
        db.exec('COMMIT');
      } catch (e) {
        db.exec('ROLLBACK');
        throw e;
      }
    },

    removeFromPlaylist(id, position) {
      db.prepare('DELETE FROM playlist_tracks WHERE playlist_id = ? AND position = ?').run(id, position);
      db.prepare('UPDATE playlists SET updated_at = ? WHERE id = ?').run(now(), id);
    },

    /** Moves the entry at `from` so it lands at index `to` of the current order. */
    movePlaylistTrack(id, from, to) {
      const rows = all('SELECT position, track_id FROM playlist_tracks WHERE playlist_id = ? ORDER BY position', id);
      const i = rows.findIndex((r) => r.position === from);
      if (i < 0) return;
      const [moved] = rows.splice(i, 1);
      rows.splice(Math.max(0, Math.min(to, rows.length)), 0, moved);
      db.exec('BEGIN');
      try {
        db.prepare('DELETE FROM playlist_tracks WHERE playlist_id = ?').run(id);
        const ins = db.prepare('INSERT INTO playlist_tracks(playlist_id, position, track_id) VALUES (?, ?, ?)');
        rows.forEach((r, n) => ins.run(id, n, r.track_id));
        db.prepare('UPDATE playlists SET updated_at = ? WHERE id = ?').run(now(), id);
        db.exec('COMMIT');
      } catch (e) {
        db.exec('ROLLBACK');
        throw e;
      }
    },
  };
  return lib;
}
