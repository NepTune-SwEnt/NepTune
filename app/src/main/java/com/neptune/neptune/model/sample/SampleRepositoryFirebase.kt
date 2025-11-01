package com.neptune.neptune.model.sample

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

const val SAMPLES_COLLECTION_PATH = "samples"

/**
 * Firebase-backed implementation of [SampleRepository] using Firestore.
 *
 * Handles loading, observing, and updating samples stored under `samples/{sampleId}`. This has been
 * written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class SampleRepositoryFirebase(private val db: FirebaseFirestore) : SampleRepository {
  private val samples = db.collection(SAMPLES_COLLECTION_PATH)

  /** Returns a list of all samples in Firestore. */
  override suspend fun getSamples(): List<Sample> {
    val snap = samples.get().await()
    return snap.toSamples()
  }

  /** Observe samples in real time */
  override fun observeSamples(): Flow<List<Sample>> = callbackFlow {
    val reg =
        samples.addSnapshotListener { snap, err ->
          if (err != null) {
            trySend(emptyList())
            return@addSnapshotListener
          }
          trySend(snap?.toSamples().orEmpty())
        }
    awaitClose { reg.remove() }
  }

  /** Toggle like (increment or decrement count) */
  override suspend fun toggleLike(sampleId: Int, isLiked: Boolean) {
    val sampleDoc = samples.document(sampleId.toString())
    db.runTransaction { transaction ->
          val snapshot = transaction.get(sampleDoc)
          val currentLikes = snapshot.getLong("likes") ?: 0L
          val newLikes = if (isLiked) currentLikes + 1 else maxOf(0, currentLikes - 1)
          transaction.update(sampleDoc, "likes", newLikes)
        }
        .await()
  }

  /** Adds a new sample (for testing). */
  override suspend fun addSample(sample: Sample) {
    samples.document(sample.id.toString()).set(sample.toMap()).await()
  }

  /** Converts a Firestore QuerySnapshot into a list of Sample */
  private fun QuerySnapshot.toSamples(): List<Sample> =
      documents.mapNotNull { doc ->
        try {
          Sample(
              id = doc.getLong("id")?.toInt() ?: return@mapNotNull null,
              name = doc.getString("name").orEmpty(),
              description = doc.getString("description").orEmpty(),
              durationSeconds = (doc.getLong("durationSeconds") ?: 0L).toInt(),
              tags = doc.get("tags") as? List<String> ?: emptyList(),
              likes = (doc.getLong("likes") ?: 0L).toInt(),
              comments = (doc.getLong("comments") ?: 0L).toInt(),
              downloads = (doc.getLong("downloads") ?: 0L).toInt(),
              uriString = doc.getString("uriString").orEmpty())
        } catch (e: Exception) {
          null
        }
      }

  private fun Sample.toMap(): Map<String, Any> =
      mapOf(
          "id" to id,
          "name" to name,
          "description" to description,
          "durationSeconds" to durationSeconds,
          "tags" to tags,
          "likes" to likes,
          "comments" to comments,
          "downloads" to downloads,
          "uriString" to uriString)
}
