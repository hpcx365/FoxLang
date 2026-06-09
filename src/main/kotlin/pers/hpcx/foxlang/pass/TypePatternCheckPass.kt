package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.*

enum class TypePatternIssueCode(
    override val key: String,
) : TypePassIssueCode {
    INVALID_PATTERN_FAMILY("type.pattern.invalid_family"),
    INVALID_PATTERN_SHAPE("type.pattern.invalid_shape"),
    EMPTY_PATTERN_BOUNDS("type.pattern.empty_bounds"),
    EMPTY_PATTERN_MERGE("type.pattern.empty_merge"),
    CONFLICTING_PATTERN_CONSTRAINT("type.pattern.conflicting_constraint"),
}

object TypePatternCheckPass : FoxTypePass {
    override fun run(input: FoxType, context: TypePassContext): TypePassResult<FoxType> {
        return TypePassResult(input, collectIssues(input))
    }
    
    private fun collectIssues(type: FoxType): List<TypePassIssue> {
        val issues = mutableListOf<TypePassIssue>()
        visit(type, issues)
        return issues
    }
    
    private fun visit(type: FoxType, issues: MutableList<TypePassIssue>) {
        when (type) {
            is FoxPrimitiveType,
            is FoxWildcardType,
            is FoxCustomizedType,
                -> Unit
            
            is FoxTupleType -> type.components.forEach { (component, _) -> visit(component, issues) }
            is FoxStructType -> type.fields.values.forEach { visit(it, issues) }
            is FoxObjectType -> type.members.values.forEach { visit(it, issues) }
            is FoxEnumType -> type.items.values.forEach { visit(it, issues) }
            is FoxArrayType -> visit(type.element, issues)
            is FoxRefType -> visit(type.referent, issues)
            is FoxMethodType -> {
                visit(type.`this`, issues)
                type.parameters.forEach { visit(it, issues) }
                visit(type.`return`, issues)
            }
            
            is FoxTransformType -> {
                checkTransform(type, issues)
                nestedTypesOf(type).forEach { visit(it, issues) }
            }
        }
    }
    
