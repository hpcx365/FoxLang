package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxEnumType
import pers.hpcx.foxlang.ast.FoxType
import pers.hpcx.foxlang.type.arity
import pers.hpcx.foxlang.type.selectItems

fun enumPattern(items: Map<String, Space<FoxType>>) = EnumPatternSpace(items)

data class EnumPatternSpace(val items: Map<String, Space<FoxType>>) : Space<FoxType> {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxEnumType) return false
        if (that.arity != items.size) return false
        return items.all { (name, type) ->
            val actual = that.items[name] ?: return false
            type.contains(actual)
        }
    }
}

fun enumRepeat(item: Space<FoxType>, minArity: Int, maxArity: Int) = EnumRepeatSpace(item, minArity, maxArity)

data class EnumRepeatSpace(val item: Space<FoxType>, val minArity: Int, val maxArity: Int) : Space<FoxType> {
    
    init {
        require(minArity >= 0) { "minArity must be non-negative: $minArity" }
        require(maxArity >= minArity) { "maxArity must be >= minArity: $maxArity < $minArity" }
    }
    
    override fun contains(that: FoxType): Boolean {
        return that is FoxEnumType && that.arity in minArity..maxArity && that.items.values.all(item::contains)
    }
}

fun enumMerge(parts: List<Space<FoxType>>) = EnumMergeSpace(parts)

data class EnumMergeSpace(val parts: List<Space<FoxType>>) : Space<FoxType> {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxEnumType) return false
        val allKeys = that.items.keys.toList()
        val totalBounds = enumArityBoundsOf(this)
        if (totalBounds != null && that.arity !in totalBounds.min..totalBounds.max) return false
        val partBounds = parts.map { enumArityBoundsOf(it) }
        
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
                        val piece = that.selectItems(selected)
                        part.contains(piece) && accepts(partIndex + 1, remainingKeys - selected)
                    }
                }
            }
            
            memo[state] = result
            return result
        }
        
        return accepts(0, allKeys.toSet())
    }
}

private data class EnumArityBounds(val min: Int, val max: Int)

private fun addEnumArity(left: Int, right: Int): Int {
    if (left == Int.MAX_VALUE || right == Int.MAX_VALUE) return Int.MAX_VALUE
    return Math.addExact(left, right)
}

private fun enumArityBoundsOf(space: Space<FoxType>): EnumArityBounds? = when (space) {
    is UniversalSpace -> EnumArityBounds(0, Int.MAX_VALUE)
    is SingleSpace -> (space.single as? FoxEnumType)?.let { EnumArityBounds(it.arity, it.arity) }
    is EnumPatternSpace -> EnumArityBounds(space.items.size, space.items.size)
    is EnumRepeatSpace -> EnumArityBounds(space.minArity, space.maxArity)
    is EnumMergeSpace -> {
        var min = 0
        var max = 0
        space.parts.forEach { part ->
            val bounds = enumArityBoundsOf(part) ?: return null
            min = addEnumArity(min, bounds.min)
            max = addEnumArity(max, bounds.max)
        }
        EnumArityBounds(min, max)
    }
    is UnionSpace<FoxType> -> {
        val bounds = space.parts.mapNotNull { enumArityBoundsOf(it) }
        if (bounds.isEmpty()) null
        else EnumArityBounds(bounds.minOf { it.min }, bounds.maxOf { it.max })
    }
    is IntersectSpace<FoxType> -> {
        val bounds = space.parts.map { enumArityBoundsOf(it) ?: return null }
        val min = bounds.maxOf { it.min }
        val max = bounds.minOf { it.max }
        if (min > max) null else EnumArityBounds(min, max)
    }
    is SubtractSpace<FoxType> -> enumArityBoundsOf(space.base)
    else -> null
}

private fun sizeRangeForPart(
    partBounds: List<EnumArityBounds?>,
    partIndex: Int,
    remainingSize: Int,
): IntRange? {
    val currentBounds = partBounds[partIndex] ?: return 0..remainingSize
    var suffixMin = 0
    var suffixMax = 0
    for (index in partIndex + 1 until partBounds.size) {
        val bounds = partBounds[index] ?: return 0..remainingSize
        suffixMin = addEnumArity(suffixMin, bounds.min)
        suffixMax = addEnumArity(suffixMax, bounds.max)
    }
    if (remainingSize !in addEnumArity(currentBounds.min, suffixMin)..addEnumArity(currentBounds.max, suffixMax)) {
        return null
    }
    val minPieceSize = maxOf(currentBounds.min, remainingSize - suffixMax)
    val maxPieceSize = minOf(currentBounds.max, remainingSize - suffixMin)
    if (minPieceSize > maxPieceSize) return null
    return minPieceSize..maxPieceSize
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

fun enumItem(generic: String, name: String) = EnumItemProjectionSpace(generic, name)

data class EnumItemProjectionSpace(val generic: String, val name: String) : PlaceHolderSpace<FoxType>

fun enumItemsOf(generic: String, names: Set<String>) = EnumItemsProjectionSpace(generic, names)

data class EnumItemsProjectionSpace(val generic: String, val names: Set<String>) : PlaceHolderSpace<FoxType>

fun enumDropItemsOf(generic: String, names: Set<String>) = EnumDropItemsProjectionSpace(generic, names)

data class EnumDropItemsProjectionSpace(val generic: String, val names: Set<String>) : PlaceHolderSpace<FoxType>
