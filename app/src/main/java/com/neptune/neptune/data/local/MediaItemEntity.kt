package com.neptune.neptune.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/* Room entity representing a media item stored in the local database
 * Fields:
 * - id: Unique identifier for the media item (Primary Key)
 * - projectUri: URI of the associated .neptune project file
 * - importedAt: Timestamp indicating when the media item was imported
 *
 * */
@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val projectUri: String,                     // <-- only the .neptune file we made
    val importedAt: Long = System.currentTimeMillis()
)
