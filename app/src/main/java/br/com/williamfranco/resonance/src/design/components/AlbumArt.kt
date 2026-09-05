package br.com.williamfranco.resonance.src.design.components

import android.content.Context
import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import br.com.williamfranco.resonance.src.services.library.loadArtworkBitmap

/**
 * Capa com marca d'água por baixo: quando o arquivo não tem arte, o ícone continua visível.
 * As imagens vêm do [ArtworkCache], então rolar a lista não redecodifica nada.
 */
@Composable
fun AlbumArt(
    artworkUri: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    decodeSize: Int = 192,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val cached = ArtworkCache.peek(artworkUri, decodeSize)

    val artwork by produceState(initialValue = cached, artworkUri, decodeSize) {
        value = cached ?: ArtworkCache.load(context, artworkUri, decodeSize)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(0.4f),
        )

        Crossfade(targetState = artwork, label = "albumArt") { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * Cache de capas em memória, limitado por bytes. Substitui uma biblioteca de imagens
 * porque o app só precisa carregar `content://` locais em poucos tamanhos fixos.
 */
private object ArtworkCache {

    private const val MAX_BYTES = 12 * 1024 * 1024

    private val cache = object : LruCache<String, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun peek(artworkUri: String?, decodeSize: Int): ImageBitmap? {
        if (artworkUri.isNullOrBlank()) return null
        return cache[keyOf(artworkUri, decodeSize)]
    }

    suspend fun load(context: Context, artworkUri: String?, decodeSize: Int): ImageBitmap? {
        if (artworkUri.isNullOrBlank()) return null
        val key = keyOf(artworkUri, decodeSize)
        cache[key]?.let { return it }

        val bitmap = loadArtworkBitmap(context, artworkUri, decodeSize)?.asImageBitmap()
        if (bitmap != null) cache.put(key, bitmap)
        return bitmap
    }

    private fun keyOf(artworkUri: String, decodeSize: Int) = "$artworkUri@$decodeSize"
}
