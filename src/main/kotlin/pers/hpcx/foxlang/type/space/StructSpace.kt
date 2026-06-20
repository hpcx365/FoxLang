package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxStructType
import pers.hpcx.foxlang.ast.FoxType
import pers.hpcx.foxlang.type.*
import pers.hpcx.foxlang.type.space.NameDictionary.StructFieldNames
import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.mutableOrderedMapOf

fun structFieldSpace(name: NameSpace, type: TraversableTypeSpace) = StructFieldSpace(name, type)

data class StructFieldSpace(
    val name: NameSpace,
    val type: TraversableTypeSpace,
) : TraversableSpace<StructField, StructFieldSpaceContextView> {
    
    fun contains(that: Map.Entry<String, FoxType>, context: TypeSpaceContext): Boolean {
        return contains(that.let { it.key to it.value }, context.structFieldSpaceContext)
    }
    
    override fun contains(that: StructField, context: StructFieldSpaceContextView): Boolean {
        return name.contains(that.first, context.context.structFieldNameSpaceContext) && type.contains(that.second, context.context)
    }
    
    override fun traverser(context: StructFieldSpaceContextView) = object : SpaceTraverser<StructField> {
        
        private var dirty = true
        private var current: StructField? = null
        private val nameTraverser = name.traverser(context.context.structFieldNameSpaceContext)
        private var typeTraverser = type.traverser(context.context)
        
        override fun current(): StructField? {
            if (!dirty) return current
            while (true) {
                val name = nameTraverser.current() ?: run {
                    exhaust()
                    break
                }
                val type = typeTraverser.current() ?: run {
                    nameTraverser.seekNext()
                    typeTraverser = type.traverser(context.context)
                    continue
                }
                dirty = false
                current = name to type
                break
            }
            check(!dirty)
            return current
        }
        
        override fun seekNext() {
            if (current() == null) return
            typeTraverser.seekNext()
            markDirty()
        }
        
        override fun seekCeilOf(that: StructField) {
            while (true) {
                if (current() == null) return
                val name = nameTraverser.current()!!
                val nameComparison = context.context.compareNames(name, that.first, StructFieldNames)
                if (nameComparison > 0) return
                if (nameComparison < 0) {
                    nameTraverser.seekCeilOf(that.first)
                    typeTraverser = type.traverser(context.context)
                    markDirty()
                    continue
                }
                val type = typeTraverser.current()!!
                val typeComparison = context.context.compare(type, that.second)
                if (typeComparison > 0) return
                if (typeComparison < 0) {
                    typeTraverser.seekCeilOf(that.second)
                    markDirty()
                    continue
                }
                return
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

fun structPattern(vararg fields: StructFieldSpace) = structPattern(fields.toList())

fun structPattern(fields: List<StructFieldSpace>) = StructPatternSpace(fields)

private fun anyStructFieldSpace(): StructFieldSpace {
    return structFieldSpace(
        universalNameSpace(StructFieldNames),
        universalTypeSpace(),
    )
}

private fun fixedStructFieldSpaces(struct: FoxStructType): List<StructFieldSpace> {
    return struct.fields.entries.map { (name, type) ->
        structFieldSpace(
            singleSpace(name),
            singleSpace(type),
        )
    }
}

private fun arityGrowingStructTraverser(
    context: TypeSpaceContext,
    initialArity: Int,
    maxArity: Int = Int.MAX_VALUE,
    buildSpace: (Int) -> TraversableTypeSpace,
): SpaceTraverser<FoxType> {
    require(initialArity >= 0) { "initialArity must be non-negative: $initialArity" }
    require(maxArity >= initialArity) { "maxArity must be >= initialArity: $maxArity < $initialArity" }
    
    return object : SpaceTraverser<FoxType> {
        
        private var dirty = true
        private var current: FoxStructType? = null
        private var currentArity = initialArity
        private var backend: SpaceTraverser<FoxType>? = null
        
        override fun current(): FoxStructType? {
            if (!dirty) return current
            while (true) {
                if (backend == null && currentArity <= maxArity) {
                    backend = buildSpace(currentArity).traverser(context)
                }
                val candidate = backend?.current() as FoxStructType?
                if (candidate != null) {
                    dirty = false
                    current = candidate
                    break
                }
                if (currentArity == maxArity) {
                    exhaust()
                    break
                }
                currentArity++
                backend = null
            }
            check(!dirty)
            return current
        }
        
        override fun seekNext() {
            if (current() == null) return
            backend!!.seekNext()
            markDirty()
        }
        
        override fun seekCeilOf(that: FoxType) {
            if (current() == null) return
            
            val family = that.family()
            if (family < ConcreteTypeFamily.STRUCT) return
            if (family > ConcreteTypeFamily.STRUCT) {
                exhaust()
                return
            }
            
            check(that is FoxStructType)
            val targetArity = that.arity
            if (targetArity < currentArity) return
            if (targetArity > maxArity) {
                exhaust()
                return
            }
            
            if (targetArity > currentArity) {
                currentArity = targetArity
                backend = null
                markDirty()
            }
            
            val cur = current() ?: return
            if (context.compare(cur, that) >= 0) return
            backend!!.seekCeilOf(that)
            markDirty()
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

data class StructPatternSpace(val fields: List<StructFieldSpace>) : TraversableTypeSpace {
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        if (that !is FoxStructType) return false
        if (that.arity != fields.size) return false
        return fields.zip(that.fields).all { (expectedField, thatField) -> expectedField.contains(thatField, context) }
    }
    
    override fun traverser(context: TypeSpaceContext) = object : SpaceTraverser<FoxType> {
        
        private var dirty = true
        private var current: FoxStructType? = null
        private val cursors by lazy(LazyThreadSafetyMode.NONE) {
            fields.map { FieldCursor(it) }.toMutableList()
        }
        
        override fun current(): FoxStructType? {
            if (!dirty) return current
            if (cursors.isEmpty()) {
                dirty = false
                current = FoxStructType(mutableOrderedMapOf())
                return current
            }
            
            while (true) {
                val usedNames = HashSet<String>(cursors.size)
                var exhausted = false
                for (index in cursors.indices) {
                    val field = cursors[index].current()
                    if (field == null) {
                        if (index > 0) {
                            cursors[index - 1].seekNext()
                            resetSuffix(index)
                        } else {
                            exhaust()
                        }
                        exhausted = true
                        break
                    }
                    if (!usedNames.add(field.first)) {
                        cursors[index].seekStrictlyAfterName(field.first)
                        resetSuffix(index + 1)
                        exhausted = true
                        break
                    }
                }
                if (exhausted) {
                    if (!dirty) break
                    continue
                }
                
                dirty = false
                current = mutableOrderedMapOf<String, FoxType>().also { result ->
                    cursors.forEach { cursor ->
                        val field = cursor.current()!!
                        result[field.first] = field.second
                    }
                }.let(::FoxStructType)
                break
            }
            check(!dirty)
            return current
        }
        
        override fun seekNext() {
            if (current() == null) return
            if (cursors.isEmpty()) {
                exhaust()
                return
            }
            cursors.last().seekNext()
            markDirty()
        }
        
        override fun seekCeilOf(that: FoxType) {
            if (current() == null) return
            
            val family = that.family()
            if (family < ConcreteTypeFamily.STRUCT) return
            if (family > ConcreteTypeFamily.STRUCT) {
                exhaust()
                return
            }
            
            check(that is FoxStructType)
            val targetSize = that.arity
            if (targetSize < cursors.size) return
            if (targetSize > cursors.size) {
                exhaust()
                return
            }
            
            while (true) {
                if (current() == null) return
                val firstDifference = (0 until targetSize).firstOrNull {
                    val comparison = context.compareStructField(
                        cursors[it].current()!!,
                        that.fieldAt(it).let { entry -> entry.key to entry.value },
                    )
                    if (comparison > 0) return
                    comparison < 0
                } ?: return
                cursors[firstDifference].seekCeilOf(that.fieldAt(firstDifference).let { it.key to it.value })
                resetSuffix(firstDifference + 1)
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
        
        private fun resetSuffix(beginIndex: Int) {
            for (index in beginIndex until cursors.size) {
                cursors[index] = FieldCursor(fields[index])
            }
        }
        
        private inner class FieldCursor(
            private val fieldSpace: StructFieldSpace,
        ) {
            
            private val nameContext = context.structFieldNameSpaceContext
            private var nameTraverser = fieldSpace.name.traverser(nameContext)
            private var typeTraverser = fieldSpace.type.traverser(context)
            
            fun current(): StructField? {
                while (true) {
                    val name = nameTraverser.current() ?: return null
                    val type = typeTraverser.current() ?: run {
                        nameTraverser.seekNext()
                        typeTraverser = fieldSpace.type.traverser(context)
                        continue
                    }
                    return name to type
                }
            }
            
            fun seekNext() {
                if (current() == null) return
                typeTraverser.seekNext()
            }
            
            fun seekCeilOf(that: StructField) {
                while (true) {
                    val current = current() ?: return
                    val nameComparison = context.compareNames(current.first, that.first, StructFieldNames)
                    if (nameComparison > 0) return
                    if (nameComparison < 0) {
                        nameTraverser.seekCeilOf(that.first)
                        typeTraverser = fieldSpace.type.traverser(context)
                        continue
                    }
                    val typeComparison = context.compare(current.second, that.second)
                    if (typeComparison >= 0) return
                    typeTraverser.seekCeilOf(that.second)
                }
            }
            
            fun seekStrictlyAfterName(name: String) {
                while (true) {
                    val current = current() ?: return
                    val nameComparison = context.compareNames(current.first, name, StructFieldNames)
                    if (nameComparison > 0) return
                    nameTraverser.seekCeilOf(name)
                    if (nameTraverser.current()?.let { context.compareNames(it, name, StructFieldNames) == 0 } == true) {
                        nameTraverser.seekNext()
                    }
                    typeTraverser = fieldSpace.type.traverser(context)
                }
            }
        }
    }
}

fun structRepeat(field: StructFieldSpace, minArity: Int, maxArity: Int) = StructRepeatSpace(field, minArity, maxArity)

data class StructRepeatSpace(val field: StructFieldSpace, val minArity: Int, val maxArity: Int) : TraversableTypeSpace {
    
    init {
        require(minArity >= 0) { "minArity must be non-negative: $minArity" }
        require(maxArity >= minArity) { "maxArity must be >= minArity: $maxArity < $minArity" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        return that is FoxStructType && that.arity in minArity..maxArity && that.fields.all { field.contains(it, context) }
    }
    
    override fun traverser(context: TypeSpaceContext) = arityGrowingStructTraverser(context, minArity, maxArity) { arity ->
        structPattern(List(arity) { field })
    }
}

fun structFieldAt(baseSpace: TraversableTypeSpace, index: Int) = StructFieldAtProjectionSpace(baseSpace, index)

data class StructFieldAtProjectionSpace(
    override val baseSpace: TraversableTypeSpace,
    val index: Int,
) : ProjectionTypeSpace {
    
    init {
        require(index >= 0) { "index must be non-negative: $index" }
    }
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return StructFieldAtPreimageSpace(index, singleSpace(that))
    }
}

data class StructFieldAtPreimageSpace(
    val index: Int,
    val fieldType: TraversableTypeSpace,
) : TraversableTypeSpace {
    
    init {
        require(index >= 0) { "index must be non-negative: $index" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        return that is FoxStructType && that.arity > index && fieldType.contains(that.fieldAt(index).value, context)
    }
    
    override fun traverser(context: TypeSpaceContext) = arityGrowingStructTraverser(context, index + 1) { arity ->
        val anyField = anyStructFieldSpace()
        structPattern(
            List(arity) { fieldIndex ->
                if (fieldIndex == index) {
                    structFieldSpace(
                        universalNameSpace(StructFieldNames),
                        fieldType,
                    )
                } else {
                    anyField
                }
            },
        )
    }
}

fun structLastFieldAt(baseSpace: TraversableTypeSpace, index: Int) = StructLastFieldAtProjectionSpace(baseSpace, index)

data class StructLastFieldAtProjectionSpace(
    override val baseSpace: TraversableTypeSpace,
    val index: Int,
) : ProjectionTypeSpace {
    
    init {
        require(index >= 0) { "index must be non-negative: $index" }
    }
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return StructLastFieldAtPreimageSpace(index, singleSpace(that))
    }
}

data class StructLastFieldAtPreimageSpace(
    val index: Int,
    val fieldType: TraversableTypeSpace,
) : TraversableTypeSpace {
    
    init {
        require(index >= 0) { "index must be non-negative: $index" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        return that is FoxStructType && that.arity > index && fieldType.contains(that.lastFieldAt(index).value, context)
    }
    
    override fun traverser(context: TypeSpaceContext) = arityGrowingStructTraverser(context, index + 1) { arity ->
        val anyField = anyStructFieldSpace()
        structPattern(
            List(arity) { fieldIndex ->
                if (fieldIndex == arity - 1 - index) {
                    structFieldSpace(
                        universalNameSpace(StructFieldNames),
                        fieldType,
                    )
                } else {
                    anyField
                }
            },
        )
    }
}

fun structFirstOf(baseSpace: TraversableTypeSpace, count: Int, exact: Boolean) = StructFirstProjectionSpace(baseSpace, count, exact)

data class StructFirstProjectionSpace(
    override val baseSpace: TraversableTypeSpace,
    val count: Int,
    val exact: Boolean,
) : ProjectionTypeSpace {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return if (that is FoxStructType) StructFirstPreimageSpace(count, that, exact) else emptySpace()
    }
}

data class StructFirstPreimageSpace(
    val count: Int,
    val prefix: FoxStructType,
    val exact: Boolean,
) : TraversableTypeSpace {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        if (that !is FoxStructType) return false
        return when {
            prefix.arity > count -> false
            exact && prefix.arity != count -> false
            that.arity < prefix.arity -> false
            context.compareStruct(that.firstFields(prefix.arity), prefix) != 0 -> false
            prefix.arity < count -> that.arity == prefix.arity
            else -> true
        }
    }
    
    override fun traverser(context: TypeSpaceContext): SpaceTraverser<FoxType> {
        return when {
            prefix.arity > count -> emptySpace<FoxType, TypeSpaceContext>().structTraverser(context)
            exact && prefix.arity != count -> emptySpace<FoxType, TypeSpaceContext>().structTraverser(context)
            !exact && prefix.arity < count -> singleSpace<FoxType, TypeSpaceContext>(prefix).structTraverser(context)
            else -> arityGrowingStructTraverser(context, prefix.arity) { arity ->
                structPattern(
                    fixedStructFieldSpaces(prefix) + List(arity - prefix.arity) { anyStructFieldSpace() },
                )
            }
        }
    }
}

fun structLastOf(baseSpace: TraversableTypeSpace, count: Int, exact: Boolean) = StructLastProjectionSpace(baseSpace, count, exact)

data class StructLastProjectionSpace(
    override val baseSpace: TraversableTypeSpace,
    val count: Int,
    val exact: Boolean,
) : ProjectionTypeSpace {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return if (that is FoxStructType) StructLastPreimageSpace(count, that, exact) else emptySpace()
    }
}

data class StructLastPreimageSpace(
    val count: Int,
    val suffix: FoxStructType,
    val exact: Boolean,
) : TraversableTypeSpace {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        if (that !is FoxStructType) return false
        return when {
            suffix.arity > count -> false
            exact && suffix.arity != count -> false
            that.arity < suffix.arity -> false
            context.compareStruct(that.lastFields(suffix.arity), suffix) != 0 -> false
            suffix.arity < count -> that.arity == suffix.arity
            else -> true
        }
    }
    
    override fun traverser(context: TypeSpaceContext): SpaceTraverser<FoxType> {
        return when {
            suffix.arity > count -> emptySpace<FoxType, TypeSpaceContext>().structTraverser(context)
            exact && suffix.arity != count -> emptySpace<FoxType, TypeSpaceContext>().structTraverser(context)
            !exact && suffix.arity < count -> singleSpace<FoxType, TypeSpaceContext>(suffix).structTraverser(context)
            else -> arityGrowingStructTraverser(context, suffix.arity) { arity ->
                structPattern(
                    List(arity - suffix.arity) { anyStructFieldSpace() } + fixedStructFieldSpaces(suffix),
                )
            }
        }
    }
}

fun structDropFirstOf(baseSpace: TraversableTypeSpace, count: Int, exact: Boolean) = StructDropFirstProjectionSpace(baseSpace, count, exact)

data class StructDropFirstProjectionSpace(
    override val baseSpace: TraversableTypeSpace,
    val count: Int,
    val exact: Boolean,
) : ProjectionTypeSpace {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return if (that is FoxStructType) StructDropFirstPreimageSpace(count, that, exact) else emptySpace()
    }
}

data class StructDropFirstPreimageSpace(
    val count: Int,
    val suffix: FoxStructType,
    val exact: Boolean,
) : TraversableTypeSpace {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        if (that !is FoxStructType) return false
        if (that.arity < count) {
            return !exact && suffix.arity == 0
        }
        if (!exact && suffix.arity == 0 && that.arity <= count) {
            return true
        }
        return context.compareStruct(that.dropFirstFields(count), suffix) == 0
    }
    
    override fun traverser(context: TypeSpaceContext): SpaceTraverser<FoxType> {
        return if (!exact && suffix.arity == 0) {
            structRepeat(anyStructFieldSpace(), 0, count).structTraverser(context)
        } else {
            arityGrowingStructTraverser(context, count + suffix.arity, count + suffix.arity) { arity ->
                check(arity == count + suffix.arity)
                structPattern(
                    List(count) { anyStructFieldSpace() } + fixedStructFieldSpaces(suffix),
                )
            }
        }
    }
}

fun structDropLastOf(baseSpace: TraversableTypeSpace, count: Int, exact: Boolean) = StructDropLastProjectionSpace(baseSpace, count, exact)

data class StructDropLastProjectionSpace(
    override val baseSpace: TraversableTypeSpace,
    val count: Int,
    val exact: Boolean,
) : ProjectionTypeSpace {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return if (that is FoxStructType) StructDropLastPreimageSpace(count, that, exact) else emptySpace()
    }
}

data class StructDropLastPreimageSpace(
    val count: Int,
    val prefix: FoxStructType,
    val exact: Boolean,
) : TraversableTypeSpace {
    
    init {
        require(count >= 0) { "count must be non-negative: $count" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        if (that !is FoxStructType) return false
        if (that.arity < count) {
            return !exact && prefix.arity == 0
        }
        if (!exact && prefix.arity == 0 && that.arity <= count) {
            return true
        }
        return context.compareStruct(that.dropLastFields(count), prefix) == 0
    }
    
    override fun traverser(context: TypeSpaceContext): SpaceTraverser<FoxType> {
        return if (!exact && prefix.arity == 0) {
            structRepeat(anyStructFieldSpace(), 0, count).structTraverser(context)
        } else {
            arityGrowingStructTraverser(context, prefix.arity + count, prefix.arity + count) { arity ->
                check(arity == prefix.arity + count)
                structPattern(
                    fixedStructFieldSpaces(prefix) + List(count) { anyStructFieldSpace() },
                )
            }
        }
    }
}

fun TraversableTypeSpace.structTraverser(context: TypeSpaceContext) = object : SpaceTraverser<FoxType> {
    
    private val backend = this@structTraverser.traverser(context)
    
    override fun current(): FoxStructType? {
        while (true) {
            val family = backend.current()?.family() ?: return null
            if (family > ConcreteTypeFamily.STRUCT) return null
            if (family == ConcreteTypeFamily.STRUCT) return backend.current() as FoxStructType
            backend.seekCeilOf(FoxStructType(emptyOrderedMap()))
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
