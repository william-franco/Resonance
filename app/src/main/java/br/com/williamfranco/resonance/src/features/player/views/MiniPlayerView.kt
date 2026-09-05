package br.com.williamfranco.resonance.src.features.player.views

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.williamfranco.resonance.src.design.components.AlbumArt
import br.com.williamfranco.resonance.src.design.components.Dimens
import br.com.williamfranco.resonance.src.design.components.verticalSwipe
import br.com.williamfranco.resonance.src.features.library.models.Song

@Composable
fun MiniPlayerView(
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) = with(sharedScope) {
    Column(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onExpand)
            .verticalSwipe(onSwipeUp = onExpand),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.MiniPlayerHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AlbumArt(
                artworkUri = song.artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(Dimens.ArtMini)
                    .sharedElement(
                        rememberSharedContentState(key = PlayerSharedKeys.ART),
                        animatedScope,
                    ),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sharedBounds(
                        rememberSharedContentState(key = PlayerSharedKeys.TITLE),
                        animatedScope,
                    ),
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sharedBounds(
                        rememberSharedContentState(key = PlayerSharedKeys.ARTIST),
                        animatedScope,
                    ),
                )
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Tocar",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Próxima faixa",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            drawStopIndicator = {},
            gapSize = 0.dp,
        )
    }
}

object PlayerSharedKeys {
    const val ART = "player-art"
    const val TITLE = "player-title"
    const val ARTIST = "player-artist"
}
