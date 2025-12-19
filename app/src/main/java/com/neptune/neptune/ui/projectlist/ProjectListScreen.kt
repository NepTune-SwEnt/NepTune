package com.neptune.neptune.ui.projectlist

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.neptune.neptune.R
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.media.NeptuneRecorder
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.TotalProjectItemsRepositoryProvider
import com.neptune.neptune.ui.offline.OfflineBanner
import com.neptune.neptune.ui.picker.ImportViewModel
import com.neptune.neptune.ui.picker.MAX_AUDIO_DURATION_MS
import com.neptune.neptune.ui.picker.NameProjectDialog
import com.neptune.neptune.ui.picker.sanitizeAndRename
import com.neptune.neptune.ui.theme.NepTuneTheme
import java.io.File
import android.os.Environment
import androidx.compose.material.icons.filled.Pause
import kotlinx.coroutines.runBlocking

object ProjectListScreenTestTags {
  const val PROJECT_LIST_SCREEN = "ProjectListScreen"
  const val PROJECT_LIST = "projectList"
  const val SEARCH_BAR = "searchBar"
  const val PROJECT_NAME = "ProjectName"
  const val DESCRIPTION_TEXT_FIELD = "DescriptionTextField"
  const val SEARCH_TEXT_FIELD = "SearchTextField"
  const val CONFIRM_DIALOG = "ConfirmDialog"
  const val CHANGE_DESCRIPTION_BUTTON = "ChangeDescriptionButton"
  const val RENAME_BUTTON = "RenameButton"
  const val DELETE_BUTTON = "DeleteButton"
  const val DOWNLOAD_BUTTON = "DownloadButton"
}

private const val SEARCHBAR_FONT_SIZE = 21

