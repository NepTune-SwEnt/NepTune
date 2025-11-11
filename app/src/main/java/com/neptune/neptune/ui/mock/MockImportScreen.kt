package com.neptune.neptune.ui.mock

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.media.NeptuneRecorder
import com.neptune.neptune.ui.picker.ImportViewModel
import com.neptune.neptune.ui.picker.ProjectList
import java.io.File

object MockImportTestTags {
  const val BUTTON_RECORD = "RecordFAB"
  const val MIC_ICON = "MicIcon"
  const val STOP_ICON = "StopIcon"
}

val padding = 16.dp

@SuppressLint("VisibleForTests")
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MockImportScreen(vm: ImportViewModel = viewModel(), recorder: NeptuneRecorder? = null) {
  val items by vm.library.collectAsState(initial = emptyList())

  val pickAudio =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importFromSaf(it.toString()) }
      }

  val context = LocalContext.current
  val actualRecorder = recorder ?: remember { NeptuneRecorder(context, StoragePaths(context)) }
  var isRecording by remember { mutableStateOf(actualRecorder.isRecording) }
  var hasAudioPermission by remember { mutableStateOf(false) }

  // Dialog state for naming the recorded project
  var showNameDialog by remember { mutableStateOf(false) }
  var proposedFileToImport by remember { mutableStateOf<File?>(null) }
  var projectName by remember { mutableStateOf("") }

  val permissionLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
          isGranted ->
        hasAudioPermission = isGranted
      }

  LaunchedEffect(key1 = true) {
    hasAudioPermission =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
  }

  Scaffold(
      topBar = { TopAppBar(title = { Text("Neptune • placeholder") }) },
      floatingActionButton = {
        Column(horizontalAlignment = Alignment.End) {
          FloatingActionButton(
              onClick = {
                if (isRecording) {
                  val recorded =
                      try {
                        actualRecorder.stop()
                      } catch (_: Exception) {
                        null
                      }
                  // If we have a recorded file, ask for a project name and import it
                  if (recorded != null) {
                    proposedFileToImport = recorded
                    projectName = recorded.nameWithoutExtension
                    showNameDialog = true
                  }
                } else {
                  if (hasAudioPermission) {
                    try {
                      actualRecorder.start()
                    } catch (_: Exception) {
                      // ignore
                    }
                  } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                }
                isRecording = actualRecorder.isRecording
              },
              modifier =
                  Modifier.padding(bottom = padding).testTag(MockImportTestTags.BUTTON_RECORD)) {
                Icon(
                    if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isRecording) "Stop recording" else "Start recording",
                    modifier =
                        Modifier.testTag(
                            if (isRecording) MockImportTestTags.STOP_ICON
                            else MockImportTestTags.MIC_ICON))
              }
          ExtendedFloatingActionButton(onClick = { pickAudio.launch(arrayOf("audio/*")) }) {
            Text("Import audio")
          }
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

  // Name dialog
  if (showNameDialog && proposedFileToImport != null) {
    val fileToImport = proposedFileToImport!!
    AlertDialog(
        onDismissRequest = { showNameDialog = false },
        title = { Text("Name project") },
        text = {
          Column {
            Text("Enter a name for the new project")
            Spacer(Modifier.height(8.dp))
            TextField(
                value = projectName,
                onValueChange = { projectName = it },
                modifier = Modifier.fillMaxWidth())
          }
        },
        confirmButton = {
          Button(
              onClick = {
                // Sanitize project name and rename the recorded file before importing
                val sanitized =
                    projectName
                        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
                        .trim('_', '.', ' ')
                        .ifEmpty { projectName }
                val ext = fileToImport.extension
                val parent = fileToImport.parentFile
                val desiredName = if (ext.isNotBlank()) "$sanitized.$ext" else sanitized
                val desiredFile = File(parent, desiredName)
                val finalFile =
                    if (desiredFile.exists()) {
                      // avoid overwrite: keep original file name
                      fileToImport
                    } else {
                      val moved = fileToImport.renameTo(desiredFile)
                      if (moved) desiredFile else fileToImport
                    }

                // Ask ViewModel to import the file
                vm.importRecordedFile(finalFile)

                showNameDialog = false
                proposedFileToImport = null
              }) {
                Text("Create")
              }
        },
        dismissButton = {
          Button(
              onClick = {
                // If dismissed, still import with original filename
                vm.importRecordedFile(fileToImport)
                showNameDialog = false
                proposedFileToImport = null
              }) {
                Text("Skip")
              }
        })
  }
}
