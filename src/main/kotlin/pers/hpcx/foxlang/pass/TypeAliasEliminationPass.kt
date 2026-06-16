package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.mapTypes
import pers.hpcx.foxlang.utils.mapValues

sealed interface TypeAliasEliminationResult
data class TypeAliasEliminationSuccess(val newFile: FoxFile) : TypeAliasEliminationResult
data class TypeAliasEliminationFailure(val errors: List<TypeAliasEliminationError>) : TypeAliasEliminationResult

sealed interface TypeAliasEliminationError
data class TypeAliasEliminationNotFound(val referredBy: FoxMethodDefinition, val typeName: String) : TypeAliasEliminationError
data class TypeAliasEliminationGenericCountMismatch(val type: FoxType) : TypeAliasEliminationError

fun runTypeAliasElimination(file: FoxFile): TypeAliasEliminationResult {
    val errors = mutableListOf<TypeAliasEliminationError>()
    
    val aliases = file.elements.filterIsInstance<FoxTypeAlias>().associateBy { it.name }
    
    fun runTypeAliasElimination(method: FoxMethodDefinition): FoxMethodDefinition {
        val genericNames = method.generics.map { it.key }
        
        fun expandTypeAlias(type: FoxType): FoxType = type.mapTypes<FoxUnresolvedType> { unresolved ->
            if (unresolved.name in genericNames && unresolved.parameters == null) {
                return@mapTypes unresolved
            }
            val typeAlias = aliases[unresolved.name]
            if (typeAlias == null) {
                errors += TypeAliasEliminationNotFound(method, unresolved.name)
                return@mapTypes unresolved
            }
            val parameters = unresolved.parameters.orEmpty()
            if (parameters.size != typeAlias.generics.size) {
                errors += TypeAliasEliminationGenericCountMismatch(unresolved)
                return@mapTypes unresolved
            }
            val replacement = typeAlias.generics.zip(parameters.map { expandTypeAlias(it) }).toMap()
            typeAlias.alias.mapTypes<FoxUnresolvedType> { replacement.getValue(it.name) }
        }
        
        return method.mapTypes { type -> expandTypeAlias(type) }
    }
    
    val newMethods = file.elements.filterIsInstance<FoxMethodDefinition>().map { runTypeAliasElimination(it) }
    
    if (errors.isNotEmpty()) return TypeAliasEliminationFailure(errors)
    
    return TypeAliasEliminationSuccess(FoxFile(newMethods))
}

fun FoxMethodDefinition.mapTypes(transform: (FoxType) -> FoxType) = FoxMethodDefinition(
    generics.mapValues { it.value.mapTypes(transform) },
    thisType.mapTypes(transform),
    name,
    parameters.mapValues { it.value.mapTypes(transform) },
    returnType.mapTypes(transform),
    body.mapTypes(transform),
)

fun FoxStatement.mapTypes(transform: (FoxType) -> FoxType): FoxStatement = when (this) {
    FoxThis -> this
    is FoxSymbol -> this
    is FoxEntityStatement -> this
    is FoxBreak -> this
    is FoxContinue -> this
    is FoxYield -> FoxYield(label, value.mapTypes(transform))
    is FoxReturn -> FoxReturn(value?.mapTypes(transform))
    is FoxUnary -> FoxUnary(operator, right.mapTypes(transform))
    is FoxBinary -> FoxBinary(left.mapTypes(transform), operator, right.mapTypes(transform))
    is FoxTypeBinding -> FoxTypeBinding(name, transform(type))
    is FoxAssign -> FoxAssign(left, operator, right.mapTypes(transform), beforeEvaluation)
    is FoxComponentAccess -> FoxComponentAccess(target.mapTypes(transform), index)
    is FoxFieldAccess -> FoxFieldAccess(target.mapTypes(transform), name)
    is FoxFormattedString -> FoxFormattedString(
        parts.map { part ->
            when (part) {
                is FoxFormattedText -> part
                is FoxFormattedExpression -> FoxFormattedExpression(part.expression.mapTypes(transform))
            }
        },
        isRaw,
    )
    is FoxConstruct -> FoxConstruct(
        transform(type),
        parameters.map { (name, parameter) -> name to parameter.mapTypes(transform) },
    )
    is FoxCall -> FoxCall(
        target.mapTypes(transform),
        name,
        generics?.map { (name, type) -> name to transform(type) },
        parameters.map { (name, parameter) -> name to parameter.mapTypes(transform) },
    )
    is FoxIndirectCall -> FoxIndirectCall(
        target.mapTypes(transform),
        method.mapTypes(transform),
        parameters.map { it.mapTypes(transform) },
    )
    is FoxBlock -> FoxBlock(
        label,
        statements.map { it.mapTypes(transform) },
    )
    is FoxIf -> FoxIf(
        label,
        condition.mapTypes(transform),
        thenBody.mapTypes(transform),
        elseBody?.mapTypes(transform),
    )
    is FoxWhen -> FoxWhen(
        label,
        value?.mapTypes(transform),
        cases.map { FoxCase(it.conditions.map { condition -> condition.mapTypes(transform) }, it.body.mapTypes(transform)) },
    )
    is FoxWhile -> FoxWhile(label, condition.mapTypes(transform), body.mapTypes(transform))
    is FoxDoWhile -> FoxDoWhile(label, body.mapTypes(transform), condition.mapTypes(transform))
}
