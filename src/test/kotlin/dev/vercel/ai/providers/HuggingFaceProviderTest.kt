package dev.vercel.ai.providers

import dev.vercel.ai.ChatMessage
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.options.ProviderOptions
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

private class TestProviderOptions(
    override val model: String = "test-model",
    override val temperature: Double? = 0.7,
    override val maxTokens: Int? = 100,
    override val stopSequences: List<String>? = null,
    override val stream: Boolean = true
) : ProviderOptions {
    override fun toMap(): Map<String, Any> = buildMap {
        put("model", model)
        temperature?.let { put("temperature", it) }
        maxTokens?.let { put("max_tokens", it) }
        stopSequences?.let { put("stop", it) }
        put("stream", stream)
    }
}

class HuggingFaceProviderTest {
    @Test
    fun `complete should handle rate limit errors`() = runBlocking {
        val mockClient = mockk<OkHttpClient>()
        val mockResponse = mockk<Response>()
        
        every { mockResponse.code } returns 429
        every { mockResponse.header("Retry-After") } returns "30"
        every { mockClient.newCall(any()).execute() } returns mockResponse
        
        val provider = HuggingFaceProvider(
            apiKey = "test-key",
            client = mockClient
        )
        
        val error = assertThrows<AIError.RateLimitError> {
            provider.complete("test prompt", TestProviderOptions()).toList()
        }
        
        assertEquals("huggingface", error.provider)
        assertEquals(30000L, error.retryAfter)
    }
    
    @Test
    fun `chat should handle invalid message roles`() = runBlocking {
        val provider = HuggingFaceProvider("test-key")
        
        val error = assertThrows<AIError.ConfigurationError> {
            provider.chat(listOf(
                ChatMessage(role = "invalid", content = "test")
            ), TestProviderOptions()).toList()
        }
        
        assertEquals("Unsupported message role: invalid", error.message)
    }
    
    @Test
    fun `chat should properly format conversation`() = runBlocking {
        val mockClient = mockk<OkHttpClient>()
        val mockResponse = mockk<Response>()
        
        every { mockResponse.code } returns 200
        every { mockResponse.body } returns "data: {\"text\": \"test\"}\n\n".toResponseBody()
        every { mockClient.newCall(any()).execute() } returns mockResponse
        
        val provider = HuggingFaceProvider(
            apiKey = "test-key",
            client = mockClient
        )
        
        val messages = listOf(
            ChatMessage(role = "system", content = "You are a helpful assistant"),
            ChatMessage(role = "user", content = "Hello"),
            ChatMessage(role = "assistant", content = "Hi there!")
        )
        
        val result = provider.chat(messages, TestProviderOptions()).toList()
        assertEquals(1, result.size)
    }
}