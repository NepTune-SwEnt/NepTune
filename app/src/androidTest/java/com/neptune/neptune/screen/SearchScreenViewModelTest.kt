package com.neptune.neptune.screen

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepositoryFirebase
import com.neptune.neptune.ui.search.SearchViewModel
import java.util.UUID
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/*
    Tests for SearchViewModel's loadSamplesFromFirebase() functionality.
    Written with assistance from ChatGPT.
*/
@RunWith(AndroidJUnit4::class)
class SearchViewModelTest {

  private lateinit var auth: FirebaseAuth
  private lateinit var firestore: FirebaseFirestore
  private lateinit var storage: FirebaseStorage
  private lateinit var context: Context

  private val emulatorHost = "10.0.2.2"
  private val timeOut: Long = 5000

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()

    auth = FirebaseAuth.getInstance()
    firestore = FirebaseFirestore.getInstance()
    storage = FirebaseStorage.getInstance()

    try {
      auth.useEmulator(emulatorHost, 9099)
      firestore.useEmulator(emulatorHost, 8080)
      storage.useEmulator(emulatorHost, 9199)
    } catch (_: IllegalStateException) {}

    val settings = FirebaseFirestoreSettings.Builder().build()
    firestore.firestoreSettings = settings
  }

  @Test
  fun integrationLoadSamplesAndIncrementalUpdate() = runBlocking {
    clearFirestoreSamples()

    val audioPath = "test_samples/audio_${UUID.randomUUID()}.mp3"
    val storageRef = storage.reference.child(audioPath)
    storageRef.putBytes(byteArrayOf(0, 1, 2, 3)).await()

    val realRepo = SampleRepositoryFirebase(firestore)

    val realStorageService = StorageService(storage)

    val viewModel =
        SearchViewModel(
            repo = realRepo,
            context = context,
            useMockData = false,
            profileRepo = ProfileRepositoryProvider.repository,
            explicitStorageService = realStorageService,
            explicitDownloadsFolder = null,
            auth = auth)

    val sampleId1 = "id_${UUID.randomUUID()}"
    val sample1 =
        Sample(
            id = sampleId1,
            name = "Integration Test Sample 1",
            description = "Created by emulator test",
            durationSeconds = 10,
            tags = listOf("#test"),
            likes = 0,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            ownerId = "tester",
            storagePreviewSamplePath = audioPath)
    realRepo.addSample(sample1)

    var samples = withTimeout(timeOut) { viewModel.samples.filter { it.isNotEmpty() }.first() }
    assertEquals("The sample must be loaded", 1, samples.size)
    assertEquals("The name must match", sample1.name, samples[0].name)

    val loadedResource =
        withTimeout(timeOut) {
          viewModel.sampleResources
              .filter { resourcesMap -> resourcesMap[sampleId1]?.audioUrl != null }
              .first()[sampleId1]
        }

    assertTrue("Resources need to be loaded", loadedResource != null)
    assertTrue("The audio URL must not be null", loadedResource?.audioUrl?.contains("9199") == true)

    val sampleId2 = "id_${UUID.randomUUID()}"
    val sample2 = sample1.copy(id = sampleId2, name = "Integration Test Sample 2")

    realRepo.addSample(sample2)

    samples = withTimeout(timeOut) { viewModel.samples.filter { it.size >= 2 }.first() }
    assertEquals("There should be 2 samples now", 2, samples.size)
    assertTrue("Sample 2 must be present", samples.any { it.id == sampleId2 })
  }

  private suspend fun clearFirestoreSamples() {
    val snapshot = firestore.collection("samples").get().await()
    for (doc in snapshot.documents) {
      firestore.collection("samples").document(doc.id).delete().await()
    }
  }
}
