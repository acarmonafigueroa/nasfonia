# Publicar Nasfonía en Google Play

Todo lo que la consola de Google Play te pide, listo para copiar.

## Archivos

| Qué | Dónde |
|---|---|
| App (Android App Bundle) | `android/app/build/outputs/bundle/release/app-release.aab` (`./gradlew bundleRelease`) |
| Icono 512×512 | `play/graficos/icono-512.png` |
| Gráfico destacado 1024×500 | `play/graficos/destacado-1024x500.png` |
| Capturas de móvil (mín. 2) | Hazlas en el móvil: Inicio, álbum, reproductor, búsqueda |
| Política de privacidad | https://acarmonafigueroa.github.io/nasfonia/privacidad.html |

**Clave de subida:** `android/keystore/nasfonia-upload.jks` + `android/keystore.properties`. No están en Git. **Haz copia de seguridad de los dos** (por ejemplo en tu NAS y en un USB). Si los pierdes, Google permite restablecer la clave de subida, pero es un trámite lento.

Activa **Firma de apps de Google Play** al crear la app (viene activada por defecto).

## Ficha (español)

- **Nombre:** Nasfonía: música de tu NAS
- **Descripción breve (80):** Escucha en streaming la música de tu NAS. Sin suscripciones ni anuncios.
- **Descripción completa:**

> Nasfonía convierte tu NAS en tu propio servicio de música. Instala el servidor gratuito Nasfonía en tu NAS (UGREEN, Synology, QNAP, Unraid… cualquiera con Docker) y escucha toda tu colección desde el móvil, como en un servicio de streaming, pero con tu música y sin que tus datos salgan de casa.
>
> • Inicio con lo que has escuchado hace poco, lo último añadido y lo que más escuchas
> • Álbumes, artistas y géneros con sus portadas
> • Búsqueda instantánea, sin importar las tildes
> • Canciones que te gustan y playlists, sincronizadas con la web
> • Cola, aleatorio y repetir
> • Reproducción en segundo plano, controles en la notificación y en la pantalla de bloqueo
> • Elige las carpetas de tu NAS que forman tu biblioteca
> • MP3, FLAC, AAC/M4A, OGG, Opus y WAV
>
> Sin cuentas, sin anuncios y sin rastreadores: la app solo se conecta al servidor que tú instalas.
>
> Necesitas el servidor Nasfonía instalado en tu NAS. Instrucciones: github.com/acarmonafigueroa/nasfonia

## Store listing (English)

- **Name:** Nasfonía: music from your NAS
- **Short description:** Stream the music on your NAS. No subscriptions, no ads.
- **Full description:**

> Nasfonía turns your NAS into your own music streaming service. Install the free Nasfonía server on your NAS (UGREEN, Synology, QNAP, Unraid… anything that runs Docker) and play your whole collection from your phone like a streaming service, with your music and without your data leaving home.
>
> • Home with recently played, recently added and most played
> • Albums, artists and genres with cover art
> • Instant, accent-insensitive search
> • Liked songs and playlists, synced with the web player
> • Queue, shuffle and repeat
> • Background playback with notification and lock-screen controls
> • Pick which NAS folders make up your library
> • MP3, FLAC, AAC/M4A, OGG, Opus and WAV
>
> No accounts, no ads, no trackers: the app only talks to the server you install.
>
> Requires the Nasfonía server on your NAS. Setup: github.com/acarmonafigueroa/nasfonia

## Respuestas de la consola

- **Categoría:** Música y audio · **Etiquetas:** Reproductor de música
- **Contenido:** sin anuncios · sin compras · clasificación: cuestionario IARC → todo «No» → PEGI 3
- **Público objetivo:** 18+ (evita los requisitos de la política de familias)
- **Acceso a la app:** «Toda la funcionalidad requiere acceso especial». Para la revisión, Google necesita un servidor accesible desde Internet: la forma sencilla es dejar temporalmente uno de demostración (por ejemplo un Nasfonía con un par de canciones libres detrás de Cloudflare Tunnel) y darles la URL y la contraseña.
- **Seguridad de los datos:**
  - ¿Recoge o comparte datos? → **No** (los datos solo viajan a un servidor que controla el usuario, no al desarrollador).
  - ¿Cifrado en tránsito? → Depende del servidor del usuario (HTTP en red local); responde «No».
  - ¿Los usuarios pueden pedir que se borren sus datos? → No aplica (no se recogen).
- **Servicio en primer plano:** tipo `mediaPlayback` → justificación: «Reproducir la música del usuario en segundo plano con controles en la notificación».
- **Anuncios:** No.

## Cuentas nuevas

Si tu cuenta de desarrollador personal es de después de noviembre de 2023, Google exige una **prueba cerrada con 12 testers durante 14 días** antes de pasar a producción. Sube el `.aab` a «Prueba cerrada», invita a 12 personas (por correo o un grupo de Google) y, pasados 14 días, solicita el acceso a producción.

## Nueva versión

1. Sube `versionCode` y `versionName` en `android/app/build.gradle.kts`.
2. `./gradlew bundleRelease assembleRelease`
3. Sube el `.aab` a Play y crea la Release en GitHub con el `.apk` (`gh release create vX.Y.Z android/app/build/outputs/apk/release/app-release.apk`).
4. La etiqueta `vX.Y.Z` publica también la imagen Docker nueva automáticamente.
