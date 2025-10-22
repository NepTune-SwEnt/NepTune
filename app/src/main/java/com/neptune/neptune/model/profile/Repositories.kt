package com.neptune.neptune.model.profile

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object Repositories {
  val profile by lazy { ProfileRepositoryFirebase(Firebase.firestore /*, Firebase.storage*/) }
}