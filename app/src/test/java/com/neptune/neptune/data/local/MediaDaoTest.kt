package com.neptune.neptune.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaDaoTest {

  private lateinit var db: MediaDb
  private lateinit var dao: MediaDao

  @Before
  fun setUp() {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(ctx, MediaDb::class.java).allowMainThreadQueries().build()
    dao = db.mediaDao()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun order_is_desc_by_importedAt() = runBlocking {
    dao.upsert(MediaItemEntity("a", "file:///a.zip", importedAt = 1))
    dao.upsert(MediaItemEntity("b", "file:///b.zip", importedAt = 3))
    dao.upsert(MediaItemEntity("c", "file:///c.zip", importedAt = 2))

    val ids = dao.observeAll().first().map { it.id }
    assertThat(ids).isEqualTo(listOf("b", "c", "a"))
  }

  @Test
  fun upsert_replaces_same_id() = runBlocking {
    dao.upsert(MediaItemEntity("same", "file:///1.zip", 1))
    dao.upsert(MediaItemEntity("same", "file:///2.zip", 2))
    val list = dao.observeAll().first()
    assertThat(list).hasSize(1)
    assertThat(list.first().projectUri).isEqualTo("file:///2.zip")
  }

  @Test
  fun insert_replace_and_order_by_importedAt_desc() = runBlocking {
    // insert two, older first
    dao.upsert(MediaItemEntity("a", "file:///a.zip", importedAt = 10L))
    dao.upsert(MediaItemEntity("b", "file:///b.zip", importedAt = 20L))

    // initial order by importedAt DESC -> b then a
    val first = dao.observeAll().first()
    assertThat(first.map { it.id }).containsExactly("b", "a").inOrder()

    // replace 'a' with newer timestamp -> should jump to the top
    dao.upsert(MediaItemEntity("a", "file:///a2.zip", importedAt = 99L))

    val second = dao.observeAll().first()
    assertThat(second.map { it.id }).containsExactly("a", "b").inOrder()
    assertThat(second.first().projectUri).isEqualTo("file:///a2.zip")
  }
}