    private fun checkTransform(type: FoxTransformType, issues: MutableList<TypePassIssue>) {
        when (type) {
            is FoxTupleComponentAtType -> {
                expectTargetFamily(type.type, TypePatternFamily.TUPLE, issues, type)
                checkNonNegativeIndex(type.index, "tuple component", issues, type)
                checkBounds(classifyTupleIndexBounds(type.type, type.index), issues, type)
            }
            is FoxTupleLastComponentAtType -> {
                expectTargetFamily(type.type, TypePatternFamily.TUPLE, issues, type)
                checkNonNegativeIndex(type.index, "tuple component", issues, type)
                checkBounds(classifyTupleLastIndexBounds(type.type, type.index), issues, type)
            }
            is FoxTupleFirstComponentsOfType -> {
                expectTargetFamily(type.type, TypePatternFamily.TUPLE, issues, type)
                checkNonNegativeCount(type.count, "tuple component", issues, type)
                checkBounds(classifyTupleCountBounds(type.type, type.count), issues, type)
            }
            is FoxTupleLastComponentsOfType -> {
                expectTargetFamily(type.type, TypePatternFamily.TUPLE, issues, type)
                checkNonNegativeCount(type.count, "tuple component", issues, type)
                checkBounds(classifyTupleCountBounds(type.type, type.count), issues, type)
            }
            is FoxTupleDropFirstComponentsOfType -> {
                expectTargetFamily(type.type, TypePatternFamily.TUPLE, issues, type)
                checkNonNegativeCount(type.count, "tuple component", issues, type)
                checkBounds(classifyTupleCountBounds(type.type, type.count), issues, type)
            }
            is FoxTupleDropLastComponentsOfType -> {
                expectTargetFamily(type.type, TypePatternFamily.TUPLE, issues, type)
                checkNonNegativeCount(type.count, "tuple component", issues, type)
                checkBounds(classifyTupleCountBounds(type.type, type.count), issues, type)
            }
            is FoxTupleMergeComponentsOfType -> {
                warnIfEmptyList(type.types, "MergeComponentsOf", "Tuple<>", issues, type)
                type.types.forEach { expectTargetFamily(it, TypePatternFamily.TUPLE, issues, type) }
            }
            
            is FoxStructFieldOfType -> expectTargetFamily(type.type, TypePatternFamily.STRUCT, issues, type)
            is FoxStructFieldAtType -> {
                expectTargetFamily(type.type, TypePatternFamily.STRUCT, issues, type)
                checkNonNegativeIndex(type.index, "struct field", issues, type)
                checkBounds(classifyStructIndexBounds(type.type, type.index), issues, type)
            }
            is FoxStructLastFieldAtType -> {
                expectTargetFamily(type.type, TypePatternFamily.STRUCT, issues, type)
                checkNonNegativeIndex(type.index, "struct field", issues, type)
                checkBounds(classifyStructLastIndexBounds(type.type, type.index), issues, type)
            }
            is FoxStructFirstFieldsOfType -> {
                expectTargetFamily(type.type, TypePatternFamily.STRUCT, issues, type)
                checkNonNegativeCount(type.count, "struct field", issues, type)
                checkBounds(classifyStructCountBounds(type.type, type.count), issues, type)
            }
            is FoxStructLastFieldsOfType -> {
                expectTargetFamily(type.type, TypePatternFamily.STRUCT, issues, type)
                checkNonNegativeCount(type.count, "struct field", issues, type)
                checkBounds(classifyStructCountBounds(type.type, type.count), issues, type)
            }
            is FoxStructDropFirstFieldsOfType -> {
                expectTargetFamily(type.type, TypePatternFamily.STRUCT, issues, type)
                checkNonNegativeCount(type.count, "struct field", issues, type)
                checkBounds(classifyStructCountBounds(type.type, type.count), issues, type)
            }
            is FoxStructDropLastFieldsOfType -> {
                expectTargetFamily(type.type, TypePatternFamily.STRUCT, issues, type)
                checkNonNegativeCount(type.count, "struct field", issues, type)
                checkBounds(classifyStructCountBounds(type.type, type.count), issues, type)
            }
            is FoxStructFieldsOfType -> expectTargetFamily(type.type, TypePatternFamily.STRUCT, issues, type)
            is FoxStructDropFieldsOfType -> expectTargetFamily(type.type, TypePatternFamily.STRUCT, issues, type)
            is FoxStructMergeFieldsOfType -> {
                warnIfEmptyList(type.types, "MergeFieldsOf", "Struct<>", issues, type)
                type.types.forEach { expectTargetFamily(it, TypePatternFamily.STRUCT, issues, type) }
                checkStructFieldConflicts(type, issues)
            }
            
            is FoxObjectMemberOfType -> expectTargetFamily(type.type, TypePatternFamily.OBJECT, issues, type)
            is FoxObjectMembersOfType -> expectTargetFamily(type.type, TypePatternFamily.OBJECT, issues, type)
            is FoxObjectDropMembersOfType -> expectTargetFamily(type.type, TypePatternFamily.OBJECT, issues, type)
            is FoxObjectMergeMembersOfType -> {
                warnIfEmptyList(type.types, "MergeMembersOf", "Object<>", issues, type)
                type.types.forEach { expectTargetFamily(it, TypePatternFamily.OBJECT, issues, type) }
                checkObjectMemberConflicts(type, issues)
            }
            
            is FoxEnumItemOfType -> expectTargetFamily(type.type, TypePatternFamily.ENUM, issues, type)
            is FoxEnumItemsOfType -> expectTargetFamily(type.type, TypePatternFamily.ENUM, issues, type)
            is FoxEnumDropItemsOfType -> expectTargetFamily(type.type, TypePatternFamily.ENUM, issues, type)
            is FoxEnumMergeItemsOfType -> {
                warnIfEmptyList(type.types, "MergeItemsOf", "Enum<>", issues, type)
                type.types.forEach { expectTargetFamily(it, TypePatternFamily.ENUM, issues, type) }
                checkEnumItemConflicts(type, issues)
            }
            
            is FoxArrayElementOfType -> expectTargetFamily(type.type, TypePatternFamily.ARRAY, issues, type)
            is FoxRefReferentOfType -> expectTargetFamily(type.type, TypePatternFamily.REF, issues, type)
            is FoxMethodThisOfType -> expectTargetFamily(type.type, TypePatternFamily.METHOD, issues, type)
            is FoxMethodParametersOfType -> expectTargetFamily(type.type, TypePatternFamily.METHOD, issues, type)
            is FoxMethodReturnOfType -> expectTargetFamily(type.type, TypePatternFamily.METHOD, issues, type)
        }
    }
    
