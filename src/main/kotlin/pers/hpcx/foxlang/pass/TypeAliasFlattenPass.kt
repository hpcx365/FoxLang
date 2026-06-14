package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.mapValues
import pers.hpcx.foxlang.utils.orEmpty

sealed interface TypeAliasFlattenResult
data class TypeAliasFlattenSuccess(val newFile: FoxFile) : TypeAliasFlattenResult
data class TypeAliasFlattenFailure(val errors: List<TypeAliasFlattenError>) : TypeAliasFlattenResult

sealed interface TypeAliasFlattenError
data class TypeAliasDuplicated(val typeAlias: FoxTypeAlias) : TypeAliasFlattenError
data class TypeAliasNotFound(val referredBy: FoxTypeAlias, val typeName: String) : TypeAliasFlattenError
data class TypeAliasUnexpectedGenerics(val referredBy: FoxTypeAlias, val type: FoxType) : TypeAliasFlattenError
data class TypeAliasMissingGenerics(val referredBy: FoxTypeAlias, val type: FoxType) : TypeAliasFlattenError
data class TypeAliasGenericCountMismatch(val referredBy: FoxTypeAlias, val type: FoxType) : TypeAliasFlattenError
data class TypeAliasLoopDetected(val typeAliases: List<FoxTypeAlias>) : TypeAliasFlattenError

private sealed interface FoxFlattenMarker : FoxPlaceholderType
private data class FoxGenericRefMarker(val genericName: String) : FoxFlattenMarker
private data class FoxAliasRefMarker(
    val aliasName: String,
    val parameters: List<FoxType>?,
    val originalType: FoxUnresolvedType,
) : FoxFlattenMarker

