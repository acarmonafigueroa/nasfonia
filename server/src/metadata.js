import { parseLrc } from './scanner.js';

const USER_AGENT = 'Nasfonia/1.4 (https://github.com/acarmonafigueroa/nasfonia)';
const GENERIC_ALBUMS = /^(music|m[uú]sica|unknown(\s+album)?|[aá]lbum\s+desconocido|downloads?|descargas|mp3|audio|varios|various(\s+artists)?|carpeta\s+personal|volumen\s*\d*)$/i;
const UNKNOWN_ARTISTS = /^(artista\s+desconocido|unknown(\s+artist)?|va|varios|various(\s+artists)?)$/i;

/**
 * Cleans noisy tags from filenames/ID3 tags (e.g. "(Official Video)", "[HD]", track numbers)
 * and infers artist + title when the title looks like "Artist - Song".
 */
export function cleanTrackQuery({ title = '', artist = '', album = '' }) {
  const stripNoise = (s) =>
    String(s ?? '')
      .replace(/\.(mp3|flac|m4a|m4b|aac|ogg|opus|wav|wma|ape|wv|aiff?)$/i, '')
      .replace(/^\s*\d{1,3}\s*[-._)]\s*/, '')
      .replace(/\[[^\]]*\]/g, ' ')
      .replace(/\((?:official|oficial|video|audio|lyric|lyrics|letra|visualizer|videoclip|hd|hq|4k|remaster(?:ed|izado)?|live|en\s+directo|feat\.?|ft\.?|prod\.?|con\s)[^)]*\)/gi, ' ')
      .replace(/\b(?:feat\.?|ft\.?)\s+[^-–—()]+$/i, ' ')
      .replace(/\b(?:www\.[^\s]+|\d{3}\s*kbps)\b/gi, ' ')
      .replace(/[_]+/g, ' ')
      .replace(/\s{2,}/g, ' ')
      .trim();

  let cTitle = stripNoise(title);
  let cArtist = stripNoise(artist);
  let cAlbum = stripNoise(album);

  if (UNKNOWN_ARTISTS.test(cArtist)) cArtist = '';
  if (GENERIC_ALBUMS.test(cAlbum)) cAlbum = '';

  // Many loose files have no artist tag and are named "Artist - Title".
  const sepMatch = cTitle.match(/^(.+?)\s+[-–—]\s+(.+)$/);
  if (sepMatch) {
    const left = sepMatch[1].trim();
    const right = sepMatch[2].trim();
    if (!cArtist && left.length >= 2 && right.length >= 2) {
      cArtist = left;
      cTitle = right;
    } else if (cArtist && left.toLowerCase() === cArtist.toLowerCase()) {
      cTitle = right;
    }
  }

  return { title: cTitle, artist: cArtist, album: cAlbum };
}

async function fetchJson(url, timeoutMs = 7000) {
  const res = await fetch(url, {
    headers: { 'User-Agent': USER_AGENT, Accept: 'application/json' },
    signal: AbortSignal.timeout(timeoutMs),
  });
  if (!res.ok) return null;
  return res.json();
}

async function downloadImage(url, timeoutMs = 8000) {
  const res = await fetch(url, {
    headers: { 'User-Agent': USER_AGENT },
    signal: AbortSignal.timeout(timeoutMs),
  });
  if (!res.ok) return null;
  const ct = res.headers.get('content-type') || '';
  if (ct && !ct.startsWith('image/') && !ct.includes('octet-stream')) return null;
  const buf = Buffer.from(await res.arrayBuffer());
  return buf.length > 2000 ? buf : null;
}

/**
 * Searches Deezer and iTunes for an album or track cover and returns the image Buffer (or null).
 */
