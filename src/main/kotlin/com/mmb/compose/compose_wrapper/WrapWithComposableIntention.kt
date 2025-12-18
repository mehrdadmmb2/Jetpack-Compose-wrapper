package com.mmb.compose.compose_wrapper

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.psi.PsiFile
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.ui.Messages
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

class WrapWithComposableIntention : PsiElementBaseIntentionAction() {

    override fun getText(): String = "Wrap with Row/Column/Box/Lazy..."
    override fun getFamilyName(): String = "Jetpack Compose Wrappers"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return true
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = element.containingFile

        val callExpression = com.intellij.psi.util.PsiTreeUtil.getParentOfType(
            element,
            org.jetbrains.kotlin.psi.KtCallExpression::class.java
        )

        if (callExpression == null) {
            com.intellij.openapi.ui.Messages.showErrorDialog(
                project,
                "نشانگر موس باید روی یک تابع کامپوزابل (مثل Text) باشد",
                "خطا"
            )
            return
        }

        val choices = arrayOf("Row", "Column", "Box", "LazyRow", "LazyColumn")
        com.intellij.openapi.ui.popup.JBPopupFactory.getInstance()
            .createPopupChooserBuilder(choices.toList())
            .setTitle("Wrap with...")
            .setItemChosenCallback { selected ->
                com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
                    wrapCode(project, file, callExpression, selected)
                }
            }
            .createPopup()
            .showInBestPositionFor(editor!!)

    }

    override fun startInWriteAction(): Boolean = true

    private fun wrapCode(project: Project, file: PsiFile, callExpression: KtCallExpression, selected: String) {
        val factory = org.jetbrains.kotlin.psi.KtPsiFactory(project)
        val originalCode = callExpression.text
        val ktFile = file as org.jetbrains.kotlin.psi.KtFile

        val newCode = when (selected) {
            "Row" -> "Row {\n $originalCode \n}"
            "Column" -> "Column {\n $originalCode \n}"
            "Box" -> "Box {\n $originalCode \n}"
            "LazyRow" -> "LazyRow {\n item { $originalCode } \n}"
            "LazyColumn" -> "LazyColumn {\n item { $originalCode } \n}"
            else -> return
        }

        val newExpression = factory.createExpression(newCode)
        val replaced = callExpression.replace(newExpression)


        val importsToAdd = when (selected) {
            "Row" -> listOf("androidx.compose.foundation.layout.Row")
            "Column" -> listOf("androidx.compose.foundation.layout.Column")
            "Box" -> listOf("androidx.compose.foundation.layout.Box")
            "LazyRow" -> listOf("androidx.compose.foundation.lazy.LazyRow")
            "LazyColumn" -> listOf("androidx.compose.foundation.lazy.LazyColumn")
            else -> emptyList()
        }

        importsToAdd.forEach { fqName ->
            val importPath = org.jetbrains.kotlin.resolve.ImportPath(org.jetbrains.kotlin.name.FqName(fqName), false)
            if (ktFile.importDirectives.none { it.importPath == importPath }) {
                val newImport = factory.createImportDirective(importPath)
                ktFile.importList?.add(newImport) ?: ktFile.addBefore(newImport, ktFile.firstChild)
            }
        }

        com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(replaced)
    }
}