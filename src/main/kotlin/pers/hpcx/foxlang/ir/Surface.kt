package pers.hpcx.foxlang.ir

import pers.hpcx.foxlang.runtime.FoxEntity
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet
import pers.hpcx.foxlang.utils.WithInstanceId

sealed class SurfaceNode : WithInstanceId()

data class SurfaceFile(val elements: List<SurfaceFileElement>)

sealed class SurfaceFileElement : SurfaceNode()

data class SurfaceTypeAlias(
    val name: String,
    val generics: OrderedSet<String>,
    val alias: SurfaceType,
) : SurfaceFileElement()

data class SurfaceMethodDefinition(
    val generics: OrderedMap<String, SurfaceType>,
    val thisType: SurfaceType,
    val name: String,
    val parameters: OrderedMap<String, SurfaceType>,
    val returnType: SurfaceType,
    val body: SurfaceStatement,
) : SurfaceFileElement()

sealed class SurfaceType : SurfaceNode()

data class SurfacePrimitiveType(val type: PrimitiveTypeEnum) : SurfaceType()

sealed class SurfaceWildcardType : SurfaceType()

data class SurfaceAnyType(val dummy: Unit = Unit) : SurfaceWildcardType()
data class SurfaceAnyTupleType(val dummy: Unit = Unit) : SurfaceWildcardType()
data class SurfaceAnyStructType(val dummy: Unit = Unit) : SurfaceWildcardType()
data class SurfaceAnyObjectType(val dummy: Unit = Unit) : SurfaceWildcardType()
data class SurfaceAnyEnumType(val dummy: Unit = Unit) : SurfaceWildcardType()
data class SurfaceAnyOfType(val types: List<SurfaceType>) : SurfaceWildcardType()
data class SurfaceAllOfType(val types: List<SurfaceType>) : SurfaceWildcardType()
data class SurfaceNoneOfType(val types: List<SurfaceType>) : SurfaceWildcardType()
data class SurfaceAnyTupleOfType(val component: SurfaceType) : SurfaceWildcardType()
data class SurfaceAnyStructOfType(val fields: List<SurfaceType>) : SurfaceWildcardType()

sealed class SurfaceBuiltInType : SurfaceType()

data class SurfaceTupleType(val components: List<SurfaceType>) : SurfaceBuiltInType()
data class SurfaceStructType(val fields: OrderedMap<String, SurfaceType>) : SurfaceBuiltInType()
data class SurfaceObjectType(val members: Map<String, SurfaceType>) : SurfaceBuiltInType()
data class SurfaceEnumType(val entries: Map<String, SurfaceType>) : SurfaceBuiltInType()
data class SurfaceArrayType(val element: SurfaceType) : SurfaceBuiltInType()
data class SurfaceRefType(val referent: SurfaceType) : SurfaceBuiltInType()
data class SurfaceMethodType(
    val `this`: SurfaceType,
    val parameters: OrderedMap<String, SurfaceType>,
    val `return`: SurfaceType,
) : SurfaceBuiltInType()

sealed class SurfaceTransformType : SurfaceType()

data class SurfaceTupleGetComponentType(val type: SurfaceType, val index: Int) : SurfaceTransformType()
data class SurfaceTupleGetComponentBackType(val type: SurfaceType, val index: Int) : SurfaceTransformType()
data class SurfaceTupleGetFirstComponentsType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceTupleGetFirstComponentsExactType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceTupleGetLastComponentsType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceTupleGetLastComponentsExactType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceTupleDropFirstComponentsType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceTupleDropFirstComponentsExactType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceTupleDropLastComponentsType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceTupleDropLastComponentsExactType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceTupleMergeTuplesType(val types: List<SurfaceType>) : SurfaceTransformType()
data class SurfaceStructGetFieldTypeByNameType(val type: SurfaceType, val name: String) : SurfaceTransformType()
data class SurfaceStructGetFieldTypeByIndexType(val type: SurfaceType, val index: Int) : SurfaceTransformType()
data class SurfaceStructGetFieldTypeByIndexBackType(val type: SurfaceType, val index: Int) : SurfaceTransformType()
data class SurfaceStructGetFirstFieldsType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceStructGetFirstFieldsExactType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceStructGetLastFieldsType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceStructGetLastFieldsExactType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceStructDropFirstFieldsType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceStructDropFirstFieldsExactType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceStructDropLastFieldsType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceStructDropLastFieldsExactType(val type: SurfaceType, val count: Int) : SurfaceTransformType()
data class SurfaceStructSelectFieldsType(val type: SurfaceType, val names: Set<String>) : SurfaceTransformType()
data class SurfaceStructSelectFieldsExactType(val type: SurfaceType, val names: Set<String>) : SurfaceTransformType()
data class SurfaceStructDropFieldsType(val type: SurfaceType, val names: Set<String>) : SurfaceTransformType()
data class SurfaceStructDropFieldsExactType(val type: SurfaceType, val names: Set<String>) : SurfaceTransformType()
data class SurfaceStructExtractFieldTypesType(val type: SurfaceType) : SurfaceTransformType()
data class SurfaceStructMergeStructsType(val types: List<SurfaceType>) : SurfaceTransformType()
data class SurfaceObjectGetMemberTypeType(val type: SurfaceType, val name: String) : SurfaceTransformType()
data class SurfaceObjectSelectMembersType(val type: SurfaceType, val names: Set<String>) : SurfaceTransformType()
data class SurfaceObjectSelectMembersExactType(val type: SurfaceType, val names: Set<String>) : SurfaceTransformType()
data class SurfaceObjectDropMembersType(val type: SurfaceType, val names: Set<String>) : SurfaceTransformType()
data class SurfaceObjectDropMembersExactType(val type: SurfaceType, val names: Set<String>) : SurfaceTransformType()
data class SurfaceObjectMergeObjectsType(val types: List<SurfaceType>) : SurfaceTransformType()
data class SurfaceEnumGetEntryTypeType(val type: SurfaceType, val name: String) : SurfaceTransformType()
data class SurfaceEnumSelectEntriesType(val type: SurfaceType, val names: Set<String>) : SurfaceTransformType()
data class SurfaceEnumSelectEntriesExactType(val type: SurfaceType, val names: Set<String>) : SurfaceTransformType()
data class SurfaceEnumDropEntriesType(val type: SurfaceType, val names: Set<String>) : SurfaceTransformType()
data class SurfaceEnumDropEntriesExactType(val type: SurfaceType, val names: Set<String>) : SurfaceTransformType()
data class SurfaceEnumMergeEnumsType(val types: List<SurfaceType>) : SurfaceTransformType()
data class SurfaceArrayGetElementTypeType(val type: SurfaceType) : SurfaceTransformType()
data class SurfaceRefGetReferentTypeType(val type: SurfaceType) : SurfaceTransformType()
data class SurfaceMethodGetThisTypeType(val type: SurfaceType) : SurfaceTransformType()
data class SurfaceMethodGetParameterStructType(val type: SurfaceType) : SurfaceTransformType()
data class SurfaceMethodGetReturnTypeType(val type: SurfaceType) : SurfaceTransformType()
data class SurfaceMethodOfType(val `this`: SurfaceType, val parameters: SurfaceType, val `return`: SurfaceType) : SurfaceTransformType()

