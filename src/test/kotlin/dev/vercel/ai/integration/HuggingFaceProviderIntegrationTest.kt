package dev.vercel.ai.integration

import dev.vercel.ai.ChatMessage
import dev.vercel.ai.options.HuggingFaceOptions
import dev.vercel.ai.options.ModelParameters
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.providers.HuggingFaceProvider
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class HuggingFaceProviderIntegrationTest : BaseIntegrationTest() {
    
    @Test
    @Timeout(30)
    fun `should complete text with GPT-2`() = runBlocking {
        val options = HuggingFaceOptions.gpt2(
            ModelParameters.TextGeneration(
                temperature = 0.7,
                maxTokens = 100
            )
        )
        
        val response = huggingFaceProvider.complete(
            prompt = "Once upon a time",
            options = options
        ).toList()
        
        assertTrue(response.isNotEmpty(), "Response should not be empty")
        response.forEach { chunk ->
            validateStreamResponse(chunk)
        }
    }
    
    @Test
    @Timeout(30)
    fun `should handle chat with Blenderbot`() = runBlocking {
        val messages = listOf(
            ChatMessage(role = "user", content = "Hello, how are you?")
        )
        
        val options = HuggingFaceOptions.blenderbot(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 100
            )
        )
        
        val response = huggingFaceProvider.chat(
            messages = messages,
            options = options
        ).toList()
        
        assertTrue(response.isNotEmpty(), "Response should not be empty")
        response.forEach { chunk ->
            validateStreamResponse(chunk)
        }
    }
    
    @Test
    @Timeout(30)
    fun `should handle rate limit errors`() = runBlocking {
        // Create provider with invalid API key to trigger rate limit
        val invalidProvider = HuggingFaceProvider(
            apiKey = "invalid-key",
            baseUrl = properties.getProperty("huggingface.base.url"),
            httpClient = HttpClient {
                install(ContentNegotiation) {
                    json()
                }
            }
        )
        
        val options = HuggingFaceOptions.gpt2()
        
        val error = assertThrows<AIError.ProviderError> {
            invalidProvider.complete(
                prompt = "Test prompt",
                options = options
            ).toList()
        }
        
        assertTrue(error.statusCode in listOf(401, 429), "Should get auth error or rate limit")
    }
    
    @Test
    @Timeout(30)
    fun `should handle different models for completion`() = runBlocking {
        val models = listOf(
            "gpt2",
            "gpt2-medium",
            "gpt2-large"
        )
        
        models.forEach { model ->
            val options = HuggingFaceOptions(
                model = model,
                temperature = 0.7,
                maxTokens = 100
            )
            
            val response = huggingFaceProvider.complete(
                prompt = "The future of AI is",
                options = options
            ).toList()
            
            assertTrue(response.isNotEmpty(), "Response should not be empty for $model")
            response.forEach { chunk ->
                validateStreamResponse(chunk)
            }
        }
    }
    
    @Test
    @Timeout(30)
    fun `should handle generation parameters`() = runBlocking {
        val options = HuggingFaceOptions.gpt2(
            ModelParameters.TextGeneration(
                temperature = 0.9,
                maxTokens = 50,
                topP = 0.9,
            )
        )
        
        val response = huggingFaceProvider.complete(
            prompt = "Write a story about",
            options = options
        ).toList()
        
        assertTrue(response.isNotEmpty(), "Response should not be empty")
        response.forEach { chunk ->
            validateStreamResponse(chunk)
        }
    }
    
    @Test
    @Timeout(30)
    fun `should handle malformed responses`() = runBlocking {
        val options = HuggingFaceOptions(
            model = "invalid-model",
            temperature = 0.7,
            maxTokens = 100
        )
        
        val error = assertThrows<AIError.ProviderError> {
            huggingFaceProvider.complete(
                prompt = "Test prompt",
                options = options
            ).toList()
        }
        
        assertTrue(error.statusCode in 400..499, "Should get client error for invalid model")
    }
}