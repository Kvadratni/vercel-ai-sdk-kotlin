package dev.vercel.ai.errors

import kotlinx.coroutines.delay

/**
 * Utility class for handling retries with exponential backoff
 */
object RetryHandler {
    /**
     * Executes a block of code with retry logic based on the provided configuration
     * @param config Retry configuration
     * @param block The code block to execute
     * @return The result of the successful execution
     * @throws AIError The last error encountered if all retries fail
     */
    suspend fun <T> withRetry(
        config: RetryConfig = RetryConfig(),
        block: suspend () -> T
    ): T {
        var lastError: AIError? = null
        var currentDelay = config.initialDelay
        
        repeat(config.maxRetries) { attempt ->
            try {
                return block()
            } catch (e: AIError) {
                lastError = e
                if (!config.shouldRetry(e)) throw e
                
                delay(currentDelay)
                currentDelay = (currentDelay * 1.5).toLong().coerceAtMost(config.maxDelay)
            }
        }
        
        throw lastError ?: AIError.ConfigurationError("Retry failed")
    }
}