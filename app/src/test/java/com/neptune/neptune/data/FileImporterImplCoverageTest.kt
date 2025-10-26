package com.neptune.neptune.data

import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.net.URI
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowContentResolver

/*
   Coverage tests for FileImporterImpl.
   It includes tests for handling file:// and content:// URIs, as well as error
   conditions like unsupported URI schemes. A minimal valid WAV file writer and
   test ContentProviders are implemented to facilitate testing.
   Written with help from ChatGPT.
*/
@RunWith(RobolectricTestRunner::class)
class FileImporterImplCoverageTest {

  private fun importer(ctx: Context) =
      FileImporterImpl(context = ctx, cr = ctx.contentResolver, paths = StoragePaths(ctx))

  // --- Tiny WAV writer (valid header) ---
  private fun writeTinyWav(dst: File, seconds: Double = 0.2, sampleRate: Int = 8000) {
    val numChannels = 1
    val bitsPerSample = 16
    val bytesPerSample = bitsPerSample / 8
    val totalSamples = (seconds * sampleRate).toInt()
    val dataSize = totalSamples * numChannels * bytesPerSample
    val byteRate = sampleRate * numChannels * bytesPerSample
    val blockAlign = numChannels * bytesPerSample
    val chunkSize = 36 + dataSize

    dst.outputStream().use { out ->
      fun w32(v: Int) =
          out.write(
              byteArrayOf(
                  (v and 0xFF).toByte(),
                  ((v shr 8) and 0xFF).toByte(),
                  ((v shr 16) and 0xFF).toByte(),
                  ((v shr 24) and 0xFF).toByte()))
      fun w16(v: Int) = out.write(byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte()))

      out.write("RIFF".toByteArray())
      w32(chunkSize)
      out.write("WAVE".toByteArray())
      out.write("fmt ".toByteArray())
      w32(16)
      w16(1) // PCM
      w16(numChannels)
      w32(sampleRate)
      w32(byteRate)
      w16(blockAlign)
      w16(bitsPerSample)
      out.write("data".toByteArray())
      w32(dataSize)
      val frame = ByteArray(blockAlign)
      repeat(totalSamples) { out.write(frame) }
    }
  }

  // --- Test ContentProvider serving audio for content:// URIs ---
  class TestAudioProvider : android.content.ContentProvider() {
    companion object {
      const val AUTH = "com.neptune.test.audio"
      lateinit var backingFile: File
    }

    override fun onCreate() = true

    override fun getType(uri: Uri) = "audio/wav"

    override fun openFile(uri: Uri, mode: String) =
        android.os.ParcelFileDescriptor.open(
            backingFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)

    override fun query(
        u: Uri,
        p: Array<out String>?,
        s: String?,
        a: Array<out String>?,
        o: String?
    ) = null

    override fun insert(u: Uri, v: android.content.ContentValues?) = null

    override fun delete(u: Uri, s: String?, a: Array<out String>?) = 0

    override fun update(
        u: Uri,
        v: android.content.ContentValues?,
        s: String?,
        a: Array<out String>?
    ) = 0
  }

  private fun registerTestProvider(ctx: Context, file: File) {
    TestAudioProvider.backingFile = file
    val provider = TestAudioProvider()
    val info = ProviderInfo().apply { authority = TestAudioProvider.AUTH }
    provider.attachInfo(ctx, info)
    ShadowContentResolver.registerProviderInternal(TestAudioProvider.AUTH, provider)
  }

  // ---------- A configurable provider for extra branches ----------
  class ConfigAudioProvider : android.content.ContentProvider() {
    companion object {
      const val AUTH = "com.neptune.test.configprovider"
      lateinit var backingFile: File
      var reportedMime: String? = "audio/wav"
      var displayNameForQuery: String? = null
    }

    override fun onCreate() = true

    override fun getType(uri: Uri) = reportedMime

    override fun openFile(uri: Uri, mode: String) =
        android.os.ParcelFileDescriptor.open(
            backingFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ) =
        displayNameForQuery?.let { name ->
          MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf(name)) }
        }

    override fun insert(uri: Uri, values: ContentValues?) = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ) = 0
  }

  private fun registerConfigProvider(ctx: Context, file: File, mime: String?, queryName: String?) {
    ConfigAudioProvider.backingFile = file
    ConfigAudioProvider.reportedMime = mime
    ConfigAudioProvider.displayNameForQuery = queryName
    val provider = ConfigAudioProvider()
    val info = ProviderInfo().apply { authority = ConfigAudioProvider.AUTH }
    provider.attachInfo(ctx, info)
    ShadowContentResolver.registerProviderInternal(ConfigAudioProvider.AUTH, provider)
  }

  // --- importFile(): file:// success + uniqueFile() collision branch ---
  @Test
  fun importFile_withFileUri_copies_and_handles_name_collision() = runBlocking {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val imp = importer(ctx)

    val src = File(ctx.cacheDir, "sample.wav").apply { writeTinyWav(this, seconds = 0.25) }

    // Pre-create destination 'sample.wav' so importer must generate a unique name
    val audioWs = StoragePaths(ctx).audioWorkspace().apply { mkdirs() }
    File(audioWs, "sample.wav").writeBytes(byteArrayOf(1, 2, 3)) // collision trigger

    val result = imp.importFile(src.toURI())

    // Because of collision, displayName should not be exactly "sample.wav"
    assertThat(result.displayName).isNotEqualTo("sample.wav")
    assertThat(result.sizeBytes).isGreaterThan(0)
    assertThat(result.durationMs).isNotNull()
    assertThat(result.durationMs!!).isAtLeast(0L)

    val copied = File(result.localUri)
    assertThat(copied.exists()).isTrue()
  }

  // --- importFile(): content:// happy path (exercises resolveAndValidateAudio for content scheme)
  @Test
  fun importFile_withContentUri_reads_via_provider() = runBlocking {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val imp = importer(ctx)

    val src = File(ctx.cacheDir, "voice.wav").apply { writeTinyWav(this, seconds = 0.15) }
    registerTestProvider(ctx, src)

    val contentUri = Uri.parse("content://${TestAudioProvider.AUTH}/voice.wav")
    val result = imp.importFile(URI(contentUri.toString()))

    assertThat(result.mimeType).contains("audio")
    assertThat(result.sizeBytes).isEqualTo(src.length())
    assertThat(File(result.localUri).exists()).isTrue()
    assertThat(result.durationMs).isNotNull()
    assertThat(result.durationMs!!).isAtLeast(0L)
  }

  // --- importFile(): unsupported scheme failure path ---
  @Test
  fun importFile_withHttpUri_fails_fast() = runBlocking {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val imp = importer(ctx)

    var threw = false
    try {
      imp.importFile(URI("http://example.com/foo.wav"))
    } catch (_: Throwable) {
      threw = true
    }

    assertThat(threw).isTrue()
  }

  @Test
  fun fileUri_mp3_infers_audioMpeg_from_extension() = runBlocking {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val imp = importer(ctx)

    val src = File(ctx.cacheDir, "clip.mp3").apply { writeBytes(ByteArray(32)) }
    val out = imp.importFile(src.toURI())

    assertThat(out.mimeType).isEqualTo("audio/mpeg")
    assertThat(out.displayName.lowercase()).endsWith(".mp3")
  }

  @Test
  fun contentUri_mime_wins_over_query_extension_and_ext_is_remapped() = runBlocking {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val imp = importer(ctx)

    // Provider claims audio/mpeg but query name has .wav → must normalize to mp3
    val backing = File(ctx.cacheDir, "blob").apply { writeBytes(ByteArray(64)) }
    registerConfigProvider(ctx, backing, mime = "audio/mpeg", queryName = "something.wav")

    val u = Uri.parse("content://${ConfigAudioProvider.AUTH}/ignored")
    val out = imp.importFile(URI(u.toString()))

    assertThat(out.mimeType).isEqualTo("audio/mpeg")
    assertThat(out.displayName.lowercase()).endsWith(".mp3")
  }

  @Test
  fun contentUri_uses_query_name_and_sanitizes_base() = runBlocking {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val imp = importer(ctx)

    val src = File(ctx.cacheDir, "voice.wav").apply { writeTinyWav(this) }
    registerConfigProvider(
        ctx,
        file = src,
        mime = "audio/wav",
        queryName = "  My Voice (Demo)#1 .wav " // spaces + special chars
        )

    val u = Uri.parse("content://${ConfigAudioProvider.AUTH}/noUsefulPath")
    val out = imp.importFile(URI(u.toString()))

    // Base should be sanitized like "My_Voice_Demo_1"
    assertThat(out.displayName).contains("My_Voice_Demo_1")
    assertThat(out.displayName.lowercase()).endsWith(".wav")
  }

  @Test
  fun contentUri_unknown_mime_throws() = runBlocking {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val imp = importer(ctx)

    val backing = File(ctx.cacheDir, "x.bin").apply { writeBytes(ByteArray(8)) }
    registerConfigProvider(ctx, backing, mime = "application/octet-stream", queryName = "x.bin")

    val u = Uri.parse("content://${ConfigAudioProvider.AUTH}/x.bin")
    var threw = false
    try {
      imp.importFile(URI(u.toString()))
    } catch (_: UnsupportedAudioFormat) {
      threw = true
    }
    assertThat(threw).isTrue()
  }

  @Test
  fun contentUri_no_query_no_path_name_defaults_to_audio_base_and_duration_may_be_null() =
      runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val imp = importer(ctx)

        // Not a valid WAV payload; reported as audio/wav; duration extraction may fail → null
        val backing = File(ctx.cacheDir, "backing.dat").apply { writeBytes(ByteArray(10)) }
        registerConfigProvider(ctx, backing, mime = "audio/wav", queryName = null)

        // trailing slash → lastPathSegment = null → base defaults to "audio"
        val u = Uri.parse("content://${ConfigAudioProvider.AUTH}/")
        val out = imp.importFile(URI(u.toString()))

        assertThat(out.displayName.lowercase()).startsWith("audio")
        assertThat(out.displayName.lowercase()).endsWith(".wav")
        assertThat(out.durationMs == null || out.durationMs!! >= 0).isTrue()
      }
}
