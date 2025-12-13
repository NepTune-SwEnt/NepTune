package com.neptune.neptune.data.local

import androidx.room.*
import androidx.room.Dao
import kotlinx.coroutines.flow.Flow

/* DataAccessObject interface for accessing media items in the local database
 * using Room persistence library
 * Methods:
 * - upsert(item: MediaItemEntity): Inserts or updates a media item in the
 *       database. If a media item with the same ID already exists, it will be
 *       replaced.
 * - observeAll(): Returns a Flow that emits a list of all media items,
 *       ordered by import date in descending order
 */
@Dao
interface MediaDao {
  @Query("SELECT * FROM media_items ORDER BY importedAt DESC")
  fun observeAll(): Flow<List<MediaItemEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: MediaItemEntity)

  @Delete suspend fun delete(item: MediaItemEntity)
}
