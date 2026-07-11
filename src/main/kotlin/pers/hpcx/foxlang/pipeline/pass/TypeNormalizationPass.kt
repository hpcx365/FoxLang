package pers.hpcx.foxlang.pipeline.pass

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
    
    fun normalizeCountLoose(transform: FoxType, count: Int, size: Int, action: (Int) -> FoxType): FoxType {
        return if (count < 0) indexOutOfBounds(transform, count, size) else action(minOf(count, size))
    }
    
    fun normalizeStructNameSelectionExact(transform: FoxType, struct: FoxStructType, names: Set<String>): FoxType {
        names.forEach { name -> if (name !in struct.fields) return nameNotFound(transform, name) }
        return struct.selectFields(names)
    }
    
    fun normalizeObjectNameSelectionExact(transform: FoxType, obj: FoxObjectType, names: Set<String>): FoxType {
        names.forEach { name -> if (name !in obj.members) return nameNotFound(transform, name) }
        return obj.selectMembers(names)
    }
    
    fun normalizeEnumNameSelectionExact(transform: FoxType, enumType: FoxEnumType, names: Set<String>): FoxType {
        names.forEach { name -> if (name !in enumType.entries) return nameNotFound(transform, name) }
        return enumType.selectEntries(names)
    }
    
    fun normalizeStructNameDropExact(transform: FoxType, struct: FoxStructType, names: Set<String>): FoxType {
        names.forEach { name -> if (name !in struct.fields) return nameNotFound(transform, name) }
        return struct.dropFields(names)
    }
    
    fun normalizeObjectNameDropExact(transform: FoxType, obj: FoxObjectType, names: Set<String>): FoxType {
        names.forEach { name -> if (name !in obj.members) return nameNotFound(transform, name) }
        return obj.dropMembers(names)
    }
    
    fun normalizeEnumNameDropExact(transform: FoxType, enumType: FoxEnumType, names: Set<String>): FoxType {
        names.forEach { name -> if (name !in enumType.entries) return nameNotFound(transform, name) }
        return enumType.dropEntries(names)
    }
    
    fun normalizeCountExact(transform: FoxType, count: Int, size: Int, action: () -> FoxType): FoxType {
        return if (count !in 0..size) indexOutOfBounds(transform, count, size) else action()
    }
    
    fun normalizeTupleIndex(transform: FoxType, index: Int, size: Int, action: () -> FoxType): FoxType {
        return if (index !in 0..<size) indexOutOfBounds(transform, index, size) else action()
    }
    
    fun normalizeStructIndex(transform: FoxType, index: Int, size: Int, action: () -> FoxType): FoxType {
        return if (index !in 0..<size) indexOutOfBounds(transform, index, size) else action()
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
        if (errors.isNotEmpty()) return FoxTupleMergeTuplesType(normalizedTypes)
        if (normalizedTypes.any { it is FoxUnresolvedType || it is FoxTransformType }) {
            return FoxTupleMergeTuplesType(normalizedTypes)
        }
        val tuples = normalizedTypes.map { normalizedType ->
            normalizedType as? FoxTupleType ?: return familyMismatch(FoxTupleMergeTuplesType(normalizedTypes), "Tuple", normalizedType)
        }
        return tuples.mergeTuples()
    }
    
    fun normalizeMergeStruct(types: List<FoxType>): FoxType {
        val normalizedTypes = types.map { normalizeType(it) }
        if (errors.isNotEmpty()) return FoxStructMergeStructsType(normalizedTypes)
        if (normalizedTypes.any { it is FoxUnresolvedType || it is FoxTransformType }) {
            return FoxStructMergeStructsType(normalizedTypes)
        }
        val structs = normalizedTypes.map { normalizedType ->
            normalizedType as? FoxStructType ?: return familyMismatch(FoxStructMergeStructsType(normalizedTypes), "Struct", normalizedType)
        }
        return mergeStructFieldsStrict(FoxStructMergeStructsType(normalizedTypes), structs)
    }
    
    fun normalizeMergeObject(types: List<FoxType>): FoxType {
        val normalizedTypes = types.map { normalizeType(it) }
        if (errors.isNotEmpty()) return FoxObjectMergeObjectsType(normalizedTypes)
        if (normalizedTypes.any { it is FoxUnresolvedType || it is FoxTransformType }) {
            return FoxObjectMergeObjectsType(normalizedTypes)
        }
        val objects = normalizedTypes.map { normalizedType ->
            normalizedType as? FoxObjectType ?: return familyMismatch(FoxObjectMergeObjectsType(normalizedTypes), "Object", normalizedType)
        }
        return mergeObjectMembersStrict(FoxObjectMergeObjectsType(normalizedTypes), objects)
    }
    
    fun normalizeMergeEnum(types: List<FoxType>): FoxType {
        val normalizedTypes = types.map { normalizeType(it) }
        if (errors.isNotEmpty()) return FoxEnumMergeEnumsType(normalizedTypes)
        if (normalizedTypes.any { it is FoxUnresolvedType || it is FoxTransformType }) {
            return FoxEnumMergeEnumsType(normalizedTypes)
        }
        val enums = normalizedTypes.map { normalizedType ->
            normalizedType as? FoxEnumType ?: return familyMismatch(FoxEnumMergeEnumsType(normalizedTypes), "Enum", normalizedType)
        }
        return mergeEnumEntriesStrict(FoxEnumMergeEnumsType(normalizedTypes), enums)
    }
    
    normalizeType = { currentType ->
        when (currentType) {
            is FoxPrimitiveType -> currentType
            is FoxWildcardType -> when (currentType) {
                FoxAnyType -> currentType
                is FoxAnyOfType -> FoxAnyOfType(currentType.types.map { normalizeType(it) })
                is FoxAllOfType -> FoxAllOfType(currentType.types.map { normalizeType(it) })
                is FoxNoneOfType -> FoxNoneOfType(currentType.types.map { normalizeType(it) })
                FoxAnyTupleType -> FoxAnyTupleType
                is FoxAnyTupleOfType -> FoxAnyTupleOfType(normalizeType(currentType.component))
                FoxAnyStructType -> FoxAnyStructType
                is FoxAnyStructOfType -> FoxAnyStructOfType(currentType.fields.map { normalizeType(it) })
                FoxAnyObjectType -> FoxAnyObjectType
                FoxAnyEnumType -> FoxAnyEnumType
            }
            is FoxBuiltInType -> when (currentType) {
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
            }
            is FoxTransformType -> when (currentType) {
                is FoxTupleGetComponentType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleGetComponentType(normalizedBase, currentType.index)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeTupleIndex(transform, currentType.index, tuple.arity) { tuple.getComponent(currentType.index) }
                    }
                }
                is FoxTupleGetComponentBackType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleGetComponentBackType(normalizedBase, currentType.index)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeTupleIndex(transform, currentType.index, tuple.arity) { tuple.getComponentBack(currentType.index) }
                    }
                }
                is FoxTupleGetFirstComponentsType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleGetFirstComponentsType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeCountLoose(transform, currentType.count, tuple.arity) { count ->
                            tuple.getFirstComponents(count)
                        }
                    }
                }
                is FoxTupleGetFirstComponentsExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleGetFirstComponentsExactType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeCountExact(transform, currentType.count, tuple.arity) {
                            tuple.getFirstComponents(currentType.count)
                        }
                    }
                }
                is FoxTupleGetLastComponentsType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleGetLastComponentsType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeCountLoose(transform, currentType.count, tuple.arity) { count ->
                            tuple.getLastComponents(count)
                        }
                    }
                }
                is FoxTupleGetLastComponentsExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleGetLastComponentsExactType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeCountExact(transform, currentType.count, tuple.arity) {
                            tuple.getLastComponents(currentType.count)
                        }
                    }
                }
                is FoxTupleDropFirstComponentsType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleDropFirstComponentsType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeCountLoose(transform, currentType.count, tuple.arity) { count ->
                            tuple.dropFirstComponents(count)
                        }
                    }
                }
                is FoxTupleDropFirstComponentsExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleDropFirstComponentsExactType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeCountExact(transform, currentType.count, tuple.arity) {
                            tuple.dropFirstComponents(currentType.count)
                        }
                    }
                }
                is FoxTupleDropLastComponentsType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleDropLastComponentsType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeCountLoose(transform, currentType.count, tuple.arity) { count ->
                            tuple.dropLastComponents(count)
                        }
                    }
                }
                is FoxTupleDropLastComponentsExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxTupleDropLastComponentsExactType(normalizedBase, currentType.count)
                    normalizeTupleTransform(transform, normalizedBase) { tuple ->
                        normalizeCountExact(transform, currentType.count, tuple.arity) {
                            tuple.dropLastComponents(currentType.count)
                        }
                    }
                }
                is FoxTupleMergeTuplesType -> normalizeMergeTuple(currentType.types)
                is FoxStructGetFieldTypeByNameType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructGetFieldTypeByNameType(normalizedBase, currentType.name)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        if (currentType.name !in struct.fields) nameNotFound(transform, currentType.name)
                        else struct.fields.getValue(currentType.name)
                    }
                }
                is FoxStructGetFieldTypeByIndexType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructGetFieldTypeByIndexType(normalizedBase, currentType.index)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeStructIndex(transform, currentType.index, struct.arity) { struct.getFieldTypeByIndex(currentType.index).value }
                    }
                }
                is FoxStructGetFieldTypeByIndexBackType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructGetFieldTypeByIndexBackType(normalizedBase, currentType.index)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeStructIndex(transform, currentType.index, struct.arity) { struct.getFieldTypeByIndexBack(currentType.index).value }
                    }
                }
                is FoxStructGetFirstFieldsType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructGetFirstFieldsType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeCountLoose(transform, currentType.count, struct.arity) { count ->
                            struct.getFirstFields(count)
                        }
                    }
                }
                is FoxStructGetFirstFieldsExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructGetFirstFieldsExactType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeCountExact(transform, currentType.count, struct.arity) {
                            struct.getFirstFields(currentType.count)
                        }
                    }
                }
                is FoxStructGetLastFieldsType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructGetLastFieldsType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeCountLoose(transform, currentType.count, struct.arity) { count ->
                            struct.getLastFields(count)
                        }
                    }
                }
                is FoxStructGetLastFieldsExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructGetLastFieldsExactType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeCountExact(transform, currentType.count, struct.arity) {
                            struct.getLastFields(currentType.count)
                        }
                    }
                }
                is FoxStructDropFirstFieldsType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructDropFirstFieldsType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeCountLoose(transform, currentType.count, struct.arity) { count ->
                            struct.dropFirstFields(count)
                        }
                    }
                }
                is FoxStructDropFirstFieldsExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructDropFirstFieldsExactType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeCountExact(transform, currentType.count, struct.arity) {
                            struct.dropFirstFields(currentType.count)
                        }
                    }
                }
                is FoxStructDropLastFieldsType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructDropLastFieldsType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeCountLoose(transform, currentType.count, struct.arity) { count ->
                            struct.dropLastFields(count)
                        }
                    }
                }
                is FoxStructDropLastFieldsExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructDropLastFieldsExactType(normalizedBase, currentType.count)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeCountExact(transform, currentType.count, struct.arity) {
                            struct.dropLastFields(currentType.count)
                        }
                    }
                }
                is FoxStructSelectFieldsType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructSelectFieldsType(normalizedBase, currentType.names)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        struct.selectFields(currentType.names)
                    }
                }
                is FoxStructSelectFieldsExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructSelectFieldsExactType(normalizedBase, currentType.names)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeStructNameSelectionExact(transform, struct, currentType.names)
                    }
                }
                is FoxStructDropFieldsType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructDropFieldsType(normalizedBase, currentType.names)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        struct.dropFields(currentType.names)
                    }
                }
                is FoxStructDropFieldsExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructDropFieldsExactType(normalizedBase, currentType.names)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        normalizeStructNameDropExact(transform, struct, currentType.names)
                    }
                }
                is FoxStructExtractFieldTypesType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxStructExtractFieldTypesType(normalizedBase)
                    normalizeStructTransform(transform, normalizedBase) { struct ->
                        struct.fields.values.toFoxTupleType()
                    }
                }
                is FoxStructMergeStructsType -> normalizeMergeStruct(currentType.types)
                is FoxObjectGetMemberTypeType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxObjectGetMemberTypeType(normalizedBase, currentType.name)
                    normalizeObjectTransform(transform, normalizedBase) { obj ->
                        if (currentType.name !in obj.members) nameNotFound(transform, currentType.name)
                        else obj.members.getValue(currentType.name)
                    }
                }
                is FoxObjectSelectMembersType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxObjectSelectMembersType(normalizedBase, currentType.names)
                    normalizeObjectTransform(transform, normalizedBase) { obj ->
                        obj.selectMembers(currentType.names)
                    }
                }
                is FoxObjectSelectMembersExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxObjectSelectMembersExactType(normalizedBase, currentType.names)
                    normalizeObjectTransform(transform, normalizedBase) { obj ->
                        normalizeObjectNameSelectionExact(transform, obj, currentType.names)
                    }
                }
                is FoxObjectDropMembersType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxObjectDropMembersType(normalizedBase, currentType.names)
                    normalizeObjectTransform(transform, normalizedBase) { obj ->
                        obj.dropMembers(currentType.names)
                    }
                }
                is FoxObjectDropMembersExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxObjectDropMembersExactType(normalizedBase, currentType.names)
                    normalizeObjectTransform(transform, normalizedBase) { obj ->
                        normalizeObjectNameDropExact(transform, obj, currentType.names)
                    }
                }
                is FoxObjectMergeObjectsType -> normalizeMergeObject(currentType.types)
                is FoxEnumGetEntryTypeType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxEnumGetEntryTypeType(normalizedBase, currentType.name)
                    normalizeEnumTransform(transform, normalizedBase) { enumType ->
                        if (currentType.name !in enumType.entries) nameNotFound(transform, currentType.name)
                        else enumType.entries.getValue(currentType.name)
                    }
                }
                is FoxEnumSelectEntriesType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxEnumSelectEntriesType(normalizedBase, currentType.names)
                    normalizeEnumTransform(transform, normalizedBase) { enumType ->
                        enumType.selectEntries(currentType.names)
                    }
                }
                is FoxEnumSelectEntriesExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxEnumSelectEntriesExactType(normalizedBase, currentType.names)
                    normalizeEnumTransform(transform, normalizedBase) { enumType ->
                        normalizeEnumNameSelectionExact(transform, enumType, currentType.names)
                    }
                }
                is FoxEnumDropEntriesType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxEnumDropEntriesType(normalizedBase, currentType.names)
                    normalizeEnumTransform(transform, normalizedBase) { enumType ->
                        enumType.dropEntries(currentType.names)
                    }
                }
                is FoxEnumDropEntriesExactType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxEnumDropEntriesExactType(normalizedBase, currentType.names)
                    normalizeEnumTransform(transform, normalizedBase) { enumType ->
                        normalizeEnumNameDropExact(transform, enumType, currentType.names)
                    }
                }
                is FoxEnumMergeEnumsType -> normalizeMergeEnum(currentType.types)
                is FoxArrayGetElementTypeType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxArrayGetElementTypeType(normalizedBase)
                    normalizeArrayTransform(transform, normalizedBase) { array -> array.element }
                }
                is FoxRefGetReferentTypeType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxRefGetReferentTypeType(normalizedBase)
                    normalizeRefTransform(transform, normalizedBase) { ref -> ref.referent }
                }
                is FoxMethodGetThisTypeType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxMethodGetThisTypeType(normalizedBase)
                    normalizeMethodTransform(transform, normalizedBase) { method -> method.`this` }
                }
                is FoxMethodGetParameterStructType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxMethodGetParameterStructType(normalizedBase)
                    normalizeMethodTransform(transform, normalizedBase) { method ->
                        FoxStructType(method.parameters)
                    }
                }
                is FoxMethodGetReturnTypeType -> {
                    val normalizedBase = normalizeType(currentType.type)
                    val transform = FoxMethodGetReturnTypeType(normalizedBase)
                    normalizeMethodTransform(transform, normalizedBase) { method -> method.`return` }
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
            }
            is FoxUnresolvedType -> FoxUnresolvedType(currentType.name, currentType.parameters?.map { normalizeType(it) })
            is FoxPlaceholderType -> error("unreachable")
        }
    }
    
    val normalizedType = normalizeType(type)
    if (errors.isNotEmpty()) return TypeNormalizationFailure(errors)
    return TypeNormalizationSuccess(normalizedType)
}
