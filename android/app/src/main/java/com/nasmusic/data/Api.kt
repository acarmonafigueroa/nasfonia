package com.nasmusic.data

import kotlinx.coroutines.Dispatchers
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

    private val jsonType = "application/json".toMediaType()
    private val base get() = session.serverUrl.trimEnd('/')
    private val token get() = session.token.value.orEmpty()

    fun coverUrl(albumId: Long?): String? =
        albumId?.let { "$base/api/cover/$it?token=${enc(token)}" }

    fun streamUrl(trackId: Long): String = "$base/api/stream/$trackId?token=${enc(token)}"

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun call(method: String, path: String, body: String? = null, url: String = base): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("$url/api$path")
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

    // --- session ------------------------------------------------------------

    /** Normalises what the user typed ("192.168.1.10" → "http://192.168.1.10:8090"). */
    fun normalizeUrl(input: String): String? {
        var s = input.trim().trimEnd('/')
        if (s.isEmpty()) return null
        if (!s.startsWith("http://") && !s.startsWith("https://")) s = "http://$s"
        val url = s.toHttpUrlOrNull() ?: return null
        val hasPort = Regex("^https?://[^/]+:\\d+").containsMatchIn(s)
        return if (hasPort || url.scheme == "https") s else "${url.scheme}://${url.host}:8090${url.encodedPath.trimEnd('/')}"
    }

    suspend fun login(serverUrl: String, password: String): LoginResponse {
        val ping: Ping = json.decodeFromString(call("GET", "/ping", url = serverUrl))
        if (ping.app != "nasfonia") throw IOException("Ese servidor no es de Nasfonía")
        val body = buildJsonObject {
            put("password", password)
            put("device", android.os.Build.MODEL ?: "android")
        }.toString()
        val res: LoginResponse = json.decodeFromString(call("POST", "/login", body, url = serverUrl))
        session.save(serverUrl, res.name.ifBlank { ping.name }, res.token)
        return res
    }

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

    // --- scan ---------------------------------------------------------------
    suspend fun startScan(full: Boolean = false): ScanStatus =
        json.decodeFromString(call("POST", if (full) "/scan?full=1" else "/scan"))

    suspend fun scanStatus(): ScanStatus = get("/scan")

    // --- password -----------------------------------------------------------
    suspend fun canChangePassword(): Boolean =
        json.parseToJsonElement(call("GET", "/password")).jsonObject["changeable"]?.jsonPrimitive?.content == "true"

    /** Changes it and keeps this device signed in with the new token. */
    suspend fun changePassword(current: String, next: String) {
        val body = buildJsonObject {
            put("current", current)
            put("next", next)
            put("device", android.os.Build.MODEL ?: "android")
        }.toString()
        val res: LoginResponse = json.decodeFromString(call("POST", "/password", body))
        session.save(session.serverUrl, session.serverName, res.token)
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
