package dev.vercel.ai

import dev.vercel.ai.errors.AIError
import dev.vercel.ai.stream.AIStream
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AIStreamTest {
    @Test
    fun `should handle successful streaming response`() = runBlocking {
        val mockResponse = mockk<Response>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns """
            data: {"text": "Hello"}
            
            data: {"text": " World"}
            
            data: [DONE]
        """.trimIndent().toResponseBody()

        val flow = AIStream.fromResponse(mockResponse)
        val results = flow.toList()

        assertEquals(2, results.size)
        assertTrue(results[0].contains("Hello"))
        assertTrue(results[1].contains("World"))
    }

    @Test
    fun `should handle error response`() = runBlocking {
        val mockResponse = mockk<Response>()
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code } returns 500
        every { mockResponse.message } returns "Internal Server Error"

        val error = assertThrows<AIError.ProviderError> {
            AIStream.fromResponse(mockResponse).toList()
        }

        assertEquals(500, error.statusCode)
        assertEquals("Internal Server Error", error.message)
    }

    @Test
    fun `should handle empty response body`() = runBlocking {
        val mockResponse = mockk<Response>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns null

        val error = assertThrows<AIError.StreamError> {
            AIStream.fromResponse(mockResponse).toList()
        }

        assertEquals("Empty response body", error.message)
    }

    @Test
    fun `should handle malformed stream data`() = runBlocking {
        val mockResponse = mockk<Response>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns "invalid data format".toResponseBody()

        val results = AIStream.fromResponse(mockResponse).toList()
        assertTrue(results.isEmpty())
    }
}