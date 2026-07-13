package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ast.*
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
                "T" to FoxAnyTupleOfType(FoxUnresolvedType("R", null)),
                "R" to FoxTupleGetComponentType(FoxUnresolvedType("T", null), 2),
            ),
        )
        
        assertIs<ConstraintCompilePrecheckSuccess>(runConstraintCompilePrecheck(method))
    }
    
    @Test
    fun allowsProjectionFromGenericReferenceBase() {
        val method = method(
            orderedMapOf(
                "T" to FoxAnyTupleType,
                "R" to FoxTupleGetComponentType(FoxUnresolvedType("T", null), 2),
            ),
        )
        
        assertIs<ConstraintCompilePrecheckSuccess>(runConstraintCompilePrecheck(method))
    }
    
    @Test
    fun allowsProjectionFromConcreteBase() {
        val method = method(
            orderedMapOf(
                "T" to FoxTupleGetComponentType(FoxTupleType(listOf(FoxIntType)), 0),
            ),
        )
        
        assertIs<ConstraintCompilePrecheckSuccess>(runConstraintCompilePrecheck(method))
    }
    
    @Test
    fun detectsProjectionBaseTypeSpace() {
        val transform = FoxTupleGetComponentType(FoxAnyTupleType, 2)
        val method = method(orderedMapOf("R" to transform))
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileProjectionBaseMustBeConcrete>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(FoxAnyTupleType, error.base)
    }
    
    @Test
    fun detectsProjectionBasePatternSpace() {
        val base = FoxTupleType(listOf(FoxAnyType))
        val transform = FoxTupleGetComponentType(base, 0)
        val method = method(orderedMapOf("R" to transform))
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileProjectionBaseMustBeConcrete>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(base, error.base)
    }
    
    @Test
    fun detectsAnyOfProjectionBaseAsTypeSpace() {
        val base = FoxAnyOfType(
            listOf(
                FoxTupleType(listOf(FoxIntType, FoxFloatType, FoxStringType)),
            ),
        )
        val transform = FoxTupleGetComponentType(base, 2)
        val method = method(orderedMapOf("R" to transform))
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileProjectionBaseMustBeConcrete>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(base, error.base)
    }
    
    @Test
    fun detectsStructProjectionBaseTypeSpace() {
        val transform = FoxStructGetFieldTypeByNameType(FoxAnyStructType, "value")
        val method = method(orderedMapOf("R" to transform))
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileProjectionBaseMustBeConcrete>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(FoxAnyStructType, error.base)
    }
    
    private fun method(generics: OrderedMap<String, FoxType>) = FoxMethodDefinition(
        generics = generics,
        thisType = FoxUnitType,
        name = "test",
        parameters = emptyOrderedMap(),
        returnType = FoxUnitType,
        body = FoxBlock(null, emptyList()),
    )
}
