package dev.vercel.ai.hooks

import dev.vercel.ai.AIModel
import dev.vercel.ai.common.AbortController
import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.options.ProviderOptions
import io.mockk.coEvery
import io.mockk.every
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
import kotlin.test.assertTrue

class UseChatTest {
    private val mockModel = mockk<AIModel>(relaxed = true)
    private val mockOptions = mockk<ProviderOptions>(relaxed = true)

    @Test
    fun `sendMessage should handle successful chat response`() = runTest {
        val useChat = UseChat(mockModel, mockOptions)
        val messagesSlot = slot<List<ChatMessage>>()
        
        coEvery {
            mockModel.chat(capture(messagesSlot), any(), any(), any())
        } returns flowOf("Hello")

        val response = useChat.sendMessage("Hi").toList()
        
        assertEquals(listOf("Hello"), response)
        assertTrue(messagesSlot.captured.any { it.content == "Hi" && it.role == "user" })
        assertTrue(useChat.getMessages().any { it.content == "Hello" && it.role == "assistant" })
    }

    @Test
    fun `sendMessage should handle error response`() = runTest {
        val useChat = UseChat(mockModel, mockOptions)
        
        coEvery {
            mockModel.chat(any<List<ChatMessage>>(), any(), any(), any())
        } throws AIError.ProviderError(500, "Server error", "test")

        assertThrows<AIError.ProviderError> {
            useChat.sendMessage("Hi").toList()
        }
        
        assertTrue(useChat.getMessages().any { it.content == "Hi" && it.role == "user" })
        assertTrue(useChat.getMessages().none { it.role == "assistant" })
    }

    @Test
    fun `abort should cancel ongoing request`() = runTest {
        val useChat = UseChat(mockModel, mockOptions)
        val signal = slot<AbortSignal>()
        
        coEvery {
            mockModel.chat(any<List<ChatMessage>>(), any(), any(), capture(signal))
        } coAnswers {
            flow {
                emit("Hello")
                signal.captured.throwIfAborted()
                emit("World")
            }
        }

        val flow = useChat.sendMessage("Hi")
        delay(50) // Give time for the flow to start
        useChat.abort()

        assertThrows<CancellationException> {
            flow.toList()
        }
    }

    @Test
    fun `sendMessage should handle system prompt`() = runTest {
        val useChat = UseChat(mockModel, mockOptions)
        val messagesSlot = slot<List<ChatMessage>>()
        val systemPrompt = "You are a helpful assistant."
        
        coEvery {
            mockModel.chat(capture(messagesSlot), any(), any(), any())
        } returns flowOf("Hello")

        val response = useChat.sendMessage("Hi", systemPrompt).toList()
        
        assertEquals(listOf("Hello"), response)
        assertTrue(messagesSlot.captured.any { it.content == systemPrompt && it.role == "system" })
        assertTrue(messagesSlot.captured.any { it.content == "Hi" && it.role == "user" })
        assertTrue(useChat.getMessages().any { it.content == "Hello" && it.role == "assistant" })
    }

    @Test
    fun `clearMessages should reset chat history`() = runTest {
        val useChat = UseChat(mockModel, mockOptions)
        val messagesSlot = slot<List<ChatMessage>>()
        
        coEvery {
            mockModel.chat(capture(messagesSlot), any(), any(), any())
        } returns flowOf("Hello")

        useChat.sendMessage("Hi").toList()
        assertTrue(useChat.getMessages().size >= 2) // At least user and assistant messages
        
        useChat.clearMessages()
        assertEquals(0, useChat.getMessages().size)
    }
}