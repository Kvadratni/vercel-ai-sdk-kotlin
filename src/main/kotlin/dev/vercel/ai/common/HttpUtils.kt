package dev.vercel.ai.common

import io.ktor.client.statement.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import java.nio.charset.StandardCharsets

/**
 * Converts a Ktor HttpResponse to an OkHttp Response.
 * This is needed because AIStream expects an OkHttp Response.
 */
suspend fun HttpResponse.toOkHttpResponse(): Response {
    val responseBody = bodyAsText()
    val buffer = Buffer().writeString(responseBody, StandardCharsets.UTF_8)
    
    return Response.Builder()
        .code(status.value)
        .message(status.description)
        .protocol(Protocol.HTTP_1_1)
        .body(ResponseBody.create(
            "application/json".toMediaType(),
            buffer.size,
            buffer
        ))
        .request(Request.Builder().url("http://placeholder.url").build())
        .build()
}