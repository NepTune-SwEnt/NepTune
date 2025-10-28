package com.neptune.neptune.ui.navigation

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.neptune.neptune.MainActivity
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepository
import com.neptune.neptune.model.project.ProjectItemsRepositoryVar
import com.neptune.neptune.ui.theme.SampleAppTheme
import com.neptune.neptune.ui.sampler.SamplerTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.core.net.toUri
import com.google.firebase.Timestamp
import com.neptune.neptune.NeptuneApp
import com.neptune.neptune.model.project.ProjectItemsRepositoryProvider
import com.neptune.neptune.ui.projectlist.ProjectListScreen
import com.neptune.neptune.ui.projectlist.ProjectListViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue


class FakeLoadingRepository(
    private val zipFile: File
) : ProjectItemsRepository by ProjectItemsRepositoryVar() {

    override suspend fun getAllProjects(): List<ProjectItem> {
        return listOf(
            ProjectItem(
                id = "42",
                name = "Test Project ZIP",
                filePath = zipFile.path,
                lastUpdated = Timestamp(100, 0)
            )
        )
    }
}

class LocalProjectLoadingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var assetZipFile: File
    private lateinit var fakeRepository: FakeLoadingRepository

    private val ASSET_ZIP_PATH = "fakeProject.zip"
    private val TARGET_PROJECT_ID = "42"

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assetZipFile = File(context.cacheDir, "test_${ASSET_ZIP_PATH}")
        context.assets.open(ASSET_ZIP_PATH).use { inputStream ->
            FileOutputStream(assetZipFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        val absoluteZipPath = assetZipFile.absolutePath


        runBlocking {
            ProjectItemsRepositoryProvider.repository.addProject(
                ProjectItem(
                    id = TARGET_PROJECT_ID,
                    name = "Test Project ZIP",
                    filePath = absoluteZipPath,
                    lastUpdated = Timestamp(100, 0)
                )
            )
        }


        composeTestRule.activity.setContent {
            SampleAppTheme {
                NeptuneApp(startDestination = Screen.ProjectList.route)
            }
        }


    }

    private fun openSection(title: String) {
        val tag = "${title.replace(" ", "")}ClickableHeader"
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.waitForIdle()
    }



    @Test
    fun endToEnd_projectClick_loadsSamplerKnobsCorrectly() {
        composeTestRule.onNodeWithTag("project_$TARGET_PROJECT_ID")
            .assertIsDisplayed()

        composeTestRule.waitForIdle()

        composeTestRule.mainClock.advanceTimeBy(500L)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_ATTACK)
            .assertTextContains("0.35s")

        composeTestRule.onNodeWithText("ADSR ENVELOPE CONTROLS").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_SUSTAIN)
            .assertTextContains("60%")

        composeTestRule.onNodeWithText("COMP").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SamplerTestTags.INPUT_COMP_RATIO)
            .assertTextContains("4:1")

        composeTestRule.onNodeWithText("-10.00").assertIsDisplayed()
    }
}