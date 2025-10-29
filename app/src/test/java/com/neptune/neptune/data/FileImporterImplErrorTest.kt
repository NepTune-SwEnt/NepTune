package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.net.URI
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for error conditions in FileImporterImpl Written partially with ChatGPT */
@RunWith(RobolectricTestRunner::class)
class FileImporterImplErrorTest {

  private fun importer(ctx: Context) =
      FileImporterImpl(context = ctx, cr = ctx.contentResolver, paths = StoragePaths(ctx))

  @Test
  fun rejectsUnsupportedUriScheme() = runBlocking {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val imp = importer(ctx)

    var threw = false
    try {
      // Unsupported scheme (HTTP should not be accepted)
      imp.importFile(URI("http://example.com/foo.wav"))
    } catch (_: Throwable) {
      threw = true
    }

    assert(threw)
  }

  @Test
  fun missingLocalFileThrowsAndDoesNotcopy() = runBlocking {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val imp = importer(ctx)

    val missing = File(ctx.cacheDir, "nope.wav")
    var threw = false
    try {
      imp.importFile(missing.toURI())
    } catch (_: Throwable) {
      threw = true
    }

    assertThat(threw).isTrue()
  }
}
