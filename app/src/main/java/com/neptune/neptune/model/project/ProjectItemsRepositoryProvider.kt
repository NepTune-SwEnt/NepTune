package com.neptune.neptune.model.project

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object ProjectItemsRepositoryProvider {
    private val _repository: ProjectItemsRepository by lazy {
//        ProjectItemsRepositoryFirestore(Firebase.firestore)
        ProjectItemsRepositoryVar()
    }

    var repository: ProjectItemsRepository = _repository
}