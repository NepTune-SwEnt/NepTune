package com.neptune.neptune.domain.usecase

import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.MediaRepository
import kotlinx.coroutines.flow.Flow

/*
A use case for retrieving the media library from local storage as a flow of media items.
 */
class GetLibraryUseCase(private val repo: MediaRepository) {
  operator fun invoke(): Flow<List<MediaItem>> = repo.observeAll()
}
