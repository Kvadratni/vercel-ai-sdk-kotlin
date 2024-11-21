package dev.vercel.ai.stream

import dev.vercel.ai.errors.AIError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.Response
import java.io.BufferedReader

/**
 * Utility class for handling streaming responses from AI providers
 */
object AIStream {
    /**
     * Creates a Flow from an OkHttp Response that emits chunks of text
     * @param response The OkHttp Response object
     * @return A Flow emitting text chunks
     */
    fun fromResponse(response: Response): Flow<String> = flow {
        if (!response.isSuccessful) {
            throw AIError.ProviderError(
                statusCode = response.code,
                message = response.message,
                provider = "openai"
            )
        }

        val reader = response.body?.byteStream()?.bufferedReader()
            ?: throw AIError.StreamError("Empty response body", null)

        try {
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("data: ")) {
                        val data = line.substring(6).trim()
                        if (data != "[DONE]") {
                            emit(data)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw AIError.StreamError("Error reading stream", e)
        }
    }
}