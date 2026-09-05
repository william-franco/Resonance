package br.com.williamfranco.resonance.src.widgets

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import br.com.williamfranco.resonance.src.services.playback.PlaybackService
import com.google.common.util.concurrent.MoreExecutors

/**
 * Traduz os toques do widget em comandos da sessão de mídia. Um [MediaController]
 * efêmero é criado, usado e liberado, então nada fica rodando por conta do widget.
 */
class PlayerWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) return

        val pendingResult = goAsync()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()

        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    when (action) {
                        ACTION_PLAY_PAUSE -> if (controller.isPlaying) controller.pause() else controller.play()
                        ACTION_NEXT -> controller.seekToNextMediaItem()
                        ACTION_PREVIOUS -> controller.seekToPreviousMediaItem()
                    }
                    controller.release()
                }
                pendingResult.finish()
            },
            MoreExecutors.directExecutor(),
        )
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "br.com.williamfranco.resonance.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "br.com.williamfranco.resonance.widget.NEXT"
        const val ACTION_PREVIOUS = "br.com.williamfranco.resonance.widget.PREVIOUS"

        private val SUPPORTED_ACTIONS = setOf(ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREVIOUS)
    }
}
