@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.neptune.neptune.model.project

import kotlinx.serialization.Serializable

@Serializable
data class AudioFileMetadata(val name: String, val volume: Float, val durationSeconds: Float = 0f)

@Serializable
data class ParameterMetadata(val type: String, val value: Float, val targetAudioFile: String)

@Serializable
data class SamplerProjectData(
    val audioFiles: List<AudioFileMetadata>,
    val parameters: List<ParameterMetadata>,
)
