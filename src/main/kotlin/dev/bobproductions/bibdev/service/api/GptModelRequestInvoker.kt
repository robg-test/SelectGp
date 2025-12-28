
package dev.bobproductions.bibdev.service.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class GptModelRequestInvoker(
    private val request: Request,
) {
    private val mapper = jacksonObjectMapper()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ApiResponse(
        val choices: List<Choice>
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Choice(
        val message: Message
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Message(
        val content: String
    )

    fun invokeRequest(): String {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
                ?: error("Empty response body")

            if (!response.isSuccessful) {
                error("HTTP ${response.code}: $body")
            }

            val parsed: ApiResponse = mapper.readValue(body)
            return parsed.choices.first().message.content
        }
    }
}
