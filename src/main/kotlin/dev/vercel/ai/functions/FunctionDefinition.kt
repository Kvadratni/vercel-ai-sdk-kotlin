package dev.vercel.ai.functions

/**
 * Represents a parameter in a function definition
 */
data class FunctionParameter(
    val name: String,
    val type: String,
    val description: String? = null,
    val required: Boolean = true,
    val enum: List<String>? = null
)

/**
 * Represents a function that can be called by the AI model
 */
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: List<FunctionParameter>
)

/**
 * Represents a function call made by the AI model
 */
data class FunctionCall(
    val name: String,
    val arguments: Map<String, Any>
)

/**
 * Represents a function response returned to the AI model
 */
data class FunctionResponse(
    val name: String,
    val result: Any
)

/**
 * Interface for implementing callable functions
 */
interface CallableFunction {
    val definition: FunctionDefinition
    suspend fun call(arguments: Map<String, Any>): Any
}