package pers.hpcx.foxlang.utils

import kotlin.math.min

class ArrayHashOrderedMap<K, V> private constructor(
    private val orderedKeys: MutableList<K>,
    private val valuesByKey: MutableMap<K, V>,
) : MutableOrderedMap<K, V> {
    
    private val indexByKey = HashMap<K, Int>(orderedKeys.size)
    private var hashCodeDirty = true
    private var cachedHashCode = 1
    
    constructor() : this(mutableListOf(), mutableMapOf())
    
    constructor(entries: Iterable<Pair<K, V>>) : this() {
        putAll(entries)
    }
    
    constructor(map: Map<K, V>) : this(map.entries.map { it.key to it.value })
    
    override val entries: MutableList<MutableMap.MutableEntry<K, V>> = EntriesView()
    
    override val keys: MutableOrderedSet<K> = KeysView()
    
    override val values: MutableList<V> = ValuesView()
    
    init {
        require(orderedKeys.size == orderedKeys.toSet().size) { "Ordered keys contain duplicates" }
        require(orderedKeys.size == valuesByKey.size) {
            "Ordered keys and mapped values have different sizes: ${orderedKeys.size} != ${valuesByKey.size}"
        }
        require(orderedKeys.all(valuesByKey::containsKey)) {
            "Ordered keys and mapped values must contain the same keys"
        }
        rebuildIndices()
    }
    
    override val size: Int
        get() = orderedKeys.size
    
    override fun containsKey(key: K): Boolean = indexByKey.containsKey(key)
    
    override fun containsValue(value: V): Boolean = valuesByKey.containsValue(value)
    
    override fun get(key: K): V? = valuesByKey[key]
    
    override fun getValue(key: K): V = valuesByKey.getValue(key)
    
    override fun keyAt(index: Int): K = orderedKeys[index]
    
    override fun valueAt(index: Int): V = valuesByKey.getValue(orderedKeys[index])
    
    override fun entryAt(index: Int): MutableMap.MutableEntry<K, V> = EntryView(orderedKeys[index])
    
    override fun indexOfKey(key: K): Int = indexByKey[key] ?: -1
    
    override fun put(key: K, value: V): V? {
        val existingIndex = indexByKey[key]
        if (existingIndex == null) {
            orderedKeys.add(key)
            indexByKey[key] = orderedKeys.lastIndex
            valuesByKey[key] = value
            markChanged()
            return null
        }
        val previous = valuesByKey.getValue(key)
        valuesByKey[key] = value
        markChanged()
        return previous
    }
    
    override fun putAt(index: Int, key: K, value: V): V? {
        require(index in 0..size) { "Index out of bounds: $index, size=$size" }
        
        val existingIndex = indexByKey[key]
        if (existingIndex == null) {
            orderedKeys.add(index, key)
            valuesByKey[key] = value
            reindexFrom(index)
            markChanged()
            return null
        }
        
        val previous = valuesByKey.getValue(key)
        valuesByKey[key] = value
        
        val targetIndex = if (existingIndex < index) index - 1 else index
        if (existingIndex != targetIndex) {
            orderedKeys.removeAt(existingIndex)
            orderedKeys.add(targetIndex, key)
            reindexFrom(min(existingIndex, targetIndex))
        }
        
        markChanged()
        return previous
    }
    
    override fun getOrPut(key: K, defaultValue: () -> V): V {
        if (indexByKey.containsKey(key)) return valuesByKey.getValue(key)
        val value = defaultValue()
        orderedKeys += key
        indexByKey[key] = orderedKeys.lastIndex
        valuesByKey[key] = value
        markChanged()
        return value
    }
    
    override fun move(key: K, index: Int) {
        require(index in 0 until size) { "Index out of bounds: $index, size=$size" }
        val oldIndex = indexByKey[key] ?: error("Key does not exist: $key")
        if (oldIndex == index) return
        orderedKeys.removeAt(oldIndex)
        orderedKeys.add(index, key)
        reindexFrom(min(oldIndex, index))
        markChanged()
    }
    
    override fun remove(key: K): V? {
        val index = indexByKey[key] ?: return null
        val previous = valuesByKey.getValue(key)
        orderedKeys.removeAt(index)
        valuesByKey.remove(key)
        indexByKey.remove(key)
        reindexFrom(index)
        markChanged()
        return previous
    }
    
    override fun removeAt(index: Int): MutableMap.MutableEntry<K, V> {
        val key = orderedKeys.removeAt(index)
        val value = valuesByKey.getValue(key)
        valuesByKey.remove(key)
        indexByKey.remove(key)
        reindexFrom(index)
        markChanged()
        return EntrySnapshot(key, value)
    }
    
    override fun clear() {
        if (size == 0) return
        orderedKeys.clear()
        valuesByKey.clear()
        indexByKey.clear()
        markChanged()
    }
    
    override fun copy(): MutableOrderedMap<K, V> {
        return ArrayHashOrderedMap(orderedKeys.toMutableList(), valuesByKey.toMutableMap())
    }
    
    override fun toMap(): Map<K, V> = LinkedHashMap<K, V>(size).also { map ->
        orderedKeys.forEach { key -> map[key] = valuesByKey.getValue(key) }
    }
    
    override fun toMutableMap(): MutableMap<K, V> = LinkedHashMap(toMap())
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrderedMap<*, *>) return false
        if (size != other.size) return false
        for (index in orderedKeys.indices) {
            val key = orderedKeys[index]
            if (key != other.keyAt(index)) return false
            if (valuesByKey.getValue(key) != other.valueAt(index)) return false
        }
        return true
    }
    
    override fun hashCode(): Int {
        if (!hashCodeDirty) return cachedHashCode
        var result = 1
        for (key in orderedKeys) {
            val value = valuesByKey.getValue(key)
            val entryHash = 31 * (key?.hashCode() ?: 0) + (value?.hashCode() ?: 0)
            result = 31 * result + entryHash
        }
        cachedHashCode = result
        hashCodeDirty = false
        return result
    }
    
    override fun toString(): String = entries.joinToString(
        prefix = "ArrayHashOrderedMap(",
        postfix = ")",
    ) { "${it.key}=${it.value}" }
    
    private fun replaceKeyAt(index: Int, newKey: K): K {
        val oldKey = orderedKeys[index]
        if (oldKey == newKey) return oldKey
        require(!indexByKey.containsKey(newKey)) { "Key already exists: $newKey" }
        val value = valuesByKey.getValue(oldKey)
        orderedKeys[index] = newKey
        valuesByKey.remove(oldKey)
        valuesByKey[newKey] = value
        indexByKey.remove(oldKey)
        indexByKey[newKey] = index
        markChanged()
        return oldKey
    }
    
    private fun replaceEntryAt(index: Int, key: K, value: V): MutableMap.MutableEntry<K, V> {
        val previous = EntrySnapshot(keyAt(index), valueAt(index))
        replaceKeyAt(index, key)
        valuesByKey[key] = value
        markChanged()
        return previous
    }
    
    private fun setValueAt(index: Int, value: V): V {
        val key = orderedKeys[index]
        val previous = valuesByKey.getValue(key)
        valuesByKey[key] = value
        markChanged()
        return previous
    }
    
    private fun reindexFrom(startIndex: Int) {
        for (index in startIndex until orderedKeys.size) {
            indexByKey[orderedKeys[index]] = index
        }
    }
    
    private fun rebuildIndices() {
        indexByKey.clear()
        reindexFrom(0)
    }
    
    private fun markChanged() {
        hashCodeDirty = true
    }
    
    private inner class KeysView : AbstractMutableList<K>(), MutableOrderedSet<K> {
        override val elements: MutableList<K>
            get() = this
        
        override val size: Int
            get() = this@ArrayHashOrderedMap.size
        
        override fun isEmpty(): Boolean = size == 0
        
        override fun iterator(): MutableIterator<K> = super<AbstractMutableList>.iterator()
        
        override fun contains(element: K): Boolean = containsKey(element)
        
        override fun get(index: Int): K = keyAt(index)
        
        override fun elementAt(index: Int): K = keyAt(index)
        
        override fun indexOf(element: K): Int = indexOfKey(element)
        
        override fun add(element: K): Boolean {
            throw UnsupportedOperationException("Use put(key, value) to insert a key")
        }
        
        override fun addAt(index: Int, element: K): Boolean {
            throw UnsupportedOperationException("Use putAt(index, key, value) to insert a key")
        }
        
        override fun add(index: Int, element: K) {
            throw UnsupportedOperationException("Use putAt(index, key, value) to insert a key")
        }
        
        override fun move(element: K, index: Int) {
            this@ArrayHashOrderedMap.move(element, index)
        }
        
        override fun set(index: Int, element: K): K {
            val previous = keyAt(index)
            replaceKeyAt(index, element)
            return previous
        }
        
        override fun remove(element: K): Boolean = this@ArrayHashOrderedMap.remove(element) != null
        
        override fun removeAt(index: Int): K = this@ArrayHashOrderedMap.removeAt(index).key
        
        override fun clear() {
            this@ArrayHashOrderedMap.clear()
        }
        
        override fun copy(): MutableOrderedSet<K> = ArrayHashOrderedSet(orderedKeys.toList())
        
        override fun toSet(): Set<K> = LinkedHashSet<K>(size).also { it.addAll(orderedKeys) }
        
        override fun toMutableSet(): MutableSet<K> = LinkedHashSet(toSet())
    }
    
    private inner class ValuesView : AbstractMutableList<V>() {
        override val size: Int
            get() = this@ArrayHashOrderedMap.size
        
        override fun get(index: Int): V = valueAt(index)
        
        override fun add(index: Int, element: V) {
            throw UnsupportedOperationException("Use putAt(index, key, value) to insert a value")
        }
        
        override fun set(index: Int, element: V): V = setValueAt(index, element)
        
        override fun removeAt(index: Int): V = this@ArrayHashOrderedMap.removeAt(index).value
    }
    
    private inner class EntriesView : AbstractMutableList<MutableMap.MutableEntry<K, V>>() {
        override val size: Int
            get() = this@ArrayHashOrderedMap.size
        
        override fun get(index: Int): MutableMap.MutableEntry<K, V> = entryAt(index)
        
        override fun add(index: Int, element: MutableMap.MutableEntry<K, V>) {
            require(!containsKey(element.key)) { "Key already exists: ${element.key}" }
            putAt(index, element.key, element.value)
        }
        
        override fun set(index: Int, element: MutableMap.MutableEntry<K, V>): MutableMap.MutableEntry<K, V> {
            return replaceEntryAt(index, element.key, element.value)
        }
        
        override fun removeAt(index: Int): MutableMap.MutableEntry<K, V> = this@ArrayHashOrderedMap.removeAt(index)
    }
    
    private inner class EntryView(
        private val keyRef: K,
    ) : MutableMap.MutableEntry<K, V> {
        override val key: K
            get() = keyRef
        
        override val value: V
            get() = valuesByKey.getValue(keyRef)
        
        override fun setValue(newValue: V): V {
            val previous = valuesByKey.getValue(keyRef)
            valuesByKey[keyRef] = newValue
            markChanged()
            return previous
        }
        
        override fun equals(other: Any?): Boolean {
            if (other !is Map.Entry<*, *>) return false
            return other.key == key && other.value == value
        }
        
        override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
        
        override fun toString(): String = "$key=$value"
    }
    
    private data class EntrySnapshot<K, V>(
        override val key: K,
        private var storedValue: V,
    ) : MutableMap.MutableEntry<K, V> {
        override val value: V
            get() = storedValue
        
        override fun setValue(newValue: V): V {
            val previous = storedValue
            storedValue = newValue
            return previous
        }
        
        override fun equals(other: Any?): Boolean {
            if (other !is Map.Entry<*, *>) return false
            return other.key == key && other.value == value
        }
        
        override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
        
        override fun toString(): String = "$key=$value"
    }
}
