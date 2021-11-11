package com.github.shinichy.intelliintegrant.providers

import com.github.shinichy.intelliintegrant.Util
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import cursive.psi.api.ClKeyword
import cursive.psi.api.ClMap


class ComponentLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>?>
    ) {
        val isQualifiedKeyword by lazy { (element.parent as ClKeyword).namespace != null }
        val isTopLevel by lazy { element.parent.parent is ClMap }

        if (element is LeafPsiElement &&
            element.parent is ClKeyword &&
            isQualifiedKeyword &&
            isTopLevel
        ) {
            val implementations = Util.findImplementations(element.project)
            implementations.firstOrNull { it.qualifiedName == element.text }?.let { component ->
                val builder = NavigationGutterIconBuilder.create(AllIcons.Actions.GroupByClass)
                    .setTargets(component)
                    .setTooltipText("Navigate to Integrant component implementation")
                result.add(builder.createLineMarkerInfo(element))
            }
        }
    }
}
