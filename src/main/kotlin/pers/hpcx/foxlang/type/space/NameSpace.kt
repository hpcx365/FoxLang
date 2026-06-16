package pers.hpcx.foxlang.type.space

fun namesOf(): NameSpace = emptySpace()

fun namesOf(name: String): NameSpace = singleSpace(name)

fun namesOf(vararg names: String) = namesOf(names.toSet())

fun namesOf(names: Set<String>) = if (names.isEmpty()) {
    namesOf()
} else if (names.size == 1) {
    namesOf(names.first())
} else {
    GenericNameSpace(names)
}

typealias NameSpace = TraversableSpace<String, NameSpaceContextView>

data class GenericNameSpace(val names: Set<String>) : NameSpace {
    
    override fun contains(that: String, context: NameSpaceContextView) = names.any { context.compare(it, that) == 0 }
    
    override fun traverser(context: NameSpaceContextView) = object : SpaceTraverser<String> {
        
        private var dirty = true
        private var current: String? = null
        private var iterator: Iterator<String> = names.sortedWith { left, right -> context.compare(left, right) }.iterator()
        
        override fun current(): String? {
            if (!dirty) return current
            dirty = false
            current = if (iterator.hasNext()) iterator.next() else null
            return current
        }
        
        override fun seekNext() {
            if (current() == null) return
            markDirty()
        }
        
        override fun seekCeilOf(that: String) {
            while (true) {
                val cur = current() ?: return
                if (context.compare(cur, that) >= 0) return
                markDirty()
            }
        }
        
        private fun markDirty() {
            check(!dirty)
            dirty = true
        }
    }
}
