package dev.vercel.ai.integration

import dev.vercel.ai.ChatMessage
import dev.vercel.ai.options.OpenAIOptions
import dev.vercel.ai.options.ModelParameters
import dev.vercel.ai.tools.CallableTool
import dev.vercel.ai.tools.ToolDefinition
import dev.vercel.ai.tools.ToolFunction
import dev.vercel.ai.tools.ToolParameter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.assertTrue

class OpenAIProviderIntegrationTest : BaseIntegrationTest() {
    
    @Test
    @Timeout(30) // 30 second timeout
    fun `should complete text with GPT-3_5`() = runBlocking {
        val options = OpenAIOptions.gpt35Turbo(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 100
            )
        )
        
        val response = openAIProvider.complete(
            prompt = "Write a haiku about programming",
            options = options
        ).toList()
        
        assertTrue(response.isNotEmpty(), "Response should not be empty")
        response.forEach { chunk ->
            validateStreamResponse(chunk)
        }
    }
    
    @Test
    @Timeout(30)
    fun `should handle chat conversation with GPT-4`() = runBlocking {
        val messages = listOf(
            ChatMessage(role = "system", content = "You are a helpful assistant."),
            ChatMessage(role = "user", content = "What is the capital of France?")
        )
        
        val options = OpenAIOptions.gpt4(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 100
            )
        )
        
        val response = openAIProvider.chat(
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
    fun `should handle function calling`() = runBlocking {
        val messages = listOf(
            ChatMessage(role = "user", content = "What's the weather in London?")
        )
        
        val getWeather = object : CallableTool {
            override val definition = ToolDefinition(
                function = ToolFunction(
                    name = "get_weather",
                    description = "Get the current weather in a location",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "location" to mapOf(
                                "type" to "string",
                                "description" to "The city name"
                            )
                        ),
                        "required" to listOf("location")
                    )
                )
            )
            
            override suspend fun call(arguments: Map<String, Any>): Any {
                return mapOf(
                    "temperature" to 20,
                    "condition" to "sunny"
                )
            }
        }
        
        val options = OpenAIOptions.gpt4(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 100
            )
        )
        
        val response = openAIProvider.chat(
            messages = messages,
            options = options,
            tools = listOf(getWeather)
        ).toList()
        
        assertTrue(response.isNotEmpty(), "Response should not be empty")
        response.forEach { chunk ->
            validateStreamResponse(chunk)
        }
    }

    override fun validateStreamResponse(chunk: String) {
        assertTrue(chunk.isNotBlank(), "Response chunk should not be blank")
    }
}