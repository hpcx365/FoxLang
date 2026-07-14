package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.utils.mapValues
import pers.hpcx.foxlang.utils.toOrderedMap
import pers.hpcx.foxlang.utils.toRleArrayList
import pers.hpcx.foxlang.utils.toRleArrayListFromRuns

@JvmName("tupleComponentListToFoxTupleType")
fun List<Pair<SurfaceType, Int>>.toFoxTupleType(): SurfaceTupleType {
    if (isEmpty()) return SurfaceTupleType(emptyList())
    val expandedSize = fold(0) { acc, (_, count) ->
        require(count > 0) { "Tuple component count must be positive: $count" }
        Math.addExact(acc, count)
    }
    val result = ArrayList<SurfaceType>(expandedSize)
    var previous: SurfaceType? = null
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
    return SurfaceTupleType(if (shouldUseRle) toRleArrayListFromRuns() else result)
}

@JvmName("typeListToFoxTupleType")
fun List<SurfaceType>.toFoxTupleType(): SurfaceTupleType {
    if (isEmpty()) return SurfaceTupleType(emptyList())
    val copied = toList()
    val shouldUseRle = copied.zipWithNext().any { (left, right) -> left == right }
    return SurfaceTupleType(if (shouldUseRle) copied.toRleArrayList() else copied)
}

fun List<Pair<String, SurfaceType>>.toFoxStructType() = SurfaceStructType(toOrderedMap())

fun List<Pair<String, SurfaceType>>.toFoxObjectType() = SurfaceObjectType(toMap())

fun List<Pair<String, SurfaceType>>.toFoxEnumType() = SurfaceEnumType(toMap())

fun SurfaceTupleType.getComponent(index: Int) = components[index]

fun SurfaceTupleType.getComponentBack(index: Int) = getComponent(arity - 1 - index)

fun SurfaceTupleType.getFirstComponents(count: Int) = sliceComponents(0, count)

fun SurfaceTupleType.getLastComponents(count: Int) = sliceComponents(arity - count, arity)

fun SurfaceTupleType.dropFirstComponents(count: Int) = sliceComponents(count, arity)

fun SurfaceTupleType.dropLastComponents(count: Int) = sliceComponents(0, arity - count)

fun SurfaceTupleType.sliceComponents(startIndex: Int, endIndex: Int) =
    components.subList(startIndex, endIndex).toFoxTupleType()

fun Iterable<SurfaceTupleType>.mergeTuples() = flatMap { it.components }.toFoxTupleType()

fun SurfaceStructType.getFieldTypeByName(name: String) = fields.getValue(name)

fun SurfaceStructType.getFieldTypeByIndex(index: Int) = fields.entryAt(index)

fun SurfaceStructType.getFieldTypeByIndexBack(index: Int) = getFieldTypeByIndex(arity - 1 - index)

fun SurfaceStructType.getFirstFields(count: Int) = sliceFields(0, count)

fun SurfaceStructType.getLastFields(count: Int) = sliceFields(arity - count, arity)

fun SurfaceStructType.dropFirstFields(count: Int) = sliceFields(count, arity)

fun SurfaceStructType.dropLastFields(count: Int) = sliceFields(0, arity - count)

fun SurfaceStructType.sliceFields(startIndex: Int, endIndex: Int) =
    fields.entries.subList(startIndex, endIndex).map { it.key to it.value }.toFoxStructType()

fun SurfaceStructType.selectFields(names: Set<String>) =
    fields.entries.filter { it.key in names }.map { it.key to it.value }.toFoxStructType()

fun SurfaceStructType.dropFields(names: Set<String>) =
    fields.entries.filter { it.key !in names }.map { it.key to it.value }.toFoxStructType()

fun SurfaceStructType.extractFieldTypes() = fields.values.toFoxTupleType()

fun Iterable<SurfaceStructType>.mergeStructs() =
    flatMap { it.fields.entries }.map { it.key to it.value }.toFoxStructType()

fun SurfaceObjectType.getMemberType(name: String) = members.getValue(name)

fun SurfaceObjectType.selectMembers(names: Set<String>) =
    members.entries.filter { it.key in names }.map { it.key to it.value }.toFoxObjectType()

