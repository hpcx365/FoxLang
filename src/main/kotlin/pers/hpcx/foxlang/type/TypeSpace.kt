package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.OrderedMap

sealed interface TypeSpace {
    operator fun contains(that: FoxType): Boolean
}

fun emptyTypeSpace() = EmptyTypeSpace
fun universalTypeSpace() = UniversalTypeSpace
fun singleTypeSpace(single: FoxType) = SingleTypeSpace(single)

fun intersect(vararg spaces: TypeSpace) = intersect(spaces.toList())
fun intersect(spaces: Iterable<TypeSpace>) = IntersectTypeSpace(spaces)
fun union(vararg spaces: TypeSpace) = union(spaces.toList())
fun union(spaces: Iterable<TypeSpace>) = UnionTypeSpace(spaces)
fun TypeSpace.complement() = ComplementTypeSpace(this)

sealed interface GeneralTypeSpace : TypeSpace

data object EmptyTypeSpace : GeneralTypeSpace {
    override fun contains(that: FoxType) = false
}

data object UniversalTypeSpace : GeneralTypeSpace {
    override fun contains(that: FoxType) = true
}

data class SingleTypeSpace(val single: FoxType) : GeneralTypeSpace {
    override fun contains(that: FoxType) = single == that
}

data class IntersectTypeSpace(val parts: Iterable<TypeSpace>) : GeneralTypeSpace {
    override fun contains(that: FoxType) = parts.all { that in it }
}

data class UnionTypeSpace(val parts: Iterable<TypeSpace>) : GeneralTypeSpace {
    override fun contains(that: FoxType) = parts.any { that in it }
}

data class ComplementTypeSpace(val space: TypeSpace) : GeneralTypeSpace {
    override fun contains(that: FoxType) = that !in space
}

sealed interface TupleSubTypeSpace : TypeSpace
sealed interface StructSubTypeSpace : TypeSpace
sealed interface ObjectSubTypeSpace : TypeSpace
sealed interface EnumSubTypeSpace : TypeSpace
sealed interface ArraySubTypeSpace : TypeSpace
sealed interface RefSubTypeSpace : TypeSpace
sealed interface MethodSubTypeSpace : TypeSpace

fun tupleSpace() = TupleTypeSpace
fun tupleRepeatSpace(component: TypeSpace) = TupleRepeatTypeSpace(component)
fun tuplePatternSpace(components: List<TypeSpace>) = TuplePatternTypeSpace(components)
fun tupleConcatSpace(parts: List<TypeSpace>) = TupleConcatTypeSpace(parts)

data object TupleTypeSpace : TupleSubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxTupleType
}

data class TupleRepeatTypeSpace(val component: TypeSpace) : TupleSubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxTupleType
        && that.components.all { it in component }
}

data class TuplePatternTypeSpace(val components: List<TypeSpace>) : TupleSubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxTupleType
        && that.components.size == components.size
        && that.components.zip(components).all { it.first in it.second }
}

data class TupleConcatTypeSpace(val parts: List<TypeSpace>) : TupleSubTypeSpace {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxTupleType) return false
        val targetSize = that.arity
        val partBounds = parts.map { arityBoundsOf(it) }
        val totalBounds = arityBoundsOf(this)
        if (totalBounds != null && targetSize !in totalBounds.min..totalBounds.max) return false
        val suffixBounds = concatSuffixBoundsOf(partBounds)
        
        val memo = mutableMapOf<Pair<Int, Int>, Boolean>()
        
        fun accepts(partIndex: Int, offset: Int): Boolean {
            val state = partIndex to offset
            memo[state]?.let { return it }
            
            val result = if (partIndex == parts.size) {
                offset == targetSize
            } else {
                val space = parts[partIndex]
                val remaining = targetSize - offset
                val sizeRange = if (suffixBounds != null) {
                    val remainingBounds = suffixBounds[partIndex]
                    if (remaining !in remainingBounds.min..remainingBounds.max) {
                        memo[state] = false
                        return false
                    }
                    val currentBounds = partBounds[partIndex]!!
                    val suffix = suffixBounds[partIndex + 1]
                    val minPieceSize = maxOf(currentBounds.min, remaining - suffix.max)
                    val maxPieceSize = minOf(currentBounds.max, remaining - suffix.min)
                    if (minPieceSize > maxPieceSize) {
                        memo[state] = false
                        return false
                    }
                    minPieceSize..maxPieceSize
                } else {
                    0..remaining
                }
                sizeRange.any { pieceSize ->
                    val end = offset + pieceSize
                    val piece = that.sliceComponents(offset, end)
                    piece in space && accepts(partIndex + 1, end)
                }
            }
            
            memo[state] = result
            return result
        }
        
        return accepts(0, 0)
    }
}

