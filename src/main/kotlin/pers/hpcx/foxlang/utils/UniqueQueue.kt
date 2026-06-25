package pers.hpcx.foxlang.utils

import java.util.AbstractQueue
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.collections.ArrayDeque
import kotlin.collections.Collection
import kotlin.collections.HashSet
import kotlin.collections.MutableIterator

class UniqueQueue<E> : AbstractQueue<E>() {
    
    private val set = HashSet<E>()
    private val queue = ArrayDeque<E>()
    
    override val size get() = queue.size
    
    override fun isEmpty() = queue.isEmpty()
    
    override fun contains(e: E) = e in set
    
    override fun containsAll(c: Collection<E>) = set.containsAll(c)
    
    override fun peek() = queue.firstOrNull()
    
    override fun add(e: E) = offer(e)
    
    override fun offer(e: E) = set.add(e).ifTrue { queue.add(e) }
    
    override fun poll() = queue.removeFirstOrNull().ifNotNull { check(set.remove(it)) }
    
    override fun remove(e: E) = set.remove(e).ifTrue { check(queue.remove(e)) }
    
    override fun removeAll(c: Collection<E>) = set.removeAll(c).ifTrue { sync() }
    
    override fun retainAll(c: Collection<E>) = set.retainAll(c).ifTrue { sync() }
    
    override fun removeIf(filter: Predicate<in E>) = set.removeIf(filter).ifTrue { sync() }
    
    private fun sync() {
        queue.removeIf { it !in set }
        check(set.size == queue.size)
    }
    
    override fun clear() {
        set.clear()
        queue.clear()
    }
    
    override fun hashCode() = queue.hashCode()
    
    override fun equals(other: Any?) = queue == other
    
    override fun toString() = queue.toString()
    
    override fun toArray() = queue.toArray()
    
    override fun <T> toArray(a: Array<out T>) = queue.toArray(a)
    
    override fun stream() = queue.stream()
    
    override fun parallelStream() = queue.parallelStream()
    
    override fun spliterator() = queue.spliterator()
    
    override fun forEach(action: Consumer<in E>) = queue.forEach(action)
    
    override fun iterator() = object : MutableIterator<E> {
        
        private val backend = queue.iterator()
        private var removable: E? = null
        
        override fun hasNext() = backend.hasNext()
        
        override fun next(): E {
            val e = backend.next()
            removable = e
            return e
        }
        
        override fun remove() {
            val e = removable ?: throw IllegalStateException()
            removable = null
            set.remove(e)
            backend.remove()
        }
    }
}

private inline fun Boolean.ifTrue(block: () -> Unit): Boolean {
    if (this) block()
    return this
}

private inline fun <T : Any> T?.ifNotNull(block: (T) -> Unit): T? {
    if (this != null) block(this)
    return this
}
