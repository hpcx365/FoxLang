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

private object EmptyOrderedMap : OrderedMap<Any?, Any?> {
    override val size: Int = 0
    override val entries: List<Map.Entry<Any?, Any?>> = emptyList()
    override val keys: OrderedSet<Any?> = emptyOrderedSet()
    override val values: List<Any?> = emptyList()
    override fun containsKey(key: Any?): Boolean = false
    override fun containsValue(value: Any?): Boolean = false
    override fun get(key: Any?): Any? = null
    override fun getValue(key: Any?): Any = throw NoSuchElementException("Map is empty")
    override fun keyAt(index: Int): Any = throw IndexOutOfBoundsException("Map is empty")
    override fun valueAt(index: Int): Any = throw IndexOutOfBoundsException("Map is empty")
    override fun indexOfKey(key: Any?): Int = -1
    override fun toMap(): Map<Any?, Any?> = emptyMap()
}

fun <K, V> emptyOrderedMap(): OrderedMap<K, V> = @Suppress("UNCHECKED_CAST") (EmptyOrderedMap as OrderedMap<K, V>)

fun <K, V> OrderedMap<K, V>?.orEmpty(): OrderedMap<K, V> = this ?: emptyOrderedMap()

fun <K, V> orderedMapOf(vararg entries: Pair<K, V>): OrderedMap<K, V> = ArrayHashOrderedMap(entries.asIterable())

fun <K, V> List<Pair<K, V>>.toOrderedMap(): OrderedMap<K, V> = ArrayHashOrderedMap(this)

fun <K, V, T> OrderedMap<K, V>.mapValues(transform: (Map.Entry<K, V>) -> T): OrderedMap<K, T> {
    val result = ArrayHashOrderedMap<K, T>()
    forEach { entry -> result[entry.key] = transform(entry) }
    return result
}
