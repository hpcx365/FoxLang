package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ast.*

sealed interface ConstraintCompilePrecheckResult
data object ConstraintCompilePrecheckSuccess : ConstraintCompilePrecheckResult
data class ConstraintCompilePrecheckFailure(val errors: List<ConstraintCompilePrecheckError>) : ConstraintCompilePrecheckResult

sealed interface ConstraintCompilePrecheckError

data class ConstraintCompileProjectionBaseMustBeConcrete(
    val method: FoxMethodDefinition,
    val transform: FoxTransformType,
    val base: FoxType,
) : ConstraintCompilePrecheckError

fun runConstraintCompilePrecheck(file: FoxFile) =
    ConstraintCompilePrecheckContext().check(file.elements.filterIsInstance<FoxMethodDefinition>())

fun runConstraintCompilePrecheck(method: FoxMethodDefinition) =
    ConstraintCompilePrecheckContext().check(listOf(method))

private class ConstraintCompilePrecheckContext {
    
    private val errors = mutableListOf<ConstraintCompilePrecheckError>()
    
    fun check(methods: List<FoxMethodDefinition>): ConstraintCompilePrecheckResult {
        methods.forEach { errors += ConstraintCompilePrecheckCollector(it).run() }
        return if (errors.isEmpty()) ConstraintCompilePrecheckSuccess else ConstraintCompilePrecheckFailure(errors)
    }
}

