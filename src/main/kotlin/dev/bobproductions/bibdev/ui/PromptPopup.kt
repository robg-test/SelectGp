package dev.bobproductions.bibdev.ui

import com.fasterxml.jackson.core.io.JsonStringEncoder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.awt.RelativePoint
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
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
            if (overridePrompt != null) { applyChanges(); return}
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
            val preexistingCode = editor.selectionModel.selectedText ?: editor.document.text
            val fileExtension = FileDocumentManager.getInstance().getFile(editor.document)?.extension ?: return


            val preexistingCodeEscaped = jsonEscape(preexistingCode)
            val gptModelRequestBuilder = GptModelRequestBuilder()
            gptModelRequestBuilder.fileExtension = fileExtension

            val startOffset = editor.selectionModel.selectionStart
            val endOffset = editor.selectionModel.selectionEnd

            gptModelRequestBuilder.documentErrors = getLineErrorsBetweenOffset(editor,  startOffset, endOffset)

            gptModelRequestBuilder.prompt = overridePrompt ?: inputField.text

            gptModelRequestBuilder.model = Model(System.getenv("MODEL_NAME") ?: "gpt-5.1")
            gptModelRequestBuilder.existingCode = preexistingCodeEscaped
            if (wholeFile) {
                gptModelRequestBuilder.buildGenerationBody(GptModelRequestType.FULL)
            }
            else {
                gptModelRequestBuilder.buildGenerationBody(GptModelRequestType.PARTIAL)
            }
            val modelInvoker = GptModelRequestInvoker(gptModelRequestBuilder.buildGenerationRequest())


            ProgressManager.getInstance().run(object : Task.Modal(
                editor.project,
                "Calling Model",
                true
            ) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    indicator.text = "Waiting for AI response..."

                    val rawResult = modelInvoker.performModelRequest()

                    ApplicationManager.getApplication().invokeLater {
                        val indent = if (!wholeFile) {
                            editor.document.getIndentation(editor.selectionModel.selectionStart)
                        } else {
                            editor.document.getIndentation(0)
                        }
                        val result = rawResult.reindent(indent)

                        var start: Int
                        var end: Int

                        if (wholeFile) {
                            start = 0
                            end = editor.document.textLength
                            WriteCommandAction.runWriteCommandAction(project) {
                                editor.document.deleteString(start, end)
                                editor.document.insertString(start,  preexistingCode.leadingWhitespace() + result)
                            }
                        }
                        else {
                            start = editor.selectionModel.selectionStart
                            end = editor.selectionModel.selectionEnd

                            if (start <= editor.document.textLength) {
                                WriteCommandAction.runWriteCommandAction(project) {
                                    editor.document.deleteString(start, end)
                                    editor.document.insertString( start, preexistingCode.leadingWhitespace() + result)
                                }
                            }
                        }
                    }
                }
            })

        }

        private fun jsonEscape(value: String): String =
            String(JsonStringEncoder.getInstance().quoteAsString(value))
    }
