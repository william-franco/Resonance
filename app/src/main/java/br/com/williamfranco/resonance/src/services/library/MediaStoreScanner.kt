package br.com.williamfranco.resonance.src.services.library

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import br.com.williamfranco.resonance.src.data.local.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ALBUM_ART_BASE_URI = "content://media/external/audio/albumart".toUri()

/** Antes do Android 13 o acesso ao áudio ainda passa pela permissão de armazenamento. */
fun audioPermission(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Manifest.permission.READ_MEDIA_AUDIO
} else {
    Manifest.permission.READ_EXTERNAL_STORAGE
}

class MediaStoreScanner(private val context: Context) {

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(context, requiredPermission()) ==
        PackageManager.PERMISSION_GRANTED

    fun requiredPermission(): String = audioPermission()

    suspend fun scan(minDurationMs: Long): List<SongEntity> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.coerceAtLeast(0L).toString())
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val cursor = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        ) ?: return@withContext emptyList()

        cursor.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            buildList(it.count) {
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val albumId = it.getLong(albumIdColumn)
                    add(
                        SongEntity(
                            id = id,
                            title = it.getString(titleColumn) ?: "Faixa desconhecida",
                            artist = it.getString(artistColumn)?.takeUnless(::isUnknownTag)
                                ?: "Artista desconhecido",
                            album = it.getString(albumColumn)?.takeUnless(::isUnknownTag)
                                ?: "Álbum desconhecido",
                            albumId = albumId,
                            durationMs = it.getLong(durationColumn),
                            uri = ContentUris.withAppendedId(collection, id).toString(),
                            artworkUri = ContentUris.withAppendedId(ALBUM_ART_BASE_URI, albumId).toString(),
                            // O MediaStore codifica o disco no milhar: 1004 é a faixa 4 do disco 1.
                            trackNumber = it.getInt(trackColumn) % 1000,
                            year = it.getInt(yearColumn),
                            dateAddedSeconds = it.getLong(dateAddedColumn),
                        ),
                    )
                }
            }
        }
    }

    private fun isUnknownTag(value: String): Boolean =
        value == MediaStore.UNKNOWN_STRING || value.isBlank()
}