fun runTypeAliasFlatten(file: FoxFile): TypeAliasFlattenResult {
    val errors = mutableListOf<TypeAliasFlattenError>()
    
    val originalAliases = file.elements.filterIsInstance<FoxTypeAlias>()
    val classifiedAliases = originalAliases.map { typeAlias ->
        fun classifyTypeRef(type: FoxType): FoxType = type.mapTypes<FoxUnresolvedType> { unresolved ->
            if (unresolved.name in typeAlias.generics.orEmpty() && unresolved.parameters == null) {
                FoxGenericRefMarker(unresolved.name)
            } else {
                FoxAliasRefMarker(unresolved.name, unresolved.parameters?.map { classifyTypeRef(it) }, unresolved)
            }
        }
        typeAlias to FoxTypeAlias(typeAlias.name, typeAlias.generics, classifyTypeRef(typeAlias.alias))
    }
    
    val aliasByName = mutableMapOf<String, FoxTypeAlias>()
    val originalAliasByName = mutableMapOf<String, FoxTypeAlias>()
    classifiedAliases.forEach { (originalAlias, classifiedAlias) ->
        if (classifiedAlias.name in aliasByName) {
            errors += TypeAliasDuplicated(originalAlias)
        } else {
            aliasByName[classifiedAlias.name] = classifiedAlias
            originalAliasByName[originalAlias.name] = originalAlias
        }
    }
    
    if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
    
    val aliasDeps = mutableMapOf<String, MutableSet<String>>()
    classifiedAliases.forEach { (originalAlias, classifiedAlias) ->
        fun collectAliasDeps(type: FoxType): Unit = type.visitTypes<FoxFlattenMarker> { marker ->
            when (marker) {
                is FoxGenericRefMarker -> {}
                is FoxAliasRefMarker -> {
                    val alias = aliasByName[marker.aliasName] ?: run {
                        errors += TypeAliasNotFound(originalAlias, marker.aliasName)
                        return@visitTypes
                    }
                    if (alias.generics == null) {
                        if (marker.parameters != null) {
                            errors += TypeAliasUnexpectedGenerics(originalAlias, marker.originalType)
                            return@visitTypes
                        }
                    } else {
                        if (marker.parameters == null) {
                            errors += TypeAliasMissingGenerics(originalAlias, marker.originalType)
                            return@visitTypes
                        }
                        if (marker.parameters.size != alias.generics.size) {
                            errors += TypeAliasGenericCountMismatch(originalAlias, marker.originalType)
                            return@visitTypes
                        }
                    }
                    aliasDeps.getOrPut(classifiedAlias.name) { mutableSetOf() }.add(marker.aliasName)
                    marker.parameters?.forEach { collectAliasDeps(it) }
                }
            }
        }
        collectAliasDeps(classifiedAlias.alias)
    }
    
    if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
    
    val visitedAliases = mutableSetOf<String>()
    val activeAliases = mutableSetOf<String>()
    val aliasStack = mutableListOf<String>()
    
    fun detectAliasCycleFrom(typeAliasName: String): Boolean {
        if (typeAliasName in visitedAliases) return false
        if (!activeAliases.add(typeAliasName)) {
            val index = aliasStack.indexOf(typeAliasName)
            check(index >= 0)
            errors += TypeAliasLoopDetected(aliasStack.subList(index, aliasStack.size).map { originalAliasByName.getValue(it) })
            return true
        }
        aliasStack += typeAliasName
        aliasDeps[typeAliasName]?.forEach { dependency ->
            if (detectAliasCycleFrom(dependency)) return true
        }
        check(aliasStack.removeAt(aliasStack.lastIndex) == typeAliasName)
        activeAliases.remove(typeAliasName)
        visitedAliases += typeAliasName
        return false
    }
    
    for ((_, typeAlias) in classifiedAliases) {
        if (detectAliasCycleFrom(typeAlias.name)) {
            break
        }
    }
    
    if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
    
    val flattenedAliasByName = mutableMapOf<String, FoxTypeAlias>()
    
    fun flattenAlias(typeAlias: FoxTypeAlias): FoxTypeAlias {
        flattenedAliasByName[typeAlias.name]?.let { return it }
        
        fun substituteGenerics(type: FoxType, replacement: Map<String, FoxType>): FoxType = type.mapTypes<FoxFlattenMarker> { marker ->
            when (marker) {
                is FoxGenericRefMarker -> replacement.getValue(marker.genericName)
                is FoxAliasRefMarker -> error("unreachable")
            }
        }
        
        fun flattenType(type: FoxType): FoxType = type.mapTypes<FoxFlattenMarker> { marker ->
            when (marker) {
                is FoxGenericRefMarker -> marker
                is FoxAliasRefMarker -> {
                    val flattened = flattenAlias(aliasByName.getValue(marker.aliasName))
                    if (flattened.generics != null && marker.parameters != null) {
                        val genericReplacement = flattened.generics.zip(marker.parameters.map { flattenType(it) }).toMap()
                        substituteGenerics(flattened.alias, genericReplacement)
                    } else {
                        flattened.alias
                    }
                }
            }
        }
        
        val result = FoxTypeAlias(typeAlias.name, typeAlias.generics, flattenType(typeAlias.alias))
        flattenedAliasByName[typeAlias.name] = result
        return result
    }
    
    val flattenedAliases = classifiedAliases.map { (_, typeAlias) -> flattenAlias(typeAlias) }
    
    if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
    
    val publicAliases = flattenedAliases.map { typeAlias ->
        fun restoreGenericRefs(type: FoxType): FoxType = type.mapTypes<FoxFlattenMarker> { marker ->
            when (marker) {
                is FoxGenericRefMarker -> FoxUnresolvedType(marker.genericName, null)
                is FoxAliasRefMarker -> error("unreachable")
            }
        }
        FoxTypeAlias(typeAlias.name, typeAlias.generics, restoreGenericRefs(typeAlias.alias))
    }
    
    if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
    
    val publicAliasByName = publicAliases.associateBy { it.name }
    return TypeAliasFlattenSuccess(
        FoxFile(
            file.elements.map { element ->
                if (element is FoxTypeAlias) publicAliasByName.getValue(element.name)
                else element
            },
        ),
    )
}

inline fun <reified T : FoxType> FoxType.visitTypes(crossinline visitor: (T) -> Unit) {
    visitTypes({ type -> type is T }) { type -> visitor(type as T) }
}

