package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.*
import pers.hpcx.foxlang.utils.mapValues
import kotlin.math.min

sealed interface ConstraintCompilePrecheckResult
data object ConstraintCompilePrecheckSuccess : ConstraintCompilePrecheckResult
data class ConstraintCompilePrecheckFailure(val errors: List<ConstraintCompilePrecheckError>) : ConstraintCompilePrecheckResult

sealed interface ConstraintCompilePrecheckError

data class ConstraintCompileUnknownGenericReference(
    val method: FoxMethodDefinition,
    val type: FoxUnresolvedType,
) : ConstraintCompilePrecheckError

data class ConstraintCompileParameterizedGenericReference(
    val method: FoxMethodDefinition,
    val type: FoxUnresolvedType,
) : ConstraintCompilePrecheckError

data class ConstraintCompileTargetFamilyMismatch(
    val method: FoxMethodDefinition,
    val transform: FoxTransformType,
    val target: FoxType,
    val expectedFamily: ConcreteTypeFamily,
    val actualFamily: ConcreteTypeFamily?,
) : ConstraintCompilePrecheckError

data class ConstraintCompileGenericFamilyConflict(
    val method: FoxMethodDefinition,
    val generic: String,
    val families: Set<ConcreteTypeFamily>,
) : ConstraintCompilePrecheckError

data class ConstraintCompileProjectionBaseMustBeConcrete(
    val method: FoxMethodDefinition,
    val transform: FoxTransformType,
    val base: FoxType,
) : ConstraintCompilePrecheckError

fun runConstraintCompilePrecheck(file: FoxFile): ConstraintCompilePrecheckResult {
    val errors = file.elements
        .filterIsInstance<FoxMethodDefinition>()
        .flatMap(::collectConstraintCompilePrecheckErrors)
    return if (errors.isEmpty()) ConstraintCompilePrecheckSuccess else ConstraintCompilePrecheckFailure(errors)
}

fun runConstraintCompilePrecheck(method: FoxMethodDefinition): ConstraintCompilePrecheckResult {
    val errors = collectConstraintCompilePrecheckErrors(method)
    return if (errors.isEmpty()) ConstraintCompilePrecheckSuccess else ConstraintCompilePrecheckFailure(errors)
}

private fun collectConstraintCompilePrecheckErrors(method: FoxMethodDefinition): List<ConstraintCompilePrecheckError> {
    val genericNames = method.generics.map { it.key }.toSet()
    val genericFamilies = inferGenericFamilies(method, genericNames)
    val errors = mutableListOf<ConstraintCompilePrecheckError>()
    val analyzer = ConstraintCompilePrecheckAnalyzer(method, genericNames, genericFamilies, errors)
    
    genericFamilies.forEach { (generic, family) ->
        if (family is ConflictConstraintFamily) {
            errors += ConstraintCompileGenericFamilyConflict(method, generic, family.families)
        }
    }
    method.generics.forEach { analyzer.compile(it.value) }
    
    return errors
}

private fun inferGenericFamilies(
    method: FoxMethodDefinition,
    genericNames: Set<String>,
): Map<String, ConstraintCompileFamily> {
    var families = genericNames.associateWith { UnknownConstraintFamily as ConstraintCompileFamily }
    
    while (true) {
        val analyzer = ConstraintCompilePrecheckAnalyzer(method, genericNames, families, null)
        val next = method.generics.associate { it.key to families.getValue(it.key).join(analyzer.compile(it.value).family) }
        if (next == families) return families
        families = next
    }
}

private sealed interface ConstraintCompileFamily
private data object UnknownConstraintFamily : ConstraintCompileFamily
private data class KnownConstraintFamily(val family: ConcreteTypeFamily) : ConstraintCompileFamily
private data class ConflictConstraintFamily(val families: Set<ConcreteTypeFamily>) : ConstraintCompileFamily {
    init {
        require(families.size >= 2)
    }
}

