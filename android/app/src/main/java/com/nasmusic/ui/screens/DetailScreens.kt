package com.nasmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nasmusic.ui.AlbumCard
import com.nasmusic.ui.CoverImage
import com.nasmusic.ui.LoadBox
import com.nasmusic.ui.LocalApp
import com.nasmusic.ui.LocalNav
import com.nasmusic.ui.LocalToast
import com.nasmusic.ui.NameDialog
import com.nasmusic.ui.PageHeader
import com.nasmusic.ui.PlayBar
import com.nasmusic.ui.ReloadOn
import com.nasmusic.ui.Routes
import com.nasmusic.ui.SectionTitle
import com.nasmusic.ui.TrackMenuButton
import com.nasmusic.ui.TrackRow
import com.nasmusic.ui.formatLong
import com.nasmusic.ui.plural
import com.nasmusic.ui.rememberLoad
import com.nasmusic.ui.theme.Muted
import com.nasmusic.ui.theme.hueColor
import kotlinx.coroutines.launch

/** Detail page frame: content scrolls under a floating back button. */
@Composable
fun DetailScaffold(content: @Composable () -> Unit) {
    val nav = LocalNav.current
    Box(Modifier.fillMaxSize()) {
        content()
        IconButton(
            onClick = { nav.popBackStack() },
            modifier = Modifier.statusBarsPadding().padding(8.dp).clip(CircleShape).background(Color(0x99000000)),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás")
        }
    }
}

@Composable
fun BackTitle(title: String) {
    val nav = LocalNav.current
    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás") }
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun AlbumScreen(id: Long) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val loaded = rememberLoad(id) { app.api.album(id) }
    DetailScaffold {
        LoadBox(loaded) { a ->
            val multiDisc = a.tracks.map { it.discNo ?: 1 }.distinct().size > 1
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    PageHeader(
                        tint = hueColor(a.id),
                        title = a.title,
                        subtitle = a.artist,
                        onSubtitleClick = { nav.navigate(Routes.artist(a.artistId)) },
                        meta = listOfNotNull("Álbum", a.year?.toString(), plural(a.trackCount, "canción", "canciones"), formatLong(a.duration)).joinToString(" · "),
                        cover = { CoverImage(a.id, a.hasCover, it, RoundedCornerShape(6.dp), Icons.Rounded.Album) },
                    )
                }
                item { PlayBar(a.tracks) { TrackMenuButton(a.tracks) } }
                itemsIndexed(a.tracks, key = { _, t -> t.id }) { i, t ->
                    if (multiDisc && (i == 0 || a.tracks[i - 1].discNo != t.discNo)) {
                        Text("Disco ${t.discNo ?: 1}", color = Muted, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                    }
                    TrackRow(t, onClick = { app.player.play(a.tracks, i) }, showCover = false, number = t.trackNo ?: (i + 1))
                }
                a.genre?.let { g -> item { Text(g, color = Muted, modifier = Modifier.padding(16.dp)) } }
            }
        }
    }
}

