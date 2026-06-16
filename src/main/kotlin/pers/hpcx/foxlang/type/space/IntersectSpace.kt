package pers.hpcx.foxlang.type.space

fun <T, C : SpaceContext<T>> intersect(vararg spaces: Space<T, C>) = intersect(spaces.toList())

fun <T, C : SpaceContext<T>> intersect(vararg spaces: TraversableSpace<T, C>) = intersect(spaces.toList())

fun <T, C : SpaceContext<T>> intersect(spaces: List<Space<T, C>>) = if (spaces.all { it is TraversableSpace<T, C> }) {
    @Suppress("UNCHECKED_CAST") TraversableIntersectSpace(spaces as List<TraversableSpace<T, C>>)
} else {
    GenericIntersectSpace(spaces)
}

fun <T, C : SpaceContext<T>> intersect(spaces: List<TraversableSpace<T, C>>) = TraversableIntersectSpace(spaces)

sealed interface IntersectSpace<T, C : SpaceContext<T>> : Space<T, C> {
    
    val parts: List<Space<T, C>>
    
    override fun contains(that: T, context: C): Boolean {
        return parts.all { it.contains(that, context) }
    }
}

data class GenericIntersectSpace<T, C : SpaceContext<T>>(override val parts: List<Space<T, C>>) : IntersectSpace<T, C>

data class TraversableIntersectSpace<T, C : SpaceContext<T>>(override val parts: List<TraversableSpace<T, C>>) : IntersectSpace<T, C>, TraversableSpace<T, C> {
    
    init {
        require(parts.isNotEmpty()) { "IntersectLang must have at least one part" }
    }
    
    override fun traverser(context: C) = object : SpaceTraverser<T> {
        
        private var dirty = true
        private var current: T? = null
        private val traversers by lazy(LazyThreadSafetyMode.NONE) { parts.map { it.traverser(context) } }
        
        override fun current(): T? {
            if (!dirty) return current
            loop@ while (true) {
                var maximum: T? = null
                traversers.forEach { traverser ->
                    val candidate = traverser.current() ?: run {
                        exhaust()
                        break@loop
                    }
                    if (maximum == null || context.compare(maximum, candidate) < 0) {
                        maximum = candidate
                    }
                }
                checkNotNull(maximum)
                var allEqual = true
                traversers.forEach { traverser ->
                    if (context.compare(traverser.current()!!, maximum) < 0) {
                        allEqual = false
                        traverser.seekCeilOf(maximum)
                    }
                }
                if (allEqual) {
                    dirty = false
                    current = maximum
                    break@loop
                }
            }
            check(!dirty)
            return current
        }
        
        override fun seekNext() {
            if (current() == null) return
            traversers.forEach { it.seekNext() }
            markDirty()
        }
        
        override fun seekCeilOf(that: T) {
            if (current() == null) return
            traversers.forEach { it.seekCeilOf(that) }
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
