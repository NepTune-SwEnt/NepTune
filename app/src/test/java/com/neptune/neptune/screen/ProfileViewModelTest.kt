package com.neptune.neptune.screen

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.data.ImageStorageRepository
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.fakes.FakeProfileRepository
import com.neptune.neptune.ui.profile.ProfileMode
import com.neptune.neptune.ui.profile.SelfProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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

  private lateinit var viewModel: SelfProfileViewModel
  private lateinit var fakeRepo: FakeProfileRepository
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser
  private lateinit var mockImageRepo: ImageStorageRepository

  private lateinit var mockStorageService: StorageService

  @Before
  fun setup() {
    fakeRepo = FakeProfileRepository()
    mockAuth = mock()
    mockFirebaseUser = mock()
    mockImageRepo = mock()
    mockStorageService = mock()

    whenever(mockAuth.currentUser).thenReturn(mockFirebaseUser)
    whenever(mockFirebaseUser.uid).thenReturn("fake_user_id_for_test")

    viewModel = SelfProfileViewModel(fakeRepo, mockAuth, mockImageRepo, mockStorageService)
  }

  @Test
  fun onAvatarCroppedWithValidUriUpdatesTempAvatarUri() {
    val mockUri: Uri = mock()
    assertNull("Initial tempAvatarUri should be null", viewModel.tempAvatarUri.value)
    viewModel.onAvatarCropped(mockUri)
    assertEquals(
        "tempAvatarUri should be updated with the new Uri", mockUri, viewModel.tempAvatarUri.value)
  }

  @Test
  fun onAvatarCroppedWithNullUriDoesNotChangeTempAvatarUri() {
    assertNull("Initial tempAvatarUri should be null", viewModel.tempAvatarUri.value)
    viewModel.onAvatarCropped(null)
    assertNull(
        "tempAvatarUri should remain null when called with null", viewModel.tempAvatarUri.value)
  }

  @Test
  fun initialStateLoadsDefaultsInViewMode() {
    val s = viewModel.uiState.value
    assertEquals(ProfileMode.VIEW, s.mode)
    assertEquals("John Doe", s.name)
    assertEquals("johndoe", s.username)
    assertTrue(s.bio.contains("NepTune"))
    assertFalse(s.isSaving)
    assertTrue(s.isValid)
  }

  @Test
  fun onNameChangeIgnoredWhileInViewMode() {
    viewModel.onNameChange("Alice")
    assertEquals("John Doe", viewModel.uiState.value.name) // unchanged
  }

  @Test
  fun onEditClickEntersEditAndRestoresCurrentSavedValues() {
    viewModel.onEditClick()
    val s = viewModel.uiState.value
    assertEquals(ProfileMode.EDIT, s.mode)
    assertEquals("John Doe", s.name)
    assertEquals("johndoe", s.username)
    assertTrue(s.isValid)
  }

  @Test
  fun usernameValidationFailsForBadFormat() {
    viewModel.onEditClick()
    viewModel.onUsernameChange("__bad__")
    val s = viewModel.uiState.value
    assertNotNull(s.usernameError)
    assertFalse(s.isValid)
  }

  @Test
  fun bioValidationFailsWhenTooLong() {
    viewModel.onEditClick()
    viewModel.onBioChange("x".repeat(161))
    val s = viewModel.uiState.value
    assertNotNull(s.bioError)
    assertFalse(s.isValid)
  }

  @Test
  fun onSaveClickNoOpWhenNotInEditMode() {
    val before = viewModel.uiState.value
    viewModel.onSaveClick()
    val after = viewModel.uiState.value
    assertEquals(before, after)
  }

  @Test
  fun onSaveClickDoesNotProceedWhenInvalid() {
    viewModel.onEditClick()
    viewModel.onNameChange("A") // invalid (too short)
    viewModel.onSaveClick()

    val s = viewModel.uiState.value
    assertEquals(ProfileMode.EDIT, s.mode) // stays in edit
    assertFalse(s.isSaving)
    assertNotNull(s.nameError)
  }

  @Test
  fun successfulSaveTrimsFieldsAndReturnsToView() {
    viewModel.onEditClick()
    viewModel.onNameChange("  Alice  ")
    viewModel.onUsernameChange("alice_123")
    viewModel.onBioChange("  Hello there  ")

    viewModel.onSaveClick()

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

  @Test
  fun onTagInputChangeAddDuplicateAndDelete() {
    // In VIEW mode, input change is ignored
    viewModel.onTagInputFieldChange(" rock ")
    assertEquals("", viewModel.uiState.value.inputTag) // unchanged in VIEW

    // Enter EDIT and verify input field change clears errors
    viewModel.onEditClick()
    viewModel.onTagInputFieldChange("  rock  ")
    var s = viewModel.uiState.value
    assertEquals("  rock  ", s.inputTag)
    assertNull(s.tagError)

    // Empty/whitespace addition is a no-op
    viewModel.onTagInputFieldChange("   ")
    viewModel.onTagAddition()
    s = viewModel.uiState.value
    assertTrue("No tags should be added on empty input", s.tags.isEmpty())

    // Add a valid tag
    viewModel.onTagInputFieldChange("  Rock  ")
    viewModel.onTagAddition()
    s = viewModel.uiState.value
    assertTrue("rock should be added once", s.tags.contains("rock"))
    assertEquals("", s.inputTag)
    assertNull(s.tagError)

    // Attempt to add duplicate
    viewModel.onTagInputFieldChange("ROCK")
    viewModel.onTagAddition()
    s = viewModel.uiState.value
    assertEquals("Tag already exists.", s.tagError)

    // Deletion of existing tag
    viewModel.onTagDeletion("rock")
    s = viewModel.uiState.value
    assertFalse("rock should be removed", s.tags.contains("rock"))

    // Deletion of a non-existing tag is ignored
    viewModel.onTagDeletion("does-not-exist")
    assertEquals(s, viewModel.uiState.value)
  }
}
