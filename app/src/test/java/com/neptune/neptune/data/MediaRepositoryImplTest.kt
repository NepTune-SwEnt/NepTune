package com.neptune.neptune.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.neptune.neptune.data.local.MediaDb
import com.neptune.neptune.domain.model.MediaItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
/*
    Tests that MediaRepositoryImpl correctly maps between domain and database models,
    and that upsert and observeAll work as expected.
    Uses an in-memory Room database for testing.
    Written with help from ChatGPT.
 */
@RunWith(RobolectricTestRunner::class)
class MediaRepositoryImplTest {

  private lateinit var db: MediaDb
  private lateinit var repo: MediaRepositoryImpl

  @Before
  fun setUp() {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(ctx, MediaDb::class.java).allowMainThreadQueries().build()
    repo = MediaRepositoryImpl(db.mediaDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun upsert_and_observe_maps_domain_and_emits() = runBlocking {
    val dom = MediaItem(id = "x", projectUri = "file:///x.zip")
    repo.upsert(dom)

    val list = repo.observeAll().first()
    assertThat(list).hasSize(1)
    assertThat(list.first().id).isEqualTo("x")
    assertThat(list.first().projectUri).isEqualTo("file:///x.zip")
  }

  @Test
  fun upsert_with_same_id_replaces_existing() = runBlocking {
    repo.upsert(MediaItem("a", "file:///1.zip"))
    repo.upsert(MediaItem("a", "file:///2.zip"))

    val list = repo.observeAll().first()
    assertThat(list).hasSize(1)
    assertThat(list.first().projectUri).isEqualTo("file:///2.zip")
  }
}
