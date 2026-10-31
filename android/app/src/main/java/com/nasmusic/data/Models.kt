package com.nasmusic.data

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: Long,
    val title: String,
    val artistId: Long,
    val artist: String,
    val albumId: Long,
    val album: String,
    val trackNo: Int? = null,
    val discNo: Int? = null,
    val duration: Double? = null,
    val genre: String? = null,
    val year: Int? = null,
    val format: String? = null,
    val bitrate: Int? = null,
    val hasCover: Boolean = false,
    val favorite: Boolean = false,
    /** Only set inside a playlist: the entry's slot, used to remove it. */
    val position: Int? = null,
)

@Serializable
data class Album(
    val id: Long,
    val title: String,
    val artistId: Long,
    val artist: String,
    val year: Int? = null,
    val hasCover: Boolean = false,
    val trackCount: Int = 0,
    val duration: Double = 0.0,
    val genre: String? = null,
    val tracks: List<Track> = emptyList(),
)

@Serializable
data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int = 0,
    val trackCount: Int = 0,
    val coverAlbumId: Long? = null,
    val albums: List<Album> = emptyList(),
    val appearsOn: List<Album> = emptyList(),
    val topTracks: List<Track> = emptyList(),
)

@Serializable
data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int = 0,
    val duration: Double = 0.0,
    val coverAlbumId: Long? = null,
    val tracks: List<Track> = emptyList(),
)

@Serializable
data class Genre(val name: String, val trackCount: Int = 0, val coverAlbumId: Long? = null)

@Serializable
data class Stats(val tracks: Int = 0, val albums: Int = 0, val artists: Int = 0, val duration: Double = 0.0)

@Serializable
data class Home(
    val recentAlbums: List<Album> = emptyList(),
    val recentTracks: List<Track> = emptyList(),
    val mostPlayed: List<Track> = emptyList(),
    val newAlbums: List<Album> = emptyList(),
    val randomAlbums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val stats: Stats = Stats(),
)

@Serializable
data class SearchResult(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)

@Serializable
data class ScanStatus(
    val running: Boolean = false,
    val added: Int = 0,
    val removed: Int = 0,
    val error: String? = null,
)

@Serializable
data class MusicFolder(val path: String, val name: String, val exists: Boolean = true)

@Serializable
data class FoldersInfo(val dirs: List<MusicFolder> = emptyList(), val browseRoot: String = "", val canBrowse: Boolean = false)

@Serializable
data class FolderEntry(val name: String, val path: String)

@Serializable
data class FolderListing(
    val path: String,
    val name: String,
    val parent: String? = null,
    val folders: List<FolderEntry> = emptyList(),
    val audioFiles: Int = 0,
)

@Serializable
data class Ping(val name: String = "", val version: String = "", val app: String = "")

@Serializable
data class LoginResponse(val token: String, val name: String = "")
