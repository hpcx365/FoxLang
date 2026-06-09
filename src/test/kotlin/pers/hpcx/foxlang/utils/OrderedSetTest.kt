package pers.hpcx.foxlang.utils

import kotlin.test.*

class OrderedSetTest {
    private fun <E> mutableOrderedSetOf(vararg elements: E): MutableOrderedSet<E> {
        return pers.hpcx.foxlang.utils.mutableOrderedSetOf(*elements)
    }
    
    @Test
    fun preservesInsertionOrderAndUniqueness() {
        val set = mutableOrderedSetOf<String>()
        
        assertTrue(set.add("b"))
        assertTrue(set.add("a"))
        assertFalse(set.add("b"))
        
        assertEquals(listOf("b", "a"), set.elements)
    }
    
    @Test
    fun addAtInsertsAndReordersExistingElement() {
        val set = mutableOrderedSetOf("a", "b", "c")
        
        assertTrue(set.addAt(1, "x"))
        assertFalse(set.addAt(0, "c"))
        
        assertEquals(listOf("c", "a", "x", "b"), set.elements)
    }
    
    @Test
    fun moveAndRemoveKeepOrderConsistent() {
        val set = mutableOrderedSetOf("a", "b", "c")
        
        set.move("a", 2)
        assertTrue(set.remove("b"))
        val removed = set.removeAt(0)
        
        assertEquals("c", removed)
        assertEquals(listOf("a"), set.elements)
    }
    
    @Test
    fun mutableElementsViewWritesThrough() {
        val set = mutableOrderedSetOf("a", "b", "c")
        
        set.elements[1] = "x"
        set.elements.add(1, "y")
        val removed = set.elements.removeAt(0)
        
        assertEquals("a", removed)
        assertEquals(listOf("y", "x", "c"), set.elements)
        assertTrue("x" in set)
    }
    
    @Test
    fun copyFactoriesAndLookupHelpersBehaveNormally() {
        val set = mutableOrderedSetOf("a")
        
        val copy = set.copy()
        set.add("b")
        val readable: OrderedSet<String> = orderedSetOf("x", "y")
        val fromIterable = listOf("m", "n").toMutableOrderedSet()
        
        assertEquals(listOf("a"), copy.elements)
        assertEquals(listOf("a", "b"), set.elements)
        assertEquals(listOf("x", "y"), readable.elements)
        assertEquals(listOf("m", "n"), fromIterable.elements)
        assertEquals("a", copy.first())
        assertEquals("a", copy.last())
        assertEquals(0, copy.indexOf("a"))
        assertEquals(-1, copy.indexOf("z"))
    }
    
    @Test
    fun equalityAndHashCodeAreOrderSensitive() {
        val left = mutableOrderedSetOf("a", "b")
        val same = mutableOrderedSetOf("a", "b")
        val differentOrder = mutableOrderedSetOf("b", "a")
        
        assertEquals(left, same)
        assertEquals(left.hashCode(), same.hashCode())
        assertNotEquals(left, differentOrder)
    }
}
