package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.mutableOrderedSetOf
import pers.hpcx.foxlang.utils.orderedMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypePatternCheckPassTest {
    @Test
    fun reportsInvalidPatternFamilies() {
        val invalidMerge = FoxTupleMergeComponentsOfType(
            listOf(FoxStructType(orderedMapOf("id" to FoxIntType))),
        )
        val invalidTarget = FoxStructFieldOfType(FoxIntType, "name")
        
        val mergeResult = TypePatternCheckPass.run(invalidMerge)
        val targetResult = TypePatternCheckPass.run(invalidTarget)
        
        assertFalse(mergeResult.isSuccess)
        assertFalse(targetResult.isSuccess)
        assertEquals(TypePatternIssueCode.INVALID_PATTERN_FAMILY, mergeResult.issues.single().code)
        assertEquals(TypePatternIssueCode.INVALID_PATTERN_FAMILY, targetResult.issues.single().code)
    }
    
    @Test
    fun reportsInvalidPatternShapes() {
        val negativeIndex = FoxTupleComponentAtType(FoxAnyTupleType, -1)
        val negativeReverseIndex = FoxStructLastFieldAtType(FoxAnyStructType, -1)
        val negativeCount = FoxStructFirstFieldsOfType(FoxAnyStructType, -1)
        
        val indexResult = TypePatternCheckPass.run(negativeIndex)
        val reverseIndexResult = TypePatternCheckPass.run(negativeReverseIndex)
        val countResult = TypePatternCheckPass.run(negativeCount)
        
        assertFalse(indexResult.isSuccess)
        assertFalse(reverseIndexResult.isSuccess)
        assertFalse(countResult.isSuccess)
        assertEquals(TypePatternIssueCode.INVALID_PATTERN_SHAPE, indexResult.issues.single().code)
        assertEquals(TypePatternIssueCode.INVALID_PATTERN_SHAPE, reverseIndexResult.issues.single().code)
        assertEquals(TypePatternIssueCode.INVALID_PATTERN_SHAPE, countResult.issues.single().code)
    }
    
    @Test
    fun warnsWhenMergeHasNoArguments() {
        val tupleResult = TypePatternCheckPass.run(FoxTupleMergeComponentsOfType(emptyList()))
        val structResult = TypePatternCheckPass.run(FoxStructMergeFieldsOfType(emptyList()))
        val objectResult = TypePatternCheckPass.run(FoxObjectMergeMembersOfType(emptyList()))
        val enumResult = TypePatternCheckPass.run(FoxEnumMergeItemsOfType(emptyList()))
        
        assertTrue(tupleResult.isSuccess)
        assertTrue(structResult.isSuccess)
        assertTrue(objectResult.isSuccess)
        assertTrue(enumResult.isSuccess)
        assertEquals(TypePatternIssueCode.EMPTY_PATTERN_MERGE, tupleResult.issues.single().code)
        assertEquals(TypePatternIssueCode.EMPTY_PATTERN_MERGE, structResult.issues.single().code)
        assertEquals(TypePatternIssueCode.EMPTY_PATTERN_MERGE, objectResult.issues.single().code)
        assertEquals(TypePatternIssueCode.EMPTY_PATTERN_MERGE, enumResult.issues.single().code)
        assertEquals(TypePassIssueSeverity.WARNING, tupleResult.issues.single().severity)
        assertEquals(TypePassIssueSeverity.WARNING, structResult.issues.single().severity)
        assertEquals(TypePassIssueSeverity.WARNING, objectResult.issues.single().severity)
        assertEquals(TypePassIssueSeverity.WARNING, enumResult.issues.single().severity)
    }
    
    @Test
    fun warnsWhenBoundsAreProvablyOutOfRange() {
        val tupleIndex = FoxTupleComponentAtType(listOf(FoxIntType).toFoxTupleType(), 1)
        val tupleCount = FoxTupleFirstComponentsOfType(listOf(FoxIntType).toFoxTupleType(), 2)
        val structIndex = FoxStructFieldAtType(FoxStructType(orderedMapOf("id" to FoxIntType)), 1)
        
        val tupleIndexResult = TypePatternCheckPass.run(tupleIndex)
        val tupleCountResult = TypePatternCheckPass.run(tupleCount)
        val structIndexResult = TypePatternCheckPass.run(structIndex)
        
        assertTrue(tupleIndexResult.isSuccess)
        assertTrue(tupleCountResult.isSuccess)
        assertTrue(structIndexResult.isSuccess)
        assertEquals(TypePatternIssueCode.EMPTY_PATTERN_BOUNDS, tupleIndexResult.issues.single().code)
        assertEquals(TypePatternIssueCode.EMPTY_PATTERN_BOUNDS, tupleCountResult.issues.single().code)
        assertEquals(TypePatternIssueCode.EMPTY_PATTERN_BOUNDS, structIndexResult.issues.single().code)
        assertEquals(TypePassIssueSeverity.WARNING, tupleIndexResult.issues.single().severity)
        assertEquals(TypePassIssueSeverity.WARNING, tupleCountResult.issues.single().severity)
        assertEquals(TypePassIssueSeverity.WARNING, structIndexResult.issues.single().severity)
    }
    
    @Test
    fun acceptsValidPatterns() {
        val valid = FoxStructFieldsOfType(
            FoxStructMergeFieldsOfType(
                listOf(
                    FoxStructType(orderedMapOf("id" to FoxIntType)),
                    FoxAnyStructType,
                ),
            ),
            mutableOrderedSetOf("id"),
        )
        
        val result = TypePatternCheckPass.run(valid)
        
        assertTrue(result.isSuccess)
        assertTrue(result.issues.isEmpty())
    }
    
    @Test
    fun reportsConflictingNamedConstraints() {
        val conflictingStruct = FoxStructMergeFieldsOfType(
            listOf(
                FoxStructType(orderedMapOf("id" to FoxIntType)),
                FoxStructType(orderedMapOf("id" to FoxIntType)),
            ),
        )
        val conflictingObject = FoxObjectMergeMembersOfType(
            listOf(
                FoxObjectType(mapOf("name" to FoxIntType)),
                FoxObjectType(mapOf("name" to FoxStringType)),
            ),
        )
        val conflictingEnum = FoxEnumMergeItemsOfType(
            listOf(
                FoxEnumType(mapOf("Success" to FoxIntType)),
                FoxEnumType(mapOf("Success" to FoxStringType)),
            ),
        )
        
        val structResult = TypePatternCheckPass.run(conflictingStruct)
        val objectResult = TypePatternCheckPass.run(conflictingObject)
        val enumResult = TypePatternCheckPass.run(conflictingEnum)
        
        assertTrue(structResult.isSuccess)
        assertTrue(objectResult.isSuccess)
        assertTrue(enumResult.isSuccess)
        assertEquals(TypePatternIssueCode.CONFLICTING_PATTERN_CONSTRAINT, structResult.issues.single().code)
        assertEquals(TypePatternIssueCode.CONFLICTING_PATTERN_CONSTRAINT, objectResult.issues.single().code)
        assertEquals(TypePatternIssueCode.CONFLICTING_PATTERN_CONSTRAINT, enumResult.issues.single().code)
        assertEquals(TypePassIssueSeverity.WARNING, structResult.issues.single().severity)
        assertEquals(TypePassIssueSeverity.WARNING, objectResult.issues.single().severity)
        assertEquals(TypePassIssueSeverity.WARNING, enumResult.issues.single().severity)
    }
    
    @Test
    fun allowsCompatibleNamedConstraints() {
        val compatibleObject = FoxObjectMergeMembersOfType(
            listOf(
                FoxObjectType(mapOf("name" to FoxAnyType)),
                FoxObjectType(mapOf("name" to FoxStringType)),
            ),
        )
        val compatibleEnum = FoxEnumMergeItemsOfType(
            listOf(
                FoxEnumType(mapOf("Success" to FoxAnyType)),
                FoxEnumType(mapOf("Success" to FoxIntType)),
            ),
        )
        
        val objectResult = TypePatternCheckPass.run(compatibleObject)
        val enumResult = TypePatternCheckPass.run(compatibleEnum)
        
        assertTrue(objectResult.isSuccess)
        assertTrue(enumResult.isSuccess)
    }
}