    private fun nestedTypesOf(type: FoxTransformType): List<FoxType> = when (type) {
        is FoxTupleComponentAtType -> listOf(type.type)
        is FoxTupleLastComponentAtType -> listOf(type.type)
        is FoxTupleFirstComponentsOfType -> listOf(type.type)
        is FoxTupleLastComponentsOfType -> listOf(type.type)
        is FoxTupleDropFirstComponentsOfType -> listOf(type.type)
        is FoxTupleDropLastComponentsOfType -> listOf(type.type)
        is FoxTupleMergeComponentsOfType -> type.types
        is FoxStructFieldOfType -> listOf(type.type)
        is FoxStructFieldAtType -> listOf(type.type)
        is FoxStructLastFieldAtType -> listOf(type.type)
        is FoxStructFirstFieldsOfType -> listOf(type.type)
        is FoxStructLastFieldsOfType -> listOf(type.type)
        is FoxStructDropFirstFieldsOfType -> listOf(type.type)
        is FoxStructDropLastFieldsOfType -> listOf(type.type)
        is FoxStructFieldsOfType -> listOf(type.type)
        is FoxStructDropFieldsOfType -> listOf(type.type)
        is FoxStructMergeFieldsOfType -> type.types
        is FoxObjectMemberOfType -> listOf(type.type)
        is FoxObjectMembersOfType -> listOf(type.type)
        is FoxObjectDropMembersOfType -> listOf(type.type)
        is FoxObjectMergeMembersOfType -> type.types
        is FoxEnumItemOfType -> listOf(type.type)
        is FoxEnumItemsOfType -> listOf(type.type)
        is FoxEnumDropItemsOfType -> listOf(type.type)
        is FoxEnumMergeItemsOfType -> type.types
        is FoxArrayElementOfType -> listOf(type.type)
        is FoxRefReferentOfType -> listOf(type.type)
        is FoxMethodThisOfType -> listOf(type.type)
        is FoxMethodParametersOfType -> listOf(type.type)
        is FoxMethodReturnOfType -> listOf(type.type)
    }
    
    private fun expectTargetFamily(
        type: FoxType,
        expected: TypePatternFamily,
        issues: MutableList<TypePassIssue>,
        owner: FoxType,
    ) {
        val actual = familyOf(type)
        if (actual != expected) {
            issues += TypePassIssue(
                code = TypePatternIssueCode.INVALID_PATTERN_FAMILY,
                severity = TypePassIssueSeverity.ERROR,
                message = "Expected ${expected.label} pattern, found ${actual.label}",
                type = owner,
            )
        }
    }
    
    private fun warnIfEmptyList(
        values: List<FoxType>,
        shapeName: String,
        normalizedForm: String,
        issues: MutableList<TypePassIssue>,
        owner: FoxType,
    ) {
        if (values.isEmpty()) {
            issues += TypePassIssue(
                code = TypePatternIssueCode.EMPTY_PATTERN_MERGE,
                severity = TypePassIssueSeverity.WARNING,
                message = "$shapeName<> is defined as $normalizedForm",
                type = owner,
            )
        }
    }
    
    private fun checkNonNegativeIndex(
        index: Int,
        subject: String,
        issues: MutableList<TypePassIssue>,
        owner: FoxType,
    ) {
        if (index < 0) {
            issues += TypePassIssue(
                code = TypePatternIssueCode.INVALID_PATTERN_SHAPE,
                severity = TypePassIssueSeverity.ERROR,
                message = "${subject.replaceFirstChar(Char::uppercaseChar)} index must be non-negative: $index",
                type = owner,
            )
        }
    }
    
