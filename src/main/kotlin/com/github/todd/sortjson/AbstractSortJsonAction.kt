package com.github.todd.sortjson

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager

abstract class AbstractSortJsonAction(private val recursive: Boolean) : AnAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = project != null && editor != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val document = editor.document

        val targetRange = ObjectRangeFinder.findTargetRange(editor, psiFile)
        if (targetRange == null) {
            HintManager.getInstance().showInformationHint(editor, "No sortable JSON, TypeScript interface/type, or key-value block found")
            return
        }

        val originalText = document.getText(targetRange)
        val engine = JsonSortEngine(recursive = recursive)
        val sortedText = engine.sort(originalText)

        if (sortedText == originalText) {
            HintManager.getInstance().showInformationHint(editor, "Keys are already sorted")
            return
        }

        val commandName = if (recursive) "Sort Keys Recursively" else "Sort Keys"
        WriteCommandAction.runWriteCommandAction(project, commandName, "SortJson", {
            document.replaceString(targetRange.startOffset, targetRange.endOffset, sortedText)
            if (psiFile != null) {
                val psiDocManager = PsiDocumentManager.getInstance(project)
                psiDocManager.commitDocument(document)
                try {
                    CodeStyleManager.getInstance(project).reformatRange(
                        psiFile,
                        targetRange.startOffset,
                        targetRange.startOffset + sortedText.length
                    )
                } catch (_: Exception) {
                    // Ignore reformatting failure if unsupported for file type
                }
            }
        })
    }
}
