package dev.vercel.ai.providers

import dev.vercel.ai.AIModel
import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.errors.RetryHandler
import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.models.ToolCall
import dev.vercel.ai.options.ProviderOptions
import dev.vercel.ai.stream.AIStream
import dev.vercel.ai.tools.CallableTool
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.serialization.json.JsonObject

/**
 * OpenAI provider implementation for the Vercel AI SDK
 */
class OpenAIProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val client: OkHttpClient = OkHttpClient()
) : AIModel {
    private val mapper = jacksonObjectMapper()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun complete(
        prompt: String,
        options: ProviderOptions,
        signal: AbortSignal?
    ): Flow<String> {
        val requestBody = options.toMap().toMutableMap().apply {
            put("prompt", prompt)
            put("stream", true)
        }

        return RetryHandler.withRetry {
            val request = Request.Builder()
                .url("$baseUrl/completions")
                .post(mapper.writeValueAsString(requestBody).toRequestBody(jsonMediaType))
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "text/event-stream")
                .build()

            try {
                val response = client.newCall(request).execute()
                AIStream.fromResponse(response)
            } catch (e: Exception) {
                throw when (e) {
                    is AIError -> e
                    else -> AIError.ProviderError(
                        statusCode = 500,
                        message = e.message ?: "Unknown error",
                        provider = "openai"
                    )
                }
            }
        }
    }

    override suspend fun chat(
        messages: List<ChatMessage>,
        options: ProviderOptions,
        tools: List<CallableTool>?,
        signal: AbortSignal?
    ): Flow<String> {
        val formattedMessages = messages.map { message ->
            buildMap<String, Any?> {
                put("role", message.role)
                put("content", message.content)
                message.name?.let { put("name", it) }
                
                // Handle function calls
                message.functionCall?.let { functionCall ->
                    put("function_call", mapOf(
                        "name" to functionCall.name,
                        "arguments" to functionCall.arguments
                    ))
                }
                
                // Handle tool calls
                message.toolCalls?.let { toolCalls ->
                    put("tool_calls", toolCalls.map { toolCall ->
                        when (toolCall) {
                            is ToolCall.Function -> mapOf(
                                "id" to toolCall.id,
                                "type" to toolCall.type,
                                "function" to mapOf(
                                    "name" to toolCall.name,
                                    "arguments" to toolCall.arguments
                                )
                            )
                        }
                    })
                }
            }.filterValues { it != null }
        }

        val requestBody = options.toMap().toMutableMap().apply {
            put("messages", formattedMessages)
            put("stream", true)
            
            // Handle tools configuration
            tools?.let { toolList ->
                // First try to use the newer tools format
                put("tools", toolList.map { tool ->
                    mapOf<String, Any>(
                        "type" to "function",
                        "function" to mapOf(
                            "name" to tool.definition.function.name,
                            "description" to tool.definition.function.description,
                            "parameters" to tool.definition.function.parameters
                        )
                    )
                })
                
                // Also include functions for backward compatibility
                put("functions", toolList.map { tool ->
                    mapOf<String, Any>(
                        "name" to tool.definition.function.name,
                        "description" to tool.definition.function.description,
                        "parameters" to tool.definition.function.parameters
                    )
                })
            }
        }

        return RetryHandler.withRetry {
            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .post(mapper.writeValueAsString(requestBody).toRequestBody(jsonMediaType))
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "text/event-stream")
                .build()

            try {
                val response = client.newCall(request).execute()
                AIStream.fromResponse(response)
            } catch (e: Exception) {
                throw when (e) {
                    is AIError -> e
                    else -> AIError.ProviderError(
                        statusCode = 500,
                        message = e.message ?: "Unknown error",
                        provider = "openai"
                    )
                }
            }
        }
    }
}