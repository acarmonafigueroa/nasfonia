# Nasfonía

**Tu propio servicio de música en streaming, con tu música y en tu NAS.** Nasfonía indexa la música que tienes en tu NAS y te la sirve en streaming: en el navegador, con un reproductor web, y en el móvil, con una app Android.

Sin suscripciones, sin anuncios y sin que tus datos salgan de casa.

- 🎵 Inicio con lo que has escuchado hace poco, lo último añadido y lo que más escuchas
- 🔎 Búsqueda instantánea, sin acentos («camaron» encuentra «Camarón»)
- 💿 Álbumes, artistas, géneros y portadas optimizadas (incrustadas, `cover.jpg` o descargadas automáticamente de Deezer/iTunes si faltan)
- ❤️ Canciones que te gustan, playlists (privadas o compartidas en el NAS), reordenación, cola, aleatorio y repetir
- 📻 **Radio de canción**: genera mezclas automáticas por artista, género y época a partir de cualquier tema
- 🎤 **Letras sincronizadas**: lee archivos `.lrc` / `.txt`, etiquetas incrustadas (`USLT`/`SYLT`) o las descarga automáticamente de LRCLIB / Lyrics.ovh con salto al pulsar cada verso
- 📥 **Modo sin conexión en Android**: descarga canciones, álbumes o playlists enteras para escuchar sin datos
- 🚗 **Android Auto**, temporizador de apagado (Sleep Timer) y persistencia de cola entre sesiones
- 🎛️ **Calidad de audio y transcodificación (FFmpeg)**: sirve el archivo original o comprime al vuelo (128 / 192 / 320 kbps) para ahorrar datos móviles, y convierte automáticamente formatos como WMA, APE, WavPack o AIFF
- 👥 Varios usuarios, cada uno con sus favoritos, playlists e historial
- 📁 Eliges las carpetas de música desde la propia app
- 🌍 En casa por IP y fuera por tu dominio: la app cambia sola de una a otra
- 📱 Android: reproducción en segundo plano, notificación, pantalla de bloqueo y Bluetooth
- 🎧 MP3, FLAC, M4A/AAC, OGG, Opus, WAV, WMA, APE, WV, AIFF… con avance rápido dentro de la canción

Funciona en NAS **UGREEN (UGOS Pro)**, Synology, QNAP, Unraid, TrueNAS o cualquier equipo con Docker, en procesadores Intel/AMD y ARM.

---

## 1. Instalar el servidor

### UGREEN (UGOS Pro)

1. Abre **Docker → Proyecto → Crear**.
2. Elige una carpeta para el proyecto, por ejemplo `/volume1/docker/nasfonia`.
3. Pega el contenido de [`server/docker-compose.yml`](server/docker-compose.yml) y cambia `/volume1/Music` por la carpeta donde tienes tu música.
4. Pulsa **Desplegar** y abre `http://IP-DE-TU-NAS:8090`.
5. Entra con el usuario **`admin`** y la contraseña **`adminadmin`**, y cámbiala por una tuya cuando te lo pida.

### Cualquier otro NAS o Linux con Docker

```bash
mkdir nasfonia && cd nasfonia
curl -O https://raw.githubusercontent.com/acarmonafigueroa/nasfonia/main/server/docker-compose.yml
# edita las rutas de tu música
docker compose up -d
```

### Elegir qué carpetas forman tu biblioteca

Todo lo que montes bajo `/nas` en el `docker-compose.yml` se puede explorar desde la web (icono de usuario → **Carpetas de música**) o desde la app (⚙ en Inicio). Las carpetas se montan en **solo lectura**: Nasfonía nunca modifica tus archivos.

### Variables de entorno

| Variable | Por defecto | Para qué sirve |
|---|---|---|
| `MUSIC_PASSWORD` | — | Fija la contraseña del usuario `admin` (entonces no se puede cambiar desde las apps). Sin ella, la de serie es `adminadmin` |
| `MUSIC_DIR` | `/music` | Carpetas iniciales, separadas por comas |
| `RESET_PASSWORD` | — | `1` devuelve la contraseña del usuario `admin` a `adminadmin` al arrancar (quítala después) |
| `BROWSE_ROOT` | `/nas` | Raíz que se puede explorar al elegir carpetas |
| `SERVER_NAME` | `Nasfonía` | Nombre que se muestra en la app |
| `SCAN_INTERVAL_MINUTES` | `60` | Reescaneo automático (0 = desactivado) |
| `PORT` | `8090` | Puerto |
| `TRUST_PROXY` | redes privadas | Proxies de los que se acepta `X-Forwarded-For` (para saber la IP real del cliente) |

### Usuarios y contraseñas

La primera vez entra con **`admin` / `adminadmin`** y cámbiala cuando te lo pida. Desde la web (icono de usuario) o la app (⚙ en Inicio):

- **Cambiar contraseña** — cada usuario la suya. Se guarda cifrada (scrypt) y se cierra la sesión en sus otros dispositivos.
- **Usuarios** (solo administradores) — crea uno para cada persona de casa. Cada uno tiene sus propios favoritos, playlists e historial; la música es la misma para todos. El administrador puede ponerles contraseña nueva, hacerlos administradores o borrarlos.
- Solo los administradores eligen las carpetas de música y lanzan escaneos.

