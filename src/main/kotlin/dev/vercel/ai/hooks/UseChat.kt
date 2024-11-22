package dev.vercel.ai.hooks

import dev.vercel.ai.AIModel
import dev.vercel.ai.common.AbortController
import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.options.ProviderOptions
import dev.vercel.ai.tools.CallableTool
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion

/**
 * Hook for managing chat-based interactions with AI models
 *
 * @property model The AI model to use for chat
 * @property options Configuration options for the AI provider
 * @property tools Optional list of function calling tools
 */
class UseChat(
    private val model: AIModel,
    private val options: ProviderOptions,
    private val tools: List<CallableTool>? = null
) {
    private val messages = mutableListOf<ChatMessage>()
    private var currentController: AbortController? = null
    
    /**
     * Send a message to the chat and receive a streaming response
     *
     * @param message The message to send
     * @param systemPrompt Optional system prompt to prepend
     * @return Flow of response text chunks
     */
    suspend fun sendMessage(message: String, systemPrompt: String? = null): Flow<String> {
        // Add system prompt if provided
        if (systemPrompt != null && messages.isEmpty()) {
            messages.add(ChatMessage(content = systemPrompt, role = "system"))
        }
        
        // Add user message
        messages.add(ChatMessage(content = message, role = "user"))
        
        // Create a new controller for this request
        val requestController = AbortController()
        currentController = requestController
        
        // Get response stream
        return model.chat(
            messages = messages.toList(),
            options = options,
            tools = tools,
            signal = requestController.signal
        ).map { chunk ->
            // Accumulate assistant response
            if (chunk.isNotEmpty()) {
                if (messages.lastOrNull()?.role != "assistant") {
                    messages.add(ChatMessage(content = chunk, role = "assistant"))
                } else {
                    val lastMessage = messages.last()
                    messages[messages.lastIndex] = lastMessage.copy(
                        content = lastMessage.content + chunk
                    )
                }
            }
            chunk
        }.onCompletion { error ->
            if (error != null) {
                // Remove failed message on error
                if (messages.lastOrNull()?.role == "assistant") {
                    messages.removeLast()
                }
            }
            if (currentController === requestController) {
                currentController = null
            }
        }
    }

    /**
     * Get the current chat history
     *
     * @return List of chat messages
     */
    fun getMessages(): List<ChatMessage> = messages.toList()

    /**
     * Clear the chat history
     */
    fun clearMessages() {
        messages.clear()
    }

    /**
     * Abort the current chat request if any
     */
    fun abort() {
        currentController?.abort()
        currentController = null
    }
}