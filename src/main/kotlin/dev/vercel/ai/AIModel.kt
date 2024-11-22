package dev.vercel.ai

import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.options.ProviderOptions
import dev.vercel.ai.tools.CallableTool
import dev.vercel.ai.models.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Base interface for AI model implementations
 */
interface AIModel {
    /**
     * Generates a completion for the given prompt
     */
    suspend fun complete(
        prompt: String,
        options: ProviderOptions,
        signal: AbortSignal? = null
    ): Flow<String>
    
    /**
     * Generates a chat completion for the given messages
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        options: ProviderOptions,
        tools: List<CallableTool>? = null,
        signal: AbortSignal? = null
    ): Flow<String>
}