fun FoxType.visitTypes(filter: (FoxType) -> Boolean, visitor: (FoxType) -> Unit) {
    if (filter(this)) visitor(this)
    else when (this) {
        is FoxPrimitiveType -> {}
        is FoxWildcardType -> {}
        is FoxBuiltInType -> when (this) {
            is FoxTupleType -> components.forEach { it.first.visitTypes(filter, visitor) }
            is FoxStructType -> fields.values.forEach { it.visitTypes(filter, visitor) }
            is FoxObjectType -> members.values.forEach { it.visitTypes(filter, visitor) }
            is FoxEnumType -> items.values.forEach { it.visitTypes(filter, visitor) }
            is FoxArrayType -> element.visitTypes(filter, visitor)
            is FoxRefType -> referent.visitTypes(filter, visitor)
            is FoxMethodType -> {
                `this`.visitTypes(filter, visitor)
                parameters.values.forEach { it.visitTypes(filter, visitor) }
                `return`.visitTypes(filter, visitor)
            }
        }
        is FoxTransformType -> when (this) {
            is FoxTupleComponentAtType -> type.visitTypes(filter, visitor)
            is FoxTupleLastComponentAtType -> type.visitTypes(filter, visitor)
            is FoxTupleFirstComponentsOfType -> type.visitTypes(filter, visitor)
            is FoxTupleLastComponentsOfType -> type.visitTypes(filter, visitor)
            is FoxTupleDropFirstComponentsOfType -> type.visitTypes(filter, visitor)
            is FoxTupleDropLastComponentsOfType -> type.visitTypes(filter, visitor)
            is FoxTupleMergeComponentsOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxStructFieldOfType -> type.visitTypes(filter, visitor)
            is FoxStructFieldAtType -> type.visitTypes(filter, visitor)
            is FoxStructLastFieldAtType -> type.visitTypes(filter, visitor)
            is FoxStructFirstFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructLastFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructDropFirstFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructDropLastFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructDropFieldsOfType -> type.visitTypes(filter, visitor)
            is FoxStructMergeFieldsOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxObjectMemberOfType -> type.visitTypes(filter, visitor)
            is FoxObjectMembersOfType -> type.visitTypes(filter, visitor)
            is FoxObjectDropMembersOfType -> type.visitTypes(filter, visitor)
            is FoxObjectMergeMembersOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxEnumItemOfType -> type.visitTypes(filter, visitor)
            is FoxEnumItemsOfType -> type.visitTypes(filter, visitor)
            is FoxEnumDropItemsOfType -> type.visitTypes(filter, visitor)
            is FoxEnumMergeItemsOfType -> types.forEach { it.visitTypes(filter, visitor) }
            is FoxArrayElementOfType -> type.visitTypes(filter, visitor)
            is FoxRefReferentOfType -> type.visitTypes(filter, visitor)
            is FoxMethodThisOfType -> type.visitTypes(filter, visitor)
            is FoxMethodParametersOfType -> type.visitTypes(filter, visitor)
            is FoxMethodReturnOfType -> type.visitTypes(filter, visitor)
        }
        is FoxUnresolvedType -> parameters?.forEach { it.visitTypes(filter, visitor) }
        is FoxPlaceholderType -> error("Placeholder type cannot be visited")
    }
}

inline fun <reified T : FoxType> FoxType.mapTypes(crossinline mapper: (T) -> FoxType): FoxType {
    return mapTypes({ type -> type is T }) { type -> mapper(type as T) }
}

