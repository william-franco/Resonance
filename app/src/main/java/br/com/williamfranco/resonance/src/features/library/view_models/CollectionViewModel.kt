package br.com.williamfranco.resonance.src.features.library.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.williamfranco.resonance.src.common.patterns.StatePattern
import br.com.williamfranco.resonance.src.features.library.exceptions.LibraryException
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
import kotlinx.coroutines.launch

data class CollectionContent(
    val type: CollectionType = CollectionType.ALBUM,
    val title: String = "",
    val subtitle: String = "",
    val artworkUri: String? = null,
    val songs: List<Song> = emptyList(),
    val playlistId: Long? = null,
)

typealias CollectionState = StatePattern<CollectionContent, LibraryException>

interface CollectionViewModel {
    val state: StateFlow<CollectionState>

    fun load(type: CollectionType, key: String)
    fun toggleFavorite(songId: Long)
    fun removeFromPlaylist(songId: Long)
}

class CollectionViewModelImpl(
    private val libraryRepository: LibraryRepository,
) : ViewModel(), CollectionViewModel {

    private val _state = MutableStateFlow<CollectionState>(StatePattern.Initial)
    override val state: StateFlow<CollectionState> = _state.asStateFlow()

    private var songsJob: Job? = null
    private var loadedType: CollectionType? = null
    private var loadedKey: String? = null

    override fun load(type: CollectionType, key: String) {
        if (loadedType == type && loadedKey == key && _state.value is StatePattern.Success) return

        songsJob?.cancel()
        loadedType = type
        loadedKey = key

        val playlistId = key.toLongOrNull().takeIf { type == CollectionType.PLAYLIST }

        _state.value = StatePattern.Loading

        if (type == CollectionType.PLAYLIST && playlistId != null) {
            viewModelScope.launch {
                val name = libraryRepository.playlistName(playlistId)
                if (name == null) {
                    _state.value = StatePattern.Error(LibraryException("Playlist não encontrada."))
                    return@launch
                }
                startObserving(type, key, name, playlistId)
            }
            return
        }

        startObserving(
            type = type,
            key = key,
            title = defaultTitleFor(type, key),
            playlistId = playlistId,
        )
    }

    override fun toggleFavorite(songId: Long) {
        viewModelScope.launch { libraryRepository.toggleFavorite(songId) }
    }

    override fun removeFromPlaylist(songId: Long) {
        val playlistId = (_state.value as? StatePattern.Success)?.data?.playlistId ?: return
        viewModelScope.launch { libraryRepository.removeFromPlaylist(playlistId, songId) }
    }

    private fun startObserving(
        type: CollectionType,
        key: String,
        title: String,
        playlistId: Long?,
    ) {
        songsJob = songsFlowFor(type, key)
            .onEach { songs ->
                _state.value = StatePattern.Success(
                    withSongs(
                        content = CollectionContent(
                            type = type,
                            title = title,
                            playlistId = playlistId,
                        ),
                        songs = songs,
                    ),
                )
            }
            .launchIn(viewModelScope)
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

    private fun withSongs(content: CollectionContent, songs: List<Song>): CollectionContent {
        val reference = songs.firstOrNull()
        val resolvedTitle = when (content.type) {
            CollectionType.ALBUM -> reference?.album ?: content.title
            else -> content.title
        }
        val totalMinutes = songs.sumOf { it.durationMs } / 60_000
        return content.copy(
            title = resolvedTitle.ifBlank { "Coleção" },
            subtitle = "${songs.size} faixas · $totalMinutes min",
            artworkUri = songs.firstNotNullOfOrNull { it.artworkUri },
            songs = songs,
        )
    }
}
