package com.github.shinichy.integrant

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope

class FilesSearchScope(private val files: Set<VirtualFile>) : GlobalSearchScope() {
    override fun contains(file: VirtualFile): Boolean {
        return files.contains(file)
    }

    override fun isSearchInModuleContent(aModule: Module): Boolean {
        return false
    }

    override fun isSearchInLibraries(): Boolean {
        return false
    }
}
