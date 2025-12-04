package com.neptune.neptune.epic

import androidx.activity.compose.setContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.MainActivity
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.NeptuneApp
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryVarVar
import com.neptune.neptune.model.project.TotalProjectItemsRepositoryProvider
import com.neptune.neptune.ui.navigation.NavigationTestTags
import com.neptune.neptune.ui.sampler.SamplerTestTags
import com.neptune.neptune.ui.sampler.SamplerViewModel
import com.neptune.neptune.ui.theme.SampleAppTheme
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EpicProjectE2ETest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  private lateinit var assetZipFile: File
  private val TARGET_PROJECT_ID = "42"
  private val ASSET_ZIP_PATH = "fakeProject.zip"

  private fun openSection(title: String) {
    val tag = "${title.replace(" ", "")}ClickableHeader"
    composeTestRule.onNodeWithTag(tag).performClick()
    composeTestRule.waitForIdle()
  }

  @Before
  fun setup() {
    val context = NepTuneApplication.appContext
    assetZipFile = File(context.cacheDir, "test_$ASSET_ZIP_PATH")
    context.assets.open(ASSET_ZIP_PATH).use { input ->
      FileOutputStream(assetZipFile).use { output -> input.copyTo(output) }
    }

    val fakeRepository = ProjectItemsRepositoryVarVar()
    runBlocking {
      fakeRepository.addProject(
          ProjectItem(
              uid = TARGET_PROJECT_ID,
              name = "Test Project ZIP",
              projectFileLocalPath = assetZipFile.absolutePath,
              lastUpdated = Timestamp(100, 0)))
      TotalProjectItemsRepositoryProvider.repository = fakeRepository
    }

    val host = "10.0.2.2"
    val authPort = 9099
    val firestorePort = 8080
    val auth = FirebaseAuth.getInstance()
    runCatching { auth.useEmulator(host, authPort) }
    runCatching { FirebaseFirestore.getInstance().useEmulator(host, firestorePort) }
    runCatching { auth.signOut() }
    val signInTask = auth.signInAnonymously()
    Tasks.await(signInTask, 10, java.util.concurrent.TimeUnit.SECONDS)
    assertTrue("Firebase anonymous sign-in failed", signInTask.isSuccessful)

    composeTestRule.activity.setContent { SampleAppTheme { NeptuneApp() } }
    composeTestRule.waitForIdle()
  }

  @Test
  fun epicFlow_modifyViaUI_save_reload_verifyValues() = runBlocking {
    // --- NAVIGATE TO PROJECTLIST ---
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).performClick()
    composeTestRule.waitForIdle()

    // Wait for project item to appear
    composeTestRule.waitUntil(5000L) {
      composeTestRule
          .onAllNodesWithTag("project_$TARGET_PROJECT_ID")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Open project
    composeTestRule.onNodeWithTag("project_$TARGET_PROJECT_ID").performClick()
    composeTestRule.waitForIdle()

    delay(500)

    // --- INTERACT WITH REAL UI ---

    openSection("ADSR Envelope Controls")
    // Set attack with knob
    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_ATTACK).performTouchInput {
      swipe(start = center, end = center + Offset(x = 0f, y = -100f), durationMillis = 50)
    }

    // Set sustain
    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_SUSTAIN).performTouchInput {
      swipe(start = center, end = center + Offset(x = 0f, y = -100f), durationMillis = 50)
    }

    // --- SAVE WITH REAL BUTTON ---
    composeTestRule.onNodeWithContentDescription("Save").performClick()
    composeTestRule.waitForIdle()

    delay(2000)

    // --- RETURN TO HOME ---
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.waitForIdle()

    // --- REOPEN ---
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("project_$TARGET_PROJECT_ID").performClick()

    composeTestRule.waitForIdle()

    delay(10000)

    val vmReload = SamplerViewModel()
    vmReload.loadProjectData(assetZipFile.absolutePath)
    delay(5000)

    val reload = vmReload.uiState.value

    // --- VERIFY PERSISTED VALUES ---
    assertTrue("Attack was not changed!", reload.attack != 0.toFloat())
    assertTrue("Sustain was not changed!", reload.sustain != 1.toFloat())
  }
}
