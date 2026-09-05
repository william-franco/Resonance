package br.com.williamfranco.resonance.src.design.theme

import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import br.com.williamfranco.resonance.src.services.library.loadArtworkBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Observa a capa em reprodução e devolve a cor semente usada pelo [ResonanceTheme].
 * Enquanto a extração não termina, mantém a cor anterior para evitar piscadas.
 */
@Composable
fun rememberSeedColor(artworkUri: String?, enabled: Boolean): Color? {
    val context = LocalContext.current
    var seed by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(artworkUri, enabled) {
        if (!enabled) {
            seed = null
            return@LaunchedEffect
        }
        val bitmap = loadArtworkBitmap(context, artworkUri, maxSize = 128)
        seed = bitmap?.let { extractSeedColor(it) }
    }

    return seed
}

/**
 * Escolhe o swatch mais expressivo da capa: prioriza saturação alta sem abrir mão
 * da representatividade, para não terminar em um tom quase cinza.
 */
suspend fun extractSeedColor(bitmap: Bitmap): Color? = withContext(Dispatchers.Default) {
    runCatching {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        val swatches = palette.swatches
        if (swatches.isEmpty()) return@runCatching null

        val maxPopulation = swatches.maxOf { it.population }.coerceAtLeast(1)
        val best = swatches.maxByOrNull { swatch ->
            val hsl = swatch.hsl
            val saturation = hsl[1]
            val lightness = hsl[2]
            val populationScore = swatch.population.toFloat() / maxPopulation
            // Penaliza tons quase pretos ou quase brancos, que geram esquemas sem contraste.
            val lightnessScore = 1f - (kotlin.math.abs(lightness - 0.5f) * 1.6f).coerceIn(0f, 1f)
            saturation * 0.6f + lightnessScore * 0.25f + populationScore * 0.15f
        } ?: return@runCatching null

        Color(best.rgb)
    }.getOrNull()
}

/**
 * Constrói um [ColorScheme] Material 3 a partir de uma única cor semente, gerando
 * rampas tonais em HSL. Substitui a material-color-utilities sem dependências extras.
 */
fun colorSchemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)

    val hue = hsl[0]
    val chroma = hsl[1].coerceIn(0.32f, 0.85f)
    val tertiaryHue = (hue + 60f) % 360f

    fun tone(t: Int, saturation: Float, h: Float = hue): Color {
        val components = floatArrayOf(h, saturation.coerceIn(0f, 1f), (t / 100f).coerceIn(0f, 1f))
        return Color(ColorUtils.HSLToColor(components))
    }

    fun accent(t: Int) = tone(t, chroma)
    fun secondary(t: Int) = tone(t, chroma * 0.42f)
    fun tertiary(t: Int) = tone(t, chroma * 0.55f, tertiaryHue)
    fun neutral(t: Int) = tone(t, 0.05f)
    fun neutralVariant(t: Int) = tone(t, 0.12f)

    return if (dark) {
        darkColorScheme(
            primary = accent(80),
            onPrimary = accent(20),
            primaryContainer = accent(30),
            onPrimaryContainer = accent(90),
            inversePrimary = accent(40),
            secondary = secondary(80),
            onSecondary = secondary(20),
            secondaryContainer = secondary(30),
            onSecondaryContainer = secondary(90),
            tertiary = tertiary(80),
            onTertiary = tertiary(20),
            tertiaryContainer = tertiary(30),
            onTertiaryContainer = tertiary(90),
            background = neutral(6),
            onBackground = neutral(90),
            surface = neutral(6),
            onSurface = neutral(90),
            surfaceVariant = neutralVariant(30),
            onSurfaceVariant = neutralVariant(80),
            surfaceTint = accent(80),
            inverseSurface = neutral(90),
            inverseOnSurface = neutral(20),
            surfaceDim = neutral(6),
            surfaceBright = neutral(24),
            surfaceContainerLowest = neutral(4),
            surfaceContainerLow = neutral(10),
            surfaceContainer = neutral(12),
            surfaceContainerHigh = neutral(17),
            surfaceContainerHighest = neutral(22),
            outline = neutralVariant(60),
            outlineVariant = neutralVariant(30),
            error = ErrorDark,
            onError = OnErrorDark,
            errorContainer = ErrorContainerDark,
            onErrorContainer = OnErrorContainerDark,
            scrim = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = accent(40),
            onPrimary = accent(100),
            primaryContainer = accent(90),
            onPrimaryContainer = accent(10),
            inversePrimary = accent(80),
            secondary = secondary(40),
            onSecondary = secondary(100),
            secondaryContainer = secondary(90),
            onSecondaryContainer = secondary(10),
            tertiary = tertiary(40),
            onTertiary = tertiary(100),
            tertiaryContainer = tertiary(90),
            onTertiaryContainer = tertiary(10),
            background = neutral(98),
            onBackground = neutral(10),
            surface = neutral(98),
            onSurface = neutral(10),
            surfaceVariant = neutralVariant(92),
            onSurfaceVariant = neutralVariant(30),
            surfaceTint = accent(40),
            inverseSurface = neutral(20),
            inverseOnSurface = neutral(95),
            surfaceDim = neutral(87),
            surfaceBright = neutral(98),
            surfaceContainerLowest = neutral(100),
            surfaceContainerLow = neutral(96),
            surfaceContainer = neutral(94),
            surfaceContainerHigh = neutral(92),
            surfaceContainerHighest = neutral(90),
            outline = neutralVariant(50),
            outlineVariant = neutralVariant(80),
            error = ErrorLight,
            onError = OnErrorLight,
            errorContainer = ErrorContainerLight,
            onErrorContainer = OnErrorContainerLight,
            scrim = Color.Black,
        )
    }
}
