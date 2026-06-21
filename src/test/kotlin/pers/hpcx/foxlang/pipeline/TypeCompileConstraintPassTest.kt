package pers.hpcx.foxlang.pipeline

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.orderedMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TypeCompileConstraintPassTest {
    @Test
    fun acceptsSafeGenericConstraints() {
        val method = FoxMethodDefinition(
            generics = orderedMapOf(
                "T" to FoxAnyOfType(
                    listOf(
                        FoxArrayType(FoxUnresolvedType("U", null)),
                        FoxTupleComponentAtType(FoxUnresolvedType("V", null), 0),
                    ),
                ),
                "U" to FoxAnyType,
                "V" to FoxAnyTupleType,
            ),
            thisType = FoxUnitType,
            name = "safe",
            parameters = emptyOrderedMap(),
            returnType = FoxUnitType,
            body = FoxBlock(null, emptyList()),
        )
        
        val result = runTypeCompileConstraintCheck(method)
        
        assertEquals(TypeCompileConstraintSuccess, result)
    }
    
    @Test
    fun rejectsProjectionBaseThatIsNotGeneric() {
        val method = FoxMethodDefinition(
            generics = orderedMapOf(
                "T" to FoxTupleComponentAtType(FoxTupleType(listOf(FoxIntType)), 0),
            ),
            thisType = FoxUnitType,
            name = "unsafeProjection",
            parameters = emptyOrderedMap(),
            returnType = FoxUnitType,
            body = FoxBlock(null, emptyList()),
        )
        
        val result = assertIs<TypeCompileConstraintFailure>(runTypeCompileConstraintCheck(method))
        val error = assertIs<TypeCompileConstraintProjectionBaseMustBeGeneric>(result.errors.single())
        
        assertEquals("T", error.generic)
        assertEquals(FoxTupleType(listOf(FoxIntType)), error.actualBase)
    }
    
    @Test
    fun aggregatesErrorsAcrossMethodsInFile() {
        val safeMethod = FoxMethodDefinition(
            generics = orderedMapOf("T" to FoxAnyType),
            thisType = FoxUnitType,
            name = "safe",
            parameters = emptyOrderedMap(),
            returnType = FoxUnitType,
            body = FoxBlock(null, emptyList()),
        )
        val unsafeMethod = FoxMethodDefinition(
            generics = orderedMapOf(
                "T" to FoxMethodParametersOfType(
                    FoxMethodType(
                        FoxUnitType,
                        orderedMapOf("value" to FoxIntType),
                        FoxUnitType,
                    ),
                ),
            ),
            thisType = FoxUnitType,
            name = "unsafe",
            parameters = emptyOrderedMap(),
            returnType = FoxUnitType,
            body = FoxBlock(null, emptyList()),
        )
        
        val result = assertIs<TypeCompileConstraintFailure>(
            runTypeCompileConstraintCheck(FoxFile(listOf(safeMethod, unsafeMethod))),
        )
        val error = assertIs<TypeCompileConstraintProjectionBaseMustBeGeneric>(result.errors.single())
        
        assertEquals(unsafeMethod, error.method)
        assertEquals("T", error.generic)
    }
}