private fun ConstraintCompileFamily.join(other: ConstraintCompileFamily): ConstraintCompileFamily = when (this) {
    UnknownConstraintFamily -> other
    is KnownConstraintFamily -> when (other) {
        UnknownConstraintFamily -> this
        is KnownConstraintFamily -> if (family == other.family) this else ConflictConstraintFamily(setOf(family, other.family))
        is ConflictConstraintFamily -> ConflictConstraintFamily(other.families + family)
    }
    is ConflictConstraintFamily -> when (other) {
        UnknownConstraintFamily -> this
        is KnownConstraintFamily -> ConflictConstraintFamily(families + other.family)
        is ConflictConstraintFamily -> ConflictConstraintFamily(families + other.families)
    }
}

private sealed interface ConstraintCompileShape {
    val family: ConstraintCompileFamily
}

private data class SingleConstraintShape(
    val exact: FoxType?,
    override val family: ConstraintCompileFamily,
) : ConstraintCompileShape

private data class SpaceConstraintShape(
    override val family: ConstraintCompileFamily,
) : ConstraintCompileShape

private class ConstraintCompilePrecheckAnalyzer(
    private val method: FoxMethodDefinition,
    private val genericNames: Set<String>,
    private val genericFamilies: Map<String, ConstraintCompileFamily>,
    private val errors: MutableList<ConstraintCompilePrecheckError>?,
) {
    
    fun compile(type: FoxType): ConstraintCompileShape = when (type) {
        is FoxPrimitiveType -> SingleConstraintShape(type, knownFamily(ConcreteTypeFamily.PRIMITIVE))
        is FoxBuiltInType -> compileBuiltInType(type)
        is FoxWildcardType -> compileWildcardType(type)
        is FoxTransformType -> compileTransformType(type)
        is FoxUnresolvedType -> compileUnresolvedType(type)
        is FoxPlaceholderType -> error("unreachable")
    }
    
    private fun compileBuiltInType(type: FoxBuiltInType): ConstraintCompileShape = when (type) {
        is FoxTupleType -> {
            val components = type.components.map(::compile)
            if (components.any { it is SpaceConstraintShape }) {
                SpaceConstraintShape(knownFamily(ConcreteTypeFamily.TUPLE))
            } else {
                SingleConstraintShape(
                    components.map { (it as SingleConstraintShape).exact }.toFoxTupleTypeOrNull(),
                    knownFamily(ConcreteTypeFamily.TUPLE),
                )
            }
        }
        is FoxStructType -> {
            val fields = type.fields.map { it.key to compile(it.value) }
            if (fields.any { it.second is SpaceConstraintShape }) {
                SpaceConstraintShape(knownFamily(ConcreteTypeFamily.STRUCT))
            } else {
                SingleConstraintShape(
                    fields.map { it.first to (it.second as SingleConstraintShape).exact }.toFoxStructTypeOrNull(),
                    knownFamily(ConcreteTypeFamily.STRUCT),
                )
            }
        }
        is FoxObjectType -> {
            val members = type.members.map { it.key to compile(it.value) }
            if (members.any { it.second is SpaceConstraintShape }) {
                SpaceConstraintShape(knownFamily(ConcreteTypeFamily.OBJECT))
            } else {
                SingleConstraintShape(
                    members.map { it.first to (it.second as SingleConstraintShape).exact }.toFoxObjectTypeOrNull(),
                    knownFamily(ConcreteTypeFamily.OBJECT),
                )
            }
        }
        is FoxEnumType -> {
            val entries = type.entries.map { it.key to compile(it.value) }
            if (entries.any { it.second is SpaceConstraintShape }) {
                SpaceConstraintShape(knownFamily(ConcreteTypeFamily.ENUM))
            } else {
                SingleConstraintShape(
                    entries.map { it.first to (it.second as SingleConstraintShape).exact }.toFoxEnumTypeOrNull(),
                    knownFamily(ConcreteTypeFamily.ENUM),
                )
            }
        }
        is FoxArrayType -> when (val element = compile(type.element)) {
            is SingleConstraintShape -> SingleConstraintShape(
                element.exact?.let(::FoxArrayType),
                knownFamily(ConcreteTypeFamily.ARRAY),
            )
            is SpaceConstraintShape -> SpaceConstraintShape(knownFamily(ConcreteTypeFamily.ARRAY))
        }
        is FoxRefType -> when (val referent = compile(type.referent)) {
            is SingleConstraintShape -> SingleConstraintShape(
                referent.exact?.let(::FoxRefType),
                knownFamily(ConcreteTypeFamily.REF),
            )
            is SpaceConstraintShape -> SpaceConstraintShape(knownFamily(ConcreteTypeFamily.REF))
        }
        is FoxMethodType -> {
            val thisShape = compile(type.`this`)
            val parameterShapes = type.parameters.mapValues { compile(it.value) }
            val returnShape = compile(type.`return`)
            if (
                thisShape is SingleConstraintShape &&
                parameterShapes.all { it.value is SingleConstraintShape } &&
                returnShape is SingleConstraintShape
            ) {
                val exactParameters = parameterShapes
                    .map { it.key to (it.value as SingleConstraintShape).exact }
                    .toFoxStructFieldsOrNull()
                val exact = if (thisShape.exact != null && exactParameters != null && returnShape.exact != null) {
                    FoxMethodType(thisShape.exact, exactParameters, returnShape.exact)
                } else {
                    null
                }
                SingleConstraintShape(exact, knownFamily(ConcreteTypeFamily.METHOD))
            } else {
                SpaceConstraintShape(knownFamily(ConcreteTypeFamily.METHOD))
            }
        }
    }
    
    private fun compileWildcardType(type: FoxWildcardType): ConstraintCompileShape = when (type) {
        FoxAnyType -> SpaceConstraintShape(UnknownConstraintFamily)
        is FoxAnyOfType -> SpaceConstraintShape(unionFamily(type.types.map { compile(it).family }))
        is FoxAllOfType -> SpaceConstraintShape(intersectionFamily(type.types.map { compile(it).family }))
        is FoxNoneOfType -> {
            type.types.forEach { compile(it) }
            SpaceConstraintShape(UnknownConstraintFamily)
        }
        FoxAnyTupleType -> SpaceConstraintShape(knownFamily(ConcreteTypeFamily.TUPLE))
        is FoxAnyTupleOfType -> {
            compile(type.component)
            SpaceConstraintShape(knownFamily(ConcreteTypeFamily.TUPLE))
        }
        FoxAnyStructType -> SpaceConstraintShape(knownFamily(ConcreteTypeFamily.STRUCT))
        is FoxAnyStructOfType -> {
            type.fields.forEach { compile(it) }
            SpaceConstraintShape(knownFamily(ConcreteTypeFamily.STRUCT))
        }
        FoxAnyObjectType -> SpaceConstraintShape(knownFamily(ConcreteTypeFamily.OBJECT))
        FoxAnyEnumType -> SpaceConstraintShape(knownFamily(ConcreteTypeFamily.ENUM))
    }
    
    private fun compileUnresolvedType(type: FoxUnresolvedType): ConstraintCompileShape {
        type.parameters?.forEach { compile(it) }
        if (type.parameters != null) {
            errors?.add(ConstraintCompileParameterizedGenericReference(method, type))
        }
        if (type.name !in genericNames) {
            errors?.add(ConstraintCompileUnknownGenericReference(method, type))
        }
        return SingleConstraintShape(null, genericFamilies[type.name] ?: UnknownConstraintFamily)
    }
    
    private fun compileTransformType(type: FoxTransformType): ConstraintCompileShape = when (type) {
        is FoxTupleGetComponentType -> compileTupleProjection(type, type.type, SingleConstraintShape(null, UnknownConstraintFamily)) {
            if (type.index !in 0 until it.arity) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.getComponent(type.index))
        }
        is FoxTupleGetComponentBackType -> compileTupleProjection(type, type.type, SingleConstraintShape(null, UnknownConstraintFamily)) {
            if (type.index !in 0 until it.arity) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.getComponentBack(type.index))
        }
        is FoxTupleGetFirstComponentsType -> compileTupleProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.TUPLE))) {
            singleExact(it.getFirstComponents(min(type.count, it.arity)))
        }
        is FoxTupleGetFirstComponentsExactType -> compileTupleProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.TUPLE))) {
            if (type.count !in 0..it.arity) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.getFirstComponents(type.count))
        }
        is FoxTupleGetLastComponentsType -> compileTupleProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.TUPLE))) {
            singleExact(it.getLastComponents(min(type.count, it.arity)))
        }
        is FoxTupleGetLastComponentsExactType -> compileTupleProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.TUPLE))) {
            if (type.count !in 0..it.arity) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.getLastComponents(type.count))
        }
        is FoxTupleDropFirstComponentsType -> compileTupleProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.TUPLE))) {
            singleExact(it.dropFirstComponents(min(type.count, it.arity)))
        }
        is FoxTupleDropFirstComponentsExactType -> compileTupleProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.TUPLE))) {
            if (type.count !in 0..it.arity) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.dropFirstComponents(type.count))
        }
        is FoxTupleDropLastComponentsType -> compileTupleProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.TUPLE))) {
            singleExact(it.dropLastComponents(min(type.count, it.arity)))
        }
        is FoxTupleDropLastComponentsExactType -> compileTupleProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.TUPLE))) {
            if (type.count !in 0..it.arity) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.dropLastComponents(type.count))
        }
        is FoxTupleMergeTuplesType -> compileMergeTransform(
            type,
            type.types,
            ConcreteTypeFamily.TUPLE,
            ConcreteTypeFamily.TUPLE,
        ) { tuples ->
            tuples.flatMap { (it as FoxTupleType).components }.toFoxTupleType()
        }
        
        is FoxStructGetFieldTypeByNameType -> compileStructProjection(type, type.type, SingleConstraintShape(null, UnknownConstraintFamily)) {
            if (type.name !in it.fields) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.getFieldTypeByName(type.name))
        }
        is FoxStructGetFieldTypeByIndexType -> compileStructProjection(type, type.type, SingleConstraintShape(null, UnknownConstraintFamily)) {
            if (type.index !in 0 until it.arity) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.getFieldTypeByIndex(type.index).value)
        }
        is FoxStructGetFieldTypeByIndexBackType -> compileStructProjection(type, type.type, SingleConstraintShape(null, UnknownConstraintFamily)) {
            if (type.index !in 0 until it.arity) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.getFieldTypeByIndexBack(type.index).value)
        }
        is FoxStructGetFirstFieldsType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            singleExact(it.getFirstFields(min(type.count, it.arity)))
        }
        is FoxStructGetFirstFieldsExactType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            if (type.count !in 0..it.arity) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.getFirstFields(type.count))
        }
        is FoxStructGetLastFieldsType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            singleExact(it.getLastFields(min(type.count, it.arity)))
        }
        is FoxStructGetLastFieldsExactType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            if (type.count !in 0..it.arity) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.getLastFields(type.count))
        }
        is FoxStructDropFirstFieldsType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            singleExact(it.dropFirstFields(min(type.count, it.arity)))
        }
        is FoxStructDropFirstFieldsExactType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            if (type.count !in 0..it.arity) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.dropFirstFields(type.count))
        }
        is FoxStructDropLastFieldsType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            singleExact(it.dropLastFields(min(type.count, it.arity)))
        }
        is FoxStructDropLastFieldsExactType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            if (type.count !in 0..it.arity) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.dropLastFields(type.count))
        }
        is FoxStructSelectFieldsType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            singleExact(it.selectFields(type.names))
        }
        is FoxStructSelectFieldsExactType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            if (!type.names.all(it.fields::contains)) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.selectFields(type.names))
        }
        is FoxStructDropFieldsType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            singleExact(it.dropFields(type.names))
        }
        is FoxStructDropFieldsExactType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            if (!type.names.all(it.fields::contains)) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.dropFields(type.names))
        }
        is FoxStructExtractFieldTypesType -> compileStructProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.TUPLE))) {
            singleExact(it.extractFieldTypes())
        }
        is FoxStructMergeStructsType -> compileMergeTransform(
            type,
            type.types,
            ConcreteTypeFamily.STRUCT,
            ConcreteTypeFamily.STRUCT,
        ) { structs ->
            structs.flatMap { (it as FoxStructType).fields.entries }.map { it.key to it.value }.toFoxStructType()
        }
        
        is FoxObjectGetMemberTypeType -> compileObjectProjection(type, type.type, SingleConstraintShape(null, UnknownConstraintFamily)) {
            if (type.name !in it.members) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.getMemberType(type.name))
        }
        is FoxObjectSelectMembersType -> compileObjectProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.OBJECT))) {
            singleExact(it.selectMembers(type.names))
        }
        is FoxObjectSelectMembersExactType -> compileObjectProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.OBJECT))) {
            if (!type.names.all(it.members::contains)) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.selectMembers(type.names))
        }
        is FoxObjectDropMembersType -> compileObjectProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.OBJECT))) {
            singleExact(it.dropMembers(type.names))
        }
        is FoxObjectDropMembersExactType -> compileObjectProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.OBJECT))) {
            if (!type.names.all(it.members::contains)) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.dropMembers(type.names))
        }
        is FoxObjectMergeObjectsType -> compileMergeTransform(
            type,
            type.types,
            ConcreteTypeFamily.OBJECT,
            ConcreteTypeFamily.OBJECT,
        ) { objects ->
            objects.flatMap { (it as FoxObjectType).members.entries }.map { it.key to it.value }.toFoxObjectType()
        }
        
        is FoxEnumGetEntryTypeType -> compileEnumProjection(type, type.type, SingleConstraintShape(null, UnknownConstraintFamily)) {
            if (type.name !in it.entries) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.getEntryType(type.name))
        }
        is FoxEnumSelectEntriesType -> compileEnumProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.ENUM))) {
            singleExact(it.selectEntries(type.names))
        }
        is FoxEnumSelectEntriesExactType -> compileEnumProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.ENUM))) {
            if (!type.names.all(it.entries::contains)) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.selectEntries(type.names))
        }
        is FoxEnumDropEntriesType -> compileEnumProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.ENUM))) {
            singleExact(it.dropEntries(type.names))
        }
        is FoxEnumDropEntriesExactType -> compileEnumProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.ENUM))) {
            if (!type.names.all(it.entries::contains)) SpaceConstraintShape(UnknownConstraintFamily)
            else singleExact(it.dropEntries(type.names))
        }
        is FoxEnumMergeEnumsType -> compileMergeTransform(
            type,
            type.types,
            ConcreteTypeFamily.ENUM,
            ConcreteTypeFamily.ENUM,
        ) { enums ->
            enums.flatMap { (it as FoxEnumType).entries.entries }.map { it.key to it.value }.toFoxEnumType()
        }
        
        is FoxArrayGetElementTypeType -> compileArrayProjection(type, type.type, SingleConstraintShape(null, UnknownConstraintFamily)) {
            singleExact(it.element)
        }
        is FoxRefGetReferentTypeType -> compileRefProjection(type, type.type, SingleConstraintShape(null, UnknownConstraintFamily)) {
            singleExact(it.referent)
        }
        is FoxMethodGetThisTypeType -> compileMethodProjection(type, type.type, SingleConstraintShape(null, UnknownConstraintFamily)) {
            singleExact(it.`this`)
        }
        is FoxMethodGetParameterStructType -> compileMethodProjection(type, type.type, SingleConstraintShape(null, knownFamily(ConcreteTypeFamily.STRUCT))) {
            singleExact(FoxStructType(it.parameters))
        }
        is FoxMethodGetReturnTypeType -> compileMethodProjection(type, type.type, SingleConstraintShape(null, UnknownConstraintFamily)) {
            singleExact(it.`return`)
        }
        is FoxMethodOfType -> compileMethodOf(type)
    }
    
    private fun compileTupleProjection(
        transform: FoxTransformType,
        base: FoxType,
        unknownResult: ConstraintCompileShape,
        exactResult: (FoxTupleType) -> ConstraintCompileShape,
    ) = compileProjection(transform, base, ConcreteTypeFamily.TUPLE, unknownResult, exactResult)
    
    private fun compileStructProjection(
        transform: FoxTransformType,
        base: FoxType,
        unknownResult: ConstraintCompileShape,
        exactResult: (FoxStructType) -> ConstraintCompileShape,
    ) = compileProjection(transform, base, ConcreteTypeFamily.STRUCT, unknownResult, exactResult)
    
    private fun compileObjectProjection(
        transform: FoxTransformType,
        base: FoxType,
        unknownResult: ConstraintCompileShape,
        exactResult: (FoxObjectType) -> ConstraintCompileShape,
    ) = compileProjection(transform, base, ConcreteTypeFamily.OBJECT, unknownResult, exactResult)
    
    private fun compileEnumProjection(
        transform: FoxTransformType,
        base: FoxType,
        unknownResult: ConstraintCompileShape,
        exactResult: (FoxEnumType) -> ConstraintCompileShape,
    ) = compileProjection(transform, base, ConcreteTypeFamily.ENUM, unknownResult, exactResult)
    
    private fun compileArrayProjection(
        transform: FoxTransformType,
        base: FoxType,
        unknownResult: ConstraintCompileShape,
        exactResult: (FoxArrayType) -> ConstraintCompileShape,
    ) = compileProjection(transform, base, ConcreteTypeFamily.ARRAY, unknownResult, exactResult)
    
    private fun compileRefProjection(
        transform: FoxTransformType,
        base: FoxType,
        unknownResult: ConstraintCompileShape,
        exactResult: (FoxRefType) -> ConstraintCompileShape,
    ) = compileProjection(transform, base, ConcreteTypeFamily.REF, unknownResult, exactResult)
    
    private fun compileMethodProjection(
        transform: FoxTransformType,
        base: FoxType,
        unknownResult: ConstraintCompileShape,
        exactResult: (FoxMethodType) -> ConstraintCompileShape,
    ) = compileProjection(transform, base, ConcreteTypeFamily.METHOD, unknownResult, exactResult)
    
    private inline fun <reified T : FoxType> compileProjection(
        transform: FoxTransformType,
        base: FoxType,
        expectedFamily: ConcreteTypeFamily,
        unknownResult: ConstraintCompileShape,
        exactResult: (T) -> ConstraintCompileShape,
    ): ConstraintCompileShape {
        val baseShape = compile(base)
        if (baseShape is SpaceConstraintShape) {
            errors?.add(ConstraintCompileProjectionBaseMustBeConcrete(method, transform, base))
            return unknownResult
        }
        val singleBase = baseShape as SingleConstraintShape
        if (!checkTargetFamily(transform, base, singleBase, expectedFamily)) {
            return unknownResult
        }
        return singleBase.exact?.let { exactResult(it as T) } ?: unknownResult
    }
    
    private fun compileMergeTransform(
        transform: FoxTransformType,
        targets: List<FoxType>,
        expectedFamily: ConcreteTypeFamily,
        resultFamily: ConcreteTypeFamily,
        exactResult: (List<FoxType>) -> FoxType,
    ): ConstraintCompileShape {
        val targetShapes = targets.map { it to compile(it) }
        val targetsMatch = targetShapes.map { (target, shape) ->
            checkTargetFamily(transform, target, shape, expectedFamily)
        }.all { it }
        if (targetShapes.all { it.second is SingleConstraintShape }) {
            val exactTargets = targetShapes.map { (it.second as SingleConstraintShape).exact }
            return SingleConstraintShape(
                if (targetsMatch && exactTargets.all { it != null }) exactResult(exactTargets.filterNotNull()) else null,
                knownFamily(resultFamily),
            )
        }
        return SpaceConstraintShape(knownFamily(resultFamily))
    }
    
    private fun compileMethodOf(type: FoxMethodOfType): ConstraintCompileShape {
        val thisShape = compile(type.`this`)
        val parametersShape = compile(type.parameters)
        val returnShape = compile(type.`return`)
        checkTargetFamily(
            type,
            type.parameters,
            parametersShape,
            ConcreteTypeFamily.STRUCT,
        )
        if (
            thisShape is SingleConstraintShape &&
            parametersShape is SingleConstraintShape &&
            returnShape is SingleConstraintShape
        ) {
            val parameters = parametersShape.exact as? FoxStructType
            val exact = if (thisShape.exact != null && parameters != null && returnShape.exact != null) {
                FoxMethodType(thisShape.exact, parameters.fields, returnShape.exact)
            } else {
                null
            }
            return SingleConstraintShape(exact, knownFamily(ConcreteTypeFamily.METHOD))
        }
        return SpaceConstraintShape(knownFamily(ConcreteTypeFamily.METHOD))
    }
    
    private fun checkTargetFamily(
        transform: FoxTransformType,
        target: FoxType,
        shape: ConstraintCompileShape,
        expectedFamily: ConcreteTypeFamily,
    ): Boolean {
        val actualFamily = shape.family
        val matches = actualFamily == knownFamily(expectedFamily)
        val reportedActual = when (actualFamily) {
            is KnownConstraintFamily -> actualFamily.family
            else -> null
        }
        if (!matches) {
            errors?.add(
                ConstraintCompileTargetFamilyMismatch(
                    method,
                    transform,
                    target,
                    expectedFamily,
                    reportedActual,
                ),
            )
        }
        return matches
    }
    
    private fun singleExact(type: FoxType) = SingleConstraintShape(type, type.constraintCompileFamily())
}

