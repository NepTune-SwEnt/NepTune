package com.android.sample.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.sample.ui.theme.SampleAppTheme
import com.android.sample.R

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEditClick: () -> Unit = {},
    onSaveClick: (name: String, username: String, bio: String) -> Unit = { _, _, _ -> },
    onNameChange: (String) -> Unit = {},
    onUsernameChange: (String) -> Unit = {},
    onBioChange: (String) -> Unit = {},
) {
    // TODO: add profile picture, follower/following count and back button
    Column(modifier = Modifier.padding(16.dp)) {

        when (uiState.mode) {
            ProfileMode.VIEW -> {
                ProfileViewContent(
                    state = uiState,
                    onEdit = onEditClick,
                )
            }
            ProfileMode.EDIT -> {
                ProfileEditContent(
                    uiState = uiState,
                    onSave = { onSaveClick(uiState.name, uiState.username, uiState.bio) },
                    onNameChange = onNameChange,
                    onUsernameChange = onUsernameChange,
                    onBioChange = onBioChange
                )
            }
        }
    }
}

@Composable
private fun ProfileViewContent(
    state: ProfileUiState,
    onEdit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(20.dp))
        Avatar(
            showEditPencil = false
        )
        Spacer(Modifier.height(40.dp))

        Text(
            text = state.name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Text(
            text = "@${state.username}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(100.dp))

        Text(
            text = if (state.bio != "") "“ ${state.bio} ”" else "",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(200.dp))
        Row(Modifier.fillMaxWidth()) {
            StatBlock("Followers", state.followers, Modifier.weight(1f))
            StatBlock("Following", state.following, Modifier.weight(1f))
        }
        Spacer(Modifier.height(80.dp))

        Button(onClick = onEdit) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
            Spacer(Modifier.width(8.dp))
            Text("Edit")
        }
    }
}

@Composable
private fun ProfileEditContent(
    uiState: ProfileUiState,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Avatar(
            showEditPencil = true,
            onEditClick = { /* TODO: will open photo picker later */ }
        )
        Spacer(modifier = Modifier.height(40.dp))
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.nameError != null,
            supportingText = {
                val err = uiState.nameError
                if (err != null) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "${uiState.name.trim().length}/30",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.usernameError != null,
            supportingText = {
                val err = uiState.usernameError
                if (err != null) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "${uiState.username.trim().length}/15",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(40.dp))
        OutlinedTextField(
            value = uiState.bio,
            onValueChange = onBioChange,
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            isError = uiState.bioError != null,
            supportingText = {
                val err = uiState.bioError
                if (err != null) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "${uiState.bio.length}/160",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onSave,
            enabled = !uiState.isSaving && uiState.isValid
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
            Spacer(Modifier.width(8.dp))
            Text("Save")
        }
    }
}

@Composable
private fun StatBlock(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("$value", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun Avatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 120,
    showEditPencil: Boolean,
    onEditClick: () -> Unit = {} // currently NO-OP
) {
    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_avatar_placeholder),
            contentDescription = "Profile picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )

        if (showEditPencil) {
            SmallFloatingActionButton(
                onClick = onEditClick, // no-op for now
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit avatar")
            }
        }
    }
}



@Composable
fun ProfileScreenPreview(mode: ProfileMode) {
    SampleAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background) {
            ProfileScreen(
                uiState = ProfileUiState(
                    name = "John Doe",
                    username = "johndoe",
                    bio = "I'm awesome",
                    mode = mode
                )
            )
        }
    }
}

@Preview
@Composable
fun ProfileScreenViewModePreview() {
    ProfileScreenPreview(ProfileMode.VIEW)
}

@Preview
@Composable
fun ProfileScreenEditModePreview() {
    ProfileScreenPreview(ProfileMode.EDIT)
}

@Composable
fun ProfileRoute() {
    val viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state = viewModel.uiState.collectAsState().value

    ProfileScreen(
        uiState = state,
        onEditClick = viewModel::onEditClick,
        onSaveClick = { _, _, _ -> viewModel.onSaveClick() }, // VM reads from state
        onNameChange = viewModel::onNameChange,
        onUsernameChange = viewModel::onUsernameChange,
        onBioChange = viewModel::onBioChange,
    )
}
