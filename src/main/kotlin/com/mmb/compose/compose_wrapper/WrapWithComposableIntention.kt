package com.mmb.compose.compose_wrapper

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.command.WriteCommandAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.resolve.ImportPath

class WrapWithComposableIntention : PsiElementBaseIntentionAction() {

    override fun getText(): String = "Wrap with Row/Column/Box/Lazy..."
    override fun getFamilyName(): String = "Jetpack Compose Wrappers"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return PsiTreeUtil.getParentOfType(element, KtCallExpression::class.java) != null
    }

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = element.containingFile
        val callExpression = PsiTreeUtil.getParentOfType(element, KtCallExpression::class.java) ?: return
        val currentEditor = editor ?: return

        val choices = listOf("Row", "Column", "Box", "LazyRow", "LazyColumn","Card",
                "Surface")

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(choices)
            .setTitle("Wrap with...")
            .setItemChosenCallback { selected ->
                WriteCommandAction.runWriteCommandAction(project) {
                    wrapCode(project, file, callExpression, selected)
                }
            }
            .createPopup()
            .showInBestPositionFor(currentEditor)
    }

    private fun wrapCode(project: Project, file: PsiFile, callExpression: KtCallExpression, selected: String) {
        val factory = KtPsiFactory(project)
        val originalCode = callExpression.text
        val ktFile = file as? KtFile ?: return

        val newCode = when (selected) {
            "Row" -> "Row {\n $originalCode \n}"
            "Column" -> "Column {\n $originalCode \n}"
            "Box" -> "Box {\n $originalCode \n}"
            "Card" -> "Card {\n $originalCode \n}"
            "Surface" -> "Surface {\n $originalCode \n}"
            "LazyRow" -> "LazyRow {\n item { $originalCode } \n}"
            "LazyColumn" -> "LazyColumn {\n item { $originalCode } \n}"
            else -> return
        }

        val newExpression = factory.createExpression(newCode)
        val replaced = callExpression.replace(newExpression)

        val fqNameString = when (selected) {
            "Row", "Column", "Box" -> "androidx.compose.foundation.layout.$selected"
            "LazyRow", "LazyColumn" -> "androidx.compose.foundation.lazy.$selected"
            "Surface","Card" -> "androidx.compose.material3.$selected"
            else -> null
        }

        fqNameString?.let { fqNameStr ->
            val fqName = FqName(fqNameStr)
            val importPath = ImportPath(fqName, false)

            val alreadyImported = ktFile.importDirectives.any { it.importPath?.pathStr == fqNameStr }

            if (!alreadyImported) {
                val newImport = factory.createImportDirective(importPath)

                val importList = ktFile.importList
                if (importList != null) {
                    importList.add(newImport)
                } else {
                    ktFile.packageDirective?.let { ktFile.addAfter(newImport, it) }
                }
            }
        }

        CodeStyleManager.getInstance(project).reformat(replaced)
    }
}