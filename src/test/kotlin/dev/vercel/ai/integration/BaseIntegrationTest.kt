package dev.vercel.ai.integration

import dev.vercel.ai.providers.OpenAIProvider
import dev.vercel.ai.providers.AnthropicProvider
import dev.vercel.ai.providers.HuggingFaceProvider
import dev.vercel.ai.embeddings.OpenAIEmbeddingsProvider
import dev.vercel.ai.embeddings.AzureOpenAIEmbeddingsProvider
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.util.Properties

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseIntegrationTest {
    protected lateinit var openAIProvider: OpenAIProvider
    protected lateinit var anthropicProvider: AnthropicProvider
    protected lateinit var huggingFaceProvider: HuggingFaceProvider
    protected lateinit var openAIEmbeddingsProvider: OpenAIEmbeddingsProvider
    protected lateinit var azureOpenAIEmbeddingsProvider: AzureOpenAIEmbeddingsProvider
    
    protected val properties = Properties()
    protected val httpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
    
    @BeforeAll
    fun setup() {
        // Load test properties
        javaClass.classLoader.getResourceAsStream("integration-test.properties")?.use { stream ->
            properties.load(stream)
        } ?: run {
            println("Warning: integration-test.properties not found. Using environment variables only.")
        }
        
        // Initialize providers with API keys from environment variables or properties
        val openAIKey = System.getenv("OPENAI_API_KEY") ?: properties.getProperty("openai.api.key")
        requireNotNull(openAIKey) { "OpenAI API key not found in environment or properties" }
        
        openAIProvider = OpenAIProvider(
            apiKey = openAIKey,
            baseUrl = properties.getProperty("openai.base.url", "https://api.openai.com/v1")
        )
        
        anthropicProvider = AnthropicProvider(
            apiKey = System.getenv("ANTHROPIC_API_KEY") ?: properties.getProperty("anthropic.api.key"),
            baseUrl = properties.getProperty("anthropic.base.url", "https://api.anthropic.com/v1"),
            httpClient = httpClient
        )
        
        huggingFaceProvider = HuggingFaceProvider(
            apiKey = System.getenv("HUGGINGFACE_API_KEY") ?: properties.getProperty("huggingface.api.key"),
            baseUrl = properties.getProperty("huggingface.base.url", "https://api-inference.huggingface.co/models"),
            httpClient = httpClient
        )

        openAIEmbeddingsProvider = OpenAIEmbeddingsProvider(
            apiKey = System.getenv("OPENAI_API_KEY") ?: properties.getProperty("openai.api.key"),
            baseUrl = properties.getProperty("openai.base.url", "https://api.openai.com/v1")
        )

        azureOpenAIEmbeddingsProvider = AzureOpenAIEmbeddingsProvider(
            apiKey = System.getenv("AZURE_OPENAI_API_KEY") ?: properties.getProperty("azure.openai.api.key"),
            baseUrl = properties.getProperty("azure.openai.base.url")
        )
    }
    
    protected open fun validateStreamResponse(response: String) {
        require(response.isNotBlank()) { "Response should not be empty" }
    }
}