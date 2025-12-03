package com.neptune.neptune.model.messages

import com.google.firebase.Timestamp

data class Messages(
    val id: String,
    val authorId: String,
    val text: String,
    val timestamp: Timestamp?,
)
