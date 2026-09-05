package br.com.williamfranco.resonance.src.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import br.com.williamfranco.resonance.MainActivity
import br.com.williamfranco.resonance.R
import br.com.williamfranco.resonance.src.services.library.loadArtworkBitmap

private val CompactSize = DpSize(180.dp, 60.dp)
private val ExpandedSize = DpSize(280.dp, 130.dp)

/**
 * Widget único com dois layouts: compacto (capa, play/pause, próxima) e expandido
 * (capa, título, artista, progresso e controles completos). O Glance escolhe o layout
 * pelo tamanho da célula, então o mesmo provider atende os dois formatos.
 */
class PlayerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(CompactSize, ExpandedSize))

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = getAppWidgetState(context, PreferencesGlanceStateDefinition, id).toSnapshot()
        // A capa é decodificada fora da composição porque o Glance só aceita bitmaps prontos.
        val artwork = loadArtworkBitmap(context, snapshot.artworkUri, maxSize = 192)

        provideContent {
            GlanceTheme {
                WidgetContent(snapshot = snapshot, artwork = artwork)
            }
        }
    }
}

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot, artwork: Bitmap?) {
    val expanded = LocalSize.current.height >= ExpandedSize.height

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.secondaryContainer)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(artwork = artwork, size = if (expanded) 88.dp else 44.dp)
        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = snapshot.title,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = if (expanded) 16.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            if (expanded) {
                Text(
                    text = snapshot.artist,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 13.sp,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                LinearProgressIndicator(
                    progress = snapshot.progress(),
                    modifier = GlanceModifier.fillMaxWidth(),
                    color = GlanceTheme.colors.primary,
                    backgroundColor = GlanceTheme.colors.surfaceVariant,
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (expanded) {
                    ControlButton(
                        iconRes = R.drawable.ic_widget_previous,
                        description = "Faixa anterior",
                        action = PlayerWidgetActionReceiver.ACTION_PREVIOUS,
                    )
                }
                ControlButton(
                    iconRes = if (snapshot.isPlaying) {
                        R.drawable.ic_widget_pause
                    } else {
                        R.drawable.ic_widget_play
                    },
                    description = if (snapshot.isPlaying) "Pausar" else "Tocar",
                    action = PlayerWidgetActionReceiver.ACTION_PLAY_PAUSE,
                )
                ControlButton(
                    iconRes = R.drawable.ic_widget_next,
                    description = "Próxima faixa",
                    action = PlayerWidgetActionReceiver.ACTION_NEXT,
                )
            }
        }
    }
}

@Composable
private fun Artwork(artwork: Bitmap?, size: Dp) {
    Image(
        provider = artwork?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.ic_widget_artwork),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = GlanceModifier
            .size(size)
            .cornerRadius(16.dp),
    )
}

@Composable
private fun ControlButton(iconRes: Int, description: String, action: String) {
    val context = LocalContext.current
    val intent = Intent(context, PlayerWidgetActionReceiver::class.java).setAction(action)

    Image(
        provider = ImageProvider(iconRes),
        contentDescription = description,
        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
        modifier = GlanceModifier
            .size(40.dp)
            .padding(8.dp)
            .cornerRadius(20.dp)
            .clickable(actionSendBroadcast(intent)),
    )
}

private fun Preferences.toSnapshot(): WidgetSnapshot = WidgetSnapshot(
    title = this[PlayerWidgetKeys.title] ?: WidgetSnapshot.Empty.title,
    artist = this[PlayerWidgetKeys.artist] ?: WidgetSnapshot.Empty.artist,
    artworkUri = this[PlayerWidgetKeys.artworkUri]?.takeIf { it.isNotBlank() },
    isPlaying = this[PlayerWidgetKeys.isPlaying] ?: false,
    positionMs = this[PlayerWidgetKeys.positionMs] ?: 0L,
    durationMs = this[PlayerWidgetKeys.durationMs] ?: 0L,
)

private fun WidgetSnapshot.progress(): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
