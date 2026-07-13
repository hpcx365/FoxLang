package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.mapValues
import pers.hpcx.foxlang.utils.toOrderedMap
import kotlin.math.min

typealias TypeAssignments = Map<String, FoxType>
typealias TypeConstraints = Map<String, FoxType>

fun TypeAssignments.satisfies(constraints: TypeConstraints): Boolean {
    val compiled = ConstraintCompileContext(this).compile(constraints)
    return entries.all { (name, type) -> type.satisfies(compiled.getValue(name)) }
}

private fun FoxType.satisfies(constraint: ConstraintCompileResult) = when (constraint) {
    is ConcreteConstraint -> this == constraint.type
    is SpaceConstraint -> this in constraint.space
}

private sealed interface ConstraintCompileResult
private data class ConcreteConstraint(val type: FoxType) : ConstraintCompileResult
private data class SpaceConstraint(val space: TypeSpace) : ConstraintCompileResult

private val EmptySpaceConstraint = SpaceConstraint(emptyTypeSpace())

private class ConstraintCompileContext(
    val assignments: TypeAssignments,
) {
    
    fun compile(constraints: TypeConstraints) = constraints.mapValues { compile(it.value) }
    
    private fun compile(type: FoxType): ConstraintCompileResult = when (type) {
        is FoxPrimitiveType -> ConcreteConstraint(type)
        is FoxBuiltInType -> compileBuiltInType(type)
        is FoxWildcardType -> compileWildcardType(type)
        is FoxTransformType -> compileTransformType(type)
        is FoxUnresolvedType -> ConcreteConstraint(assignments.getValue(type.name)).also { check(type.parameters == null) }
        is FoxPlaceholderType -> error("unreachable")
    }
    
    private fun compileBuiltInType(type: FoxBuiltInType): ConstraintCompileResult = when (type) {
        is FoxTupleType -> {
            val results = type.components.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                ConcreteConstraint(results.map { (it as ConcreteConstraint).type }.toFoxTupleType())
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(tuplePatternSpace(spaces))
            }
        }
        is FoxStructType -> {
            val results = type.fields.map { it.key to compile(it.value) }
            if (results.all { it.second is ConcreteConstraint }) {
                ConcreteConstraint(results.map { it.first to (it.second as ConcreteConstraint).type }.toFoxStructType())
            } else {
                val spaces = results.map { it.first to it.second.toSpace() }.toOrderedMap()
                SpaceConstraint(structFieldPatternSpace(spaces))
            }
        }
        is FoxObjectType -> {
            val results = type.members.map { it.key to compile(it.value) }
            if (results.all { it.second is ConcreteConstraint }) {
                ConcreteConstraint(results.map { it.first to (it.second as ConcreteConstraint).type }.toFoxObjectType())
            } else {
                val spaces = results.associate { it.first to it.second.toSpace() }
                SpaceConstraint(objectPatternSpace(spaces))
            }
        }
        is FoxEnumType -> {
            val results = type.entries.map { it.key to compile(it.value) }
            if (results.all { it.second is ConcreteConstraint }) {
                ConcreteConstraint(results.map { it.first to (it.second as ConcreteConstraint).type }.toFoxEnumType())
            } else {
                val spaces = results.associate { it.first to it.second.toSpace() }
                SpaceConstraint(enumPatternSpace(spaces))
            }
        }
        is FoxArrayType -> when (val result = compile(type.element)) {
            is ConcreteConstraint -> ConcreteConstraint(FoxArrayType(result.type))
            is SpaceConstraint -> SpaceConstraint(arrayPatternSpace(result.space))
        }
        is FoxRefType -> when (val result = compile(type.referent)) {
            is ConcreteConstraint -> ConcreteConstraint(FoxRefType(result.type))
            is SpaceConstraint -> SpaceConstraint(refPatternSpace(result.space))
        }
        is FoxMethodType -> {
            val thisResult = compile(type.`this`)
            val parameterResults = type.parameters.mapValues { compile(it.value) }
            val returnResult = compile(type.`return`)
            if (
                thisResult is ConcreteConstraint &&
                parameterResults.all { it.value is ConcreteConstraint } &&
                returnResult is ConcreteConstraint
            ) {
                ConcreteConstraint(
                    FoxMethodType(
                        thisResult.type,
                        parameterResults.map { it.key to (it.value as ConcreteConstraint).type }.toOrderedMap(),
                        returnResult.type,
                    ),
                )
            } else {
                val thisSpace = thisResult.toSpace()
                val parameterStructSpace = structFieldPatternSpace(
                    parameterResults.map { it.key to it.value.toSpace() }.toOrderedMap(),
                )
                val returnSpace = returnResult.toSpace()
                SpaceConstraint(methodPatternSpace(thisSpace, parameterStructSpace, returnSpace))
            }
        }
    }
    
    private fun compileWildcardType(type: FoxWildcardType): ConstraintCompileResult = when (type) {
        FoxAnyType -> universalTypeSpace()
        is FoxAnyOfType -> union(type.types.map { compile(it).toSpace() })
        is FoxAllOfType -> intersect(type.types.map { compile(it).toSpace() })
        is FoxNoneOfType -> union(type.types.map { compile(it).toSpace() }).complement()
        FoxAnyTupleType -> tupleSpace()
        is FoxAnyTupleOfType -> tupleRepeatSpace(compile(type.component).toSpace())
        FoxAnyStructType -> structSpace()
        is FoxAnyStructOfType -> structPatternSpace(type.fields.map { compile(it).toSpace() })
        FoxAnyObjectType -> objectSpace()
        FoxAnyEnumType -> enumSpace()
    }.let { SpaceConstraint(it) }
    
    private fun compileTransformType(type: FoxTransformType): ConstraintCompileResult = when (type) {
        is FoxTupleGetComponentType -> compileProjection<FoxTupleType>(type.type) {
            if (type.index !in 0..<it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getComponent(type.index))
        }
        is FoxTupleGetComponentBackType -> compileProjection<FoxTupleType>(type.type) {
            if (type.index !in 0..<it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getComponentBack(type.index))
        }
        is FoxTupleGetFirstComponentsType -> compileProjection<FoxTupleType>(type.type) {
            ConcreteConstraint(it.getFirstComponents(min(type.count, it.arity)))
        }
        is FoxTupleGetFirstComponentsExactType -> compileProjection<FoxTupleType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getFirstComponents(type.count))
        }
        is FoxTupleGetLastComponentsType -> compileProjection<FoxTupleType>(type.type) {
            ConcreteConstraint(it.getLastComponents(min(type.count, it.arity)))
        }
        is FoxTupleGetLastComponentsExactType -> compileProjection<FoxTupleType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getLastComponents(type.count))
        }
        is FoxTupleDropFirstComponentsType -> compileProjection<FoxTupleType>(type.type) {
            ConcreteConstraint(it.dropFirstComponents(min(type.count, it.arity)))
        }
        is FoxTupleDropFirstComponentsExactType -> compileProjection<FoxTupleType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.dropFirstComponents(type.count))
        }
        is FoxTupleDropLastComponentsType -> compileProjection<FoxTupleType>(type.type) {
            ConcreteConstraint(it.dropLastComponents(min(type.count, it.arity)))
        }
        is FoxTupleDropLastComponentsExactType -> compileProjection<FoxTupleType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.dropLastComponents(type.count))
        }
        is FoxTupleMergeTuplesType -> {
            val results = type.types.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                val resultTypes = results.map { (it as ConcreteConstraint).type }
                if (resultTypes.all { it is FoxTupleType }) {
                    val components = resultTypes.flatMap { (it as FoxTupleType).components }
                    ConcreteConstraint(components.toFoxTupleType())
                } else EmptySpaceConstraint
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(tupleConcatSpace(spaces))
            }
        }
        
        is FoxStructGetFieldTypeByNameType -> compileProjection<FoxStructType>(type.type) {
            if (type.name !in it.fields) EmptySpaceConstraint
            else ConcreteConstraint(it.getFieldTypeByName(type.name))
        }
        is FoxStructGetFieldTypeByIndexType -> compileProjection<FoxStructType>(type.type) {
            if (type.index !in 0..<it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getFieldTypeByIndex(type.index).value)
        }
        is FoxStructGetFieldTypeByIndexBackType -> compileProjection<FoxStructType>(type.type) {
            if (type.index !in 0..<it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getFieldTypeByIndexBack(type.index).value)
        }
        is FoxStructGetFirstFieldsType -> compileProjection<FoxStructType>(type.type) {
            ConcreteConstraint(it.getFirstFields(min(type.count, it.arity)))
        }
        is FoxStructGetFirstFieldsExactType -> compileProjection<FoxStructType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getFirstFields(type.count))
        }
        is FoxStructGetLastFieldsType -> compileProjection<FoxStructType>(type.type) {
            ConcreteConstraint(it.getLastFields(min(type.count, it.arity)))
        }
        is FoxStructGetLastFieldsExactType -> compileProjection<FoxStructType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getLastFields(type.count))
        }
        is FoxStructDropFirstFieldsType -> compileProjection<FoxStructType>(type.type) {
            ConcreteConstraint(it.dropFirstFields(min(type.count, it.arity)))
        }
        is FoxStructDropFirstFieldsExactType -> compileProjection<FoxStructType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.dropFirstFields(type.count))
        }
        is FoxStructDropLastFieldsType -> compileProjection<FoxStructType>(type.type) {
            ConcreteConstraint(it.dropLastFields(min(type.count, it.arity)))
        }
        is FoxStructDropLastFieldsExactType -> compileProjection<FoxStructType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.dropLastFields(type.count))
        }
        is FoxStructSelectFieldsType -> compileProjection<FoxStructType>(type.type) {
            ConcreteConstraint(it.selectFields(type.names))
        }
        is FoxStructSelectFieldsExactType -> compileProjection<FoxStructType>(type.type) {
            if (!type.names.all(it.fields::contains)) EmptySpaceConstraint
            else ConcreteConstraint(it.selectFields(type.names))
        }
        is FoxStructDropFieldsType -> compileProjection<FoxStructType>(type.type) {
            ConcreteConstraint(it.dropFields(type.names))
        }
        is FoxStructDropFieldsExactType -> compileProjection<FoxStructType>(type.type) {
            if (!type.names.all(it.fields::contains)) EmptySpaceConstraint
            else ConcreteConstraint(it.dropFields(type.names))
        }
        is FoxStructExtractFieldTypesType -> compileProjection<FoxStructType>(type.type) {
            ConcreteConstraint(it.extractFieldTypes())
        }
        is FoxStructMergeStructsType -> {
            val results = type.types.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                val resultTypes = results.map { (it as ConcreteConstraint).type }
                if (resultTypes.all { it is FoxStructType }) {
                    val fields = resultTypes.flatMap { (it as FoxStructType).fields.entries }.map { it.key to it.value }
                    ConcreteConstraint(fields.toFoxStructType())
                } else EmptySpaceConstraint
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(structConcatSpace(spaces))
            }
        }
        
        is FoxObjectGetMemberTypeType -> compileProjection<FoxObjectType>(type.type) {
            if (type.name !in it.members) EmptySpaceConstraint
            else ConcreteConstraint(it.getMemberType(type.name))
        }
        is FoxObjectSelectMembersType -> compileProjection<FoxObjectType>(type.type) {
            ConcreteConstraint(it.selectMembers(type.names))
        }
        is FoxObjectSelectMembersExactType -> compileProjection<FoxObjectType>(type.type) {
            if (!type.names.all(it.members::contains)) EmptySpaceConstraint
            else ConcreteConstraint(it.selectMembers(type.names))
        }
        is FoxObjectDropMembersType -> compileProjection<FoxObjectType>(type.type) {
            ConcreteConstraint(it.dropMembers(type.names))
        }
        is FoxObjectDropMembersExactType -> compileProjection<FoxObjectType>(type.type) {
            if (!type.names.all(it.members::contains)) EmptySpaceConstraint
            else ConcreteConstraint(it.dropMembers(type.names))
        }
        is FoxObjectMergeObjectsType -> {
            val results = type.types.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                val resultTypes = results.map { (it as ConcreteConstraint).type }
                if (resultTypes.all { it is FoxObjectType }) {
                    val members = resultTypes.flatMap { (it as FoxObjectType).members.entries }.map { it.key to it.value }
                    ConcreteConstraint(members.toFoxObjectType())
                } else EmptySpaceConstraint
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(objectMergeSpace(spaces))
            }
        }
        
        is FoxEnumGetEntryTypeType -> compileProjection<FoxEnumType>(type.type) {
            if (type.name !in it.entries) EmptySpaceConstraint
            else ConcreteConstraint(it.getEntryType(type.name))
        }
        is FoxEnumSelectEntriesType -> compileProjection<FoxEnumType>(type.type) {
            ConcreteConstraint(it.selectEntries(type.names))
        }
        is FoxEnumSelectEntriesExactType -> compileProjection<FoxEnumType>(type.type) {
            if (!type.names.all(it.entries::contains)) EmptySpaceConstraint
            else ConcreteConstraint(it.selectEntries(type.names))
        }
        is FoxEnumDropEntriesType -> compileProjection<FoxEnumType>(type.type) {
            ConcreteConstraint(it.dropEntries(type.names))
        }
        is FoxEnumDropEntriesExactType -> compileProjection<FoxEnumType>(type.type) {
            if (!type.names.all(it.entries::contains)) EmptySpaceConstraint
            else ConcreteConstraint(it.dropEntries(type.names))
        }
        is FoxEnumMergeEnumsType -> {
            val results = type.types.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                val resultTypes = results.map { (it as ConcreteConstraint).type }
                if (resultTypes.all { it is FoxEnumType }) {
                    val entries = resultTypes.flatMap { (it as FoxEnumType).entries.entries }.map { it.key to it.value }
                    ConcreteConstraint(entries.toFoxEnumType())
                } else EmptySpaceConstraint
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(enumMergeSpace(spaces))
            }
        }
        
        is FoxArrayGetElementTypeType -> compileProjection<FoxArrayType>(type.type) {
            ConcreteConstraint(it.element)
        }
        is FoxRefGetReferentTypeType -> compileProjection<FoxRefType>(type.type) {
            ConcreteConstraint(it.referent)
        }
        
        is FoxMethodGetThisTypeType -> compileProjection<FoxMethodType>(type.type) {
            ConcreteConstraint(it.`this`)
        }
        is FoxMethodGetParameterStructType -> compileProjection<FoxMethodType>(type.type) {
            ConcreteConstraint(FoxStructType(it.parameters))
        }
        is FoxMethodGetReturnTypeType -> compileProjection<FoxMethodType>(type.type) {
            ConcreteConstraint(it.`return`)
        }
        is FoxMethodOfType -> {
            val thisResult = compile(type.`this`)
            val parametersResult = compile(type.parameters)
            val returnResult = compile(type.`return`)
            if (
                thisResult is ConcreteConstraint &&
                parametersResult is ConcreteConstraint &&
                returnResult is ConcreteConstraint
            ) {
                val parameters = parametersResult.type
                if (parameters is FoxStructType) ConcreteConstraint(
                    FoxMethodType(
                        thisResult.type,
                        parameters.fields,
                        returnResult.type,
                    ),
                ) else EmptySpaceConstraint
            } else {
                SpaceConstraint(
                    methodPatternSpace(
                        thisResult.toSpace(),
                        parametersResult.toSpace(),
                        returnResult.toSpace(),
                    ),
                )
            }
        }
    }
    
    private inline fun <reified T : FoxType> compileProjection(
        base: FoxType,
        factory: (T) -> ConstraintCompileResult,
    ): ConstraintCompileResult {
        val result = compile(base)
        check(result is ConcreteConstraint)
        val resultType = result.type
        return if (resultType is T) factory(resultType) else EmptySpaceConstraint
    }
    
    private fun ConstraintCompileResult.toSpace(): TypeSpace = when (this) {
        is ConcreteConstraint -> singleTypeSpace(type)
        is SpaceConstraint -> space
    }
}
