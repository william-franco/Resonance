package br.com.williamfranco.resonance.src.services.playback

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import br.com.williamfranco.resonance.src.features.library.models.Song

private const val EXTRA_ID = "resonance:id"
private const val EXTRA_ALBUM_ID = "resonance:albumId"
private const val EXTRA_DURATION = "resonance:durationMs"
private const val EXTRA_URI = "resonance:uri"
private const val EXTRA_ARTWORK = "resonance:artworkUri"
private const val EXTRA_TRACK = "resonance:trackNumber"
private const val EXTRA_YEAR = "resonance:year"
private const val EXTRA_DATE_ADDED = "resonance:dateAddedSeconds"
private const val EXTRA_FAVORITE = "resonance:isFavorite"

/**
 * Os campos do modelo viajam nos extras porque a fila é lida de volta do
 * `MediaController`, que só conhece `MediaItem`s.
 */
fun Song.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putLong(EXTRA_ID, id)
        putLong(EXTRA_ALBUM_ID, albumId)
        putLong(EXTRA_DURATION, durationMs)
        putString(EXTRA_URI, uri)
        putString(EXTRA_ARTWORK, artworkUri)
        putInt(EXTRA_TRACK, trackNumber)
        putInt(EXTRA_YEAR, year)
        putLong(EXTRA_DATE_ADDED, dateAddedSeconds)
        putBoolean(EXTRA_FAVORITE, isFavorite)
    }

    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setAlbumArtist(artist)
        .setTrackNumber(trackNumber)
        .setArtworkUri(artworkUri?.toUri())
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .setExtras(extras)
        .build()

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}

fun MediaItem.toSongOrNull(): Song? {
    val extras = mediaMetadata.extras ?: return null
    val id = extras.getLong(EXTRA_ID, -1L).takeIf { it >= 0L } ?: return null
    val uri = extras.getString(EXTRA_URI) ?: return null

    return Song(
        id = id,
        title = mediaMetadata.title?.toString() ?: "Faixa desconhecida",
        artist = mediaMetadata.artist?.toString() ?: "Artista desconhecido",
        album = mediaMetadata.albumTitle?.toString() ?: "Álbum desconhecido",
        albumId = extras.getLong(EXTRA_ALBUM_ID),
        durationMs = extras.getLong(EXTRA_DURATION),
        uri = uri,
        artworkUri = extras.getString(EXTRA_ARTWORK),
        trackNumber = extras.getInt(EXTRA_TRACK),
        year = extras.getInt(EXTRA_YEAR),
        dateAddedSeconds = extras.getLong(EXTRA_DATE_ADDED),
        isFavorite = extras.getBoolean(EXTRA_FAVORITE),
    )
}

fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.UMA
    Player.REPEAT_MODE_ALL -> RepeatMode.TODAS
    else -> RepeatMode.DESLIGADO
}

fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
    RepeatMode.UMA -> Player.REPEAT_MODE_ONE
    RepeatMode.TODAS -> Player.REPEAT_MODE_ALL
    RepeatMode.DESLIGADO -> Player.REPEAT_MODE_OFF
}