data class SurfaceUnresolvedType(val name: String, val parameters: List<SurfaceType>?) : SurfaceType()

open class SurfacePlaceholderType : SurfaceType()

sealed class SurfaceStatement : SurfaceNode()

object SurfaceThis : SurfaceStatement()
data class SurfaceUnresolvedSymbol(val name: String) : SurfaceStatement()
data class SurfaceEntityStatement(val value: FoxEntity) : SurfaceStatement()
data class SurfaceBreak(val label: String?) : SurfaceStatement()
data class SurfaceContinue(val label: String?) : SurfaceStatement()
data class SurfaceYield(val label: String?, val value: SurfaceStatement) : SurfaceStatement()
data class SurfaceReturn(val value: SurfaceStatement?) : SurfaceStatement()
data class SurfaceTypeBinding(val name: String, val type: SurfaceType) : SurfaceStatement()
data class SurfaceFieldAccess(val target: SurfaceStatement, val name: String) : SurfaceStatement()
data class SurfaceIndexAccess(val target: SurfaceStatement, val indices: List<SurfaceStatement>) : SurfaceStatement()

data class FoxUnaryOperator(val operator: UnaryOperatorEnum) : SurfaceNode()
data class SurfaceUnary(val operator: FoxUnaryOperator, val right: SurfaceStatement) : SurfaceStatement()

data class FoxBinaryOperator(val operator: BinaryOperatorEnum) : SurfaceNode()
data class SurfaceBinary(val left: SurfaceStatement, val operator: FoxBinaryOperator, val right: SurfaceStatement) : SurfaceStatement()

data class FoxAssignOperator(val operator: AssignOperatorEnum) : SurfaceNode()
data class SurfaceAssign(
    val left: SurfaceStatement,
    val operator: FoxAssignOperator,
    val right: SurfaceStatement,
    val beforeEvaluation: Boolean,
) : SurfaceStatement()

sealed interface FoxFormattedStringPart
data class FoxFormattedText(val text: String) : FoxFormattedStringPart
data class FoxFormattedExpression(val expression: SurfaceStatement) : FoxFormattedStringPart
data class SurfaceFormattedString(val parts: List<FoxFormattedStringPart>) : SurfaceStatement()

data class SurfaceConstruct(
    val type: SurfaceType,
    val parameters: List<Pair<String?, SurfaceStatement>>,
) : SurfaceStatement()

data class SurfaceCall(
    val target: SurfaceStatement,
    val name: String,
    val generics: List<Pair<String?, SurfaceType>>?,
    val parameters: List<Pair<String?, SurfaceStatement>>,
) : SurfaceStatement()

data class SurfaceIndirectCall(
    val target: SurfaceStatement,
    val method: SurfaceStatement,
    val parameters: List<Pair<String?, SurfaceStatement>>,
) : SurfaceStatement()

data class SurfaceBlock(
    val label: String?,
    val statements: List<SurfaceStatement>,
) : SurfaceStatement()

data class SurfaceIf(
    val label: String?,
    val condition: SurfaceStatement,
    val thenBody: SurfaceStatement,
    val elseBody: SurfaceStatement?,
) : SurfaceStatement()

data class FoxCase(
    val conditions: List<SurfaceStatement>?,
    val body: SurfaceStatement,
)

data class SurfaceWhen(
    val label: String?,
    val value: SurfaceStatement?,
    val cases: List<FoxCase>,
) : SurfaceStatement()

data class SurfaceWhile(
    val label: String?,
    val condition: SurfaceStatement,
    val body: SurfaceStatement,
) : SurfaceStatement()

data class SurfaceDoWhile(
    val label: String?,
    val body: SurfaceStatement,
    val condition: SurfaceStatement,
) : SurfaceStatement()

data class SurfaceLambda(
    val parameters: List<Pair<String, SurfaceType?>>?,
    val body: SurfaceStatement,
) : SurfaceStatement()
