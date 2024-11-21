package dev.vercel.ai.options

/**
 * OpenAI-specific options
 */
data class OpenAIOptions(
    override val model: String,
    override val temperature: Double? = 0.7,
    override val maxTokens: Int? = 1000,
    override val stopSequences: List<String>? = null,
    override val stream: Boolean = true,
    val topP: Double? = 1.0,
    val n: Int? = 1,
    val frequencyPenalty: Double? = 0.0,
    val presencePenalty: Double? = 0.0,
    val logitBias: Map<String, Double>? = null,
    val user: String? = null
) : ProviderOptions {
    override fun toMap(): Map<String, Any> = buildMap {
        put("model", model)
        put("stream", stream)
        temperature?.let { put("temperature", it) }
        maxTokens?.let { put("max_tokens", it) }
        stopSequences?.let { put("stop", it) }
        topP?.let { put("top_p", it) }
        n?.let { put("n", it) }
        frequencyPenalty?.let { put("frequency_penalty", it) }
        presencePenalty?.let { put("presence_penalty", it) }
        logitBias?.let { put("logit_bias", it) }
        user?.let { put("user", it) }
    }
    
    /**
     * Validates the options
     * @throws IllegalArgumentException if any parameters are invalid
     */
    fun validate() {
        temperature?.let { require(it in 0.0..1.0) { "Temperature must be between 0 and 1" } }
        maxTokens?.let { require(it > 0) { "Max tokens must be positive" } }
        topP?.let { require(it in 0.0..1.0) { "Top P must be between 0 and 1" } }
        n?.let { require(it > 0) { "N must be positive" } }
        frequencyPenalty?.let { require(it >= -2.0 && it <= 2.0) { "Frequency penalty must be between -2 and 2" } }
        presencePenalty?.let { require(it >= -2.0 && it <= 2.0) { "Presence penalty must be between -2 and 2" } }
    }
    
    companion object {
        // Common model configurations
        fun gpt4(params: ModelParameters.Chat = ModelParameters.Chat()) = OpenAIOptions(
            model = "gpt-4",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP,
            frequencyPenalty = params.frequencyPenalty,
            presencePenalty = params.presencePenalty
        )

        fun gpt4Vision(params: ModelParameters.Chat = ModelParameters.Chat()) = OpenAIOptions(
            model = "gpt-4-vision-preview",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP,
            frequencyPenalty = params.frequencyPenalty,
            presencePenalty = params.presencePenalty
        )
        
        fun gpt4Turbo(params: ModelParameters.Chat = ModelParameters.Chat()) = OpenAIOptions(
            model = "gpt-4-1106-preview",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP,
            frequencyPenalty = params.frequencyPenalty,
            presencePenalty = params.presencePenalty
        )
        
        fun gpt35Turbo(params: ModelParameters.Chat = ModelParameters.Chat()) = OpenAIOptions(
            model = "gpt-3.5-turbo",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP,
            frequencyPenalty = params.frequencyPenalty,
            presencePenalty = params.presencePenalty
        )

        fun gpt35Turbo16k(params: ModelParameters.Chat = ModelParameters.Chat()) = OpenAIOptions(
            model = "gpt-3.5-turbo-16k",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP,
            frequencyPenalty = params.frequencyPenalty,
            presencePenalty = params.presencePenalty
        )

        fun davinci(params: ModelParameters.TextGeneration = ModelParameters.TextGeneration()) = OpenAIOptions(
            model = "text-davinci-003",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP,
            frequencyPenalty = params.frequencyPenalty,
            presencePenalty = params.presencePenalty
        )
    }
}