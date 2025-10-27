package com.neptune.neptune.ui.mock

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.ui.picker.ImportViewModel
import com.neptune.neptune.ui.picker.ProjectList

// TODO replace with real ImportScreen when ready
@SuppressLint("VisibleForTests")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockImportScreen(vm: ImportViewModel = viewModel()) {
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

/** ViewModel factory Crafts a ImportViewModel with required use cases */
