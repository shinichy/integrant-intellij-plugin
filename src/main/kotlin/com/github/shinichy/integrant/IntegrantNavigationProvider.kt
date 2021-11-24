@file:Suppress("UnstableApiUsage")

package com.github.shinichy.integrant

import com.intellij.navigation.DirectNavigationProvider
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import cursive.ClojureLanguage
import cursive.psi.api.ClKeyword
import cursive.psi.api.ClMap
import cursive.psi.api.ClTaggedLiteral
import cursive.psi.api.ClVector
import cursive.psi.impl.ClEditorKeyword
import cursive.psi.impl.ClLiteralImpl
import cursive.psi.impl.ClSharp
import cursive.psi.impl.ClTaggedLiteralImpl
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

    private fun getIncludedFiles(element: PsiElement): List<VirtualFile> {
        val module = ModuleUtil.findModuleForPsiElement(element)

        return module?.let {
            PsiTreeUtil.findChildrenOfType(element.containingFile, ClTaggedLiteralImpl::class.java)
                .filter { taggedLiteral ->
                    (PsiTreeUtil.findChildOfType(taggedLiteral, ClSharp::class.java)?.let {
                        it.containedElement?.text == "duct/include"
                    } == true)
                }
                .mapNotNull { includeLiteral ->
                    val maybeOrigFilePath =
                        PsiTreeUtil.findChildOfType(includeLiteral, ClLiteralImpl::class.java)?.let {
                            it.text.substring(1, it.text.length - 1)
                        }

                    maybeOrigFilePath?.let { origFilePath ->
                        val librarySourceRoots = ModuleRootManager.getInstance(module).orderEntries().allSourceRoots
                        val sourceRoots = ModuleRootManager.getInstance(it).sourceRoots + librarySourceRoots
                        listOf(origFilePath, "$origFilePath.edn").flatMap { filePath ->
                            sourceRoots.mapNotNull {
                                VfsUtilCore.findRelativeFile(filePath, it)
                            }
                        }
                    }
                }.flatten()
        }.orEmpty()
    }

    private fun findKeywordUsages(element: PsiElement): List<PsiElement> {
        val keyword = (element.parent as ClEditorKeyword).resolve() as SyntheticKeyword
        val includedFiles = getIncludedFiles(element) + element.containingFile.virtualFile
        val scope = FilesSearchScope(includedFiles.toSet())
        return ReferencesSearch.search(keyword, scope).mapNotNull { UsageInfo(it).element }
    }

    private fun findConfiguration(element: PsiElement): PsiElement? {
        return findKeywordUsages(element).firstOrNull { it.parent is ClMap }
    }

    private fun findConfigurationByCompositeKey(element: PsiElement): PsiElement? {
        val vector = element.parent.parent as ClVector
        val keywords = PsiTreeUtil.findChildrenOfType(vector, ClKeyword::class.java)

        return if (keywords.isNotEmpty()) {
            findKeywordUsages(element).firstOrNull { usedElement ->
                val targetVector = usedElement.parent
                val isTopLevelMapKey = targetVector is ClVector && targetVector.parent is ClMap
                val targetKeywords =
                    PsiTreeUtil.findChildrenOfType(targetVector, ClKeyword::class.java).map { it.text }
                isTopLevelMapKey && keywords.map { it.text } == targetKeywords
            }

        } else {
            null
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
                val module = ModuleUtil.findModuleForPsiElement(element)
                module?.let {
                    Util.findImplementations(
                        element.project,
                        module.getModuleWithDependenciesAndLibrariesScope(false)
                    )
                        .firstOrNull { it.qualifiedName == element.text }
                }
            } else null
        } else {
            return null
        }
    }
}
