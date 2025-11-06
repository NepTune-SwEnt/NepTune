package com.neptune.neptune.model

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

  override suspend fun getSamples(): List<Sample> = samples.toList()

  override fun observeSamples(): Flow<List<Sample>> = _samples.asStateFlow()

  override suspend fun addSample(sample: Sample) {
    samples.add(sample)
    _samples.value = samples.toList()
  }

  override suspend fun toggleLike(sampleId: Int, isLiked: Boolean) {
    val index = samples.indexOfFirst { it.id == sampleId }
    if (index == -1) return
    val sample = samples[index]
    val newLikes = (sample.likes + if (isLiked) 1 else -1).coerceAtLeast(0)
    samples[index] = sample.copy(likes = newLikes)
    _samples.value = samples.toList()
  }
}
