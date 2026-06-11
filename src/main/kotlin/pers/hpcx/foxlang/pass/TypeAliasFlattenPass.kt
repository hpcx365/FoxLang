package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.*

sealed interface TypeAliasFlattenResult
data class TypeAliasFlattenSuccess(val newFile: FoxFile) : TypeAliasFlattenResult
data class TypeAliasFlattenFailure(val errors: List<TypeAliasFlattenError>) : TypeAliasFlattenResult

sealed interface TypeAliasFlattenError
data class TypeAliasUnexpectedWildcard(val typeAlias: FoxTypeAlias) : TypeAliasFlattenError
data class TypeAliasDuplicated(val typeAlias: FoxTypeAlias) : TypeAliasFlattenError
data class TypeAliasNotFound(val referredBy: FoxTypeAlias, val typeName: String) : TypeAliasFlattenError
data class TypeAliasUnexpectedGenerics(val referredBy: FoxTypeAlias, val type: FoxType) : TypeAliasFlattenError
data class TypeAliasMissingGenerics(val referredBy: FoxTypeAlias, val type: FoxType) : TypeAliasFlattenError
data class TypeAliasGenericCountMismatch(val referredBy: FoxTypeAlias, val type: FoxType) : TypeAliasFlattenError
data class TypeAliasLoopDetected(val typeAliases: List<FoxTypeAlias>) : TypeAliasFlattenError

private sealed interface FoxFlattenMarker : FoxPlaceholderType
private data class FoxGenericRefMarker(val genericName: String) : FoxFlattenMarker
private data class FoxAliasRefMarker(val aliasName: String, val parameters: List<FoxType>?) : FoxFlattenMarker

