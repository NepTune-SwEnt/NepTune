package com.neptune.neptune.ui.projectlist

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.R
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.media.NeptuneRecorder
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.ui.theme.NepTuneTheme
import java.io.File

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
  const val ADD_TO_CLOUD_BUTTON = "AddToCloudButton"
  const val REMOVE_FROM_CLOUD_BUTTON = "RemoveFromCloudButton"

  const val BUTTON_RECORD = "RecordFAB"
  const val MIC_ICON = "MicIcon"
  const val STOP_ICON = "StopIcon"
  const val BUTTON_CREATE = "ButtonCreate"
  const val BUTTON_CANCEL = "ButtonCancel"
  const val EMPTY_LIST = "EmptyList"
  const val NAME_FIELD = "NameField"

  const val IMPORT_AUDIO_BUTTON = "ImportAudioFAB"
}

private const val SEARCHBAR_FONT_SIZE = 21

/**
 * Composable function representing the Project List Screen. This has been written with the help of
 * LLMs.
 *
 * @param credentialManager Manages user credentials.
 * @param onProjectClick Lambda function to navigate.
 * @param projectListViewModel ViewModel managing the state of the project list.
 * @param recorder NeptuneRecorder instance for audio recording.
 * @param testRecordedFile test file to pre-populate the naming dialog.
 * @param onDeleteFailed callback triggered if deleting a temp file fails.
 * @author Uri Jaquet and Angéline Bignens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onProjectClick: (ProjectItem) -> Unit = {},
    projectListViewModel: ProjectListViewModel = viewModel(),
    recorder: NeptuneRecorder? = null,
    testRecordedFile: File? = null,
    onDeleteFailed: (() -> Unit)? = null,
) {
  val uiState by projectListViewModel.uiState.collectAsState()
  val projects: List<ProjectItem> = uiState.projects
  val selectedProjects: String? = uiState.selectedProject

  val context = LocalContext.current

  val pickAudio =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { projectListViewModel.importFromSaf(it.toString()) }
      }

  val actualRecorder = recorder ?: remember { NeptuneRecorder(context, StoragePaths(context)) }
  var isRecording by remember { mutableStateOf(actualRecorder.isRecording) }
  var hasAudioPermission by remember { mutableStateOf(false) }

  // Dialog state for naming the recorded project
  var showNameDialog by remember { mutableStateOf(false) }
  var proposedFileToImport by remember { mutableStateOf<File?>(null) }
  var projectName by remember { mutableStateOf("") }

  // If a test provides a recorded file, open the name dialog immediately so tests can exercise
  // the dialog actions without going through recording flow.
  LaunchedEffect(testRecordedFile) {
    testRecordedFile?.let {
      proposedFileToImport = it
      projectName = it.nameWithoutExtension
      showNameDialog = true
    }
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

  Scaffold(
      containerColor = NepTuneTheme.colors.background,
      floatingActionButton = {
        RecordControls(
            isRecording = isRecording,
            onToggleRecord = {
              // Toggle recording: start or stop and handle produced file
              if (isRecording) {
                val recorded =
                    try {
                      actualRecorder.stop()
                    } catch (_: Exception) {
                      null
                    }
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
                    /* ignore */
                  }
                } else {
                  permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              }
              isRecording = actualRecorder.isRecording
            },
            onImportAudio = { pickAudio.launch(arrayOf("audio/*")) })
      },
      content = { it ->
        Column(
            modifier =
                Modifier.testTag(ProjectListScreenTestTags.PROJECT_LIST_SCREEN)
                    .fillMaxSize()
                    .padding(it)) {
              SearchBar(value = searchText, onValueChange = { searchText = it })

              if (projects.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 5.dp)) {
                  Text(
                      "No projects yet.",
                      modifier = Modifier.testTag(ProjectListScreenTestTags.EMPTY_LIST),
                      style =
                          TextStyle(
                              fontSize = 25.sp,
                              fontFamily = FontFamily(Font(R.font.markazi_text)),
                              fontWeight = FontWeight(400),
                              color = NepTuneTheme.colors.onBackground))
                  Spacer(Modifier.height(10.dp))
                  Text(
                      "Tap “Import audio” to create a .neptune project (zip with config.json + audio).",
                      style =
                          TextStyle(
                              fontSize = 21.sp,
                              fontFamily = FontFamily(Font(R.font.markazi_text)),
                              fontWeight = FontWeight(400),
                              color = NepTuneTheme.colors.onBackground.copy(alpha = 0.75f)))
                }
              } else {
                ProjectList(
                    projects = filteredProjects,
                    selectedProject = selectedProjects,
                    modifier = Modifier.padding(it),
                    projectListViewModel = projectListViewModel,
                    onProjectClick = onProjectClick)
              }
            }
        // Name dialog
        if (showNameDialog && proposedFileToImport != null) {
          NameProjectDialog(
              projectName = projectName,
              onNameChange = { projectName = it },
              onConfirm = { name ->
                val finalFile = sanitizeAndRename(proposedFileToImport!!, name)
                projectListViewModel.importRecordedFile(finalFile)
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
      })
}

