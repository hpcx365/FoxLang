package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.runtime.FoxEntity

sealed interface FoxStatement

object FoxThis : FoxStatement
data class FoxSymbol(val name: String) : FoxStatement
data class FoxEntityStatement(val value: FoxEntity) : FoxStatement

data class FoxBreak(val label: String?) : FoxStatement
data class FoxContinue(val label: String?) : FoxStatement
data class FoxYield(val label: String?, val value: FoxStatement) : FoxStatement
data class FoxReturn(val value: FoxStatement?) : FoxStatement

data class FoxUnary(val operator: FoxUnaryOperator, val right: FoxStatement) : FoxStatement
data class FoxBinary(val left: FoxStatement, val operator: FoxBinaryOperator, val right: FoxStatement) : FoxStatement

data class FoxTypeBinding(val name: String, val type: FoxType) : FoxStatement
data class FoxAssign(val left: FoxStatement, val operator: FoxAssignOperator, val right: FoxStatement, val beforeEvaluation: Boolean) : FoxStatement

data class FoxComponentAccess(val target: FoxStatement, val index: Int) : FoxStatement
data class FoxFieldAccess(val target: FoxStatement, val name: String) : FoxStatement

sealed interface FoxFormattedStringPart
data class FoxFormattedText(val text: String) : FoxFormattedStringPart
data class FoxFormattedExpression(val expression: FoxStatement) : FoxFormattedStringPart
data class FoxFormattedString(val parts: List<FoxFormattedStringPart>, val isRaw: Boolean) : FoxStatement

data class FoxConstruct(
    val type: FoxType,
    val parameters: List<Pair<String?, FoxStatement>>,
) : FoxStatement

data class FoxCall(
    val target: FoxStatement,
    val name: String,
    val generics: List<Pair<String?, FoxType>>?,
    val parameters: List<Pair<String?, FoxStatement>>,
) : FoxStatement

data class FoxIndirectCall(
    val target: FoxStatement,
    val method: FoxStatement,
    val parameters: List<Pair<String?, FoxStatement>>,
) : FoxStatement

data class FoxBlock(val label: String?, val statements: List<FoxStatement>) : FoxStatement
data class FoxIf(val label: String?, val condition: FoxStatement, val thenBody: FoxStatement, val elseBody: FoxStatement?) : FoxStatement
data class FoxWhen(val label: String?, val value: FoxStatement?, val cases: List<FoxCase>) : FoxStatement
data class FoxCase(val conditions: List<FoxStatement>, val body: FoxStatement)
data class FoxWhile(val label: String?, val condition: FoxStatement, val body: FoxStatement) : FoxStatement
data class FoxDoWhile(val label: String?, val body: FoxStatement, val condition: FoxStatement) : FoxStatement
