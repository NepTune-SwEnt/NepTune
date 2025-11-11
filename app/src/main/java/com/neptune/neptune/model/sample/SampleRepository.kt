package com.neptune.neptune.model.sample

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing samples stored in Firestore.
 *
 * Provides suspend functions to create, read, update, and observe audio samples. This has been
 * written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
interface SampleRepository {

  /**
   * Returns a list of all samples available in the Firestore database.
   *
   * @return list of sample
   */
  suspend fun getSamples(): List<Sample>

  /**
   * Returns the corresponding sample from the Firestore database.
   *
   * @param sampleId the ID of the sample
   * @return the sample
   */
  suspend fun getSample(sampleId: String): Sample

  /**
   * Observes real-time updates for all samples in Firestore.
   *
   * @return a flow of lists of sample as they change
   */
  fun observeSamples(): Flow<List<Sample>>

  /**
   * Adds a new sample under `samples/{sampleId}` in Firestore.
   *
   * @param sample the sample to be added
   */
  suspend fun addSample(sample: Sample)

  /**
   * Toggles the like count for a given sample (increment or decrement).
   *
   * @param sampleId the ID of the sample
   * @param isLiked true to increment likes, false to decrement
   */
  suspend fun toggleLike(sampleId: String, isLiked: Boolean)
}
