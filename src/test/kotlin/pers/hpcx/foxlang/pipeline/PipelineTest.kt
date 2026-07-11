package pers.hpcx.foxlang.pipeline

import pers.hpcx.foxlang.ast.ParsedFoxFile
import pers.hpcx.foxlang.frontend.fox.SourceFragmentationSuccess
import pers.hpcx.foxlang.frontend.fox.parseFox
import pers.hpcx.foxlang.frontend.fox.sourceFox
import pers.hpcx.foxlang.pipeline.pass.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PipelineTest {
    
    @Test
    fun stopsAtCompileConstraintStage() {
        val file = """
            def <T = GetComponent<AnyTuple, 0>> unsafe() {}
        """.trimIndent().parseFoxFileOrThrow()
        
        val result = assertIs<PipelineCompileConstraintFailure>(runPipeline(file))
        assertIs<ConstraintCompileProjectionBaseMustBeConcrete>(result.errors.single())
    }
    
    @Test
    fun stopsAtNormalizationStage() {
        val file = """
            def badNormalization(): GetFieldTypeByIndex<Struct<value: Int>, 1> {}
        """.trimIndent().parseFoxFileOrThrow()
        
        val result = assertIs<PipelineNormalizationFailure>(runPipeline(file))
        val error = result.errors.single()
        
        assertEquals("badNormalization", error.method.name)
        assertIs<TypeNormalizationIndexOutOfBounds>(error.error)
    }
    
    @Test
    fun stopsAtAliasFlattenStage() {
        val file = """
            type A = Missing
        """.trimIndent().parseFoxFileOrThrow()
        
        val result = assertIs<PipelineAliasFlattenFailure>(runPipeline(file))
        assertEquals(1, result.errors.size)
    }
    
    @Test
    fun stopsAtNumericLiteralStage() {
        val file = """
            def badLiteral() {
                value := 2147483648
            }
        """.trimIndent().parseFoxFileOrThrow()
        
        val result = assertIs<PipelineNumericLiteralCheckFailure>(runPipeline(file))
        assertIs<NumericIntLiteralOutOfRange>(result.errors.single())
    }
    
    @Test
    fun stopsAtDuplicateItemStage() {
        val file = """
            def duplicateParameter(value: Int, value: String) {}
        """.trimIndent().parseFoxFileOrThrow()
        
        val result = assertIs<PipelineDuplicateItemCheckFailure>(runPipeline(file))
        assertIs<DuplicateMethodParameter>(result.errors.single())
    }
    
    @Test
    fun stopsAtStatementStructureStage() {
        val file = """
            def badAssignment() {
                call() = 1
            }
        """.trimIndent().parseFoxFileOrThrow()
        
        val result = assertIs<PipelineStatementStructureCheckFailure>(runPipeline(file))
        assertIs<StatementAssignmentTargetMustBeAssignable>(result.errors.single())
    }
}

private fun String.parseFoxFileOrThrow(): ParsedFoxFile {
    val source = assertIs<SourceFragmentationSuccess>((if (endsWith("\n")) this else "$this\n").sourceFox()).value
    return source.parseFox().value()
}
