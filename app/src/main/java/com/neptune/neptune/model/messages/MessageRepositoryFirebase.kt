package com.neptune.neptune.model.messages

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

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

                  // Build previews
                  val previews =
                      snapshot.documents.mapNotNull { doc ->
                        val participants =
                            (doc["participants"] as? List<*>)?.mapNotNull { it as? String }
                                ?: return@mapNotNull null
                        val otherUid =
                            participants.firstOrNull { it != currentUid } ?: return@mapNotNull null

                        val lastMessage = doc.getString("lastMessage")
                        val lastTimestamp = doc.getTimestamp("lastTimestamp")

                        Triple(otherUid, lastMessage, lastTimestamp)
                      }

                  launch {
                    val result =
                        previews.mapNotNull { (otherUid, msg, ts) ->
                          val profile: Profile? = profileRepo.getProfile(otherUid)
                          profile?.let {
                            UserMessagePreview(
                                profile = it,
                                lastMessage = msg,
                                lastTimestamp = ts,
                                isOnline = false)
                          }
                        }
                    trySend(result)
                  }
                }

        awaitClose { reg.remove() }
      }
  /** Observe online status in real time */
  override fun observeUserOnlineState(uid: String): Flow<Boolean> = callbackFlow {
    val ref =
        FirebaseDatabase.getInstance(
                "https://neptune-e2728-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("status/$uid")

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
}
