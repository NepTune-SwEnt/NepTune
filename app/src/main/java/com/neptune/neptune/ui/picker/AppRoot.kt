package com.neptune.neptune.ui.picker

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.neptune.neptune.data.FileImporterImpl
import com.neptune.neptune.data.MediaRepositoryImpl
import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.data.local.MediaDb
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.usecase.GetLibraryUseCase
import com.neptune.neptune.domain.usecase.ImportMediaUseCase
/*
    Root composable for the Importer app.
    Sets up singletons, ViewModel, and hosts the temp ImportScreen.
     partially written with ChatGPT
 */
@Composable
fun AppRoot() {
  val context = LocalContext.current

  // --- infra singletons in composition ---
  val db = remember { provideDb(context) }
  val repo = remember { MediaRepositoryImpl(db.mediaDao()) }
  val paths = remember { StoragePaths(context) }
  val importer = remember { FileImporterImpl(context, context.contentResolver, paths) }
  val packager = remember { NeptunePackager(paths) }
  val importUC = remember { ImportMediaUseCase(importer, repo, packager) }
  val libraryUC = remember { GetLibraryUseCase(repo) }

  val vm: ImportViewModel = viewModel(factory = ImportVMFactory(importUC, libraryUC))

  // ---- Temporary placeholder UI (no ImportScreen required) ----
  PlaceholderImportHost(vm)
}

// TODO replace with real ImportScreen when ready
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderImportHost(vm: ImportViewModel) {
  val items by vm.library.collectAsState(initial = emptyList())

  val pickAudio =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importFromSaf(it.toString()) }
      }

  Scaffold(
      topBar = { TopAppBar(title = { Text("Neptune • placeholder") }) },
      floatingActionButton = {
        ExtendedFloatingActionButton(onClick = { pickAudio.launch(arrayOf("audio/*")) }) {
          Text("Import audio")
        }
      }) { padding ->
        if (items.isEmpty()) {
          Column(Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
            Text("No projects yet.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
          }
        } else {
          ProjectList(items, Modifier.padding(padding))
        }
      }
}
@VisibleForTesting
@Composable
public fun ProjectList(items: List<MediaItem>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize().testTag("project_list")) {
        items(items) { item ->
            val fileName = remember(item.projectUri) { item.projectUri.substringAfterLast('/') }
            ListItem(
                headlineContent = { Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(item.projectUri, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        }
    }
}

private fun provideDb(context: Context): MediaDb =
    Room.databaseBuilder(context.applicationContext, MediaDb::class.java, "media.db")
        .fallbackToDestructiveMigration(false)
        .build()

/** ViewModel factory Crafts a ImportViewModel with required use cases */
private class ImportVMFactory(
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
