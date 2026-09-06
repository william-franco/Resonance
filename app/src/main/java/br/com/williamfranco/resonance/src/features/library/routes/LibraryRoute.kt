package br.com.williamfranco.resonance.src.features.library.routes

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.williamfranco.resonance.src.common.patterns.StatePattern
import br.com.williamfranco.resonance.src.features.library.view_models.LibraryViewModel
import br.com.williamfranco.resonance.src.features.library.view_models.LibraryViewModelImpl
import br.com.williamfranco.resonance.src.features.library.views.LibraryView
import br.com.williamfranco.resonance.src.features.player.view_models.PlayerViewModel
import br.com.williamfranco.resonance.src.features.player.view_models.PlayerViewModelImpl
import br.com.williamfranco.resonance.src.routes.CollectionType
import br.com.williamfranco.resonance.src.services.library.audioPermission
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryRoute(
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onOpenCollection: (CollectionType, String) -> Unit,
) {
    val activity = LocalContext.current as ComponentActivity
    val libraryViewModel: LibraryViewModel = koinViewModel<LibraryViewModelImpl>(viewModelStoreOwner = activity)
    val playerViewModel: PlayerViewModel = koinViewModel<PlayerViewModelImpl>(viewModelStoreOwner = activity)

    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()
    val playbackState by playerViewModel.state.collectAsStateWithLifecycle()
    val songs = (libraryState as? StatePattern.Success)?.data?.songs.orEmpty()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = libraryViewModel::onPermissionResult,
    )

    LibraryView(
        libraryState = libraryState,
        currentSongId = playbackState.currentSong?.id,
        bottomPadding = bottomPadding,
        onTabSelected = libraryViewModel::selectTab,
        onQueryChange = libraryViewModel::updateQuery,
        onSongClick = { index -> playerViewModel.play(songs, index) },
        onPlayAll = { playerViewModel.play(songs, 0) },
        onShuffleAll = { playerViewModel.shuffle(songs) },
        onToggleFavorite = libraryViewModel::toggleFavorite,
        onAlbumClick = { onOpenCollection(CollectionType.ALBUM, it.id.toString()) },
        onArtistClick = { onOpenCollection(CollectionType.ARTISTA, it.name) },
        onPlaylistClick = { onOpenCollection(CollectionType.PLAYLIST, it.id.toString()) },
        onFavoritesClick = { onOpenCollection(CollectionType.FAVORITAS, "favoritas") },
        onCreatePlaylist = libraryViewModel::createPlaylist,
        onDeletePlaylist = { libraryViewModel.deletePlaylist(it.id) },
        onAddToPlaylist = { song, playlist -> libraryViewModel.addToPlaylist(playlist.id, listOf(song.id)) },
        onOpenSettings = onOpenSettings,
        onRequestPermission = { permissionLauncher.launch(audioPermission()) },
        onRetry = libraryViewModel::refresh,
    )
}
