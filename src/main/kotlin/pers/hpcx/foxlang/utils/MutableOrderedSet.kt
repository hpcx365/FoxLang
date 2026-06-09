package pers.hpcx.foxlang.utils

interface MutableOrderedSet<E> : OrderedSet<E> {
    override val elements: MutableList<E>
    
    fun add(element: E): Boolean
    
    operator fun plusAssign(element: E) {
        add(element)
    }
    
    fun addAt(index: Int, element: E): Boolean
    
    fun addAll(elements: Iterable<E>) {
        elements.forEach(::add)
    }
    
    fun move(element: E, index: Int)
    
    fun remove(element: E): Boolean
    
    override fun elementAt(index: Int): E
    
    fun removeAt(index: Int): E
    
    fun clear()
    
    fun copy(): MutableOrderedSet<E>
    
    fun toMutableSet(): MutableSet<E>
}

fun <E> emptyMutableOrderedSet(): MutableOrderedSet<E> = ArrayHashOrderedSet()

fun <E> mutableOrderedSetOf(vararg elements: E): MutableOrderedSet<E> = ArrayHashOrderedSet(elements.asIterable())

fun <E> Iterable<E>.toMutableOrderedSet(): MutableOrderedSet<E> = ArrayHashOrderedSet(this)
