package dev.vercel.ai.providers

import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.options.AnthropicOptions
import io.ktor.client.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AnthropicProviderTest {
    @Test
    fun `complete should handle rate limit errors`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"error":{"type":"rate_limit_error","message":"Rate limit exceeded"}}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                    "Retry-After" to listOf("30")
                )
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        
        val provider = AnthropicProvider(
            apiKey = "test-key",
            httpClient = client
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
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"error": {"type": "invalid_request_error", "message": "Invalid role"}}""",
                status = HttpStatusCode.BadRequest
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        
        val provider = AnthropicProvider(
            apiKey = "test-key",
            httpClient = client
        )
        val options = AnthropicOptions.claude2()
        
        val error = assertThrows<AIError.ConfigurationError> {
            provider.chat(listOf(
                ChatMessage(role = "invalid", content = "test")
            ), options).toList()
        }

        assertEquals("Unsupported message role: invalid", error.message)
    }
    
    @Test
    fun `chat should properly format system messages`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    data: {"completion":"Hello! How can I help you today?","stop_reason":null}
                    
                    data: {"completion":" I'll do my best to assist.","stop_reason":"stop"}
                    
                    data: [DONE]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString())
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        
        val provider = AnthropicProvider(
            apiKey = "test-key",
            httpClient = client
        )
        
        val messages = listOf(
            ChatMessage(role = "system", content = "You are a helpful assistant"),
            ChatMessage(role = "user", content = "Hello")
        )
        
        val options = AnthropicOptions.claude2()
        val response = provider.chat(messages, options).toList()
        assertEquals(listOf(
            "Hello! How can I help you today?",
            " I'll do my best to assist."
        ), response)
    }
}