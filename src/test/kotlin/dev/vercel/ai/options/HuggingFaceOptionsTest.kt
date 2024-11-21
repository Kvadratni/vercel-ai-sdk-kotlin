package dev.vercel.ai.options

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HuggingFaceOptionsTest {
    @Test
    fun `should validate temperature range`() {
        assertThrows<IllegalArgumentException> {
            HuggingFaceOptions(
                model = "gpt2",
                temperature = 1.5
            ).validate()
        }

        assertThrows<IllegalArgumentException> {
            HuggingFaceOptions(
                model = "gpt2",
                temperature = -0.1
            ).validate()
        }

        // Valid temperature should not throw
        HuggingFaceOptions(
            model = "gpt2",
            temperature = 0.7
        ).validate()
    }

    @Test
    fun `should validate maxTokens`() {
        assertThrows<IllegalArgumentException> {
            HuggingFaceOptions(
                model = "gpt2",
                maxTokens = 0
            ).validate()
        }

        assertThrows<IllegalArgumentException> {
            HuggingFaceOptions(
                model = "gpt2",
                maxTokens = -1
            ).validate()
        }

        // Valid maxTokens should not throw
        HuggingFaceOptions(
            model = "gpt2",
            maxTokens = 1000
        ).validate()
    }

    @Test
    fun `should validate generation parameters`() {
        assertThrows<IllegalArgumentException> {
            HuggingFaceOptions(
                model = "gpt2",
                topP = 1.5
            ).validate()
        }

        assertThrows<IllegalArgumentException> {
            HuggingFaceOptions(
                model = "gpt2",
                numBeams = 0
            ).validate()
        }

        assertThrows<IllegalArgumentException> {
            HuggingFaceOptions(
                model = "gpt2",
                noRepeatNgramSize = -1
            ).validate()
        }

        // Valid values should not throw
        HuggingFaceOptions(
            model = "gpt2",
            topP = 0.9,
            numBeams = 4,
            noRepeatNgramSize = 3
        ).validate()
    }

    @Test
    fun `should create valid gpt2 configuration`() {
        val options = HuggingFaceOptions.gpt2()
        assertEquals("gpt2", options.model)
        assertEquals(0.7, options.temperature)
        assertEquals(1000, options.maxTokens)
        assertEquals(true, options.stream)
        assertEquals(true, options.doSample)
    }

    @Test
    fun `should create valid blenderbot configuration`() {
        val options = HuggingFaceOptions.blenderbot()
        assertEquals("facebook/blenderbot-400M-distill", options.model)
        assertEquals(0.7, options.temperature)
        assertEquals(1000, options.maxTokens)
        assertEquals(true, options.stream)
    }

    @Test
    fun `should convert to map with correct keys`() {
        val options = HuggingFaceOptions(
            model = "gpt2",
            temperature = 0.8,
            maxTokens = 500,
            stopSequences = listOf("END"),
            topP = 0.9,
            numBeams = 4,
            noRepeatNgramSize = 3,
            earlyStopping = true
        )

        val map = options.toMap()
        
        assertEquals("gpt2", map["model"])
        assertEquals(0.8, map["temperature"])
        assertEquals(500, map["max_new_tokens"])
        assertEquals(listOf("END"), map["stop_sequences"])
        assertEquals(0.9, map["top_p"])
        assertEquals(4, map["num_beams"])
        assertEquals(3, map["no_repeat_ngram_size"])
        assertEquals(true, map["early_stopping"])
        assertEquals(true, map["stream"])
        assertEquals(true, map["do_sample"])
    }
}