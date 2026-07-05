package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.*

val FoxTupleType.arity: Int
    get() = components.size

val FoxStructType.arity: Int
    get() = fields.size

val FoxObjectType.arity: Int
    get() = members.size

val FoxEnumType.arity: Int
    get() = items.size

fun FoxTupleType.componentAt(index: Int): FoxType {
    require(index in 0 until arity) { "Tuple component index out of bounds: $index, size=$arity" }
    return components[index]
}

fun FoxTupleType.lastComponentAt(index: Int): FoxType {
    require(index >= 0) { "Tuple component index must be non-negative: $index" }
    return componentAt(arity - 1 - index)
}

fun FoxTupleType.sliceComponents(startIndex: Int, endIndex: Int): FoxTupleType {
    require(startIndex >= 0) { "Tuple slice start index must be non-negative: $startIndex" }
    require(endIndex >= startIndex) { "Tuple slice end index must be >= start index: $endIndex < $startIndex" }
    require(endIndex <= arity) { "Tuple slice end index out of bounds: $endIndex, size=$arity" }
    if (startIndex == endIndex) return FoxTupleType(emptyList())
    return components.subList(startIndex, endIndex).toFoxTupleType()
}

fun FoxTupleType.firstComponents(count: Int): FoxTupleType {
    require(count >= 0) { "Tuple component count must be non-negative: $count" }
    require(count <= arity) { "Tuple component count out of bounds: $count, size=$arity" }
    return sliceComponents(0, count)
}

fun FoxTupleType.lastComponents(count: Int): FoxTupleType {
    require(count >= 0) { "Tuple component count must be non-negative: $count" }
    require(count <= arity) { "Tuple component count out of bounds: $count, size=$arity" }
    return sliceComponents(arity - count, arity)
}

fun FoxTupleType.dropFirstComponents(count: Int): FoxTupleType {
    require(count >= 0) { "Tuple component count must be non-negative: $count" }
    require(count <= arity) { "Tuple component count out of bounds: $count, size=$arity" }
    return sliceComponents(count, arity)
}

fun FoxTupleType.dropLastComponents(count: Int): FoxTupleType {
    require(count >= 0) { "Tuple component count must be non-negative: $count" }
    require(count <= arity) { "Tuple component count out of bounds: $count, size=$arity" }
    return sliceComponents(0, arity - count)
}

fun Iterable<FoxTupleType>.mergeTupleComponents(): FoxTupleType {
    return flatMap { it.components }.toFoxTupleType()
}

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

fun FoxStructType.fieldAt(index: Int): Map.Entry<String, FoxType> {
    require(index in 0 until arity) { "Struct field index out of bounds: $index, size=$arity" }
    return fields.entryAt(index)
}

fun FoxStructType.lastFieldAt(index: Int): Map.Entry<String, FoxType> {
    require(index >= 0) { "Struct field index must be non-negative: $index" }
    return fieldAt(arity - 1 - index)
}

fun FoxStructType.sliceFields(startIndex: Int, endIndex: Int): FoxStructType {
    require(startIndex >= 0) { "Struct slice start index must be non-negative: $startIndex" }
    require(endIndex >= startIndex) { "Struct slice end index must be >= start index: $endIndex < $startIndex" }
    require(endIndex <= arity) { "Struct slice end index out of bounds: $endIndex, size=$arity" }
    if (startIndex == endIndex) return FoxStructType(emptyOrderedMap())
    
    val result = mutableOrderedMapOf<String, FoxType>()
    for (index in startIndex until endIndex) {
        val entry = fields.entryAt(index)
        result[entry.key] = entry.value
    }
    return FoxStructType(result)
}

fun FoxStructType.firstFields(count: Int): FoxStructType {
    require(count >= 0) { "Struct field count must be non-negative: $count" }
    require(count <= arity) { "Struct field count out of bounds: $count, size=$arity" }
    return sliceFields(0, count)
}

fun FoxStructType.lastFields(count: Int): FoxStructType {
    require(count >= 0) { "Struct field count must be non-negative: $count" }
    require(count <= arity) { "Struct field count out of bounds: $count, size=$arity" }
    return sliceFields(arity - count, arity)
}

