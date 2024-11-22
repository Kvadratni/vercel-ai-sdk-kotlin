package dev.vercel.ai.hooks

import dev.vercel.ai.AIModel
import dev.vercel.ai.common.AbortController
import dev.vercel.ai.models.CompletionMessage
import dev.vercel.ai.options.ProviderOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion

/**
 * Hook for managing completion-based interactions with AI models
 *
 * @property model The AI model to use for completions
 * @property options Configuration options for the AI provider
 */
class UseCompletion(
    private val model: AIModel,
    private val options: ProviderOptions
) {
    private var lastMessage: CompletionMessage? = null
    private var lastResponse: CompletionMessage? = null
    private var currentController: AbortController? = null
    
    /**
     * Send a prompt to the model and receive a streaming response
     *
     * @param prompt The prompt to send
     * @return Flow of response text chunks
     */
    suspend fun complete(prompt: String): Flow<String> {
        // Store user prompt
        lastMessage = CompletionMessage(content = prompt)
        lastResponse = null
        
        // Create a new AbortController for this request
        val requestController = AbortController()
        currentController = requestController
        
        // Get response stream
        return model.complete(
            prompt = prompt,
            options = options,
            signal = requestController.signal
        ).map { chunk ->
            // Accumulate assistant response
            if (chunk.isNotEmpty()) {
                lastResponse = if (lastResponse == null) {
                    CompletionMessage(content = chunk, role = "assistant")
                } else {
                    lastResponse!!.copy(content = lastResponse!!.content + chunk)
                }
            }
            chunk
        }.onCompletion { error ->
            if (error != null) {
                // Clear response on error
                lastResponse = null
            }
            if (currentController === requestController) {
                currentController = null
            }
        }
    }

    /**
     * Get the last user message
     *
     * @return The last user message or null if none exists
     */
    fun getLastMessage(): CompletionMessage? = lastMessage
    
    /**
     * Get the last model response
     *
     * @return The last model response or null if none exists
     */
    fun getLastResponse(): CompletionMessage? = lastResponse
    
    /**
     * Clear the last message and response
     */
    fun clear() {
        lastMessage = null
        lastResponse = null
    }

    /**
     * Abort the current completion request if any
     */
    fun abort() {
        currentController?.abort()
        currentController = null
    }
}