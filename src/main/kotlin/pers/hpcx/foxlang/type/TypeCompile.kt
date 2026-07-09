package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.space.*

fun compileType(type: FoxType): Space<FoxType> = when (type) {
    is FoxPrimitiveType -> singleSpace(type)
    is FoxBuiltInType -> compileBuiltInType(type)
    is FoxWildcardType -> compileWildcardType(type)
    is FoxTransformType -> compileTransformType(type)
    is FoxUnresolvedType -> generic(compileGeneric(type))
    is FoxPlaceholderType -> error("Placeholder type cannot be compiled")
}

private fun compileBuiltInType(type: FoxBuiltInType): Space<FoxType> = when (type) {
    is FoxTupleType -> tuplePattern((0 until type.arity).map { compileType(type.componentAt(it)) })
    is FoxStructType -> structPattern(
        type.fields.keys.map { singleSpace(it) }.zip(
            type.fields.values.map { compileType(it) },
        ) { name, type -> structFieldSpace(name, type) },
    )
    is FoxObjectType -> objectPattern(type.members.mapValues { compileType(it.value) })
    is FoxEnumType -> enumPattern(type.entries.mapValues { compileType(it.value) })
    is FoxArrayType -> arraySpace(compileType(type.element))
    is FoxRefType -> refSpace(compileType(type.referent))
    is FoxMethodType -> methodSpace(
        compileType(type.`this`),
        structPattern(
            type.parameters.keys.map { singleSpace(it) }.zip(
                type.parameters.values.map { compileType(it) },
            ) { name, parameterType -> structFieldSpace(name, parameterType) },
        ),
        compileType(type.`return`),
    )
}

private fun compileWildcardType(type: FoxWildcardType): Space<FoxType> = when (type) {
    FoxAnyType -> universalSpace()
    is FoxAnyOfType -> union(type.types.map { compileType(it) })
    is FoxAllOfType -> intersect(type.types.map { compileType(it) })
    is FoxNoneOfType -> subtract(universalSpace(), union(type.types.map { compileType(it) }))
    FoxAnyTupleType -> tupleRepeat(universalSpace(), 0, Int.MAX_VALUE)
    is FoxAnyTupleOfType -> tupleRepeat(compileType(type.component), 0, Int.MAX_VALUE)
    FoxAnyStructType -> structRepeat(structFieldSpace(universalSpace(), universalSpace()), 0, Int.MAX_VALUE)
    is FoxAnyStructOfType -> structPattern(type.fields.map { type -> structFieldSpace(universalSpace(), compileType(type)) })
    FoxAnyObjectType -> objectRepeat(universalSpace(), 0, Int.MAX_VALUE)
    FoxAnyEnumType -> enumRepeat(universalSpace(), 0, Int.MAX_VALUE)
}

private fun compileGeneric(type: FoxType): String {
    check(type is FoxUnresolvedType)
    check(type.parameters == null)
    return type.name
}

private fun compileTransformType(type: FoxTransformType): Space<FoxType> = when (type) {
    is FoxTupleComponentAtType -> tupleComponentAt(compileGeneric(type.type), type.index)
    is FoxTupleLastComponentAtType -> tupleLastComponentAt(compileGeneric(type.type), type.index)
    is FoxTupleFirstComponentsOfType -> tupleFirstOf(compileGeneric(type.type), type.count, exact = false)
    is FoxTupleExactFirstComponentsOfType -> tupleFirstOf(compileGeneric(type.type), type.count, exact = true)
    is FoxTupleLastComponentsOfType -> tupleLastOf(compileGeneric(type.type), type.count, exact = false)
    is FoxTupleExactLastComponentsOfType -> tupleLastOf(compileGeneric(type.type), type.count, exact = true)
    is FoxTupleDropFirstComponentsOfType -> tupleDropFirstOf(compileGeneric(type.type), type.count, exact = false)
    is FoxTupleExactDropFirstComponentsOfType -> tupleDropFirstOf(compileGeneric(type.type), type.count, exact = true)
    is FoxTupleDropLastComponentsOfType -> tupleDropLastOf(compileGeneric(type.type), type.count, exact = false)
    is FoxTupleExactDropLastComponentsOfType -> tupleDropLastOf(compileGeneric(type.type), type.count, exact = true)
    is FoxTupleMergeComponentsOfType -> tupleConcat(type.types.map { compileType(it) })
    
    is FoxStructFieldOfType -> structField(compileGeneric(type.type), type.name)
    is FoxStructFieldAtType -> structFieldAt(compileGeneric(type.type), type.index)
    is FoxStructLastFieldAtType -> structLastFieldAt(compileGeneric(type.type), type.index)
    is FoxStructFirstFieldsOfType -> structFirstOf(compileGeneric(type.type), type.count, exact = false)
    is FoxStructExactFirstFieldsOfType -> structFirstOf(compileGeneric(type.type), type.count, exact = true)
    is FoxStructLastFieldsOfType -> structLastOf(compileGeneric(type.type), type.count, exact = false)
    is FoxStructExactLastFieldsOfType -> structLastOf(compileGeneric(type.type), type.count, exact = true)
    is FoxStructDropFirstFieldsOfType -> structDropFirstOf(compileGeneric(type.type), type.count, exact = false)
    is FoxStructExactDropFirstFieldsOfType -> structDropFirstOf(compileGeneric(type.type), type.count, exact = true)
    is FoxStructDropLastFieldsOfType -> structDropLastOf(compileGeneric(type.type), type.count, exact = false)
    is FoxStructExactDropLastFieldsOfType -> structDropLastOf(compileGeneric(type.type), type.count, exact = true)
    is FoxStructFieldsOfType -> structFieldsOf(compileGeneric(type.type), type.names.elements.toList())
    is FoxStructDropFieldsOfType -> structDropFieldsOf(compileGeneric(type.type), type.names)
    is FoxStructMergeFieldsOfType -> structConcat(type.types.map { compileType(it) })
    
    is FoxObjectMemberOfType -> objectMember(compileGeneric(type.type), type.name)
    is FoxObjectMembersOfType -> objectMembersOf(compileGeneric(type.type), type.names)
    is FoxObjectDropMembersOfType -> objectDropMembersOf(compileGeneric(type.type), type.names)
    is FoxObjectMergeMembersOfType -> objectMerge(type.types.map { compileType(it) })
    
    is FoxEnumEntryOfType -> enumEntry(compileGeneric(type.type), type.name)
    is FoxEnumEntriesOfType -> enumEntriesOf(compileGeneric(type.type), type.names)
    is FoxEnumDropEntriesOfType -> enumDropEntriesOf(compileGeneric(type.type), type.names)
    is FoxEnumMergeEntriesOfType -> enumMerge(type.types.map { compileType(it) })
    
    is FoxArrayElementOfType -> arrayElementOf(compileGeneric(type.type))
    is FoxRefReferentOfType -> refReferentOf(compileGeneric(type.type))
    
    is FoxMethodOfType -> methodSpace(compileType(type.`this`), compileType(type.parameters), compileType(type.`return`))
    is FoxMethodThisOfType -> methodThisOf(compileGeneric(type.type))
    is FoxMethodParametersOfType -> methodParametersOf(compileGeneric(type.type))
    is FoxMethodReturnOfType -> methodReturnOf(compileGeneric(type.type))
}
