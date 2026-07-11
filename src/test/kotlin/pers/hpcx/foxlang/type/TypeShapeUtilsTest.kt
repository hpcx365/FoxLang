package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.RleArrayList
import pers.hpcx.foxlang.utils.orderedMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        
        assertEquals(FoxDoubleType, tuple.getComponentBack(0))
        assertEquals(FoxIntType, tuple.getComponentBack(1))
        assertEquals("height" to FoxDoubleType, struct.getFieldTypeByIndexBack(0).toPair())
        assertEquals("age" to FoxIntType, struct.getFieldTypeByIndexBack(1).toPair())
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
        
        assertEquals("age" to FoxIntType, struct.getFieldTypeByIndex(1).toPair())
        assertEquals(
            FoxStructType(orderedMapOf("name" to FoxStringType, "age" to FoxIntType)),
            struct.getFirstFields(2),
        )
        assertEquals(
            FoxStructType(orderedMapOf("age" to FoxIntType, "height" to FoxDoubleType)),
            struct.getLastFields(2),
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
            FoxStructType(orderedMapOf("a" to FoxIntType, "c" to FoxDoubleType)),
            left.selectFields(setOf("c", "a")),
        )
        assertEquals(
            FoxStructType(orderedMapOf("a" to FoxIntType, "c" to FoxDoubleType)),
            left.dropFields(setOf("b")),
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
            listOf(left, right).mergeStructs(),
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
        
        assertEquals(FoxStringType, left.getMemberType("b"))
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
            listOf(left, right).mergeObjects(),
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
        
        assertEquals(FoxStringType, left.getEntryType("B"))
        assertEquals(setOf("C", "A"), left.selectEntries(setOf("C", "A")).entries.keys)
        assertEquals(setOf("A", "C"), left.dropEntries(setOf("B")).entries.keys)
        val merged = listOf(left, right).mergeEnums()
        assertEquals(setOf("A", "B", "C", "D"), merged.entries.keys)
        assertEquals(FoxBoolType, merged.entries.getValue("B"))
    }
}
