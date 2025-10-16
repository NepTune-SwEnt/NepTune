package com.neptune.neptune.model.project

object ProjectItemsRepositoryProvider {
  private val _repository: ProjectItemsRepository by lazy {
    //        ProjectItemsRepositoryFirestore(Firebase.firestore)
    ProjectItemsRepositoryVar()
  }

  var repository: ProjectItemsRepository = _repository
}
