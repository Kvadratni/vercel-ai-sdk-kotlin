package dev.vercel.ai.embeddings

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Base interface for embeddings providers
 */
interface EmbeddingsProvider {
    /**
     * Creates embeddings for the given input text
     * @param input The text to create embeddings for
     * @param options Provider-specific options
     * @return List of embeddings (vectors of floats)
     */
    suspend fun createEmbeddings(input: String, options: EmbeddingsOptions): List<List<Float>>
    
    /**
     * Creates embeddings for multiple input texts
     * @param inputs List of texts to create embeddings for
     * @param options Provider-specific options
     * @return List of embeddings (vectors of floats) for each input
     */
    suspend fun createEmbeddings(inputs: List<String>, options: EmbeddingsOptions): List<List<Float>>
}

/**
 * OpenAI embeddings provider implementation
 */
class OpenAIEmbeddingsProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val client: OkHttpClient = OkHttpClient()
) : EmbeddingsProvider {
    
    override suspend fun createEmbeddings(input: String, options: EmbeddingsOptions): List<List<Float>> {
        return createEmbeddings(listOf(input), options)
    }
    
    override suspend fun createEmbeddings(inputs: List<String>, options: EmbeddingsOptions): List<List<Float>> {
        require(options is OpenAIEmbeddingsOptions) { "Options must be OpenAIEmbeddingsOptions" }
        options.validate()
        
        val requestBody = JSONObject().apply {
            put("model", options.model)
            put("input", inputs)
            options.user?.let { put("user", it) }
            options.dimensions?.let { put("dimensions", it) }
        }
        
        val request = Request.Builder()
            .url("$baseUrl/embeddings")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
            
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            throw IOException("API call failed with code ${response.code}: $errorBody")
        }
        
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        val jsonResponse = JSONObject(responseBody)
        
        return jsonResponse.getJSONArray("data").let { data ->
            (0 until data.length()).map { i ->
                val embedding = data.getJSONObject(i).getJSONArray("embedding")
                (0 until embedding.length()).map { j ->
                    embedding.getDouble(j).toFloat()
                }
            }
        }
    }
}

/**
 * Azure OpenAI embeddings provider implementation
 */
class AzureOpenAIEmbeddingsProvider(
    private val apiKey: String,
    private val baseUrl: String,
    private val client: OkHttpClient = OkHttpClient()
) : EmbeddingsProvider {
    
    override suspend fun createEmbeddings(input: String, options: EmbeddingsOptions): List<List<Float>> {
        return createEmbeddings(listOf(input), options)
    }
    
    override suspend fun createEmbeddings(inputs: List<String>, options: EmbeddingsOptions): List<List<Float>> {
        require(options is AzureOpenAIEmbeddingsOptions) { "Options must be AzureOpenAIEmbeddingsOptions" }
        options.validate()
        
        val requestBody = JSONObject().apply {
            put("input", inputs)
            options.user?.let { put("user", it) }
            options.dimensions?.let { put("dimensions", it) }
        }
        
        val request = Request.Builder()
            .url("$baseUrl/embeddings?api-version=${options.apiVersion}")
            .addHeader("api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
            
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            throw IOException("API call failed with code ${response.code}: $errorBody")
        }
        
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        val jsonResponse = JSONObject(responseBody)
        
        return jsonResponse.getJSONArray("data").let { data ->
            (0 until data.length()).map { i ->
                val embedding = data.getJSONObject(i).getJSONArray("embedding")
                (0 until embedding.length()).map { j ->
                    embedding.getDouble(j).toFloat()
                }
            }
        }
    }
}