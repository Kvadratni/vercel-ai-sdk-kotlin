package dev.vercel.ai.providers

import dev.vercel.ai.AIModel
import dev.vercel.ai.models.ChatMessage
import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.errors.AIError
import dev.vercel.ai.options.AzureOpenAIOptions
import dev.vercel.ai.options.ProviderOptions
import dev.vercel.ai.stream.AIStream
import dev.vercel.ai.tools.CallableTool
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.*

/**
 * Azure OpenAI API provider implementation.
 *
 * @property apiKey The Azure OpenAI API key
 * @property httpClient Optional custom HTTP client
 */
class AzureOpenAIProvider(
    private val apiKey: String,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }
    }
) : AIModel {
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun chat(
        messages: List<ChatMessage>,
        options: ProviderOptions,
        tools: List<CallableTool>?,
        signal: AbortSignal?
    ): Flow<String> {
        require(options is AzureOpenAIOptions) { "Options must be AzureOpenAIOptions" }
        
        val requestBody = buildJsonObject {
            // Add messages
            put("messages", buildJsonArray {
                messages.forEach { message ->
                    addJsonObject {
                        put("role", message.role)
                        put("content", message.content)
                        message.name?.let { put("name", it) }
                        message.functionCall?.let { functionCall ->
                            put("function_call", buildJsonObject {
                                put("name", functionCall.name)
                                put("arguments", functionCall.arguments.toString())
                            })
                        }
                    }
                }
            })
            
            // Add model parameters from options
            val optionsMap = options.toMap()
            optionsMap.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    is Boolean -> put(key, value)
                    is List<*> -> put(key, buildJsonArray { 
                        value.forEach { item ->
                            when (item) {
                                is String -> add(item)
                                else -> throw IllegalArgumentException("Unsupported list type")
                            }
                        }
                    })
                }
            }
            
            // Add tools if provided
            tools?.let { toolList ->
                put("tools", buildJsonArray {
                    toolList.forEach { tool ->
                        addJsonObject {
                            put("type", "function")
                            put("function", buildJsonObject {
                                put("name", tool.definition.function.name)
                                put("description", tool.definition.function.description)
                                put("parameters", json.parseToJsonElement(
                                    json.encodeToString(
                                        JsonObject.serializer(),
                                        JsonObject(tool.definition.function.parameters.mapValues { (_, v) ->
                                            json.parseToJsonElement(v.toString())
                                        })
                                    )
                                ))
                            })
                        }
                    }
                })
            }
            
            // Always stream
            put("stream", true)
        }

        try {
            val response = httpClient.post("${options.endpoint}/openai/deployments/${options.deploymentId}/chat/completions") {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                    append("api-key", apiKey)
                    append("api-version", options.apiVersion)
                }
                setBody(requestBody.toString())
            }

            when (response.status) {
                HttpStatusCode.TooManyRequests -> {
                    val retryAfter = response.headers["Retry-After"]?.toLongOrNull()?.times(1000)
                    throw AIError.RateLimitError(
                        provider = "azure_openai",
                        retryAfter = retryAfter
                    )
                }
                HttpStatusCode.Unauthorized -> {
                    throw AIError.ProviderError(
                        statusCode = response.status.value,
                        message = "Invalid API key",
                        provider = "azure_openai"
                    )
                }
                HttpStatusCode.BadRequest -> {
                    throw AIError.ProviderError(
                        statusCode = response.status.value,
                        message = "Bad request - check your input parameters",
                        provider = "azure_openai"
                    )
                }
                else -> {
                    if (response.status.value >= 400) {
                        throw AIError.ProviderError(
                            statusCode = response.status.value,
                            message = "Unexpected error from Azure OpenAI API",
                            provider = "azure_openai"
                        )
                    }
                }
            }

            return AIStream.fromResponse(response, signal) { content ->
                try {
                    val jsonElement = Json.parseToJsonElement(content)
                    jsonElement.jsonObject["choices"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.content
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            when (e) {
                is AIError -> throw e
                else -> throw AIError.ProviderError(
                    statusCode = 500,
                    message = e.message ?: "Unknown error",
                    provider = "azure_openai"
                )
            }
        }
    }

    override suspend fun complete(
        prompt: String,
        options: ProviderOptions,
        signal: AbortSignal?
    ): Flow<String> {
        require(options is AzureOpenAIOptions) { "Options must be AzureOpenAIOptions" }
        return chat(
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            options = options,
            tools = null,
            signal = signal
        )
    }
}
