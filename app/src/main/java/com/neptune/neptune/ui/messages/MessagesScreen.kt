package com.neptune.neptune.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.neptune.neptune.R
import com.neptune.neptune.ui.theme.NepTuneTheme
import com.neptune.neptune.util.formatTime
import kotlinx.coroutines.launch

object MessagesScreenTestTags {
  const val MESSAGES_SCREEN = "MessagesScreen"
  const val TOP_BAR = "TopBarMessages"
  const val BACK_BUTTON = "BackButton"
  const val AVATAR = "Avatar"
  const val USERNAME = "Username"
  const val ONLINE_INDICATOR = "OnlineIndicator"

  const val MESSAGE_LIST = "MessageList"
  const val MESSAGE_BUBBLE = "MessageBubble"

  const val INPUT_BAR = "MessageInputBar"
  const val INPUT_FIELD = "MessageInputField"
  const val SEND_BUTTON = "SendButton"
}
/**
 * Factory used to create a [MessagesViewModel] instance with a specific user ID. This has been
 * written with the help of LLMs.
 *
 * @param uid The ID of the user whose conversation should be loaded.
 * @author Angéline Bignens
 */
class MessagesViewModelFactory(private val uid: String) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(MessagesViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST") return MessagesViewModel(uid) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}

/**
 * Composable function representing the Messages Screen. This has been written with the help of
 * LLMs.
 *
 * @param uid The ID of the user the conversation belongs to.
 * @param goBack Callback used for navigating back.
 * @param messagesViewModel Injected ViewModel.
 * @author Angéline Bignens
 */
@Composable
fun MessagesScreen(
    uid: String,
    goBack: () -> Unit,
    messagesViewModel: MessagesViewModel = viewModel(factory = MessagesViewModelFactory(uid)),
    autoScroll: Boolean = true // false when testing
) {
  val messages by messagesViewModel.messages.collectAsState()
  val otherUsername by messagesViewModel.otherUsername.collectAsState()
  val otherAvatar by messagesViewModel.otherAvatar.collectAsState()
  val isOnline by messagesViewModel.isOnline.collectAsState()
  val currentUserId = messagesViewModel.currentUserId
  val listState = rememberLazyListState()
  val coroutine = rememberCoroutineScope()

  // Scroll to latest message when list changes
  LaunchedEffect(messages.size) {
    if (autoScroll && messages.isNotEmpty()) {
      coroutine.launch { listState.animateScrollToItem(messages.lastIndex) }
    }
  }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(NepTuneTheme.colors.background)
              .testTag(MessagesScreenTestTags.MESSAGES_SCREEN)) {
        TopBarMessages(
            username = otherUsername, avatarUrl = otherAvatar, isOnline = isOnline, onBack = goBack)

        HorizontalDivider(
            thickness = 1.dp, color = NepTuneTheme.colors.onBackground.copy(alpha = 0.1f))

        LazyColumn(
            modifier =
                Modifier.weight(1f)
                    .padding(horizontal = 10.dp)
                    .testTag(MessagesScreenTestTags.MESSAGE_LIST),
            state = listState) {
              items(messages) { msg ->
                MessageBubble(
                    isMe = msg.authorId == currentUserId,
                    text = msg.text,
                    timestamp = msg.timestamp,
                    testTag = MessagesScreenTestTags.MESSAGE_BUBBLE)
              }
            }

        MessageInputBar(onSend = { text -> messagesViewModel.sendMessage(text) })
      }
}

