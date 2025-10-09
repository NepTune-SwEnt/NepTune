package com.android.sample.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.Sample
import com.android.sample.ui.theme.DarkBlue1
import com.android.sample.ui.theme.DarkBlue2
import com.android.sample.ui.theme.DarkBlueGray
import com.android.sample.ui.theme.LightPurpleBlue
import com.android.sample.ui.theme.LightTurquoise

object MainScreenTestTags {
  // General
  const val MAIN_SCREEN = "mainScreen"
  const val TOP_APP_BAR = "topAppBar"
  const val APP_TITLE = "appTitle"
  const val PROFILE_BUTTON = "profileButton"
  const val BOTTOM_NAVIGATION_BAR = "bottomNavigationBar"

  // Bottom navigation items
  const val NAV_HOME = "navHome"
  const val NAV_SEARCH = "navSearch"
  const val NAV_SAMPLER = "navSampler"
  const val NAV_NEW_POST = "navNewPost"

  // Sample Card
  const val SAMPLE_CARD = "sampleCard"
  const val SAMPLE_PROFILE_ICON = "sampleProfileIcon"
  const val SAMPLE_USERNAME = "sampleUsername"
  const val SAMPLE_NAME = "sampleName"
  const val SAMPLE_DURATION = "sampleDuration"
  const val SAMPLE_TAGS = "sampleTags"

  const val SAMPLE_LIKES = "sampleLikes"
  const val SAMPLE_COMMENTS = "sampleComments"
  const val SAMPLE_DOWNLOADS = "sampleDownloads"

  // Lazy column
  const val LAZY_COLUMN_SAMPLE_LIST = "sampleList"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Implementation of the main screen
fun MainScreen(mainViewModel: MainViewModel = viewModel()) {
  val discoverSamples by mainViewModel.discoverSamples.collectAsState()
  val followedSamples by mainViewModel.followedSamples.collectAsState()

  var selectedItem by remember { mutableStateOf(0) }

  Scaffold(
      modifier = Modifier.testTag(MainScreenTestTags.MAIN_SCREEN),
      topBar = {
        CenterAlignedTopAppBar(
            // App title
            title = {
              Text(
                  text = "NepTune",
                  style =
                      TextStyle(
                          fontSize = 45.sp,
                          fontFamily = FontFamily(Font(R.font.lily_script_one)),
                          fontWeight = FontWeight(149),
                          color = LightTurquoise,
                      ),
                  modifier =
                      Modifier.padding(vertical = 25.dp).testTag(MainScreenTestTags.APP_TITLE),
                  textAlign = TextAlign.Center)
            },
            // Profile icon
            actions = {
              IconButton(
                  onClick = { /*Does nothing for now, Todo: Add an action with the click*/},
                  modifier =
                      Modifier.padding(vertical = 25.dp, horizontal = 17.dp)
                          .size(57.dp)
                          .testTag(MainScreenTestTags.PROFILE_BUTTON)) {
                    Icon(
                        painter = painterResource(R.drawable.profile),
                        contentDescription = "Profile",
                        tint = Color.Unspecified // Keep the original icon color
                        )
                  }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkBlue1 // sets TopAppBar background
                    ),
            modifier =
                Modifier.fillMaxWidth().height(112.dp).testTag(MainScreenTestTags.TOP_APP_BAR))
      },
      bottomBar = {
        Column(modifier = Modifier.testTag(MainScreenTestTags.BOTTOM_NAVIGATION_BAR)) {
          HorizontalDivider(
              modifier = Modifier.fillMaxWidth(), thickness = 0.75.dp, color = LightTurquoise)
          NavigationBar(containerColor = DarkBlue1) {
            NavigationBarItem(
                icon = {
                  Icon(
                      painter = painterResource(R.drawable.home_planet),
                      contentDescription = "Home",
                      modifier = Modifier.size(33.dp))
                },
                selected = selectedItem == 0,
                onClick = { selectedItem = 0 },
                modifier = Modifier.testTag(MainScreenTestTags.NAV_HOME),
                alwaysShowLabel = false,
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = LightPurpleBlue,
                        unselectedIconColor = LightTurquoise,
                        indicatorColor = DarkBlue2))

            NavigationBarItem(
                icon = {
                  Icon(
                      Icons.Default.Search,
                      contentDescription = "Search",
                      modifier = Modifier.size(33.dp))
                },
                selected = selectedItem == 1,
                onClick = { selectedItem = 1 },
                modifier = Modifier.testTag(MainScreenTestTags.NAV_SEARCH),
                alwaysShowLabel = false,
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = LightPurpleBlue,
                        unselectedIconColor = LightTurquoise,
                        indicatorColor = DarkBlue2))

            NavigationBarItem(
                icon = {
                  Icon(
                      painter = painterResource(R.drawable.music_note),
                      contentDescription = "Sampler",
                      modifier = Modifier.size(33.dp))
                },
                selected = selectedItem == 2,
                onClick = { selectedItem = 2 },
                modifier = Modifier.testTag(MainScreenTestTags.NAV_SAMPLER),
                alwaysShowLabel = false,
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = LightPurpleBlue,
                        unselectedIconColor = LightTurquoise,
                        indicatorColor = DarkBlue2))

            NavigationBarItem(
                icon = {
                  Icon(
                      Icons.Default.Add,
                      contentDescription = "New Post",
                      modifier = Modifier.size(33.dp))
                },
                selected = selectedItem == 3,
                onClick = { selectedItem = 3 },
                modifier = Modifier.testTag(MainScreenTestTags.NAV_NEW_POST),
                alwaysShowLabel = false,
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = LightPurpleBlue,
                        unselectedIconColor = LightTurquoise,
                        indicatorColor = DarkBlue2))
          }
        }
      },
      containerColor = DarkBlue1) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          // Bottom border of the topAppBar
          HorizontalDivider(
              modifier = Modifier.fillMaxWidth(), thickness = 0.75.dp, color = LightTurquoise)
          LazyColumn(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(horizontal = 30.dp)
                      .testTag(MainScreenTestTags.LAZY_COLUMN_SAMPLE_LIST)) {
                // ----------------Discover Section-----------------
                item { SectionHeader(title = "Discover") }
                items(discoverSamples.chunked(2)) { samples -> SampleCardRow(samples) }

                // ----------------Followed Section-----------------
                item { SectionHeader(title = "Followed") }
                items(followedSamples.chunked(2)) { samples -> SampleCardRow(samples) }
              }
        }
      }
}

