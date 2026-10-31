// Nasfonía – web client (no build step, plain ES modules).

// ---------------------------------------------------------------- icons
const ICONS = {
  home: 'M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z',
  search: 'M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z',
  library: 'M20 2H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 5h-3v5.5a2.5 2.5 0 0 1-5 0 2.5 2.5 0 0 1 4-2V5h4v2zM4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6z',
  add: 'M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z',
  play: 'M8 5v14l11-7z',
  pause: 'M6 19h4V5H6v14zm8-14v14h4V5h-4z',
  next: 'M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z',
  prev: 'M6 6h2v12H6zm3.5 6l8.5 6V6z',
  shuffle: 'M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z',
  repeat: 'M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z',
  repeatOne: 'M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4zm-4-2V9h-1l-2 1v1h1.5v4H13z',
  heart: 'M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z',
  heartOutline: 'M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z',
  volume: 'M3 9v6h4l5 5V4L7 9H3zm13.5 3A4.5 4.5 0 0 0 14 7.97v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z',
  mute: 'M16.5 12A4.5 4.5 0 0 0 14 7.97v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51A8.8 8.8 0 0 0 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06a8.99 8.99 0 0 0 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z',
  queue: 'M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18A3 3 0 1 0 19 17V8h3V6h-5z',
  more: 'M6 10a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm12 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm-6 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4z',
  clock: 'M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z',
  note: 'M12 3v10.55A4 4 0 1 0 14 17V7h4V3h-6z',
  album: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 14.5a4.5 4.5 0 1 1 0-9 4.5 4.5 0 0 1 0 9zm0-5.5a1 1 0 1 0 0 2 1 1 0 0 0 0-2z',
  person: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z',
  chevronLeft: 'M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z',
  chevronRight: 'M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z',
  refresh: 'M17.65 6.35A7.96 7.96 0 0 0 12 4a8 8 0 1 0 7.73 10h-2.08A6 6 0 1 1 12 6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z',
  logout: 'M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z',
  trash: 'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z',
  close: 'M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z',
  playlistAdd: 'M14 10H2v2h12v-2zm0-4H2v2h12V6zm4 8v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zM2 16h8v-2H2v2z',
  edit: 'M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z',
  playNext: 'M3 10h11v2H3zm0-4h11v2H3zm0 8h7v2H3zm13-1v8l6-4z',
};
const icon = (name, cls = '') => `<svg class="ic ${cls}" viewBox="0 0 24 24" aria-hidden="true"><path d="${ICONS[name]}"/></svg>`;
const hydrateIcons = (root = document) => root.querySelectorAll('[data-icon]').forEach((el) => (el.outerHTML = icon(el.dataset.icon)));

// ---------------------------------------------------------------- utils
const $ = (s, r = document) => r.querySelector(s);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const fmt = (sec) => {
  sec = Math.max(0, Math.floor(sec || 0));
  const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60), s = String(sec % 60).padStart(2, '0');
  return h ? `${h}:${String(m).padStart(2, '0')}:${s}` : `${m}:${s}`;
};
const fmtLong = (sec) => {
  const h = Math.floor(sec / 3600), m = Math.round((sec % 3600) / 60);
  return h ? `${h} h ${m} min` : `${m} min`;
};
const plural = (n, one, many) => `${n} ${n === 1 ? one : many}`;
const hueOf = (n) => `hsl(${(Number(n) * 137.508) % 360} 45% 35%)`;
const shuffled = (arr) => {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
};
const store = {
  get(k, d) { try { const v = localStorage.getItem(k); return v == null ? d : JSON.parse(v); } catch { return d; } },
  set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch { /* private mode */ } },
  del(k) { try { localStorage.removeItem(k); } catch { /* private mode */ } },
};

