@file:Suppress("UnstableApiUsage")

package com.github.shinichy.integrant

import com.intellij.navigation.DirectNavigationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import cursive.ClojureLanguage
import cursive.psi.api.ClKeyword
import cursive.psi.api.ClMap
import cursive.psi.api.ClTaggedLiteral
import cursive.psi.api.ClVector
import cursive.psi.impl.ClEditorKeyword
import cursive.psi.impl.ClSharp
import cursive.psi.impl.synthetic.SyntheticKeyword


class IntegrantNavigationProvider : DirectNavigationProvider {

    private fun isQualifiedKeyword(element: PsiElement): Boolean {
        return element.parent is ClKeyword &&
                (element.parent as ClKeyword).namespace != null
    }

    private fun isSingleRef(element: PsiElement): Boolean {
        return if (element.parent is ClKeyword && element.parent.parent is ClTaggedLiteral) {
            val taggedLiteral = element.parent.parent
            return (PsiTreeUtil.findChildOfType(taggedLiteral, ClSharp::class.java)?.let {
                it.containedElement?.text == "ig/ref"
            } == true)
        } else false
    }

    private fun isCompositeRef(element: PsiElement): Boolean {
        return if (element.parent is ClKeyword && element.parent.parent is ClVector && element.parent.parent.parent is ClTaggedLiteral) {
            val taggedLiteral = element.parent.parent.parent
            return (PsiTreeUtil.findChildOfType(taggedLiteral, ClSharp::class.java)?.let {
                it.containedElement?.text == "ig/ref"
            } == true)
        } else false
    }

    private fun findConfiguration(element: PsiElement): PsiElement? {
        val keyword = (element.parent as ClEditorKeyword).resolve() as SyntheticKeyword
        val scope = GlobalSearchScopesCore.directoryScope(element.containingFile.containingDirectory, true)
        return ReferencesSearch.search(keyword, scope)
            .mapNotNull { UsageInfo(it).element }
            .firstOrNull { it.parent is ClMap }
    }

    private fun findConfigurationByCompositeKey(element: PsiElement): PsiElement? {
        val vector = element.parent.parent as ClVector
        val keywords = PsiTreeUtil.findChildrenOfType(vector, ClKeyword::class.java)

        if (keywords.isNotEmpty()) {
            val keyword = (keywords.first() as ClEditorKeyword).resolve() as SyntheticKeyword
            val scope = GlobalSearchScopesCore.directoryScope(element.containingFile.containingDirectory, true)
            return ReferencesSearch.search(keyword, scope)
                .mapNotNull { UsageInfo(it).element }
                .firstOrNull { usedElement ->
                    val targetVector = usedElement.parent
                    val isTopLevelMapKey = targetVector is ClVector && targetVector.parent is ClMap
                    val targetKeywords =
                        PsiTreeUtil.findChildrenOfType(targetVector, ClKeyword::class.java).map { it.text }
                    isTopLevelMapKey && keywords.map { it.text } == targetKeywords
                }

        } else {
            return null
        }
    }

    override fun getNavigationElement(element: PsiElement): PsiElement? {
        if (element.language == ClojureLanguage.getInstance()) {
            if (isCompositeRef(element)) {
                return findConfigurationByCompositeKey(element)
            }

            if (isSingleRef(element)) {
                return findConfiguration(element)
            }

            return if (isQualifiedKeyword(element)) {
                Util.findImplementations(
                    element.project,
                    GlobalSearchScopesCore.projectProductionScope(element.project)
                )
                    .firstOrNull { it.qualifiedName == element.text }
            } else null
        } else {
            return null
        }
    }
}