fun FoxType.mapTypes(filter: (FoxType) -> Boolean, mapper: (FoxType) -> FoxType): FoxType {
    if (filter(this)) return mapper(this)
    else return when (this) {
        is FoxPrimitiveType -> this
        is FoxWildcardType -> this
        is FoxBuiltInType -> when (this) {
            is FoxTupleType -> FoxTupleType(components.map { it.first.mapTypes(filter, mapper) to it.second })
            is FoxStructType -> FoxStructType(fields.mapValues { it.value.mapTypes(filter, mapper) })
            is FoxObjectType -> FoxObjectType(members.mapValues { it.value.mapTypes(filter, mapper) })
            is FoxEnumType -> FoxEnumType(items.mapValues { it.value.mapTypes(filter, mapper) })
            is FoxArrayType -> FoxArrayType(element.mapTypes(filter, mapper))
            is FoxRefType -> FoxRefType(referent.mapTypes(filter, mapper))
            is FoxMethodType -> FoxMethodType(
                `this`.mapTypes(filter, mapper),
                parameters.mapValues { it.value.mapTypes(filter, mapper) },
                `return`.mapTypes(filter, mapper),
            )
        }
        is FoxTransformType -> when (this) {
            is FoxTupleComponentAtType -> FoxTupleComponentAtType(type.mapTypes(filter, mapper), index)
            is FoxTupleLastComponentAtType -> FoxTupleLastComponentAtType(type.mapTypes(filter, mapper), index)
            is FoxTupleFirstComponentsOfType -> FoxTupleFirstComponentsOfType(type.mapTypes(filter, mapper), count)
            is FoxTupleLastComponentsOfType -> FoxTupleLastComponentsOfType(type.mapTypes(filter, mapper), count)
            is FoxTupleDropFirstComponentsOfType -> FoxTupleDropFirstComponentsOfType(type.mapTypes(filter, mapper), count)
            is FoxTupleDropLastComponentsOfType -> FoxTupleDropLastComponentsOfType(type.mapTypes(filter, mapper), count)
            is FoxTupleMergeComponentsOfType -> FoxTupleMergeComponentsOfType(types.map { it.mapTypes(filter, mapper) })
            is FoxStructFieldOfType -> FoxStructFieldOfType(type.mapTypes(filter, mapper), name)
            is FoxStructFieldAtType -> FoxStructFieldAtType(type.mapTypes(filter, mapper), index)
            is FoxStructLastFieldAtType -> FoxStructLastFieldAtType(type.mapTypes(filter, mapper), index)
            is FoxStructFirstFieldsOfType -> FoxStructFirstFieldsOfType(type.mapTypes(filter, mapper), count)
            is FoxStructLastFieldsOfType -> FoxStructLastFieldsOfType(type.mapTypes(filter, mapper), count)
            is FoxStructDropFirstFieldsOfType -> FoxStructDropFirstFieldsOfType(type.mapTypes(filter, mapper), count)
            is FoxStructDropLastFieldsOfType -> FoxStructDropLastFieldsOfType(type.mapTypes(filter, mapper), count)
            is FoxStructFieldsOfType -> FoxStructFieldsOfType(type.mapTypes(filter, mapper), names)
            is FoxStructDropFieldsOfType -> FoxStructDropFieldsOfType(type.mapTypes(filter, mapper), names)
            is FoxStructMergeFieldsOfType -> FoxStructMergeFieldsOfType(types.map { it.mapTypes(filter, mapper) })
            is FoxObjectMemberOfType -> FoxObjectMemberOfType(type.mapTypes(filter, mapper), name)
            is FoxObjectMembersOfType -> FoxObjectMembersOfType(type.mapTypes(filter, mapper), names)
            is FoxObjectDropMembersOfType -> FoxObjectDropMembersOfType(type.mapTypes(filter, mapper), names)
            is FoxObjectMergeMembersOfType -> FoxObjectMergeMembersOfType(types.map { it.mapTypes(filter, mapper) })
            is FoxEnumItemOfType -> FoxEnumItemOfType(type.mapTypes(filter, mapper), name)
            is FoxEnumItemsOfType -> FoxEnumItemsOfType(type.mapTypes(filter, mapper), names)
            is FoxEnumDropItemsOfType -> FoxEnumDropItemsOfType(type.mapTypes(filter, mapper), names)
            is FoxEnumMergeItemsOfType -> FoxEnumMergeItemsOfType(types.map { it.mapTypes(filter, mapper) })
            is FoxArrayElementOfType -> FoxArrayElementOfType(type.mapTypes(filter, mapper))
            is FoxRefReferentOfType -> FoxRefReferentOfType(type.mapTypes(filter, mapper))
            is FoxMethodThisOfType -> FoxMethodThisOfType(type.mapTypes(filter, mapper))
            is FoxMethodParametersOfType -> FoxMethodParametersOfType(type.mapTypes(filter, mapper))
            is FoxMethodReturnOfType -> FoxMethodReturnOfType(type.mapTypes(filter, mapper))
        }
        is FoxUnresolvedType -> FoxUnresolvedType(name, parameters?.map { it.mapTypes(filter, mapper) })
        is FoxPlaceholderType -> error("Placeholder type cannot be mapped")
    }
}
