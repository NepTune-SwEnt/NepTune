package com.neptune.neptune.model

import com.google.firebase.Timestamp
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake Sample Repository for testing purposes. This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class FakeSampleRepository(initialSamples: List<Sample> = emptyList()) : SampleRepository {

  private val _samples = MutableStateFlow(initialSamples)
  private val samples = initialSamples.toMutableList()
  private val _commentsMap = mutableMapOf<Int, MutableStateFlow<List<Comment>>>()
  private val likedSamples = mutableSetOf<Int>()

  override suspend fun getSamples(): List<Sample> = samples.toList()

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

  override suspend fun hasUserLiked(sampleId: Int): Boolean {
    return likedSamples.contains(sampleId)
  }

  override suspend fun addComment(sampleId: Int, author: String, text: String) {
    val flow = _commentsMap.getOrPut(sampleId) { MutableStateFlow(emptyList()) }
    val currentComments = flow.value.toMutableList()
    val newComment = Comment(author = author, text = text, timestamp = Timestamp.now())
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

  override fun observeComments(sampleId: Int): Flow<List<Comment>> {
    return _commentsMap.getOrPut(sampleId) { MutableStateFlow(emptyList()) }.asStateFlow()
  }
}