private class ConstraintCompilePrecheckCollector(
    private val method: FoxMethodDefinition,
) {
    
    private val errors = mutableListOf<ConstraintCompilePrecheckError>()
    
    fun run(): List<ConstraintCompilePrecheckError> {
        method.generics.values.forEach { it.compilesToConcrete() }
        return errors
    }
    
    private fun Iterable<FoxType>.allCompileToConcrete(): Boolean {
        var result = true
        forEach { if (!it.compilesToConcrete()) result = false }
        return result
    }
    
    private fun FoxType.compilesToConcrete(): Boolean = when (this) {
        is FoxPrimitiveType -> true
        is FoxBuiltInType -> when (this) {
            is FoxTupleType -> components.allCompileToConcrete()
            is FoxStructType -> fields.values.allCompileToConcrete()
            is FoxObjectType -> members.values.allCompileToConcrete()
            is FoxEnumType -> entries.values.allCompileToConcrete()
            is FoxArrayType -> element.compilesToConcrete()
            is FoxRefType -> referent.compilesToConcrete()
            is FoxMethodType -> {
                `this`.compilesToConcrete()
                parameters.values.allCompileToConcrete()
                `return`.compilesToConcrete()
            }
        }
        is FoxWildcardType -> {
            when (this) {
                FoxAnyType -> {}
                FoxAnyTupleType -> {}
                FoxAnyStructType -> {}
                FoxAnyObjectType -> {}
                FoxAnyEnumType -> {}
                is FoxAnyOfType -> types.allCompileToConcrete()
                is FoxAllOfType -> types.allCompileToConcrete()
                is FoxNoneOfType -> types.allCompileToConcrete()
                is FoxAnyTupleOfType -> component.compilesToConcrete()
                is FoxAnyStructOfType -> fields.allCompileToConcrete()
            }
            false
        }
        is FoxTransformType -> {
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
        is FoxUnresolvedType -> {
            check(parameters == null)
            check(name in method.generics)
            true
        }
        is FoxPlaceholderType -> error("unreachable")
    }
}

private fun FoxTransformType.projectionBase(): FoxType? = when (this) {
    is FoxTupleGetComponentType -> type
    is FoxTupleGetComponentBackType -> type
    is FoxTupleGetFirstComponentsType -> type
    is FoxTupleGetFirstComponentsExactType -> type
    is FoxTupleGetLastComponentsType -> type
    is FoxTupleGetLastComponentsExactType -> type
    is FoxTupleDropFirstComponentsType -> type
    is FoxTupleDropFirstComponentsExactType -> type
    is FoxTupleDropLastComponentsType -> type
    is FoxTupleDropLastComponentsExactType -> type
    is FoxStructGetFieldTypeByNameType -> type
    is FoxStructGetFieldTypeByIndexType -> type
    is FoxStructGetFieldTypeByIndexBackType -> type
    is FoxStructGetFirstFieldsType -> type
    is FoxStructGetFirstFieldsExactType -> type
    is FoxStructGetLastFieldsType -> type
    is FoxStructGetLastFieldsExactType -> type
    is FoxStructDropFirstFieldsType -> type
    is FoxStructDropFirstFieldsExactType -> type
    is FoxStructDropLastFieldsType -> type
    is FoxStructDropLastFieldsExactType -> type
    is FoxStructSelectFieldsType -> type
    is FoxStructSelectFieldsExactType -> type
    is FoxStructDropFieldsType -> type
    is FoxStructDropFieldsExactType -> type
    is FoxStructExtractFieldTypesType -> type
    is FoxObjectGetMemberTypeType -> type
    is FoxObjectSelectMembersType -> type
    is FoxObjectSelectMembersExactType -> type
    is FoxObjectDropMembersType -> type
    is FoxObjectDropMembersExactType -> type
    is FoxEnumGetEntryTypeType -> type
    is FoxEnumSelectEntriesType -> type
    is FoxEnumSelectEntriesExactType -> type
    is FoxEnumDropEntriesType -> type
    is FoxEnumDropEntriesExactType -> type
    is FoxArrayGetElementTypeType -> type
    is FoxRefGetReferentTypeType -> type
    is FoxMethodGetThisTypeType -> type
    is FoxMethodGetParameterStructType -> type
    is FoxMethodGetReturnTypeType -> type
    is FoxTupleMergeTuplesType,
    is FoxStructMergeStructsType,
    is FoxObjectMergeObjectsType,
    is FoxEnumMergeEnumsType,
    is FoxMethodOfType,
        -> null
}

private fun FoxTransformType.transformChildren(): List<FoxType> = when (this) {
    is FoxTupleGetComponentType -> listOf(type)
    is FoxTupleGetComponentBackType -> listOf(type)
    is FoxTupleGetFirstComponentsType -> listOf(type)
    is FoxTupleGetFirstComponentsExactType -> listOf(type)
    is FoxTupleGetLastComponentsType -> listOf(type)
    is FoxTupleGetLastComponentsExactType -> listOf(type)
    is FoxTupleDropFirstComponentsType -> listOf(type)
    is FoxTupleDropFirstComponentsExactType -> listOf(type)
    is FoxTupleDropLastComponentsType -> listOf(type)
    is FoxTupleDropLastComponentsExactType -> listOf(type)
    is FoxTupleMergeTuplesType -> types
    is FoxStructGetFieldTypeByNameType -> listOf(type)
    is FoxStructGetFieldTypeByIndexType -> listOf(type)
    is FoxStructGetFieldTypeByIndexBackType -> listOf(type)
    is FoxStructGetFirstFieldsType -> listOf(type)
    is FoxStructGetFirstFieldsExactType -> listOf(type)
    is FoxStructGetLastFieldsType -> listOf(type)
    is FoxStructGetLastFieldsExactType -> listOf(type)
    is FoxStructDropFirstFieldsType -> listOf(type)
    is FoxStructDropFirstFieldsExactType -> listOf(type)
    is FoxStructDropLastFieldsType -> listOf(type)
    is FoxStructDropLastFieldsExactType -> listOf(type)
    is FoxStructSelectFieldsType -> listOf(type)
    is FoxStructSelectFieldsExactType -> listOf(type)
    is FoxStructDropFieldsType -> listOf(type)
    is FoxStructDropFieldsExactType -> listOf(type)
    is FoxStructExtractFieldTypesType -> listOf(type)
    is FoxStructMergeStructsType -> types
    is FoxObjectGetMemberTypeType -> listOf(type)
    is FoxObjectSelectMembersType -> listOf(type)
    is FoxObjectSelectMembersExactType -> listOf(type)
    is FoxObjectDropMembersType -> listOf(type)
    is FoxObjectDropMembersExactType -> listOf(type)
    is FoxObjectMergeObjectsType -> types
    is FoxEnumGetEntryTypeType -> listOf(type)
    is FoxEnumSelectEntriesType -> listOf(type)
    is FoxEnumSelectEntriesExactType -> listOf(type)
    is FoxEnumDropEntriesType -> listOf(type)
    is FoxEnumDropEntriesExactType -> listOf(type)
    is FoxEnumMergeEnumsType -> types
    is FoxArrayGetElementTypeType -> listOf(type)
    is FoxRefGetReferentTypeType -> listOf(type)
    is FoxMethodGetThisTypeType -> listOf(type)
    is FoxMethodGetParameterStructType -> listOf(type)
    is FoxMethodGetReturnTypeType -> listOf(type)
    is FoxMethodOfType -> listOf(`this`, parameters, `return`)
}
