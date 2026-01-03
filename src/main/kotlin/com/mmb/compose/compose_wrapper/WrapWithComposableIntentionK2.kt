package com.mmb.compose.compose_wrapper

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.mmb.compose.compose_wrapper.common.ComposeWrapLogic
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.intentions.AbstractKotlinApplicableIntention
import org.jetbrains.kotlin.psi.*


class WrapWithComposableIntentionK2 :
    AbstractKotlinApplicableIntention<KtCallExpression>() {

    override fun getFamilyName() = "Jetpack Compose Wrappers"
    override fun getActionName(element: KtCallExpression) =
        "Wrap with Compose layout"

    override fun isApplicableByPsi(element: KtCallExpression) = true

    override fun isApplicableByAnalyze(
        element: KtCallExpression,
        analysisSession: KtAnalysisSession
    ) = true

    override fun apply(element: KtCallExpression, project: Project) {
        val editor = element.findEditor() ?: return

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(ComposeWrapLogic.choices)
            .setTitle("Wrap with")
            .setItemChosenCallback { selected ->
                applyWrap(project, element, selected)
            }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    private fun applyWrap(
        project: Project,
        call: KtCallExpression,
        selected: String
    ) {
        val file = call.containingFile as? KtFile ?: return
        val factory = KtPsiFactory(project)

        val wrapped = factory.createExpression(
            ComposeWrapLogic.buildWrappedCode(call.text, selected)
        )

        call.replace(wrapped)

        ComposeWrapLogic.importFor(selected)?.let { fqName ->
            if (file.importDirectives.none { it.importPath?.pathStr == fqName }) {
                val importDirective = factory.createImportDirective(
                    org.jetbrains.kotlin.resolve.ImportPath(
                        org.jetbrains.kotlin.name.FqName(fqName),
                        false
                    )
                )
                file.importList?.add(importDirective)
            }
        }
    }
}
