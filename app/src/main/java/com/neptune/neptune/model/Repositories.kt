package com.neptune.neptune.model

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
//import com.google.firebase.storage.storage

object Repositories {
    val profile by lazy {
        ProfileRepositoryFirebase(Firebase.firestore/*, Firebase.storage*/)
    }
}