    private fun checkNonNegativeCount(
        count: Int,
        subject: String,
        issues: MutableList<TypePassIssue>,
        owner: FoxType,
    ) {
        if (count < 0) {
            issues += TypePassIssue(
                code = TypePatternIssueCode.INVALID_PATTERN_SHAPE,
                severity = TypePassIssueSeverity.ERROR,
                message = "${subject.replaceFirstChar(Char::uppercaseChar)} count must be non-negative: $count",
                type = owner,
            )
        }
    }
    
    private fun checkBounds(
        bounds: BoundsCheck,
        issues: MutableList<TypePassIssue>,
        owner: FoxType,
    ) {
        when (bounds) {
            BoundsCheck.KnownValid,
            BoundsCheck.Unknown,
                -> Unit
            
            is BoundsCheck.KnownInvalid -> issues += TypePassIssue(
                code = TypePatternIssueCode.EMPTY_PATTERN_BOUNDS,
                severity = TypePassIssueSeverity.WARNING,
                message = bounds.reason,
                type = owner,
            )
        }
    }
    
    private fun checkStructFieldConflicts(
        type: FoxStructMergeFieldsOfType,
        issues: MutableList<TypePassIssue>,
    ) {
        val seen = LinkedHashSet<String>()
        collectStructFieldNames(type, seen, issues, type)
    }
    
    private fun collectStructFieldNames(
        type: FoxType,
        seen: MutableSet<String>,
        issues: MutableList<TypePassIssue>,
        owner: FoxType,
    ) {
        when (type) {
            FoxAnyStructType -> Unit
            is FoxStructType -> type.fields.keys.forEach { name ->
                if (!seen.add(name)) {
                    issues += TypePassIssue(
                        code = TypePatternIssueCode.CONFLICTING_PATTERN_CONSTRAINT,
                        severity = TypePassIssueSeverity.WARNING,
                        message = "Struct field '$name' is constrained multiple times in MergeFieldsOf",
                        type = owner,
                    )
                }
            }
            is FoxStructMergeFieldsOfType -> type.types.forEach { collectStructFieldNames(it, seen, issues, owner) }
            else -> Unit
        }
    }
    
    private fun checkObjectMemberConflicts(
        type: FoxObjectMergeMembersOfType,
        issues: MutableList<TypePassIssue>,
    ) {
        val merged = LinkedHashMap<String, FoxType>()
        collectObjectMemberConstraints(type, merged, issues, type)
    }
    
    private fun collectObjectMemberConstraints(
        type: FoxType,
        merged: MutableMap<String, FoxType>,
        issues: MutableList<TypePassIssue>,
        owner: FoxType,
    ) {
        when (type) {
            FoxAnyObjectType -> Unit
            is FoxObjectType -> type.members.forEach { (name, value) ->
                mergeNamedConstraint(merged, name, value, "object member", issues, owner)
            }
            is FoxObjectMergeMembersOfType -> type.types.forEach { collectObjectMemberConstraints(it, merged, issues, owner) }
            else -> Unit
        }
    }
    
    private fun checkEnumItemConflicts(
        type: FoxEnumMergeItemsOfType,
        issues: MutableList<TypePassIssue>,
    ) {
        val merged = LinkedHashMap<String, FoxType>()
        collectEnumItemConstraints(type, merged, issues, type)
    }
    
    private fun collectEnumItemConstraints(
        type: FoxType,
        merged: MutableMap<String, FoxType>,
        issues: MutableList<TypePassIssue>,
        owner: FoxType,
    ) {
        when (type) {
            FoxAnyEnumType -> Unit
            is FoxEnumType -> type.items.forEach { (name, value) ->
                mergeNamedConstraint(merged, name, value, "enum item", issues, owner)
            }
            is FoxEnumMergeItemsOfType -> type.types.forEach { collectEnumItemConstraints(it, merged, issues, owner) }
            else -> Unit
        }
    }
    
    private fun mergeNamedConstraint(
        merged: MutableMap<String, FoxType>,
        name: String,
        incoming: FoxType,
        subject: String,
        issues: MutableList<TypePassIssue>,
        owner: FoxType,
    ) {
        val existing = merged[name]
        if (existing == null) {
            merged[name] = incoming
            return
        }
        when {
            existing == incoming -> Unit
            TypeCheckPass.matches(existing, incoming) -> Unit
            TypeCheckPass.matches(incoming, existing) -> merged[name] = incoming
            else -> issues += TypePassIssue(
                code = TypePatternIssueCode.CONFLICTING_PATTERN_CONSTRAINT,
                severity = TypePassIssueSeverity.WARNING,
                message = "${subject.replaceFirstChar(Char::uppercaseChar)} '$name' has incompatible constraints: $existing vs $incoming",
                type = owner,
            )
        }
    }
    
