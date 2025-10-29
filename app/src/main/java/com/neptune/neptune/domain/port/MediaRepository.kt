package com.neptune.neptune.domain.port

import com.neptune.neptune.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

/*
   Port interface for accessing media items in the app's local audio workspace.
*/
interface MediaRepository {
  fun observeAll(): Flow<List<MediaItem>>

  suspend fun upsert(item: MediaItem)
}
