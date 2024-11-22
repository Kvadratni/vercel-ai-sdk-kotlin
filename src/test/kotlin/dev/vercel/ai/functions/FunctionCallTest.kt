package dev.vercel.ai.functions

import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.options.ModelParameters
import dev.vercel.ai.options.OpenAIOptions
import dev.vercel.ai.providers.OpenAIProvider
import dev.vercel.ai.tools.CallableTool
import dev.vercel.ai.tools.ToolDefinition
import dev.vercel.ai.tools.ToolFunction
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FunctionCallTest {
    @Test
    fun `chat should handle function calls correctly`() = runBlocking {
        val mockClient = mockk<OkHttpClient>()
        val mockResponse = mockk<Response>(relaxed = true)
        
        // Mock response with function call
        every { mockResponse.code } returns 200
        every { mockResponse.body } returns """
            data: {"role": "assistant", "content": null, "function_call": {"name": "searchEmails", "arguments": "{\"query\": \"investor updates\", \"has_attachments\": false}"}}
            
            data: {"role": "function", "name": "searchEmails", "content": "[{\"id\":\"1\",\"subject\":\"Q1 Investor Update\",\"date\":\"Apr 1, 2023\"}]"}
            
            data: {"role": "assistant", "content": "I found an investor update from Q1 2023. Would you like me to open it?"}
            
        """.trimIndent().toResponseBody()
        every { mockClient.newCall(any()).execute() } returns mockResponse
        
        // Create test tool
        val searchEmails = object : CallableTool {
            override val definition = ToolDefinition(
                function = ToolFunction(
                    name = "searchEmails",
                    description = "Search through email messages",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "query" to mapOf(
                                "type" to "string",
                                "description" to "The search query"
                            ),
                            "has_attachments" to mapOf(
                                "type" to "boolean",
                                "description" to "Filter for emails with attachments"
                            )
                        ),
                        "required" to listOf("query")
                    )
                )
            )
            
            override suspend fun call(arguments: Map<String, Any>): Any {
                return listOf(
                    mapOf(
                        "id" to "1",
                        "subject" to "Q1 Investor Update",
                        "date" to "Apr 1, 2023"
                    )
                )
            }
        }
        
        val provider = OpenAIProvider(
            apiKey = "test-key",
            client = mockClient
        )
        
        val messages = listOf(
            ChatMessage(role = "user", content = "Find my investor updates")
        )
        
        val result = provider.chat(
            messages = messages,
            options = OpenAIOptions.gpt4(
                ModelParameters.Chat(
                    temperature = 0.7,
                    maxTokens = 1000
                )
            ),
            tools = listOf(searchEmails)
        ).toList()
        
        assertEquals(3, result.size)
        assertNotNull(result[2])
        assert(result[2].contains("I found an investor update"))
    }
    
    @Test
    fun `should handle tool call validation`() = runBlocking {
        val tool = object : CallableTool {
            override val definition = ToolDefinition(
                function = ToolFunction(
                    name = "testTool",
                    description = "A test tool",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "required_param" to mapOf(
                                "type" to "string",
                                "description" to "Required parameter"
                            ),
                            "optional_param" to mapOf(
                                "type" to "number",
                                "description" to "Optional parameter"
                            )
                        ),
                        "required" to listOf("required_param")
                    )
                )
            )
            
            override suspend fun call(arguments: Map<String, Any>): Any {
                require(arguments.containsKey("required_param")) { "Missing required parameter" }
                return "Success"
            }
        }
        
        // Test with valid arguments
        val result = tool.call(mapOf(
            "required_param" to "test",
            "optional_param" to 42.0
        ))
        assertEquals("Success", result)
        
        // Test with missing required parameter
        try {
            tool.call(mapOf(
                "optional_param" to 42.0
            ))
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Missing required parameter", e.message)
        }
    }
}