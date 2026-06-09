package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.*

data class TypeCheckRequest(
    val actual: FoxType,
    val expected: FoxType,
)

data class TypeCheckResult(
    val actual: FoxType,
    val expected: FoxType,
    val matched: Boolean,
)

object TypeCheckPass : TypePass<TypeCheckRequest, TypeCheckResult> {
    override fun run(input: TypeCheckRequest, context: TypePassContext): TypePassResult<TypeCheckResult> {
        return TypePassResult(
            TypeCheckResult(
                actual = input.actual,
                expected = input.expected,
                matched = matches(input.actual, input.expected),
            ),
        )
    }
    
    fun matches(actual: FoxType, pattern: FoxType): Boolean = when (pattern) {
        FoxAnyType -> true
        is FoxPrimitiveType -> actual == pattern
        
        FoxAnyTupleType,
        is FoxTupleType,
        is FoxTupleMergeComponentsOfType,
            -> actual is FoxTupleType && matchesTupleConstraint(actual, pattern)
        
        FoxAnyStructType,
        is FoxStructType,
        is FoxStructMergeFieldsOfType,
            -> actual is FoxStructType && matchesStructConstraint(actual, pattern)
        
        FoxAnyObjectType,
        is FoxObjectType,
        is FoxObjectMergeMembersOfType,
            -> actual is FoxObjectType && matchesObjectConstraint(actual, pattern)
        
        FoxAnyEnumType,
        is FoxEnumType,
        is FoxEnumMergeItemsOfType,
            -> actual is FoxEnumType && matchesEnumConstraint(actual, pattern)
        
        FoxAnyArrayType -> actual is FoxArrayType
        is FoxArrayType -> actual is FoxArrayType && matches(actual.element, pattern.element)
        
        FoxAnyRefType -> actual is FoxRefType
        is FoxRefType -> actual is FoxRefType && matches(actual.referent, pattern.referent)
        
        FoxAnyMethodType -> actual is FoxMethodType
        is FoxMethodType -> actual is FoxMethodType &&
            matches(actual.`this`, pattern.`this`) &&
            actual.parameters.size == pattern.parameters.size &&
            actual.parameters.zip(pattern.parameters).all { (left, right) -> matches(left, right) } &&
            matches(actual.`return`, pattern.`return`)
        
        else -> actual == pattern
    }
    
    private fun matchesTupleConstraint(actual: FoxTupleType, pattern: FoxType): Boolean {
        return matchesSequencePattern(
            actual = actual.expandedTypes(),
            parts = normalizeTuplePattern(pattern),
            exactMatches = ::matches,
        )
    }
    
    private fun matchesStructConstraint(actual: FoxStructType, pattern: FoxType): Boolean {
        return matchesSequencePattern(
            actual = actual.fields.entries.map { it.key to it.value },
            parts = normalizeStructPattern(pattern),
            exactMatches = { actualField, expectedField ->
                actualField.first == expectedField.first && matches(actualField.second, expectedField.second)
            },
        )
    }
    
    private fun matchesObjectConstraint(actual: FoxObjectType, pattern: FoxType): Boolean {
        val normalized = normalizeObjectPattern(pattern)
        if (normalized.requiredMembers.any { (name, type) -> !matches(actual.members[name] ?: return false, type) }) {
            return false
        }
        return normalized.allowExtra || actual.members.keys == normalized.requiredMembers.keys
    }
    
    private fun matchesEnumConstraint(actual: FoxEnumType, pattern: FoxType): Boolean {
        val normalized = normalizeEnumPattern(pattern)
        if (normalized.requiredItems.any { (name, type) -> !matches(actual.items[name] ?: return false, type) }) {
            return false
        }
        return normalized.allowExtra || actual.items.keys == normalized.requiredItems.keys
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
            matches(existing, incoming) -> existing
            matches(incoming, existing) -> incoming
            else -> error("Conflicting constraints for $subject '$name': $existing vs $incoming")
        }
    }
    
    private fun <A, P> matchesSequencePattern(
        actual: List<A>,
        parts: List<SequencePatternPart<P>>,
        exactMatches: (A, P) -> Boolean,
    ): Boolean {
        val memo = HashMap<Pair<Int, Int>, Boolean>()
        
        fun dp(actualIndex: Int, partIndex: Int): Boolean {
            val key = actualIndex to partIndex
            memo[key]?.let { return it }
            
            val result = when {
                partIndex == parts.size -> actualIndex == actual.size
                parts[partIndex] === SequencePatternPart.Gap -> (actualIndex..actual.size).any { nextIndex -> dp(nextIndex, partIndex + 1) }
                else -> {
                    val part = parts[partIndex] as SequencePatternPart.Exact<P>
                    val values = part.values
                    if (actualIndex + values.size > actual.size) false
                    else values.indices.all { offset -> exactMatches(actual[actualIndex + offset], values[offset]) } &&
                        dp(actualIndex + values.size, partIndex + 1)
                }
            }
            
            memo[key] = result
            return result
        }
        
        return dp(0, 0)
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
}
