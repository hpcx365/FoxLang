package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.frontend.common.SourceSpan
import pers.hpcx.foxlang.type.toFoxEnumType
import pers.hpcx.foxlang.type.toFoxObjectType
import pers.hpcx.foxlang.type.toFoxStructType
import pers.hpcx.foxlang.type.toFoxTupleType
import pers.hpcx.foxlang.utils.*

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

data class FoxTupleComponentAtType(val type: FoxType, val index: Int) : FoxTransformType
data class ParsedFoxTupleComponentAtType(
    val type: ParsedFoxType<*>,
    val index: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleComponentAtType> {
    override val node get() = FoxTupleComponentAtType(type.node, index.node)
}

data class FoxTupleLastComponentAtType(val type: FoxType, val index: Int) : FoxTransformType
data class ParsedFoxTupleLastComponentAtType(
    val type: ParsedFoxType<*>,
    val index: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleLastComponentAtType> {
    override val node get() = FoxTupleLastComponentAtType(type.node, index.node)
}

data class FoxTupleFirstComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleFirstComponentsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleFirstComponentsOfType> {
    override val node get() = FoxTupleFirstComponentsOfType(type.node, count.node)
}

data class FoxTupleExactFirstComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleExactFirstComponentsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleExactFirstComponentsOfType> {
    override val node get() = FoxTupleExactFirstComponentsOfType(type.node, count.node)
}

data class FoxTupleLastComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleLastComponentsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleLastComponentsOfType> {
    override val node get() = FoxTupleLastComponentsOfType(type.node, count.node)
}

data class FoxTupleExactLastComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleExactLastComponentsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleExactLastComponentsOfType> {
    override val node get() = FoxTupleExactLastComponentsOfType(type.node, count.node)
}

data class FoxTupleDropFirstComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleDropFirstComponentsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleDropFirstComponentsOfType> {
    override val node get() = FoxTupleDropFirstComponentsOfType(type.node, count.node)
}

data class FoxTupleExactDropFirstComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleExactDropFirstComponentsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleExactDropFirstComponentsOfType> {
    override val node get() = FoxTupleExactDropFirstComponentsOfType(type.node, count.node)
}

data class FoxTupleDropLastComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleDropLastComponentsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleDropLastComponentsOfType> {
    override val node get() = FoxTupleDropLastComponentsOfType(type.node, count.node)
}

data class FoxTupleExactDropLastComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxTupleExactDropLastComponentsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleExactDropLastComponentsOfType> {
    override val node get() = FoxTupleExactDropLastComponentsOfType(type.node, count.node)
}

data class FoxTupleMergeComponentsOfType(val types: List<FoxType>) : FoxTransformType
data class ParsedFoxTupleMergeComponentsOfType(
    val types: ParsedList<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxTupleMergeComponentsOfType> {
    override val node get() = FoxTupleMergeComponentsOfType(types.node.map { it.node })
}

data class FoxStructFieldOfType(val type: FoxType, val name: String) : FoxTransformType
data class ParsedFoxStructFieldOfType(
    val type: ParsedFoxType<*>,
    val name: ParsedString,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructFieldOfType> {
    override val node get() = FoxStructFieldOfType(type.node, name.node)
}

data class FoxStructFieldAtType(val type: FoxType, val index: Int) : FoxTransformType
data class ParsedFoxStructFieldAtType(
    val type: ParsedFoxType<*>,
    val index: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructFieldAtType> {
    override val node get() = FoxStructFieldAtType(type.node, index.node)
}

data class FoxStructLastFieldAtType(val type: FoxType, val index: Int) : FoxTransformType
data class ParsedFoxStructLastFieldAtType(
    val type: ParsedFoxType<*>,
    val index: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructLastFieldAtType> {
    override val node get() = FoxStructLastFieldAtType(type.node, index.node)
}

data class FoxStructFirstFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructFirstFieldsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructFirstFieldsOfType> {
    override val node get() = FoxStructFirstFieldsOfType(type.node, count.node)
}

data class FoxStructExactFirstFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructExactFirstFieldsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructExactFirstFieldsOfType> {
    override val node get() = FoxStructExactFirstFieldsOfType(type.node, count.node)
}

data class FoxStructLastFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructLastFieldsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructLastFieldsOfType> {
    override val node get() = FoxStructLastFieldsOfType(type.node, count.node)
}

data class FoxStructExactLastFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructExactLastFieldsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructExactLastFieldsOfType> {
    override val node get() = FoxStructExactLastFieldsOfType(type.node, count.node)
}

data class FoxStructDropFirstFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructDropFirstFieldsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructDropFirstFieldsOfType> {
    override val node get() = FoxStructDropFirstFieldsOfType(type.node, count.node)
}

data class FoxStructExactDropFirstFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructExactDropFirstFieldsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructExactDropFirstFieldsOfType> {
    override val node get() = FoxStructExactDropFirstFieldsOfType(type.node, count.node)
}

data class FoxStructDropLastFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructDropLastFieldsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructDropLastFieldsOfType> {
    override val node get() = FoxStructDropLastFieldsOfType(type.node, count.node)
}

data class FoxStructExactDropLastFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class ParsedFoxStructExactDropLastFieldsOfType(
    val type: ParsedFoxType<*>,
    val count: ParsedInt,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructExactDropLastFieldsOfType> {
    override val node get() = FoxStructExactDropLastFieldsOfType(type.node, count.node)
}

data class FoxStructFieldsOfType(val type: FoxType, val names: OrderedSet<String>) : FoxTransformType
data class ParsedFoxStructFieldsOfType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructFieldsOfType> {
    override val node get() = FoxStructFieldsOfType(type.node, names.node.map { it.node }.toOrderedSet())
}

data class FoxStructDropFieldsOfType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxStructDropFieldsOfType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructDropFieldsOfType> {
    override val node get() = FoxStructDropFieldsOfType(type.node, names.node.map { it.node }.toSet())
}

data class FoxStructMergeFieldsOfType(val types: List<FoxType>) : FoxTransformType
data class ParsedFoxStructMergeFieldsOfType(
    val types: ParsedList<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxStructMergeFieldsOfType> {
    override val node get() = FoxStructMergeFieldsOfType(types.node.map { it.node })
}

data class FoxObjectMemberOfType(val type: FoxType, val name: String) : FoxTransformType
data class ParsedFoxObjectMemberOfType(
    val type: ParsedFoxType<*>,
    val name: ParsedString,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxObjectMemberOfType> {
    override val node get() = FoxObjectMemberOfType(type.node, name.node)
}

data class FoxObjectMembersOfType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxObjectMembersOfType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxObjectMembersOfType> {
    override val node get() = FoxObjectMembersOfType(type.node, names.node.map { it.node }.toSet())
}

data class FoxObjectDropMembersOfType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxObjectDropMembersOfType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxObjectDropMembersOfType> {
    override val node get() = FoxObjectDropMembersOfType(type.node, names.node.map { it.node }.toSet())
}

data class FoxObjectMergeMembersOfType(val types: List<FoxType>) : FoxTransformType
data class ParsedFoxObjectMergeMembersOfType(
    val types: ParsedList<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxObjectMergeMembersOfType> {
    override val node get() = FoxObjectMergeMembersOfType(types.node.map { it.node })
}

data class FoxEnumEntryOfType(val type: FoxType, val name: String) : FoxTransformType
data class ParsedFoxEnumEntryOfType(
    val type: ParsedFoxType<*>,
    val name: ParsedString,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxEnumEntryOfType> {
    override val node get() = FoxEnumEntryOfType(type.node, name.node)
}

data class FoxEnumEntriesOfType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxEnumEntriesOfType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxEnumEntriesOfType> {
    override val node get() = FoxEnumEntriesOfType(type.node, names.node.map { it.node }.toSet())
}

data class FoxEnumDropEntriesOfType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class ParsedFoxEnumDropEntriesOfType(
    val type: ParsedFoxType<*>,
    val names: ParsedList<ParsedString>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxEnumDropEntriesOfType> {
    override val node get() = FoxEnumDropEntriesOfType(type.node, names.node.map { it.node }.toSet())
}

data class FoxEnumMergeEntriesOfType(val types: List<FoxType>) : FoxTransformType
data class ParsedFoxEnumMergeEntriesOfType(
    val types: ParsedList<ParsedFoxType<*>>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxEnumMergeEntriesOfType> {
    override val node get() = FoxEnumMergeEntriesOfType(types.node.map { it.node })
}

data class FoxArrayElementOfType(val type: FoxType) : FoxTransformType
data class ParsedFoxArrayElementOfType(
    val type: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxArrayElementOfType> {
    override val node get() = FoxArrayElementOfType(type.node)
}

data class FoxRefReferentOfType(val type: FoxType) : FoxTransformType
data class ParsedFoxRefReferentOfType(
    val type: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxRefReferentOfType> {
    override val node get() = FoxRefReferentOfType(type.node)
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

data class FoxMethodThisOfType(val type: FoxType) : FoxTransformType
data class ParsedFoxMethodThisOfType(
    val type: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxMethodThisOfType> {
    override val node get() = FoxMethodThisOfType(type.node)
}

data class FoxMethodParametersOfType(val type: FoxType) : FoxTransformType
data class ParsedFoxMethodParametersOfType(
    val type: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxMethodParametersOfType> {
    override val node get() = FoxMethodParametersOfType(type.node)
}

data class FoxMethodReturnOfType(val type: FoxType) : FoxTransformType
data class ParsedFoxMethodReturnOfType(
    val type: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxTransformType<FoxMethodReturnOfType> {
    override val node get() = FoxMethodReturnOfType(type.node)
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
