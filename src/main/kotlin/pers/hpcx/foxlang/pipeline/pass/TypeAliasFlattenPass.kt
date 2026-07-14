package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.type.mapTypes
import pers.hpcx.foxlang.type.visitTypes

sealed interface TypeAliasFlattenResult
data class TypeAliasFlattenSuccess(val newFile: SurfaceFile) : TypeAliasFlattenResult
data class TypeAliasFlattenFailure(val errors: List<TypeAliasFlattenError>) : TypeAliasFlattenResult

sealed interface TypeAliasFlattenError
data class TypeAliasDuplicated(val typeAlias: SurfaceTypeAlias) : TypeAliasFlattenError
data class TypeAliasNotFound(val referredBy: SurfaceTypeAlias, val typeName: String) : TypeAliasFlattenError
data class TypeAliasGenericCountMismatch(val referredBy: SurfaceTypeAlias, val type: SurfaceType) : TypeAliasFlattenError
data class TypeAliasLoopDetected(val typeAliases: List<SurfaceTypeAlias>) : TypeAliasFlattenError

private sealed class SurfaceFlattenMarker : SurfacePlaceholderType()
private data class SurfaceGenericRefMarker(val genericName: String) : SurfaceFlattenMarker()
private data class SurfaceAliasRefMarker(
    val aliasName: String,
    val parameters: List<SurfaceType>?,
    val originalType: SurfaceUnresolvedType,
) : SurfaceFlattenMarker()

fun runTypeAliasFlatten(file: SurfaceFile) = TypeAliasFlattenContext(file).run()

private class TypeAliasFlattenContext(
    private val file: SurfaceFile,
) {
    
    private val errors = mutableListOf<TypeAliasFlattenError>()
    private val originalAliases = file.elements.filterIsInstance<SurfaceTypeAlias>()
    private val classifiedAliases = originalAliases.map { classifyAlias(it) }
    private val aliasByName = mutableMapOf<String, SurfaceTypeAlias>()
    private val originalAliasByName = mutableMapOf<String, SurfaceTypeAlias>()
    private val aliasDeps = mutableMapOf<String, MutableSet<String>>()
    private val visitedAliases = mutableSetOf<String>()
    private val activeAliases = mutableSetOf<String>()
    private val aliasStack = mutableListOf<String>()
    private val flattenedAliasByName = mutableMapOf<String, SurfaceTypeAlias>()
    
    fun run(): TypeAliasFlattenResult {
        buildAliasMaps()
        if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
        
        collectAllAliasDeps()
        if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
        
        detectAliasCycles()
        if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
        
        val flattenedAliases = classifiedAliases.map { (_, typeAlias) -> flattenAlias(typeAlias) }
        if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
        
        val publicAliases = flattenedAliases.map { restorePublicAlias(it) }
        if (errors.isNotEmpty()) return TypeAliasFlattenFailure(errors)
        
        val publicAliasByName = publicAliases.associateBy { it.name }
        return TypeAliasFlattenSuccess(
            SurfaceFile(
                file.elements.map { element ->
                    if (element is SurfaceTypeAlias) publicAliasByName.getValue(element.name)
                    else element
                },
            ),
        )
    }
    
    private fun classifyAlias(typeAlias: SurfaceTypeAlias): Pair<SurfaceTypeAlias, SurfaceTypeAlias> {
        return typeAlias to SurfaceTypeAlias(typeAlias.name, typeAlias.generics, classifyTypeRef(typeAlias, typeAlias.alias))
    }
    
    private fun classifyTypeRef(typeAlias: SurfaceTypeAlias, type: SurfaceType): SurfaceType =
        type.mapTypes<SurfaceUnresolvedType> { unresolved ->
            if (unresolved.name in typeAlias.generics && unresolved.parameters == null) {
                SurfaceGenericRefMarker(unresolved.name)
            } else {
                SurfaceAliasRefMarker(
                    unresolved.name,
                    unresolved.parameters?.map { classifyTypeRef(typeAlias, it) },
                    unresolved,
                )
            }
        }
    
    private fun buildAliasMaps() {
        classifiedAliases.forEach { (originalAlias, classifiedAlias) ->
            if (classifiedAlias.name in aliasByName) {
                errors += TypeAliasDuplicated(originalAlias)
            } else {
                aliasByName[classifiedAlias.name] = classifiedAlias
                originalAliasByName[originalAlias.name] = originalAlias
            }
        }
    }
    
    private fun collectAllAliasDeps() {
        classifiedAliases.forEach { (originalAlias, classifiedAlias) ->
            collectAliasDeps(originalAlias, classifiedAlias, classifiedAlias.alias)
        }
    }
    
    private fun collectAliasDeps(
        originalAlias: SurfaceTypeAlias,
        classifiedAlias: SurfaceTypeAlias,
        type: SurfaceType,
    ): Unit = type.visitTypes<SurfaceFlattenMarker> { marker ->
        when (marker) {
            is SurfaceGenericRefMarker -> {}
            is SurfaceAliasRefMarker -> {
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
                parameters.forEach { collectAliasDeps(originalAlias, classifiedAlias, it) }
            }
        }
    }
    
    private fun detectAliasCycles() {
        for ((_, typeAlias) in classifiedAliases) {
            if (detectAliasCycleFrom(typeAlias.name)) {
                break
            }
        }
    }
    
    private fun detectAliasCycleFrom(typeAliasName: String): Boolean {
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
    
    private fun flattenAlias(typeAlias: SurfaceTypeAlias): SurfaceTypeAlias {
        flattenedAliasByName[typeAlias.name]?.let { return it }
        
        val result = SurfaceTypeAlias(typeAlias.name, typeAlias.generics, flattenType(typeAlias.alias))
        flattenedAliasByName[typeAlias.name] = result
        return result
    }
    
    private fun flattenType(type: SurfaceType): SurfaceType = type.mapTypes<SurfaceFlattenMarker> { marker ->
        when (marker) {
            is SurfaceGenericRefMarker -> marker
            is SurfaceAliasRefMarker -> {
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
    
    private fun substituteGenerics(type: SurfaceType, replacement: Map<String, SurfaceType>): SurfaceType =
        type.mapTypes<SurfaceFlattenMarker> { marker ->
            when (marker) {
                is SurfaceGenericRefMarker -> replacement.getValue(marker.genericName)
                is SurfaceAliasRefMarker -> error("unreachable")
            }
        }
    
    private fun restorePublicAlias(typeAlias: SurfaceTypeAlias): SurfaceTypeAlias {
        return SurfaceTypeAlias(typeAlias.name, typeAlias.generics, restoreGenericRefs(typeAlias.alias))
    }
    
    private fun restoreGenericRefs(type: SurfaceType): SurfaceType =
        type.mapTypes<SurfaceFlattenMarker> { marker ->
            when (marker) {
                is SurfaceGenericRefMarker -> SurfaceUnresolvedType(marker.genericName, null)
                is SurfaceAliasRefMarker -> error("unreachable")
            }
        }
}