/** Composable for the Top Bar */
@Composable
fun TopBarMessages(username: String, avatarUrl: String?, isOnline: Boolean, onBack: () -> Unit) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .background(NepTuneTheme.colors.background)
              .padding(horizontal = 10.dp, vertical = 10.dp)
              .testTag(MessagesScreenTestTags.TOP_BAR),
      verticalAlignment = Alignment.CenterVertically) {
        // Back Button
        IconButton(
            onClick = onBack, modifier = Modifier.testTag(MessagesScreenTestTags.BACK_BUTTON)) {
              Icon(
                  imageVector = Icons.Filled.ArrowBackIosNew,
                  contentDescription = "Back",
                  tint = NepTuneTheme.colors.onBackground,
                  modifier = Modifier.size(32.dp))
            }

        // Avatar
        AsyncImage(
            model = avatarUrl ?: R.drawable.profile,
            contentDescription = "Avatar",
            modifier =
                Modifier.size(40.dp).clip(CircleShape).testTag(MessagesScreenTestTags.AVATAR))

        // Online / Offline dot
        Box(
            modifier =
                Modifier.offset(x = (-12).dp, y = 14.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .testTag(MessagesScreenTestTags.ONLINE_INDICATOR)
                    .background(
                        color =
                            if (isOnline) NepTuneTheme.colors.online
                            else NepTuneTheme.colors.offline))

        Spacer(modifier = Modifier.width(6.dp))

        // Username
        Text(
            text = username,
            modifier = Modifier.testTag(MessagesScreenTestTags.USERNAME),
            color = NepTuneTheme.colors.onBackground,
            style = TextStyle(fontSize = 37.sp, fontFamily = FontFamily(Font(R.font.markazi_text))),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
      }
}

/** Composable to represent the text that a user writes */
@Composable
fun MessageBubble(isMe: Boolean, text: String, timestamp: Timestamp?, testTag: String) {
  val bubbleColor = if (isMe) NepTuneTheme.colors.animation else NepTuneTheme.colors.postButton
  val alignment = if (isMe) Arrangement.End else Arrangement.Start

  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag(testTag),
      horizontalArrangement = alignment) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
          Text(
              text = if (timestamp != null) "• " + formatTime(timestamp) else "",
              color = NepTuneTheme.colors.onBackground,
              style =
                  TextStyle(
                      fontSize = 18.sp,
                      fontWeight = FontWeight(400),
                      fontFamily = FontFamily(Font(R.font.markazi_text))),
              modifier = Modifier.padding(start = 3.dp, bottom = 2.dp))

          Box(
              modifier =
                  Modifier.background(bubbleColor, RoundedCornerShape(10.dp))
                      .padding(12.dp)
                      .widthIn(max = 260.dp)) {
                Text(
                    text = text,
                    color = NepTuneTheme.colors.onBackground,
                    style =
                        TextStyle(
                            fontSize = 21.sp,
                            fontWeight = FontWeight(400),
                            fontFamily = FontFamily(Font(R.font.markazi_text))))
              }
        }
      }
}

/**
 * Bottom input bar used to write and send messages.
 *
 * @param onSend Callback triggered when the send button is pressed.
 */
@Composable
fun MessageInputBar(onSend: (String) -> Unit) {
  var text by remember { mutableStateOf("") }

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp)
              .testTag(MessagesScreenTestTags.INPUT_BAR),
      verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier.weight(1f)
                    .height(57.dp)
                    .background(
                        color = NepTuneTheme.colors.searchBar, shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.CenterStart) {
              TextField(
                  value = text,
                  onValueChange = { text = it },
                  placeholder = {
                    Text(
                        text = "Message...",
                        color = NepTuneTheme.colors.onBackground.copy(alpha = 0.6f),
                        style =
                            TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight(500),
                                fontFamily = FontFamily(Font(R.font.markazi_text))))
                  },
                  modifier = Modifier.fillMaxSize().testTag(MessagesScreenTestTags.INPUT_FIELD),
                  colors =
                      TextFieldDefaults.colors(
                          focusedContainerColor = Color.Transparent,
                          unfocusedContainerColor = Color.Transparent,
                          focusedIndicatorColor = Color.Transparent,
                          unfocusedIndicatorColor = Color.Transparent,
                          cursorColor = NepTuneTheme.colors.onBackground,
                          focusedTextColor = NepTuneTheme.colors.onBackground,
                          unfocusedTextColor = NepTuneTheme.colors.onBackground),
                  maxLines = 3,
                  textStyle =
                      TextStyle(
                          fontSize = 24.sp,
                          fontFamily = FontFamily(Font(R.font.markazi_text)),
                          fontWeight = FontWeight(300),
                          color = NepTuneTheme.colors.onBackground))
            }

        Spacer(modifier = Modifier.width(10.dp))

        IconButton(
            onClick = {
              if (text.isNotBlank()) {
                onSend(text.trim())
                text = ""
              }
            },
            modifier =
                Modifier.testTag(MessagesScreenTestTags.SEND_BUTTON)
                    .size(57.dp)
                    .background(
                        color = NepTuneTheme.colors.postButton,
                        shape = RoundedCornerShape(16.dp))) {
              Icon(
                  painter = painterResource(id = R.drawable.messageicon),
                  contentDescription = "Send",
                  modifier = Modifier.size(30.dp),
                  tint = NepTuneTheme.colors.onBackground)
            }
      }
}
