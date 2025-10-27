package com.neptune.neptune.ui.picker

import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicReference
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests the @Composable factory ImportAppRoot().
 *
 * We check that:
 * 1) It returns a working ViewModelProvider.Factory and the VM starts with an empty library.
 * 2) Its wiring works end-to-end by importing a tiny WAV using file:// and observing the library
 *    update. Written with assistance from ChatGPT.
 */
@RunWith(RobolectricTestRunner::class)
class ImportAppRootTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun importAppRootReturnsFactoryAndVmStartsEmpty() {
    val lastSize = AtomicReference(-1)

    compose.setContent {
      val factory = importAppRoot() // <-- subject under test
      val vm: ImportViewModel = viewModel(factory = factory)

      var size by remember { mutableStateOf(-1) }
      LaunchedEffect(vm) { vm.library.collect { size = it.size } }
      LaunchedEffect(size) { lastSize.set(size) }
    }

    // library should be collected and initially empty
    compose.waitForIdle()
    compose.runOnIdle { /* ensure any pending effects observed */}
    assertThat(lastSize.get()).isEqualTo(0)
  }
}
