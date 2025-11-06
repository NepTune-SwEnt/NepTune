package com.neptune.neptune.model.sample

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
    return snap.documents.mapNotNull { it.toSampleOrNull() }
  }

  /** Observe samples in real time */
  override fun observeSamples(): Flow<List<Sample>> = callbackFlow {
    val reg =
        samples.addSnapshotListener { snap, err ->
          if (err != null) {
            trySend(emptyList())
            return@addSnapshotListener
          }
          trySend(snap?.documents?.mapNotNull { it.toSampleOrNull() }.orEmpty())
        }
    awaitClose { reg.remove() }
  }

  /** Toggle like (increment or decrement count) */
  override suspend fun toggleLike(sampleId: Int, isLiked: Boolean) {
    val sampleDoc = samples.document(sampleId.toString())

    val snapshot = sampleDoc.get().await()
    // Make sure the document exist
    if (!snapshot.exists()) {
      println("SampleRepositoryFirebase.toggleLike: Sample with id=$sampleId doesn't exist")
      return
    }
    db.runTransaction { transaction ->
          val docSnapshot = transaction[sampleDoc]
          val currentLikes = docSnapshot.getLong("likes") ?: 0L
          val newLikes = if (isLiked) currentLikes + 1 else maxOf(0, currentLikes - 1)
          transaction.update(sampleDoc, "likes", newLikes)
        }
        .await()
  }

  /** Adds a new sample (for testing). */
  override suspend fun addSample(sample: Sample) {
    samples.document(sample.id.toString()).set(sample.toMap()).await()
  }

  /** Converts a Firestore [DocumentSnapshot] to a [Sample] instance, or null if missing. */
  private fun DocumentSnapshot.toSampleOrNull(): Sample? {
    if (!exists()) return null
    val id = (get("id") as? Number)?.toInt() ?: return null
    val tags = (get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    val ownerId = getString("ownerId") ?: return null
    return Sample(
        id = id,
        name = getString("name").orEmpty(),
        description = getString("description").orEmpty(),
        durationSeconds = (get("durationSeconds") as? Number)?.toInt() ?: 0,
        tags = tags,
        likes = (get("likes") as? Number)?.toInt() ?: 0,
        comments = (get("comments") as? Number)?.toInt() ?: 0,
        downloads = (get("downloads") as? Number)?.toInt() ?: 0,
        uriString = getString("uriString").orEmpty(),
        ownerId = ownerId)
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
          "uriString" to uriString,
          "ownerId" to ownerId)
}
