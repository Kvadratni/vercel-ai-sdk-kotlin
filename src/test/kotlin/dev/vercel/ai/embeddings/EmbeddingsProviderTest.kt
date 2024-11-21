package dev.vercel.ai.embeddings

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EmbeddingsProviderTest {
    @Test
    fun `OpenAI embeddings provider should handle successful response`() = runBlocking {
        val mockClient = mockk<OkHttpClient>()
        val mockCall = mockk<Call>()
        val mockResponse = mockk<Response>()
        
        // Mock successful response with embeddings
        val responseJson = """
            {
                "data": [
                    {
                        "embedding": [0.1, 0.2, 0.3],
                        "index": 0
                    }
                ],
                "model": "text-embedding-ada-002",
                "usage": {
                    "prompt_tokens": 5,
                    "total_tokens": 5
                }
            }
        """.trimIndent()
        
        every { mockResponse.code } returns 200
        every { mockResponse.body?.string() } returns responseJson
        every { mockResponse.isSuccessful } returns true
        every { mockClient.newCall(any()).execute() } returns mockResponse
        
        val provider = OpenAIEmbeddingsProvider(
            apiKey = "test-key",
            client = mockClient
        )
        
        val options = OpenAIEmbeddingsOptions.ada002()
        val result = provider.createEmbeddings("Test input", options)
        
        assertEquals(1, result.size)
        assertEquals(listOf(0.1f, 0.2f, 0.3f), result[0])
    }
    
    @Test
    fun `OpenAI embeddings provider should handle error response`() = runBlocking {
        val mockClient = mockk<OkHttpClient>()
        val mockResponse = mockk<Response>()
        
        // Mock error response
        val errorJson = """
            {
                "error": {
                    "message": "Invalid API key",
                    "type": "invalid_request_error"
                }
            }
        """.trimIndent()
        
        every { mockResponse.code } returns 401
        every { mockResponse.body?.string() } returns errorJson
        every { mockResponse.isSuccessful } returns false
        every { mockClient.newCall(any()).execute() } returns mockResponse
        
        val provider = OpenAIEmbeddingsProvider(
            apiKey = "invalid-key",
            client = mockClient
        )
        
        val options = OpenAIEmbeddingsOptions.ada002()
        
        assertFailsWith<IOException> {
            provider.createEmbeddings("Test input", options)
        }
    }
    
    @Test
    fun `Azure OpenAI embeddings provider should handle successful response`() = runBlocking {
        val mockClient = mockk<OkHttpClient>()
        val mockResponse = mockk<Response>()
        
        // Mock successful response with embeddings
        val responseJson = """
            {
                "data": [
                    {
                        "embedding": [0.1, 0.2, 0.3],
                        "index": 0
                    }
                ],
                "usage": {
                    "prompt_tokens": 5,
                    "total_tokens": 5
                }
            }
        """.trimIndent()
        
        every { mockResponse.code } returns 200
        every { mockResponse.body?.string() } returns responseJson
        every { mockResponse.isSuccessful } returns true
        every { mockClient.newCall(any()).execute() } returns mockResponse
        
        val provider = AzureOpenAIEmbeddingsProvider(
            apiKey = "test-key",
            baseUrl = "https://example.azure.openai.com/deployment-name",
            client = mockClient
        )
        
        val options = AzureOpenAIEmbeddingsOptions(
            model = "text-embedding-ada-002"
        )
        val result = provider.createEmbeddings("Test input", options)
        
        assertEquals(1, result.size)
        assertEquals(listOf(0.1f, 0.2f, 0.3f), result[0])
    }
    
    @Test
    fun `Azure OpenAI embeddings provider should handle error response`() = runBlocking {
        val mockClient = mockk<OkHttpClient>()
        val mockResponse = mockk<Response>()
        
        // Mock error response
        val errorJson = """
            {
                "error": {
                    "code": "InvalidAPIKey",
                    "message": "Invalid API key"
                }
            }
        """.trimIndent()
        
        every { mockResponse.code } returns 401
        every { mockResponse.body?.string() } returns errorJson
        every { mockResponse.isSuccessful } returns false
        every { mockClient.newCall(any()).execute() } returns mockResponse
        
        val provider = AzureOpenAIEmbeddingsProvider(
            apiKey = "invalid-key",
            baseUrl = "https://example.azure.openai.com/deployment-name",
            client = mockClient
        )
        
        val options = AzureOpenAIEmbeddingsOptions(
            model = "text-embedding-ada-002"
        )
        
        assertFailsWith<IOException> {
            provider.createEmbeddings("Test input", options)
        }
    }
}