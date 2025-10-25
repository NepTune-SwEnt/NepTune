package com.neptune.neptune.data.project


import com.neptune.neptune.model.project.SamplerProjectMetadata
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ProjectExtractorTest {

    private lateinit var extractor: ProjectExtractor

    // JSON de test simulant la structure de votre projet
    private val validJson = """
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
    """.trimIndent()

    private val invalidJson = """
        {
          "audioFiles": [
            { "name": "kick.wav" }
          ],
          "parameters": [
            { "type": "attack", "value": "INVALID_FLOAT", "targetAudioFile": "kick.wav" }
          ]
        }
    """.trimIndent()

    private val missingFieldJson = """
        {
          "audioFiles": [],
          "unknown_field": "data" 
        }
    """.trimIndent()

    @Before
    fun setup() {
        extractor = ProjectExtractor()
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
    fun getAudioFileUri_returnsCorrectPlaceholderUri() {
        val metadata = extractor.json.decodeFromString<SamplerProjectMetadata>(validJson)
        val expectedFileName = "kick.wav"

        // Teste que la fonction retourne bien l'URI simulée pour le fichier
        val resultUri = extractor.getAudioFileUri(metadata, expectedFileName)

        assertTrue("URI must contain the expected filename.", resultUri.contains(expectedFileName))
        assertTrue("URI should start with the expected temporary path.", resultUri.startsWith("file:///tmp/neptune/extracted/"))
    }

    @Test
    fun getAudioFileUri_throwsException_whenFileNotFound() {
        val metadata = extractor.json.decodeFromString<SamplerProjectMetadata>(validJson)

        // Tente de récupérer un fichier qui n'existe pas dans la metadata
        assertThrows(IllegalArgumentException::class.java) {
            extractor.getAudioFileUri(metadata, "nonexistent.wav")
        }
    }

}