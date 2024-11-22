package dev.vercel.ai.stream

import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.errors.AIError
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object AIStream {
    fun fromResponse(
        response: HttpResponse,
        signal: AbortSignal? = null,
        parser: (String) -> String? = { it }
    ): Flow<String> = flow {
        if (!response.status.isSuccess()) {
            throw AIError.ProviderError(
                message = "Request failed with status code ${response.status.value}",
                statusCode = response.status.value,
                provider = "openai"
            )
        }

        val channel = response.bodyAsChannel()
        val buffer = StringBuilder()

        while (!channel.isClosedForRead) {
            signal?.throwIfAborted()

            val chunk = channel.readRemaining().readText()
            buffer.append(chunk)

            val lines = buffer.toString().split("\n")
            buffer.clear()

            for (line in lines) {
                if (line.isEmpty() || !line.startsWith("data: ")) {
                    buffer.append(line).append("\n")
                    continue
                }

                val data = line.substring(6)
                if (data == "[DONE]") return@flow

                val content = parser(data)
                if (content != null) {
                    emit(content)
                }
            }
        }
    }
}