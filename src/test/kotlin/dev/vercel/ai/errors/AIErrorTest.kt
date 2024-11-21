package dev.vercel.ai.errors

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AIErrorTest {
    @Test
    fun `RetryHandler should retry on retryable errors`() = runBlocking {
        var attempts = 0
        val result = RetryHandler.withRetry {
            attempts++
            if (attempts < 2) {
                throw AIError.RateLimitError("test-provider", 1000)
            }
            "success"
        }
        
        assertEquals(2, attempts)
        assertEquals("success", result)
    }
    
    @Test
    fun `RetryHandler should not retry on non-retryable errors`() = runBlocking {
        var attempts = 0
        val error = assertThrows<AIError.ConfigurationError> {
            RetryHandler.withRetry {
                attempts++
                throw AIError.ConfigurationError("test error")
            }
        }
        
        assertEquals(1, attempts)
        assertEquals("test error", error.message)
    }
    
    @Test
    fun `RetryHandler should respect max retries`() = runBlocking {
        var attempts = 0
        val config = RetryConfig(maxRetries = 3)
        
        val error = assertThrows<AIError.RateLimitError> {
            RetryHandler.withRetry(config) {
                attempts++
                throw AIError.RateLimitError("test-provider", 1000)
            }
        }
        
        assertEquals(3, attempts)
        assertEquals("test-provider", error.provider)
    }
    
    @Test
    fun `RetryHandler should respect custom retry predicate`() = runBlocking {
        var attempts = 0
        val config = RetryConfig(
            maxRetries = 3,
            shouldRetry = { error -> error is AIError.ConfigurationError }
        )
        
        val error = assertThrows<AIError.RateLimitError> {
            RetryHandler.withRetry(config) {
                attempts++
                throw AIError.RateLimitError("test-provider", 1000)
            }
        }
        
        assertEquals(1, attempts)
        assertEquals("test-provider", error.provider)
    }
}