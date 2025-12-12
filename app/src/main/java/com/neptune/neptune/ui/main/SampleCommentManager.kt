package com.neptune.neptune.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Composable function is used to manage the comments. This has been written with the help of LLMs.
 *
 * @author Grégory Blanc
 */
@Composable
fun SampleCommentManager(mainViewModel: MainViewModel, onProfileClicked: (String) -> Unit) {
  val activeCommentSampleId by mainViewModel.activeCommentSampleId.collectAsState()
  val isAnonymous by mainViewModel.isAnonymous.collectAsState()

  if (activeCommentSampleId != null) {
    val comments by mainViewModel.comments.collectAsState()
    val usernames by mainViewModel.usernames.collectAsState()
    CommentDialog(
        sampleId = activeCommentSampleId!!,
        comments = comments,
        usernames = usernames,
        onDismiss = { mainViewModel.closeCommentSection() },
        onAddComment = { id, text -> mainViewModel.addComment(id, text) },
        onDeleteComment = { sampleId, authorId, timestamp ->
          mainViewModel.onDeleteComment(sampleId, authorId, timestamp)
        },
        isAnonymous = isAnonymous,
        onProfileClicked = onProfileClicked)
  }
}
