package com.android.sample.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.ui.theme.DarkBlue1
import com.android.sample.ui.theme.DarkBlue2
import com.android.sample.ui.theme.LightPurpleBlue
import com.android.sample.ui.theme.LightTurquoise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Implementation of the main screen
fun MainScreen(
    // --> ViewModel to pass when implemented
) {
  var selectedItem by remember { mutableStateOf(0) }

  Scaffold(
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
                  modifier = Modifier.padding(vertical = 25.dp),
                  textAlign = TextAlign.Center)
            },
            // Profile icon
            actions = {
              IconButton(
                  onClick = { /*Does nothing for now*/},
                  modifier = Modifier.padding(vertical = 25.dp, horizontal = 17.dp).size(57.dp)) {
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
            modifier = Modifier.fillMaxWidth().height(112.dp))
      },
      bottomBar = {
        Column {
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
        }
      }
}

@Preview
@Composable
fun MainScreenPreview() {
  MainScreen()
}
