package pers.hpcx.foxlang.type.space

fun <T, C : SpaceContext<T>> union(vararg spaces: TraversableSpace<T, C>) = union(spaces.toList())

fun <T, C : SpaceContext<T>> union(spaces: List<TraversableSpace<T, C>>) = UnionSpace(spaces)

data class UnionSpace<T, C : SpaceContext<T>>(val parts: List<TraversableSpace<T, C>>) : TraversableSpace<T, C> {
    
    override fun contains(that: T, context: C): Boolean {
        return parts.any { it.contains(that, context) }
    }
    
    override fun traverser(context: C) = object : SpaceTraverser<T> {
        
        private var dirty = true
        private var current: T? = null
        private val traversers by lazy(LazyThreadSafetyMode.NONE) { parts.map { it.traverser(context) } }
        
        override fun current(): T? {
            if (!dirty) return current
            var minimum: T? = null
            traversers.forEach { traverser ->
                val candidate = traverser.current() ?: return@forEach
                if (minimum == null || context.compare(minimum, candidate) > 0) {
                    minimum = candidate
                }
            }
            dirty = false
            current = minimum
            return current
        }
        
        override fun seekNext() {
            val cur = current() ?: return
            traversers.forEach { traverser ->
                traverser.current()?.let {
                    if (context.compare(it, cur) == 0) {
                        traverser.seekNext()
                    }
                }
            }
            markDirty()
        }
        
        override fun seekCeilOf(that: T) {
            if (current() == null) return
            traversers.forEach { it.seekCeilOf(that) }
            markDirty()
        }
        
        private fun markDirty() {
            check(!dirty)
            dirty = true
        }
    }
}
