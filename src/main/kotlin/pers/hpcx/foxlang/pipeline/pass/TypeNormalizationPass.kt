package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.type.*
import pers.hpcx.foxlang.utils.MutableOrderedMap
import pers.hpcx.foxlang.utils.mapValues
import pers.hpcx.foxlang.utils.mutableOrderedMapOf

sealed interface TypeNormalizationResult
data class TypeNormalizationSuccess(val type: SurfaceType) : TypeNormalizationResult
data class TypeNormalizationFailure(val errors: List<TypeNormalizationError>) : TypeNormalizationResult

sealed interface TypeNormalizationError
data class TypeNormalizationFamilyMismatch(
    val transform: SurfaceType,
    val expectedFamily: String,
    val actualType: SurfaceType,
) : TypeNormalizationError

data class TypeNormalizationIndexOutOfBounds(
    val transform: SurfaceType,
    val index: Int,
    val size: Int,
) : TypeNormalizationError

data class TypeNormalizationNameNotFound(
    val transform: SurfaceType,
    val name: String,
) : TypeNormalizationError

data class TypeNormalizationDuplicateName(
    val transform: SurfaceType,
    val name: String,
) : TypeNormalizationError

fun runTypeNormalization(type: SurfaceType) = TypeNormalizationContext().normalize(type)

private class TypeNormalizationContext {
    
    private val errors = mutableListOf<TypeNormalizationError>()
    
    fun normalize(type: SurfaceType): TypeNormalizationResult {
        val normalizedType = normalizeType(type)
        if (errors.isNotEmpty()) return TypeNormalizationFailure(errors)
        return TypeNormalizationSuccess(normalizedType)
    }
    
    private fun familyMismatch(transform: SurfaceType, expectedFamily: String, actualType: SurfaceType): SurfaceType {
        errors += TypeNormalizationFamilyMismatch(transform, expectedFamily, actualType)
        return transform
    }
    
    private fun indexOutOfBounds(transform: SurfaceType, index: Int, size: Int): SurfaceType {
        errors += TypeNormalizationIndexOutOfBounds(transform, index, size)
        return transform
    }
    
    private fun nameNotFound(transform: SurfaceType, name: String): SurfaceType {
        errors += TypeNormalizationNameNotFound(transform, name)
        return transform
    }
    
    private fun duplicateName(transform: SurfaceType, name: String): SurfaceType {
        errors += TypeNormalizationDuplicateName(transform, name)
        return transform
    }
    
    private fun normalizeTupleTransform(
        transform: SurfaceType,
        normalizedType: SurfaceType,
        action: (SurfaceTupleType) -> SurfaceType,
    ): SurfaceType = when (normalizedType) {
        is SurfaceTupleType -> action(normalizedType)
        is SurfaceUnresolvedType, is SurfaceTransformType -> transform
        else -> familyMismatch(transform, "Tuple", normalizedType)
    }
    
    private fun normalizeStructTransform(
        transform: SurfaceType,
        normalizedType: SurfaceType,
        action: (SurfaceStructType) -> SurfaceType,
    ): SurfaceType = when (normalizedType) {
        is SurfaceStructType -> action(normalizedType)
        is SurfaceUnresolvedType, is SurfaceTransformType -> transform
        else -> familyMismatch(transform, "Struct", normalizedType)
    }
    
    private fun normalizeObjectTransform(
        transform: SurfaceType,
        normalizedType: SurfaceType,
        action: (SurfaceObjectType) -> SurfaceType,
    ): SurfaceType = when (normalizedType) {
        is SurfaceObjectType -> action(normalizedType)
        is SurfaceUnresolvedType, is SurfaceTransformType -> transform
        else -> familyMismatch(transform, "Object", normalizedType)
    }
    
