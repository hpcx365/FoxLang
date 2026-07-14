package pers.hpcx.foxlang.ir

import pers.hpcx.foxlang.frontend.common.SourceSpan
import pers.hpcx.foxlang.runtime.*
import pers.hpcx.foxlang.type.toFoxEnumType
import pers.hpcx.foxlang.type.toFoxObjectType
import pers.hpcx.foxlang.type.toFoxStructType
import pers.hpcx.foxlang.type.toFoxTupleType
import pers.hpcx.foxlang.utils.*

sealed class SyntaxNode<N> : WithInstanceId() {
    abstract val node: N
    abstract val span: SourceSpan
}

data class SyntaxUnit(
    override val span: SourceSpan,
) : SyntaxNode<Unit>() {
    override val node get() = Unit
}

data class SyntaxBoolean(
    override val node: Boolean,
    override val span: SourceSpan,
) : SyntaxNode<Boolean>()

data class SyntaxInt(
    val radix: Int,
    val text: String,
    override val span: SourceSpan,
) : SyntaxNode<Int>() {
    override val node get() = text.toInt(radix)
}

data class SyntaxLong(
    val radix: Int,
    val text: String,
    override val span: SourceSpan,
) : SyntaxNode<Long>() {
    override val node get() = text.toLong(radix)
}

data class SyntaxFloat(
    val text: String,
    override val span: SourceSpan,
) : SyntaxNode<Float>() {
    override val node get() = text.toFloat()
}

data class SyntaxDouble(
    val text: String,
    override val span: SourceSpan,
) : SyntaxNode<Double>() {
    override val node get() = text.toDouble()
}

data class SyntaxChar(
    override val node: Char,
    override val span: SourceSpan,
) : SyntaxNode<Char>()

data class SyntaxString(
    override val node: String,
    override val span: SourceSpan,
) : SyntaxNode<String>()

data class SyntaxPair<T, U>(
    override val node: Pair<T, U>,
    override val span: SourceSpan,
) : SyntaxNode<Pair<T, U>>()

data class SyntaxList<T>(
    override val node: List<T>,
    override val span: SourceSpan,
) : SyntaxNode<List<T>>()

operator fun <T> SyntaxList<T>.plus(other: SyntaxNode<T>) = SyntaxList(node + other.node, span + other.span)
operator fun <T> SyntaxList<T>.plus(other: SyntaxList<T>) = SyntaxList(node + other.node, span + other.span)

fun mergeSpan(vararg spans: SyntaxNode<*>): SourceSpan {
    var result = spans[0].span
    (1..<spans.size).forEach { result += spans[it].span }
    return result
}

data class SyntaxFile(
    val elements: List<SyntaxFileElement<*>>,
    override val span: SourceSpan,
) : SyntaxNode<SurfaceFile>() {
    override val node get() = SurfaceFile(elements.map { it.node })
}

sealed class SyntaxFileElement<N : SurfaceFileElement> : SyntaxNode<N>()

data class SyntaxTypeAlias(
    val name: SyntaxString,
    val generics: SyntaxList<SyntaxString>?,
    val alias: SyntaxType<*>,
    override val span: SourceSpan,
) : SyntaxFileElement<SurfaceTypeAlias>() {
    override val node
        get() = SurfaceTypeAlias(
            name.node,
            generics?.node?.map { it.node }?.toOrderedSet() ?: emptyOrderedSet(),
            alias.node,
        )
}

data class SyntaxMethodDefinition(
    val generics: SyntaxList<SyntaxPair<SyntaxString, SyntaxType<*>?>>?,
    val thisType: SyntaxType<*>?,
    val name: SyntaxString,
    val parameters: SyntaxList<SyntaxPair<SyntaxString, SyntaxType<*>>>,
    val returnType: SyntaxType<*>?,
    val body: SyntaxStatement<*>,
    override val span: SourceSpan,
) : SyntaxFileElement<SurfaceMethodDefinition>() {
    override val node
        get() = SurfaceMethodDefinition(
            generics?.node?.map { it.node.first.node to (it.node.second?.node ?: SurfaceAnyType()) }?.toOrderedMap() ?: emptyOrderedMap(),
            thisType?.node ?: SurfacePrimitiveType(PrimitiveTypeEnum.Unit),
            name.node,
            parameters.node.map { it.node.first.node to it.node.second.node }.toOrderedMap(),
            returnType?.node ?: SurfacePrimitiveType(PrimitiveTypeEnum.Unit),
            body.node,
        )
}

