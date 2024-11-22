package dev.vercel.ai.providers

import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.common.AbortController
import dev.vercel.ai.common.AbortError
import dev.vercel.ai.errors.AIError
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import dev.vercel.ai.options.AzureOpenAIOptions
import dev.vercel.ai.options.ModelParameters

class AzureOpenAIProviderTest {
    @Test
    fun `chat should handle successful streaming response`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    data: {"id":"1","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}
                    
                    data: {"id":"1","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4","choices":[{"index":0,"delta":{"content":" world"},"finish_reason":null}]}
                    
                    data: {"id":"1","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":"stop"}]}
                    
                    data: [DONE]
                """.trimIndent(),
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Text.EventStream.toString())
                ),
                status = HttpStatusCode.OK
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }

        val provider = AzureOpenAIProvider(
            apiKey = "test-key",
            httpClient = client
        )

        val options = AzureOpenAIOptions.gpt4(
            deploymentId = "gpt4",
            endpoint = "https://example.azure.openai.com",
            parameters = ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 100
            )
        )

        val messages = listOf(
            ChatMessage(role = "user", content = "Say hello!")
        )

        val response = provider.chat(
            messages = messages,
            options = options
        ).toList()

        assertEquals(listOf("Hello", " world", "!"), response)
    }

    @Test
    fun `chat should handle error response`() = runTest {
        val errorEngine = MockEngine { _ ->
            respond(
                content = """{"error":{"message":"Rate limit exceeded","type":"rate_limit_error","code":"429"}}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                    "Retry-After" to listOf("30")
                )
            )
        }

        val errorClient = HttpClient(errorEngine) {
            install(ContentNegotiation) {
                json()
            }
        }

        val provider = AzureOpenAIProvider(
            apiKey = "test-key",
            httpClient = errorClient
        )

        val options = AzureOpenAIOptions.gpt4(
            deploymentId = "gpt4",
            endpoint = "https://example.azure.openai.com"
        )

        val messages = listOf(
            ChatMessage(role = "user", content = "Say hello!")
        )

        val error = assertThrows<AIError.RateLimitError> {
            provider.chat(
                messages = messages,
                options = options
            ).toList()
        }

        assertEquals("azure_openai", error.provider)
        assertEquals(30000L, error.retryAfter)
    }

    @Test
    fun `chat should handle abort signal`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    data: {"id":"1","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}
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
        
        val provider = AzureOpenAIProvider(
            apiKey = "test-key",
            httpClient = client
        )

        val options = AzureOpenAIOptions.gpt4(
            deploymentId = "gpt4",
            endpoint = "https://example.azure.openai.com"
        )

        val messages = listOf(
            ChatMessage(role = "user", content = "Hello!")
        )

        val controller = AbortController()
        controller.abort()

        assertThrows<CancellationException> {
            provider.chat(
                messages = messages,
                options = options,
                signal = controller.signal
            ).toList()
        }
    }
}