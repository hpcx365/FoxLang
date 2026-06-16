package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxTupleType
import pers.hpcx.foxlang.ast.FoxType
import pers.hpcx.foxlang.ast.FoxVoidType
import pers.hpcx.foxlang.type.*

fun tupleProduct(vararg components: TraversableTypeSpace) = tupleProduct(components.toList())

fun tupleProduct(components: List<TraversableTypeSpace>) = TupleProductSpace(components)

data class TupleProductSpace(val components: List<TraversableTypeSpace>) : TraversableTypeSpace {
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        if (that !is FoxTupleType) return false
        if (that.arity != components.size) return false
        components.forEachIndexed { index, space ->
            if (!space.contains(that.componentAt(index), context)) return false
        }
        return true
    }
    
    override fun traverser(context: TypeSpaceContext) = object : SpaceTraverser<FoxType> {
        
        private var dirty = true
        private var current: FoxTupleType? = null
        private val traversers by lazy(LazyThreadSafetyMode.NONE) {
            components.map { it.traverser(context) }.toMutableList()
        }
        
        private fun newTraverser(index: Int) = components[index].traverser(context)
        
        override fun current(): FoxTupleType? {
            if (!dirty) return current
            while (true) {
                val firstExhausted = traversers.indexOfFirst { it.current() == null }
                if (firstExhausted < 0) {
                    dirty = false
                    current = traversers.map { it.current()!! }.toFoxTupleType()
                    break
                }
                if (firstExhausted > 0) {
                    traversers[firstExhausted - 1].seekNext()
                    (firstExhausted until traversers.size).forEach { traversers[it] = newTraverser(it) }
                    continue
                }
                exhaust()
                break
            }
            check(!dirty)
            return current
        }
        
        override fun seekNext() {
            if (current() == null) return
            if (traversers.isEmpty()) {
                exhaust()
                return
            }
            traversers.last().seekNext()
            markDirty()
        }
        
        override fun seekCeilOf(that: FoxType) {
            if (current() == null) return
            
            val family = that.family()
            if (family < ConcreteTypeFamily.TUPLE) return
            if (family > ConcreteTypeFamily.TUPLE) {
                exhaust()
                return
            }
            
            check(that is FoxTupleType)
            val targetSize = that.arity
            if (targetSize < traversers.size) return
            if (targetSize > traversers.size) {
                exhaust()
                return
            }
            
            while (true) {
                if (current() == null) return
                val firstDifference = (0 until targetSize).firstOrNull {
                    val comparison = context.compare(traversers[it].current()!!, that.componentAt(it))
                    if (comparison > 0) return
                    comparison < 0
                } ?: return
                traversers[firstDifference].seekCeilOf(that.componentAt(firstDifference))
                (firstDifference + 1 until targetSize).forEach { traversers[it] = newTraverser(it) }
                markDirty()
            }
        }
        
        private fun exhaust() {
            dirty = false
            current = null
        }
        
        private fun markDirty() {
            check(!dirty)
            dirty = true
        }
    }
}

fun tupleRepeat(component: TraversableTypeSpace, minArity: Int, maxArity: Int) = TupleRepeatSpace(component, minArity, maxArity)

