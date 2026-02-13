
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

    private var rawBody: String = ""

    private val jsonTool = JsonTools()

    private val chatCompletionsUri =
        "https://api.openai.com/v1/chat/completions"

    private val modelsUri =
        "https://api.openai.com/v1/models"

    var apiKey: String =
       loadGptApiKey().toString()

    fun buildGenerationBody(promptType: GptModelRequestType): String {
        val model = requireNotNull(this.model) { "model is required" }
        val prompt = requireNotNull(this.prompt) { "prompt is required" }
        val fileExtension = requireNotNull(this.fileExtension) { "fileExtension is required" }

        var systemPrompt: String

        val fullFilePrompt = when (fileExtension.lowercase()) {
            "java" -> """
        You are a code generation engine.
        Output ONLY valid Java source code as a complete file.
        Do not include explanations, comments, markdown, or backticks.
        Include required structural elements for valid syntax.
        Do not include unnecessary scaffolding.
        If the request cannot be fulfilled, return an empty response.
    """.trimIndent()

            "cs", "csharp" -> """
        You are a code generation engine.
        Output ONLY valid C# source code as a complete file.
        Do not include explanations, comments, markdown, or backticks.
        Include required structural elements for valid syntax.
        Do not include unnecessary scaffolding.
        If the request cannot be fulfilled, return an empty response.
    """.trimIndent()

            "kt", "kotlin" -> """
        You are a code generation engine.
        Output ONLY valid Kotlin source code as a complete file.
        Do not include explanations, comments, markdown, or backticks.
        Include required structural elements for valid syntax.
        If the request cannot be fulfilled, return an empty response.
    """.trimIndent()

            "go" -> """
        You are a code generation engine.
        Output ONLY valid Go source code as a complete file.
        Do not include explanations, comments, markdown, or backticks.
        Include required structural elements for valid syntax.
        If the request cannot be fulfilled, return an empty response.
    """.trimIndent()

            "rs", "rust" -> """
        You are a code generation engine.
        Output ONLY valid Rust source code as a complete file.
        Do not include explanations, comments, markdown, or backticks.
        Include required structural elements for valid syntax.
        If the request cannot be fulfilled, return an empty response.
    """.trimIndent()

            "c", "cpp", "cc", "cxx" -> """
        You are a code generation engine.
        Output ONLY valid C/C++ source code as a complete file.
        Do not include explanations, comments, markdown, or backticks.
        Include required structural elements for valid syntax.
        If the request cannot be fulfilled, return an empty response.
    """.trimIndent()

            "py", "python" -> """
        You are a code generation engine.
        Output ONLY valid Python source code as a complete file.
        Do not include explanations, comments, markdown, or backticks.
        If the request cannot be fulfilled, return an empty response.
    """.trimIndent()

            "js", "javascript", "ts", "typescript" -> """
        You are a code generation engine.
        Output ONLY valid JavaScript or TypeScript source code as a complete file.
        Do not include explanations, comments, markdown, or backticks.
        If the request cannot be fulfilled, return an empty response.
    """.trimIndent()

            else -> """
        You are a code generation engine.
        Output ONLY valid source code as a complete file.
        Do not include explanations, comments, markdown, or backticks.
        If the request cannot be fulfilled, return an empty response.
    """.trimIndent()
        }


        val patchPrompt = when (fileExtension.lowercase()) {
            "java" -> """
        You are a code generation engine.
        The prompt contains existing Java source code.
        Output ONLY the minimal modified or newly added code fragment.
        The output does NOT need to be a complete file.
        Do NOT output class declarations.
        Do NOT output a main method.
        Do NOT repeat unchanged code.
        Return only the exact fragment required to insert or replace.
        If structural changes are required, return an empty response.
    """.trimIndent()

            "cs", "csharp" -> """
        You are a code generation engine.
        The prompt contains existing C# source code.
        Output ONLY the minimal modified or newly added code fragment.
        The output does NOT need to be a complete file.
        Do NOT output class declarations.
        Do NOT output Program or Main.
        Do NOT repeat unchanged code.
        Return only the exact fragment required to insert or replace.
        If structural changes are required, return an empty response.
    """.trimIndent()

            "kt", "kotlin" -> """
        You are a code generation engine.
        The prompt contains existing Kotlin source code.
        Output ONLY the minimal modified or newly added code fragment.
        The output does NOT need to be a complete file.
        Do NOT output class declarations.
        Do NOT output a main function.
        Do NOT repeat unchanged code.
        Return only the exact fragment required to insert or replace.
        If structural changes are required, return an empty response.
    """.trimIndent()

            "go" -> """
        You are a code generation engine.
        The prompt contains existing Go source code.
        Output ONLY the minimal modified or newly added code fragment.
        The output does NOT need to be a complete file.
        Do NOT output package declarations.
        Do NOT output func main().
        Do NOT repeat unchanged code.
        Return only the exact fragment required to insert or replace.
        If structural changes are required, return an empty response.
    """.trimIndent()

            "rs", "rust" -> """
        You are a code generation engine.
        The prompt contains existing Rust source code.
        Output ONLY the minimal modified or newly added code fragment.
        The output does NOT need to be a complete file.
        Do NOT output fn main().
        Do NOT repeat unchanged code.
        Return only the exact fragment required to insert or replace.
        If structural changes are required, return an empty response.
    """.trimIndent()

            "c", "cpp", "cc", "cxx" -> """
        You are a code generation engine.
        The prompt contains existing C/C++ source code.
        Output ONLY the minimal modified or newly added code fragment.
        The output does NOT need to be a complete file.
        Do NOT output a main function.
        Do NOT repeat unchanged code.
        Return only the exact fragment required to insert or replace.
        If structural changes are required, return an empty response.
    """.trimIndent()

            "py", "python" -> """
        You are a code generation engine.
        The prompt contains existing Python source code.
        Output ONLY the minimal modified or newly added code fragment.
        The output does NOT need to be a complete file.
        Do NOT output a __main__ block.
        Do NOT repeat unchanged code.
        Return only the exact fragment required to insert or replace.
        If structural changes are required, return an empty response.
    """.trimIndent()

            "js", "javascript", "ts", "typescript" -> """
        You are a code generation engine.
        The prompt contains existing JavaScript or TypeScript source code.
        Output ONLY the minimal modified or newly added code fragment.
        The output does NOT need to be a complete file.
        Do NOT create wrapper functions or entry points.
        Do NOT repeat unchanged code.
        Return only the exact fragment required to insert or replace.
        If structural changes are required, return an empty response.
    """.trimIndent()

            else -> """
        You are a code generation engine.
        The prompt contains existing source code.
        Output ONLY the minimal modified or newly added code fragment.
        The output does NOT need to be a complete file.
        Do NOT repeat unchanged code.
        Return only the exact fragment required to insert or replace.
        If structural changes are required, return an empty response.
    """.trimIndent()
        }

        systemPrompt = ""

        if (promptType == GptModelRequestType.PARTIAL) {
            systemPrompt = patchPrompt
        }

        if (promptType == GptModelRequestType.FULL) {
            systemPrompt = fullFilePrompt
        }


        rawBody = """
            {
              "model": "${model.name}",
              "messages": [
                {
                  "role": "system",
                  "content": "${jsonTool.jsonEscape(systemPrompt)}"
                },
                {
                  "role": "user",
                  "content": "'${jsonTool.jsonEscape(prompt)}' my existing code is '${jsonTool.jsonEscape(existingCode)}' ${documentErrors?.let { "with the following file errors: ${jsonTool.jsonEscape(it)}"} ?: ""} try and come up with a working solution based on my prompt"
                  
                }
              ]
            }
        """.trimIndent()

        LOG.warn(rawBody)
        return rawBody
    }

    fun buildGenerationRequest(): Request {

        return Request.Builder()
            .url(chatCompletionsUri)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(rawBody.toRequestBody("application/json".toMediaType()))
            .build()
    }

    fun buildTestRequest(): Request =
        Request.Builder()
            .url(modelsUri)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()
}