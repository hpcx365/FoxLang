package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.mapValues
import pers.hpcx.foxlang.utils.toOrderedMap
import pers.hpcx.foxlang.utils.toRleArrayList
import pers.hpcx.foxlang.utils.toRleArrayListFromRuns

@JvmName("tupleComponentListToFoxTupleType")
fun List<Pair<FoxType, Int>>.toFoxTupleType(): FoxTupleType {
    if (isEmpty()) return FoxTupleType(emptyList())
    val expandedSize = fold(0) { acc, (_, count) ->
        require(count > 0) { "Tuple component count must be positive: $count" }
        Math.addExact(acc, count)
    }
    val result = ArrayList<FoxType>(expandedSize)
    var previous: FoxType? = null
    var hasPrevious = false
    var shouldUseRle = false
    forEach { (type, count) ->
        if (count > 1 || hasPrevious && previous == type) {
            shouldUseRle = true
        }
        repeat(count) {
            result += type
        }
        previous = type
        hasPrevious = true
    }
    return FoxTupleType(if (shouldUseRle) toRleArrayListFromRuns() else result)
}

@JvmName("typeListToFoxTupleType")
fun List<FoxType>.toFoxTupleType(): FoxTupleType {
    if (isEmpty()) return FoxTupleType(emptyList())
    val copied = toList()
    val shouldUseRle = copied.zipWithNext().any { (left, right) -> left == right }
    return FoxTupleType(if (shouldUseRle) copied.toRleArrayList() else copied)
}

fun List<Pair<String, FoxType>>.toFoxStructType() = FoxStructType(toOrderedMap())

fun List<Pair<String, FoxType>>.toFoxObjectType() = FoxObjectType(toMap())

fun List<Pair<String, FoxType>>.toFoxEnumType() = FoxEnumType(toMap())

fun FoxTupleType.getComponent(index: Int) = components[index]

fun FoxTupleType.getComponentBack(index: Int) = getComponent(arity - 1 - index)

fun FoxTupleType.getFirstComponents(count: Int) = sliceComponents(0, count)

fun FoxTupleType.getLastComponents(count: Int) = sliceComponents(arity - count, arity)

fun FoxTupleType.dropFirstComponents(count: Int) = sliceComponents(count, arity)

fun FoxTupleType.dropLastComponents(count: Int) = sliceComponents(0, arity - count)

fun FoxTupleType.sliceComponents(startIndex: Int, endIndex: Int) =
    components.subList(startIndex, endIndex).toFoxTupleType()

fun Iterable<FoxTupleType>.mergeTuples() = flatMap { it.components }.toFoxTupleType()

fun FoxStructType.getFieldTypeByName(name: String) = fields.getValue(name)

fun FoxStructType.getFieldTypeByIndex(index: Int) = fields.entryAt(index)

fun FoxStructType.getFieldTypeByIndexBack(index: Int) = getFieldTypeByIndex(arity - 1 - index)

fun FoxStructType.getFirstFields(count: Int) = sliceFields(0, count)

fun FoxStructType.getLastFields(count: Int) = sliceFields(arity - count, arity)

fun FoxStructType.dropFirstFields(count: Int) = sliceFields(count, arity)

fun FoxStructType.dropLastFields(count: Int) = sliceFields(0, arity - count)

fun FoxStructType.sliceFields(startIndex: Int, endIndex: Int) =
    fields.entries.subList(startIndex, endIndex).map { it.key to it.value }.toFoxStructType()

fun FoxStructType.selectFields(names: Set<String>) =
    fields.entries.filter { it.key in names }.map { it.key to it.value }.toFoxStructType()

fun FoxStructType.dropFields(names: Set<String>) =
    fields.entries.filter { it.key !in names }.map { it.key to it.value }.toFoxStructType()

fun FoxStructType.extractFieldTypes() = fields.values.toFoxTupleType()