data class TupleRepeatSpace(
    val component: TraversableTypeSpace,
    val minArity: Int,
    val maxArity: Int,
) : TraversableTypeSpace {
    
    init {
        require(minArity >= 0) { "minArity must be non-negative: $minArity" }
        require(maxArity >= minArity) { "maxArity must be >= minArity: $maxArity < $minArity" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        return that is FoxTupleType && that.arity in minArity..maxArity && that.components.all { component.contains(it.first, context) }
    }
    
    override fun traverser(context: TypeSpaceContext): SpaceTraverser<FoxType> = object : SpaceTraverser<FoxType> {
        
        private var dirty = true
        private var current: FoxTupleType? = null
        private val traversers by lazy(LazyThreadSafetyMode.NONE) {
            mutableListOf<SpaceTraverser<FoxType>>().apply { repeat(minArity) { add(newTraverser()) } }
        }
        
        private fun newTraverser() = component.traverser(context)
        
        override fun current(): FoxTupleType? {
            if (!dirty) return current
            while (true) {
                val firstExhausted = traversers.indexOfFirst { it.current() == null }
                if (firstExhausted < 0) {
                    dirty = false
                    current = traversers.map { it.current()!! }.toFoxTupleType()
                    break
                }
                if (firstExhausted > 0) {
                    traversers[firstExhausted - 1].seekNext()
                    (firstExhausted until traversers.size).forEach { traversers[it] = newTraverser() }
                    continue
                }
                if (traversers.size < maxArity) {
                    traversers.indices.forEach { traversers[it] = newTraverser() }
                    traversers += newTraverser()
                    continue
                }
                exhaust()
                break
            }
            check(!dirty)
            return current
        }
        
        override fun seekNext() {
            if (current() == null) return
            if (traversers.isEmpty()) {
                if (maxArity == 0) {
                    exhaust()
                    return
                }
                traversers += newTraverser()
            } else {
                traversers.last().seekNext()
            }
            markDirty()
        }
        
        override fun seekCeilOf(that: FoxType) {
            if (current() == null) return
            
            val family = that.family()
            if (family < ConcreteTypeFamily.TUPLE) return
            if (family > ConcreteTypeFamily.TUPLE) {
                exhaust()
                return
            }
            
            check(that is FoxTupleType)
            val targetSize = that.arity
            if (targetSize < traversers.size) return
            if (targetSize > maxArity) {
                exhaust()
                return
            }
            
            if (targetSize > traversers.size) {
                traversers.indices.forEach { traversers[it] = newTraverser() }
                while (targetSize > traversers.size) {
                    traversers += newTraverser()
                }
                markDirty()
            }
            
            while (true) {
                val current = current() ?: return
                if (current.arity > targetSize) return
                val firstDifference = (0 until targetSize).firstOrNull {
                    val comparison = context.compare(current.componentAt(it), that.componentAt(it))
                    if (comparison > 0) return
                    comparison < 0
                } ?: return
                traversers[firstDifference].seekCeilOf(that.componentAt(firstDifference))
                (firstDifference + 1 until targetSize).forEach { traversers[it] = newTraverser() }
                markDirty()
            }
        }
        
        private fun exhaust() {
            dirty = false
            current = null
        }
        
        private fun markDirty() {
            check(!dirty)
            dirty = true
        }
    }
}

fun tupleConcat(vararg parts: TraversableTypeSpace) = tupleConcat(parts.toList())

fun tupleConcat(parts: List<TraversableTypeSpace>) = TupleConcatSpace(parts)

data class TupleConcatSpace(val parts: List<TraversableTypeSpace>) : TraversableTypeSpace {
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        if (that !is FoxTupleType) return false
        val targetSize = that.arity
        val partBounds = parts.map { tupleArityBoundsOf(it, context) }
        val totalBounds = tupleArityBoundsOf(this, context)
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
                    space.contains(piece, context) && accepts(partIndex + 1, end)
                }
            }
            
            memo[state] = result
            return result
        }
        
        return accepts(0, 0)
    }
    
    override fun traverser(context: TypeSpaceContext) = object : SpaceTraverser<FoxType> {
        
        private var dirty = true
        private var current: FoxTupleType? = null
        private var lowerBound: FoxTupleType? = null
        private var strict = false
        private val partBounds = parts.map { tupleArityBoundsOf(it, context) }
        private val totalBounds = tupleArityBoundsOf(this@TupleConcatSpace, context)
        private val suffixBounds = tupleConcatSuffixBoundsOf(partBounds)
        private var minArity = totalBounds?.min ?: 0
        
        override fun current(): FoxTupleType? {
            if (!dirty) return current
            dirty = false
            current = if (lowerBound != null) findCeil(lowerBound!!, strict) else findFromArity(minArity)
            return current
        }
        
        override fun seekNext() {
            val value = current() ?: return
            lowerBound = value
            strict = true
            markDirty()
        }
        
        override fun seekCeilOf(that: FoxType) {
            val cur = current() ?: return
            
            val family = that.family()
            if (family < ConcreteTypeFamily.TUPLE) return
            if (family > ConcreteTypeFamily.TUPLE) {
                exhaust()
                return
            }
            
            check(that is FoxTupleType)
            if (context.compare(cur, that) >= 0) return
            lowerBound = that
            strict = false
            minArity = that.arity
            markDirty()
        }
        
        private fun findFromArity(minArity: Int): FoxTupleType? {
            val bounds = totalBounds ?: return null
            val start = maxOf(minArity, bounds.min)
            for (targetSize in start..bounds.max) {
                val candidate = findExactSize(targetSize, lower = null, strict = false)
                if (candidate != null) return candidate
            }
            return null
        }
        
        private fun findCeil(lower: FoxTupleType, strict: Boolean): FoxTupleType? {
            val bounds = totalBounds ?: return null
            val start = maxOf(lower.arity, bounds.min)
            for (targetSize in start..bounds.max) {
                val sizeLower = lower.takeIf { it.arity == targetSize }
                val candidate = findExactSize(targetSize, sizeLower, strict && sizeLower != null)
                if (candidate != null) return candidate
            }
            return null
        }
        
        private fun findExactSize(
            targetSize: Int,
            lower: FoxTupleType?,
            strict: Boolean,
        ): FoxTupleType? {
            if (lower != null && lower.arity != targetSize) return null
            val totalBounds = totalBounds ?: return null
            val suffixBounds = suffixBounds ?: return null
            if (targetSize !in totalBounds.min..totalBounds.max) return null
            
            data class State(val partIndex: Int, val offset: Int, val equalPrefix: Boolean)
            
            val memo = mutableMapOf<State, FoxTupleType?>()
            
            fun solve(partIndex: Int, offset: Int, equalPrefix: Boolean): FoxTupleType? {
                val state = State(partIndex, offset, equalPrefix)
                if (state in memo) return memo[state]
                
                fun solveTerminal(): FoxTupleType? {
                    if (offset != targetSize) return null
                    if (strict && equalPrefix) return null
                    return FoxTupleType(emptyList())
                }
                
                fun solveRecursive(): FoxTupleType? {
                    val remaining = targetSize - offset
                    val currentBounds = partBounds[partIndex] ?: return null
                    val remainingBounds = suffixBounds[partIndex]
                    if (remaining !in remainingBounds.min..remainingBounds.max) return null
                    
                    val suffix = suffixBounds[partIndex + 1]
                    val minPieceSize = maxOf(currentBounds.min, remaining - suffix.max)
                    val maxPieceSize = minOf(currentBounds.max, remaining - suffix.min)
                    if (minPieceSize > maxPieceSize) return null
                    
                    var best: FoxTupleType? = null
                    for (pieceSize in minPieceSize..maxPieceSize) {
                        val lowerPiece = if (equalPrefix) lower!!.sliceComponents(offset, offset + pieceSize) else null
                        var piece = exactTupleCeilOf(parts[partIndex], pieceSize, lowerPiece, strict = false, context = context)
                        while (piece != null) {
                            val nextEqualPrefix = lowerPiece != null && context.compare(piece, lowerPiece) == 0
                            val suffixValue = solve(partIndex + 1, offset + pieceSize, nextEqualPrefix)
                            if (suffixValue != null) {
                                val candidate = piece.mergeComponents(suffixValue)
                                val currentBest = best
                                if (currentBest == null || context.compare(candidate, currentBest) < 0) {
                                    best = candidate
                                }
                                break
                            }
                            piece = exactTupleCeilOf(parts[partIndex], pieceSize, piece, strict = true, context = context)
                        }
                    }
                    return best
                }
                
                val result = if (partIndex == parts.size) solveTerminal() else solveRecursive()
                
                memo[state] = result
                return result
            }
            
            return solve(partIndex = 0, offset = 0, equalPrefix = lower != null)
        }
        
        private fun exhaust() {
            dirty = false
            current = null
        }
        
        private fun markDirty() {
            check(!dirty)
            dirty = true
        }
    }
    
    private fun exactTupleCeilOf(
        lang: TraversableTypeSpace,
        arity: Int,
        lower: FoxTupleType?,
        strict: Boolean,
        context: TypeSpaceContext,
    ): FoxTupleType? {
        val traverser = lang.tupleTraverserOfSize(arity, context)
        if (lower != null) traverser.seekCeilOf(lower) else traverser.seekCeilOf(FoxTupleType(listOf(FoxVoidType to arity)))
        if (strict && lower != null && traverser.current()?.let { context.compare(it, lower) == 0 } == true) {
            traverser.seekNext()
        }
        return traverser.current() as FoxTupleType?
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

private fun tupleArityBoundsOf(lang: TypeSpace, context: TypeSpaceContext): TupleArityBounds? = when (lang) {
    is UniverseTypeSpace -> TupleArityBounds(0, context.bounds.maxTupleArity)
    is SingleSpace -> (lang.single as? FoxTupleType)?.let { TupleArityBounds(it.arity, it.arity) }
    is TupleComponentAtPreimageSpace -> TupleArityBounds(lang.index + 1, context.bounds.maxTupleArity)
    is TupleLastComponentAtPreimageSpace -> TupleArityBounds(lang.index + 1, context.bounds.maxTupleArity)
    is TupleFirstPreimageSpace -> when {
        lang.prefix.arity > lang.count -> null
        lang.exact && lang.prefix.arity != lang.count -> null
        !lang.exact && lang.prefix.arity < lang.count -> TupleArityBounds(lang.prefix.arity, lang.prefix.arity)
        else -> TupleArityBounds(lang.prefix.arity, context.bounds.maxTupleArity)
    }
    is TupleLastPreimageSpace -> when {
        lang.suffix.arity > lang.count -> null
        lang.exact && lang.suffix.arity != lang.count -> null
        !lang.exact && lang.suffix.arity < lang.count -> TupleArityBounds(lang.suffix.arity, lang.suffix.arity)
        else -> TupleArityBounds(lang.suffix.arity, context.bounds.maxTupleArity)
    }
    is TupleDropFirstPreimageSpace -> {
        val resultArity = lang.suffix.arity
        if (!lang.exact && resultArity == 0) TupleArityBounds(0, lang.count)
        else TupleArityBounds(addTupleArity(lang.count, resultArity), addTupleArity(lang.count, resultArity))
    }
    is TupleDropLastPreimageSpace -> {
        val resultArity = lang.prefix.arity
        if (!lang.exact && resultArity == 0) TupleArityBounds(0, lang.count)
        else TupleArityBounds(addTupleArity(resultArity, lang.count), addTupleArity(resultArity, lang.count))
    }
    is TupleProductSpace -> TupleArityBounds(lang.components.size, lang.components.size)
    is TupleRepeatSpace -> TupleArityBounds(lang.minArity, lang.maxArity)
    is TupleConcatSpace -> {
        var min = 0
        var max = 0
        lang.parts.forEach { part ->
            val bounds = tupleArityBoundsOf(part, context) ?: return null
            min = addTupleArity(min, bounds.min)
            max = addTupleArity(max, bounds.max)
        }
        TupleArityBounds(min, max)
    }
    is UnionSpace<FoxType, TypeSpaceContext> -> {
        val bounds = lang.parts.mapNotNull { tupleArityBoundsOf(it, context) }
        if (bounds.isEmpty()) null
        else TupleArityBounds(bounds.minOf { it.min }, bounds.maxOf { it.max })
    }
    is IntersectSpace<FoxType, TypeSpaceContext> -> {
        val bounds = lang.parts.map { tupleArityBoundsOf(it, context) ?: return null }
        val min = bounds.maxOf { it.min }
        val max = bounds.minOf { it.max }
        if (min > max) null else TupleArityBounds(min, max)
    }
    is SubtractSpace<FoxType, TypeSpaceContext> -> tupleArityBoundsOf(lang.base, context)
    else -> null
}

fun TraversableTypeSpace.tupleTraverser(context: TypeSpaceContext) = object : SpaceTraverser<FoxType> {
    
    private val backend = this@tupleTraverser.traverser(context)
    
    override fun current(): FoxTupleType? {
        while (true) {
            val family = backend.current()?.family() ?: return null
            if (family > ConcreteTypeFamily.TUPLE) return null
            if (family == ConcreteTypeFamily.TUPLE) return backend.current() as FoxTupleType
            backend.seekCeilOf(FoxTupleType(emptyList()))
        }
    }
    
    override fun seekNext() {
        if (current() == null) return
        backend.seekNext()
    }
    
    override fun seekCeilOf(that: FoxType) {
        if (current() == null) return
        backend.seekCeilOf(that)
    }
}

fun TraversableTypeSpace.tupleTraverserOfSize(size: Int, context: TypeSpaceContext) = object : SpaceTraverser<FoxType> {
    
    private val backend = this@tupleTraverserOfSize.tupleTraverser(context)
    
    override fun current(): FoxTupleType? {
        while (true) {
            val current = backend.current() ?: return null
            check(current is FoxTupleType)
            if (current.arity > size) return null
            if (current.arity == size) return current
            backend.seekCeilOf(FoxTupleType(listOf(FoxVoidType to size)))
        }
    }
    
    override fun seekNext() {
        if (current() == null) return
        backend.seekNext()
    }
    
    override fun seekCeilOf(that: FoxType) {
        if (current() == null) return
        backend.seekCeilOf(that)
    }
}

fun tupleComponentAt(baseSpace: TraversableTypeSpace, index: Int) = TupleComponentAtProjectiveSpace(baseSpace, index)

data class TupleComponentAtProjectiveSpace(
    override val baseSpace: TraversableTypeSpace,
    val index: Int,
) : ProjectiveSpace<FoxType, TypeSpaceContext> {
    
    init {
        require(index >= 0) { "index must be non-negative: $index" }
    }
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return TupleComponentAtPreimageSpace(index, singleSpace(that))
    }
}

