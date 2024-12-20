package dev.vercel.ai.common

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

class AbortControllerTest {
    @Test
    fun `should abort running operation`() = runTest {
        val controller = AbortController()
        var wasAborted = false
        
        val job = launch {
            try {
                controller.signal.withScope {
                    delay(1000)
                }
            } catch (e: CancellationException) {
                wasAborted = true
            }
        }
        
        delay(100)
        controller.abort()
        job.join()
        
        assertTrue(wasAborted)
    }
    
    @Test
    fun `should not affect other operations when one is aborted`() = runTest {
        val controller1 = AbortController()
        val controller2 = AbortController()
        var operation1Aborted = false
        var operation2Completed = false
        
        val job1 = launch {
            try {
                controller1.signal.withScope {
                    delay(1000)
                }
            } catch (e: CancellationException) {
                operation1Aborted = true
            }
        }
        
        val job2 = launch {
            controller2.signal.withScope {
                delay(500)
                operation2Completed = true
            }
        }
        
        delay(100)
        controller1.abort()
        
        job1.join()
        job2.join()
        
        assertTrue(operation1Aborted)
        assertTrue(operation2Completed)
    }
}