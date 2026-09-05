package br.com.williamfranco.resonance.src.fakes

import br.com.williamfranco.resonance.src.features.settings.models.ColorSource
import br.com.williamfranco.resonance.src.features.settings.models.SettingsModel
import br.com.williamfranco.resonance.src.features.settings.models.ThemeMode
import br.com.williamfranco.resonance.src.features.settings.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsRepository(
    initial: SettingsModel = SettingsModel(),
) : SettingsRepository {

    private val _settings = MutableStateFlow(initial)

    override val settings: Flow<SettingsModel> = _settings.asStateFlow()

    val current: SettingsModel
        get() = _settings.value

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        _settings.value = _settings.value.copy(themeMode = themeMode)
    }

    override suspend fun updateColorSource(colorSource: ColorSource) {
        _settings.value = _settings.value.copy(colorSource = colorSource)
    }

    override suspend fun updateCrossfadeEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(crossfadeEnabled = enabled)
    }

    override suspend fun updateCrossfadeSeconds(seconds: Int) {
        _settings.value = _settings.value.copy(crossfadeSeconds = seconds.coerceIn(1, 12))
    }

    override suspend fun updateIgnoreShortTracks(ignore: Boolean) {
        _settings.value = _settings.value.copy(ignoreShortTracks = ignore)
    }
}
