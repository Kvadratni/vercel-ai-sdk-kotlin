package dev.vercel.ai.providers

import dev.vercel.ai.AIModel
import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.errors.RetryHandler
import dev.vercel.ai.stream.AIStream
import dev.vercel.ai.options.CohereOptions
import dev.vercel.ai.options.ProviderOptions
import dev.vercel.ai.tools.CallableTool
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

class CohereProvider(
    protected val options: CohereOptions,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
) : AIModel {

    @Serializable
    data class Message(
        val role: String,
        val content: String
    )

    override suspend fun complete(
        prompt: String,
        options: ProviderOptions,
        signal: AbortSignal?
    ): Flow<String> {
        throw NotImplementedError("Cohere complete not implemented")
    }

    override suspend fun chat(
        messages: List<ChatMessage>,
        options: ProviderOptions,
        tools: List<CallableTool>?,
        signal: AbortSignal?
    ): Flow<String> {
        if (messages.isEmpty()) {
            throw IllegalArgumentException("No messages provided")
        }

        // Convert messages to Cohere format
        val cohereMessages = messages.map {
            Message(role = it.role, content = it.content)
        }

        // Build request body
        val requestBody = buildJsonObject {
            put("message", JsonPrimitive(messages.last().content))
            put("model", JsonPrimitive(options.toMap()["model"]?.toString() ?: "command"))
            
            // Add chat history if available
            if (messages.size > 1) {
                putJsonArray("chat_history") {
                    messages.dropLast(1).forEach { msg ->
                        addJsonObject {
                            put("role", JsonPrimitive(msg.role))
                            put("content", JsonPrimitive(msg.content))
                        }
                    }
                }
            }
            
            // Add temperature and other model parameters
            options.toMap()["temperature"]?.let { 
                put("temperature", JsonPrimitive(it.toString().toDouble()))
            }
            options.toMap()["maxTokens"]?.let { 
                put("max_tokens", JsonPrimitive(it.toString().toInt()))
            }

            // Add stream parameter
            put("stream", JsonPrimitive(true))
        }

        return RetryHandler.withRetry {
            try {
                val response = httpClient.post {
                    url("${options.toMap()["baseUrl"]?.toString() ?: "https://api.cohere.ai/v1"}/chat")
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${options.toMap()["apiKey"]?.toString() ?: this@CohereProvider.options.apiKey}")
                    header("Accept", "text/event-stream")
                    setBody(requestBody.toString())
                }

                if (!response.status.isSuccess()) {
                    throw AIError.ProviderError(
                        statusCode = response.status.value,
                        message = response.bodyAsText(),
                        provider = "cohere"
                    )
                }

                flow {
                    response.bodyAsText().split("\n").forEach { line ->
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ")
                            if (data != "[DONE]") {
                                val json = Json.parseToJsonElement(data).jsonObject
                                json["text"]?.jsonPrimitive?.content?.let { emit(it) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                throw when (e) {
                    is AIError -> e
                    else -> AIError.ProviderError(
                        statusCode = 500,
                        message = e.message ?: "Unknown error",
                        provider = "cohere"
                    )
                }
            }
        }
    }
}