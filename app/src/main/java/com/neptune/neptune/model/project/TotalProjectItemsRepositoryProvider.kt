package com.neptune.neptune.model.project

import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.NepTuneApplication // Import your new Application class

/**
 * Singleton object providing access to the ProjectItemsRepository instance.
 *
 * @author Uri Jaquet
 */
object TotalProjectItemsRepositoryProvider {
  private val _repository: TotalProjectItemsRepository by lazy {
    // Use the application context to create an instance of the local repository
    val localRepository = ProjectItemsRepositoryLocal(NepTuneApplication.appContext)
    val cloudRepository = ProjectItemsRepositoryFirestore(FirebaseFirestore.getInstance())
    TotalProjectItemsRepositoryCompose(localRepository, cloudRepository)
  }

  var repository: TotalProjectItemsRepository = _repository
}
