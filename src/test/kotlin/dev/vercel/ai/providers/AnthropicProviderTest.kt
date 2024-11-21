package dev.vercel.ai.providers

import dev.vercel.ai.ChatMessage
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.options.AnthropicOptions
import dev.vercel.ai.options.ModelParameters
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

class AnthropicProviderTest {
    @Test
    fun `complete should handle rate limit errors`() = runBlocking {
        val mockClient = mockk<OkHttpClient>()
        val mockResponse = mockk<Response>()
        
        every { mockResponse.code } returns 429
        every { mockResponse.header("Retry-After") } returns "30"
        every { mockClient.newCall(any()).execute() } returns mockResponse
        
        val provider = AnthropicProvider(
            apiKey = "test-key",
            client = mockClient
        )
        val options = AnthropicOptions.claude3Opus(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 100
            )
        )
        
        val error = assertThrows<AIError.RateLimitError> {
            provider.complete("test prompt", options).toList()
        }
        
        assertEquals("anthropic", error.provider)
        assertEquals(30000L, error.retryAfter)
    }
    
    @Test
    fun `chat should handle invalid message roles`() = runBlocking {
        val provider = AnthropicProvider("test-key")
        val options = AnthropicOptions.claude3Opus(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 100
            )
        )
        val error = assertThrows<AIError.ConfigurationError> {
            provider.chat(listOf(
                ChatMessage(role = "invalid", content = "test")
            ),
                options = options).toList()
        }
        
        assertEquals("Unsupported message role: invalid", error.message)
    }
    
    @Test
    fun `chat should properly format system messages`() = runBlocking {
        val mockClient = mockk<OkHttpClient>()
        val mockResponse = mockk<Response>()
        val options = AnthropicOptions.claude3Opus(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 100
            )
        )
        every { mockResponse.code } returns 200
        every { mockResponse.body } returns "data: {\"text\": \"test\"}\n\n".toResponseBody()
        every { mockClient.newCall(any()).execute() } returns mockResponse
        
        val provider = AnthropicProvider(
            apiKey = "test-key",
            client = mockClient
        )
        
        val messages = listOf(
            ChatMessage(role = "system", content = "You are a helpful assistant"),
            ChatMessage(role = "user", content = "Hello")
        )
        
        val result = provider.chat(messages, options).toList()
        assertEquals(1, result.size)
    }
}
