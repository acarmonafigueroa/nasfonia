package com.nasmusic.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import okhttp3.Request
import java.io.File

/**
 * Manages offline audio & cover downloads so songs, albums and playlists
 * can be played without network or mobile data usage.
 */
class OfflineStore(context: Context, private val api: Api) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val rootDir = File(context.filesDir, "offline").apply { mkdirs() }
    private val tracksDir = File(rootDir, "tracks").apply { mkdirs() }
    private val coversDir = File(rootDir, "covers").apply { mkdirs() }
    private val indexFile = File(rootDir, "index.json")
    private val writeLock = Mutex()

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val downloadedTracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _downloadedIds = MutableStateFlow<Set<Long>>(emptySet())
    val downloadedIds: StateFlow<Set<Long>> = _downloadedIds.asStateFlow()

    private val _downloadingIds = MutableStateFlow<Set<Long>>(emptySet())
    val downloadingIds: StateFlow<Set<Long>> = _downloadingIds.asStateFlow()

    init {
        scope.launch { loadIndex() }
    }

    private suspend fun loadIndex() = writeLock.withLock {
        val list = runCatching {
            if (indexFile.exists()) api.json.decodeFromString<List<Track>>(indexFile.readText()) else emptyList()
        }.getOrDefault(emptyList()).filter { audioFile(it.id).exists() }
        _tracks.value = list
        _downloadedIds.value = list.map { it.id }.toSet()
    }

    private suspend fun saveIndex(list: List<Track>) = writeLock.withLock {
        _tracks.value = list
        _downloadedIds.value = list.map { it.id }.toSet()
        runCatching { indexFile.writeText(api.json.encodeToString(list)) }
    }

    private fun audioFile(trackId: Long): File = File(tracksDir, "$trackId.audio")
    private fun coverFile(albumId: Long): File = File(coversDir, "$albumId.img")

    fun localTrackFile(trackId: Long): File? =
        audioFile(trackId).takeIf { it.exists() && it.length() > 0 }

    fun localCoverFile(albumId: Long?): File? =
        albumId?.let { coverFile(it).takeIf { f -> f.exists() && f.length() > 0 } }

    fun download(tracks: List<Track>, onComplete: ((Int) -> Unit)? = null) {
        val pending = tracks.filter { it.id !in _downloadedIds.value && it.id !in _downloadingIds.value }
        if (pending.isEmpty()) {
            onComplete?.invoke(0)
            return
        }
        _downloadingIds.update { it + pending.map { t -> t.id } }
        scope.launch {
            var count = 0
            for (t in pending) {
                val ok = downloadSingle(t)
                _downloadingIds.update { it - t.id }
                if (ok) {
                    count++
                    val current = _tracks.value.filterNot { it.id == t.id } + t
                    saveIndex(current)
                }
            }
            if (onComplete != null) {
                withContext(Dispatchers.Main) { onComplete(count) }
            }
        }
    }

    private fun downloadSingle(t: Track): Boolean = runCatching {
        val target = audioFile(t.id)
        if (!target.exists() || target.length() == 0L) {
            val tmp = File(tracksDir, "${t.id}.tmp")
            val req = Request.Builder().url(api.streamUrl(t.id)).build()
            api.http.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return false
                val body = res.body ?: return false
                tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
            }
            if (tmp.length() == 0L) {
                tmp.delete()
                return false
            }
            tmp.renameTo(target)
        }
        if (t.hasCover) {
            val cFile = coverFile(t.albumId)
            if (!cFile.exists() || cFile.length() == 0L) {
                api.coverUrl(t.albumId)?.let { url ->
                    val tmpC = File(coversDir, "${t.albumId}.tmp")
                    runCatching {
                        api.http.newCall(Request.Builder().url(url).build()).execute().use { res ->
                            if (res.isSuccessful) {
                                res.body?.byteStream()?.use { inp -> tmpC.outputStream().use { out -> inp.copyTo(out) } }
                                if (tmpC.length() > 0L) tmpC.renameTo(cFile) else tmpC.delete()
                            }
                        }
                    }
                }
            }
        }
        true
    }.getOrDefault(false)

    fun remove(trackId: Long) {
        scope.launch {
            audioFile(trackId).delete()
            val updated = _tracks.value.filterNot { it.id == trackId }
            saveIndex(updated)
        }
    }

    fun removeAll(tracks: List<Track>) {
        val ids = tracks.map { it.id }.toSet()
        scope.launch {
            ids.forEach { audioFile(it).delete() }
            val updated = _tracks.value.filterNot { it.id in ids }
            saveIndex(updated)
        }
    }

    fun clearAll() {
        scope.launch {
            tracksDir.listFiles()?.forEach { it.delete() }
            coversDir.listFiles()?.forEach { it.delete() }
            saveIndex(emptyList())
        }
    }
}