data class TupleComponentAtPreimageSpace(
    val index: Int,
    val component: TraversableTypeSpace,
) : TraversableTypeSpace {
    
    init {
        require(index >= 0) { "index must be non-negative: $index" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        return that is FoxTupleType && that.arity > index && component.contains(that.componentAt(index), context)
    }
    
    override fun traverser(context: TypeSpaceContext): SpaceTraverser<FoxType> {
        val maxArity = context.bounds.maxTupleArity
        if (maxArity <= index) return emptySpace<FoxType, TypeSpaceContext>().tupleTraverser(context)
        val any = universe(context.bounds.maxHeight)
        return tupleConcat(
            tupleRepeat(any, index, index),
            tupleProduct(component),
            tupleRepeat(any, 0, maxArity - index - 1),
        ).traverser(context)
    }
}

fun tupleLastComponentAt(baseSpace: TraversableTypeSpace, index: Int) = TupleLastComponentAtProjectiveSpace(baseSpace, index)

data class TupleLastComponentAtProjectiveSpace(
    override val baseSpace: TraversableTypeSpace,
    val index: Int,
) : ProjectiveSpace<FoxType, TypeSpaceContext> {
    
    init {
        require(index >= 0) { "index must be non-negative: $index" }
    }
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return TupleLastComponentAtPreimageSpace(index, singleSpace(that))
    }
}

