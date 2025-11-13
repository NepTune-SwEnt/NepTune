package com.neptune.neptune.ui.sampler

import android.app.Application
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.platform.app.InstrumentationRegistry
import com.neptune.neptune.MainActivity
import com.neptune.neptune.screen.FakeSamplerViewModel // Assumé que FakeSamplerViewModel est ici
import com.neptune.neptune.ui.theme.SampleAppTheme
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SamplerViewModelFactory(
    private val viewModel: FakeSamplerViewModel,
    private val application: Application
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(SamplerViewModel::class.java)) {
      return viewModel as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}

class InitialSetupDialogTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  private lateinit var fakeViewModel: FakeSamplerViewModel
  private val PLAY_BUTTON_DESC = "Play"

  // --- HELPER : Swipe Atomique pour le Pitch ---
  // Simule un mouvement rapide pour déclencher UNE SEULE incrémentation
  private fun simulatePitchFlick(tag: String, distanceY: Float) {
    composeTestRule.onNodeWithTag(tag).performTouchInput {
      // Utilise une distance Y de 30px (suffisante pour détecter le drag > 20)
      // Le Fling est plus fiable pour un seul événement sur detectVerticalDragGestures
      swipeWithVelocity(
          start = center, end = center + Offset(x = 0f, y = distanceY), endVelocity = 4000f)
    }
  }

  @Before
  fun setup() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val application = context.applicationContext as Application
    fakeViewModel = FakeSamplerViewModel()
    val factory = SamplerViewModelFactory(fakeViewModel, application)

    composeTestRule.activity.setContent {
      SampleAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
          SamplerScreen(viewModel = viewModel(factory = factory), zipFilePath = null)
        }
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun initialSetupDialog_showsFieldsAndConfirmButton() {
    // Force l'état d'ouverture du dialogue
    fakeViewModel.mutableUiState.update { it.copy(showInitialSetupDialog = true) }
    composeTestRule.waitForIdle()

    // Vérifie que le dialog est visible
    composeTestRule.onNodeWithText("Setup required").assertIsDisplayed()
    composeTestRule.onNodeWithText("Define the project pitch and tempo").assertIsDisplayed()

    // Vérifie que les champs existent
    composeTestRule.onNodeWithTag(SamplerTestTags.INIT_TEMPO_SELECTOR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.INIT_PITCH_SELECTOR).assertIsDisplayed()

    composeTestRule.onNodeWithText("Confirm").assertIsDisplayed()
  }

  @Test
  fun initialSetupDialog_updateTempoAndConfirm() {
    // Force l'état d'ouverture du dialogue
    fakeViewModel.mutableUiState.update { it.copy(showInitialSetupDialog = true, inputTempo = 120) }
    composeTestRule.waitForIdle()

    // 1. Simule la saisie d’un nouveau tempo
    composeTestRule
        .onNodeWithTag(SamplerTestTags.INIT_TEMPO_SELECTOR)
        .performTextInput("140") // Le ViewModel met à jour inputTempo

    // Vérifie l'état intermédiaire
    composeTestRule.runOnIdle {
      // Le ViewModel doit avoir mis à jour inputTempo à 140
      assertEquals(140, fakeViewModel.uiState.value.inputTempo)
    }

    // 2. Confirme le setup
    composeTestRule.onNodeWithTag(SamplerTestTags.INIT_CONFIRM_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // 3. Vérifie l'état final (tempo doit être transféré, dialogue fermé)
    composeTestRule.runOnIdle {
      val state = fakeViewModel.uiState.value
      assertEquals(140, state.tempo) // Valeur finale transférée
      assertFalse(state.showInitialSetupDialog) // Dialogue fermé
    }
  }

  @Test
  fun pitchDragField_performsDragAndUpdatesPitch() {
    // Arrange: Position C4
    fakeViewModel.mutableUiState.update {
      it.copy(showInitialSetupDialog = true, inputPitchNote = "C", inputPitchOctave = 4)
    }
    composeTestRule.waitForIdle()

    // 1. Act UP: Simuler un drag vers le haut (C4 -> C#4)
    // Utilisation de la distance -30f pour garantir le dragAmount > 20
    simulatePitchFlick(SamplerTestTags.INIT_PITCH_SELECTOR, distanceY = -30f)
    composeTestRule.waitForIdle()

    // 2. Assert UP: Vérifie la tonalité (C#4)
    composeTestRule.runOnIdle {
      assertEquals("C#", fakeViewModel.uiState.value.inputPitchNote)
      assertEquals(4, fakeViewModel.uiState.value.inputPitchOctave)
    }

    // 3. Act DOWN: Simuler un drag vers le bas (C#4 -> C4)
    simulatePitchFlick(SamplerTestTags.INIT_PITCH_SELECTOR, distanceY = 30f)
    composeTestRule.waitForIdle()

    // 4. Assert DOWN: Vérifie que la tonalité est revenue à C4
    composeTestRule.runOnIdle {
      assertEquals("C", fakeViewModel.uiState.value.inputPitchNote)
      assertEquals(4, fakeViewModel.uiState.value.inputPitchOctave)
    }
  }
}
