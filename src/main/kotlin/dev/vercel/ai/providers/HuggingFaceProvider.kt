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
 * HuggingFace provider implementation for the Vercel AI SDK
 */
class HuggingFaceProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api-inference.huggingface.co/models",
    private val client: OkHttpClient = OkHttpClient()
) : AIModel {
    private val mapper = jacksonObjectMapper()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun complete(
        prompt: String,
        options: ProviderOptions,
        signal: AbortSignal?
    ): Flow<String> {
        val requestBody = buildMap<String, Any?> {
            put("inputs", prompt)
            put("parameters", mapOf(
                "max_new_tokens" to options.maxTokens,
                "temperature" to options.temperature,
                "stream" to true
            ))
        }.filterValues { it != null }

        return RetryHandler.withRetry {
            val request = Request.Builder()
                .url("$baseUrl/${options.model}")
                .post(mapper.writeValueAsString(requestBody).toRequestBody(jsonMediaType))
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "text/event-stream")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.code == 429) {
                    throw AIError.RateLimitError(
                        provider = "huggingface",
                        retryAfter = response.header("Retry-After")?.toLongOrNull()?.times(1000)
                    )
                }
                AIStream.fromResponse(response)
            } catch (e: Exception) {
                throw when (e) {
                    is AIError -> e
                    else -> AIError.ProviderError(
                        statusCode = 500,
                        message = e.message ?: "Unknown error",
                        provider = "huggingface"
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
        if (tools != null) {
            throw AIError.ConfigurationError("HuggingFace provider does not support tools/functions yet")
        }

        val conversation = messages.joinToString("\n") { message ->
            when (message.role) {
                "user" -> "User: ${message.content}"
                "assistant" -> "Assistant: ${message.content}"
                "system" -> message.content
                else -> throw AIError.ConfigurationError("Unsupported message role: ${message.role}")
            }
        }
        
        return complete(conversation, options, signal)
    }
}