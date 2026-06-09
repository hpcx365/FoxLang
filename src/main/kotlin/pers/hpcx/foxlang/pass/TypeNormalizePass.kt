package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet
import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.mutableOrderedMapOf

object TypeNormalizePass : FoxTypePass {
    override fun run(input: FoxType, context: TypePassContext): TypePassResult<FoxType> {
        return TypePassResult(normalize(input))
    }
    
    fun normalize(type: FoxType): FoxType {
        require(type !is FoxCustomizedType) { "Type aliases must be normalized before type matching: $type" }
        
        return when (type) {
            is FoxPrimitiveType,
            is FoxWildcardType,
                -> type
            
            is FoxTupleType -> type.components.map { (component, count) -> normalize(component) to count }.toFoxTupleType()
            is FoxStructType -> FoxStructType(type.fields.normalizeValues())
            is FoxObjectType -> FoxObjectType(type.members.mapValuesTo(LinkedHashMap()) { (_, value) -> normalize(value) })
            is FoxEnumType -> FoxEnumType(type.items.mapValuesTo(LinkedHashMap()) { (_, value) -> normalize(value) })
            is FoxArrayType -> FoxArrayType(normalize(type.element))
            is FoxRefType -> FoxRefType(normalize(type.referent))
            is FoxMethodType -> FoxMethodType(
                normalize(type.`this`),
                type.parameters.map(::normalize),
                normalize(type.`return`),
            )
            
            is FoxTransformType -> normalizeTransform(type)
        }
    }
    
