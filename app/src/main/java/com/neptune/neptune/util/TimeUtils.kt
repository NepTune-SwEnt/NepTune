package com.neptune.neptune.util

import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

/**
 * Utility function that format a Timestamp into a readable string. This has been written with the
 * help of LLMs.
 *
 * @param timestamp The Timestamp to format.
 * @return a readable String of the Timestamp (of how long ago that timestamp occurred).
 * @author Angéline Bignens
 */
fun formatTime(timestamp: Timestamp?): String {
  if (timestamp == null) return ""

  val now = System.currentTimeMillis()
  val time = timestamp.toDate().time
  val diff = now - time

  val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
  val hours = TimeUnit.MILLISECONDS.toHours(diff)
  val days = TimeUnit.MILLISECONDS.toDays(diff)
  val months = days / 30
  val years = days / 365

  return when {
    minutes < 1 -> "just now"
    minutes < 60 -> "${minutes}min ago"
    hours < 24 -> "${hours}h ago"
    days < 30 -> "${days}d ago"
    days < 365 -> "${months}mo ago"
    else -> "${years}y ago"
  }
}