sealed class SyntaxType<T : SurfaceType> : SyntaxNode<T>()

data class SyntaxPrimitiveType(
    override val node: SurfacePrimitiveType,
    override val span: SourceSpan,
) : SyntaxType<SurfacePrimitiveType>()

sealed class SyntaxWildcardType<T : SurfaceWildcardType> : SyntaxType<T>()

data class SyntaxAnyType(override val span: SourceSpan) : SyntaxWildcardType<SurfaceAnyType>() {
    override val node get() = SurfaceAnyType()
}

data class SyntaxAnyTupleType(override val span: SourceSpan) : SyntaxWildcardType<SurfaceAnyTupleType>() {
    override val node get() = SurfaceAnyTupleType()
}

data class SyntaxAnyStructType(override val span: SourceSpan) : SyntaxWildcardType<SurfaceAnyStructType>() {
    override val node get() = SurfaceAnyStructType()
}

data class SyntaxAnyObjectType(override val span: SourceSpan) : SyntaxWildcardType<SurfaceAnyObjectType>() {
    override val node get() = SurfaceAnyObjectType()
}

data class SyntaxAnyEnumType(override val span: SourceSpan) : SyntaxWildcardType<SurfaceAnyEnumType>() {
    override val node get() = SurfaceAnyEnumType()
}

data class SyntaxAnyOfType(
    val types: List<SyntaxType<*>>,
    override val span: SourceSpan,
) : SyntaxWildcardType<SurfaceAnyOfType>() {
    override val node get() = SurfaceAnyOfType(types.map { it.node })
}

data class SyntaxAllOfType(
    val types: List<SyntaxType<*>>,
    override val span: SourceSpan,
) : SyntaxWildcardType<SurfaceAllOfType>() {
    override val node get() = SurfaceAllOfType(types.map { it.node })
}

data class SyntaxNoneOfType(
    val types: List<SyntaxType<*>>,
    override val span: SourceSpan,
) : SyntaxWildcardType<SurfaceNoneOfType>() {
    override val node get() = SurfaceNoneOfType(types.map { it.node })
}

data class SyntaxAnyTupleOfType(
    val component: SyntaxType<*>,
    override val span: SourceSpan,
) : SyntaxWildcardType<SurfaceAnyTupleOfType>() {
    override val node get() = SurfaceAnyTupleOfType(component.node)
}

data class SyntaxAnyStructOfType(
    val fields: SyntaxList<SyntaxType<*>>,
    override val span: SourceSpan,
) : SyntaxWildcardType<SurfaceAnyStructOfType>() {
    override val node get() = SurfaceAnyStructOfType(fields.node.map { it.node })
}

sealed class SyntaxBuiltInType<T : SurfaceBuiltInType> : SyntaxType<T>()

data class SyntaxTupleType(
    val components: SyntaxList<SyntaxType<*>>,
    override val span: SourceSpan,
) : SyntaxBuiltInType<SurfaceTupleType>() {
    override val node get() = components.node.map { it.node }.toFoxTupleType()
}

data class SyntaxStructType(
    val fields: SyntaxList<SyntaxPair<SyntaxString, SyntaxType<*>>>,
    override val span: SourceSpan,
) : SyntaxBuiltInType<SurfaceStructType>() {
    override val node get() = fields.node.map { it.node.first.node to it.node.second.node }.toFoxStructType()
}

data class SyntaxObjectType(
    val members: SyntaxList<SyntaxPair<SyntaxString, SyntaxType<*>>>,
    override val span: SourceSpan,
) : SyntaxBuiltInType<SurfaceObjectType>() {
    override val node get() = members.node.map { it.node.first.node to it.node.second.node }.toFoxObjectType()
}

data class SyntaxEnumType(
    val entries: SyntaxList<SyntaxPair<SyntaxString, SyntaxType<*>>>,
    override val span: SourceSpan,
) : SyntaxBuiltInType<SurfaceEnumType>() {
    override val node get() = entries.node.map { it.node.first.node to it.node.second.node }.toFoxEnumType()
}

