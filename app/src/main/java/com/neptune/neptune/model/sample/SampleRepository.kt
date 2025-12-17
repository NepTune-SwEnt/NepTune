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
   * Returns a flow of a sample available in the Firestore database.
   *
   * @param sampleId the Id of the sample we want to observe.
   * @return list of sample
   */
  fun observeSample(sampleId: String): Flow<Sample?>

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

  /**
   * Adds a new comment to a specific sample.
   *
   * @param sampleId the ID of the sample
   * @param authorId the ID of the person who commented
   * @param authorName the username of the person who comments
   * @param authorProfilePicUrl the profile picture of the user who comments
   * @param text the comment
   */
  suspend fun addComment(
      sampleId: String,
      authorId: String,
      authorName: String,
      text: String,
      authorProfilePicUrl: String = "",
  )

  /**
   * Deletes a comment from a specific sample. Implementations should identify the comment using the
   * authorId and timestamp. If timestamp is null, behavior is implementation-defined.
   *
   * @param sampleId the ID of the sample
   * @param authorId the ID of the comment's author
   * @param timestamp the timestamp of the comment (used to identify the precise comment)
   */
  suspend fun deleteComment(
      sampleId: String,
      authorId: String,
      timestamp: com.google.firebase.Timestamp?
  )

  /**
   * Observes real-time comments of a specific sample.
   *
   * @param sampleId the ID of the sample
   * @return a Flow lists of comments
   */
  fun observeComments(sampleId: String): Flow<List<Comment>>

  /**
   * Check if the user has already liked a sample.
   *
   * @param sampleId the ID of the sample
   * @return True if the user has already liked; false otherwise
   */
  suspend fun hasUserLiked(sampleId: String): Boolean

  /**
   * Increases the download count of a specific sample by one.
   *
   * @param sampleId the ID of the sample
   */
  suspend fun increaseDownloadCount(sampleId: String)
  /**
   * Fetch samples by recency.
   *
   * @param limit the number of samples to fetch
   */
  suspend fun getLatestSamples(limit: Int): List<Sample>

  /**
   * Fetch samples by popularity.
   *
   * @param limit the number of samples to fetch
   */
  suspend fun getTrendingSamples(limit: Int): List<Sample>

  /**
   * Fetch samples by tags.
   *
   * @param tags the tags to match
   * @param perTagLimit the number of samples to fetch per tag
   */
  suspend fun getSamplesByTags(tags: List<String>, perTagLimit: Int = 100): List<Sample>
}
