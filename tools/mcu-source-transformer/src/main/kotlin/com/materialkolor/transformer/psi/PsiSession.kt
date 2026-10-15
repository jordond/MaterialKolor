package com.materialkolor.transformer.psi

import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * The compiler-internal boundary is deliberately confined to this disposable environment.
 *
 * [KotlinCoreEnvironment.createForProduction] is a K1 entry point, deprecated since JetBrains is
 * removing the K1 frontend, hence the [K1Deprecation] and [CoreEnvironmentDeprecation] opt-ins. Its successor for standalone PSI
 * parsing is the Analysis API standalone session (`buildStandaloneAnalysisAPISession`), which this
 * project does not use yet. That session is still experimental and its artifacts are not published
 * to Maven Central (tracked by KT-56203). The transformer stays on the pinned parser until both
 * hold. The switch plan lives in docs/upstream-psi.md under parser succession.
 *
 * `:mcu-source-transformer:testAlternateParser` runs this session against the newest compiler pin,
 * so the build fails visibly when a candidate pin finally drops the entry point.
 */
@OptIn(K1Deprecation::class, CoreEnvironmentDeprecation::class, CompilerConfiguration.Internals::class)
internal class PsiSession : AutoCloseable {
    private val disposable = Disposer.newDisposable()
    private val environment = KotlinCoreEnvironment.createForProduction(
        disposable,
        CompilerConfiguration.create(),
        EnvironmentConfigFiles.JVM_CONFIG_FILES,
    )
    private val factory = KtPsiFactory(environment.project, false)

    fun parse(
        name: String,
        source: String,
    ): KtFile =
        factory.createFile(name, source).also { file ->
            PsiTreeUtil.findChildOfType(file, PsiErrorElement::class.java)?.let { error ->
                throw IllegalArgumentException(
                    "$name:${error.textRange.startOffset}: syntax: invalid Kotlin syntax: ${error.errorDescription}",
                )
            }
        }

    override fun close() = Disposer.dispose(disposable)
}

internal inline fun <reified T : PsiElement> PsiElement.nodes(): Collection<T> =
    PsiTreeUtil.collectElementsOfType(this, T::class.java)

internal fun comments(source: String): Map<String, Int> {
    val lexer = KotlinLexer()
    val comments = mutableListOf<String>()
    lexer.start(source)
    while (lexer.tokenType != null) {
        if (KtTokens.COMMENTS.contains(lexer.tokenType)) {
            comments += source.substring(lexer.tokenStart, lexer.tokenEnd)
        }
        lexer.advance()
    }
    return comments.groupingBy { it }.eachCount()
}
