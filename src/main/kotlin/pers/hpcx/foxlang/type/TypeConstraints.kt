package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.utils.mapValues
import pers.hpcx.foxlang.utils.toOrderedMap
import kotlin.math.min

typealias TypeAssignments = Map<String, SurfaceType>
typealias TypeConstraints = Map<String, SurfaceType>

fun TypeAssignments.satisfies(constraints: TypeConstraints): Boolean {
    val compiled = ConstraintCompileContext(this).compile(constraints)
    return entries.all { (name, type) -> type.satisfies(compiled.getValue(name)) }
}

private fun SurfaceType.satisfies(constraint: ConstraintCompileResult) = when (constraint) {
    is ConcreteConstraint -> this == constraint.type
    is SpaceConstraint -> this in constraint.space
}

private sealed interface ConstraintCompileResult
private data class ConcreteConstraint(val type: SurfaceType) : ConstraintCompileResult
private data class SpaceConstraint(val space: TypeSpace) : ConstraintCompileResult

private val EmptySpaceConstraint = SpaceConstraint(emptyTypeSpace())

private class ConstraintCompileContext(
    val assignments: TypeAssignments,
) {
    
    fun compile(constraints: TypeConstraints) = constraints.mapValues { compile(it.value) }
    
    private fun compile(type: SurfaceType): ConstraintCompileResult = when (type) {
        is SurfacePrimitiveType -> ConcreteConstraint(type)
        is SurfaceBuiltInType -> compileBuiltInType(type)
        is SurfaceWildcardType -> compileWildcardType(type)
        is SurfaceTransformType -> compileTransformType(type)
        is SurfaceUnresolvedType -> ConcreteConstraint(assignments.getValue(type.name)).also { check(type.parameters == null) }
        is SurfacePlaceholderType -> error("unreachable")
    }
    
    private fun compileBuiltInType(type: SurfaceBuiltInType): ConstraintCompileResult = when (type) {
        is SurfaceTupleType -> {
            val results = type.components.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                ConcreteConstraint(results.map { (it as ConcreteConstraint).type }.toFoxTupleType())
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(tuplePatternSpace(spaces))
            }
        }
        is SurfaceStructType -> {
            val results = type.fields.map { it.key to compile(it.value) }
            if (results.all { it.second is ConcreteConstraint }) {
                ConcreteConstraint(results.map { it.first to (it.second as ConcreteConstraint).type }.toFoxStructType())
            } else {
                val spaces = results.map { it.first to it.second.toSpace() }.toOrderedMap()
                SpaceConstraint(structFieldPatternSpace(spaces))
            }
        }
        is SurfaceObjectType -> {
            val results = type.members.map { it.key to compile(it.value) }
            if (results.all { it.second is ConcreteConstraint }) {
                ConcreteConstraint(results.map { it.first to (it.second as ConcreteConstraint).type }.toFoxObjectType())
            } else {
                val spaces = results.associate { it.first to it.second.toSpace() }
                SpaceConstraint(objectPatternSpace(spaces))
            }
        }
        is SurfaceEnumType -> {
            val results = type.entries.map { it.key to compile(it.value) }
            if (results.all { it.second is ConcreteConstraint }) {
                ConcreteConstraint(results.map { it.first to (it.second as ConcreteConstraint).type }.toFoxEnumType())
            } else {
                val spaces = results.associate { it.first to it.second.toSpace() }
                SpaceConstraint(enumPatternSpace(spaces))
            }
        }
        is SurfaceArrayType -> when (val result = compile(type.element)) {
            is ConcreteConstraint -> ConcreteConstraint(SurfaceArrayType(result.type))
            is SpaceConstraint -> SpaceConstraint(arrayPatternSpace(result.space))
        }
        is SurfaceRefType -> when (val result = compile(type.referent)) {
            is ConcreteConstraint -> ConcreteConstraint(SurfaceRefType(result.type))
            is SpaceConstraint -> SpaceConstraint(refPatternSpace(result.space))
        }
        is SurfaceMethodType -> {
            val thisResult = compile(type.`this`)
            val parameterResults = type.parameters.mapValues { compile(it.value) }
            val returnResult = compile(type.`return`)
            if (
                thisResult is ConcreteConstraint &&
                parameterResults.all { it.value is ConcreteConstraint } &&
                returnResult is ConcreteConstraint
            ) {
                ConcreteConstraint(
                    SurfaceMethodType(
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
    
    private fun compileWildcardType(type: SurfaceWildcardType): ConstraintCompileResult = when (type) {
        is SurfaceAnyType -> universalTypeSpace()
        is SurfaceAnyTupleType -> tupleSpace()
        is SurfaceAnyStructType -> structSpace()
        is SurfaceAnyObjectType -> objectSpace()
        is SurfaceAnyEnumType -> enumSpace()
        is SurfaceAnyOfType -> union(type.types.map { compile(it).toSpace() })
        is SurfaceAllOfType -> intersect(type.types.map { compile(it).toSpace() })
        is SurfaceNoneOfType -> union(type.types.map { compile(it).toSpace() }).complement()
        is SurfaceAnyTupleOfType -> tupleRepeatSpace(compile(type.component).toSpace())
        is SurfaceAnyStructOfType -> structPatternSpace(type.fields.map { compile(it).toSpace() })
    }.let { SpaceConstraint(it) }
    
    private fun compileTransformType(type: SurfaceTransformType): ConstraintCompileResult = when (type) {
        is SurfaceTupleGetComponentType -> compileProjection<SurfaceTupleType>(type.type) {
            if (type.index !in 0..<it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getComponent(type.index))
        }
        is SurfaceTupleGetComponentBackType -> compileProjection<SurfaceTupleType>(type.type) {
            if (type.index !in 0..<it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getComponentBack(type.index))
        }
        is SurfaceTupleGetFirstComponentsType -> compileProjection<SurfaceTupleType>(type.type) {
            ConcreteConstraint(it.getFirstComponents(min(type.count, it.arity)))
        }
        is SurfaceTupleGetFirstComponentsExactType -> compileProjection<SurfaceTupleType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getFirstComponents(type.count))
        }
        is SurfaceTupleGetLastComponentsType -> compileProjection<SurfaceTupleType>(type.type) {
            ConcreteConstraint(it.getLastComponents(min(type.count, it.arity)))
        }
        is SurfaceTupleGetLastComponentsExactType -> compileProjection<SurfaceTupleType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getLastComponents(type.count))
        }
        is SurfaceTupleDropFirstComponentsType -> compileProjection<SurfaceTupleType>(type.type) {
            ConcreteConstraint(it.dropFirstComponents(min(type.count, it.arity)))
        }
        is SurfaceTupleDropFirstComponentsExactType -> compileProjection<SurfaceTupleType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.dropFirstComponents(type.count))
        }
        is SurfaceTupleDropLastComponentsType -> compileProjection<SurfaceTupleType>(type.type) {
            ConcreteConstraint(it.dropLastComponents(min(type.count, it.arity)))
        }
        is SurfaceTupleDropLastComponentsExactType -> compileProjection<SurfaceTupleType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.dropLastComponents(type.count))
        }
        is SurfaceTupleMergeTuplesType -> {
            val results = type.types.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                val resultTypes = results.map { (it as ConcreteConstraint).type }
                if (resultTypes.all { it is SurfaceTupleType }) {
                    val components = resultTypes.flatMap { (it as SurfaceTupleType).components }
                    ConcreteConstraint(components.toFoxTupleType())
                } else EmptySpaceConstraint
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(tupleConcatSpace(spaces))
            }
        }
        
        is SurfaceStructGetFieldTypeByNameType -> compileProjection<SurfaceStructType>(type.type) {
            if (type.name !in it.fields) EmptySpaceConstraint
            else ConcreteConstraint(it.getFieldTypeByName(type.name))
        }
        is SurfaceStructGetFieldTypeByIndexType -> compileProjection<SurfaceStructType>(type.type) {
            if (type.index !in 0..<it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getFieldTypeByIndex(type.index).value)
        }
        is SurfaceStructGetFieldTypeByIndexBackType -> compileProjection<SurfaceStructType>(type.type) {
            if (type.index !in 0..<it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getFieldTypeByIndexBack(type.index).value)
        }
        is SurfaceStructGetFirstFieldsType -> compileProjection<SurfaceStructType>(type.type) {
            ConcreteConstraint(it.getFirstFields(min(type.count, it.arity)))
        }
        is SurfaceStructGetFirstFieldsExactType -> compileProjection<SurfaceStructType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getFirstFields(type.count))
        }
        is SurfaceStructGetLastFieldsType -> compileProjection<SurfaceStructType>(type.type) {
            ConcreteConstraint(it.getLastFields(min(type.count, it.arity)))
        }
        is SurfaceStructGetLastFieldsExactType -> compileProjection<SurfaceStructType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.getLastFields(type.count))
        }
        is SurfaceStructDropFirstFieldsType -> compileProjection<SurfaceStructType>(type.type) {
            ConcreteConstraint(it.dropFirstFields(min(type.count, it.arity)))
        }
        is SurfaceStructDropFirstFieldsExactType -> compileProjection<SurfaceStructType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.dropFirstFields(type.count))
        }
        is SurfaceStructDropLastFieldsType -> compileProjection<SurfaceStructType>(type.type) {
            ConcreteConstraint(it.dropLastFields(min(type.count, it.arity)))
        }
        is SurfaceStructDropLastFieldsExactType -> compileProjection<SurfaceStructType>(type.type) {
            if (type.count !in 0..it.arity) EmptySpaceConstraint
            else ConcreteConstraint(it.dropLastFields(type.count))
        }
        is SurfaceStructSelectFieldsType -> compileProjection<SurfaceStructType>(type.type) {
            ConcreteConstraint(it.selectFields(type.names))
        }
        is SurfaceStructSelectFieldsExactType -> compileProjection<SurfaceStructType>(type.type) {
            if (!type.names.all(it.fields::contains)) EmptySpaceConstraint
            else ConcreteConstraint(it.selectFields(type.names))
        }
        is SurfaceStructDropFieldsType -> compileProjection<SurfaceStructType>(type.type) {
            ConcreteConstraint(it.dropFields(type.names))
        }
        is SurfaceStructDropFieldsExactType -> compileProjection<SurfaceStructType>(type.type) {
            if (!type.names.all(it.fields::contains)) EmptySpaceConstraint
            else ConcreteConstraint(it.dropFields(type.names))
        }
        is SurfaceStructExtractFieldTypesType -> compileProjection<SurfaceStructType>(type.type) {
            ConcreteConstraint(it.extractFieldTypes())
        }
        is SurfaceStructMergeStructsType -> {
            val results = type.types.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                val resultTypes = results.map { (it as ConcreteConstraint).type }
                if (resultTypes.all { it is SurfaceStructType }) {
                    val fields = resultTypes.flatMap { (it as SurfaceStructType).fields.entries }.map { it.key to it.value }
                    ConcreteConstraint(fields.toFoxStructType())
                } else EmptySpaceConstraint
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(structConcatSpace(spaces))
            }
        }
        
        is SurfaceObjectGetMemberTypeType -> compileProjection<SurfaceObjectType>(type.type) {
            if (type.name !in it.members) EmptySpaceConstraint
            else ConcreteConstraint(it.getMemberType(type.name))
        }
        is SurfaceObjectSelectMembersType -> compileProjection<SurfaceObjectType>(type.type) {
            ConcreteConstraint(it.selectMembers(type.names))
        }
        is SurfaceObjectSelectMembersExactType -> compileProjection<SurfaceObjectType>(type.type) {
            if (!type.names.all(it.members::contains)) EmptySpaceConstraint
            else ConcreteConstraint(it.selectMembers(type.names))
        }
        is SurfaceObjectDropMembersType -> compileProjection<SurfaceObjectType>(type.type) {
            ConcreteConstraint(it.dropMembers(type.names))
        }
        is SurfaceObjectDropMembersExactType -> compileProjection<SurfaceObjectType>(type.type) {
            if (!type.names.all(it.members::contains)) EmptySpaceConstraint
            else ConcreteConstraint(it.dropMembers(type.names))
        }
        is SurfaceObjectMergeObjectsType -> {
            val results = type.types.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                val resultTypes = results.map { (it as ConcreteConstraint).type }
                if (resultTypes.all { it is SurfaceObjectType }) {
                    val members = resultTypes.flatMap { (it as SurfaceObjectType).members.entries }.map { it.key to it.value }
                    ConcreteConstraint(members.toFoxObjectType())
                } else EmptySpaceConstraint
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(objectMergeSpace(spaces))
            }
        }
        
        is SurfaceEnumGetEntryTypeType -> compileProjection<SurfaceEnumType>(type.type) {
            if (type.name !in it.entries) EmptySpaceConstraint
            else ConcreteConstraint(it.getEntryType(type.name))
        }
        is SurfaceEnumSelectEntriesType -> compileProjection<SurfaceEnumType>(type.type) {
            ConcreteConstraint(it.selectEntries(type.names))
        }
        is SurfaceEnumSelectEntriesExactType -> compileProjection<SurfaceEnumType>(type.type) {
            if (!type.names.all(it.entries::contains)) EmptySpaceConstraint
            else ConcreteConstraint(it.selectEntries(type.names))
        }
        is SurfaceEnumDropEntriesType -> compileProjection<SurfaceEnumType>(type.type) {
            ConcreteConstraint(it.dropEntries(type.names))
        }
        is SurfaceEnumDropEntriesExactType -> compileProjection<SurfaceEnumType>(type.type) {
            if (!type.names.all(it.entries::contains)) EmptySpaceConstraint
            else ConcreteConstraint(it.dropEntries(type.names))
        }
        is SurfaceEnumMergeEnumsType -> {
            val results = type.types.map { compile(it) }
            if (results.all { it is ConcreteConstraint }) {
                val resultTypes = results.map { (it as ConcreteConstraint).type }
                if (resultTypes.all { it is SurfaceEnumType }) {
                    val entries = resultTypes.flatMap { (it as SurfaceEnumType).entries.entries }.map { it.key to it.value }
                    ConcreteConstraint(entries.toFoxEnumType())
                } else EmptySpaceConstraint
            } else {
                val spaces = results.map { it.toSpace() }
                SpaceConstraint(enumMergeSpace(spaces))
            }
        }
        
        is SurfaceArrayGetElementTypeType -> compileProjection<SurfaceArrayType>(type.type) {
            ConcreteConstraint(it.element)
        }
        is SurfaceRefGetReferentTypeType -> compileProjection<SurfaceRefType>(type.type) {
            ConcreteConstraint(it.referent)
        }
        
        is SurfaceMethodGetThisTypeType -> compileProjection<SurfaceMethodType>(type.type) {
            ConcreteConstraint(it.`this`)
        }
        is SurfaceMethodGetParameterStructType -> compileProjection<SurfaceMethodType>(type.type) {
            ConcreteConstraint(SurfaceStructType(it.parameters))
        }
        is SurfaceMethodGetReturnTypeType -> compileProjection<SurfaceMethodType>(type.type) {
            ConcreteConstraint(it.`return`)
        }
        is SurfaceMethodOfType -> {
            val thisResult = compile(type.`this`)
            val parametersResult = compile(type.parameters)
            val returnResult = compile(type.`return`)
            if (
                thisResult is ConcreteConstraint &&
                parametersResult is ConcreteConstraint &&
                returnResult is ConcreteConstraint
            ) {
                val parameters = parametersResult.type
                if (parameters is SurfaceStructType) ConcreteConstraint(
                    SurfaceMethodType(
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
    
    private inline fun <reified T : SurfaceType> compileProjection(
        base: SurfaceType,
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
