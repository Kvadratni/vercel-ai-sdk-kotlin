package dev.vercel.ai.options

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AnthropicOptionsTest {
    @Test
    fun `should validate temperature range`() {
        assertThrows<IllegalArgumentException> {
            AnthropicOptions(
                model = "claude-2",
                temperature = 1.5
            ).validate()
        }

        assertThrows<IllegalArgumentException> {
            AnthropicOptions(
                model = "claude-2",
                temperature = -0.1
            ).validate()
        }

        // Valid temperature should not throw
        AnthropicOptions(
            model = "claude-2",
            temperature = 0.7
        ).validate()
    }

    @Test
    fun `should validate maxTokens`() {
        assertThrows<IllegalArgumentException> {
            AnthropicOptions(
                model = "claude-2",
                maxTokens = 0
            ).validate()
        }

        assertThrows<IllegalArgumentException> {
            AnthropicOptions(
                model = "claude-2",
                maxTokens = -1
            ).validate()
        }

        // Valid maxTokens should not throw
        AnthropicOptions(
            model = "claude-2",
            maxTokens = 1000
        ).validate()
    }

    @Test
    fun `should validate topP and topK`() {
        assertThrows<IllegalArgumentException> {
            AnthropicOptions(
                model = "claude-2",
                topP = 1.5
            ).validate()
        }

        assertThrows<IllegalArgumentException> {
            AnthropicOptions(
                model = "claude-2",
                topK = 0
            ).validate()
        }

        // Valid values should not throw
        AnthropicOptions(
            model = "claude-2",
            topP = 0.9,
            topK = 10
        ).validate()
    }

    @Test
    fun `should create valid claude2 configuration`() {
        val options = AnthropicOptions.claude2()
        assertEquals("claude-2", options.model)
        assertEquals(0.7, options.temperature)
        assertEquals(1000, options.maxTokens)
        assertEquals(true, options.stream)
    }

    @Test
    fun `should create valid claude1 configuration`() {
        val options = AnthropicOptions.claude1()
        assertEquals("claude-1", options.model)
        assertEquals(0.7, options.temperature)
        assertEquals(1000, options.maxTokens)
        assertEquals(true, options.stream)
    }

    @Test
    fun `should convert to map with correct keys`() {
        val metadata = mapOf("source" to "test")
        val options = AnthropicOptions(
            model = "claude-2",
            temperature = 0.8,
            maxTokens = 500,
            stopSequences = listOf("END"),
            topP = 0.9,
            topK = 10,
            metadata = metadata
        )

        val map = options.toMap()
        
        assertEquals("claude-2", map["model"])
        assertEquals(0.8, map["temperature"])
        assertEquals(500, map["max_tokens_to_sample"])
        assertEquals(listOf("END"), map["stop_sequences"])
        assertEquals(0.9, map["top_p"])
        assertEquals(10, map["top_k"])
        assertEquals(metadata, map["metadata"])
        assertEquals(true, map["stream"])
    }
}