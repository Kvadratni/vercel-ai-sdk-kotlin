package dev.vercel.ai.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope

/**
 * Kotlin implementation of the AbortController pattern
 */
class AbortController {
    private var job: Job? = null
    
    /**
     * Signal for checking if the operation has been aborted
     */
    val signal: AbortSignal
        get() = AbortSignal(this)
    
    /**
     * Aborts the current operation
     */
    fun abort() {
        job?.cancel(AbortError())
    }
    
    internal fun attachJob(newJob: Job) {
        job = newJob
    }
}

/**
 * Signal that can be used to check if an operation has been aborted
 */
class AbortSignal(private val controller: AbortController) {
    /**
     * Throws if the operation has been aborted
     */
    @Throws(AbortError::class)
    suspend fun throwIfAborted() {
        // Implementation will check the job status
    }
    
    /**
     * Attaches a coroutine scope to this signal
     */
    suspend fun withScope(block: suspend () -> Unit) = coroutineScope {
        controller.attachJob(coroutineContext[Job]!!)
        block()
    }
}

/**
 * Error thrown when an operation is aborted
 */
class AbortError : CancellationException("The operation was aborted")