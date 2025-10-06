package com.android.sample.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.sample.ui.theme.SampleAppTheme

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    uiState: ProfileUiState,
    onEditClick: () -> Unit = {},
    onSaveClick: (name: String, username: String, bio: String) -> Unit = { _, _, _ -> },
    onNameChange: (String) -> Unit = {},
    onUsernameChange: (String) -> Unit = {},
    onBioChange: (String) -> Unit = {},
    onGoBack: () -> Unit = {},
) {
    val uiState = viewModel.uiState.collectAsState().value

    Column(modifier = Modifier.padding(16.dp)) {
        Header()
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            enabled = uiState.isInEditMode,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            enabled = uiState.isInEditMode,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.bio,
            onValueChange = onBioChange,
            label = { Text("Bio") },
            enabled = uiState.isInEditMode,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = onGoBack) {
                Text("Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (uiState.isInEditMode) {
                Button(onClick = { onSaveClick(uiState.name, uiState.username, uiState.bio) }) {
                    Text("Save")
                }
            } else {
                Button(onClick = onEditClick) {
                    Text("Edit")
                }
            }
        }
    }

}

@Preview
@Composable
fun ProfileScreenPreview() {
    SampleAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize().semantics {  },
            color = MaterialTheme.colorScheme.background) {
            ProfileScreen(
                uiState = ProfileUiState(
                    name = "John Doe",
                    username = "johndoe",
                    bio = "I'm awesome",
                    isInEditMode = false
                )
            )
        }


    }
}

@Composable
fun Header() {

}