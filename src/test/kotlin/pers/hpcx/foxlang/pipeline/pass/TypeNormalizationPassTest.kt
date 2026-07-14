package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.utils.orderedMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TypeNormalizationPassTest {
    
    @Test
    fun normalizesNestedTupleTransformsAndCompressesComponents() {
        val type = SurfaceTupleMergeTuplesType(
            listOf(
                SurfaceTupleType(listOf(FoxIntType, FoxIntType)),
                SurfaceTupleType(
                    listOf(
                        SurfaceTupleGetComponentType(
                            SurfaceTupleType(listOf(FoxFloatType, FoxIntType, FoxDoubleType)),
                            1,
                        ),
                    ),
                ),
            ),
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(SurfaceTupleType(listOf(FoxIntType, FoxIntType, FoxIntType)), result.type)
    }
    
    @Test
    fun normalizesMethodDerivedTransforms() {
        val type = SurfaceMethodGetParameterStructType(
            SurfaceMethodType(
                SurfaceRefType(FoxIntType),
                orderedMapOf(
                    "left" to SurfaceTupleMergeTuplesType(
                        listOf(
                            SurfaceTupleType(listOf(FoxStringType)),
                            SurfaceTupleType(listOf(FoxStringType)),
                        ),
                    ),
                    "right" to SurfaceArrayGetElementTypeType(SurfaceArrayType(FoxDoubleType)),
                ),
                SurfaceMethodGetReturnTypeType(
                    SurfaceMethodType(
                        FoxUnitType,
                        orderedMapOf(),
                        SurfaceTupleType(listOf(FoxBoolType)),
                    ),
                ),
            ),
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(
            SurfaceStructType(
                orderedMapOf(
                    "left" to SurfaceTupleType(listOf(FoxStringType, FoxStringType)),
                    "right" to FoxDoubleType,
                ),
            ),
            result.type,
        )
    }
    
    @Test
    fun detectsFamilyMismatch() {
        val type = SurfaceStructMergeStructsType(
            listOf(
                SurfaceStructType(orderedMapOf("name" to FoxStringType)),
                SurfaceObjectType(mapOf("age" to FoxIntType)),
            ),
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationFamilyMismatch>(result.errors.single())
        
        assertEquals("Struct", error.expectedFamily)
        assertEquals(SurfaceObjectType(mapOf("age" to FoxIntType)), error.actualType)
    }
    
    @Test
    fun detectsIndexOutOfBounds() {
        val type = SurfaceTupleGetComponentBackType(
            SurfaceTupleType(listOf(FoxIntType, FoxFloatType)),
            2,
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationIndexOutOfBounds>(result.errors.single())
        
        assertEquals(2, error.index)
        assertEquals(2, error.size)
    }
    
    @Test
    fun clampsNonExactTupleCount() {
        val type = SurfaceTupleDropFirstComponentsType(
            SurfaceTupleType(listOf(FoxIntType)),
            2,
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(SurfaceTupleType(emptyList()), result.type)
    }
    
    @Test
    fun detectsExactTupleCountOutOfBounds() {
        val type = SurfaceTupleDropFirstComponentsExactType(
            SurfaceTupleType(listOf(FoxIntType)),
            2,
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationIndexOutOfBounds>(result.errors.single())
        
        assertEquals(2, error.index)
        assertEquals(1, error.size)
    }
    
    @Test
    fun ignoresMissingFieldNameInNonExactDropOperation() {
        val type = SurfaceStructDropFieldsType(
            SurfaceStructType(orderedMapOf("name" to FoxStringType)),
            setOf("age"),
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(SurfaceStructType(orderedMapOf("name" to FoxStringType)), result.type)
    }
    
    @Test
    fun ignoresMissingFieldNameInNonExactSelection() {
        val type = SurfaceStructSelectFieldsType(
            SurfaceStructType(orderedMapOf("age" to FoxIntType)),
            setOf("name", "age"),
        )
        
        val result = assertIs<TypeNormalizationSuccess>(runTypeNormalization(type))
        
        assertEquals(SurfaceStructType(orderedMapOf("age" to FoxIntType)), result.type)
    }
    
    @Test
    fun detectsMissingFieldNameInExactSelection() {
        val type = SurfaceStructSelectFieldsExactType(
            SurfaceStructType(orderedMapOf("age" to FoxIntType)),
            setOf("name", "age"),
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationNameNotFound>(result.errors.single())
        
        assertEquals("name", error.name)
    }
    
    @Test
    fun detectsDuplicateFieldDuringStructMerge() {
        val type = SurfaceStructMergeStructsType(
            listOf(
                SurfaceStructType(orderedMapOf("name" to FoxStringType)),
                SurfaceStructType(orderedMapOf("name" to FoxIntType)),
            ),
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationDuplicateName>(result.errors.single())
        
        assertEquals("name", error.name)
    }
    
    @Test
    fun detectsDuplicateMemberDuringObjectMerge() {
        val type = SurfaceObjectMergeObjectsType(
            listOf(
                SurfaceObjectType(mapOf("name" to FoxStringType)),
                SurfaceObjectType(mapOf("name" to FoxIntType)),
            ),
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationDuplicateName>(result.errors.single())
        
        assertEquals("name", error.name)
    }
    
    @Test
    fun detectsDuplicateItemDuringEnumMerge() {
        val type = SurfaceEnumMergeEnumsType(
            listOf(
                SurfaceEnumType(mapOf("Ok" to FoxStringType)),
                SurfaceEnumType(mapOf("Ok" to FoxIntType)),
            ),
        )
        
        val result = assertIs<TypeNormalizationFailure>(runTypeNormalization(type))
        val error = assertIs<TypeNormalizationDuplicateName>(result.errors.single())
        
        assertEquals("Ok", error.name)
    }
    
    @Test
    fun preservesOriginalFieldOrder() {
        val type = SurfaceStructSelectFieldsType(
            SurfaceStructType(
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
            SurfaceStructType(
                orderedMapOf(
                    "a" to FoxIntType,
                    "c" to FoxDoubleType,
                ),
            ),
            result.type,
        )
    }
}

private val FoxUnitType = SurfacePrimitiveType(PrimitiveTypeEnum.Unit)
private val FoxBoolType = SurfacePrimitiveType(PrimitiveTypeEnum.Bool)
private val FoxIntType = SurfacePrimitiveType(PrimitiveTypeEnum.Int)
private val FoxFloatType = SurfacePrimitiveType(PrimitiveTypeEnum.Float)
private val FoxDoubleType = SurfacePrimitiveType(PrimitiveTypeEnum.Double)
private val FoxStringType = SurfacePrimitiveType(PrimitiveTypeEnum.String)