private fun knownFamily(family: ConcreteTypeFamily) = KnownConstraintFamily(family)

private fun FoxType.constraintCompileFamily(): ConstraintCompileFamily = when (this) {
    is FoxPrimitiveType -> knownFamily(ConcreteTypeFamily.PRIMITIVE)
    is FoxTupleType -> knownFamily(ConcreteTypeFamily.TUPLE)
    is FoxStructType -> knownFamily(ConcreteTypeFamily.STRUCT)
    is FoxObjectType -> knownFamily(ConcreteTypeFamily.OBJECT)
    is FoxEnumType -> knownFamily(ConcreteTypeFamily.ENUM)
    is FoxArrayType -> knownFamily(ConcreteTypeFamily.ARRAY)
    is FoxRefType -> knownFamily(ConcreteTypeFamily.REF)
    is FoxMethodType -> knownFamily(ConcreteTypeFamily.METHOD)
    is FoxWildcardType, is FoxTransformType, is FoxUnresolvedType -> UnknownConstraintFamily
    is FoxPlaceholderType -> error("unreachable")
}

private fun unionFamily(families: List<ConstraintCompileFamily>): ConstraintCompileFamily {
    if (families.isEmpty() || families.any { it == UnknownConstraintFamily }) return UnknownConstraintFamily
    if (families.any { it is ConflictConstraintFamily }) {
        return families.fold(UnknownConstraintFamily as ConstraintCompileFamily) { acc, family -> acc.join(family) }
    }
    val known = families.flatMap { family ->
        when (family) {
            UnknownConstraintFamily -> emptyList()
            is KnownConstraintFamily -> listOf(family.family)
            is ConflictConstraintFamily -> family.families.toList()
        }
    }.toSet()
    return if (known.size == 1) knownFamily(known.single()) else UnknownConstraintFamily
}

