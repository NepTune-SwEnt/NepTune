package com.neptune.neptune.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MediaItemEntity::class],
  version = 2, exportSchema = true)

abstract class MediaDb : RoomDatabase() {
  abstract fun mediaDao(): MediaDao
}
