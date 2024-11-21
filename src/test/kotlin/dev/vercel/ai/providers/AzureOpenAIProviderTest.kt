package dev.vercel.ai.providers

import dev.vercel.ai.ChatMessage
import dev.vercel.ai.common.AbortController
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.options.AzureOpenAIOptions
import dev.vercel.ai.options.ModelParameters
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AzureOpenAIProviderTest {

    private val mockEngine = MockEngine { request ->
        when (request.url.toString()) {
            "https://example.azure.openai.com/openai/deployments/gpt4/chat/completions" -> {
                respond(
                    content = """
                        data: {"id":"1","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}
                        
                        data: {"id":"1","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4","choices":[{"index":0,"delta":{"content":" world"},"finish_reason":null}]}
                        
                        data: {"id":"1","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":"stop"}]}
                        
                        data: [DONE]
                        
                    """.trimIndent(),
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf(ContentType.Text.EventStream.toString())
                    )
                )
            }
            else -> error("Unhandled ${request.url}")
        }
    }

    private val httpClient = HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json()
        }
    }

    @Test
    fun `chat should handle successful streaming response`() = runTest {
        val provider = AzureOpenAIProvider(
            apiKey = "test-key",
            httpClient = httpClient
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
        val errorEngine = MockEngine {
            respond(
                content = """{"error":{"message":"Rate limit exceeded","type":"rate_limit_error"}}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                    "Retry-After" to listOf("30")
                )
            )
        }

        val provider = AzureOpenAIProvider(
            apiKey = "test-key",
            httpClient = HttpClient(errorEngine) {
                install(ContentNegotiation) {
                    json()
                }
            }
        )

        val options = AzureOpenAIOptions.gpt4(
            deploymentId = "gpt4",
            endpoint = "https://example.azure.openai.com"
        )

        val messages = listOf(
            ChatMessage(role = "user", content = "Say hello!")
        )

        assertFailsWith<AIError.ProviderError> {
            provider.chat(
                messages = messages,
                options = options
            ).toList()
        }
    }

    @Test
    fun `chat should handle abort signal`() = runTest {
        val provider = AzureOpenAIProvider(
            apiKey = "test-key",
            httpClient = httpClient
        )

        val options = AzureOpenAIOptions.gpt4(
            deploymentId = "gpt4",
            endpoint = "https://example.azure.openai.com"
        )

        val messages = listOf(
            ChatMessage(role = "user", content = "Say hello!")
        )

        val controller = AbortController()
        val signal = controller.signal

        controller.abort()

        assertFailsWith<AIError.StreamError> {
            provider.chat(
                messages = messages,
                options = options,
                signal = signal
            ).toList()
        }
    }
}