# Nasfonía

**Tu propio servicio de música en streaming, con tu música y en tu NAS.** Nasfonía indexa la música que tienes en tu NAS y te la sirve en streaming: en el navegador, con un reproductor web, y en el móvil, con una app Android.

Sin suscripciones, sin anuncios y sin que tus datos salgan de casa.

- 🎵 Inicio con lo que has escuchado hace poco, lo último añadido y lo que más escuchas
- 🔎 Búsqueda instantánea, sin acentos («camaron» encuentra «Camarón»)
- 💿 Álbumes, artistas, géneros y portadas (incrustadas o `cover.jpg`)
- ❤️ Canciones que te gustan, playlists, cola, aleatorio y repetir
- 📁 Eliges las carpetas de música desde la propia app
- 📱 Android: reproducción en segundo plano, notificación, pantalla de bloqueo y Bluetooth
- 🎧 MP3, FLAC, M4A/AAC, OGG, Opus, WAV… con avance rápido dentro de la canción

Funciona en NAS **UGREEN (UGOS Pro)**, Synology, QNAP, Unraid, TrueNAS o cualquier equipo con Docker, en procesadores Intel/AMD y ARM.

---

## 1. Instalar el servidor

### UGREEN (UGOS Pro)

1. Abre **Docker → Proyecto → Crear**.
2. Elige una carpeta para el proyecto, por ejemplo `/volume1/docker/nasfonia`.
3. Pega el contenido de [`server/docker-compose.yml`](server/docker-compose.yml) y cambia:
   - `MUSIC_PASSWORD`: la contraseña con la que entrarás.
   - `/volume1/Music`: la carpeta donde tienes tu música.
4. Pulsa **Desplegar** y abre `http://IP-DE-TU-NAS:8090`.

### Cualquier otro NAS o Linux con Docker

```bash
mkdir nasfonia && cd nasfonia
curl -O https://raw.githubusercontent.com/acarmonafigueroa/nasfonia/main/server/docker-compose.yml
# edita la contraseña y las rutas
docker compose up -d
```

### Elegir qué carpetas forman tu biblioteca

Todo lo que montes bajo `/nas` en el `docker-compose.yml` se puede explorar desde la web (icono de usuario → **Carpetas de música**) o desde la app (⚙ en Inicio). Las carpetas se montan en **solo lectura**: Nasfonía nunca modifica tus archivos.

### Variables de entorno

| Variable | Por defecto | Para qué sirve |
|---|---|---|
| `MUSIC_PASSWORD` | se genera y aparece en el registro | Contraseña de acceso |
| `MUSIC_DIR` | `/music` | Carpetas iniciales, separadas por comas |
| `BROWSE_ROOT` | `/nas` | Raíz que se puede explorar al elegir carpetas |
| `SERVER_NAME` | `Nasfonía` | Nombre que se muestra en la app |
| `SCAN_INTERVAL_MINUTES` | `60` | Reescaneo automático (0 = desactivado) |
| `PORT` | `8090` | Puerto |

Si cambias `MUSIC_PASSWORD`, se cierra la sesión en todos los dispositivos.

### Actualizar

En UGOS: **Docker → Proyecto → nasfonia → Actualizar imagen**. En la terminal: `docker compose pull && docker compose up -d`.

## 2. App Android

- **Google Play:** próximamente.
- **APK:** descárgalo en [Releases](https://github.com/acarmonafigueroa/nasfonia/releases/latest).

Al abrirla escribe la IP de tu NAS (por ejemplo `192.168.1.50`; si no pones puerto, usa el 8090) y la contraseña. El móvil tiene que estar en la misma red que el NAS.

**Fuera de casa:** no abras el puerto 8090 a Internet. Conéctate a tu red con una VPN (WireGuard, Tailscale, la de tu router o la de tu NAS) y usa la misma dirección.

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
| POST | `/api/login` `{password}` | Devuelve `{token}` |
| GET | `/api/home` | Pantalla de inicio |
| GET | `/api/albums?sort=title\|artist\|year\|recent\|random`, `/api/albums/:id` | |
| GET | `/api/artists`, `/api/artists/:id`, `/api/artists/:id/tracks` | |
| GET | `/api/genres`, `/api/genres/:name/tracks` | |
| GET | `/api/search?q=` | |
| GET | `/api/stream/:id` | Audio original, con soporte de Range |
| GET | `/api/cover/:albumId` | Portada |
| GET/PUT/DELETE | `/api/favorites`, `/api/favorites/:trackId` | |
| GET/POST/PATCH/DELETE | `/api/playlists`, `/api/playlists/:id` | |
| POST/DELETE | `/api/playlists/:id/tracks`, `/api/playlists/:id/tracks/:position` | |
| GET/PUT | `/api/folders`, `/api/folders/browse?path=` | Carpetas de música |
| GET/POST | `/api/scan` (`?full=1` relee todas las etiquetas) | |

</details>

## Limitaciones

- No convierte formatos: se envía el archivo original. ALAC, WMA o APE pueden no sonar en el navegador.
- Un único usuario, con una contraseña compartida.
- Todavía no hay descargas para escuchar sin conexión.

## Licencia

© 2026 Alfonso Carmona Figueroa. Distribuido bajo la [PolyForm Noncommercial License 1.0.0](LICENSE): puedes usarlo, estudiarlo y modificarlo gratis para fines **no comerciales**. La venta o explotación comercial está reservada al autor.

---

### English

**Nasfonía** is a self-hosted music streaming server for your NAS (Docker, amd64/arm64) with a web player and an Android app. Install it with [`server/docker-compose.yml`](server/docker-compose.yml) and open `http://YOUR-NAS:8090`. It is free for non-commercial use under PolyForm Noncommercial 1.0.0; commercial rights are reserved by the author.
