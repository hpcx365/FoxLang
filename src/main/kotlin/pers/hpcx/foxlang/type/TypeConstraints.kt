package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.mapValues
import pers.hpcx.foxlang.utils.toOrderedMap
import kotlin.math.min

typealias TypeAssignments = Map<String, FoxType>
typealias TypeConstraints = Map<String, FoxType>

fun TypeAssignments.satisfies(constraint: TypeConstraints): Boolean {
    TODO()
}

private sealed interface ConstraintCompileResult
private class ConcreteConstraint(val type: FoxType) : ConstraintCompileResult
private class SpaceConstraint(val space: TypeSpace) : ConstraintCompileResult

private fun ConstraintCompileResult.toSpace(): TypeSpace = when (this) {
    is ConcreteConstraint -> singleTypeSpace(type)
    is SpaceConstraint -> space
}

private class ConstraintCompileContext(
    val assignments: TypeAssignments,
) {
    
    private fun compile(type: FoxType): ConstraintCompileResult = when (type) {
        is FoxPrimitiveType -> ConcreteConstraint(type)
        is FoxBuiltInType -> compileBuiltInType(type)
        is FoxWildcardType -> compileWildcardType(type)
        is FoxTransformType -> compileTransformType(type)
        is FoxUnresolvedType -> {
            check(type.parameters == null)
            ConcreteConstraint(assignments.getValue(type.name))
        }
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
        is FoxTupleGetComponentType -> compileAndCheckIs<FoxTupleType>(type.type).let {
            if (type.index !in 0..<it.arity) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.getComponent(type.index))
        }
        is FoxTupleGetComponentBackType -> compileAndCheckIs<FoxTupleType>(type.type).let {
            if (type.index !in 0..<it.arity) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.getComponentBack(type.index))
        }
        is FoxTupleGetFirstComponentsType -> compileAndCheckIs<FoxTupleType>(type.type).let {
            ConcreteConstraint(it.getFirstComponents(min(type.count, it.arity)))
        }
        is FoxTupleGetFirstComponentsExactType -> compileAndCheckIs<FoxTupleType>(type.type).let {
            if (type.count !in 0..it.arity) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.getFirstComponents(type.count))
        }
        is FoxTupleGetLastComponentsType -> compileAndCheckIs<FoxTupleType>(type.type).let {
            ConcreteConstraint(it.getLastComponents(min(type.count, it.arity)))
        }
        is FoxTupleGetLastComponentsExactType -> compileAndCheckIs<FoxTupleType>(type.type).let {
            if (type.count !in 0..it.arity) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.getLastComponents(type.count))
        }
        is FoxTupleDropFirstComponentsType -> compileAndCheckIs<FoxTupleType>(type.type).let {
            ConcreteConstraint(it.dropFirstComponents(min(type.count, it.arity)))
        }
        is FoxTupleDropFirstComponentsExactType -> compileAndCheckIs<FoxTupleType>(type.type).let {
            if (type.count !in 0..it.arity) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.dropFirstComponents(type.count))
        }
        is FoxTupleDropLastComponentsType -> compileAndCheckIs<FoxTupleType>(type.type).let {
            ConcreteConstraint(it.dropLastComponents(min(type.count, it.arity)))
        }
        is FoxTupleDropLastComponentsExactType -> compileAndCheckIs<FoxTupleType>(type.type).let {
            if (type.count !in 0..it.arity) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.dropLastComponents(type.count))
        }
        is FoxTupleMergeTuplesType -> {
            val results = type.types.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                val resultTypes = results.map { checkIs<FoxTupleType>((it as ConcreteConstraint).type) }
                ConcreteConstraint(resultTypes.flatMap { it.components }.toFoxTupleType())
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(tupleConcatSpace(spaces))
            }
        }
        
        is FoxStructGetFieldTypeByNameType -> compileAndCheckIs<FoxStructType>(type.type).let {
            if (type.name !in it.fields) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.getFieldTypeByName(type.name))
        }
        is FoxStructGetFieldTypeByIndexType -> compileAndCheckIs<FoxStructType>(type.type).let {
            if (type.index !in 0..<it.arity) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.getFieldTypeByIndex(type.index).value)
        }
        is FoxStructGetFieldTypeByIndexBackType -> compileAndCheckIs<FoxStructType>(type.type).let {
            if (type.index !in 0..<it.arity) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.getFieldTypeByIndexBack(type.index).value)
        }
        is FoxStructGetFirstFieldsType -> compileAndCheckIs<FoxStructType>(type.type).let {
            ConcreteConstraint(it.getFirstFields(min(type.count, it.arity)))
        }
        is FoxStructGetFirstFieldsExactType -> compileAndCheckIs<FoxStructType>(type.type).let {
            if (type.count !in 0..it.arity) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.getFirstFields(type.count))
        }
        is FoxStructGetLastFieldsType -> compileAndCheckIs<FoxStructType>(type.type).let {
            ConcreteConstraint(it.getLastFields(min(type.count, it.arity)))
        }
        is FoxStructGetLastFieldsExactType -> compileAndCheckIs<FoxStructType>(type.type).let {
            if (type.count !in 0..it.arity) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.getLastFields(type.count))
        }
        is FoxStructDropFirstFieldsType -> compileAndCheckIs<FoxStructType>(type.type).let {
            ConcreteConstraint(it.dropFirstFields(min(type.count, it.arity)))
        }
        is FoxStructDropFirstFieldsExactType -> compileAndCheckIs<FoxStructType>(type.type).let {
            if (type.count !in 0..it.arity) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.dropFirstFields(type.count))
        }
        is FoxStructDropLastFieldsType -> compileAndCheckIs<FoxStructType>(type.type).let {
            ConcreteConstraint(it.dropLastFields(min(type.count, it.arity)))
        }
        is FoxStructDropLastFieldsExactType -> compileAndCheckIs<FoxStructType>(type.type).let {
            if (type.count !in 0..it.arity) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.dropLastFields(type.count))
        }
        is FoxStructSelectFieldsType -> ConcreteConstraint(
            compileAndCheckIs<FoxStructType>(type.type).selectFields(type.names),
        )
        is FoxStructSelectFieldsExactType -> compileAndCheckIs<FoxStructType>(type.type).let {
            if (!type.names.all(it.fields::contains)) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.selectFields(type.names))
        }
        is FoxStructDropFieldsType -> ConcreteConstraint(
            compileAndCheckIs<FoxStructType>(type.type).dropFields(type.names),
        )
        is FoxStructDropFieldsExactType -> compileAndCheckIs<FoxStructType>(type.type).let {
            if (!type.names.all(it.fields::contains)) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.dropFields(type.names))
        }
        is FoxStructExtractFieldTypesType -> ConcreteConstraint(
            compileAndCheckIs<FoxStructType>(type.type).extractFieldTypes(),
        )
        is FoxStructMergeStructsType -> {
            val results = type.types.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                val resultTypes = results.map { checkIs<FoxStructType>((it as ConcreteConstraint).type) }
                ConcreteConstraint(resultTypes.flatMap { it.fields }.map { it.key to it.value }.toFoxStructType())
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(structConcatSpace(spaces))
            }
        }
        
        is FoxObjectGetMemberTypeType -> compileAndCheckIs<FoxObjectType>(type.type).let {
            if (type.name !in it.members) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.getMemberType(type.name))
        }
        is FoxObjectSelectMembersType -> ConcreteConstraint(
            compileAndCheckIs<FoxObjectType>(type.type).selectMembers(type.names),
        )
        is FoxObjectSelectMembersExactType -> compileAndCheckIs<FoxObjectType>(type.type).let {
            if (!type.names.all(it.members::contains)) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.selectMembers(type.names))
        }
        is FoxObjectDropMembersType -> ConcreteConstraint(
            compileAndCheckIs<FoxObjectType>(type.type).dropMembers(type.names),
        )
        is FoxObjectDropMembersExactType -> compileAndCheckIs<FoxObjectType>(type.type).let {
            if (!type.names.all(it.members::contains)) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.dropMembers(type.names))
        }
        is FoxObjectMergeObjectsType -> {
            val results = type.types.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                val resultTypes = results.map { checkIs<FoxObjectType>((it as ConcreteConstraint).type) }
                ConcreteConstraint(resultTypes.flatMap { it.members.entries }.map { it.key to it.value }.toFoxObjectType())
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(objectMergeSpace(spaces))
            }
        }
        
        is FoxEnumGetEntryTypeType -> compileAndCheckIs<FoxEnumType>(type.type).let {
            if (type.name !in it.entries) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.getEntryType(type.name))
        }
        is FoxEnumSelectEntriesType -> ConcreteConstraint(
            compileAndCheckIs<FoxEnumType>(type.type).selectEntries(type.names),
        )
        is FoxEnumSelectEntriesExactType -> compileAndCheckIs<FoxEnumType>(type.type).let {
            if (!type.names.all(it.entries::contains)) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.selectEntries(type.names))
        }
        is FoxEnumDropEntriesType -> ConcreteConstraint(
            compileAndCheckIs<FoxEnumType>(type.type).dropEntries(type.names),
        )
        is FoxEnumDropEntriesExactType -> compileAndCheckIs<FoxEnumType>(type.type).let {
            if (!type.names.all(it.entries::contains)) SpaceConstraint(emptyTypeSpace())
            else ConcreteConstraint(it.dropEntries(type.names))
        }
        is FoxEnumMergeEnumsType -> {
            val results = type.types.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                val resultTypes = results.map { checkIs<FoxEnumType>((it as ConcreteConstraint).type) }
                ConcreteConstraint(resultTypes.flatMap { it.entries.entries }.map { it.key to it.value }.toFoxEnumType())
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(enumMergeSpace(spaces))
            }
        }
        
        is FoxArrayGetElementTypeType -> ConcreteConstraint(
            compileAndCheckIs<FoxArrayType>(type.type).element,
        )
        is FoxRefGetReferentTypeType -> ConcreteConstraint(
            compileAndCheckIs<FoxRefType>(type.type).referent,
        )
        
        is FoxMethodGetThisTypeType -> ConcreteConstraint(
            compileAndCheckIs<FoxMethodType>(type.type).`this`,
        )
        is FoxMethodGetParameterStructType -> ConcreteConstraint(
            FoxStructType(
                compileAndCheckIs<FoxMethodType>(type.type).parameters,
            ),
        )
        is FoxMethodGetReturnTypeType -> ConcreteConstraint(
            compileAndCheckIs<FoxMethodType>(type.type).`return`,
        )
        is FoxMethodOfType -> {
            val thisResult = compile(type.`this`)
            val parametersResult = compile(type.parameters)
            val returnResult = compile(type.`return`)
            if (
                thisResult is ConcreteConstraint &&
                parametersResult is ConcreteConstraint &&
                returnResult is ConcreteConstraint
            ) {
                ConcreteConstraint(
                    FoxMethodType(
                        thisResult.type,
                        checkIs<FoxStructType>(parametersResult.type).fields,
                        returnResult.type,
                    ),
                )
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
    
    private inline fun <reified T : FoxType> compileAndCheckIs(value: FoxType): T =
        checkIs<T>(checkIs<ConcreteConstraint>(compile(value)).type)
    
    private inline fun <reified T> checkIs(value: Any?): T {
        check(value is T)
        return value
    }
}
