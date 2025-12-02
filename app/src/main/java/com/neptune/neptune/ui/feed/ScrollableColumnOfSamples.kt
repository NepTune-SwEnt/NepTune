package com.neptune.neptune.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.BaseSampleTestTags
import com.neptune.neptune.ui.main.CommentDialog
import com.neptune.neptune.ui.main.SampleItem
import com.neptune.neptune.ui.main.SampleResourceState
import com.neptune.neptune.ui.main.onClickFunctions
import com.neptune.neptune.ui.theme.NepTuneTheme

/**
 * Shared scrolling list of sample cards used by search/profile feeds.
 *
 * The UI remains the same; test tags and navigation callbacks are injected to adapt to different
 * screens.
 */
@Composable
fun ScrollableColumnOfSamples(
    samples: List<Sample>,
    controller: SampleFeedController,
    modifier: Modifier = Modifier,
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current,
    likedSamples: Map<String, Boolean> = emptyMap(),
    activeCommentSampleId: String? = null,
    comments: List<Comment> = emptyList(),
    navigateToProfile: () -> Unit = {},
    navigateToOtherUserProfile: (String) -> Unit = {},
    sampleResources: Map<String, SampleResourceState> = emptyMap(),
    listTestTag: String? = null,
    testTagsForSample: (Sample) -> BaseSampleTestTags = {
      object : BaseSampleTestTags {
        override val prefix = "sampleList"
      }
    },
) {
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val width = screenWidth - 20.dp
  val height = width * (150f / 166f) // the same ratio than in the feedScreen

  val listModifier =
      (if (listTestTag != null) modifier.testTag(listTestTag) else modifier)
          .fillMaxSize()
          .background(NepTuneTheme.colors.background)

  LazyColumn(
      modifier = listModifier,
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        items(samples) { sample ->
          LaunchedEffect(sample.id, sample.storagePreviewSamplePath) {
            controller.loadSampleResources(sample)
          }
          val resources = sampleResources[sample.id] ?: SampleResourceState()
          val testTags = testTagsForSample(sample)
          val isLiked = likedSamples[sample.id] == true
          val actions =
              onClickFunctions(
                  onDownloadClick = { controller.onDownloadSample(sample) },
                  onLikeClick = {
                    val newIsLiked = !isLiked
                    controller.onLikeClick(sample, newIsLiked)
                  },
                  onCommentClick = { controller.onCommentClicked(sample) },
                  onProfileClick = {
                    val ownerId = sample.ownerId
                    if (ownerId.isNotBlank()) {
                      if (controller.isCurrentUser(ownerId)) {
                        navigateToProfile()
                      } else {
                        navigateToOtherUserProfile(ownerId)
                      }
                    }
                  },
              )
          SampleItem(
              sample = sample,
              width = width,
              height = height,
              clickHandlers = actions,
              isLiked = likedSamples[sample.id] == true,
              testTags = testTags,
              mediaPlayer = mediaPlayer,
              resourceState = resources,
              iconSize = 20.dp)
        }
      }

  if (activeCommentSampleId != null) {
    val usernames = controller.usernames.collectAsState()
    Box(modifier = Modifier.fillMaxSize().padding(bottom = 0.dp)) {
      CommentDialog(
          sampleId = activeCommentSampleId,
          comments = comments,
          usernames = usernames.value,
          onDismiss = { controller.resetCommentSampleId() },
          onAddComment = { id, text -> controller.onAddComment(id, text) })
    }
  }
}
