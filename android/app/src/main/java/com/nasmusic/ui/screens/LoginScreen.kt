package com.nasmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nasmusic.data.ApiException
import com.nasmusic.ui.LocalApp
import com.nasmusic.ui.theme.Accent
import com.nasmusic.ui.theme.Muted
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@Composable
fun LoginScreen() {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf(app.session.serverUrl) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val url = app.api.normalizeUrl(server)
        if (url == null) {
            error = "Escribe la dirección del NAS, por ejemplo 192.168.1.50:8090"
            return
        }
        busy = true
        error = null
        scope.launch {
            try {
                app.api.login(url, password)
                app.loadFavorites()
            } catch (e: ApiException) {
                error = e.message
            } catch (e: UnknownHostException) {
                error = "No se encuentra el servidor. Revisa la dirección."
            } catch (e: ConnectException) {
                error = "No se puede conectar. ¿Está el contenedor en marcha y el móvil en la misma red?"
            } catch (e: SocketTimeoutException) {
                error = "El servidor no responde."
            } catch (e: Exception) {
                error = e.message ?: "Error de conexión"
            } finally {
                busy = false
            }
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1F3A2A), Color.Black)))
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(Accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MusicNote, null, tint = Color.Black, modifier = Modifier.size(44.dp))
            }
            Text("Nasfonía", style = MaterialTheme.typography.headlineLarge)
            Text("Conéctate al servidor Nasfonía de tu NAS", color = Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                label = { Text("Dirección del servidor") },
                placeholder = { Text("192.168.1.50:8090") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { if (!busy) submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("¿Primera vez? La contraseña de serie es adminadmin.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = ::submit,
                enabled = !busy && server.isNotBlank() && password.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                else Text("Conectar", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