let toastTimer;
function toast(msg) {
  const t = $('#toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('show'), 2500);
}

// ---------------------------------------------------------------- api
let token = store.get('nm.token', null);

async function api(path, { method = 'GET', body } = {}) {
  const res = await fetch(`/api${path}`, {
    method,
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401 && path !== '/login') {
    logout();
    throw new Error('Sesión caducada');
  }
  const data = res.status === 204 ? null : await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.error || `Error ${res.status}`);
  return data;
}
const coverUrl = (albumId) => `/api/cover/${albumId}?token=${encodeURIComponent(token)}`;
const streamUrl = (id) => `/api/stream/${id}?token=${encodeURIComponent(token)}`;

function coverHtml(albumId, has, { cls = '', fallback = 'note' } = {}) {
  const img = has && albumId ? `<img loading="lazy" src="${coverUrl(albumId)}" alt="" onerror="this.remove()">` : '';
  return `<div class="cover ${cls}">${icon(fallback)}${img}</div>`;
}

// ---------------------------------------------------------------- shared state
const favs = new Set();
let playlists = [];

async function refreshPlaylists() {
  playlists = await api('/playlists');
  renderLibrary();
}

async function setFavorite(id, on) {
  await api(`/favorites/${id}`, { method: on ? 'PUT' : 'DELETE' });
  if (on) favs.add(id);
  else favs.delete(id);
  document.querySelectorAll(`[data-fav="${id}"]`).forEach((b) => {
    b.classList.toggle('on', on);
    b.innerHTML = icon(on ? 'heart' : 'heartOutline', 'sm');
  });
  updateNowPlaying();
  if (libTab === 'playlists') renderLibrary();
  toast(on ? 'Añadida a Canciones que te gustan' : 'Eliminada de Canciones que te gustan');
  if (location.hash === '#/liked') route();
}

// ---------------------------------------------------------------- player
const audio = new Audio();
audio.preload = 'auto';
const player = {
  queue: [],
  original: null, // order before shuffling
  index: -1,
  shuffle: false,
  repeat: 'off', // off | all | one
  counted: false,
  context: null, // e.g. "#/album/3", to highlight what is playing
};
const current = () => player.queue[player.index] ?? null;

function savePlayer() {
  store.set('nm.player', {
    queue: player.queue, original: player.original, index: player.index, shuffle: player.shuffle,
    repeat: player.repeat, time: audio.currentTime, context: player.context,
  });
}

function playList(tracks, start = 0, { shuffle = false, context = location.hash } = {}) {
  if (!tracks.length) return;
  player.context = context;
  if (shuffle) {
    player.shuffle = true;
    const first = tracks[start] && start > 0 ? tracks[start] : tracks[Math.floor(Math.random() * tracks.length)];
    player.original = [...tracks];
    player.queue = [first, ...shuffled(tracks.filter((t) => t !== first))];
    start = 0;
  } else if (player.shuffle) {
    const first = tracks[start];
    player.original = [...tracks];
    player.queue = [first, ...shuffled(tracks.filter((_, i) => i !== start))];
    start = 0;
  } else {
    player.original = null;
    player.queue = [...tracks];
  }
  playIndex(start);
}

function playIndex(i, { autoplay = true, at = 0 } = {}) {
  if (i < 0 || i >= player.queue.length) return;
  player.index = i;
  player.counted = false;
  const t = current();
  audio.src = streamUrl(t.id);
  if (at) audio.currentTime = at;
  if (autoplay) audio.play().catch(() => {});
  updateNowPlaying();
  renderQueue();
  savePlayer();
}

function next(auto = false) {
  if (!player.queue.length) return;
  if (auto && player.repeat === 'one') return playIndex(player.index);
  if (player.index + 1 < player.queue.length) return playIndex(player.index + 1);
  if (player.repeat === 'all' || !auto) return playIndex(0);
  audio.pause();
}

function prev() {
  if (audio.currentTime > 3 || player.index === 0) audio.currentTime = 0;
  else playIndex(player.index - 1);
}

function togglePlay() {
  if (!current()) return;
  if (audio.paused) audio.play().catch(() => {});
  else audio.pause();
}

function toggleShuffle() {
  player.shuffle = !player.shuffle;
  const t = current();
  if (player.shuffle) {
    player.original = [...player.queue];
    const rest = player.queue.filter((_, i) => i !== player.index);
    player.queue = t ? [t, ...shuffled(rest)] : shuffled(rest);
    player.index = t ? 0 : -1;
  } else if (player.original) {
    player.queue = player.original;
    player.original = null;
    player.index = Math.max(0, player.queue.indexOf(t));
  }
  updateButtons();
  renderQueue();
  savePlayer();
}

function cycleRepeat() {
  player.repeat = { off: 'all', all: 'one', one: 'off' }[player.repeat];
  updateButtons();
  savePlayer();
}

function addToQueue(tracks, { next: asNext = false } = {}) {
  if (!tracks.length) return;
  if (!current()) return playList(tracks);
  const at = asNext ? player.index + 1 : player.queue.length;
  player.queue.splice(at, 0, ...tracks);
  if (player.original) player.original.push(...tracks);
  renderQueue();
  savePlayer();
  toast(asNext ? 'Se reproducirá a continuación' : 'Añadido a la cola');
}

function updateButtons() {
  const shuffleBtn = $('#btn-shuffle');
  shuffleBtn.classList.toggle('on', player.shuffle);
  const rep = $('#btn-repeat');
  rep.classList.toggle('on', player.repeat !== 'off');
  rep.innerHTML = icon(player.repeat === 'one' ? 'repeatOne' : 'repeat');
  rep.title = { off: 'Repetir', all: 'Repetir todo', one: 'Repetir una' }[player.repeat];
  $('#btn-play').innerHTML = icon(audio.paused ? 'play' : 'pause');
  $('#btn-play').title = audio.paused ? 'Reproducir' : 'Pausar';
  document.body.classList.toggle('paused', audio.paused);
  // The big button of the page that is playing turns into pause.
  const pagePlaying = !audio.paused && player.context === location.hash;
  document.querySelectorAll('.big-play[data-action="play-list"]').forEach((b) => (b.innerHTML = icon(pagePlaying ? 'pause' : 'play')));
  const vol = audio.muted ? 0 : audio.volume;
  $('#btn-mute').innerHTML = icon(vol === 0 ? 'mute' : 'volume');
}

function updateNowPlaying() {
  const t = current();
  $('#player').classList.toggle('empty', !t);
  $('#np-cover').innerHTML = t ? icon('note') + (t.hasCover ? `<img src="${coverUrl(t.albumId)}" alt="" onerror="this.remove()">` : '') : '';
  $('#np-title').textContent = t?.title ?? '';
  $('#np-title').href = t ? `#/album/${t.albumId}` : '#/';
  $('#np-artist').textContent = t?.artist ?? '';
  $('#np-artist').href = t ? `#/artist/${t.artistId}` : '#/';
  const fav = $('#np-fav');
  fav.classList.toggle('hidden', !t);
  const isFav = t && favs.has(t.id);
  fav.classList.toggle('on', Boolean(isFav));
  fav.innerHTML = icon(isFav ? 'heart' : 'heartOutline');
  document.title = t ? `${t.title} · ${t.artist}` : 'Nasfonía';
  // Highlight the playing row wherever it is shown.
  document.querySelectorAll('.track.playing').forEach((r) => r.classList.remove('playing'));
  if (t) document.querySelectorAll(`.track[data-id="${t.id}"]`).forEach((r) => r.classList.add('playing'));
  updateButtons();
  if ('mediaSession' in navigator && t) {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: t.title,
      artist: t.artist,
      album: t.album,
      artwork: t.hasCover ? [{ src: new URL(coverUrl(t.albumId), location.href).href, sizes: '512x512' }] : [],
    });
  }
}

function setSlider(el, value, max) {
  el.max = max;
  el.value = value;
  el.style.setProperty('--p', `${max ? (value / max) * 100 : 0}%`);
}

let seeking = false;
audio.addEventListener('timeupdate', () => {
  const d = audio.duration || current()?.duration || 0;
  if (!seeking) setSlider($('#seek'), audio.currentTime, d || 1);
  $('#time-cur').textContent = fmt(audio.currentTime);
  $('#time-total').textContent = fmt(d);
  const t = current();
  // Counts as a play after 30 s or half the song, like most services.
  if (t && !player.counted && audio.currentTime >= Math.min(30, d / 2 || 30)) {
    player.counted = true;
    api(`/plays/${t.id}`, { method: 'POST' }).catch(() => {});
  }
  if (Math.floor(audio.currentTime) % 5 === 0) savePlayer();
});
audio.addEventListener('play', updateButtons);
audio.addEventListener('pause', updateButtons);
audio.addEventListener('ended', () => next(true));
audio.addEventListener('volumechange', () => {
  setSlider($('#volume'), audio.muted ? 0 : Math.round(audio.volume * 100), 100);
  store.set('nm.volume', { v: audio.volume, m: audio.muted });
  updateButtons();
});
audio.addEventListener('error', () => {
  if (current()) toast('No se puede reproducir este archivo');
});

function bindPlayerControls() {
  $('#btn-play').onclick = togglePlay;
  $('#btn-next').onclick = () => next();
  $('#btn-prev').onclick = prev;
  $('#btn-shuffle').onclick = toggleShuffle;
  $('#btn-repeat').onclick = cycleRepeat;
  $('#btn-queue').onclick = () => {
    $('#queue-panel').classList.toggle('hidden');
    $('#btn-queue').classList.toggle('on', !$('#queue-panel').classList.contains('hidden'));
    renderQueue();
  };
  $('#btn-mute').onclick = () => (audio.muted = !audio.muted);
  const seek = $('#seek');
  seek.addEventListener('input', () => {
    seeking = true;
    setSlider(seek, seek.value, seek.max);
    $('#time-cur').textContent = fmt(seek.value);
  });
  seek.addEventListener('change', () => {
    audio.currentTime = Number(seek.value);
    seeking = false;
  });
  $('#volume').addEventListener('input', (e) => {
    audio.muted = false;
    audio.volume = e.target.value / 100;
  });
  const v = store.get('nm.volume', { v: 0.8, m: false });
  audio.volume = v.v;
  audio.muted = v.m;
  setSlider($('#volume'), audio.muted ? 0 : Math.round(audio.volume * 100), 100);

  if ('mediaSession' in navigator) {
    const ms = navigator.mediaSession;
    ms.setActionHandler('play', togglePlay);
    ms.setActionHandler('pause', togglePlay);
    ms.setActionHandler('nexttrack', () => next());
    ms.setActionHandler('previoustrack', prev);
    ms.setActionHandler('seekto', (e) => (audio.currentTime = e.seekTime));
  }

  document.addEventListener('keydown', (e) => {
    if (e.target.matches('input, textarea')) return;
    if (e.code === 'Space') {
      e.preventDefault();
      togglePlay();
    } else if (e.code === 'ArrowRight' && e.altKey) next();
    else if (e.code === 'ArrowLeft' && e.altKey) prev();
  });

  // Restore the last session, paused.
  const saved = store.get('nm.player', null);
  if (saved?.queue?.length) {
    Object.assign(player, { queue: saved.queue, original: saved.original, shuffle: saved.shuffle, repeat: saved.repeat, context: saved.context });
    playIndex(Math.min(saved.index, saved.queue.length - 1), { autoplay: false, at: saved.time || 0 });
  }
  updateNowPlaying();
}

