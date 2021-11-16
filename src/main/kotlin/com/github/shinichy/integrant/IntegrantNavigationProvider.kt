@file:Suppress("UnstableApiUsage")

package com.github.shinichy.integrant

import com.intellij.navigation.DirectNavigationProvider
import com.intellij.psi.PsiElement
import cursive.ClojureLanguage
import cursive.psi.api.ClKeyword


class IntegrantNavigationProvider : DirectNavigationProvider {

    private fun isQualifiedKeyword(element: PsiElement): Boolean {
        return element.parent is ClKeyword &&
                (element.parent as ClKeyword).namespace != null
    }

    override fun getNavigationElement(element: PsiElement): PsiElement? {
        return if (element.language == ClojureLanguage.getInstance() && isQualifiedKeyword(element)) {
            Util.findImplementations(element.project)
                .firstOrNull { it.qualifiedName == element.text }
        } else null
    }
}
