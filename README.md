# Vercel AI SDK for Kotlin

A Kotlin implementation of the [Vercel AI SDK](https://github.com/vercel/ai) for building AI-powered streaming text and chat UIs.

This is a port of the official JavaScript/TypeScript SDK to Kotlin, providing the same functionality with idiomatic Kotlin APIs.

## Features

- 🚀 Streaming support for OpenAI, Anthropic, and HuggingFace
- ⚡️ Optimized for edge runtime environments
- 🔥 Native Kotlin coroutines and Flow support
- 🛠️ Easy to extend with custom providers
- 🔧 Tool calling support for OpenAI models

## Installation

Add the following dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("dev.vercel:ai:0.1.0")
}
```

## Usage

### OpenAI Example

```kotlin
import dev.vercel.ai.providers.OpenAIProvider
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val openai = OpenAIProvider(apiKey = "your-api-key")
    
    // Text completion with type-safe options
    openai.complete(
        prompt = "The capital of France is",
        options = OpenAIOptions.gpt35Turbo(
            ModelParameters.Chat(
                temperature = 0.7,
                maxTokens = 100
            )
        )
    ).collect { chunk ->
        print(chunk)
    }
    
    // Chat completion with tool calling
    val searchEmails = object : CallableTool {
        override val definition = ToolDefinition(
            function = ToolFunction(
                name = "search_emails",
                description = "Search through email messages",
                parameters = mapOf(
                    "query" to ToolParameterDefinition(
                        type = "string",
                        description = "The search query",
                        required = true
                    ),
                    "has_attachments" to ToolParameterDefinition(
                        type = "boolean",
                        description = "Filter for emails with attachments",
                        required = false
                    )
                )
            )
        )
        
        override suspend fun call(arguments: Map<String, Any>): Any {
            val query = arguments["query"] as String
            val hasAttachments = arguments["has_attachments"] as? Boolean ?: false
            // Implement email search logic
            return emptyList<Map<String, String>>()
        }
    }
    
    openai.chat(
        messages = listOf(
            ChatMessage(role = "system", content = "You are a helpful assistant."),
            ChatMessage(role = "user", content = "Find my recent investor updates")
        ),
        options = OpenAIOptions.gpt4(),
        functions = listOf(searchEmails)
    ).collect { chunk ->
        print(chunk)
    }
}
```

### Anthropic Example

```kotlin
import dev.vercel.ai.providers.AnthropicProvider

val anthropic = AnthropicProvider(apiKey = "your-api-key")

anthropic.chat(
    messages = listOf(
        ChatMessage(role = "user", content = "What is the meaning of life?")
    ),
    options = AnthropicOptions.claude2()
).collect { chunk ->
    print(chunk)
}
```

### HuggingFace Example

```kotlin
import dev.vercel.ai.providers.HuggingFaceProvider

val huggingface = HuggingFaceProvider(apiKey = "your-api-key")

huggingface.complete(
    prompt = "Once upon a time",
    options = HuggingFaceOptions.gpt2(
        ModelParameters.TextGeneration(
            maxTokens = 100
        )
    )
).collect { chunk ->
    print(chunk)
}
```

## Tool Calling

The SDK supports both function calling and tool calling with OpenAI models. Here's how to use tools:

```kotlin
val searchEmails = object : CallableTool {
    override val definition = ToolDefinition(
        function = ToolFunction(
            name = "search_emails",
            description = "Search through email messages",
            parameters = mapOf(
                "query" to ToolParameterDefinition(
                    type = "string",
                    description = "The search query",
                    required = true
                ),
                "has_attachments" to ToolParameterDefinition(
                    type = "boolean",
                    description = "Filter for emails with attachments",
                    required = false
                )
            )
        )
    )
    
    override suspend fun call(arguments: Map<String, Any>): Any {
        val query = arguments["query"] as String
        val hasAttachments = arguments["has_attachments"] as? Boolean ?: false
        
        // Implement email search logic
        return listOf(
            mapOf(
                "id" to "1",
                "subject" to "Q1 Investor Update",
                "date" to "Apr 1, 2023"
            )
        )
    }
}

// Use the tool with OpenAI chat
val openai = OpenAIProvider(apiKey = "your-api-key")
openai.chat(
    messages = listOf(
        ChatMessage(role = "user", content = "Find my investor updates")
    ),
    options = OpenAIOptions.gpt4(),
    functions = listOf(searchEmails)
).collect { chunk ->
    print(chunk)
}
```

Tools can also be used with a handler for more complex interactions:

```kotlin
val handler = object : ToolCallHandler {
    override suspend fun handleToolCall(toolCall: ToolCall): ToolCallResponse {
        // Handle the tool call
        return when (toolCall.function.name) {
            "search_emails" -> {
                val args = toolCall.function.arguments
                val result = searchEmails.call(searchEmails.parseArguments(args))
                ToolCallResponse.Success(mapper.writeValueAsString(result))
            }
            else -> ToolCallResponse.Failed("Unknown tool")
        }
    }
    
    override suspend fun submitToolOutputs(
        threadId: String,
        runId: String,
        outputs: List<ToolOutput>
    ): ToolCallResponse {
        // Submit tool outputs back to the model
        return ToolCallResponse.Success("Outputs submitted")
    }
}
```

## Error Handling

The SDK provides comprehensive error handling through the `AIError` sealed class hierarchy:

```kotlin
try {
    openai.chat(
        messages = listOf(ChatMessage(role = "user", content = "Hello")),
        options = OpenAIOptions.gpt4()
    ).collect { chunk ->
        print(chunk)
    }
} catch (e: AIError) {
    when (e) {
        is AIError.ProviderError -> println("API Error: ${e.message}")
        is AIError.RateLimitError -> println("Rate limit exceeded, retry after ${e.retryAfter}ms")
        is AIError.StreamError -> println("Stream error: ${e.message}")
        else -> println("Other error: ${e.message}")
    }
}
```

The SDK also includes automatic retry functionality for certain types of errors:

```kotlin
val config = RetryConfig(
    maxRetries = 3,
    initialDelay = 1000,
    maxDelay = 10000,
    shouldRetry = { error -> 
        error is AIError.RateLimitError || 
        (error is AIError.ProviderError && error.statusCode in 500..599)
    }
)

// The SDK will automatically retry on rate limits and server errors
openai.chat(
    messages = listOf(ChatMessage(role = "user", content = "Hello")),
    options = OpenAIOptions.gpt4()
).collect { chunk ->
    print(chunk)
}
```

## Type-Safe Provider Options

Each provider has type-safe options with validation:

```kotlin
// OpenAI
val openaiOptions = OpenAIOptions.gpt4Vision(
    ModelParameters.Chat(
        temperature = 0.7,
        maxTokens = 1000,
        topP = 0.9,
        frequencyPenalty = 0.5,
        presencePenalty = 0.5
    )
)

// Anthropic
val anthropicOptions = AnthropicOptions.claude3Opus(
    ModelParameters.Chat(
        temperature = 0.7,
        maxTokens = 2000,
        stopSequences = listOf("##")
    )
)

// HuggingFace
val huggingfaceOptions = HuggingFaceOptions.gpt2(
    ModelParameters.TextGeneration(
        temperature = 0.8,
        maxTokens = 500,
        topP = 0.9,
        noRepeatNgramSize = 3
    )
)

// Options are validated at runtime
openaiOptions.validate() // Throws if invalid
```

## License

MIT License