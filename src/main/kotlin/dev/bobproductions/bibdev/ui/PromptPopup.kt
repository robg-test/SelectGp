package dev.bobproductions.bibdev.ui

import com.fasterxml.jackson.core.io.JsonStringEncoder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
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
import dev.bobproductions.bibdev.service.builders.Model
import dev.bobproductions.bibdev.service.postprocessors.getIndentation
import dev.bobproductions.bibdev.service.postprocessors.leadingWhitespace
import dev.bobproductions.bibdev.service.postprocessors.reindent
import java.awt.Point
import javax.swing.JComponent

    class GptPrompt(
        private val contextualString: String,
        private val document: Document,
        private val selection: SelectionModel,
        private val project: Project,
        private val editor: Editor,
        private val overridePrompt: String? = null
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
            val preexistingCode = contextualString
            val fileExtension = FileDocumentManager.getInstance().getFile(editor.document)?.extension ?: return


            val preexistingCodeEscaped = jsonEscape(preexistingCode)
            val gptModelRequestBuilder =
                GptModelRequestBuilder(
                    fileExtension,
                    overridePrompt ?: inputField.text,
                    Model(System.getenv("MODEL_NAME") ?: "gpt-4o")
                )
            gptModelRequestBuilder.existingCode = preexistingCodeEscaped
            gptModelRequestBuilder.buildBody()
            val modelInvoker = GptModelRequestInvoker(gptModelRequestBuilder.buildRequest())


            ProgressManager.getInstance().run(object : Task.Modal(
                project,
                "Calling Model",
                true
            ) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    indicator.text = "Waiting for AI response..."

                    val rawResult = modelInvoker.invokeRequest()

                    ApplicationManager.getApplication().invokeLater {
                        val indent = document.getIndentation(selection.selectionStart)
                        val result = rawResult.reindent(indent)

                        val start = selection.selectionStart
                        val end = selection.selectionEnd

                        if (start <= document.textLength) {
                            WriteCommandAction.runWriteCommandAction(project) {
                                document.deleteString(start, end)
                                document.insertString( start, preexistingCode.leadingWhitespace() + result)
                            }
                        }
                    }
                }
            })

        }

        private fun jsonEscape(value: String): String =
            String(JsonStringEncoder.getInstance().quoteAsString(value))
    }
