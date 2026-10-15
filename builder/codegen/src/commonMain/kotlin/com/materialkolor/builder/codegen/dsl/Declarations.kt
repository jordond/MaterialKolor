package com.materialkolor.builder.codegen.dsl

import com.materialkolor.builder.codegen.symbol.Symbol
import kotlin.jvm.JvmName

/**
 * An annotation as it is written above a declaration, with its arguments if it takes any.
 *
 * `AnnotationSpec(Symbols.OptIn, listOf(classLiteral(Symbols.ExperimentalMaterial3ExpressiveApi)))`
 * writes `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` and imports what the arguments name.
 * Anywhere that takes a list of annotation symbols also takes a list of these.
 */
public class AnnotationSpec(
    internal val symbol: Symbol,
    internal val arguments: List<Expression> = emptyList(),
) {
    internal val expression: Expression = annotationExpression(symbol, arguments)
}

private fun annotationExpression(
    symbol: Symbol,
    values: List<Expression>,
): Expression {
    val name = listOf(annotationToken("@${symbol.simpleName}"))
    if (values.isEmpty()) return Expression(name, listOf(symbol))

    return buildCall(name, listOf(symbol), multiline = false) {
        values.forEach { value -> argument(value) }
    }
}

/**
 * The visibility a declaration can ask for. Leaving it out gives Kotlin's default, public.
 */
public enum class Visibility {
    Internal,
    Private,
}

/**
 * Which kind of class [KotlinFileScope.classDeclaration] writes.
 */
public enum class ClassKind {
    Class,
    DataClass,
}

internal fun Visibility?.modifierTokens(): List<Token> =
    when (this) {
        null -> emptyList()
        Visibility.Internal -> listOf(keywordToken("internal"), spaceToken)
        Visibility.Private -> listOf(keywordToken("private"), spaceToken)
    }

private fun ClassKind.keywordTokens(): List<Token> =
    when (this) {
        ClassKind.Class -> listOf(keywordToken("class"))
        ClassKind.DataClass -> listOf(keywordToken("data"), spaceToken, keywordToken("class"))
    }

/**
 * The constructor properties of a class, and optionally its body.
 */
@CodegenDsl
public class ClassScope internal constructor() {
    internal val properties: MutableList<ParameterSpec> = mutableListOf()
    internal val members: MutableList<Declaration> = mutableListOf()

    /**
     * A `val` in the primary constructor, with an optional default.
     */
    public fun property(
        name: String,
        type: TypeRef,
        default: Expression? = null,
    ) {
        properties += ParameterSpec(name, type, default, isProperty = true)
    }

    /**
     * The same, for the common case where the type is just a symbol.
     */
    public fun property(
        name: String,
        symbol: Symbol,
        default: Expression? = null,
    ) {
        property(name, type(symbol), default)
    }

    /**
     * The properties and functions between the braces.
     */
    public fun body(build: MembersScope.() -> Unit) {
        members += MembersScope().apply(build).declarations
    }
}

/**
 * The properties and functions in the body of a class or an object.
 */
@CodegenDsl
public class MembersScope internal constructor() {
    internal val declarations: MutableList<Declaration> = mutableListOf()

    /**
     * A `val` in the body, as in `val all = listOf(brand)`.
     */
    public fun property(
        name: String,
        value: Expression,
        type: TypeRef? = null,
        visibility: Visibility? = null,
    ) {
        declarations += PropertyDeclaration(name, value, type, const = false, visibility, receiver = null)
    }

    /**
     * A member function.
     */
    public fun function(
        name: String,
        annotations: List<Symbol> = emptyList(),
        returns: TypeRef? = null,
        visibility: Visibility? = null,
        receiver: TypeRef? = null,
        build: FunctionScope.() -> Unit,
    ) {
        function(name, annotations.map { AnnotationSpec(it) }, returns, visibility, receiver, build)
    }

    /**
     * The same, for annotations that take arguments.
     */
    @JvmName("functionWithAnnotations")
    public fun function(
        name: String,
        annotations: List<AnnotationSpec>,
        returns: TypeRef? = null,
        visibility: Visibility? = null,
        receiver: TypeRef? = null,
        build: FunctionScope.() -> Unit,
    ) {
        val scope = FunctionScope().apply(build)
        declarations += FunctionDeclaration(name, annotations, returns, visibility, receiver, scope)
    }
}

/**
 * Something a file or a class body declares, which knows its own names, imports and layout.
 */
internal sealed interface Declaration {
    /**
     * Every name this declares, its members included, for the import clash guard.
     */
    val names: List<String>

    val symbols: List<Symbol>

    fun render(writer: CodeWriter)
}

/**
 * A `val`, or with a [receiver] an extension property.
 *
 * An extension property has no backing field to initialise, so its value becomes a getter on the
 * line below.
 */
