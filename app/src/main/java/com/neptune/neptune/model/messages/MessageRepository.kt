package com.neptune.neptune.model.messages

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing messages stored in Firestore.
 *
 * Provides functions to observe messages. This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
interface MessageRepository {

  /**
   * Observes all message previews for the current user. Real-time updates, sorted by lastTimestamp
   */
  fun observeMessagePreviews(currentUid: String): Flow<List<UserMessagePreview>>

  /** Observe real-time online/offline state of a user */
  fun observeUserOnlineState(uid: String): Flow<Boolean>

  /** Observe real-time messages for a conversation */
  fun observeMessages(conversationId: String): Flow<List<Message>>

  /** Send a message in a conversation */
  suspend fun sendMessage(conversationId: String, message: Message)
}
