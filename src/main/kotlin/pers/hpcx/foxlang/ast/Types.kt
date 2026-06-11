package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet
import pers.hpcx.foxlang.utils.orderedMapOf

sealed interface FoxType

sealed interface FoxPrimitiveType : FoxType
object FoxVoidType : FoxPrimitiveType
object FoxUnitType : FoxPrimitiveType
object FoxBoolType : FoxPrimitiveType
object FoxByteType : FoxPrimitiveType
object FoxShortType : FoxPrimitiveType
object FoxIntType : FoxPrimitiveType
object FoxLongType : FoxPrimitiveType
object FoxFloatType : FoxPrimitiveType
object FoxDoubleType : FoxPrimitiveType
object FoxCharType : FoxPrimitiveType
object FoxStringType : FoxPrimitiveType

sealed interface FoxWildcardType : FoxType
object FoxAnyType : FoxWildcardType
object FoxAnyTupleType : FoxWildcardType
object FoxAnyStructType : FoxWildcardType
object FoxAnyObjectType : FoxWildcardType
object FoxAnyEnumType : FoxWildcardType
object FoxAnyArrayType : FoxWildcardType
object FoxAnyRefType : FoxWildcardType
object FoxAnyMethodType : FoxWildcardType

sealed interface FoxBuiltInType : FoxType
data class FoxTupleType(val components: List<Pair<FoxType, Int>>) : FoxBuiltInType
data class FoxStructType(val fields: OrderedMap<String, FoxType>) : FoxBuiltInType
data class FoxObjectType(val members: Map<String, FoxType>) : FoxBuiltInType
data class FoxEnumType(val items: Map<String, FoxType>) : FoxBuiltInType
data class FoxArrayType(val element: FoxType) : FoxBuiltInType
data class FoxRefType(val referent: FoxType) : FoxBuiltInType
data class FoxMethodType(val `this`: FoxType, val parameters: OrderedMap<String, FoxType>, val `return`: FoxType) : FoxBuiltInType

sealed interface FoxTransformType : FoxType
data class FoxTupleComponentAtType(val type: FoxType, val index: Int) : FoxTransformType
data class FoxTupleLastComponentAtType(val type: FoxType, val index: Int) : FoxTransformType
data class FoxTupleFirstComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleLastComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleDropFirstComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleDropLastComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleMergeComponentsOfType(val types: List<FoxType>) : FoxTransformType
data class FoxStructFieldOfType(val type: FoxType, val name: String) : FoxTransformType
data class FoxStructFieldAtType(val type: FoxType, val index: Int) : FoxTransformType
data class FoxStructLastFieldAtType(val type: FoxType, val index: Int) : FoxTransformType
data class FoxStructFirstFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxStructLastFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxStructDropFirstFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxStructDropLastFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxStructFieldsOfType(val type: FoxType, val names: OrderedSet<String>) : FoxTransformType
data class FoxStructDropFieldsOfType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class FoxStructMergeFieldsOfType(val types: List<FoxType>) : FoxTransformType
data class FoxObjectMemberOfType(val type: FoxType, val name: String) : FoxTransformType
data class FoxObjectMembersOfType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class FoxObjectDropMembersOfType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class FoxObjectMergeMembersOfType(val types: List<FoxType>) : FoxTransformType
data class FoxEnumItemOfType(val type: FoxType, val name: String) : FoxTransformType
data class FoxEnumItemsOfType(val type: FoxType, val names: List<String>) : FoxTransformType
data class FoxEnumDropItemsOfType(val type: FoxType, val names: List<String>) : FoxTransformType
data class FoxEnumMergeItemsOfType(val types: List<FoxType>) : FoxTransformType
data class FoxArrayElementOfType(val type: FoxType) : FoxTransformType
data class FoxRefReferentOfType(val type: FoxType) : FoxTransformType
data class FoxMethodThisOfType(val type: FoxType) : FoxTransformType
data class FoxMethodParametersOfType(val type: FoxType) : FoxTransformType
data class FoxMethodReturnOfType(val type: FoxType) : FoxTransformType

data class FoxUnresolvedType(val name: String, val parameters: List<FoxType>?) : FoxType

interface FoxPlaceholderType : FoxType

fun FoxType.isConcrete(): Boolean = when (this) {
    is FoxPrimitiveType -> true
    is FoxBuiltInType -> nestedTypes().all { it.isConcrete() }
    is FoxWildcardType,
    is FoxTransformType,
    is FoxUnresolvedType,
    is FoxPlaceholderType,
        -> false
}

fun FoxType.isActual(): Boolean = when (this) {
    is FoxPrimitiveType -> true
    is FoxBuiltInType -> nestedTypes().all { it.isActual() }
    is FoxTransformType -> nestedTypes().all { it.isActual() }
    is FoxWildcardType,
    is FoxUnresolvedType,
    is FoxPlaceholderType,
        -> false
}

fun FoxBuiltInType.nestedTypes(): List<FoxType> = when (this) {
    is FoxTupleType -> components.map { it.first }
    is FoxStructType -> fields.values
    is FoxObjectType -> members.values.toList()
    is FoxEnumType -> items.values.toList()
    is FoxArrayType -> listOf(element)
    is FoxRefType -> listOf(referent)
    is FoxMethodType -> listOf(`this`) + parameters.values + listOf(`return`)
}

