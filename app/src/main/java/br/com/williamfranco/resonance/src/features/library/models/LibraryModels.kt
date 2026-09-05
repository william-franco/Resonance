package br.com.williamfranco.resonance.src.features.library.models

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String?,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val dateAddedSeconds: Long = 0L,
    val isFavorite: Boolean = false,
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val songCount: Int,
    val year: Int,
)

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val artworkUri: String?,
)

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val artworkUri: String?,
)

enum class LibraryTab(val label: String) {
    MUSICAS("Músicas"),
    ALBUNS("Álbuns"),
    ARTISTAS("Artistas"),
    PLAYLISTS("Playlists"),
}

fun Long.toClock(): String {
    val totalSeconds = (this / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
