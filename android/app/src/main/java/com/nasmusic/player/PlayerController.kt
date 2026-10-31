package com.nasmusic.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.nasmusic.data.Api
import com.nasmusic.data.Track
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

data class QueueEntry(val index: Int, val track: Track)

data class PlayerState(
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    /** Upcoming items in play order (shuffle-aware), current first. */
    val queue: List<QueueEntry> = emptyList(),
    val error: String? = null,
)

/** UI-side handle on PlaybackService. Calls made before it connects are queued. */
class PlayerController(private val context: Context, private val api: Api) {
    private val scope = MainScope()
    private var controller: MediaController? = null
    private var connecting = false
    private val pending = mutableListOf<(MediaController) -> Unit>()

    // Full track info by id; the session only carries title/artist/artwork.
    private val known = mutableMapOf<Long, Track>()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _state.value = _state.value.copy(error = "No se puede reproducir: ${error.errorCodeName}")
        }
    }

    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        scope.launch {
            try {
                val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
                val c = MediaController.Builder(context, token).buildAsync().await()
                c.addListener(listener)
                controller = c
                pending.forEach { it(c) }
                pending.clear()
                publish()
            } finally {
                connecting = false
            }
        }
        scope.launch {
            // Progress bar ticks.
            while (true) {
                delay(500)
                val c = controller ?: continue
                if (c.isPlaying) _state.value = _state.value.copy(positionMs = c.currentPosition, durationMs = duration(c))
            }
        }
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else {
            pending += block
            connect()
        }
    }

    private fun duration(c: Player): Long {
        val d = c.duration
        if (d != C.TIME_UNSET && d > 0) return d
        return ((trackOf(c.currentMediaItem)?.duration ?: 0.0) * 1000).toLong()
    }

    private fun trackOf(item: MediaItem?): Track? {
        item ?: return null
        val id = item.mediaId.toLongOrNull() ?: return null
        known[id]?.let { return it }
        // Items restored by the service after the UI process restarted.
        val m = item.mediaMetadata
        val extras = m.extras
        return Track(
            id = id,
            title = m.title?.toString() ?: "",
            artist = m.artist?.toString() ?: "",
            album = m.albumTitle?.toString() ?: "",
            artistId = extras?.getLong("artistId") ?: 0,
            albumId = extras?.getLong("albumId") ?: 0,
            hasCover = m.artworkUri != null,
            duration = extras?.getDouble("duration"),
        ).also { known[id] = it }
    }

    private fun publish() {
        val c = controller ?: return
        val timeline = c.currentTimeline
        val queue = buildList {
            if (!timeline.isEmpty && c.currentMediaItemIndex != C.INDEX_UNSET) {
                var i = c.currentMediaItemIndex
                val window = Timeline.Window()
                var guard = 0
                while (i != C.INDEX_UNSET && guard++ < 500) {
                    timeline.getWindow(i, window)
                    trackOf(window.mediaItem)?.let { add(QueueEntry(i, it)) }
                    i = timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, c.shuffleModeEnabled)
                }
            }
        }
        _state.value = PlayerState(
            current = trackOf(c.currentMediaItem),
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            positionMs = c.currentPosition,
            durationMs = duration(c),
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            queue = queue,
            error = _state.value.error.takeIf { c.playerError != null },
        )
    }

    private fun toItem(t: Track): MediaItem {
        known[t.id] = t
        val uri = Uri.parse(api.streamUrl(t.id))
        val extras = Bundle().apply {
            putLong("artistId", t.artistId)
            putLong("albumId", t.albumId)
            putDouble("duration", t.duration ?: 0.0)
        }
        return MediaItem.Builder()
            .setMediaId(t.id.toString())
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(t.title)
                    .setArtist(t.artist)
                    .setAlbumTitle(t.album)
                    .setArtworkUri(if (t.hasCover) api.coverUrl(t.albumId)?.let(Uri::parse) else null)
                    .setTrackNumber(t.trackNo)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    // --- commands -----------------------------------------------------------

    fun play(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val start = if (shuffle) tracks.indices.random() else startIndex.coerceIn(tracks.indices)
        withController { c ->
            // Like Spotify: the shuffle button stays as the user left it unless asked to shuffle.
            if (shuffle) c.shuffleModeEnabled = true
            c.setMediaItems(tracks.map(::toItem), start, 0)
            c.prepare()
            c.play()
        }
    }

    fun togglePlay() = withController { c ->
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            c.play()
        }
    }

    fun next() = withController { it.seekToNext() }
    fun previous() = withController { it.seekToPrevious() }
    fun seekTo(ms: Long) = withController {
        it.seekTo(ms)
        _state.value = _state.value.copy(positionMs = ms)
    }
    fun skipTo(index: Int) = withController {
        it.seekToDefaultPosition(index)
        it.play()
    }
    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun addToQueue(tracks: List<Track>) = withController { c ->
        if (c.mediaItemCount == 0) play(tracks) else c.addMediaItems(tracks.map(::toItem))
    }

    fun playNext(tracks: List<Track>) = withController { c ->
        if (c.mediaItemCount == 0) play(tracks) else c.addMediaItems(c.currentMediaItemIndex + 1, tracks.map(::toItem))
    }

    fun removeFromQueue(index: Int) = withController { it.removeMediaItem(index) }

    fun clearUpcoming() = withController { c ->
        val cur = c.currentMediaItemIndex
        if (cur + 1 < c.mediaItemCount) c.removeMediaItems(cur + 1, c.mediaItemCount)
    }

    /** Stream and cover URLs carry the token; rebuild the queue after it changes. */
    fun refreshUrls() = withController { c ->
        for (i in 0 until c.mediaItemCount) {
            val t = trackOf(c.getMediaItemAt(i)) ?: continue
            c.replaceMediaItem(i, toItem(t))
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun stop() {
        controller?.run {
            stop()
            clearMediaItems()
        }
    }
}