fun SurfaceObjectType.dropMembers(names: Set<String>) =
    members.entries.filter { it.key !in names }.map { it.key to it.value }.toFoxObjectType()

fun Iterable<SurfaceObjectType>.mergeObjects() =
    flatMap { it.members.entries }.map { it.key to it.value }.toFoxObjectType()

fun SurfaceEnumType.getEntryType(name: String) = entries.getValue(name)

fun SurfaceEnumType.selectEntries(names: Set<String>) =
    entries.entries.filter { it.key in names }.map { it.key to it.value }.toFoxEnumType()

fun SurfaceEnumType.dropEntries(names: Set<String>) =
    entries.entries.filter { it.key !in names }.map { it.key to it.value }.toFoxEnumType()

fun Iterable<SurfaceEnumType>.mergeEnums() =
    flatMap { it.entries.entries }.map { it.key to it.value }.toFoxEnumType()

inline fun <reified T : SurfaceType> SurfaceType.visitTypes(crossinline visitor: (T) -> Unit) {
    visitTypes({ type -> type is T }) { type -> visitor(type as T) }
}

fun SurfaceType.visitTypes(filter: (SurfaceType) -> Boolean, visitor: (SurfaceType) -> Unit) {
    if (filter(this)) visitor(this)
    else when (this) {
        is SurfacePrimitiveType -> {}
        is SurfaceWildcardType -> when (this) {
            is SurfaceAnyType -> {}
            is SurfaceAnyTupleType -> {}
            is SurfaceAnyStructType -> {}
            is SurfaceAnyObjectType -> {}
            is SurfaceAnyEnumType -> {}
            is SurfaceAnyOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is SurfaceAllOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is SurfaceNoneOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is SurfaceAnyTupleOfType -> component.visitTypes(filter, visitor)
            is SurfaceAnyStructOfType -> fields.forEach { it.visitTypes(filter, visitor) }
        }
        is SurfaceBuiltInType -> when (this) {
            is SurfaceTupleType -> components.forEach { it.visitTypes(filter, visitor) }
            is SurfaceStructType -> fields.values.forEach { it.visitTypes(filter, visitor) }
            is SurfaceObjectType -> members.values.forEach { it.visitTypes(filter, visitor) }
            is SurfaceEnumType -> entries.values.forEach { it.visitTypes(filter, visitor) }
            is SurfaceArrayType -> element.visitTypes(filter, visitor)
            is SurfaceRefType -> referent.visitTypes(filter, visitor)
            is SurfaceMethodType -> {
                `this`.visitTypes(filter, visitor)
                parameters.values.forEach { it.visitTypes(filter, visitor) }
                `return`.visitTypes(filter, visitor)
            }
        }
        is SurfaceTransformType -> when (this) {
            is SurfaceTupleGetComponentType -> type.visitTypes(filter, visitor)
            is SurfaceTupleGetComponentBackType -> type.visitTypes(filter, visitor)
            is SurfaceTupleGetFirstComponentsType -> type.visitTypes(filter, visitor)
            is SurfaceTupleGetFirstComponentsExactType -> type.visitTypes(filter, visitor)
            is SurfaceTupleGetLastComponentsType -> type.visitTypes(filter, visitor)
            is SurfaceTupleGetLastComponentsExactType -> type.visitTypes(filter, visitor)
            is SurfaceTupleDropFirstComponentsType -> type.visitTypes(filter, visitor)
            is SurfaceTupleDropFirstComponentsExactType -> type.visitTypes(filter, visitor)
            is SurfaceTupleDropLastComponentsType -> type.visitTypes(filter, visitor)
            is SurfaceTupleDropLastComponentsExactType -> type.visitTypes(filter, visitor)
            is SurfaceTupleMergeTuplesType -> types.forEach { it.visitTypes(filter, visitor) }
            is SurfaceStructGetFieldTypeByNameType -> type.visitTypes(filter, visitor)
            is SurfaceStructGetFieldTypeByIndexType -> type.visitTypes(filter, visitor)
            is SurfaceStructGetFieldTypeByIndexBackType -> type.visitTypes(filter, visitor)
            is SurfaceStructGetFirstFieldsType -> type.visitTypes(filter, visitor)
            is SurfaceStructGetFirstFieldsExactType -> type.visitTypes(filter, visitor)
            is SurfaceStructGetLastFieldsType -> type.visitTypes(filter, visitor)
            is SurfaceStructGetLastFieldsExactType -> type.visitTypes(filter, visitor)
            is SurfaceStructDropFirstFieldsType -> type.visitTypes(filter, visitor)
            is SurfaceStructDropFirstFieldsExactType -> type.visitTypes(filter, visitor)
            is SurfaceStructDropLastFieldsType -> type.visitTypes(filter, visitor)
            is SurfaceStructDropLastFieldsExactType -> type.visitTypes(filter, visitor)
            is SurfaceStructSelectFieldsType -> type.visitTypes(filter, visitor)
            is SurfaceStructSelectFieldsExactType -> type.visitTypes(filter, visitor)
            is SurfaceStructDropFieldsType -> type.visitTypes(filter, visitor)
            is SurfaceStructDropFieldsExactType -> type.visitTypes(filter, visitor)
            is SurfaceStructExtractFieldTypesType -> type.visitTypes(filter, visitor)
            is SurfaceStructMergeStructsType -> types.forEach { it.visitTypes(filter, visitor) }
            is SurfaceObjectGetMemberTypeType -> type.visitTypes(filter, visitor)
            is SurfaceObjectSelectMembersType -> type.visitTypes(filter, visitor)
            is SurfaceObjectSelectMembersExactType -> type.visitTypes(filter, visitor)
            is SurfaceObjectDropMembersType -> type.visitTypes(filter, visitor)
            is SurfaceObjectDropMembersExactType -> type.visitTypes(filter, visitor)
            is SurfaceObjectMergeObjectsType -> types.forEach { it.visitTypes(filter, visitor) }
            is SurfaceEnumGetEntryTypeType -> type.visitTypes(filter, visitor)
            is SurfaceEnumSelectEntriesType -> type.visitTypes(filter, visitor)
            is SurfaceEnumSelectEntriesExactType -> type.visitTypes(filter, visitor)
            is SurfaceEnumDropEntriesType -> type.visitTypes(filter, visitor)
            is SurfaceEnumDropEntriesExactType -> type.visitTypes(filter, visitor)
            is SurfaceEnumMergeEnumsType -> types.forEach { it.visitTypes(filter, visitor) }
            is SurfaceArrayGetElementTypeType -> type.visitTypes(filter, visitor)
            is SurfaceRefGetReferentTypeType -> type.visitTypes(filter, visitor)
            is SurfaceMethodGetThisTypeType -> type.visitTypes(filter, visitor)
            is SurfaceMethodGetParameterStructType -> type.visitTypes(filter, visitor)
            is SurfaceMethodGetReturnTypeType -> type.visitTypes(filter, visitor)
            is SurfaceMethodOfType -> {
                `this`.visitTypes(filter, visitor)
                parameters.visitTypes(filter, visitor)
                `return`.visitTypes(filter, visitor)
            }
        }
        is SurfaceUnresolvedType -> parameters?.forEach { it.visitTypes(filter, visitor) }
        is SurfacePlaceholderType -> error("Placeholder type cannot be visited")
    }
}

