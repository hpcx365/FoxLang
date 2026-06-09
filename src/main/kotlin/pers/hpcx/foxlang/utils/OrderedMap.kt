package pers.hpcx.foxlang.utils

interface OrderedMap<K, out V> : Iterable<Map.Entry<K, V>> {
    val size: Int
    val entries: List<Map.Entry<K, V>>
    val keys: OrderedSet<K>
    val values: List<V>

    fun isEmpty(): Boolean = size == 0

    fun isNotEmpty(): Boolean = size != 0

    fun containsKey(key: K): Boolean

    fun containsValue(value: @UnsafeVariance V): Boolean
    
    operator fun contains(key: K): Boolean = containsKey(key)
    
    operator fun get(key: K): V?
    
    fun getValue(key: K): V
    
    fun keyAt(index: Int): K
    
    fun valueAt(index: Int): V
    
    fun entryAt(index: Int): Map.Entry<K, V> = entries[index]

    fun firstKey(): K = keyAt(0)
    
    fun lastKey(): K = keyAt(size - 1)
    
    fun firstEntry(): Map.Entry<K, V> = entryAt(0)
    
    fun lastEntry(): Map.Entry<K, V> = entryAt(size - 1)

    fun indexOfKey(key: K): Int
    
    fun toMap(): Map<K, V>

    override fun iterator(): Iterator<Map.Entry<K, V>> = entries.iterator()
}

fun <K, V> emptyOrderedMap(): OrderedMap<K, V> = ArrayHashOrderedMap()

fun <K, V> orderedMapOf(vararg entries: Pair<K, V>): OrderedMap<K, V> = ArrayHashOrderedMap(entries.asIterable())

fun <K, V> Iterable<Pair<K, V>>.toOrderedMap(): OrderedMap<K, V> = ArrayHashOrderedMap(this)

fun <K, V> Map<K, V>.toOrderedMap(): OrderedMap<K, V> = ArrayHashOrderedMap(this)
