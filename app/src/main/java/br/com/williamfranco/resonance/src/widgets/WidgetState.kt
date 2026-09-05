package br.com.williamfranco.resonance.src.widgets

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

data class WidgetSnapshot(
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
) {
    companion object {
        val Empty = WidgetSnapshot(
            title = "Nada tocando",
            artist = "Escolha uma faixa no Resonance",
            artworkUri = null,
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L,
        )
    }
}

object PlayerWidgetKeys {
    val title = stringPreferencesKey("title")
    val artist = stringPreferencesKey("artist")
    val artworkUri = stringPreferencesKey("artworkUri")
    val isPlaying = booleanPreferencesKey("isPlaying")
    val positionMs = longPreferencesKey("positionMs")
    val durationMs = longPreferencesKey("durationMs")
}

/**
 * Grava o estado do player nas preferências do Glance e redesenha apenas os widgets
 * que existem de fato. Se não houver nenhum na tela inicial, nada é feito.
 */
object PlayerWidgetUpdater {

    suspend fun hasWidgets(context: Context): Boolean = glanceIds(context).isNotEmpty()

    suspend fun update(context: Context, snapshot: WidgetSnapshot) {
        val ids = glanceIds(context)
        if (ids.isEmpty()) return

        val widget = PlayerWidget()
        ids.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { preferences ->
                preferences[PlayerWidgetKeys.title] = snapshot.title
                preferences[PlayerWidgetKeys.artist] = snapshot.artist
                preferences[PlayerWidgetKeys.artworkUri] = snapshot.artworkUri.orEmpty()
                preferences[PlayerWidgetKeys.isPlaying] = snapshot.isPlaying
                preferences[PlayerWidgetKeys.positionMs] = snapshot.positionMs
                preferences[PlayerWidgetKeys.durationMs] = snapshot.durationMs
            }
            widget.update(context, glanceId)
        }
    }

    private suspend fun glanceIds(context: Context) = runCatching {
        GlanceAppWidgetManager(context).getGlanceIds(PlayerWidget::class.java)
    }.getOrDefault(emptyList())
}
