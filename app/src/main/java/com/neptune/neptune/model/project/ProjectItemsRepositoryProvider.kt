package com.neptune.neptune.model.project

/**
 * Singleton object providing access to the ProjectItemsRepository instance.
 *
 * @author Uri Jaquet
 */
object ProjectItemsRepositoryProvider {
  private val _repository: ProjectItemsRepository by lazy {
    ProjectItemsRepositoryVar()
  }

  var repository: ProjectItemsRepository = _repository
}
