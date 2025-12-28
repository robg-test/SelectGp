package dev.bobproductions.bibdev.service.builders

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import dev.bobproductions.bibdev.service.utils.JsonTools
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GptModelRequestBuilder(var fileExtension: String, var prompt: String, var model: Model) {
    companion object {
        private val LOG = Logger.getInstance(GptModelRequestBuilder::class.java)
    }

    var existingCode: String = ""
        set(value) {
            field = value.trim()
        }
    var rawBody: String = ""

    val jsonTool: JsonTools = JsonTools()

    private val gptUri =
        "https://api.openai.com/v1/chat/completions"

    val apiKey: String =
        System.getenv("API_COPILOT_KEY")
            ?: error("API_COPILOT_KEY not set")

    fun buildBody(): String {
        rawBody = """
            {
              "model": "${model.name}",
              "messages": [
              {
                  "role": "system",
                  "content": "You are a code generation engine. Respond with ONLY valid source code from the language with file extension '$fileExtension' Do not include explanations, comments, markdown, or formatting. Do not wrap the response in backticks. If the answer is not code, return an empty response"
              },
              {
                  "role": "user",
                  "content": "'${jsonTool.jsonEscape(prompt)}' my existing code is '${jsonTool.jsonEscape(existingCode)}' try and come up with a working solution based on my prompt"
              }
              ]
            }
        """.trimIndent()
        LOG.warn(rawBody)
        System.out.println(rawBody)
        return rawBody
    }

    fun buildRequest(): Request {
        val requestBody = rawBody
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(gptUri)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(requestBody)
            .build()
        LOG.warn(request.toString())
        return request
    }
}