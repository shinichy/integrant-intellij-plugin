@file:Suppress("UnstableApiUsage")

package com.github.shinichy.integrant

import com.intellij.navigation.DirectNavigationProvider
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import cursive.ClojureLanguage
import cursive.psi.api.*
import cursive.psi.impl.ClEditorKeyword
import cursive.psi.impl.ClSharp
import cursive.psi.impl.ClTaggedLiteralImpl
import cursive.psi.impl.synthetic.SyntheticKeyword


class IntegrantNavigationProvider : DirectNavigationProvider {
    private val log = logger<IntegrantNavigationProvider>()

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
        log.debug("module: $module")

        return module?.let {
            val moduleRootManager = ModuleRootManager.getInstance(module)

            val clTaggedLiterals =
                PsiTreeUtil.findChildrenOfType(element.containingFile, ClTaggedLiteralImpl::class.java)
            log.debug("clTaggedLiterals: ${clTaggedLiterals.map{it.text}}")

            val includes = clTaggedLiterals.filter { taggedLiteral ->
                (PsiTreeUtil.findChildOfType(taggedLiteral, ClSharp::class.java)?.let {
                    it.containedElement?.text == "duct/include"
                } == true)
            }
            log.debug("includes: ${includes.map{it.text}}")

            includes.mapNotNull { includeLiteral ->
                val maybeOrigFilePath =
                    PsiTreeUtil.findChildOfType(includeLiteral, ClLiteral::class.java)?.let {
                        it.text.substring(1, it.text.length - 1)
                    }

                if (maybeOrigFilePath == null) {
                    log.debug("origFilePath is null. includeLiteral: ${includeLiteral.text}")
                }

                maybeOrigFilePath?.let { origFilePath ->
                    val sourceRoots =
                        moduleRootManager.sourceRoots + moduleRootManager.orderEntries().allSourceRoots
                    log.debug("sourceRoots: $sourceRoots")

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
        log.debug("includedFiles: $includedFiles")
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
                log.debug("${element.text} is a composite ref")
                return findConfigurationByCompositeKey(element)
            }

            if (isSingleRef(element)) {
                log.debug("${element.text} is a ref")
                return findConfiguration(element)
            }

            return if (isQualifiedKeyword(element)) {
                log.debug("${element.text} is a qualified keyword")
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
