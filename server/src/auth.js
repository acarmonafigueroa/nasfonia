import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

export const DEFAULT_PASSWORD = 'adminadmin';
const ADMIN_ID = 1;

const sha = (s) => crypto.createHash('sha256').update(String(s)).digest();

export function hashPassword(password) {
  const salt = crypto.randomBytes(16);
  const hash = crypto.scryptSync(String(password), salt, 32);
  return `scrypt$${salt.toString('base64')}$${hash.toString('base64')}`;
}

function matchesHash(password, stored) {
  const [, salt, hash] = stored.split('$');
  const expected = Buffer.from(hash, 'base64');
  const got = crypto.scryptSync(String(password ?? ''), Buffer.from(salt, 'base64'), expected.length);
  return crypto.timingSafeEqual(got, expected);
}

const httpError = (status, message) => Object.assign(new Error(message), { status });

/**
 * Users and sessions.
 *
 * The first admin (id 1) is special: MUSIC_PASSWORD, when set, fixes its password, and a
 * NULL password_hash means it is still on DEFAULT_PASSWORD (the apps ask to change it).
 * Every other user always has a hash. Tokens carry a tag of the user's current credential,
 * so changing a password (or deleting the user) signs that user's devices out.
 */
export function createAuth(config, db) {
  const q = {
    byId: db.prepare('SELECT id, username, password_hash, is_admin FROM users WHERE id = ?'),
    byName: db.prepare('SELECT id, username, password_hash, is_admin FROM users WHERE username = ?'),
    list: db.prepare('SELECT id, username, is_admin, created_at FROM users ORDER BY id'),
    setHash: db.prepare('UPDATE users SET password_hash = ? WHERE id = ?'),
    insert: db.prepare('INSERT INTO users(username, password_hash, is_admin, created_at) VALUES (?, ?, ?, ?)'),
    setAdmin: db.prepare('UPDATE users SET is_admin = ? WHERE id = ?'),
    admins: db.prepare('SELECT COUNT(*) AS n FROM users WHERE is_admin = 1'),
  };

  // Passwords from before users existed become the admin's.
  const passwordFile = path.join(config.dataDir, 'password.txt');
  const legacyHash = db.prepare("SELECT value FROM settings WHERE key = 'passwordHash'").get();
  if (legacyHash || fs.existsSync(passwordFile)) {
    if (q.byId.get(ADMIN_ID)?.password_hash == null) {
      q.setHash.run(legacyHash ? legacyHash.value : hashPassword(fs.readFileSync(passwordFile, 'utf8').trim()), ADMIN_ID);
    }
    db.prepare("DELETE FROM settings WHERE key = 'passwordHash'").run();
    fs.rmSync(passwordFile, { force: true });
  }

  // Forgotten password: RESET_PASSWORD=1 puts the first admin back on the default.
  if (process.env.RESET_PASSWORD === '1') {
    q.setHash.run(null, ADMIN_ID);
    console.log('RESET_PASSWORD=1: el usuario admin vuelve a la contraseña de serie (adminadmin). Quita esa variable del docker-compose.');
  }

  const envFixed = (u) => u.id === ADMIN_ID && Boolean(config.passwordEnv);
  const onDefault = (u) => !envFixed(u) && u.password_hash == null;

  function checkUserPassword(u, password) {
    if (envFixed(u)) return crypto.timingSafeEqual(sha(password ?? ''), sha(config.passwordEnv));
    if (u.password_hash == null) return crypto.timingSafeEqual(sha(password ?? ''), sha(DEFAULT_PASSWORD));
    return matchesHash(password, u.password_hash);
  }

  const sign = (payload) => crypto.createHmac('sha256', config.secret).update(payload).digest('base64url');
  const tag = (u) =>
    crypto.createHash('sha256').update(`${u.id}:${envFixed(u) ? `env:${config.passwordEnv}` : u.password_hash ?? 'default'}`).digest('base64url').slice(0, 12);

  function issue(u, device = 'web') {
    const payload = Buffer.from(JSON.stringify({ uid: u.id, pw: tag(u), iat: Date.now(), dev: String(device).slice(0, 40) })).toString('base64url');
    return `${payload}.${sign(payload)}`;
  }

  /** The user a token belongs to, or null. */
  function verify(token) {
    if (typeof token !== 'string') return null;
    const [payload, sig] = token.split('.');
    if (!payload || !sig) return null;
    const expected = Buffer.from(sign(payload));
    const given = Buffer.from(sig);
    if (given.length !== expected.length || !crypto.timingSafeEqual(given, expected)) return null;
    try {
      const data = JSON.parse(Buffer.from(payload, 'base64url').toString());
      // Tokens from before 1.3.0 have no uid; they were the only (admin) user's.
      const u = q.byId.get(data.uid ?? ADMIN_ID);
      return u && data.pw === tag(u) ? u : null;
    } catch {
      return null;
    }
  }

  // --- brute-force protection: per client IP, 8 failures in 15 min lock it for 15 min.
  const attempts = new Map();
  const WINDOW = 15 * 60_000;
  const MAX_FAILS = 8;
  function pruneAttempts() {
    if (attempts.size < 200) return;
    const now = Date.now();
    for (const [k, v] of attempts) {
      if (now - v.first > WINDOW) attempts.delete(k);
    }
  }
  function lockedFor(ip) {
    const a = attempts.get(ip);
    if (!a) return 0;
    if (Date.now() - a.first > WINDOW) {
      attempts.delete(ip);
      return 0;
    }
    return a.fails >= MAX_FAILS ? Math.ceil((a.first + WINDOW - Date.now()) / 60_000) : 0;
  }
  function failed(ip) {
    pruneAttempts();
    const a = attempts.get(ip);
    if (!a || Date.now() - a.first > WINDOW) attempts.set(ip, { first: Date.now(), fails: 1 });
    else a.fails++;
  }

  /** Throws a user-facing error or returns { user, token }. */
  function login(ip, username, password, device) {
    const minutes = lockedFor(ip);
    if (minutes) throw httpError(429, `Demasiados intentos fallidos. Espera ${minutes} min.`);
    // Apps before 1.3.0 send only the password: that was the admin.
    const u = q.byName.get(String(username || 'admin').trim());
    if (!u || !checkUserPassword(u, password)) {
      failed(ip);
      throw httpError(401, 'Usuario o contraseña incorrectos');
    }
    attempts.delete(ip);
    return { user: u, token: issue(u, device) };
  }

  function validateNew(next) {
    next = String(next ?? '');
    if (next.length < 6) throw httpError(400, 'La contraseña debe tener al menos 6 caracteres');
    if (next === DEFAULT_PASSWORD) throw httpError(400, 'Elige una contraseña distinta de la de serie');
    return next;
  }

  /** The signed-in user changes their own password; returns a fresh token for this device. */
  function changePassword(u, current, next, device) {
    if (envFixed(u)) throw httpError(409, 'La contraseña del usuario admin está fijada con MUSIC_PASSWORD en el docker-compose. Cámbiala allí.');
    if (!checkUserPassword(u, current)) throw httpError(403, 'La contraseña actual no es correcta');
    q.setHash.run(hashPassword(validateNew(next)), u.id);
    return issue(q.byId.get(u.id), device);
  }

  function describe(u) {
    return {
      id: u.id,
      username: u.username,
      isAdmin: Boolean(u.is_admin),
      isDefaultPassword: onDefault(u),
      passwordChangeable: !envFixed(u),
    };
  }

  // --- admin: manage users ----------------------------------------------
  const validName = (name) => {
    name = String(name ?? '').trim();
    if (!/^[\p{L}\p{N}._-]{2,32}$/u.test(name)) throw httpError(400, 'El usuario debe tener 2–32 letras, números, puntos, guiones o guiones bajos');
    return name;
  };

  const users = {
    list: () => q.list.all().map((r) => ({ id: r.id, username: r.username, isAdmin: Boolean(r.is_admin), createdAt: r.created_at })),

    create(username, password, isAdmin) {
      const name = validName(username);
      if (q.byName.get(name)) throw httpError(409, 'Ya existe un usuario con ese nombre');
      const { lastInsertRowid } = q.insert.run(name, hashPassword(validateNew(password)), isAdmin ? 1 : 0, Date.now());
      return describe(q.byId.get(Number(lastInsertRowid)));
    },

    update(actor, id, { password, isAdmin }) {
      const u = q.byId.get(id);
      if (!u) throw httpError(404, 'No existe ese usuario');
      if (password != null) {
        if (envFixed(u)) throw httpError(409, 'La contraseña de admin está fijada con MUSIC_PASSWORD.');
        q.setHash.run(hashPassword(validateNew(password)), id);
      }
      if (isAdmin != null && Boolean(u.is_admin) !== Boolean(isAdmin)) {
        if (!isAdmin && q.admins.get().n <= 1) throw httpError(400, 'Tiene que quedar al menos un administrador');
        if (!isAdmin && id === actor.id) throw httpError(400, 'No puedes quitarte a ti mismo el permiso de administrador');
        q.setAdmin.run(isAdmin ? 1 : 0, id);
      }
      return describe(q.byId.get(id));
    },

    remove(actor, id) {
      const u = q.byId.get(id);
      if (!u) throw httpError(404, 'No existe ese usuario');
      if (id === actor.id) throw httpError(400, 'No puedes borrarte a ti mismo');
      if (u.is_admin && q.admins.get().n <= 1) throw httpError(400, 'Tiene que quedar al menos un administrador');
      db.exec('BEGIN');
      try {
        db.prepare('DELETE FROM favorites WHERE user_id = ?').run(id);
        db.prepare('DELETE FROM plays WHERE user_id = ?').run(id);
        db.prepare('DELETE FROM playlists WHERE user_id = ?').run(id);
        db.prepare('DELETE FROM users WHERE id = ?').run(id);
        db.exec('COMMIT');
      } catch (e) {
        db.exec('ROLLBACK');
        throw e;
      }
    },
  };

  // <audio> and <img> cannot send headers, so the token may also come as ?token=.
  function middleware(req, res, next) {
    const header = req.headers.authorization;
    const token = header?.startsWith('Bearer ') ? header.slice(7) : req.query.token;
    const u = verify(token);
    if (!u) return res.status(401).json({ error: 'No autorizado' });
    req.user = u;
    next();
  }

  function adminOnly(req, res, next) {
    if (req.user?.is_admin) return next();
    res.status(403).json({ error: 'Solo un administrador puede hacer esto' });
  }

  return {
    login,
    changePassword,
    describe,
    users,
    middleware,
    adminOnly,
    get adminOnDefault() {
      return onDefault(q.byId.get(ADMIN_ID));
    },
  };
}
