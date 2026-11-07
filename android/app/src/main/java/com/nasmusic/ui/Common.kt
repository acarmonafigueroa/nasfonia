package com.nasmusic.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.nasmusic.NasMusicApp
import com.nasmusic.data.Album
import com.nasmusic.data.Artist
import com.nasmusic.data.Playlist
import com.nasmusic.data.Track
import com.nasmusic.ui.theme.Accent
import com.nasmusic.ui.theme.Muted
import com.nasmusic.ui.theme.Surface2
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

val LocalApp = staticCompositionLocalOf<NasMusicApp> { error("no app") }
val LocalNav = staticCompositionLocalOf<NavController> { error("no nav") }
val LocalToast = staticCompositionLocalOf<(String) -> Unit> { {} }

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val LIKED = "liked"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val FOLDERS = "folders"
    const val USERS = "users"
    fun album(id: Long) = "album/$id"
    fun artist(id: Long) = "artist/$id"
    fun playlist(id: Long) = "playlist/$id"
    fun genre(name: String) = "genre/${android.net.Uri.encode(name)}"
}

// ------------------------------------------------------------------ loading

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ok<T>(val value: T) : Load<T>
    data class Err(val message: String) : Load<Nothing>
}

class Loaded<T>(val state: Load<T>, val reload: () -> Unit)

/** Runs [loader] when [keys] change; `reload()` runs it again keeping the old content on screen. */
@Composable
fun <T> rememberLoad(vararg keys: Any?, loader: suspend () -> T): Loaded<T> {
    var version by remember { mutableIntStateOf(0) }
    val state by produceState<Load<T>>(Load.Loading, *keys, version) {
        value = try {
            Load.Ok(loader())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (value is Load.Ok) value else Load.Err(e.message ?: "Error de conexión")
        }
    }
    return Loaded(state) { version++ }
}

@Composable
fun <T> LoadBox(loaded: Loaded<T>, content: @Composable (T) -> Unit) {
    when (val s = loaded.state) {
        is Load.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent)
        }
        is Load.Err -> Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No se pudo cargar", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(s.message, color = Muted)
            Spacer(Modifier.height(16.dp))
            Button(onClick = loaded.reload) { Text("Reintentar") }
        }
        is Load.Ok -> content(s.value)
    }
}

// ------------------------------------------------------------------ formatting

fun formatTime(seconds: Double?): String {
    val s = (seconds ?: 0.0).toLong().coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec) else String.format(Locale.ROOT, "%d:%02d", m, sec)
}

fun formatLong(seconds: Double): String {
    val h = (seconds / 3600).toInt()
    val m = ((seconds % 3600) / 60).toInt()
    return if (h > 0) "$h h $m min" else "$m min"
}

fun plural(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"

// ------------------------------------------------------------------ images

@Composable
fun CoverImage(
    albumId: Long?,
    hasCover: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
    fallback: ImageVector = Icons.Rounded.MusicNote,
) {
    val app = LocalApp.current
    val model: Any? = app.offline.localCoverFile(albumId) ?: if (hasCover) app.api.coverUrl(albumId) else null
    Box(modifier.clip(shape).background(Surface2), contentAlignment = Alignment.Center) {
        val placeholder = @Composable {
            Icon(fallback, null, tint = Color(0xFF7A7A7A), modifier = Modifier.fillMaxSize(0.45f))
        }
        if (model == null) placeholder()
        else SubcomposeAsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { placeholder() },
            error = { placeholder() },
        )
    }
}

