package com.nasmusic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nasmusic.data.AppUser
import com.nasmusic.ui.LoadBox
import com.nasmusic.ui.LocalApp
import com.nasmusic.ui.LocalToast
import com.nasmusic.ui.rememberLoad
import com.nasmusic.ui.theme.Accent
import com.nasmusic.ui.theme.Muted
import kotlinx.coroutines.launch

/** Administrators: everyone who can sign in, with their own favorites, playlists and history. */
@Composable
fun UsersScreen() {
    val app = LocalApp.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val me by app.me.collectAsState()
    val loaded = rememberLoad { app.api.users() }
    var adding by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf<AppUser?>(null) }
    var deleting by remember { mutableStateOf<AppUser?>(null) }

    fun run(done: String? = null, block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }
                .onSuccess {
                    loaded.reload()
                    done?.let(toast)
                }
                .onFailure { toast(it.message ?: "Error") }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BackTitle("Usuarios")
        Text(
            "Cada usuario tiene sus propios favoritos, playlists e historial. La biblioteca de música es la misma para todos.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LoadBox(loaded) { users ->
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(users, key = { it.id }) { u ->
                    var menu by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (u.isAdmin) Icons.Rounded.Shield else Icons.Rounded.Person, null, tint = if (u.isAdmin) Accent else Muted)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.username + if (u.id == me?.id) " (tú)" else "")
                            Text(if (u.isAdmin) "Administrador" else "Usuario", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Opciones", tint = Muted) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                // Your own: "Cambiar contraseña" in the settings menu keeps this device signed in.
                                if (u.id == me?.id) {
                                    Text("Para tu contraseña usa «Cambiar contraseña» en ⚙", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Poner contraseña nueva") },
                                        leadingIcon = { Icon(Icons.Rounded.Key, null) },
                                        onClick = { menu = false; resetting = u },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (u.isAdmin) "Quitar administrador" else "Hacer administrador") },
                                        leadingIcon = { Icon(Icons.Rounded.Shield, null) },
                                        onClick = { menu = false; run { app.api.setUserAdmin(u.id, !u.isAdmin) } },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Borrar usuario", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = { menu = false; deleting = u },
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = { adding = true }, modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Añadir usuario")
                    }
                }
            }
        }
    }

    if (adding) {
        var name by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        var admin by remember { mutableStateOf(false) }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!busy) adding = false },
            title = { Text("Nuevo usuario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre de usuario") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false))
                    OutlinedTextField(pass, { pass = it }, label = { Text("Contraseña") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(admin, { admin = it })
                        Text("Administrador (usuarios, carpetas y escaneos)", style = MaterialTheme.typography.bodySmall)
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && name.isNotBlank() && pass.length >= 6, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            app.api.createUser(name, pass, admin)
                            adding = false
                            loaded.reload()
                            toast("Usuario creado")
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            busy = false
                        }
                    }
                }) { if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { adding = false }, enabled = !busy) { Text("Cancelar") } },
        )
    }

    resetting?.let { u ->
        var pass by remember(u.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { resetting = null },
            title = { Text("Contraseña nueva para ${u.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(pass, { pass = it }, label = { Text("Mínimo 6 caracteres") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    Text("Se cerrará su sesión en todos sus dispositivos.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = pass.length >= 6, onClick = {
                    resetting = null
                    run("Contraseña cambiada") { app.api.setUserPassword(u.id, pass) }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { resetting = null }) { Text("Cancelar") } },
        )
    }

    deleting?.let { u ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("¿Borrar a ${u.username}?") },
            text = { Text("Se borrarán sus favoritos, playlists e historial. La música no se toca.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    run("Usuario borrado") { app.api.deleteUser(u.id) }
                }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

/** Edit the home and outside addresses without signing out. */
@Composable
fun AddressesDialog(onDismiss: () -> Unit) {
    val app = LocalApp.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var home by remember { mutableStateOf(app.session.serverUrl) }
    var outside by remember { mutableStateOf(app.session.altUrl) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Direcciones del servidor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(home, { home = it }, label = { Text("En casa") }, placeholder = { Text("192.168.1.50") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                OutlinedTextField(outside, { outside = it }, label = { Text("Desde fuera (opcional)") }, placeholder = { Text("musica.midominio.com") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                Text(
                    "Ahora se usa: ${app.session.activeUrl}. La app prueba primero la de casa y cambia sola a la de fuera.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && home.isNotBlank(), onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        app.api.updateAddresses(home, outside)
                        toast("Direcciones guardadas")
                        onDismiss()
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        busy = false
                    }
                }
            }) { if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } },
    )
}

@Composable
fun AudioQualityDialog(onDismiss: () -> Unit) {
    val app = LocalApp.current
    val toast = LocalToast.current
    var selected by remember { mutableStateOf(app.session.streamBitrate) }
    val options = listOf(
        Triple(0, "Original (sin comprimir)", "Envía el archivo original del NAS (FLAC, MP3, etc.)"),
        Triple(320, "Alta (320 kbps)", "Convierte al vuelo en el servidor con FFmpeg"),
        Triple(192, "Estándar (192 kbps)", "Equilibrio entre calidad y consumo de datos"),
        Triple(128, "Ahorro de datos (128 kbps)", "Ideal fuera de casa con cobertura móvil limitada"),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calidad de audio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (kbps, title, desc) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = kbps }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == kbps, onClick = { selected = kbps })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.bodyLarge)
                            Text(desc, color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                app.session.setBitrate(selected)
                app.player.refreshUrls(resume = true)
                toast(if (selected == 0) "Calidad original activada" else "Calidad ajustada a $selected kbps")
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
