package com.neptune.neptune.ui.mock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.neptune.neptune.domain.model.MediaItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit (Robolectric) Compose tests for MockImportScreen. written partially with help from ChatGPT.
 * These run in the JVM (no device/emulator) so JaCoCo 0.8.8 still works.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Robolectric API level
class MockImportScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var vm: com.neptune.neptune.ui.picker.ImportViewModel
  private lateinit var libraryFlow: MutableStateFlow<List<MediaItem>>

  @Before
  fun setup() {
    vm = mockk(relaxed = true)
    libraryFlow = MutableStateFlow(emptyList())
    every { vm.library } returns libraryFlow as StateFlow<List<MediaItem>>
  }

  @Test
  fun topAppBarAndFabAreVisibleInEmptyState() {
    composeRule.setContent { MockImportScreen(vm = vm) }

    composeRule.onNodeWithText("Neptune • placeholder").assertIsDisplayed()
    composeRule.onNodeWithText("No projects yet.").assertIsDisplayed()
    composeRule
        .onNodeWithText(
            "Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
        .assertIsDisplayed()
    composeRule.onNodeWithText("Import audio").assertIsDisplayed()
  }

  @Test
  fun fabIsClickableDoesNotCrash() {
    composeRule.setContent { MockImportScreen(vm = vm) }

    // Just verify the FAB is present and "clickable" logically.
    // Robolectric can't actually launch SAF intents.
    composeRule.onNodeWithText("Import audio").performClick()
    composeRule.onNodeWithText("Neptune • placeholder").assertIsDisplayed()
  }

  @Test
  fun whenLibraryIsEmptyHidesEmptyText() {
    composeRule.setContent { MockImportScreen(vm = vm) }

    composeRule.onNodeWithText("No projects yet.").assertIsDisplayed()

    composeRule.runOnIdle { libraryFlow.value = listOf(mockk<MediaItem>(relaxed = true)) }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("No projects yet.").assertIsNotDisplayed()
    composeRule
        .onNodeWithText(
            "Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
        .assertIsNotDisplayed()
    composeRule.onNodeWithText("Neptune • placeholder").assertIsDisplayed()
    composeRule.onNodeWithText("Import audio").assertIsDisplayed()
  }

  @Test
  fun doesNotCallImportWithoutActivityResult() {
    composeRule.setContent { MockImportScreen(vm = vm) }

    verify(exactly = 0) { vm.importFromSaf(any()) }
  }
}
