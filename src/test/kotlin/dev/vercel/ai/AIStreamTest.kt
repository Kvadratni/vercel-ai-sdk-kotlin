package dev.vercel.ai

import dev.vercel.ai.common.AbortController
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.stream.AIStream
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AIStreamTest {
    @Test
    fun `should handle successful streaming response`() = runTest {
        val mockResponse = mockk<HttpResponse>()
        val mockChannel = ByteReadChannel("""
            data: {"text": "Hello"}

            data: {"text": " World"}

            data: [DONE]
        """.trimIndent())
        
        every { mockResponse.status } returns HttpStatusCode.OK
        coEvery { mockResponse.bodyAsChannel() } returns mockChannel

        val flow = AIStream.fromResponse(mockResponse)
        val results = flow.toList()

        assertEquals(2, results.size)
        assertTrue(results[0].contains("Hello"))
        assertTrue(results[1].contains("World"))
    }

    @Test
    fun `should handle error response`() = runTest {
        val mockResponse = mockk<HttpResponse>()
        val mockChannel = ByteReadChannel("")
        
        every { mockResponse.status } returns HttpStatusCode.InternalServerError
        coEvery { mockResponse.bodyAsChannel() } returns mockChannel

        val error = assertThrows<AIError.ProviderError> {
            AIStream.fromResponse(mockResponse).toList()
        }

        assertEquals(500, error.statusCode)
        assertEquals("Request failed with status code 500", error.message)
    }

    @Test
    fun `should handle empty response body`() = runTest {
        val mockResponse = mockk<HttpResponse>()
        val mockChannel = ByteReadChannel("")
        
        every { mockResponse.status } returns HttpStatusCode.OK
        coEvery { mockResponse.bodyAsChannel() } returns mockChannel

        val results = AIStream.fromResponse(mockResponse).toList()
        assertTrue(results.isEmpty())
    }

    @Test
    fun `should handle malformed stream data`() = runTest {
        val mockResponse = mockk<HttpResponse>()
        val mockChannel = ByteReadChannel("invalid data format")
        
        every { mockResponse.status } returns HttpStatusCode.OK
        coEvery { mockResponse.bodyAsChannel() } returns mockChannel

        val results = AIStream.fromResponse(mockResponse).toList()
        assertTrue(results.isEmpty())
    }

    @Test
    fun `should handle abort signal`() = runTest {
        val mockResponse = mockk<HttpResponse>()
        val abortController = AbortController()
        
        every { mockResponse.status } returns HttpStatusCode.OK
        coEvery { mockResponse.bodyAsChannel() } coAnswers {
            ByteReadChannel("""
                data: {"text": "Hello"}

                data: {"text": " World"}

                data: {"text": " !"}
            """.trimIndent())
        }

        val flow = AIStream.fromResponse(mockResponse, abortController.signal)
        
        // Start collecting in a separate coroutine
        val deferred = async {
            try {
                delay(50) // Give time for the flow to start
                abortController.abort()
                flow.toList()
                throw AssertionError("Expected CancellationException")
            } catch (e: CancellationException) {
                // Expected - test passes
            }
        }
        
        // Wait for collection to complete
        deferred.await()
    }

    @Test
    fun `should propagate CancellationException`() = runTest {
        val mockResponse = mockk<HttpResponse>()
        
        every { mockResponse.status } returns HttpStatusCode.OK
        coEvery { mockResponse.bodyAsChannel() } coAnswers {
            throw CancellationException("Test cancellation")
        }

        assertThrows<CancellationException> {
            AIStream.fromResponse(mockResponse).toList()
        }
    }
}