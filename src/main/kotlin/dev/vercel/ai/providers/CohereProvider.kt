package dev.vercel.ai.providers

import dev.vercel.ai.AIModel
import dev.vercel.ai.ChatMessage
import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.options.CohereOptions
import dev.vercel.ai.options.ProviderOptions
import dev.vercel.ai.stream.AIStream
import dev.vercel.ai.tools.CallableTool
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

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
        TODO("Not implemented")
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

        // Make API request and return response
        TODO("Not implemented")
    }
}