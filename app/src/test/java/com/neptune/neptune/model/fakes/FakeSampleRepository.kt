package com.neptune.neptune.model.fakes

import com.google.firebase.Timestamp
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake Sample Repository for testing purposes. This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class FakeSampleRepository(initialSamples: List<Sample> = emptyList()) : SampleRepository {

  private val _samples = MutableStateFlow(initialSamples)
  private val samples = initialSamples.toMutableList()
  private val _commentsMap = mutableMapOf<String, MutableStateFlow<List<Comment>>>()
  private val likedSamples = mutableSetOf<String>()

  override suspend fun getSamples(): List<Sample> = samples.toList()

  override fun observeSample(sampleId: String): Flow<Sample?> {
    return observeSamples().map { samples -> samples.find { it.id == sampleId } }
  }

  override suspend fun getSample(sampleId: String): Sample {
    return getSamples().first()
  }

  override fun observeSamples(): Flow<List<Sample>> = _samples.asStateFlow()

  override suspend fun addSample(sample: Sample) {
    samples.add(sample)
    _samples.value = samples.toList()
  }

  override suspend fun toggleLike(sampleId: String, isLiked: Boolean) {
    val index = samples.indexOfFirst { it.id == sampleId }
    if (index == -1) return
    val sample = samples[index]
    val currentlyLiked = likedSamples.contains(sampleId)

    val newLikes =
        when {
          isLiked && !currentlyLiked -> {
            likedSamples.add(sampleId)
            sample.likes + 1
          }
          !isLiked && currentlyLiked -> {
            likedSamples.remove(sampleId)
            (sample.likes - 1).coerceAtLeast(0)
          }
          else -> sample.likes
        }
    samples[index] = sample.copy(likes = newLikes)
    _samples.value = samples.toList()
  }

  override suspend fun hasUserLiked(sampleId: String): Boolean {
    return likedSamples.contains(sampleId)
  }

  override suspend fun increaseDownloadCount(sampleId: String) {
    val index = samples.indexOfFirst { it.id == sampleId }
    if (index == -1) return

    val sample = samples[index]
    val newDownloads = sample.downloads + 1

    samples[index] = sample.copy(downloads = newDownloads)

    // Emit new list for observers
    _samples.value = samples.toList()
  }

  override suspend fun getLatestSamples(limit: Int): List<Sample> {
    // Sort by creationTime descending, newest first
    return samples.sortedByDescending { it.creationTime }.take(limit.coerceAtLeast(0))
  }

  override suspend fun getTrendingSamples(limit: Int): List<Sample> {
    // Same trending formula as the real repo:
    // score = downloads + 2 * likes
    val scored =
        samples.map { sample ->
          val score = sample.downloads + sample.likes * 2
          sample to score
        }

    return scored.sortedByDescending { it.second }.take(limit.coerceAtLeast(0)).map { it.first }
  }

  override suspend fun getSamplesByTags(tags: List<String>, perTagLimit: Int): List<Sample> {
    if (tags.isEmpty()) return emptyList()

    val distinctTags = tags.distinct()
    val result = mutableListOf<Sample>()
    val seenIds = mutableSetOf<String>()

    for (tag in distinctTags) {
      val matchingForTag = samples.filter { sample -> tag in sample.tags }

      var addedForThisTag = 0

      for (sample in matchingForTag) {
        if (sample.id !in seenIds) {
          result += sample
          seenIds += sample.id
          addedForThisTag++

          if (perTagLimit > 0 && addedForThisTag >= perTagLimit) {
            break
          }
        }
      }
    }

    return result
  }

  override suspend fun addComment(
      sampleId: String,
      authorId: String,
      authorName: String,
      text: String,
      authorProfilePicUrl: String
  ) {
    addComment(sampleId, authorId, authorName, text, Timestamp.now())
  }

  // enable to give a custom Timestamp
  fun addComment(
      sampleId: String,
      authorId: String,
      authorName: String,
      text: String,
      timestamp: Timestamp
  ) {
    val flow = _commentsMap.getOrPut(sampleId) { MutableStateFlow(emptyList()) }
    val currentComments = flow.value.toMutableList()
    val newComment =
        Comment(authorName = authorName, authorId = authorId, text = text, timestamp = timestamp)
    currentComments.add(newComment)
    flow.value = currentComments.toList()

    // Update sample's comment count
    val index = samples.indexOfFirst { it.id == sampleId }
    if (index != -1) {
      val sample = samples[index]
      samples[index] = sample.copy(comments = currentComments.size)
      _samples.value = samples.toList()
    }
  }

  override fun observeComments(sampleId: String): Flow<List<Comment>> {
    return _commentsMap.getOrPut(sampleId) { MutableStateFlow(emptyList()) }.asStateFlow()
  }
}
