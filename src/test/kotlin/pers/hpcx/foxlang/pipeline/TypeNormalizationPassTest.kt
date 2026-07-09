package pers.hpcx.foxlang.pipeline

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.orderedMapOf
import pers.hpcx.foxlang.utils.orderedSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TypeNormalizationPassTest {
    
    @Test
    fun normalizesNestedTupleTransformsAndCompressesComponents() {
        val type = FoxTupleMergeComponentsOfType(
            listOf(
                FoxTupleType(listOf(FoxIntType, FoxIntType)),
                FoxTupleType(
                    listOf(
                        FoxTupleComponentAtType(
                            FoxTupleType(listOf(FoxFloatType, FoxIntType, FoxDoubleType)),
                            1,
                        ),
                    ),
                ),
            ),
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(FoxTupleType(listOf(FoxIntType, FoxIntType, FoxIntType)), result.type)
    }
    
    @Test
    fun keepsTransformWhenBaseCannotBeSafelyExpanded() {
        val type = FoxTupleComponentAtType(
            FoxUnresolvedType(
                "Box",
                listOf(
                    FoxTupleMergeComponentsOfType(
                        listOf(
                            FoxTupleType(listOf(FoxIntType)),
                            FoxTupleType(listOf(FoxIntType)),
                        ),
                    ),
                ),
            ),
            0,
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(
            FoxTupleComponentAtType(
                FoxUnresolvedType(
                    "Box",
                    listOf(FoxTupleType(listOf(FoxIntType, FoxIntType))),
                ),
                0,
            ),
            result.type,
        )
    }
    
    @Test
    fun normalizesMethodDerivedTransforms() {
        val type = FoxMethodParametersOfType(
            FoxMethodType(
                FoxRefType(FoxIntType),
                orderedMapOf(
                    "left" to FoxTupleMergeComponentsOfType(
                        listOf(
                            FoxTupleType(listOf(FoxStringType)),
                            FoxTupleType(listOf(FoxStringType)),
                        ),
                    ),
                    "right" to FoxArrayElementOfType(FoxArrayType(FoxDoubleType)),
                ),
                FoxMethodReturnOfType(
                    FoxMethodType(
                        FoxUnitType,
                        orderedMapOf(),
                        FoxTupleType(listOf(FoxBoolType)),
                    ),
                ),
            ),
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(
            FoxStructType(
                orderedMapOf(
                    "left" to FoxTupleType(listOf(FoxStringType, FoxStringType)),
                    "right" to FoxDoubleType,
                ),
            ),
            result.type,
        )
    }
    
    @Test
    fun detectsFamilyMismatch() {
        val type = FoxStructMergeFieldsOfType(
            listOf(
                FoxStructType(orderedMapOf("name" to FoxStringType)),
                FoxObjectType(mapOf("age" to FoxIntType)),
            ),
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationFamilyMismatch>(result.errors.single())
        
        assertEquals("Struct", error.expectedFamily)
        assertEquals(FoxObjectType(mapOf("age" to FoxIntType)), error.actualType)
    }
    
    @Test
    fun detectsIndexOutOfBounds() {
        val type = FoxTupleLastComponentAtType(
            FoxTupleType(listOf(FoxIntType, FoxFloatType)),
            2,
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationIndexOutOfBounds>(result.errors.single())
        
        assertEquals(2, error.index)
        assertEquals(2, error.size)
    }
    
    @Test
    fun detectsMissingFieldNameInDropOperation() {
        val type = FoxStructDropFieldsOfType(
            FoxStructType(orderedMapOf("name" to FoxStringType)),
            setOf("age"),
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationNameNotFound>(result.errors.single())
        
        assertEquals("age", error.name)
    }
    
    @Test
    fun detectsDuplicateFieldDuringStructMerge() {
        val type = FoxStructMergeFieldsOfType(
            listOf(
                FoxStructType(orderedMapOf("name" to FoxStringType)),
                FoxStructType(orderedMapOf("name" to FoxIntType)),
            ),
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationDuplicateName>(result.errors.single())
        
        assertEquals("name", error.name)
    }
    
    @Test
    fun detectsDuplicateMemberDuringObjectMerge() {
        val type = FoxObjectMergeMembersOfType(
            listOf(
                FoxObjectType(mapOf("name" to FoxStringType)),
                FoxObjectType(mapOf("name" to FoxIntType)),
            ),
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationDuplicateName>(result.errors.single())
        
        assertEquals("name", error.name)
    }
    
    @Test
    fun detectsDuplicateItemDuringEnumMerge() {
        val type = FoxEnumMergeEntriesOfType(
            listOf(
                FoxEnumType(mapOf("Ok" to FoxStringType)),
                FoxEnumType(mapOf("Ok" to FoxIntType)),
            ),
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationDuplicateName>(result.errors.single())
        
        assertEquals("Ok", error.name)
    }
    
    @Test
    fun keepsMergeWhenArgumentRemainsUnresolved() {
        val type = FoxStructMergeFieldsOfType(
            listOf(
                FoxStructType(orderedMapOf("name" to FoxStringType)),
                FoxUnresolvedType(
                    "Other",
                    listOf(
                        FoxTupleMergeComponentsOfType(
                            listOf(
                                FoxTupleType(listOf(FoxIntType)),
                                FoxTupleType(listOf(FoxIntType)),
                            ),
                        ),
                    ),
                ),
            ),
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(
            FoxStructMergeFieldsOfType(
                listOf(
                    FoxStructType(orderedMapOf("name" to FoxStringType)),
                    FoxUnresolvedType("Other", listOf(FoxTupleType(listOf(FoxIntType, FoxIntType)))),
                ),
            ),
            result.type,
        )
    }
    
    @Test
    fun preservesRequestedFieldOrder() {
        val type = FoxStructFieldsOfType(
            FoxStructType(
                orderedMapOf(
                    "a" to FoxIntType,
                    "b" to FoxStringType,
                    "c" to FoxDoubleType,
                ),
            ),
            orderedSetOf("c", "a"),
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(
            FoxStructType(
                orderedMapOf(
                    "c" to FoxDoubleType,
                    "a" to FoxIntType,
                ),
            ),
            result.type,
        )
    }
}
