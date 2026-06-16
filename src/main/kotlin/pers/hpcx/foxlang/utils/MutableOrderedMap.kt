package pers.hpcx.foxlang.utils

interface MutableOrderedMap<K, V> : OrderedMap<K, V> {
    override val entries: MutableList<MutableMap.MutableEntry<K, V>>
    override val keys: MutableOrderedSet<K>
    override val values: MutableList<V>

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    fun put(key: K, value: V): V?

    fun putAt(index: Int, key: K, value: V): V?
    
    fun putAll(entries: Iterable<Pair<K, V>>) {
        entries.forEach { (key, value) -> put(key, value) }
    }
    
    fun putAll(map: Map<K, V>) {
        map.forEach { (key, value) -> put(key, value) }
    }
    
    fun getOrPut(key: K, defaultValue: () -> V): V
    
    fun move(key: K, index: Int)
    
    fun remove(key: K): V?
    
    override fun entryAt(index: Int): MutableMap.MutableEntry<K, V>
    
    fun removeAt(index: Int): MutableMap.MutableEntry<K, V>

    fun clear()
    
    fun copy(): MutableOrderedMap<K, V>
    
    fun toMutableMap(): MutableMap<K, V>
}

fun <K, V> emptyMutableOrderedMap(): MutableOrderedMap<K, V> = ArrayHashOrderedMap()

fun <K, V> mutableOrderedMapOf(vararg entries: Pair<K, V>): MutableOrderedMap<K, V> = ArrayHashOrderedMap(entries.asIterable())

fun <K, V> List<Pair<K, V>>.toMutableOrderedMap(): MutableOrderedMap<K, V> = ArrayHashOrderedMap(this)
