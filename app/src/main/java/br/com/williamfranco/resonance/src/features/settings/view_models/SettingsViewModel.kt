package br.com.williamfranco.resonance.src.features.settings.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.williamfranco.resonance.src.features.library.repositories.LibraryRepository
import br.com.williamfranco.resonance.src.features.settings.models.ColorSource
import br.com.williamfranco.resonance.src.features.settings.models.SettingsModel
import br.com.williamfranco.resonance.src.features.settings.models.ThemeMode
import br.com.williamfranco.resonance.src.features.settings.repositories.SettingsRepository
import br.com.williamfranco.resonance.src.services.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: SettingsModel = SettingsModel(),
    val isScanning: Boolean = false,
    val lastScanCount: Int? = null,
)

interface SettingsViewModel {
    val uiState: StateFlow<SettingsUiState>

    fun updateThemeMode(themeMode: ThemeMode)
    fun updateColorSource(colorSource: ColorSource)
    fun updateCrossfadeEnabled(enabled: Boolean)
    fun updateCrossfadeSeconds(seconds: Int)
    fun updateIgnoreShortTracks(ignore: Boolean)
    fun rescanLibrary()
}

class SettingsViewModelImpl(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel(), SettingsViewModel {

    private val _uiState = MutableStateFlow(SettingsUiState())
    override val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        settingsRepository.settings
            .onEach { settings -> _uiState.update { it.copy(settings = settings) } }
            .launchIn(viewModelScope)
    }

    override fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { settingsRepository.updateThemeMode(themeMode) }
    }

    override fun updateColorSource(colorSource: ColorSource) {
        viewModelScope.launch { settingsRepository.updateColorSource(colorSource) }
    }

    override fun updateCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateCrossfadeEnabled(enabled) }
    }

    override fun updateCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.updateCrossfadeSeconds(seconds) }
    }

    override fun updateIgnoreShortTracks(ignore: Boolean) {
        viewModelScope.launch { settingsRepository.updateIgnoreShortTracks(ignore) }
    }

    override fun rescanLibrary() {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, lastScanCount = null) }
            val minDuration = if (_uiState.value.settings.ignoreShortTracks) {
                Constants.SHORT_TRACK_THRESHOLD_MS
            } else {
                0L
            }
            val count = runCatching { libraryRepository.sync(minDuration) }.getOrDefault(0)
            _uiState.update { it.copy(isScanning = false, lastScanCount = count) }
        }
    }
}