    private fun classifyTupleIndexBounds(type: FoxType, index: Int): BoundsCheck {
        val parts = when (val normalized = TypeNormalizePass.normalize(type)) {
            FoxAnyTupleType,
            is FoxTupleType,
            is FoxTupleMergeComponentsOfType,
                -> normalizeTuplePattern(normalized)
            
            else -> return BoundsCheck.Unknown
        }
        return classifyForwardIndexBounds(parts, index, "Tuple component index out of bounds: $index")
    }
    
    private fun classifyTupleLastIndexBounds(type: FoxType, index: Int): BoundsCheck {
        val parts = when (val normalized = TypeNormalizePass.normalize(type)) {
            FoxAnyTupleType,
            is FoxTupleType,
            is FoxTupleMergeComponentsOfType,
                -> normalizeTuplePattern(normalized)
            
            else -> return BoundsCheck.Unknown
        }
        return classifyBackwardIndexBounds(parts, index, "Tuple component index out of bounds: $index")
    }
    
    private fun classifyTupleCountBounds(type: FoxType, count: Int): BoundsCheck {
        val parts = when (val normalized = TypeNormalizePass.normalize(type)) {
            FoxAnyTupleType,
            is FoxTupleType,
            is FoxTupleMergeComponentsOfType,
                -> normalizeTuplePattern(normalized)
            
            else -> return BoundsCheck.Unknown
        }
        return classifyCountBounds(parts, count, "Tuple component count out of bounds: $count")
    }
    
    private fun classifyStructIndexBounds(type: FoxType, index: Int): BoundsCheck {
        val parts = when (val normalized = TypeNormalizePass.normalize(type)) {
            FoxAnyStructType,
            is FoxStructType,
            is FoxStructMergeFieldsOfType,
                -> normalizeStructPattern(normalized)
            
            else -> return BoundsCheck.Unknown
        }
        return classifyForwardIndexBounds(parts, index, "Struct field index out of bounds: $index")
    }
    
    private fun classifyStructLastIndexBounds(type: FoxType, index: Int): BoundsCheck {
        val parts = when (val normalized = TypeNormalizePass.normalize(type)) {
            FoxAnyStructType,
            is FoxStructType,
            is FoxStructMergeFieldsOfType,
                -> normalizeStructPattern(normalized)
            
            else -> return BoundsCheck.Unknown
        }
        return classifyBackwardIndexBounds(parts, index, "Struct field index out of bounds: $index")
    }
    
    private fun classifyStructCountBounds(type: FoxType, count: Int): BoundsCheck {
        val parts = when (val normalized = TypeNormalizePass.normalize(type)) {
            FoxAnyStructType,
            is FoxStructType,
            is FoxStructMergeFieldsOfType,
                -> normalizeStructPattern(normalized)
            
            else -> return BoundsCheck.Unknown
        }
        return classifyCountBounds(parts, count, "Struct field count out of bounds: $count")
    }
    
    private fun <T> classifyForwardIndexBounds(
        parts: List<SequencePatternPart<T>>,
        index: Int,
        message: String,
    ): BoundsCheck {
        var consumed = 0
        for (part in parts) {
            when (part) {
                SequencePatternPart.Gap -> return BoundsCheck.Unknown
                is SequencePatternPart.Exact -> {
                    val next = consumed + part.values.size
                    if (index < next) return BoundsCheck.KnownValid
                    consumed = next
                }
            }
        }
        return BoundsCheck.KnownInvalid(message)
    }
    
    private fun <T> classifyBackwardIndexBounds(
        parts: List<SequencePatternPart<T>>,
        index: Int,
        message: String,
    ): BoundsCheck {
        var consumed = 0
        for (part in parts.asReversed()) {
            when (part) {
                SequencePatternPart.Gap -> return BoundsCheck.Unknown
                is SequencePatternPart.Exact -> {
                    val next = consumed + part.values.size
                    if (index < next) return BoundsCheck.KnownValid
                    consumed = next
                }
            }
        }
        return BoundsCheck.KnownInvalid(message)
    }
    
