package dev.vercel.ai.options

/**
 * Options specific to the Anthropic provider
 */
class AnthropicOptions(
    override val model: String,
    override val temperature: Double = 0.7,
    override val maxTokens: Int = 1000,
    override val stopSequences: List<String>? = null,
    val topP: Double = 1.0,
    val topK: Int? = null,
    val metadata: Map<String, String>? = null,
    override val stream: Boolean = true
) : ProviderOptions {

    fun validate() {
        require(temperature in 0.0..1.0) { "Temperature must be between 0 and 1" }
        require(maxTokens > 0) { "Max tokens must be positive" }
        require(topP in 0.0..1.0) { "Top P must be between 0 and 1" }
        topK?.let { require(it > 0) { "Top K must be positive" } }
    }

    override fun toMap(): Map<String, Any> = buildMap {
        put("model", model)
        put("temperature", temperature)
        put("max_tokens_to_sample", maxTokens)
        stopSequences?.let { put("stop_sequences", it) }
        put("top_p", topP)
        topK?.let { put("top_k", it) }
        metadata?.let { put("metadata", it) }
        put("stream", stream)
    }

    companion object {
        // Common model configurations
        fun claude3Opus(params: ModelParameters.Chat = ModelParameters.Chat()) = AnthropicOptions(
            model = "claude-3-opus-20240229",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP
        )

        fun claude3Sonnet(params: ModelParameters.Chat = ModelParameters.Chat()) = AnthropicOptions(
            model = "claude-3-sonnet-20240229",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP
        )

        fun claude3Haiku(params: ModelParameters.Chat = ModelParameters.Chat()) = AnthropicOptions(
            model = "claude-3-haiku-20240307",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP
        )

        fun claude35Sonnet(params: ModelParameters.Chat = ModelParameters.Chat()) = AnthropicOptions(
            model = "claude-3-5-sonnet-20241022",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP
        )

        fun claude35Haiku(params: ModelParameters.Chat = ModelParameters.Chat()) = AnthropicOptions(
            model = "claude-3-5-haiku-20241022",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP
        )

        // Legacy models (deprecated)
        fun claude21(params: ModelParameters.Chat = ModelParameters.Chat()) = AnthropicOptions(
            model = "claude-2.1",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP
        )

        fun claude2(params: ModelParameters.Chat = ModelParameters.Chat()) = AnthropicOptions(
            model = "claude-2.0",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP
        )

        fun claude1(params: ModelParameters.Chat = ModelParameters.Chat()) = AnthropicOptions(
            model = "claude-1",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP
        )

        fun claudeInstant12(params: ModelParameters.Chat = ModelParameters.Chat()) = AnthropicOptions(
            model = "claude-instant-1.2",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP
        )
    }
}