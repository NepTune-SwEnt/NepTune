package com.neptune.neptune

data class Sample(
    val id: Int,
    val name: String,
    val description: String,
    val durationSeconds: Int,
    val tags: List<String>,
    val likes: Int,
    val comments: Int,
    val downloads: Int,
    val uriString: String = ""
)
