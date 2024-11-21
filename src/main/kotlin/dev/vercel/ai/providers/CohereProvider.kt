package dev.vercel.ai.providers

import dev.vercel.ai.stream.AIStream
import dev.vercel.ai.options.CohereOptions
import dev.vercel.ai.common.toOkHttpResponse
import dev.vercel.ai.errors.AIError
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * Provider implementation for Cohere's chat completion API.
 *
 * @property options Configuration options for the Cohere provider
 * @property httpClient HTTP client for making API requests
 */
class CohereProvider(
    private val options: CohereOptions,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
        install(io.ktor.client.plugins.DefaultRequest) {
            contentType(ContentType.Application.Json)
        }
    }
) {
    companion object {
        private const val COHERE_API_URL = "https://api.cohere.ai/v1/chat"
    }

    @Serializable
    data class Message(
        val role: String,
        val content: String
    )

    @Serializable
    private data class ChatRequest(
        val model: String,
        val message: String,
        val temperature: Double,
        val max_tokens: Int,
        val stream: Boolean
    )

    @Serializable
    private data class ChatResponse(
        val text: String
    )

    /**
     * Creates a chat completion using the Cohere API.
     *
     * @param messages List of messages in the conversation
     * @return Flow of generated text chunks
     */
    suspend fun chat(messages: List<Message>): Flow<String> {
        val lastMessage = messages.lastOrNull()?.content ?: throw IllegalArgumentException("No messages provided")
        
        val response = httpClient.post(COHERE_API_URL) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${options.apiKey}")
            setBody(ChatRequest(
                model = options.model,
                message = lastMessage,
                temperature = options.temperature,
                max_tokens = options.maxTokens,
                stream = options.stream
            ))
        }

        if (!response.status.isSuccess()) {
            throw AIError.ProviderError(
                statusCode = response.status.value,
                message = response.bodyAsText(),
                provider = "cohere"
            )
        }

        return AIStream.fromResponse(response.toOkHttpResponse())
    }
}