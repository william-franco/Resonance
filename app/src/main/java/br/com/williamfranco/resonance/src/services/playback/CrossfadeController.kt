package br.com.williamfranco.resonance.src.services.playback

import androidx.media3.common.Player
import br.com.williamfranco.resonance.src.services.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val IDLE_POLL_MS = 500L

/**
 * Curva de volume do crossfade. Isolada da reprodução para poder ser testada sem player.
 */
object CrossfadeVolume {

    fun volumeFor(positionMs: Long, durationMs: Long, crossfadeMs: Long): Float {
        if (crossfadeMs <= 0L || durationMs <= 0L || positionMs < 0L) return 1f

        // Faixas curtas não comportam a rampa inteira nas duas pontas.
        val fade = minOf(crossfadeMs, durationMs / 2)
        if (fade <= 0L) return 1f

        val remaining = durationMs - positionMs
        return when {
            positionMs < fade -> (positionMs.toFloat() / fade).coerceIn(0f, 1f)
            remaining < fade -> (remaining.toFloat() / fade).coerceIn(0f, 1f)
            else -> 1f
        }
    }
}

/**
 * O Media3 não tem crossfade nativo: a transição é simulada com uma rampa de volume
 * no próprio player, que some no fim da faixa e volta no início da seguinte.
 * Com a duração em zero o player fica em volume cheio e a troca continua gapless.
 */
class CrossfadeController(
    private val player: Player,
    private val scope: CoroutineScope,
) {
    @Volatile
    var crossfadeMs: Long = 0L

    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                val duration = crossfadeMs
                if (duration > 0L && player.isPlaying) {
                    player.volume = CrossfadeVolume.volumeFor(
                        positionMs = player.currentPosition,
                        durationMs = player.duration,
                        crossfadeMs = duration,
                    )
                    delay(Constants.CROSSFADE_STEP_MS)
                } else {
                    if (player.volume != 1f) player.volume = 1f
                    delay(IDLE_POLL_MS)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
