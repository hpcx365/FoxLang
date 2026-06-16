package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.space.*

sealed interface TypeCompileResult
data class TypeCompileSuccess(val typeSpace: TypeSpace) : TypeCompileResult
sealed interface TypeCompileError : TypeCompileResult
object NotOutermostProjectiveTransform : TypeCompileError

private inline fun TypeCompileResult.ifSuccess(block: (TraversableTypeSpace) -> TypeSpace): TypeCompileResult = when {
    this is TypeCompileSuccess && typeSpace is TraversableTypeSpace -> {
        TypeCompileSuccess(block(typeSpace))
    }
    else -> NotOutermostProjectiveTransform
}

private inline fun List<TypeCompileResult>.ifSuccess(block: (List<TraversableTypeSpace>) -> TypeSpace): TypeCompileResult = when {
    all { it is TypeCompileSuccess && it.typeSpace is TraversableTypeSpace } -> {
        TypeCompileSuccess(block(map { (it as TypeCompileSuccess).typeSpace as TraversableTypeSpace }))
    }
    else -> NotOutermostProjectiveTransform
}

fun compileType(type: FoxType, context: TypeSpaceContext): TypeCompileResult = when (type) {
    is FoxPrimitiveType -> TypeCompileSuccess(singleSpace(type))
    is FoxBuiltInType -> compileBuiltInType(type, context)
    is FoxWildcardType -> compileWildcardType(type, context)
    is FoxTransformType -> compileTransformType(type, context)
    is FoxUnresolvedType -> error("Unresolved type compilation is not implemented yet: $type")
    is FoxPlaceholderType -> error("Placeholder type cannot be compiled")
}

private fun compileBuiltInType(type: FoxBuiltInType, context: TypeSpaceContext): TypeCompileResult = when (type) {
    is FoxTupleType -> (0 until type.arity).map { compileType(type.componentAt(it), context) }.ifSuccess { tupleProduct(it) }
    is FoxStructType -> error("Struct type compilation is not implemented yet: $type")
    is FoxObjectType -> error("Object type compilation is not implemented yet: $type")
    is FoxEnumType -> error("Enum type compilation is not implemented yet: $type")
    is FoxArrayType -> compileType(type.element, context).ifSuccess { array(it) }
    is FoxRefType -> compileType(type.referent, context).ifSuccess { ref(it) }
    is FoxMethodType -> error("Method type compilation is not implemented yet: $type")
}

private fun compileWildcardType(type: FoxWildcardType, context: TypeSpaceContext): TypeCompileResult = when (type) {
    FoxAnyType -> TypeCompileSuccess(universe(context.bounds.maxHeight))
    is FoxAnyOfType -> type.types.map { compileType(it, context) }.ifSuccess { union(it) }
    is FoxAllOfType -> type.types.map { compileType(it, context) }.ifSuccess { intersect(it) }
    is FoxNoneOfType -> type.types.map { compileType(it, context) }.ifSuccess { subtract(universe(context.bounds.maxHeight), union(it)) }
    FoxAnyTupleType -> TypeCompileSuccess(tupleRepeat(universe(context.bounds.maxHeight), 0, context.bounds.maxTupleArity))
    is FoxAnyTupleOfType -> compileType(type.component, context).ifSuccess { tupleRepeat(it, 0, context.bounds.maxTupleArity) }
    else -> error("Wildcard type compilation is not implemented yet: $type")
}

private fun compileTransformType(type: FoxTransformType, context: TypeSpaceContext): TypeCompileResult = when (type) {
    is FoxTupleComponentAtType -> compileType(type.type, context).ifSuccess { tupleComponentAt(it, type.index) }
    is FoxTupleLastComponentAtType -> compileType(type.type, context).ifSuccess { tupleLastComponentAt(it, type.index) }
    is FoxTupleFirstComponentsOfType -> compileType(type.type, context).ifSuccess { tupleFirstOf(it, type.count, exact = false) }
    is FoxTupleExactFirstComponentsOfType -> compileType(type.type, context).ifSuccess { tupleFirstOf(it, type.count, exact = true) }
    is FoxTupleLastComponentsOfType -> compileType(type.type, context).ifSuccess { tupleLastOf(it, type.count, exact = false) }
    is FoxTupleExactLastComponentsOfType -> compileType(type.type, context).ifSuccess { tupleLastOf(it, type.count, exact = true) }
    is FoxTupleDropFirstComponentsOfType -> compileType(type.type, context).ifSuccess { tupleDropFirstOf(it, type.count, exact = false) }
    is FoxTupleExactDropFirstComponentsOfType -> compileType(type.type, context).ifSuccess { tupleDropFirstOf(it, type.count, exact = true) }
    is FoxTupleDropLastComponentsOfType -> compileType(type.type, context).ifSuccess { tupleDropLastOf(it, type.count, exact = false) }
    is FoxTupleExactDropLastComponentsOfType -> compileType(type.type, context).ifSuccess { tupleDropLastOf(it, type.count, exact = true) }
    is FoxTupleMergeComponentsOfType -> type.types.map { compileType(it, context) }.ifSuccess { tupleConcat(it) }
    is FoxArrayElementOfType -> compileType(type.type, context).ifSuccess { arrayElementOf(it) }
    is FoxRefReferentOfType -> compileType(type.type, context).ifSuccess { refReferentOf(it) }
    else -> error("Transform type compilation is not implemented yet: $type")
}