@Composable
fun ArtistScreen(id: Long) {
    val app = LocalApp.current
    // The play buttons play the whole discography, not just the popular songs.
    val loaded = rememberLoad(id) { app.api.artist(id) to app.api.artistTracks(id) }
    DetailScaffold {
        LoadBox(loaded) { (a, allTracks) ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    PageHeader(
                        tint = hueColor(a.id + 7),
                        title = a.name,
                        subtitle = "${plural(a.albumCount, "álbum", "álbumes")} · ${plural(a.trackCount, "canción", "canciones")}",
                        cover = { CoverImage(a.coverAlbumId, a.coverAlbumId != null, it, CircleShape, Icons.Rounded.Person) },
                    )
                }
                item { PlayBar(allTracks) }
                if (a.topTracks.isNotEmpty()) {
                    item { SectionTitle("Populares") }
                    itemsIndexed(a.topTracks, key = { _, t -> "top${t.id}" }) { i, t ->
                        TrackRow(t, onClick = { app.player.play(a.topTracks, i) }, number = i + 1)
                    }
                }
                if (a.albums.isNotEmpty()) {
                    item { SectionTitle("Discografía") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(a.albums, key = { it.id }) { AlbumCard(it) }
                        }
                    }
                }
                if (a.appearsOn.isNotEmpty()) {
                    item { SectionTitle("Aparece en") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(a.appearsOn, key = { it.id }) { AlbumCard(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistScreen(id: Long) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val version by app.playlistsVersion.collectAsState()
    val loaded = rememberLoad(id) { app.api.playlist(id) }
    ReloadOn(version) { loaded.reload() }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    DetailScaffold {
        LoadBox(loaded) { p ->
            val subtitle = buildString {
                append(if (p.isPublic) "Playlist pública" else "Playlist")
                if (!p.isMine && !p.owner.isNullOrBlank()) append(" · de ${p.owner}")
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    PageHeader(
                        tint = hueColor(p.id + 3),
                        title = p.name,
                        subtitle = subtitle,
                        meta = listOfNotNull(plural(p.trackCount, "canción", "canciones"), p.duration.takeIf { it > 0 }?.let(::formatLong)).joinToString(" · "),
                        cover = { CoverImage(p.coverAlbumId, p.coverAlbumId != null, it, RoundedCornerShape(6.dp), Icons.AutoMirrored.Rounded.QueueMusic) },
                    )
                }
                item {
                    PlayBar(p.tracks) {
                        if (p.isMine) {
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Más opciones", tint = Muted) }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Cambiar nombre") },
                                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                        onClick = { menu = false; renaming = true },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (p.isPublic) "Hacer privada" else "Compartir con usuarios del NAS") },
                                        leadingIcon = { Icon(Icons.Rounded.Public, null) },
                                        onClick = {
                                            menu = false
                                            scope.launch {
                                                runCatching { app.api.setPlaylistPublic(p.id, !p.isPublic) }
                                                    .onSuccess {
                                                        app.playlistsChanged()
                                                        toast(if (!p.isPublic) "Playlist compartida en el NAS" else "La playlist vuelve a ser privada")
                                                    }
                                                    .onFailure { toast(it.message ?: "Error") }
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Eliminar playlist", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = { menu = false; deleting = true },
                                    )
                                }
                            }
                        }
                    }
                }
                itemsIndexed(p.tracks, key = { _, t -> "${t.position}-${t.id}" }) { i, t ->
                    TrackRow(
                        track = t,
                        onClick = { app.player.play(p.tracks, i) },
                        playlistId = p.id,
                        indexInPlaylist = i,
                        playlistSize = p.tracks.size,
                        canEditPlaylist = p.isMine,
                        onRemoved = loaded.reload,
                    )
                }
                if (p.tracks.isEmpty()) {
                    item {
                        Text(
                            "Playlist vacía. Usa el menú ⋮ de cualquier canción y elige «Añadir a playlist».",
                            color = Muted,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
            }

            if (renaming) {
                NameDialog("Cambiar nombre", initial = p.name, onDismiss = { renaming = false }) { name ->
                    renaming = false
                    scope.launch {
                        runCatching { app.api.renamePlaylist(p.id, name) }
                            .onSuccess { app.playlistsChanged() }
                            .onFailure { toast(it.message ?: "Error") }
                    }
                }
            }
            if (deleting) {
                AlertDialog(
                    onDismissRequest = { deleting = false },
                    title = { Text("¿Eliminar playlist?") },
                    text = { Text("Se eliminará «${p.name}». Las canciones siguen en tu biblioteca.") },
                    confirmButton = {
                        TextButton(onClick = {
                            deleting = false
                            scope.launch {
                                runCatching { app.api.deletePlaylist(p.id) }
                                    .onSuccess {
                                        app.playlistsChanged()
                                        nav.popBackStack()
                                    }
                                    .onFailure { toast(it.message ?: "Error") }
                            }
                        }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancelar") } },
                )
            }
        }
    }
}

@Composable
fun GenreScreen(name: String) {
    val app = LocalApp.current
    val loaded = rememberLoad(name) { app.api.genreTracks(name) }
    DetailScaffold {
        LoadBox(loaded) { tracks ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    PageHeader(
                        tint = hueColor(name.hashCode().toLong() and 0xffff),
                        title = name,
                        subtitle = "Género · ${plural(tracks.size, "canción", "canciones")}",
                        cover = {
                            val first = tracks.firstOrNull()
                            CoverImage(first?.albumId, first?.hasCover == true, it, RoundedCornerShape(6.dp), Icons.Rounded.MusicNote)
                        },
                    )
                }
                item { PlayBar(tracks) }
                itemsIndexed(tracks, key = { _, t -> t.id }) { i, t -> TrackRow(t, onClick = { app.player.play(tracks, i) }) }
            }
        }
    }
}
