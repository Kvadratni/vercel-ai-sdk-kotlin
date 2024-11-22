package dev.vercel.ai.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope

/**
 * Kotlin implementation of the AbortController pattern
 */
class AbortController {
    private var _isAborted = false
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
        _isAborted = true
        job?.cancel(CancellationException("Operation aborted"))
    }
    
    internal fun attachJob(newJob: Job) {
        job = newJob
    }

    internal fun isAborted() = _isAborted
}

/**
 * Signal that can be used to check if an operation has been aborted
 */
class AbortSignal(private val controller: AbortController) {
    /**
     * Whether the signal has been aborted
     */
    val isAborted: Boolean
        get() = controller.isAborted()

    /**
     * Throws if the operation has been aborted
     */
    @Throws(CancellationException::class)
    fun throwIfAborted() {
        if (isAborted) {
            throw CancellationException("Operation aborted")
        }
    }
    
    /**
     * Attaches a coroutine scope to this signal
     */
    suspend fun withScope(block: suspend () -> Unit) = coroutineScope {
        controller.attachJob(coroutineContext[Job]!!)
        try {
            block()
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            throw e
        }
    }
}