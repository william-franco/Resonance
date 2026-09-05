package br.com.williamfranco.resonance.src.features.player.routes

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.williamfranco.resonance.src.features.player.view_models.PlayerViewModel
import br.com.williamfranco.resonance.src.features.player.view_models.PlayerViewModelImpl
import br.com.williamfranco.resonance.src.features.player.views.PlayerContainer
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayerRoute(bottomPadding: Dp) {
    val activity = LocalContext.current as ComponentActivity
    val viewModel: PlayerViewModel = koinViewModel<PlayerViewModelImpl>(viewModelStoreOwner = activity)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isExpanded by viewModel.isExpanded.collectAsStateWithLifecycle()

    PlayerContainer(
        state = state,
        isExpanded = isExpanded,
        bottomPadding = bottomPadding,
        onExpand = viewModel::expand,
        onCollapse = viewModel::collapse,
        onPlayPause = viewModel::togglePlayPause,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onSeek = viewModel::seekTo,
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeat,
        onToggleFavorite = viewModel::toggleFavorite,
        onQueueSongClick = viewModel::seekToQueueIndex,
        onQueueRemove = viewModel::removeFromQueue,
        onQueueMove = viewModel::moveQueueItem,
    )
}
