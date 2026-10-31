package com.nasmusic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.nasmusic.ui.Load
import com.nasmusic.ui.LocalApp
import com.nasmusic.ui.LocalToast
import com.nasmusic.ui.rememberLoad
import com.nasmusic.ui.theme.Muted
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit) {
    val app = LocalApp.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val changeable = rememberLoad { app.api.canChangePassword() }
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    if ((changeable.state as? Load.Ok)?.value == false) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Cambiar contraseña") },
            text = { Text("La contraseña está fijada con MUSIC_PASSWORD en el docker-compose del servidor. Cámbiala allí y vuelve a desplegar el contenedor.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } },
        )
        return
    }

    fun submit() {
        error = when {
            next.length < 6 -> "La nueva contraseña debe tener al menos 6 caracteres"
            next != again -> "Las contraseñas nuevas no coinciden"
            else -> null
        }
        if (error != null) return
        busy = true
        scope.launch {
            try {
                app.api.changePassword(current, next)
                app.player.refreshUrls()
                toast("Contraseña cambiada")
                onDismiss()
            } catch (e: Exception) {
                error = e.message ?: "Error"
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Cambiar contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordField("Contraseña actual", current) { current = it }
                PasswordField("Nueva contraseña", next) { next = it }
                PasswordField("Repite la nueva", again) { again = it }
                Text("Se cerrará la sesión en el resto de dispositivos.", color = Muted, style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = ::submit, enabled = !busy && current.isNotEmpty() && next.isNotEmpty() && again.isNotEmpty()) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Cambiar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } },
    )
}

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}