/**
 * Composable function representing the Project List Screen. This has been written with the help of
 * LLMs.
 *
 * @param credentialManager Manages user credentials.
 * @param onProjectClick Lambda function to navigate.
 * @param projectListViewModel ViewModel managing the state of the project list.
 * @param mediaPlayer NeptuneMediaPlayer instance used to play audio previews.
 * @author Uri Jaquet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onProjectClick: (ProjectItem) -> Unit = {},
    projectListViewModel: ProjectListViewModel = viewModel(),
    importViewModel: ImportViewModel = viewModel(),
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current,
    recorder: NeptuneRecorder? = null,
    testRecordedFile: File? = null,
    onDeleteFailed: (() -> Unit)? = null,
) {
  val uiState by projectListViewModel.uiState.collectAsState()
  // Use the reactive uiState.projects directly so additions trigger recomposition
  val projects: List<ProjectItem> = uiState.projects
  val selectedProjects: String? = uiState.selectedProject
  val isOnline by projectListViewModel.isOnline.collectAsState()
  val isUserLoggedIn = projectListViewModel.isUserLoggedIn

  var searchText by remember { mutableStateOf("") }
  val filteredProjects =
      if (searchText.isBlank()) {
        projects
      } else {
        projects.filter { p ->
          p.name.contains(searchText, ignoreCase = true) ||
              p.description.contains(searchText, ignoreCase = true) ||
              p.tags.any { it.contains(searchText, ignoreCase = true) }
        }
      }

  // Import audio part
  val context = LocalContext.current

  val pickAudio =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importViewModel.importFromSaf(it.toString()) }
      }

  val actualRecorder = recorder ?: remember { NeptuneRecorder(context, StoragePaths(context)) }
  var isRecording by remember { mutableStateOf(actualRecorder.isRecording) }
  var hasAudioPermission by remember { mutableStateOf(false) }

  // Dialog state for naming the recorded project
  var showNameDialog by remember { mutableStateOf(false) }
  var proposedFileToImport by remember { mutableStateOf<File?>(null) }
  var projectName by remember { mutableStateOf("") }

  LaunchedEffect(isRecording) {
    if (isRecording) {
      kotlinx.coroutines.delay(MAX_AUDIO_DURATION_MS) // 1 minute
      com.neptune.neptune.ui.picker.performToggleRecord(
          isRecording = true,
          actualRecorder = actualRecorder,
          hasAudioPermission = hasAudioPermission,
          requestPermission = {}, // is already recording so no need to request
          onRecordedFile = { recorded ->
            proposedFileToImport = recorded
            projectName = recorded.nameWithoutExtension
            showNameDialog = true
          },
          updateIsRecording = { isRecording = it })
      Toast.makeText(context, "Time limit reached (1 min)", Toast.LENGTH_SHORT).show()
    }
  }

  val importError by importViewModel.errorMessage.collectAsState()

  LaunchedEffect(importError) {
    importError?.let { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
      importViewModel.clearError()
    }
  }

  LaunchedEffect(testRecordedFile) {
    testRecordedFile?.let {
      proposedFileToImport = it
      projectName = it.nameWithoutExtension
      showNameDialog = true
    }
  }

  // Ensure SAF / external imports refresh the project list after completion by registering a
  // callback on the ImportViewModel. This will be called by ImportMediaUseCase via the
  // top-level onImportFinished captured when the use case was created in importAppRoot().
  LaunchedEffect(importViewModel) {
    importViewModel.setOnImportFinished { projectListViewModel.refreshProjects() }
  }

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
      containerColor = NepTuneTheme.colors.background,
      content = { it ->
        Column(
            modifier =
                Modifier.testTag(ProjectListScreenTestTags.PROJECT_LIST_SCREEN)
                    .fillMaxSize()
                    .padding(it)) {
              SearchBar(value = searchText, onValueChange = { searchText = it })
              if (!isOnline && isUserLoggedIn) {
                OfflineBanner()
              }
              ProjectList(
                  projects = filteredProjects,
                  selectedProject = selectedProjects,
                  modifier = Modifier.padding(it),
                  projectListViewModel = projectListViewModel,
                  onProjectClick = onProjectClick,
                  mediaPlayer = mediaPlayer)
            }
      },
      floatingActionButton = {
        // Reuse the centralized createProjectButtons to avoid duplication
        com.neptune.neptune.ui.picker.CreateProjectButtons(
            isRecording = isRecording,
            actualRecorder = actualRecorder,
            hasAudioPermission = hasAudioPermission,
            requestPermission = { permissionLauncher.launch(it) },
            onRecordedFile = { recorded ->
              proposedFileToImport = recorded
              projectName = recorded.nameWithoutExtension
              showNameDialog = true
            },
            onImportAudio = { pickAudio.launch(arrayOf("audio/*")) },
            updateIsRecording = { isRecording = it })
      })

  // Name dialog
  if (showNameDialog && proposedFileToImport != null) {
    NameProjectDialog(
        projectName = projectName,
        onNameChange = { projectName = it },
        onConfirm = { name ->
          val finalFile = sanitizeAndRename(proposedFileToImport!!, name)
          // For recorded files we already call refresh after import via the lambda param.
          importViewModel.importRecordedFile(finalFile) { projectListViewModel.refreshProjects() }
          showNameDialog = false
          proposedFileToImport = null
        },
        onCancel = {
          val deleted = proposedFileToImport?.delete() ?: true
          if (!deleted) {
            onDeleteFailed?.invoke()
            // provide the same toast behavior as before
            Toast.makeText(
                    context,
                    "Could not delete temporary file ${proposedFileToImport?.absolutePath}",
                    Toast.LENGTH_SHORT)
                .show()
          }
          showNameDialog = false
          proposedFileToImport = null
        })
  }
}

/**
 * Composable function to display a list of projects. This has been written with the help of LLMs.
 *
 * @param projects List of ProjectItem to display.
 * @param selectedProject ID of the currently selected project, if any.
 * @param modifier Modifier for styling.
 * @param projectListViewModel ViewModel managing the state of the project list.
 * @param onProjectClick Lambda function to navigate.
 * @param mediaPlayer NeptuneMediaPlayer instance used to play audio previews.
 * @author Uri Jaquet
 */
@Composable
fun ProjectList(
    modifier: Modifier = Modifier,
    projects: List<ProjectItem>,
    selectedProject: String? = null,
    projectListViewModel: ProjectListViewModel,
    onProjectClick: (ProjectItem) -> Unit = {},
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current,
) {
  val lineColor = NepTuneTheme.colors.onBackground
  Column(
      modifier =
          modifier.drawBehind {
            drawLine(
                color = lineColor,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 2.dp.toPx())
          },
  ) {
    if (projects.isNotEmpty()) {
      LazyColumn(modifier = modifier.testTag(ProjectListScreenTestTags.PROJECT_LIST)) {
        items(items = projects, key = { project -> project.uid }) { project ->
          ProjectListItem(
              project = project,
              selectedProject = selectedProject,
              onProjectClick = onProjectClick,
              projectListViewModel = projectListViewModel,
              mediaPlayer = mediaPlayer)
        }
      }
    } else {
      Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text =
                "Tap “Import audio” to create a project.",
            style =
                TextStyle(
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.markazi_text)),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = NepTuneTheme.colors.onBackground.copy(alpha = 0.7f)))
      }
    }
  }
}

