package com.neptune.neptune.media

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.neptune.neptune.ui.sampler.SamplerViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.math.sin
import java.io.File

interface AudioProcessor {
    suspend fun process(
        uri: Uri,
        eqBands: List<Float>,
        reverbWet: Float,
        reverbSize: Float,
        reverbWidth: Float,
        reverbDepth: Float,
        reverbPredelay: Float
    ): Uri?
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReverbTests {

    private lateinit var vm: SamplerViewModel
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = spyk(SamplerViewModel())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun wetZeroReturnsDrySignal() = runTest {
        val samples = FloatArray(500) { sin(it * 0.1f) }

        val out = vm.applyReverb(
            input = samples,
            sampleRate = 44100,
            wet = 0f,
            size = 0.7f,
            width = 0.5f,
            depth = 0.5f,
            predelayMs = 40f
        )

        for (i in samples.indices) {
            assertThat(out[i]).isWithin(1e-6f).of(samples[i])
        }
    }

    @Test
    fun wetOneProducesDifferentSignal() = runTest {
        val samples = FloatArray(500) { sin(it * 0.03f) }

        val out = vm.applyReverb(
            input = samples,
            sampleRate = 44100,
            wet = 1f,
            size = 0.9f,
            width = 0.8f,
            depth = 0.8f,
            predelayMs = 0f
        )

        assertThat(out.toList()).isNotEqualTo(samples.toList())
    }

    @Test
    fun predelayAddsSilentStart() = runTest {
        val samples = FloatArray(500) { 1f }

        val out = vm.applyReverb(
            input = samples,
            sampleRate = 1000,
            wet = 1f,
            size = 0.5f,
            width = 0.5f,
            depth = 0.5f,
            predelayMs = 50f
        )

        for (i in 0 until 50) {
            assertThat(abs(out[i])).isLessThan(1e-4f)
        }
    }

    @Test
    fun predelayShiftsReverbStart() = runTest {
        val impulse = FloatArray(3000) { if (it == 0) 1f else 0f }

        val noDelay = vm.applyReverb(
            input = impulse,
            sampleRate = 44100,
            wet = 1f,
            size = 0.5f,
            width = 0.5f,
            depth = 0.5f,
            predelayMs = 0f
        )

        val withDelay = vm.applyReverb(
            input = impulse,
            sampleRate = 44100,
            wet = 1f,
            size = 0.5f,
            width = 0.5f,
            depth = 0.5f,
            predelayMs = 50f   // 50ms = 2205 samples
        )

        fun firstEcho(data: FloatArray): Int {
            for (i in data.indices) {
                if (kotlin.math.abs(data[i]) > 0.001f) return i
            }
            return -1
        }

        val startNo = firstEcho(noDelay)
        val startWith = firstEcho(withDelay)

        assertThat(startWith).isGreaterThan(startNo + 2000) // 50ms approx
    }

    @Test
    fun audioBuildingReturnsNullWhenNoOriginalAudio() = runTest {
        vm._uiState.update {
            it.copy(originalAudioUri = null)
        }

        val result = vm.audioBuilding()

        assertThat(result).isNull()
    }
}