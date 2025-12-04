package com.neptune.neptune.model.sample

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.String
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
 * @authors Angéline Bignens, Tony Andriamampianina
 */
class SampleRepositoryFirebase(private val db: FirebaseFirestore) : SampleRepository {
  private val samples = db.collection(SAMPLES_COLLECTION_PATH)

  /** Returns a list of all samples in Firestore. */
  override suspend fun getSamples(): List<Sample> {
    val snap = samples.get().await()
    return snap.documents.mapNotNull { it.toSampleOrNull() }
  }

  override suspend fun getSample(sampleId: String): Sample {
    val snap = samples.document(sampleId).get().await()
    return snap.toSampleOrNull()
        ?: throw Exception("SampleRepositoryFirebase: Sample with id=$sampleId doesn't exist")
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

  /** Observe a single sample in real time */
  override fun observeSample(sampleId: String): Flow<Sample?> = callbackFlow {
    val listener =
        samples.document(sampleId).addSnapshotListener { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          if (snapshot != null && snapshot.exists()) {
            trySend(snapshot.toSampleOrNull())
          } else {
            trySend(null) // Le document a été supprimé ou n'existe pas
          }
        }
    awaitClose { listener.remove() }
  }

  /** Toggle like (increment or decrement count) */
  override suspend fun toggleLike(sampleId: String, isLiked: Boolean) {
    val userId =
        FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User must be logged in to like a sample")
    val sampleDoc = samples.document(sampleId)
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
  override suspend fun hasUserLiked(sampleId: String): Boolean {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return false
    val likeDoc = samples.document(sampleId).collection("likes").document(userId).get().await()
    return likeDoc.exists()
  }

  override suspend fun increaseDownloadCount(sampleId: String) {
    val sampleDoc = samples.document(sampleId)
    val snapshot = sampleDoc.get().await()
    check(snapshot.exists()) {
      "SampleRepositoryFirebase.toggleLike: Sample with id=$sampleId doesn't exist"
    }
    db.runTransaction { transaction ->
          val docSnapshot = transaction[sampleDoc]
          val currentDownloads = docSnapshot.getLong("downloads") ?: 0L
          val newDownloads = currentDownloads + 1
          transaction.update(sampleDoc, "downloads", newDownloads)
        }
        .await()
  }

  /** Add a new comment */
  override suspend fun addComment(
      sampleId: String,
      authorId: String,
      authorName: String,
      text: String
  ) {
    val sampleDoc = samples.document(sampleId)
    val snapshot = sampleDoc.get().await()

    // Throws IllegalStateException if the document does not exist
    check(snapshot.exists()) {
      "SampleRepositoryFirebase.toggleLike: Sample with id=$sampleId doesn't exist"
    }

    val comment =
        mapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "text" to text,
            "timestamp" to Timestamp.now())

    sampleDoc.collection("comments").add(comment).await()

    // Increment counter
    val currentCount = snapshot.getLong("comments") ?: 0L
    sampleDoc.update("comments", currentCount + 1).await()
  }
  /** Observe the comment of a sample */
  override fun observeComments(sampleId: String): Flow<List<Comment>> = callbackFlow {
    val listener =
        samples
            .document(sampleId)
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snap, error ->
              if (error != null) {
                close(error)
                return@addSnapshotListener
              }
              val comments =
                  snap?.documents?.mapNotNull { doc ->
                    val authorId = doc.getString("authorId") ?: return@mapNotNull null
                    val authorName = doc.getString("authorName") ?: ""
                    val text = doc.getString("text") ?: ""
                    val timestamp = doc.getTimestamp("timestamp")
                    Comment(
                        authorId = authorId,
                        authorName = authorName,
                        text = text,
                        timestamp = timestamp)
                  } ?: emptyList()
              trySend(comments)
            }
    awaitClose { listener.remove() }
  }

  /** Adds a new sample (for testing). */
  override suspend fun addSample(sample: Sample) {
    val data = sample.toMap().toMutableMap()
    data["creationTime"] = FieldValue.serverTimestamp()
    samples.document(sample.id).set(data).await()
  }
    /**
     * Fetches the most recent [limit] samples ordered by creationTime desc.
     */
    override suspend fun getLatestSamples(limit: Int): List<Sample> {
        val snap =
            samples.orderBy("creationTime", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
        return snap.documents.mapNotNull { it.toSampleOrNull() }
    }
    /**
     * Fetches trending samples based on a combined popularity score:
     *
     *   trending = downloads + 2 * likes
     *
     * Firestore can't sort by computed fields, so we fetch a batch ordered by downloads,
     * compute scores locally, and then return the top [limit].
     */
    override suspend fun getTrendingSamples(limit: Int): List<Sample> {
        // Fetch a big enough window to compute trending properly
        val snap = samples
            .orderBy("downloads", Query.Direction.DESCENDING)
            .limit((limit * 3).toLong()) // fetch more, then filter
            .get()
            .await()

        val all = snap.documents.mapNotNull { it.toSampleOrNull() }

        // Compute trending score
        val scored = all.map { sample ->
            val score = sample.downloads + sample.likes * 2 // trending formula
            sample to score
        }

        // Sort by score desc & return only top "limit"
        return scored
            .sortedByDescending { it.second }
            .take(limit.toInt())
            .map { it.first }
    }
    /**
     *
     */
    override suspend fun getSamplesByTags(tags: List<String>, perTagLimit: Int): List<Sample> {
        if (tags.isEmpty()) return emptyList()

        val all = mutableListOf<Sample>()
        val seenIds = mutableSetOf<String>()

        for (tag in tags.distinct()) {
            val snap =
                samples
                    .whereArrayContains("tags", tag)
                    .orderBy("creationTime", Query.Direction.DESCENDING)
                    .limit(perTagLimit.toLong())
                    .get()
                    .await()

            snap.documents.mapNotNull { it.toSampleOrNull() }.forEach { sample ->
                if (seenIds.add(sample.id)) {
                    all += sample
                }
            }
        }

        return all
    }

    /** Converts a Firestore [DocumentSnapshot] to a [Sample] instance, or null if missing. */
  private fun DocumentSnapshot.toSampleOrNull(): Sample? {
    if (!exists()) return null
    val id = getString("id") ?: return null
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
        ownerId = ownerId,
        isPublic = (get("isPublic") as? Boolean) ?: false,
        usersLike = (get("usersLike") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        storageZipPath = getString("storageZipPath").orEmpty(),
        storageImagePath = getString("storageImagePath").orEmpty(),
        storagePreviewSamplePath = getString("storagePreviewSamplePath").orEmpty(),
        creationTime = getTimestamp("creationTime")?.toDate()?.time ?: 0L)
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
          "ownerId" to ownerId,
          "isPublic" to isPublic,
          "usersLike" to usersLike,
          "storageZipPath" to storageZipPath,
          "storageImagePath" to storageImagePath,
          "storagePreviewSamplePath" to storagePreviewSamplePath)
}
