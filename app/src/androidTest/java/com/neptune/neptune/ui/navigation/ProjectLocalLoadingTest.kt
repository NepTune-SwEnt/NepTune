package com.neptune.neptune.ui.navigation

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.Timestamp
import com.neptune.neptune.MainActivity
import com.neptune.neptune.NeptuneApp
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryVarVar
import com.neptune.neptune.model.project.TotalProjectItemsRepository
import com.neptune.neptune.model.project.TotalProjectItemsRepositoryProvider
import com.neptune.neptune.ui.sampler.SamplerTestTags
import com.neptune.neptune.ui.theme.SampleAppTheme
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LocalProjectLoadingTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  private lateinit var assetZipFile: File
  private var fakeRepository: TotalProjectItemsRepository = ProjectItemsRepositoryVarVar()

  private val ASSET_ZIP_PATH = "fakeProject.zip"
  private val TARGET_PROJECT_ID = "42"

  @Before
  fun setUp() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    assetZipFile = File(context.cacheDir, "test_${ASSET_ZIP_PATH}")
    context.assets.open(ASSET_ZIP_PATH).use { inputStream ->
      FileOutputStream(assetZipFile).use { outputStream -> inputStream.copyTo(outputStream) }
    }
    val absoluteZipPath = assetZipFile.absolutePath

    runBlocking {
      fakeRepository.addProject(
          ProjectItem(
              uid = TARGET_PROJECT_ID,
              name = "Test Project ZIP",
              filePath = absoluteZipPath,
              lastUpdated = Timestamp(100, 0)))
      TotalProjectItemsRepositoryProvider.repository = fakeRepository
    }

    composeTestRule.activity.setContent {
      SampleAppTheme { NeptuneApp(startDestination = Screen.ProjectList.route) }
    }
  }

  @Test
  fun projectClick_loadsSamplerKnobsCorrectly() {
    composeTestRule.onNodeWithTag("project_$TARGET_PROJECT_ID").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("COMP").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("4:1", substring = true).assertIsDisplayed()
  }

  @Test
  fun endToEnd_loadProject_setsCorrectAudioDuration() {
    composeTestRule.onNodeWithTag("project_$TARGET_PROJECT_ID").performClick()
    composeTestRule.mainClock.advanceTimeBy(500L)
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(SamplerTestTags.TIME_DISPLAY)
        .assertTextContains(" / 04 s", substring = true)
        .assertIsDisplayed()
  }
}
