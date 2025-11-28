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
fun SampleCommentManager(mainViewModel: MainViewModel) {
  val activeCommentSampleId by mainViewModel.activeCommentSampleId.collectAsState()
  val comments by mainViewModel.comments.collectAsState()
  val usernames by mainViewModel.usernames.collectAsState()

  if (activeCommentSampleId != null) {
    CommentDialog(
        sampleId = activeCommentSampleId!!,
        comments = comments,
        usernames = usernames,
        onDismiss = { mainViewModel.closeCommentSection() },
        onAddComment = { id, text -> mainViewModel.addComment(id, text) })
  }
}
