package com.neptune.neptune.ui
/*
 * Tags for UI tests related to screens that display samples.
 */
interface BaseSampleTestTags {

  val prefix: String

  fun tag(name: String) = "$prefix/$name"

  // Common reusable elements for all screens with sample cards in them
  val SAMPLE_CARD
    get() = tag("sampleCard")

  val SAMPLE_PROFILE_ICON
    get() = tag("sampleProfileIcon")

  val SAMPLE_USERNAME
    get() = tag("sampleUsername")

  val SAMPLE_NAME
    get() = tag("sampleName")

  val SAMPLE_DURATION
    get() = tag("sampleDuration")

  val SAMPLE_TAGS
    get() = tag("sampleTags")

  val SAMPLE_LIKES
    get() = tag("sampleLikes")

  val SAMPLE_COMMENTS
    get() = tag("sampleComments")

  val SAMPLE_DOWNLOADS
    get() = tag("sampleDownloads")
}
