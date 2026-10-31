package com.nasmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nasmusic.data.Genre
import com.nasmusic.data.SearchResult
import com.nasmusic.ui.AlbumCard
import com.nasmusic.ui.ArtistCard
import com.nasmusic.ui.CoverImage
import com.nasmusic.ui.Load
import com.nasmusic.ui.LoadBox
import com.nasmusic.ui.LocalApp
import com.nasmusic.ui.LocalNav
import com.nasmusic.ui.PlaylistCard
import com.nasmusic.ui.Routes
import com.nasmusic.ui.SectionTitle
import com.nasmusic.ui.TrackRow
import com.nasmusic.ui.rememberLoad
import com.nasmusic.ui.theme.Liked
import com.nasmusic.ui.theme.Muted
import com.nasmusic.ui.theme.hueColor
import kotlinx.coroutines.delay

@Composable
fun SearchScreen() {
    val app = LocalApp.current
    var query by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Buscar", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp))
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("¿Qué quieres escuchar?", color = Color.DarkGray) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Black) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Clear, "Borrar", tint = Color.Black) }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = Color.Black,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        if (query.isBlank()) Browse() else Results(query.trim())
    }
}

@Composable
private fun Browse() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val loaded = rememberLoad { app.api.genres() }
    LoadBox(loaded) { genres ->
        val tiles: List<Triple<String, Color, () -> Unit>> = buildList {
            add(Triple("Todos los álbumes", hueColor(1), { nav.navigate(Routes.ALBUMS) }))
            add(Triple("Todos los artistas", hueColor(2), { nav.navigate(Routes.ARTISTS) }))
            add(Triple("Me gusta", Liked, { nav.navigate(Routes.LIKED) }))
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item { SectionTitle("Explorar todo") }
            val all: List<Any> = tiles + genres
            items(all.chunked(2)) { pair ->
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { cell ->
                        val mod = Modifier.weight(1f)
                        @Suppress("UNCHECKED_CAST")
                        when (cell) {
                            is Triple<*, *, *> -> GenreTile(cell.first as String, cell.second as Color, null, mod, cell.third as () -> Unit)
                            is Genre -> GenreTile(cell.name, hueColor(cell.name.hashCode().toLong() and 0xffff), cell.coverAlbumId, mod) {
                                nav.navigate(Routes.genre(cell.name))
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GenreTile(title: String, color: Color, coverAlbumId: Long?, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.aspectRatio(1.7f).clip(RoundedCornerShape(8.dp)).background(color).clickable(onClick = onClick)) {
        Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(12.dp))
        if (coverAlbumId != null) {
            CoverImage(
                coverAlbumId,
                true,
                Modifier.size(64.dp).align(Alignment.BottomEnd).offset(x = 12.dp, y = 4.dp).rotate(25f),
            )
        }
    }
}

@Composable
private fun Results(query: String) {
    val app = LocalApp.current
    val loaded = rememberLoad(query) {
        delay(250) // debounce typing
        app.api.search(query)
    }
    if (loaded.state is Load.Ok && (loaded.state as Load.Ok<SearchResult>).value.let { it.tracks.isEmpty() && it.albums.isEmpty() && it.artists.isEmpty() && it.playlists.isEmpty() }) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No hay resultados para «$query»", style = MaterialTheme.typography.titleMedium)
            Text("Prueba con otras palabras.", color = Muted)
        }
        return
    }
    LoadBox(loaded) { r ->
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            if (r.artists.isNotEmpty()) {
                item { SectionTitle("Artistas") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(r.artists, key = { it.id }) { ArtistCard(it) }
                    }
                }
            }
            if (r.tracks.isNotEmpty()) {
                item { SectionTitle("Canciones") }
                itemsIndexed(r.tracks, key = { _, t -> t.id }) { i, t -> TrackRow(t, onClick = { app.player.play(r.tracks, i) }) }
            }
            if (r.albums.isNotEmpty()) {
                item { SectionTitle("Álbumes") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(r.albums, key = { it.id }) { AlbumCard(it) }
                    }
                }
            }
            if (r.playlists.isNotEmpty()) {
                item { SectionTitle("Playlists") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(r.playlists, key = { it.id }) { PlaylistCard(it) }
                    }
                }
            }
        }
    }
}
