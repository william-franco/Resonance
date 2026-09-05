package br.com.williamfranco.resonance.src.services.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodifica a capa de um `content://` já reduzida ao tamanho necessário.
 * Usada pela extração de cor dominante e pelo widget, que não podem depender do Coil.
 */
suspend fun loadArtworkBitmap(
    context: Context,
    artworkUri: String?,
    maxSize: Int = 256,
): Bitmap? = withContext(Dispatchers.IO) {
    if (artworkUri.isNullOrBlank()) return@withContext null
    runCatching {
        val bytes = context.contentResolver.openInputStream(artworkUri.toUri())
            ?.use { it.readBytes() }
            ?: return@runCatching null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSize)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }.getOrNull()
}

internal fun sampleSizeFor(width: Int, height: Int, maxSize: Int): Int {
    if (width <= 0 || height <= 0 || maxSize <= 0) return 1
    var sampleSize = 1
    while (width / (sampleSize * 2) >= maxSize && height / (sampleSize * 2) >= maxSize) {
        sampleSize *= 2
    }
    return sampleSize
}
