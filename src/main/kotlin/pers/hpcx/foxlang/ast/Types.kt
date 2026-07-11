package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.frontend.common.SourceSpan
import pers.hpcx.foxlang.type.toFoxEnumType
import pers.hpcx.foxlang.type.toFoxObjectType
import pers.hpcx.foxlang.type.toFoxStructType
import pers.hpcx.foxlang.type.toFoxTupleType
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.orEmpty
import pers.hpcx.foxlang.utils.toOrderedMap

sealed interface FoxType
sealed interface ParsedFoxType<T : FoxType> : Parsed<T>

sealed interface FoxPrimitiveType : FoxType
object FoxVoidType : FoxPrimitiveType
object FoxUnitType : FoxPrimitiveType
object FoxBoolType : FoxPrimitiveType
object FoxByteType : FoxPrimitiveType
object FoxShortType : FoxPrimitiveType
object FoxIntType : FoxPrimitiveType
object FoxLongType : FoxPrimitiveType
object FoxFloatType : FoxPrimitiveType
object FoxDoubleType : FoxPrimitiveType
object FoxCharType : FoxPrimitiveType
object FoxStringType : FoxPrimitiveType

data class ParsedFoxPrimitiveType(
    override val node: FoxPrimitiveType,
    override val span: SourceSpan,
) : ParsedFoxType<FoxPrimitiveType>

sealed interface FoxWildcardType : FoxType
sealed interface ParsedFoxWildcardType<T : FoxWildcardType> : ParsedFoxType<T>

object FoxAnyType : FoxWildcardType
data class ParsedFoxAnyType(override val span: SourceSpan) : ParsedFoxWildcardType<FoxAnyType> {
    override val node get() = FoxAnyType
}

