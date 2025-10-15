package com.neptune.neptune.ui.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.R
import com.neptune.neptune.Sample
import com.neptune.neptune.ui.theme.DarkBlue1
import com.neptune.neptune.ui.theme.DarkBlueGray
import com.neptune.neptune.ui.theme.FadedDarkBlue
import com.neptune.neptune.ui.theme.LightTurquoise

object ProjectListScreenTestTags {
  const val PROJECT_LIST_SCREEN = "ProjectListScreen"
  const val PROJECT_LIST = "projectList"
  const val PROJECT_CARD = "projectCard"
  const val SEARCH_BAR = "searchBar"
  const val BACK_BUTTON = "backButton"

  const val SEARCH_TEXT_FIELD = "searchTextField"
}

@Composable
// Implementation of the file access screen
fun ProjectListScreen(
    viewModel: ProjectListViewModel = viewModel(),
    onBack: () -> Unit = {},
    onFileSelected: (String) -> Unit = {}, // Return URI if needed
    onNavigateToSampler: () -> Unit = {}
) {
  val projects by viewModel.projects.collectAsState()
  val selectedProjects by viewModel.projectsSelected.collectAsState()

  Scaffold(
      modifier = Modifier.testTag(ProjectListScreenTestTags.PROJECT_LIST_SCREEN),
      containerColor = DarkBlue1) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

          // Top Search Bar with a back arrow
          TopSearch(onBack)
          // Columns of samples projects
          LazyColumn(
              modifier = Modifier.fillMaxSize().testTag(ProjectListScreenTestTags.PROJECT_LIST)) {
                items(projects) { project ->
                  ProjectSampleCard(
                      project,
                      project.id == selectedProjects?.id,
                      onClick = {
                        viewModel.selectProject(project)
                        if (project.uriString.isNotEmpty()) {
                          onFileSelected(project.uriString) // returns the URI
                          /*TODO: connect the URI (actually it's just a placeholder) */
                          onNavigateToSampler() // Navigate to the SamplerScreen
                        }
                      })
                }
              }
        }
      }
}

// ----------------Displays the Text and the Icon-----------------
@Composable
fun ProjectSample(sample: Sample) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Project Icon
          Icon(
              painterResource(R.drawable.file),
              contentDescription = "Project",
              tint = LightTurquoise,
              modifier = Modifier.size(26.dp))

          Spacer(modifier = Modifier.width(8.dp))

          // Sample Name Text
          Text(
              text = sample.name,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style =
                  TextStyle(
                      fontSize = 27.sp,
                      fontFamily = FontFamily(Font(R.font.markazi_text)),
                      fontWeight = FontWeight(150),
                      color = LightTurquoise))
        }
        val minutes = sample.durationSeconds / 60
        val seconds = sample.durationSeconds % 60
        val durationText = "%02d:%02d".format(minutes, seconds)

        // Duration Text
        Text(
            durationText,
            color = LightTurquoise,
            modifier = Modifier.padding(end = 16.dp),
            style =
                TextStyle(
                    fontSize = 27.sp,
                    fontFamily = FontFamily(Font(R.font.markazi_text)),
                    fontWeight = FontWeight(400)))
      }
}

// ----------------Top Bar Search--------------
@Composable
fun TopSearch(onBack: () -> Unit) {
  var searchText by remember { mutableStateOf("") }

  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().testTag(ProjectListScreenTestTags.SEARCH_BAR)) {
        // Back Icon
        IconButton(
            onClick = onBack, modifier = Modifier.testTag(ProjectListScreenTestTags.BACK_BUTTON)) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                  contentDescription = "Back",
                  tint = LightTurquoise,
                  modifier = Modifier.size(40.dp))
            }

        Spacer(modifier = Modifier.width(6.dp))

        // Search Field
        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = {
              Text(
                  text = "Search for a Project",
                  color = FadedDarkBlue,
                  style =
                      TextStyle(
                          fontSize = 21.sp,
                          fontFamily = FontFamily(Font(R.font.markazi_text)),
                          fontWeight = FontWeight(100)))
            },
            modifier =
                Modifier.height(70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBlue1, RoundedCornerShape(8.dp))
                    .padding(top = 9.dp, bottom = 9.dp)
                    .testTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD),
            singleLine = true,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = DarkBlueGray,
                    unfocusedContainerColor = DarkBlueGray,
                    disabledContainerColor = DarkBlueGray,
                    cursorColor = LightTurquoise,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = LightTurquoise,
                    unfocusedTextColor = LightTurquoise),

            // Search Icon
            leadingIcon = {
              Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = "Search Icon",
                  tint = FadedDarkBlue,
                  modifier = Modifier.size(30.dp))
            },
        )
      }
}

// ----------------Sample Project Card-----------------
@Composable
fun ProjectSampleCard(sample: Sample, isSelected: Boolean, onClick: () -> Unit) {

  val backGroundColor = if (isSelected) DarkBlueGray else DarkBlue1

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .clickable { onClick() }
              .testTag(ProjectListScreenTestTags.PROJECT_CARD),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      colors = CardDefaults.cardColors(backGroundColor),
      border = BorderStroke(1.dp, FadedDarkBlue),
      shape = RoundedCornerShape(0.dp)) {
        ProjectSample(sample)
      }
}

/*
@Preview
@Composable
fun ProjectListScreenPreview() {
  ProjectListScreen()
}*/
