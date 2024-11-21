package dev.vercel.ai.integration

import dev.vercel.ai.ChatMessage
import dev.vercel.ai.options.AnthropicOptions
import dev.vercel.ai.options.ModelParameters
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.providers.AnthropicProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class AnthropicProviderIntegrationTest : BaseIntegrationTest() {
    
    @Test
    @Timeout(30) // 30 second timeout
    fun `should complete chat with Claude3 Opus`() = runBlocking {
        val options = AnthropicOptions.claude3Opus(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 100
            )
        )
        
        val messages = listOf(
            ChatMessage(role = "user", content = "Write a haiku about artificial intelligence")
        )
        
        val response = anthropicProvider.chat(
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
    fun `should handle chat conversation with system message`() = runBlocking {
        val messages = listOf(
            ChatMessage(role = "system", content = "You are a helpful AI assistant that only speaks in haiku."),
            ChatMessage(role = "user", content = "What is the capital of France?")
        )
        
        val options = AnthropicOptions.claude3Opus(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 100
            )
        )
        
        val response = anthropicProvider.chat(
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
        val invalidProvider = AnthropicProvider(
            apiKey = "invalid-key",
            baseUrl = properties.getProperty("anthropic.base.url")
        )
        
        val messages = listOf(
            ChatMessage(role = "user", content = "Hello")
        )
        
        val options = AnthropicOptions.claude3Opus()
        
        val error = assertThrows<AIError.ProviderError> {
            invalidProvider.chat(
                messages = messages,
                options = options
            ).toList()
        }
        
        assertTrue(error.statusCode in listOf(401, 429), "Should get auth error or rate limit")
    }
    
    @Test
    @Timeout(30)
    fun `should handle multiple message conversation`() = runBlocking {
        val messages = listOf(
            ChatMessage(role = "system", content = "You are a helpful AI assistant."),
            ChatMessage(role = "user", content = "Hi, I'd like to learn about Paris."),
            ChatMessage(role = "assistant", content = "Paris is the capital of France. What would you like to know?"),
            ChatMessage(role = "user", content = "What's the population?")
        )
        
        val options = AnthropicOptions.claude3Opus(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 200
            )
        )
        
        val response = anthropicProvider.chat(
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
    fun `should handle different Claude models`() = runBlocking {
        val messages = listOf(
            ChatMessage(role = "user", content = "What is 2+2?")
        )
        
        // Test with different Claude models
        val models = listOf(
            AnthropicOptions.claude3Opus(),
            AnthropicOptions.claude3Sonnet(),
            AnthropicOptions.claude3Haiku()
        )
        
        models.forEach { options ->
            val response = anthropicProvider.chat(
                messages = messages,
                options = options
            ).toList()
            
            assertTrue(response.isNotEmpty(), "Response should not be empty for ${options.model}")
            response.forEach { chunk ->
                validateStreamResponse(chunk)
            }
        }
    }
}
