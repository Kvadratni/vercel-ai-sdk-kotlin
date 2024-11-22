package dev.vercel.ai.options

/**
 * Configuration options for Azure OpenAI API calls.
 *
 * @property deploymentId The deployment ID for the Azure OpenAI model
 * @property endpoint The Azure OpenAI endpoint URL
 * @property apiVersion The Azure OpenAI API version to use
 * @property parameters Model-specific parameters for the request
 */
data class AzureOpenAIOptions(
    val deploymentId: String,
    val endpoint: String,
    val apiVersion: String = "2024-02-15-preview",
    val parameters: ModelParameters.Chat,
    override val model: String = "gpt-4",
    override val temperature: Double? = parameters.temperature,
    override val maxTokens: Int? = parameters.maxTokens,
    override val stopSequences: List<String>? = parameters.stopSequences,
    override val stream: Boolean = true
) : ProviderOptions {
    override fun toMap(): Map<String, Any> = buildMap {
        put("model", model)
        put("temperature", temperature ?: 0.7)
        put("max_tokens", maxTokens ?: 1000)
        stopSequences?.let { put("stop", it) }
        put("stream", stream)
    }

    companion object {
        /**
         * Creates options for GPT-4 model
         */
        fun gpt4(
            deploymentId: String,
            endpoint: String,
            parameters: ModelParameters.Chat = ModelParameters.Chat()
        ) = AzureOpenAIOptions(
            deploymentId = deploymentId,
            endpoint = endpoint,
            parameters = parameters,
            model = "gpt-4"
        )

        /**
         * Creates options for GPT-3.5 Turbo model
         */
        fun gpt35Turbo(
            deploymentId: String,
            endpoint: String,
            parameters: ModelParameters.Chat = ModelParameters.Chat()
        ) = AzureOpenAIOptions(
            deploymentId = deploymentId,
            endpoint = endpoint,
            parameters = parameters,
            model = "gpt-35-turbo"
        )
    }

    init {
        require(deploymentId.isNotBlank()) { "deploymentId must not be blank" }
        require(endpoint.isNotBlank()) { "endpoint must not be blank" }
        require(apiVersion.isNotBlank()) { "apiVersion must not be blank" }
        temperature?.let { require(it in 0.0..1.0) { "temperature must be between 0 and 1" } }
        maxTokens?.let { require(it > 0) { "maxTokens must be positive" } }
    }
}