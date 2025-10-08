package com.android.sample.ui.main

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MainViewModelTest {
  private lateinit var viewModel: MainViewModel

  @Before
  fun setup() {
    viewModel = MainViewModel()
  }

  @Test
  fun `discoverSamples loads correctly`() {
    val discover = viewModel.discoverSamples.value
    assertEquals(4, discover.size)
    assertEquals("Sample 1", discover[0].name)
  }

  @Test
  fun `followedSamples loads correctly`() {
    val followed = viewModel.followedSamples.value
    assertEquals(2, followed.size)
    assertEquals("Sample 5", followed[0].name)
  }
}