@Composable
fun LikedCover(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(4.dp)) {
    Box(
        modifier.clip(shape).background(Brush.linearGradient(listOf(Color(0xFF450AF5), Color(0xFF8E8EE5)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Favorite, null, tint = Color.White, modifier = Modifier.fillMaxSize(0.4f))
    }
}

// ------------------------------------------------------------------ rows & cards

@Composable
fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    showCover: Boolean = true,
    number: Int? = null,
    playlistId: Long? = null,
    indexInPlaylist: Int? = null,
    playlistSize: Int = 0,
    canEditPlaylist: Boolean = false,
    onRemoved: () -> Unit = {},
) {
    val app = LocalApp.current
    val player by app.player.state.collectAsState()
    val favorites by app.favorites.collectAsState()
    val downloadedIds by app.offline.downloadedIds.collectAsState()
    val downloadingIds by app.offline.downloadingIds.collectAsState()
    val playing = player.current?.id == track.id
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) {
            Text(
                "$number",
                color = if (playing) Accent else Muted,
                modifier = Modifier.width(28.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (showCover) {
            CoverImage(track.albumId, track.hasCover, Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = if (playing) Accent else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.id in downloadedIds) {
                    Icon(Icons.Rounded.DownloadDone, "Descargada", tint = Accent, modifier = Modifier.size(14.dp).padding(end = 4.dp))
                } else if (track.id in downloadingIds) {
                    CircularProgressIndicator(Modifier.size(12.dp).padding(end = 4.dp), strokeWidth = 1.5.dp, color = Accent)
                }
                Text(
                    track.artist,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (track.id in favorites) {
            IconButton(onClick = { app.toggleFavorite(track.id) }) {
                Icon(Icons.Rounded.Favorite, "Quitar de Me gusta", tint = Accent, modifier = Modifier.size(20.dp))
            }
        }
        TrackMenuButton(
            tracks = listOf(track),
            playlistId = playlistId,
            position = track.position,
            indexInPlaylist = indexInPlaylist,
            playlistSize = playlistSize,
            canEditPlaylist = canEditPlaylist,
            onRemoved = onRemoved,
        )
    }
}

/** "···" menu for one song (or a whole album when given several). */
@Composable
fun TrackMenuButton(
    tracks: List<Track>,
    playlistId: Long? = null,
    position: Int? = null,
    indexInPlaylist: Int? = null,
    playlistSize: Int = 0,
    canEditPlaylist: Boolean = false,
    onRemoved: () -> Unit = {},
    icon: ImageVector = Icons.Rounded.MoreVert,
) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val favorites by app.favorites.collectAsState()
    val downloadedIds by app.offline.downloadedIds.collectAsState()
    var open by remember { mutableStateOf(false) }
    var addTo by remember { mutableStateOf(false) }
    val single = tracks.singleOrNull()
    val allDownloaded = tracks.isNotEmpty() && tracks.all { it.id in downloadedIds }

    Box {
        IconButton(onClick = { open = true }) { Icon(icon, "Más opciones", tint = Muted) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Reproducir a continuación") },
                leadingIcon = { Icon(Icons.Rounded.SkipNext, null) },
                onClick = {
                    open = false
                    app.player.playNext(tracks)
                    toast("Se reproducirá a continuación")
                },
            )
            DropdownMenuItem(
                text = { Text("Añadir a la cola") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
                onClick = {
                    open = false
                    app.player.addToQueue(tracks)
                    toast("Añadido a la cola")
                },
            )
            if (single != null) {
                DropdownMenuItem(
                    text = { Text("Iniciar radio de la canción") },
                    leadingIcon = { Icon(Icons.Rounded.Radio, null) },
                    onClick = {
                        open = false
                        scope.launch {
                            runCatching { app.api.radio(single.id) }
                                .onSuccess { radio ->
                                    app.player.play(radio)
                                    toast("Radio iniciada a partir de «${single.title}»")
                                }
                                .onFailure { toast(it.message ?: "Error al iniciar la radio") }
                        }
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Añadir a playlist") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                onClick = {
                    open = false
                    addTo = true
                },
            )
            DropdownMenuItem(
                text = { Text(if (allDownloaded) "Borrar descarga del móvil" else "Descargar para escuchar sin conexión") },
                leadingIcon = { Icon(if (allDownloaded) Icons.Rounded.Delete else Icons.Rounded.Download, null) },
                onClick = {
                    open = false
                    if (allDownloaded) {
                        app.offline.removeAll(tracks)
                        toast("Descarga eliminada")
                    } else {
                        toast("Descargando ${plural(tracks.size, "canción", "canciones")}…")
                        app.offline.download(tracks) { n ->
                            if (n > 0) toast("${plural(n, "canción descargada", "canciones descargadas")}")
                        }
                    }
                },
            )
            if (single != null) {
                val fav = single.id in favorites
                DropdownMenuItem(
                    text = { Text(if (fav) "Quitar de Me gusta" else "Guardar en Me gusta") },
                    leadingIcon = { Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null) },
                    onClick = {
                        open = false
                        app.toggleFavorite(single.id)
                    },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Ir al álbum") },
                    leadingIcon = { Icon(Icons.Rounded.Album, null) },
                    onClick = {
                        open = false
                        nav.navigate(Routes.album(single.albumId))
                    },
                )
                DropdownMenuItem(
                    text = { Text("Ir al artista") },
                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                    onClick = {
                        open = false
                        nav.navigate(Routes.artist(single.artistId))
                    },
                )
            }
            if (playlistId != null && position != null && canEditPlaylist) {
                HorizontalDivider()
                if (indexInPlaylist != null && indexInPlaylist > 0) {
                    DropdownMenuItem(
                        text = { Text("Subir en la playlist") },
                        leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) },
                        onClick = {
                            open = false
                            scope.launch {
                                runCatching { app.api.movePlaylistTrack(playlistId, position, indexInPlaylist - 1) }
                                    .onSuccess { onRemoved() }
                                    .onFailure { toast(it.message ?: "Error") }
                            }
                        },
                    )
                }
                if (indexInPlaylist != null && indexInPlaylist + 1 < playlistSize) {
                    DropdownMenuItem(
                        text = { Text("Bajar en la playlist") },
                        leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) },
                        onClick = {
                            open = false
                            scope.launch {
                                runCatching { app.api.movePlaylistTrack(playlistId, position, indexInPlaylist + 1) }
                                    .onSuccess { onRemoved() }
                                    .onFailure { toast(it.message ?: "Error") }
                            }
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Quitar de esta playlist", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        open = false
                        scope.launch {
                            runCatching { app.api.removeFromPlaylist(playlistId, position) }
                                .onSuccess {
                                    app.playlistsChanged()
                                    onRemoved()
                                }
                                .onFailure { toast(it.message ?: "Error") }
                        }
                    },
                )
            }
        }
    }
    if (addTo) AddToPlaylistDialog(tracks) { addTo = false }
}

