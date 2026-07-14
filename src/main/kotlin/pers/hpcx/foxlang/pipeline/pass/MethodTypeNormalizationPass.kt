package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.type.mapTypes
import pers.hpcx.foxlang.type.visitTypes
import pers.hpcx.foxlang.utils.mapValues

sealed interface MethodTypeNormalizationResult
data class MethodTypeNormalizationSuccess(val newFile: SurfaceFile) : MethodTypeNormalizationResult
data class MethodTypeNormalizationFailure(val errors: List<MethodTypeNormalizationError>) : MethodTypeNormalizationResult

sealed interface MethodTypeNormalizationError

data class MethodTypeNormalizationWildcardNotAllowed(
    val method: SurfaceMethodDefinition,
    val type: SurfaceWildcardType,
) : MethodTypeNormalizationError

data class MethodTypeNormalizationTransformNotAllowed(
    val method: SurfaceMethodDefinition,
    val type: SurfaceTransformType,
) : MethodTypeNormalizationError

data class MethodTypeNormalizationFailedError(
    val method: SurfaceMethodDefinition,
    val error: TypeNormalizationError,
) : MethodTypeNormalizationError

data class MethodTypeNormalizationUnknownGenericReference(
    val method: SurfaceMethodDefinition,
    val type: SurfaceUnresolvedType,
) : MethodTypeNormalizationError

fun runMethodTypeNormalization(file: SurfaceFile) = MethodTypeNormalizationContext().run(file)

private class MethodTypeNormalizationContext {
    
    private val errors = mutableListOf<MethodTypeNormalizationError>()
    
    fun run(file: SurfaceFile): MethodTypeNormalizationResult {
        val newElements = file.elements.map { element ->
            when (element) {
                is SurfaceTypeAlias -> error("unreachable")
                is SurfaceMethodDefinition -> {
                    element.generics.values.forEach { checkGenericReferences(element, it) }
                    SurfaceMethodDefinition(
                        element.generics,
                        normalizeType(element, element.thisType),
                        element.name,
                        element.parameters.mapValues { normalizeType(element, it.value) },
                        normalizeType(element, element.returnType),
                        normalizeStatement(element, element.body),
                    )
                }
            }
        }
        
        if (errors.isNotEmpty()) return MethodTypeNormalizationFailure(errors)
        return MethodTypeNormalizationSuccess(SurfaceFile(newElements))
    }
    
    private fun normalizeType(method: SurfaceMethodDefinition, type: SurfaceType): SurfaceType {
        checkGenericReferences(method, type)
        
        type.visitTypes<SurfaceWildcardType> { wildcard ->
            errors += MethodTypeNormalizationWildcardNotAllowed(method, wildcard)
        }
        
        var normalizationFailed = false
        val normalizedType = type.mapTypes<SurfaceType> { currentType ->
            when (val result = runTypeNormalization(currentType)) {
                is TypeNormalizationSuccess -> result.type
                is TypeNormalizationFailure -> {
                    normalizationFailed = true
                    errors += result.errors.map { MethodTypeNormalizationFailedError(method, it) }
                    currentType
                }
            }
        }
        if (!normalizationFailed) {
            normalizedType.visitTypes<SurfaceTransformType> { transform ->
                errors += MethodTypeNormalizationTransformNotAllowed(method, transform)
            }
        }
        return normalizedType
    }
    
    private fun checkGenericReferences(method: SurfaceMethodDefinition, type: SurfaceType) {
        type.visitTypes<SurfaceUnresolvedType> { unresolved ->
            check(unresolved.parameters == null)
            if (unresolved.name !in method.generics) {
                errors += MethodTypeNormalizationUnknownGenericReference(method, unresolved)
            }
        }
    }
    
    private fun normalizeStatement(method: SurfaceMethodDefinition, statement: SurfaceStatement): SurfaceStatement = when (statement) {
        SurfaceThis -> statement
        is SurfaceUnresolvedSymbol -> statement
        is SurfaceEntityStatement -> statement
        is SurfaceBreak -> statement
        is SurfaceContinue -> statement
        is SurfaceYield -> SurfaceYield(statement.label, normalizeStatement(method, statement.value))
        is SurfaceReturn -> SurfaceReturn(statement.value?.let { normalizeStatement(method, it) })
        is SurfaceUnary -> SurfaceUnary(statement.operator, normalizeStatement(method, statement.right))
        is SurfaceBinary -> SurfaceBinary(
            normalizeStatement(method, statement.left),
            statement.operator,
            normalizeStatement(method, statement.right),
        )
        is SurfaceTypeBinding -> SurfaceTypeBinding(statement.name, normalizeType(method, statement.type))
        is SurfaceAssign -> SurfaceAssign(statement.left, statement.operator, normalizeStatement(method, statement.right), statement.beforeEvaluation)
        is SurfaceFieldAccess -> SurfaceFieldAccess(normalizeStatement(method, statement.target), statement.name)
        is SurfaceIndexAccess -> SurfaceIndexAccess(
            normalizeStatement(method, statement.target),
            statement.indices.map { normalizeStatement(method, it) },
        )
        is SurfaceFormattedString -> SurfaceFormattedString(
            statement.parts.map { part ->
                when (part) {
                    is FoxFormattedText -> part
                    is FoxFormattedExpression -> FoxFormattedExpression(normalizeStatement(method, part.expression))
                }
            },
        )
        is SurfaceLambda -> SurfaceLambda(
            statement.parameters?.map { (name, type) -> name to type?.let { normalizeType(method, it) } },
            normalizeStatement(method, statement.body),
        )
        is SurfaceConstruct -> SurfaceConstruct(
            normalizeType(method, statement.type),
            statement.parameters.map { (name, value) -> name to normalizeStatement(method, value) },
        )
        is SurfaceCall -> SurfaceCall(
            normalizeStatement(method, statement.target),
            statement.name,
            statement.generics?.map { (name, type) -> name to normalizeType(method, type) },
            statement.parameters.map { (name, value) -> name to normalizeStatement(method, value) },
        )
        is SurfaceIndirectCall -> SurfaceIndirectCall(
            normalizeStatement(method, statement.target),
            normalizeStatement(method, statement.method),
            statement.parameters.map { (name, value) -> name to normalizeStatement(method, value) },
        )
        is SurfaceBlock -> SurfaceBlock(statement.label, statement.statements.map { normalizeStatement(method, it) })
        is SurfaceIf -> SurfaceIf(
            statement.label,
            normalizeStatement(method, statement.condition),
            normalizeStatement(method, statement.thenBody),
            statement.elseBody?.let { normalizeStatement(method, it) },
        )
        is SurfaceWhen -> SurfaceWhen(
            statement.label,
            statement.value?.let { normalizeStatement(method, it) },
            statement.cases.map { case ->
                FoxCase(
                    case.conditions?.map { normalizeStatement(method, it) },
                    normalizeStatement(method, case.body),
                )
            },
        )
        is SurfaceWhile -> SurfaceWhile(
            statement.label,
            normalizeStatement(method, statement.condition),
            normalizeStatement(method, statement.body),
        )
        is SurfaceDoWhile -> SurfaceDoWhile(
            statement.label,
            normalizeStatement(method, statement.body),
            normalizeStatement(method, statement.condition),
        )
    }
}
