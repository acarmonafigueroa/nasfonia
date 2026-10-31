package com.nasmusic.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Server address and token, kept across launches. */
class Session(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    var serverUrl: String = prefs.getString("server", "") ?: ""
        private set
    var serverName: String = prefs.getString("name", "") ?: ""
        private set

    private val _token = MutableStateFlow(prefs.getString("token", null))
    val token: StateFlow<String?> = _token.asStateFlow()

    val isLoggedIn get() = _token.value != null

    fun save(url: String, name: String, token: String) {
        serverUrl = url
        serverName = name
        prefs.edit().putString("server", url).putString("name", name).putString("token", token).apply()
        _token.value = token
    }

    fun logout() {
        prefs.edit().remove("token").apply()
        _token.value = null
    }
}
