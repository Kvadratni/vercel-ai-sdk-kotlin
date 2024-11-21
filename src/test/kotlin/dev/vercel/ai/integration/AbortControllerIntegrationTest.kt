package dev.vercel.ai.integration

import dev.vercel.ai.ChatMessage
import dev.vercel.ai.common.AbortController
import dev.vercel.ai.options.OpenAIOptions
import dev.vercel.ai.options.AnthropicOptions
import dev.vercel.ai.options.HuggingFaceOptions
import dev.vercel.ai.options.ModelParameters
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.assertTrue

class AbortControllerIntegrationTest : BaseIntegrationTest() {
    
    @Test
    @Timeout(30)
    fun `should abort OpenAI chat completion`() = runBlocking {
        val controller = AbortController()
        var wasAborted = false
        
        val messages = listOf(
            ChatMessage(role = "user", content = "Write a very long story about artificial intelligence")
        )
        
        val options = OpenAIOptions.gpt4(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 1000
            )
        )
        
        val job = launch {
            try {
                controller.signal.withScope {
                    openAIProvider.chat(
                        messages = messages,
                        options = options
                    ).toList()
                }
            } catch (e: CancellationException) {
                wasAborted = true
            }
        }
        
        delay(1000) // Wait for stream to start
        controller.abort()
        job.join()
        
        assertTrue(wasAborted, "Chat completion should be aborted")
    }
    
    @Test
    @Timeout(30)
    fun `should abort Anthropic chat completion`() = runBlocking {
        val controller = AbortController()
        var wasAborted = false
        
        val messages = listOf(
            ChatMessage(role = "user", content = "Write a very long story about space exploration")
        )
        
        val options = AnthropicOptions.claude3Opus(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 1000
            )
        )
        
        val job = launch {
            try {
                controller.signal.withScope {
                    anthropicProvider.chat(
                        messages = messages,
                        options = options
                    ).toList()
                }
            } catch (e: CancellationException) {
                wasAborted = true
            }
        }
        
        delay(1000) // Wait for stream to start
        controller.abort()
        job.join()
        
        assertTrue(wasAborted, "Chat completion should be aborted")
    }
    
    @Test
    @Timeout(30)
    fun `should abort HuggingFace completion`() = runBlocking {
        val controller = AbortController()
        var wasAborted = false
        
        val options = HuggingFaceOptions.gpt2(
            ModelParameters.TextGeneration(
                temperature = 0.7,
                maxTokens = 200
            )
        )
        
        val job = launch {
            try {
                controller.signal.withScope {
                    huggingFaceProvider.complete(
                        prompt = "Once upon a time in a galaxy far far away",
                        options = options
                    ).toList()
                }
            } catch (e: CancellationException) {
                wasAborted = true
            }
        }
        
        delay(1000) // Wait for stream to start
        controller.abort()
        job.join()
        
        assertTrue(wasAborted, "Text completion should be aborted")
    }
    
    @Test
    @Timeout(30)
    fun `should abort multiple providers simultaneously`() = runBlocking {
        val controller = AbortController()
        var openaiAborted = false
        var anthropicAborted = false
        var huggingfaceAborted = false
        
        val messages = listOf(
            ChatMessage(role = "user", content = "Write a story")
        )
        
        val jobs = listOf(
            launch {
                try {
                    controller.signal.withScope {
                        openAIProvider.chat(
                            messages = messages,
                            options = OpenAIOptions.gpt4()
                        ).toList()
                    }
                } catch (e: CancellationException) {
                    openaiAborted = true
                }
            },
            launch {
                try {
                    controller.signal.withScope {
                        anthropicProvider.chat(
                            messages = messages,
                            options = AnthropicOptions.claude3Opus()
                        ).toList()
                    }
                } catch (e: CancellationException) {
                    anthropicAborted = true
                }
            },
            launch {
                try {
                    controller.signal.withScope {
                        huggingFaceProvider.complete(
                            prompt = "Once upon a time",
                            options = HuggingFaceOptions.gpt2()
                        ).toList()
                    }
                } catch (e: CancellationException) {
                    huggingfaceAborted = true
                }
            }
        )
        
        delay(1000) // Wait for streams to start
        controller.abort()
        jobs.forEach { it.join() }
        
        assertTrue(openaiAborted, "OpenAI completion should be aborted")
        assertTrue(anthropicAborted, "Anthropic completion should be aborted")
        assertTrue(huggingfaceAborted, "HuggingFace completion should be aborted")
    }
}