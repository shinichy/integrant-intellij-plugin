package com.github.shinichy.intelliintegrant.providers

import com.github.shinichy.intelliintegrant.Util
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
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
import cursive.psi.api.symbols.ClSymbol
import cursive.psi.impl.synthetic.SyntheticSymbol


class ComponentLineMarkerProvider : RelatedItemLineMarkerProvider() {

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

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>?>
    ) {
        if (isTopLevelMapKey(element)) {
            val implementations = Util.findImplementations(element.project)

            implementations.firstOrNull { it.qualifiedName == element.text }?.let { component ->
                val builder = NavigationGutterIconBuilder.create(AllIcons.Actions.GroupByClass)
                    .setTargets(component)
                    .setTooltipText("Navigate to Integrant component implementation")
                result.add(builder.createLineMarkerInfo(element))
            }
        }

        if (isComponentImplementation(element)) {
            (element.parent as? PsiReference)?.let { keyword ->
                keyword.resolve()?.let { resolvedElement ->
                    val projectScope = ProjectScopeBuilder.getInstance(element.project).buildProjectScope()

                    val topLevelMapKeys = ReferencesSearch.search(
                        resolvedElement,
                        projectScope
                    )
                        .mapNotNull { UsageInfo(it).element }
                        .filter { it.parent is ClMap }

                    val builder = NavigationGutterIconBuilder.create(AllIcons.Actions.GroupByClass)
                        .setTargets(topLevelMapKeys)
                        .setTooltipText("Navigate to Integrant component configuration")
                    result.add(builder.createLineMarkerInfo(element))

                }
            }
        }
    }
}
