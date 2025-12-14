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
import com.neptune.neptune.ui.main.SampleResourceState
import com.neptune.neptune.ui.main.onClickFunctions

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
    navigateToProfile: () -> Unit,
    navigateToOtherUserProfile: (String) -> Unit,
    testTagsForSample: (Sample) -> BaseSampleTestTags,
    width: Dp,
    height: Dp,
    iconSize: Dp = 20.dp,
) {
  items(samples) { sample ->
    LaunchedEffect(sample.id, sample.storagePreviewSamplePath) {
      controller.loadSampleResources(sample)
    }
    val resources = sampleResources[sample.id] ?: SampleResourceState()
    val testTags = testTagsForSample(sample)
    val isLiked = likedSamples[sample.id] == true
    val actions =
        onClickFunctions(
            onDownloadClick = { controller.onDownloadZippedSample(sample) },
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
        iconSize = iconSize)
    Spacer(Modifier.height(12.dp))
  }
}
