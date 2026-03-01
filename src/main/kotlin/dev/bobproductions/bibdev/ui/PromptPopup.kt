package dev.bobproductions.bibdev.ui

import com.fasterxml.jackson.core.io.JsonStringEncoder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import dev.bobproductions.bibdev.service.api.GptModelRequestInvoker
import dev.bobproductions.bibdev.service.builders.GptModelRequestBuilder
import dev.bobproductions.bibdev.service.builders.GptModelRequestType
import dev.bobproductions.bibdev.service.builders.Model
import dev.bobproductions.bibdev.service.postprocessors.getIndentation
import dev.bobproductions.bibdev.service.postprocessors.leadingWhitespace
import dev.bobproductions.bibdev.service.postprocessors.reindent
import getLineErrorsBetweenOffset
import java.awt.Point
import javax.swing.JComponent

class GptPrompt(
    private val editor: Editor,
    private val overridePrompt: String? = null,
    private val wholeFile: Boolean = false
) {

    private val inputField = JBTextField()

    fun show() {
        if (overridePrompt != null) {
            applyChanges(); return
        }
        val content: JComponent = panel {
            row {
                label("")
                    .gap(RightGap.SMALL)

                cell(inputField)
                    .resizableColumn()
                    .columns(40)
                    .focused()
            }
        }

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, inputField)
            .setTitle("Command")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setCancelOnClickOutside(true)
            .createPopup()

        inputField.addActionListener {
            applyChanges()
            popup.closeOk(null)
        }


        val editorComponent = editor.component
        val bounds = editorComponent.bounds
        val location = Point(bounds.width / 2 - 200, 40)

        popup.show(RelativePoint(editorComponent, location))
    }

    private fun applyChanges() {
        var preexistingCode = ""
        var entireCode = ""

        if (!wholeFile && editor.selectionModel.hasSelection()) {
            preexistingCode = editor.selectionModel.selectedText ?: ""
            entireCode = editor.document.text
        } else {
            preexistingCode = editor.document.text
        }
        val fileExtension = FileDocumentManager.getInstance().getFile(editor.document)?.extension ?: return

        val preexistingCodeEscaped = jsonEscape(preexistingCode)
        val gptModelRequestBuilder = GptModelRequestBuilder()
        gptModelRequestBuilder.fileExtension = fileExtension

        val startOffset = editor.selectionModel.selectionStart
        val endOffset = editor.selectionModel.selectionEnd

        gptModelRequestBuilder.documentErrors = getLineErrorsBetweenOffset(editor, startOffset, endOffset)

        gptModelRequestBuilder.prompt = overridePrompt ?: inputField.text

        gptModelRequestBuilder.model = Model(System.getenv("MODEL_NAME") ?: "gpt-5.2-codex")
        gptModelRequestBuilder.existingCode = preexistingCodeEscaped
        if (wholeFile) {
            gptModelRequestBuilder.buildGenerationBody(GptModelRequestType.FULL)
        } else {
            gptModelRequestBuilder.entireCode = entireCode
            gptModelRequestBuilder.buildGenerationBody(GptModelRequestType.PARTIAL)
        }
        val modelInvoker = GptModelRequestInvoker(gptModelRequestBuilder.buildGenerationRequest())

        val anchorOffset =
            if (!wholeFile && editor.selectionModel.hasSelection()) editor.selectionModel.selectionStart
            else editor.caretModel.offset
        val originalStartLine = editor.document.getLineNumber(anchorOffset)

        val indent = editor.document.getIndentation(0)

        ProgressManager.getInstance().run(object : Task.Modal(
            editor.project,
            "Calling Model",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Waiting for AI response..."

                val rawResult = modelInvoker.performModelRequest()

                ApplicationManager.getApplication().invokeAndWait {
                    val result = rawResult.reindent(indent)
                    val document = editor.document

                    WriteCommandAction.runWriteCommandAction(project) {
                        document.setText(preexistingCode.leadingWhitespace() + result)
                    }

                    val safeLine = minOf(originalStartLine, document.lineCount - 1)
                    val lineStartOffset = document.getLineStartOffset(safeLine)

                    editor.selectionModel.removeSelection()
                    editor.caretModel.moveToOffset(lineStartOffset)
                    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
                }
            }
        })
    }

    private fun jsonEscape(value: String): String =
        String(JsonStringEncoder.getInstance().quoteAsString(value))
}