data class SyntaxArrayType(
    val element: SyntaxType<*>,
    override val span: SourceSpan,
) : SyntaxBuiltInType<SurfaceArrayType>() {
    override val node get() = SurfaceArrayType(element.node)
}

data class SyntaxRefType(
    val referent: SyntaxType<*>,
    override val span: SourceSpan,
) : SyntaxBuiltInType<SurfaceRefType>() {
    override val node get() = SurfaceRefType(referent.node)
}

data class SyntaxMethodTypeHead(
    val `this`: SyntaxPair<SyntaxUnit, SyntaxType<*>>?,
    val parameters: SyntaxList<SyntaxPair<SyntaxString, SyntaxType<*>>>,
    override val span: SourceSpan,
) : SyntaxNode<Unit>() {
    override val node get() = Unit
}

data class SyntaxMethodType(
    val `this`: SyntaxPair<SyntaxUnit, SyntaxType<*>>?,
    val parameters: SyntaxList<SyntaxPair<SyntaxString, SyntaxType<*>>>?,
    val `return`: SyntaxPair<SyntaxUnit, SyntaxType<*>>?,
    override val span: SourceSpan,
) : SyntaxBuiltInType<SurfaceMethodType>() {
    override val node
        get() = SurfaceMethodType(
            `this`?.node?.second?.node ?: SurfacePrimitiveType(PrimitiveTypeEnum.Unit),
            parameters?.node?.map { it.node.first.node to it.node.second.node }?.toOrderedMap().orEmpty(),
            `return`?.node?.second?.node ?: SurfacePrimitiveType(PrimitiveTypeEnum.Unit),
        )
}

sealed class SyntaxTransformType<T : SurfaceTransformType> : SyntaxType<T>()

