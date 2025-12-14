package com.neptune.neptune.ui.picker

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.neptune.neptune.R
import com.neptune.neptune.data.FileImporterImpl
import com.neptune.neptune.data.MediaRepositoryImpl
import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.data.local.MediaDb
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.usecase.GetLibraryUseCase
import com.neptune.neptune.domain.usecase.ImportMediaUseCase
import com.neptune.neptune.ui.theme.NepTuneTheme
import com.neptune.neptune.util.AudioUtils
import java.io.File
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MEDIA_DB = "media.db"
private var onImportFinished = {}

/**
 * ViewModel for the ImportScreen. This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class ImportViewModel(
  private val importMedia: ImportMediaUseCase,
  getLibrary: GetLibraryUseCase,
) : ViewModel() {
  val library: StateFlow<List<MediaItem>> =
      getLibrary().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  // Accept either SAF/content URIs (string) or file:// URIs: for file URIs call the recorded-file
  // overload
  fun importFromSaf(uriString: String) =
      viewModelScope.launch {
        val parsed =
            try {
              URI(uriString)
            } catch (_: Exception) {
              null
            }
        if (parsed != null && parsed.scheme == "file") {
          // use File overload
          try {
            val f = File(parsed)
            importMedia(f)
          } catch (_: Exception) {
            // fallback to string-based import in case of any issue
            importMedia(uriString)
          }
        } else {
          importMedia(uriString)
        }
      }
  /** Takes the recorded file (M4A), converts it to WAV, then imports it. */
  fun processAndImportRecording(rawM4aFile: File, rawProjectName: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val sanitizedName =
          rawProjectName
              .replace(Regex("[^A-Za-z0-9._-]+"), "_")
              .replace(Regex("_+"), "_")
              .trim('_', '.', ' ')
              .ifEmpty { rawProjectName }
      val destinationWav = File(rawM4aFile.parent, "$sanitizedName.wav")

      val conversionSuccess = AudioUtils.convertToWav(rawM4aFile.toUri(), destinationWav)

      if (conversionSuccess) {
        try {
          rawM4aFile.delete()
        } catch (_: Exception) {}
        importMedia(destinationWav)
      } else {
        importMedia(rawM4aFile)
      }
    }
  }

  // New convenience: import a File produced by the in-app recorder directly
  fun importRecordedFile(file: File, refreshProjects: () -> Unit = {}) = viewModelScope.launch {
    importMedia(file)
    refreshProjects()
  }

  // Register a callback that will be invoked when an import completes (used for SAF / external
  // imports). We assign the top-level `onImportFinished` so the ImportMediaUseCase created in
  // `importAppRoot` (which captures a lambda calling that var) will call this callback.
  fun setOnImportFinished(callback: () -> Unit) {
    onImportFinished = callback
  }
}

class ImportVMFactory(
    private val importUC: ImportMediaUseCase,
    private val libraryUC: GetLibraryUseCase,
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
fun importAppRoot(): ImportVMFactory {
  val context = LocalContext.current
  val db = remember { provideDb(context) }
  val repo = remember { MediaRepositoryImpl(db.mediaDao()) }
  val paths = remember { StoragePaths(context) }
  val importer = remember { FileImporterImpl(context, context.contentResolver, paths) }
  val packager = remember { NeptunePackager(paths) }
  val importUC = remember { ImportMediaUseCase(importer, repo, packager){ onImportFinished() } }
  val libraryUC = remember { GetLibraryUseCase(repo) }

  return ImportVMFactory(importUC, libraryUC)
}

@VisibleForTesting
@Composable
fun ProjectList(items: List<MediaItem>, modifier: Modifier = Modifier) {
  LazyColumn(modifier = modifier.fillMaxSize().testTag("project_list")) {
    itemsIndexed(items) { index, item ->
      val fileName = item.projectUri.substringAfterLast('/')

      // Alternate background 1/2
      val rowColor =
          if (index % 2 == 0) NepTuneTheme.colors.shadow.copy(alpha = 0.1f)
          else NepTuneTheme.colors.indicatorColor.copy(alpha = 0.1f)
      Column(
          modifier =
              Modifier.fillMaxWidth()
                  .background(rowColor)
                  .padding(vertical = 12.dp, horizontal = 16.dp)) {
            Text(
                text = fileName,
                style =
                    TextStyle(
                        fontSize = 28.sp,
                        fontFamily = FontFamily(Font(R.font.markazi_text)),
                        fontWeight = FontWeight(500),
                        color = NepTuneTheme.colors.onBackground),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)

            Text(
                text = item.projectUri,
                style =
                    TextStyle(
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(R.font.markazi_text)),
                        fontWeight = FontWeight(300),
                        color = NepTuneTheme.colors.onBackground.copy(alpha = 0.7f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
          }
      HorizontalDivider(
          Modifier, thickness = 1.dp, color = NepTuneTheme.colors.onBackground.copy(alpha = 0.3f))
    }
  }
}