fun Iterable<FoxStructType>.mergeStructs() =
    flatMap { it.fields.entries }.map { it.key to it.value }.toFoxStructType()

fun FoxObjectType.getMemberType(name: String) = members.getValue(name)

fun FoxObjectType.selectMembers(names: Set<String>) =
    members.entries.filter { it.key in names }.map { it.key to it.value }.toFoxObjectType()

fun FoxObjectType.dropMembers(names: Set<String>) =
    members.entries.filter { it.key !in names }.map { it.key to it.value }.toFoxObjectType()

fun Iterable<FoxObjectType>.mergeObjects() =
    flatMap { it.members.entries }.map { it.key to it.value }.toFoxObjectType()

fun FoxEnumType.getEntryType(name: String) = entries.getValue(name)

fun FoxEnumType.selectEntries(names: Set<String>) =
    entries.entries.filter { it.key in names }.map { it.key to it.value }.toFoxEnumType()

fun FoxEnumType.dropEntries(names: Set<String>) =
    entries.entries.filter { it.key !in names }.map { it.key to it.value }.toFoxEnumType()

fun Iterable<FoxEnumType>.mergeEnums() =
    flatMap { it.entries.entries }.map { it.key to it.value }.toFoxEnumType()

inline fun <reified T : FoxType> FoxType.visitTypes(crossinline visitor: (T) -> Unit) {
    visitTypes({ type -> type is T }) { type -> visitor(type as T) }
}