// ---------------------------------------------------------------- track lists
// Every rendered list registers its tracks here; rows point back by index.
let lists = [];
function registerList(tracks) {
  lists.push(tracks);
  return lists.length - 1;
}

function trackRows(tracks, { album = true, cover = true, numbers = false, playlistId = null, discs = false } = {}) {
  const li = registerList(tracks);
  let lastDisc = null;
  const multiDisc = discs && new Set(tracks.map((t) => t.discNo || 1)).size > 1;
  const rows = tracks.map((t, i) => {
    let disc = '';
    if (multiDisc && (t.discNo || 1) !== lastDisc) {
      lastDisc = t.discNo || 1;
      disc = `<div class="disc">${icon('album', 'sm')} Disco ${lastDisc}</div>`;
    }
    const fav = favs.has(t.id);
    const playing = current()?.id === t.id;
    return `${disc}<div class="track ${playing ? 'playing' : ''}" data-id="${t.id}" data-list="${li}" data-i="${i}" ${playlistId ? `data-pl="${playlistId}" data-pos="${t.position}"` : ''}>
      <div class="num"><span class="n">${numbers ? t.trackNo || i + 1 : i + 1}</span><button data-action="play-row" title="Reproducir">${icon('play', 'sm')}</button></div>
      <div class="ti">${cover ? coverHtml(t.albumId, t.hasCover) : ''}<div>
        <span class="title-text">${esc(t.title)}</span>
        <span class="sub"><a href="#/artist/${t.artistId}">${esc(t.artist)}</a></span></div></div>
      ${album ? `<div class="alb"><a href="#/album/${t.albumId}">${esc(t.album)}</a></div>` : ''}
      <button class="icon-btn fav ${fav ? 'on' : ''}" data-fav="${t.id}" data-action="fav" title="Me gusta">${icon(fav ? 'heart' : 'heartOutline', 'sm')}</button>
      <div class="dur">${fmt(t.duration)}</div>
      <button class="icon-btn more" data-action="track-menu" title="Más opciones">${icon('more', 'sm')}</button>
    </div>`;
  });
  return `<div class="tracks ${album ? '' : 'no-album'}">
    <div class="tracks-head"><div class="r">#</div><div>Título</div>${album ? '<div>Álbum</div>' : ''}<div></div><div class="r">${icon('clock', 'sm')}</div><div></div></div>
    ${rows.join('')}</div>`;
}

function trackFromRow(row) {
  return lists[row.dataset.list][row.dataset.i];
}

// ---------------------------------------------------------------- cards
function albumCard(a) {
  return `<a class="card" href="#/album/${a.id}">
    ${coverHtml(a.id, a.hasCover, { fallback: 'album' })}
    <button class="card-play" data-action="play-album" data-id="${a.id}" title="Reproducir">${icon('play')}</button>
    <div class="t">${esc(a.title)}</div><div class="s">${a.year ? `${a.year} · ` : ''}${esc(a.artist)}</div></a>`;
}
function artistCard(a) {
  return `<a class="card" href="#/artist/${a.id}">
    ${coverHtml(a.coverAlbumId, Boolean(a.coverAlbumId), { cls: 'round', fallback: 'person' })}
    <button class="card-play" data-action="play-artist" data-id="${a.id}" title="Reproducir">${icon('play')}</button>
    <div class="t">${esc(a.name)}</div><div class="s">Artista</div></a>`;
}
function playlistCard(p) {
  return `<a class="card" href="#/playlist/${p.id}">
    ${coverHtml(p.coverAlbumId, Boolean(p.coverAlbumId), { fallback: 'queue' })}
    <button class="card-play" data-action="play-playlist" data-id="${p.id}" title="Reproducir">${icon('play')}</button>
    <div class="t">${esc(p.name)}</div><div class="s">${plural(p.trackCount, 'canción', 'canciones')}</div></a>`;
}
const section = (title, body, more = '') =>
  `<section class="section"><div class="section-head"><h2>${title}</h2>${more ? `<a class="more" href="${more}">Mostrar todo</a>` : ''}</div>${body}</section>`;

// ---------------------------------------------------------------- sidebar library
let libTab = store.get('nm.libTab', 'playlists');
let libAlbums = null;
let libArtists = null;

async function renderLibrary(target = $('#lib-list')) {
  document.querySelectorAll('[data-lib]').forEach((c) => c.classList.toggle('active', c.dataset.lib === libTab));
  let html = '';
  if (libTab === 'playlists') {
    html += `<a class="lib-item" href="#/liked">${`<div class="cover liked">${icon('heart')}</div>`}<div><div class="t">Canciones que te gustan</div><div class="s">Playlist · ${plural(favs.size, 'canción', 'canciones')}</div></div></a>`;
    html += playlists
      .map((p) => `<a class="lib-item" href="#/playlist/${p.id}">${coverHtml(p.coverAlbumId, Boolean(p.coverAlbumId), { fallback: 'queue' })}<div><div class="t">${esc(p.name)}</div><div class="s">Playlist · ${plural(p.trackCount, 'canción', 'canciones')}</div></div></a>`)
      .join('');
  } else if (libTab === 'albums') {
    libAlbums ??= await api('/albums?sort=artist');
    html = libAlbums.map((a) => `<a class="lib-item" href="#/album/${a.id}">${coverHtml(a.id, a.hasCover, { fallback: 'album' })}<div><div class="t">${esc(a.title)}</div><div class="s">Álbum · ${esc(a.artist)}</div></div></a>`).join('');
  } else {
    libArtists ??= await api('/artists');
    html = libArtists.map((a) => `<a class="lib-item" href="#/artist/${a.id}">${coverHtml(a.coverAlbumId, Boolean(a.coverAlbumId), { cls: 'round', fallback: 'person' })}<div><div class="t">${esc(a.name)}</div><div class="s">Artista</div></div></a>`).join('');
  }
  if (target) target.innerHTML = html || '<p class="muted" style="padding:8px">Nada por aquí todavía.</p>';
  markActiveLinks();
  return html;
}

