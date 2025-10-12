package com.neptune.neptune.screen

import com.neptune.neptune.ui.main.MainViewModel
import org.junit.Assert
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
    Assert.assertEquals(4, discover.size)
    Assert.assertEquals("Sample 1", discover[0].name)
  }

  @Test
  fun `followedSamples loads correctly`() {
    val followed = viewModel.followedSamples.value
    Assert.assertEquals(2, followed.size)
    Assert.assertEquals("Sample 5", followed[0].name)
  }
}