export async function fetchOnlineCover({ title = '', artist = '', samples = [] }) {
  const cleanedAlbum = cleanTrackQuery({ album: title, artist });
  const queries = [];

  if (cleanedAlbum.album && cleanedAlbum.artist) {
    queries.push({ q: `${cleanedAlbum.artist} ${cleanedAlbum.album}`, type: 'album' });
  } else if (cleanedAlbum.album) {
    queries.push({ q: cleanedAlbum.album, type: 'album' });
  }

  for (const s of samples.slice(0, 3)) {
    const ct = cleanTrackQuery({ title: s.title, artist: s.artist || artist, album: title });
    const q = `${ct.artist} ${ct.title}`.trim();
    if (q.length >= 3 && !queries.some((x) => x.q.toLowerCase() === q.toLowerCase())) {
      queries.push({ q, type: 'track' });
    }
  }

  if (!queries.length && cleanedAlbum.artist) {
    queries.push({ q: cleanedAlbum.artist, type: 'track' });
  }

  for (const { q, type } of queries.slice(0, 3)) {
    // 1. Deezer API (free, no key, fast 500x500 / 1000x1000 covers)
    try {
      const endpoint = type === 'album' ? 'search/album' : 'search';
      const data = await fetchJson(`https://api.deezer.com/${endpoint}?q=${encodeURIComponent(q)}&limit=5`);
      const items = Array.isArray(data?.data) ? data.data : [];
      for (const item of items) {
        const coverUrl =
          item?.cover_big ||
          item?.cover_xl ||
          item?.album?.cover_big ||
          item?.album?.cover_xl ||
          item?.artist?.picture_big;
        if (coverUrl && !coverUrl.includes('/images/cover//')) {
          const buf = await downloadImage(coverUrl);
          if (buf) return buf;
        }
      }
    } catch {
      /* ignore and try iTunes */
    }

    // 2. iTunes Search API (free, no key, 600x600 covers)
    try {
      const entity = type === 'album' ? 'album' : 'song';
      const data = await fetchJson(`https://itunes.apple.com/search?term=${encodeURIComponent(q)}&media=music&entity=${entity}&limit=5`);
      const results = Array.isArray(data?.results) ? data.results : [];
      for (const r of results) {
        if (r?.artworkUrl100) {
          const hiRes = r.artworkUrl100.replace(/100x100bb/g, '600x600bb');
          const buf = await downloadImage(hiRes);
          if (buf) return buf;
        }
      }
    } catch {
      /* ignore */
    }
  }

  return null;
}

/**
 * Searches LRCLIB (and Lyrics.ovh fallback) for synced or plain lyrics.
 */
export async function fetchOnlineLyrics({ title = '', artist = '', album = '', duration = 0 }) {
  const { title: cTitle, artist: cArtist, album: cAlbum } = cleanTrackQuery({ title, artist, album });
  if (!cTitle) return { synced: false, lines: [], source: null };

  // 1. LRCLIB (https://lrclib.net/api) — synced .lrc + plain lyrics
  const urls = [];
  if (cArtist) {
    const params = new URLSearchParams({ track_name: cTitle, artist_name: cArtist });
    if (cAlbum) params.set('album_name', cAlbum);
    urls.push(`https://lrclib.net/api/search?${params.toString()}`);
  }
  const combinedQuery = `${cArtist} ${cTitle}`.trim();
  if (combinedQuery) {
    urls.push(`https://lrclib.net/api/search?q=${encodeURIComponent(combinedQuery)}`);
  }

  for (const url of urls) {
    try {
      const list = await fetchJson(url);
      if (Array.isArray(list) && list.length) {
        const candidates = list.filter((item) => item?.syncedLyrics || item?.plainLyrics);
        if (candidates.length) {
          candidates.sort((a, b) => scoreLrcCandidate(b, duration, cArtist) - scoreLrcCandidate(a, duration, cArtist));
          const best = candidates[0];
          const raw = best.syncedLyrics || best.plainLyrics;
          const parsed = parseLrc(raw);
          if (parsed.lines.length) {
            return { ...parsed, source: 'lrclib' };
          }
        }
      }
    } catch {
      /* try next */
    }
  }

  // 2. Fallback: lyrics.ovh (plain lyrics)
  if (cArtist && cTitle) {
    try {
      const data = await fetchJson(
        `https://api.lyrics.ovh/v1/${encodeURIComponent(cArtist)}/${encodeURIComponent(cTitle)}`,
        5000,
      );
      if (typeof data?.lyrics === 'string' && data.lyrics.trim()) {
        const cleaned = data.lyrics.replace(/^Paroles de la chanson .*?\r?\n/i, '').trim();
        const parsed = parseLrc(cleaned);
        if (parsed.lines.length) {
          return { ...parsed, source: 'lyrics.ovh' };
        }
      }
    } catch {
      /* no fallback */
    }
  }

  return { synced: false, lines: [], source: null };
}

function scoreLrcCandidate(item, targetDuration, targetArtist) {
  let score = 0;
  if (item.syncedLyrics) score += 100;
  if (targetDuration > 0 && Number.isFinite(item.duration)) {
    const diff = Math.abs(item.duration - targetDuration);
    if (diff <= 3) score += 50;
    else if (diff <= 10) score += 30;
    else if (diff <= 25) score += 10;
    else score -= 20;
  }
  if (targetArtist && item.artistName) {
    const a = targetArtist.toLowerCase();
    const b = String(item.artistName).toLowerCase();
    if (a === b) score += 35;
    else if (b.includes(a) || a.includes(b)) score += 20;
  }
  return score;
}
