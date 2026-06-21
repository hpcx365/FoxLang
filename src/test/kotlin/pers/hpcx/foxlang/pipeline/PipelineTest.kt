package pers.hpcx.foxlang.pipeline

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.emptyOrderedSet
import pers.hpcx.foxlang.utils.orderedMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PipelineTest {
    
    @Test
    fun stopsAtCompileConstraintStage() {
        val file = FoxFile(
            listOf(
                FoxMethodDefinition(
                    generics = orderedMapOf(
                        "T" to FoxTupleComponentAtType(FoxTupleType(listOf(FoxIntType)), 0),
                    ),
                    thisType = FoxUnitType,
                    name = "unsafe",
                    parameters = emptyOrderedMap(),
                    returnType = FoxUnitType,
                    body = FoxBlock(null, emptyList()),
                ),
            ),
        )
        
        val result = assertIs<PipelineCompileConstraintFailure>(runPipeline(file))
        assertEquals(1, result.errors.size)
    }
    
    @Test
    fun stopsAtNormalizationStage() {
        val file = FoxFile(
            listOf(
                FoxMethodDefinition(
                    generics = emptyOrderedMap(),
                    thisType = FoxUnitType,
                    name = "badNormalization",
                    parameters = emptyOrderedMap(),
                    returnType = FoxStructFieldAtType(
                        FoxStructType(orderedMapOf("value" to FoxIntType)),
                        1,
                    ),
                    body = FoxBlock(null, emptyList()),
                ),
            ),
        )
        
        val result = assertIs<PipelineNormalizationFailure>(runPipeline(file))
        val error = result.errors.single()
        
        assertEquals("badNormalization", error.method.name)
        assertIs<TypeNormalizationIndexOutOfBounds>(error.error)
    }
    
    @Test
    fun stopsAtAliasFlattenStage() {
        val file = FoxFile(
            listOf(
                FoxTypeAlias("A", emptyOrderedSet(), FoxUnresolvedType("Missing", null)),
            ),
        )
        
        val result = assertIs<PipelineAliasFlattenFailure>(runPipeline(file))
        assertEquals(1, result.errors.size)
    }
}
