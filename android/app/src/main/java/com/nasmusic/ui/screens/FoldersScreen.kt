package com.nasmusic.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nasmusic.data.FolderListing
import com.nasmusic.data.MusicFolder
import com.nasmusic.ui.Load
import com.nasmusic.ui.LoadBox
import com.nasmusic.ui.LocalApp
import com.nasmusic.ui.LocalNav
import com.nasmusic.ui.LocalToast
import com.nasmusic.ui.plural
import com.nasmusic.ui.rememberLoad
import com.nasmusic.ui.theme.Accent
import com.nasmusic.ui.theme.Muted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Picks which NAS folders make up the library. */
@Composable
fun FoldersScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val loaded = rememberLoad { app.api.folders() }
    val dirs = remember { mutableStateListOf<MusicFolder>() }
    var browsing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(loaded.state) {
        (loaded.state as? Load.Ok)?.let { dirs.clear(); dirs.addAll(it.value.dirs) }
    }

    if (browsing) {
        FolderBrowser(
            onCancel = { browsing = false },
            onPick = { picked ->
                if (dirs.none { it.path == picked.path }) dirs += MusicFolder(picked.path, picked.name)
                browsing = false
            },
        )
        return
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BackTitle("Carpetas de música")
        LoadBox(loaded) { info -> Column(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    Text(
                        "La biblioteca se forma con estas carpetas del NAS y todas sus subcarpetas.",
                        color = Muted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(dirs, key = { it.path }) { d ->
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Folder, null, tint = Accent)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (!d.exists) Text("No existe", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { dirs.remove(d) }) { Icon(Icons.Rounded.Close, "Quitar", tint = Muted) }
                    }
                }
                item {
                    if (info.canBrowse) {
                        OutlinedButton(onClick = { browsing = true }, modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Rounded.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Añadir carpeta")
                        }
                    } else {
                        Text("El servidor no tiene volúmenes del NAS montados en ${info.browseRoot}.", color = Muted, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            Button(
                onClick = {
                    saving = true
                    scope.launch {
                        try {
                            app.api.setFolders(dirs.map { it.path })
                            toast("Carpetas guardadas. Escaneando…")
                            while (true) {
                                delay(1500)
                                val s = app.api.scanStatus()
                                if (!s.running) {
                                    toast(s.error?.let { "Error al escanear: $it" } ?: "Escaneo terminado: ${s.added} nuevas, ${s.removed} eliminadas")
                                    break
                                }
                            }
                            nav.popBackStack()
                        } catch (e: Exception) {
                            toast(e.message ?: "Error")
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = dirs.isNotEmpty() && !saving,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
            ) {
                if (saving) CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.width(22.dp).height(22.dp))
                else Text("Guardar y escanear", style = MaterialTheme.typography.labelLarge)
            }
        } }
    }
}

@Composable
private fun FolderBrowser(onCancel: () -> Unit, onPick: (FolderListing) -> Unit) {
    val app = LocalApp.current
    var path by remember { mutableStateOf<String?>(null) }
    val loaded = rememberLoad(path) { app.api.browseFolder(path) }
    val current = (loaded.state as? Load.Ok)?.value

    BackHandler {
        val parent = current?.parent
        if (parent != null) path = parent else onCancel()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { current?.parent?.let { path = it } ?: onCancel() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás")
            }
            Text(current?.name ?: "NAS", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, "Cancelar") }
        }
        Box(Modifier.weight(1f)) {
            LoadBox(loaded) { listing ->
                LazyColumn {
                    items(listing.folders, key = { it.path }) { f ->
                        Row(
                            Modifier.fillMaxWidth().clickable { path = f.path }.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Folder, null, tint = Muted)
                            Spacer(Modifier.width(16.dp))
                            Text(f.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Muted)
                        }
                    }
                    if (listing.folders.isEmpty()) {
                        item { Text("No hay subcarpetas.", color = Muted, modifier = Modifier.padding(16.dp)) }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                current?.let { if (it.audioFiles > 0) "${plural(it.audioFiles, "canción", "canciones")} directamente en esta carpeta" else "Se incluyen también las subcarpetas." }.orEmpty(),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = { current?.let(onPick) },
                // The root itself (all volumes) is too broad to pick.
                enabled = current != null && current.parent != null,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Elegir esta carpeta", style = MaterialTheme.typography.labelLarge) }
        }
    }
}
