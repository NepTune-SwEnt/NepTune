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
  val activeSample by mainViewModel.activeCommentSample.collectAsState()
  val isAnonymous by mainViewModel.isAnonymous.collectAsState()
  val currentUser by mainViewModel.currentUser.collectAsState()

  if (activeCommentSampleId != null) {
    val comments by mainViewModel.comments.collectAsState()
    val usernames by mainViewModel.usernames.collectAsState()
    val discoverSamples by mainViewModel.discoverSamples.collectAsState()
    val followedSamples by mainViewModel.followedSamples.collectAsState()

    // Priority to activeSample (most reliable), fallback to lists
    val sampleOwnerId =
        activeSample?.ownerId
            ?: (discoverSamples + followedSamples)
                .firstOrNull { it.id == activeCommentSampleId }
                ?.ownerId

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
        onProfileClicked = onProfileClicked,
        sampleOwnerId = sampleOwnerId,
        currentUserId = currentUser?.uid)
  }
}