    private fun normalizeEnumTransform(
        transform: SurfaceType,
        normalizedType: SurfaceType,
        action: (SurfaceEnumType) -> SurfaceType,
    ): SurfaceType = when (normalizedType) {
        is SurfaceEnumType -> action(normalizedType)
        is SurfaceUnresolvedType, is SurfaceTransformType -> transform
        else -> familyMismatch(transform, "Enum", normalizedType)
    }
    
    private fun normalizeArrayTransform(
        transform: SurfaceType,
        normalizedType: SurfaceType,
        action: (SurfaceArrayType) -> SurfaceType,
    ): SurfaceType = when (normalizedType) {
        is SurfaceArrayType -> action(normalizedType)
        is SurfaceUnresolvedType, is SurfaceTransformType -> transform
        else -> familyMismatch(transform, "Array", normalizedType)
    }
    
    private fun normalizeRefTransform(
        transform: SurfaceType,
        normalizedType: SurfaceType,
        action: (SurfaceRefType) -> SurfaceType,
    ): SurfaceType = when (normalizedType) {
        is SurfaceRefType -> action(normalizedType)
        is SurfaceUnresolvedType, is SurfaceTransformType -> transform
        else -> familyMismatch(transform, "Ref", normalizedType)
    }
    
    private fun normalizeMethodOf(
        transform: SurfaceType,
        normalizedThis: SurfaceType,
        normalizedParameters: SurfaceType,
        normalizedReturn: SurfaceType,
        action: (SurfaceType, SurfaceStructType, SurfaceType) -> SurfaceType,
    ): SurfaceType = when (normalizedParameters) {
        is SurfaceStructType -> action(normalizedThis, normalizedParameters, normalizedReturn)
        is SurfaceUnresolvedType, is SurfaceTransformType -> transform
        else -> familyMismatch(transform, "Struct", normalizedParameters)
    }
    
    private fun normalizeMethodTransform(
        transform: SurfaceType,
        normalizedType: SurfaceType,
        action: (SurfaceMethodType) -> SurfaceType,
    ): SurfaceType = when (normalizedType) {
        is SurfaceMethodType -> action(normalizedType)
        is SurfaceUnresolvedType, is SurfaceTransformType -> transform
        else -> familyMismatch(transform, "Method", normalizedType)
    }
    
    private fun normalizeCountLoose(transform: SurfaceType, count: Int, size: Int, action: (Int) -> SurfaceType): SurfaceType {
        return if (count < 0) indexOutOfBounds(transform, count, size) else action(minOf(count, size))
    }
    
    private fun normalizeStructNameSelectionExact(transform: SurfaceType, struct: SurfaceStructType, names: Set<String>): SurfaceType {
        names.forEach { name -> if (name !in struct.fields) return nameNotFound(transform, name) }
        return struct.selectFields(names)
    }
    
    private fun normalizeObjectNameSelectionExact(transform: SurfaceType, obj: SurfaceObjectType, names: Set<String>): SurfaceType {
        names.forEach { name -> if (name !in obj.members) return nameNotFound(transform, name) }
        return obj.selectMembers(names)
    }
    
    private fun normalizeEnumNameSelectionExact(transform: SurfaceType, enumType: SurfaceEnumType, names: Set<String>): SurfaceType {
        names.forEach { name -> if (name !in enumType.entries) return nameNotFound(transform, name) }
        return enumType.selectEntries(names)
    }
    
    private fun normalizeStructNameDropExact(transform: SurfaceType, struct: SurfaceStructType, names: Set<String>): SurfaceType {
        names.forEach { name -> if (name !in struct.fields) return nameNotFound(transform, name) }
        return struct.dropFields(names)
    }
    
    private fun normalizeObjectNameDropExact(transform: SurfaceType, obj: SurfaceObjectType, names: Set<String>): SurfaceType {
        names.forEach { name -> if (name !in obj.members) return nameNotFound(transform, name) }
        return obj.dropMembers(names)
    }
    
    private fun normalizeEnumNameDropExact(transform: SurfaceType, enumType: SurfaceEnumType, names: Set<String>): SurfaceType {
        names.forEach { name -> if (name !in enumType.entries) return nameNotFound(transform, name) }
        return enumType.dropEntries(names)
    }
    
