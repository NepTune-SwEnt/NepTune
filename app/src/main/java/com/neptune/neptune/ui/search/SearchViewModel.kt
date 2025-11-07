package com.neptune.neptune.ui.search

import androidx.lifecycle.ViewModel
import com.neptune.neptune.model.sample.Sample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/*
Search ViewModel
Holds the list of samples and performs search filtering
Uses: MutableStateFlow to hold the list of samples
Provides: search function to filter samples based on query
Written with assistance from ChatGPT
 */

const val NATURE_TAG = "#nature"

open class SearchViewModel() : ViewModel() {
  private val _samples = MutableStateFlow<List<Sample>>(emptyList())
  val samples: StateFlow<List<Sample>> = _samples

  // TO DO : Load data from real source
  private fun loadData() {
    _samples.value =
        listOf(
            Sample(
                1,
                "Sample 1",
                "This is a sample description 1",
                21,
                listOf(NATURE_TAG),
                21,
                21,
                21),
            Sample(2, "Sample 2", "This is a sample description 2", 42, listOf("#sea"), 42, 42, 42),
            Sample(3, "Sample 3", "sea", 12, listOf("#relax", NATURE_TAG), 12, 12, 12),
            Sample(
                4, "nature", "This is a sample description 4", 2, listOf("#takeItEasy"), 1, 2, 1),
            Sample(
                5,
                "Sample 5",
                "This is a sample description 5",
                75,
                listOf(NATURE_TAG, "#forest"),
                210,
                210,
                210))
  }

  open fun search(query: String) {
    loadData()
    val normalizedQuery = normalize(query)
    if (normalizedQuery.isEmpty()) {
      return
    }
    _samples.value =
        _samples.value.filter {
          normalize(it.name).contains(normalizedQuery, ignoreCase = true) ||
              normalize(it.description).contains(normalizedQuery, ignoreCase = true) ||
              it.tags.any { tag -> normalize(tag).contains(normalizedQuery, ignoreCase = true) }
        }
  }
  // Normalizes text by converting it to lowercase and removing non-alphanumeric characters.
  fun normalize(text: String): String {
    // FYI .toRegex allows to detect patterns in strings
    return text.lowercase().replace("\\p{M}".toRegex(), "").replace(Regex("[^a-z0-9]"), "").trim()
  }
}