fun FoxBuiltInType.rebuildWith(types: List<FoxType>): FoxBuiltInType {
    fun <T, R> Collection<T>.checkedZip(other: Collection<R>): List<Pair<T, R>> {
        check(size == other.size) { "Collection size mismatch" }
        return zip(other)
    }
    return when (this) {
        is FoxTupleType -> FoxTupleType(types.checkedZip(components.map { it.second }))
        is FoxStructType -> FoxStructType(orderedMapOf(*fields.keys.elements.checkedZip(types).toTypedArray()))
        is FoxObjectType -> FoxObjectType(mapOf(*members.keys.checkedZip(types).toTypedArray()))
        is FoxEnumType -> FoxEnumType(mapOf(*items.keys.checkedZip(types).toTypedArray()))
        is FoxArrayType -> FoxArrayType(types.single())
        is FoxRefType -> FoxRefType(types.single())
        is FoxMethodType -> FoxMethodType(
            types.first(),
            orderedMapOf(*parameters.keys.elements.checkedZip(types.subList(1, types.size - 1)).toTypedArray()),
            types.last(),
        )
    }
}

fun FoxTransformType.nestedTypes(): List<FoxType> = when (this) {
    is FoxTupleComponentAtType -> listOf(type)
    is FoxTupleLastComponentAtType -> listOf(type)
    is FoxTupleFirstComponentsOfType -> listOf(type)
    is FoxTupleLastComponentsOfType -> listOf(type)
    is FoxTupleDropFirstComponentsOfType -> listOf(type)
    is FoxTupleDropLastComponentsOfType -> listOf(type)
    is FoxTupleMergeComponentsOfType -> types
    is FoxStructFieldOfType -> listOf(type)
    is FoxStructFieldAtType -> listOf(type)
    is FoxStructLastFieldAtType -> listOf(type)
    is FoxStructFirstFieldsOfType -> listOf(type)
    is FoxStructLastFieldsOfType -> listOf(type)
    is FoxStructDropFirstFieldsOfType -> listOf(type)
    is FoxStructDropLastFieldsOfType -> listOf(type)
    is FoxStructFieldsOfType -> listOf(type)
    is FoxStructDropFieldsOfType -> listOf(type)
    is FoxStructMergeFieldsOfType -> types
    is FoxObjectMemberOfType -> listOf(type)
    is FoxObjectMembersOfType -> listOf(type)
    is FoxObjectDropMembersOfType -> listOf(type)
    is FoxObjectMergeMembersOfType -> types
    is FoxEnumItemOfType -> listOf(type)
    is FoxEnumItemsOfType -> listOf(type)
    is FoxEnumDropItemsOfType -> listOf(type)
    is FoxEnumMergeItemsOfType -> types
    is FoxArrayElementOfType -> listOf(type)
    is FoxRefReferentOfType -> listOf(type)
    is FoxMethodThisOfType -> listOf(type)
    is FoxMethodParametersOfType -> listOf(type)
    is FoxMethodReturnOfType -> listOf(type)
}

fun FoxTransformType.rebuildWith(types: List<FoxType>) = when (this) {
    is FoxTupleComponentAtType -> FoxTupleComponentAtType(types.single(), index)
    is FoxTupleLastComponentAtType -> FoxTupleLastComponentAtType(types.single(), index)
    is FoxTupleFirstComponentsOfType -> FoxTupleFirstComponentsOfType(types.single(), count)
    is FoxTupleLastComponentsOfType -> FoxTupleLastComponentsOfType(types.single(), count)
    is FoxTupleDropFirstComponentsOfType -> FoxTupleDropFirstComponentsOfType(types.single(), count)
    is FoxTupleDropLastComponentsOfType -> FoxTupleDropLastComponentsOfType(types.single(), count)
    is FoxTupleMergeComponentsOfType -> FoxTupleMergeComponentsOfType(types)
    is FoxStructFieldOfType -> FoxStructFieldOfType(types.single(), name)
    is FoxStructFieldAtType -> FoxStructFieldAtType(types.single(), index)
    is FoxStructLastFieldAtType -> FoxStructLastFieldAtType(types.single(), index)
    is FoxStructFirstFieldsOfType -> FoxStructFirstFieldsOfType(types.single(), count)
    is FoxStructLastFieldsOfType -> FoxStructLastFieldsOfType(types.single(), count)
    is FoxStructDropFirstFieldsOfType -> FoxStructDropFirstFieldsOfType(types.single(), count)
    is FoxStructDropLastFieldsOfType -> FoxStructDropLastFieldsOfType(types.single(), count)
    is FoxStructFieldsOfType -> FoxStructFieldsOfType(types.single(), names)
    is FoxStructDropFieldsOfType -> FoxStructDropFieldsOfType(types.single(), names)
    is FoxStructMergeFieldsOfType -> FoxStructMergeFieldsOfType(types)
    is FoxObjectMemberOfType -> FoxObjectMemberOfType(types.single(), name)
    is FoxObjectMembersOfType -> FoxObjectMembersOfType(types.single(), names)
    is FoxObjectDropMembersOfType -> FoxObjectDropMembersOfType(types.single(), names)
    is FoxObjectMergeMembersOfType -> FoxObjectMergeMembersOfType(types)
    is FoxEnumItemOfType -> FoxEnumItemOfType(types.single(), name)
    is FoxEnumItemsOfType -> FoxEnumItemsOfType(types.single(), names)
    is FoxEnumDropItemsOfType -> FoxEnumDropItemsOfType(types.single(), names)
    is FoxEnumMergeItemsOfType -> FoxEnumMergeItemsOfType(types)
    is FoxArrayElementOfType -> FoxArrayElementOfType(types.single())
    is FoxRefReferentOfType -> FoxRefReferentOfType(types.single())
    is FoxMethodThisOfType -> FoxMethodThisOfType(types.single())
    is FoxMethodParametersOfType -> FoxMethodParametersOfType(types.single())
    is FoxMethodReturnOfType -> FoxMethodReturnOfType(types.single())
}
