package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_history ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download_history WHERE folderId = :folderId ORDER BY timestamp DESC")
    fun getDownloadsByFolder(folderId: Long): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("UPDATE download_history SET title = :newTitle WHERE id = :id")
    suspend fun updateTitle(id: Long, newTitle: String)

    @Query("UPDATE download_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE download_history SET folderId = :folderId WHERE id = :id")
    suspend fun updateFolderId(id: Long, folderId: Long?)

    @Query("UPDATE download_history SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolderIdFromDownloads(folderId: Long)

    @Query("DELETE FROM download_history")
    suspend fun clearAll()
}
