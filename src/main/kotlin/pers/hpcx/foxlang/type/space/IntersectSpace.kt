package pers.hpcx.foxlang.type.space

fun <T, C : SpaceContext<T>> intersect(vararg spaces: TraversableSpace<T, C>) = intersect(spaces.toList())

fun <T, C : SpaceContext<T>> intersect(spaces: List<TraversableSpace<T, C>>) = IntersectSpace(spaces)

data class IntersectSpace<T, C : SpaceContext<T>>(val parts: List<TraversableSpace<T, C>>) : TraversableSpace<T, C> {
    
    init {
        require(parts.isNotEmpty()) { "IntersectLang must have at least one part" }
    }
    
    override fun contains(that: T, context: C): Boolean {
        return parts.all { it.contains(that, context) }
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
