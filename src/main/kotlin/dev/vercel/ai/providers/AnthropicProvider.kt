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
 * Anthropic provider implementation for the Vercel AI SDK
 */
class AnthropicProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api.anthropic.com/v1",
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
            put("model", options.model)
            put("prompt", prompt)
            put("stream", true)
            put("temperature", options.temperature)
            put("max_tokens_to_sample", options.maxTokens)
            put("stop_sequences", options.stopSequences)
        }.filterValues { it != null }

        return RetryHandler.withRetry {
            val request = Request.Builder()
                .url("$baseUrl/complete")
                .post(mapper.writeValueAsString(requestBody).toRequestBody(jsonMediaType))
                .header("X-API-Key", apiKey)
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
                        provider = "anthropic"
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
            throw AIError.ConfigurationError("Anthropic provider does not support tools/functions yet")
        }

        val prompt = messages.joinToString("\n\n") { message ->
            when (message.role) {
                "user" -> "Human: ${message.content}"
                "assistant" -> "Assistant: ${message.content}"
                else -> message.content
            }
        }

        return complete(prompt, options, signal)
    }
}