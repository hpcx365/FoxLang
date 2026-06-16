package pers.hpcx.foxlang.type.space

fun <T, C : SpaceContext<T>> subtract(base: Space<T, C>, removed: Space<T, C>) = if (base is TraversableSpace<T, C> && removed is TraversableSpace<T, C>) {
    TraversableSubtractSpace(base, removed)
} else {
    GenericSubtractSpace(base, removed)
}

fun <T, C : SpaceContext<T>> subtract(base: TraversableSpace<T, C>, removed: TraversableSpace<T, C>) = TraversableSubtractSpace(base, removed)

sealed interface SubtractSpace<T, C : SpaceContext<T>> : Space<T, C> {
    
    val base: Space<T, C>
    val removed: Space<T, C>
    
    override fun contains(that: T, context: C): Boolean {
        return base.contains(that, context) && !removed.contains(that, context)
    }
}

data class GenericSubtractSpace<T, C : SpaceContext<T>>(
    override val base: Space<T, C>,
    override val removed: Space<T, C>,
) : SubtractSpace<T, C>

data class TraversableSubtractSpace<T, C : SpaceContext<T>>(
    override val base: TraversableSpace<T, C>,
    override val removed: TraversableSpace<T, C>,
) : SubtractSpace<T, C>, TraversableSpace<T, C> {
    
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
