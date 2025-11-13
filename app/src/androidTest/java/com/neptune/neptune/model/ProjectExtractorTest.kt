package com.neptune.neptune.model

import android.content.res.AssetManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neptune.neptune.model.project.ProjectExtractor
import com.neptune.neptune.model.project.SamplerProjectMetadata
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectExtractorTest {

  private val validJson =
      """
        {
          "audioFiles": [
            { "name": "kick.wav", "volume": 0.8, "durationSeconds": 3.5 },
            { "name": "snare.wav", "volume": 1.0, "durationSeconds": 1.2 }
          ],
          "parameters": [
            { "type": "attack", "value": 0.5, "targetAudioFile": "kick.wav" },
            { "type": "compRatio", "value": 4.0, "targetAudioFile": "master" }
          ]
        }
    """
          .trimIndent()

  private val invalidJson =
      """
        {
          "audioFiles": [
            { "name": "kick.wav" }
          ],
          "parameters": [        { "type": "attack", "value": "INVALID_FLOAT", "targetAudioFile": "kick.wav" }
          ]
        }
    """
          .trimIndent()

  @get:Rule val tempFolder = TemporaryFolder()

  private lateinit var extractor: ProjectExtractor
  private lateinit var assetManager: AssetManager
  private lateinit var zipFile: File

  private val ASSET_ZIP_PATH = "fakeProject.zip"

  @Before
  fun setup() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    assetManager = context.assets

    zipFile = File(tempFolder.root, "temp_project.zip")
    copyAssetToTempFile(ASSET_ZIP_PATH, zipFile)

    extractor = ProjectExtractor()
  }

  private fun copyAssetToTempFile(assetPath: String, targetFile: File) {
    assetManager.open(assetPath).use { inputStream ->
      FileOutputStream(targetFile).use { outputStream -> inputStream.copyTo(outputStream) }
    }
  }

  @Test
  fun deserialization_withValidJson_returnsCorrectMetadata() {
    val metadata = extractor.json.decodeFromString<SamplerProjectMetadata>(validJson)
    assertEquals(2, metadata.audioFiles.size)
    assertEquals("kick.wav", metadata.audioFiles.first().name)
    assertEquals(2, metadata.parameters.size)
    assertEquals(0.5f, metadata.parameters.first().value)
  }

  @Test
  fun deserialization_withInvalidJson_throwsException() {
    assertThrows(Exception::class.java) {
      extractor.json.decodeFromString<SamplerProjectMetadata>(invalidJson)
    }
  }

  @Test
  fun extractMetadata_fromRealAssetZip_returnsCorrectMetadata() {
    val metadata: SamplerProjectMetadata = extractor.extractMetadata(zipFile)

    assertNotNull("Metadata should not be null", metadata)
    assertEquals(1, metadata.audioFiles.size)
    assertEquals(15, metadata.parameters.size)
    assertEquals("electric-picked-bass-long-release_C_major.wav", metadata.audioFiles.first().name)

    val parameters = metadata.parameters

    val attackParam = parameters.find { it.type == "attack" }
    assertNotNull("Attack parameter must be present.", attackParam)
    assertEquals(0.35f, attackParam!!.value, 0.001f)

    val sustainParam = parameters.find { it.type == "sustain" }
    assertNotNull("Sustain parameter must be present.", sustainParam)
    assertEquals(0.6f, sustainParam!!.value, 0.001f)

    val ratioParam = parameters.find { it.type == "compRatio" }
    assertNotNull("CompRatio parameter must be present.", ratioParam)
    assertEquals(4.0f, ratioParam!!.value, 0.001f)

    val eqParam = parameters.find { it.type == "eq_band_0" }
    assertNotNull("EQ band 0 parameter must be present.", eqParam)
    assertEquals(6.0f, eqParam!!.value, 0.001f)
  }
}
