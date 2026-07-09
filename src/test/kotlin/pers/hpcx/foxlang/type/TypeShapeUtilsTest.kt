package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.RleArrayList
import pers.hpcx.foxlang.utils.orderedMapOf
import kotlin.test.*

class TypeShapeUtilsTest {
    
    @Test
    fun tupleFactoryChoosesStorageByCompressionBenefit() {
        val plainTuple = listOf(FoxIntType, FoxStringType).toFoxTupleType()
        val packedTuple = listOf(FoxIntType, FoxIntType, FoxStringType).toFoxTupleType()
        val countedTuple = listOf(FoxIntType to 3, FoxStringType to 1).toFoxTupleType()
        
        assertFalse(plainTuple.components is RleArrayList<*>)
        assertTrue(packedTuple.components is RleArrayList<*>)
        assertTrue(countedTuple.components is RleArrayList<*>)
    }
    
    @Test
    fun reverseIndexHelpersWork() {
        val tuple = listOf(FoxStringType, FoxIntType, FoxDoubleType).toFoxTupleType()
        val struct = FoxStructType(
            orderedMapOf(
                "name" to FoxStringType,
                "age" to FoxIntType,
                "height" to FoxDoubleType,
            ),
        )
        
        assertEquals(FoxDoubleType, tuple.lastComponentAt(0))
        assertEquals(FoxIntType, tuple.lastComponentAt(1))
        assertEquals("height" to FoxDoubleType, struct.lastFieldAt(0).toPair())
        assertEquals("age" to FoxIntType, struct.lastFieldAt(1).toPair())
        assertFailsWith<IllegalArgumentException> { tuple.lastComponentAt(-1) }
        assertFailsWith<IllegalArgumentException> { struct.lastFieldAt(-1) }
    }
    
    @Test
    fun structFieldOrderHelpersWork() {
        val struct = FoxStructType(
            orderedMapOf(
                "name" to FoxStringType,
                "age" to FoxIntType,
                "height" to FoxDoubleType,
            ),
        )
        
        assertEquals("age" to FoxIntType, struct.fieldAt(1).toPair())
        assertEquals(
            FoxStructType(orderedMapOf("name" to FoxStringType, "age" to FoxIntType)),
            struct.firstFields(2),
        )
        assertEquals(
            FoxStructType(orderedMapOf("age" to FoxIntType, "height" to FoxDoubleType)),
            struct.lastFields(2),
        )
        assertEquals(
            FoxStructType(orderedMapOf("age" to FoxIntType, "height" to FoxDoubleType)),
            struct.dropFirstFields(1),
        )
        assertEquals(
            FoxStructType(orderedMapOf("name" to FoxStringType, "age" to FoxIntType)),
            struct.dropLastFields(1),
        )
    }
    
    @Test
    fun structSelectionDropAndMergeRespectOrder() {
        val left = FoxStructType(
            orderedMapOf(
                "a" to FoxIntType,
                "b" to FoxStringType,
                "c" to FoxDoubleType,
            ),
        )
        val right = FoxStructType(
            orderedMapOf(
                "b" to FoxBoolType,
                "d" to FoxUnitType,
            ),
        )
        
        assertEquals(
            FoxStructType(orderedMapOf("c" to FoxDoubleType, "a" to FoxIntType)),
            left.selectFields(listOf("c", "a")),
        )
        assertEquals(
            FoxStructType(orderedMapOf("a" to FoxIntType, "c" to FoxDoubleType)),
            left.dropFields(listOf("b")),
        )
        assertEquals(
            FoxStructType(
                orderedMapOf(
                    "a" to FoxIntType,
                    "b" to FoxBoolType,
                    "c" to FoxDoubleType,
                    "d" to FoxUnitType,
                ),
            ),
            listOf(left, right).mergeStructFields(),
        )
    }
    
    @Test
    fun objectSelectionDropAndMergeIgnoreLogicalOrder() {
        val left = FoxObjectType(
            linkedMapOf(
                "a" to FoxIntType,
                "b" to FoxStringType,
                "c" to FoxDoubleType,
            ),
        )
        val right = FoxObjectType(
            linkedMapOf(
                "b" to FoxBoolType,
                "d" to FoxUnitType,
            ),
        )
        
        assertEquals(FoxStringType, left.member("b"))
        assertEquals(
            FoxObjectType(mapOf("c" to FoxDoubleType, "a" to FoxIntType)),
            left.selectMembers(linkedSetOf("c", "a")),
        )
        assertEquals(
            FoxObjectType(mapOf("a" to FoxIntType, "c" to FoxDoubleType)),
            left.dropMembers(setOf("b")),
        )
        assertEquals(
            FoxObjectType(
                mapOf(
                    "a" to FoxIntType,
                    "b" to FoxBoolType,
                    "c" to FoxDoubleType,
                    "d" to FoxUnitType,
                ),
            ),
            listOf(left, right).mergeObjectMembers(),
        )
    }
    
    @Test
    fun enumSelectionDropAndMergePreserveRequestedNames() {
        val left = FoxEnumType(
            linkedMapOf(
                "A" to FoxIntType,
                "B" to FoxStringType,
                "C" to FoxDoubleType,
            ),
        )
        val right = FoxEnumType(
            linkedMapOf(
                "B" to FoxBoolType,
                "D" to FoxUnitType,
            ),
        )
        
        assertEquals(FoxStringType, left.entry("B"))
        assertEquals(listOf("C", "A"), left.selectEntries(listOf("C", "A")).entries.keys.toList())
        assertEquals(listOf("A", "C"), left.dropEntries(listOf("B")).entries.keys.toList())
        val merged = listOf(left, right).mergeEnumEntries()
        assertEquals(listOf("A", "B", "C", "D"), merged.entries.keys.toList())
        assertEquals(FoxBoolType, merged.entries.getValue("B"))
    }
}