function markActiveLinks() {
  const h = location.hash || '#/';
  document.querySelectorAll('.lib-item').forEach((a) => a.classList.toggle('active', a.getAttribute('href') === h));
  const nav = h.startsWith('#/search') ? 'search' : h === '#/' || h === '#' ? 'home' : h.startsWith('#/library') ? 'library' : '';
  document.querySelectorAll('[data-nav]').forEach((a) => a.classList.toggle('active', a.dataset.nav === nav));
}

// ---------------------------------------------------------------- views
const view = () => $('#view');
const spinner = () => (view().innerHTML = '<div class="pad-top"></div><div class="spinner"></div>');

function header({ kind, title, meta, coverHtml: c, hue }) {
  return `<div class="page-head" style="--hue:${hue}">${c}<div><div class="kind">${kind}</div><h1>${esc(title)}</h1><div class="meta">${meta}</div></div></div>`;
}

// Picks a header colour from the cover itself; falls back to a stable hue.
function tintFromCover(albumId, has) {
  if (!has) return;
  const img = new Image();
  img.onload = () => {
    try {
      const c = document.createElement('canvas');
      c.width = c.height = 8;
      const ctx = c.getContext('2d');
      ctx.drawImage(img, 0, 0, 8, 8);
      const d = ctx.getImageData(0, 0, 8, 8).data;
      let r = 0, g = 0, b = 0;
      for (let i = 0; i < d.length; i += 4) { r += d[i]; g += d[i + 1]; b += d[i + 2]; }
      const n = d.length / 4;
      const k = 0.75;
      const color = `rgb(${(r / n) * k | 0} ${(g / n) * k | 0} ${(b / n) * k | 0})`;
      document.querySelectorAll('.page-head, .page-body').forEach((el) => el.style.setProperty('--hue', color));
      $('#main').style.setProperty('--topbar-bg', `color-mix(in srgb, ${color} 60%, #121212)`);
    } catch { /* tainted canvas */ }
  };
  img.src = coverUrl(albumId);
}

const greeting = () => {
  const h = new Date().getHours();
  return h < 6 ? 'Buenas noches' : h < 13 ? 'Buenos días' : h < 21 ? 'Buenas tardes' : 'Buenas noches';
};

