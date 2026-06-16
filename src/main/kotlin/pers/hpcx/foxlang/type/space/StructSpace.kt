package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxStructType
import pers.hpcx.foxlang.ast.FoxType
import pers.hpcx.foxlang.type.ConcreteTypeFamily
import pers.hpcx.foxlang.type.arity
import pers.hpcx.foxlang.type.family

typealias StructField = Pair<String, FoxType>

fun structField(name: NameSpace, type: TraversableTypeSpace) = StructFieldSpace(name, type)

data class StructFieldSpace(
    val name: NameSpace,
    val type: TraversableTypeSpace,
) : TraversableSpace<StructField, StructFieldSpaceContextView> {
    
    fun contains(that: Map.Entry<String, FoxType>, context: TypeSpaceContext): Boolean {
        return contains(that.let { it.key to it.value }, context.structFieldSpaceContext)
    }
    
    override fun contains(that: StructField, context: StructFieldSpaceContextView): Boolean {
        return name.contains(that.first, context.context.nameSpaceContext) && type.contains(that.second, context.context)
    }
    
    override fun traverser(context: StructFieldSpaceContextView) = object : SpaceTraverser<StructField> {
        
        private var dirty = true
        private var current: StructField? = null
        private val nameTraverser = name.traverser(context.context.nameSpaceContext)
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
                val nameComparison = context.context.compareNames(name, that.first)
                if (nameComparison > 0) return
                if (nameComparison < 0) {
                    nameTraverser.seekCeilOf(name)
                    typeTraverser = type.traverser(context.context)
                    markDirty()
                    continue
                }
                val type = typeTraverser.current()!!
                val typeComparison = context.context.compare(type, that.second)
                if (typeComparison > 0) return
                if (typeComparison < 0) {
                    typeTraverser.seekCeilOf(type)
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

fun structPattern(fields: List<StructFieldSpace>) = StructPatternSpace(fields)

data class StructPatternSpace(val fields: List<StructFieldSpace>) : TraversableTypeSpace {
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        if (that !is FoxStructType) return false
        if (that.arity != fields.size) return false
        return fields.zip(that.fields).all { (expectedField, thatField) -> expectedField.contains(thatField, context) }
    }
    
    override fun traverser(context: TypeSpaceContext) = object : SpaceTraverser<FoxType> {
        
        private var dirty = true
        private var current: FoxStructType? = null
        
        override fun current(): FoxStructType? = TODO()
        
        override fun seekNext() = TODO()
        
        override fun seekCeilOf(that: FoxType) = TODO()
        
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

fun structRepeat(field: TraversableTypeSpace, minArity: Int, maxArity: Int) = StructRepeatSpace(field, minArity, maxArity)

data class StructRepeatSpace(
    val field: TraversableTypeSpace,
    val minArity: Int,
    val maxArity: Int,
) : TraversableTypeSpace {
    
    init {
        require(minArity >= 0) { "minArity must be non-negative: $minArity" }
        require(maxArity >= minArity) { "maxArity must be >= minArity: $maxArity < $minArity" }
    }
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        return that is FoxStructType && that.arity in minArity..maxArity && that.fields.values.all { field.contains(it, context) }
    }
    
    override fun traverser(context: TypeSpaceContext) = object : SpaceTraverser<FoxType> {
        
        private var dirty = true
        private var current: FoxStructType? = null
        
        override fun current(): FoxStructType? = TODO()
        
        override fun seekNext() = TODO()
        
        override fun seekCeilOf(that: FoxType) = TODO()
        
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

fun TraversableTypeSpace.structTraverser(context: TypeSpaceContext) = object : SpaceTraverser<FoxType> {
    
    private val backend = this@structTraverser.traverser(context)
    
    override fun current(): FoxStructType? {
        while (true) {
            val family = backend.current()?.family() ?: return null
            if (family > ConcreteTypeFamily.STRUCT) return null
            if (family == ConcreteTypeFamily.STRUCT) return backend.current() as FoxStructType
            backend.seekCeilOf(TODO())
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