@Composable
fun AddToPlaylistDialog(tracks: List<Track>, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val loaded = rememberLoad { app.api.playlists() }
    var creating by remember { mutableStateOf(false) }

    if (creating) {
        NameDialog(title = "Nueva playlist", confirm = "Crear", onDismiss = onDismiss) { name ->
            scope.launch {
                runCatching { app.api.createPlaylist(name, tracks.map { it.id }) }
                    .onSuccess {
                        app.playlistsChanged()
                        toast("Añadido a ${it.name}")
                    }
                    .onFailure { toast(it.message ?: "Error") }
                onDismiss()
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir a playlist") },
        text = {
            LazyColumn(Modifier.height(320.dp)) {
                item {
                    Row(
                        Modifier.fillMaxWidth().clickable { creating = true }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(4.dp)).background(Surface2), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Add, null)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Nueva playlist", fontWeight = FontWeight.Bold)
                    }
                }
                val list = (loaded.state as? Load.Ok)?.value.orEmpty().filter { it.isMine }
                items(list, key = { it.id }) { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            scope.launch {
                                runCatching { app.api.addToPlaylist(p.id, tracks.map { it.id }) }
                                    .onSuccess {
                                        app.playlistsChanged()
                                        toast("Añadido a ${p.name}")
                                    }
                                    .onFailure { toast(it.message ?: "Error") }
                                onDismiss()
                            }
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverImage(p.coverAlbumId, p.coverAlbumId != null, Modifier.size(44.dp), fallback = Icons.AutoMirrored.Rounded.QueueMusic)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(plural(p.trackCount, "canción", "canciones"), color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun NameDialog(title: String, initial: String = "", confirm: String = "Guardar", onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it.take(120) }, singleLine = true, placeholder = { Text("Mi playlist") })
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim()) }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun AlbumCard(album: Album, width: Dp = 150.dp) {
    val nav = LocalNav.current
    Column(Modifier.width(width).clip(RoundedCornerShape(6.dp)).clickable { nav.navigate(Routes.album(album.id)) }.padding(4.dp)) {
        CoverImage(album.id, album.hasCover, Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(6.dp), Icons.Rounded.Album)
        Spacer(Modifier.height(8.dp))
        Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text(
            listOfNotNull(album.year?.toString(), album.artist).joinToString(" · "),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun ArtistCard(artist: Artist, width: Dp = 130.dp) {
    val nav = LocalNav.current
    Column(
        Modifier.width(width).clip(RoundedCornerShape(6.dp)).clickable { nav.navigate(Routes.artist(artist.id)) }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverImage(artist.coverAlbumId, artist.coverAlbumId != null, Modifier.fillMaxWidth().aspectRatio(1f), CircleShape, Icons.Rounded.Person)
        Spacer(Modifier.height(8.dp))
        Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text("Artista", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PlaylistCard(p: Playlist, width: Dp = 150.dp) {
    val nav = LocalNav.current
    Column(Modifier.width(width).clip(RoundedCornerShape(6.dp)).clickable { nav.navigate(Routes.playlist(p.id)) }.padding(4.dp)) {
        CoverImage(p.coverAlbumId, p.coverAlbumId != null, Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(6.dp), Icons.AutoMirrored.Rounded.QueueMusic)
        Spacer(Modifier.height(8.dp))
        Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text(plural(p.trackCount, "canción", "canciones"), color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

/** Row used in library lists: square/round thumbnail, title and subtitle. */
@Composable
fun ListItemRow(title: String, subtitle: String, onClick: () -> Unit, image: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        image()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SectionTitle(text: String, onMore: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (onMore != null) TextButton(onClick = onMore) { Text("Mostrar todo", color = Muted) }
    }
}

/** Header of album / artist / playlist pages: big cover over a tinted gradient. */
@Composable
fun PageHeader(
    tint: Color,
    title: String,
    subtitle: String,
    cover: @Composable (Modifier) -> Unit,
    onSubtitleClick: (() -> Unit)? = null,
    meta: String? = null,
) {
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = 0.35f).compositeOver(MaterialTheme.colorScheme.background), MaterialTheme.colorScheme.background)))
            .padding(top = 72.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        cover(Modifier.size(232.dp))
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().then(if (onSubtitleClick != null) Modifier.clickable(onClick = onSubtitleClick) else Modifier),
        )
        if (meta != null) Text(meta, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

/** Download + Shuffle + big green play/pause button under a page header. */
@Composable
fun PlayBar(tracks: List<Track>, extra: @Composable () -> Unit = {}) {
    val app = LocalApp.current
    val toast = LocalToast.current
    val player by app.player.state.collectAsState()
    val downloadedIds by app.offline.downloadedIds.collectAsState()
    val downloadingIds by app.offline.downloadingIds.collectAsState()
    val allDownloaded = tracks.isNotEmpty() && tracks.all { it.id in downloadedIds }
    val anyDownloading = tracks.any { it.id in downloadingIds }
    val activeInList = player.current?.id != null && tracks.any { it.id == player.current?.id }
    val isPlayingHere = activeInList && player.isPlaying

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tracks.isNotEmpty()) {
            IconButton(
                onClick = {
                    if (allDownloaded) {
                        app.offline.removeAll(tracks)
                        toast("Descarga eliminada")
                    } else if (!anyDownloading) {
                        toast("Descargando ${plural(tracks.size, "canción", "canciones")}…")
                        app.offline.download(tracks) { n ->
                            if (n > 0) toast("${plural(n, "canción descargada", "canciones descargadas")}")
                        }
                    }
                },
            ) {
                when {
                    anyDownloading -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Accent)
                    allDownloaded -> Icon(Icons.Rounded.DownloadDone, "Descargado", tint = Accent)
                    else -> Icon(Icons.Rounded.Download, "Descargar", tint = Muted)
                }
            }
        }
        extra()
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { app.player.play(tracks, shuffle = true) }, enabled = tracks.isNotEmpty()) {
            Icon(Icons.Rounded.Shuffle, "Aleatorio", tint = if (player.shuffle && activeInList) Accent else Muted, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = { if (activeInList) app.player.togglePlay() else app.player.play(tracks) },
            enabled = tracks.isNotEmpty(),
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Accent, contentColor = Color.Black),
        ) {
            Icon(if (isPlayingHere) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isPlayingHere) "Pausar" else "Reproducir", modifier = Modifier.size(32.dp))
        }
    }
}

/** Calls [reload] when [keys] change, but not on first composition. */
@Composable
fun ReloadOn(vararg keys: Any?, reload: () -> Unit) {
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(*keys) {
        if (first) first = false else reload()
    }
}