const views = {
  async home() {
    const d = await api('/home');
    const quick = [...d.recentAlbums, ...d.newAlbums.filter((a) => !d.recentAlbums.some((r) => r.id === a.id))].slice(0, 8);
    let html = `<div class="home-head"><h1 class="greeting">${greeting()}</h1>
      <div class="quick-grid">
        <a class="quick" href="#/liked"><div class="cover liked">${icon('heart')}</div><div class="t">Canciones que te gustan</div></a>
        ${quick.map((a) => `<a class="quick" href="#/album/${a.id}">${coverHtml(a.id, a.hasCover, { fallback: 'album' })}<div class="t">${esc(a.title)}</div><button class="card-play" data-action="play-album" data-id="${a.id}">${icon('play')}</button></a>`).join('')}
      </div></div>`;
    if (!d.stats.tracks) {
      html += `<div class="empty"><h2>Tu biblioteca está vacía</h2><p>Copia música en la carpeta compartida del NAS y pulsa «Volver a escanear» en el menú de usuario.</p></div>`;
    }
    if (d.recentAlbums.length) html += section('Escuchado recientemente', `<div class="row">${d.recentAlbums.map(albumCard).join('')}</div>`);
    if (d.playlists.length) html += section('Tus playlists', `<div class="row">${d.playlists.map(playlistCard).join('')}</div>`);
    if (d.newAlbums.length) html += section('Añadido recientemente', `<div class="row">${d.newAlbums.map(albumCard).join('')}</div>`, '#/albums');
    if (d.mostPlayed.length) html += section('Lo que más escuchas', trackRows(d.mostPlayed));
    if (d.artists.length) html += section('Artistas', `<div class="row">${d.artists.map(artistCard).join('')}</div>`, '#/artists');
    if (d.randomAlbums.length) html += section('Descubre en tu biblioteca', `<div class="row">${d.randomAlbums.map(albumCard).join('')}</div>`);
    if (d.stats.tracks) {
      html += `<p class="muted" style="margin-top:32px">${plural(d.stats.tracks, 'canción', 'canciones')} · ${plural(d.stats.albums, 'álbum', 'álbumes')} · ${plural(d.stats.artists, 'artista', 'artistas')} · ${fmtLong(d.stats.duration)}</p>`;
    }
    view().innerHTML = html;
  },

  async album(id) {
    const a = await api(`/albums/${id}`);
    view().innerHTML = header({
      kind: 'Álbum',
      title: a.title,
      hue: hueOf(a.id),
      coverHtml: coverHtml(a.id, a.hasCover, { fallback: 'album' }),
      meta: `<a href="#/artist/${a.artistId}">${esc(a.artist)}</a>${a.year ? ` · ${a.year}` : ''} · ${plural(a.trackCount, 'canción', 'canciones')}, ${fmtLong(a.duration)}`,
    }) + `<div class="page-body" style="--hue:${hueOf(a.id)}">
      <div class="actions">
        <button class="big-play" data-action="play-list" data-list="${registerList(a.tracks)}" title="Reproducir">${icon('play')}</button>
        <button class="icon-btn" data-action="shuffle-list" data-list="${lists.length - 1}" title="Aleatorio">${icon('shuffle')}</button>
        <button class="icon-btn" data-action="list-menu" data-list="${lists.length - 1}" title="Más opciones">${icon('more')}</button>
      </div>
      ${trackRows(a.tracks, { album: false, cover: false, numbers: true, discs: true })}
      ${a.genre ? `<p class="muted" style="margin-top:24px">${esc(a.genre)}</p>` : ''}
    </div>`;
    tintFromCover(a.id, a.hasCover);
  },

  async artist(id) {
    const a = await api(`/artists/${id}`);
    view().innerHTML = header({
      kind: 'Artista',
      title: a.name,
      hue: hueOf(a.id + 7),
      coverHtml: coverHtml(a.coverAlbumId, Boolean(a.coverAlbumId), { cls: 'round', fallback: 'person' }),
      meta: `${plural(a.albumCount, 'álbum', 'álbumes')} · ${plural(a.trackCount, 'canción', 'canciones')}`,
    }) + `<div class="page-body" style="--hue:${hueOf(a.id + 7)}">
      <div class="actions">
        <button class="big-play" data-action="play-artist" data-id="${a.id}" title="Reproducir">${icon('play')}</button>
        <button class="icon-btn" data-action="shuffle-artist" data-id="${a.id}" title="Aleatorio">${icon('shuffle')}</button>
      </div>
      ${a.topTracks.length ? section('Populares', trackRows(a.topTracks.slice(0, 10))) : ''}
      ${a.albums.length ? section('Discografía', `<div class="grid">${a.albums.map(albumCard).join('')}</div>`) : ''}
      ${a.appearsOn.length ? section('Aparece en', `<div class="grid">${a.appearsOn.map(albumCard).join('')}</div>`) : ''}
    </div>`;
    tintFromCover(a.coverAlbumId, Boolean(a.coverAlbumId));
  },

  async playlist(id) {
    const p = await api(`/playlists/${id}`);
    view().innerHTML = header({
      kind: 'Playlist',
      title: p.name,
      hue: hueOf(p.id + 3),
      coverHtml: coverHtml(p.coverAlbumId, Boolean(p.coverAlbumId), { fallback: 'queue' }),
      meta: `${plural(p.trackCount, 'canción', 'canciones')}${p.duration ? `, ${fmtLong(p.duration)}` : ''}`,
    }) + `<div class="page-body" style="--hue:${hueOf(p.id + 3)}">
      <div class="actions">
        <button class="big-play" data-action="play-list" data-list="${registerList(p.tracks)}" title="Reproducir">${icon('play')}</button>
        <button class="icon-btn" data-action="shuffle-list" data-list="${lists.length - 1}" title="Aleatorio">${icon('shuffle')}</button>
        <button class="icon-btn" data-action="playlist-menu" data-id="${p.id}" title="Más opciones">${icon('more')}</button>
      </div>
      ${p.tracks.length ? trackRows(p.tracks, { playlistId: p.id }) : `<div class="empty"><h2>Vamos a añadir algo</h2><p>Usa el menú «···» de cualquier canción y elige «Añadir a playlist».</p><a class="btn-outline" href="#/search">Buscar canciones</a></div>`}
    </div>`;
    tintFromCover(p.coverAlbumId, Boolean(p.coverAlbumId));
  },

  async liked() {
    const tracks = await api('/favorites');
    favs.clear();
    tracks.forEach((t) => favs.add(t.id));
    view().innerHTML = header({
      kind: 'Playlist',
      title: 'Canciones que te gustan',
      hue: '#5038a0',
      coverHtml: `<div class="cover liked">${icon('heart')}</div>`,
      meta: plural(tracks.length, 'canción', 'canciones'),
    }) + `<div class="page-body" style="--hue:#5038a0">
      <div class="actions">
        <button class="big-play" data-action="play-list" data-list="${registerList(tracks)}" title="Reproducir">${icon('play')}</button>
        <button class="icon-btn" data-action="shuffle-list" data-list="${lists.length - 1}" title="Aleatorio">${icon('shuffle')}</button>
      </div>
      ${tracks.length ? trackRows(tracks) : '<div class="empty"><h2>Las canciones que te gusten aparecerán aquí</h2><p>Pulsa el corazón de cualquier canción para guardarla.</p></div>'}
    </div>`;
    $('#main').style.setProperty('--topbar-bg', '#3a2a7a');
  },

  async albums() {
    const all = await api('/albums?sort=artist');
    view().innerHTML = `<div class="pad-top"></div><h1 class="greeting">Álbumes</h1><div class="grid">${all.map(albumCard).join('')}</div>`;
  },

  async artists() {
    const all = await api('/artists');
    view().innerHTML = `<div class="pad-top"></div><h1 class="greeting">Artistas</h1><div class="grid">${all.map(artistCard).join('')}</div>`;
  },

  async genre(name) {
    const tracks = await api(`/genres/${encodeURIComponent(name)}/tracks`);
    view().innerHTML = header({
      kind: 'Género',
      title: name,
      hue: hueOf(name.length * 13),
      coverHtml: coverHtml(tracks[0]?.albumId, tracks[0]?.hasCover, { fallback: 'note' }),
      meta: plural(tracks.length, 'canción', 'canciones'),
    }) + `<div class="page-body" style="--hue:${hueOf(name.length * 13)}">
      <div class="actions">
        <button class="big-play" data-action="play-list" data-list="${registerList(tracks)}">${icon('play')}</button>
        <button class="icon-btn" data-action="shuffle-list" data-list="${lists.length - 1}">${icon('shuffle')}</button>
      </div>${trackRows(tracks)}</div>`;
  },

  async library() {
    view().innerHTML = `<div class="pad-top"></div><h1 class="greeting">Tu biblioteca</h1>
      <div class="chips" style="padding:0 0 16px">
        <button class="chip" data-lib="playlists">Playlists</button><button class="chip" data-lib="albums">Álbumes</button><button class="chip" data-lib="artists">Artistas</button>
        <button class="chip" data-action="new-playlist">+ Nueva playlist</button></div>
      <div class="lib-list" id="lib-list-main"></div>`;
    await renderLibrary($('#lib-list-main'));
  },

  async search(q = '') {
    const input = $('#search-input');
    if (input.value !== q) input.value = q;
    if (!q.trim()) {
      const genres = await api('/genres');
      view().innerHTML = `<div class="pad-top"></div>
        ${section('Explorar todo', `<div class="genres">
          <a class="genre" href="#/albums" style="background:${hueOf(1)}">Todos los álbumes</a>
          <a class="genre" href="#/artists" style="background:${hueOf(2)}">Todos los artistas</a>
          <a class="genre" href="#/liked" style="background:#5038a0">Me gusta</a>
          ${genres.map((g, i) => `<a class="genre" href="#/genre/${encodeURIComponent(g.name)}" style="background:${hueOf(i + 5)}">${esc(g.name)}${coverHtml(g.coverAlbumId, Boolean(g.coverAlbumId))}</a>`).join('')}
        </div>`)}`;
      return;
    }
    const r = await api(`/search?q=${encodeURIComponent(q)}`);
    if (!r.tracks.length && !r.albums.length && !r.artists.length && !r.playlists.length) {
      view().innerHTML = `<div class="pad-top"></div><div class="empty"><h2>No hay resultados para «${esc(q)}»</h2><p>Comprueba la ortografía o prueba con otras palabras.</p></div>`;
      return;
    }
    const top = r.artists[0]
      ? `<a class="top-result" href="#/artist/${r.artists[0].id}">${coverHtml(r.artists[0].coverAlbumId, Boolean(r.artists[0].coverAlbumId), { cls: 'round', fallback: 'person' })}<div class="t">${esc(r.artists[0].name)}</div><div><span class="tag">Artista</span></div><button class="card-play" data-action="play-artist" data-id="${r.artists[0].id}">${icon('play')}</button></a>`
      : r.albums[0]
        ? `<a class="top-result" href="#/album/${r.albums[0].id}">${coverHtml(r.albums[0].id, r.albums[0].hasCover, { fallback: 'album' })}<div class="t">${esc(r.albums[0].title)}</div><div class="muted"><span class="tag">Álbum</span> ${esc(r.albums[0].artist)}</div><button class="card-play" data-action="play-album" data-id="${r.albums[0].id}">${icon('play')}</button></a>`
        : '';
    view().innerHTML = `<div class="pad-top"></div>
      <div class="search-top">
        ${top ? `<section class="section"><div class="section-head"><h2>Resultado principal</h2></div>${top}</section>` : ''}
        ${r.tracks.length ? `<section class="section"><div class="section-head"><h2>Canciones</h2></div>${trackRows(r.tracks.slice(0, 5), { album: false })}</section>` : ''}
      </div>
      ${r.tracks.length > 5 ? section('Más canciones', trackRows(r.tracks.slice(5))) : ''}
      ${r.artists.length ? section('Artistas', `<div class="row">${r.artists.map(artistCard).join('')}</div>`) : ''}
      ${r.albums.length ? section('Álbumes', `<div class="row">${r.albums.map(albumCard).join('')}</div>`) : ''}
      ${r.playlists.length ? section('Playlists', `<div class="row">${r.playlists.map(playlistCard).join('')}</div>`) : ''}`;
  },
};

