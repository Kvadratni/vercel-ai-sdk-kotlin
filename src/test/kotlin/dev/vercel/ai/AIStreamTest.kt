package dev.vercel.ai

import dev.vercel.ai.errors.AIError
import dev.vercel.ai.stream.AIStream
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.InternalAPI
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AIStreamTest {
    @OptIn(InternalAPI::class)
    @Test
    fun `should handle successful streaming response`() = runBlocking {
        val mockResponse = mockk<HttpResponse>()
        
        // Mock successful response with streaming data
        every { mockResponse.status } returns HttpStatusCode.OK
        every { mockResponse.content } returns ByteReadChannel("""
            data: {"text": "Hello"}

            data: {"text": " World"}

            data: [DONE]
        """.trimIndent())

        val flow = AIStream.fromResponse(mockResponse)
        val results = flow.toList()

        assertEquals(2, results.size)
        assertTrue(results[0].contains("Hello"))
        assertTrue(results[1].contains("World"))
    }

    @OptIn(InternalAPI::class)
    @Test
    fun `should handle error response`() = runBlocking {
        val mockResponse = mockk<HttpResponse>()
        
        // Mock error response
        every { mockResponse.status } returns HttpStatusCode.InternalServerError
        every { mockResponse.content } returns ByteReadChannel("")

        val error = assertThrows<AIError.ProviderError> {
            AIStream.fromResponse(mockResponse).toList()
        }

        assertEquals(500, error.statusCode)
        assertEquals("Request failed with status code 500", error.message)
    }

    @OptIn(InternalAPI::class)
    @Test
    fun `should handle empty response body`() = runBlocking {
        val mockResponse = mockk<HttpResponse>()
        
        // Mock empty response
        every { mockResponse.status } returns HttpStatusCode.OK
        every { mockResponse.content } returns ByteReadChannel("")

        // Empty response should result in empty list of results
        val results = AIStream.fromResponse(mockResponse).toList()
        assertTrue(results.isEmpty())
    }

    @OptIn(InternalAPI::class)
    @Test
    fun `should handle malformed stream data`() = runBlocking {
        val mockResponse = mockk<HttpResponse>()
        
        // Mock malformed response
        every { mockResponse.status } returns HttpStatusCode.OK
        every { mockResponse.content } returns ByteReadChannel("invalid data format")

        // Malformed data should result in empty list of results
        val results = AIStream.fromResponse(mockResponse).toList()
        assertTrue(results.isEmpty())
    }
}
