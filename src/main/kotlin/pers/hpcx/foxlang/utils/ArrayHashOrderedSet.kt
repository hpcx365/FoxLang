package pers.hpcx.foxlang.utils

class ArrayHashOrderedSet<E>() : MutableOrderedSet<E> {
    private val backingMap = ArrayHashOrderedMap<E, Unit>()
    
    constructor(elements: Iterable<E>) : this() {
        addAll(elements)
    }
    
    override val size: Int
        get() = backingMap.size
    
    override val elements: MutableList<E> = ElementsView()
    
    override fun contains(element: E): Boolean = backingMap.containsKey(element)
    
    override fun elementAt(index: Int): E = backingMap.keyAt(index)
    
    override fun indexOf(element: E): Int = backingMap.indexOfKey(element)
    
    override fun add(element: E): Boolean = backingMap.put(element, Unit) == null
    
    override fun addAt(index: Int, element: E): Boolean {
        val existed = element in backingMap
        backingMap.putAt(index, element, Unit)
        return !existed
    }
    
    override fun move(element: E, index: Int) {
        backingMap.move(element, index)
    }
    
    override fun remove(element: E): Boolean = backingMap.remove(element) != null
    
    override fun removeAt(index: Int): E = backingMap.removeAt(index).key
    
    override fun clear() {
        backingMap.clear()
    }
    
    override fun copy(): MutableOrderedSet<E> = ArrayHashOrderedSet(elements.toList())
    
    override fun toSet(): Set<E> = LinkedHashSet<E>(size).also { it.addAll(elements) }
    
    override fun toMutableSet(): MutableSet<E> = LinkedHashSet(toSet())
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrderedSet<*>) return false
        if (size != other.size) return false
        for (index in elements.indices) {
            if (elements[index] != other.elementAt(index)) return false
        }
        return true
    }
    
    override fun hashCode(): Int {
        var result = 1
        for (element in elements) {
            result = 31 * result + (element?.hashCode() ?: 0)
        }
        return result
    }
    
    override fun toString(): String = elements.joinToString(
        prefix = "ArrayHashOrderedSet(",
        postfix = ")",
    )
    
    private inner class ElementsView : AbstractMutableList<E>() {
        override val size: Int
            get() = this@ArrayHashOrderedSet.size
        
        override fun get(index: Int): E = this@ArrayHashOrderedSet.elementAt(index)
        
        override fun add(index: Int, element: E) {
            this@ArrayHashOrderedSet.addAt(index, element)
        }
        
        override fun set(index: Int, element: E): E {
            val previous = this@ArrayHashOrderedSet.elementAt(index)
            if (previous == element) return previous
            require(element !in backingMap) { "Element already exists: $element" }
            backingMap.entries[index] = mutableMapOf(element to Unit).entries.first()
            return previous
        }
        
        override fun removeAt(index: Int): E = this@ArrayHashOrderedSet.removeAt(index)
    }
}
