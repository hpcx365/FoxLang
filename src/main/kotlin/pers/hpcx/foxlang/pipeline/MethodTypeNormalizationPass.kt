package pers.hpcx.foxlang.pipeline

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.mapTypes
import pers.hpcx.foxlang.utils.mapValues

sealed interface MethodTypeNormalizationResult
data class MethodTypeNormalizationSuccess(val newFile: FoxFile) : MethodTypeNormalizationResult
data class MethodTypeNormalizationFailure(val errors: List<MethodTypeNormalizationError>) : MethodTypeNormalizationResult

data class MethodTypeNormalizationError(
    val method: FoxMethodDefinition,
    val error: TypeNormalizationError,
)

fun runMethodTypeNormalization(file: FoxFile): MethodTypeNormalizationResult {
    val errors = mutableListOf<MethodTypeNormalizationError>()
    
    fun normalizeType(method: FoxMethodDefinition, type: FoxType): FoxType = type.mapTypes<FoxType> { currentType ->
        when (val result = runTypeNormalization(currentType)) {
            is TypeNormalizationSuccess -> result.type
            is TypeNormalizationFailure -> {
                errors += result.errors.map { MethodTypeNormalizationError(method, it) }
                currentType
            }
        }
    }
    
    fun normalizeStatement(method: FoxMethodDefinition, statement: FoxStatement): FoxStatement = when (statement) {
        FoxThis -> statement
        is FoxSymbol -> statement
        is FoxEntityStatement -> statement
        is FoxBreak -> statement
        is FoxContinue -> statement
        is FoxYield -> FoxYield(statement.label, normalizeStatement(method, statement.value))
        is FoxReturn -> FoxReturn(statement.value?.let { normalizeStatement(method, it) })
        is FoxUnary -> FoxUnary(statement.operator, normalizeStatement(method, statement.right))
        is FoxBinary -> FoxBinary(
            normalizeStatement(method, statement.left),
            statement.operator,
            normalizeStatement(method, statement.right),
        )
        is FoxTypeBinding -> FoxTypeBinding(statement.name, normalizeType(method, statement.type))
        is FoxAssign -> FoxAssign(statement.left, statement.operator, normalizeStatement(method, statement.right), statement.beforeEvaluation)
        is FoxFieldAccess -> FoxFieldAccess(normalizeStatement(method, statement.target), statement.name)
        is FoxIndexAccess -> FoxIndexAccess(
            normalizeStatement(method, statement.target),
            statement.indices.map { normalizeStatement(method, it) },
        )
        is FoxFormattedString -> FoxFormattedString(
            statement.parts.map { part ->
                when (part) {
                    is FoxFormattedText -> part
                    is FoxFormattedExpression -> FoxFormattedExpression(normalizeStatement(method, part.expression))
                }
            },
        )
        is FoxLambda -> FoxLambda(
            statement.parameters?.map { (name, type) -> name to type?.let { normalizeType(method, it) } },
            normalizeStatement(method, statement.body),
        )
        is FoxConstruct -> FoxConstruct(
            normalizeType(method, statement.type),
            statement.parameters.map { (name, value) -> name to normalizeStatement(method, value) },
        )
        is FoxCall -> FoxCall(
            normalizeStatement(method, statement.target),
            statement.name,
            statement.generics?.map { (name, type) -> name to normalizeType(method, type) },
            statement.parameters.map { (name, value) -> name to normalizeStatement(method, value) },
        )
        is FoxIndirectCall -> FoxIndirectCall(
            normalizeStatement(method, statement.target),
            normalizeStatement(method, statement.method),
            statement.parameters.map { (name, value) -> name to normalizeStatement(method, value) },
        )
        is FoxBlock -> FoxBlock(statement.label, statement.statements.map { normalizeStatement(method, it) })
        is FoxIf -> FoxIf(
            statement.label,
            normalizeStatement(method, statement.condition),
            normalizeStatement(method, statement.thenBody),
            statement.elseBody?.let { normalizeStatement(method, it) },
        )
        is FoxWhen -> FoxWhen(
            statement.label,
            statement.value?.let { normalizeStatement(method, it) },
            statement.cases.map { case ->
                FoxCase(
                    case.conditions?.map { normalizeStatement(method, it) },
                    normalizeStatement(method, case.body),
                )
            },
        )
        is FoxWhile -> FoxWhile(
            statement.label,
            normalizeStatement(method, statement.condition),
            normalizeStatement(method, statement.body),
        )
        is FoxDoWhile -> FoxDoWhile(
            statement.label,
            normalizeStatement(method, statement.body),
            normalizeStatement(method, statement.condition),
        )
    }
    
    val newElements = file.elements.map { element ->
        when (element) {
            is FoxTypeAlias -> error("unreachable")
            is FoxMethodDefinition -> FoxMethodDefinition(
                element.generics,
                normalizeType(element, element.thisType),
                element.name,
                element.parameters.mapValues { normalizeType(element, it.value) },
                normalizeType(element, element.returnType),
                normalizeStatement(element, element.body),
            )
        }
    }
    
    if (errors.isNotEmpty()) return MethodTypeNormalizationFailure(errors)
    return MethodTypeNormalizationSuccess(FoxFile(newElements))
}
