package dev.vercel.ai.providers

import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.options.HuggingFaceOptions
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class HuggingFaceProviderTest {
    @Test
    fun `chat should properly format conversation`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    data: {"token": {"text": "Hello"}}
                    
                    data: {"token": {"text": " world"}}
                    
                    data: {"token": {"text": "!"}}
                    
                    data: [DONE]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream")
            )
        }
        
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        
        val provider = HuggingFaceProvider(
            apiKey = "test-key",
            baseUrl = "https://api-inference.huggingface.co/models",
            httpClient = client
        )

        val messages = listOf(
            ChatMessage(role = "system", content = "You are a helpful assistant"),
            ChatMessage(role = "user", content = "Hello")
        )

        val options = HuggingFaceOptions.gpt2()
        val response = provider.chat(messages, options).toList()

        assertEquals(listOf("Hello", " world", "!"), response)
    }

    @Test
    fun `complete should handle rate limit errors`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": "Rate limit exceeded"}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf("Retry-After", "30")
            )
        }
        
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        
        val provider = HuggingFaceProvider(
            apiKey = "test-key",
            baseUrl = "https://api-inference.huggingface.co/models",
            httpClient = client
        )

        val options = HuggingFaceOptions.gpt2()
        val error = assertThrows<AIError.RateLimitError> {
            provider.complete("test prompt", options).toList()
        }

        assertEquals("huggingface", error.provider)
        assertEquals(30000L, error.retryAfter)
    }

    @Test
    fun `chat should handle invalid message roles`() = runTest {
        val provider = HuggingFaceProvider(
            apiKey = "test-key",
            baseUrl = "https://api-inference.huggingface.co/models",
            httpClient = HttpClient(MockEngine) {
                install(ContentNegotiation) {
                    json()
                }
            }
        )

        val options = HuggingFaceOptions.gpt2()

        val error = assertThrows<AIError.ConfigurationError> {
            provider.chat(listOf(
                ChatMessage(role = "invalid", content = "test")
            ), options).toList()
        }

        assertEquals("Unsupported message role: invalid", error.message)
    }
}