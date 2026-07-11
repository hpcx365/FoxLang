package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.ConcreteTypeFamily
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.orderedMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ConstraintCompilePrecheckPassTest {
    
    @Test
    fun detectsUnknownGenericReference() {
        val method = method(
            orderedMapOf(
                "R" to FoxTupleType(listOf(FoxUnresolvedType("T", null))),
            ),
        )
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileUnknownGenericReference>(result.errors.single())
        
        assertEquals(method, error.method)
        assertEquals(FoxUnresolvedType("T", null), error.type)
    }
    
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
    fun allowsProjectionFromConcreteBase() {
        val method = method(
            orderedMapOf(
                "T" to FoxTupleGetComponentType(FoxTupleType(listOf(FoxIntType)), 0),
            ),
        )
        
        assertIs<ConstraintCompilePrecheckSuccess>(runConstraintCompilePrecheck(method))
    }
    
    @Test
    fun detectsTargetFamilyMismatch() {
        val transform = FoxStructMergeStructsType(
            listOf(
                FoxAnyTupleType,
                FoxStructType(orderedMapOf("name" to FoxStringType)),
            ),
        )
        val method = method(orderedMapOf("R" to transform))
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileTargetFamilyMismatch>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(FoxAnyTupleType, error.target)
        assertEquals(ConcreteTypeFamily.STRUCT, error.expectedFamily)
        assertEquals(ConcreteTypeFamily.TUPLE, error.actualFamily)
    }
    
    @Test
    fun detectsAnyAsTargetFamilyMismatch() {
        val transform = FoxStructMergeStructsType(
            listOf(
                FoxAnyType,
                FoxStructType(orderedMapOf("name" to FoxStringType)),
            ),
        )
        val method = method(orderedMapOf("R" to transform))
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileTargetFamilyMismatch>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(FoxAnyType, error.target)
        assertEquals(ConcreteTypeFamily.STRUCT, error.expectedFamily)
        assertEquals(null, error.actualFamily)
    }
    
    @Test
    fun detectsUnknownGenericFamilyInTargetPosition() {
        val target = FoxUnresolvedType("T", null)
        val transform = FoxStructMergeStructsType(
            listOf(
                target,
                FoxStructType(orderedMapOf("name" to FoxStringType)),
            ),
        )
        val method = method(
            orderedMapOf(
                "T" to FoxAnyType,
                "R" to transform,
            ),
        )
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileTargetFamilyMismatch>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(target, error.target)
        assertEquals(ConcreteTypeFamily.STRUCT, error.expectedFamily)
        assertEquals(null, error.actualFamily)
    }
    
    @Test
    fun detectsConcreteTargetFamilyMismatch() {
        val target = FoxTupleType(listOf(FoxIntType))
        val transform = FoxStructMergeStructsType(
            listOf(
                target,
                FoxStructType(orderedMapOf("name" to FoxStringType)),
            ),
        )
        val method = method(orderedMapOf("R" to transform))
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileTargetFamilyMismatch>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(target, error.target)
        assertEquals(ConcreteTypeFamily.STRUCT, error.expectedFamily)
        assertEquals(ConcreteTypeFamily.TUPLE, error.actualFamily)
    }
    
    @Test
    fun allowsUnionAcrossFamiliesAsUnknownFamily() {
        val method = method(
            orderedMapOf(
                "T" to FoxAnyOfType(
                    listOf(
                        FoxTupleType(listOf(FoxIntType)),
                        FoxStructType(orderedMapOf("name" to FoxStringType)),
                    ),
                ),
            ),
        )
        
        assertIs<ConstraintCompilePrecheckSuccess>(runConstraintCompilePrecheck(method))
    }
    
    @Test
    fun detectsGenericFamilyConflict() {
        val method = method(
            orderedMapOf(
                "T" to FoxAllOfType(listOf(FoxAnyTupleType, FoxAnyStructType)),
            ),
        )
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileGenericFamilyConflict>(result.errors.single())
        
        assertEquals("T", error.generic)
        assertEquals(setOf(ConcreteTypeFamily.TUPLE, ConcreteTypeFamily.STRUCT), error.families)
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
    fun detectsTargetFamilyMismatchThroughGenericFamily() {
        val transform = FoxStructMergeStructsType(
            listOf(
                FoxUnresolvedType("T", null),
                FoxStructType(orderedMapOf("name" to FoxStringType)),
            ),
        )
        val method = method(
            orderedMapOf(
                "T" to FoxAnyTupleType,
                "R" to transform,
            ),
        )
        
        val result = assertIs<ConstraintCompilePrecheckFailure>(runConstraintCompilePrecheck(method))
        val error = assertIs<ConstraintCompileTargetFamilyMismatch>(result.errors.single())
        
        assertEquals(transform, error.transform)
        assertEquals(FoxUnresolvedType("T", null), error.target)
        assertEquals(ConcreteTypeFamily.STRUCT, error.expectedFamily)
        assertEquals(ConcreteTypeFamily.TUPLE, error.actualFamily)
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
