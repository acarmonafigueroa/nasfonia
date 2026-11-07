package com.nasmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.nasmusic.player.PlayerState
import com.nasmusic.ui.CoverImage
import com.nasmusic.ui.Load
import com.nasmusic.ui.LocalApp
import com.nasmusic.ui.LocalNav
import com.nasmusic.ui.LocalToast
import com.nasmusic.ui.Routes
import com.nasmusic.ui.TrackMenuButton
import com.nasmusic.ui.formatTime
import com.nasmusic.ui.rememberLoad
import com.nasmusic.ui.theme.Accent
import com.nasmusic.ui.theme.Muted
import com.nasmusic.ui.theme.hueColor

@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val app = LocalApp.current
    val s by app.player.state.collectAsState()
    val favorites by app.favorites.collectAsState()
    val t = s.current ?: return
    val tint = hueColor(t.albumId)
    Box(Modifier.padding(horizontal = 8.dp).padding(bottom = 4.dp).clip(RoundedCornerShape(8.dp)).background(tint).clickable(onClick = onOpen)) {
        Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverImage(t.albumId, t.hasCover, Modifier.size(44.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFFDDDDDD), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { app.toggleFavorite(t.id) }) {
                val fav = t.id in favorites
                Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Me gusta", tint = if (fav) Accent else Color.White)
            }
            IconButton(onClick = app.player::togglePlay) {
                Icon(if (s.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (s.isPlaying) "Pausar" else "Reproducir", modifier = Modifier.size(30.dp))
            }
        }
        LinearProgressIndicator(
            progress = { if (s.durationMs > 0) s.positionMs.toFloat() / s.durationMs else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).padding(horizontal = 8.dp),
            color = Color.White,
            trackColor = Color(0x44FFFFFF),
            drawStopIndicator = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(onClose: () -> Unit) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val toast = LocalToast.current
    val s by app.player.state.collectAsState()
    val favorites by app.favorites.collectAsState()
    val t = s.current ?: return
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    // While dragging, the slider shows the finger's position instead of playback.
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val tint = hueColor(t.albumId)

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(tint, Color(0xFF121212))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Cerrar", modifier = Modifier.size(32.dp)) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REPRODUCIENDO DESDE", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDDDDDD))
                Text(t.album, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            }
            TrackMenuButton(listOf(t))
        }

        Spacer(Modifier.weight(1f))
        if (showLyrics) {
            LyricsBox(
                trackId = t.id,
                positionMs = s.positionMs,
                onSeek = { ms -> app.player.seekTo(ms) },
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
        } else {
            CoverImage(
                t.albumId,
                t.hasCover,
                Modifier.fillMaxWidth().aspectRatio(1f).clickable {
                    onClose()
                    nav.navigate(Routes.album(t.albumId))
                },
                RoundedCornerShape(8.dp),
            )
        }
        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    t.artist,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable {
                        onClose()
                        nav.navigate(Routes.artist(t.artistId))
                    },
                )
            }
            IconButton(onClick = { app.toggleFavorite(t.id) }) {
                val fav = t.id in favorites
                Icon(
                    if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    "Me gusta",
                    tint = if (fav) Accent else Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        val duration = s.durationMs.coerceAtLeast(1)
        Slider(
            value = if (dragging) dragValue else (s.positionMs.toFloat() / duration).coerceIn(0f, 1f),
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                app.player.seekTo((dragValue * duration).toLong())
                dragging = false
            },
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color(0x55FFFFFF)),
        )
        Row(Modifier.fillMaxWidth()) {
            val pos = if (dragging) dragValue * duration else s.positionMs.toFloat()
            Text(formatTime(pos / 1000.0), color = Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            Text(formatTime(s.durationMs / 1000.0), color = Muted, style = MaterialTheme.typography.bodySmall)
        }

        Controls(s)

        val sleepActive = s.sleepRemainingMs != null || s.sleepEndOfTrack
        val sleepLabel = when {
            s.sleepEndOfTrack -> "Fin de pista"
            s.sleepRemainingMs != null -> "${((s.sleepRemainingMs!! + 59_000L) / 60_000L)} min"
            else -> null
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            val qualityTag = if (app.session.streamBitrate > 0) "${app.session.streamBitrate} kbps" else listOfNotNull(t.format?.uppercase(), t.bitrate?.let { "$it kbps" }).joinToString(" · ")
            Text(
                listOfNotNull(qualityTag.ifBlank { null }, sleepLabel?.let { "☾ $it" }).joinToString(" · "),
                color = if (sleepActive) Accent else Muted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showLyrics = !showLyrics }) {
                Icon(Icons.Rounded.Lyrics, "Letra", tint = if (showLyrics) Accent else Color.White)
            }
            IconButton(onClick = { showSleepDialog = true }) {
                Icon(Icons.Rounded.Bedtime, "Temporizador", tint = if (sleepActive) Accent else Color.White)
            }
            IconButton(onClick = { showQueue = true }) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Cola") }
        }
    }

    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }, containerColor = Color(0xFF1F1F1F)) {
            QueueList(s)
        }
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            active = s.sleepRemainingMs != null || s.sleepEndOfTrack,
            onPick = { mins, endOfTrack ->
                app.player.setSleepTimer(mins, endOfTrack)
                showSleepDialog = false
                toast(
                    when {
                        endOfTrack -> "La reproducción se detendrá al acabar la canción"
                        mins > 0 -> "Temporizador activado: $mins min"
                        else -> "Temporizador desactivado"
                    },
                )
            },
            onDismiss = { showSleepDialog = false },
        )
    }
}