fun FoxStructType.dropFirstFields(count: Int): FoxStructType {
    require(count >= 0) { "Struct field count must be non-negative: $count" }
    require(count <= arity) { "Struct field count out of bounds: $count, size=$arity" }
    return sliceFields(count, arity)
}

fun FoxStructType.dropLastFields(count: Int): FoxStructType {
    require(count >= 0) { "Struct field count must be non-negative: $count" }
    require(count <= arity) { "Struct field count out of bounds: $count, size=$arity" }
    return sliceFields(0, arity - count)
}

fun FoxStructType.selectFields(names: Iterable<String>): FoxStructType {
    val result = mutableOrderedMapOf<String, FoxType>()
    names.forEach { name ->
        result[name] = fields.getValue(name)
    }
    return FoxStructType(result)
}

fun FoxStructType.dropFields(names: Iterable<String>): FoxStructType {
    val removed = names.toSet()
    val result = mutableOrderedMapOf<String, FoxType>()
    fields.entries.forEach { (name, type) ->
        if (name !in removed) result[name] = type
    }
    return FoxStructType(result)
}

fun Iterable<FoxStructType>.mergeStructFields(): FoxStructType {
    val result = mutableOrderedMapOf<String, FoxType>()
    forEach { struct ->
        struct.fields.entries.forEach { (name, type) ->
            result[name] = type
        }
    }
    return FoxStructType(result)
}

fun FoxObjectType.member(name: String): FoxType = members.getValue(name)

fun FoxObjectType.selectMembers(names: Iterable<String>): FoxObjectType {
    val result = LinkedHashMap<String, FoxType>()
    names.forEach { name ->
        result[name] = members.getValue(name)
    }
    return FoxObjectType(result)
}

fun FoxObjectType.dropMembers(names: Iterable<String>): FoxObjectType {
    val removed = names.toSet()
    val result = LinkedHashMap<String, FoxType>()
    members.forEach { (name, type) ->
        if (name !in removed) result[name] = type
    }
    return FoxObjectType(result)
}

fun Iterable<FoxObjectType>.mergeObjectMembers(): FoxObjectType {
    val result = LinkedHashMap<String, FoxType>()
    forEach { obj ->
        obj.members.forEach { (name, type) ->
            result[name] = type
        }
    }
    return FoxObjectType(result)
}

fun FoxEnumType.item(name: String): FoxType = items.getValue(name)

fun FoxEnumType.selectItems(names: Iterable<String>): FoxEnumType {
    val result = LinkedHashMap<String, FoxType>()
    names.forEach { name -> result[name] = items.getValue(name) }
    return FoxEnumType(result)
}

fun FoxEnumType.dropItems(names: Iterable<String>): FoxEnumType {
    val removed = names.toSet()
    val result = LinkedHashMap<String, FoxType>()
    items.forEach { (name, type) ->
        if (name !in removed) result[name] = type
    }
    return FoxEnumType(result)
}

