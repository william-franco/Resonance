package br.com.williamfranco.resonance.src.features.library.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.williamfranco.resonance.src.design.components.Dimens
import br.com.williamfranco.resonance.src.design.components.EmptyState
import br.com.williamfranco.resonance.src.design.components.PlayShuffleRow
import br.com.williamfranco.resonance.src.design.components.SongRow
import br.com.williamfranco.resonance.src.features.library.models.Song

@Composable
fun SongsTab(
    songs: List<Song>,
    currentSongId: Long?,
    contentPadding: PaddingValues,
    onSongClick: (Int) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (songs.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.MusicOff,
            title = "Nenhuma faixa por aqui",
            description = "Ajuste a busca ou reindexe a biblioteca nas configurações.",
            modifier = modifier.padding(contentPadding),
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "play-shuffle") {
            PlayShuffleRow(onPlayAll = onPlayAll, onShuffleAll = onShuffleAll)
        }

        itemsIndexed(items = songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                song = song,
                isCurrent = song.id == currentSongId,
                onClick = { onSongClick(index) },
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding - 4.dp),
            ) {
                IconButton(onClick = { onToggleFavorite(song.id) }) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (song.isFavorite) "Remover dos favoritos" else "Favoritar",
                        tint = if (song.isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = { onAddToPlaylist(song) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Adicionar à playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