**¿Se te ha olvidado la de `admin`?** Añade `RESET_PASSWORD=1` en `environment` del docker-compose y vuelve a desplegar: vuelve a ser `adminadmin`. Después quita esa línea y despliega otra vez, o se restablecería en cada reinicio.

### Entrar desde fuera de casa

La app y la web funcionan igual con una IP local, una IP pública o un dominio. En la app puedes guardar **dos direcciones**: la de casa y la de fuera; usa la de casa cuando puede y cambia sola a la otra al salir.

Formas de llegar a tu NAS desde fuera, de más a menos recomendable:

1. **VPN** (WireGuard, Tailscale o la VPN de tu router/NAS). No expones nada a Internet; usa la IP de casa.
2. **Dominio con HTTPS** a través de un proxy inverso (por ejemplo **Nginx Proxy Manager** en el propio NAS, con certificado de Let's Encrypt): `https://musica.tudominio.com` → `http://IP-DEL-NAS:8090`. Abre solo el 443 en el router.
3. **IP pública y puerto 8090** abierto en el router. Funciona, pero sin cifrar: tu contraseña viajaría a la vista. Úsalo solo para probar.

Si expones Nasfonía a Internet: cambia la contraseña de serie, usa contraseñas largas, y ten en cuenta que tras 8 intentos fallidos desde una IP se bloquea durante 15 minutos.
### Actualizar

En UGOS: **Docker → Proyecto → nasfonia → Actualizar imagen**. En la terminal: `docker compose pull && docker compose up -d`.

## 2. App Android

- **Google Play:** próximamente.
- **APK:** descárgalo en [Releases](https://github.com/acarmonafigueroa/nasfonia/releases/latest).

Al abrirla escribe la dirección de tu NAS: una IP (`192.168.1.50`; si no pones puerto, usa el 8090), un dominio (`musica.tudominio.com`, prueba primero HTTPS) o una URL completa. Opcionalmente, añade la dirección para cuando estés fuera de casa. Después, tu usuario y contraseña.

## Privacidad

Nasfonía no tiene servidores propios ni recoge datos: la app solo habla con el servidor que tú instalas. Lee la [política de privacidad](https://acarmonafigueroa.github.io/nasfonia/privacidad.html).

## Desarrollo

```
server/    Node.js 24 + SQLite (node:sqlite) + web sin compilar (HTML/CSS/JS)
android/   Kotlin, Jetpack Compose, Media3 (ExoPlayer + MediaSession)
```

```bash
cd server && npm install && MUSIC_DIR=./music MUSIC_PASSWORD=test npm start
cd android && ./gradlew assembleDebug
```

<details>
<summary>API</summary>

Todas las rutas, salvo `/api/ping` y `/api/login`, piden `Authorization: Bearer <token>` o `?token=<token>`.

| Método | Ruta | |
|---|---|---|
| POST | `/api/login` `{username, password}` | Devuelve `{token, user}` |
| GET | `/api/me` | Usuario actual |
| POST | `/api/password` `{current, next}` | Cambia la propia contraseña |
| GET/POST/PATCH/DELETE | `/api/users`, `/api/users/:id` | Usuarios (administradores) |
| GET | `/api/home` | Pantalla de inicio |
| GET | `/api/albums?sort=title\|artist\|year\|recent\|random`, `/api/albums/:id` | |
| GET | `/api/artists`, `/api/artists/:id`, `/api/artists/:id/tracks` | |
| GET | `/api/genres`, `/api/genres/:name/tracks` | |
| GET | `/api/search?q=` | |
| GET | `/api/stream/:id?bitrate=128\|192\|320` | Audio original (Range) o transcodificado con FFmpeg |
| GET | `/api/lyrics/:id` | Letras sincronizadas (`.lrc`) o texto plano |
| GET | `/api/tracks/:id/radio` | Radio generada a partir de la canción |
| GET | `/api/cover/:albumId` | Portada (optimizada a 600×600 si supera 250 KB) |
| GET/PUT/DELETE | `/api/favorites`, `/api/favorites/:trackId` | |
| GET/POST/PATCH/DELETE | `/api/playlists`, `/api/playlists/:id` | Soporta `{name, isPublic}` |
| POST/DELETE | `/api/playlists/:id/tracks`, `/api/playlists/:id/tracks/:position` | |
| POST | `/api/playlists/:id/move` `{from, to}` | Reordena pistas dentro de la playlist |
| GET/PUT | `/api/folders`, `/api/folders/browse?path=` | Carpetas de música |
| GET/POST | `/api/scan` (`?full=1` relee todas las etiquetas) | |

</details>

## Licencia

© 2026 Alfonso Carmona Figueroa. Distribuido bajo la [PolyForm Noncommercial License 1.0.0](LICENSE): puedes usarlo, estudiarlo y modificarlo gratis para fines **no comerciales**. La venta o explotación comercial está reservada al autor.

---

### English

**Nasfonía** is a self-hosted music streaming server for your NAS (Docker, amd64/arm64) with a web player and an Android app. Install it with [`server/docker-compose.yml`](server/docker-compose.yml) and open `http://YOUR-NAS:8090`. It is free for non-commercial use under PolyForm Noncommercial 1.0.0; commercial rights are reserved by the author.
