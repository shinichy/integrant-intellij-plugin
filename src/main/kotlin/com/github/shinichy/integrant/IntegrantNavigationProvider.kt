@file:Suppress("UnstableApiUsage")

package com.github.shinichy.integrant

import com.intellij.navigation.DirectNavigationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import cursive.ClojureLanguage
import cursive.psi.api.ClKeyword
import cursive.psi.api.ClMap
import cursive.psi.api.ClTaggedLiteral
import cursive.psi.impl.ClEditorKeyword
import cursive.psi.impl.ClSharp
import cursive.psi.impl.synthetic.SyntheticKeyword


class IntegrantNavigationProvider : DirectNavigationProvider {

    private fun isQualifiedKeyword(element: PsiElement): Boolean {
        return element.parent is ClKeyword &&
                (element.parent as ClKeyword).namespace != null
    }

    private fun isRef(element: PsiElement): Boolean {
        return if (element.parent is ClKeyword && element.parent.parent is ClTaggedLiteral) {
            val taggedLiteral = element.parent.parent
            return (PsiTreeUtil.findChildOfType(taggedLiteral, ClSharp::class.java)?.let {
                it.containedElement?.text == "ig/ref"
            } == true)
        } else false
    }

    private fun findConfiguration(keyword: SyntheticKeyword): PsiElement? {
        return ReferencesSearch.search(keyword, keyword.useScope)
            .map { UsageInfo(it) }
            .firstOrNull { info ->
                info.element?.let { it.parent is ClMap } == true
            }?.element
    }

    override fun getNavigationElement(element: PsiElement): PsiElement? {
        if (element.language == ClojureLanguage.getInstance()) {
            if (isRef(element)) {
                return findConfiguration((element.parent as ClEditorKeyword).resolve() as SyntheticKeyword)
            }

            return if (isQualifiedKeyword(element)) {
                Util.findImplementations(element.project, element.useScope)
                    .firstOrNull { it.qualifiedName == element.text }
            } else null
        } else {
            return null
        }
    }
}
