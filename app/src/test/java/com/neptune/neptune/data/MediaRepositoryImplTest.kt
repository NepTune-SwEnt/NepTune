package com.neptune.neptune.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.data.local.MediaDb
import com.neptune.neptune.data.local.MediaItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaRepositoryImplTest {
  private lateinit var db: MediaDb
  private lateinit var repo: MediaRepositoryImpl

  @Before
  fun setUp() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(ctx, MediaDb::class.java).allowMainThreadQueries().build()
    repo = MediaRepositoryImpl(db.mediaDao())
  }

  @After fun tearDown() = db.close()

  @Test
  fun upsert_maps_domain_to_entity_and_observe_maps_back() = runBlocking {
    repo.upsert(com.neptune.neptune.domain.model.MediaItem("id-1", "file:///x.zip"))
    val fromDb = db.mediaDao().observeAll().first()
    Assert.assertEquals(
        listOf(MediaItemEntity("id-1", "file:///x.zip", fromDb.first().importedAt)), fromDb)
    val domain = repo.observeAll().first()
    Assert.assertEquals("id-1", domain.first().id)
    Assert.assertEquals("file:///x.zip", domain.first().projectUri)
  }
}
