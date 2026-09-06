package br.com.williamfranco.resonance.src.features.library.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.williamfranco.resonance.src.common.patterns.StatePattern
import br.com.williamfranco.resonance.src.design.components.Dimens
import br.com.williamfranco.resonance.src.design.components.EmptyState
import br.com.williamfranco.resonance.src.features.library.models.Album
import br.com.williamfranco.resonance.src.features.library.models.Artist
import br.com.williamfranco.resonance.src.features.library.models.LibraryTab
import br.com.williamfranco.resonance.src.features.library.models.Playlist
import br.com.williamfranco.resonance.src.features.library.models.Song
import br.com.williamfranco.resonance.src.features.library.view_models.LibraryState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryView(
    libraryState: LibraryState,
    currentSongId: Long?,
    bottomPadding: Dp,
    onTabSelected: (LibraryTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onSongClick: (Int) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onFavoritesClick: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onAddToPlaylist: (Song, Playlist) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
) {
    val content = (libraryState as? StatePattern.Success)?.data
    val query = content?.query.orEmpty()
    val selectedTab = content?.tab ?: LibraryTab.MUSICAS
    val isScanning = content?.isScanning == true ||
        libraryState is StatePattern.Loading
    var searchVisible by remember { mutableStateOf(false) }
    var newPlaylistVisible by remember { mutableStateOf(false) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Resonance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                searchVisible = !searchVisible
                                if (!searchVisible) onQueryChange("")
                            },
                        ) {
                            Icon(
                                imageVector = if (searchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = if (searchVisible) "Fechar busca" else "Buscar",
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Configurações")
                        }
                    },
                )

                AnimatedVisibility(visible = searchVisible && content != null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Buscar faixas, álbuns ou artistas") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.ScreenPadding, vertical = 4.dp),
                    )
                }

                AnimatedVisibility(visible = isScanning) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                LibraryTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(tab.icon(), contentDescription = tab.label) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { padding ->
        when (libraryState) {
            is StatePattern.Initial -> {
                PermissionView(
                    onRequestPermission = onRequestPermission,
                    modifier = Modifier.padding(padding),
                )
            }

            is StatePattern.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is StatePattern.Error -> {
                EmptyState(
                    icon = Icons.Rounded.ErrorOutline,
                    title = "Não foi possível carregar a biblioteca",
                    description = libraryState.error.message.orEmpty(),
                    modifier = Modifier.padding(padding),
                    action = {
                        TextButton(onClick = onRetry) {
                            Text("Tentar novamente")
                        }
                    },
                )
            }

            is StatePattern.Success -> {
                if (!libraryState.data.hasPermission) {
                    PermissionView(
                        onRequestPermission = onRequestPermission,
                        modifier = Modifier.padding(padding),
                    )
                    return@Scaffold
                }

                val uiState = libraryState.data
                val contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + bottomPadding + 12.dp,
                )

                AnimatedContent(
                    targetState = uiState.tab,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "libraryTab",
                    modifier = Modifier.fillMaxSize(),
                ) { tab ->
                    when (tab) {
                        LibraryTab.MUSICAS -> SongsTab(
                            songs = uiState.songs,
                            currentSongId = currentSongId,
                            contentPadding = contentPadding,
                            onSongClick = onSongClick,
                            onPlayAll = onPlayAll,
                            onShuffleAll = onShuffleAll,
                            onToggleFavorite = onToggleFavorite,
                            onAddToPlaylist = { songForPlaylist = it },
                        )

                        LibraryTab.ALBUNS -> AlbumsTab(
                            albums = uiState.albums,
                            contentPadding = contentPadding,
                            onAlbumClick = onAlbumClick,
                        )

                        LibraryTab.ARTISTAS -> ArtistsTab(
                            artists = uiState.artists,
                            contentPadding = contentPadding,
                            onArtistClick = onArtistClick,
                        )

                        LibraryTab.PLAYLISTS -> PlaylistsTab(
                            playlists = uiState.playlists,
                            favoritesCount = uiState.favoritesCount,
                            contentPadding = contentPadding,
                            onPlaylistClick = onPlaylistClick,
                            onFavoritesClick = onFavoritesClick,
                            onDeletePlaylist = onDeletePlaylist,
                            onCreatePlaylistRequest = { newPlaylistVisible = true },
                        )
                    }
                }
            }
        }
    }

    if (newPlaylistVisible) {
        NewPlaylistDialog(
            onConfirm = {
                onCreatePlaylist(it)
                newPlaylistVisible = false
            },
            onDismiss = { newPlaylistVisible = false },
        )
    }

    songForPlaylist?.let { song ->
        ChoosePlaylistDialog(
            playlists = content?.playlists.orEmpty(),
            onSelect = { playlist ->
                onAddToPlaylist(song, playlist)
                songForPlaylist = null
            },
            onCreateRequest = {
                songForPlaylist = null
                newPlaylistVisible = true
            },
            onDismiss = { songForPlaylist = null },
        )
    }
}

@Composable
private fun NewPlaylistDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Nome da playlist") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun ChoosePlaylistDialog(
    playlists: List<Playlist>,
    onSelect: (Playlist) -> Unit,
    onCreateRequest: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar à playlist") },
        text = {
            if (playlists.isEmpty()) {
                Text("Você ainda não tem playlists.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    playlists.forEach { playlist ->
                        TextButton(onClick = { onSelect(playlist) }) {
                            Text("${playlist.name} · ${playlist.songCount} faixas")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreateRequest) { Text("Nova playlist") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private fun LibraryTab.icon() = when (this) {
    LibraryTab.MUSICAS -> Icons.Rounded.MusicNote
    LibraryTab.ALBUNS -> Icons.Rounded.Album
    LibraryTab.ARTISTAS -> Icons.Rounded.Person
    LibraryTab.PLAYLISTS -> Icons.AutoMirrored.Rounded.PlaylistPlay
}
