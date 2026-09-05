package br.com.williamfranco.resonance.src.features.library.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.williamfranco.resonance.src.features.library.models.Song
import br.com.williamfranco.resonance.src.features.library.repositories.LibraryRepository
import br.com.williamfranco.resonance.src.routes.CollectionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollectionUiState(
    val isLoading: Boolean = true,
    val type: CollectionType = CollectionType.ALBUM,
    val title: String = "",
    val subtitle: String = "",
    val artworkUri: String? = null,
    val songs: List<Song> = emptyList(),
    val playlistId: Long? = null,
)

interface CollectionViewModel {
    val uiState: StateFlow<CollectionUiState>

    fun load(type: CollectionType, key: String)
    fun toggleFavorite(songId: Long)
    fun removeFromPlaylist(songId: Long)
}

class CollectionViewModelImpl(
    private val libraryRepository: LibraryRepository,
) : ViewModel(), CollectionViewModel {

    private val _uiState = MutableStateFlow(CollectionUiState())
    override val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private var songsJob: Job? = null

    override fun load(type: CollectionType, key: String) {
        if (_uiState.value.type == type && _uiState.value.keyMatches(key) && !_uiState.value.isLoading) return

        songsJob?.cancel()
        val playlistId = key.toLongOrNull().takeIf { type == CollectionType.PLAYLIST }

        _uiState.value = CollectionUiState(
            isLoading = true,
            type = type,
            title = defaultTitleFor(type, key),
            playlistId = playlistId,
        )

        if (type == CollectionType.PLAYLIST && playlistId != null) {
            viewModelScope.launch {
                val name = libraryRepository.playlistName(playlistId)
                if (name != null) _uiState.update { it.copy(title = name) }
            }
        }

        songsJob = songsFlowFor(type, key)
            .onEach { songs -> _uiState.update { it.withSongs(songs) } }
            .launchIn(viewModelScope)
    }

    override fun toggleFavorite(songId: Long) {
        viewModelScope.launch { libraryRepository.toggleFavorite(songId) }
    }

    override fun removeFromPlaylist(songId: Long) {
        val playlistId = _uiState.value.playlistId ?: return
        viewModelScope.launch { libraryRepository.removeFromPlaylist(playlistId, songId) }
    }

    private fun songsFlowFor(type: CollectionType, key: String): Flow<List<Song>> = when (type) {
        CollectionType.ALBUM -> libraryRepository.songsOfAlbum(key.toLongOrNull() ?: -1L)
        CollectionType.ARTISTA -> libraryRepository.songsOfArtist(key)
        CollectionType.PLAYLIST -> libraryRepository.songsOfPlaylist(key.toLongOrNull() ?: -1L)
        CollectionType.FAVORITAS -> libraryRepository.favorites
    }

    private fun defaultTitleFor(type: CollectionType, key: String): String = when (type) {
        CollectionType.ARTISTA -> key
        CollectionType.FAVORITAS -> "Favoritas"
        else -> ""
    }

    private fun CollectionUiState.keyMatches(key: String): Boolean = when (type) {
        CollectionType.PLAYLIST -> playlistId == key.toLongOrNull()
        CollectionType.ARTISTA -> title == key
        else -> false
    }

    private fun CollectionUiState.withSongs(songs: List<Song>): CollectionUiState {
        val reference = songs.firstOrNull()
        val resolvedTitle = when (type) {
            CollectionType.ALBUM -> reference?.album ?: title
            else -> title
        }
        val totalMinutes = songs.sumOf { it.durationMs } / 60_000
        return copy(
            isLoading = false,
            title = resolvedTitle.ifBlank { "Coleção" },
            subtitle = "${songs.size} faixas · $totalMinutes min",
            artworkUri = songs.firstNotNullOfOrNull { it.artworkUri },
            songs = songs,
        )
    }
}
