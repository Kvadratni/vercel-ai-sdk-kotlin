package dev.vercel.ai.tools

import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.options.ModelParameters
import dev.vercel.ai.options.OpenAIOptions
import dev.vercel.ai.providers.OpenAIProvider
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ToolCallTest {
    @Test
    fun `chat should handle tool calls correctly`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    data: {"role": "assistant", "content": null, "tool_calls": [{"id": "call_123", "type": "function", "function": {"name": "searchEmails", "arguments": "{\\"query\\": \\"investor updates\\", \\"has_attachments\\": false}"}}]}
                    
                    data: {"role": "tool", "name": "searchEmails", "content": "[{\\"id\\":\\"1\\",\\"subject\\":\\"Q1 Investor Update\\",\\"date\\":\\"Apr 1, 2023\\"}]"}
                    
                    data: {"role": "assistant", "content": "I found an investor update from Q1 2023. Would you like me to open it?"}
                    
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString())
            )
        }
        
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        
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
            httpClient = client
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
        assertTrue(result[2].contains("I found an investor update"))
    }
}