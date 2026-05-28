package pers.hpcx.foxlang

import java.util.*

data class NodeProgram(
    val usingAs: List<NodeUsingAs>,
    val methods: List<NodeMethod>,
)

data class NodeUsingAs(
    val name: String,
    val generics: SequencedSet<String>,
    val alias: NodeType,
)

data class NodeMethod(
    val signature: NodeMethodSignature,
    val body: NodeStatement?,
)

data class NodeMethodSignature(
    val generics: SequencedMap<String, FoxGenericConstraint>,
    val thisType: NodeType,
    val name: String,
    val parameters: SequencedMap<String, NodeType>,
    val returnType: NodeType,
)

sealed interface NodeStatement

sealed interface NodeType
sealed interface NodeLeftExpression
sealed interface NodeRightExpression

data class NodeEntity(
    val value: FoxEntity,
) : NodeRightExpression

data class NodeSymbol(
    val name: String,
) : NodeLeftExpression, NodeRightExpression

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

data class NodeMethodType(
    val thisType: NodeType,
    val parameters: SequencedMap<String, NodeType>,
    val returnType: NodeType,
) : NodeType

data class NodeBlock(
    val label: String?,
    val statements: List<NodeStatement>,
) : NodeRightExpression, NodeStatement

data class NodeUnary(
    val operator: NodeUnaryOperator,
    val right: NodeRightExpression,
) : NodeRightExpression

data class NodeBinary(
    val left: NodeRightExpression,
    val operator: NodeBinaryOperator,
    val right: NodeRightExpression,
) : NodeRightExpression

data class NodeAssign(
    val left: NodeLeftExpression,
    val operator: NodeAssignOperator,
    val right: NodeRightExpression,
    val beforeEvaluation: Boolean,
) : NodeRightExpression, NodeStatement

data class NodeFieldAccess(
    val target: NodeRightExpression,
    val name: String,
) : NodeLeftExpression, NodeRightExpression

data class NodeComponentAccess(
    val target: NodeRightExpression,
    val index: NodeRightExpression,
) : NodeLeftExpression, NodeRightExpression

data class NodeCall(
    val target: NodeRightExpression,
    val name: String,
    val generics: List<Pair<String?, NodeType>>,
    val parameters: List<Pair<String?, NodeRightExpression>>,
) : NodeRightExpression, NodeStatement

data class NodeIndirectCall(
    val target: NodeRightExpression,
    val method: NodeRightExpression,
    val parameters: List<Pair<String?, NodeRightExpression>>,
)

data class NodeIf(
    val label: String?,
    val condition: NodeRightExpression,
    val thenBody: NodeStatement,
    val elseBody: NodeStatement?,
) : NodeRightExpression, NodeStatement

data class NodeWhile(
    val label: String?,
    val condition: NodeRightExpression,
    val body: NodeStatement,
) : NodeStatement

data class NodeDoWhile(
    val label: String?,
    val body: NodeStatement,
    val condition: NodeRightExpression,
) : NodeStatement

data class NodeWhen(
    val label: String?,
    val value: NodeRightExpression,
    val cases: List<NodeCase>,
    val elseBody: NodeStatement?,
) : NodeStatement

data class NodeCase(
    val conditions: List<NodeRightExpression>,
    val body: NodeStatement,
)

data class NodeBreak(
    val label: String?,
) : NodeStatement

data class NodeContinue(
    val label: String?,
) : NodeStatement

data class NodeAlso(
    val body: NodeStatement,
    val also: NodeStatement,
) : NodeStatement

data class NodeYield(
    val label: String?,
    val value: NodeRightExpression,
) : NodeStatement

data class NodeReturn(
    val value: NodeRightExpression?,
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
object NodeShrUnsignedOperator : NodeBinaryOperator
object NodeEqualOperator : NodeBinaryOperator
object NodeNotEqualOperator : NodeBinaryOperator
object NodeLessOperator : NodeBinaryOperator
object NodeGreaterOperator : NodeBinaryOperator
object NodeLessOrEqualOperator : NodeBinaryOperator
object NodeGreaterOrEqualOperator : NodeBinaryOperator
object NodeShortCircuitAndOperator : NodeBinaryOperator
object NodeShortCircuitOrOperator : NodeBinaryOperator

object NodePlainAssignOperator : NodeAssignOperator
object NodeAddAssignOperator : NodeAssignOperator
object NodeSubAssignOperator : NodeAssignOperator
object NodeMulAssignOperator : NodeAssignOperator
object NodeDivAssignOperator : NodeAssignOperator
object NodeRemAssignOperator : NodeAssignOperator
object NodeAndAssignOperator : NodeAssignOperator
object NodeOrAssignOperator : NodeAssignOperator
object NodeXorAssignOperator : NodeAssignOperator
object NodeShortCircuitAndAssignOperator : NodeAssignOperator
object NodeShortCircuitOrAssignOperator : NodeAssignOperator
object NodeShlAssignOperator : NodeAssignOperator
object NodeShrAssignOperator : NodeAssignOperator
object NodeShrUnsignedAssignOperator : NodeAssignOperator

val NodeBinaryOperator.priority: Int
    get() = when (this) {
        NodeMulOperator -> 0
        NodeDivOperator -> 0
        NodeRemOperator -> 0
        NodeAddOperator -> 1
        NodeSubOperator -> 1
        NodeShlOperator -> 2
        NodeShrOperator -> 2
        NodeShrUnsignedOperator -> 2
        NodeGreaterOperator -> 3
        NodeGreaterOrEqualOperator -> 3
        NodeLessOperator -> 3
        NodeLessOrEqualOperator -> 3
        NodeEqualOperator -> 4
        NodeNotEqualOperator -> 4
        NodeAndOperator -> 5
        NodeXorOperator -> 6
        NodeOrOperator -> 7
        NodeShortCircuitAndOperator -> 8
        NodeShortCircuitOrOperator -> 9
    }
