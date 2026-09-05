package br.com.williamfranco.resonance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.williamfranco.resonance.src.design.theme.ResonanceTheme
import br.com.williamfranco.resonance.src.design.theme.rememberSeedColor
import br.com.williamfranco.resonance.src.features.player.view_models.PlayerViewModel
import br.com.williamfranco.resonance.src.features.player.view_models.PlayerViewModelImpl
import br.com.williamfranco.resonance.src.features.settings.models.ColorSource
import br.com.williamfranco.resonance.src.features.settings.view_models.SettingsViewModel
import br.com.williamfranco.resonance.src.features.settings.view_models.SettingsViewModelImpl
import br.com.williamfranco.resonance.src.routes.RoutesApp
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Ambas as ViewModels ficam no escopo da Activity, então as rotas veem a mesma instância.
            val settingsViewModel: SettingsViewModel = koinViewModel<SettingsViewModelImpl>()
            val playerViewModel: PlayerViewModel = koinViewModel<PlayerViewModelImpl>()

            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val playbackState by playerViewModel.state.collectAsStateWithLifecycle()

            val settings = settingsState.settings
            val seedColor = rememberSeedColor(
                artworkUri = playbackState.currentSong?.artworkUri,
                enabled = settings.colorSource == ColorSource.CAPA_DO_ALBUM,
            )

            ResonanceTheme(
                themeMode = settings.themeMode,
                colorSource = settings.colorSource,
                seedColor = seedColor,
            ) {
                RoutesApp()
            }
        }
    }
}
