package com.neptune.neptune.model.sample

import com.google.firebase.Timestamp

data class Comment(
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)