    private fun <T> classifyCountBounds(
        parts: List<SequencePatternPart<T>>,
        count: Int,
        message: String,
    ): BoundsCheck {
        val deterministicSize = parts.sumOf { part -> if (part is SequencePatternPart.Exact) part.values.size else 0 }
        val hasGap = parts.any { it === SequencePatternPart.Gap }
        return when {
            count <= deterministicSize -> BoundsCheck.KnownValid
            hasGap -> BoundsCheck.Unknown
            else -> BoundsCheck.KnownInvalid(message)
        }
    }
    
    private fun FoxTupleType.expandedTypes(): List<FoxType> {
        val result = ArrayList<FoxType>(size)
        components.forEach { (component, count) -> repeat(count) { result += component } }
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
    
    private fun familyOf(type: FoxType): TypePatternFamily = when (type) {
        is FoxPrimitiveType -> TypePatternFamily.PRIMITIVE
        FoxAnyType -> TypePatternFamily.ANY
        FoxAnyTupleType,
        is FoxTupleType,
        is FoxTupleLastComponentAtType,
        is FoxTupleFirstComponentsOfType,
        is FoxTupleLastComponentsOfType,
        is FoxTupleDropFirstComponentsOfType,
        is FoxTupleDropLastComponentsOfType,
        is FoxTupleMergeComponentsOfType,
        is FoxMethodParametersOfType,
            -> TypePatternFamily.TUPLE
        
        FoxAnyStructType,
        is FoxStructType,
        is FoxStructLastFieldAtType,
        is FoxStructFirstFieldsOfType,
        is FoxStructLastFieldsOfType,
        is FoxStructDropFirstFieldsOfType,
        is FoxStructDropLastFieldsOfType,
        is FoxStructFieldsOfType,
        is FoxStructDropFieldsOfType,
        is FoxStructMergeFieldsOfType,
            -> TypePatternFamily.STRUCT
        
        FoxAnyObjectType,
        is FoxObjectType,
        is FoxObjectMembersOfType,
        is FoxObjectDropMembersOfType,
        is FoxObjectMergeMembersOfType,
            -> TypePatternFamily.OBJECT
        
        FoxAnyEnumType,
        is FoxEnumType,
        is FoxEnumItemsOfType,
        is FoxEnumDropItemsOfType,
        is FoxEnumMergeItemsOfType,
            -> TypePatternFamily.ENUM
        
        FoxAnyArrayType,
        is FoxArrayType,
            -> TypePatternFamily.ARRAY
        
        FoxAnyRefType,
        is FoxRefType,
            -> TypePatternFamily.REF
        
        FoxAnyMethodType,
        is FoxMethodType,
            -> TypePatternFamily.METHOD
        
        is FoxCustomizedType -> TypePatternFamily.CUSTOMIZED
        
        is FoxTupleComponentAtType,
        is FoxStructFieldOfType,
        is FoxStructFieldAtType,
        is FoxObjectMemberOfType,
        is FoxEnumItemOfType,
        is FoxArrayElementOfType,
        is FoxRefReferentOfType,
        is FoxMethodThisOfType,
        is FoxMethodReturnOfType,
            -> TypePatternFamily.UNKNOWN
    }
    
    private enum class TypePatternFamily(
        val label: String,
    ) {
        ANY("any"),
        PRIMITIVE("primitive"),
        TUPLE("tuple"),
        STRUCT("struct"),
        OBJECT("object"),
        ENUM("enum"),
        ARRAY("array"),
        REF("ref"),
        METHOD("method"),
        CUSTOMIZED("customized"),
        UNKNOWN("unknown"),
    }
    
    private sealed interface SequencePatternPart<out T> {
        data object Gap : SequencePatternPart<Nothing>
        
        data class Exact<T>(
            val values: List<T>,
        ) : SequencePatternPart<T>
    }
    
    private sealed interface BoundsCheck {
        data object KnownValid : BoundsCheck
        data object Unknown : BoundsCheck
        data class KnownInvalid(
            val reason: String,
        ) : BoundsCheck
    }
}
