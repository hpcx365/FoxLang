package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.mutableOrderedMapOf

val FoxTupleType.size: Int
    get() = components.fold(0) { acc, component -> Math.addExact(acc, component.second) }

fun FoxTupleType.componentAt(index: Int): FoxType {
    require(index in 0 until size) { "Tuple component index out of bounds: $index, size=$size" }
    var offset = index
    components.forEach { component ->
        if (offset < component.second) return component.first
        offset -= component.second
    }
    error("Unreachable")
}

fun FoxTupleType.lastComponentAt(index: Int): FoxType {
    require(index >= 0) { "Tuple component index must be non-negative: $index" }
    return componentAt(size - 1 - index)
}

fun FoxTupleType.sliceComponents(beginIndex: Int, endIndex: Int): FoxTupleType {
    require(beginIndex >= 0) { "Tuple slice begin index must be non-negative: $beginIndex" }
    require(endIndex >= beginIndex) { "Tuple slice end index must be >= begin index: $endIndex < $beginIndex" }
    require(endIndex <= size) { "Tuple slice end index out of bounds: $endIndex, size=$size" }
    if (beginIndex == endIndex) return emptyFoxTupleType()
    
    val result = mutableListOf<Pair<FoxType, Int>>()
    var currentIndex = 0
    components.forEach { component ->
        val componentBegin = currentIndex
        val componentEnd = Math.addExact(currentIndex, component.second)
        currentIndex = componentEnd
        
        if (componentEnd <= beginIndex || componentBegin >= endIndex) return@forEach
        
        val overlapBegin = maxOf(componentBegin, beginIndex)
        val overlapEnd = minOf(componentEnd, endIndex)
        result += component.first to (overlapEnd - overlapBegin)
    }
    return result.toFoxTupleType()
}

fun FoxTupleType.firstComponents(count: Int): FoxTupleType {
    require(count >= 0) { "Tuple component count must be non-negative: $count" }
    require(count <= size) { "Tuple component count out of bounds: $count, size=$size" }
    return sliceComponents(0, count)
}

fun FoxTupleType.lastComponents(count: Int): FoxTupleType {
    require(count >= 0) { "Tuple component count must be non-negative: $count" }
    require(count <= size) { "Tuple component count out of bounds: $count, size=$size" }
    return sliceComponents(size - count, size)
}

fun FoxTupleType.dropFirstComponents(count: Int): FoxTupleType {
    require(count >= 0) { "Tuple component count must be non-negative: $count" }
    require(count <= size) { "Tuple component count out of bounds: $count, size=$size" }
    return sliceComponents(count, size)
}

fun FoxTupleType.dropLastComponents(count: Int): FoxTupleType {
    require(count >= 0) { "Tuple component count must be non-negative: $count" }
    require(count <= size) { "Tuple component count out of bounds: $count, size=$size" }
    return sliceComponents(0, size - count)
}

fun FoxTupleType.mergeComponents(other: FoxTupleType): FoxTupleType {
    return (components + other.components).toFoxTupleType()
}

fun Iterable<FoxTupleType>.mergeTupleComponents(): FoxTupleType {
    return flatMap { it.components }.toFoxTupleType()
}

@JvmName("tupleComponentListToFoxTupleType")
fun List<Pair<FoxType, Int>>.toFoxTupleType(): FoxTupleType {
    if (isEmpty()) return emptyFoxTupleType()
    val compressed = mutableListOf<Pair<FoxType, Int>>()
    forEach { component ->
        val last = compressed.lastOrNull()
        if (last != null && last.first == component.first) {
            compressed[compressed.lastIndex] = last.first to Math.addExact(last.second, component.second)
        } else {
            compressed += component
        }
    }
    return FoxTupleType(compressed)
}

@JvmName("typeListToFoxTupleType")
fun List<FoxType>.toFoxTupleType(): FoxTupleType {
    return map { it to 1 }.toFoxTupleType()
}

fun emptyFoxTupleType(): FoxTupleType = FoxTupleType(emptyList())

val FoxStructType.size: Int
    get() = fields.size

fun FoxStructType.fieldAt(index: Int): Map.Entry<String, FoxType> {
    require(index in 0 until size) { "Struct field index out of bounds: $index, size=$size" }
    return fields.entryAt(index)
}

fun FoxStructType.lastFieldAt(index: Int): Map.Entry<String, FoxType> {
    require(index >= 0) { "Struct field index must be non-negative: $index" }
    return fieldAt(size - 1 - index)
}

fun FoxStructType.sliceFields(beginIndex: Int, endIndex: Int): FoxStructType {
    require(beginIndex >= 0) { "Struct slice begin index must be non-negative: $beginIndex" }
    require(endIndex >= beginIndex) { "Struct slice end index must be >= begin index: $endIndex < $beginIndex" }
    require(endIndex <= size) { "Struct slice end index out of bounds: $endIndex, size=$size" }
    if (beginIndex == endIndex) return emptyFoxStructType()
    
    val result = mutableOrderedMapOf<String, FoxType>()
    for (index in beginIndex until endIndex) {
        val entry = fields.entryAt(index)
        result[entry.key] = entry.value
    }
    return FoxStructType(result)
}

fun FoxStructType.firstFields(count: Int): FoxStructType {
    require(count >= 0) { "Struct field count must be non-negative: $count" }
    require(count <= size) { "Struct field count out of bounds: $count, size=$size" }
    return sliceFields(0, count)
}

fun FoxStructType.lastFields(count: Int): FoxStructType {
    require(count >= 0) { "Struct field count must be non-negative: $count" }
    require(count <= size) { "Struct field count out of bounds: $count, size=$size" }
    return sliceFields(size - count, size)
}

fun FoxStructType.dropFirstFields(count: Int): FoxStructType {
    require(count >= 0) { "Struct field count must be non-negative: $count" }
    require(count <= size) { "Struct field count out of bounds: $count, size=$size" }
    return sliceFields(count, size)
}

fun FoxStructType.dropLastFields(count: Int): FoxStructType {
    require(count >= 0) { "Struct field count must be non-negative: $count" }
    require(count <= size) { "Struct field count out of bounds: $count, size=$size" }
    return sliceFields(0, size - count)
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

fun emptyFoxStructType(): FoxStructType = FoxStructType(emptyOrderedMap())

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

fun emptyFoxObjectType(): FoxObjectType = FoxObjectType(emptyMap())

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

fun emptyFoxEnumType(): FoxEnumType = FoxEnumType(emptyMap())