/**
 * Composable function representing a single item in the project list. This has been written with
 * the help of LLMs.
 *
 * @param project The ProjectItem to display.
 * @param selectedProject ID of the currently selected project, if any.
 * @param onProjectClick Lambda function to open the selected project.
 * @param projectListViewModel ViewModel managing the state of the project list.
 * @param mediaPlayer NeptuneMediaPlayer used to play audio previews.
 * @author Uri Jaquet
 */
@Composable
fun ProjectListItem(
    project: ProjectItem,
    selectedProject: String? = null,
    onProjectClick: (ProjectItem) -> Unit = {},
    projectListViewModel: ProjectListViewModel,
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current,
) {
  val backGroundColor =
      if (project.uid == selectedProject) NepTuneTheme.colors.listBackground
      else NepTuneTheme.colors.background
  val lineColor = NepTuneTheme.colors.onBackground
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 2.dp)
              .clickable(
                  onClick = {
                    projectListViewModel.selectProject(project)
                    onProjectClick(project)
                  })
              .drawBehind {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx())
              }
              .testTag("project_${project.uid}"),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      colors = CardDefaults.cardColors(backGroundColor),
      shape = RoundedCornerShape(0.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          Row(
              modifier = Modifier.padding(start = 5.dp).weight(1f),
              verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                      // Play the project's preview audio if available, otherwise fall back to a
                      // demo resource
                      try {
                        val path = project.audioPreviewLocalPath
                        val uri =
                            if (!path.isNullOrBlank()) {
                              path.toUri()
                            } else {
                              // fallback demo URI from the media player helper
                              mediaPlayer.getUriFromSampleId(project.uid)
                            }
                        mediaPlayer.togglePlay(uri)
                      } catch (e: Exception) {
                        Log.e("ProjectListItem", "Error playing preview for ${project.uid}", e)
                      }
                    },
                    modifier = Modifier.testTag("play_${project.uid}"),
                    content = {
                      Icon(
                          Icons.Filled.PlayArrow,
                          contentDescription = "Play",
                          tint = NepTuneTheme.colors.onBackground,
                          modifier = Modifier.size(26.dp))
                    })
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = project.name,
                    modifier = Modifier.testTag(ProjectListScreenTestTags.PROJECT_NAME),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style =
                        TextStyle(
                            fontSize = 27.sp,
                            fontFamily = FontFamily(Font(R.font.markazi_text)),
                            fontWeight = FontWeight(150),
                            color = NepTuneTheme.colors.onBackground))
              }
          EditMenu(project, projectListViewModel = projectListViewModel)
        }
  }
}

/**
 * Composable function representing the edit menu for a project item. This has been written with the
 * help of LLMs.
 *
 * @param project The ProjectItem for which the edit menu is displayed.
 * @param projectListViewModel ViewModel managing the state of the project list.
 * @author Uri Jaquet
 */
