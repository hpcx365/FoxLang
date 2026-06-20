package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxArrayType
import pers.hpcx.foxlang.ast.FoxType
import pers.hpcx.foxlang.type.ConcreteTypeFamily
import pers.hpcx.foxlang.type.family

fun arraySpace(element: TraversableTypeSpace) = ArraySpace(element)

data class ArraySpace(val element: TraversableTypeSpace) : TraversableTypeSpace {
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        return that is FoxArrayType && element.contains(that.element, context)
    }
    
    override fun traverser(context: TypeSpaceContext) = object : SpaceTraverser<FoxType> {
        
        private var dirty = true
        private var current: FoxArrayType? = null
        private val traverser by lazy(LazyThreadSafetyMode.NONE) { element.traverser(context) }
        
        override fun current(): FoxArrayType? {
            if (!dirty) return current
            dirty = false
            current = traverser.current()?.let { FoxArrayType(it) }
            return current
        }
        
        override fun seekNext() {
            if (current() == null) return
            traverser.seekNext()
            markDirty()
        }
        
        override fun seekCeilOf(that: FoxType) {
            if (current() == null) return
            
            val family = that.family()
            if (family < ConcreteTypeFamily.ARRAY) return
            if (family > ConcreteTypeFamily.ARRAY) {
                exhaust()
                return
            }
            
            check(that is FoxArrayType)
            traverser.seekCeilOf(that.element)
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

fun arrayElementOf(baseSpace: TraversableTypeSpace) = ArrayElementProjectionSpace(baseSpace)

data class ArrayElementProjectionSpace(override val baseSpace: TraversableTypeSpace) : ProjectionTypeSpace {
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return arraySpace(singleSpace(that))
    }
}
