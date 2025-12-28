package dev.bobproductions.bibdev.postprocessors

import dev.bobproductions.bibdev.service.postprocessors.reindent
import kotlin.test.Test
import kotlin.test.assertEquals

class PostProcessorTest {
    @Test
    fun `check reindent for basic text`() {
        val text = "first line\n  second line\n    third line"

        val expected = "first line\n   second line\n     third line"


        assertEquals(expected.debugVisibleWhitespace(), text.reindent(" ").debugVisibleWhitespace())
    }
}

fun String.debugVisibleWhitespace(): String =
    this.replace("\t", "<TAB>")
        .replace(" ", "·")


