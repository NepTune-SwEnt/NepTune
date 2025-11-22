package com.neptune.neptune.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.data.ImageStorageRepository
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.profile.UsernameTakenException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val MAX_TAGS = 10
private const val MAX_TAG_LEN = 20
private val TAG_REGEX = Regex("^[A-Za-z0-9 _-]+$")

private fun normalizeTag(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")

/**
 * ViewModel responsible for managing and validating profile data.
 *
 * Holds the current [SelfProfileUiState] and exposes update functions for UI-driven changes (name,
 * username, bio). Simulates save operations (to be replaced with repository calls).
 *
 * @param repo The profile repository for data operations.
 *
 * This class was created using AI assistance.
 */
class SelfProfileViewModel(
    private val repo: ProfileRepository = ProfileRepositoryProvider.repository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val imageRepo: ImageStorageRepository = ImageStorageRepository(),
    private val storageService: StorageService = StorageService(FirebaseStorage.getInstance())
) : ViewModel() {

  private val _uiState = MutableStateFlow(SelfProfileUiState())
  val uiState: StateFlow<SelfProfileUiState> = _uiState.asStateFlow()

  private val _localAvatarUri = MutableStateFlow<Uri?>(null)
  val localAvatarUri: StateFlow<Uri?> = _localAvatarUri.asStateFlow()

  private val _tempAvatarUri = MutableStateFlow<Uri?>(null)
  val tempAvatarUri: StateFlow<Uri?> = _tempAvatarUri.asStateFlow()

  /**
   * Generates a user-specific filename for the avatar based on the logged-in user's UID. Returns
   * null if no user is logged in.
   */
  private val avatarFileName: String?
    get() = auth.currentUser?.uid?.let { "avatar_$it.jpg" }

  /** Generates the *Firebase Storage* path for the avatar. */
  private val avatarStoragePath: String?
    get() = auth.currentUser?.uid?.let { "profile_pictures/$it.jpg" }

  /** Latest snapshot we saw from Firestore, used to detect changes on save. */
  private var snapshot: Profile? = null
  /** Cancelable job for username availability checks. */
  private var usernameCheckJob: Job? = null // suggested by ChatGPT

  init {
    viewModelScope.launch {
      repo.observeProfile().collectLatest { p ->
        snapshot = p

        if (p != null && _uiState.value.mode == ProfileMode.VIEW) {
          _uiState.value =
              _uiState.value.copy(
                  name = p.name.orEmpty(),
                  username = p.username,
                  bio = p.bio.orEmpty(),
                  avatarUrl = p.avatarUrl,
                  followers = p.subscribers.toInt(),
                  following = p.subscriptions.toInt(),
                  likes = p.likes.toInt(),
                  posts = p.posts.toInt(),
                  tags = p.tags,
                  error = null)
        } else if (p != null && _uiState.value.mode == ProfileMode.EDIT) {
          _uiState.value =
              _uiState.value.copy(
                  followers = p.subscribers.toInt(),
                  following = p.subscriptions.toInt(),
                  likes = p.likes.toInt(),
                  posts = p.posts.toInt(),
                  tags = p.tags,
                  avatarUrl = p.avatarUrl,
              )
        }
        loadInitialLocalAvatar()
      }
    }
  }

  private fun loadInitialLocalAvatar() {
    viewModelScope.launch {
      // Retrieves the dynamic filename. If it is null (no user), the URI is sanitized.
      val fileName = avatarFileName
      if (fileName == null) {
        _localAvatarUri.value = null
        return@launch
      }

      val baseUri = imageRepo.getImageUri(fileName)
      if (baseUri != null) {
        _localAvatarUri.value =
            baseUri
                .buildUpon()
                .appendQueryParameter("t", System.currentTimeMillis().toString())
                .build()
      } else {
        _localAvatarUri.value = null
      }
    }
  }

  /** Call this when the user has cropped a new avatar image. */
  fun onAvatarCropped(newUri: Uri?) {
    if (newUri != null) {
      _tempAvatarUri.value = newUri
    }
  }

  /** Call this to save the new avatar */
  private suspend fun saveAvatar() {
    val uriToSave = _tempAvatarUri.value ?: return

    val storagePath = avatarStoragePath
    val localCacheFileName = avatarFileName

    if (storagePath == null || localCacheFileName == null) {
      _uiState.value = _uiState.value.copy(error = "User not logged in.")
      return
    }
    storageService.uploadFile(uriToSave, storagePath)
    val downloadUrl = storageService.getDownloadUrl(storagePath)
    if (downloadUrl != null) {
      repo.updateAvatarUrl(downloadUrl)
    } else {
      _uiState.value = _uiState.value.copy(error = "Failed to retrieve avatar URL.")
    }

    imageRepo.saveImageFromUri(uriToSave, localCacheFileName)

    loadInitialLocalAvatar()
    _tempAvatarUri.value = null
  }

  /** Call this once after auth to ensure profile exists (first login). */
  fun loadOrEnsure(suggestedUsernameBase: String? = null, displayName: String? = null) {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isSaving = true, error = null)
        val prof = repo.ensureProfile(suggestedUsernameBase, displayName)
        snapshot = prof
        _uiState.value =
            _uiState.value.copy(
                name = prof.name.orEmpty(),
                username = prof.username,
                bio = prof.bio.orEmpty(),
                avatarUrl = prof.avatarUrl,
                followers = prof.subscribers.toInt(),
                following = prof.subscriptions.toInt(),
                likes = prof.likes.toInt(),
                posts = prof.posts.toInt(),
                tags = prof.tags,
                isSaving = false,
                error = null)
      } catch (t: Throwable) {
        _uiState.value = _uiState.value.copy(isSaving = false, error = t.message)
      }
    }
  }

  /** Enters edit mode and restores current saved profile data for editing. */
  fun onEditClick() {
    _uiState.value =
        _uiState.value
            .copy(
                mode = ProfileMode.EDIT,
                error = null,
            )
            .validated()
  }

  /**
   * Updates the name field when in edit mode and revalidates the input.
   *
   * @param newName The new name entered by the user.
   */
  fun onNameChange(newName: String) {
    if (_uiState.value.mode != ProfileMode.EDIT) return
    _uiState.value = _uiState.value.copy(name = newName).validated()
  }

  /**
   * Updates the username field when in edit mode and revalidates the input.
   *
   * @param newUsername The new username entered by the user.
   */
  fun onUsernameChange(newUsername: String) {
    if (_uiState.value.mode != ProfileMode.EDIT) return
    val updated = _uiState.value.copy(username = newUsername).validated()
    _uiState.value = updated

    usernameCheckJob?.cancel()
    if (updated.usernameError == null && newUsername.isNotBlank()) {
      usernameCheckJob =
          viewModelScope.launch {
            try {
              val free = repo.isUsernameAvailable(newUsername.trim())
              if (_uiState.value.username.trim() == newUsername.trim()) {
                _uiState.value =
                    _uiState.value.copy(
                        usernameError = if (free) null else "This username is already taken.")
              }
            } catch (_: Throwable) {
              if (_uiState.value.username.trim() == newUsername.trim()) {
                _uiState.value =
                    _uiState.value.copy(error = "Couldn’t verify username. Check your connection.")
              }
            }
          }
    }
  }

  /**
   * Updates the bio field when in edit mode and revalidates the input.
   *
   * @param newBio The new bio text entered by the user.
   */
  fun onBioChange(newBio: String) {
    if (_uiState.value.mode != ProfileMode.EDIT) return
    _uiState.value = _uiState.value.copy(bio = newBio).validated()
  }

  fun onTagInputFieldChange(newTag: String) {
    if (_uiState.value.mode != ProfileMode.EDIT) return
    _uiState.value = _uiState.value.copy(inputTag = newTag, tagError = null)
  }

  fun onTagAddition() {
    if (_uiState.value.mode != ProfileMode.EDIT) return

    val s = _uiState.value
    val tag = s.inputTag.trim()

    when {
      tag.isEmpty() -> return
      tag.length > MAX_TAG_LEN -> {
        _uiState.value = s.copy(tagError = "Max $MAX_TAG_LEN characters.")
        return
      }
      !TAG_REGEX.matches(tag) -> {
        _uiState.value = s.copy(tagError = "Only letters, numbers, spaces, - and _.")
        return
      }
      s.tags.size >= MAX_TAGS -> {
        _uiState.value = s.copy(tagError = "You can add up to $MAX_TAGS tags.")
        return
      }
      s.tags.any { it.equals(tag, true) } -> {
        _uiState.value = s.copy(tagError = "Tag already exists.")
        return
      }
    }

    val normalized = normalizeTag(tag)

    _uiState.value = s.copy(tags = s.tags + normalized, inputTag = "", tagError = null)

    viewModelScope.launch {
      try {
        repo.addNewTag(normalized)
      } catch (t: Throwable) {
        _uiState.value =
            _uiState.value.copy(
                tags = _uiState.value.tags.filterNot { it == normalized },
                tagError = t.message ?: "Couldn't add tag.")
      }
    }
  }

  fun onTagDeletion(tagToRemove: String) {
    if (_uiState.value.mode != ProfileMode.EDIT) return

    val prev = _uiState.value
    if (prev.tags.none { it.equals(tagToRemove, true) }) return

    _uiState.value = prev.copy(tags = prev.tags.filterNot { it.equals(tagToRemove, true) })

    viewModelScope.launch {
      try {
        repo.removeTag(tagToRemove)
      } catch (t: Throwable) {
        _uiState.value =
            _uiState.value.copy(tags = prev.tags, tagError = t.message ?: "Couldn't remove tag.")
      }
    }
  }

  /**
   * Validates and saves the profile data.
   *
   * If validation succeeds, simulates a repository save and switches back to view mode. If any
   * field is invalid, the corresponding error message is displayed.
   */
  fun onSaveClick() {
    val currentState = _uiState.value
    if (currentState.mode != ProfileMode.EDIT || currentState.isSaving) return

    val validated = currentState.validated()
    _uiState.value = validated
    if (!validated.isValid) return

    viewModelScope.launch {
      _uiState.value = validated.copy(isSaving = true, error = null)

      try {
        val prev = snapshot
        val newName = currentState.name.trim()
        val newUsername = currentState.username.trim()
        val newBio = currentState.bio.trim()

        if (prev == null) {
          // Safety: if we don’t have a snapshot yet, update all fields
          if (newUsername.isNotEmpty()) repo.setUsername(newUsername)
          repo.updateName(newName)
          repo.updateBio(newBio)
        } else {
          if (newUsername.isNotEmpty() && newUsername != prev.username) {
            repo.setUsername(newUsername)
          }
          if (newName != (prev.name.orEmpty())) {
            repo.updateName(newName)
          }
          if (newBio != (prev.bio.orEmpty())) {
            repo.updateBio(newBio)
          }
        }

        saveAvatar()

        _uiState.value =
            _uiState.value.copy(
                isSaving = false,
                mode = ProfileMode.VIEW,
                error = null,
                nameError = null,
                usernameError = null,
                bioError = null)
      } catch (_: UsernameTakenException) {
        _uiState.value =
            _uiState.value.copy(isSaving = false, usernameError = "This username is already taken.")
      } catch (t: Throwable) {
        _uiState.value =
            _uiState.value.copy(isSaving = false, error = t.message ?: "Couldn’t save changes.")
      }
    }
  }

  /**
   * Validates all editable fields in the current [SelfProfileUiState].
   *
   * @return A copy of the state with error fields populated if validation fails.
   */
  private fun SelfProfileUiState.validated(): SelfProfileUiState {
    val nameErr = validateName(name)
    val userErr = validateUsername(username)
    val bioErr = validateBio(bio)
    return copy(nameError = nameErr, usernameError = userErr, bioError = bioErr)
  }

  /**
   * Checks if the provided name meets length constraints.
   *
   * @param name The input name.
   * @return An error message if invalid, or null if valid.
   */
  private fun validateName(name: String): String? {
    return when {
      name.trim().length !in 2..30 -> "Name must be between 2 and 30 characters."
      else -> null
    }
  }

  /**
   * Validates username format and basic constraints.
   *
   * Usernames must:
   * - Start with a lowercase letter
   * - Contain only lowercase letters, numbers, or underscores
   * - Be 3–15 characters long
   *
   * @param username The input username.
   * @return An error message if invalid, or null if valid.
   */
  private fun validateUsername(username: String): String? {
    val usernameRegex = Regex("^[a-z][a-z0-9_]{2,14}$")
    return if (!usernameRegex.matches(username.trim())) {
      "Username must be 3–15 chars, start with a letter, and contain only lowercase letters, numbers, or underscores."
    } else null
  }

  /**
   * Validates that the bio length does not exceed 160 characters.
   *
   * @param bio The input biography text.
   * @return An error message if too long, or null if valid.
   */
  private fun validateBio(bio: String): String? {
    return if (bio.length > 160) "Bio is too long (max 160 characters)." else null
  }
}
