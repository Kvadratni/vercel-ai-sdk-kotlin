package dev.vercel.ai.tools

/**
 * Interface representing a callable function tool
 */
interface CallableTool {
    /** The tool's definition */
    val definition: ToolDefinition
    
    /** Execute the tool with given arguments */
    suspend fun call(arguments: Map<String, Any>): Any
}

/**
 * Represents a parameter in a tool definition
 */
data class ToolParameter(
    val type: String,
    val description: String? = null,
    val required: Boolean = false,
    val enum: List<String>? = null,
    val properties: Map<String, ToolParameter>? = null,
    val items: ToolParameter? = null
)

/**
 * Represents a function definition within a tool
 */
data class ToolFunction(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

/**
 * Represents a tool that can be called by the AI model
 */
data class ToolDefinition(
    val type: String = "function",
    val function: ToolFunction
)

/**
 * Represents a function call made by the AI model
 */
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

/**
 * Represents a function call within a tool call
 */
data class ToolCallFunction(
    val name: String,
    val arguments: String
)

/**
 * Represents the output from executing a tool call
 */
data class ToolOutput(
    val toolCallId: String,
    val output: String
)

/**
 * Represents a tool call response from the model
 */
sealed class ToolCallResponse {
    /** Tool call requires action */
    data class RequiresAction(
        val toolCalls: List<ToolCall>
    ) : ToolCallResponse()
    
    /** Tool call completed successfully */
    data class Success(
        val output: String
    ) : ToolCallResponse()
    
    /** Tool call failed */
    data class Failed(
        val error: String
    ) : ToolCallResponse()
}