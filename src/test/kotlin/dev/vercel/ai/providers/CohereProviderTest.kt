package dev.vercel.ai.providers

import dev.vercel.ai.options.CohereOptions
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CohereProviderTest {
    private val mockEngine = MockEngine { request ->
        // Print request details for debugging
        val requestBody = request.body.toByteArray().decodeToString()
        println("Request body: $requestBody")
        println("Request headers: ${request.headers.entries()}")

        if (requestBody.contains("\"message\":") && requestBody.contains("\"model\":")) {
            respond(
                content = """data: {"text": "Hello, world!"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream")
            )
        } else {
            respond(
                content = """{"error": "Invalid request body"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
    }

    private val mockClient = HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json()
        }
    }

    private val provider = CohereProvider(
        options = CohereOptions(
            apiKey = "test-key",
            model = "command",
            temperature = 0.7,
            maxTokens = 256,
            stream = true
        ),
        httpClient = mockClient
    )

    @Test
    fun `chat should throw exception when no messages provided`() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                provider.chat(emptyList())
            }
        }
    }

    @Test
    fun `chat should make correct API request`() = runBlocking {
        val messages = listOf(
            CohereProvider.Message(role = "user", content = "Hello")
        )

        val response = provider.chat(messages)
        assertEquals(1, mockEngine.requestHistory.size)
        
        val request = mockEngine.requestHistory.first()
        assertEquals("Bearer test-key", request.headers["Authorization"])
        assertTrue(request.headers.entries().any { (key, value) ->
            key.equals("Content-Type", ignoreCase = true) && value.contains("application/json")
        })
        assertEquals("https://api.cohere.ai/v1/chat", request.url.toString())
    }
}