import express from 'express';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { checkFfmpeg, readLyrics } from './scanner.js';

const VERSION = '1.4.0';
const NON_WEB_AUDIO = new Set(['.wma', '.ape', '.wv', '.aif', '.aiff']);

export function apiRouter({ libs, scanner, folders, auth, config }) {
  const r = express.Router();
  r.use(express.json({ limit: '1mb' }));

  const id = (req) => Number.parseInt(req.params.id, 10);
  const found = (res, value) => (value ? res.json(value) : res.status(404).json({ error: 'No encontrado' }));
  // Handlers that may throw an error with a .status and a user-facing message.
  const safe = (fn) => async (req, res) => {
    try {
      await fn(req, res);
    } catch (e) {
      if (!e.status) console.error(e);
      res.status(e.status || 500).json({ error: e.status ? e.message : 'Error interno' });
    }
  };

  // --- public -------------------------------------------------------------
  r.get('/ping', (req, res) => res.json({ name: config.serverName, version: VERSION, app: 'nasfonia' }));

  r.post(
    '/login',
    safe(async (req, res) => {
      try {
        const { user, token } = auth.login(req.ip, req.body?.username, req.body?.password, req.body?.device);
        const me = auth.describe(user);
        // defaultPassword kept for apps before 1.3.0.
        res.json({ token, name: config.serverName, version: VERSION, user: me, defaultPassword: me.isDefaultPassword });
      } catch (e) {
        // Slows down password guessing.
        if (e.status === 401) await new Promise((ok) => setTimeout(ok, 800));
        throw e;
      }
    }),
  );

  r.use(auth.middleware);
  // The library as the signed-in user sees it (their favorites, playlists, history).
  r.use((req, res, next) => {
    req.lib = libs.forUser(req.user.id);
    next();
  });

  // --- me -----------------------------------------------------------------
  r.get('/me', (req, res) => res.json(auth.describe(req.user)));

  // Kept for apps before 1.3.0.
  r.get('/password', (req, res) => {
    const me = auth.describe(req.user);
    res.json({ changeable: me.passwordChangeable, isDefault: me.isDefaultPassword });
  });
  r.post(
    '/password',
    safe(async (req, res) => {
      try {
        // Every other device of this user is signed out; this one gets a fresh token.
        res.json({ token: auth.changePassword(req.user, req.body?.current, req.body?.next, req.body?.device) });
      } catch (e) {
        if (e.status === 403) await new Promise((ok) => setTimeout(ok, 800));
        throw e;
      }
    }),
  );

  // --- library ------------------------------------------------------------
  r.get('/home', (req, res) => res.json(req.lib.home()));
  r.get('/stats', (req, res) => res.json(req.lib.stats()));
  r.get('/tracks', (req, res) => res.json(req.lib.tracks(req.query)));
  r.get('/tracks/random', (req, res) => res.json(req.lib.randomTracks(req.query.limit)));
  r.get('/tracks/:id/radio', (req, res) => res.json(req.lib.radio(id(req), req.query.limit)));
  r.get('/tracks/:id', (req, res) => found(res, req.lib.track(id(req))));
  r.get(
    '/lyrics/:id',
    safe(async (req, res) => {
      const lyr = await scanner.getLyrics(id(req), { force: req.query.force === '1' });
      if (!lyr) return res.status(404).json({ error: 'No encontrado' });
      res.json(lyr);
    }),
  );
  r.get('/albums', (req, res) => res.json(req.lib.albums(req.query)));
  r.get('/albums/:id', (req, res) => found(res, req.lib.album(id(req))));
  r.get('/artists', (req, res) => res.json(req.lib.artists()));
  r.get('/artists/:id', (req, res) => found(res, req.lib.artist(id(req))));
  r.get('/artists/:id/tracks', (req, res) => res.json(req.lib.artistTracks(id(req))));
  r.get('/genres', (req, res) => res.json(req.lib.genres()));
  r.get('/genres/:name/tracks', (req, res) => res.json(req.lib.genreTracks(req.params.name)));
  r.get('/search', (req, res) => res.json(req.lib.search(String(req.query.q ?? ''))));

  // --- media --------------------------------------------------------------
  r.get(
    '/stream/:id',
    safe(async (req, res) => {
      const t = req.lib.trackFile(id(req));
      if (!t) return res.status(404).json({ error: 'No encontrado' });
      const ext = path.extname(t.path).toLowerCase();
      const reqBitrate = Number.parseInt(req.query.bitrate, 10);
      const wantTranscode =
        (Number.isFinite(reqBitrate) && reqBitrate >= 64 && reqBitrate <= 320) ||
        req.query.transcode === '1' ||
        (NON_WEB_AUDIO.has(ext) && req.query.raw !== '1');

      if (wantTranscode && (await checkFfmpeg())) {
        const br = Number.isFinite(reqBitrate) && reqBitrate >= 64 && reqBitrate <= 320 ? `${reqBitrate}k` : '192k';
        const seek = Number.parseFloat(req.query.seek);
        const args = ['-loglevel', 'error'];
        if (Number.isFinite(seek) && seek > 0) args.push('-ss', String(seek));
        args.push('-i', t.path, '-vn', '-map_metadata', '-1', '-c:a', 'libmp3lame', '-b:a', br, '-f', 'mp3', 'pipe:1');
        res.setHeader('Content-Type', 'audio/mpeg');
        res.setHeader('Cache-Control', 'no-store');
        const ff = spawn('ffmpeg', args, { stdio: ['ignore', 'pipe', 'ignore'] });
        req.on('close', () => ff.kill('SIGKILL'));
        ff.stdout.pipe(res);
        ff.on('error', () => {
          if (!res.headersSent) res.status(500).end();
        });
        return;
      }

      // sendFile answers Range requests, which is what lets players seek.
      res.sendFile(t.path, { dotfiles: 'allow', acceptRanges: true, headers: { 'Cache-Control': 'private, max-age=86400' } }, (err) => {
        if (err && !res.headersSent) res.status(err.status || 500).json({ error: 'No se puede leer el archivo' });
      });
    }),
  );

  r.get(
    '/cover/:id',
    safe(async (req, res) => {
      const albumId = id(req);
      let cover = req.lib.albumCover(albumId);
      if (!cover) cover = await scanner.ensureAlbumCover(albumId);
      if (!cover) return res.status(404).end();
      res.sendFile(path.join(config.coversDir, cover), { headers: { 'Cache-Control': 'private, max-age=604800' } }, (err) => {
        if (err && !res.headersSent) res.status(404).end();
      });
    }),
  );

  // --- favorites & plays --------------------------------------------------
  r.get('/favorites', (req, res) => res.json(req.lib.favorites()));
  r.put('/favorites/:id', (req, res) => {
    req.lib.setFavorite(id(req), true);
    res.status(204).end();
  });
  r.delete('/favorites/:id', (req, res) => {
    req.lib.setFavorite(id(req), false);
    res.status(204).end();
  });
  r.post('/plays/:id', (req, res) => {
    req.lib.recordPlay(id(req));
    res.status(204).end();
  });

  // --- playlists (users see their own and shared ones; only owners edit) ----
  const name = (req) => String(req.body?.name ?? '').trim().slice(0, 120);
  const trackIds = (req) => (Array.isArray(req.body?.trackIds) ? req.body.trackIds.map(Number).filter(Number.isFinite) : []);
  const ownPlaylist = (req, res, next) => {
    const p = req.lib.playlist(id(req));
    if (!p) return res.status(404).json({ error: 'No encontrado' });
    if (!p.isMine) return res.status(403).json({ error: 'Solo el creador de la playlist puede modificarla' });
    next();
  };

  r.get('/playlists', (req, res) => res.json(req.lib.playlists()));
  r.post('/playlists', (req, res) => {
    const n = name(req);
    if (!n) return res.status(400).json({ error: 'Falta el nombre' });
    res.status(201).json(req.lib.createPlaylist(n, trackIds(req), Boolean(req.body?.isPublic)));
  });
  r.get('/playlists/:id', (req, res) => found(res, req.lib.playlist(id(req))));
  r.patch('/playlists/:id', ownPlaylist, (req, res) => {
    const n = req.body?.name != null ? name(req) : undefined;
    if (req.body?.name != null && !n) return res.status(400).json({ error: 'Falta el nombre' });
    const isPublic = req.body?.isPublic != null ? Boolean(req.body.isPublic) : undefined;
    req.lib.updatePlaylist(id(req), { name: n, isPublic });
    res.json(req.lib.playlist(id(req)));
  });
  r.delete('/playlists/:id', ownPlaylist, (req, res) => {
    req.lib.deletePlaylist(id(req));
    res.status(204).end();
  });
  r.post('/playlists/:id/tracks', ownPlaylist, (req, res) => {
    req.lib.addToPlaylist(id(req), trackIds(req));
    res.json(req.lib.playlist(id(req)));
  });
  r.delete('/playlists/:id/tracks/:position', ownPlaylist, (req, res) => {
    req.lib.removeFromPlaylist(id(req), Number(req.params.position));
    res.json(req.lib.playlist(id(req)));
  });
  r.post('/playlists/:id/move', ownPlaylist, (req, res) => {
    req.lib.movePlaylistTrack(id(req), Number(req.body?.from), Number(req.body?.to));
    res.json(req.lib.playlist(id(req)));
  });

  // --- scan status (everyone may look) -------------------------------------
  r.get('/scan', (req, res) => res.json(scanner.status));

  // ======================= administrators only ===========================
  r.use(auth.adminOnly);

  r.post('/scan', (req, res) => {
    scanner.scan({ full: req.query.full === '1' });
    res.status(202).json(scanner.status);
  });

  r.get('/folders', safe(async (req, res) => res.json(await folders.info())));
  r.get('/folders/browse', safe(async (req, res) => res.json(await folders.browse(req.query.path))));
  r.put(
    '/folders',
    safe(async (req, res) => {
      const dirs = Array.isArray(req.body?.dirs) ? req.body.dirs : [];
      if (!dirs.length) return res.status(400).json({ error: 'Elige al menos una carpeta' });
      await folders.setDirs(dirs);
      // Songs from folders that left the list are dropped by this scan.
      scanner.scan();
      res.json(await folders.info());
    }),
  );

  r.get('/users', (req, res) => res.json(auth.users.list()));
  r.post('/users', safe(async (req, res) => res.status(201).json(auth.users.create(req.body?.username, req.body?.password, Boolean(req.body?.isAdmin)))));
  r.patch('/users/:id', safe(async (req, res) => res.json(auth.users.update(req.user, id(req), { password: req.body?.password, isAdmin: req.body?.isAdmin }))));
  r.delete(
    '/users/:id',
    safe(async (req, res) => {
      auth.users.remove(req.user, id(req));
      res.status(204).end();
    }),
  );

  r.use((err, req, res, _next) => {
    console.error(err);
    res.status(500).json({ error: 'Error interno' });
  });
  return r;
}
