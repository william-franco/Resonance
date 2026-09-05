package br.com.williamfranco.resonance.src.fakes

import br.com.williamfranco.resonance.src.features.library.models.Song

fun song(
    id: Long,
    title: String = "Faixa $id",
    artist: String = "Artista A",
    album: String = "Álbum A",
    albumId: Long = 1L,
    durationMs: Long = 210_000L,
    artworkUri: String? = "content://media/external/audio/albumart/$albumId",
    trackNumber: Int = id.toInt(),
    year: Int = 2024,
    isFavorite: Boolean = false,
): Song = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    uri = "content://media/external/audio/media/$id",
    artworkUri = artworkUri,
    trackNumber = trackNumber,
    year = year,
    dateAddedSeconds = 1_700_000_000L + id,
    isFavorite = isFavorite,
)
