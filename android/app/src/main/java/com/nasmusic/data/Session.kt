package com.nasmusic.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Server addresses, user and token, kept across launches.
 *
 * There can be two addresses: [serverUrl] (usually the one at home, e.g. http://192.168.1.50:8090)
 * and [altUrl] (from outside, e.g. https://musica.example.com). [activeUrl] is whichever answers now.
 */
class Session(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    var serverUrl: String = prefs.getString("server", "") ?: ""
        private set
    var altUrl: String = prefs.getString("alt", "") ?: ""
        private set
    var serverName: String = prefs.getString("name", "") ?: ""
        private set
    var username: String = prefs.getString("user", "") ?: ""
        private set

    /** Not persisted: picked again on every start and network change. */
    @Volatile
    var activeUrl: String = serverUrl

    private val _token = MutableStateFlow(prefs.getString("token", null))
    val token: StateFlow<String?> = _token.asStateFlow()

    val isLoggedIn get() = _token.value != null

    fun save(url: String, alt: String, name: String, user: String, token: String) {
        serverUrl = url
        altUrl = alt
        serverName = name
        username = user
        activeUrl = url
        prefs.edit()
            .putString("server", url)
            .putString("alt", alt)
            .putString("name", name)
            .putString("user", user)
            .putString("token", token)
            .apply()
        _token.value = token
    }

    fun setToken(token: String) {
        prefs.edit().putString("token", token).apply()
        _token.value = token
    }

    fun setUrls(url: String, alt: String) {
        serverUrl = url
        altUrl = alt
        prefs.edit().putString("server", url).putString("alt", alt).apply()
    }

    fun logout() {
        prefs.edit().remove("token").apply()
        _token.value = null
    }
}
