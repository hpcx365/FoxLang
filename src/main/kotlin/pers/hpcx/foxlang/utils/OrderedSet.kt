package pers.hpcx.foxlang.utils

interface OrderedSet<out E> : Iterable<E> {
    val size: Int
    val elements: List<E>
    
    fun isEmpty(): Boolean = size == 0
    
    fun isNotEmpty(): Boolean = size != 0
    
    operator fun contains(element: @UnsafeVariance E): Boolean
    
    fun elementAt(index: Int): E = elements[index]
    
    fun first(): E = elementAt(0)
    
    fun last(): E = elementAt(size - 1)
    
    fun indexOf(element: @UnsafeVariance E): Int
    
    fun toSet(): Set<E>
    
    override fun iterator(): Iterator<E> = elements.iterator()
}

fun <E> emptyOrderedSet(): OrderedSet<E> = ArrayHashOrderedSet()

fun <E> orderedSetOf(vararg elements: E): OrderedSet<E> = ArrayHashOrderedSet(elements.asIterable())

fun <E> Iterable<E>.toOrderedSet(): OrderedSet<E> = ArrayHashOrderedSet(this)
