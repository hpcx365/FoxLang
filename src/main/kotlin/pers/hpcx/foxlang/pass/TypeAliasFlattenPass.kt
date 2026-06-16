package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.mapTypes
import pers.hpcx.foxlang.type.visitTypes

sealed interface TypeAliasFlattenResult
data class TypeAliasFlattenSuccess(val newFile: FoxFile) : TypeAliasFlattenResult
data class TypeAliasFlattenFailure(val errors: List<TypeAliasFlattenError>) : TypeAliasFlattenResult

sealed interface TypeAliasFlattenError
data class TypeAliasDuplicated(val typeAlias: FoxTypeAlias) : TypeAliasFlattenError
data class TypeAliasNotFound(val referredBy: FoxTypeAlias, val typeName: String) : TypeAliasFlattenError
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
            if (unresolved.name in typeAlias.generics && unresolved.parameters == null) {
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
                    val parameters = marker.parameters.orEmpty()
                    if (parameters.size != alias.generics.size) {
                        errors += TypeAliasGenericCountMismatch(originalAlias, marker.originalType)
                        return@visitTypes
                    }
                    aliasDeps.getOrPut(classifiedAlias.name) { mutableSetOf() }.add(marker.aliasName)
                    parameters.forEach { collectAliasDeps(it) }
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
                    if (marker.parameters != null) {
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
