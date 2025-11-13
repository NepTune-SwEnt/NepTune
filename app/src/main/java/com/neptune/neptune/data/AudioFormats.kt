package com.neptune.neptune.data

object AudioFormats {
  val SUPPORTED_FORMATS: Map<String, String> =
      mapOf("mp3" to "audio/mpeg", "wav" to "audio/wav", "m4a" to "audio/mp4")

  val allowedExts: Set<String> = SUPPORTED_FORMATS.keys
  val allowedMimes: Set<String> = SUPPORTED_FORMATS.values.toSet()
  val supportedLabel: String = SUPPORTED_FORMATS.keys.joinToString("/") { it.uppercase() }

  fun extFromMime(mime: String?): String? =
      mime?.let { m -> SUPPORTED_FORMATS.entries.firstOrNull { it.value == m }?.key }

  fun mimeFromExt(ext: String?): String? = ext?.let { SUPPORTED_FORMATS[it] }
}
