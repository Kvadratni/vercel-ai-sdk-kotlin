package dev.vercel.ai.test

import io.ktor.client.engine.mock.*
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlin.coroutines.EmptyCoroutineContext
import io.ktor.util.date.*

object TestHelpers {
    /**
     * Creates a standard streaming response for chat completions
     */
    fun createStreamingResponse(messages: List<String>): String {
        return messages.joinToString("\n\n") { message ->
            """data: {"id":"1","object":"chat.completion.chunk","created":1700000000,"model":"test-model","choices":[{"index":0,"delta":{"content":"$message"},"finish_reason":null}]}"""
        } + "\n\ndata: [DONE]\n"
    }
    
    /**
     * Creates a standard error response
     */
    fun createErrorResponse(code: String, message: String): String {
        return """{"error":{"code":"$code","message":"$message"}}"""
    }
    
    /**
     * Creates a mock response with authentication check
     */
    fun createAuthenticatedResponse(
        request: HttpRequestData,
        expectedApiKey: String,
        onSuccess: () -> HttpResponseData,
        onFailure: () -> HttpResponseData = {
            respond(
                content = createErrorResponse("401", "Access denied due to invalid subscription key"),
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
            )
        }
    ): HttpResponseData {
        val authHeader = request.headers["Authorization"]
        val apiKey = request.headers["api-key"] ?: request.headers["Authorization"]?.removePrefix("Bearer ")
        
        return if (apiKey == expectedApiKey) {
            onSuccess()
        } else {
            onFailure()
        }
    }
    
    /**
     * Creates a standard mock response
     */
    fun respond(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        headers: Headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
    ): HttpResponseData {
        return HttpResponseData(
            statusCode = status,
            requestTime = GMTDate(),
            headers = headers,
            version = HttpProtocolVersion.HTTP_2_0,
            body = ByteReadChannel(content),
            callContext = EmptyCoroutineContext
        )
    }
}