@Composable
fun EditMenu(
    project: ProjectItem,
    projectListViewModel: ProjectListViewModel,
) {
  var expanded by remember { mutableStateOf(false) }
  var showRenameDialog by remember { mutableStateOf(false) }
  var showChangeDescDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  val isOnline by projectListViewModel.isOnline.collectAsState()
  val context = LocalContext.current

  if (showRenameDialog) {
    RenameProjectDialog(
        initialName = project.name,
        onDismiss = { showRenameDialog = false },
        onConfirm = { newName ->
          projectListViewModel.renameProject(project.uid, newName)
          showRenameDialog = false
        })
  }

  if (showChangeDescDialog) {
    ChangeDescriptionDialog(
        initialDescription = project.description,
        onDismiss = { showChangeDescDialog = false },
        onConfirm = { newDesc ->
          projectListViewModel.changeProjectDescription(project.uid, newDesc)
          showChangeDescDialog = false
        })
  }

  if (showDeleteDialog) {
    DeleteConfirmationDialog(
        onConfirm = {
          projectListViewModel.deleteProject(project.uid)
          showDeleteDialog = false
        },
        onDismiss = { showDeleteDialog = false })
  }

  if (isOnline) {
    // Right  buttons
    Row {
      // Star favorite toggle
      IconButton(
          onClick = { projectListViewModel.toggleFavorite(project.uid) },
          modifier = Modifier.testTag("favorite_${project.uid}")) {
            Icon(
                imageVector =
                    if (project.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Favorite",
                tint = NepTuneTheme.colors.onBackground,
                modifier = Modifier.size(26.dp))
          }

      Box(modifier = Modifier.padding(end = 0.dp)) {
        IconButton(
            onClick = { expanded = true }, modifier = Modifier.testTag("menu_${project.uid}")) {
              Icon(
                  Icons.Rounded.MoreVert,
                  contentDescription = "Edit",
                  tint = NepTuneTheme.colors.onBackground,
                  modifier = Modifier.size(30.dp).padding(end = 0.dp),
              )
            }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          DropdownMenuItem(
              modifier = Modifier.testTag(ProjectListScreenTestTags.RENAME_BUTTON),
              text = { Text("Rename") },
              onClick = {
                showRenameDialog = true
                expanded = false
              })
          DropdownMenuItem(
              modifier = Modifier.testTag(ProjectListScreenTestTags.CHANGE_DESCRIPTION_BUTTON),
              text = { Text("Change Description") },
              onClick = {
                showChangeDescDialog = true
                expanded = false
              })
          // TODO Add/remove from cloud button to add when fully implemented
          // Download preview file to app external Downloads folder (no special permission needed)
          DropdownMenuItem(
              modifier = Modifier.testTag(ProjectListScreenTestTags.DOWNLOAD_BUTTON),
              text = { Text("Download Audio") },
              onClick = {
                expanded = false
                val path = project.audioPreviewLocalPath
                if (path.isNullOrBlank()) {
                  Toast.makeText(context, "No preview available", Toast.LENGTH_SHORT).show()
                  return@DropdownMenuItem
                }
                try {
                  val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.canonicalFile
                  Log.d("ProjectListScreen", "downloadsDir: ${downloadsDir?.canonicalPath}")
                  if (downloadsDir == null) {
                    Toast.makeText(context, "Unable to access downloads folder", Toast.LENGTH_SHORT).show()
                    return@DropdownMenuItem
                  }
                  if (!downloadsDir.exists()) downloadsDir.mkdirs()

                  // Handle content:// URIs and file paths/file:// URIs
                  if (path.startsWith("content:")) {
                    val uri = path.toUri()
                    val srcName = uri.lastPathSegment ?: "audio.wav"
                    val destFile = File(downloadsDir, srcName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                      destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    Toast.makeText(context, "Saved audio to downloads folder", Toast.LENGTH_SHORT).show()
                    return@DropdownMenuItem
                  }

                  // Otherwise assume it's a file path or file:// URI
                  val cleanedPath = path.removePrefix("file:").removePrefix("file://")
                  val srcFile = File(cleanedPath)
                  if (!srcFile.exists()) {
                    Toast.makeText(context, "Preview file not found", Toast.LENGTH_SHORT).show()
                    return@DropdownMenuItem
                  }

                  val destFile = File(downloadsDir, srcFile.name)
                  srcFile.copyTo(destFile, overwrite = true)
                  Toast.makeText(context, "Saved audio to the downloads folder", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                  Log.e("ProjectListScreen", "Error downloading preview for ${project.uid}", e)
                  Toast.makeText(context, "Failed to download preview", Toast.LENGTH_SHORT).show()
                }
              })
          DropdownMenuItem(
            modifier = Modifier.testTag(ProjectListScreenTestTags.DELETE_BUTTON),
            text = { Text("Delete") },
            onClick = {
              showDeleteDialog = true
              expanded = false
            })
        }
      }
    }
  }
}

/**
 * Composable function representing a dialog to rename a project. This has been written with the
 * help of LLMs.
 *
 * @param onDismiss Lambda function to call when the dialog is dismissed.
 * @param onConfirm Lambda function to call with the new name when confirmed.
 * @author Uri Jaquet
 */
@Composable
fun RenameProjectDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
  var text by remember { mutableStateOf(initialName) }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Rename Project") },
      text = {
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("New name") })
      },
      confirmButton = {
        Button(
            onClick = { onConfirm(text) },
            modifier = Modifier.testTag(ProjectListScreenTestTags.CONFIRM_DIALOG)) {
              Text("Confirm")
            }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/**
 * Composable function representing a dialog to change a project's description. This has been
 * written with the help of LLMs.
 *
 * @param initialDescription The initial description to display in the text field.
 * @param onDismiss Lambda function to call when the dialog is dismissed.
 * @param onConfirm Lambda function to call with the new description when confirmed.
 * @author Uri Jaquet
 */
@Composable
fun ChangeDescriptionDialog(
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
  var text by remember { mutableStateOf(initialDescription) }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Change Description") },
      text = {
        OutlinedTextField(
            modifier = Modifier.testTag(ProjectListScreenTestTags.DESCRIPTION_TEXT_FIELD),
            value = text,
            onValueChange = { text = it },
            label = { Text("Description") },
            singleLine = false,
            maxLines = 4)
      },
      confirmButton = {
        Button(
            onClick = { onConfirm(text) },
            modifier = Modifier.testTag(ProjectListScreenTestTags.CONFIRM_DIALOG)) {
              Text("Confirm")
            }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/**
 * Composable function representing a confirmation dialog for deleting a project. This has been
 * written with the help of LLMs.
 *
 * @param onConfirm Lambda function to call when the deletion is confirmed.
 * @param onDismiss Lambda function to call when the dialog is dismissed.
 * @author Uri Jaquet
 */
@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Delete Project") },
      text = {
        Text("Are you sure you want to delete this project? This action cannot be undone.")
      },
      confirmButton = {
        TextButton(
            onClick = onConfirm,
            modifier = Modifier.testTag(ProjectListScreenTestTags.CONFIRM_DIALOG)) {
              Text("Delete")
            }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/**
 * Composable function representing a search bar. This has been written with the help of LLMs.
 *
 * @param value The current text in the search bar.
 * @param onValueChange Lambda function to call when the text changes.
 * @author Uri Jaquet
 */
@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String = ProjectListScreenTestTags.SEARCH_BAR,
    whatToSearchFor: String? = "a Project"
) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().testTag(testTag),
      horizontalArrangement = Arrangement.Center) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
              Text(
                  text = "Search for $whatToSearchFor",
                  color = NepTuneTheme.colors.onBackground,
                  style =
                      TextStyle(
                          fontSize = SEARCHBAR_FONT_SIZE.sp,
                          fontFamily = FontFamily(Font(R.font.markazi_text)),
                          fontWeight = FontWeight(100)))
            },
            modifier =
                Modifier.height(70.dp)
                    .testTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NepTuneTheme.colors.background, RoundedCornerShape(8.dp))
                    .padding(top = 9.dp, bottom = 9.dp),
            singleLine = true,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = NepTuneTheme.colors.searchBar,
                    unfocusedContainerColor = NepTuneTheme.colors.searchBar,
                    disabledContainerColor = NepTuneTheme.colors.searchBar,
                    cursorColor = Color.Unspecified,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = NepTuneTheme.colors.onBackground,
                    unfocusedTextColor = NepTuneTheme.colors.onBackground),
            leadingIcon = {
              Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = "Search Icon",
                  tint = NepTuneTheme.colors.onBackground,
                  modifier = Modifier.size(30.dp))
            },
        )
      }
}