// ----------------Section Header-----------------
@Composable
fun SectionHeader(title: String) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = LightTurquoise,
            style =
                TextStyle(
                    fontSize = 37.sp,
                    fontFamily = FontFamily(Font(R.font.markazi_text)),
                    fontWeight = FontWeight(400)))
        IconButton(onClick = { /*Does nothing for now, Todo: Add an action with the click*/}) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = "See More",
              modifier = Modifier.size(40.dp),
              tint = LightTurquoise)
        }
      }
}

// ----------------Sample Card in Row (2 per row)-----------------
@Composable
fun SampleCardRow(samples: List<Sample>) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween) {
        samples.forEach { sample -> SampleCard(sample) }
      }
}

// ----------------Sample Card-----------------
@Composable
fun SampleCard(sample: Sample) {
  Card(
      modifier = Modifier.width(150.dp).height(166.dp).testTag(MainScreenTestTags.SAMPLE_CARD),
      colors = CardDefaults.cardColors(containerColor = DarkBlueGray),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, LightTurquoise)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
          // Profile and Name
          Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(start = 4.dp, top = 2.dp)) {
                Icon(
                    painter = painterResource(R.drawable.profile),
                    contentDescription = "Profile",
                    tint = Color.Unspecified, // Keep the original icon color,
                    modifier = Modifier.size(22.dp).testTag(MainScreenTestTags.SAMPLE_PROFILE_ICON))
                Spacer(Modifier.width(6.dp))
                Text(
                    /*Todo: Replace the hardCoded "Name" with the one provided by the Profile ViewModel*/
                    text = "Name",
                    color = LightTurquoise,
                    modifier = Modifier.testTag(MainScreenTestTags.SAMPLE_USERNAME),
                    style =
                        TextStyle(
                            fontSize = 19.sp,
                            fontFamily = FontFamily(Font(R.font.markazi_text)),
                            fontWeight = FontWeight(400)))
              }

          Spacer(Modifier.height(8.dp))

          // Waveform Image
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.waveform),
                contentDescription = "Waveform",
                modifier = Modifier.fillMaxWidth(0.8f).height(60.dp),
                alignment = Alignment.Center)
          }

          Spacer(Modifier.height(4.dp))

          // Sample name and duration
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    sample.name,
                    color = LightTurquoise,
                    modifier =
                        Modifier.padding(start = 6.dp).testTag(MainScreenTestTags.SAMPLE_NAME),
                    style =
                        TextStyle(
                            fontSize = 10.sp,
                            fontFamily = FontFamily(Font(R.font.markazi_text)),
                            fontWeight = FontWeight(400)))
                val minutes = sample.durationSeconds / 60
                val seconds = sample.durationSeconds % 60
                val durationText = "%02d:%02d".format(minutes, seconds)
                Text(
                    durationText,
                    color = LightTurquoise,
                    modifier =
                        Modifier.padding(end = 8.dp).testTag(MainScreenTestTags.SAMPLE_DURATION),
                    style =
                        TextStyle(
                            fontSize = 10.sp,
                            fontFamily = FontFamily(Font(R.font.markazi_text)),
                            fontWeight = FontWeight(400)))
              }
          // Bottom Turquoise bar
          Column(
              modifier =
                  Modifier.fillMaxWidth()
                      .background(LightTurquoise)
                      .padding(vertical = 4.dp, horizontal = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      Text(
                          sample.tags.joinToString(", "),
                          color = DarkBlue1,
                          modifier = Modifier.testTag(MainScreenTestTags.SAMPLE_TAGS),
                          style =
                              TextStyle(
                                  fontSize = 10.sp,
                                  fontFamily = FontFamily(Font(R.font.markazi_text)),
                                  fontWeight = FontWeight(400)))
                      Text(
                          text = "see more…",
                          color = DarkBlue1,
                          style =
                              TextStyle(
                                  fontSize = 10.sp,
                                  fontFamily = FontFamily(Font(R.font.markazi_text)),
                                  fontWeight = FontWeight(400)))
                    }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      IconWithText(
                          icon = Icons.Default.FavoriteBorder,
                          iconDescription = "Like",
                          text = sample.likes.toString(),
                          modifier = Modifier.testTag(MainScreenTestTags.SAMPLE_LIKES))
                      IconWithTextPainter(
                          icon = painterResource(R.drawable.comments),
                          iconDescription = "Comments",
                          text = sample.comments.toString(),
                          modifier = Modifier.testTag(MainScreenTestTags.SAMPLE_COMMENTS))
                      IconWithTextPainter(
                          icon = painterResource(R.drawable.download),
                          iconDescription = "Downloads",
                          text = sample.downloads.toString(),
                          modifier = Modifier.testTag(MainScreenTestTags.SAMPLE_DOWNLOADS))
                    }
              }
        }
      }
}

// Helper function for icons with text
@Composable
fun IconWithText(
    icon: ImageVector,
    iconDescription: String,
    text: String,
    modifier: Modifier = Modifier
) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
    Icon(
        icon,
        contentDescription = iconDescription,
        tint = DarkBlue1,
        modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(3.dp))
    Text(text, color = DarkBlue1, fontSize = 10.sp)
  }
}

// Helper function for icons with text but from a painterResource
@Composable
fun IconWithTextPainter(
    icon: Painter,
    iconDescription: String,
    text: String,
    modifier: Modifier = Modifier
) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
    Icon(
        icon,
        contentDescription = iconDescription,
        tint = DarkBlue1,
        modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(3.dp))
    Text(text, color = DarkBlue1, fontSize = 10.sp)
  }
}

/*
@Preview
@Composable
fun MainScreenPreview() {
  MainScreen()
}*/
