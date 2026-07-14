package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.type.mapTypes
import pers.hpcx.foxlang.utils.mapValues

sealed interface TypeAliasEliminationResult
data class TypeAliasEliminationSuccess(val newFile: SurfaceFile) : TypeAliasEliminationResult
data class TypeAliasEliminationFailure(val errors: List<TypeAliasEliminationError>) : TypeAliasEliminationResult

sealed interface TypeAliasEliminationError
data class TypeAliasEliminationNotFound(val referredBy: SurfaceMethodDefinition, val typeName: String) : TypeAliasEliminationError
data class TypeAliasEliminationGenericCountMismatch(val type: SurfaceType) : TypeAliasEliminationError

fun runTypeAliasElimination(file: SurfaceFile) = TypeAliasEliminationContext(file).run()

private class TypeAliasEliminationContext(
    private val file: SurfaceFile,
) {
    
    private val errors = mutableListOf<TypeAliasEliminationError>()
    private val aliases = file.elements.filterIsInstance<SurfaceTypeAlias>().associateBy { it.name }
    
    fun run(): TypeAliasEliminationResult {
        val newMethods = file.elements.filterIsInstance<SurfaceMethodDefinition>().map { eliminateMethod(it) }
        if (errors.isNotEmpty()) return TypeAliasEliminationFailure(errors)
        return TypeAliasEliminationSuccess(SurfaceFile(newMethods))
    }
    
    private fun eliminateMethod(method: SurfaceMethodDefinition): SurfaceMethodDefinition {
        val genericNames = method.generics.map { it.key }
        return method.mapTypes { type -> expandTypeAlias(method, genericNames, type) }
    }
    
    private fun expandTypeAlias(
        method: SurfaceMethodDefinition,
        genericNames: List<String>,
        type: SurfaceType,
    ): SurfaceType = type.mapTypes<SurfaceUnresolvedType> { unresolved ->
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
        val replacement = typeAlias.generics.zip(parameters.map { expandTypeAlias(method, genericNames, it) }).toMap()
        typeAlias.alias.mapTypes<SurfaceUnresolvedType> { replacement.getValue(it.name) }
    }
}

fun SurfaceMethodDefinition.mapTypes(transform: (SurfaceType) -> SurfaceType) = SurfaceMethodDefinition(
    generics.mapValues { it.value.mapTypes(transform) },
    thisType.mapTypes(transform),
    name,
    parameters.mapValues { it.value.mapTypes(transform) },
    returnType.mapTypes(transform),
    body.mapTypes(transform),
)

fun SurfaceStatement.mapTypes(transform: (SurfaceType) -> SurfaceType): SurfaceStatement = when (this) {
    SurfaceThis -> this
    is SurfaceUnresolvedSymbol -> this
    is SurfaceEntityStatement -> this
    is SurfaceBreak -> this
    is SurfaceContinue -> this
    is SurfaceYield -> SurfaceYield(label, value.mapTypes(transform))
    is SurfaceReturn -> SurfaceReturn(value?.mapTypes(transform))
    is SurfaceUnary -> SurfaceUnary(operator, right.mapTypes(transform))
    is SurfaceBinary -> SurfaceBinary(left.mapTypes(transform), operator, right.mapTypes(transform))
    is SurfaceTypeBinding -> SurfaceTypeBinding(name, transform(type))
    is SurfaceAssign -> SurfaceAssign(left, operator, right.mapTypes(transform), beforeEvaluation)
    is SurfaceFieldAccess -> SurfaceFieldAccess(target.mapTypes(transform), name)
    is SurfaceIndexAccess -> SurfaceIndexAccess(target.mapTypes(transform), indices.map { it.mapTypes(transform) })
    is SurfaceFormattedString -> SurfaceFormattedString(
        parts.map { part ->
            when (part) {
                is FoxFormattedText -> part
                is FoxFormattedExpression -> FoxFormattedExpression(part.expression.mapTypes(transform))
            }
        },
    )
    is SurfaceLambda -> SurfaceLambda(
        parameters?.map { (name, type) -> name to type?.mapTypes(transform) },
        body.mapTypes(transform),
    )
    is SurfaceConstruct -> SurfaceConstruct(
        transform(type),
        parameters.map { (name, parameter) -> name to parameter.mapTypes(transform) },
    )
    is SurfaceCall -> SurfaceCall(
        target.mapTypes(transform),
        name,
        generics?.map { (name, type) -> name to transform(type) },
        parameters.map { (name, parameter) -> name to parameter.mapTypes(transform) },
    )
    is SurfaceIndirectCall -> SurfaceIndirectCall(
        target.mapTypes(transform),
        method.mapTypes(transform),
        parameters.map { (name, parameter) -> name to parameter.mapTypes(transform) },
    )
    is SurfaceBlock -> SurfaceBlock(
        label,
        statements.map { it.mapTypes(transform) },
    )
    is SurfaceIf -> SurfaceIf(
        label,
        condition.mapTypes(transform),
        thenBody.mapTypes(transform),
        elseBody?.mapTypes(transform),
    )
    is SurfaceWhen -> SurfaceWhen(
        label,
        value?.mapTypes(transform),
        cases.map { FoxCase(it.conditions?.map { condition -> condition.mapTypes(transform) }, it.body.mapTypes(transform)) },
    )
    is SurfaceWhile -> SurfaceWhile(label, condition.mapTypes(transform), body.mapTypes(transform))
    is SurfaceDoWhile -> SurfaceDoWhile(label, body.mapTypes(transform), condition.mapTypes(transform))
}
