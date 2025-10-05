package com.android.sample.ui.main

import androidx.lifecycle.ViewModel
import com.android.sample.Sample
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
            Sample(1, "Sample 1", "This is a sample description 1", "00:21", "#nature", 21, 21, 21),
            Sample(2, "Sample 2", "This is a sample description 2", "00:42", "#sea", 42, 42, 42),
            Sample(3, "Sample 3", "This is a sample description 3", "00:12", "#relax", 12, 12, 12),
            Sample(
                4, "Sample 4", "This is a sample description 4", "00:02", "#takeItEasy", 1, 2, 1),
        )
    _followedSamples.value =
        listOf(
            Sample(
                5, "Sample 5", "This is a sample description 5", "00:21", "#nature", 210, 210, 210),
            Sample(
                6, "Sample 6", "This is a sample description 6", "00:42", "#nature", 420, 420, 420),
        )
  }
}
