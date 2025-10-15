package com.neptune.neptune.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaDaoTest {
  private lateinit var db: MediaDb
  private lateinit var dao: MediaDao

  @Before
  fun setUp() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(ctx, MediaDb::class.java).allowMainThreadQueries().build()
    dao = db.mediaDao()
  }

  @After fun tearDown() = db.close()

  @Test
  fun upsert_and_observe_order_desc_by_importedAt() = runBlocking {
    dao.upsert(MediaItemEntity(id = "a", projectUri = "file:///a.zip", importedAt = 1))
    dao.upsert(MediaItemEntity(id = "b", projectUri = "file:///b.zip", importedAt = 3))
    dao.upsert(MediaItemEntity(id = "c", projectUri = "file:///c.zip", importedAt = 2))
    val items = dao.observeAll().first()
    Assert.assertEquals(listOf("b", "c", "a"), items.map { it.id })
  }
}
