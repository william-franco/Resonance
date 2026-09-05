package br.com.williamfranco.resonance.src.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Acima do limite de variáveis do SQLite, as remoções são feitas em lotes. */
private const val DELETE_CHUNK = 500

@Dao
interface SongDao {

    @Query(
        """
        SELECT s.*, (f.songId IS NOT NULL) AS isFavorite
        FROM songs s LEFT JOIN favorites f ON f.songId = s.id
        ORDER BY s.title COLLATE NOCASE ASC
        """,
    )
    fun observeAll(): Flow<List<SongWithFavorite>>

    @Query(
        """
        SELECT s.*, (f.songId IS NOT NULL) AS isFavorite
        FROM songs s LEFT JOIN favorites f ON f.songId = s.id
        WHERE s.albumId = :albumId
        ORDER BY s.trackNumber ASC, s.title COLLATE NOCASE ASC
        """,
    )
    fun observeByAlbum(albumId: Long): Flow<List<SongWithFavorite>>

    @Query(
        """
        SELECT s.*, (f.songId IS NOT NULL) AS isFavorite
        FROM songs s LEFT JOIN favorites f ON f.songId = s.id
        WHERE s.artist = :artist
        ORDER BY s.album COLLATE NOCASE ASC, s.trackNumber ASC
        """,
    )
    fun observeByArtist(artist: String): Flow<List<SongWithFavorite>>

    @Query(
        """
        SELECT s.*, 1 AS isFavorite
        FROM songs s JOIN favorites f ON f.songId = s.id
        ORDER BY s.title COLLATE NOCASE ASC
        """,
    )
    fun observeFavorites(): Flow<List<SongWithFavorite>>

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("SELECT id FROM songs")
    suspend fun allIds(): List<Long>

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM favorites WHERE songId IN (:ids)")
    suspend fun deleteFavoritesByIds(ids: List<Long>)

    @Query("DELETE FROM playlist_songs WHERE songId IN (:ids)")
    suspend fun deletePlaylistLinksByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM favorites WHERE songId = :songId")
    suspend fun favoriteCount(songId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)

    /**
     * Reconcilia o banco com o resultado do scan: grava o que existe e remove o que sumiu,
     * derrubando junto os vínculos órfãos de playlist e favorito.
     */
    @Transaction
    suspend fun sync(scanned: List<SongEntity>) {
        val scannedIds = scanned.mapTo(HashSet()) { it.id }
        val removed = allIds().filterNot { it in scannedIds }
        removed.chunked(DELETE_CHUNK).forEach { chunk ->
            deletePlaylistLinksByIds(chunk)
            deleteFavoritesByIds(chunk)
            deleteByIds(chunk)
        }
        if (scanned.isNotEmpty()) {
            upsertAll(scanned)
        }
    }

    @Transaction
    suspend fun toggleFavorite(songId: Long) {
        if (favoriteCount(songId) > 0) removeFavorite(songId) else addFavorite(FavoriteEntity(songId))
    }
}

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT p.id AS id,
               p.name AS name,
               (SELECT COUNT(*) FROM playlist_songs ps WHERE ps.playlistId = p.id) AS songCount,
               (
                   SELECT s.artworkUri FROM playlist_songs ps
                   JOIN songs s ON s.id = ps.songId
                   WHERE ps.playlistId = p.id AND s.artworkUri IS NOT NULL
                   ORDER BY ps.position ASC LIMIT 1
               ) AS artworkUri
        FROM playlists p
        ORDER BY p.name COLLATE NOCASE ASC
        """,
    )
    fun observeAll(): Flow<List<PlaylistSummary>>

    @Query(
        """
        SELECT s.*, (f.songId IS NOT NULL) AS isFavorite
        FROM playlist_songs ps
        JOIN songs s ON s.id = ps.songId
        LEFT JOIN favorites f ON f.songId = s.id
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
        """,
    )
    fun observeSongs(playlistId: Long): Flow<List<SongWithFavorite>>

    @Query("SELECT name FROM playlists WHERE id = :playlistId")
    suspend fun nameOf(playlistId: Long): String?

    @Insert
    suspend fun create(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun delete(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun lastPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addLinks(links: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: Long)

    @Transaction
    suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        var position = lastPosition(playlistId)
        val links = songIds.map { songId ->
            position += 1
            PlaylistSongEntity(playlistId = playlistId, songId = songId, position = position)
        }
        addLinks(links)
    }
}
