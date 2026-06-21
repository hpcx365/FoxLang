package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxTupleType
import pers.hpcx.foxlang.ast.FoxType
import pers.hpcx.foxlang.type.arity
import pers.hpcx.foxlang.type.componentAt
import pers.hpcx.foxlang.type.sliceComponents

fun tuplePattern(components: List<Space<FoxType>>) = TuplePatternSpace(components)

data class TuplePatternSpace(val components: List<Space<FoxType>>) : Space<FoxType> {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxTupleType) return false
        if (that.arity != components.size) return false
        components.forEachIndexed { index, space ->
            if (!space.contains(that.componentAt(index))) return false
        }
        return true
    }
}

fun tupleRepeat(component: Space<FoxType>, minArity: Int, maxArity: Int) = TupleRepeatSpace(component, minArity, maxArity)

data class TupleRepeatSpace(val component: Space<FoxType>, val minArity: Int, val maxArity: Int) : Space<FoxType> {
    
    init {
        require(minArity >= 0) { "minArity must be non-negative: $minArity" }
        require(maxArity >= minArity) { "maxArity must be >= minArity: $maxArity < $minArity" }
    }
    
    override fun contains(that: FoxType): Boolean {
        return that is FoxTupleType && that.arity in minArity..maxArity && that.components.all(component::contains)
    }
}

fun tupleConcat(parts: List<Space<FoxType>>) = TupleConcatSpace(parts)

data class TupleConcatSpace(val parts: List<Space<FoxType>>) : Space<FoxType> {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxTupleType) return false
        val targetSize = that.arity
        val partBounds = parts.map { tupleArityBoundsOf(it) }
        val totalBounds = tupleArityBoundsOf(this)
        if (totalBounds != null && targetSize !in totalBounds.min..totalBounds.max) return false
        val suffixBounds = tupleConcatSuffixBoundsOf(partBounds)
        
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
                    space.contains(piece) && accepts(partIndex + 1, end)
                }
            }
            
            memo[state] = result
            return result
        }
        
        return accepts(0, 0)
    }
}

private data class TupleArityBounds(val min: Int, val max: Int)

private fun addTupleArity(left: Int, right: Int): Int {
    if (left == Int.MAX_VALUE || right == Int.MAX_VALUE) return Int.MAX_VALUE
    return Math.addExact(left, right)
}

private fun tupleConcatSuffixBoundsOf(partBounds: List<TupleArityBounds?>): List<TupleArityBounds>? {
    if (partBounds.any { it == null }) return null
    return buildList(partBounds.size + 1) {
        repeat(partBounds.size + 1) { add(TupleArityBounds(0, 0)) }
    }.toMutableList().also { bounds ->
        for (index in partBounds.size - 1 downTo 0) {
            val current = partBounds[index]!!
            val suffix = bounds[index + 1]
            bounds[index] = TupleArityBounds(
                addTupleArity(current.min, suffix.min),
                addTupleArity(current.max, suffix.max),
            )
        }
    }
}

private fun tupleArityBoundsOf(lang: Space<FoxType>): TupleArityBounds? = when (lang) {
    is UniversalSpace -> TupleArityBounds(0, Int.MAX_VALUE)
    is SingleSpace -> (lang.single as? FoxTupleType)?.let { TupleArityBounds(it.arity, it.arity) }
    is TuplePatternSpace -> TupleArityBounds(lang.components.size, lang.components.size)
    is TupleRepeatSpace -> TupleArityBounds(lang.minArity, lang.maxArity)
    is TupleConcatSpace -> {
        var min = 0
        var max = 0
        lang.parts.forEach { part ->
            val bounds = tupleArityBoundsOf(part) ?: return null
            min = addTupleArity(min, bounds.min)
            max = addTupleArity(max, bounds.max)
        }
        TupleArityBounds(min, max)
    }
    is UnionSpace<FoxType> -> {
        val bounds = lang.parts.mapNotNull { tupleArityBoundsOf(it) }
        if (bounds.isEmpty()) null
        else TupleArityBounds(bounds.minOf { it.min }, bounds.maxOf { it.max })
    }
    is IntersectSpace<FoxType> -> {
        val bounds = lang.parts.map { tupleArityBoundsOf(it) ?: return null }
        val min = bounds.maxOf { it.min }
        val max = bounds.minOf { it.max }
        if (min > max) null else TupleArityBounds(min, max)
    }
    is SubtractSpace<FoxType> -> tupleArityBoundsOf(lang.base)
    else -> null
}

fun tupleComponentAt(generic: String, index: Int) = TupleComponentAtProjectionSpace(generic, index)

data class TupleComponentAtProjectionSpace(val generic: String, val index: Int) : PlaceHolderSpace<FoxType> {
    
    init {
        require(index >= 0) { "index must be non-negative: $index" }
    }
}

fun tupleLastComponentAt(generic: String, index: Int) = TupleLastComponentAtProjectionSpace(generic, index)

data class TupleLastComponentAtProjectionSpace(val generic: String, val index: Int) : PlaceHolderSpace<FoxType> {
    
    init {
        require(index >= 0) { "index must be non-negative: $index" }
    }
}

fun tupleFirstOf(generic: String, count: Int, exact: Boolean) = TupleFirstProjectionSpace(generic, count, exact)

data class TupleFirstProjectionSpace(val generic: String, val count: Int, val exact: Boolean) : PlaceHolderSpace<FoxType> {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
}

fun tupleLastOf(generic: String, count: Int, exact: Boolean) = TupleLastProjectionSpace(generic, count, exact)

data class TupleLastProjectionSpace(val generic: String, val count: Int, val exact: Boolean) : PlaceHolderSpace<FoxType> {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
}

fun tupleDropFirstOf(generic: String, count: Int, exact: Boolean) = TupleDropFirstProjectionSpace(generic, count, exact)

data class TupleDropFirstProjectionSpace(val generic: String, val count: Int, val exact: Boolean) : PlaceHolderSpace<FoxType> {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
}

fun tupleDropLastOf(generic: String, count: Int, exact: Boolean) = TupleDropLastProjectionSpace(generic, count, exact)

data class TupleDropLastProjectionSpace(val generic: String, val count: Int, val exact: Boolean) : PlaceHolderSpace<FoxType> {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
}
