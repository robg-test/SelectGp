package dev.bobproductions.bibdev.ui.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import dev.bobproductions.bibdev.ui.GptPrompt

class FixSelectionAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val project = e.project ?: return
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText ?: document.text

        GptPrompt(selectedText, document, selectionModel, project, editor, "Fix").show()
    }
}