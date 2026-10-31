package com.nasmusic.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.nasmusic.NasMusicApp
import com.nasmusic.ui.theme.NasMusicTheme

class MainActivity : ComponentActivity() {
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        // The playback notification needs this on Android 13+.
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        val app = application as NasMusicApp
        setContent {
            NasMusicTheme { AppRoot(app) }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as NasMusicApp).player.connect()
    }
}
