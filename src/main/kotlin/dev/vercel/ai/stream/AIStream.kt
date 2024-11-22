package dev.vercel.ai.stream

import dev.vercel.ai.common.AbortSignal
import dev.vercel.ai.errors.AIError
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.Response

object AIStream {
    fun fromResponse(
        response: Response,
        signal: AbortSignal? = null
    ): Flow<String> = flow {
        response.use { r ->
            if (!r.isSuccessful) {
                throw AIError.ProviderError(
                    message = "Request failed with status code ${r.code}",
                    statusCode = r.code,
                    provider = "azure"
                )
            }

            val body = r.body ?: throw AIError.StreamError(
                message = "Empty response body",
                cause = null
            )
            val source = body.source()
            val buffer = source.buffer

            while (!source.exhausted()) {
                signal?.throwIfAborted()

                val line = buffer.readUtf8Line() ?: continue
                if (line.isEmpty() || !line.startsWith("data: ")) continue

                val data = line.substring(6)
                if (data == "[DONE]") break

                emit(data)
            }
        }
    }

    fun fromResponse(
        response: HttpResponse,
        signal: AbortSignal? = null,
        parser: (String) -> String?
    ): Flow<String> = flow {
        if (!response.status.isSuccess()) {
            throw AIError.ProviderError(
                message = "Request failed with status code ${response.status.value}",
                statusCode = response.status.value,
                provider = "azure"
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
