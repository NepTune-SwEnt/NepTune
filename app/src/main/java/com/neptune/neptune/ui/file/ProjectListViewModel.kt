package com.neptune.neptune.ui.file

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neptune.neptune.Sample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ProjectListViewModel : ViewModel() {

  // All the projects we have
  private val _projects = MutableStateFlow<List<Sample>>(emptyList())
  val projects: MutableStateFlow<List<Sample>> = _projects

  // The project we have selected (to open in the sampler or to post it)
  private val _projectsSelected = MutableStateFlow<Sample?>(null)
  val projectsSelected: MutableStateFlow<Sample?> = _projectsSelected

  init {
    loadExistingProjects()
  }

  /** Loads the existing files */
  // Todo: Replace with actual data from the repository
  private fun loadExistingProjects() {
    viewModelScope.launch {
      _projects.value =
          listOf(
              Sample(
                  1,
                  "Sample 1",
                  "This is a sample description 1",
                  21,
                  listOf("#nature"),
                  21,
                  21,
                  21),
              Sample(
                  2, "Sample 2", "This is a sample description 2", 42, listOf("#sea"), 42, 42, 42),
              Sample(
                  3,
                  "Sample 3",
                  "This is a sample description 3",
                  12,
                  listOf("#relax"),
                  12,
                  12,
                  12),
              Sample(
                  4,
                  "Sample 4",
                  "This is a sample description 4",
                  2,
                  listOf("#takeItEasy"),
                  1,
                  2,
                  1),
          )
    }
  }

  fun selectProject(sample: Sample) {
    _projectsSelected.value = sample
  }
}