fun structSpace() = StructTypeSpace
fun structPatternSpace(fieldTypes: List<TypeSpace>) = StructPatternTypeSpace(fieldTypes)
fun structFieldPatternSpace(fields: OrderedMap<String, TypeSpace>) = StructFieldPatternTypeSpace(fields)
fun structConcatSpace(parts: List<TypeSpace>) = StructConcatTypeSpace(parts)

data object StructTypeSpace : StructSubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxStructType
}

data class StructPatternTypeSpace(val fieldTypes: List<TypeSpace>) : StructSubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxStructType
        && that.fields.size == fieldTypes.size
        && that.fields.values.zip(fieldTypes).all { it.first in it.second }
}

data class StructFieldPatternTypeSpace(val fields: OrderedMap<String, TypeSpace>) : StructSubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxStructType
        && that.fields.keys == fields.keys
        && that.fields.values.zip(fields.values).all { it.first in it.second }
}

data class StructConcatTypeSpace(val parts: List<TypeSpace>) : StructSubTypeSpace {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxStructType) return false
        val targetSize = that.arity
        val partBounds = parts.map { arityBoundsOf(it) }
        val totalBounds = arityBoundsOf(this)
        if (totalBounds != null && targetSize !in totalBounds.min..totalBounds.max) return false
        val suffixBounds = concatSuffixBoundsOf(partBounds)
        
        val memo = mutableMapOf<Pair<Int, Int>, Boolean>()
        
        fun accepts(partIndex: Int, offset: Int): Boolean {
            val state = partIndex to offset
            memo[state]?.let { return it }
            
            val result = if (partIndex == parts.size) {
                offset == targetSize
            } else {
                val space = parts[partIndex]
                val remaining = targetSize - offset
                val sizeRange = if (suffixBounds != null) {
                    val remainingBounds = suffixBounds[partIndex]
                    if (remaining !in remainingBounds.min..remainingBounds.max) {
                        memo[state] = false
                        return false
                    }
                    val currentBounds = partBounds[partIndex]!!
                    val suffix = suffixBounds[partIndex + 1]
                    val minPieceSize = maxOf(currentBounds.min, remaining - suffix.max)
                    val maxPieceSize = minOf(currentBounds.max, remaining - suffix.min)
                    if (minPieceSize > maxPieceSize) {
                        memo[state] = false
                        return false
                    }
                    minPieceSize..maxPieceSize
                } else {
                    0..remaining
                }
                sizeRange.any { pieceSize ->
                    val end = offset + pieceSize
                    val piece = that.sliceFields(offset, end)
                    piece in space && accepts(partIndex + 1, end)
                }
            }
            
            memo[state] = result
            return result
        }
        
        return accepts(0, 0)
    }
}

fun objectSpace() = ObjectTypeSpace
fun objectPatternSpace(members: Map<String, TypeSpace>) = ObjectPatternTypeSpace(members)
fun objectMergeSpace(parts: List<TypeSpace>) = ObjectMergeTypeSpace(parts)

data object ObjectTypeSpace : ObjectSubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxObjectType
}

data class ObjectPatternTypeSpace(val members: Map<String, TypeSpace>) : ObjectSubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxObjectType
        && that.members.keys == members.keys
        && that.members.entries.all { it.value in members.getValue(it.key) }
}

data class ObjectMergeTypeSpace(val parts: List<TypeSpace>) : ObjectSubTypeSpace {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxObjectType) return false
        val allKeys = that.members.keys.toList()
        val totalBounds = arityBoundsOf(this)
        if (totalBounds != null && that.arity !in totalBounds.min..totalBounds.max) return false
        val partBounds = parts.map { arityBoundsOf(it) }
        
        val memo = mutableMapOf<Pair<Int, Set<String>>, Boolean>()
        
        fun accepts(partIndex: Int, remainingKeys: Set<String>): Boolean {
            val state = partIndex to remainingKeys
            memo[state]?.let { return it }
            
            val result = if (partIndex == parts.size) {
                remainingKeys.isEmpty()
            } else {
                val part = parts[partIndex]
                val sizeRange = sizeRangeForPart(partBounds, partIndex, remainingKeys.size) ?: run {
                    memo[state] = false
                    return false
                }
                sizeRange.any { pieceSize ->
                    chooseSubsets(allKeys, remainingKeys, pieceSize).any { selected ->
                        val piece = that.selectMembers(selected)
                        piece in part && accepts(partIndex + 1, remainingKeys - selected)
                    }
                }
            }
            
            memo[state] = result
            return result
        }
        
        return accepts(0, allKeys.toSet())
    }
}

