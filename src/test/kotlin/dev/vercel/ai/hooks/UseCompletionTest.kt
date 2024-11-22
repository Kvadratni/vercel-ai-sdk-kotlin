package dev.vercel.ai.hooks

import dev.vercel.ai.AIModel
import dev.vercel.ai.common.AbortError
import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.models.CompletionMessage
import dev.vercel.ai.options.ProviderOptions
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UseCompletionTest {
    private val mockModel = mockk<AIModel>()
    private val mockOptions = mockk<ProviderOptions>()

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
        assertEquals("user", lastMessage?.role)
        
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
        assertEquals("user", lastMessage?.role)
        
        assertNull(useCompletion.getLastResponse())
    }

    @Test
    fun `abort should cancel ongoing request`() = runTest {
        val useCompletion = UseCompletion(mockModel, mockOptions)
        
        coEvery {
            mockModel.complete(any(), any(), any())
        } coAnswers {
            val signal = arg<AbortSignal>(2)
            if (signal?.isAborted == true) {
                throw CancellationException("Aborted")
            }
            flowOf("Response")
        }

        val flow = useCompletion.complete("Hi")
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