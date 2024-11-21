package dev.vercel.ai.errors

/**
 * Base sealed class for all AI-related errors
 */
sealed class AIError : Exception() {
    /**
     * Error from the AI provider's API
     * @property statusCode HTTP status code from the provider
     * @property message Error message
     * @property provider Name of the AI provider (e.g., "openai", "anthropic")
     */
    data class ProviderError(
        val statusCode: Int,
        override val message: String,
        val provider: String
    ) : AIError()
    
    /**
     * Error during stream processing
     * @property message Error message
     * @property cause Original exception that caused this error
     */
    data class StreamError(
        override val message: String,
        override val cause: Throwable?
    ) : AIError()
    
    /**
     * Error during function calling
     * @property functionName Name of the function that failed
     * @property message Error message
     */
    data class FunctionCallError(
        val functionName: String,
        override val message: String
    ) : AIError()
    
    /**
     * Error in SDK configuration
     * @property message Error message
     */
    data class ConfigurationError(
        override val message: String
    ) : AIError()
    
    /**
     * Rate limit exceeded error
     * @property provider Name of the AI provider
     * @property retryAfter Time in milliseconds after which to retry, if available
     */
    data class RateLimitError(
        val provider: String,
        val retryAfter: Long?
    ) : AIError()
}

/**
 * Configuration for retry behavior
 * @property maxRetries Maximum number of retry attempts
 * @property initialDelay Initial delay in milliseconds between retries
 * @property maxDelay Maximum delay in milliseconds between retries
 * @property shouldRetry Function that determines if an error should trigger a retry
 */
data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelay: Long = 1000,
    val maxDelay: Long = 10000,
    val shouldRetry: (AIError) -> Boolean = { error ->
        error is AIError.RateLimitError || 
        (error is AIError.ProviderError && error.statusCode in 500..599)
    }
)