fun runTypeAliasFlatten(file: FoxFile): TypeAliasFlattenResult {
    val errors = mutableListOf<TypeAliasFlattenError>()
    
    val originalAliases = file.elements.filterIsInstance<FoxTypeAlias>()
    originalAliases.forEach { typeAlias ->
        fun checkNoWildcard(type: FoxType) {
            when (type) {
                is FoxPrimitiveType -> {}
                is FoxBuiltInType -> type.nestedTypes().forEach { checkNoWildcard(it) }
                is FoxTransformType -> type.nestedTypes().forEach { checkNoWildcard(it) }
                is FoxUnresolvedType -> type.parameters?.forEach { checkNoWildcard(it) }
                is FoxWildcardType -> errors += TypeAliasUnexpectedWildcard(typeAlias)
                is FoxPlaceholderType -> error("unreachable")
            }
        }
        checkNoWildcard(typeAlias.alias)
    }
    
    if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
    
    val classifiedAliases = originalAliases.map { typeAlias ->
        fun classifyTypeRef(type: FoxType): FoxType = when (type) {
            is FoxPrimitiveType -> type
            is FoxBuiltInType -> type.rebuildWith(type.nestedTypes().map { classifyTypeRef(it) })
            is FoxTransformType -> type.rebuildWith(type.nestedTypes().map { classifyTypeRef(it) })
            is FoxUnresolvedType -> {
                if (type.parameters == null && typeAlias.generics?.let { generics -> type.name in generics } == true) {
                    FoxGenericRefMarker(type.name)
                } else {
                    FoxAliasRefMarker(type.name, type.parameters?.map { classifyTypeRef(it) })
                }
            }
            is FoxWildcardType,
            is FoxPlaceholderType,
                -> error("unreachable")
        }
        FoxTypeAlias(typeAlias.name, typeAlias.generics, classifyTypeRef(typeAlias.alias))
    }
    
    val aliasByName = mutableMapOf<String, FoxTypeAlias>()
    classifiedAliases.forEach { typeAlias ->
        if (typeAlias.name in aliasByName) {
            errors += TypeAliasDuplicated(typeAlias)
        } else {
            aliasByName[typeAlias.name] = typeAlias
        }
    }
    
    if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
    
    val aliasDeps = mutableMapOf<String, MutableSet<String>>()
    classifiedAliases.forEach { typeAlias ->
        fun collectAliasDeps(type: FoxType) {
            when (type) {
                is FoxPrimitiveType -> {}
                is FoxBuiltInType -> type.nestedTypes().forEach { collectAliasDeps(it) }
                is FoxTransformType -> type.nestedTypes().forEach { collectAliasDeps(it) }
                is FoxPlaceholderType -> {
                    check(type is FoxFlattenMarker)
                    when (type) {
                        is FoxGenericRefMarker -> {}
                        is FoxAliasRefMarker -> {
                            val alias = aliasByName[type.aliasName] ?: run {
                                errors += TypeAliasNotFound(typeAlias, type.aliasName)
                                return
                            }
                            if (alias.generics == null) {
                                if (type.parameters != null) {
                                    errors += TypeAliasUnexpectedGenerics(typeAlias, type)
                                    return
                                }
                            } else {
                                if (type.parameters == null) {
                                    errors += TypeAliasMissingGenerics(typeAlias, type)
                                    return
                                }
                                if (type.parameters.size != alias.generics.size) {
                                    errors += TypeAliasGenericCountMismatch(typeAlias, type)
                                    return
                                }
                            }
                            aliasDeps.getOrPut(typeAlias.name) { mutableSetOf() }.add(type.aliasName)
                            type.parameters?.forEach { collectAliasDeps(it) }
                        }
                    }
                }
                is FoxWildcardType,
                is FoxUnresolvedType,
                    -> error("unreachable")
            }
        }
        collectAliasDeps(typeAlias.alias)
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
            errors += TypeAliasLoopDetected(aliasStack.subList(index, aliasStack.size).map { aliasByName.getValue(it) })
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
    
    for (typeAlias in classifiedAliases) {
        if (detectAliasCycleFrom(typeAlias.name)) {
            break
        }
    }
    
    if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
    
    val flattenedAliasByName = mutableMapOf<String, FoxTypeAlias>()
    
    fun flattenAlias(typeAlias: FoxTypeAlias): FoxTypeAlias {
        flattenedAliasByName[typeAlias.name]?.let { return it }
        
        fun substituteGenerics(type: FoxType, replacement: Map<String, FoxType>): FoxType = when (type) {
            is FoxPrimitiveType -> type
            is FoxBuiltInType -> type.rebuildWith(type.nestedTypes().map { substituteGenerics(it, replacement) })
            is FoxTransformType -> type.rebuildWith(type.nestedTypes().map { substituteGenerics(it, replacement) })
            is FoxPlaceholderType -> {
                check(type is FoxFlattenMarker)
                when (type) {
                    is FoxGenericRefMarker -> replacement.getValue(type.genericName)
                    is FoxAliasRefMarker -> error("unreachable")
                }
            }
            is FoxWildcardType,
            is FoxUnresolvedType,
                -> error("unreachable")
        }
        
        fun flattenType(type: FoxType): FoxType = when (type) {
            is FoxPrimitiveType -> type
            is FoxBuiltInType -> type.rebuildWith(type.nestedTypes().map { flattenType(it) })
            is FoxTransformType -> type.rebuildWith(type.nestedTypes().map { flattenType(it) })
            is FoxPlaceholderType -> {
                check(type is FoxFlattenMarker)
                when (type) {
                    is FoxGenericRefMarker -> type
                    is FoxAliasRefMarker -> {
                        val flattened = flattenAlias(aliasByName.getValue(type.aliasName))
                        if (flattened.generics != null && type.parameters != null) {
                            val genericReplacement = flattened.generics.zip(type.parameters.map { flattenType(it) }).toMap()
                            substituteGenerics(flattened.alias, genericReplacement)
                        } else {
                            flattened.alias
                        }
                    }
                }
            }
            is FoxWildcardType,
            is FoxUnresolvedType,
                -> error("unreachable")
        }
        
        val result = FoxTypeAlias(typeAlias.name, typeAlias.generics, flattenType(typeAlias.alias))
        flattenedAliasByName[typeAlias.name] = result
        return result
    }
    
    val flattenedAliases = classifiedAliases.map { flattenAlias(it) }
    
    if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
    
    val publicAliases = flattenedAliases.map { typeAlias ->
        fun restoreGenericRefs(type: FoxType): FoxType = when (type) {
            is FoxPrimitiveType -> type
            is FoxBuiltInType -> type.rebuildWith(type.nestedTypes().map { restoreGenericRefs(it) })
            is FoxTransformType -> type.rebuildWith(type.nestedTypes().map { restoreGenericRefs(it) })
            is FoxPlaceholderType -> {
                check(type is FoxFlattenMarker)
                when (type) {
                    is FoxGenericRefMarker -> FoxUnresolvedType(type.genericName, null)
                    is FoxAliasRefMarker -> error("unreachable")
                }
            }
            is FoxWildcardType,
            is FoxUnresolvedType,
                -> error("unreachable")
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