private fun intersectionFamily(families: List<ConstraintCompileFamily>): ConstraintCompileFamily {
    val known = families.flatMap { family ->
        when (family) {
            UnknownConstraintFamily -> emptyList()
            is KnownConstraintFamily -> listOf(family.family)
            is ConflictConstraintFamily -> family.families.toList()
        }
    }.toSet()
    if (known.isEmpty()) return UnknownConstraintFamily
    return if (known.size == 1) knownFamily(known.single()) else ConflictConstraintFamily(known)
}

private fun List<FoxType?>.toFoxTupleTypeOrNull(): FoxTupleType? {
    if (any { it == null }) return null
    return filterNotNull().toFoxTupleType()
}

private fun List<Pair<String, FoxType?>>.toFoxStructTypeOrNull(): FoxStructType? {
    if (any { it.second == null }) return null
    return map { it.first to it.second!! }.toFoxStructType()
}

private fun List<Pair<String, FoxType?>>.toFoxObjectTypeOrNull(): FoxObjectType? {
    if (any { it.second == null }) return null
    return map { it.first to it.second!! }.toFoxObjectType()
}

private fun List<Pair<String, FoxType?>>.toFoxEnumTypeOrNull(): FoxEnumType? {
    if (any { it.second == null }) return null
    return map { it.first to it.second!! }.toFoxEnumType()
}

private fun List<Pair<String, FoxType?>>.toFoxStructFieldsOrNull() =
    toFoxStructTypeOrNull()?.fields