fun FoxType.visitTypes(filter: (FoxType) -> Boolean, visitor: (FoxType) -> Unit) {
    if (filter(this)) visitor(this)
    else when (this) {
        is FoxPrimitiveType -> {}
        is FoxWildcardType -> when (this) {
            FoxAnyType,
            FoxAnyTupleType,
            FoxAnyStructType,
            FoxAnyObjectType,
            FoxAnyEnumType,
                -> {
            }
            is FoxAnyOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxAllOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxNoneOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxAnyTupleOfType -> component.visitTypes(filter, visitor)
            is FoxAnyStructOfType -> fields.forEach { it.visitTypes(filter, visitor) }
        }
        is FoxBuiltInType -> when (this) {
            is FoxTupleType -> components.forEach { it.visitTypes(filter, visitor) }
            is FoxStructType -> fields.values.forEach { it.visitTypes(filter, visitor) }
            is FoxObjectType -> members.values.forEach { it.visitTypes(filter, visitor) }
            is FoxEnumType -> entries.values.forEach { it.visitTypes(filter, visitor) }
            is FoxArrayType -> element.visitTypes(filter, visitor)
            is FoxRefType -> referent.visitTypes(filter, visitor)
            is FoxMethodType -> {
                `this`.visitTypes(filter, visitor)
                parameters.values.forEach { it.visitTypes(filter, visitor) }
                `return`.visitTypes(filter, visitor)
            }
        }
        is FoxTransformType -> when (this) {
            is FoxTupleGetComponentType -> type.visitTypes(filter, visitor)
            is FoxTupleGetComponentBackType -> type.visitTypes(filter, visitor)
            is FoxTupleGetFirstComponentsType -> type.visitTypes(filter, visitor)
            is FoxTupleGetFirstComponentsExactType -> type.visitTypes(filter, visitor)
            is FoxTupleGetLastComponentsType -> type.visitTypes(filter, visitor)
            is FoxTupleGetLastComponentsExactType -> type.visitTypes(filter, visitor)
            is FoxTupleDropFirstComponentsType -> type.visitTypes(filter, visitor)
            is FoxTupleDropFirstComponentsExactType -> type.visitTypes(filter, visitor)
            is FoxTupleDropLastComponentsType -> type.visitTypes(filter, visitor)
            is FoxTupleDropLastComponentsExactType -> type.visitTypes(filter, visitor)
            is FoxTupleMergeTuplesType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxStructGetFieldTypeByNameType -> type.visitTypes(filter, visitor)
            is FoxStructGetFieldTypeByIndexType -> type.visitTypes(filter, visitor)
            is FoxStructGetFieldTypeByIndexBackType -> type.visitTypes(filter, visitor)
            is FoxStructGetFirstFieldsType -> type.visitTypes(filter, visitor)
            is FoxStructGetFirstFieldsExactType -> type.visitTypes(filter, visitor)
            is FoxStructGetLastFieldsType -> type.visitTypes(filter, visitor)
            is FoxStructGetLastFieldsExactType -> type.visitTypes(filter, visitor)
            is FoxStructDropFirstFieldsType -> type.visitTypes(filter, visitor)
            is FoxStructDropFirstFieldsExactType -> type.visitTypes(filter, visitor)
            is FoxStructDropLastFieldsType -> type.visitTypes(filter, visitor)
            is FoxStructDropLastFieldsExactType -> type.visitTypes(filter, visitor)
            is FoxStructSelectFieldsType -> type.visitTypes(filter, visitor)
            is FoxStructSelectFieldsExactType -> type.visitTypes(filter, visitor)
            is FoxStructDropFieldsType -> type.visitTypes(filter, visitor)
            is FoxStructDropFieldsExactType -> type.visitTypes(filter, visitor)
            is FoxStructExtractFieldTypesType -> type.visitTypes(filter, visitor)
            is FoxStructMergeStructsType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxObjectGetMemberTypeType -> type.visitTypes(filter, visitor)
            is FoxObjectSelectMembersType -> type.visitTypes(filter, visitor)
            is FoxObjectSelectMembersExactType -> type.visitTypes(filter, visitor)
            is FoxObjectDropMembersType -> type.visitTypes(filter, visitor)
            is FoxObjectDropMembersExactType -> type.visitTypes(filter, visitor)
            is FoxObjectMergeObjectsType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxEnumGetEntryTypeType -> type.visitTypes(filter, visitor)
            is FoxEnumSelectEntriesType -> type.visitTypes(filter, visitor)
            is FoxEnumSelectEntriesExactType -> type.visitTypes(filter, visitor)
            is FoxEnumDropEntriesType -> type.visitTypes(filter, visitor)
            is FoxEnumDropEntriesExactType -> type.visitTypes(filter, visitor)
            is FoxEnumMergeEnumsType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxArrayGetElementTypeType -> type.visitTypes(filter, visitor)
            is FoxRefGetReferentTypeType -> type.visitTypes(filter, visitor)
            is FoxMethodGetThisTypeType -> type.visitTypes(filter, visitor)
            is FoxMethodGetParameterStructType -> type.visitTypes(filter, visitor)
            is FoxMethodGetReturnTypeType -> type.visitTypes(filter, visitor)
            is FoxMethodOfType -> {
                `this`.visitTypes(filter, visitor)
                parameters.visitTypes(filter, visitor)
                `return`.visitTypes(filter, visitor)
            }
        }
        is FoxUnresolvedType -> parameters?.forEach { it.visitTypes(filter, visitor) }
        is FoxPlaceholderType -> error("Placeholder type cannot be visited")
    }
}

inline fun <reified T : FoxType> FoxType.mapTypes(crossinline mapper: (T) -> FoxType): FoxType {
    return mapTypes({ type -> type is T }) { type -> mapper(type as T) }
}