data class TupleLastComponentAtPreimageSpace(
    val index: Int,
    val component: TraversableTypeSpace,
) : TraversableTypeSpace {
    
    init {
        require(index >= 0) { "index must be non-negative: $index" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        return that is FoxTupleType && that.arity > index && component.contains(that.lastComponentAt(index), context)
    }
    
    override fun traverser(context: TypeSpaceContext): SpaceTraverser<FoxType> {
        val maxArity = context.bounds.maxTupleArity
        if (maxArity <= index) return emptySpace<FoxType, TypeSpaceContext>().tupleTraverser(context)
        val any = universe(context.bounds.maxHeight)
        return tupleConcat(
            tupleRepeat(any, 0, maxArity - index - 1),
            tupleProduct(component),
            tupleRepeat(any, index, index),
        ).traverser(context)
    }
}

fun tupleFirstOf(baseSpace: TraversableTypeSpace, count: Int, exact: Boolean) = TupleFirstProjectiveSpace(baseSpace, count, exact)

data class TupleFirstProjectiveSpace(
    override val baseSpace: TraversableTypeSpace,
    val count: Int,
    val exact: Boolean,
) : ProjectiveSpace<FoxType, TypeSpaceContext> {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return if (that is FoxTupleType) TupleFirstPreimageSpace(count, that, exact) else emptySpace()
    }
}

