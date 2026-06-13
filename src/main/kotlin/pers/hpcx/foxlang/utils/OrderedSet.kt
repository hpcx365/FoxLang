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

private object EmptyOrderedSet : OrderedSet<Any?> {
    override val size: Int = 0
    override val elements: List<Any?> = emptyList()
    override fun contains(element: Any?): Boolean = false
    override fun indexOf(element: Any?): Int = -1
    override fun toSet(): Set<Any?> = emptySet()
}

fun <E> emptyOrderedSet(): OrderedSet<E> = @Suppress("UNCHECKED_CAST") (EmptyOrderedSet as OrderedSet<E>)

fun <E> OrderedSet<E>?.orEmpty(): OrderedSet<E> = this ?: emptyOrderedSet()

fun <E> orderedSetOf(vararg elements: E): OrderedSet<E> = ArrayHashOrderedSet(elements.asIterable())

fun <E> Iterable<E>.toOrderedSet(): OrderedSet<E> = ArrayHashOrderedSet(this)