data class FoxAnyOfType(val types: List<FoxType>) : FoxWildcardType
data class ParsedFoxAnyOfType(
    val types: List<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxWildcardType<FoxAnyOfType> {
    override val node get() = FoxAnyOfType(types.map { it.node })
}

data class FoxAllOfType(val types: List<FoxType>) : FoxWildcardType
data class ParsedFoxAllOfType(
    val types: List<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxWildcardType<FoxAllOfType> {
    override val node get() = FoxAllOfType(types.map { it.node })
}

data class FoxNoneOfType(val types: List<FoxType>) : FoxWildcardType
data class ParsedFoxNoneOfType(
    val types: List<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxWildcardType<FoxNoneOfType> {
    override val node get() = FoxNoneOfType(types.map { it.node })
}

object FoxAnyTupleType : FoxWildcardType
data class ParsedFoxAnyTupleType(override val span: SourceSpan) : ParsedFoxWildcardType<FoxAnyTupleType> {
    override val node get() = FoxAnyTupleType
}

data class FoxAnyTupleOfType(val component: FoxType) : FoxWildcardType
data class ParsedFoxAnyTupleOfType(
    val component: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxWildcardType<FoxAnyTupleOfType> {
    override val node get() = FoxAnyTupleOfType(component.node)
}

object FoxAnyStructType : FoxWildcardType
data class ParsedFoxAnyStructType(override val span: SourceSpan) : ParsedFoxWildcardType<FoxAnyStructType> {
    override val node get() = FoxAnyStructType
}

data class FoxAnyStructOfType(val fields: List<FoxType>) : FoxWildcardType
data class ParsedFoxAnyStructOfType(
    val fields: ParsedList<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxWildcardType<FoxAnyStructOfType> {
    override val node get() = FoxAnyStructOfType(fields.node.map { it.node })
}

object FoxAnyObjectType : FoxWildcardType
data class ParsedFoxAnyObjectType(override val span: SourceSpan) : ParsedFoxWildcardType<FoxAnyObjectType> {
    override val node get() = FoxAnyObjectType
}

object FoxAnyEnumType : FoxWildcardType
data class ParsedFoxAnyEnumType(override val span: SourceSpan) : ParsedFoxWildcardType<FoxAnyEnumType> {
    override val node get() = FoxAnyEnumType
}

sealed interface FoxBuiltInType : FoxType
sealed interface ParsedFoxBuiltInType<T : FoxBuiltInType> : ParsedFoxType<T>

data class FoxTupleType(val components: List<FoxType>) : FoxBuiltInType
data class ParsedFoxTupleType(
    val components: ParsedList<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxBuiltInType<FoxTupleType> {
    override val node get() = components.node.map { it.node }.toFoxTupleType()
}

data class FoxStructType(val fields: OrderedMap<String, FoxType>) : FoxBuiltInType
data class ParsedFoxStructType(
    val fields: ParsedList<ParsedPair<ParsedString, ParsedFoxType<*>>>,
    override val span: SourceSpan,
) : ParsedFoxBuiltInType<FoxStructType> {
    override val node get() = fields.node.map { it.node.first.node to it.node.second.node }.toFoxStructType()
}

data class FoxObjectType(val members: Map<String, FoxType>) : FoxBuiltInType
data class ParsedFoxObjectType(
    val members: ParsedList<ParsedPair<ParsedString, ParsedFoxType<*>>>,
    override val span: SourceSpan,
) : ParsedFoxBuiltInType<FoxObjectType> {
    override val node get() = members.node.map { it.node.first.node to it.node.second.node }.toFoxObjectType()
}

data class FoxEnumType(val entries: Map<String, FoxType>) : FoxBuiltInType
data class ParsedFoxEnumType(
    val entries: ParsedList<ParsedPair<ParsedString, ParsedFoxType<*>>>,
    override val span: SourceSpan,
) : ParsedFoxBuiltInType<FoxEnumType> {
    override val node get() = entries.node.map { it.node.first.node to it.node.second.node }.toFoxEnumType()
}

data class FoxArrayType(val element: FoxType) : FoxBuiltInType
data class ParsedFoxArrayType(
    val element: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxBuiltInType<FoxArrayType> {
    override val node get() = FoxArrayType(element.node)
}

data class FoxRefType(val referent: FoxType) : FoxBuiltInType
data class ParsedFoxRefType(
    val referent: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxBuiltInType<FoxRefType> {
    override val node get() = FoxRefType(referent.node)
}

data class FoxMethodType(val `this`: FoxType, val parameters: OrderedMap<String, FoxType>, val `return`: FoxType) : FoxBuiltInType

data class ParsedFoxMethodTypeHead(
    val `this`: ParsedPair<ParsedUnit, ParsedFoxType<*>>?,
    val parameters: ParsedList<ParsedPair<ParsedString, ParsedFoxType<*>>>,
    override val span: SourceSpan,
) : Parsed<Unit> {
    override val node get() = Unit
}

data class ParsedFoxMethodType(
    val `this`: ParsedPair<ParsedUnit, ParsedFoxType<*>>?,
    val parameters: ParsedList<ParsedPair<ParsedString, ParsedFoxType<*>>>?,
    val `return`: ParsedPair<ParsedUnit, ParsedFoxType<*>>?,
    override val span: SourceSpan,
) : ParsedFoxBuiltInType<FoxMethodType> {
    override val node
        get() = FoxMethodType(
            `this`?.node?.second?.node ?: FoxUnitType,
            parameters?.node?.map { it.node.first.node to it.node.second.node }?.toOrderedMap().orEmpty(),
            `return`?.node?.second?.node ?: FoxUnitType,
        )
}

sealed interface FoxTransformType : FoxType
sealed interface ParsedFoxTransformType<T : FoxTransformType> : ParsedFoxType<T>

data class FoxTupleGetComponentType(val type: FoxType, val index: Int) : FoxTransformType
data class ParsedFoxTupleGetComponentType(
    val type: ParsedFoxType<*>,
    val index: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleGetComponentType> {
    override val node get() = FoxTupleGetComponentType(type.node, index.node)
}

data class FoxTupleGetComponentBackType(val type: FoxType, val index: Int) : FoxTransformType
data class ParsedFoxTupleGetComponentBackType(
    val type: ParsedFoxType<*>,
    val index: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleGetComponentBackType> {
    override val node get() = FoxTupleGetComponentBackType(type.node, index.node)
}

data class FoxTupleGetFirstComponentsType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleGetFirstComponentsType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleGetFirstComponentsType> {
    override val node get() = FoxTupleGetFirstComponentsType(type.node, count.node)
}

data class FoxTupleGetFirstComponentsExactType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleGetFirstComponentsExactType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleGetFirstComponentsExactType> {
    override val node get() = FoxTupleGetFirstComponentsExactType(type.node, count.node)
}

data class FoxTupleGetLastComponentsType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleGetLastComponentsType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleGetLastComponentsType> {
    override val node get() = FoxTupleGetLastComponentsType(type.node, count.node)
}

data class FoxTupleGetLastComponentsExactType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleGetLastComponentsExactType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleGetLastComponentsExactType> {
    override val node get() = FoxTupleGetLastComponentsExactType(type.node, count.node)
}

data class FoxTupleDropFirstComponentsType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleDropFirstComponentsType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleDropFirstComponentsType> {
    override val node get() = FoxTupleDropFirstComponentsType(type.node, count.node)
}

data class FoxTupleDropFirstComponentsExactType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleDropFirstComponentsExactType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleDropFirstComponentsExactType> {
    override val node get() = FoxTupleDropFirstComponentsExactType(type.node, count.node)
}

data class FoxTupleDropLastComponentsType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleDropLastComponentsType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleDropLastComponentsType> {
    override val node get() = FoxTupleDropLastComponentsType(type.node, count.node)
}

data class FoxTupleDropLastComponentsExactType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleDropLastComponentsExactType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleDropLastComponentsExactType> {
    override val node get() = FoxTupleDropLastComponentsExactType(type.node, count.node)
}

data class FoxTupleMergeTuplesType(val types: List<FoxType>) : FoxTransformType
data class ParsedFoxTupleMergeTuplesType(
    val types: ParsedList<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleMergeTuplesType> {
    override val node get() = FoxTupleMergeTuplesType(types.node.map { it.node })
}

data class FoxStructGetFieldTypeByNameType(val type: FoxType, val name: String) : FoxTransformType
data class ParsedFoxStructGetFieldTypeByNameType(
    val type: ParsedFoxType<*>,
    val name: ParsedString,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructGetFieldTypeByNameType> {
    override val node get() = FoxStructGetFieldTypeByNameType(type.node, name.node)
}

data class FoxStructGetFieldTypeByIndexType(val type: FoxType, val index: Int) : FoxTransformType
data class ParsedFoxStructGetFieldTypeByIndexType(
    val type: ParsedFoxType<*>,
    val index: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructGetFieldTypeByIndexType> {
    override val node get() = FoxStructGetFieldTypeByIndexType(type.node, index.node)
}

data class FoxStructGetFieldTypeByIndexBackType(val type: FoxType, val index: Int) : FoxTransformType
data class ParsedFoxStructGetFieldTypeByIndexBackType(
    val type: ParsedFoxType<*>,
    val index: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructGetFieldTypeByIndexBackType> {
    override val node get() = FoxStructGetFieldTypeByIndexBackType(type.node, index.node)
}

data class FoxStructGetFirstFieldsType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructGetFirstFieldsType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructGetFirstFieldsType> {
    override val node get() = FoxStructGetFirstFieldsType(type.node, count.node)
}

data class FoxStructGetFirstFieldsExactType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructGetFirstFieldsExactType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructGetFirstFieldsExactType> {
    override val node get() = FoxStructGetFirstFieldsExactType(type.node, count.node)
}

data class FoxStructGetLastFieldsType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructGetLastFieldsType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructGetLastFieldsType> {
    override val node get() = FoxStructGetLastFieldsType(type.node, count.node)
}

data class FoxStructGetLastFieldsExactType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructGetLastFieldsExactType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructGetLastFieldsExactType> {
    override val node get() = FoxStructGetLastFieldsExactType(type.node, count.node)
}

data class FoxStructDropFirstFieldsType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructDropFirstFieldsType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructDropFirstFieldsType> {
    override val node get() = FoxStructDropFirstFieldsType(type.node, count.node)
}

data class FoxStructDropFirstFieldsExactType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructDropFirstFieldsExactType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructDropFirstFieldsExactType> {
    override val node get() = FoxStructDropFirstFieldsExactType(type.node, count.node)
}

data class FoxStructDropLastFieldsType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructDropLastFieldsType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructDropLastFieldsType> {
    override val node get() = FoxStructDropLastFieldsType(type.node, count.node)
}

data class FoxStructDropLastFieldsExactType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructDropLastFieldsExactType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructDropLastFieldsExactType> {
    override val node get() = FoxStructDropLastFieldsExactType(type.node, count.node)
}

data class FoxStructSelectFieldsType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxStructSelectFieldsType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructSelectFieldsType> {
    override val node get() = FoxStructSelectFieldsType(type.node, names.node.map { it.node }.toSet())
}

data class FoxStructSelectFieldsExactType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxStructSelectFieldsExactType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructSelectFieldsExactType> {
    override val node get() = FoxStructSelectFieldsExactType(type.node, names.node.map { it.node }.toSet())
}

data class FoxStructDropFieldsType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxStructDropFieldsType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructDropFieldsType> {
    override val node get() = FoxStructDropFieldsType(type.node, names.node.map { it.node }.toSet())
}

data class FoxStructDropFieldsExactType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxStructDropFieldsExactType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructDropFieldsExactType> {
    override val node get() = FoxStructDropFieldsExactType(type.node, names.node.map { it.node }.toSet())
}

data class FoxStructExtractFieldTypesType(val type: FoxType) : FoxTransformType
data class ParsedFoxStructExtractFieldTypesType(
    val type: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructExtractFieldTypesType> {
    override val node get() = FoxStructExtractFieldTypesType(type.node)
}

data class FoxStructMergeStructsType(val types: List<FoxType>) : FoxTransformType
data class ParsedFoxStructMergeStructsType(
    val types: ParsedList<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructMergeStructsType> {
    override val node get() = FoxStructMergeStructsType(types.node.map { it.node })
}

data class FoxObjectGetMemberTypeType(val type: FoxType, val name: String) : FoxTransformType
data class ParsedFoxObjectGetMemberTypeType(
    val type: ParsedFoxType<*>,
    val name: ParsedString,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxObjectGetMemberTypeType> {
    override val node get() = FoxObjectGetMemberTypeType(type.node, name.node)
}

data class FoxObjectSelectMembersType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxObjectSelectMembersType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxObjectSelectMembersType> {
    override val node get() = FoxObjectSelectMembersType(type.node, names.node.map { it.node }.toSet())
}

data class FoxObjectSelectMembersExactType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxObjectSelectMembersExactType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxObjectSelectMembersExactType> {
    override val node get() = FoxObjectSelectMembersExactType(type.node, names.node.map { it.node }.toSet())
}

data class FoxObjectDropMembersType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxObjectDropMembersType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxObjectDropMembersType> {
    override val node get() = FoxObjectDropMembersType(type.node, names.node.map { it.node }.toSet())
}

data class FoxObjectDropMembersExactType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxObjectDropMembersExactType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxObjectDropMembersExactType> {
    override val node get() = FoxObjectDropMembersExactType(type.node, names.node.map { it.node }.toSet())
}

data class FoxObjectMergeObjectsType(val types: List<FoxType>) : FoxTransformType
data class ParsedFoxObjectMergeObjectsType(
    val types: ParsedList<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxObjectMergeObjectsType> {
    override val node get() = FoxObjectMergeObjectsType(types.node.map { it.node })
}

data class FoxEnumGetEntryTypeType(val type: FoxType, val name: String) : FoxTransformType
data class ParsedFoxEnumGetEntryTypeType(
    val type: ParsedFoxType<*>,
    val name: ParsedString,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxEnumGetEntryTypeType> {
    override val node get() = FoxEnumGetEntryTypeType(type.node, name.node)
}

data class FoxEnumSelectEntriesType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxEnumSelectEntriesType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxEnumSelectEntriesType> {
    override val node get() = FoxEnumSelectEntriesType(type.node, names.node.map { it.node }.toSet())
}

data class FoxEnumSelectEntriesExactType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxEnumSelectEntriesExactType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxEnumSelectEntriesExactType> {
    override val node get() = FoxEnumSelectEntriesExactType(type.node, names.node.map { it.node }.toSet())
}

data class FoxEnumDropEntriesType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxEnumDropEntriesType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxEnumDropEntriesType> {
    override val node get() = FoxEnumDropEntriesType(type.node, names.node.map { it.node }.toSet())
}

data class FoxEnumDropEntriesExactType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxEnumDropEntriesExactType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxEnumDropEntriesExactType> {
    override val node get() = FoxEnumDropEntriesExactType(type.node, names.node.map { it.node }.toSet())
}

data class FoxEnumMergeEnumsType(val types: List<FoxType>) : FoxTransformType
data class ParsedFoxEnumMergeEnumsType(
    val types: ParsedList<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxEnumMergeEnumsType> {
    override val node get() = FoxEnumMergeEnumsType(types.node.map { it.node })
}

data class FoxArrayGetElementTypeType(val type: FoxType) : FoxTransformType
data class ParsedFoxArrayGetElementTypeType(
    val type: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxArrayGetElementTypeType> {
    override val node get() = FoxArrayGetElementTypeType(type.node)
}

data class FoxRefGetReferentTypeType(val type: FoxType) : FoxTransformType
data class ParsedFoxRefGetReferentTypeType(
    val type: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxRefGetReferentTypeType> {
    override val node get() = FoxRefGetReferentTypeType(type.node)
}

data class FoxMethodGetThisTypeType(val type: FoxType) : FoxTransformType
data class ParsedFoxMethodGetThisTypeType(
    val type: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxMethodGetThisTypeType> {
    override val node get() = FoxMethodGetThisTypeType(type.node)
}

data class FoxMethodGetParameterStructType(val type: FoxType) : FoxTransformType
data class ParsedFoxMethodGetParameterStructType(
    val type: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxMethodGetParameterStructType> {
    override val node get() = FoxMethodGetParameterStructType(type.node)
}

data class FoxMethodGetReturnTypeType(val type: FoxType) : FoxTransformType
data class ParsedFoxMethodGetReturnTypeType(
    val type: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxMethodGetReturnTypeType> {
    override val node get() = FoxMethodGetReturnTypeType(type.node)
}

data class FoxMethodOfType(val `this`: FoxType, val parameters: FoxType, val `return`: FoxType) : FoxTransformType
data class ParsedFoxMethodOfType(
    val `this`: ParsedFoxType<*>,
    val parameters: ParsedFoxType<*>,
    val `return`: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxMethodOfType> {
    override val node get() = FoxMethodOfType(`this`.node, parameters.node, `return`.node)
}

data class FoxUnresolvedType(val name: String, val parameters: List<FoxType>?) : FoxType
data class ParsedFoxUnresolvedType(
    val name: ParsedString,
    val parameters: ParsedList<ParsedFoxType<*>>?,
    override val span: SourceSpan,
) : ParsedFoxType<FoxUnresolvedType> {
    override val node get() = FoxUnresolvedType(name.node, parameters?.node?.map { it.node })
}

interface FoxPlaceholderType : FoxType
