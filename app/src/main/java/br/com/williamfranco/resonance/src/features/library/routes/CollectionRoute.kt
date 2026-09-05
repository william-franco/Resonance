package br.com.williamfranco.resonance.src.features.library.routes

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.williamfranco.resonance.src.features.library.view_models.CollectionViewModel
import br.com.williamfranco.resonance.src.features.library.view_models.CollectionViewModelImpl
import br.com.williamfranco.resonance.src.features.library.views.CollectionView
import br.com.williamfranco.resonance.src.features.player.view_models.PlayerViewModel
import br.com.williamfranco.resonance.src.features.player.view_models.PlayerViewModelImpl
import br.com.williamfranco.resonance.src.routes.CollectionType
import org.koin.androidx.compose.koinViewModel

@Composable
fun CollectionRoute(
    type: CollectionType,
    collectionKey: String,
    bottomPadding: Dp,
    onBack: () -> Unit,
) {
    val activity = LocalContext.current as ComponentActivity
    val collectionViewModel: CollectionViewModel = koinViewModel<CollectionViewModelImpl>()
    val playerViewModel: PlayerViewModel = koinViewModel<PlayerViewModelImpl>(viewModelStoreOwner = activity)

    LaunchedEffect(type, collectionKey) {
        collectionViewModel.load(type, collectionKey)
    }

    val uiState by collectionViewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by playerViewModel.state.collectAsStateWithLifecycle()

    CollectionView(
        uiState = uiState,
        currentSongId = playbackState.currentSong?.id,
        bottomPadding = bottomPadding,
        onBack = onBack,
        onSongClick = { index -> playerViewModel.play(uiState.songs, index) },
        onPlayAll = { playerViewModel.play(uiState.songs, 0) },
        onShuffleAll = { playerViewModel.shuffle(uiState.songs) },
        onToggleFavorite = collectionViewModel::toggleFavorite,
        onRemoveFromPlaylist = collectionViewModel::removeFromPlaylist,
    )
}
