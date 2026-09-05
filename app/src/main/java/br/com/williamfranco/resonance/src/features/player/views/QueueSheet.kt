package br.com.williamfranco.resonance.src.features.player.views

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import br.com.williamfranco.resonance.src.design.components.Dimens
import br.com.williamfranco.resonance.src.design.components.SongRow
import br.com.williamfranco.resonance.src.features.library.models.Song

@Composable
fun QueueSheet(
    queue: List<Song>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSongClick: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Text(
            text = "Fila · ${queue.size} faixas",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(items = queue, key = { _, song -> song.id }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = index == currentIndex,
                    onClick = { onSongClick(index) },
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding - 4.dp),
                ) {
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remover da fila",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DragHandle(index = index, itemCount = queue.size, onMove = onMove)
                }
            }
        }
    }
}

/**
 * Reordenação por arraste sem biblioteca externa: o deslocamento acumulado é convertido
 * em trocas de uma posição por vez, o que mantém a lista e o player sempre sincronizados.
 */
@Composable
private fun DragHandle(index: Int, itemCount: Int, onMove: (Int, Int) -> Unit) {
    val rowHeightPx = with(LocalDensity.current) { Dimens.SongRowHeight.toPx() }
    var travelled by remember { mutableFloatStateOf(0f) }

    Icon(
        imageVector = Icons.Rounded.DragHandle,
        contentDescription = "Reordenar",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(24.dp)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    travelled += delta
                    while (travelled >= rowHeightPx && index < itemCount - 1) {
                        onMove(index, index + 1)
                        travelled -= rowHeightPx
                    }
                    while (travelled <= -rowHeightPx && index > 0) {
                        onMove(index, index - 1)
                        travelled += rowHeightPx
                    }
                },
                onDragStopped = { travelled = 0f },
            ),
    )
}
