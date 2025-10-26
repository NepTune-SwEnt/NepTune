package com.neptune.neptune.ui.picker

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.neptune.neptune.data.FileImporterImpl
import com.neptune.neptune.data.MediaRepositoryImpl
import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.data.local.MediaDb
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.usecase.GetLibraryUseCase
import com.neptune.neptune.domain.usecase.ImportMediaUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MEDIA_DB = "media.db"

class ImportViewModel(private val importMedia: ImportMediaUseCase, getLibrary: GetLibraryUseCase) :
    ViewModel() {
  val library: StateFlow<List<MediaItem>> =
      getLibrary().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  fun importFromSaf(uriString: String) = viewModelScope.launch { importMedia(uriString) }
}

class ImportVMFactory(
    private val importUC: ImportMediaUseCase,
    private val libraryUC: GetLibraryUseCase
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(ImportViewModel::class.java)) {
      return ImportViewModel(importUC, libraryUC) as T
    }
    error("Unknown ViewModel class")
  }
}

private fun provideDb(context: Context): MediaDb =
    Room.databaseBuilder(context.applicationContext, MediaDb::class.java, MEDIA_DB)
        .fallbackToDestructiveMigration(false)
        .build()

// Sets up singletons and provides the ImportViewModel factory
@Composable
fun ImportAppRoot(): ImportVMFactory {
  val context = LocalContext.current
  val db = remember { provideDb(context) }
  val repo = remember { MediaRepositoryImpl(db.mediaDao()) }
  val paths = remember { StoragePaths(context) }
  val importer = remember { FileImporterImpl(context, context.contentResolver, paths) }
  val packager = remember { NeptunePackager(paths) }
  val importUC = remember { ImportMediaUseCase(importer, repo, packager) }
  val libraryUC = remember { GetLibraryUseCase(repo) }

  return ImportVMFactory(importUC, libraryUC)
}

@VisibleForTesting
@Composable
fun ProjectList(items: List<MediaItem>, modifier: Modifier = Modifier) {
  LazyColumn(modifier = modifier.fillMaxSize().testTag("project_list")) {
    items(items) { item ->
      val fileName = item.projectUri.substringAfterLast('/')
      ListItem(
          headlineContent = { Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
          supportingContent = {
            Text(item.projectUri, maxLines = 1, overflow = TextOverflow.Ellipsis)
          })
      HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
    }
  }
}
