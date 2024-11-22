package dev.vercel.ai.hooks

import dev.vercel.ai.AIModel
import dev.vercel.ai.common.AbortController
import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.models.CompletionMessage
import dev.vercel.ai.options.ProviderOptions
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UseCompletionTest {
    private val mockModel = mockk<AIModel>(relaxed = true)
    private val mockOptions = mockk<ProviderOptions>(relaxed = true)

    @Test
    fun `complete should handle successful response`() = runTest {
        val useCompletion = UseCompletion(mockModel, mockOptions)
        
        coEvery {
            mockModel.complete(any(), any(), any())
        } returns flowOf("Hello", " world", "!")

        val response = useCompletion.complete("Hi").toList()
        
        assertEquals(listOf("Hello", " world", "!"), response)
        
        val lastMessage = useCompletion.getLastMessage()
        assertEquals("Hi", lastMessage?.content)
        
        val lastResponse = useCompletion.getLastResponse()
        assertEquals("Hello world!", lastResponse?.content)
        assertEquals("assistant", lastResponse?.role)
    }

    @Test
    fun `complete should handle error response`() = runTest {
        val useCompletion = UseCompletion(mockModel, mockOptions)
        
        coEvery {
            mockModel.complete(any(), any(), any())
        } throws AIError.ProviderError(500, "Server error", "test")

        assertThrows<AIError.ProviderError> {
            useCompletion.complete("Hi").toList()
        }
        
        val lastMessage = useCompletion.getLastMessage()
        assertEquals("Hi", lastMessage?.content)
        
        assertNull(useCompletion.getLastResponse())
    }

    @Test
    fun `abort should cancel ongoing request with CancellationException`() = runTest {
        val useCompletion = UseCompletion(mockModel, mockOptions)
        val signal = slot<AbortSignal>()
        
        coEvery {
            mockModel.complete(any(), any(), capture(signal))
        } coAnswers {
            flow {
                emit("Response part 1")
                signal.captured.throwIfAborted()
                emit("Response part 2")
            }
        }

        val flow = useCompletion.complete("Hi")
        delay(50) // Give time for the flow to start
        useCompletion.abort()
        
        assertThrows<CancellationException> {
            flow.toList()
        }
    }

    @Test
    fun `abort should handle nested CancellationException`() = runTest {
        val useCompletion = UseCompletion(mockModel, mockOptions)
        val signal = slot<AbortSignal>()
        
        coEvery {
            mockModel.complete(any(), any(), capture(signal))
        } coAnswers {
            flow {
                emit("Response part 1")
                signal.captured.throwIfAborted()
                emit("Response part 2")
            }
        }

        val flow = useCompletion.complete("Hi")
        delay(50) // Give time for the flow to start
        useCompletion.abort()
        
        assertThrows<CancellationException> {
            flow.toList()
        }
    }

    @Test
    fun `clear should reset state`() = runTest {
        val useCompletion = UseCompletion(mockModel, mockOptions)
        
        coEvery {
            mockModel.complete(any(), any(), any())
        } returns flowOf("Response")

        useCompletion.complete("Hi").toList()
        assertEquals("Hi", useCompletion.getLastMessage()?.content)
        assertEquals("Response", useCompletion.getLastResponse()?.content)
        
        useCompletion.clear()
        assertNull(useCompletion.getLastMessage())
        assertNull(useCompletion.getLastResponse())
    }
}