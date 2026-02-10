package com.mmb.compose.compose_wrapper

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtBlockExpression

class UnwrapComposableIntention : PsiElementBaseIntentionAction() {

    override fun getText(): String = "Remove Wrapper (Unwrap)"
    override fun getFamilyName(): String = "Jetpack Compose Wrappers"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val callExpression = PsiTreeUtil.getParentOfType(element, KtCallExpression::class.java)
        return callExpression?.lambdaArguments?.isNotEmpty() ?: false
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val callExpression = PsiTreeUtil.getParentOfType(element, KtCallExpression::class.java) ?: return
        val project = project

        WriteCommandAction.runWriteCommandAction(project) {
            val lambdaArgument = callExpression.lambdaArguments.firstOrNull()
            val bodyExpression = lambdaArgument?.getLambdaExpression()?.bodyExpression ?: return@runWriteCommandAction


            val firstStatement = bodyExpression.statements.firstOrNull()
            val isLazyItem = firstStatement is KtCallExpression && firstStatement.calleeExpression?.text == "item"

            val targetBody = if (isLazyItem) {
                (firstStatement as KtCallExpression).lambdaArguments.firstOrNull()?.getLambdaExpression()?.bodyExpression
            } else {
                bodyExpression
            } ?: bodyExpression

            val parent = callExpression.parent

            var child = targetBody.firstChild
            while (child != null) {
                if (child.text != "{" && child.text != "}") {
                    parent.addBefore(child, callExpression)
                }
                child = child.nextSibling
            }
            // -----------------------

            callExpression.delete()

            CodeStyleManager.getInstance(project).reformat(parent)
        }
    }
}