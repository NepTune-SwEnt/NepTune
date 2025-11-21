package com.neptune.neptune.ui.projectlist

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
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.neptune.neptune.R
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.TotalProjectItemsRepositoryProvider
import com.neptune.neptune.ui.theme.NepTuneTheme
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
  const val ADD_TO_CLOUD_BUTTON = "AddToCloudButton"
  const val REMOVE_FROM_CLOUD_BUTTON = "RemoveFromCloudButton"
}

private const val SEARCHBAR_FONT_SIZE = 21

/**
 * Composable function representing the Project List Screen. This has been written with the help of
 * LLMs.
 *
 * @param credentialManager Manages user credentials.
 * @param onProjectClick Lambda function to navigate.
 * @param projectListViewModel ViewModel managing the state of the project list.
 * @author Uri Jaquet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onProjectClick: (ProjectItem) -> Unit = {},
    projectListViewModel: ProjectListViewModel = viewModel(),
) {
  val uiState by projectListViewModel.uiState.collectAsState()
  val projects: List<ProjectItem> = uiState.projects
  val selectedProjects: String? = uiState.selectedProject

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
      content = { it ->
        Column(
            modifier =
                Modifier.testTag(ProjectListScreenTestTags.PROJECT_LIST_SCREEN)
                    .fillMaxSize()
                    .padding(it)) {
              SearchBar(value = searchText, onValueChange = { searchText = it })
              ProjectList(
                  projects = filteredProjects,
                  selectedProject = selectedProjects,
                  modifier = Modifier.padding(it),
                  projectListViewModel = projectListViewModel,
                  onProjectClick = onProjectClick)
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
  val colorSearchBar = NepTuneTheme.colors.searchBar
  Column(
      modifier =
          modifier.drawBehind {
            drawLine(
                color = colorSearchBar,
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
  val lineColor = NepTuneTheme.colors.searchBar
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
                  color = NepTuneTheme.colors.searchBar,
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
                    focusedContainerColor = NepTuneTheme.colors.listBackground,
                    unfocusedContainerColor = NepTuneTheme.colors.listBackground,
                    disabledContainerColor = NepTuneTheme.colors.listBackground,
                    cursorColor = NepTuneTheme.colors.onBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = NepTuneTheme.colors.searchBar,
                    unfocusedTextColor = NepTuneTheme.colors.searchBar),
            leadingIcon = {
              Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = "Search Icon",
                  tint = NepTuneTheme.colors.searchBar,
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

  ProjectListScreen(projectListViewModel = vm, onProjectClick = onProjectClick)
}
