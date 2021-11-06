package com.github.shinichy.intelliintegrant.services

import com.intellij.openapi.project.Project
import com.github.shinichy.intelliintegrant.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
