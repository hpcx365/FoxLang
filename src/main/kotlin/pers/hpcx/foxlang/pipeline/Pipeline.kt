package pers.hpcx.foxlang.pipeline

import pers.hpcx.foxlang.ast.FoxFile
import pers.hpcx.foxlang.ast.ParsedFoxFile
import pers.hpcx.foxlang.pipeline.pass.*

sealed interface PipelineResult
data class PipelineSuccess(val newFile: FoxFile) : PipelineResult
data class PipelineNumericLiteralCheckFailure(val errors: List<NumericLiteralCheckError>) : PipelineResult
data class PipelineDuplicateItemCheckFailure(val errors: List<DuplicateItemCheckError>) : PipelineResult
data class PipelineStatementStructureCheckFailure(val errors: List<StatementStructureCheckError>) : PipelineResult
data class PipelineAliasFlattenFailure(val errors: List<TypeAliasFlattenError>) : PipelineResult
data class PipelineAliasEliminationFailure(val errors: List<TypeAliasEliminationError>) : PipelineResult
data class PipelineNormalizationFailure(val errors: List<MethodTypeNormalizationError>) : PipelineResult
data class PipelineCompileConstraintFailure(val errors: List<ConstraintCompilePrecheckError>) : PipelineResult

fun runPipeline(file: ParsedFoxFile): PipelineResult {
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
        is TypeAliasFlattenFailure -> return PipelineAliasFlattenFailure(result.errors)
    }
    
    val eliminated = when (val result = runTypeAliasElimination(flattened)) {
        is TypeAliasEliminationSuccess -> result.newFile
        is TypeAliasEliminationFailure -> return PipelineAliasEliminationFailure(result.errors)
    }
    
    val normalized = when (val result = runMethodTypeNormalization(eliminated)) {
        is MethodTypeNormalizationSuccess -> result.newFile
        is MethodTypeNormalizationFailure -> return PipelineNormalizationFailure(result.errors)
    }
    
    when (val result = runConstraintCompilePrecheck(normalized)) {
        ConstraintCompilePrecheckSuccess -> {}
        is ConstraintCompilePrecheckFailure -> return PipelineCompileConstraintFailure(result.errors)
    }
    
    TODO()
}
