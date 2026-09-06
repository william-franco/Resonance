package br.com.williamfranco.resonance.src.features.library.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.williamfranco.resonance.src.common.patterns.StatePattern
import br.com.williamfranco.resonance.src.data.local.toAlbums
import br.com.williamfranco.resonance.src.data.local.toArtists
import br.com.williamfranco.resonance.src.features.library.exceptions.LibraryException
import br.com.williamfranco.resonance.src.features.library.models.Album
import br.com.williamfranco.resonance.src.features.library.models.Artist
import br.com.williamfranco.resonance.src.features.library.models.LibraryTab
import br.com.williamfranco.resonance.src.features.library.models.Playlist
import br.com.williamfranco.resonance.src.features.library.models.Song
import br.com.williamfranco.resonance.src.features.library.repositories.LibraryRepository
import br.com.williamfranco.resonance.src.features.settings.repositories.SettingsRepository
import br.com.williamfranco.resonance.src.services.Constants
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class LibraryContent(
    val hasPermission: Boolean = false,
    val isScanning: Boolean = false,
    val tab: LibraryTab = LibraryTab.MUSICAS,
    val query: String = "",
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val favoritesCount: Int = 0,
) {
    val isEmptyLibrary: Boolean
        get() = !isScanning && songs.isEmpty() && query.isBlank()
}

typealias LibraryState = StatePattern<LibraryContent, LibraryException>

interface LibraryViewModel {
    val state: StateFlow<LibraryState>

    fun onPermissionResult(granted: Boolean)
    fun selectTab(tab: LibraryTab)
    fun updateQuery(query: String)
    fun toggleFavorite(songId: Long)
    fun createPlaylist(name: String)
    fun deletePlaylist(playlistId: Long)
    fun addToPlaylist(playlistId: Long, songIds: List<Long>)
    fun refresh()
}

private data class LibraryData(
    val songs: List<Song>,
    val playlists: List<Playlist>,
    val favoritesCount: Int,
) {
    val albums: List<Album> = songs.toAlbums()
    val artists: List<Artist> = songs.toArtists()
}

class LibraryViewModelImpl(
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel(), LibraryViewModel {

    private val tab = MutableStateFlow(LibraryTab.MUSICAS)
    private val query = MutableStateFlow("")
    private val permission = MutableStateFlow(libraryRepository.hasPermission())
    private val scanning = MutableStateFlow(false)

    private val _state = MutableStateFlow<LibraryState>(
        if (permission.value) StatePattern.Loading else StatePattern.Initial,
    )
    override val state: StateFlow<LibraryState> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        if (permission.value) refresh()
    }

    private fun observeLibrary() {
        observeJob?.cancel()
        val data = combine(
            libraryRepository.songs,
            libraryRepository.playlists,
            libraryRepository.favorites,
        ) { songs, playlists, favorites ->
            LibraryData(songs = songs, playlists = playlists, favoritesCount = favorites.size)
        }

        observeJob = combine(data, query, tab, permission, scanning) { library, search, currentTab, granted, isScanning ->
            LibraryContent(
                hasPermission = granted,
                isScanning = isScanning,
                tab = currentTab,
                query = search,
                songs = library.songs.filterByQuery(search),
                albums = library.albums.filter { it.matches(search) },
                artists = library.artists.filter { it.name.contains(search, ignoreCase = true) },
                playlists = library.playlists.filter { it.name.contains(search, ignoreCase = true) },
                favoritesCount = library.favoritesCount,
            )
        }
            .onEach { content -> _state.value = StatePattern.Success(content) }
            .launchIn(viewModelScope)
    }

    override fun onPermissionResult(granted: Boolean) {
        permission.value = granted
        if (granted) refresh()
    }

    override fun selectTab(tab: LibraryTab) {
        this.tab.value = tab
    }

    override fun updateQuery(query: String) {
        this.query.value = query
    }

    override fun toggleFavorite(songId: Long) {
        viewModelScope.launch { libraryRepository.toggleFavorite(songId) }
    }

    override fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { libraryRepository.createPlaylist(trimmed) }
    }

    override fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { libraryRepository.deletePlaylist(playlistId) }
    }

    override fun addToPlaylist(playlistId: Long, songIds: List<Long>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch { libraryRepository.addToPlaylist(playlistId, songIds) }
    }

    override fun refresh() {
        if (scanning.value) return
        viewModelScope.launch {
            val previousContent = (_state.value as? StatePattern.Success)?.data
            scanning.value = true
            if (previousContent == null) {
                _state.value = StatePattern.Loading
            } else {
                _state.value = StatePattern.Success(previousContent.copy(isScanning = true))
            }

            val settings = settingsRepository.settings.first()
            val minDuration = if (settings.ignoreShortTracks) Constants.SHORT_TRACK_THRESHOLD_MS else 0L
            val syncResult = libraryRepository.sync(minDuration)
            scanning.value = false

            syncResult.fold(
                onSuccess = {
                    if (observeJob == null) observeLibrary()
                },
                onError = { error ->
                    _state.value = StatePattern.Error(error)
                },
            )
        }
    }

    private fun List<Song>.filterByQuery(query: String): List<Song> {
        if (query.isBlank()) return this
        return filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
    }

    private fun Album.matches(query: String): Boolean =
        query.isBlank() || title.contains(query, ignoreCase = true) || artist.contains(query, ignoreCase = true)
}