    private fun normalizeCountExact(transform: SurfaceType, count: Int, size: Int, action: () -> SurfaceType): SurfaceType {
        return if (count !in 0..size) indexOutOfBounds(transform, count, size) else action()
    }
    
    private fun normalizeTupleIndex(transform: SurfaceType, index: Int, size: Int, action: () -> SurfaceType): SurfaceType {
        return if (index !in 0..<size) indexOutOfBounds(transform, index, size) else action()
    }
    
    private fun normalizeStructIndex(transform: SurfaceType, index: Int, size: Int, action: () -> SurfaceType): SurfaceType {
        return if (index !in 0..<size) indexOutOfBounds(transform, index, size) else action()
    }
    
    private fun mergeStructFieldsStrict(transform: SurfaceType, structs: List<SurfaceStructType>): SurfaceType {
        val result: MutableOrderedMap<String, SurfaceType> = mutableOrderedMapOf()
        structs.forEach { struct ->
            struct.fields.entries.forEach { (name, fieldType) ->
                if (name in result) return duplicateName(transform, name)
                result[name] = fieldType
            }
        }
        return SurfaceStructType(result)
    }
    
    private fun mergeObjectMembersStrict(transform: SurfaceType, objects: List<SurfaceObjectType>): SurfaceType {
        val result = LinkedHashMap<String, SurfaceType>()
        objects.forEach { obj ->
            obj.members.forEach { (name, memberType) ->
                if (name in result) return duplicateName(transform, name)
                result[name] = memberType
            }
        }
        return SurfaceObjectType(result)
    }
    
    private fun mergeEnumEntriesStrict(transform: SurfaceType, enums: List<SurfaceEnumType>): SurfaceType {
        val result = LinkedHashMap<String, SurfaceType>()
        enums.forEach { enumType ->
            enumType.entries.forEach { (name, itemType) ->
                if (name in result) return duplicateName(transform, name)
                result[name] = itemType
            }
        }
        return SurfaceEnumType(result)
    }
    
    private fun normalizeMergeTuple(types: List<SurfaceType>): SurfaceType {
        val normalizedTypes = types.map { normalizeType(it) }
        if (errors.isNotEmpty()) return SurfaceTupleMergeTuplesType(normalizedTypes)
        if (normalizedTypes.any { it is SurfaceUnresolvedType || it is SurfaceTransformType }) {
            return SurfaceTupleMergeTuplesType(normalizedTypes)
        }
        val tuples = normalizedTypes.map { normalizedType ->
            normalizedType as? SurfaceTupleType ?: return familyMismatch(SurfaceTupleMergeTuplesType(normalizedTypes), "Tuple", normalizedType)
        }
        return tuples.mergeTuples()
    }
    
    private fun normalizeMergeStruct(types: List<SurfaceType>): SurfaceType {
        val normalizedTypes = types.map { normalizeType(it) }
        if (errors.isNotEmpty()) return SurfaceStructMergeStructsType(normalizedTypes)
        if (normalizedTypes.any { it is SurfaceUnresolvedType || it is SurfaceTransformType }) {
            return SurfaceStructMergeStructsType(normalizedTypes)
        }
        val structs = normalizedTypes.map { normalizedType ->
            normalizedType as? SurfaceStructType ?: return familyMismatch(SurfaceStructMergeStructsType(normalizedTypes), "Struct", normalizedType)
        }
        return mergeStructFieldsStrict(SurfaceStructMergeStructsType(normalizedTypes), structs)
    }
    
