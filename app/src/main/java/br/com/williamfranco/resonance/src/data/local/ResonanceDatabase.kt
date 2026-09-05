package br.com.williamfranco.resonance.src.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.williamfranco.resonance.src.services.Constants

@Database(
    entities = [
        SongEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ResonanceDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao

    abstract fun playlistDao(): PlaylistDao

    companion object {
        fun build(context: Context): ResonanceDatabase =
            Room.databaseBuilder(context, ResonanceDatabase::class.java, Constants.DATABASE_NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
