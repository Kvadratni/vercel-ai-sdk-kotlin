package dev.vercel.ai.options

/**
 * Base interface for provider-specific options
 */
interface ProviderOptions {
    /** The model to use */
    val model: String
    /** Temperature controls randomness in the output (0-1) */
    val temperature: Double?
    /** Maximum number of tokens to generate */
    val maxTokens: Int?
    /** Stop sequences that will end generation */
    val stopSequences: List<String>?
    /** Whether to stream the response */
    val stream: Boolean
    
    /** Convert options to a Map for API requests */
    fun toMap(): Map<String, Any>
}

/**
 * Common model parameters shared across providers
 */
sealed class ModelParameters {
    /** Parameters for text generation models */
    data class TextGeneration(
        val temperature: Double = 0.7,
        val maxTokens: Int = 1000,
        val topP: Double = 1.0,
        val frequencyPenalty: Double = 0.0,
        val presencePenalty: Double = 0.0,
        val stopSequences: List<String> = emptyList()
    ) : ModelParameters()
    
    /** Parameters for chat models */
    data class Chat(
        val temperature: Double = 0.7,
        val maxTokens: Int = 1000,
        val topP: Double = 1.0,
        val frequencyPenalty: Double = 0.0,
        val presencePenalty: Double = 0.0,
        val stopSequences: List<String> = emptyList()
    ) : ModelParameters()
}

/**
 * Base class for model-specific options
 */
abstract class ModelOptions {
    abstract val parameters: ModelParameters
    abstract fun validate()
    
    protected fun validateTemperature(temp: Double) {
        require(temp in 0.0..1.0) { "Temperature must be between 0 and 1" }
    }
    
    protected fun validateMaxTokens(tokens: Int) {
        require(tokens > 0) { "Max tokens must be positive" }
    }
}