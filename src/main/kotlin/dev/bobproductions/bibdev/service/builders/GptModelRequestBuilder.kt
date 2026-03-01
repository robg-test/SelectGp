package dev.bobproductions.bibdev.service.builders

import com.intellij.openapi.diagnostic.Logger
import dev.bobproductions.bibdev.service.utils.JsonTools
import loadGptApiKey
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

enum class GptModelRequestType {
    PARTIAL, FULL
}

class GptModelRequestBuilder {

    companion object {
        private val LOG = Logger.getInstance(GptModelRequestBuilder::class.java)
    }

    var fileExtension: String? = null
    var prompt: String? = null
    var model: Model? = null
    var documentErrors: String? = null

    var existingCode: String = ""
        set(value) {
            field = value.trim()
        }

    var entireCode: String = ""

    private var rawBody: String = ""

    private val jsonTool = JsonTools()

    private val responsesUri = "https://api.openai.com/v1/responses"
    private val modelsUri = "https://api.openai.com/v1/models"

    var apiKey: String = loadGptApiKey().toString()

    private val systemPrompt = """
        You are a senior software engineer

        Your task is to generate correct, minimal, production-ready code based strictly on the user's prompt and provided context.

        Behaviour rules:

        PARTIAL request:
        - Modify ONLY the provided existing code block.
        - Output the full file provided in FULL_FILE_CONTEXT but with the modified code block.

        FULL request:
        - Generate a complete working file.
        - Ensure it compiles without errors.
        - Follow best practices for the specified language.

        Always:
            - Respect the provided file extension and language.
            - Fix any provided compiler or document errors.
            - Do not include explanations.
            - Do not include markdown.
            - Do not include code fences.
            - Do not include comments unless required by the language.
            - Output ONLY raw compilable code.
            - Avoid unused imports.
            - Avoid undefined symbols.
            - Avoid placeholders unless unavoidable.

        If ambiguous, choose the safest minimal correct implementation.

        Return only the final code.
    """.trimIndent()

    fun buildGenerationBody(promptType: GptModelRequestType): String {
        val model = requireNotNull(this.model) { "model is required" }
        val prompt = requireNotNull(this.prompt) { "prompt is required" }
        val fileExtension = requireNotNull(this.fileExtension) { "fileExtension is required" }

        val userText = buildString {
            appendLine("REQUEST_TYPE: ${promptType.name}")
            appendLine("FILE_EXTENSION: $fileExtension")
            appendLine()
            appendLine("USER_PROMPT:")
            appendLine(prompt)
            appendLine()
            appendLine("EXISTING_CODE:")
            appendLine(existingCode)

            if (promptType == GptModelRequestType.PARTIAL) {
                appendLine()
                appendLine("FULL_FILE_CONTEXT:")
                appendLine(entireCode)
            }

            documentErrors?.let {
                appendLine()
                appendLine("DOCUMENT_ERRORS:")
                appendLine(it)
            }
        }

        rawBody = """
            {
              "model": "${model.name}",
              "reasoning": { "effort": "medium" },
              "input": [
                {
                  "role": "system",
                  "content": [
                    {
                      "type": "input_text",
                      "text": "${jsonTool.jsonEscape(systemPrompt)}"
                    }
                  ]
                },
                {
                  "role": "user",
                  "content": [
                    {
                      "type": "input_text",
                      "text": "${jsonTool.jsonEscape(userText)}"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        LOG.debug(rawBody)
        return rawBody
    }

    fun buildGenerationRequest(): Request =
        Request.Builder()
            .url(responsesUri)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(rawBody.toRequestBody("application/json".toMediaType()))
            .build()

    fun buildTestRequest(): Request =
        Request.Builder()
            .url(modelsUri)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()
}