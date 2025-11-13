package com.neptune.neptune.model.sample

data class Sample(
    val id: Int,
    val name: String,
    val description: String,
    val durationSeconds: Int,
    val tags: List<String>,
    val likes: Int,
    val comments: Int,
    val downloads: Int,
    val uriString: String = "",
    val ownerId: String = "",
    val storageZipPath: String = "",
    val storageImagePath: String = "",
    val storagePreviewSamplePath: String = ""
)