data class TupleFirstPreimageSpace(
    val count: Int,
    val prefix: FoxTupleType,
    val exact: Boolean,
) : TraversableTypeSpace {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        if (that !is FoxTupleType) return false
        return when {
            prefix.arity > count -> false
            exact && prefix.arity != count -> false
            that.arity < prefix.arity -> false
            context.compare(that.firstComponents(prefix.arity), prefix) != 0 -> false
            prefix.arity < count -> that.arity == prefix.arity
            else -> true
        }
    }
    
    override fun traverser(context: TypeSpaceContext): SpaceTraverser<FoxType> {
        return when {
            prefix.arity > count -> emptySpace<FoxType, TypeSpaceContext>().tupleTraverser(context)
            exact && prefix.arity != count -> emptySpace<FoxType, TypeSpaceContext>().tupleTraverser(context)
            !exact && prefix.arity < count -> singleSpace<FoxType, TypeSpaceContext>(prefix).tupleTraverser(context)
            else -> tupleConcat(
                singleSpace(prefix),
                tupleRepeat(universe(context.bounds.maxHeight), 0, context.bounds.maxTupleArity - prefix.arity),
            ).traverser(context)
        }
    }
}

fun tupleLastOf(baseSpace: TraversableTypeSpace, count: Int, exact: Boolean) = TupleLastProjectiveSpace(baseSpace, count, exact)

