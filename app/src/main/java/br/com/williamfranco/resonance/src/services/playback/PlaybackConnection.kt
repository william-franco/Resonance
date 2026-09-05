package br.com.williamfranco.resonance.src.services.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import br.com.williamfranco.resonance.src.features.library.models.Song
import br.com.williamfranco.resonance.src.services.Constants
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Ponte entre a UI e o [PlaybackService]. Mantém um único [MediaController] e projeta
 * o estado do player em um [StateFlow] consumido pelas ViewModels.
 */
interface PlaybackConnection {
    val state: StateFlow<PlaybackState>

    fun playQueue(songs: List<Song>, startIndex: Int)
    fun shufflePlay(songs: List<Song>)
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun seekToQueueIndex(index: Int)
    fun toggleShuffle()
    fun cycleRepeat()
    fun addToQueue(songs: List<Song>)
    fun moveQueueItem(from: Int, to: Int)
    fun removeFromQueue(index: Int)
    fun release()
}

class PlaybackConnectionImpl(private val context: Context) : PlaybackConnection {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var positionJob: Job? = null

    /** Guarda o primeiro comando disparado antes de o controller ficar pronto. */
    private var pendingAction: (() -> Unit)? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncState()
    }

    init {
        connect()
    }

    private fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                runCatching { future.get() }.onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(listener)
                    syncState()
                    pendingAction?.invoke()
                    pendingAction = null
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    private inline fun withController(crossinline block: (MediaController) -> Unit) {
        val mediaController = controller
        if (mediaController == null) {
            pendingAction = { controller?.let { block(it) } }
            return
        }
        block(mediaController)
    }

    private fun syncState() {
        val mediaController = controller ?: return
        val queue = (0 until mediaController.mediaItemCount)
            .mapNotNull { mediaController.getMediaItemAt(it).toSongOrNull() }
        val index = mediaController.currentMediaItemIndex
        val current = queue.getOrNull(index)
        val reportedDuration = mediaController.duration

        _state.update {
            it.copy(
                currentSong = current,
                queue = queue,
                queueIndex = if (queue.isEmpty()) -1 else index,
                isPlaying = mediaController.isPlaying,
                positionMs = mediaController.currentPosition.coerceAtLeast(0L),
                durationMs = if (reportedDuration > 0L) reportedDuration else current?.durationMs ?: 0L,
                shuffleEnabled = mediaController.shuffleModeEnabled,
                repeatMode = mediaController.repeatMode.toRepeatMode(),
            )
        }

        trackPosition(mediaController.isPlaying)
    }

    private fun trackPosition(isPlaying: Boolean) {
        if (!isPlaying) {
            positionJob?.cancel()
            positionJob = null
            return
        }
        if (positionJob?.isActive == true) return

        positionJob = scope.launch {
            while (isActive) {
                delay(Constants.POSITION_TICK_MS)
                val mediaController = controller ?: break
                _state.update { it.copy(positionMs = mediaController.currentPosition.coerceAtLeast(0L)) }
            }
        }
    }

    override fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        withController { mediaController ->
            mediaController.shuffleModeEnabled = false
            mediaController.setMediaItems(
                songs.map { it.toMediaItem() },
                startIndex.coerceIn(0, songs.lastIndex),
                0L,
            )
            mediaController.prepare()
            mediaController.play()
        }
    }

    override fun shufflePlay(songs: List<Song>) {
        if (songs.isEmpty()) return
        withController { mediaController ->
            mediaController.setMediaItems(songs.map { it.toMediaItem() }, songs.indices.random(), 0L)
            mediaController.shuffleModeEnabled = true
            mediaController.prepare()
            mediaController.play()
        }
    }

    override fun togglePlayPause() = withController { mediaController ->
        if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
    }

    override fun next() = withController { it.seekToNextMediaItem() }

    override fun previous() = withController { mediaController ->
        // Padrão dos players: só volta de faixa no início; depois disso, reinicia a atual.
        if (mediaController.currentPosition > 3_000L) {
            mediaController.seekTo(0L)
        } else {
            mediaController.seekToPreviousMediaItem()
        }
    }

    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0L)) }

    override fun seekToQueueIndex(index: Int) = withController { mediaController ->
        if (index in 0 until mediaController.mediaItemCount) {
            mediaController.seekTo(index, 0L)
            mediaController.play()
        }
    }

    override fun toggleShuffle() = withController {
        it.shuffleModeEnabled = !it.shuffleModeEnabled
    }

    override fun cycleRepeat() = withController {
        it.repeatMode = it.repeatMode.toRepeatMode().next().toPlayerRepeatMode()
    }

    override fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        withController { mediaController ->
            mediaController.addMediaItems(songs.map { it.toMediaItem() })
            if (mediaController.playbackState == Player.STATE_IDLE) mediaController.prepare()
        }
    }

    override fun moveQueueItem(from: Int, to: Int) = withController { mediaController ->
        val count = mediaController.mediaItemCount
        if (from in 0 until count && to in 0 until count && from != to) {
            mediaController.moveMediaItem(from, to)
        }
    }

    override fun removeFromQueue(index: Int) = withController { mediaController ->
        if (index in 0 until mediaController.mediaItemCount) {
            mediaController.removeMediaItem(index)
        }
    }

    override fun release() {
        positionJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }
}