    private fun normalizeTransform(type: FoxTransformType): FoxType = when (type) {
        is FoxTupleComponentAtType -> {
            val target = normalize(type.type)
            when (val resolved = tryResolveTupleComponentAt(target, type.index)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxTupleComponentAtType(target, type.index)
                PartialTypeEval.Unresolved -> FoxTupleComponentAtType(target, type.index)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxTupleLastComponentAtType -> {
            val target = normalize(type.type)
            when (val resolved = tryResolveTupleLastComponentAt(target, type.index)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxTupleLastComponentAtType(target, type.index)
                PartialTypeEval.Unresolved -> FoxTupleLastComponentAtType(target, type.index)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxTupleFirstComponentsOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeTupleFirstComponents(target, type.count)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxTupleFirstComponentsOfType(target, type.count)
                PartialTypeEval.Unresolved -> FoxTupleFirstComponentsOfType(target, type.count)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxTupleLastComponentsOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeTupleLastComponents(target, type.count)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxTupleLastComponentsOfType(target, type.count)
                PartialTypeEval.Unresolved -> FoxTupleLastComponentsOfType(target, type.count)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxTupleDropFirstComponentsOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeTupleDropFirstComponents(target, type.count)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxTupleDropFirstComponentsOfType(target, type.count)
                PartialTypeEval.Unresolved -> FoxTupleDropFirstComponentsOfType(target, type.count)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxTupleDropLastComponentsOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeTupleDropLastComponents(target, type.count)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxTupleDropLastComponentsOfType(target, type.count)
                PartialTypeEval.Unresolved -> FoxTupleDropLastComponentsOfType(target, type.count)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxTupleMergeComponentsOfType -> {
            val normalized = type.types.map(::normalize)
            if (normalized.isEmpty()) emptyFoxTupleType()
            else if (normalized.all { it is FoxTupleType }) normalized.filterIsInstance<FoxTupleType>().mergeTupleComponents()
            else FoxTupleMergeComponentsOfType(normalized)
        }
        
        is FoxStructFieldOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryResolveStructFieldOf(target, type.name)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxStructFieldOfType(target, type.name)
                PartialTypeEval.Unresolved -> FoxStructFieldOfType(target, type.name)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxStructFieldAtType -> {
            val target = normalize(type.type)
            when (val resolved = tryResolveStructFieldAt(target, type.index)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxStructFieldAtType(target, type.index)
                PartialTypeEval.Unresolved -> FoxStructFieldAtType(target, type.index)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxStructLastFieldAtType -> {
            val target = normalize(type.type)
            when (val resolved = tryResolveStructLastFieldAt(target, type.index)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxStructLastFieldAtType(target, type.index)
                PartialTypeEval.Unresolved -> FoxStructLastFieldAtType(target, type.index)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxStructFirstFieldsOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeStructFirstFields(target, type.count)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxStructFirstFieldsOfType(target, type.count)
                PartialTypeEval.Unresolved -> FoxStructFirstFieldsOfType(target, type.count)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxStructLastFieldsOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeStructLastFields(target, type.count)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxStructLastFieldsOfType(target, type.count)
                PartialTypeEval.Unresolved -> FoxStructLastFieldsOfType(target, type.count)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxStructDropFirstFieldsOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeStructDropFirstFields(target, type.count)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxStructDropFirstFieldsOfType(target, type.count)
                PartialTypeEval.Unresolved -> FoxStructDropFirstFieldsOfType(target, type.count)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxStructDropLastFieldsOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeStructDropLastFields(target, type.count)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxStructDropLastFieldsOfType(target, type.count)
                PartialTypeEval.Unresolved -> FoxStructDropLastFieldsOfType(target, type.count)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxStructFieldsOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeStructFields(target, type.names)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxStructFieldsOfType(target, type.names)
                PartialTypeEval.Unresolved -> FoxStructFieldsOfType(target, type.names)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxStructDropFieldsOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeStructDropFields(target, type.names)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxStructDropFieldsOfType(target, type.names)
                PartialTypeEval.Unresolved -> FoxStructDropFieldsOfType(target, type.names)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxStructMergeFieldsOfType -> {
            val normalized = type.types.map(::normalize)
            if (normalized.isEmpty()) emptyFoxStructType()
            else if (normalized.all { it is FoxStructType }) normalized.filterIsInstance<FoxStructType>().mergeStructFields()
            else FoxStructMergeFieldsOfType(normalized)
        }
        
        is FoxObjectMemberOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryResolveObjectMember(target, type.name)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxObjectMemberOfType(target, type.name)
                PartialTypeEval.Unresolved -> FoxObjectMemberOfType(target, type.name)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxObjectMembersOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeObjectMembers(target, type.names)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxObjectMembersOfType(target, type.names)
                PartialTypeEval.Unresolved -> FoxObjectMembersOfType(target, type.names)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxObjectDropMembersOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeObjectDropMembers(target, type.names)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxObjectDropMembersOfType(target, type.names)
                PartialTypeEval.Unresolved -> FoxObjectDropMembersOfType(target, type.names)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxObjectMergeMembersOfType -> {
            val normalized = type.types.map(::normalize)
            if (normalized.isEmpty()) emptyFoxObjectType()
            else if (normalized.all { it is FoxObjectType }) normalized.filterIsInstance<FoxObjectType>().mergeObjectMembers()
            else FoxObjectMergeMembersOfType(normalized)
        }
        
        is FoxEnumItemOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryResolveEnumItem(target, type.name)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxEnumItemOfType(target, type.name)
                PartialTypeEval.Unresolved -> FoxEnumItemOfType(target, type.name)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxEnumItemsOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeEnumItems(target, type.names)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxEnumItemsOfType(target, type.names)
                PartialTypeEval.Unresolved -> FoxEnumItemsOfType(target, type.names)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxEnumDropItemsOfType -> {
            val target = normalize(type.type)
            when (val resolved = tryNormalizeEnumDropItems(target, type.names)) {
                is PartialTypeEval.Resolved -> normalize(resolved.type)
                is PartialTypeEval.KnownInvalid -> FoxEnumDropItemsOfType(target, type.names)
                PartialTypeEval.Unresolved -> FoxEnumDropItemsOfType(target, type.names)
                is PartialTypeEval.Invalid -> throw IllegalArgumentException(resolved.reason)
            }
        }
        is FoxEnumMergeItemsOfType -> {
            val normalized = type.types.map(::normalize)
            if (normalized.isEmpty()) emptyFoxEnumType()
            else if (normalized.all { it is FoxEnumType }) normalized.filterIsInstance<FoxEnumType>().mergeEnumItems()
            else FoxEnumMergeItemsOfType(normalized)
        }
        
        is FoxArrayElementOfType -> {
            val target = normalize(type.type)
            if (target is FoxArrayType) target.element else FoxArrayElementOfType(target)
        }
        is FoxRefReferentOfType -> {
            val target = normalize(type.type)
            if (target is FoxRefType) target.referent else FoxRefReferentOfType(target)
        }
        is FoxMethodThisOfType -> {
            val target = normalize(type.type)
            if (target is FoxMethodType) target.`this` else FoxMethodThisOfType(target)
        }
        is FoxMethodParametersOfType -> {
            val target = normalize(type.type)
            if (target is FoxMethodType) target.parameters.toFoxTupleType() else FoxMethodParametersOfType(target)
        }
        is FoxMethodReturnOfType -> {
            val target = normalize(type.type)
            if (target is FoxMethodType) target.`return` else FoxMethodReturnOfType(target)
        }
    }
    
    private sealed interface SequencePatternPart<out T> {
        data object Gap : SequencePatternPart<Nothing>
        
        data class Exact<T>(
            val values: List<T>,
        ) : SequencePatternPart<T>
    }
    
    private data class UnorderedPattern(
        val requiredMembers: Map<String, FoxType>,
        val allowExtra: Boolean,
    ) {
        val requiredItems: Map<String, FoxType>
            get() = requiredMembers
    }
    
    private sealed interface PartialTypeEval {
        data class Resolved(
            val type: FoxType,
        ) : PartialTypeEval
        
        data object Unresolved : PartialTypeEval
        
        data class KnownInvalid(
            val reason: String,
        ) : PartialTypeEval
        
        data class Invalid(
            val reason: String,
        ) : PartialTypeEval
    }
    
    private sealed interface PartialLookup<out T> {
        data class Resolved<T>(
            val value: T,
        ) : PartialLookup<T>
        
        data object Unresolved : PartialLookup<Nothing>
        
        data class Invalid(
            val reason: String,
        ) : PartialLookup<Nothing>
    }
    
    private fun FoxTupleType.expandedTypes(): List<FoxType> {
        val result = ArrayList<FoxType>(size)
        components.forEach { (type, count) -> repeat(count) { result += type } }
        return result
    }
    
    private fun normalizeTuplePattern(pattern: FoxType): List<SequencePatternPart<FoxType>> = when (pattern) {
        FoxAnyTupleType -> listOf(SequencePatternPart.Gap)
        is FoxTupleType -> listOf(SequencePatternPart.Exact(pattern.expandedTypes()))
        is FoxTupleMergeComponentsOfType -> pattern.types.flatMap(::normalizeTuplePattern).compact()
        else -> error("Not a tuple constraint pattern: $pattern")
    }
    
    private fun normalizeStructPattern(pattern: FoxType): List<SequencePatternPart<Pair<String, FoxType>>> = when (pattern) {
        FoxAnyStructType -> listOf(SequencePatternPart.Gap)
        is FoxStructType -> listOf(SequencePatternPart.Exact(pattern.fields.entries.map { it.key to it.value }))
        is FoxStructMergeFieldsOfType -> pattern.types.flatMap(::normalizeStructPattern).compact()
        else -> error("Not a struct constraint pattern: $pattern")
    }
    
    private fun normalizeObjectPattern(pattern: FoxType): UnorderedPattern = when (pattern) {
        FoxAnyObjectType -> UnorderedPattern(emptyMap(), allowExtra = true)
        is FoxObjectType -> UnorderedPattern(LinkedHashMap(pattern.members), allowExtra = false)
        is FoxObjectMergeMembersOfType -> {
            val required = LinkedHashMap<String, FoxType>()
            var allowExtra = false
            pattern.types.forEach { type ->
                val normalized = normalizeObjectPattern(type)
                allowExtra = allowExtra || normalized.allowExtra
                normalized.requiredMembers.forEach { (name, value) -> required.mergeConstraint(name, value, "object member") }
            }
            UnorderedPattern(required, allowExtra)
        }
        else -> error("Not an object constraint pattern: $pattern")
    }
    
    private fun normalizeEnumPattern(pattern: FoxType): UnorderedPattern = when (pattern) {
        FoxAnyEnumType -> UnorderedPattern(emptyMap(), allowExtra = true)
        is FoxEnumType -> UnorderedPattern(LinkedHashMap(pattern.items), allowExtra = false)
        is FoxEnumMergeItemsOfType -> {
            val required = LinkedHashMap<String, FoxType>()
            var allowExtra = false
            pattern.types.forEach { type ->
                val normalized = normalizeEnumPattern(type)
                allowExtra = allowExtra || normalized.allowExtra
                normalized.requiredItems.forEach { (name, value) -> required.mergeConstraint(name, value, "enum item") }
            }
            UnorderedPattern(required, allowExtra)
        }
        else -> error("Not an enum constraint pattern: $pattern")
    }
    
    private fun MutableMap<String, FoxType>.mergeConstraint(name: String, incoming: FoxType, subject: String) {
        val existing = this[name]
        this[name] = when {
            existing == null -> incoming
            existing == incoming -> existing
            else -> error("Conflicting constraints for $subject '$name': $existing vs $incoming")
        }
    }
    
    private fun rebuildTuplePattern(parts: List<SequencePatternPart<FoxType>>): FoxType = when (parts.size) {
        0 -> emptyFoxTupleType()
        1 -> when (val part = parts.single()) {
            SequencePatternPart.Gap -> FoxAnyTupleType
            is SequencePatternPart.Exact -> part.values.toFoxTupleType()
        }
        else -> FoxTupleMergeComponentsOfType(
            parts.map { part ->
                when (part) {
                    SequencePatternPart.Gap -> FoxAnyTupleType
                    is SequencePatternPart.Exact -> part.values.toFoxTupleType()
                }
            },
        )
    }
    
    private fun rebuildStructExact(fields: List<Pair<String, FoxType>>): FoxStructType {
        val result = mutableOrderedMapOf<String, FoxType>()
        fields.forEach { (name, type) -> result[name] = type }
        return FoxStructType(result)
    }
    
    private fun rebuildStructPattern(parts: List<SequencePatternPart<Pair<String, FoxType>>>): FoxType = when (parts.size) {
        0 -> emptyFoxStructType()
        1 -> when (val part = parts.single()) {
            SequencePatternPart.Gap -> FoxAnyStructType
            is SequencePatternPart.Exact -> rebuildStructExact(part.values)
        }
        else -> FoxStructMergeFieldsOfType(
            parts.map { part ->
                when (part) {
                    SequencePatternPart.Gap -> FoxAnyStructType
                    is SequencePatternPart.Exact -> rebuildStructExact(part.values)
                }
            },
        )
    }
    
    private fun <T> deterministicPrefix(parts: List<SequencePatternPart<T>>): List<T> {
        val result = mutableListOf<T>()
        for (part in parts) when (part) {
            SequencePatternPart.Gap -> break
            is SequencePatternPart.Exact -> result += part.values
        }
        return result
    }
    
    private fun <T> deterministicSuffix(parts: List<SequencePatternPart<T>>): List<T> {
        val result = mutableListOf<T>()
        for (index in parts.indices.reversed()) when (val part = parts[index]) {
            SequencePatternPart.Gap -> break
            is SequencePatternPart.Exact -> result.addAll(0, part.values)
        }
        return result
    }
    
    private fun <T> trimDeterministicPrefix(parts: List<SequencePatternPart<T>>, count: Int): List<SequencePatternPart<T>>? {
        if (count == 0) return parts
        var remaining = count
        val result = mutableListOf<SequencePatternPart<T>>()
        for (index in parts.indices) {
            when (val part = parts[index]) {
                SequencePatternPart.Gap -> return if (remaining == 0) (result + part).compact() else null
                is SequencePatternPart.Exact -> {
                    if (remaining >= part.values.size) remaining -= part.values.size
                    else {
                        result += SequencePatternPart.Exact(part.values.drop(remaining))
                        remaining = 0
                    }
                    if (remaining == 0) return (result + parts.drop(index + 1)).compact()
                }
            }
        }
        return if (remaining == 0) result.compact() else null
    }
    
    private fun <T> trimDeterministicSuffix(parts: List<SequencePatternPart<T>>, count: Int): List<SequencePatternPart<T>>? {
        if (count == 0) return parts
        var remaining = count
        val kept = mutableListOf<SequencePatternPart<T>>()
        for (index in parts.indices.reversed()) {
            when (val part = parts[index]) {
                SequencePatternPart.Gap -> return if (remaining == 0) (parts.take(index + 1) + kept).compact() else null
                is SequencePatternPart.Exact -> {
                    if (remaining >= part.values.size) remaining -= part.values.size
                    else {
                        kept.add(0, SequencePatternPart.Exact(part.values.dropLast(remaining)))
                        remaining = 0
                    }
                    if (remaining == 0) return (parts.take(index) + kept).compact()
                }
            }
        }
        return if (remaining == 0) kept.compact() else null
    }
    
    private fun tryResolveTupleComponentAt(type: FoxType, index: Int): PartialTypeEval {
        if (index < 0) return PartialTypeEval.Invalid("Tuple component index must be non-negative: $index")
        val parts = when (type) {
            FoxAnyTupleType, is FoxTupleType, is FoxTupleMergeComponentsOfType -> normalizeTuplePattern(type)
            else -> return PartialTypeEval.Unresolved
        }
        return tryResolveSequenceAt(parts, index, "Tuple component index out of bounds: $index")
    }
    
    private fun tryResolveTupleLastComponentAt(type: FoxType, index: Int): PartialTypeEval {
        if (index < 0) return PartialTypeEval.Invalid("Tuple component index must be non-negative: $index")
        val parts = when (type) {
            FoxAnyTupleType, is FoxTupleType, is FoxTupleMergeComponentsOfType -> normalizeTuplePattern(type)
            else -> return PartialTypeEval.Unresolved
        }
        return tryResolveSequenceFromLastAt(parts, index, "Tuple component index out of bounds: $index")
    }
    
    private fun tryResolveStructFieldAt(type: FoxType, index: Int): PartialTypeEval {
        if (index < 0) return PartialTypeEval.Invalid("Struct field index must be non-negative: $index")
        val parts = when (type) {
            FoxAnyStructType, is FoxStructType, is FoxStructMergeFieldsOfType -> normalizeStructPattern(type)
            else -> return PartialTypeEval.Unresolved
        }
        return tryResolveSequenceAt(parts, index, "Struct field index out of bounds: $index") { it.second }
    }
    
    private fun tryResolveStructLastFieldAt(type: FoxType, index: Int): PartialTypeEval {
        if (index < 0) return PartialTypeEval.Invalid("Struct field index must be non-negative: $index")
        val parts = when (type) {
            FoxAnyStructType, is FoxStructType, is FoxStructMergeFieldsOfType -> normalizeStructPattern(type)
            else -> return PartialTypeEval.Unresolved
        }
        return tryResolveSequenceFromLastAt(parts, index, "Struct field index out of bounds: $index") { it.second }
    }
    
    private fun tryNormalizeTupleFirstComponents(type: FoxType, count: Int): PartialTypeEval {
        if (count < 0) return PartialTypeEval.Invalid("Tuple component count must be non-negative: $count")
        val parts = when (type) {
            FoxAnyTupleType, is FoxTupleType, is FoxTupleMergeComponentsOfType -> normalizeTuplePattern(type)
            else -> return PartialTypeEval.Unresolved
        }
        val prefix = deterministicPrefix(parts)
        return when {
            count == 0 -> PartialTypeEval.Resolved(emptyFoxTupleType())
            count <= prefix.size -> PartialTypeEval.Resolved(prefix.take(count).toFoxTupleType())
            type is FoxTupleType -> PartialTypeEval.KnownInvalid("Tuple component count out of bounds: $count, size=${type.size}")
            else -> PartialTypeEval.Unresolved
        }
    }
    
    private fun tryNormalizeTupleLastComponents(type: FoxType, count: Int): PartialTypeEval {
        if (count < 0) return PartialTypeEval.Invalid("Tuple component count must be non-negative: $count")
        val parts = when (type) {
            FoxAnyTupleType, is FoxTupleType, is FoxTupleMergeComponentsOfType -> normalizeTuplePattern(type)
            else -> return PartialTypeEval.Unresolved
        }
        val suffix = deterministicSuffix(parts)
        return when {
            count == 0 -> PartialTypeEval.Resolved(emptyFoxTupleType())
            count <= suffix.size -> PartialTypeEval.Resolved(suffix.takeLast(count).toFoxTupleType())
            type is FoxTupleType -> PartialTypeEval.KnownInvalid("Tuple component count out of bounds: $count, size=${type.size}")
            else -> PartialTypeEval.Unresolved
        }
    }
    
    private fun tryNormalizeTupleDropFirstComponents(type: FoxType, count: Int): PartialTypeEval {
        if (count < 0) return PartialTypeEval.Invalid("Tuple component count must be non-negative: $count")
        val parts = when (type) {
            FoxAnyTupleType, is FoxTupleType, is FoxTupleMergeComponentsOfType -> normalizeTuplePattern(type)
            else -> return PartialTypeEval.Unresolved
        }
        val trimmed = trimDeterministicPrefix(parts, count)
        return when {
            trimmed != null -> PartialTypeEval.Resolved(rebuildTuplePattern(trimmed))
            type is FoxTupleType -> PartialTypeEval.KnownInvalid("Tuple component count out of bounds: $count, size=${type.size}")
            else -> PartialTypeEval.Unresolved
        }
    }
    
    private fun tryNormalizeTupleDropLastComponents(type: FoxType, count: Int): PartialTypeEval {
        if (count < 0) return PartialTypeEval.Invalid("Tuple component count must be non-negative: $count")
        val parts = when (type) {
            FoxAnyTupleType, is FoxTupleType, is FoxTupleMergeComponentsOfType -> normalizeTuplePattern(type)
            else -> return PartialTypeEval.Unresolved
        }
        val trimmed = trimDeterministicSuffix(parts, count)
        return when {
            trimmed != null -> PartialTypeEval.Resolved(rebuildTuplePattern(trimmed))
            type is FoxTupleType -> PartialTypeEval.KnownInvalid("Tuple component count out of bounds: $count, size=${type.size}")
            else -> PartialTypeEval.Unresolved
        }
    }
    
    private fun tryNormalizeStructFirstFields(type: FoxType, count: Int): PartialTypeEval {
        if (count < 0) return PartialTypeEval.Invalid("Struct field count must be non-negative: $count")
        val parts = when (type) {
            FoxAnyStructType, is FoxStructType, is FoxStructMergeFieldsOfType -> normalizeStructPattern(type)
            else -> return PartialTypeEval.Unresolved
        }
        val prefix = deterministicPrefix(parts)
        return when {
            count == 0 -> PartialTypeEval.Resolved(emptyFoxStructType())
            count <= prefix.size -> PartialTypeEval.Resolved(rebuildStructExact(prefix.take(count)))
            type is FoxStructType -> PartialTypeEval.KnownInvalid("Struct field count out of bounds: $count, size=${type.size}")
            else -> PartialTypeEval.Unresolved
        }
    }
    
    private fun tryNormalizeStructLastFields(type: FoxType, count: Int): PartialTypeEval {
        if (count < 0) return PartialTypeEval.Invalid("Struct field count must be non-negative: $count")
        val parts = when (type) {
            FoxAnyStructType, is FoxStructType, is FoxStructMergeFieldsOfType -> normalizeStructPattern(type)
            else -> return PartialTypeEval.Unresolved
        }
        val suffix = deterministicSuffix(parts)
        return when {
            count == 0 -> PartialTypeEval.Resolved(emptyFoxStructType())
            count <= suffix.size -> PartialTypeEval.Resolved(rebuildStructExact(suffix.takeLast(count)))
            type is FoxStructType -> PartialTypeEval.KnownInvalid("Struct field count out of bounds: $count, size=${type.size}")
            else -> PartialTypeEval.Unresolved
        }
    }
    
    private fun tryNormalizeStructDropFirstFields(type: FoxType, count: Int): PartialTypeEval {
        if (count < 0) return PartialTypeEval.Invalid("Struct field count must be non-negative: $count")
        val parts = when (type) {
            FoxAnyStructType, is FoxStructType, is FoxStructMergeFieldsOfType -> normalizeStructPattern(type)
            else -> return PartialTypeEval.Unresolved
        }
        val trimmed = trimDeterministicPrefix(parts, count)
        return when {
            trimmed != null -> PartialTypeEval.Resolved(rebuildStructPattern(trimmed))
            type is FoxStructType -> PartialTypeEval.KnownInvalid("Struct field count out of bounds: $count, size=${type.size}")
            else -> PartialTypeEval.Unresolved
        }
    }
    
    private fun tryNormalizeStructDropLastFields(type: FoxType, count: Int): PartialTypeEval {
        if (count < 0) return PartialTypeEval.Invalid("Struct field count must be non-negative: $count")
        val parts = when (type) {
            FoxAnyStructType, is FoxStructType, is FoxStructMergeFieldsOfType -> normalizeStructPattern(type)
            else -> return PartialTypeEval.Unresolved
        }
        val trimmed = trimDeterministicSuffix(parts, count)
        return when {
            trimmed != null -> PartialTypeEval.Resolved(rebuildStructPattern(trimmed))
            type is FoxStructType -> PartialTypeEval.KnownInvalid("Struct field count out of bounds: $count, size=${type.size}")
            else -> PartialTypeEval.Unresolved
        }
    }
    
    private fun tryResolveStructFieldOf(type: FoxType, name: String): PartialTypeEval = when (type) {
        is FoxStructType -> type.fields[name]?.let { PartialTypeEval.Resolved(it) }
            ?: PartialTypeEval.Invalid("Struct field does not exist: $name")
        FoxAnyStructType, is FoxStructMergeFieldsOfType -> {
            val parts = normalizeStructPattern(type)
            when (val resolved = tryResolveNamedSequenceValue(parts, name, { it.first == name }, { it.second })) {
                is PartialLookup.Resolved -> PartialTypeEval.Resolved(resolved.value)
                PartialLookup.Unresolved -> PartialTypeEval.Unresolved
                is PartialLookup.Invalid -> PartialTypeEval.Invalid(resolved.reason)
            }
        }
        else -> PartialTypeEval.Unresolved
    }
    
    private fun tryNormalizeStructFields(type: FoxType, names: OrderedSet<String>): PartialTypeEval = when (type) {
        is FoxStructType -> PartialTypeEval.Resolved(type.selectFields(names))
        FoxAnyStructType, is FoxStructMergeFieldsOfType -> {
            val parts = normalizeStructPattern(type)
            val selected = mutableListOf<Pair<String, FoxType>>()
            for (name in names) {
                when (val resolved = tryResolveNamedSequenceValue(parts, name, { it.first == name }, { it })) {
                    is PartialLookup.Resolved -> selected += resolved.value
                    PartialLookup.Unresolved -> return PartialTypeEval.Unresolved
                    is PartialLookup.Invalid -> return PartialTypeEval.Invalid(resolved.reason)
                }
            }
            PartialTypeEval.Resolved(rebuildStructExact(selected))
        }
        else -> PartialTypeEval.Unresolved
    }
    
    private fun tryNormalizeStructDropFields(type: FoxType, names: Set<String>): PartialTypeEval = when (type) {
        is FoxStructType -> PartialTypeEval.Resolved(type.dropFields(names))
        FoxAnyStructType -> PartialTypeEval.Unresolved
        is FoxStructMergeFieldsOfType -> PartialTypeEval.Resolved(
            rebuildStructPattern(
                parts = normalizeStructPattern(type).mapNotNull { part ->
                    when (part) {
                        SequencePatternPart.Gap -> part
                        is SequencePatternPart.Exact -> part.values.filter { it.first !in names }
                            .takeIf { it.isNotEmpty() }
                            ?.let { SequencePatternPart.Exact(it) }
                    }
                }.compact(),
            ),
        )
        else -> PartialTypeEval.Unresolved
    }
    
    private fun tryResolveObjectMember(type: FoxType, name: String): PartialTypeEval = when (type) {
        is FoxObjectType -> type.members[name]?.let { PartialTypeEval.Resolved(it) }
            ?: PartialTypeEval.Invalid("Object member does not exist: $name")
        FoxAnyObjectType, is FoxObjectMergeMembersOfType -> {
            val normalized = normalizeObjectPattern(type)
            normalized.requiredMembers[name]?.let { PartialTypeEval.Resolved(it) }
                ?: if (normalized.allowExtra) PartialTypeEval.Unresolved else PartialTypeEval.Invalid("Object member does not exist: $name")
        }
        else -> PartialTypeEval.Unresolved
    }
    
    private fun tryNormalizeObjectMembers(type: FoxType, names: Set<String>): PartialTypeEval = when (type) {
        is FoxObjectType -> PartialTypeEval.Resolved(type.selectMembers(names))
        FoxAnyObjectType, is FoxObjectMergeMembersOfType -> {
            val normalized = normalizeObjectPattern(type)
            val selected = LinkedHashMap<String, FoxType>()
            for (name in names) {
                val value = normalized.requiredMembers[name]
                when {
                    value != null -> selected[name] = value
                    normalized.allowExtra -> return PartialTypeEval.Unresolved
                    else -> return PartialTypeEval.Invalid("Object member does not exist: $name")
                }
            }
            PartialTypeEval.Resolved(FoxObjectType(selected))
        }
        else -> PartialTypeEval.Unresolved
    }
    
    private fun tryNormalizeObjectDropMembers(type: FoxType, names: Set<String>): PartialTypeEval = when (type) {
        is FoxObjectType -> PartialTypeEval.Resolved(type.dropMembers(names))
        FoxAnyObjectType -> PartialTypeEval.Unresolved
        is FoxObjectMergeMembersOfType -> {
            val normalized = normalizeObjectPattern(type)
            val remaining = normalized.requiredMembers.filterKeys { it !in names }
            PartialTypeEval.Resolved(
                if (normalized.allowExtra) {
                    if (remaining.isEmpty()) FoxAnyObjectType else FoxObjectMergeMembersOfType(listOf(FoxObjectType(remaining), FoxAnyObjectType))
                } else {
                    FoxObjectType(remaining)
                },
            )
        }
        else -> PartialTypeEval.Unresolved
    }
    
    private fun tryResolveEnumItem(type: FoxType, name: String): PartialTypeEval = when (type) {
        is FoxEnumType -> type.items[name]?.let { PartialTypeEval.Resolved(it) }
            ?: PartialTypeEval.Invalid("Enum item does not exist: $name")
        FoxAnyEnumType, is FoxEnumMergeItemsOfType -> {
            val normalized = normalizeEnumPattern(type)
            normalized.requiredItems[name]?.let { PartialTypeEval.Resolved(it) }
                ?: if (normalized.allowExtra) PartialTypeEval.Unresolved else PartialTypeEval.Invalid("Enum item does not exist: $name")
        }
        else -> PartialTypeEval.Unresolved
    }
    
    private fun tryNormalizeEnumItems(type: FoxType, names: List<String>): PartialTypeEval = when (type) {
        is FoxEnumType -> PartialTypeEval.Resolved(type.selectItems(names))
        FoxAnyEnumType, is FoxEnumMergeItemsOfType -> {
            val normalized = normalizeEnumPattern(type)
            val selected = LinkedHashMap<String, FoxType>()
            for (name in names) {
                val value = normalized.requiredItems[name]
                when {
                    value != null -> selected[name] = value
                    normalized.allowExtra -> return PartialTypeEval.Unresolved
                    else -> return PartialTypeEval.Invalid("Enum item does not exist: $name")
                }
            }
            PartialTypeEval.Resolved(FoxEnumType(selected))
        }
        else -> PartialTypeEval.Unresolved
    }
    
    private fun tryNormalizeEnumDropItems(type: FoxType, names: List<String>): PartialTypeEval = when (type) {
        is FoxEnumType -> PartialTypeEval.Resolved(type.dropItems(names))
        FoxAnyEnumType -> PartialTypeEval.Unresolved
        is FoxEnumMergeItemsOfType -> {
            val normalized = normalizeEnumPattern(type)
            val remaining = normalized.requiredItems.filterKeys { it !in names }
            PartialTypeEval.Resolved(
                if (normalized.allowExtra) {
                    if (remaining.isEmpty()) FoxAnyEnumType else FoxEnumMergeItemsOfType(listOf(FoxEnumType(remaining), FoxAnyEnumType))
                } else {
                    FoxEnumType(remaining)
                },
            )
        }
        else -> PartialTypeEval.Unresolved
    }
    
    private fun tryResolveSequenceAt(parts: List<SequencePatternPart<FoxType>>, index: Int, outOfBoundsMessage: String): PartialTypeEval =
        tryResolveSequenceAt(parts, index, outOfBoundsMessage) { it }
    
    private fun <T> tryResolveSequenceAt(
        parts: List<SequencePatternPart<T>>,
        index: Int,
        outOfBoundsMessage: String,
        project: (T) -> FoxType,
    ): PartialTypeEval {
        var consumed = 0
        for (part in parts) {
            when (part) {
                SequencePatternPart.Gap -> return PartialTypeEval.Unresolved
                is SequencePatternPart.Exact -> {
                    val next = consumed + part.values.size
                    if (index < next) return PartialTypeEval.Resolved(project(part.values[index - consumed]))
                    consumed = next
                }
            }
        }
        return PartialTypeEval.KnownInvalid(outOfBoundsMessage)
    }
    
    private fun tryResolveSequenceFromLastAt(parts: List<SequencePatternPart<FoxType>>, index: Int, outOfBoundsMessage: String): PartialTypeEval =
        tryResolveSequenceFromLastAt(parts, index, outOfBoundsMessage) { it }
    
    private fun <T> tryResolveSequenceFromLastAt(
        parts: List<SequencePatternPart<T>>,
        index: Int,
        outOfBoundsMessage: String,
        project: (T) -> FoxType,
    ): PartialTypeEval {
        var consumed = 0
        for (partIndex in parts.indices.reversed()) {
            when (val part = parts[partIndex]) {
                SequencePatternPart.Gap -> return PartialTypeEval.Unresolved
                is SequencePatternPart.Exact -> {
                    val next = consumed + part.values.size
                    if (index < next) {
                        val reversedIndex = part.values.lastIndex - (index - consumed)
                        return PartialTypeEval.Resolved(project(part.values[reversedIndex]))
                    }
                    consumed = next
                }
            }
        }
        return PartialTypeEval.KnownInvalid(outOfBoundsMessage)
    }
    
    private fun <T, R> tryResolveNamedSequenceValue(
        parts: List<SequencePatternPart<T>>,
        name: String,
        matchesName: (T) -> Boolean,
        project: (T) -> R,
    ): PartialLookup<R> {
        var sawGap = false
        for (part in parts) {
            when (part) {
                SequencePatternPart.Gap -> sawGap = true
                is SequencePatternPart.Exact -> part.values.firstOrNull(matchesName)?.let { return PartialLookup.Resolved(project(it)) }
            }
        }
        return if (sawGap) PartialLookup.Unresolved else PartialLookup.Invalid("Named part does not exist: $name")
    }
    
    private fun OrderedMap<String, FoxType>.normalizeValues(): OrderedMap<String, FoxType> {
        val result = mutableOrderedMapOf<String, FoxType>()
        entries.forEach { (name, value) -> result[name] = normalize(value) }
        return result
    }
    
    private fun FoxEnumType.item(name: String): FoxType = items.getValue(name)
    
    private fun FoxEnumType.selectItems(names: Iterable<String>): FoxEnumType {
        val result = LinkedHashMap<String, FoxType>()
        names.forEach { name -> result[name] = items.getValue(name) }
        return FoxEnumType(result)
    }
    
    private fun FoxEnumType.dropItems(names: Iterable<String>): FoxEnumType {
        val removed = names.toSet()
        val result = LinkedHashMap<String, FoxType>()
        items.forEach { (name, type) -> if (name !in removed) result[name] = type }
        return FoxEnumType(result)
    }
    
    private fun Iterable<FoxEnumType>.mergeEnumItems(): FoxEnumType {
        val result = LinkedHashMap<String, FoxType>()
        forEach { enum -> enum.items.forEach { (name, type) -> result[name] = type } }
        return FoxEnumType(result)
    }
    
    private fun emptyFoxTupleType(): FoxTupleType = FoxTupleType(emptyList())
    
    private fun emptyFoxStructType(): FoxStructType = FoxStructType(emptyOrderedMap())
    
    private fun emptyFoxObjectType(): FoxObjectType = FoxObjectType(emptyMap())
    
    private fun emptyFoxEnumType(): FoxEnumType = FoxEnumType(emptyMap())
    
    private fun FoxTupleType.mergeTupleComponents(other: FoxTupleType): FoxTupleType = (components + other.components).toFoxTupleType()
    
    private fun Iterable<FoxTupleType>.mergeTupleComponents(): FoxTupleType = flatMap { it.components }.toFoxTupleType()
    
    @JvmName("tupleComponentListToFoxTupleTypeInNormalizePass")
    private fun List<Pair<FoxType, Int>>.toFoxTupleType(): FoxTupleType {
        if (isEmpty()) return emptyFoxTupleType()
        val compressed = mutableListOf<Pair<FoxType, Int>>()
        forEach { component ->
            val last = compressed.lastOrNull()
            if (last != null && last.first == component.first) compressed[compressed.lastIndex] = last.first to Math.addExact(last.second, component.second)
            else compressed += component
        }
        return FoxTupleType(compressed)
    }
    
    @JvmName("typeListToFoxTupleTypeInNormalizePass")
    private fun List<FoxType>.toFoxTupleType(): FoxTupleType = map { it to 1 }.toFoxTupleType()
    
    private fun FoxObjectType.member(name: String): FoxType = members.getValue(name)
    
    private fun FoxObjectType.selectMembers(names: Iterable<String>): FoxObjectType {
        val result = LinkedHashMap<String, FoxType>()
        names.forEach { name -> result[name] = members.getValue(name) }
        return FoxObjectType(result)
    }
    
    private fun FoxObjectType.dropMembers(names: Iterable<String>): FoxObjectType {
        val removed = names.toSet()
        val result = LinkedHashMap<String, FoxType>()
        members.forEach { (name, type) -> if (name !in removed) result[name] = type }
        return FoxObjectType(result)
    }
    
    private fun Iterable<FoxObjectType>.mergeObjectMembers(): FoxObjectType {
        val result = LinkedHashMap<String, FoxType>()
        forEach { obj -> obj.members.forEach { (name, type) -> result[name] = type } }
        return FoxObjectType(result)
    }
    
    private fun FoxStructType.selectFields(names: Iterable<String>): FoxStructType {
        val result = mutableOrderedMapOf<String, FoxType>()
        names.forEach { name -> result[name] = fields.getValue(name) }
        return FoxStructType(result)
    }
    
    private fun FoxStructType.dropFields(names: Iterable<String>): FoxStructType {
        val removed = names.toSet()
        val result = mutableOrderedMapOf<String, FoxType>()
        fields.entries.forEach { (name, type) -> if (name !in removed) result[name] = type }
        return FoxStructType(result)
    }
    
    private fun Iterable<FoxStructType>.mergeStructFields(): FoxStructType {
        val result = mutableOrderedMapOf<String, FoxType>()
        forEach { struct -> struct.fields.entries.forEach { (name, type) -> result[name] = type } }
        return FoxStructType(result)
    }
    
    private fun <T> List<SequencePatternPart<T>>.compact(): List<SequencePatternPart<T>> {
        val result = mutableListOf<SequencePatternPart<T>>()
        for (part in this) {
            when (part) {
                SequencePatternPart.Gap -> if (result.lastOrNull() !== SequencePatternPart.Gap) result += SequencePatternPart.Gap
                is SequencePatternPart.Exact -> {
                    if (part.values.isEmpty()) continue
                    val last = result.lastOrNull()
                    if (last is SequencePatternPart.Exact) result[result.lastIndex] = SequencePatternPart.Exact(last.values + part.values)
                    else result += part
                }
            }
        }
        return result
    }
}
