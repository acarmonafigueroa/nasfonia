package com.nasmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nasmusic.data.Album
import com.nasmusic.ui.AlbumCard
import com.nasmusic.ui.ArtistCard
import com.nasmusic.ui.CoverImage
import com.nasmusic.ui.LikedCover
import com.nasmusic.ui.LoadBox
import com.nasmusic.ui.LocalApp
import com.nasmusic.ui.LocalNav
import com.nasmusic.ui.LocalToast
import com.nasmusic.ui.PlaylistCard
import com.nasmusic.ui.ReloadOn
import com.nasmusic.ui.Routes
import com.nasmusic.ui.SectionTitle
import com.nasmusic.ui.TrackRow
import com.nasmusic.ui.formatLong
import com.nasmusic.ui.plural
import com.nasmusic.ui.rememberLoad
import com.nasmusic.ui.theme.Muted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 6 -> "Buenas noches"
        h < 13 -> "Buenos días"
        h < 21 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

@Composable
fun HomeScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val loaded = rememberLoad { app.api.home() }
    val playlistsVersion by app.playlistsVersion.collectAsState()
    ReloadOn(playlistsVersion) { loaded.reload() }
    var menu by remember { mutableStateOf(false) }
    var changingPassword by remember { mutableStateOf(false) }
    var editingAddresses by remember { mutableStateOf(false) }
    var editingQuality by remember { mutableStateOf(false) }
    val me by app.me.collectAsState()
    val onDefaultPassword = me?.isDefaultPassword == true
    if (changingPassword) ChangePasswordDialog(nag = onDefaultPassword) {
        changingPassword = false
        // So the warning disappears once it is changed.
        app.refreshMe()
    }
    if (editingAddresses) AddressesDialog { editingAddresses = false }
    if (editingQuality) AudioQualityDialog { editingQuality = false }

    fun scan(full: Boolean) {
        scope.launch {
            runCatching { app.api.startScan(full) }.onFailure { toast(it.message ?: "Error"); return@launch }
            toast("Escaneando la biblioteca…")
            while (true) {
                delay(1500)
                val s = runCatching { app.api.scanStatus() }.getOrNull() ?: break
                if (!s.running) {
                    toast(s.error?.let { "Error al escanear: $it" } ?: "Escaneo terminado: ${s.added} nuevas, ${s.removed} eliminadas")
                    loaded.reload()
                    break
                }
            }
        }
    }

    LoadBox(loaded) { home ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1F4A33), Color(0xFF121212))))
                        .statusBarsPadding()
                        .padding(top = 8.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(greeting(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.Settings, "Opciones") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                Text(
                                    "${me?.username ?: app.session.username} · ${app.session.serverName.ifBlank { app.session.activeUrl }}",
                                    color = Muted,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                                if (me?.isAdmin == true) {
                                    DropdownMenuItem(
                                        text = { Text("Usuarios") },
                                        leadingIcon = { Icon(Icons.Rounded.Group, null) },
                                        onClick = { menu = false; nav.navigate(Routes.USERS) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Carpetas de música") },
                                        leadingIcon = { Icon(Icons.Rounded.Folder, null) },
                                        onClick = { menu = false; nav.navigate(Routes.FOLDERS) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Volver a escanear la biblioteca") },
                                        leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                                        onClick = { menu = false; scan(false) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Escaneo completo") },
                                        leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                                        onClick = { menu = false; scan(true) },
                                    )
                                    HorizontalDivider()
                                }
                                DropdownMenuItem(
                                    text = { Text("Calidad de audio") },
                                    leadingIcon = { Icon(Icons.Rounded.Equalizer, null) },
                                    onClick = { menu = false; editingQuality = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Direcciones del servidor") },
                                    leadingIcon = { Icon(Icons.Rounded.Public, null) },
                                    onClick = { menu = false; editingAddresses = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Cambiar contraseña") },
                                    leadingIcon = { Icon(Icons.Rounded.Key, null) },
                                    onClick = { menu = false; changingPassword = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Cerrar sesión") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Logout, null) },
                                    onClick = { menu = false; app.logout() },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    QuickGrid(home.recentAlbums, home.newAlbums)
                }
            }

            if (onDefaultPassword) {
                item {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF4A3A10)).padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Estás usando la contraseña de serie", fontWeight = FontWeight.Bold)
                            Text("adminadmin es pública. Cámbiala por una tuya.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE8DDBB))
                        }
                        TextButton(onClick = { changingPassword = true }) { Text("Cambiar", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            if (home.stats.tracks == 0) {
                item {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tu biblioteca está vacía", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Copia música en la carpeta del NAS y pulsa «Volver a escanear» en el menú de arriba.",
                            color = Muted,
                        )
                    }
                }
            }
            if (home.recentAlbums.isNotEmpty()) {
                item { SectionTitle("Escuchado recientemente") }
                item { AlbumRow(home.recentAlbums) }
            }
            if (home.playlists.isNotEmpty()) {
                item { SectionTitle("Tus playlists", onMore = { nav.navigate(Routes.LIBRARY) }) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(home.playlists, key = { it.id }) { PlaylistCard(it) }
                    }
                }
            }
            if (home.newAlbums.isNotEmpty()) {
                item { SectionTitle("Añadido recientemente", onMore = { nav.navigate(Routes.ALBUMS) }) }
                item { AlbumRow(home.newAlbums) }
            }
            if (home.mostPlayed.isNotEmpty()) {
                item { SectionTitle("Lo que más escuchas") }
                itemsIndexed(home.mostPlayed.take(5), key = { _, t -> "mp${t.id}" }) { i, t ->
                    TrackRow(t, onClick = { app.player.play(home.mostPlayed, i) })
                }
            }
            if (home.artists.isNotEmpty()) {
                item { SectionTitle("Artistas", onMore = { nav.navigate(Routes.ARTISTS) }) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(home.artists, key = { it.id }) { ArtistCard(it) }
                    }
                }
            }
            if (home.randomAlbums.isNotEmpty()) {
                item { SectionTitle("Descubre en tu biblioteca") }
                item { AlbumRow(home.randomAlbums) }
            }
            if (home.stats.tracks > 0) {
                item {
                    Text(
                        "${plural(home.stats.tracks, "canción", "canciones")} · ${plural(home.stats.albums, "álbum", "álbumes")} · ${formatLong(home.stats.duration)}",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumRow(albums: List<Album>) {
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(albums, key = { it.id }) { AlbumCard(it) }
    }
}

/** The 2-column grid of shortcuts at the top, like Spotify's home. */
@Composable
private fun QuickGrid(recent: List<Album>, fresh: List<Album>) {
    val nav = LocalNav.current
    val albums = (recent + fresh.filter { f -> recent.none { it.id == f.id } }).take(5)
    val cells: List<@Composable (Modifier) -> Unit> = buildList {
        add { m -> QuickTile("Canciones que te gustan", m, { nav.navigate(Routes.LIKED) }) { LikedCover(Modifier.size(56.dp), RoundedCornerShape(0.dp)) } }
        albums.forEach { a ->
            add { m ->
                QuickTile(a.title, m, { nav.navigate(Routes.album(a.id)) }) {
                    CoverImage(a.id, a.hasCover, Modifier.size(56.dp), RoundedCornerShape(0.dp), Icons.Rounded.Album)
                }
            }
        }
    }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { it(Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickTile(title: String, modifier: Modifier, onClick: () -> Unit, image: @Composable () -> Unit) {
    Row(
        modifier.height(56.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x1FFFFFFF)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        image()
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}
