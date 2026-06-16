package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxRefType
import pers.hpcx.foxlang.ast.FoxType
import pers.hpcx.foxlang.type.ConcreteTypeFamily
import pers.hpcx.foxlang.type.family

fun ref(referent: TraversableTypeSpace) = RefSpace(referent)

data class RefSpace(val referent: TraversableTypeSpace) : TraversableTypeSpace {
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        return that is FoxRefType && referent.contains(that.referent, context)
    }
    
    override fun traverser(context: TypeSpaceContext) = object : SpaceTraverser<FoxType> {
        
        private var dirty = true
        private var current: FoxRefType? = null
        private val traverser by lazy(LazyThreadSafetyMode.NONE) { referent.traverser(context) }
        
        override fun current(): FoxRefType? {
            if (!dirty) return current
            dirty = false
            current = traverser.current()?.let { FoxRefType(it) }
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
            if (family < ConcreteTypeFamily.REF) return
            if (family > ConcreteTypeFamily.REF) {
                exhaust()
                return
            }
            
            check(that is FoxRefType)
            traverser.seekCeilOf(that.referent)
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

fun refReferentOf(baseSpace: TraversableTypeSpace) = RefReferentProjectiveSpace(baseSpace)

data class RefReferentProjectiveSpace(
    override val baseSpace: TraversableTypeSpace,
) : ProjectiveSpace<FoxType, TypeSpaceContext> {
    
    override fun preimageOf(that: FoxType): TraversableTypeSpace {
        return ref(singleSpace(that))
    }
}