fun FoxType.mapTypes(filter: (FoxType) -> Boolean, mapper: (FoxType) -> FoxType): FoxType {
    if (filter(this)) return mapper(this)
    else return when (this) {
        is FoxPrimitiveType -> this
        is FoxWildcardType -> when (this) {
            FoxAnyType,
            FoxAnyTupleType,
            FoxAnyStructType,
            FoxAnyObjectType,
            FoxAnyEnumType,
                -> this
            is FoxAnyOfType -> FoxAnyOfType(types.map { it.mapTypes(filter, mapper) })
            is FoxAllOfType -> FoxAllOfType(types.map { it.mapTypes(filter, mapper) })
            is FoxNoneOfType -> FoxNoneOfType(types.map { it.mapTypes(filter, mapper) })
            is FoxAnyTupleOfType -> FoxAnyTupleOfType(component.mapTypes(filter, mapper))
            is FoxAnyStructOfType -> FoxAnyStructOfType(fields.map { it.mapTypes(filter, mapper) })
        }
        is FoxBuiltInType -> when (this) {
            is FoxTupleType -> components.map { it.mapTypes(filter, mapper) }.toFoxTupleType()
            is FoxStructType -> FoxStructType(fields.mapValues { it.value.mapTypes(filter, mapper) })
            is FoxObjectType -> FoxObjectType(members.mapValues { it.value.mapTypes(filter, mapper) })
            is FoxEnumType -> FoxEnumType(entries.mapValues { it.value.mapTypes(filter, mapper) })
            is FoxArrayType -> FoxArrayType(element.mapTypes(filter, mapper))
            is FoxRefType -> FoxRefType(referent.mapTypes(filter, mapper))
            is FoxMethodType -> FoxMethodType(
                `this`.mapTypes(filter, mapper),
                parameters.mapValues { it.value.mapTypes(filter, mapper) },
                `return`.mapTypes(filter, mapper),
            )
        }
        is FoxTransformType -> when (this) {
            is FoxTupleGetComponentType -> FoxTupleGetComponentType(type.mapTypes(filter, mapper), index)
            is FoxTupleGetComponentBackType -> FoxTupleGetComponentBackType(type.mapTypes(filter, mapper), index)
            is FoxTupleGetFirstComponentsType -> FoxTupleGetFirstComponentsType(type.mapTypes(filter, mapper), count)
            is FoxTupleGetFirstComponentsExactType -> FoxTupleGetFirstComponentsExactType(type.mapTypes(filter, mapper), count)
            is FoxTupleGetLastComponentsType -> FoxTupleGetLastComponentsType(type.mapTypes(filter, mapper), count)
            is FoxTupleGetLastComponentsExactType -> FoxTupleGetLastComponentsExactType(type.mapTypes(filter, mapper), count)
            is FoxTupleDropFirstComponentsType -> FoxTupleDropFirstComponentsType(type.mapTypes(filter, mapper), count)
            is FoxTupleDropFirstComponentsExactType -> FoxTupleDropFirstComponentsExactType(type.mapTypes(filter, mapper), count)
            is FoxTupleDropLastComponentsType -> FoxTupleDropLastComponentsType(type.mapTypes(filter, mapper), count)
            is FoxTupleDropLastComponentsExactType -> FoxTupleDropLastComponentsExactType(type.mapTypes(filter, mapper), count)
            is FoxTupleMergeTuplesType -> FoxTupleMergeTuplesType(types.map { it.mapTypes(filter, mapper) })
            is FoxStructGetFieldTypeByNameType -> FoxStructGetFieldTypeByNameType(type.mapTypes(filter, mapper), name)
            is FoxStructGetFieldTypeByIndexType -> FoxStructGetFieldTypeByIndexType(type.mapTypes(filter, mapper), index)
            is FoxStructGetFieldTypeByIndexBackType -> FoxStructGetFieldTypeByIndexBackType(type.mapTypes(filter, mapper), index)
            is FoxStructGetFirstFieldsType -> FoxStructGetFirstFieldsType(type.mapTypes(filter, mapper), count)
            is FoxStructGetFirstFieldsExactType -> FoxStructGetFirstFieldsExactType(type.mapTypes(filter, mapper), count)
            is FoxStructGetLastFieldsType -> FoxStructGetLastFieldsType(type.mapTypes(filter, mapper), count)
            is FoxStructGetLastFieldsExactType -> FoxStructGetLastFieldsExactType(type.mapTypes(filter, mapper), count)
            is FoxStructDropFirstFieldsType -> FoxStructDropFirstFieldsType(type.mapTypes(filter, mapper), count)
            is FoxStructDropFirstFieldsExactType -> FoxStructDropFirstFieldsExactType(type.mapTypes(filter, mapper), count)
            is FoxStructDropLastFieldsType -> FoxStructDropLastFieldsType(type.mapTypes(filter, mapper), count)
            is FoxStructDropLastFieldsExactType -> FoxStructDropLastFieldsExactType(type.mapTypes(filter, mapper), count)
            is FoxStructSelectFieldsType -> FoxStructSelectFieldsType(type.mapTypes(filter, mapper), names)
            is FoxStructSelectFieldsExactType -> FoxStructSelectFieldsExactType(type.mapTypes(filter, mapper), names)
            is FoxStructDropFieldsType -> FoxStructDropFieldsType(type.mapTypes(filter, mapper), names)
            is FoxStructDropFieldsExactType -> FoxStructDropFieldsExactType(type.mapTypes(filter, mapper), names)
            is FoxStructExtractFieldTypesType -> FoxStructExtractFieldTypesType(type.mapTypes(filter, mapper))
            is FoxStructMergeStructsType -> FoxStructMergeStructsType(types.map { it.mapTypes(filter, mapper) })
            is FoxObjectGetMemberTypeType -> FoxObjectGetMemberTypeType(type.mapTypes(filter, mapper), name)
            is FoxObjectSelectMembersType -> FoxObjectSelectMembersType(type.mapTypes(filter, mapper), names)
            is FoxObjectSelectMembersExactType -> FoxObjectSelectMembersExactType(type.mapTypes(filter, mapper), names)
            is FoxObjectDropMembersType -> FoxObjectDropMembersType(type.mapTypes(filter, mapper), names)
            is FoxObjectDropMembersExactType -> FoxObjectDropMembersExactType(type.mapTypes(filter, mapper), names)
            is FoxObjectMergeObjectsType -> FoxObjectMergeObjectsType(types.map { it.mapTypes(filter, mapper) })
            is FoxEnumGetEntryTypeType -> FoxEnumGetEntryTypeType(type.mapTypes(filter, mapper), name)
            is FoxEnumSelectEntriesType -> FoxEnumSelectEntriesType(type.mapTypes(filter, mapper), names)
            is FoxEnumSelectEntriesExactType -> FoxEnumSelectEntriesExactType(type.mapTypes(filter, mapper), names)
            is FoxEnumDropEntriesType -> FoxEnumDropEntriesType(type.mapTypes(filter, mapper), names)
            is FoxEnumDropEntriesExactType -> FoxEnumDropEntriesExactType(type.mapTypes(filter, mapper), names)
            is FoxEnumMergeEnumsType -> FoxEnumMergeEnumsType(types.map { it.mapTypes(filter, mapper) })
            is FoxArrayGetElementTypeType -> FoxArrayGetElementTypeType(type.mapTypes(filter, mapper))
            is FoxRefGetReferentTypeType -> FoxRefGetReferentTypeType(type.mapTypes(filter, mapper))
            is FoxMethodGetThisTypeType -> FoxMethodGetThisTypeType(type.mapTypes(filter, mapper))
            is FoxMethodGetParameterStructType -> FoxMethodGetParameterStructType(type.mapTypes(filter, mapper))
            is FoxMethodGetReturnTypeType -> FoxMethodGetReturnTypeType(type.mapTypes(filter, mapper))
            is FoxMethodOfType -> FoxMethodOfType(`this`.mapTypes(filter, mapper), parameters.mapTypes(filter, mapper), `return`.mapTypes(filter, mapper))
        }
        is FoxUnresolvedType -> FoxUnresolvedType(name, parameters?.map { it.mapTypes(filter, mapper) })
        is FoxPlaceholderType -> error("Placeholder type cannot be mapped")
    }
}
