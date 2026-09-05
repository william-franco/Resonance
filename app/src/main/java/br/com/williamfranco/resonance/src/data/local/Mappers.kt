package br.com.williamfranco.resonance.src.data.local

import br.com.williamfranco.resonance.src.features.library.models.Album
import br.com.williamfranco.resonance.src.features.library.models.Artist
import br.com.williamfranco.resonance.src.features.library.models.Playlist
import br.com.williamfranco.resonance.src.features.library.models.Song

fun SongWithFavorite.toModel(): Song = Song(
    id = song.id,
    title = song.title,
    artist = song.artist,
    album = song.album,
    albumId = song.albumId,
    durationMs = song.durationMs,
    uri = song.uri,
    artworkUri = song.artworkUri,
    trackNumber = song.trackNumber,
    year = song.year,
    dateAddedSeconds = song.dateAddedSeconds,
    isFavorite = isFavorite,
)

fun List<SongWithFavorite>.toModels(): List<Song> = map { it.toModel() }

fun PlaylistSummary.toModel(): Playlist = Playlist(
    id = id,
    name = name,
    songCount = songCount,
    artworkUri = artworkUri,
)

/**
 * Álbuns e artistas não têm tabela própria: são agregações da lista de faixas,
 * que já vem ordenada e cabe em memória mesmo em bibliotecas grandes.
 */
fun List<Song>.toAlbums(): List<Album> = groupBy { it.albumId }
    .map { (albumId, songs) ->
        val reference = songs.first()
        Album(
            id = albumId,
            title = reference.album,
            artist = songs.map { it.artist }.distinct().singleOrNull() ?: "Vários artistas",
            artworkUri = songs.firstNotNullOfOrNull { it.artworkUri },
            songCount = songs.size,
            year = songs.maxOf { it.year },
        )
    }
    .sortedBy { it.title.lowercase() }

fun List<Song>.toArtists(): List<Artist> = groupBy { it.artist }
    .map { (name, songs) ->
        Artist(
            name = name,
            songCount = songs.size,
            albumCount = songs.map { it.albumId }.distinct().size,
            artworkUri = songs.firstNotNullOfOrNull { it.artworkUri },
        )
    }
    .sortedBy { it.name.lowercase() }
