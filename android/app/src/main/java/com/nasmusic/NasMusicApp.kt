package com.nasmusic

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import com.nasmusic.data.Api
import com.nasmusic.data.Session
import com.nasmusic.data.UserInfo
import com.nasmusic.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NasMusicApp : Application() {
    lateinit var session: Session
        private set
    lateinit var api: Api
        private set
    lateinit var player: PlayerController
        private set

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Liked songs, shared by every screen so hearts stay in sync.
    private val _favorites = MutableStateFlow<Set<Long>>(emptySet())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    // Bumped whenever playlists change, so open screens reload them.
    private val _playlistsVersion = MutableStateFlow(0)
    val playlistsVersion: StateFlow<Int> = _playlistsVersion.asStateFlow()

    // Who is signed in; decides which admin options show.
    private val _me = MutableStateFlow<UserInfo?>(null)
    val me: StateFlow<UserInfo?> = _me.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        session = Session(this)
        api = Api(session)
        player = PlayerController(this, api)
        // Stream and cover URLs carry the address: rebuild the queue when it changes.
        api.onAddressChanged = { player.refreshUrls(resume = true) }
        player.onStreamError = { scope.launch { if (api.pickReachable()) player.resume() } }
        watchNetwork()
        if (session.isLoggedIn) onSignedIn()
    }

    /** Leaving home Wi-Fi or coming back: use whichever address answers (home first). */
    private fun watchNetwork() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    // Give the new network a moment (DHCP, DNS) before probing.
                    delay(1500)
                    if (session.isLoggedIn) api.pickReachable()
                }
            }
        })
    }

    fun onSignedIn() {
        scope.launch {
            api.pickReachable()
            runCatching { api.me() }.onSuccess { _me.value = it }
            runCatching { api.favorites() }.onSuccess { list -> _favorites.value = list.map { it.id }.toSet() }
        }
    }

    fun refreshMe() {
        scope.launch { runCatching { api.me() }.onSuccess { _me.value = it } }
    }

    fun toggleFavorite(trackId: Long) {
        val on = trackId !in _favorites.value
        _favorites.update { if (on) it + trackId else it - trackId }
        scope.launch {
            runCatching { api.setFavorite(trackId, on) }.onFailure {
                _favorites.update { if (on) it - trackId else it + trackId }
            }
        }
    }

    fun playlistsChanged() {
        _playlistsVersion.update { it + 1 }
    }

    fun recordPlay(trackId: Long) {
        scope.launch { runCatching { api.recordPlay(trackId) } }
    }

    fun logout() {
        player.stop()
        session.logout()
        _favorites.value = emptySet()
        _me.value = null
    }
}
