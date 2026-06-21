package pers.hpcx.foxlang.pipeline

import pers.hpcx.foxlang.ast.FoxFile

sealed interface PipelineResult
data class PipelineSuccess(val newFile: FoxFile) : PipelineResult
data class PipelineAliasFlattenFailure(val errors: List<TypeAliasFlattenError>) : PipelineResult
data class PipelineAliasEliminationFailure(val errors: List<TypeAliasEliminationError>) : PipelineResult
data class PipelineNormalizationFailure(val errors: List<MethodTypeNormalizationError>) : PipelineResult
data class PipelineCompileConstraintFailure(val errors: List<TypeCompileConstraintError>) : PipelineResult

fun runPipeline(file: FoxFile): PipelineResult {
    val flattened = when (val result = runTypeAliasFlatten(file)) {
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
    
    when (val result = runTypeCompileConstraintCheck(normalized)) {
        TypeCompileConstraintSuccess -> {}
        is TypeCompileConstraintFailure -> return PipelineCompileConstraintFailure(result.errors)
    }
    
    TODO()
}
