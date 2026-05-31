package pers.hpcx.foxlang

import java.util.*

data class NodeFile(
    val elements: List<NodeFileElement>,
)

sealed interface NodeFileElement

data class NodeTypeAlias(
    val name: String,
    val generics: SequencedSet<String>?,
    val alias: NodeType,
) : NodeFileElement

data class NodeMethodDefinition(
    val generics: SequencedMap<String, NodeGenericConstraint>?,
    val thisType: NodeType?,
    val name: String,
    val parameters: SequencedMap<String, NodeType>,
    val returnType: NodeType?,
    val body: NodeStatement,
) : NodeFileElement

data class NodeGenericConstraint(
    val match: NodeType?,
)

sealed interface NodeType

data class NodePrimitiveType(
    val type: FoxPrimitiveType,
) : NodeType

data class NodeNamedType(
    val name: String,
    val generics: List<Pair<String?, NodeType>>,
) : NodeType

data class NodeArrayType(
    val elementType: NodeType,
) : NodeType

data class NodeTupleType(
    val componentTypes: List<NodeType>,
) : NodeType

data class NodeStructType(
    val fields: Map<String, NodeType>,
) : NodeType

data class NodeEnumType(
    val items: Map<String, NodeType>,
) : NodeType

data class NodeRefType(
    val referentType: NodeType,
) : NodeType

data class NodeLambdaType(
    val thisType: NodeType,
    val parameters: List<NodeType>,
    val returnType: NodeType,
) : NodeType

sealed interface NodeStatement

data class NodeEntity(
    val value: FoxEntity,
) : NodeStatement

data class NodeFormattedString(
    val parts: List<NodeFormattedStringPart>,
    val isRaw: Boolean = false,
) : NodeStatement

sealed interface NodeFormattedStringPart

data class NodeFormattedText(
    val text: String,
) : NodeFormattedStringPart

data class NodeFormattedExpression(
    val expression: NodeStatement,
) : NodeFormattedStringPart

data class NodeSymbol(
    val name: String,
) : NodeStatement

data class NodeBlock(
    val label: String?,
    val statements: List<NodeStatement>,
) : NodeStatement

data class NodeUnary(
    val operator: NodeUnaryOperator,
    val right: NodeStatement,
) : NodeStatement

data class NodeBinary(
    val left: NodeStatement,
    val operator: NodeBinaryOperator,
    val right: NodeStatement,
) : NodeStatement

data class NodeAssign(
    val left: NodeStatement,
    val operator: NodeAssignOperator,
    val right: NodeStatement,
    val beforeEvaluation: Boolean,
) : NodeStatement

data class NodeTypeBinding(
    val name: String,
    val type: NodeType,
) : NodeStatement

data class NodeFieldAccess(
    val target: NodeStatement,
    val name: String,
) : NodeStatement

data class NodeComponentAccess(
    val target: NodeStatement,
    val index: Int,
) : NodeStatement

data class NodeCall(
    val target: NodeStatement?,
    val name: String,
    val generics: List<Pair<String?, NodeType>>?,
    val parameters: List<Pair<String?, NodeStatement>>,
) : NodeStatement

data class NodeConstruct(
    val type: NodeType,
    val parameters: List<Pair<String?, NodeStatement>>,
) : NodeStatement

data class NodeLambda(
    val parameters: List<Pair<String, NodeType?>>,
    val body: NodeStatement,
) : NodeStatement

data class NodeLambdaCall(
    val target: NodeStatement?,
    val method: NodeStatement,
    val parameters: List<NodeStatement>,
) : NodeStatement

data class NodeIf(
    val label: String?,
    val condition: NodeStatement,
    val thenBody: NodeStatement,
    val elseBody: NodeStatement?,
) : NodeStatement

data class NodeWhen(
    val label: String?,
    val value: NodeStatement?,
    val cases: List<NodeCase>,
) : NodeStatement

data class NodeCase(
    val conditions: List<NodeStatement>,
    val body: NodeStatement,
)

data class NodeWhile(
    val label: String?,
    val condition: NodeStatement,
    val body: NodeStatement,
) : NodeStatement

data class NodeDoWhile(
    val label: String?,
    val body: NodeStatement,
    val condition: NodeStatement,
) : NodeStatement

data class NodeBreak(
    val label: String?,
) : NodeStatement

data class NodeContinue(
    val label: String?,
) : NodeStatement

data class NodeYield(
    val label: String?,
    val value: NodeStatement,
) : NodeStatement

data class NodeReturn(
    val value: NodeStatement?,
) : NodeStatement

sealed interface NodeUnaryOperator
sealed interface NodeBinaryOperator
sealed interface NodeAssignOperator

object NodeNotOperator : NodeUnaryOperator
object NodeNegOperator : NodeUnaryOperator

object NodeAddOperator : NodeBinaryOperator
object NodeSubOperator : NodeBinaryOperator
object NodeMulOperator : NodeBinaryOperator
object NodeDivOperator : NodeBinaryOperator
object NodeRemOperator : NodeBinaryOperator
object NodeAndOperator : NodeBinaryOperator
object NodeOrOperator : NodeBinaryOperator
object NodeXorOperator : NodeBinaryOperator
object NodeShlOperator : NodeBinaryOperator
object NodeShrOperator : NodeBinaryOperator
object NodeUshrOperator : NodeBinaryOperator
object NodeEqOperator : NodeBinaryOperator
object NodeNeqOperator : NodeBinaryOperator
object NodeLtOperator : NodeBinaryOperator
object NodeGtOperator : NodeBinaryOperator
object NodeLeOperator : NodeBinaryOperator
object NodeGeOperator : NodeBinaryOperator
object NodeAndAndOperator : NodeBinaryOperator
object NodeOrOrOperator : NodeBinaryOperator

object NodePlainAssignOperator : NodeAssignOperator
object NodeTypeBindingAssignOperator : NodeAssignOperator
object NodeAddAssignOperator : NodeAssignOperator
object NodeSubAssignOperator : NodeAssignOperator
object NodeMulAssignOperator : NodeAssignOperator
object NodeDivAssignOperator : NodeAssignOperator
object NodeRemAssignOperator : NodeAssignOperator
object NodeAndAssignOperator : NodeAssignOperator
object NodeOrAssignOperator : NodeAssignOperator
object NodeXorAssignOperator : NodeAssignOperator
object NodeAndAndAssignOperator : NodeAssignOperator
object NodeOrOrAssignOperator : NodeAssignOperator
object NodeShlAssignOperator : NodeAssignOperator
object NodeShrAssignOperator : NodeAssignOperator
object NodeUshrAssignOperator : NodeAssignOperator
