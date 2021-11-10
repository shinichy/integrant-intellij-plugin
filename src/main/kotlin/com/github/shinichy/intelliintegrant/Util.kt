package com.github.shinichy.intelliintegrant

import com.intellij.openapi.project.Project
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import cursive.gotoclass.ClojureGoToSymbolContributor
import cursive.parser.ClojureElementTypes
import cursive.psi.impl.ClEditorKeyword
import cursive.psi.impl.synthetic.SyntheticSymbol

object Util {
    fun findComponents(project: Project): List<ClEditorKeyword> {
        return ClojureGoToSymbolContributor()
            .getItemsByName("init-key", "init-key", project, true)
            .filterIsInstance(SyntheticSymbol::class.java)
            .firstOrNull()
            ?.let { symbol ->
                ReferencesSearch.search(symbol).map { UsageInfo(it) }.mapNotNull { info ->
                    info.element?.let { element ->
                        PsiTreeUtil.findSiblingForward(element, ClojureElementTypes.KEYWORD, null)?.let {
                            it as? ClEditorKeyword
                        }
                    }
                }
            }.orEmpty()
    }
}
