package dev.vercel.ai.hooks

import dev.vercel.ai.AIModel
import dev.vercel.ai.common.AbortError
import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.options.ProviderOptions
import dev.vercel.ai.tools.CallableTool
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UseChatTest {
    private val mockModel = mockk<AIModel>()
    private val mockOptions = mockk<ProviderOptions>()

    @Test
    fun `sendMessage should handle successful chat response`() = runTest {
        val useChat = UseChat(mockModel, mockOptions)
        
        coEvery {
            mockModel.chat(any(), any(), any(), any())
        } returns flowOf("Hello", " world", "!")

        val response = useChat.sendMessage("Hi").toList()
        
        assertEquals(listOf("Hello", " world", "!"), response)
        
        val messages = useChat.getMessages()
        assertEquals(2, messages.size)
        assertEquals("Hi", messages[0].content)
        assertEquals("user", messages[0].role)
        assertEquals("Hello world!", messages[1].content)
        assertEquals("assistant", messages[1].role)
    }

    @Test
    fun `sendMessage should handle system prompt`() = runTest {
        val useChat = UseChat(mockModel, mockOptions)
        
        coEvery {
            mockModel.chat(any(), any(), any(), any())
        } returns flowOf("Response")

        useChat.sendMessage("Hi", systemPrompt = "Be helpful").toList()
        
        val messages = useChat.getMessages()
        assertEquals(3, messages.size)
        assertEquals("Be helpful", messages[0].content)
        assertEquals("system", messages[0].role)
        assertEquals("Hi", messages[1].content)
        assertEquals("user", messages[1].role)
        assertEquals("Response", messages[2].content)
        assertEquals("assistant", messages[2].role)
    }

    @Test
    fun `sendMessage should handle error response`() = runTest {
        val useChat = UseChat(mockModel, mockOptions)
        
        coEvery {
            mockModel.chat(any(), any(), any(), any())
        } throws AIError.ProviderError(500, "Server error", "test")

        assertThrows<AIError.ProviderError> {
            useChat.sendMessage("Hi").toList()
        }
        
        val messages = useChat.getMessages()
        assertEquals(1, messages.size)
        assertEquals("Hi", messages[0].content)
        assertEquals("user", messages[0].role)
    }

    @Test
    fun `abort should cancel ongoing request`() = runTest {
        val useChat = UseChat(mockModel, mockOptions)
        
        coEvery {
            mockModel.chat(any(), any(), any(), any())
        } coAnswers {
            val signal = arg<AbortSignal>(3)
            if (signal?.isAborted == true) {
                throw CancellationException("Aborted")
            }
            flowOf("Response")
        }

        val flow = useChat.sendMessage("Hi")
        useChat.abort()
        
        assertThrows<CancellationException> {
            flow.toList()
        }
    }

    @Test
    fun `clearMessages should reset chat history`() = runTest {
        val useChat = UseChat(mockModel, mockOptions)
        
        coEvery {
            mockModel.chat(any(), any(), any(), any())
        } returns flowOf("Response")

        useChat.sendMessage("Hi").toList()
        assertTrue(useChat.getMessages().isNotEmpty())
        
        useChat.clearMessages()
        assertTrue(useChat.getMessages().isEmpty())
    }
}