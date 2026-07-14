package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*

sealed interface ConstraintCompilePrecheckResult
data object ConstraintCompilePrecheckSuccess : ConstraintCompilePrecheckResult
data class ConstraintCompilePrecheckFailure(val errors: List<ConstraintCompilePrecheckError>) : ConstraintCompilePrecheckResult

sealed interface ConstraintCompilePrecheckError

data class ConstraintCompileProjectionBaseMustBeConcrete(
    val method: SurfaceMethodDefinition,
    val transform: SurfaceTransformType,
    val base: SurfaceType,
) : ConstraintCompilePrecheckError

fun runConstraintCompilePrecheck(file: SurfaceFile) =
    ConstraintCompilePrecheckContext().check(file.elements.filterIsInstance<SurfaceMethodDefinition>())

fun runConstraintCompilePrecheck(method: SurfaceMethodDefinition) =
    ConstraintCompilePrecheckContext().check(listOf(method))

private class ConstraintCompilePrecheckContext {
    
    private val errors = mutableListOf<ConstraintCompilePrecheckError>()
    
    fun check(methods: List<SurfaceMethodDefinition>): ConstraintCompilePrecheckResult {
        methods.forEach { errors += ConstraintCompilePrecheckCollector(it).run() }
        return if (errors.isEmpty()) ConstraintCompilePrecheckSuccess else ConstraintCompilePrecheckFailure(errors)
    }
}

private class ConstraintCompilePrecheckCollector(
    private val method: SurfaceMethodDefinition,
) {
    
    private val errors = mutableListOf<ConstraintCompilePrecheckError>()
    
    fun run(): List<ConstraintCompilePrecheckError> {
        method.generics.values.forEach { it.compilesToConcrete() }
        return errors
    }
    
    private fun Iterable<SurfaceType>.allCompileToConcrete(): Boolean {
        var result = true
        forEach { if (!it.compilesToConcrete()) result = false }
        return result
    }
    
    private fun SurfaceType.compilesToConcrete(): Boolean = when (this) {
        is SurfacePrimitiveType -> true
        is SurfaceBuiltInType -> when (this) {
            is SurfaceTupleType -> components.allCompileToConcrete()
            is SurfaceStructType -> fields.values.allCompileToConcrete()
            is SurfaceObjectType -> members.values.allCompileToConcrete()
            is SurfaceEnumType -> entries.values.allCompileToConcrete()
            is SurfaceArrayType -> element.compilesToConcrete()
            is SurfaceRefType -> referent.compilesToConcrete()
            is SurfaceMethodType -> {
                `this`.compilesToConcrete()
                parameters.values.allCompileToConcrete()
                `return`.compilesToConcrete()
            }
        }
        is SurfaceWildcardType -> {
            when (this) {
                is SurfaceAnyType -> {}
                is SurfaceAnyTupleType -> {}
                is SurfaceAnyStructType -> {}
                is SurfaceAnyObjectType -> {}
                is SurfaceAnyEnumType -> {}
                is SurfaceAnyOfType -> types.allCompileToConcrete()
                is SurfaceAllOfType -> types.allCompileToConcrete()
                is SurfaceNoneOfType -> types.allCompileToConcrete()
                is SurfaceAnyTupleOfType -> component.compilesToConcrete()
                is SurfaceAnyStructOfType -> fields.allCompileToConcrete()
            }
            false
        }
        is SurfaceTransformType -> {
            val projectionBase = projectionBase()
            if (projectionBase != null) {
                val baseCompilesToConcrete = projectionBase.compilesToConcrete()
                if (!baseCompilesToConcrete) {
                    errors += ConstraintCompileProjectionBaseMustBeConcrete(method, this, projectionBase)
                }
                baseCompilesToConcrete
            } else {
                transformChildren().allCompileToConcrete()
            }
        }
        is SurfaceUnresolvedType -> {
            check(parameters == null)
            check(name in method.generics)
            true
        }
        is SurfacePlaceholderType -> error("unreachable")
    }
}

