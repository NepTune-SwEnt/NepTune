package com.neptune.neptune.ui.feed

enum class FeedType(val title: String) {
  DISCOVER("Discover"),
  FOLLOWED("Followed");

  fun toggle(): FeedType {
    return if (this == DISCOVER) FOLLOWED else DISCOVER
  }
}
