package dev.bobproductions.bibdev.service
import dev.bobproductions.bibdev.service.builders.GptModelRequestBuilder
import dev.bobproductions.bibdev.service.builders.Model
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelPromptBuilderTest {
    @Test
    fun shouldBuildPrompt() {
        val builder = GptModelRequestBuilder(
            ".py",
            "Write a Hello World program",
            Model("gpt-3.5-turbo")
        )
        builder.existingCode = "pront(\"Hi World\")"

        val expectedPrompt = """
        {
          "model": "gpt-3.5-turbo",
          "messages": [
          {
              "role": "system",
              "content": "You are a code generation engine. Respond with ONLY valid source code from the language with file extension '.py' Do not include explanations, comments, markdown, or formatting. Do not wrap the response in backticks. If the answer is not code, return an empty response"
          },
          {
              "role": "user",
              "content": "'Write a Hello World program' my existing code is 'pront(\"Hi World\")' try and come up with a working solution based on my prompt"
          }
          ]
        }
        """.trimIndent()
        assertEquals(builder.buildBody(), expectedPrompt)

    }

    @Test
    fun shouldBuildRequest() {
        val builder = GptModelRequestBuilder(
            ".py",
            "Write a Hello World program",
            Model("gpt-3.5-turbo")
        )
        builder.existingCode = "pront(\"Hi World\")"
        val request = builder.buildRequest()
        assertEquals(request.method, "POST")
        assertEquals(request.url.toString(), "https://api.githubcopilot.com/chat/completions")
        assertEquals(request.header("Content-Type"), "application/json")
        assertEquals(request.header("Accept"), "application/json")
        assertEquals(request.header("Authorization"), "Bearer API_COPILOT_KEY not set")
    }
}