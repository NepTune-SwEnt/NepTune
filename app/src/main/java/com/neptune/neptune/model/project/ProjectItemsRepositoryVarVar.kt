package com.neptune.neptune.model.project

import android.util.Log

class ProjectItemsRepositoryVarVar : TotalProjectItemsRepositoryCompose(
  localRepo = ProjectItemsRepositoryVar(),
  cloudRepo = ProjectItemsRepositoryVar()) {

}
