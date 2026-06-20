package pers.hpcx.foxlang.type.space

fun universalNameSpace(dictionary: NameDictionary) = UniversalNameSpace(dictionary)

fun nameSpace(dictionary: NameDictionary, vararg names: String) = nameSpace(dictionary, names.toSet())

fun nameSpace(dictionary: NameDictionary, names: Set<String>) = ChoiceNameSpace(dictionary, names)

data class UniversalNameSpace(val dictionary: NameDictionary) : NameSpace {
    
    override fun contains(that: String, context: NameSpaceContextView): Boolean {
        check(context.context.namesOf(dictionary).contains(that))
        return true
    }
    
    override fun traverser(context: NameSpaceContextView) = object : SpaceTraverser<String> {
        
        private var dirty = true
        private var current: String? = null
        private val iterator: Iterator<String> = context.context.namesOf(dictionary).iterator()
        
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

data class ChoiceNameSpace(val dictionary: NameDictionary, val names: Set<String>) : NameSpace {
    
    override fun contains(that: String, context: NameSpaceContextView) = names.any { context.compare(it, that) == 0 }
    
    override fun traverser(context: NameSpaceContextView) = object : SpaceTraverser<String> {
        
        init {
            check(names.all { context.context.namesOf(dictionary).contains(it) })
        }
        
        private var dirty = true
        private var current: String? = null
        private val iterator: Iterator<String> by lazy(LazyThreadSafetyMode.NONE) {
            names.sortedWith { left, right -> context.compare(left, right) }.iterator()
        }
        
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
