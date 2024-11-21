package dev.vercel.ai.integration

import dev.vercel.ai.providers.OpenAIProvider
import dev.vercel.ai.providers.AnthropicProvider
import dev.vercel.ai.providers.HuggingFaceProvider
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.util.Properties

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseIntegrationTest {
    protected lateinit var openAIProvider: OpenAIProvider
    protected lateinit var anthropicProvider: AnthropicProvider
    protected lateinit var huggingFaceProvider: HuggingFaceProvider
    
    protected val properties = Properties()
    
    @BeforeAll
    fun setup() {
        // Load test properties
        javaClass.classLoader.getResourceAsStream("integration-test.properties")?.use { stream ->
            properties.load(stream)
        }
        
        // Initialize providers with API keys from environment variables or properties
        openAIProvider = OpenAIProvider(
            apiKey = System.getenv("OPENAI_API_KEY") ?: properties.getProperty("openai.api.key"),
            baseUrl = properties.getProperty("openai.base.url", "https://api.openai.com/v1")
        )
        
        anthropicProvider = AnthropicProvider(
            apiKey = System.getenv("ANTHROPIC_API_KEY") ?: properties.getProperty("anthropic.api.key"),
            baseUrl = properties.getProperty("anthropic.base.url", "https://api.anthropic.com/v1")
        )
        
        huggingFaceProvider = HuggingFaceProvider(
            apiKey = System.getenv("HUGGINGFACE_API_KEY") ?: properties.getProperty("huggingface.api.key"),
            baseUrl = properties.getProperty("huggingface.base.url", "https://api-inference.huggingface.co/models")
        )
    }
    
    protected open fun validateStreamResponse(response: String) {
        require(response.isNotBlank()) { "Response should not be empty" }
    }
}