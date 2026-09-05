package br.com.williamfranco.resonance.src.features.settings.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.williamfranco.resonance.src.features.settings.models.ColorSource
import br.com.williamfranco.resonance.src.features.settings.models.SettingsModel
import br.com.williamfranco.resonance.src.features.settings.models.ThemeMode
import br.com.williamfranco.resonance.src.services.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.SETTINGS_DATASTORE_NAME,
)

interface SettingsRepository {
    val settings: Flow<SettingsModel>

    suspend fun updateThemeMode(themeMode: ThemeMode)
    suspend fun updateColorSource(colorSource: ColorSource)
    suspend fun updateCrossfadeEnabled(enabled: Boolean)
    suspend fun updateCrossfadeSeconds(seconds: Int)
    suspend fun updateIgnoreShortTracks(ignore: Boolean)
}

class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    override val settings: Flow<SettingsModel> = dataStore.data.map { preferences ->
        SettingsModel(
            themeMode = preferences[THEME_MODE_KEY]?.toEnum() ?: SettingsModel().themeMode,
            colorSource = preferences[COLOR_SOURCE_KEY]?.toColorSource() ?: SettingsModel().colorSource,
            crossfadeEnabled = preferences[CROSSFADE_ENABLED_KEY] ?: SettingsModel().crossfadeEnabled,
            crossfadeSeconds = preferences[CROSSFADE_SECONDS_KEY] ?: SettingsModel().crossfadeSeconds,
            ignoreShortTracks = preferences[IGNORE_SHORT_TRACKS_KEY] ?: SettingsModel().ignoreShortTracks,
        )
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = themeMode.name }
    }

    override suspend fun updateColorSource(colorSource: ColorSource) {
        dataStore.edit { it[COLOR_SOURCE_KEY] = colorSource.name }
    }

    override suspend fun updateCrossfadeEnabled(enabled: Boolean) {
        dataStore.edit { it[CROSSFADE_ENABLED_KEY] = enabled }
    }

    override suspend fun updateCrossfadeSeconds(seconds: Int) {
        dataStore.edit { it[CROSSFADE_SECONDS_KEY] = seconds.coerceIn(1, 12) }
    }

    override suspend fun updateIgnoreShortTracks(ignore: Boolean) {
        dataStore.edit { it[IGNORE_SHORT_TRACKS_KEY] = ignore }
    }

    private fun String.toEnum(): ThemeMode? = ThemeMode.entries.firstOrNull { it.name == this }

    private fun String.toColorSource(): ColorSource? = ColorSource.entries.firstOrNull { it.name == this }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val COLOR_SOURCE_KEY = stringPreferencesKey("color_source")
        val CROSSFADE_ENABLED_KEY = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_SECONDS_KEY = intPreferencesKey("crossfade_seconds")
        val IGNORE_SHORT_TRACKS_KEY = booleanPreferencesKey("ignore_short_tracks")
    }
}
