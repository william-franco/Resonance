package br.com.williamfranco.resonance.src.services.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class CrossfadeVolumeTest {

    @Test
    fun `mantem volume cheio quando o crossfade esta desligado`() {
        val volume = CrossfadeVolume.volumeFor(positionMs = 1_000, durationMs = 200_000, crossfadeMs = 0)

        assertEquals(1f, volume, TOLERANCE)
    }

    @Test
    fun `mantem volume cheio quando a duracao e desconhecida`() {
        val volume = CrossfadeVolume.volumeFor(positionMs = 1_000, durationMs = -1, crossfadeMs = 4_000)

        assertEquals(1f, volume, TOLERANCE)
    }

    @Test
    fun `sobe o volume no inicio da faixa`() {
        val crossfade = 4_000L
        val duration = 200_000L

        assertEquals(0f, CrossfadeVolume.volumeFor(0, duration, crossfade), TOLERANCE)
        assertEquals(0.25f, CrossfadeVolume.volumeFor(1_000, duration, crossfade), TOLERANCE)
        assertEquals(0.5f, CrossfadeVolume.volumeFor(2_000, duration, crossfade), TOLERANCE)
    }

    @Test
    fun `desce o volume no fim da faixa`() {
        val crossfade = 4_000L
        val duration = 200_000L

        assertEquals(1f, CrossfadeVolume.volumeFor(196_000, duration, crossfade), TOLERANCE)
        assertEquals(0.5f, CrossfadeVolume.volumeFor(198_000, duration, crossfade), TOLERANCE)
        assertEquals(0f, CrossfadeVolume.volumeFor(200_000, duration, crossfade), TOLERANCE)
    }

    @Test
    fun `mantem volume cheio no meio da faixa`() {
        val volume = CrossfadeVolume.volumeFor(positionMs = 100_000, durationMs = 200_000, crossfadeMs = 4_000)

        assertEquals(1f, volume, TOLERANCE)
    }

    @Test
    fun `limita a rampa a metade da faixa em audios curtos`() {
        // Faixa de 4s com crossfade de 6s: a rampa cai para 2s em cada ponta.
        val duration = 4_000L
        val crossfade = 6_000L

        assertEquals(0.5f, CrossfadeVolume.volumeFor(1_000, duration, crossfade), TOLERANCE)
        assertEquals(1f, CrossfadeVolume.volumeFor(2_000, duration, crossfade), TOLERANCE)
        assertEquals(0.5f, CrossfadeVolume.volumeFor(3_000, duration, crossfade), TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