fun enumSpace() = EnumTypeSpace
fun enumPatternSpace(entries: Map<String, TypeSpace>) = EnumPatternTypeSpace(entries)
fun enumMergeSpace(parts: List<TypeSpace>) = EnumMergeTypeSpace(parts)

data object EnumTypeSpace : EnumSubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxEnumType
}

data class EnumPatternTypeSpace(val entries: Map<String, TypeSpace>) : EnumSubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxEnumType
        && that.entries.keys == entries.keys
        && that.entries.entries.all { it.value in entries.getValue(it.key) }
}

data class EnumMergeTypeSpace(val parts: List<TypeSpace>) : EnumSubTypeSpace {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxEnumType) return false
        val allKeys = that.entries.keys.toList()
        val totalBounds = arityBoundsOf(this)
        if (totalBounds != null && that.arity !in totalBounds.min..totalBounds.max) return false
        val partBounds = parts.map { arityBoundsOf(it) }
        
        val memo = mutableMapOf<Pair<Int, Set<String>>, Boolean>()
        
        fun accepts(partIndex: Int, remainingKeys: Set<String>): Boolean {
            val state = partIndex to remainingKeys
            memo[state]?.let { return it }
            
            val result = if (partIndex == parts.size) {
                remainingKeys.isEmpty()
            } else {
                val part = parts[partIndex]
                val sizeRange = sizeRangeForPart(partBounds, partIndex, remainingKeys.size) ?: run {
                    memo[state] = false
                    return false
                }
                sizeRange.any { pieceSize ->
                    chooseSubsets(allKeys, remainingKeys, pieceSize).any { selected ->
                        val piece = that.selectEntries(selected)
                        piece in part && accepts(partIndex + 1, remainingKeys - selected)
                    }
                }
            }
            
            memo[state] = result
            return result
        }
        
        return accepts(0, allKeys.toSet())
    }
}

fun arrayPatternSpace(element: TypeSpace) = ArrayPatternTypeSpace(element)

data class ArrayPatternTypeSpace(val element: TypeSpace) : ArraySubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxArrayType && that.element in element
}

fun refPatternSpace(referent: TypeSpace) = RefPatternTypeSpace(referent)

data class RefPatternTypeSpace(val referent: TypeSpace) : RefSubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxRefType && that.referent in referent
}

fun methodPatternSpace(
    `this`: TypeSpace,
    parameterStruct: TypeSpace,
    `return`: TypeSpace,
) = MethodPatternTypeSpace(`this`, parameterStruct, `return`)

data class MethodPatternTypeSpace(
    val `this`: TypeSpace,
    val parameterStruct: TypeSpace,
    val `return`: TypeSpace,
) : MethodSubTypeSpace {
    
    override fun contains(that: FoxType) = that is FoxMethodType
        && `this`.contains(that.`this`)
        && parameterStruct.contains(FoxStructType(that.parameters))
        && `return`.contains(that.`return`)
}

private fun chooseSubsets(
    orderedKeys: List<String>,
    allowedKeys: Set<String>,
    targetSize: Int,
): Sequence<Set<String>> = sequence {
    val pool = orderedKeys.filter { it in allowedKeys }
    val selected = ArrayList<String>(targetSize)
    
    suspend fun SequenceScope<Set<String>>.visit(start: Int) {
        if (selected.size == targetSize) {
            yield(selected.toSet())
            return
        }
        val needed = targetSize - selected.size
        val lastStart = pool.size - needed
        for (index in start..lastStart) {
            selected += pool[index]
            visit(index + 1)
            selected.removeAt(selected.lastIndex)
        }
    }
    
    if (targetSize == 0) {
        yield(emptySet())
    } else if (targetSize <= pool.size) {
        visit(0)
    }
}
