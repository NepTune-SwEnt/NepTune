package com.neptune.neptune.screen

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.model.fakes.FakeProfileRepository
import com.neptune.neptune.model.fakes.FakeSampleRepository
import com.neptune.neptune.ui.main.MainViewModel
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MainViewModelTest {
  private lateinit var fakeRepository: FakeSampleRepository

  private lateinit var fakeProfileRepository: FakeProfileRepository
  private lateinit var viewModel: MainViewModel

  @Before
  fun setup() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    fakeRepository = FakeSampleRepository()
    fakeProfileRepository = FakeProfileRepository()
    viewModel =
        MainViewModel(
            application,
            repo = fakeRepository,
            profileRepo = fakeProfileRepository,
            useMockData = true)
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
