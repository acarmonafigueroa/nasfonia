package com.nasmusic.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiException(val code: Int, message: String) : IOException(message)

/** Talks to the Nasfonía server (see server/src/routes.js). */
class Api(private val session: Session) {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Quick checks of which address answers; a dead one must not hold the app for long.
    private val probe: OkHttpClient = http.newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS)
        .build()

    /** Called after the active address changes, so URLs already handed out (the queue) are rebuilt. */
    var onAddressChanged: () -> Unit = {}

    private val jsonType = "application/json".toMediaType()
    private val base get() = session.activeUrl.ifBlank { session.serverUrl }.trimEnd('/')
    private val token get() = session.token.value.orEmpty()
    private val switching = Mutex()

    fun coverUrl(albumId: Long?): String? =
        albumId?.let { "$base/api/cover/$it?token=${enc(token)}" }

    fun streamUrl(trackId: Long, raw: Boolean = false): String {
        val br = if (!raw && session.streamBitrate > 0) "&bitrate=${session.streamBitrate}" else ""
        return "$base/api/stream/$trackId?token=${enc(token)}$br"
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun call(method: String, path: String, body: String? = null, url: String? = null): String {
        if (url != null) return send(method, path, body, url)
        return try {
            send(method, path, body, base)
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            // Unreachable: maybe we left home (or came back). Try the other address once.
            if (pickReachable()) send(method, path, body, base) else throw e
        }
    }

    private suspend fun send(method: String, path: String, body: String?, url: String): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("${url.trimEnd('/')}/api$path")
                .header("Authorization", "Bearer $token")
                .method(method, body?.toRequestBody(jsonType) ?: if (method == "GET" || method == "DELETE") null else "".toRequestBody(jsonType))
                .build()
            http.newCall(req).execute().use { res ->
                val text = res.body?.string().orEmpty()
                if (res.code == 401 && path != "/login") session.logout()
                if (!res.isSuccessful) {
                    val msg = runCatching { json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.content }.getOrNull()
                    throw ApiException(res.code, msg ?: "Error ${res.code}")
                }
                text
            }
        }

    private suspend inline fun <reified T> get(path: String): T = json.decodeFromString(call("GET", path))

    // --- addresses ----------------------------------------------------------

    /** Does a Nasfonía server answer at [url]? */
    suspend fun reachable(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            probe.newCall(Request.Builder().url("${url.trimEnd('/')}/api/ping").build()).execute().use { res ->
                res.isSuccessful && json.decodeFromString<Ping>(res.body?.string().orEmpty()).app == "nasfonia"
            }
        }.getOrDefault(false)
    }

    /**
     * Makes the first address that answers the active one, home address first.
     * Returns true when it switched (and tells [onAddressChanged]).
     */
    suspend fun pickReachable(): Boolean = switching.withLock {
        val options = listOf(session.serverUrl, session.altUrl).filter { it.isNotBlank() }.distinct()
        if (options.size < 2) return@withLock false
        val ok = options.firstOrNull { reachable(it) } ?: return@withLock false
        if (ok == session.activeUrl) return@withLock false
        session.activeUrl = ok
        withContext(Dispatchers.Main) { onAddressChanged() }
        true
    }

    /**
     * What the user may type, turned into URLs to try in order:
     * "192.168.1.50" → http://192.168.1.50:8090 · "musica.example.com" → https://musica.example.com, then :8090 ·
     * anything with http(s):// is kept as typed (plus :8090 when it has no port).
     */
    fun candidates(input: String): List<String> {
        val s = input.trim().trimEnd('/')
        if (s.isEmpty()) return emptyList()
        val hasScheme = s.startsWith("http://", true) || s.startsWith("https://", true)
        val url = (if (hasScheme) s else "http://$s").toHttpUrlOrNull() ?: return emptyList()
        val hasPort = Regex("^(https?://)?[^/]+:\\d+", RegexOption.IGNORE_CASE).containsMatchIn(s)
        val path = url.encodedPath.trimEnd('/')
        val host = url.host
        val local = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$").matches(host) || '.' !in host ||
            host.endsWith(".local") || host.endsWith(".lan") || host.endsWith(".home") || host.contains(':')
        val h = if (host.contains(':')) "[$host]" else host
        return when {
            hasScheme && hasPort -> listOf(s)
            hasScheme -> listOf(s, "${url.scheme}://$h:8090$path")
            hasPort -> listOf("http://$s", "https://$s")
            local -> listOf("http://$h:8090$path", "http://$h$path")
            else -> listOf("https://$h$path", "http://$h:8090$path", "http://$h$path")
        }.distinct()
    }

    /** First candidate of [input] that answers, or null. */
    suspend fun resolve(input: String): String? = candidates(input).firstOrNull { reachable(it) }

    /**
     * Signs in. [address] is required; [altAddress] (the other way in, e.g. from outside home) is optional
     * and may be unreachable right now — it is then kept as the best guess.
     */
    suspend fun login(address: String, altAddress: String, username: String, password: String): LoginResponse {
        val primaryGuess = candidates(address).firstOrNull() ?: throw IOException("Escribe la dirección del servidor")
        val altGuess = candidates(altAddress).firstOrNull().orEmpty()
        val primary = resolve(address)
        val alt = if (altAddress.isBlank()) "" else resolve(altAddress) ?: altGuess
        val use = primary ?: alt.takeIf { it.isNotEmpty() && reachable(it) }
            ?: throw IOException("No se encuentra Nasfonía en esa dirección. Revisa que el contenedor esté en marcha y que el móvil llegue a esa red.")
        val ping: Ping = json.decodeFromString(send("GET", "/ping", null, use))
        val body = buildJsonObject {
            put("username", username.trim())
            put("password", password)
            put("device", android.os.Build.MODEL ?: "android")
        }.toString()
        val res: LoginResponse = json.decodeFromString(send("POST", "/login", body, use))
        session.save(primary ?: primaryGuess, alt, res.name.ifBlank { ping.name }, res.user?.username ?: username.trim(), res.token)
        session.activeUrl = use
        return res
    }

    /** Changes the saved addresses after checking at least one answers. */
    suspend fun updateAddresses(address: String, altAddress: String) {
        val primary = resolve(address)
        val alt = if (altAddress.isBlank()) "" else resolve(altAddress) ?: candidates(altAddress).firstOrNull().orEmpty()
        if (primary == null && (alt.isEmpty() || !reachable(alt))) throw IOException("Ninguna de las dos direcciones responde ahora mismo")
        session.setUrls(primary ?: candidates(address).firstOrNull().orEmpty(), alt)
        val use = primary ?: alt
        if (use != session.activeUrl) {
            session.activeUrl = use
            onAddressChanged()
        }
    }

    suspend fun me(): UserInfo = get("/me")

    // --- library ------------------------------------------------------------
    suspend fun home(): Home = get("/home")
    suspend fun albums(sort: String = "artist"): List<Album> = get("/albums?sort=$sort")
    suspend fun album(id: Long): Album = get("/albums/$id")
    suspend fun artists(): List<Artist> = get("/artists")
    suspend fun artist(id: Long): Artist = get("/artists/$id")
    suspend fun artistTracks(id: Long): List<Track> = get("/artists/$id/tracks")
    suspend fun genres(): List<Genre> = get("/genres")
    suspend fun genreTracks(name: String): List<Track> = get("/genres/${enc(name).replace("+", "%20")}/tracks")
    suspend fun search(q: String): SearchResult = get("/search?q=${enc(q)}")
    suspend fun randomTracks(limit: Int = 100): List<Track> = get("/tracks/random?limit=$limit")
    suspend fun radio(trackId: Long, limit: Int = 50): List<Track> = get("/tracks/$trackId/radio?limit=$limit")
    suspend fun lyrics(trackId: Long): LyricsResponse = get("/lyrics/$trackId")

    // --- favorites & plays --------------------------------------------------
    suspend fun favorites(): List<Track> = get("/favorites")
    suspend fun setFavorite(trackId: Long, on: Boolean) {
        call(if (on) "PUT" else "DELETE", "/favorites/$trackId")
    }
    suspend fun recordPlay(trackId: Long) {
        call("POST", "/plays/$trackId")
    }

    // --- playlists ----------------------------------------------------------
    suspend fun playlists(): List<Playlist> = get("/playlists")
    suspend fun playlist(id: Long): Playlist = get("/playlists/$id")

    suspend fun createPlaylist(name: String, trackIds: List<Long> = emptyList()): Playlist {
        val body = buildJsonObject {
            put("name", name)
            put("trackIds", JsonArray(trackIds.map { JsonPrimitive(it) }))
        }.toString()
        return json.decodeFromString(call("POST", "/playlists", body))
    }

    suspend fun renamePlaylist(id: Long, name: String) {
        call("PATCH", "/playlists/$id", buildJsonObject { put("name", name) }.toString())
    }

    suspend fun setPlaylistPublic(id: Long, isPublic: Boolean): Playlist {
        val body = buildJsonObject { put("isPublic", isPublic) }.toString()
        return json.decodeFromString(call("PATCH", "/playlists/$id", body))
    }

    suspend fun deletePlaylist(id: Long) {
        call("DELETE", "/playlists/$id")
    }

    suspend fun addToPlaylist(id: Long, trackIds: List<Long>): Playlist {
        val body = buildJsonObject { put("trackIds", JsonArray(trackIds.map { JsonPrimitive(it) })) }.toString()
        return json.decodeFromString(call("POST", "/playlists/$id/tracks", body))
    }

    suspend fun removeFromPlaylist(id: Long, position: Int) {
        call("DELETE", "/playlists/$id/tracks/$position")
    }

    suspend fun movePlaylistTrack(id: Long, fromPosition: Int, toIndex: Int): Playlist {
        val body = buildJsonObject {
            put("from", fromPosition)
            put("to", toIndex)
        }.toString()
        return json.decodeFromString(call("POST", "/playlists/$id/move", body))
    }

    // --- scan ---------------------------------------------------------------
    suspend fun startScan(full: Boolean = false): ScanStatus =
        json.decodeFromString(call("POST", if (full) "/scan?full=1" else "/scan"))

    suspend fun scanStatus(): ScanStatus = get("/scan")

    // --- password -----------------------------------------------------------
    suspend fun passwordInfo(): PasswordInfo = get("/password")

    /** Changes it and keeps this device signed in with the new token. */
    suspend fun changePassword(current: String, next: String) {
        val body = buildJsonObject {
            put("current", current)
            put("next", next)
            put("device", android.os.Build.MODEL ?: "android")
        }.toString()
        val res: LoginResponse = json.decodeFromString(call("POST", "/password", body))
        session.setToken(res.token)
    }

    // --- users (administrators) ---------------------------------------------
    suspend fun users(): List<AppUser> = get("/users")

    suspend fun createUser(username: String, password: String, isAdmin: Boolean) {
        val body = buildJsonObject {
            put("username", username.trim())
            put("password", password)
            put("isAdmin", isAdmin)
        }.toString()
        call("POST", "/users", body)
    }

    suspend fun setUserPassword(id: Long, password: String) {
        call("PATCH", "/users/$id", buildJsonObject { put("password", password) }.toString())
    }

    suspend fun setUserAdmin(id: Long, isAdmin: Boolean) {
        call("PATCH", "/users/$id", buildJsonObject { put("isAdmin", isAdmin) }.toString())
    }

    suspend fun deleteUser(id: Long) {
        call("DELETE", "/users/$id")
    }

    // --- music folders ------------------------------------------------------
    suspend fun folders(): FoldersInfo = get("/folders")

    suspend fun browseFolder(path: String?): FolderListing =
        get(if (path == null) "/folders/browse" else "/folders/browse?path=${enc(path)}")

    suspend fun setFolders(paths: List<String>): FoldersInfo {
        val body = buildJsonObject { put("dirs", JsonArray(paths.map { JsonPrimitive(it) })) }.toString()
        return json.decodeFromString(call("PUT", "/folders", body))
    }
}
