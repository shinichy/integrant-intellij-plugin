package com.github.shinichy.integrant

import com.intellij.openapi.project.DumbService
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.TestIndexingModeSupporter
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.UnindexedFilesUpdater
import cursive.gotoclass.ClojureGoToSymbolContributor
import cursive.index.CljIndex
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import java.io.File
import java.util.*

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class IntegrantNavigationProviderTest : LightJavaCodeInsightFixtureTestCase() {

    private fun attachMavenLibrary(@Suppress("SameParameterValue") mavenArtifact: String) {
        val jars = getMavenArtifacts(mavenArtifact)
        Arrays.stream(jars).forEach { jar ->
            PsiTestUtil.addLibrary(myFixture.projectDisposable, module, jar.name, jar.parent, jar.name)
        }
    }

    private fun getMavenArtifacts(vararg mavenArtifacts: String): Array<File> {
        val files = Maven.configureResolver()
            .withRemoteRepo("clojars.org", "https://repo.clojars.org", "default")
            .resolve(*mavenArtifacts)
            .withTransitivity()
            .asFile()
        require(files.isNotEmpty()) { "Failed to resolve artifacts " + mavenArtifacts.contentToString() }
        return files
    }

    override fun setUp() {
        super.setUp()

        attachMavenLibrary("integrant:integrant:0.8.0")
    }

    override fun getTestDataPath() = "src/test/testData/integrant"

    fun testGotoImplementation() {
        myFixture.configureByFile("simple.clj")
        val dumbService = DumbService.getInstance(project)

        dumbService.runWhenSmart {
            assertFalse(dumbService.isDumb)
            // fixme: integrant library is not indexed for some reason...
            val components = ClojureGoToSymbolContributor().getItemsByName("init-key", "init-key", myFixture.project, true)
            assertNotEmpty(components.toList())
//            println(this.myFixture.elementAtCaret)
//            val element = myFixture.getReferenceAtCaretPositionWithAssertion("simple.clj").resolve()
//            myFixture.performEditorAction("GotoDeclaration")
//            println(myFixture.elementAtCaret.textOffset)
//            assertNotNull(element)
        }
    }
}
