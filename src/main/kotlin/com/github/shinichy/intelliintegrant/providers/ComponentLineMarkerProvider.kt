package com.github.shinichy.intelliintegrant.providers

import com.github.shinichy.intelliintegrant.Util
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import cursive.psi.impl.ClEditorKeyword


class ComponentLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>?>
    ) {
        if (element !is ClEditorKeyword) {
            return
        }

        val components = Util.findComponents(element.project)
        components.firstOrNull { it.qualifiedName == element.qualifiedName }?.let { component ->
            val builder = NavigationGutterIconBuilder.create(AllIcons.Actions.GroupByClass)
                .setTargets(component)
                .setTooltipText("Navigate to Integrant component implementation")
            result.add(builder.createLineMarkerInfo(element))
        }
    }
}
