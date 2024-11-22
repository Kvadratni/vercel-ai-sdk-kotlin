package dev.vercel.ai.providers

import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.options.AnthropicOptions
import io.ktor.client.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.request.*
import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AnthropicProviderTest {
    @Test
    fun `complete should handle rate limit errors`() = runTest {
        val mockClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse>(relaxed = true)
        
        every { mockResponse.status } returns HttpStatusCode.TooManyRequests
        every { mockResponse.headers["Retry-After"] } returns "30"
        coEvery { mockClient.post(any<String>(), any()) } returns mockResponse
        
        val provider = AnthropicProvider(
            apiKey = "test-key",
            httpClient = mockClient
        )
        val options = AnthropicOptions.claude2()
        
        val error = assertThrows<AIError.RateLimitError> {
            provider.complete("test prompt", options).toList()
        }
        
        assertEquals("anthropic", error.provider)
        assertEquals(30000L, error.retryAfter)
    }
    
    @Test
    fun `chat should handle invalid message roles`() = runTest {
        val provider = AnthropicProvider("test-key")
        val options = AnthropicOptions.claude2()
        
        assertThrows<AIError.ConfigurationError> {
            provider.chat(listOf(
                ChatMessage(role = "invalid", content = "test")
            ),
            options = options).toList()
        }
    }
    
    @Test
    fun `chat should properly format system messages`() = runTest {
        val mockClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse>(relaxed = true)
        
        every { mockResponse.status } returns HttpStatusCode.OK
        coEvery { mockClient.post(any<String>(), any()) } returns mockResponse
        
        val provider = AnthropicProvider(
            apiKey = "test-key",
            httpClient = mockClient
        )
        
        val messages = listOf(
            ChatMessage(role = "system", content = "You are a helpful assistant"),
            ChatMessage(role = "user", content = "Hello")
        )
        
        val options = AnthropicOptions.claude2()
        val result = provider.chat(messages, options).toList()
        assertEquals(1, result.size)
    }
}