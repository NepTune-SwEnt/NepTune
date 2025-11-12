package com.neptune.neptune.model.sample

import com.google.firebase.Timestamp

data class Comment(
    val author: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)
