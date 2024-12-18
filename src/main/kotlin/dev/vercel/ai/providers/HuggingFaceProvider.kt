package dev.vercel.ai.providers

import dev.vercel.ai.AIModel
import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.errors.RetryHandler
import dev.vercel.ai.options.ProviderOptions
import dev.vercel.ai.stream.AIStream
import dev.vercel.ai.tools.CallableTool
import kotlinx.coroutines.flow.Flow
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * HuggingFace provider implementation for the Vercel AI SDK
 */
class HuggingFaceProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api-inference.huggingface.co/models",
    private val httpClient: HttpClient
) : AIModel {
    override suspend fun complete(
        prompt: String,
        options: ProviderOptions,
        signal: AbortSignal?
    ): Flow<String> {
        val requestBody = buildJsonObject {
            put("inputs", JsonPrimitive(prompt))
            putJsonObject("parameters") {
                put("max_new_tokens", JsonPrimitive(options.maxTokens))
                put("temperature", JsonPrimitive(options.temperature))
                put("stream", JsonPrimitive(true))
            }
        }

        return RetryHandler.withRetry {
            val response = httpClient.post("$baseUrl/${options.model}") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                header("Accept", "text/event-stream")
                setBody(requestBody.toString())
            }

            when (response.status) {
                HttpStatusCode.TooManyRequests -> {
                    val retryAfter = response.headers["Retry-After"]?.toLongOrNull()?.times(1000)
                    throw AIError.RateLimitError(
                        provider = "huggingface",
                        retryAfter = retryAfter
                    )
                }
                HttpStatusCode.Unauthorized -> {
                    throw AIError.ProviderError(
                        statusCode = response.status.value,
                        message = "Invalid API key",
                        provider = "huggingface"
                    )
                }
                HttpStatusCode.BadRequest -> {
                    throw AIError.ProviderError(
                        statusCode = response.status.value,
                        message = "Bad request - check your input parameters",
                        provider = "huggingface"
                    )
                }
                else -> {
                    if (response.status.value >= 400) {
                        throw AIError.ProviderError(
                            statusCode = response.status.value,
                            message = "Unexpected error from HuggingFace API",
                            provider = "huggingface"
                        )
                    }
                }
            }

            AIStream.fromResponse(response, signal) { data ->
                try {
                    val jsonElement = Json.parseToJsonElement(data)
                    jsonElement.jsonObject["token"]?.jsonObject?.get("text")?.jsonPrimitive?.content
                } catch (e: Exception) {
                    null
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