package dev.vercel.ai.providers

import dev.vercel.ai.AIModel
import dev.vercel.ai.ChatMessage
import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.errors.RetryHandler
import dev.vercel.ai.options.ProviderOptions
import dev.vercel.ai.stream.AIStream
import dev.vercel.ai.tools.CallableTool
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

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
                message.functionCall?.let { functionCall ->
                    put("function_call", mapOf(
                        "name" to functionCall.name,
                        "arguments" to functionCall.arguments
                    ))
                }
            }.filterValues { it != null }
        }

        val requestBody = options.toMap().toMutableMap().apply {
            put("messages", formattedMessages)
            put("stream", true)
            
            tools?.let { toolList ->
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