inline fun <reified T : SurfaceType> SurfaceType.mapTypes(crossinline mapper: (T) -> SurfaceType): SurfaceType {
    return mapTypes({ type -> type is T }) { type -> mapper(type as T) }
}

fun SurfaceType.mapTypes(filter: (SurfaceType) -> Boolean, mapper: (SurfaceType) -> SurfaceType): SurfaceType {
    if (filter(this)) return mapper(this)
    else return when (this) {
        is SurfacePrimitiveType -> this
        is SurfaceWildcardType -> when (this) {
            is SurfaceAnyType -> this
            is SurfaceAnyTupleType -> this
            is SurfaceAnyStructType -> this
            is SurfaceAnyObjectType -> this
            is SurfaceAnyEnumType -> this
            is SurfaceAnyOfType -> SurfaceAnyOfType(types.map { it.mapTypes(filter, mapper) })
            is SurfaceAllOfType -> SurfaceAllOfType(types.map { it.mapTypes(filter, mapper) })
            is SurfaceNoneOfType -> SurfaceNoneOfType(types.map { it.mapTypes(filter, mapper) })
            is SurfaceAnyTupleOfType -> SurfaceAnyTupleOfType(component.mapTypes(filter, mapper))
            is SurfaceAnyStructOfType -> SurfaceAnyStructOfType(fields.map { it.mapTypes(filter, mapper) })
        }
        is SurfaceBuiltInType -> when (this) {
            is SurfaceTupleType -> components.map { it.mapTypes(filter, mapper) }.toFoxTupleType()
            is SurfaceStructType -> SurfaceStructType(fields.mapValues { it.value.mapTypes(filter, mapper) })
            is SurfaceObjectType -> SurfaceObjectType(members.mapValues { it.value.mapTypes(filter, mapper) })
            is SurfaceEnumType -> SurfaceEnumType(entries.mapValues { it.value.mapTypes(filter, mapper) })
            is SurfaceArrayType -> SurfaceArrayType(element.mapTypes(filter, mapper))
            is SurfaceRefType -> SurfaceRefType(referent.mapTypes(filter, mapper))
            is SurfaceMethodType -> SurfaceMethodType(
                `this`.mapTypes(filter, mapper),
                parameters.mapValues { it.value.mapTypes(filter, mapper) },
                `return`.mapTypes(filter, mapper),
            )
        }
        is SurfaceTransformType -> when (this) {
            is SurfaceTupleGetComponentType -> SurfaceTupleGetComponentType(type.mapTypes(filter, mapper), index)
            is SurfaceTupleGetComponentBackType -> SurfaceTupleGetComponentBackType(type.mapTypes(filter, mapper), index)
            is SurfaceTupleGetFirstComponentsType -> SurfaceTupleGetFirstComponentsType(type.mapTypes(filter, mapper), count)
            is SurfaceTupleGetFirstComponentsExactType -> SurfaceTupleGetFirstComponentsExactType(type.mapTypes(filter, mapper), count)
            is SurfaceTupleGetLastComponentsType -> SurfaceTupleGetLastComponentsType(type.mapTypes(filter, mapper), count)
            is SurfaceTupleGetLastComponentsExactType -> SurfaceTupleGetLastComponentsExactType(type.mapTypes(filter, mapper), count)
            is SurfaceTupleDropFirstComponentsType -> SurfaceTupleDropFirstComponentsType(type.mapTypes(filter, mapper), count)
            is SurfaceTupleDropFirstComponentsExactType -> SurfaceTupleDropFirstComponentsExactType(type.mapTypes(filter, mapper), count)
            is SurfaceTupleDropLastComponentsType -> SurfaceTupleDropLastComponentsType(type.mapTypes(filter, mapper), count)
            is SurfaceTupleDropLastComponentsExactType -> SurfaceTupleDropLastComponentsExactType(type.mapTypes(filter, mapper), count)
            is SurfaceTupleMergeTuplesType -> SurfaceTupleMergeTuplesType(types.map { it.mapTypes(filter, mapper) })
            is SurfaceStructGetFieldTypeByNameType -> SurfaceStructGetFieldTypeByNameType(type.mapTypes(filter, mapper), name)
            is SurfaceStructGetFieldTypeByIndexType -> SurfaceStructGetFieldTypeByIndexType(type.mapTypes(filter, mapper), index)
            is SurfaceStructGetFieldTypeByIndexBackType -> SurfaceStructGetFieldTypeByIndexBackType(type.mapTypes(filter, mapper), index)
            is SurfaceStructGetFirstFieldsType -> SurfaceStructGetFirstFieldsType(type.mapTypes(filter, mapper), count)
            is SurfaceStructGetFirstFieldsExactType -> SurfaceStructGetFirstFieldsExactType(type.mapTypes(filter, mapper), count)
            is SurfaceStructGetLastFieldsType -> SurfaceStructGetLastFieldsType(type.mapTypes(filter, mapper), count)
            is SurfaceStructGetLastFieldsExactType -> SurfaceStructGetLastFieldsExactType(type.mapTypes(filter, mapper), count)
            is SurfaceStructDropFirstFieldsType -> SurfaceStructDropFirstFieldsType(type.mapTypes(filter, mapper), count)
            is SurfaceStructDropFirstFieldsExactType -> SurfaceStructDropFirstFieldsExactType(type.mapTypes(filter, mapper), count)
            is SurfaceStructDropLastFieldsType -> SurfaceStructDropLastFieldsType(type.mapTypes(filter, mapper), count)
            is SurfaceStructDropLastFieldsExactType -> SurfaceStructDropLastFieldsExactType(type.mapTypes(filter, mapper), count)
            is SurfaceStructSelectFieldsType -> SurfaceStructSelectFieldsType(type.mapTypes(filter, mapper), names)
            is SurfaceStructSelectFieldsExactType -> SurfaceStructSelectFieldsExactType(type.mapTypes(filter, mapper), names)
            is SurfaceStructDropFieldsType -> SurfaceStructDropFieldsType(type.mapTypes(filter, mapper), names)
            is SurfaceStructDropFieldsExactType -> SurfaceStructDropFieldsExactType(type.mapTypes(filter, mapper), names)
            is SurfaceStructExtractFieldTypesType -> SurfaceStructExtractFieldTypesType(type.mapTypes(filter, mapper))
            is SurfaceStructMergeStructsType -> SurfaceStructMergeStructsType(types.map { it.mapTypes(filter, mapper) })
            is SurfaceObjectGetMemberTypeType -> SurfaceObjectGetMemberTypeType(type.mapTypes(filter, mapper), name)
            is SurfaceObjectSelectMembersType -> SurfaceObjectSelectMembersType(type.mapTypes(filter, mapper), names)
            is SurfaceObjectSelectMembersExactType -> SurfaceObjectSelectMembersExactType(type.mapTypes(filter, mapper), names)
            is SurfaceObjectDropMembersType -> SurfaceObjectDropMembersType(type.mapTypes(filter, mapper), names)
            is SurfaceObjectDropMembersExactType -> SurfaceObjectDropMembersExactType(type.mapTypes(filter, mapper), names)
            is SurfaceObjectMergeObjectsType -> SurfaceObjectMergeObjectsType(types.map { it.mapTypes(filter, mapper) })
            is SurfaceEnumGetEntryTypeType -> SurfaceEnumGetEntryTypeType(type.mapTypes(filter, mapper), name)
            is SurfaceEnumSelectEntriesType -> SurfaceEnumSelectEntriesType(type.mapTypes(filter, mapper), names)
            is SurfaceEnumSelectEntriesExactType -> SurfaceEnumSelectEntriesExactType(type.mapTypes(filter, mapper), names)
            is SurfaceEnumDropEntriesType -> SurfaceEnumDropEntriesType(type.mapTypes(filter, mapper), names)
            is SurfaceEnumDropEntriesExactType -> SurfaceEnumDropEntriesExactType(type.mapTypes(filter, mapper), names)
            is SurfaceEnumMergeEnumsType -> SurfaceEnumMergeEnumsType(types.map { it.mapTypes(filter, mapper) })
            is SurfaceArrayGetElementTypeType -> SurfaceArrayGetElementTypeType(type.mapTypes(filter, mapper))
            is SurfaceRefGetReferentTypeType -> SurfaceRefGetReferentTypeType(type.mapTypes(filter, mapper))
            is SurfaceMethodGetThisTypeType -> SurfaceMethodGetThisTypeType(type.mapTypes(filter, mapper))
            is SurfaceMethodGetParameterStructType -> SurfaceMethodGetParameterStructType(type.mapTypes(filter, mapper))
            is SurfaceMethodGetReturnTypeType -> SurfaceMethodGetReturnTypeType(type.mapTypes(filter, mapper))
            is SurfaceMethodOfType -> SurfaceMethodOfType(`this`.mapTypes(filter, mapper), parameters.mapTypes(filter, mapper), `return`.mapTypes(filter, mapper))
        }
        is SurfaceUnresolvedType -> SurfaceUnresolvedType(name, parameters?.map { it.mapTypes(filter, mapper) })
        is SurfacePlaceholderType -> error("Placeholder type cannot be mapped")
    }
}
