import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

const sha = (s) => crypto.createHash('sha256').update(String(s)).digest();

function hashPassword(password) {
  const salt = crypto.randomBytes(16);
  const hash = crypto.scryptSync(password, salt, 32);
  return `scrypt$${salt.toString('base64')}$${hash.toString('base64')}`;
}

function matchesHash(password, stored) {
  const [, salt, hash] = stored.split('$');
  const expected = Buffer.from(hash, 'base64');
  const got = crypto.scryptSync(String(password ?? ''), Buffer.from(salt, 'base64'), expected.length);
  return crypto.timingSafeEqual(got, expected);
}

/**
 * Where the password comes from, by priority:
 *  1. MUSIC_PASSWORD (environment) — fixed, cannot be changed from the apps.
 *  2. A scrypt hash in the settings table — set by "Cambiar contraseña".
 *  3. data/password.txt — generated on first start so a fresh install has one.
 * Tokens carry a tag of the current credential, so changing it signs every device out.
 */
export function createAuth(config, db) {
  const readHash = db.prepare("SELECT value FROM settings WHERE key = 'passwordHash'");
  const writeHash = db.prepare("INSERT INTO settings(key, value) VALUES ('passwordHash', ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value");
  const passwordFile = path.join(config.dataDir, 'password.txt');

  let source; // 'env' | 'hash' | 'file'
  let credential; // plain password (env/file) or stored hash
  let generated = null;

  function load() {
    if (config.passwordEnv) {
      source = 'env';
      credential = config.passwordEnv;
      return;
    }
    const row = readHash.get();
    if (row) {
      source = 'hash';
      credential = row.value;
      return;
    }
    source = 'file';
    if (fs.existsSync(passwordFile)) credential = fs.readFileSync(passwordFile, 'utf8').trim();
    else {
      credential = crypto.randomBytes(6).toString('hex');
      fs.writeFileSync(passwordFile, credential);
      generated = credential;
    }
  }
  load();

  const sign = (payload) => crypto.createHmac('sha256', config.secret).update(payload).digest('base64url');
  const tag = () => crypto.createHash('sha256').update(`${source}:${credential}`).digest('base64url').slice(0, 10);

  function issue(device = 'web') {
    const payload = Buffer.from(JSON.stringify({ iat: Date.now(), pw: tag(), dev: String(device).slice(0, 40) })).toString('base64url');
    return `${payload}.${sign(payload)}`;
  }

  function verify(token) {
    if (typeof token !== 'string') return false;
    const [payload, sig] = token.split('.');
    if (!payload || !sig) return false;
    const expected = Buffer.from(sign(payload));
    const given = Buffer.from(sig);
    if (given.length !== expected.length || !crypto.timingSafeEqual(given, expected)) return false;
    try {
      return JSON.parse(Buffer.from(payload, 'base64url').toString()).pw === tag();
    } catch {
      return false;
    }
  }

  function checkPassword(password) {
    if (source === 'hash') return matchesHash(password, credential);
    return crypto.timingSafeEqual(sha(password ?? ''), sha(credential));
  }

  /** Throws with a user-facing message; on success every other device is signed out. */
  function changePassword(current, next) {
    if (source === 'env') {
      throw Object.assign(new Error('La contraseña está fijada con MUSIC_PASSWORD en el docker-compose. Cámbiala allí.'), { status: 409 });
    }
    if (!checkPassword(current)) throw Object.assign(new Error('La contraseña actual no es correcta'), { status: 403 });
    next = String(next ?? '');
    if (next.length < 6) throw Object.assign(new Error('La nueva contraseña debe tener al menos 6 caracteres'), { status: 400 });
    writeHash.run(hashPassword(next));
    // The plain-text copy is no longer needed.
    fs.rmSync(passwordFile, { force: true });
    load();
  }

  // <audio> and <img> cannot send headers, so the token may also come as ?token=.
  function middleware(req, res, next) {
    const header = req.headers.authorization;
    const token = header?.startsWith('Bearer ') ? header.slice(7) : req.query.token;
    if (verify(token)) return next();
    res.status(401).json({ error: 'No autorizado' });
  }

  return {
    issue,
    verify,
    checkPassword,
    changePassword,
    middleware,
    get source() {
      return source;
    },
    /** Set only on the start that created password.txt, to print it once. */
    generated,
    passwordFile,
  };
}
