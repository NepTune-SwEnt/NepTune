package com.neptune.neptune.model.messages

import com.google.firebase.Timestamp
import com.neptune.neptune.model.profile.Profile

data class UserMessagePreview(
    val profile: Profile,
    val lastMessage: String?,
    val lastTimestamp: Timestamp?,
    val isOnline: Boolean = false
) {
  val uid
    get() = profile.uid

  val username
    get() = profile.username
}
