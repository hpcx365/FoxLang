package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.orderedMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TypeNormalizationPassTest {
    
    @Test
    fun normalizesNestedTupleTransformsAndCompressesComponents() {
        val type = FoxTupleMergeTuplesType(
            listOf(
                FoxTupleType(listOf(FoxIntType, FoxIntType)),
                FoxTupleType(
                    listOf(
                        FoxTupleGetComponentType(
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
        val type = FoxTupleGetComponentType(
            FoxUnresolvedType(
                "Box",
                listOf(
                    FoxTupleMergeTuplesType(
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
            FoxTupleGetComponentType(
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
        val type = FoxMethodGetParameterStructType(
            FoxMethodType(
                FoxRefType(FoxIntType),
                orderedMapOf(
                    "left" to FoxTupleMergeTuplesType(
                        listOf(
                            FoxTupleType(listOf(FoxStringType)),
                            FoxTupleType(listOf(FoxStringType)),
                        ),
                    ),
                    "right" to FoxArrayGetElementTypeType(FoxArrayType(FoxDoubleType)),
                ),
                FoxMethodGetReturnTypeType(
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
        val type = FoxStructMergeStructsType(
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
        val type = FoxTupleGetComponentBackType(
            FoxTupleType(listOf(FoxIntType, FoxFloatType)),
            2,
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationIndexOutOfBounds>(result.errors.single())
        
        assertEquals(2, error.index)
        assertEquals(2, error.size)
    }
    
    @Test
    fun clampsNonExactTupleCount() {
        val type = FoxTupleDropFirstComponentsType(
            FoxTupleType(listOf(FoxIntType)),
            2,
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(FoxTupleType(emptyList()), result.type)
    }
    
    @Test
    fun detectsExactTupleCountOutOfBounds() {
        val type = FoxTupleDropFirstComponentsExactType(
            FoxTupleType(listOf(FoxIntType)),
            2,
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationIndexOutOfBounds>(result.errors.single())
        
        assertEquals(2, error.index)
        assertEquals(1, error.size)
    }
    
    @Test
    fun ignoresMissingFieldNameInNonExactDropOperation() {
        val type = FoxStructDropFieldsType(
            FoxStructType(orderedMapOf("name" to FoxStringType)),
            setOf("age"),
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(FoxStructType(orderedMapOf("name" to FoxStringType)), result.type)
    }
    
    @Test
    fun ignoresMissingFieldNameInNonExactSelection() {
        val type = FoxStructSelectFieldsType(
            FoxStructType(orderedMapOf("age" to FoxIntType)),
            setOf("name", "age"),
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(FoxStructType(orderedMapOf("age" to FoxIntType)), result.type)
    }
    
    @Test
    fun detectsMissingFieldNameInExactSelection() {
        val type = FoxStructSelectFieldsExactType(
            FoxStructType(orderedMapOf("age" to FoxIntType)),
            setOf("name", "age"),
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationNameNotFound>(result.errors.single())
        
        assertEquals("name", error.name)
    }
    
    @Test
    fun detectsDuplicateFieldDuringStructMerge() {
        val type = FoxStructMergeStructsType(
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
        val type = FoxObjectMergeObjectsType(
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
        val type = FoxEnumMergeEnumsType(
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
        val type = FoxStructMergeStructsType(
            listOf(
                FoxStructType(orderedMapOf("name" to FoxStringType)),
                FoxUnresolvedType(
                    "Other",
                    listOf(
                        FoxTupleMergeTuplesType(
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
            FoxStructMergeStructsType(
                listOf(
                    FoxStructType(orderedMapOf("name" to FoxStringType)),
                    FoxUnresolvedType("Other", listOf(FoxTupleType(listOf(FoxIntType, FoxIntType)))),
                ),
            ),
            result.type,
        )
    }
    
    @Test
    fun preservesOriginalFieldOrder() {
        val type = FoxStructSelectFieldsType(
            FoxStructType(
                orderedMapOf(
                    "a" to FoxIntType,
                    "b" to FoxStringType,
                    "c" to FoxDoubleType,
                ),
            ),
            setOf("c", "a"),
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(
            FoxStructType(
                orderedMapOf(
                    "a" to FoxIntType,
                    "c" to FoxDoubleType,
                ),
            ),
            result.type,
        )
    }
}