let routeSeq = 0;
async function route() {
  const seq = ++routeSeq;
  const [, name = '', ...rest] = (location.hash || '#/').slice(1).split('/');
  const arg = decodeURIComponent(rest.join('/'));
  lists = [];
  closeMenu();
  $('#search-box').classList.toggle('hidden', name !== 'search');
  $('#main').style.removeProperty('--topbar-bg');
  markActiveLinks();
  const fn = views[name || 'home'];
  if (!fn) {
    location.hash = '#/';
    return;
  }
  if (name !== 'search') spinner();
  try {
    await fn(arg);
    if (seq !== routeSeq) return;
    if (name !== 'search') $('#main').scrollTop = 0;
    updateNowPlaying();
  } catch (e) {
    if (seq === routeSeq) view().innerHTML = `<div class="pad-top"></div><div class="empty"><h2>Algo ha fallado</h2><p>${esc(e.message)}</p></div>`;
  }
}

// ---------------------------------------------------------------- queue panel
function renderQueue() {
  const panel = $('#queue-panel');
  if (panel.classList.contains('hidden')) return;
  const t = current();
  const upcoming = player.queue.slice(player.index + 1);
  const li = registerList(player.queue);
  const row = (tr, i) => `<div class="lib-item track-q ${i === player.index ? 'playing' : ''}" data-q="${i}" style="cursor:pointer">
      ${coverHtml(tr.albumId, tr.hasCover)}<div><div class="t">${esc(tr.title)}</div><div class="s">${esc(tr.artist)}</div></div></div>`;
  panel.innerHTML = `<h2>Cola <button class="icon-btn" data-action="close-queue">${icon('close')}</button></h2>
    ${t ? `<h3>Sonando</h3>${row(t, player.index)}` : '<p class="muted">No hay nada en la cola.</p>'}
    ${upcoming.length ? `<h3>A continuación</h3>${upcoming.map((tr, k) => row(tr, player.index + 1 + k)).join('')}` : ''}
    ${upcoming.length ? '<p style="margin-top:16px"><button class="btn-outline" data-action="clear-queue">Borrar cola</button></p>' : ''}`;
  panel.dataset.list = li;
}

// ---------------------------------------------------------------- menus & dialogs
function closeMenu() {
  $('#menu').classList.add('hidden');
}

function openMenu(anchor, items) {
  const m = $('#menu');
  m.innerHTML = items
    .map((it) => (it === '-' ? '<hr>' : it.label && !it.run ? `<div class="label">${esc(it.label)}</div>` : `<button class="${it.danger ? 'danger' : ''}">${it.icon ? icon(it.icon, 'sm') : ''}${esc(it.text)}</button>`))
    .join('');
  const runnable = items.filter((it) => it !== '-' && it.run);
  m.querySelectorAll('button').forEach((b, i) =>
    (b.onclick = async (e) => {
      e.stopPropagation();
      closeMenu();
      try {
        await runnable[i].run();
      } catch (err) {
        toast(err.message);
      }
    }),
  );
  m.classList.remove('hidden');
  const r = anchor.getBoundingClientRect();
  const w = m.offsetWidth, h = m.offsetHeight;
  m.style.left = `${Math.max(8, Math.min(r.right - w, innerWidth - w - 8))}px`;
  m.style.top = `${r.bottom + h + 8 > innerHeight ? Math.max(8, r.top - h - 4) : r.bottom + 4}px`;
}

function ask(title, { value = '', placeholder = '', ok = 'Guardar' } = {}) {
  return new Promise((resolve) => {
    const modal = $('#modal');
    modal.innerHTML = `<form class="modal-card"><h2>${esc(title)}</h2><input value="${esc(value)}" placeholder="${esc(placeholder)}" maxlength="120" required>
      <div class="row-btns"><button type="button" class="link">Cancelar</button><button class="btn-primary" type="submit">${esc(ok)}</button></div></form>`;
    modal.classList.remove('hidden');
    const input = modal.querySelector('input');
    input.focus();
    input.select();
    const done = (v) => {
      modal.classList.add('hidden');
      resolve(v);
    };
    modal.querySelector('form').onsubmit = (e) => {
      e.preventDefault();
      done(input.value.trim() || null);
    };
    modal.querySelector('.link').onclick = () => done(null);
    modal.onclick = (e) => e.target === modal && done(null);
  });
}

function confirmBox(title, text, ok = 'Eliminar') {
  return new Promise((resolve) => {
    const modal = $('#modal');
    modal.innerHTML = `<div class="modal-card"><h2>${esc(title)}</h2><p class="muted">${esc(text)}</p>
      <div class="row-btns"><button class="link">Cancelar</button><button class="btn-primary">${esc(ok)}</button></div></div>`;
    modal.classList.remove('hidden');
    const [cancel, yes] = modal.querySelectorAll('button');
    const done = (v) => {
      modal.classList.add('hidden');
      resolve(v);
    };
    cancel.onclick = () => done(false);
    yes.onclick = () => done(true);
    modal.onclick = (e) => e.target === modal && done(false);
  });
}

async function newPlaylist(trackIds = []) {
  const name = await ask('Crear playlist', { placeholder: 'Mi playlist', ok: 'Crear' });
  if (!name) return null;
  const p = await api('/playlists', { method: 'POST', body: { name, trackIds } });
  await refreshPlaylists();
  toast(trackIds.length ? `Añadido a ${p.name}` : 'Playlist creada');
  return p;
}

async function addToPlaylist(id, tracks) {
  const p = await api(`/playlists/${id}/tracks`, { method: 'POST', body: { trackIds: tracks.map((t) => t.id) } });
  await refreshPlaylists();
  toast(`Añadido a ${p.name}`);
}

function playlistItems(tracks) {
  return [
    { label: 'Añadir a playlist' },
    { text: 'Nueva playlist…', icon: 'add', run: () => newPlaylist(tracks.map((t) => t.id)) },
    ...playlists.map((p) => ({ text: p.name, icon: 'queue', run: () => addToPlaylist(p.id, tracks) })),
  ];
}

function trackMenu(anchor, row) {
  const t = trackFromRow(row);
  const items = [
    { text: 'Añadir a la cola', icon: 'queue', run: () => addToQueue([t]) },
    { text: 'Reproducir a continuación', icon: 'playNext', run: () => addToQueue([t], { next: true }) },
    { text: favs.has(t.id) ? 'Quitar de Me gusta' : 'Guardar en Me gusta', icon: favs.has(t.id) ? 'heart' : 'heartOutline', run: () => setFavorite(t.id, !favs.has(t.id)) },
    '-',
    { text: 'Ir al artista', icon: 'person', run: () => (location.hash = `#/artist/${t.artistId}`) },
    { text: 'Ir al álbum', icon: 'album', run: () => (location.hash = `#/album/${t.albumId}`) },
  ];
  if (row.dataset.pl) {
    items.push('-', {
      text: 'Quitar de esta playlist',
      icon: 'trash',
      danger: true,
      run: async () => {
        await api(`/playlists/${row.dataset.pl}/tracks/${row.dataset.pos}`, { method: 'DELETE' });
        await refreshPlaylists();
        route();
      },
    });
  }
  items.push('-', ...playlistItems([t]));
  openMenu(anchor, items);
}

