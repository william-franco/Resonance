package br.com.williamfranco.resonance.src.features.library.repositories

import br.com.williamfranco.resonance.src.common.patterns.ResultPattern
import br.com.williamfranco.resonance.src.data.local.PlaylistDao
import br.com.williamfranco.resonance.src.features.library.exceptions.LibraryException
import br.com.williamfranco.resonance.src.data.local.PlaylistEntity
import br.com.williamfranco.resonance.src.data.local.SongDao
import br.com.williamfranco.resonance.src.data.local.toAlbums
import br.com.williamfranco.resonance.src.data.local.toArtists
import br.com.williamfranco.resonance.src.data.local.toModel
import br.com.williamfranco.resonance.src.data.local.toModels
import br.com.williamfranco.resonance.src.features.library.models.Album
import br.com.williamfranco.resonance.src.features.library.models.Artist
import br.com.williamfranco.resonance.src.features.library.models.Playlist
import br.com.williamfranco.resonance.src.features.library.models.Song
import br.com.williamfranco.resonance.src.services.library.MediaStoreScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface LibraryRepository {
    val songs: Flow<List<Song>>
    val albums: Flow<List<Album>>
    val artists: Flow<List<Artist>>
    val playlists: Flow<List<Playlist>>
    val favorites: Flow<List<Song>>

    fun songsOfAlbum(albumId: Long): Flow<List<Song>>
    fun songsOfArtist(artist: String): Flow<List<Song>>
    fun songsOfPlaylist(playlistId: Long): Flow<List<Song>>

    fun hasPermission(): Boolean
    fun requiredPermission(): String

    suspend fun sync(minDurationMs: Long): ResultPattern<Int, LibraryException>
    suspend fun toggleFavorite(songId: Long)
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun playlistName(playlistId: Long): String?
    suspend fun addToPlaylist(playlistId: Long, songIds: List<Long>)
    suspend fun removeFromPlaylist(playlistId: Long, songId: Long)
}

class LibraryRepositoryImpl(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val scanner: MediaStoreScanner,
) : LibraryRepository {

    override val songs: Flow<List<Song>> = songDao.observeAll().map { it.toModels() }

    override val albums: Flow<List<Album>> = songs.map { it.toAlbums() }

    override val artists: Flow<List<Artist>> = songs.map { it.toArtists() }

    override val playlists: Flow<List<Playlist>> =
        playlistDao.observeAll().map { summaries -> summaries.map { it.toModel() } }

    override val favorites: Flow<List<Song>> = songDao.observeFavorites().map { it.toModels() }

    override fun songsOfAlbum(albumId: Long): Flow<List<Song>> =
        songDao.observeByAlbum(albumId).map { it.toModels() }

    override fun songsOfArtist(artist: String): Flow<List<Song>> =
        songDao.observeByArtist(artist).map { it.toModels() }

    override fun songsOfPlaylist(playlistId: Long): Flow<List<Song>> =
        playlistDao.observeSongs(playlistId).map { it.toModels() }

    override fun hasPermission(): Boolean = scanner.hasPermission()

    override fun requiredPermission(): String = scanner.requiredPermission()

    override suspend fun sync(minDurationMs: Long): ResultPattern<Int, LibraryException> {
        if (!hasPermission()) {
            return ResultPattern.Error(LibraryException("Permissão de áudio não concedida."))
        }

        return try {
            val scanned = scanner.scan(minDurationMs)
            songDao.sync(scanned)
            ResultPattern.Success(scanned.size)
        } catch (error: Exception) {
            ResultPattern.Error(LibraryException("Erro ao indexar biblioteca: $error"))
        }
    }

    override suspend fun toggleFavorite(songId: Long) = songDao.toggleFavorite(songId)

    override suspend fun createPlaylist(name: String): Long =
        playlistDao.create(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))

    override suspend fun deletePlaylist(playlistId: Long) = playlistDao.delete(playlistId)

    override suspend fun playlistName(playlistId: Long): String? = playlistDao.nameOf(playlistId)

    override suspend fun addToPlaylist(playlistId: Long, songIds: List<Long>) =
        playlistDao.addSongs(playlistId, songIds)

    override suspend fun removeFromPlaylist(playlistId: Long, songId: Long) =
        playlistDao.removeSong(playlistId, songId)
}
