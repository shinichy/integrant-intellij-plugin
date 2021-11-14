package com.github.shinichy.intelliintegrant

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import cursive.gotoclass.ClojureGoToSymbolContributor
import cursive.parser.ClojureElementTypes
import cursive.psi.api.ClKeyword
import cursive.psi.api.symbols.ClSymbol

object Util {
    fun findImplementations(project: Project): List<ClKeyword> {
        return ClojureGoToSymbolContributor()
            .getItemsByName("init-key", "init-key", project, true)
            .filterIsInstance(ClSymbol::class.java)
            .firstOrNull { it.namespace == "integrant.core" }
            ?.let { symbol ->
                val searchScope = GlobalSearchScopesCore.projectProductionScope(project)
                ReferencesSearch.search(symbol, searchScope).map { UsageInfo(it) }.mapNotNull { info ->
                    info.element?.let { element ->
                        PsiTreeUtil.findSiblingForward(element, ClojureElementTypes.KEYWORD, null)?.let {
                            it as? ClKeyword
                        }
                    }
                }
            }.orEmpty()
    }
}