data class SyntaxTupleGetComponentType(
    val type: SyntaxType<*>,
    val index: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceTupleGetComponentType>() {
    override val node get() = SurfaceTupleGetComponentType(type.node, index.node)
}

data class SyntaxTupleGetComponentBackType(
    val type: SyntaxType<*>,
    val index: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceTupleGetComponentBackType>() {
    override val node get() = SurfaceTupleGetComponentBackType(type.node, index.node)
}

data class SyntaxTupleGetFirstComponentsType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceTupleGetFirstComponentsType>() {
    override val node get() = SurfaceTupleGetFirstComponentsType(type.node, count.node)
}

data class SyntaxTupleGetFirstComponentsExactType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceTupleGetFirstComponentsExactType>() {
    override val node get() = SurfaceTupleGetFirstComponentsExactType(type.node, count.node)
}

data class SyntaxTupleGetLastComponentsType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceTupleGetLastComponentsType>() {
    override val node get() = SurfaceTupleGetLastComponentsType(type.node, count.node)
}

data class SyntaxTupleGetLastComponentsExactType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceTupleGetLastComponentsExactType>() {
    override val node get() = SurfaceTupleGetLastComponentsExactType(type.node, count.node)
}

data class SyntaxTupleDropFirstComponentsType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceTupleDropFirstComponentsType>() {
    override val node get() = SurfaceTupleDropFirstComponentsType(type.node, count.node)
}

data class SyntaxTupleDropFirstComponentsExactType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceTupleDropFirstComponentsExactType>() {
    override val node get() = SurfaceTupleDropFirstComponentsExactType(type.node, count.node)
}

data class SyntaxTupleDropLastComponentsType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceTupleDropLastComponentsType>() {
    override val node get() = SurfaceTupleDropLastComponentsType(type.node, count.node)
}

data class SyntaxTupleDropLastComponentsExactType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceTupleDropLastComponentsExactType>() {
    override val node get() = SurfaceTupleDropLastComponentsExactType(type.node, count.node)
}

data class SyntaxTupleMergeTuplesType(
    val types: SyntaxList<SyntaxType<*>>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceTupleMergeTuplesType>() {
    override val node get() = SurfaceTupleMergeTuplesType(types.node.map { it.node })
}

data class SyntaxStructGetFieldTypeByNameType(
    val type: SyntaxType<*>,
    val name: SyntaxString,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructGetFieldTypeByNameType>() {
    override val node get() = SurfaceStructGetFieldTypeByNameType(type.node, name.node)
}

data class SyntaxStructGetFieldTypeByIndexType(
    val type: SyntaxType<*>,
    val index: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructGetFieldTypeByIndexType>() {
    override val node get() = SurfaceStructGetFieldTypeByIndexType(type.node, index.node)
}

data class SyntaxStructGetFieldTypeByIndexBackType(
    val type: SyntaxType<*>,
    val index: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructGetFieldTypeByIndexBackType>() {
    override val node get() = SurfaceStructGetFieldTypeByIndexBackType(type.node, index.node)
}

data class SyntaxStructGetFirstFieldsType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructGetFirstFieldsType>() {
    override val node get() = SurfaceStructGetFirstFieldsType(type.node, count.node)
}

data class SyntaxStructGetFirstFieldsExactType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructGetFirstFieldsExactType>() {
    override val node get() = SurfaceStructGetFirstFieldsExactType(type.node, count.node)
}

data class SyntaxStructGetLastFieldsType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructGetLastFieldsType>() {
    override val node get() = SurfaceStructGetLastFieldsType(type.node, count.node)
}

data class SyntaxStructGetLastFieldsExactType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructGetLastFieldsExactType>() {
    override val node get() = SurfaceStructGetLastFieldsExactType(type.node, count.node)
}

data class SyntaxStructDropFirstFieldsType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructDropFirstFieldsType>() {
    override val node get() = SurfaceStructDropFirstFieldsType(type.node, count.node)
}

data class SyntaxStructDropFirstFieldsExactType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructDropFirstFieldsExactType>() {
    override val node get() = SurfaceStructDropFirstFieldsExactType(type.node, count.node)
}

data class SyntaxStructDropLastFieldsType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructDropLastFieldsType>() {
    override val node get() = SurfaceStructDropLastFieldsType(type.node, count.node)
}

data class SyntaxStructDropLastFieldsExactType(
    val type: SyntaxType<*>,
    val count: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructDropLastFieldsExactType>() {
    override val node get() = SurfaceStructDropLastFieldsExactType(type.node, count.node)
}

data class SyntaxStructSelectFieldsType(
    val type: SyntaxType<*>,
    val names: SyntaxList<SyntaxString>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructSelectFieldsType>() {
    override val node get() = SurfaceStructSelectFieldsType(type.node, names.node.map { it.node }.toSet())
}

data class SyntaxStructSelectFieldsExactType(
    val type: SyntaxType<*>,
    val names: SyntaxList<SyntaxString>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructSelectFieldsExactType>() {
    override val node get() = SurfaceStructSelectFieldsExactType(type.node, names.node.map { it.node }.toSet())
}

data class SyntaxStructDropFieldsType(
    val type: SyntaxType<*>,
    val names: SyntaxList<SyntaxString>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructDropFieldsType>() {
    override val node get() = SurfaceStructDropFieldsType(type.node, names.node.map { it.node }.toSet())
}

data class SyntaxStructDropFieldsExactType(
    val type: SyntaxType<*>,
    val names: SyntaxList<SyntaxString>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructDropFieldsExactType>() {
    override val node get() = SurfaceStructDropFieldsExactType(type.node, names.node.map { it.node }.toSet())
}

data class SyntaxStructExtractFieldTypesType(
    val type: SyntaxType<*>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructExtractFieldTypesType>() {
    override val node get() = SurfaceStructExtractFieldTypesType(type.node)
}

data class SyntaxStructMergeStructsType(
    val types: SyntaxList<SyntaxType<*>>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceStructMergeStructsType>() {
    override val node get() = SurfaceStructMergeStructsType(types.node.map { it.node })
}

data class SyntaxObjectGetMemberTypeType(
    val type: SyntaxType<*>,
    val name: SyntaxString,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceObjectGetMemberTypeType>() {
    override val node get() = SurfaceObjectGetMemberTypeType(type.node, name.node)
}

data class SyntaxObjectSelectMembersType(
    val type: SyntaxType<*>,
    val names: SyntaxList<SyntaxString>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceObjectSelectMembersType>() {
    override val node get() = SurfaceObjectSelectMembersType(type.node, names.node.map { it.node }.toSet())
}

data class SyntaxObjectSelectMembersExactType(
    val type: SyntaxType<*>,
    val names: SyntaxList<SyntaxString>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceObjectSelectMembersExactType>() {
    override val node get() = SurfaceObjectSelectMembersExactType(type.node, names.node.map { it.node }.toSet())
}

data class SyntaxObjectDropMembersType(
    val type: SyntaxType<*>,
    val names: SyntaxList<SyntaxString>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceObjectDropMembersType>() {
    override val node get() = SurfaceObjectDropMembersType(type.node, names.node.map { it.node }.toSet())
}

data class SyntaxObjectDropMembersExactType(
    val type: SyntaxType<*>,
    val names: SyntaxList<SyntaxString>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceObjectDropMembersExactType>() {
    override val node get() = SurfaceObjectDropMembersExactType(type.node, names.node.map { it.node }.toSet())
}

data class SyntaxObjectMergeObjectsType(
    val types: SyntaxList<SyntaxType<*>>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceObjectMergeObjectsType>() {
    override val node get() = SurfaceObjectMergeObjectsType(types.node.map { it.node })
}

data class SyntaxEnumGetEntryTypeType(
    val type: SyntaxType<*>,
    val name: SyntaxString,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceEnumGetEntryTypeType>() {
    override val node get() = SurfaceEnumGetEntryTypeType(type.node, name.node)
}

data class SyntaxEnumSelectEntriesType(
    val type: SyntaxType<*>,
    val names: SyntaxList<SyntaxString>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceEnumSelectEntriesType>() {
    override val node get() = SurfaceEnumSelectEntriesType(type.node, names.node.map { it.node }.toSet())
}

data class SyntaxEnumSelectEntriesExactType(
    val type: SyntaxType<*>,
    val names: SyntaxList<SyntaxString>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceEnumSelectEntriesExactType>() {
    override val node get() = SurfaceEnumSelectEntriesExactType(type.node, names.node.map { it.node }.toSet())
}

data class SyntaxEnumDropEntriesType(
    val type: SyntaxType<*>,
    val names: SyntaxList<SyntaxString>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceEnumDropEntriesType>() {
    override val node get() = SurfaceEnumDropEntriesType(type.node, names.node.map { it.node }.toSet())
}

data class SyntaxEnumDropEntriesExactType(
    val type: SyntaxType<*>,
    val names: SyntaxList<SyntaxString>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceEnumDropEntriesExactType>() {
    override val node get() = SurfaceEnumDropEntriesExactType(type.node, names.node.map { it.node }.toSet())
}

data class SyntaxEnumMergeEnumsType(
    val types: SyntaxList<SyntaxType<*>>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceEnumMergeEnumsType>() {
    override val node get() = SurfaceEnumMergeEnumsType(types.node.map { it.node })
}

data class SyntaxArrayGetElementTypeType(
    val type: SyntaxType<*>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceArrayGetElementTypeType>() {
    override val node get() = SurfaceArrayGetElementTypeType(type.node)
}

data class SyntaxRefGetReferentTypeType(
    val type: SyntaxType<*>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceRefGetReferentTypeType>() {
    override val node get() = SurfaceRefGetReferentTypeType(type.node)
}

data class SyntaxMethodGetThisTypeType(
    val type: SyntaxType<*>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceMethodGetThisTypeType>() {
    override val node get() = SurfaceMethodGetThisTypeType(type.node)
}

data class SyntaxMethodGetParameterStructType(
    val type: SyntaxType<*>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceMethodGetParameterStructType>() {
    override val node get() = SurfaceMethodGetParameterStructType(type.node)
}

data class SyntaxMethodGetReturnTypeType(
    val type: SyntaxType<*>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceMethodGetReturnTypeType>() {
    override val node get() = SurfaceMethodGetReturnTypeType(type.node)
}

data class SyntaxMethodOfType(
    val `this`: SyntaxType<*>,
    val parameters: SyntaxType<*>,
    val `return`: SyntaxType<*>,
    override val span: SourceSpan,
) : SyntaxTransformType<SurfaceMethodOfType>() {
    override val node get() = SurfaceMethodOfType(`this`.node, parameters.node, `return`.node)
}

data class SyntaxUnresolvedType(
    val name: SyntaxString,
    val parameters: SyntaxList<SyntaxType<*>>?,
    override val span: SourceSpan,
) : SyntaxType<SurfaceUnresolvedType>() {
    override val node get() = SurfaceUnresolvedType(name.node, parameters?.node?.map { it.node })
}

sealed class SyntaxStatement<N : SurfaceStatement> : SyntaxNode<N>()

data class SyntaxThis(override val span: SourceSpan) : SyntaxStatement<SurfaceThis>() {
    override val node get() = SurfaceThis
}

data class SyntaxUnresolvedSymbol(
    val name: SyntaxString,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceUnresolvedSymbol>() {
    override val node get() = SurfaceUnresolvedSymbol(name.node)
}

data class SyntaxEntityStatement(
    val value: FoxEntity,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceEntityStatement>() {
    override val node get() = SurfaceEntityStatement(value)
}

data class SyntaxIntStatement(
    val value: SyntaxInt,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceEntityStatement>() {
    override val node get() = SurfaceEntityStatement(FoxInt(value.node))
}

data class SyntaxLongStatement(
    val value: SyntaxLong,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceEntityStatement>() {
    override val node get() = SurfaceEntityStatement(FoxLong(value.node))
}

data class SyntaxFloatStatement(
    val value: SyntaxFloat,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceEntityStatement>() {
    override val node get() = SurfaceEntityStatement(FoxFloat(value.node))
}

data class SyntaxDoubleStatement(
    val value: SyntaxDouble,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceEntityStatement>() {
    override val node get() = SurfaceEntityStatement(FoxDouble(value.node))
}

data class SyntaxBreak(
    val label: SyntaxString?,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceBreak>() {
    override val node get() = SurfaceBreak(label?.node)
}

data class SyntaxContinue(
    val label: SyntaxString?,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceContinue>() {
    override val node get() = SurfaceContinue(label?.node)
}

data class SyntaxYield(
    val label: SyntaxString?,
    val value: SyntaxStatement<*>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceYield>() {
    override val node get() = SurfaceYield(label?.node, value.node)
}

data class SyntaxReturn(
    val value: SyntaxStatement<*>?,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceReturn>() {
    override val node get() = SurfaceReturn(value?.node)
}

data class SyntaxTypeBinding(
    val name: SyntaxString,
    val type: SyntaxType<*>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceTypeBinding>() {
    override val node get() = SurfaceTypeBinding(name.node, type.node)
}

data class SyntaxUnaryOperator(
    override val node: FoxUnaryOperator,
    override val span: SourceSpan,
) : SyntaxNode<FoxUnaryOperator>()

data class SyntaxUnary(
    val operator: SyntaxUnaryOperator,
    val right: SyntaxStatement<*>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceUnary>() {
    override val node get() = SurfaceUnary(operator.node, right.node)
}

data class SyntaxBinaryOperator(
    override val node: FoxBinaryOperator,
    override val span: SourceSpan,
) : SyntaxNode<FoxBinaryOperator>()

data class SyntaxBinary(
    val left: SyntaxStatement<*>,
    val operator: SyntaxBinaryOperator,
    val right: SyntaxStatement<*>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceBinary>() {
    override val node get() = SurfaceBinary(left.node, operator.node, right.node)
}

data class SyntaxAssignOperator(
    override val node: FoxAssignOperator,
    override val span: SourceSpan,
) : SyntaxNode<FoxAssignOperator>()

data class SyntaxAssign(
    val left: SyntaxStatement<*>,
    val operator: SyntaxAssignOperator,
    val right: SyntaxStatement<*>,
    val beforeEvaluation: Boolean,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceAssign>() {
    override val node get() = SurfaceAssign(left.node, operator.node, right.node, beforeEvaluation)
}

data class SyntaxFieldAccess(
    val target: SyntaxStatement<*>,
    val name: SyntaxString,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceFieldAccess>() {
    override val node get() = SurfaceFieldAccess(target.node, name.node)
}

data class SyntaxIndexAccess(
    val target: SyntaxStatement<*>,
    val indices: SyntaxList<SyntaxStatement<*>>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceIndexAccess>() {
    override val node get() = SurfaceIndexAccess(target.node, indices.node.map { it.node })
}

sealed class SyntaxFormattedStringPart : SyntaxNode<FoxFormattedStringPart>()

data class SyntaxFormattedText(
    val text: String,
    override val span: SourceSpan,
) : SyntaxFormattedStringPart() {
    override val node get() = FoxFormattedText(text)
}

data class SyntaxFormattedExpression(
    val expression: SyntaxStatement<*>,
    override val span: SourceSpan,
) : SyntaxFormattedStringPart() {
    override val node get() = FoxFormattedExpression(expression.node)
}

data class SyntaxFormattedString(
    val parts: SyntaxList<SyntaxFormattedStringPart>?,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceFormattedString>() {
    override val node get() = SurfaceFormattedString(parts?.node?.map { it.node }.orEmpty())
}

data class SyntaxConstruct(
    val type: SyntaxType<*>,
    val parameters: SyntaxList<SyntaxPair<SyntaxString?, SyntaxStatement<*>>>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceConstruct>() {
    override val node
        get() = SurfaceConstruct(
            type.node,
            parameters.node.map { it.node.first?.node to it.node.second.node },
        )
}

data class SyntaxCall(
    val target: SyntaxStatement<*>?,
    val name: SyntaxString,
    val generics: SyntaxList<SyntaxPair<SyntaxString?, SyntaxType<*>>>?,
    val parameters: SyntaxList<SyntaxPair<SyntaxString?, SyntaxStatement<*>>>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceCall>() {
    override val node
        get() = SurfaceCall(
            target?.node ?: SurfaceEntityStatement(FoxUnit),
            name.node,
            generics?.node?.map { it.node.first?.node to it.node.second.node },
            parameters.node.map { it.node.first?.node to it.node.second.node },
        )
}

data class SyntaxIndirectCall(
    val target: SyntaxStatement<*>?,
    val method: SyntaxStatement<*>,
    val parameters: SyntaxList<SyntaxPair<SyntaxString?, SyntaxStatement<*>>>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceIndirectCall>() {
    override val node
        get() = SurfaceIndirectCall(
            target?.node ?: SurfaceEntityStatement(FoxUnit),
            method.node,
            parameters.node.map { it.node.first?.node to it.node.second.node },
        )
}

data class SyntaxBlock(
    val label: SyntaxString?,
    val statements: SyntaxList<SyntaxStatement<*>>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceBlock>() {
    override val node
        get() = SurfaceBlock(
            label?.node,
            statements.node.map { it.node },
        )
}

data class SyntaxIf(
    val label: SyntaxString?,
    val condition: SyntaxStatement<*>,
    val thenBody: SyntaxStatement<*>,
    val elseBody: SyntaxStatement<*>?,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceIf>() {
    override val node
        get() = SurfaceIf(
            label?.node,
            condition.node,
            thenBody.node,
            elseBody?.node,
        )
}

data class SyntaxCase(
    val conditions: SyntaxList<SyntaxStatement<*>>?,
    val body: SyntaxStatement<*>,
    override val span: SourceSpan,
) : SyntaxNode<FoxCase>() {
    override val node
        get() = FoxCase(
            conditions?.node?.map { it.node },
            body.node,
        )
}

data class SyntaxWhen(
    val label: SyntaxString?,
    val value: SyntaxStatement<*>?,
    val cases: SyntaxList<SyntaxCase>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceWhen>() {
    override val node
        get() = SurfaceWhen(
            label?.node,
            value?.node,
            cases.node.map { it.node },
        )
}

data class SyntaxWhile(
    val label: SyntaxString?,
    val condition: SyntaxStatement<*>,
    val body: SyntaxStatement<*>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceWhile>() {
    override val node
        get() = SurfaceWhile(
            label?.node,
            condition.node,
            body.node,
        )
}

data class SyntaxDoWhile(
    val label: SyntaxString?,
    val body: SyntaxStatement<*>,
    val condition: SyntaxStatement<*>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceDoWhile>() {
    override val node
        get() = SurfaceDoWhile(
            label?.node,
            body.node,
            condition.node,
        )
}

data class SyntaxLambda(
    val parameters: SyntaxList<SyntaxPair<SyntaxString, SyntaxType<*>?>>?,
    val body: SyntaxStatement<*>,
    override val span: SourceSpan,
) : SyntaxStatement<SurfaceLambda>() {
    override val node
        get() = SurfaceLambda(
            parameters?.node?.map { it.node.first.node to it.node.second?.node },
            body.node,
        )
}
