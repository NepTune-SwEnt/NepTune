package com.neptune.neptune.model.sample

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Singleton object providing access to the SampleRepository instance.
 *
 * @author Angéline Bignens
 */
object SampleRepositoryProvider {
  private val _repository: SampleRepository by lazy { SampleRepositoryFirebase(Firebase.firestore) }

  var repository: SampleRepository = _repository
}
