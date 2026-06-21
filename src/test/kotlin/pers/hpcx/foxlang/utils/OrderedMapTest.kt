package pers.hpcx.foxlang.utils

import java.util.AbstractMap.SimpleEntry
import kotlin.test.*

class OrderedMapTest {
    private fun <K, V> mutableOrderedMapOf(vararg entries: Pair<K, V>): MutableOrderedMap<K, V> {
        return pers.hpcx.foxlang.utils.mutableOrderedMapOf(*entries)
    }
    
    private fun <K, V> entryOf(key: K, value: V): MutableMap.MutableEntry<K, V> {
        return SimpleEntry(key, value)
    }
    
    @Test
    fun preservesInsertionOrder() {
        val map = mutableOrderedMapOf<String, Int>()
        map["b"] = 2
        map["a"] = 1
        map["c"] = 3
        
        assertEquals(listOf("b", "a", "c"), map.keys.elements)
        assertEquals(listOf(2, 1, 3), map.values)
        assertEquals(listOf(entryOf("b", 2), entryOf("a", 1), entryOf("c", 3)), map.entries)
    }
    
    @Test
    fun updatingExistingKeyKeepsOrder() {
        val map = mutableOrderedMapOf("a" to 1, "b" to 2)
        
        val previous = map.put("a", 3)
        
        assertEquals(1, previous)
        assertEquals(listOf("a", "b"), map.keys.elements)
        assertEquals(listOf(3, 2), map.values)
    }
    
    @Test
    fun putAtInsertsAndReorders() {
        val map = mutableOrderedMapOf("a" to 1, "b" to 2, "c" to 3)
        
        map.putAt(1, "x", 9)
        map.putAt(0, "c", 30)
        
        assertEquals(listOf("c", "a", "x", "b"), map.keys.elements)
        assertEquals(listOf(30, 1, 9, 2), map.values)
    }
    
    @Test
    fun moveChangesOrderOnly() {
        val map = mutableOrderedMapOf("a" to 1, "b" to 2, "c" to 3)
        
        map.move("a", 2)
        
        assertEquals(listOf("b", "c", "a"), map.keys.elements)
        assertEquals(1, map.getValue("a"))
    }
    
    @Test
    fun removeByKeyAndIndexKeepStructuresInSync() {
        val map = mutableOrderedMapOf("a" to 1, "b" to 2, "c" to 3)
        
        val removedByKey = map.remove("b")
        val removedByIndex = map.removeAt(0)
        
        assertEquals(2, removedByKey)
        assertEquals(entryOf("a", 1), removedByIndex)
        assertEquals(listOf("c"), map.keys.elements)
        assertEquals(listOf(3), map.values)
        assertFalse("a" in map)
        assertFalse("b" in map)
    }
    
    @Test
    fun getOrPutAndCopyWorkAsExpected() {
        val map = mutableOrderedMapOf("a" to 1)
        
        val existing = map.getOrPut("a") { 99 }
        val inserted = map.getOrPut("b") { 2 }
        val copy = map.copy()
        map["c"] = 3
        
        assertEquals(1, existing)
        assertEquals(2, inserted)
        assertEquals(listOf("a", "b"), copy.keys.elements)
        assertEquals(listOf(1, 2), copy.values)
        assertEquals(listOf("a", "b", "c"), map.keys.elements)
    }
    
    @Test
    fun emptyAndLookupHelpersBehaveNormally() {
        val map = mutableOrderedMapOf<String, Int>()
        
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertNull(map["missing"])
        assertEquals(-1, map.indexOfKey("missing"))
        
        map["x"] = 7
        
        assertTrue(map.isNotEmpty())
        assertEquals("x", map.firstKey())
        assertEquals("x", map.lastKey())
        assertEquals(entryOf("x", 7), map.firstEntry())
        assertEquals(entryOf("x", 7), map.lastEntry())
        assertEquals("x", map.keyAt(0))
        assertEquals(7, map.valueAt(0))
        assertEquals(entryOf("x", 7), map.entryAt(0))
    }
    
    @Test
    fun orderedMapFactoryReturnsReadableView() {
        val map: OrderedMap<String, Int> = orderedMapOf("a" to 1, "b" to 2)
        
        assertEquals(listOf("a", "b"), map.keys.elements)
        assertEquals(mapOf("a" to 1, "b" to 2), map.toMap())
    }
    
    @Test
    fun mutableViewsWriteThroughToBackingMap() {
        val map = mutableOrderedMapOf("a" to 1, "b" to 2, "c" to 3)
        
        map.keys.elements[1] = "x"
        map.values[0] = 10
        map.entries[2].setValue(30)
        map.entries.add(1, entryOf("y", 20))
        val removedValue = map.values.removeAt(0)
        
        assertEquals(10, removedValue)
        assertEquals(listOf("y", "x", "c"), map.keys.elements)
        assertEquals(listOf(20, 2, 30), map.values)
        assertEquals(2, map.getValue("x"))
        assertEquals(30, map.getValue("c"))
    }
    
    @Test
    fun equalityAndHashCodeAreOrderSensitive() {
        val left = mutableOrderedMapOf("a" to 1, "b" to 2)
        val same = mutableOrderedMapOf("a" to 1, "b" to 2)
        val differentOrder = mutableOrderedMapOf("b" to 2, "a" to 1)
        
        assertEquals(left, same)
        assertEquals(left.hashCode(), same.hashCode())
        assertNotEquals(left, differentOrder)
    }
    
    @Test
    fun topLevelFactoriesSupportConversions() {
        val fromEntries = listOf("a" to 1, "b" to 2).toOrderedMap()
        assertEquals(listOf("a", "b"), fromEntries.keys.elements)
    }
}
