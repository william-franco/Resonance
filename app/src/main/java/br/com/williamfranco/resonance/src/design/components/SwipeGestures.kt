package br.com.williamfranco.resonance.src.design.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Arraste vertical simples: acumula o deslocamento e dispara a ação quando o gesto
 * termina além do limiar. Usado para abrir e fechar o player em tela cheia.
 */
@Composable
fun Modifier.verticalSwipe(
    thresholdPx: Float = 140f,
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
): Modifier {
    var travelled by remember { mutableFloatStateOf(0f) }

    return this.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { delta -> travelled += delta },
        onDragStopped = {
            when {
                travelled <= -thresholdPx -> onSwipeUp()
                travelled >= thresholdPx -> onSwipeDown()
            }
            travelled = 0f
        },
    )
}