    private fun normalizeMergeObject(types: List<SurfaceType>): SurfaceType {
        val normalizedTypes = types.map { normalizeType(it) }
        if (errors.isNotEmpty()) return SurfaceObjectMergeObjectsType(normalizedTypes)
        if (normalizedTypes.any { it is SurfaceUnresolvedType || it is SurfaceTransformType }) {
            return SurfaceObjectMergeObjectsType(normalizedTypes)
        }
        val objects = normalizedTypes.map { normalizedType ->
            normalizedType as? SurfaceObjectType ?: return familyMismatch(SurfaceObjectMergeObjectsType(normalizedTypes), "Object", normalizedType)
        }
        return mergeObjectMembersStrict(SurfaceObjectMergeObjectsType(normalizedTypes), objects)
    }
    
    private fun normalizeMergeEnum(types: List<SurfaceType>): SurfaceType {
        val normalizedTypes = types.map { normalizeType(it) }
        if (errors.isNotEmpty()) return SurfaceEnumMergeEnumsType(normalizedTypes)
        if (normalizedTypes.any { it is SurfaceUnresolvedType || it is SurfaceTransformType }) {
            return SurfaceEnumMergeEnumsType(normalizedTypes)
        }
        val enums = normalizedTypes.map { normalizedType ->
            normalizedType as? SurfaceEnumType ?: return familyMismatch(SurfaceEnumMergeEnumsType(normalizedTypes), "Enum", normalizedType)
        }
        return mergeEnumEntriesStrict(SurfaceEnumMergeEnumsType(normalizedTypes), enums)
    }
    
