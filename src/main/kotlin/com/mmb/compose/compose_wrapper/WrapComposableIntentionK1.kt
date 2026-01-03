package com.mmb.compose.compose_wrapper

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.mmb.compose.compose_wrapper.common.ComposeWrapLogic
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath

class WrapWithComposableIntentionK1 : PsiElementBaseIntentionAction() {

    override fun getText() = "Wrap with Compose layout"
    override fun getFamilyName() = "Jetpack Compose Wrappers"
    override fun startInWriteAction() = false

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        element: PsiElement
    ): Boolean =
        PsiTreeUtil.getParentOfType(element, KtCallExpression::class.java) != null

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val call = PsiTreeUtil.getParentOfType(element, KtCallExpression::class.java) ?: return
        val file = call.containingFile as? KtFile ?: return
        val currentEditor = editor ?: return

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(ComposeWrapLogic.choices)
            .setTitle("Wrap with")
            .setItemChosenCallback { selected ->
                WriteCommandAction.runWriteCommandAction(project) {
                    applyWrap(project, file, call, selected)
                }
            }
            .createPopup()
            .showInBestPositionFor(currentEditor)
    }

    private fun applyWrap(
        project: Project,
        file: KtFile,
        call: KtCallExpression,
        selected: String
    ) {
        val factory = KtPsiFactory(project)
        val wrappedCode = ComposeWrapLogic.buildWrappedCode(call.text, selected)
        val newExpr = factory.createExpression(wrappedCode)
        val replaced = call.replace(newExpr)

        ComposeWrapLogic.importFor(selected)?.let { fqName ->
            addImportIfNeeded(file, factory, fqName)
        }

        CodeStyleManager.getInstance(project).reformat(replaced)
    }

    private fun addImportIfNeeded(
        file: KtFile,
        factory: KtPsiFactory,
        fqNameStr: String
    ) {
        val exists = file.importDirectives.any {
            it.importPath?.pathStr == fqNameStr
        }
        if (exists) return

        val importPath = ImportPath(FqName(fqNameStr), false)
        val directive = factory.createImportDirective(importPath)

        file.importList?.add(directive)
            ?: file.packageDirective?.let { file.addAfter(directive, it) }
    }
}
