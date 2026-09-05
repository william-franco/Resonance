package br.com.williamfranco.resonance.src.services.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import br.com.williamfranco.resonance.MainActivity
import br.com.williamfranco.resonance.src.features.settings.repositories.SettingsRepository
import br.com.williamfranco.resonance.src.services.Constants
import br.com.williamfranco.resonance.src.widgets.PlayerWidgetUpdater
import br.com.williamfranco.resonance.src.widgets.WidgetSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Serviço de reprodução do Resonance.
 *
 * O ExoPlayer é afinado para arquivos locais: buffers curtos, prioridade para tempo
 * em vez de tamanho e leitura de metadados desligada, já que os dados da faixa vêm
 * do banco. A fila inteira é entregue ao player, então a próxima faixa já está
 * pré-carregada quando a atual termina.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService(), KoinComponent {

    private val settingsRepository: SettingsRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaSession: MediaSession? = null
    private var crossfadeController: CrossfadeController? = null
    private var widgetTickerJob: Job? = null

    private val widgetListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (
                events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_MEDIA_METADATA_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                )
            ) {
                pushWidgetSnapshot()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2_000,
                /* maxBufferMs = */ 15_000,
                /* bufferForPlaybackMs = */ 250,
                /* bufferForPlaybackAfterRebufferMs = */ 500,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val extractorsFactory = DefaultExtractorsFactory()
            .setMp3ExtractorFlags(Mp3Extractor.FLAG_DISABLE_ID3_METADATA)
            .setConstantBitrateSeekingEnabled(true)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractorsFactory))
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent())
            .build()

        player.addListener(widgetListener)

        crossfadeController = CrossfadeController(player, scope).also { it.start() }
        scope.launch {
            settingsRepository.settings.collect { settings ->
                crossfadeController?.crossfadeMs = settings.crossfadeMs
            }
        }

        startWidgetTicker()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        widgetTickerJob?.cancel()
        crossfadeController?.stop()
        mediaSession?.run {
            player.removeListener(widgetListener)
            player.release()
            release()
        }
        mediaSession = null
        scope.cancel()
        super.onDestroy()
    }

    private fun sessionActivityIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /**
     * O progresso do widget só é reenviado enquanto há reprodução e algum widget na
     * tela inicial, em cadência baixa. Fora disso o widget é atualizado por evento.
     */
    private fun startWidgetTicker() {
        widgetTickerJob = scope.launch {
            while (isActive) {
                delay(Constants.WIDGET_PROGRESS_TICK_MS)
                val player = mediaSession?.player ?: continue
                if (player.isPlaying && PlayerWidgetUpdater.hasWidgets(applicationContext)) {
                    pushWidgetSnapshot()
                }
            }
        }
    }

    private fun pushWidgetSnapshot() {
        val player = mediaSession?.player ?: return
        val metadata = player.mediaMetadata
        val snapshot = if (player.mediaItemCount == 0) {
            WidgetSnapshot.Empty
        } else {
            WidgetSnapshot(
                title = metadata.title?.toString() ?: WidgetSnapshot.Empty.title,
                artist = metadata.artist?.toString() ?: WidgetSnapshot.Empty.artist,
                artworkUri = metadata.artworkUri?.toString(),
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeIf { it > 0L } ?: 0L,
            )
        }
        scope.launch { PlayerWidgetUpdater.update(applicationContext, snapshot) }
    }
}
