package com.neptune.neptune.screen

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.model.fakes.FakeProfileRepository
import com.neptune.neptune.model.fakes.FakeSampleRepository
import com.neptune.neptune.ui.main.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MainViewModelTest {

  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
  private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

  private lateinit var mockContext: Context
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser

  private lateinit var fakeRepository: FakeSampleRepository
  private lateinit var fakeProfileRepository: FakeProfileRepository
  private lateinit var viewModel: MainViewModel

  @Before
  // This function was made using AI assistance
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    mockContext = mock()
    mockAuth = mock()
    mockFirebaseUser = mock()

    fakeRepository = FakeSampleRepository()
    fakeProfileRepository = FakeProfileRepository()

    whenever(mockAuth.currentUser).thenReturn(mockFirebaseUser)
    whenever(mockFirebaseUser.uid).thenReturn("fake_user_id_for_test")

    viewModel =
        MainViewModel(
            context = mockContext,
            repo = fakeRepository,
            profileRepo = fakeProfileRepository,
            useMockData = true,
            auth = mockAuth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
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
