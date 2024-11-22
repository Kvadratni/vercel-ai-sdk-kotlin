package dev.vercel.ai.functions

import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.options.ModelParameters
import dev.vercel.ai.options.OpenAIOptions
import dev.vercel.ai.providers.OpenAIProvider
import dev.vercel.ai.tools.CallableTool
import dev.vercel.ai.tools.ToolDefinition
import dev.vercel.ai.tools.ToolFunction
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FunctionCallTest {
    @Test
    fun `chat should handle function calls correctly`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    data: {"role": "assistant", "content": null, "function_call": {"name": "searchEmails", "arguments": "{\\"query\\": \\"investor updates\\", \\"has_attachments\\": false}"}}
                    
                    data: {"role": "function", "name": "searchEmails", "content": "[{\\"id\\":\\"1\\",\\"subject\\":\\"Q1 Investor Update\\",\\"date\\":\\"Apr 1, 2023\\"}]"}
                    
                    data: {"role": "assistant", "content": "I found an investor update from Q1 2023. Would you like me to open it?"}
                    
                    data: [DONE]
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
        
        val provider = OpenAIProvider(
            apiKey = "test-key",
            httpClient = client
        )
        
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
}