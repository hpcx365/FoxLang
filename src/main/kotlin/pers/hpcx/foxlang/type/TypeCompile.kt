package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.space.*
import pers.hpcx.foxlang.type.space.NameDictionary.StructFieldNames

fun compileType(type: FoxType, context: TypeSpaceContext): TypeSpace = when (type) {
    is FoxPrimitiveType -> singleSpace(type)
    is FoxBuiltInType -> compileBuiltInType(type, context)
    is FoxWildcardType -> compileWildcardType(type, context)
    is FoxTransformType -> compileTransformType(type, context)
    is FoxUnresolvedType -> TODO()
    is FoxPlaceholderType -> error("Placeholder type cannot be compiled")
}

private fun compileBuiltInType(type: FoxBuiltInType, context: TypeSpaceContext): TraversableTypeSpace = when (type) {
    is FoxTupleType -> tupleProduct((0 until type.arity).map { compileType(type.componentAt(it), context) as TraversableTypeSpace })
    is FoxStructType -> structPattern(
        type.fields.keys.map { singleSpace<String, NameSpaceContextView>(it) }.zip(
            type.fields.values.map { compileType(it, context) as TraversableTypeSpace },
        ) { name, type -> structFieldSpace(name, type) },
    )
    is FoxObjectType -> TODO()
    is FoxEnumType -> TODO()
    is FoxArrayType -> arraySpace(compileType(type.element, context) as TraversableTypeSpace)
    is FoxRefType -> refSpace(compileType(type.referent, context) as TraversableTypeSpace)
    is FoxMethodType -> TODO()
}

private fun compileWildcardType(type: FoxWildcardType, context: TypeSpaceContext): TraversableTypeSpace = when (type) {
    FoxAnyType -> universalTypeSpace()
    is FoxAnyOfType -> union(type.types.map { compileType(it, context) as TraversableTypeSpace })
    is FoxAllOfType -> intersect(type.types.map { compileType(it, context) as TraversableTypeSpace })
    is FoxNoneOfType -> subtract(
        universalTypeSpace(),
        union(type.types.map { compileType(it, context) as TraversableTypeSpace }),
    )
    FoxAnyTupleType -> tupleRepeat(
        universalTypeSpace(),
        0, Int.MAX_VALUE,
    )
    is FoxAnyTupleOfType -> tupleRepeat(
        compileType(type.component, context) as TraversableTypeSpace,
        0, Int.MAX_VALUE,
    )
    FoxAnyStructType -> structRepeat(
        structFieldSpace(
            universalNameSpace(StructFieldNames),
            universalTypeSpace(),
        ),
        0, Int.MAX_VALUE,
    )
    is FoxAnyStructOfType -> structPattern(
        type.fields.map { type ->
            structFieldSpace(
                universalNameSpace(StructFieldNames),
                compileType(type, context) as TraversableTypeSpace,
            )
        },
    )
    FoxAnyObjectType -> TODO()
    FoxAnyEnumType -> TODO()
}

private fun compileTransformType(type: FoxTransformType, context: TypeSpaceContext): TypeSpace = when (type) {
    is FoxTupleComponentAtType -> tupleComponentAt(compileType(type.type, context) as TraversableTypeSpace, type.index)
    is FoxTupleLastComponentAtType -> tupleLastComponentAt(compileType(type.type, context) as TraversableTypeSpace, type.index)
    is FoxTupleFirstComponentsOfType -> tupleFirstOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = false)
    is FoxTupleExactFirstComponentsOfType -> tupleFirstOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = true)
    is FoxTupleLastComponentsOfType -> tupleLastOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = false)
    is FoxTupleExactLastComponentsOfType -> tupleLastOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = true)
    is FoxTupleDropFirstComponentsOfType -> tupleDropFirstOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = false)
    is FoxTupleExactDropFirstComponentsOfType -> tupleDropFirstOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = true)
    is FoxTupleDropLastComponentsOfType -> tupleDropLastOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = false)
    is FoxTupleExactDropLastComponentsOfType -> tupleDropLastOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = true)
    is FoxTupleMergeComponentsOfType -> tupleConcat(type.types.map { compileType(it, context) as TraversableTypeSpace })
    
    is FoxStructFieldOfType -> TODO()
    is FoxStructFieldAtType -> structFieldAt(compileType(type.type, context) as TraversableTypeSpace, type.index)
    is FoxStructLastFieldAtType -> structLastFieldAt(compileType(type.type, context) as TraversableTypeSpace, type.index)
    is FoxStructFirstFieldsOfType -> structFirstOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = false)
    is FoxStructExactFirstFieldsOfType -> structFirstOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = true)
    is FoxStructLastFieldsOfType -> structLastOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = false)
    is FoxStructExactLastFieldsOfType -> structLastOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = true)
    is FoxStructDropFirstFieldsOfType -> structDropFirstOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = false)
    is FoxStructExactDropFirstFieldsOfType -> structDropFirstOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = true)
    is FoxStructDropLastFieldsOfType -> structDropLastOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = false)
    is FoxStructExactDropLastFieldsOfType -> structDropLastOf(compileType(type.type, context) as TraversableTypeSpace, type.count, exact = true)
    is FoxStructFieldsOfType -> TODO()
    is FoxStructDropFieldsOfType -> TODO()
    is FoxStructMergeFieldsOfType -> TODO()
    
    is FoxObjectMemberOfType -> TODO()
    is FoxObjectMembersOfType -> TODO()
    is FoxObjectDropMembersOfType -> TODO()
    is FoxObjectMergeMembersOfType -> TODO()
    
    is FoxEnumItemOfType -> TODO()
    is FoxEnumItemsOfType -> TODO()
    is FoxEnumDropItemsOfType -> TODO()
    is FoxEnumMergeItemsOfType -> TODO()
    
    is FoxArrayElementOfType -> arrayElementOf(compileType(type.type, context) as TraversableTypeSpace)
    is FoxRefReferentOfType -> refReferentOf(compileType(type.type, context) as TraversableTypeSpace)
    
    is FoxMethodOfType -> TODO()
    is FoxMethodThisOfType -> TODO()
    is FoxMethodParametersOfType -> TODO()
    is FoxMethodReturnOfType -> TODO()
}