    private fun normalizeType(currentType: SurfaceType): SurfaceType = when (currentType) {
        is SurfacePrimitiveType -> currentType
        is SurfaceWildcardType -> when (currentType) {
            is SurfaceAnyType -> currentType
            is SurfaceAnyTupleType -> SurfaceAnyTupleType()
            is SurfaceAnyStructType -> SurfaceAnyStructType()
            is SurfaceAnyObjectType -> SurfaceAnyObjectType()
            is SurfaceAnyEnumType -> SurfaceAnyEnumType()
            is SurfaceAnyOfType -> SurfaceAnyOfType(currentType.types.map { normalizeType(it) })
            is SurfaceAllOfType -> SurfaceAllOfType(currentType.types.map { normalizeType(it) })
            is SurfaceNoneOfType -> SurfaceNoneOfType(currentType.types.map { normalizeType(it) })
            is SurfaceAnyTupleOfType -> SurfaceAnyTupleOfType(normalizeType(currentType.component))
            is SurfaceAnyStructOfType -> SurfaceAnyStructOfType(currentType.fields.map { normalizeType(it) })
        }
        is SurfaceBuiltInType -> when (currentType) {
            is SurfaceTupleType -> currentType.components.map { normalizeType(it) }.toFoxTupleType()
            is SurfaceStructType -> SurfaceStructType(currentType.fields.mapValues { normalizeType(it.value) })
            is SurfaceObjectType -> SurfaceObjectType(currentType.members.mapValues { normalizeType(it.value) })
            is SurfaceEnumType -> SurfaceEnumType(currentType.entries.mapValues { normalizeType(it.value) })
            is SurfaceArrayType -> SurfaceArrayType(normalizeType(currentType.element))
            is SurfaceRefType -> SurfaceRefType(normalizeType(currentType.referent))
            is SurfaceMethodType -> SurfaceMethodType(
                normalizeType(currentType.`this`),
                currentType.parameters.mapValues { normalizeType(it.value) },
                normalizeType(currentType.`return`),
            )
        }
        is SurfaceTransformType -> when (currentType) {
            is SurfaceTupleGetComponentType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceTupleGetComponentType(normalizedBase, currentType.index)
                normalizeTupleTransform(transform, normalizedBase) { tuple ->
                    normalizeTupleIndex(transform, currentType.index, tuple.arity) { tuple.getComponent(currentType.index) }
                }
            }
            is SurfaceTupleGetComponentBackType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceTupleGetComponentBackType(normalizedBase, currentType.index)
                normalizeTupleTransform(transform, normalizedBase) { tuple ->
                    normalizeTupleIndex(transform, currentType.index, tuple.arity) { tuple.getComponentBack(currentType.index) }
                }
            }
            is SurfaceTupleGetFirstComponentsType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceTupleGetFirstComponentsType(normalizedBase, currentType.count)
                normalizeTupleTransform(transform, normalizedBase) { tuple ->
                    normalizeCountLoose(transform, currentType.count, tuple.arity) { count ->
                        tuple.getFirstComponents(count)
                    }
                }
            }
            is SurfaceTupleGetFirstComponentsExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceTupleGetFirstComponentsExactType(normalizedBase, currentType.count)
                normalizeTupleTransform(transform, normalizedBase) { tuple ->
                    normalizeCountExact(transform, currentType.count, tuple.arity) {
                        tuple.getFirstComponents(currentType.count)
                    }
                }
            }
            is SurfaceTupleGetLastComponentsType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceTupleGetLastComponentsType(normalizedBase, currentType.count)
                normalizeTupleTransform(transform, normalizedBase) { tuple ->
                    normalizeCountLoose(transform, currentType.count, tuple.arity) { count ->
                        tuple.getLastComponents(count)
                    }
                }
            }
            is SurfaceTupleGetLastComponentsExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceTupleGetLastComponentsExactType(normalizedBase, currentType.count)
                normalizeTupleTransform(transform, normalizedBase) { tuple ->
                    normalizeCountExact(transform, currentType.count, tuple.arity) {
                        tuple.getLastComponents(currentType.count)
                    }
                }
            }
            is SurfaceTupleDropFirstComponentsType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceTupleDropFirstComponentsType(normalizedBase, currentType.count)
                normalizeTupleTransform(transform, normalizedBase) { tuple ->
                    normalizeCountLoose(transform, currentType.count, tuple.arity) { count ->
                        tuple.dropFirstComponents(count)
                    }
                }
            }
            is SurfaceTupleDropFirstComponentsExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceTupleDropFirstComponentsExactType(normalizedBase, currentType.count)
                normalizeTupleTransform(transform, normalizedBase) { tuple ->
                    normalizeCountExact(transform, currentType.count, tuple.arity) {
                        tuple.dropFirstComponents(currentType.count)
                    }
                }
            }
            is SurfaceTupleDropLastComponentsType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceTupleDropLastComponentsType(normalizedBase, currentType.count)
                normalizeTupleTransform(transform, normalizedBase) { tuple ->
                    normalizeCountLoose(transform, currentType.count, tuple.arity) { count ->
                        tuple.dropLastComponents(count)
                    }
                }
            }
            is SurfaceTupleDropLastComponentsExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceTupleDropLastComponentsExactType(normalizedBase, currentType.count)
                normalizeTupleTransform(transform, normalizedBase) { tuple ->
                    normalizeCountExact(transform, currentType.count, tuple.arity) {
                        tuple.dropLastComponents(currentType.count)
                    }
                }
            }
            is SurfaceTupleMergeTuplesType -> normalizeMergeTuple(currentType.types)
            is SurfaceStructGetFieldTypeByNameType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructGetFieldTypeByNameType(normalizedBase, currentType.name)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    if (currentType.name !in struct.fields) nameNotFound(transform, currentType.name)
                    else struct.fields.getValue(currentType.name)
                }
            }
            is SurfaceStructGetFieldTypeByIndexType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructGetFieldTypeByIndexType(normalizedBase, currentType.index)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    normalizeStructIndex(transform, currentType.index, struct.arity) { struct.getFieldTypeByIndex(currentType.index).value }
                }
            }
            is SurfaceStructGetFieldTypeByIndexBackType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructGetFieldTypeByIndexBackType(normalizedBase, currentType.index)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    normalizeStructIndex(transform, currentType.index, struct.arity) { struct.getFieldTypeByIndexBack(currentType.index).value }
                }
            }
            is SurfaceStructGetFirstFieldsType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructGetFirstFieldsType(normalizedBase, currentType.count)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    normalizeCountLoose(transform, currentType.count, struct.arity) { count ->
                        struct.getFirstFields(count)
                    }
                }
            }
            is SurfaceStructGetFirstFieldsExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructGetFirstFieldsExactType(normalizedBase, currentType.count)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    normalizeCountExact(transform, currentType.count, struct.arity) {
                        struct.getFirstFields(currentType.count)
                    }
                }
            }
            is SurfaceStructGetLastFieldsType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructGetLastFieldsType(normalizedBase, currentType.count)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    normalizeCountLoose(transform, currentType.count, struct.arity) { count ->
                        struct.getLastFields(count)
                    }
                }
            }
            is SurfaceStructGetLastFieldsExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructGetLastFieldsExactType(normalizedBase, currentType.count)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    normalizeCountExact(transform, currentType.count, struct.arity) {
                        struct.getLastFields(currentType.count)
                    }
                }
            }
            is SurfaceStructDropFirstFieldsType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructDropFirstFieldsType(normalizedBase, currentType.count)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    normalizeCountLoose(transform, currentType.count, struct.arity) { count ->
                        struct.dropFirstFields(count)
                    }
                }
            }
            is SurfaceStructDropFirstFieldsExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructDropFirstFieldsExactType(normalizedBase, currentType.count)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    normalizeCountExact(transform, currentType.count, struct.arity) {
                        struct.dropFirstFields(currentType.count)
                    }
                }
            }
            is SurfaceStructDropLastFieldsType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructDropLastFieldsType(normalizedBase, currentType.count)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    normalizeCountLoose(transform, currentType.count, struct.arity) { count ->
                        struct.dropLastFields(count)
                    }
                }
            }
            is SurfaceStructDropLastFieldsExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructDropLastFieldsExactType(normalizedBase, currentType.count)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    normalizeCountExact(transform, currentType.count, struct.arity) {
                        struct.dropLastFields(currentType.count)
                    }
                }
            }
            is SurfaceStructSelectFieldsType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructSelectFieldsType(normalizedBase, currentType.names)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    struct.selectFields(currentType.names)
                }
            }
            is SurfaceStructSelectFieldsExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructSelectFieldsExactType(normalizedBase, currentType.names)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    normalizeStructNameSelectionExact(transform, struct, currentType.names)
                }
            }
            is SurfaceStructDropFieldsType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructDropFieldsType(normalizedBase, currentType.names)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    struct.dropFields(currentType.names)
                }
            }
            is SurfaceStructDropFieldsExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructDropFieldsExactType(normalizedBase, currentType.names)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    normalizeStructNameDropExact(transform, struct, currentType.names)
                }
            }
            is SurfaceStructExtractFieldTypesType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceStructExtractFieldTypesType(normalizedBase)
                normalizeStructTransform(transform, normalizedBase) { struct ->
                    struct.fields.values.toFoxTupleType()
                }
            }
            is SurfaceStructMergeStructsType -> normalizeMergeStruct(currentType.types)
            is SurfaceObjectGetMemberTypeType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceObjectGetMemberTypeType(normalizedBase, currentType.name)
                normalizeObjectTransform(transform, normalizedBase) { obj ->
                    if (currentType.name !in obj.members) nameNotFound(transform, currentType.name)
                    else obj.members.getValue(currentType.name)
                }
            }
            is SurfaceObjectSelectMembersType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceObjectSelectMembersType(normalizedBase, currentType.names)
                normalizeObjectTransform(transform, normalizedBase) { obj ->
                    obj.selectMembers(currentType.names)
                }
            }
            is SurfaceObjectSelectMembersExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceObjectSelectMembersExactType(normalizedBase, currentType.names)
                normalizeObjectTransform(transform, normalizedBase) { obj ->
                    normalizeObjectNameSelectionExact(transform, obj, currentType.names)
                }
            }
            is SurfaceObjectDropMembersType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceObjectDropMembersType(normalizedBase, currentType.names)
                normalizeObjectTransform(transform, normalizedBase) { obj ->
                    obj.dropMembers(currentType.names)
                }
            }
            is SurfaceObjectDropMembersExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceObjectDropMembersExactType(normalizedBase, currentType.names)
                normalizeObjectTransform(transform, normalizedBase) { obj ->
                    normalizeObjectNameDropExact(transform, obj, currentType.names)
                }
            }
            is SurfaceObjectMergeObjectsType -> normalizeMergeObject(currentType.types)
            is SurfaceEnumGetEntryTypeType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceEnumGetEntryTypeType(normalizedBase, currentType.name)
                normalizeEnumTransform(transform, normalizedBase) { enumType ->
                    if (currentType.name !in enumType.entries) nameNotFound(transform, currentType.name)
                    else enumType.entries.getValue(currentType.name)
                }
            }
            is SurfaceEnumSelectEntriesType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceEnumSelectEntriesType(normalizedBase, currentType.names)
                normalizeEnumTransform(transform, normalizedBase) { enumType ->
                    enumType.selectEntries(currentType.names)
                }
            }
            is SurfaceEnumSelectEntriesExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceEnumSelectEntriesExactType(normalizedBase, currentType.names)
                normalizeEnumTransform(transform, normalizedBase) { enumType ->
                    normalizeEnumNameSelectionExact(transform, enumType, currentType.names)
                }
            }
            is SurfaceEnumDropEntriesType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceEnumDropEntriesType(normalizedBase, currentType.names)
                normalizeEnumTransform(transform, normalizedBase) { enumType ->
                    enumType.dropEntries(currentType.names)
                }
            }
            is SurfaceEnumDropEntriesExactType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceEnumDropEntriesExactType(normalizedBase, currentType.names)
                normalizeEnumTransform(transform, normalizedBase) { enumType ->
                    normalizeEnumNameDropExact(transform, enumType, currentType.names)
                }
            }
            is SurfaceEnumMergeEnumsType -> normalizeMergeEnum(currentType.types)
            is SurfaceArrayGetElementTypeType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceArrayGetElementTypeType(normalizedBase)
                normalizeArrayTransform(transform, normalizedBase) { array -> array.element }
            }
            is SurfaceRefGetReferentTypeType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceRefGetReferentTypeType(normalizedBase)
                normalizeRefTransform(transform, normalizedBase) { ref -> ref.referent }
            }
            is SurfaceMethodGetThisTypeType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceMethodGetThisTypeType(normalizedBase)
                normalizeMethodTransform(transform, normalizedBase) { method -> method.`this` }
            }
            is SurfaceMethodGetParameterStructType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceMethodGetParameterStructType(normalizedBase)
                normalizeMethodTransform(transform, normalizedBase) { method ->
                    SurfaceStructType(method.parameters)
                }
            }
            is SurfaceMethodGetReturnTypeType -> {
                val normalizedBase = normalizeType(currentType.type)
                val transform = SurfaceMethodGetReturnTypeType(normalizedBase)
                normalizeMethodTransform(transform, normalizedBase) { method -> method.`return` }
            }
            is SurfaceMethodOfType -> {
                val normalizedThis = normalizeType(currentType.`this`)
                val normalizedParameters = normalizeType(currentType.parameters)
                val normalizedReturn = normalizeType(currentType.`return`)
                val transform = SurfaceMethodOfType(normalizedThis, normalizedParameters, normalizedReturn)
                normalizeMethodOf(transform, normalizedThis, normalizedParameters, normalizedReturn) { `this`, parameters, `return` ->
                    SurfaceMethodType(`this`, parameters.fields, `return`)
                }
            }
        }
        is SurfaceUnresolvedType -> currentType.also { check(currentType.parameters == null) }
        is SurfacePlaceholderType -> error("unreachable")
    }
}
