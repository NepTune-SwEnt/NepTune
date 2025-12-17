package com.neptune.neptune.model.messages

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.util.RealtimeDatabaseProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Firebase-backed implementation of [MessageRepository] using Firestore.
 *
 * Handles observing message previews and online status under `status/$uid`. This has been written
 * with the help of LLMs.
 *
 * @authors Angéline Bignens
 */
class MessageRepositoryFirebase(
    private val firestore: FirebaseFirestore,
    private val profileRepo: ProfileRepository
) : MessageRepository {

  /** Observe messages preview in real time */
  override fun observeMessagePreviews(currentUid: String): Flow<List<UserMessagePreview>> =
      callbackFlow {
        val reg =
            firestore
                .collection("messages")
                .whereArrayContains("participants", currentUid)
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                  if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                  }

                  launch {
                    // Build preview
                    val previews =
                        snapshot.documents.mapNotNull { doc ->
                          val participants =
                              (doc["participants"] as? List<*>)?.mapNotNull { it as? String }
                                  ?: return@mapNotNull null

                          val otherUid =
                              participants.firstOrNull { it != currentUid }
                                  ?: return@mapNotNull null

                          val lastMessage = doc.getString("lastMessage") ?: ""
                          val lastTimestamp = doc.getTimestamp("lastTimestamp")

                          val profile: Profile? = profileRepo.getProfile(otherUid)
                          profile?.let {
                            UserMessagePreview(
                                profile = it,
                                lastMessage = lastMessage,
                                lastTimestamp = lastTimestamp,
                                isOnline = false)
                          }
                        }

                    trySend(previews)
                  }
                }

        awaitClose { reg.remove() }
      }
  /** Observe online status in real time */
  override fun observeUserOnlineState(uid: String): Flow<Boolean> = callbackFlow {
    val ref = RealtimeDatabaseProvider.getDatabase().getReference("status/$uid")

    val listener =
        object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            val state = snapshot.child("state").getValue(String::class.java)
            trySend(state == "online")
          }

          // Not needed
          override fun onCancelled(error: DatabaseError) {
            Log.w(
                "MessageRepoFirebase",
                "observeUserOnlineState listener cancelled: ${error.message}")
          }
        }

    ref.addValueEventListener(listener)

    awaitClose { ref.removeEventListener(listener) }
  }

  /** Observe messages in real time */
  override fun observeMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
    val ref =
        firestore
            .collection("messages")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

    val reg =
        ref.addSnapshotListener { snapshot, error ->
          if (error != null || snapshot == null) {
            trySend(emptyList())
            return@addSnapshotListener
          }

          val messages =
              snapshot.documents.mapNotNull { doc ->
                Message(
                    id = doc.id,
                    authorId = doc.getString("authorId") ?: return@mapNotNull null,
                    text = doc.getString("text") ?: "",
                    timestamp = doc.getTimestamp("timestamp"))
              }

          trySend(messages)
        }

    awaitClose { reg.remove() }
  }

  /** sends messages */
  override suspend fun sendMessage(conversationId: String, message: Message) {
    val convRef = firestore.collection("messages").document(conversationId)
    val msgRef = convRef.collection("messages").document()

    // Add message
    msgRef
        .set(
            mapOf(
                "authorId" to message.authorId,
                "text" to message.text,
                "timestamp" to FieldValue.serverTimestamp()))
        .await()
  }
}
