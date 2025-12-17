package com.neptune.neptune.data

import com.neptune.neptune.data.local.MediaDao
import com.neptune.neptune.data.local.MediaItemEntity
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of MediaRepository that uses a MediaDao to access media items in local storage.
 *
 * @param dao The MediaDao to use for accessing media items. Partially written with ChatGPT
 */
class MediaRepositoryImpl(private val dao: MediaDao) : MediaRepository {
  override fun observeAll(): Flow<List<MediaItem>> =
      dao.observeAll().map { it.map(MediaRepositoryImpl::toDomain) }

  override suspend fun upsert(item: MediaItem) = dao.upsert(toEntity(item))

  override suspend fun delete(item: MediaItem) {
    dao.delete(toEntity(item))
  }

  private companion object {
    // Convert MediaItemEntity to MediaItem
    fun toDomain(e: MediaItemEntity) = MediaItem(id = e.id, e.projectUri)

    fun toEntity(m: MediaItem) = MediaItemEntity(m.id, m.projectUri)
  }
}
