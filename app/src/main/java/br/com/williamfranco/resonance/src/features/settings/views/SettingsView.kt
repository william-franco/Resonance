package br.com.williamfranco.resonance.src.features.settings.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import br.com.williamfranco.resonance.BuildConfig
import br.com.williamfranco.resonance.src.design.components.Dimens
import br.com.williamfranco.resonance.src.features.settings.models.ColorSource
import br.com.williamfranco.resonance.src.features.settings.models.ThemeMode
import br.com.williamfranco.resonance.src.features.settings.view_models.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorSourceChange: (ColorSource) -> Unit,
    onCrossfadeEnabledChange: (Boolean) -> Unit,
    onCrossfadeSecondsChange: (Int) -> Unit,
    onIgnoreShortTracksChange: (Boolean) -> Unit,
    onRescan: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var themeDialogVisible by remember { mutableStateOf(false) }
    var colorDialogVisible by remember { mutableStateOf(false) }
    val settings = uiState.settings

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + Dimens.MiniPlayerHeight + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SettingsGroup(title = "Aparência") {
                    SettingsRow(
                        icon = Icons.Rounded.Palette,
                        title = "Tema",
                        description = settings.themeMode.label,
                        onClick = { themeDialogVisible = true },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Palette,
                        title = "Origem das cores",
                        description = settings.colorSource.label,
                        onClick = { colorDialogVisible = true },
                    )
                }
            }

            item {
                SettingsGroup(title = "Reprodução") {
                    SettingsRow(
                        icon = Icons.Rounded.Bolt,
                        title = "Crossfade",
                        description = if (settings.crossfadeEnabled) {
                            "Transição suave de ${settings.crossfadeSeconds}s entre faixas"
                        } else {
                            "Desligado: troca instantânea e sem falhas"
                        },
                        onClick = { onCrossfadeEnabledChange(!settings.crossfadeEnabled) },
                        trailing = {
                            Switch(
                                checked = settings.crossfadeEnabled,
                                onCheckedChange = onCrossfadeEnabledChange,
                            )
                        },
                    )
                    if (settings.crossfadeEnabled) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Text(
                                text = "Duração: ${settings.crossfadeSeconds} segundos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Slider(
                                value = settings.crossfadeSeconds.toFloat(),
                                onValueChange = { onCrossfadeSecondsChange(it.toInt()) },
                                valueRange = 1f..12f,
                                steps = 10,
                            )
                        }
                    }
                }
            }

            item {
                SettingsGroup(title = "Biblioteca") {
                    SettingsRow(
                        icon = Icons.Rounded.LibraryMusic,
                        title = "Ignorar faixas curtas",
                        description = "Descarta arquivos com menos de 30 segundos",
                        onClick = { onIgnoreShortTracksChange(!settings.ignoreShortTracks) },
                        trailing = {
                            Switch(
                                checked = settings.ignoreShortTracks,
                                onCheckedChange = onIgnoreShortTracksChange,
                            )
                        },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.LibraryMusic,
                        title = "Reindexar biblioteca",
                        description = when {
                            uiState.isScanning -> "Procurando arquivos no dispositivo..."
                            uiState.lastScanCount != null -> "${uiState.lastScanCount} faixas encontradas"
                            else -> "Relê o MediaStore e atualiza o catálogo"
                        },
                        onClick = onRescan,
                        trailing = {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            }
                        },
                    )
                }
            }

            item {
                SettingsGroup(title = "Sobre") {
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = "Resonance",
                        description = "Versão ${BuildConfig.VERSION_NAME}",
                        onClick = {},
                    )
                }
            }
        }
    }

    if (themeDialogVisible) {
        OptionsDialog(
            title = "Tema",
            options = ThemeMode.entries,
            selected = settings.themeMode,
            labelOf = { it.label },
            descriptionOf = { null },
            onSelect = {
                onThemeModeChange(it)
                themeDialogVisible = false
            },
            onDismiss = { themeDialogVisible = false },
        )
    }

    if (colorDialogVisible) {
        OptionsDialog(
            title = "Origem das cores",
            options = ColorSource.entries,
            selected = settings.colorSource,
            labelOf = { it.label },
            descriptionOf = { it.description },
            onSelect = {
                onColorSourceChange(it)
                colorDialogVisible = false
            },
            onDismiss = { colorDialogVisible = false },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(vertical = 4.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

@Composable
private fun <T> OptionsDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    descriptionOf: (T) -> String?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .selectable(
                                selected = option == selected,
                                onClick = { onSelect(option) },
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Column {
                            Text(labelOf(option), style = MaterialTheme.typography.bodyLarge)
                            descriptionOf(option)?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        },
    )
}
