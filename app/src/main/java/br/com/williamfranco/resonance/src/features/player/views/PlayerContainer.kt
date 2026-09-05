package br.com.williamfranco.resonance.src.features.player.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.williamfranco.resonance.src.services.playback.PlaybackState

/**
 * Camada global do player. O mini-player e a tela cheia são dois estados do mesmo
 * [AnimatedContent] dentro de um [SharedTransitionLayout]: a capa, o título e o artista
 * são elementos compartilhados, então a expansão parece um único componente crescendo.
 */
@Composable
fun PlayerContainer(
    state: PlaybackState,
    isExpanded: Boolean,
    bottomPadding: Dp,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onQueueSongClick: (Int) -> Unit,
    onQueueRemove: (Int) -> Unit,
    onQueueMove: (Int, Int) -> Unit,
) {
    val song = state.currentSong ?: return
    var queueVisible by remember { mutableStateOf(false) }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
            label = "player",
        ) { expanded ->
            if (expanded) {
                FullPlayerView(
                    sharedScope = this@SharedTransitionLayout,
                    animatedScope = this@AnimatedContent,
                    song = song,
                    isPlaying = state.isPlaying,
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    shuffleEnabled = state.shuffleEnabled,
                    repeatMode = state.repeatMode,
                    onCollapse = onCollapse,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeek = onSeek,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    onToggleFavorite = onToggleFavorite,
                    onOpenQueue = { queueVisible = true },
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    MiniPlayerView(
                        sharedScope = this@SharedTransitionLayout,
                        animatedScope = this@AnimatedContent,
                        song = song,
                        isPlaying = state.isPlaying,
                        progress = state.progress,
                        onExpand = onExpand,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = bottomPadding + 8.dp),
                    )
                }
            }
        }
    }

    if (queueVisible) {
        QueueSheet(
            queue = state.queue,
            currentIndex = state.queueIndex,
            onDismiss = { queueVisible = false },
            onSongClick = onQueueSongClick,
            onRemove = onQueueRemove,
            onMove = onQueueMove,
        )
    }
}
