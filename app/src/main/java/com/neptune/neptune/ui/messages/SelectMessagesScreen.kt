package com.neptune.neptune.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.neptune.neptune.R
import com.neptune.neptune.model.messages.UserMessagePreview
import com.neptune.neptune.ui.theme.NepTuneTheme
import com.neptune.neptune.util.formatTime

/**
 * Composable function representing the Select Message Screen. This has been written with the help
 * of LLMs.
 *
 * @author Angéline Bignens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectMessageScreen(
    goBack: () -> Unit,
    onSelectUser: (String) -> Unit,
    selectMessagesViewModel: SelectMessagesViewModel = viewModel()
) {
  val users by selectMessagesViewModel.users.collectAsState()
  Column(modifier = Modifier.fillMaxSize().background(NepTuneTheme.colors.background)) {
    TopAppBar(
        title = {
          Text(
              text = "Messages",
              color = NepTuneTheme.colors.onBackground,
              modifier = Modifier.padding(horizontal = 15.dp),
              fontSize = 40.sp,
              fontFamily = FontFamily(Font(R.font.markazi_text)),
              fontWeight = FontWeight(400),
          )
        },
        navigationIcon = {
          Icon(
              imageVector = Icons.Filled.ArrowBackIosNew,
              contentDescription = "Back",
              tint = NepTuneTheme.colors.onBackground,
              modifier = Modifier.padding(start = 8.dp).size(32.dp).clickable { goBack() })
        },
        colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = NepTuneTheme.colors.background,
                navigationIconContentColor = NepTuneTheme.colors.onBackground,
                actionIconContentColor = NepTuneTheme.colors.onBackground))

    HorizontalDivider(thickness = 1.dp, color = NepTuneTheme.colors.onBackground.copy(alpha = 0.1f))

    // When no conversations
    if (users.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No conversations yet",
            fontFamily = FontFamily(Font(R.font.markazi_text)),
            color = NepTuneTheme.colors.onBackground.copy(alpha = 0.6f),
            fontSize = 30.sp)
      }
      return
    }

    // List of Users
    LazyColumn(
        modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          items(users) { user ->
            UserMessagePreviewRow(user = user, onClick = { onSelectUser(user.uid) })
          }
        }
  }
}

@Composable
fun UserMessagePreviewRow(user: UserMessagePreview, onClick: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically) {
        // Avatar + online indicator
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.BottomEnd) {
          AsyncImage(
              model = user.profile.avatarUrl.ifEmpty { R.drawable.profile },
              contentDescription = "Avatar",
              modifier = Modifier.size(48.dp).clip(CircleShape),
              contentScale = ContentScale.Crop,
              placeholder = painterResource(id = R.drawable.profile),
              error = painterResource(id = R.drawable.profile))

          // Online / Offline dot
          Box(
              modifier =
                  Modifier.size(12.dp)
                      .clip(CircleShape)
                      .background(
                          color =
                              if (user.isOnline) NepTuneTheme.colors.online
                              else NepTuneTheme.colors.offline,
                          shape = CircleShape))
        }

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
          // Username
          Text(
              text = user.username,
              fontSize = 30.sp,
              color = NepTuneTheme.colors.onBackground,
              lineHeight = 33.sp,
              fontWeight = FontWeight(400),
              fontFamily = FontFamily(Font(R.font.markazi_text)),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)
          // Last Message
          Text(
              text = user.lastMessage ?: "No messages yet",
              fontFamily = FontFamily(Font(R.font.markazi_text)),
              fontSize = 19.sp,
              fontWeight = FontWeight(400),
              color = NepTuneTheme.colors.onBackground.copy(alpha = 0.7f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)
        }

        // Time of last message
        user.lastTimestamp?.let {
          Text(
              text = formatTime(it),
              fontFamily = FontFamily(Font(R.font.markazi_text)),
              fontSize = 21.sp,
              fontWeight = FontWeight(400),
              color = NepTuneTheme.colors.onBackground)
        }
      }
}
