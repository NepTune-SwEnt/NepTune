package com.neptune.neptune.screen

import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.TotalProjectItemsRepository
import com.neptune.neptune.ui.post.PostUiState
import com.neptune.neptune.ui.post.PostViewModel
import java.io.File
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * REQUIREMENTS:
 * - Firestore emulator on port 8080
 * - Auth emulator on port 9099
 * - Storage emulator on port 9199
 * - Start them: firebase emulators:start --only firestore,auth,storage
 *
 * On Android emulator, host is 10.0.2.2 This class was made using AI assistance.
 */
@RunWith(AndroidJUnit4::class)
class PostViewModelTest {

  private lateinit var viewModel: PostViewModel
  private lateinit var mockProjectRepo: TotalProjectItemsRepository
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val host = "10.0.2.2"
  private val storagePort = 9199
  private val authPort = 9099
  private val firestorePort = 8080

  @Before
  fun setUp() = runBlocking {
    // 1. Connect to Emulators
    try {
      Firebase.storage.useEmulator(host, storagePort)
      Firebase.auth.useEmulator(host, authPort)
      Firebase.firestore.useEmulator(host, firestorePort)
    } catch (e: IllegalStateException) {
      Log.e("setUp", "Error in firebase initialization: ${e.message}")
    }

    // 2. Auth
    Firebase.auth.signOut()
    Firebase.auth.signInAnonymously().await()

    // 3. Create Mock Repository
    mockProjectRepo = mock(TotalProjectItemsRepository::class.java)

    // 4. Inject into ViewModel
    viewModel = PostViewModel(projectRepository = mockProjectRepo)
  }

  @After
  fun tearDown() = runBlocking {
    val storageRef = Firebase.storage.reference
    try {
      storageRef.listAll().await().items.forEach { it.delete().await() }
    } catch (_: Exception) {}
    Firebase.auth.signOut()
  }

  private fun createDummyFile(fileName: String, content: String): Uri {
    val file = File(context.cacheDir, fileName)
    file.writeText(content)
    return Uri.fromFile(file)
  }

  @Test
  fun toggleAudienceSwitchesState() {
    val initialState = viewModel.uiState.value.sample.isPublic
    viewModel.toggleAudience()
    assertTrue(viewModel.uiState.value.sample.isPublic != initialState)
    viewModel.toggleAudience()
    assertEquals(initialState, viewModel.uiState.value.sample.isPublic)
  }

  @Test
  fun submitPostUploadsAndCompletes() = runBlocking {
    Firebase.auth.signOut()
    val uniqueEmail = "post-test-${System.currentTimeMillis()}@example.com"
    Firebase.auth.createUserWithEmailAndPassword(uniqueEmail, "password").await()

    // --- DATA ---
    val projectId = "proj_integration_test"
    val userId = Firebase.auth.currentUser?.uid ?: ""
    val zipUri = createDummyFile("project.zip", "FAKE ZIP CONTENT")

    // --- MOCK: Stub Repository response ---
    val dummyProject =
        ProjectItem(
            uid = projectId,
            name = "Integration Project",
            description = "Desc",
            projectFileLocalPath = "file:${zipUri.path}",
            tags = listOf("test"),
            ownerId = userId)
    `when`(mockProjectRepo.getProject(projectId)).thenReturn(dummyProject)

    // --- ACTION: Load ---
    viewModel.loadProject(projectId)

    // --- ASSERT: Verify loading ---
    waitForState(timeout = 2000) { it.sample.name == "Integration Project" }
    assertEquals(projectId, viewModel.uiState.value.sample.id)

    // --- ACTION: Add Image ---
    val imageUri = createDummyFile("cover.jpg", "FAKE IMAGE CONTENT")
    viewModel.onImageChanged(imageUri)
    waitForState { viewModel.localImageUri.value != null }

    // --- ACTION: Upload (Uses real Storage Emulator) ---
    viewModel.submitPost()

    assertTrue("Should be uploading", viewModel.uiState.value.isUploading)

    // --- FINAL ASSERT: Wait for real upload to finish ---
    try {
      // Increase timeout for Storage Emulator
      waitForState(timeout = 8000) { it.postComplete }
    } catch (_: TimeoutException) {
      assertFalse(
          "Timeout Upload. State: ${viewModel.uiState.value}", viewModel.uiState.value.isUploading)
    }

    assertTrue(viewModel.uiState.value.postComplete)
    assertFalse(viewModel.uiState.value.isUploading)

    // Check: Is the file really in Storage?
    val list = Firebase.storage.reference.child("sample_image").listAll().await()
    assertFalse(
        "The sample_image folder should not be empty",
        list.items.isEmpty() && list.prefixes.isEmpty())
  }

  private suspend fun waitForState(timeout: Long = 3000, condition: (PostUiState) -> Boolean) {
    val start = System.currentTimeMillis()
    while (!condition(viewModel.uiState.value)) {
      if (System.currentTimeMillis() - start > timeout) throw TimeoutException()
      delay(100)
    }
  }
}
