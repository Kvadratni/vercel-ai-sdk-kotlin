package dev.vercel.ai.options

/**
 * Options for configuring the Cohere provider.
 *
 * @property apiKey The Cohere API key
 * @property model The model to use for chat completion (e.g. "command", "command-light", "command-nightly")
 * @property temperature The sampling temperature to use between 0 and 5. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic. Default: 0.7
 * @property maxTokens The maximum number of tokens to generate in the completion. Default: 256
 * @property stream Whether to stream the response or not. Default: true
 */
data class CohereOptions(
    val apiKey: String,
    override val model: String = "command",
    override val temperature: Double = 0.7,
    override val maxTokens: Int = 256,
    override val stream: Boolean = true,
    override val stopSequences: List<String>? = null
) : ProviderOptions {
    override fun toMap(): Map<String, Any> = mapOf(
        "model" to model,
        "temperature" to temperature,
        "maxTokens" to maxTokens,
        "stream" to stream
    ).let { map ->
        stopSequences?.let { map + ("stopSequences" to it) } ?: map
    }
}