private fun SurfaceTransformType.projectionBase(): SurfaceType? = when (this) {
    is SurfaceTupleGetComponentType -> type
    is SurfaceTupleGetComponentBackType -> type
    is SurfaceTupleGetFirstComponentsType -> type
    is SurfaceTupleGetFirstComponentsExactType -> type
    is SurfaceTupleGetLastComponentsType -> type
    is SurfaceTupleGetLastComponentsExactType -> type
    is SurfaceTupleDropFirstComponentsType -> type
    is SurfaceTupleDropFirstComponentsExactType -> type
    is SurfaceTupleDropLastComponentsType -> type
    is SurfaceTupleDropLastComponentsExactType -> type
    is SurfaceStructGetFieldTypeByNameType -> type
    is SurfaceStructGetFieldTypeByIndexType -> type
    is SurfaceStructGetFieldTypeByIndexBackType -> type
    is SurfaceStructGetFirstFieldsType -> type
    is SurfaceStructGetFirstFieldsExactType -> type
    is SurfaceStructGetLastFieldsType -> type
    is SurfaceStructGetLastFieldsExactType -> type
    is SurfaceStructDropFirstFieldsType -> type
    is SurfaceStructDropFirstFieldsExactType -> type
    is SurfaceStructDropLastFieldsType -> type
    is SurfaceStructDropLastFieldsExactType -> type
    is SurfaceStructSelectFieldsType -> type
    is SurfaceStructSelectFieldsExactType -> type
    is SurfaceStructDropFieldsType -> type
    is SurfaceStructDropFieldsExactType -> type
    is SurfaceStructExtractFieldTypesType -> type
    is SurfaceObjectGetMemberTypeType -> type
    is SurfaceObjectSelectMembersType -> type
    is SurfaceObjectSelectMembersExactType -> type
    is SurfaceObjectDropMembersType -> type
    is SurfaceObjectDropMembersExactType -> type
    is SurfaceEnumGetEntryTypeType -> type
    is SurfaceEnumSelectEntriesType -> type
    is SurfaceEnumSelectEntriesExactType -> type
    is SurfaceEnumDropEntriesType -> type
    is SurfaceEnumDropEntriesExactType -> type
    is SurfaceArrayGetElementTypeType -> type
    is SurfaceRefGetReferentTypeType -> type
    is SurfaceMethodGetThisTypeType -> type
    is SurfaceMethodGetParameterStructType -> type
    is SurfaceMethodGetReturnTypeType -> type
    is SurfaceTupleMergeTuplesType,
    is SurfaceStructMergeStructsType,
    is SurfaceObjectMergeObjectsType,
    is SurfaceEnumMergeEnumsType,
    is SurfaceMethodOfType,
        -> null
}

private fun SurfaceTransformType.transformChildren(): List<SurfaceType> = when (this) {
    is SurfaceTupleGetComponentType -> listOf(type)
    is SurfaceTupleGetComponentBackType -> listOf(type)
    is SurfaceTupleGetFirstComponentsType -> listOf(type)
    is SurfaceTupleGetFirstComponentsExactType -> listOf(type)
    is SurfaceTupleGetLastComponentsType -> listOf(type)
    is SurfaceTupleGetLastComponentsExactType -> listOf(type)
    is SurfaceTupleDropFirstComponentsType -> listOf(type)
    is SurfaceTupleDropFirstComponentsExactType -> listOf(type)
    is SurfaceTupleDropLastComponentsType -> listOf(type)
    is SurfaceTupleDropLastComponentsExactType -> listOf(type)
    is SurfaceTupleMergeTuplesType -> types
    is SurfaceStructGetFieldTypeByNameType -> listOf(type)
    is SurfaceStructGetFieldTypeByIndexType -> listOf(type)
    is SurfaceStructGetFieldTypeByIndexBackType -> listOf(type)
    is SurfaceStructGetFirstFieldsType -> listOf(type)
    is SurfaceStructGetFirstFieldsExactType -> listOf(type)
    is SurfaceStructGetLastFieldsType -> listOf(type)
    is SurfaceStructGetLastFieldsExactType -> listOf(type)
    is SurfaceStructDropFirstFieldsType -> listOf(type)
    is SurfaceStructDropFirstFieldsExactType -> listOf(type)
    is SurfaceStructDropLastFieldsType -> listOf(type)
    is SurfaceStructDropLastFieldsExactType -> listOf(type)
    is SurfaceStructSelectFieldsType -> listOf(type)
    is SurfaceStructSelectFieldsExactType -> listOf(type)
    is SurfaceStructDropFieldsType -> listOf(type)
    is SurfaceStructDropFieldsExactType -> listOf(type)
    is SurfaceStructExtractFieldTypesType -> listOf(type)
    is SurfaceStructMergeStructsType -> types
    is SurfaceObjectGetMemberTypeType -> listOf(type)
    is SurfaceObjectSelectMembersType -> listOf(type)
    is SurfaceObjectSelectMembersExactType -> listOf(type)
    is SurfaceObjectDropMembersType -> listOf(type)
    is SurfaceObjectDropMembersExactType -> listOf(type)
    is SurfaceObjectMergeObjectsType -> types
    is SurfaceEnumGetEntryTypeType -> listOf(type)
    is SurfaceEnumSelectEntriesType -> listOf(type)
    is SurfaceEnumSelectEntriesExactType -> listOf(type)
    is SurfaceEnumDropEntriesType -> listOf(type)
    is SurfaceEnumDropEntriesExactType -> listOf(type)
    is SurfaceEnumMergeEnumsType -> types
    is SurfaceArrayGetElementTypeType -> listOf(type)
    is SurfaceRefGetReferentTypeType -> listOf(type)
    is SurfaceMethodGetThisTypeType -> listOf(type)
    is SurfaceMethodGetParameterStructType -> listOf(type)
    is SurfaceMethodGetReturnTypeType -> listOf(type)
    is SurfaceMethodOfType -> listOf(`this`, parameters, `return`)
}
