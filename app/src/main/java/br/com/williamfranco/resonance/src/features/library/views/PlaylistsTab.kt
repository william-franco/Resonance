package br.com.williamfranco.resonance.src.features.library.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.williamfranco.resonance.src.design.components.AlbumArt
import br.com.williamfranco.resonance.src.design.components.Dimens
import br.com.williamfranco.resonance.src.features.library.models.Playlist

@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    favoritesCount: Int,
    contentPadding: PaddingValues,
    onPlaylistClick: (Playlist) -> Unit,
    onFavoritesClick: () -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onCreatePlaylistRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "create") {
            TextButton(
                onClick = onCreatePlaylistRequest,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 4.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(text = "Nova playlist", modifier = Modifier.padding(start = 8.dp))
            }
        }

        item(key = "favorites") {
            PlaylistRow(
                name = "Favoritas",
                subtitle = "$favoritesCount faixas",
                onClick = onFavoritesClick,
                leading = {
                    Box(
                        modifier = Modifier
                            .size(Dimens.ArtSmall)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                },
            )
        }

        items(items = playlists, key = { it.id }) { playlist ->
            PlaylistRow(
                name = playlist.name,
                subtitle = "${playlist.songCount} faixas",
                onClick = { onPlaylistClick(playlist) },
                leading = {
                    AlbumArt(
                        artworkUri = playlist.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.ArtSmall),
                    )
                },
                trailing = {
                    IconButton(onClick = { onDeletePlaylist(playlist) }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Excluir playlist",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun PlaylistRow(
    name: String,
    subtitle: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding - 4.dp)
            .height(Dimens.SongRowHeight)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}
