package dev.vercel.ai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import java.util.UUID

/**
 * Base interface for all message types in the Vercel AI SDK
 */
interface Message {
    val content: String
    val role: String
    val id: String
    val createdAt: Instant
}

/**
 * Chat message data class for chat-based interactions
 *
 * @property content The content of the message
 * @property role The role of the message sender (e.g., "user", "assistant", "system")
 * @property id Unique identifier for the message
 * @property createdAt Timestamp when the message was created
 * @property name Optional name of the message sender
 * @property functionCall Optional function call details if this message represents a function call
 * @property toolCalls Optional list of tool calls made by the message
 * @property attachments Optional list of file attachments
 * @property annotations Optional list of custom annotations
 */
@Serializable
data class ChatMessage(
    override val content: String,
    override val role: String,
    override val id: String = UUID.randomUUID().toString(),
    @SerialName("created_at")
    override val createdAt: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
    val name: String? = null,
    @SerialName("function_call")
    val functionCall: FunctionCall? = null,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCall>? = null,
    val attachments: List<Attachment>? = null,
    val annotations: List<String>? = null
) : Message

/**
 * Function call details for messages that represent function calls
 *
 * @property name The name of the function to call
 * @property arguments The arguments to pass to the function
 */
@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String
)

/**
 * Tool call representation for function calling
 */
@Serializable
sealed class ToolCall {
    abstract val id: String
    abstract val type: String
    
    @Serializable
    @SerialName("function")
    data class Function(
        override val id: String,
        override val type: String = "function",
        val name: String,
        val arguments: JsonObject
    ) : ToolCall()
}

/**
 * Represents the result of a tool call
 */
@Serializable
data class ToolCallResult(
    val toolCallId: String,
    val output: String
)

/**
 * File attachment representation
 */
@Serializable
data class Attachment(
    val name: String,
    val type: String,
    @Contextual
    val content: ByteArray? = null,
    val url: String? = null,
    val dataUrl: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Attachment) return false
        return name == other.name && 
               type == other.type &&
               content.contentEquals(other.content ?: byteArrayOf()) &&
               url == other.url &&
               dataUrl == other.dataUrl
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (content?.contentHashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (dataUrl?.hashCode() ?: 0)
        return result
    }
}

/**
 * Completion message data class for single-turn completion interactions
 *
 * @property content The content of the message
 * @property role The role of the message sender (always "user" for completions)
 * @property id Unique identifier for the message
 * @property createdAt Timestamp when the message was created
 * @property attachments Optional list of file attachments
 * @property annotations Optional list of custom annotations
 */
@Serializable
data class CompletionMessage(
    override val content: String,
    override val role: String = "user",
    override val id: String = UUID.randomUUID().toString(),
    @SerialName("created_at")
    override val createdAt: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
    val attachments: List<Attachment>? = null,
    val annotations: List<String>? = null
) : Message