package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.FoxEnumType
import pers.hpcx.foxlang.ast.FoxObjectType
import pers.hpcx.foxlang.ast.FoxStructType
import pers.hpcx.foxlang.ast.FoxTupleType

val FoxTupleType.arity get() = components.size
val FoxStructType.arity get() = fields.size
val FoxObjectType.arity get() = members.size
val FoxEnumType.arity get() = entries.size

val INFINITY_ARITY_BOUNDS = ArityBounds(0, Int.MAX_VALUE)
fun fixedArityBounds(arity: Int) = ArityBounds(arity, arity)
fun addArityBounds(bounds: Iterable<ArityBounds?>): ArityBounds? {
    var min = 0
    var max = 0
    bounds.forEach {
        if (it == null) return null
        min = addArity(min, it.min)
        max = addArity(max, it.max)
    }
    return ArityBounds(min, max)
}

data class ArityBounds(val min: Int, val max: Int) {
    
    init {
        require(min >= 0)
        require(min <= max)
    }
    
    operator fun contains(that: Int) = that in min..max
}

fun arityBoundsOf(space: TypeSpace): ArityBounds? = when (space) {
    is GeneralTypeSpace -> when (space) {
        is EmptyTypeSpace -> null
        is UniversalTypeSpace -> INFINITY_ARITY_BOUNDS
        is SingleTypeSpace -> (space.single as? FoxTupleType)?.let { fixedArityBounds(it.arity) }
        is IntersectTypeSpace -> {
            val bounds = space.parts.map { arityBoundsOf(it) ?: return null }
            val min = bounds.maxOf { it.min }
            val max = bounds.minOf { it.max }
            if (min > max) null else ArityBounds(min, max)
        }
        is UnionTypeSpace -> {
            val bounds = space.parts.mapNotNull { arityBoundsOf(it) }
            if (bounds.isEmpty()) null else ArityBounds(bounds.minOf { it.min }, bounds.maxOf { it.max })
        }
        is ComplementTypeSpace -> INFINITY_ARITY_BOUNDS
    }
    is TupleSubTypeSpace -> when (space) {
        TupleTypeSpace -> INFINITY_ARITY_BOUNDS
        is TupleRepeatTypeSpace -> INFINITY_ARITY_BOUNDS
        is TuplePatternTypeSpace -> ArityBounds(space.components.size, space.components.size)
        is TupleConcatTypeSpace -> addArityBounds(space.parts.map { arityBoundsOf(it) })
    }
    is StructSubTypeSpace -> when (space) {
        StructTypeSpace -> INFINITY_ARITY_BOUNDS
        is StructPatternTypeSpace -> fixedArityBounds(space.fieldTypes.size)
        is StructFieldPatternTypeSpace -> fixedArityBounds(space.fields.size)
        is StructConcatTypeSpace -> addArityBounds(space.parts.map { arityBoundsOf(it) })
    }
    is ObjectSubTypeSpace -> when (space) {
        ObjectTypeSpace -> INFINITY_ARITY_BOUNDS
        is ObjectPatternTypeSpace -> fixedArityBounds(space.members.size)
        is ObjectMergeTypeSpace -> addArityBounds(space.parts.map { arityBoundsOf(it) })
    }
    is EnumSubTypeSpace -> when (space) {
        EnumTypeSpace -> INFINITY_ARITY_BOUNDS
        is EnumPatternTypeSpace -> fixedArityBounds(space.entries.size)
        is EnumMergeTypeSpace -> addArityBounds(space.parts.map { arityBoundsOf(it) })
    }
    is ArraySubTypeSpace -> null
    is RefSubTypeSpace -> null
    is MethodSubTypeSpace -> null
}

fun addArity(left: Int, right: Int): Int =
    if (left == Int.MAX_VALUE || right == Int.MAX_VALUE) Int.MAX_VALUE
    else Math.addExact(left, right)

fun sizeRangeForPart(
    partBounds: List<ArityBounds?>,
    partIndex: Int,
    remainingSize: Int,
): IntRange? {
    val currentBounds = partBounds[partIndex] ?: return 0..remainingSize
    var suffixMin = 0
    var suffixMax = 0
    for (index in partIndex + 1 until partBounds.size) {
        val bounds = partBounds[index] ?: return 0..remainingSize
        suffixMin = addArity(suffixMin, bounds.min)
        suffixMax = addArity(suffixMax, bounds.max)
    }
    if (remainingSize !in addArity(currentBounds.min, suffixMin)..addArity(currentBounds.max, suffixMax)) {
        return null
    }
    val minPieceSize = maxOf(currentBounds.min, remainingSize - suffixMax)
    val maxPieceSize = minOf(currentBounds.max, remainingSize - suffixMin)
    if (minPieceSize > maxPieceSize) return null
    return minPieceSize..maxPieceSize
}

fun concatSuffixBoundsOf(partBounds: List<ArityBounds?>): List<ArityBounds>? {
    if (partBounds.any { it == null }) return null
    return buildList(partBounds.size + 1) {
        repeat(partBounds.size + 1) { add(ArityBounds(0, 0)) }
    }.toMutableList().also { bounds ->
        for (index in partBounds.size - 1 downTo 0) {
            val current = partBounds[index]!!
            val suffix = bounds[index + 1]
            bounds[index] = ArityBounds(
                addArity(current.min, suffix.min),
                addArity(current.max, suffix.max),
            )
        }
    }
}
