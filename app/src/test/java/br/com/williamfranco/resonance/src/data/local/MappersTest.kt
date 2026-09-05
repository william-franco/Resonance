package br.com.williamfranco.resonance.src.data.local

import br.com.williamfranco.resonance.src.fakes.song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    @Test
    fun `converte entidade em modelo preservando o favorito da consulta`() {
        val entity = SongEntity(
            id = 7L,
            title = "Aurora",
            artist = "Nébula",
            album = "Primeiro Sinal",
            albumId = 3L,
            durationMs = 245_000L,
            uri = "content://media/external/audio/media/7",
            artworkUri = "content://media/external/audio/albumart/3",
            trackNumber = 4,
            year = 2023,
            dateAddedSeconds = 1_700_000_000L,
        )

        val model = SongWithFavorite(song = entity, isFavorite = true).toModel()

        assertEquals(7L, model.id)
        assertEquals("Aurora", model.title)
        assertEquals("Nébula", model.artist)
        assertEquals(3L, model.albumId)
        assertEquals(245_000L, model.durationMs)
        assertTrue(model.isFavorite)
    }

    @Test
    fun `agrupa faixas em albuns ordenados por titulo`() {
        val songs = listOf(
            song(id = 1L, album = "Zenite", albumId = 2L),
            song(id = 2L, album = "Alvorada", albumId = 1L),
            song(id = 3L, album = "Alvorada", albumId = 1L),
        )

        val albums = songs.toAlbums()

        assertEquals(listOf("Alvorada", "Zenite"), albums.map { it.title })
        assertEquals(2, albums.first().songCount)
        assertEquals(1, albums.last().songCount)
    }

    @Test
    fun `marca album com artistas diferentes como coletanea`() {
        val songs = listOf(
            song(id = 1L, album = "Coletânea", albumId = 9L, artist = "Artista A"),
            song(id = 2L, album = "Coletânea", albumId = 9L, artist = "Artista B"),
        )

        val album = songs.toAlbums().single()

        assertEquals("Vários artistas", album.artist)
    }

    @Test
    fun `usa a primeira capa disponivel do album`() {
        val songs = listOf(
            song(id = 1L, albumId = 5L, artworkUri = null),
            song(id = 2L, albumId = 5L, artworkUri = "content://capa/5"),
        )

        assertEquals("content://capa/5", songs.toAlbums().single().artworkUri)
    }

    @Test
    fun `agrupa faixas em artistas contando albuns distintos`() {
        val songs = listOf(
            song(id = 1L, artist = "Nébula", albumId = 1L),
            song(id = 2L, artist = "Nébula", albumId = 2L),
            song(id = 3L, artist = "Nébula", albumId = 2L),
            song(id = 4L, artist = "Ares", albumId = 3L),
        )

        val artists = songs.toArtists()

        assertEquals(listOf("Ares", "Nébula"), artists.map { it.name })
        val nebula = artists.last()
        assertEquals(3, nebula.songCount)
        assertEquals(2, nebula.albumCount)
    }

    @Test
    fun `converte resumo de playlist em modelo`() {
        val summary = PlaylistSummary(id = 4L, name = "Foco", songCount = 12, artworkUri = null)

        val playlist = summary.toModel()

        assertEquals(4L, playlist.id)
        assertEquals("Foco", playlist.name)
        assertEquals(12, playlist.songCount)
        assertNull(playlist.artworkUri)
    }
}
