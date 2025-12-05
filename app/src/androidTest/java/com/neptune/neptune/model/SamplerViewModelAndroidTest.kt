import com.neptune.neptune.ui.sampler.SamplerViewModel
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SamplerFunctionsTest {

  private lateinit var vm: SamplerViewModel

  @Before
  fun setup() {
    vm = SamplerViewModel()
  }

  @Test
  fun testApplyADSR() {
    val samples = FloatArray(5) { 1f }
    val result =
        vm.applyADSR(samples, sampleRate = 1, attack = 0.2f, decay = 0f, sustain = 1f, release = 0f)

    assertEquals(5, result.size)
    result.forEach { assert(it in 0f..1f) }
  }

  @Test
  fun testApplyReverb() {
    val samples = FloatArray(5) { 1f }
    val result =
        vm.applyReverb(
            samples,
            sampleRate = 1,
            wet = 0.5f,
            size = 0.5f,
            width = 0.5f,
            depth = 0.5f,
            predelayMs = 0f)

    assertEquals(5, result.size)
    result.forEach { assert(abs(it) <= 1f) }
  }
}
