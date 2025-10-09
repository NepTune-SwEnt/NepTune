package com.neptune.neptune.ui.main

import androidx.lifecycle.ViewModel
import com.neptune.neptune.Sample
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel() : ViewModel() {
  // No real data for now
  private val _discoverSamples = MutableStateFlow<List<Sample>>(emptyList())
  val discoverSamples: MutableStateFlow<List<Sample>> = _discoverSamples

  private val _followedSamples = MutableStateFlow<List<Sample>>(emptyList())
  val followedSamples: MutableStateFlow<List<Sample>> = _followedSamples

  init {
    loadData()
  }

  // Fake load data for now
  private fun loadData() {
    _discoverSamples.value =
        listOf(
            Sample(
                1, "Sample 1", "This is a sample description 1", 21, listOf("#nature"), 21, 21, 21),
            Sample(2, "Sample 2", "This is a sample description 2", 42, listOf("#sea"), 42, 42, 42),
            Sample(
                3, "Sample 3", "This is a sample description 3", 12, listOf("#relax"), 12, 12, 12),
            Sample(
                4, "Sample 4", "This is a sample description 4", 2, listOf("#takeItEasy"), 1, 2, 1),
        )
    _followedSamples.value =
        listOf(
            Sample(
                5,
                "Sample 5",
                "This is a sample description 5",
                75,
                listOf("#nature", "#forest"),
                210,
                210,
                210),
            Sample(
                6,
                "Sample 6",
                "This is a sample description 6",
                80,
                listOf("#nature"),
                420,
                420,
                420),
        )
  }
}
