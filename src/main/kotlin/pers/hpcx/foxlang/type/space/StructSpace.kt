package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxStructType
import pers.hpcx.foxlang.ast.FoxType
import pers.hpcx.foxlang.type.arity
import pers.hpcx.foxlang.type.sliceFields

typealias StructField = Pair<String, FoxType>

fun structFieldSpace(name: Space<String>, type: Space<FoxType>) = StructFieldSpace(name, type)

data class StructFieldSpace(val name: Space<String>, val type: Space<FoxType>) : Space<StructField> {
    
    fun contains(that: Map.Entry<String, FoxType>): Boolean {
        return contains(that.let { it.key to it.value })
    }
    
    override fun contains(that: StructField): Boolean {
        return name.contains(that.first) && type.contains(that.second)
    }
}

fun structPattern(fields: List<StructFieldSpace>) = StructPatternSpace(fields)

data class StructPatternSpace(val fields: List<StructFieldSpace>) : Space<FoxType> {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxStructType) return false
        if (that.arity != fields.size) return false
        return fields.zip(that.fields).all { (expectedField, thatField) -> expectedField.contains(thatField) }
    }
}

fun structRepeat(field: StructFieldSpace, minArity: Int, maxArity: Int) = StructRepeatSpace(field, minArity, maxArity)

data class StructRepeatSpace(val field: StructFieldSpace, val minArity: Int, val maxArity: Int) : Space<FoxType> {
    
    init {
        require(minArity >= 0) { "minArity must be non-negative: $minArity" }
        require(maxArity >= minArity) { "maxArity must be >= minArity: $maxArity < $minArity" }
    }
    
    override fun contains(that: FoxType): Boolean {
        return that is FoxStructType && that.arity in minArity..maxArity && that.fields.all { field.contains(it) }
    }
}

fun structConcat(parts: List<Space<FoxType>>) = StructConcatSpace(parts)

data class StructConcatSpace(val parts: List<Space<FoxType>>) : Space<FoxType> {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxStructType) return false
        val targetSize = that.arity
        val partBounds = parts.map { structArityBoundsOf(it) }
        val totalBounds = structArityBoundsOf(this)
        if (totalBounds != null && targetSize !in totalBounds.min..totalBounds.max) return false
        val suffixBounds = structConcatSuffixBoundsOf(partBounds)
        
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
                    space.contains(piece) && accepts(partIndex + 1, end)
                }
            }
            
            memo[state] = result
            return result
        }
        
        return accepts(0, 0)
    }
}

private data class StructArityBounds(val min: Int, val max: Int)

private fun addStructArity(left: Int, right: Int): Int {
    if (left == Int.MAX_VALUE || right == Int.MAX_VALUE) return Int.MAX_VALUE
    return Math.addExact(left, right)
}

private fun structConcatSuffixBoundsOf(partBounds: List<StructArityBounds?>): List<StructArityBounds>? {
    if (partBounds.any { it == null }) return null
    return buildList(partBounds.size + 1) {
        repeat(partBounds.size + 1) { add(StructArityBounds(0, 0)) }
    }.toMutableList().also { bounds ->
        for (index in partBounds.size - 1 downTo 0) {
            val current = partBounds[index]!!
            val suffix = bounds[index + 1]
            bounds[index] = StructArityBounds(
                addStructArity(current.min, suffix.min),
                addStructArity(current.max, suffix.max),
            )
        }
    }
}

private fun structArityBoundsOf(space: Space<FoxType>): StructArityBounds? = when (space) {
    is UniversalSpace -> StructArityBounds(0, Int.MAX_VALUE)
    is SingleSpace -> (space.single as? FoxStructType)?.let { StructArityBounds(it.arity, it.arity) }
    is StructPatternSpace -> StructArityBounds(space.fields.size, space.fields.size)
    is StructRepeatSpace -> StructArityBounds(space.minArity, space.maxArity)
    is StructConcatSpace -> {
        var min = 0
        var max = 0
        space.parts.forEach { part ->
            val bounds = structArityBoundsOf(part) ?: return null
            min = addStructArity(min, bounds.min)
            max = addStructArity(max, bounds.max)
        }
        StructArityBounds(min, max)
    }
    is UnionSpace<FoxType> -> {
        val bounds = space.parts.mapNotNull { structArityBoundsOf(it) }
        if (bounds.isEmpty()) null
        else StructArityBounds(bounds.minOf { it.min }, bounds.maxOf { it.max })
    }
    is IntersectSpace<FoxType> -> {
        val bounds = space.parts.map { structArityBoundsOf(it) ?: return null }
        val min = bounds.maxOf { it.min }
        val max = bounds.minOf { it.max }
        if (min > max) null else StructArityBounds(min, max)
    }
    is SubtractSpace<FoxType> -> structArityBoundsOf(space.base)
    else -> null
}

fun structField(generic: String, name: String) = StructFieldProjectionSpace(generic, name)

data class StructFieldProjectionSpace(val generic: String, val name: String) : PlaceHolderSpace<FoxType>

fun structFieldsOf(generic: String, names: List<String>) = StructFieldsProjectionSpace(generic, names)

data class StructFieldsProjectionSpace(val generic: String, val names: List<String>) : PlaceHolderSpace<FoxType>

fun structDropFieldsOf(generic: String, names: Set<String>) = StructDropFieldsProjectionSpace(generic, names)

data class StructDropFieldsProjectionSpace(val generic: String, val names: Set<String>) : PlaceHolderSpace<FoxType>

fun structFieldAt(generic: String, index: Int) = StructFieldAtProjectionSpace(generic, index)

data class StructFieldAtProjectionSpace(val generic: String, val index: Int) : PlaceHolderSpace<FoxType> {
    
    init {
        require(index >= 0) { "index must be non-negative: $index" }
    }
}

fun structLastFieldAt(generic: String, index: Int) = StructLastFieldAtProjectionSpace(generic, index)

data class StructLastFieldAtProjectionSpace(val generic: String, val index: Int) : PlaceHolderSpace<FoxType> {
    
    init {
        require(index >= 0) { "index must be non-negative: $index" }
    }
}

fun structFirstOf(generic: String, count: Int, exact: Boolean) = StructFirstProjectionSpace(generic, count, exact)

data class StructFirstProjectionSpace(val generic: String, val count: Int, val exact: Boolean) : PlaceHolderSpace<FoxType> {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
}

fun structLastOf(generic: String, count: Int, exact: Boolean) = StructLastProjectionSpace(generic, count, exact)

data class StructLastProjectionSpace(val generic: String, val count: Int, val exact: Boolean) : PlaceHolderSpace<FoxType> {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
}

fun structDropFirstOf(generic: String, count: Int, exact: Boolean) = StructDropFirstProjectionSpace(generic, count, exact)

data class StructDropFirstProjectionSpace(val generic: String, val count: Int, val exact: Boolean) : PlaceHolderSpace<FoxType> {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
}

fun structDropLastOf(generic: String, count: Int, exact: Boolean) = StructDropLastProjectionSpace(generic, count, exact)

data class StructDropLastProjectionSpace(val generic: String, val count: Int, val exact: Boolean) : PlaceHolderSpace<FoxType> {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
}
