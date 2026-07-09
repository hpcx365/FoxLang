package pers.hpcx.foxlang.pipeline

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.*
import pers.hpcx.foxlang.utils.MutableOrderedMap
import pers.hpcx.foxlang.utils.mapValues
import pers.hpcx.foxlang.utils.mutableOrderedMapOf

sealed interface TypeNormalizationResult
data class TypeNormalizationSuccess(val type: FoxType) : TypeNormalizationResult
data class TypeNormalizationFailure(val errors: List<TypeNormalizationError>) : TypeNormalizationResult

sealed interface TypeNormalizationError
data class TypeNormalizationFamilyMismatch(
    val transform: FoxType,
    val expectedFamily: String,
    val actualType: FoxType,
) : TypeNormalizationError

data class TypeNormalizationIndexOutOfBounds(
    val transform: FoxType,
    val index: Int,
    val size: Int,
) : TypeNormalizationError

data class TypeNormalizationNameNotFound(
    val transform: FoxType,
    val name: String,
) : TypeNormalizationError

data class TypeNormalizationDuplicateName(
    val transform: FoxType,
    val name: String,
) : TypeNormalizationError

fun runTypeNormalization(type: FoxType): TypeNormalizationResult {
    val errors = mutableListOf<TypeNormalizationError>()
    lateinit var normalizeType: (FoxType) -> FoxType
    
    fun familyMismatch(transform: FoxType, expectedFamily: String, actualType: FoxType): FoxType {
        errors += TypeNormalizationFamilyMismatch(transform, expectedFamily, actualType)
        return transform
    }
    
    fun indexOutOfBounds(transform: FoxType, index: Int, size: Int): FoxType {
        errors += TypeNormalizationIndexOutOfBounds(transform, index, size)
        return transform
    }
    
    fun nameNotFound(transform: FoxType, name: String): FoxType {
        errors += TypeNormalizationNameNotFound(transform, name)
        return transform
    }
    
    fun duplicateName(transform: FoxType, name: String): FoxType {
        errors += TypeNormalizationDuplicateName(transform, name)
        return transform
    }
    
    fun normalizeTupleTransform(
        transform: FoxType,
        normalizedType: FoxType,
        action: (FoxTupleType) -> FoxType,
    ): FoxType = when (normalizedType) {
        is FoxTupleType -> action(normalizedType)
        is FoxUnresolvedType, is FoxTransformType -> transform
        else -> familyMismatch(transform, "Tuple", normalizedType)
    }
    
    fun normalizeStructTransform(
        transform: FoxType,
        normalizedType: FoxType,
        action: (FoxStructType) -> FoxType,
    ): FoxType = when (normalizedType) {
        is FoxStructType -> action(normalizedType)
        is FoxUnresolvedType, is FoxTransformType -> transform
        else -> familyMismatch(transform, "Struct", normalizedType)
    }
    
    fun normalizeObjectTransform(
        transform: FoxType,
        normalizedType: FoxType,
        action: (FoxObjectType) -> FoxType,
    ): FoxType = when (normalizedType) {
        is FoxObjectType -> action(normalizedType)
        is FoxUnresolvedType, is FoxTransformType -> transform
        else -> familyMismatch(transform, "Object", normalizedType)
    }
    
    fun normalizeEnumTransform(
        transform: FoxType,
        normalizedType: FoxType,
        action: (FoxEnumType) -> FoxType,
    ): FoxType = when (normalizedType) {
        is FoxEnumType -> action(normalizedType)
        is FoxUnresolvedType, is FoxTransformType -> transform
        else -> familyMismatch(transform, "Enum", normalizedType)
    }
    
    fun normalizeArrayTransform(
        transform: FoxType,
        normalizedType: FoxType,
        action: (FoxArrayType) -> FoxType,
    ): FoxType = when (normalizedType) {
        is FoxArrayType -> action(normalizedType)
        is FoxUnresolvedType, is FoxTransformType -> transform
        else -> familyMismatch(transform, "Array", normalizedType)
    }
    
    fun normalizeRefTransform(
        transform: FoxType,
        normalizedType: FoxType,
        action: (FoxRefType) -> FoxType,
    ): FoxType = when (normalizedType) {
        is FoxRefType -> action(normalizedType)
        is FoxUnresolvedType, is FoxTransformType -> transform
        else -> familyMismatch(transform, "Ref", normalizedType)
    }
    
    fun normalizeMethodOf(
        transform: FoxType,
        normalizedThis: FoxType,
        normalizedParameters: FoxType,
        normalizedReturn: FoxType,
        action: (FoxType, FoxStructType, FoxType) -> FoxType,
    ): FoxType = when (normalizedParameters) {
        is FoxStructType -> action(normalizedThis, normalizedParameters, normalizedReturn)
        is FoxUnresolvedType, is FoxTransformType -> transform
        else -> familyMismatch(transform, "Struct", normalizedParameters)
    }
    
    fun normalizeMethodTransform(
        transform: FoxType,
        normalizedType: FoxType,
        action: (FoxMethodType) -> FoxType,
    ): FoxType = when (normalizedType) {
        is FoxMethodType -> action(normalizedType)
        is FoxUnresolvedType, is FoxTransformType -> transform
        else -> familyMismatch(transform, "Method", normalizedType)
    }
    
    fun normalizeStructNameSelection(
        transform: FoxType,
        struct: FoxStructType,
        names: Iterable<String>,
    ): FoxType {
        names.forEach { name ->
            if (name !in struct.fields) return nameNotFound(transform, name)
        }
        return struct.selectFields(names)
    }
    
    fun normalizeObjectNameSelection(
        transform: FoxType,
        obj: FoxObjectType,
        names: Iterable<String>,
    ): FoxType {
        names.forEach { name ->
            if (name !in obj.members) return nameNotFound(transform, name)
        }
        return obj.selectMembers(names)
    }
    
    fun normalizeEnumNameSelection(
        transform: FoxType,
        enumType: FoxEnumType,
        names: Iterable<String>,
    ): FoxType {
        names.forEach { name ->
            if (name !in enumType.entries) return nameNotFound(transform, name)
        }
        return enumType.selectEntries(names)
    }
    
    fun normalizeStructNameDrop(
        transform: FoxType,
        struct: FoxStructType,
        names: Iterable<String>,
    ): FoxType {
        names.forEach { name ->
            if (name !in struct.fields) return nameNotFound(transform, name)
        }
        return struct.dropFields(names)
    }
    
    fun normalizeObjectNameDrop(
        transform: FoxType,
        obj: FoxObjectType,
        names: Iterable<String>,
    ): FoxType {
        names.forEach { name ->
            if (name !in obj.members) return nameNotFound(transform, name)
        }
        return obj.dropMembers(names)
    }
    
    fun normalizeEnumNameDrop(
        transform: FoxType,
        enumType: FoxEnumType,
        names: Iterable<String>,
    ): FoxType {
        names.forEach { name ->
            if (name !in enumType.entries) return nameNotFound(transform, name)
        }
        return enumType.dropEntries(names)
    }
    
    fun normalizeTupleCount(transform: FoxType, count: Int, size: Int, action: () -> FoxType): FoxType {
        if (count !in 0..size) return indexOutOfBounds(transform, count, size)
        return action()
    }
    
    fun normalizeTupleIndex(transform: FoxType, index: Int, size: Int, action: () -> FoxType): FoxType {
        if (index !in 0 until size) return indexOutOfBounds(transform, index, size)
        return action()
    }
    
    fun normalizeStructIndex(transform: FoxType, index: Int, size: Int, action: () -> FoxType): FoxType {
        if (index !in 0 until size) return indexOutOfBounds(transform, index, size)
        return action()
    }
    
    fun mergeStructFieldsStrict(transform: FoxType, structs: List<FoxStructType>): FoxType {
        val result: MutableOrderedMap<String, FoxType> = mutableOrderedMapOf()
        structs.forEach { struct ->
            struct.fields.entries.forEach { (name, fieldType) ->
                if (name in result) return duplicateName(transform, name)
                result[name] = fieldType
            }
        }
        return FoxStructType(result)
    }
    
    fun mergeObjectMembersStrict(transform: FoxType, objects: List<FoxObjectType>): FoxType {
        val result = LinkedHashMap<String, FoxType>()
        objects.forEach { obj ->
            obj.members.forEach { (name, memberType) ->
                if (name in result) return duplicateName(transform, name)
                result[name] = memberType
            }
        }
        return FoxObjectType(result)
    }
    
    fun mergeEnumEntriesStrict(transform: FoxType, enums: List<FoxEnumType>): FoxType {
        val result = LinkedHashMap<String, FoxType>()
        enums.forEach { enumType ->
            enumType.entries.forEach { (name, itemType) ->
                if (name in result) return duplicateName(transform, name)
                result[name] = itemType
            }
        }
        return FoxEnumType(result)
    }
    
    fun normalizeMergeTuple(types: List<FoxType>): FoxType {
        val normalizedTypes = types.map { normalizeType(it) }
        if (errors.isNotEmpty()) return FoxTupleMergeComponentsOfType(normalizedTypes)
        if (normalizedTypes.any { it is FoxUnresolvedType || it is FoxTransformType }) {
            return FoxTupleMergeComponentsOfType(normalizedTypes)
        }
        val tuples = normalizedTypes.map { normalizedType ->
            when (normalizedType) {
                is FoxTupleType -> normalizedType
                else -> return familyMismatch(FoxTupleMergeComponentsOfType(normalizedTypes), "Tuple", normalizedType)
            }
        }
        return tuples.mergeTupleComponents()
    }
    
    fun normalizeMergeStruct(types: List<FoxType>): FoxType {
        val normalizedTypes = types.map { normalizeType(it) }
        if (errors.isNotEmpty()) return FoxStructMergeFieldsOfType(normalizedTypes)
        if (normalizedTypes.any { it is FoxUnresolvedType || it is FoxTransformType }) {
            return FoxStructMergeFieldsOfType(normalizedTypes)
        }
        val structs = normalizedTypes.map { normalizedType ->
            when (normalizedType) {
                is FoxStructType -> normalizedType
                else -> return familyMismatch(FoxStructMergeFieldsOfType(normalizedTypes), "Struct", normalizedType)
            }
        }
        return mergeStructFieldsStrict(FoxStructMergeFieldsOfType(normalizedTypes), structs)
    }
    
    fun normalizeMergeObject(types: List<FoxType>): FoxType {
        val normalizedTypes = types.map { normalizeType(it) }
        if (errors.isNotEmpty()) return FoxObjectMergeMembersOfType(normalizedTypes)
        if (normalizedTypes.any { it is FoxUnresolvedType || it is FoxTransformType }) {
            return FoxObjectMergeMembersOfType(normalizedTypes)
        }
        val objects = normalizedTypes.map { normalizedType ->
            when (normalizedType) {
                is FoxObjectType -> normalizedType
                else -> return familyMismatch(FoxObjectMergeMembersOfType(normalizedTypes), "Object", normalizedType)
            }
        }
        return mergeObjectMembersStrict(FoxObjectMergeMembersOfType(normalizedTypes), objects)
    }
    
    fun normalizeMergeEnum(types: List<FoxType>): FoxType {
        val normalizedTypes = types.map { normalizeType(it) }
        if (errors.isNotEmpty()) return FoxEnumMergeEntriesOfType(normalizedTypes)
        if (normalizedTypes.any { it is FoxUnresolvedType || it is FoxTransformType }) {
            return FoxEnumMergeEntriesOfType(normalizedTypes)
        }
        val enums = normalizedTypes.map { normalizedType ->
            when (normalizedType) {
                is FoxEnumType -> normalizedType
                else -> return familyMismatch(FoxEnumMergeEntriesOfType(normalizedTypes), "Enum", normalizedType)
            }
        }
        return mergeEnumEntriesStrict(FoxEnumMergeEntriesOfType(normalizedTypes), enums)
    }
    
    normalizeType = { currentType ->
        when (currentType) {
            is FoxPrimitiveType -> currentType
            is FoxWildcardType -> error("unreachable")
            is FoxPlaceholderType -> error("unreachable")
            is FoxTupleType -> currentType.components.map(normalizeType).toFoxTupleType()
            is FoxStructType -> FoxStructType(currentType.fields.mapValues { normalizeType(it.value) })
            is FoxObjectType -> FoxObjectType(currentType.members.mapValues { normalizeType(it.value) })
            is FoxEnumType -> FoxEnumType(currentType.entries.mapValues { normalizeType(it.value) })
            is FoxArrayType -> FoxArrayType(normalizeType(currentType.element))
            is FoxRefType -> FoxRefType(normalizeType(currentType.referent))
            is FoxMethodType -> FoxMethodType(
                normalizeType(currentType.`this`),
                currentType.parameters.mapValues { normalizeType(it.value) },
                normalizeType(currentType.`return`),
            )
            is FoxUnresolvedType -> FoxUnresolvedType(currentType.name, currentType.parameters?.map { normalizeType(it) })
            is FoxTransformType -> when (currentType) {
                is FoxTupleComponentAtType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleComponentAtType(normalizedBase, currentType.index)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeTupleIndex(transform, currentType.index, tuple.arity) { tuple.componentAt(currentType.index) }
                    }
                }
                is FoxTupleLastComponentAtType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleLastComponentAtType(normalizedBase, currentType.index)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeTupleIndex(transform, currentType.index, tuple.arity) { tuple.lastComponentAt(currentType.index) }
                    }
                }
                is FoxTupleFirstComponentsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleFirstComponentsOfType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeTupleCount(transform, currentType.count, tuple.arity) { tuple.firstComponents(currentType.count) }
                    }
                }
                is FoxTupleExactFirstComponentsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleExactFirstComponentsOfType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeTupleCount(transform, currentType.count, tuple.arity) { tuple.firstComponents(currentType.count) }
                    }
                }
                is FoxTupleLastComponentsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleLastComponentsOfType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeTupleCount(transform, currentType.count, tuple.arity) { tuple.lastComponents(currentType.count) }
                    }
                }
                is FoxTupleExactLastComponentsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleExactLastComponentsOfType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeTupleCount(transform, currentType.count, tuple.arity) { tuple.lastComponents(currentType.count) }
                    }
                }
                is FoxTupleDropFirstComponentsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleDropFirstComponentsOfType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeTupleCount(transform, currentType.count, tuple.arity) { tuple.dropFirstComponents(currentType.count) }
                    }
                }
                is FoxTupleExactDropFirstComponentsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleExactDropFirstComponentsOfType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeTupleCount(transform, currentType.count, tuple.arity) { tuple.dropFirstComponents(currentType.count) }
                    }
                }
                is FoxTupleDropLastComponentsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleDropLastComponentsOfType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeTupleCount(transform, currentType.count, tuple.arity) { tuple.dropLastComponents(currentType.count) }
                    }
                }
                is FoxTupleExactDropLastComponentsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleExactDropLastComponentsOfType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeTupleCount(transform, currentType.count, tuple.arity) { tuple.dropLastComponents(currentType.count) }
                    }
                }
                is FoxTupleMergeComponentsOfType -> normalizeMergeTuple(currentType.types)
                is FoxStructFieldOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructFieldOfType(normalizedBase, currentType.name)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        if (currentType.name !in struct.fields) nameNotFound(transform, currentType.name)
                        else struct.fields.getValue(currentType.name)
                    }
                }
                is FoxStructFieldAtType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructFieldAtType(normalizedBase, currentType.index)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeStructIndex(transform, currentType.index, struct.arity) { struct.fieldAt(currentType.index).value }
                    }
                }
                is FoxStructLastFieldAtType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructLastFieldAtType(normalizedBase, currentType.index)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeStructIndex(transform, currentType.index, struct.arity) { struct.lastFieldAt(currentType.index).value }
                    }
                }
                is FoxStructFirstFieldsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructFirstFieldsOfType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeTupleCount(transform, currentType.count, struct.arity) { struct.firstFields(currentType.count) }
                    }
                }
                is FoxStructExactFirstFieldsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructExactFirstFieldsOfType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeTupleCount(transform, currentType.count, struct.arity) { struct.firstFields(currentType.count) }
                    }
                }
                is FoxStructLastFieldsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructLastFieldsOfType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeTupleCount(transform, currentType.count, struct.arity) { struct.lastFields(currentType.count) }
                    }
                }
                is FoxStructExactLastFieldsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructExactLastFieldsOfType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeTupleCount(transform, currentType.count, struct.arity) { struct.lastFields(currentType.count) }
                    }
                }
                is FoxStructDropFirstFieldsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructDropFirstFieldsOfType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeTupleCount(transform, currentType.count, struct.arity) { struct.dropFirstFields(currentType.count) }
                    }
                }
                is FoxStructExactDropFirstFieldsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructExactDropFirstFieldsOfType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeTupleCount(transform, currentType.count, struct.arity) { struct.dropFirstFields(currentType.count) }
                    }
                }
                is FoxStructDropLastFieldsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructDropLastFieldsOfType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeTupleCount(transform, currentType.count, struct.arity) { struct.dropLastFields(currentType.count) }
                    }
                }
                is FoxStructExactDropLastFieldsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructExactDropLastFieldsOfType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeTupleCount(transform, currentType.count, struct.arity) { struct.dropLastFields(currentType.count) }
                    }
                }
                is FoxStructFieldsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructFieldsOfType(normalizedBase, currentType.names)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeStructNameSelection(transform, struct, currentType.names)
                    }
                }
                is FoxStructDropFieldsOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructDropFieldsOfType(normalizedBase, currentType.names)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeStructNameDrop(transform, struct, currentType.names)
                    }
                }
                is FoxStructMergeFieldsOfType -> normalizeMergeStruct(currentType.types)
                is FoxObjectMemberOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxObjectMemberOfType(normalizedBase, currentType.name)
                    normalizeObjectTransform(transform, normalizedBase) { obj ->
                        if (currentType.name !in obj.members) nameNotFound(transform, currentType.name)
                        else obj.members.getValue(currentType.name)
                    }
                }
                is FoxObjectMembersOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxObjectMembersOfType(normalizedBase, currentType.names)
                    normalizeObjectTransform(transform, normalizedBase) { obj ->
                        normalizeObjectNameSelection(transform, obj, currentType.names)
                    }
                }
                is FoxObjectDropMembersOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxObjectDropMembersOfType(normalizedBase, currentType.names)
                    normalizeObjectTransform(transform, normalizedBase) { obj ->
                        normalizeObjectNameDrop(transform, obj, currentType.names)
                    }
                }
                is FoxObjectMergeMembersOfType -> normalizeMergeObject(currentType.types)
                is FoxEnumEntryOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxEnumEntryOfType(normalizedBase, currentType.name)
                    normalizeEnumTransform(transform, normalizedBase) { enumType ->
                        if (currentType.name !in enumType.entries) nameNotFound(transform, currentType.name)
                        else enumType.entries.getValue(currentType.name)
                    }
                }
                is FoxEnumEntriesOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxEnumEntriesOfType(normalizedBase, currentType.names)
                    normalizeEnumTransform(transform, normalizedBase) { enumType ->
                        normalizeEnumNameSelection(transform, enumType, currentType.names)
                    }
                }
                is FoxEnumDropEntriesOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxEnumDropEntriesOfType(normalizedBase, currentType.names)
                    normalizeEnumTransform(transform, normalizedBase) { enumType ->
                        normalizeEnumNameDrop(transform, enumType, currentType.names)
                    }
                }
                is FoxEnumMergeEntriesOfType -> normalizeMergeEnum(currentType.types)
                is FoxArrayElementOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxArrayElementOfType(normalizedBase)
                    normalizeArrayTransform(transform, normalizedBase) { array -> array.element }
                }
                is FoxRefReferentOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxRefReferentOfType(normalizedBase)
                    normalizeRefTransform(transform, normalizedBase) { ref -> ref.referent }
                }
                is FoxMethodOfType -> {
                    val normalizedThis = normalizeType(currentType.`this`)
                    val normalizedParameters = normalizeType(currentType.parameters)
                    val normalizedReturn = normalizeType(currentType.`return`)
                    val transform = FoxMethodOfType(normalizedThis, normalizedParameters, normalizedReturn)
                    normalizeMethodOf(transform, normalizedThis, normalizedParameters, normalizedReturn) { `this`, parameters, `return` ->
                        FoxMethodType(`this`, parameters.fields, `return`)
                    }
                }
                is FoxMethodThisOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxMethodThisOfType(normalizedBase)
                    normalizeMethodTransform(transform, normalizedBase) { method -> method.`this` }
                }
                is FoxMethodParametersOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxMethodParametersOfType(normalizedBase)
                    normalizeMethodTransform(transform, normalizedBase) { method ->
                        FoxStructType(method.parameters)
                    }
                }
                is FoxMethodReturnOfType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxMethodReturnOfType(normalizedBase)
                    normalizeMethodTransform(transform, normalizedBase) { method -> method.`return` }
                }
            }
        }
    }
    
    val normalizedType = normalizeType(type)
    if (errors.isNotEmpty()) return TypeNormalizationFailure(errors)
    return TypeNormalizationSuccess(normalizedType)
}
