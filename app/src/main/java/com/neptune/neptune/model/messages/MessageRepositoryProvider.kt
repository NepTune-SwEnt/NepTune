package com.neptune.neptune.model.messages

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.neptune.neptune.model.profile.ProfileRepositoryProvider

/**
 * Singleton object providing access to the MessageRepository instance.
 *
 * @author Angéline Bignens
 */
object MessageRepositoryProvider {
  private val _repository: MessageRepository by lazy {
    MessageRepositoryFirebase(Firebase.firestore, ProfileRepositoryProvider.repository)
  }

  var repository: MessageRepository = _repository
}
