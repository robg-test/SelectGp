
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor

fun getLineErrorsBetweenOffset(
    editor: Editor,
    startOffset: Int,
    endOffset: Int
): String {

    return ReadAction.compute<String, Throwable> {

        val document = editor.document
        val project = editor.project ?: return@compute ""


        val errors = mutableListOf<String>()

        DaemonCodeAnalyzerEx.processHighlights(
            document,
            project,
            HighlightSeverity.ERROR,
            startOffset,
            endOffset
        ) { highlight ->
            val lineNumber = document.getLineNumber(highlight.startOffset) + 1
            val message = highlight.description

            if (message != null) {
                errors.add("Line $lineNumber: $message")
            }

            true
        }

        errors.distinct().joinToString("; ")
    }
}