internal class PropertyDeclaration(
    private val name: String,
    private val value: Expression,
    private val type: TypeRef?,
    private val const: Boolean,
    private val visibility: Visibility?,
    private val receiver: TypeRef?,
) : Declaration {
    init {
        require(!const || receiver == null) { "An extension property cannot be const" }
    }

    override val names: List<String> = listOf(name)

    override val symbols: List<Symbol> = value.symbols + type?.symbols.orEmpty() + receiver?.symbols.orEmpty()

    override fun render(writer: CodeWriter) {
        val head = buildList {
            addAll(visibility.modifierTokens())
            if (const) {
                add(keywordToken("const"))
                add(spaceToken)
            }
            add(keywordToken("val"))
            add(spaceToken)
            if (receiver != null) {
                addAll(receiver.tokens)
                add(punctuationToken("."))
            }
            add(plainToken(name))
            if (type != null) {
                add(punctuationToken(":"))
                add(spaceToken)
                addAll(type.tokens)
            }
        }
        val assignment = listOf(spaceToken, punctuationToken("="), spaceToken)

        if (receiver == null) {
            writer.expression(value, head + assignment)
        } else {
            writer.line(head)
            writer.indented {
                writer.expression(value, listOf(keywordToken("get"), punctuationToken("()")) + assignment)
            }
        }
    }
}

internal class FunctionDeclaration(
    private val name: String,
    private val annotations: List<AnnotationSpec>,
    private val returns: TypeRef?,
    private val visibility: Visibility?,
    private val receiver: TypeRef?,
    private val scope: FunctionScope,
) : Declaration {
    override val names: List<String> = listOf(name)

    override val symbols: List<Symbol> =
        annotations.flatMap { it.expression.symbols } +
            receiver?.symbols.orEmpty() +
            scope.collectSymbols() +
            returns?.symbols.orEmpty()

    override fun render(writer: CodeWriter) {
        annotations.forEach { annotation -> writer.expression(annotation.expression) }
        val head = buildList {
            addAll(visibility.modifierTokens())
            add(keywordToken("fun"))
            add(spaceToken)
            if (receiver != null) {
                addAll(receiver.tokens)
                add(punctuationToken("."))
            }
            add(functionToken(name))
        }
        scope.render(writer, head, returns)
    }
}

/**
 * A class or an object.
 *
 * ktlint_official wraps a class signature from its first constructor parameter, unlike a function
 * signature which waits for [FORCE_MULTILINE_PARAMETERS], so every constructor property gets a
 * line of its own. The braces only appear when there is a body to put in them.
 */
internal class ClassDeclaration(
    private val name: String,
    private val keyword: List<Token>,
    private val annotations: List<AnnotationSpec>,
    private val visibility: Visibility?,
    private val properties: List<ParameterSpec>,
    private val members: List<Declaration>,
) : Declaration {
    override val names: List<String> = listOf(name) + properties.map { it.name } + members.flatMap { it.names }

    override val symbols: List<Symbol> =
        annotations.flatMap { it.expression.symbols } +
            properties.flatMap { it.type.symbols + it.default?.symbols.orEmpty() } +
            members.flatMap { it.symbols }

    override fun render(writer: CodeWriter) {
        annotations.forEach { annotation -> writer.expression(annotation.expression) }
        val head = visibility.modifierTokens() + keyword + listOf(spaceToken, typeToken(name))
        val open = if (members.isEmpty()) emptyList() else listOf(spaceToken, punctuationToken("{"))

        if (properties.isEmpty()) {
            writer.line(head + open)
        } else {
            writer.line(head + punctuationToken("("))
            writer.indented { properties.forEach { property -> property.render(writer) } }
            writer.line(listOf(punctuationToken(")")) + open)
        }

        if (members.isNotEmpty()) {
            writer.indented {
                members.forEachIndexed { index, member ->
                    if (index > 0) writer.blankLine()
                    member.render(writer)
                }
            }
            writer.line(listOf(punctuationToken("}")))
        }
    }

    companion object {
        fun ofClass(
            name: String,
            kind: ClassKind,
            annotations: List<AnnotationSpec>,
            visibility: Visibility?,
            scope: ClassScope,
        ): ClassDeclaration {
            require(kind != ClassKind.DataClass || scope.properties.isNotEmpty()) {
                "The data class $name needs at least one property in its constructor"
            }

            return ClassDeclaration(
                name = name,
                keyword = kind.keywordTokens(),
                annotations = annotations,
                visibility = visibility,
                properties = scope.properties.toList(),
                members = scope.members.toList(),
            )
        }

        fun ofObject(
            name: String,
            annotations: List<AnnotationSpec>,
            visibility: Visibility?,
            scope: MembersScope,
        ): ClassDeclaration =
            ClassDeclaration(
                name = name,
                keyword = listOf(keywordToken("object")),
                annotations = annotations,
                visibility = visibility,
                properties = emptyList(),
                members = scope.declarations.toList(),
            )
    }
}
