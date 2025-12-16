package com.neptune.neptune.ui.feed

import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.main.SampleResourceState
import kotlinx.coroutines.flow.StateFlow

interface SampleFeedController {
  fun loadSampleResources(sample: Sample)

  fun onDownloadZippedSample(sample: Sample)

  fun onDownloadProcessedSample(sample: Sample)

  fun onLikeClick(sample: Sample, isLiked: Boolean)

  fun onCommentClicked(sample: Sample)

  fun resetCommentSampleId()

  fun onAddComment(sampleId: String, text: String)

  /**
   * Request deletion of a comment attached to [sampleId]. Implementations should ensure only
   * authorized users can perform the deletion.
   */
  fun onDeleteComment(sampleId: String, authorId: String, timestamp: com.google.firebase.Timestamp?)

  fun isCurrentUser(ownerId: String): Boolean

  val usernames: StateFlow<Map<String, String>>
  val sampleResources: StateFlow<Map<String, SampleResourceState>>
}
