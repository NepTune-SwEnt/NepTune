package com.neptune.neptune.model.project

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Singleton object providing access to the ProjectItemsRepository instance.
 *
 * @author Uri Jaquet
 */
object TotalProjectItemsRepositoryProvider {
  private val _repository: TotalProjectItemsRepository by lazy {
    TotalProjectItemsRepositoryCompose(ProjectItemsRepositoryVar(),ProjectItemsRepositoryFirestore(Firebase.firestore))
//    ProjectItemsRepositoryVarVar()
  }

  var repository: TotalProjectItemsRepository = _repository
}