/**
 * Preview function for the ProjectListScreen composable.
 *
 * @param navigateBack Lambda function to navigate back (default is empty).
 * @param onProjectClick Lambda function to navigate to the sampler screen (default is empty).
 * @author Uri Jaquet
 */
@Preview
@Composable
fun ProjectListScreenPreview(
    navigateBack: () -> Unit = {},
    onProjectClick: (ProjectItem) -> Unit = {},
) {
  val repo = TotalProjectItemsRepositoryProvider.repository
  runBlocking {
    repo.addProject(
        ProjectItem(
            uid = "1",
            name = "Project 1",
            description = "Description 1",
            isFavorite = false,
            tags = listOf(),
            audioPreviewCloudUri = null,
            projectFileCloudUri = null,
            lastUpdated = Timestamp.now(),
            ownerId = null,
            collaborators = listOf(),
        ))
    repo.addProject(
        ProjectItem(
            uid = "2",
            name = "Project 2",
            description = "Description 2",
            isFavorite = true,
            tags = listOf(),
            audioPreviewCloudUri = null,
            projectFileCloudUri = null,
            lastUpdated = Timestamp.now(),
            ownerId = null,
            collaborators = listOf(),
        ))
  }
  val vm = ProjectListViewModel(projectRepository = repo)

  // Provide an explicit NeptuneMediaPlayer for preview to avoid relying on composition local
  val previewPlayer = NeptuneMediaPlayer()

  ProjectListScreen(
      projectListViewModel = vm, onProjectClick = onProjectClick, mediaPlayer = previewPlayer)
}
