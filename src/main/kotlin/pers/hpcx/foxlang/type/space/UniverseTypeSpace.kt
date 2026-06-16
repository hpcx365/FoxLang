package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.ConcreteTypeFamily
import pers.hpcx.foxlang.type.family

fun universe(maxHeight: Int) = UniverseTypeSpace(maxHeight)

data class UniverseTypeSpace(val maxHeight: Int) : TraversableTypeSpace {
    
    override fun contains(that: FoxType, context: TypeSpaceContext): Boolean {
        return intersect(this, singleSpace(that)).traverser(context).current() != null
    }
    
    override fun traverser(context: TypeSpaceContext): SpaceTraverser<FoxType> = if (maxHeight == 0) {
        emptySpace<FoxType, TypeSpaceContext>().traverser(context)
    } else object : SpaceTraverser<FoxType> {
        
        private var dirty = true
        private var current: FoxType? = null
        private var currentFamily: Iterator<ConcreteTypeFamily> = ConcreteTypeFamily.entries.iterator()
        private var traverser: SpaceTraverser<FoxType> = currentFamily.next().universeLang().traverser(context)
        
        override fun current(): FoxType? {
            if (!dirty) return current
            while (true) {
                traverser.current()?.let {
                    dirty = false
                    current = it
                    break
                }
                if (currentFamily.hasNext()) {
                    traverser = currentFamily.next().universeLang().traverser(context)
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
            traverser.seekNext()
            markDirty()
        }
        
        override fun seekCeilOf(that: FoxType) {
            val cur = current() ?: return
            val curFamily = cur.family()
            
            val family = that.family()
            if (family < curFamily) return
            if (family > curFamily) {
                while (true) if (currentFamily.next() == family) break
                traverser = family.universeLang().traverser(context)
                markDirty()
                return
            }
            
            traverser.seekCeilOf(that)
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
        
        private fun ConcreteTypeFamily.universeLang(): TraversableTypeSpace = when (this) {
            ConcreteTypeFamily.VOID -> singleSpace(FoxVoidType)
            ConcreteTypeFamily.UNIT -> singleSpace(FoxUnitType)
            ConcreteTypeFamily.BOOL -> singleSpace(FoxBoolType)
            ConcreteTypeFamily.BYTE -> singleSpace(FoxByteType)
            ConcreteTypeFamily.SHORT -> singleSpace(FoxShortType)
            ConcreteTypeFamily.INT -> singleSpace(FoxIntType)
            ConcreteTypeFamily.LONG -> singleSpace(FoxLongType)
            ConcreteTypeFamily.FLOAT -> singleSpace(FoxFloatType)
            ConcreteTypeFamily.DOUBLE -> singleSpace(FoxDoubleType)
            ConcreteTypeFamily.CHAR -> singleSpace(FoxCharType)
            ConcreteTypeFamily.STRING -> singleSpace(FoxStringType)
            ConcreteTypeFamily.TUPLE -> TupleRepeatSpace(UniverseTypeSpace(maxHeight - 1), 0, context.bounds.maxTupleArity)
            ConcreteTypeFamily.STRUCT -> TODO()
            ConcreteTypeFamily.OBJECT -> TODO()
            ConcreteTypeFamily.ENUM -> TODO()
            ConcreteTypeFamily.ARRAY -> ArraySpace(UniverseTypeSpace(maxHeight - 1))
            ConcreteTypeFamily.REF -> RefSpace(UniverseTypeSpace(maxHeight - 1))
            ConcreteTypeFamily.METHOD -> TODO()
        }
    }
}
