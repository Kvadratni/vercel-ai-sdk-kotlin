# Migrating from TypeScript to Kotlin SDK

This guide helps developers migrate from the Vercel AI TypeScript SDK to the Kotlin SDK.

## Key Differences

1. **Streaming API**
   - TypeScript: Uses ReadableStream/AsyncIterator
   - Kotlin: Uses Kotlin Flow
   ```kotlin
   // TypeScript
   for await (const chunk of stream) {
     console.log(chunk)
   }
   
   // Kotlin
   flow.collect { chunk ->
     println(chunk)
   }
   ```

2. **Tool Calling**
   - TypeScript: Uses function declarations and tool definitions
   - Kotlin: Uses CallableTool interface and type-safe definitions
   ```kotlin
   // TypeScript
   const functions = [{
     name: "searchEmails",
     description: "Search emails",
     parameters: {
       type: "object",
       properties: {
         query: { type: "string" }
       }
     }
   }]
   
   // Kotlin
   val functions = listOf(
     object : CallableFunction {
       override val definition = FunctionDefinition(
         name = "searchEmails",
         description = "Search emails",
         parameters = listOf(
           FunctionParameter(
             name = "query",
             type = "string"
           )
         )
       )
       
       override suspend fun call(arguments: Map<String, Any>): Any {
         // Implementation
       }
     }
   )
   ```

3. **Provider Options**
   - TypeScript: Uses plain objects
   - Kotlin: Uses type-safe data classes
   ```kotlin
   // TypeScript
   const options = {
     model: "gpt-4",
     temperature: 0.7,
     maxTokens: 1000
   }
   
   // Kotlin
   val options = OpenAIOptions.gpt4(
     ModelParameters.Chat(
       temperature = 0.7,
       maxTokens = 1000
     )
   )
   ```

4. **Error Handling**
   - TypeScript: Uses try/catch with basic error types
   - Kotlin: Uses sealed class hierarchy with comprehensive error types
   ```kotlin
   // TypeScript
   try {
     const result = await openai.chat(messages)
   } catch (error) {
     if (error.name === "RateLimitError") {
       // Handle rate limit
     }
   }
   
   // Kotlin
   try {
     openai.chat(messages).collect { chunk ->
       // Handle chunk
     }
   } catch (e: AIError) {
     when (e) {
       is AIError.RateLimitError -> {
         // Handle rate limit with retry info
         delay(e.retryAfter ?: 1000)
       }
       is AIError.ProviderError -> {
         // Handle provider error with status code
       }
     }
   }
   ```

5. **Abort Controller**
   - TypeScript: Uses Web API AbortController
   - Kotlin: Uses custom AbortController with Flow integration
   ```kotlin
   // TypeScript
   const controller = new AbortController()
   const response = await openai.chat({
     messages,
     signal: controller.signal
   })
   
   // Kotlin
   val controller = AbortController()
   controller.signal.withScope {
     openai.chat(messages).collect { chunk ->
       // Can call controller.abort() to cancel
     }
   }
   ```

## Common Migration Patterns

1. **Converting Async/Await to Coroutines**
   ```kotlin
   // TypeScript
   async function generateText() {
     const result = await openai.complete(prompt)
     return result
   }
   
   // Kotlin
   suspend fun generateText(): Flow<String> {
     return openai.complete(prompt)
   }
   ```

2. **Converting Event Handlers to Flow**
   ```kotlin
   // TypeScript
   response.onmessage = (event) => {
     console.log(event.data)
   }
   
   // Kotlin
   flow.collect { chunk ->
     println(chunk)
   }
   ```

3. **Converting Configuration**
   ```kotlin
   // TypeScript
   const config = {
     apiKey: process.env.OPENAI_API_KEY,
     baseURL: "https://api.openai.com/v1"
   }
   
   // Kotlin
   val config = OpenAIProvider(
     apiKey = System.getenv("OPENAI_API_KEY"),
     baseUrl = "https://api.openai.com/v1"
   )
   ```

## Best Practices

1. **Use Type-Safe Options**
   - Prefer provider-specific option classes over raw maps
   - Use model parameter data classes for better type safety
   - Validate options using the built-in validation methods

2. **Handle Errors Properly**
   - Use the AIError sealed class hierarchy
   - Handle rate limits with the retry mechanism
   - Use proper error recovery strategies

3. **Leverage Kotlin Features**
   - Use Flow operators for stream transformation
   - Use coroutines for async operations
   - Use data classes for structured data

4. **Testing**
   - Use MockK for mocking in tests
   - Test error handling paths
   - Use coroutine test utilities

## Common Issues

1. **Stream Handling**
   - Issue: Stream doesn't complete
   - Solution: Ensure proper Flow collection and cancellation

2. **Memory Usage**
   - Issue: Large responses consume too much memory
   - Solution: Use proper Flow backpressure handling

3. **Error Recovery**
   - Issue: Requests fail without retry
   - Solution: Use RetryHandler with proper configuration