package com.neptune.neptune.ui.projectlist

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Import the correct items function
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.credentials.CredentialManager
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.model.project.ProjectItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    navigateBack: () -> Unit = {},
    projectListViewModel: ProjectListViewModel = viewModel(),
) {
    val uiState by projectListViewModel.uiState.collectAsState()
    val projects: List<ProjectItem> = uiState.projects

    Scaffold(
        topBar = { TopAppBar({}) },
        bottomBar = { BottomAppBar { } },
        content = {
            ProjectList(
                projects = projects,
                modifier = Modifier.padding(it),
                projectListViewModel = projectListViewModel
            )
        }
    )
}

@Composable
fun ProjectList(
    projects: List<ProjectItem>,
    modifier: Modifier = Modifier,
    projectListViewModel: ProjectListViewModel,
) {
    Column {
        if (projects.isNotEmpty()) {
            LazyColumn(modifier = modifier) {
                items(
                    items = projects,
                    key = { project -> project.id }
                ) { project ->
                    ProjectListItem(
                        project = project,
                        openProject = { /* Nav to sample with project */ },
                        projectListViewModel = projectListViewModel
                    )
                }
            }
        }
        // This Text composable is likely for debugging and can be removed
        Text(projects.toString())
    }
}

@Composable
fun ProjectListItem(
    project: ProjectItem,
    openProject: () -> Unit = {},
    projectListViewModel: ProjectListViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        RenameProjectDialog(
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                // The id is guaranteed to be non-null here
                projectListViewModel.renameProject(project.id, newName)
                showRenameDialog = false
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable(onClick = openProject)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Text(text = project.name)
                Text(text = project.description)
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Edit")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showRenameDialog = true
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Change description") },
                        onClick = {
                            // This part of the UI doesn't seem to prompt for a new description.
                            // Assuming for now you want to clear it or implement a dialog later.
                            projectListViewModel.changeProjectDescription(project.id, "")
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sync/desync") },
                        onClick = {
                            // Placeholder
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            // The id is guaranteed to be non-null here
                            projectListViewModel.deleteProject(project.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RenameProjectDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Project") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("New name") }
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
