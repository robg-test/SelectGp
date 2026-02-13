package dev.bobproductions.bibdev.ui.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import dev.bobproductions.bibdev.ui.GptPrompt

class FixFileAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        GptPrompt(editor, overridePrompt = "Fix", wholeFile = true).show()
    } }
