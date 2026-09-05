package br.com.williamfranco.resonance.src.design.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import br.com.williamfranco.resonance.src.features.settings.models.ColorSource
import br.com.williamfranco.resonance.src.features.settings.models.ThemeMode

private const val COLOR_ANIMATION_MS = 600

@Composable
fun ResonanceTheme(
    themeMode: ThemeMode = ThemeMode.SISTEMA,
    colorSource: ColorSource = ColorSource.CAPA_DO_ALBUM,
    seedColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.CLARO -> false
        ThemeMode.ESCURO -> true
        ThemeMode.SISTEMA -> isSystemInDarkTheme()
    }
    val context = LocalContext.current

    val target = when {
        colorSource == ColorSource.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        colorSource == ColorSource.CAPA_DO_ALBUM && seedColor != null ->
            colorSchemeFromSeed(seedColor, dark)

        else -> colorSchemeFromSeed(ResonanceSeed, dark)
    }

    MaterialTheme(
        colorScheme = target.animated(),
        typography = Typography,
        content = content,
    )
}

/**
 * Interpola os tokens visíveis do esquema para que a troca de capa não corte a cor
 * bruscamente. Os tokens restantes acompanham o esquema alvo diretamente.
 */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    @Composable
    fun animate(color: Color, label: String): Color {
        val animated by animateColorAsState(
            targetValue = color,
            animationSpec = tween(COLOR_ANIMATION_MS),
            label = label,
        )
        return animated
    }

    return copy(
        primary = animate(primary, "primary"),
        onPrimary = animate(onPrimary, "onPrimary"),
        primaryContainer = animate(primaryContainer, "primaryContainer"),
        onPrimaryContainer = animate(onPrimaryContainer, "onPrimaryContainer"),
        secondary = animate(secondary, "secondary"),
        secondaryContainer = animate(secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = animate(onSecondaryContainer, "onSecondaryContainer"),
        tertiary = animate(tertiary, "tertiary"),
        tertiaryContainer = animate(tertiaryContainer, "tertiaryContainer"),
        background = animate(background, "background"),
        onBackground = animate(onBackground, "onBackground"),
        surface = animate(surface, "surface"),
        onSurface = animate(onSurface, "onSurface"),
        surfaceVariant = animate(surfaceVariant, "surfaceVariant"),
        onSurfaceVariant = animate(onSurfaceVariant, "onSurfaceVariant"),
        surfaceContainerLow = animate(surfaceContainerLow, "surfaceContainerLow"),
        surfaceContainer = animate(surfaceContainer, "surfaceContainer"),
        surfaceContainerHigh = animate(surfaceContainerHigh, "surfaceContainerHigh"),
        surfaceContainerHighest = animate(surfaceContainerHighest, "surfaceContainerHighest"),
        outline = animate(outline, "outline"),
        outlineVariant = animate(outlineVariant, "outlineVariant"),
    )
}
