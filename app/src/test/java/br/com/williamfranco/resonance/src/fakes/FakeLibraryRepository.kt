package br.com.williamfranco.resonance.src.fakes

import br.com.williamfranco.resonance.src.common.patterns.ResultPattern
import br.com.williamfranco.resonance.src.data.local.toAlbums
import br.com.williamfranco.resonance.src.data.local.toArtists
import br.com.williamfranco.resonance.src.features.library.exceptions.LibraryException
import br.com.williamfranco.resonance.src.features.library.models.Album
import br.com.williamfranco.resonance.src.features.library.models.Artist
import br.com.williamfranco.resonance.src.features.library.models.Playlist
import br.com.williamfranco.resonance.src.features.library.models.Song
import br.com.williamfranco.resonance.src.features.library.repositories.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeLibraryRepository(
    initialSongs: List<Song> = emptyList(),
    initialPlaylists: List<Playlist> = emptyList(),
    private var permissionGranted: Boolean = true,
    var syncFailure: LibraryException? = null,
) : LibraryRepository {

    private val _songs = MutableStateFlow(initialSongs)
    private val _playlists = MutableStateFlow(initialPlaylists)

    var syncCount: Int = 0
        private set
    var lastSyncMinDuration: Long? = null
        private set
    val toggledFavorites = mutableListOf<Long>()
    val createdPlaylists = mutableListOf<String>()
    val addedToPlaylist = mutableListOf<Pair<Long, List<Long>>>()
    val removedFromPlaylist = mutableListOf<Pair<Long, Long>>()

    override val songs: Flow<List<Song>> = _songs.asStateFlow()
    override val albums: Flow<List<Album>> = _songs.map { it.toAlbums() }
    override val artists: Flow<List<Artist>> = _songs.map { it.toArtists() }
    override val playlists: Flow<List<Playlist>> = _playlists.asStateFlow()
    override val favorites: Flow<List<Song>> = _songs.map { songs -> songs.filter { it.isFavorite } }

    override fun songsOfAlbum(albumId: Long): Flow<List<Song>> =
        _songs.map { songs -> songs.filter { it.albumId == albumId } }

    override fun songsOfArtist(artist: String): Flow<List<Song>> =
        _songs.map { songs -> songs.filter { it.artist == artist } }

    override fun songsOfPlaylist(playlistId: Long): Flow<List<Song>> = _songs.asStateFlow()

    override fun hasPermission(): Boolean = permissionGranted

    override fun requiredPermission(): String = "android.permission.READ_MEDIA_AUDIO"

    override suspend fun sync(minDurationMs: Long): ResultPattern<Int, LibraryException> {
        syncCount += 1
        lastSyncMinDuration = minDurationMs
        syncFailure?.let { failure -> return ResultPattern.Error(failure) }
        return ResultPattern.Success(_songs.value.size)
    }

    override suspend fun toggleFavorite(songId: Long) {
        toggledFavorites.add(songId)
        _songs.value = _songs.value.map {
            if (it.id == songId) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        createdPlaylists.add(name)
        val id = (_playlists.value.maxOfOrNull { it.id } ?: 0L) + 1L
        _playlists.value = _playlists.value + Playlist(id, name, 0, null)
        return id
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        _playlists.value = _playlists.value.filterNot { it.id == playlistId }
    }

    override suspend fun playlistName(playlistId: Long): String? =
        _playlists.value.firstOrNull { it.id == playlistId }?.name

    override suspend fun addToPlaylist(playlistId: Long, songIds: List<Long>) {
        addedToPlaylist.add(playlistId to songIds)
    }

    override suspend fun removeFromPlaylist(playlistId: Long, songId: Long) {
        removedFromPlaylist.add(playlistId to songId)
    }

    fun emitSongs(songs: List<Song>) {
        _songs.value = songs
    }

    fun grantPermission() {
        permissionGranted = true
    }
}
