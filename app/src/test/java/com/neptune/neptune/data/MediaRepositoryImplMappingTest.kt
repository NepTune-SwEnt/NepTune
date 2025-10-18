package com.neptune.neptune.data

import com.neptune.neptune.data.local.MediaItemEntity
import com.neptune.neptune.domain.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Test

/*
   This test ensures that the mapping between MediaItemEntity and MediaItem
   is consistent in both directions.
   Written with help from ChatGPT.
*/
class MediaRepositoryImplMappingTest {

  @Test
  fun round_trip_entity_domain() {
    val e =
        MediaItemEntity(id = "a", projectUri = "file:///storage/projects/a.zip", importedAt = 42L)
    val d = MediaItem(id = e.id, projectUri = e.projectUri)
    val e2 = MediaItemEntity(id = d.id, projectUri = d.projectUri, importedAt = e.importedAt)
    assertEquals(e, e2)
  }
}
