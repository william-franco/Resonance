package br.com.williamfranco.resonance.src.services.playback

import br.com.williamfranco.resonance.src.features.library.models.Song

data class PlaybackState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.DESLIGADO,
) {
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

enum class RepeatMode(val label: String) {
    DESLIGADO("Repetição desligada"),
    TODAS("Repetir todas"),
    UMA("Repetir uma"),
    ;

    fun next(): RepeatMode = when (this) {
        DESLIGADO -> TODAS
        TODAS -> UMA
        UMA -> DESLIGADO
    }
}