// Folder picker: the list of library folders, and a browser to add more.
async function openFolders() {
  const info = await api('/folders');
  let dirs = info.dirs.map((d) => ({ ...d }));
  let browsing = null; // result of /folders/browse while adding a folder
  const modal = $('#modal');
  const close = () => modal.classList.add('hidden');

  async function browse(p) {
    try {
      browsing = await api(`/folders/browse${p ? `?path=${encodeURIComponent(p)}` : ''}`);
    } catch (e) {
      toast(e.message);
    }
    render();
  }

  function render() {
    if (browsing) {
      const b = browsing;
      modal.innerHTML = `<div class="modal-card folders">
        <div class="folder-head">
          ${b.parent ? `<button class="icon-btn" data-f="up" title="Subir">${icon('chevronLeft')}</button>` : ''}
          <h2>${esc(b.name)}</h2></div>
        <div class="folder-list">
          ${b.folders.map((f, i) => `<button class="folder-row" data-f="open" data-i="${i}">${icon('library', 'sm')}<span>${esc(f.name)}</span>${icon('chevronRight', 'sm')}</button>`).join('') || '<p class="muted">No hay subcarpetas.</p>'}
        </div>
        <p class="muted">${b.audioFiles ? `${plural(b.audioFiles, 'canción', 'canciones')} directamente en esta carpeta` : 'Se incluyen también las subcarpetas.'}</p>
        <div class="row-btns"><button class="link" data-f="back">Cancelar</button>
          <button class="btn-primary" data-f="pick" ${b.path === info.browseRoot ? 'disabled' : ''}>Elegir esta carpeta</button></div>
      </div>`;
    } else {
      modal.innerHTML = `<div class="modal-card folders">
        <h2>Carpetas de música</h2>
        <p class="muted">La biblioteca se forma con estas carpetas del NAS y todas sus subcarpetas.</p>
        <div class="folder-list">
          ${dirs.map((d, i) => `<div class="folder-row">${icon('library', 'sm')}<span>${esc(d.name)}${d.exists === false ? ' <em class="error">(no existe)</em>' : ''}</span>
            <button class="icon-btn" data-f="remove" data-i="${i}" title="Quitar">${icon('close', 'sm')}</button></div>`).join('') || '<p class="muted">Ninguna carpeta elegida.</p>'}
        </div>
        ${info.canBrowse ? `<button class="btn-outline" data-f="add">+ Añadir carpeta</button>` : '<p class="muted">El servidor no tiene volúmenes del NAS montados en ' + esc(info.browseRoot) + '.</p>'}
        <div class="row-btns"><button class="link" data-f="cancel">Cancelar</button><button class="btn-primary" data-f="save" ${dirs.length ? '' : 'disabled'}>Guardar y escanear</button></div>
      </div>`;
    }
    modal.classList.remove('hidden');
  }

  modal.onclick = async (e) => {
    if (e.target === modal) return close();
    const btn = e.target.closest('[data-f]');
    if (!btn) return;
    const i = Number(btn.dataset.i);
    switch (btn.dataset.f) {
      case 'add': return browse(null);
      case 'open': return browse(browsing.folders[i].path);
      case 'up': return browse(browsing.parent);
      case 'back': browsing = null; return render();
      case 'pick':
        if (!dirs.some((d) => d.path === browsing.path)) dirs.push({ path: browsing.path, name: browsing.name, exists: true });
        browsing = null;
        return render();
      case 'remove': dirs.splice(i, 1); return render();
      case 'cancel': return close();
      case 'save':
        try {
          await api('/folders', { method: 'PUT', body: { dirs: dirs.map((d) => d.path) } });
          close();
          libAlbums = libArtists = null;
          toast('Carpetas guardadas. Escaneando…');
          pollScan();
        } catch (err) {
          toast(err.message);
        }
    }
  };
  render();
}

async function openChangePassword({ nag = false } = {}) {
  const { changeable } = await api('/password');
  const modal = $('#modal');
  const close = () => modal.classList.add('hidden');
  if (!changeable) {
    modal.innerHTML = `<div class="modal-card"><h2>Cambiar contraseña</h2>
      <p class="muted">La contraseña está fijada con <code>MUSIC_PASSWORD</code> en el docker-compose del servidor. Cámbiala allí y vuelve a desplegar el contenedor.</p>
      <div class="row-btns"><button class="btn-primary">Entendido</button></div></div>`;
    modal.classList.remove('hidden');
    modal.querySelector('button').onclick = close;
    modal.onclick = (e) => e.target === modal && close();
    return;
  }
  modal.innerHTML = `<form class="modal-card pw-form"><h2>${nag ? 'Cambia la contraseña de serie' : 'Cambiar contraseña'}</h2>
    ${nag ? '<p class="muted">Sigues usando <strong>adminadmin</strong>, que es pública: cualquiera en tu red podría entrar. Elige una propia.</p>' : ''}
    <label>Contraseña actual<input type="password" name="current" autocomplete="current-password" value="${nag ? 'adminadmin' : ''}" required></label>
    <label>Nueva contraseña<input type="password" name="next" autocomplete="new-password" minlength="6" required></label>
    <label>Repite la nueva<input type="password" name="again" autocomplete="new-password" minlength="6" required></label>
    <p class="muted">Se cerrará la sesión en el resto de dispositivos.</p>
    <p class="error"></p>
    <div class="row-btns"><button type="button" class="link">Cancelar</button><button class="btn-primary" type="submit">Cambiar</button></div></form>`;
  modal.classList.remove('hidden');
  const form = modal.querySelector('form');
  const error = form.querySelector('.error');
  (nag ? form.next : form.current).focus();
  if (nag) form.querySelector('.link').textContent = 'Más tarde';
  form.querySelector('.link').onclick = close;
  modal.onclick = (e) => e.target === modal && close();
  form.onsubmit = async (e) => {
    e.preventDefault();
    error.textContent = '';
    if (form.next.value !== form.again.value) {
      error.textContent = 'Las contraseñas nuevas no coinciden';
      return;
    }
    const btn = form.querySelector('[type=submit]');
    btn.disabled = true;
    try {
      const r = await api('/password', { method: 'POST', body: { current: form.current.value, next: form.next.value, device: 'web' } });
      token = r.token;
      store.set('nm.token', token);
      close();
      toast('Contraseña cambiada');
      // Covers and the audio carry the token in their URLs.
      route();
      if (current()) {
        const t = audio.currentTime;
        const playing = !audio.paused;
        playIndex(player.index, { autoplay: playing, at: t });
      }
    } catch (err) {
      error.textContent = err.message;
    } finally {
      btn.disabled = false;
    }
  };
}

