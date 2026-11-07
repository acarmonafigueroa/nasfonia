package com.nasmusic.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nasmusic.ui.AlbumCard
import com.nasmusic.ui.ArtistCard
import com.nasmusic.ui.CoverImage
import com.nasmusic.ui.LikedCover
import com.nasmusic.ui.ListItemRow
import com.nasmusic.ui.LoadBox
import com.nasmusic.ui.LocalApp
import com.nasmusic.ui.LocalNav
import com.nasmusic.ui.LocalToast
import com.nasmusic.ui.NameDialog
import com.nasmusic.ui.PageHeader
import com.nasmusic.ui.PlayBar
import com.nasmusic.ui.ReloadOn
import com.nasmusic.ui.Routes
import com.nasmusic.ui.TrackRow
import com.nasmusic.ui.plural
import com.nasmusic.ui.rememberLoad
import com.nasmusic.ui.theme.Liked
import com.nasmusic.ui.theme.Muted
import kotlinx.coroutines.launch

private enum class LibTab(val label: String) {
    PLAYLISTS("Playlists"),
    ALBUMS("Álbumes"),
    ARTISTS("Artistas"),
    DOWNLOADS("Descargadas"),
}

private val albumSorts = listOf(
    "artist" to "Artista",
    "title" to "Título",
    "year" to "Año",
    "recent" to "Recientes",
)

@Composable
fun LibraryScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(LibTab.PLAYLISTS) }
    var albumSort by rememberSaveable { mutableStateOf("artist") }
    var creating by remember { mutableStateOf(false) }
    val favorites by app.favorites.collectAsState()
    val downloadedTracks by app.offline.downloadedTracks.collectAsState()
    val playlistsVersion by app.playlistsVersion.collectAsState()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Tu biblioteca", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, "Crear playlist") }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibTab.entries.forEach { t ->
                FilterChip(
                    selected = tab == t,
                    onClick = { tab = t },
                    label = { Text(t.label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.White, selectedLabelColor = Color.Black),
                )
            }
        }
        when (tab) {
            LibTab.PLAYLISTS -> {
                val loaded = rememberLoad(playlistsVersion) { app.api.playlists() }
                LoadBox(loaded) { list ->
                    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        item {
                            ListItemRow("Canciones que te gustan", "Playlist · ${plural(favorites.size, "canción", "canciones")}", { nav.navigate(Routes.LIKED) }) {
                                LikedCover(Modifier.size(56.dp))
                            }
                        }
                        items(list, key = { it.id }) { p ->
                            val sub = buildString {
                                append(if (p.isPublic) "Pública" else "Playlist")
                                if (!p.isMine && !p.owner.isNullOrBlank()) append(" · de ${p.owner}")
                                append(" · ${plural(p.trackCount, "canción", "canciones")}")
                            }
                            ListItemRow(p.name, sub, { nav.navigate(Routes.playlist(p.id)) }) {
                                CoverImage(p.coverAlbumId, p.coverAlbumId != null, Modifier.size(56.dp), fallback = Icons.AutoMirrored.Rounded.QueueMusic)
                            }
                        }
                    }
                }
            }
            LibTab.ALBUMS -> {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    albumSorts.forEach { (key, label) ->
                        FilterChip(
                            selected = albumSort == key,
                            onClick = { albumSort = key },
                            label = { Text(label) },
                        )
                    }
                }
                val loaded = rememberLoad(albumSort) { app.api.albums(albumSort) }
                LoadBox(loaded) { list ->
                    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(list, key = { it.id }) { a ->
                            val sub = listOfNotNull("Álbum", a.year?.toString(), a.artist).joinToString(" · ")
                            ListItemRow(a.title, sub, { nav.navigate(Routes.album(a.id)) }) {
                                CoverImage(a.id, a.hasCover, Modifier.size(56.dp), fallback = Icons.Rounded.Album)
                            }
                        }
                    }
                }
            }
            LibTab.ARTISTS -> {
                val loaded = rememberLoad { app.api.artists() }
                LoadBox(loaded) { list ->
                    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(list, key = { it.id }) { a ->
                            ListItemRow(a.name, "Artista", { nav.navigate(Routes.artist(a.id)) }) {
                                CoverImage(a.coverAlbumId, a.coverAlbumId != null, Modifier.size(56.dp), CircleShape, Icons.Rounded.Person)
                            }
                        }
                    }
                }
            }
            LibTab.DOWNLOADS -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    if (downloadedTracks.isNotEmpty()) {
                        item { PlayBar(downloadedTracks) }
                    }
                    itemsIndexed(downloadedTracks, key = { _, t -> t.id }) { i, t ->
                        TrackRow(t, onClick = { app.player.play(downloadedTracks, i) })
                    }
                    if (downloadedTracks.isEmpty()) {
                        item {
                            Text(
                                "No tienes canciones descargadas en el móvil. Usa el icono de descarga en cualquier álbum, playlist o el menú ⋮ de una canción para escucharla sin conexión.",
                                color = Muted,
                                modifier = Modifier.padding(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        NameDialog("Crear playlist", confirm = "Crear", onDismiss = { creating = false }) { name ->
            creating = false
            scope.launch {
                runCatching { app.api.createPlaylist(name) }
                    .onSuccess {
                        app.playlistsChanged()
                        nav.navigate(Routes.playlist(it.id))
                    }
                    .onFailure { toast(it.message ?: "Error") }
            }
        }
    }
}

@Composable
fun AllAlbumsScreen() {
    val app = LocalApp.current
    var sort by rememberSaveable { mutableStateOf("artist") }
    val loaded = rememberLoad(sort) { app.api.albums(sort) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BackTitle("Álbumes")
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            albumSorts.forEach { (key, label) ->
                FilterChip(
                    selected = sort == key,
                    onClick = { sort = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.White, selectedLabelColor = Color.Black),
                )
            }
        }
        LoadBox(loaded) { list ->
            LazyVerticalGrid(GridCells.Adaptive(150.dp), contentPadding = PaddingValues(12.dp)) {
                items(list, key = { it.id }) { AlbumCard(it, width = 400.dp) }
            }
        }
    }
}

@Composable
fun AllArtistsScreen() {
    val app = LocalApp.current
    val loaded = rememberLoad { app.api.artists() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BackTitle("Artistas")
        LoadBox(loaded) { list ->
            LazyVerticalGrid(GridCells.Adaptive(130.dp), contentPadding = PaddingValues(12.dp)) {
                items(list, key = { it.id }) { ArtistCard(it, width = 400.dp) }
            }
        }
    }
}

@Composable
fun LikedScreen() {
    val app = LocalApp.current
    val favorites by app.favorites.collectAsState()
    val loaded = rememberLoad { app.api.favorites() }
    // Re-fetch when a heart is toggled elsewhere so the list matches.
    ReloadOn(favorites.size) { loaded.reload() }
    DetailScaffold {
        LoadBox(loaded) { all ->
            val tracks = all.filter { it.id in favorites }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    PageHeader(
                        tint = Liked,
                        title = "Canciones que te gustan",
                        subtitle = plural(tracks.size, "canción", "canciones"),
                        cover = { LikedCover(it) },
                    )
                }
                item { PlayBar(tracks) }
                itemsIndexed(tracks, key = { _, t -> t.id }) { i, t -> TrackRow(t, onClick = { app.player.play(tracks, i) }) }
                if (tracks.isEmpty()) {
                    item {
                        Text(
                            "Pulsa el corazón de cualquier canción para guardarla aquí.",
                            modifier = Modifier.padding(24.dp),
                            color = Color.Gray,
                        )
                    }
                }
            }
        }
    }
}
