package dev.bobproductions.bibdev.service.postprocessors

import com.intellij.openapi.editor.Document

fun String.reindent(indent: String): String {
    val lines = this
        .lines()
        .dropWhile { it.isBlank() }
        .dropLastWhile { it.isBlank() }

    return lines.mapIndexed { index, line ->
        when {
            index == 0 -> line
            line.isBlank() -> line
            else -> indent + line
        }
    }.joinToString("\n")
}

fun Document.getIndentation(selectionStart: Int): String {
    val lineNumber = getLineNumber(selectionStart)
    val lineStartOffset = getLineStartOffset(lineNumber)
    val lineText = text.substring(
        lineStartOffset,
        getLineEndOffset(lineNumber)
    )

    return lineText.takeWhile { it == ' ' || it == '\t' }
}

fun String.leadingWhitespace(): String {
    return takeWhile { it.isWhitespace() }
}
