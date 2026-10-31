import crypto from 'node:crypto';

// Stateless signed tokens. The tag of the password goes inside, so changing
// MUSIC_PASSWORD signs every device out.
export function createAuth(config) {
  const sign = (payload) => crypto.createHmac('sha256', config.secret).update(payload).digest('base64url');
  const pwTag = crypto.createHash('sha256').update(config.password).digest('base64url').slice(0, 10);

  function issue(device = 'web') {
    const payload = Buffer.from(JSON.stringify({ iat: Date.now(), pw: pwTag, dev: String(device).slice(0, 40) })).toString('base64url');
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
      return JSON.parse(Buffer.from(payload, 'base64url').toString()).pw === pwTag;
    } catch {
      return false;
    }
  }

  function checkPassword(password) {
    const a = crypto.createHash('sha256').update(String(password ?? '')).digest();
    const b = crypto.createHash('sha256').update(config.password).digest();
    return crypto.timingSafeEqual(a, b);
  }

  // <audio> and <img> cannot send headers, so the token may also come as ?token=.
  function middleware(req, res, next) {
    const header = req.headers.authorization;
    const token = header?.startsWith('Bearer ') ? header.slice(7) : req.query.token;
    if (verify(token)) return next();
    res.status(401).json({ error: 'No autorizado' });
  }

  return { issue, verify, checkPassword, middleware };
}
