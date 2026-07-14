package pers.hpcx.foxlang.pipeline

import pers.hpcx.foxlang.ir.CoreProgram
import pers.hpcx.foxlang.ir.SyntaxFile
import pers.hpcx.foxlang.pipeline.pass.*

sealed interface PipelineResult
data class PipelineSuccess(val program: CoreProgram) : PipelineResult

sealed interface PipelineFailure : PipelineResult
data class PipelineNumericLiteralCheckFailure(val errors: List<NumericLiteralCheckError>) : PipelineFailure
data class PipelineDuplicateItemCheckFailure(val errors: List<DuplicateItemCheckError>) : PipelineFailure
data class PipelineStatementStructureCheckFailure(val errors: List<StatementStructureCheckError>) : PipelineFailure
data class PipelineTypeAliasFlattenFailure(val errors: List<TypeAliasFlattenError>) : PipelineFailure
data class PipelineTypeAliasEliminationFailure(val errors: List<TypeAliasEliminationError>) : PipelineFailure
data class PipelineMethodTypeNormalizationFailure(val errors: List<MethodTypeNormalizationError>) : PipelineFailure
data class PipelineConstraintCompilePrecheckFailure(val errors: List<ConstraintCompilePrecheckError>) : PipelineFailure

fun runPipeline(file: SyntaxFile): PipelineResult {
    when (val result = runNumericLiteralCheck(file)) {
        NumericLiteralCheckSuccess -> {}
        is NumericLiteralCheckFailure -> return PipelineNumericLiteralCheckFailure(result.errors)
    }
    
    when (val result = runDuplicateItemCheck(file)) {
        DuplicateItemCheckSuccess -> {}
        is DuplicateItemCheckFailure -> return PipelineDuplicateItemCheckFailure(result.errors)
    }
    
    when (val result = runStatementStructureCheck(file)) {
        StatementStructureCheckSuccess -> {}
        is StatementStructureCheckFailure -> return PipelineStatementStructureCheckFailure(result.errors)
    }
    
    val lowered = file.node
    
    val flattened = when (val result = runTypeAliasFlatten(lowered)) {
        is TypeAliasFlattenSuccess -> result.newFile
        is TypeAliasFlattenFailure -> return PipelineTypeAliasFlattenFailure(result.errors)
    }
    
    val eliminated = when (val result = runTypeAliasElimination(flattened)) {
        is TypeAliasEliminationSuccess -> result.newFile
        is TypeAliasEliminationFailure -> return PipelineTypeAliasEliminationFailure(result.errors)
    }
    
    val normalized = when (val result = runMethodTypeNormalization(eliminated)) {
        is MethodTypeNormalizationSuccess -> result.newFile
        is MethodTypeNormalizationFailure -> return PipelineMethodTypeNormalizationFailure(result.errors)
    }
    
    when (val result = runConstraintCompilePrecheck(normalized)) {
        ConstraintCompilePrecheckSuccess -> {}
        is ConstraintCompilePrecheckFailure -> return PipelineConstraintCompilePrecheckFailure(result.errors)
    }
    
    val core = when (val result = runCoreLowering(normalized)) {
        is CoreLoweringSuccess -> result.program
    }
    
    return PipelineSuccess(core)
}
