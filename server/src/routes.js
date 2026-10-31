import express from 'express';
import path from 'node:path';

const VERSION = '1.1.0';

export function apiRouter({ lib, scanner, folders, auth, config }) {
  const r = express.Router();
  r.use(express.json({ limit: '1mb' }));

  const id = (req) => Number.parseInt(req.params.id, 10);
  const found = (res, value) => (value ? res.json(value) : res.status(404).json({ error: 'No encontrado' }));

  // --- public -------------------------------------------------------------
  r.get('/ping', (req, res) => res.json({ name: config.serverName, version: VERSION, app: 'nasfonia' }));

  r.post('/login', (req, res) => {
    if (!auth.checkPassword(req.body?.password)) {
      // Slows down password guessing.
      return setTimeout(() => res.status(401).json({ error: 'Contraseña incorrecta' }), 800);
    }
    res.json({ token: auth.issue(req.body?.device), name: config.serverName, version: VERSION });
  });

  r.use(auth.middleware);

  // --- library ------------------------------------------------------------
  r.get('/home', (req, res) => res.json(lib.home()));
  r.get('/stats', (req, res) => res.json(lib.stats()));
  r.get('/tracks', (req, res) => res.json(lib.tracks(req.query)));
  r.get('/tracks/random', (req, res) => res.json(lib.randomTracks(req.query.limit)));
  r.get('/tracks/:id', (req, res) => found(res, lib.track(id(req))));
  r.get('/albums', (req, res) => res.json(lib.albums(req.query)));
  r.get('/albums/:id', (req, res) => found(res, lib.album(id(req))));
  r.get('/artists', (req, res) => res.json(lib.artists()));
  r.get('/artists/:id', (req, res) => found(res, lib.artist(id(req))));
  r.get('/artists/:id/tracks', (req, res) => res.json(lib.artistTracks(id(req))));
  r.get('/genres', (req, res) => res.json(lib.genres()));
  r.get('/genres/:name/tracks', (req, res) => res.json(lib.genreTracks(req.params.name)));
  r.get('/search', (req, res) => res.json(lib.search(String(req.query.q ?? ''))));

  // --- media --------------------------------------------------------------
  r.get('/stream/:id', (req, res) => {
    const t = lib.trackFile(id(req));
    if (!t) return res.status(404).json({ error: 'No encontrado' });
    // sendFile answers Range requests, which is what lets players seek.
    res.sendFile(t.path, { dotfiles: 'allow', acceptRanges: true, headers: { 'Cache-Control': 'private, max-age=86400' } }, (err) => {
      if (err && !res.headersSent) res.status(err.status || 500).json({ error: 'No se puede leer el archivo' });
    });
  });

  r.get('/cover/:id', (req, res) => {
    const cover = lib.albumCover(id(req));
    if (!cover) return res.status(404).end();
    res.sendFile(path.join(config.coversDir, cover), { headers: { 'Cache-Control': 'private, max-age=604800' } }, (err) => {
      if (err && !res.headersSent) res.status(404).end();
    });
  });

  // --- favorites & plays --------------------------------------------------
  r.get('/favorites', (req, res) => res.json(lib.favorites()));
  r.put('/favorites/:id', (req, res) => {
    lib.setFavorite(id(req), true);
    res.status(204).end();
  });
  r.delete('/favorites/:id', (req, res) => {
    lib.setFavorite(id(req), false);
    res.status(204).end();
  });
  r.post('/plays/:id', (req, res) => {
    lib.recordPlay(id(req));
    res.status(204).end();
  });

  // --- playlists ----------------------------------------------------------
  const name = (req) => String(req.body?.name ?? '').trim().slice(0, 120);
  const trackIds = (req) => (Array.isArray(req.body?.trackIds) ? req.body.trackIds.map(Number).filter(Number.isFinite) : []);

  r.get('/playlists', (req, res) => res.json(lib.playlists()));
  r.post('/playlists', (req, res) => {
    const n = name(req);
    if (!n) return res.status(400).json({ error: 'Falta el nombre' });
    res.status(201).json(lib.createPlaylist(n, trackIds(req)));
  });
  r.get('/playlists/:id', (req, res) => found(res, lib.playlist(id(req))));
  r.patch('/playlists/:id', (req, res) => {
    const n = name(req);
    if (!n) return res.status(400).json({ error: 'Falta el nombre' });
    if (!lib.renamePlaylist(id(req), n)) return res.status(404).json({ error: 'No encontrado' });
    res.json(lib.playlist(id(req)));
  });
  r.delete('/playlists/:id', (req, res) => (lib.deletePlaylist(id(req)) ? res.status(204).end() : res.status(404).json({ error: 'No encontrado' })));
  r.post('/playlists/:id/tracks', (req, res) => {
    if (!lib.playlist(id(req))) return res.status(404).json({ error: 'No encontrado' });
    lib.addToPlaylist(id(req), trackIds(req));
    res.json(lib.playlist(id(req)));
  });
  r.delete('/playlists/:id/tracks/:position', (req, res) => {
    lib.removeFromPlaylist(id(req), Number(req.params.position));
    res.json(lib.playlist(id(req)));
  });
  r.post('/playlists/:id/move', (req, res) => {
    lib.movePlaylistTrack(id(req), Number(req.body?.from), Number(req.body?.to));
    res.json(lib.playlist(id(req)));
  });

  // --- music folders ------------------------------------------------------
  const wrap = (fn) => (req, res) =>
    fn(req, res).catch((e) => res.status(e.status || 500).json({ error: e.message || 'Error interno' }));

  r.get('/folders', wrap(async (req, res) => res.json(await folders.info())));
  r.get('/folders/browse', wrap(async (req, res) => res.json(await folders.browse(req.query.path))));
  r.put('/folders', wrap(async (req, res) => {
    const dirs = Array.isArray(req.body?.dirs) ? req.body.dirs : [];
    if (!dirs.length) return res.status(400).json({ error: 'Elige al menos una carpeta' });
    await folders.setDirs(dirs);
    // Songs from folders that left the list are dropped by this scan.
    scanner.scan();
    res.json(await folders.info());
  }));

  // --- password -----------------------------------------------------------
  r.get('/password', (req, res) => res.json({ changeable: auth.source !== 'env' }));
  r.post('/password', (req, res) => {
    try {
      auth.changePassword(req.body?.current, req.body?.next);
      // Every other device is signed out; this one gets a fresh token.
      res.json({ token: auth.issue(req.body?.device) });
    } catch (e) {
      const status = e.status || 500;
      setTimeout(() => res.status(status).json({ error: e.message }), status === 403 ? 800 : 0);
    }
  });

  // --- scan ---------------------------------------------------------------
  r.get('/scan', (req, res) => res.json(scanner.status));
  r.post('/scan', (req, res) => {
    scanner.scan({ full: req.query.full === '1' });
    res.status(202).json(scanner.status);
  });

  r.use((err, req, res, _next) => {
    console.error(err);
    res.status(500).json({ error: 'Error interno' });
  });
  return r;
}
