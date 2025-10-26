package com.neptune.neptune.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/*
   Simple test to verify that GetLibraryUseCase emits items from the repository
   and updates when new items are added.
   Written with help from ChatGPT.
*/
private class RepoStub : MediaRepository {
  private val s = MutableStateFlow<List<MediaItem>>(emptyList())

  override fun observeAll(): Flow<List<MediaItem>> = s

  override suspend fun upsert(item: MediaItem) {
    s.value = s.value + item
  }
}

class GetLibraryUsecaseTest {

  @Test
  fun emitsRepositoryItemsAndUpdatesOnUpsert() {
    runTest {
      val repo = RepoStub()
      val uc = GetLibraryUseCase(repo)

      assertThat(uc().first()).isEmpty()

      repo.upsert(MediaItem(id = "1", projectUri = "file:///1.zip"))
      val after = uc().first()
      assertThat(after.map { it.id }).containsExactly("1")
    }
  }
}
