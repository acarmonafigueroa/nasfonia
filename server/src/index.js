import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { config } from './config.js';
import { openDb } from './db.js';
import { createLibrary } from './library.js';
import { Scanner } from './scanner.js';
import { createFolders } from './folders.js';
import { createAuth } from './auth.js';
import { apiRouter } from './routes.js';

const here = path.dirname(fileURLToPath(import.meta.url));

const db = openDb(path.join(config.dataDir, 'library.db'));
const lib = createLibrary(db);
const folders = createFolders(db, config);
const scanner = new Scanner(db, config);
const auth = createAuth(config);

const app = express();
app.disable('x-powered-by');
app.use('/api', apiRouter({ lib, scanner, folders, auth, config }));
app.use(express.static(path.join(here, '..', 'public'), { index: 'index.html', maxAge: '1h' }));

app.listen(config.port, '0.0.0.0', () => {
  console.log(`${config.serverName} escuchando en http://0.0.0.0:${config.port}`);
  console.log(`Carpetas de música: ${config.musicDirs.join(', ')}`);
  if (!config.passwordFromEnv) {
    console.log(`MUSIC_PASSWORD no está definida. Contraseña generada: ${config.password} (guardada en ${path.join(config.dataDir, 'password.txt')})`);
  }
  scanner.scan();
  if (config.scanIntervalMin > 0) setInterval(() => scanner.scan(), config.scanIntervalMin * 60_000);
});
