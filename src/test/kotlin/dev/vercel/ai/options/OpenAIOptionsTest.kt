package dev.vercel.ai.options

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OpenAIOptionsTest {
    @Test
    fun `should validate temperature range`() {
        assertThrows<IllegalArgumentException> {
            OpenAIOptions(
                model = "gpt-4",
                temperature = 1.5
            ).validate()
        }

        assertThrows<IllegalArgumentException> {
            OpenAIOptions(
                model = "gpt-4",
                temperature = -0.1
            ).validate()
        }

        // Valid temperature should not throw
        OpenAIOptions(
            model = "gpt-4",
            temperature = 0.7
        ).validate()
    }

    @Test
    fun `should validate maxTokens`() {
        assertThrows<IllegalArgumentException> {
            OpenAIOptions(
                model = "gpt-4",
                maxTokens = 0
            ).validate()
        }

        assertThrows<IllegalArgumentException> {
            OpenAIOptions(
                model = "gpt-4",
                maxTokens = -1
            ).validate()
        }

        // Valid maxTokens should not throw
        OpenAIOptions(
            model = "gpt-4",
            maxTokens = 1000
        ).validate()
    }

    @Test
    fun `should validate penalty ranges`() {
        assertThrows<IllegalArgumentException> {
            OpenAIOptions(
                model = "gpt-4",
                frequencyPenalty = 2.5
            ).validate()
        }

        assertThrows<IllegalArgumentException> {
            OpenAIOptions(
                model = "gpt-4",
                presencePenalty = -2.5
            ).validate()
        }

        // Valid penalties should not throw
        OpenAIOptions(
            model = "gpt-4",
            frequencyPenalty = 1.0,
            presencePenalty = 1.0
        ).validate()
    }

    @Test
    fun `should create valid gpt4 configuration`() {
        val options = OpenAIOptions.gpt4()
        assertEquals("gpt-4", options.model)
        assertEquals(0.7, options.temperature)
        assertEquals(1000, options.maxTokens)
        assertEquals(true, options.stream)
    }

    @Test
    fun `should create valid gpt35Turbo configuration`() {
        val options = OpenAIOptions.gpt35Turbo()
        assertEquals("gpt-3.5-turbo", options.model)
        assertEquals(0.7, options.temperature)
        assertEquals(1000, options.maxTokens)
        assertEquals(true, options.stream)
    }

    @Test
    fun `should convert to map with correct keys`() {
        val options = OpenAIOptions(
            model = "gpt-4",
            temperature = 0.8,
            maxTokens = 500,
            stopSequences = listOf("END"),
            topP = 0.9,
            frequencyPenalty = 1.0,
            presencePenalty = 0.5,
            user = "test-user"
        )

        val map = options.toMap()
        
        assertEquals("gpt-4", map["model"])
        assertEquals(0.8, map["temperature"])
        assertEquals(500, map["max_tokens"])
        assertEquals(listOf("END"), map["stop"])
        assertEquals(0.9, map["top_p"])
        assertEquals(1.0, map["frequency_penalty"])
        assertEquals(0.5, map["presence_penalty"])
        assertEquals("test-user", map["user"])
        assertEquals(true, map["stream"])
    }
}