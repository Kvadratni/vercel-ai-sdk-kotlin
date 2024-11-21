package dev.vercel.ai.options

/**
 * HuggingFace-specific options
 */
data class HuggingFaceOptions(
    override val model: String,
    override val temperature: Double? = 0.7,
    override val maxTokens: Int? = 1000,
    override val stopSequences: List<String>? = null,
    override val stream: Boolean = true,
    val topK: Int? = null,
    val topP: Double? = null,
    val doSample: Boolean = true,
    val numBeams: Int? = null,
    val noRepeatNgramSize: Int? = null,
    val earlyStopping: Boolean? = null
) : ProviderOptions {
    override fun toMap(): Map<String, Any> = buildMap {
        put("model", model)
        put("stream", stream)
        temperature?.let { put("temperature", it) }
        maxTokens?.let { put("max_new_tokens", it) }
        stopSequences?.let { put("stop_sequences", it) }
        topK?.let { put("top_k", it) }
        topP?.let { put("top_p", it) }
        put("do_sample", doSample)
        numBeams?.let { put("num_beams", it) }
        noRepeatNgramSize?.let { put("no_repeat_ngram_size", it) }
        earlyStopping?.let { put("early_stopping", it) }
    }
    
    /**
     * Validates the options
     * @throws IllegalArgumentException if any parameters are invalid
     */
    fun validate() {
        temperature?.let { require(it in 0.0..1.0) { "Temperature must be between 0 and 1" } }
        maxTokens?.let { require(it > 0) { "Max tokens must be positive" } }
        topK?.let { require(it > 0) { "Top K must be positive" } }
        topP?.let { require(it in 0.0..1.0) { "Top P must be between 0 and 1" } }
        numBeams?.let { require(it > 0) { "Number of beams must be positive" } }
        noRepeatNgramSize?.let { require(it >= 0) { "No repeat ngram size must be non-negative" } }
    }
    
    companion object {
        // Common model configurations
        fun gpt2(params: ModelParameters.TextGeneration = ModelParameters.TextGeneration()) = HuggingFaceOptions(
            model = "gpt2",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP
        )
        
        fun blenderbot(params: ModelParameters.Chat = ModelParameters.Chat()) = HuggingFaceOptions(
            model = "facebook/blenderbot-400M-distill",
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            stopSequences = params.stopSequences,
            topP = params.topP
        )
    }
}