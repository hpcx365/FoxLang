package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.orderedMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ConstraintCompilePrecheckPassTest {
    
    @Test
    fun allowsCyclicGenericConstraints() {
        val method = method(
            orderedMapOf(
                "T" to SurfaceAnyTupleOfType(SurfaceUnresolvedType("R", null)),
                "R" to SurfaceTupleGetComponentType(SurfaceUnresolvedType("T", null), 2),
            ),
        )
        
        assertIs<ConstraintCompilePrecheckSuccess>(runConstraintCompilePrecheck(method))
    }
    
    @Test
    fun allowsProjectionFromGenericReferenceBase() {
        val method = method(
            orderedMapOf(
                "T" to SurfaceAnyTupleType(),
                "R" to SurfaceTupleGetComponentType(SurfaceUnresolvedType("T", null), 2),
            ),
        )
        
        assertIs<ConstraintCompilePrecheckSuccess>(runConstraintCompilePrecheck(method))
    }
    
    @Test
    fun allowsProjectionFromConcreteBase() {
        val method = method(
            orderedMapOf(
                "T" to SurfaceTupleGetComponentType(SurfaceTupleType(listOf(SurfacePrimitiveType(PrimitiveTypeEnum.Int))), 0),
            ),
        )
        
        assertIs<ConstraintCompilePrecheckSuccess>(runConstraintCompilePrecheck(method))
    }
    
    @Test
    fun detectsProjectionBaseTypeSpace() {
        val transform = SurfaceTupleGetComponentType(SurfaceAnyTupleType(), 2)
        val method = method(orderedMapOf("R" to transform))
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileProjectionBaseMustBeConcrete>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(SurfaceAnyTupleType(), error.base)
    }
    
    @Test
    fun detectsProjectionBasePatternSpace() {
        val base = SurfaceTupleType(listOf(SurfaceAnyType()))
        val transform = SurfaceTupleGetComponentType(base, 0)
        val method = method(orderedMapOf("R" to transform))
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileProjectionBaseMustBeConcrete>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(base, error.base)
    }
    
    @Test
    fun detectsAnyOfProjectionBaseAsTypeSpace() {
        val base = SurfaceAnyOfType(
            listOf(
                SurfaceTupleType(
                    listOf(
                        SurfacePrimitiveType(PrimitiveTypeEnum.Int),
                        SurfacePrimitiveType(PrimitiveTypeEnum.Float),
                        SurfacePrimitiveType(PrimitiveTypeEnum.String),
                    ),
                ),
            ),
        )
        val transform = SurfaceTupleGetComponentType(base, 2)
        val method = method(orderedMapOf("R" to transform))
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileProjectionBaseMustBeConcrete>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(base, error.base)
    }
    
    @Test
    fun detectsStructProjectionBaseTypeSpace() {
        val transform = SurfaceStructGetFieldTypeByNameType(SurfaceAnyStructType(), "value")
        val method = method(orderedMapOf("R" to transform))
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileProjectionBaseMustBeConcrete>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(SurfaceAnyStructType(), error.base)
    }
    
    private fun method(generics: OrderedMap<String, SurfaceType>) = SurfaceMethodDefinition(
        generics = generics,
        thisType = SurfacePrimitiveType(PrimitiveTypeEnum.Unit),
        name = "test",
        parameters = emptyOrderedMap(),
        returnType = SurfacePrimitiveType(PrimitiveTypeEnum.Unit),
        body = SurfaceBlock(null, emptyList()),
    )
}
