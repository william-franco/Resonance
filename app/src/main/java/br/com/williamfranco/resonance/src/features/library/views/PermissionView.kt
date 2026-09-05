package br.com.williamfranco.resonance.src.features.library.views

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.williamfranco.resonance.src.design.components.EmptyState

@Composable
fun PermissionView(onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Rounded.LibraryMusic,
        title = "O Resonance precisa acessar seu áudio",
        description = "A permissão é usada apenas para listar as músicas guardadas no dispositivo. " +
            "Nada sai do aparelho.",
        modifier = modifier,
        action = {
            Button(onClick = onRequestPermission) {
                Text("Permitir acesso")
            }
        },
    )
}
