package pers.hpcx.foxlang.type.space

fun <T, C : SpaceContext<T>> subtract(base: TraversableSpace<T, C>, removed: TraversableSpace<T, C>) = SubtractSpace(base, removed)

data class SubtractSpace<T, C : SpaceContext<T>>(val base: TraversableSpace<T, C>, val removed: TraversableSpace<T, C>) : TraversableSpace<T, C> {
    
    override fun contains(that: T, context: C): Boolean {
        return base.contains(that, context) && !removed.contains(that, context)
    }
    
    override fun traverser(context: C) = object : SpaceTraverser<T> {
        
        private var dirty = true
        private var current: T? = null
        private val baseTraverser = base.traverser(context)
        private val removedTraverser = removed.traverser(context)
        
        override fun current(): T? {
            if (!dirty) return current
            while (true) {
                val baseCurrent = baseTraverser.current() ?: run {
                    exhaust()
                    break
                }
                removedTraverser.seekCeilOf(baseCurrent)
                val removedCurrent = removedTraverser.current()
                if (removedCurrent == null || context.compare(removedCurrent, baseCurrent) > 0) {
                    dirty = false
                    current = baseCurrent
                    break
                }
                baseTraverser.seekNext()
            }
            check(!dirty)
            return current
        }
        
        override fun seekNext() {
            if (current() == null) return
            baseTraverser.seekNext()
            markDirty()
        }
        
        override fun seekCeilOf(that: T) {
            if (current() == null) return
            baseTraverser.seekCeilOf(that)
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
