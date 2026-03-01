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
        val output: List<OutputItem> = emptyList()
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OutputItem(
        val type: String? = null,
        val content: List<ContentItem>? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ContentItem(
        val type: String? = null,
        val text: String? = null
    )

    fun performModelRequest(): String {
    client.newCall(request).execute().use { response ->
        val body = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            error("HTTP ${response.code}: $body")
        }

        val parsed: ApiResponse = mapper.readValue(body)

        val text = parsed.output
            .firstOrNull { it.type == "message" }
            ?.content
            ?.firstOrNull { it.type == "output_text" }
            ?.text

        LOG.warn(text)
        return text ?: error("No output_text found in response. Raw body: $body")
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
