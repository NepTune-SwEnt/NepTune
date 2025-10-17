package com.neptune.neptune.model.project

/**
 * Singleton object providing access to the ProjectItemsRepository instance.
 *
 * @author Uri Jaquet
 */
object TotalProjectItemsRepositoryProvider {
  private val _repository: TotalProjectItemsRepository by lazy {
    //        ProjectItemsRepositoryFirestore(Firebase.firestore)
    ProjectItemsRepositoryVarVar()
  }

  var repository: TotalProjectItemsRepository = _repository
}