function userMenu(anchor) {
  openMenu(anchor, [
    { text: 'Carpetas de música…', icon: 'library', run: openFolders },
    { text: 'Cambiar contraseña…', icon: 'edit', run: openChangePassword },
    { text: 'Volver a escanear la biblioteca', icon: 'refresh', run: () => startScan(false) },
    { text: 'Escaneo completo (releer etiquetas)', icon: 'refresh', run: () => startScan(true) },
    '-',
    { text: 'Cerrar sesión', icon: 'logout', run: logout },
  ]);
}

async function startScan(full) {
  await api(`/scan${full ? '?full=1' : ''}`, { method: 'POST' });
  toast('Escaneando la biblioteca…');
  pollScan();
}

function pollScan() {
  const poll = async () => {
    const s = await api('/scan');
    if (s.running) return setTimeout(poll, 1500);
    libAlbums = libArtists = null;
    toast(s.error ? `Error al escanear: ${s.error}` : `Escaneo terminado: ${s.added} nuevas, ${s.removed} eliminadas`);
    renderLibrary();
    route();
  };
  setTimeout(poll, 1000);
}

// ---------------------------------------------------------------- events
async function playAlbum(id, shuffle = false) {
  const a = await api(`/albums/${id}`);
  playList(a.tracks, 0, { shuffle, context: `#/album/${id}` });
}
async function playArtist(id, shuffle = false) {
  const tracks = await api(`/artists/${id}/tracks`);
  playList(tracks, 0, { shuffle, context: `#/artist/${id}` });
}
async function playPlaylist(id) {
  const p = await api(`/playlists/${id}`);
  playList(p.tracks, 0, { context: `#/playlist/${id}` });
}

function bindEvents() {
  document.addEventListener('click', async (e) => {
    if (!e.target.closest('#menu')) closeMenu();
    const q = e.target.closest('[data-q]');
    if (q) return playIndex(Number(q.dataset.q));
    const chip = e.target.closest('[data-lib]');
    if (chip) {
      libTab = chip.dataset.lib;
      store.set('nm.libTab', libTab);
      return renderLibrary(chip.closest('.view') ? $('#lib-list-main') : $('#lib-list'));
    }
    const el = e.target.closest('[data-action]');
    if (!el) return;
    e.preventDefault();
    e.stopPropagation();
    const a = el.dataset.action;
    const row = el.closest('.track');
    try {
      switch (a) {
        case 'play-row': {
          const list = lists[row.dataset.list];
          const i = Number(row.dataset.i);
          if (current()?.id === list[i].id) togglePlay();
          else playList(list, i);
          break;
        }
        case 'play-list': {
          const list = lists[el.dataset.list];
          if (player.context === location.hash && current() && list.some((t) => t.id === current().id)) togglePlay();
          else playList(list, 0);
          break;
        }
        case 'shuffle-list': return playList(lists[el.dataset.list], 0, { shuffle: true });
        case 'list-menu': {
          const list = lists[el.dataset.list];
          return openMenu(el, [
            { text: 'Añadir a la cola', icon: 'queue', run: () => addToQueue(list) },
            { text: 'Reproducir a continuación', icon: 'playNext', run: () => addToQueue(list, { next: true }) },
            '-',
            ...playlistItems(list),
          ]);
        }
        case 'playlist-menu': {
          const id = el.dataset.id;
          const p = playlists.find((x) => String(x.id) === id);
          return openMenu(el, [
            { text: 'Cambiar nombre', icon: 'edit', run: async () => {
              const name = await ask('Cambiar nombre', { value: p?.name ?? '' });
              if (!name) return;
              await api(`/playlists/${id}`, { method: 'PATCH', body: { name } });
              await refreshPlaylists();
              route();
            } },
            { text: 'Eliminar playlist', icon: 'trash', danger: true, run: async () => {
              if (!(await confirmBox('¿Eliminar playlist?', `Se eliminará «${p?.name ?? ''}». Las canciones siguen en tu biblioteca.`))) return;
              await api(`/playlists/${id}`, { method: 'DELETE' });
              await refreshPlaylists();
              location.hash = '#/library';
            } },
          ]);
        }
        case 'play-album': return playAlbum(el.dataset.id);
        case 'play-artist': return playArtist(el.dataset.id);
        case 'shuffle-artist': return playArtist(el.dataset.id, true);
        case 'play-playlist': return playPlaylist(el.dataset.id);
        case 'fav': {
          const id = Number(el.dataset.fav);
          return setFavorite(id, !favs.has(id));
        }
        case 'fav-current': {
          const t = current();
          if (t) return setFavorite(t.id, !favs.has(t.id));
          break;
        }
        case 'track-menu': return trackMenu(el, row);
        case 'new-playlist': {
          const p = await newPlaylist();
          if (p) location.hash = `#/playlist/${p.id}`;
          break;
        }
        case 'user-menu': return userMenu(el);
        case 'close-queue': return $('#btn-queue').click();
        case 'clear-queue':
          player.queue = player.queue.slice(0, player.index + 1);
          player.original = null;
          renderQueue();
          return savePlayer();
      }
    } catch (err) {
      toast(err.message);
    }
  });

  // Double click on a row plays it, like desktop players.
  document.addEventListener('dblclick', (e) => {
    const row = e.target.closest('.track');
    if (!row || e.target.closest('button, a')) return;
    playList(lists[row.dataset.list], Number(row.dataset.i));
  });
  document.addEventListener('contextmenu', (e) => {
    const row = e.target.closest('.track');
    if (!row) return;
    e.preventDefault();
    trackMenu({ getBoundingClientRect: () => ({ top: e.clientY, bottom: e.clientY, right: e.clientX + 220, left: e.clientX }) }, row);
  });

  let searchTimer;
  $('#search-input').addEventListener('input', (e) => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => history.replaceState(null, '', `#/search/${encodeURIComponent(e.target.value)}`) || route(), 250);
  });

  $('#main').addEventListener('scroll', (e) => $('#topbar').classList.toggle('solid', e.target.scrollTop > 60));
  addEventListener('hashchange', route);
}

// ---------------------------------------------------------------- session
function logout() {
  token = null;
  store.del('nm.token');
  audio.pause();
  $('#app').classList.add('hidden');
  $('#login').classList.remove('hidden');
}

async function start() {
  $('#login').classList.add('hidden');
  $('#app').classList.remove('hidden');
  const [favList] = await Promise.all([api('/favorites'), refreshPlaylists()]);
  favList.forEach((t) => favs.add(t.id));
  renderLibrary();
  route();
  // Ask once per page load while the well-known default is still in use.
  api('/password').then((p) => p.isDefault && openChangePassword({ nag: true })).catch(() => {});
}

$('#login-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const btn = e.target.querySelector('button');
  btn.disabled = true;
  $('#login-error').textContent = '';
  try {
    const r = await api('/login', { method: 'POST', body: { password: $('#login-password').value, device: 'web' } });
    token = r.token;
    store.set('nm.token', token);
    await start();
  } catch (err) {
    $('#login-error').textContent = err.message;
  } finally {
    btn.disabled = false;
  }
});

hydrateIcons();
bindPlayerControls();
bindEvents();
if (token) start().catch(() => logout());
else logout();
