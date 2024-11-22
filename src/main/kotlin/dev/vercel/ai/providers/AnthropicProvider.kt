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
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

/**
 * Anthropic provider implementation for the Vercel AI SDK
 */
class AnthropicProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api.anthropic.com/v1",
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
) : AIModel {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(
        prompt: String,
        options: ProviderOptions,
        signal: AbortSignal?
    ): Flow<String> {
        val requestBody = buildJsonObject {
            put("prompt", prompt)
            put("model", options.model)
            put("temperature", options.temperature)
            put("max_tokens_to_sample", options.maxTokens)
            put("stream", true)
            options.stopSequences?.let { sequences ->
                putJsonArray("stop_sequences") {
                    sequences.forEach { add(it) }
                }
            }
        }

        return RetryHandler.withRetry {
            val response = httpClient.post("$baseUrl/complete") {
                contentType(ContentType.Application.Json)
                header("X-API-Key", apiKey)
                header("anthropic-version", "2023-06-01")
                setBody(requestBody.toString())
            }

            when (response.status) {
                HttpStatusCode.TooManyRequests -> {
                    val retryAfter = response.headers["Retry-After"]?.toLongOrNull()?.times(1000)
                    throw AIError.RateLimitError(
                        provider = "anthropic",
                        retryAfter = retryAfter
                    )
                }
                HttpStatusCode.Unauthorized -> {
                    throw AIError.ProviderError(
                        statusCode = response.status.value,
                        message = "Invalid API key",
                        provider = "anthropic"
                    )
                }
                HttpStatusCode.BadRequest -> {
                    throw AIError.ProviderError(
                        statusCode = response.status.value,
                        message = "Bad request - check your input parameters",
                        provider = "anthropic"
                    )
                }
                else -> {
                    if (response.status.value >= 400) {
                        throw AIError.ProviderError(
                            statusCode = response.status.value,
                            message = "Unexpected error from Anthropic API",
                            provider = "anthropic"
                        )
                    }
                }
            }

            AIStream.fromResponse(response, signal) { data ->
                try {
                    val jsonElement = Json.parseToJsonElement(data)
                    jsonElement.jsonObject["completion"]?.jsonPrimitive?.content
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
            throw AIError.ConfigurationError("Anthropic provider does not support tools/functions yet")
        }

        val conversation = messages.joinToString("\n\n") { message ->
            when (message.role) {
                "user" -> "Human: ${message.content}"
                "assistant" -> "Assistant: ${message.content}"
                "system" -> message.content
                else -> throw AIError.ConfigurationError("Unsupported message role: ${message.role}")
            }
        }

        return complete(conversation, options, signal)
    }
}