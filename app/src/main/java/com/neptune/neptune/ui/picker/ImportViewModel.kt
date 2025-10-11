package com.neptune.neptune.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.usecase.GetLibraryUseCase
import com.neptune.neptune.domain.usecase.ImportMediaUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ImportViewModel(private val importMedia: ImportMediaUseCase, getLibrary: GetLibraryUseCase) :
    ViewModel() {

  val library: StateFlow<List<MediaItem>> =
      getLibrary().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  fun importFromSaf(uriString: String) = viewModelScope.launch { importMedia(uriString) }
}
