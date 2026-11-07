package com.nasmusic.player

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.nasmusic.NasMusicApp
import com.nasmusic.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plays in the background, shows the media notification / lock-screen controls,
 * and exposes a browse tree for Android Auto.
 */
class PlaybackService : MediaLibraryService() {
    private var session: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var countedItemId: String? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as NasMusicApp
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                countedItemId = null
            }
        })

        // Count a play only after 30 s (or half the track if shorter), matching the web player.
        serviceScope.launch {
            while (true) {
                delay(2000)
                if (!player.isPlaying) continue
                val item = player.currentMediaItem ?: continue
                val mediaId = item.mediaId
                if (mediaId == countedItemId) continue
                val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 60_000L
                val threshold = minOf(30_000L, (dur / 2).coerceAtLeast(5_000L))
                if (player.currentPosition >= threshold) {
                    countedItemId = mediaId
                    mediaId.toLongOrNull()?.let { app.recordPlay(it) }
                }
            }
        }

        val openApp = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        session = MediaLibrarySession.Builder(this, player, LibraryCallback(app))
            .setSessionActivity(openApp)
            .build()
    }

    private inner class LibraryCallback(private val app: NasMusicApp) : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
            LibraryResult.ofItem(browsableFolder("root", "Nasfonía"), params),
        )

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch(Dispatchers.IO) {
                val items: List<MediaItem> = runCatching {
                    when {
                        parentId == "root" -> listOf(
                            browsableFolder("liked", "Canciones que te gustan", MediaMetadata.MEDIA_TYPE_PLAYLIST),
                            browsableFolder("recent", "Escuchado recientemente", MediaMetadata.MEDIA_TYPE_PLAYLIST),
                            browsableFolder("offline", "Descargadas", MediaMetadata.MEDIA_TYPE_PLAYLIST),
                            browsableFolder("playlists", "Playlists", MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
                            browsableFolder("albums", "Álbumes", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                        )
                        parentId == "liked" -> app.api.favorites().map(::playableTrack)
                        parentId == "recent" -> app.api.home().recentTracks.map(::playableTrack)
                        parentId == "offline" -> app.offline.downloadedTracks.value.map(::playableTrack)
                        parentId == "playlists" -> app.api.playlists().map { p ->
                            browsableFolder("playlist:${p.id}", p.name, MediaMetadata.MEDIA_TYPE_PLAYLIST)
                        }
                        parentId == "albums" -> app.api.albums("recent").take(60).map { a ->
                            browsableFolder("album:${a.id}", a.title, MediaMetadata.MEDIA_TYPE_ALBUM, a.artist)
                        }
                        parentId.startsWith("playlist:") -> {
                            val id = parentId.removePrefix("playlist:").toLongOrNull() ?: 0L
                            app.api.playlist(id).tracks.map(::playableTrack)
                        }
                        parentId.startsWith("album:") -> {
                            val id = parentId.removePrefix("album:").toLongOrNull() ?: 0L
                            app.api.album(id).tracks.map(::playableTrack)
                        }
                        else -> emptyList()
                    }
                }.getOrDefault(emptyList())
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            }
            return future
        }

        // Controllers may strip the URI when handing items over; it travels in requestMetadata too,
        // or if chosen from Android Auto, we build the stream/local URI from the mediaId.
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = Futures.immediateFuture(
            mediaItems.map { item ->
                val uri = item.localConfiguration?.uri
                    ?: item.requestMetadata.mediaUri
                    ?: item.mediaId.toLongOrNull()?.let { id ->
                        app.offline.localTrackFile(id)?.let(Uri::fromFile) ?: Uri.parse(app.api.streamUrl(id))
                    }
                item.buildUpon().setUri(uri).build()
            }.toMutableList(),
        )

        private fun browsableFolder(
            id: String,
            title: String,
            mediaType: Int = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
            subtitle: String? = null,
        ): MediaItem = MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(mediaType)
                    .build(),
            )
            .build()

        private fun playableTrack(t: Track): MediaItem {
            val uri = app.offline.localTrackFile(t.id)?.let(Uri::fromFile) ?: Uri.parse(app.api.streamUrl(t.id))
            val cover = app.offline.localCoverFile(t.albumId)?.let(Uri::fromFile)
                ?: if (t.hasCover) app.api.coverUrl(t.albumId)?.let(Uri::parse) else null
            return MediaItem.Builder()
                .setMediaId(t.id.toString())
                .setUri(uri)
                .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(t.title)
                        .setArtist(t.artist)
                        .setAlbumTitle(t.album)
                        .setArtworkUri(cover)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .build(),
                )
                .build()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        // Keep playing after the app is swiped away; stop only if nothing is playing.
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
