package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
class NeptunePackagerTinyDurationTest {

    @Test
    fun tiny_duration_is_not_negative_and_rounds_to_zero_or_min_step() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val packager = NeptunePackager(StoragePaths(ctx))

        val audio = File(ctx.cacheDir, "blip.wav").apply { writeBytes(ByteArray(8) { 0x01 }) }

        val zip = packager.createProjectZip(audioFile = audio, durationMs = 1L)
        ZipFile(zip).use { z ->
            val cfg = z.getInputStream(z.getEntry("config.json")).bufferedReader().readText()
            val f0 = JSONObject(cfg).getJSONArray("files").getJSONObject(0)
            val dur = f0.getDouble("duration")

            // We don't assert an exact value (impl-specific), but ensure it's valid and not negative.
            assertThat(dur).isAtLeast(0.0)
            // and "reasonable" (if you round to 0.0 or 0.1); this line makes the branch observable.
            assertThat(dur).isAtMost(0.1)
        }
    }
}
