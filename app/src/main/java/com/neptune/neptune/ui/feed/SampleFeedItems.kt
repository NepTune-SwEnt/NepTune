package com.neptune.neptune.ui.feed

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.BaseSampleTestTags
import com.neptune.neptune.ui.main.SampleItem
import com.neptune.neptune.ui.main.SampleItemStyle
import com.neptune.neptune.ui.main.SampleResourceState
import com.neptune.neptune.ui.main.onClickFunctions

/** Data class to group navigation and action callbacks. */
data class FeedCallbacks(
    val onDownloadRequest: (Sample) -> Unit,
    val navigateToProfile: () -> Unit,
    val navigateToOtherUserProfile: (String) -> Unit
)

/** Data class to group UI dimensions and testing configuration. */
data class FeedItemStyle(
    val width: Dp,
    val height: Dp,
    val testTagsForSample: (Sample) -> BaseSampleTestTags,
    val iconSize: Dp = 20.dp
)

/**
 * Shared LazyColumn item builder for feeds showing sample cards.
 *
 * Keeps sample card rendering logic reusable while letting each screen own its surrounding
 * scrolling container.
 */
fun LazyListScope.sampleFeedItems(
    samples: List<Sample>,
    controller: SampleFeedController,
    mediaPlayer: NeptuneMediaPlayer,
    likedSamples: Map<String, Boolean>,
    sampleResources: Map<String, SampleResourceState>,
    feedItemStyle: FeedItemStyle,
    feedCallbacks: FeedCallbacks,
) {
  items(samples) { sample ->
    LaunchedEffect(sample.id, sample.storagePreviewSamplePath, sample.storageProcessedSamplePath) {
      controller.loadSampleResources(sample)
    }
    val resources = sampleResources[sample.id] ?: SampleResourceState()
    val testTags = feedItemStyle.testTagsForSample(sample)
    val isLiked = likedSamples[sample.id] == true
    val actions =
        onClickFunctions(
            onDownloadClick = { feedCallbacks.onDownloadRequest(sample) },
            onLikeClick = {
              val newIsLiked = !isLiked
              controller.onLikeClick(sample, newIsLiked)
            },
            onCommentClick = { controller.onCommentClicked(sample) },
            onProfileClick = {
              val ownerId = sample.ownerId
              if (ownerId.isNotBlank()) {
                if (controller.isCurrentUser(ownerId)) {
                  feedCallbacks.navigateToProfile()
                } else {
                  feedCallbacks.navigateToOtherUserProfile(ownerId)
                }
              }
            },
        )
    SampleItem(
        sample = sample,
        clickHandlers = actions,
        isLiked = likedSamples[sample.id] == true,
        testTags = testTags,
        mediaPlayer = mediaPlayer,
        resourceState = resources,
        sampleItemStyle =
            SampleItemStyle(
                width = feedItemStyle.width,
                height = feedItemStyle.height,
                iconSize = feedItemStyle.iconSize))
    Spacer(Modifier.height(12.dp))
  }
}
