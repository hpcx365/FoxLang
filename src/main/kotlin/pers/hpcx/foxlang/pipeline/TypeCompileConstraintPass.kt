package pers.hpcx.foxlang.pipeline

import pers.hpcx.foxlang.ast.*

sealed interface TypeCompileConstraintResult
data object TypeCompileConstraintSuccess : TypeCompileConstraintResult
data class TypeCompileConstraintFailure(val errors: List<TypeCompileConstraintError>) : TypeCompileConstraintResult

sealed interface TypeCompileConstraintError
data class TypeCompileConstraintProjectionBaseMustBeGeneric(
    val method: FoxMethodDefinition,
    val generic: String,
    val transform: FoxTransformType,
    val actualBase: FoxType,
) : TypeCompileConstraintError

fun runTypeCompileConstraintCheck(file: FoxFile): TypeCompileConstraintResult {
    val errors = file.elements
        .filterIsInstance<FoxMethodDefinition>()
        .flatMap(::collectTypeCompileConstraintErrors)
    return if (errors.isEmpty()) TypeCompileConstraintSuccess else TypeCompileConstraintFailure(errors)
}

fun runTypeCompileConstraintCheck(method: FoxMethodDefinition): TypeCompileConstraintResult {
    val errors = collectTypeCompileConstraintErrors(method)
    return if (errors.isEmpty()) TypeCompileConstraintSuccess else TypeCompileConstraintFailure(errors)
}

private fun collectTypeCompileConstraintErrors(method: FoxMethodDefinition): List<TypeCompileConstraintError> {
    val errors = mutableListOf<TypeCompileConstraintError>()
    
    method.generics.forEach { (genericName, constraint) ->
        
        fun requireBareGeneric(transform: FoxTransformType, base: FoxType) {
            if (base is FoxUnresolvedType && base.parameters == null) return
            errors += TypeCompileConstraintProjectionBaseMustBeGeneric(method, genericName, transform, base)
        }
        
        fun validateType(type: FoxType): Unit = when (type) {
            is FoxPrimitiveType -> {}
            is FoxPlaceholderType -> error("Placeholder type cannot appear in method generic constraints")
            is FoxUnresolvedType -> {}
            is FoxWildcardType -> when (type) {
                FoxAnyType -> {}
                FoxAnyTupleType -> {}
                FoxAnyStructType -> {}
                FoxAnyObjectType -> {}
                FoxAnyEnumType -> {}
                is FoxAnyOfType -> type.types.forEach { validateType(it) }
                is FoxAllOfType -> type.types.forEach { validateType(it) }
                is FoxNoneOfType -> type.types.forEach { validateType(it) }
                is FoxAnyTupleOfType -> validateType(type.component)
                is FoxAnyStructOfType -> type.fields.forEach { validateType(it) }
            }
            is FoxBuiltInType -> when (type) {
                is FoxTupleType -> type.components.forEach { validateType(it) }
                is FoxStructType -> type.fields.values.forEach { validateType(it) }
                is FoxObjectType -> type.members.values.forEach { validateType(it) }
                is FoxEnumType -> type.items.values.forEach { validateType(it) }
                is FoxArrayType -> validateType(type.element)
                is FoxRefType -> validateType(type.referent)
                is FoxMethodType -> {
                    validateType(type.`this`)
                    type.parameters.values.forEach { validateType(it) }
                    validateType(type.`return`)
                }
            }
            is FoxTransformType -> when (type) {
                is FoxTupleComponentAtType -> requireBareGeneric(type, type.type)
                is FoxTupleLastComponentAtType -> requireBareGeneric(type, type.type)
                is FoxTupleFirstComponentsOfType -> requireBareGeneric(type, type.type)
                is FoxTupleExactFirstComponentsOfType -> requireBareGeneric(type, type.type)
                is FoxTupleLastComponentsOfType -> requireBareGeneric(type, type.type)
                is FoxTupleExactLastComponentsOfType -> requireBareGeneric(type, type.type)
                is FoxTupleDropFirstComponentsOfType -> requireBareGeneric(type, type.type)
                is FoxTupleExactDropFirstComponentsOfType -> requireBareGeneric(type, type.type)
                is FoxTupleDropLastComponentsOfType -> requireBareGeneric(type, type.type)
                is FoxTupleExactDropLastComponentsOfType -> requireBareGeneric(type, type.type)
                is FoxTupleMergeComponentsOfType -> type.types.forEach { validateType(it) }
                is FoxStructFieldOfType -> requireBareGeneric(type, type.type)
                is FoxStructFieldAtType -> requireBareGeneric(type, type.type)
                is FoxStructLastFieldAtType -> requireBareGeneric(type, type.type)
                is FoxStructFirstFieldsOfType -> requireBareGeneric(type, type.type)
                is FoxStructExactFirstFieldsOfType -> requireBareGeneric(type, type.type)
                is FoxStructLastFieldsOfType -> requireBareGeneric(type, type.type)
                is FoxStructExactLastFieldsOfType -> requireBareGeneric(type, type.type)
                is FoxStructDropFirstFieldsOfType -> requireBareGeneric(type, type.type)
                is FoxStructExactDropFirstFieldsOfType -> requireBareGeneric(type, type.type)
                is FoxStructDropLastFieldsOfType -> requireBareGeneric(type, type.type)
                is FoxStructExactDropLastFieldsOfType -> requireBareGeneric(type, type.type)
                is FoxStructFieldsOfType -> requireBareGeneric(type, type.type)
                is FoxStructDropFieldsOfType -> requireBareGeneric(type, type.type)
                is FoxStructMergeFieldsOfType -> type.types.forEach { validateType(it) }
                is FoxObjectMemberOfType -> requireBareGeneric(type, type.type)
                is FoxObjectMembersOfType -> requireBareGeneric(type, type.type)
                is FoxObjectDropMembersOfType -> requireBareGeneric(type, type.type)
                is FoxObjectMergeMembersOfType -> type.types.forEach { validateType(it) }
                is FoxEnumItemOfType -> requireBareGeneric(type, type.type)
                is FoxEnumItemsOfType -> requireBareGeneric(type, type.type)
                is FoxEnumDropItemsOfType -> requireBareGeneric(type, type.type)
                is FoxEnumMergeItemsOfType -> type.types.forEach { validateType(it) }
                is FoxArrayElementOfType -> requireBareGeneric(type, type.type)
                is FoxRefReferentOfType -> requireBareGeneric(type, type.type)
                is FoxMethodOfType -> {
                    validateType(type.`this`)
                    validateType(type.parameters)
                    validateType(type.`return`)
                }
                is FoxMethodThisOfType -> requireBareGeneric(type, type.type)
                is FoxMethodParametersOfType -> requireBareGeneric(type, type.type)
                is FoxMethodReturnOfType -> requireBareGeneric(type, type.type)
            }
        }
        
        validateType(constraint)
    }
    
    return errors
}
