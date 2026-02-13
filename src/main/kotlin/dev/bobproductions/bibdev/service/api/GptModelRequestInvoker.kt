
package dev.bobproductions.bibdev.service.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.diagnostic.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class GptModelRequestInvoker(
    private val request: Request,
) {
    companion object {
        private val LOG = Logger.getInstance(GptModelRequestInvoker::class.java)
    }

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

    fun performModelRequest(): String {
        client.newCall(request).execute().use { response ->
            val body = response.body.string()

            if (!response.isSuccessful) {
                error("HTTP ${response.code}: $body")
            }

            val parsed: ApiResponse = mapper.readValue<ApiResponse>(content = body)
            val content = parsed.choices.first().message.content
            return content
        }
    }

    fun canCommunicateWithModel(): Boolean {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}")
            }
            return true
        }
    }
}