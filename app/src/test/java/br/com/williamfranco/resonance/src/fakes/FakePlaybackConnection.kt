package br.com.williamfranco.resonance.src.fakes

import br.com.williamfranco.resonance.src.features.library.models.Song
import br.com.williamfranco.resonance.src.services.playback.PlaybackConnection
import br.com.williamfranco.resonance.src.services.playback.PlaybackState
import br.com.williamfranco.resonance.src.services.playback.RepeatMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simula o comportamento do MediaController: guarda a fila e reage aos comandos
 * como o player real faria, sem precisar do serviço de reprodução.
 */
class FakePlaybackConnection : PlaybackConnection {

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    var releaseCount: Int = 0
        private set

    override fun playQueue(songs: List<Song>, startIndex: Int) {
        val index = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
        _state.value = _state.value.copy(
            queue = songs,
            queueIndex = if (songs.isEmpty()) -1 else index,
            currentSong = songs.getOrNull(index),
            durationMs = songs.getOrNull(index)?.durationMs ?: 0L,
            positionMs = 0L,
            isPlaying = songs.isNotEmpty(),
            shuffleEnabled = false,
        )
    }

    override fun shufflePlay(songs: List<Song>) {
        playQueue(songs, 0)
        _state.value = _state.value.copy(shuffleEnabled = true)
    }

    override fun togglePlayPause() {
        _state.value = _state.value.copy(isPlaying = !_state.value.isPlaying)
    }

    override fun next() {
        val current = _state.value
        val index = (current.queueIndex + 1).coerceAtMost(current.queue.lastIndex)
        _state.value = current.copy(
            queueIndex = index,
            currentSong = current.queue.getOrNull(index),
            positionMs = 0L,
        )
    }

    override fun previous() {
        val current = _state.value
        if (current.positionMs > 3_000L) {
            _state.value = current.copy(positionMs = 0L)
            return
        }
        val index = (current.queueIndex - 1).coerceAtLeast(0)
        _state.value = current.copy(
            queueIndex = index,
            currentSong = current.queue.getOrNull(index),
            positionMs = 0L,
        )
    }

    override fun seekTo(positionMs: Long) {
        _state.value = _state.value.copy(positionMs = positionMs.coerceAtLeast(0L))
    }

    override fun seekToQueueIndex(index: Int) {
        val current = _state.value
        if (index !in current.queue.indices) return
        _state.value = current.copy(
            queueIndex = index,
            currentSong = current.queue[index],
            positionMs = 0L,
            isPlaying = true,
        )
    }

    override fun toggleShuffle() {
        _state.value = _state.value.copy(shuffleEnabled = !_state.value.shuffleEnabled)
    }

    override fun cycleRepeat() {
        _state.value = _state.value.copy(repeatMode = _state.value.repeatMode.next())
    }

    override fun addToQueue(songs: List<Song>) {
        _state.value = _state.value.copy(queue = _state.value.queue + songs)
    }

    override fun moveQueueItem(from: Int, to: Int) {
        val queue = _state.value.queue.toMutableList()
        if (from !in queue.indices || to !in queue.indices || from == to) return
        queue.add(to, queue.removeAt(from))
        _state.value = _state.value.copy(queue = queue)
    }

    override fun removeFromQueue(index: Int) {
        val queue = _state.value.queue.toMutableList()
        if (index !in queue.indices) return
        queue.removeAt(index)
        _state.value = _state.value.copy(queue = queue)
    }

    override fun release() {
        releaseCount += 1
    }

    fun emitPosition(positionMs: Long) {
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    fun emitRepeatMode(repeatMode: RepeatMode) {
        _state.value = _state.value.copy(repeatMode = repeatMode)
    }
}
