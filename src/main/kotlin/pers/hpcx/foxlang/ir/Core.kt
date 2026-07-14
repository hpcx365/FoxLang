package pers.hpcx.foxlang.ir

import pers.hpcx.foxlang.runtime.FoxEntity
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.WithInstanceId

sealed class CoreNode : WithInstanceId()

data class CoreProgram(
    val methods: List<CoreMethod>,
    val objectAccessors: Set<CoreObjectAccessorRequest>,
) : CoreNode()

data class CoreMethod(
    val generics: OrderedMap<String, SurfaceType>,
    val thisType: SurfaceType,
    val name: String,
    val parameters: OrderedMap<String, SurfaceType>,
    val returnType: SurfaceType,
    val body: CoreBlock,
) : CoreNode()

data class CoreObjectAccessorRequest(
    val objectType: SurfaceObjectType,
    val name: String,
    val type: SurfaceType,
) : CoreNode()

data class CoreBlock(
    val label: String?,
    val statements: List<CoreStatement>,
) : CoreNode()

data class CoreValueBlock(
    val statements: List<CoreStatement>,
    val value: CoreValue,
) : CoreNode()

sealed class CoreValue : CoreNode()

data object CoreThis : CoreValue()
data class CoreSymbol(val name: String) : CoreValue()
data class CoreTemp(val tempId: Long) : CoreValue()
data class CoreConst(val value: FoxEntity) : CoreValue()

sealed class CoreExpression : CoreNode()

data class CoreMethodCall(
    val target: CoreValue,
    val name: String,
    val generics: List<Pair<String?, SurfaceType>>?,
    val arguments: List<Pair<String?, CoreValue>>,
) : CoreExpression()

data class CoreIndirectCall(
    val target: CoreValue,
    val method: CoreValue,
    val arguments: List<Pair<String?, CoreValue>>,
) : CoreExpression()

data class CoreConstruct(
    val type: SurfaceType,
    val arguments: List<Pair<String?, CoreValue>>,
) : CoreExpression()

data class CoreLambda(
    val parameters: List<Pair<String, SurfaceType?>>?,
    val body: CoreBlock,
) : CoreExpression()

sealed class CoreFormattedStringPart : CoreNode()
data class CoreFormattedText(val text: String) : CoreFormattedStringPart()
data class CoreFormattedExpression(val value: CoreValue) : CoreFormattedStringPart()

data class CoreFormattedString(
    val parts: List<CoreFormattedStringPart>,
) : CoreExpression()

enum class CoreShortCircuitOperator {
    And,
    Or,
}

data class CoreShortCircuit(
    val operator: CoreShortCircuitOperator,
    val left: CoreValueBlock,
    val right: CoreValueBlock,
) : CoreExpression()

data class CoreIfExpression(
    val label: String?,
    val condition: CoreValueBlock,
    val thenBranch: CoreValueBlock,
    val elseBranch: CoreValueBlock,
) : CoreExpression()

data class CoreWhenExpression(
    val label: String?,
    val value: CoreValueBlock?,
    val cases: List<CoreWhenValueCase>,
) : CoreExpression()

data class CoreWhenValueCase(
    val conditions: List<CoreValueBlock>?,
    val body: CoreValueBlock,
)

data class CoreBlockExpression(
    val block: CoreBlock,
) : CoreExpression()

sealed class CoreStatement : CoreNode()

data class CoreLet(
    val target: CoreTemp,
    val expression: CoreExpression,
) : CoreStatement()

data class CoreDefineSymbol(
    val name: String,
    val value: CoreValue,
) : CoreStatement()

data class CoreAssignSymbol(
    val name: String,
    val value: CoreValue,
) : CoreStatement()

data class CoreTypeBinding(
    val name: String,
    val type: SurfaceType,
) : CoreStatement()

data class CoreEvaluate(
    val value: CoreValue,
) : CoreStatement()

data class CoreReturn(
    val value: CoreValue?,
) : CoreStatement()

data class CoreYield(
    val label: String?,
    val value: CoreValue,
) : CoreStatement()

data class CoreBreak(
    val label: String?,
) : CoreStatement()

data class CoreContinue(
    val label: String?,
) : CoreStatement()

data class CoreBlockStatement(
    val block: CoreBlock,
) : CoreStatement()

data class CoreIfStatement(
    val label: String?,
    val condition: CoreValueBlock,
    val thenBranch: CoreBlock,
    val elseBranch: CoreBlock?,
) : CoreStatement()

data class CoreWhenStatement(
    val label: String?,
    val value: CoreValueBlock?,
    val cases: List<CoreWhenStatementCase>,
) : CoreStatement()

data class CoreWhenStatementCase(
    val conditions: List<CoreValueBlock>?,
    val body: CoreBlock,
)

data class CoreWhileStatement(
    val label: String?,
    val condition: CoreValueBlock,
    val body: CoreBlock,
) : CoreStatement()

data class CoreDoWhileStatement(
    val label: String?,
    val body: CoreBlock,
    val condition: CoreValueBlock,
) : CoreStatement()
