@file:Suppress("UnstableApiUsage")

package com.github.shinichy.intelliintegrant

import com.intellij.navigation.DirectNavigationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.ProjectScopeBuilder
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import cursive.parser.ClojureElementTypes
import cursive.psi.api.ClKeyword
import cursive.psi.api.ClMap
import cursive.psi.api.ClojureFile
import cursive.psi.api.symbols.ClSymbol
import cursive.psi.impl.synthetic.SyntheticSymbol


class IntegrantNavigationProvider : DirectNavigationProvider {

    private fun isTopLevelMapKey(element: PsiElement): Boolean {
        val isTopLevel by lazy { element.parent.parent is ClMap }

        return isQualifiedKeyword(element) && isTopLevel
    }

    private fun isQualifiedKeyword(element: PsiElement): Boolean {
        return element is LeafPsiElement &&
                element.parent is ClKeyword &&
                (element.parent as ClKeyword).namespace != null
    }

    private fun isAfterInitKey(element: PsiElement): Boolean {
        return PsiTreeUtil.findSiblingBackward(element.parent, ClojureElementTypes.SYMBOL, null)?.let { symbolElement ->
            (symbolElement as? ClSymbol)?.let { symbol ->
                symbol.reference?.let { reference ->
                    reference.resolve()?.let { targetElement ->
                        (targetElement as? SyntheticSymbol)?.qualifiedName == "integrant.core/init-key"
                    }
                }
            }
        } == true
    }

    private fun isComponentImplementation(element: PsiElement): Boolean {
        return isQualifiedKeyword(element) && isAfterInitKey(element)
    }

    override fun getNavigationElement(element: PsiElement): PsiElement? {
        return if (element.containingFile is ClojureFile) {
            return if (isTopLevelMapKey(element)) {
                val implementations = Util.findImplementations(element.project)
                implementations.firstOrNull { it.qualifiedName == element.text }
            } else if (isComponentImplementation(element)) {
                (element.parent as? PsiReference)?.let { keyword ->
                    keyword.resolve()?.let { resolvedElement ->
                        val projectScope = ProjectScopeBuilder.getInstance(element.project).buildProjectScope()

                        ReferencesSearch.search(
                            resolvedElement,
                            projectScope
                        )
                            .mapNotNull { UsageInfo(it).element }
                            .firstOrNull { it.parent is ClMap }
                    }
                }
            } else null
        } else null
    }
}
