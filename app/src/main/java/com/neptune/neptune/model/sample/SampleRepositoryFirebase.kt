package com.neptune.neptune.model.sample

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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
    val userId =
        FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User must be logged in to like a sample")
    val sampleDoc = samples.document(sampleId.toString())
    val likeDoc = sampleDoc.collection("likes").document(userId)

    val snapshot = sampleDoc.get().await()

    // Throws IllegalStateException if the document does not exist
    check(snapshot.exists()) {
      "SampleRepositoryFirebase.toggleLike: Sample with id=$sampleId doesn't exist"
    }
    db.runTransaction { transaction ->
          val likeSnapshot = transaction[likeDoc]
          val sampleSnapshot = transaction[sampleDoc]
          val currentLikes = sampleSnapshot.getLong("likes") ?: 0L

          // Cannot like if already like
          if (likeSnapshot.exists()) {
            transaction.delete(likeDoc)
            transaction.update(sampleDoc, "likes", maxOf(0, currentLikes - 1))
          } else {
            transaction[likeDoc] = mapOf("liked" to true)
            transaction.update(sampleDoc, "likes", currentLikes + 1)
          }
        }
        .await()
  }

  /** Check if the user has already like a sample */
  override suspend fun hasUserLiked(sampleId: Int): Boolean {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return false
    val likeDoc =
        samples.document(sampleId.toString()).collection("likes").document(userId).get().await()
    return likeDoc.exists()
  }

  /** Add a new comment */
  override suspend fun addComment(sampleId: Int, author: String, text: String) {
    val sampleDoc = samples.document(sampleId.toString())
    val snapshot = sampleDoc.get().await()

    // Throws IllegalStateException if the document does not exist
    check(snapshot.exists()) {
      "SampleRepositoryFirebase.toggleLike: Sample with id=$sampleId doesn't exist"
    }

    val comment = mapOf("author" to author, "text" to text, "timestamp" to Timestamp.now())

    sampleDoc.collection("comments").add(comment).await()

    // Increment counter
    val currentCount = snapshot.getLong("comments") ?: 0L
    sampleDoc.update("comments", currentCount + 1).await()
  }
  /** Observe the comment of a sample */
  override fun observeComments(sampleId: Int): Flow<List<Comment>> = callbackFlow {
    val listener =
        samples
            .document(sampleId.toString())
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snap, error ->
              if (error != null) {
                close(error)
                return@addSnapshotListener
              }
              val comments =
                  snap?.documents?.mapNotNull { it.toObject(Comment::class.java) }.orEmpty()
              trySend(comments)
            }
    awaitClose { listener.remove() }
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
