package br.com.williamfranco.resonance.src.features.settings.routes

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.williamfranco.resonance.src.features.settings.view_models.SettingsViewModel
import br.com.williamfranco.resonance.src.features.settings.view_models.SettingsViewModelImpl
import br.com.williamfranco.resonance.src.features.settings.views.SettingsView
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(onBack: () -> Unit) {
    val activity = LocalContext.current as ComponentActivity
    val viewModel: SettingsViewModel = koinViewModel<SettingsViewModelImpl>(viewModelStoreOwner = activity)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsView(
        uiState = uiState,
        onBack = onBack,
        onThemeModeChange = viewModel::updateThemeMode,
        onColorSourceChange = viewModel::updateColorSource,
        onCrossfadeEnabledChange = viewModel::updateCrossfadeEnabled,
        onCrossfadeSecondsChange = viewModel::updateCrossfadeSeconds,
        onIgnoreShortTracksChange = viewModel::updateIgnoreShortTracks,
        onRescan = viewModel::rescanLibrary,
    )
}
