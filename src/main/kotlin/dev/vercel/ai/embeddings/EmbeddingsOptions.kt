package dev.vercel.ai.embeddings

/**
 * Base interface for embedding provider options
 */
interface EmbeddingsOptions {
    /** The model to use for embeddings */
    val model: String
    /** Convert options to a Map for API requests */
    fun toMap(): Map<String, Any>
}

/**
 * OpenAI-specific embedding options
 */
data class OpenAIEmbeddingsOptions(
    override val model: String = "text-embedding-ada-002",
    val user: String? = null,
    val dimensions: Int? = null
) : EmbeddingsOptions {
    override fun toMap(): Map<String, Any> = buildMap {
        put("model", model)
        user?.let { put("user", it) }
        dimensions?.let { put("dimensions", it) }
    }

    /**
     * Validates the options
     * @throws IllegalArgumentException if any parameters are invalid
     */
    fun validate() {
        dimensions?.let { require(it > 0) { "Dimensions must be positive" } }
    }

    companion object {
        // Common model configurations
        fun ada002(user: String? = null, dimensions: Int? = null) = OpenAIEmbeddingsOptions(
            model = "text-embedding-ada-002",
            user = user,
            dimensions = dimensions
        )
    }
}

/**
 * Azure OpenAI-specific embedding options
 */
data class AzureOpenAIEmbeddingsOptions(
    override val model: String,
    val apiVersion: String = "2023-05-15",
    val user: String? = null,
    val dimensions: Int? = null
) : EmbeddingsOptions {
    override fun toMap(): Map<String, Any> = buildMap {
        put("model", model)
        put("api-version", apiVersion)
        user?.let { put("user", it) }
        dimensions?.let { put("dimensions", it) }
    }

    /**
     * Validates the options
     * @throws IllegalArgumentException if any parameters are invalid
     */
    fun validate() {
        dimensions?.let { require(it > 0) { "Dimensions must be positive" } }
        require(apiVersion.isNotBlank()) { "API version must not be blank" }
    }
}