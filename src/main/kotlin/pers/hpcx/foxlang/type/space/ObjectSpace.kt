package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxObjectType
import pers.hpcx.foxlang.ast.FoxType
import pers.hpcx.foxlang.type.arity
import pers.hpcx.foxlang.type.selectMembers

fun objectPattern(members: Map<String, Space<FoxType>>) = ObjectPatternSpace(members)

data class ObjectPatternSpace(val members: Map<String, Space<FoxType>>) : Space<FoxType> {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxObjectType) return false
        if (that.arity != members.size) return false
        return members.all { (name, type) ->
            val actual = that.members[name] ?: return false
            type.contains(actual)
        }
    }
}

fun objectRepeat(member: Space<FoxType>, minArity: Int, maxArity: Int) = ObjectRepeatSpace(member, minArity, maxArity)

data class ObjectRepeatSpace(val member: Space<FoxType>, val minArity: Int, val maxArity: Int) : Space<FoxType> {
    
    init {
        require(minArity >= 0) { "minArity must be non-negative: $minArity" }
        require(maxArity >= minArity) { "maxArity must be >= minArity: $maxArity < $minArity" }
    }
    
    override fun contains(that: FoxType): Boolean {
        return that is FoxObjectType && that.arity in minArity..maxArity && that.members.values.all(member::contains)
    }
}

fun objectMerge(parts: List<Space<FoxType>>) = ObjectMergeSpace(parts)

data class ObjectMergeSpace(val parts: List<Space<FoxType>>) : Space<FoxType> {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxObjectType) return false
        val allKeys = that.members.keys.toList()
        val totalBounds = objectArityBoundsOf(this)
        if (totalBounds != null && that.arity !in totalBounds.min..totalBounds.max) return false
        val partBounds = parts.map { objectArityBoundsOf(it) }
        
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

private data class ObjectArityBounds(val min: Int, val max: Int)

private fun addObjectArity(left: Int, right: Int): Int {
    if (left == Int.MAX_VALUE || right == Int.MAX_VALUE) return Int.MAX_VALUE
    return Math.addExact(left, right)
}

private fun objectArityBoundsOf(space: Space<FoxType>): ObjectArityBounds? = when (space) {
    is UniversalSpace -> ObjectArityBounds(0, Int.MAX_VALUE)
    is SingleSpace -> (space.single as? FoxObjectType)?.let { ObjectArityBounds(it.arity, it.arity) }
    is ObjectPatternSpace -> ObjectArityBounds(space.members.size, space.members.size)
    is ObjectRepeatSpace -> ObjectArityBounds(space.minArity, space.maxArity)
    is ObjectMergeSpace -> {
        var min = 0
        var max = 0
        space.parts.forEach { part ->
            val bounds = objectArityBoundsOf(part) ?: return null
            min = addObjectArity(min, bounds.min)
            max = addObjectArity(max, bounds.max)
        }
        ObjectArityBounds(min, max)
    }
    is UnionSpace<FoxType> -> {
        val bounds = space.parts.mapNotNull { objectArityBoundsOf(it) }
        if (bounds.isEmpty()) null
        else ObjectArityBounds(bounds.minOf { it.min }, bounds.maxOf { it.max })
    }
    is IntersectSpace<FoxType> -> {
        val bounds = space.parts.map { objectArityBoundsOf(it) ?: return null }
        val min = bounds.maxOf { it.min }
        val max = bounds.minOf { it.max }
        if (min > max) null else ObjectArityBounds(min, max)
    }
    is SubtractSpace<FoxType> -> objectArityBoundsOf(space.base)
    else -> null
}

private fun sizeRangeForPart(
    partBounds: List<ObjectArityBounds?>,
    partIndex: Int,
    remainingSize: Int,
): IntRange? {
    val currentBounds = partBounds[partIndex] ?: return 0..remainingSize
    var suffixMin = 0
    var suffixMax = 0
    for (index in partIndex + 1 until partBounds.size) {
        val bounds = partBounds[index] ?: return 0..remainingSize
        suffixMin = addObjectArity(suffixMin, bounds.min)
        suffixMax = addObjectArity(suffixMax, bounds.max)
    }
    if (remainingSize !in addObjectArity(currentBounds.min, suffixMin)..addObjectArity(currentBounds.max, suffixMax)) {
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

fun objectMember(generic: String, name: String) = ObjectMemberProjectionSpace(generic, name)

data class ObjectMemberProjectionSpace(val generic: String, val name: String) : PlaceHolderSpace<FoxType>

fun objectMembersOf(generic: String, names: Set<String>) = ObjectMembersProjectionSpace(generic, names)

data class ObjectMembersProjectionSpace(val generic: String, val names: Set<String>) : PlaceHolderSpace<FoxType>

fun objectDropMembersOf(generic: String, names: Set<String>) = ObjectDropMembersProjectionSpace(generic, names)

data class ObjectDropMembersProjectionSpace(val generic: String, val names: Set<String>) : PlaceHolderSpace<FoxType>
