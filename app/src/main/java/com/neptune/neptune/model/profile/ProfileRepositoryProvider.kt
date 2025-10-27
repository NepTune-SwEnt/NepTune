package com.neptune.neptune.model.profile

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Singleton object providing access to the ProfileRepository instance.
 *
 * @author Arianna Baur
 */
object ProfileRepositoryProvider {
  private val _repository: ProfileRepository by lazy {
    ProfileRepositoryFirebase(Firebase.firestore)
  }

  var repository: ProfileRepository = _repository
}