/**
 * Composable function to display a list of projects. This has been written with the help of LLMs.
 *
 * @param projects List of ProjectItem to display.
 * @param selectedProject ID of the currently selected project, if any.
 * @param modifier Modifier for styling.
 * @param projectListViewModel ViewModel managing the state of the project list.
 * @param onProjectClick Lambda function to navigate.
 * @author Uri Jaquet
 */
@Composable
fun ProjectList(
    modifier: Modifier = Modifier,
    projects: List<ProjectItem>,
    selectedProject: String? = null,
    projectListViewModel: ProjectListViewModel,
    onProjectClick: (ProjectItem) -> Unit = {},
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
              projectListViewModel = projectListViewModel)
        }
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
 * @author Uri Jaquet
 */
@Composable
fun ProjectListItem(
    project: ProjectItem,
    selectedProject: String? = null,
    onProjectClick: (ProjectItem) -> Unit = {},
    projectListViewModel: ProjectListViewModel,
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
              modifier = Modifier.padding(start = 5.dp),
              verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {},
                    content = {
                      Icon(
                          Icons.Default.PlayArrow,
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

  // Right  buttons
  Row {
    // Star favorite toggle
    IconButton(
        onClick = { projectListViewModel.toggleFavorite(project.uid) },
        modifier = Modifier.testTag("favorite_${project.uid}")) {
          Icon(
              imageVector = if (project.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
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
        if (!project.isStoredInCloud) {
          DropdownMenuItem(
              modifier = Modifier.testTag(ProjectListScreenTestTags.ADD_TO_CLOUD_BUTTON),
              text = { Text("Add to Cloud") },
              onClick = {
                projectListViewModel.addProjectToCloud(project.uid)
                expanded = false
              })
        } else {
          DropdownMenuItem(
              modifier = Modifier.testTag(ProjectListScreenTestTags.REMOVE_FROM_CLOUD_BUTTON),
              text = { Text("Remove from Cloud") },
              onClick = {
                projectListViewModel.removeProjectFromCloud(project.uid)
                expanded = false
              })
        }
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
 * Composable displaying the recording and import controls as floating action buttons. Shows a
 * record/stop button and an import audio button.This has been written with the help of LLMs.
 *
 * @param isRecording Indicates whether recording is currently active. Changes the icon accordingly.
 * @param onToggleRecord Lambda invoked when the record/stop button is clicked.
 * @param onImportAudio Lambda invoked when the import audio button is clicked.
 * @author Angéline Bignens
 */
@Composable
private fun RecordControls(
    isRecording: Boolean,
    onToggleRecord: () -> Unit,
    onImportAudio: () -> Unit,
) {
  Column(horizontalAlignment = Alignment.End) {
    FloatingActionButton(
        onClick = onToggleRecord,
        containerColor = NepTuneTheme.colors.postButton,
        modifier =
            Modifier.shadow(
                    elevation = 4.dp,
                    spotColor = NepTuneTheme.colors.shadow,
                    ambientColor = NepTuneTheme.colors.shadow)
                .padding(bottom = 16.dp)
                .testTag(ProjectListScreenTestTags.BUTTON_RECORD)) {
          Icon(
              if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
              contentDescription = if (isRecording) "Stop recording" else "Start recording",
              modifier =
                  Modifier.size(32.dp)
                      .testTag(
                          if (isRecording) ProjectListScreenTestTags.STOP_ICON
                          else ProjectListScreenTestTags.MIC_ICON),
              tint = NepTuneTheme.colors.onBackground)
        }
    ExtendedFloatingActionButton(
        onClick = onImportAudio,
        containerColor = NepTuneTheme.colors.postButton,
        modifier = Modifier.testTag(ProjectListScreenTestTags.IMPORT_AUDIO_BUTTON)) {
          Text(
              "Import audio",
              style =
                  TextStyle(
                      fontSize = 28.sp,
                      fontFamily = FontFamily(Font(R.font.markazi_text)),
                      fontWeight = FontWeight(400),
                      color = NepTuneTheme.colors.onBackground))
        }
  }
}

/**
 * Composable displaying a dialog to name a new project before saving/importing it.This has been
 * written with the help of LLMs.
 *
 * @param projectName Current text for the project name.
 * @param onNameChange Lambda invoked whenever the text in the input field changes.
 * @param onConfirm Lambda invoked when the user confirms the name. Returns the final name.
 * @param onCancel Lambda invoked when the dialog is dismissed or cancelled.
 * @author Angéline Bignens
 */
@Composable
private fun NameProjectDialog(
    projectName: String,
    onNameChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
  AlertDialog(
      onDismissRequest = onCancel,
      containerColor = NepTuneTheme.colors.listBackground,
      title = {
        Text(
            "Name project",
            style =
                TextStyle(
                    fontSize = 26.sp,
                    fontFamily = FontFamily(Font(R.font.markazi_text)),
                    fontWeight = FontWeight(500),
                    color = NepTuneTheme.colors.onBackground))
      },
      text = {
        Column {
          Text(
              "Enter a name for the new project",
              style =
                  TextStyle(
                      fontSize = 21.sp,
                      fontFamily = FontFamily(Font(R.font.markazi_text)),
                      fontWeight = FontWeight(300),
                      color = NepTuneTheme.colors.onBackground.copy(0.9f)))
          Spacer(Modifier.height(8.dp))
          TextField(
              value = projectName,
              onValueChange = onNameChange,
              modifier = Modifier.fillMaxWidth().testTag(ProjectListScreenTestTags.NAME_FIELD),
              textStyle =
                  TextStyle(
                      fontSize = 20.sp,
                      fontFamily = FontFamily(Font(R.font.markazi_text)),
                      color = NepTuneTheme.colors.onBackground),
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = NepTuneTheme.colors.background,
                      unfocusedContainerColor = NepTuneTheme.colors.background,
                      focusedIndicatorColor = NepTuneTheme.colors.accentPrimary,
                      unfocusedIndicatorColor = NepTuneTheme.colors.onBackground,
                      focusedTextColor = NepTuneTheme.colors.onBackground,
                      unfocusedTextColor = NepTuneTheme.colors.onBackground,
                      cursorColor = NepTuneTheme.colors.accentPrimary))
        }
      },
      confirmButton = {
        Button(
            modifier = Modifier.testTag(ProjectListScreenTestTags.BUTTON_CREATE),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = NepTuneTheme.colors.soundWave,
                    contentColor = NepTuneTheme.colors.background),
            onClick = { onConfirm(projectName) }) {
              Text(
                  "Create",
                  style =
                      TextStyle(
                          fontSize = 22.sp,
                          fontFamily = FontFamily(Font(R.font.markazi_text)),
                          fontWeight = FontWeight(400)))
            }
      },
      dismissButton = {
        Button(
            modifier = Modifier.testTag(ProjectListScreenTestTags.BUTTON_CANCEL),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = NepTuneTheme.colors.soundWave,
                    contentColor = NepTuneTheme.colors.background),
            onClick = onCancel) {
              Text(
                  "Cancel / Delete",
                  style =
                      TextStyle(
                          fontSize = 22.sp,
                          fontFamily = FontFamily(Font(R.font.markazi_text)),
                          fontWeight = FontWeight(400)))
            }
      })
}

/**
 * Sanitizes a project name and attempts to rename the given file accordingly. Invalid characters
 * are replaced with underscores, consecutive underscores are collapsed, and leading/trailing
 * special characters are removed.This has been written with the help of LLMs.
 *
 * If the desired file name already exists, the original file is returned.
 *
 * @param fileToImport The file to rename.
 * @param projectName The desired project name.
 * @return The renamed file if successful, or the original file if renaming failed or already
 *   exists.
 */
private fun sanitizeAndRename(fileToImport: File, projectName: String): File {
  val sanitized =
      projectName
          .replace(Regex("[^A-Za-z0-9._-]+"), "_")
          .replace(Regex("_+"), "_")
          .trim('_', '.', ' ')
          .ifEmpty { projectName }
  val ext = fileToImport.extension
  val parent = fileToImport.parentFile
  val desiredName = if (ext.isNotBlank()) "$sanitized.$ext" else sanitized
  val desiredFile = File(parent, desiredName)
  return if (desiredFile.exists()) {
    // avoid overwrite
    fileToImport
  } else {
    val moved =
        try {
          fileToImport.renameTo(desiredFile)
        } catch (_: Exception) {
          false
        }
    if (moved) desiredFile else fileToImport
  }
}
