package com.neptune.neptune.ui.projectlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import com.neptune.neptune.model.project.ProjectItemsRepositoryProvider
import com.neptune.neptune.ui.theme.NepTuneTheme
import kotlinx.coroutines.runBlocking

object ProjectListScreenTestTags {
  const val PROJECT_LIST_SCREEN = "ProjectListScreen"
  const val PROJECT_LIST = "projectList"
  const val SEARCH_BAR = "searchBar"
  const val PROJECT_NAME = "ProjectName"
  const val DESCRIPTION_TEXT_FIELD = "DescriptionTextField"
  const val SEARCH_TEXT_FIELD = "SearchTextField"
}

private const val SEARCHBAR_FONT_SIZE = 21

/**
 * Composable function representing the Project List Screen. This has been written with the help of
 * LLMs.
 *
 * @param credentialManager Manages user credentials.
 * @param navigateToSampler Lambda function to navigate to the sampler screen.
 * @param projectListViewModel ViewModel managing the state of the project list.
 * @author Uri Jaquet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    navigateToSampler: () -> Unit = {},
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
      content = {
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
                  navigateToSampler = navigateToSampler)
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
 * @param navigateToSampler Lambda function to navigate to the sampler screen.
 * @author Uri Jaquet
 */
@Composable
fun ProjectList(
    projects: List<ProjectItem>,
    selectedProject: String? = null,
    modifier: Modifier = Modifier,
    projectListViewModel: ProjectListViewModel,
    navigateToSampler: () -> Unit = {},
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
        items(items = projects, key = { project -> project.id }) { project ->
          ProjectListItem(
              project = project,
              selectedProject = selectedProject,
              openProject = navigateToSampler,
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
 * @param openProject Lambda function to open the selected project.
 * @param projectListViewModel ViewModel managing the state of the project list.
 * @author Uri Jaquet
 */
@Composable
fun ProjectListItem(
    project: ProjectItem,
    selectedProject: String? = null,
    openProject: () -> Unit = {},
    projectListViewModel: ProjectListViewModel,
) {
  val backGroundColor =
      if (project.id == selectedProject) NepTuneTheme.colors.listBackground
      else NepTuneTheme.colors.background
  val lineColor = NepTuneTheme.colors.searchBar
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 2.dp)
              .clickable(
                  onClick = {
                    projectListViewModel.selectProject(project)
                    openProject()
                  })
              .drawBehind {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx())
              }
              .testTag("project_${project.id}"),
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

  if (showRenameDialog) {
    RenameProjectDialog(
        onDismiss = { showRenameDialog = false },
        onConfirm = { newName ->
          projectListViewModel.renameProject(project.id, newName)
          showRenameDialog = false
        })
  }

  if (showChangeDescDialog) {
    ChangeDescriptionDialog(
        initialDescription = project.description,
        onDismiss = { showChangeDescDialog = false },
        onConfirm = { newDesc ->
          projectListViewModel.changeProjectDescription(project.id, newDesc)
          showChangeDescDialog = false
        })
  }

  // Right  buttons
  Row {
    // Star favorite toggle
    IconButton(
        onClick = { projectListViewModel.toggleFavorite(project.id) },
        modifier = Modifier.testTag("favorite_${project.id}")) {
          Icon(
              imageVector = if (project.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
              contentDescription = "Favorite",
              tint = NepTuneTheme.colors.onBackground,
              modifier = Modifier.size(26.dp))
        }

    Box(modifier = Modifier.padding(end = 0.dp)) {
      IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("menu_${project.id}")) {
        Icon(
            Icons.Rounded.MoreVert,
            contentDescription = "Edit",
            tint = NepTuneTheme.colors.onBackground,
            modifier = Modifier.size(30.dp).padding(end = 0.dp),
        )
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Rename") },
            onClick = {
              showRenameDialog = true
              expanded = false
            })
        DropdownMenuItem(
            text = { Text("Change description") },
            onClick = {
              showChangeDescDialog = true
              expanded = false
            })
        DropdownMenuItem(
            text = {
              Text((if (project.isStoredInCloud) "Remove from cloud" else "Store in cloud"))
            },
            onClick = { expanded = false })
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
              projectListViewModel.deleteProject(project.id)
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
fun RenameProjectDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
  var text by remember { mutableStateOf("") }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Rename Project") },
      text = {
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("New name") })
      },
      confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Confirm") } },
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
      confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Confirm") } },
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
fun SearchBar(value: String, onValueChange: (String) -> Unit, testTag: String = ProjectListScreenTestTags.SEARCH_BAR,
              whatToSearchFor: String? = "a Project") {
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
 * @param navigateToSampler Lambda function to navigate to the sampler screen (default is empty).
 * @author Uri Jaquet
 */
@Preview
@Composable
fun ProjectListScreenPreview(
    navigateBack: () -> Unit = {},
    navigateToSampler: () -> Unit = {},
) {
  val repo = ProjectItemsRepositoryProvider.repository
  runBlocking {
    repo.addProject(
        ProjectItem(
            id = "1",
            name = "Project 1",
            description = "Description 1",
            isFavorite = false,
            tags = listOf(),
            previewUrl = null,
            fileUrl = null,
            lastUpdated = Timestamp.now(),
            ownerId = null,
            collaborators = listOf(),
        ))
    repo.addProject(
        ProjectItem(
            id = "2",
            name = "Project 2",
            description = "Description 2",
            isFavorite = true,
            tags = listOf(),
            previewUrl = null,
            fileUrl = null,
            lastUpdated = Timestamp.now(),
            ownerId = null,
            collaborators = listOf(),
        ))
  }
  val vm = ProjectListViewModel(projectRepository = repo)

  ProjectListScreen(projectListViewModel = vm, navigateToSampler = navigateToSampler)
}