data class TupleLastProjectiveSpace(
    override val baseSpace: TraversableTypeSpace,
    val count: Int,
    val exact: Boolean,
) : ProjectiveSpace<FoxType, TypeSpaceContext> {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return if (that is FoxTupleType) TupleLastPreimageSpace(count, that, exact) else emptySpace()
    }
}

data class TupleLastPreimageSpace(
    val count: Int,
    val suffix: FoxTupleType,
    val exact: Boolean,
) : TraversableTypeSpace {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        if (that !is FoxTupleType) return false
        return when {
            suffix.arity > count -> false
            exact && suffix.arity != count -> false
            that.arity < suffix.arity -> false
            context.compare(that.lastComponents(suffix.arity), suffix) != 0 -> false
            suffix.arity < count -> that.arity == suffix.arity
            else -> true
        }
    }
    
    override fun traverser(context: TypeSpaceContext): SpaceTraverser<FoxType> {
        return when {
            suffix.arity > count -> emptySpace<FoxType, TypeSpaceContext>().tupleTraverser(context)
            exact && suffix.arity != count -> emptySpace<FoxType, TypeSpaceContext>().tupleTraverser(context)
            !exact && suffix.arity < count -> singleSpace<FoxType, TypeSpaceContext>(suffix).tupleTraverser(context)
            else -> tupleConcat(
                tupleRepeat(universe(context.bounds.maxHeight), 0, context.bounds.maxTupleArity - suffix.arity),
                singleSpace(suffix),
            ).traverser(context)
        }
    }
}

fun tupleDropFirstOf(baseSpace: TraversableTypeSpace, count: Int, exact: Boolean) = TupleDropFirstProjectiveSpace(baseSpace, count, exact)

data class TupleDropFirstProjectiveSpace(
    override val baseSpace: TraversableTypeSpace,
    val count: Int,
    val exact: Boolean,
) : ProjectiveSpace<FoxType, TypeSpaceContext> {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return if (that is FoxTupleType) TupleDropFirstPreimageSpace(count, that, exact) else emptySpace()
    }
}

data class TupleDropFirstPreimageSpace(
    val count: Int,
    val suffix: FoxTupleType,
    val exact: Boolean,
) : TraversableTypeSpace {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        if (that !is FoxTupleType) return false
        if (that.arity < count) {
            return !exact && suffix.arity == 0
        }
        if (!exact && suffix.arity == 0 && that.arity <= count) {
            return true
        }
        return context.compare(that.dropFirstComponents(count), suffix) == 0
    }
    
    override fun traverser(context: TypeSpaceContext): SpaceTraverser<FoxType> {
        val any = universe(context.bounds.maxHeight)
        return if (!exact && suffix.arity == 0) {
            tupleRepeat(any, 0, count).traverser(context)
        } else {
            tupleConcat(
                tupleRepeat(any, count, count),
                singleSpace(suffix),
            ).traverser(context)
        }
    }
}

fun tupleDropLastOf(baseSpace: TraversableTypeSpace, count: Int, exact: Boolean) = TupleDropLastProjectiveSpace(baseSpace, count, exact)

data class TupleDropLastProjectiveSpace(
    override val baseSpace: TraversableTypeSpace,
    val count: Int,
    val exact: Boolean,
) : ProjectiveSpace<FoxType, TypeSpaceContext> {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return if (that is FoxTupleType) TupleDropLastPreimageSpace(count, that, exact) else emptySpace()
    }
}

data class TupleDropLastPreimageSpace(
    val count: Int,
    val prefix: FoxTupleType,
    val exact: Boolean,
) : TraversableTypeSpace {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        if (that !is FoxTupleType) return false
        if (that.arity < count) {
            return !exact && prefix.arity == 0
        }
        if (!exact && prefix.arity == 0 && that.arity <= count) {
            return true
        }
        return context.compare(that.dropLastComponents(count), prefix) == 0
    }
    
    override fun traverser(context: TypeSpaceContext): SpaceTraverser<FoxType> {
        val any = universe(context.bounds.maxHeight)
        return if (!exact && prefix.arity == 0) {
            tupleRepeat(any, 0, count).traverser(context)
        } else {
            tupleConcat(
                singleSpace(prefix),
                tupleRepeat(any, count, count),
            ).traverser(context)
        }
    }
}