fun Iterable<FoxEnumType>.mergeEnumItems(): FoxEnumType {
    val result = LinkedHashMap<String, FoxType>()
    forEach { enum ->
        enum.items.forEach { (name, type) ->
            result[name] = type
        }
    }
    return FoxEnumType(result)
}

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
            is FoxEnumType -> items.values.forEach { it.visitTypes(filter, visitor) }
            is FoxArrayType -> element.visitTypes(filter, visitor)
            is FoxRefType -> referent.visitTypes(filter, visitor)
            is FoxMethodType -> {
                `this`.visitTypes(filter, visitor)
                parameters.values.forEach { it.visitTypes(filter, visitor) }
                `return`.visitTypes(filter, visitor)
            }
        }
        is FoxTransformType -> when (this) {
            is FoxTupleComponentAtType -> type.visitTypes(filter, visitor)
            is FoxTupleLastComponentAtType -> type.visitTypes(filter, visitor)
            is FoxTupleFirstComponentsOfType -> type.visitTypes(filter, visitor)
            is FoxTupleExactFirstComponentsOfType -> type.visitTypes(filter, visitor)
            is FoxTupleLastComponentsOfType -> type.visitTypes(filter, visitor)
            is FoxTupleExactLastComponentsOfType -> type.visitTypes(filter, visitor)
            is FoxTupleDropFirstComponentsOfType -> type.visitTypes(filter, visitor)
            is FoxTupleExactDropFirstComponentsOfType -> type.visitTypes(filter, visitor)
            is FoxTupleDropLastComponentsOfType -> type.visitTypes(filter, visitor)
            is FoxTupleExactDropLastComponentsOfType -> type.visitTypes(filter, visitor)
            is FoxTupleMergeComponentsOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxStructFieldOfType -> type.visitTypes(filter, visitor)
            is FoxStructFieldAtType -> type.visitTypes(filter, visitor)
            is FoxStructLastFieldAtType -> type.visitTypes(filter, visitor)
            is FoxStructFirstFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructExactFirstFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructLastFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructExactLastFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructDropFirstFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructExactDropFirstFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructDropLastFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructExactDropLastFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructDropFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructMergeFieldsOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxObjectMemberOfType -> type.visitTypes(filter, visitor)
            is FoxObjectMembersOfType -> type.visitTypes(filter, visitor)
            is FoxObjectDropMembersOfType -> type.visitTypes(filter, visitor)
            is FoxObjectMergeMembersOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxEnumItemOfType -> type.visitTypes(filter, visitor)
            is FoxEnumItemsOfType -> type.visitTypes(filter, visitor)
            is FoxEnumDropItemsOfType -> type.visitTypes(filter, visitor)
            is FoxEnumMergeItemsOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxArrayElementOfType -> type.visitTypes(filter, visitor)
            is FoxRefReferentOfType -> type.visitTypes(filter, visitor)
            is FoxMethodOfType -> {
                `this`.visitTypes(filter, visitor)
                parameters.visitTypes(filter, visitor)
                `return`.visitTypes(filter, visitor)
            }
            is FoxMethodThisOfType -> type.visitTypes(filter, visitor)
            is FoxMethodParametersOfType -> type.visitTypes(filter, visitor)
            is FoxMethodReturnOfType -> type.visitTypes(filter, visitor)
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
            is FoxEnumType -> FoxEnumType(items.mapValues { it.value.mapTypes(filter, mapper) })
            is FoxArrayType -> FoxArrayType(element.mapTypes(filter, mapper))
            is FoxRefType -> FoxRefType(referent.mapTypes(filter, mapper))
            is FoxMethodType -> FoxMethodType(
                `this`.mapTypes(filter, mapper),
                parameters.mapValues { it.value.mapTypes(filter, mapper) },
                `return`.mapTypes(filter, mapper),
            )
        }
        is FoxTransformType -> when (this) {
            is FoxTupleComponentAtType -> FoxTupleComponentAtType(type.mapTypes(filter, mapper), index)
            is FoxTupleLastComponentAtType -> FoxTupleLastComponentAtType(type.mapTypes(filter, mapper), index)
            is FoxTupleFirstComponentsOfType -> FoxTupleFirstComponentsOfType(type.mapTypes(filter, mapper), count)
            is FoxTupleExactFirstComponentsOfType -> FoxTupleExactFirstComponentsOfType(type.mapTypes(filter, mapper), count)
            is FoxTupleLastComponentsOfType -> FoxTupleLastComponentsOfType(type.mapTypes(filter, mapper), count)
            is FoxTupleExactLastComponentsOfType -> FoxTupleExactLastComponentsOfType(type.mapTypes(filter, mapper), count)
            is FoxTupleDropFirstComponentsOfType -> FoxTupleDropFirstComponentsOfType(type.mapTypes(filter, mapper), count)
            is FoxTupleExactDropFirstComponentsOfType -> FoxTupleExactDropFirstComponentsOfType(type.mapTypes(filter, mapper), count)
            is FoxTupleDropLastComponentsOfType -> FoxTupleDropLastComponentsOfType(type.mapTypes(filter, mapper), count)
            is FoxTupleExactDropLastComponentsOfType -> FoxTupleExactDropLastComponentsOfType(type.mapTypes(filter, mapper), count)
            is FoxTupleMergeComponentsOfType -> FoxTupleMergeComponentsOfType(types.map { it.mapTypes(filter, mapper) })
            is FoxStructFieldOfType -> FoxStructFieldOfType(type.mapTypes(filter, mapper), name)
            is FoxStructFieldAtType -> FoxStructFieldAtType(type.mapTypes(filter, mapper), index)
            is FoxStructLastFieldAtType -> FoxStructLastFieldAtType(type.mapTypes(filter, mapper), index)
            is FoxStructFirstFieldsOfType -> FoxStructFirstFieldsOfType(type.mapTypes(filter, mapper), count)
            is FoxStructExactFirstFieldsOfType -> FoxStructExactFirstFieldsOfType(type.mapTypes(filter, mapper), count)
            is FoxStructLastFieldsOfType -> FoxStructLastFieldsOfType(type.mapTypes(filter, mapper), count)
            is FoxStructExactLastFieldsOfType -> FoxStructExactLastFieldsOfType(type.mapTypes(filter, mapper), count)
            is FoxStructDropFirstFieldsOfType -> FoxStructDropFirstFieldsOfType(type.mapTypes(filter, mapper), count)
            is FoxStructExactDropFirstFieldsOfType -> FoxStructExactDropFirstFieldsOfType(type.mapTypes(filter, mapper), count)
            is FoxStructDropLastFieldsOfType -> FoxStructDropLastFieldsOfType(type.mapTypes(filter, mapper), count)
            is FoxStructExactDropLastFieldsOfType -> FoxStructExactDropLastFieldsOfType(type.mapTypes(filter, mapper), count)
            is FoxStructFieldsOfType -> FoxStructFieldsOfType(type.mapTypes(filter, mapper), names)
            is FoxStructDropFieldsOfType -> FoxStructDropFieldsOfType(type.mapTypes(filter, mapper), names)
            is FoxStructMergeFieldsOfType -> FoxStructMergeFieldsOfType(types.map { it.mapTypes(filter, mapper) })
            is FoxObjectMemberOfType -> FoxObjectMemberOfType(type.mapTypes(filter, mapper), name)
            is FoxObjectMembersOfType -> FoxObjectMembersOfType(type.mapTypes(filter, mapper), names)
            is FoxObjectDropMembersOfType -> FoxObjectDropMembersOfType(type.mapTypes(filter, mapper), names)
            is FoxObjectMergeMembersOfType -> FoxObjectMergeMembersOfType(types.map { it.mapTypes(filter, mapper) })
            is FoxEnumItemOfType -> FoxEnumItemOfType(type.mapTypes(filter, mapper), name)
            is FoxEnumItemsOfType -> FoxEnumItemsOfType(type.mapTypes(filter, mapper), names)
            is FoxEnumDropItemsOfType -> FoxEnumDropItemsOfType(type.mapTypes(filter, mapper), names)
            is FoxEnumMergeItemsOfType -> FoxEnumMergeItemsOfType(types.map { it.mapTypes(filter, mapper) })
            is FoxArrayElementOfType -> FoxArrayElementOfType(type.mapTypes(filter, mapper))
            is FoxRefReferentOfType -> FoxRefReferentOfType(type.mapTypes(filter, mapper))
            is FoxMethodOfType -> FoxMethodOfType(`this`.mapTypes(filter, mapper), parameters.mapTypes(filter, mapper), `return`.mapTypes(filter, mapper))
            is FoxMethodThisOfType -> FoxMethodThisOfType(type.mapTypes(filter, mapper))
            is FoxMethodParametersOfType -> FoxMethodParametersOfType(type.mapTypes(filter, mapper))
            is FoxMethodReturnOfType -> FoxMethodReturnOfType(type.mapTypes(filter, mapper))
        }
        is FoxUnresolvedType -> FoxUnresolvedType(name, parameters?.map { it.mapTypes(filter, mapper) })
        is FoxPlaceholderType -> error("Placeholder type cannot be mapped")
    }
}
