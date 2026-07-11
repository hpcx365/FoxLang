package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.frontend.common.SourceSpan
import pers.hpcx.foxlang.runtime.*

sealed interface FoxStatement
sealed interface ParsedFoxStatement<N : FoxStatement> : Parsed<N>

object FoxThis : FoxStatement
data class ParsedFoxThis(override val span: SourceSpan) : ParsedFoxStatement<FoxThis> {
    override val node get() = FoxThis
}

data class FoxSymbol(val name: String) : FoxStatement
data class ParsedFoxSymbol(
    val name: ParsedString,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxSymbol> {
    override val node get() = FoxSymbol(name.node)
}

data class FoxEntityStatement(val value: FoxEntity) : FoxStatement
data class ParsedFoxEntityStatement(
    val value: FoxEntity,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxEntityStatement> {
    override val node get() = FoxEntityStatement(value)
}

data class ParsedFoxIntStatement(
    val value: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxEntityStatement> {
    override val node get() = FoxEntityStatement(FoxInt(value.node))
}

data class ParsedFoxLongStatement(
    val value: ParsedLong,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxEntityStatement> {
    override val node get() = FoxEntityStatement(FoxLong(value.node))
}

data class ParsedFoxFloatStatement(
    val value: ParsedFloat,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxEntityStatement> {
    override val node get() = FoxEntityStatement(FoxFloat(value.node))
}

data class ParsedFoxDoubleStatement(
    val value: ParsedDouble,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxEntityStatement> {
    override val node get() = FoxEntityStatement(FoxDouble(value.node))
}

data class FoxBreak(val label: String?) : FoxStatement
data class ParsedFoxBreak(
    val label: ParsedString?,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxBreak> {
    override val node get() = FoxBreak(label?.node)
}

data class FoxContinue(val label: String?) : FoxStatement
data class ParsedFoxContinue(
    val label: ParsedString?,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxContinue> {
    override val node get() = FoxContinue(label?.node)
}

data class FoxYield(val label: String?, val value: FoxStatement) : FoxStatement
data class ParsedFoxYield(
    val label: ParsedString?,
    val value: ParsedFoxStatement<*>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxYield> {
    override val node get() = FoxYield(label?.node, value.node)
}

data class FoxReturn(val value: FoxStatement?) : FoxStatement
data class ParsedFoxReturn(
    val value: ParsedFoxStatement<*>?,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxReturn> {
    override val node get() = FoxReturn(value?.node)
}

data class FoxUnary(val operator: FoxUnaryOperator, val right: FoxStatement) : FoxStatement
data class ParsedFoxUnary(
    val operator: ParsedFoxUnaryOperator,
    val right: ParsedFoxStatement<*>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxUnary> {
    override val node get() = FoxUnary(operator.node, right.node)
}

data class FoxBinary(val left: FoxStatement, val operator: FoxBinaryOperator, val right: FoxStatement) : FoxStatement
data class ParsedFoxBinary(
    val left: ParsedFoxStatement<*>,
    val operator: ParsedFoxBinaryOperator,
    val right: ParsedFoxStatement<*>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxBinary> {
    override val node get() = FoxBinary(left.node, operator.node, right.node)
}

data class FoxTypeBinding(val name: String, val type: FoxType) : FoxStatement
data class ParsedFoxTypeBinding(
    val name: ParsedString,
    val type: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxTypeBinding> {
    override val node get() = FoxTypeBinding(name.node, type.node)
}

data class FoxAssign(
    val left: FoxStatement,
    val operator: FoxAssignOperator,
    val right: FoxStatement,
    val beforeEvaluation: Boolean,
) : FoxStatement

data class ParsedFoxAssign(
    val left: ParsedFoxStatement<*>,
    val operator: ParsedFoxAssignOperator,
    val right: ParsedFoxStatement<*>,
    val beforeEvaluation: Boolean,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxAssign> {
    override val node get() = FoxAssign(left.node, operator.node, right.node, beforeEvaluation)
}

data class FoxFieldAccess(val target: FoxStatement, val name: String) : FoxStatement
data class ParsedFoxFieldAccess(
    val target: ParsedFoxStatement<*>,
    val name: ParsedString,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxFieldAccess> {
    override val node get() = FoxFieldAccess(target.node, name.node)
}

data class FoxIndexAccess(val target: FoxStatement, val indices: List<FoxStatement>) : FoxStatement
data class ParsedFoxIndexAccess(
    val target: ParsedFoxStatement<*>,
    val indices: ParsedList<ParsedFoxStatement<*>>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxIndexAccess> {
    override val node get() = FoxIndexAccess(target.node, indices.node.map { it.node })
}

sealed interface FoxFormattedStringPart
sealed interface ParsedFoxFormattedStringPart : Parsed<FoxFormattedStringPart>

data class FoxFormattedText(val text: String) : FoxFormattedStringPart
data class ParsedFoxFormattedText(
    val text: String,
    override val span: SourceSpan,
) : ParsedFoxFormattedStringPart {
    override val node get() = FoxFormattedText(text)
}

data class FoxFormattedExpression(val expression: FoxStatement) : FoxFormattedStringPart
data class ParsedFoxFormattedExpression(
    val expression: ParsedFoxStatement<*>,
    override val span: SourceSpan,
) : ParsedFoxFormattedStringPart {
    override val node get() = FoxFormattedExpression(expression.node)
}

data class FoxFormattedString(val parts: List<FoxFormattedStringPart>) : FoxStatement
data class ParsedFoxFormattedString(
    val parts: ParsedList<ParsedFoxFormattedStringPart>?,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxFormattedString> {
    override val node get() = FoxFormattedString(parts?.node?.map { it.node }.orEmpty())
}

data class FoxConstruct(
    val type: FoxType,
    val parameters: List<Pair<String?, FoxStatement>>,
) : FoxStatement

data class ParsedFoxConstruct(
    val type: ParsedFoxType<*>,
    val parameters: ParsedList<ParsedPair<ParsedString?, ParsedFoxStatement<*>>>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxConstruct> {
    override val node
        get() = FoxConstruct(
            type.node,
            parameters.node.map { it.node.first?.node to it.node.second.node },
        )
}

data class FoxCall(
    val target: FoxStatement,
    val name: String,
    val generics: List<Pair<String?, FoxType>>?,
    val parameters: List<Pair<String?, FoxStatement>>,
) : FoxStatement

data class ParsedFoxCall(
    val target: ParsedFoxStatement<*>?,
    val name: ParsedString,
    val generics: ParsedList<ParsedPair<ParsedString?, ParsedFoxType<*>>>?,
    val parameters: ParsedList<ParsedPair<ParsedString?, ParsedFoxStatement<*>>>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxCall> {
    override val node
        get() = FoxCall(
            target?.node ?: FoxEntityStatement(FoxUnit),
            name.node,
            generics?.node?.map { it.node.first?.node to it.node.second.node },
            parameters.node.map { it.node.first?.node to it.node.second.node },
        )
}

data class FoxIndirectCall(
    val target: FoxStatement,
    val method: FoxStatement,
    val parameters: List<Pair<String?, FoxStatement>>,
) : FoxStatement

data class ParsedFoxIndirectCall(
    val target: ParsedFoxStatement<*>?,
    val method: ParsedFoxStatement<*>,
    val parameters: ParsedList<ParsedPair<ParsedString?, ParsedFoxStatement<*>>>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxIndirectCall> {
    override val node
        get() = FoxIndirectCall(
            target?.node ?: FoxEntityStatement(FoxUnit),
            method.node,
            parameters.node.map { it.node.first?.node to it.node.second.node },
        )
}

data class FoxBlock(
    val label: String?,
    val statements: List<FoxStatement>,
) : FoxStatement

data class ParsedFoxBlock(
    val label: ParsedString?,
    val statements: ParsedList<ParsedFoxStatement<*>>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxBlock> {
    override val node
        get() = FoxBlock(
            label?.node,
            statements.node.map { it.node },
        )
}

data class FoxIf(
    val label: String?,
    val condition: FoxStatement,
    val thenBody: FoxStatement,
    val elseBody: FoxStatement?,
) : FoxStatement

data class ParsedFoxIf(
    val label: ParsedString?,
    val condition: ParsedFoxStatement<*>,
    val thenBody: ParsedFoxStatement<*>,
    val elseBody: ParsedFoxStatement<*>?,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxIf> {
    override val node
        get() = FoxIf(
            label?.node,
            condition.node,
            thenBody.node,
            elseBody?.node,
        )
}

data class FoxCase(
    val conditions: List<FoxStatement>?,
    val body: FoxStatement,
)

data class ParsedFoxCase(
    val conditions: ParsedList<ParsedFoxStatement<*>>?,
    val body: ParsedFoxStatement<*>,
    override val span: SourceSpan,
) : Parsed<FoxCase> {
    override val node
        get() = FoxCase(
            conditions?.node?.map { it.node },
            body.node,
        )
}

data class FoxWhen(
    val label: String?,
    val value: FoxStatement?,
    val cases: List<FoxCase>,
) : FoxStatement

data class ParsedFoxWhen(
    val label: ParsedString?,
    val value: ParsedFoxStatement<*>?,
    val cases: ParsedList<ParsedFoxCase>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxWhen> {
    override val node
        get() = FoxWhen(
            label?.node,
            value?.node,
            cases.node.map { it.node },
        )
}

data class FoxWhile(
    val label: String?,
    val condition: FoxStatement,
    val body: FoxStatement,
) : FoxStatement

data class ParsedFoxWhile(
    val label: ParsedString?,
    val condition: ParsedFoxStatement<*>,
    val body: ParsedFoxStatement<*>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxWhile> {
    override val node
        get() = FoxWhile(
            label?.node,
            condition.node,
            body.node,
        )
}

data class FoxDoWhile(
    val label: String?,
    val body: FoxStatement,
    val condition: FoxStatement,
) : FoxStatement

data class ParsedFoxDoWhile(
    val label: ParsedString?,
    val body: ParsedFoxStatement<*>,
    val condition: ParsedFoxStatement<*>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxDoWhile> {
    override val node
        get() = FoxDoWhile(
            label?.node,
            body.node,
            condition.node,
        )
}

data class FoxLambda(
    val parameters: List<Pair<String, FoxType?>>?,
    val body: FoxStatement,
) : FoxStatement

data class ParsedFoxLambda(
    val parameters: ParsedList<ParsedPair<ParsedString, ParsedFoxType<*>?>>?,
    val body: ParsedFoxStatement<*>,
    override val span: SourceSpan,
) : ParsedFoxStatement<FoxLambda> {
    override val node
        get() = FoxLambda(
            parameters?.node?.map { it.node.first.node to it.node.second?.node },
            body.node,
        )
}
