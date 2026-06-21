package pers.hpcx.foxlang.utils

import kotlin.test.*

class RleArrayListTest {
    @Test
    fun behavesLikeMutableListForBasicOperations() {
        val list = RleArrayList(listOf("a", "a", "b", "b", "b"))
        
        assertEquals(5, list.size)
        assertEquals("a", list[0])
        assertEquals("b", list[4])
        
        list.add(2, "x")
        assertEquals(listOf("a", "a", "x", "b", "b", "b"), list)
        
        val replaced = list.set(3, "x")
        assertEquals("b", replaced)
        assertEquals(listOf("a", "a", "x", "x", "b", "b"), list)
        
        val removed = list.removeAt(1)
        assertEquals("a", removed)
        assertEquals(listOf("a", "x", "x", "b", "b"), list)
    }
    
    @Test
    fun mergesRunsWhenAdjacentValuesBecomeEqual() {
        val list = RleArrayList(listOf("a", "a", "b", "a", "a"))
        
        list.removeAt(2)
        
        assertEquals(listOf("a", "a", "a", "a"), list)
        assertEquals(1, runCountOf(list))
    }
    
    @Test
    fun insertionIntoRunSplitsButEdgeInsertionsReuseNeighbors() {
        val list = RleArrayList(listOf(1, 1, 1, 2, 2))
        
        list.add(1, 9)
        assertEquals(listOf(1, 9, 1, 1, 2, 2), list)
        assertEquals(4, runCountOf(list))
        
        list.add(4, 1)
        assertEquals(listOf(1, 9, 1, 1, 1, 2, 2), list)
        assertEquals(4, runCountOf(list))
    }
    
    @Test
    fun supportsNullsAndBulkAppending() {
        val list = RleArrayList<Int?>()
        
        list += null
        list += null
        list += 1
        list += 1
        list += null
        
        assertEquals(listOf<Int?>(null, null, 1, 1, null), list)
        assertEquals(3, runCountOf(list))
    }
    
    @Test
    fun clearResetsContents() {
        val list = RleArrayList(listOf("x", "x", "y"))
        
        assertFalse(list.isEmpty())
        list.clear()
        
        assertTrue(list.isEmpty())
        assertEquals(emptyList(), list)
        assertEquals(0, runCountOf(list))
    }
    
    @Test
    fun listIteratorTraversesRunsWithoutBreakingMutationSemantics() {
        val list = RleArrayList(listOf("a", "a", "b", "b", "c"))
        val iterator = list.listIterator(1)
        
        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasPrevious())
        assertEquals("a", iterator.next())
        assertEquals("b", iterator.next())
        iterator.set("x")
        assertEquals(listOf("a", "a", "x", "b", "c"), list)
        
        assertEquals("x", iterator.previous())
        iterator.remove()
        assertEquals(listOf("a", "a", "b", "c"), list)
        
        iterator.add("y")
        assertEquals(listOf("a", "a", "y", "b", "c"), list)
        assertEquals("b", iterator.next())
    }
    
    @Test
    fun iteratorDetectsConcurrentModification() {
        val list = RleArrayList(listOf(1, 1, 2))
        val iterator = list.iterator()
        
        assertEquals(1, iterator.next())
        list.add(3)
        
        assertFailsWith<ConcurrentModificationException> {
            iterator.next()
        }
    }
    
    @Test
    fun topLevelFactoriesCanBuildFromRunsEfficiently() {
        val list = rleArrayListOfRuns("a" to 3, "b" to 2, "b" to 1)
        val converted = listOf("x" to 2, "y" to 1).toRleArrayListFromRuns()
        
        assertEquals(listOf("a", "a", "a", "b", "b", "b"), list)
        assertEquals(2, runCountOf(list))
        assertEquals(listOf("x", "x", "y"), converted)
        assertEquals(2, runCountOf(converted))
    }
    
    @Test
    fun equalityAndHashCodeFollowListValueSemantics() {
        val left = RleArrayList(listOf("a", "a", "b", "b"))
        val same = RleArrayList(listOf("a", "a", "b", "b"))
        val plain = listOf("a", "a", "b", "b")
        val different = RleArrayList(listOf("a", "b", "b", "b"))
        
        assertEquals(left, same)
        assertEquals(left.hashCode(), same.hashCode())
        assertEquals(left, plain)
        assertEquals(plain, left)
        assertEquals(plain.hashCode(), left.hashCode())
        assertNotEquals(left, different)
    }
    
    @Test
    fun hashCodeIsInvalidatedAfterMutation() {
        val list = RleArrayList(listOf(1, 1, 2))
        
        val initialHash = list.hashCode()
        list.add(2)
        val afterAdd = list.hashCode()
        list[0] = 3
        val afterSet = list.hashCode()
        list.removeAt(1)
        val afterRemove = list.hashCode()
        
        assertNotEquals(initialHash, afterAdd)
        assertNotEquals(afterAdd, afterSet)
        assertNotEquals(afterSet, afterRemove)
        assertEquals(listOf(3, 2, 2).hashCode(), afterRemove)
    }
    
    private fun runCountOf(list: RleArrayList<*>): Int {
        val field = list.javaClass.getDeclaredField("runs")
        field.isAccessible = true
        val runs = field.get(list) as List<*>
        return runs.size
    }
}
