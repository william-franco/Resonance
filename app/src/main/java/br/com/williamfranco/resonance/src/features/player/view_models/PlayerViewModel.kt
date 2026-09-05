package br.com.williamfranco.resonance.src.features.player.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.williamfranco.resonance.src.features.library.models.Song
import br.com.williamfranco.resonance.src.features.library.repositories.LibraryRepository
import br.com.williamfranco.resonance.src.services.playback.PlaybackConnection
import br.com.williamfranco.resonance.src.services.playback.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface PlayerViewModel {
    val state: StateFlow<PlaybackState>
    val isExpanded: StateFlow<Boolean>

    fun expand()
    fun collapse()
    fun play(songs: List<Song>, startIndex: Int)
    fun shuffle(songs: List<Song>)
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun toggleShuffle()
    fun cycleRepeat()
    fun toggleFavorite()
    fun seekToQueueIndex(index: Int)
    fun removeFromQueue(index: Int)
    fun moveQueueItem(from: Int, to: Int)
}

class PlayerViewModelImpl(
    private val playbackConnection: PlaybackConnection,
    private val libraryRepository: LibraryRepository,
) : ViewModel(), PlayerViewModel {

    private val _isExpanded = MutableStateFlow(false)
    override val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    /**
     * A fila vive no `MediaController` e não sabe de favoritos, que mudam pelo banco.
     * O cruzamento aqui mantém o coração da UI sempre coerente com a biblioteca.
     */
    override val state: StateFlow<PlaybackState> = combine(
        playbackConnection.state,
        libraryRepository.favorites,
    ) { playback, favorites ->
        val favoriteIds = favorites.mapTo(HashSet()) { it.id }
        playback.copy(
            currentSong = playback.currentSong?.let { it.copy(isFavorite = it.id in favoriteIds) },
            queue = playback.queue.map { it.copy(isFavorite = it.id in favoriteIds) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackState())

    override fun expand() {
        if (state.value.currentSong != null) _isExpanded.value = true
    }

    override fun collapse() {
        _isExpanded.value = false
    }

    override fun play(songs: List<Song>, startIndex: Int) = playbackConnection.playQueue(songs, startIndex)

    override fun shuffle(songs: List<Song>) = playbackConnection.shufflePlay(songs)

    override fun togglePlayPause() = playbackConnection.togglePlayPause()

    override fun next() = playbackConnection.next()

    override fun previous() = playbackConnection.previous()

    override fun seekTo(positionMs: Long) = playbackConnection.seekTo(positionMs)

    override fun toggleShuffle() = playbackConnection.toggleShuffle()

    override fun cycleRepeat() = playbackConnection.cycleRepeat()

    override fun toggleFavorite() {
        val songId = state.value.currentSong?.id ?: return
        viewModelScope.launch { libraryRepository.toggleFavorite(songId) }
    }

    override fun seekToQueueIndex(index: Int) = playbackConnection.seekToQueueIndex(index)

    override fun removeFromQueue(index: Int) = playbackConnection.removeFromQueue(index)

    override fun moveQueueItem(from: Int, to: Int) = playbackConnection.moveQueueItem(from, to)
}