@Composable
private fun LyricsBox(trackId: Long, positionMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val loaded = rememberLoad(trackId) { app.api.lyrics(trackId) }
    val listState = rememberLazyListState()

    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x55000000)).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val st = loaded.state) {
            is Load.Loading -> CircularProgressIndicator(color = Accent)
            is Load.Err -> Text("No se pudo cargar la letra", color = Muted)
            is Load.Ok -> {
                val lyr = st.value
                if (lyr.lines.isEmpty()) {
                    Text(
                        "Esta canción no tiene letra guardada.\nPuedes colocar un archivo .lrc junto al archivo de audio en el NAS o incrustarla en sus etiquetas.",
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    val activeIndex = if (lyr.synced) {
                        lyr.lines.indexOfLast { it.timeMs != null && it.timeMs <= positionMs + 250 }.coerceAtLeast(0)
                    } else -1

                    LaunchedEffect(activeIndex, trackId) {
                        if (lyr.synced && activeIndex >= 0) {
                            runCatching { listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0)) }
                        }
                    }

                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(lyr.lines) { i, line ->
                            val isActive = lyr.synced && i == activeIndex
                            Text(
                                text = line.text.ifBlank { "♪" },
                                color = if (isActive) Accent else if (lyr.synced) Color(0xAAFFFFFF) else Color.White,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                style = if (isActive) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                                modifier = Modifier.fillMaxWidth().then(
                                    if (lyr.synced && line.timeMs != null) Modifier.clickable { onSeek(line.timeMs) } else Modifier,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerDialog(active: Boolean, onPick: (Int, Boolean) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        Triple("15 minutos", 15, false),
        Triple("30 minutos", 30, false),
        Triple("45 minutos", 45, false),
        Triple("1 hora", 60, false),
        Triple("Al terminar la canción actual", 0, true),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Temporizador de apagado") },
        text = {
            Column {
                options.forEach { (label, mins, eot) ->
                    TextButton(onClick = { onPick(mins, eot) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label, color = Color.White, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (active) {
                    TextButton(onClick = { onPick(0, false) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Desactivar temporizador", color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

@Composable
private fun Controls(s: PlayerState) {
    val app = LocalApp.current
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = app.player::toggleShuffle) {
            Icon(Icons.Rounded.Shuffle, "Aleatorio", tint = if (s.shuffle) Accent else Color.White, modifier = Modifier.size(28.dp))
        }
        IconButton(onClick = app.player::previous, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, "Anterior", modifier = Modifier.size(40.dp))
        }
        FilledIconButton(
            onClick = app.player::togglePlay,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black),
        ) {
            Icon(if (s.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (s.isPlaying) "Pausar" else "Reproducir", modifier = Modifier.size(40.dp))
        }
        IconButton(onClick = app.player::next, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, "Siguiente", modifier = Modifier.size(40.dp))
        }
        IconButton(onClick = app.player::cycleRepeat) {
            Icon(
                if (s.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                "Repetir",
                tint = if (s.repeatMode != Player.REPEAT_MODE_OFF) Accent else Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun QueueList(s: PlayerState) {
    val app = LocalApp.current
    LazyColumn(Modifier.fillMaxWidth().height(480.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Cola", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (s.queue.size > 1) TextButton(onClick = app.player::clearUpcoming) { Text("Borrar cola", color = Muted) }
            }
        }
        items(s.queue, key = { "${it.index}-${it.track.id}" }) { entry ->
            val isCurrent = entry.track.id == s.current?.id && entry == s.queue.first()
            Row(
                Modifier.fillMaxWidth().clickable { app.player.skipTo(entry.index) }.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverImage(entry.track.albumId, entry.track.hasCover, Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.track.title, color = if (isCurrent) Accent else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(entry.track.artist, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
                if (!isCurrent) {
                    IconButton(onClick = { app.player.removeFromQueue(entry.index) }) {
                        Icon(Icons.Rounded.Close, "Quitar de la cola", tint = Muted)
                    }
                }
            }
        }
    }
}
