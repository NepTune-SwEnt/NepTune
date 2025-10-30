package com.neptune.neptune.screen

import com.neptune.neptune.model.fakes.FakeProfileRepository
import com.neptune.neptune.ui.profile.ProfileMode
import com.neptune.neptune.ui.profile.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
    TestWatcher() {

  override fun starting(description: Description) {
    Dispatchers.setMain(dispatcher)
  }

  override fun finished(description: Description) {
    Dispatchers.resetMain()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

  @get:Rule val mainRule = MainDispatcherRule()

  private lateinit var viewModel: ProfileViewModel
  private lateinit var fakeRepo: FakeProfileRepository

  @Before
  fun setup() {
    fakeRepo = FakeProfileRepository()
    viewModel = ProfileViewModel(fakeRepo)
  }

  @Test
  fun `initial state loads defaults in VIEW mode`() {
    val s = viewModel.uiState.value
    assertEquals(ProfileMode.VIEW, s.mode)
    assertEquals("John Doe", s.name)
    assertEquals("johndoe", s.username)
    assertTrue(s.bio.contains("NepTune"))
    assertFalse(s.isSaving)
    assertTrue(s.isValid)
  }

  @Test
  fun `onNameChange ignored while in VIEW mode`() {
    viewModel.onNameChange("Alice")
    assertEquals("John Doe", viewModel.uiState.value.name) // unchanged
  }

  @Test
  fun `onEditClick enters EDIT and restores current saved values`() {
    viewModel.onEditClick()
    val s = viewModel.uiState.value
    assertEquals(ProfileMode.EDIT, s.mode)
    assertEquals("John Doe", s.name)
    assertEquals("johndoe", s.username)
    assertTrue(s.isValid)
  }

  @Test
  fun `username validation fails for bad format`() {
    viewModel.onEditClick()
    viewModel.onUsernameChange("__bad__")
    val s = viewModel.uiState.value
    assertNotNull(s.usernameError)
    assertFalse(s.isValid)
  }

  @Test
  fun `bio validation fails when too long`() {
    viewModel.onEditClick()
    viewModel.onBioChange("x".repeat(161))
    val s = viewModel.uiState.value
    assertNotNull(s.bioError)
    assertFalse(s.isValid)
  }

  @Test
  fun `onSaveClick no-op when not in EDIT mode`() = runTest {
    val before = viewModel.uiState.value
    viewModel.onSaveClick()
    val after = viewModel.uiState.value
    assertEquals(before, after)
  }

  @Test
  fun `onSaveClick does not proceed when invalid`() = runTest {
    viewModel.onEditClick()
    viewModel.onNameChange("A") // invalid (too short)
    viewModel.onSaveClick()

    val s = viewModel.uiState.value
    assertEquals(ProfileMode.EDIT, s.mode) // stays in edit
    assertFalse(s.isSaving)
    assertNotNull(s.nameError)
  }

  @Test
  fun `successful save trims fields and returns to VIEW`() = runTest {
    viewModel.onEditClick()
    viewModel.onNameChange("  Alice  ")
    viewModel.onUsernameChange("alice_123")
    viewModel.onBioChange("  Hello there  ")

    viewModel.onSaveClick()
    advanceUntilIdle() // finish the viewModelScope.launch work

    val s = viewModel.uiState.value
    assertEquals(ProfileMode.VIEW, s.mode)
    assertFalse(s.isSaving)
    assertNull(s.error)
    assertNull(s.nameError)
    assertNull(s.usernameError)
    assertNull(s.bioError)

    // trimmed values persisted
    assertEquals("Alice", s.name)
    assertEquals("alice_123", s.username)
    assertEquals("Hello there", s.bio)
  }
}
