package pers.hpcx.foxlang.lowering

import pers.hpcx.foxlang.frontend.*
import pers.hpcx.foxlang.runtime.FoxMethodSignature
import pers.hpcx.foxlang.sema.collectSemanticPlaceholders
import pers.hpcx.foxlang.sema.toMethodSignature
import pers.hpcx.foxlang.types.*
import java.util.*

data class ExpandedMethodDefinition(
    val definition: NodeMethodDefinition,
)

data class InstantiatedMethodLowering(
    val definition: NodeMethodDefinition,
    val signature: FoxMethodSignature,
)

fun NodeFile.instantiateMethodDeclaration(
    methodName: String,
    genericReplacements: SequencedMap<String, FoxConcreteType>,
    isInline: Boolean = false,
): NormalizationResult<InstantiatedMethodLowering> {
    val methods = elements.filterIsInstance<NodeMethodDefinition>().filter { it.name == methodName }
    if (methods.isEmpty()) return NormalizationError("Method '$methodName' not found")
    if (methods.size > 1) return NormalizationError("Multiple method definitions named '$methodName' found")
    return methods.single().instantiateAndLower(genericReplacements, isInline)
}

fun NodeMethodDefinition.instantiateAndLower(
    genericReplacements: SequencedMap<String, FoxConcreteType>,
    isInline: Boolean = false,
): NormalizationResult<InstantiatedMethodLowering> {
    val expanded = when (val result = expandInstantiation(genericReplacements)) {
        is Normalized -> result.value.definition
        is NormalizationError -> return result
    }
    val placeholderReport = NodeFile(listOf(expanded)).collectSemanticPlaceholders()
    val placeholder = placeholderReport.methods.singleOrNull()
        ?: return NormalizationError("Expanded method '${expanded.name}' did not produce a placeholder method")
    return when (val lowered = placeholder.toMethodSignature(isInline)) {
        is Normalized -> Normalized(
            InstantiatedMethodLowering(
                definition = expanded,
                signature = lowered.value.signature,
            ),
        )
        is NormalizationError -> lowered
    }
}

fun NodeMethodDefinition.expandInstantiation(
    genericReplacements: SequencedMap<String, FoxConcreteType>,
): NormalizationResult<ExpandedMethodDefinition> {
    val genericRows = genericReplacements.mapValuesTo(LinkedHashMap()) { (_, value) ->
        when (val row = value.asParameterRow()) {
            is Normalized -> row.value
            is NormalizationError -> return row
        }
    }
    
    val expandedParameters = mutableListOf<NodeFormalParameter>()
    val parameterNames = LinkedHashSet<String>()
    for (parameter in parameters) {
        when (parameter) {
            is NodeNamedFormalParameter -> {
                if (!parameterNames.add(parameter.name)) {
                    return NormalizationError("Duplicate method parameter '${parameter.name}' after expansion")
                }
                expandedParameters += parameter
            }
            is NodeSplatFormalParameter -> {
                val target = parameter.type as? NodeNamedType
                    ?: return NormalizationError("Splat parameter '${parameter.type.toSource()}' must be a named generic type during instantiation")
                val row = genericRows[target.name]
                    ?: return NormalizationError("No concrete generic replacement provided for splat parameter '${target.name}'")
                row.forEach { item ->
                    if (!parameterNames.add(item.name)) {
                        return NormalizationError("Duplicate method parameter '${item.name}' after expansion")
                    }
                    expandedParameters += NodeNamedFormalParameter(item.name, foxTypeToNodeType(item.type))
                }
            }
        }
    }
    
    val expandedBody = expandStatement(body, genericRows)
    return when (expandedBody) {
        is Normalized -> Normalized(
            ExpandedMethodDefinition(
                NodeMethodDefinition(
                    generics = generics,
                    thisType = thisType,
                    name = name,
                    parameters = expandedParameters,
                    returnType = returnType,
                    body = expandedBody.value,
                ),
            ),
        )
        is NormalizationError -> expandedBody
    }
}

private fun expandStatement(
    statement: NodeStatement,
    genericRows: SequencedMap<String, List<NormalizedMethodParameter>>,
): NormalizationResult<NodeStatement> = when (statement) {
    is NodeBlock -> {
        val statements = mutableListOf<NodeStatement>()
        for (item in statement.statements) {
            when (val expanded = expandStatement(item, genericRows)) {
                is Normalized -> {
                    val value = expanded.value
                    if (value is NodeBlock && value.label == null) statements += value.statements
                    else statements += value
                }
                is NormalizationError -> return expanded
            }
        }
        Normalized(NodeBlock(statement.label, statements))
    }
    is NodeGenFor -> {
        val target = statement.targetType as? NodeNamedType
            ?: return NormalizationError("genfor target '${statement.targetType.toSource()}' must be a named generic type during instantiation")
        val row = genericRows[target.name]
            ?: return NormalizationError("No concrete generic replacement provided for genfor target '${target.name}'")
        val expandedStatements = mutableListOf<NodeStatement>()
        for (item in row) {
            val substituted = substituteStatement(
                statement.body,
                valueReplacements = mapOf(statement.valueName to item.name),
                typeReplacements = mapOf(statement.typeName to foxTypeToNodeType(item.type)),
            )
            when (val expanded = expandStatement(substituted, genericRows)) {
                is Normalized -> {
                    val value = expanded.value
                    if (value is NodeBlock && value.label == null) expandedStatements += value.statements
                    else expandedStatements += value
                }
                is NormalizationError -> return expanded
            }
        }
        Normalized(NodeBlock(null, expandedStatements))
    }
    is NodeIf -> {
        val thenBody = when (val expanded = expandStatement(statement.thenBody, genericRows)) {
            is Normalized -> expanded.value
            is NormalizationError -> return expanded
        }
        val elseBody = when (val elseBody = statement.elseBody) {
            null -> null
            else -> when (val expanded = expandStatement(elseBody, genericRows)) {
                is Normalized -> expanded.value
                is NormalizationError -> return expanded
            }
        }
        Normalized(statement.copy(thenBody = thenBody, elseBody = elseBody))
    }
    is NodeWhen -> {
        val cases = mutableListOf<NodeCase>()
        for (case in statement.cases) {
            val body = when (val expanded = expandStatement(case.body, genericRows)) {
                is Normalized -> expanded.value
                is NormalizationError -> return expanded
            }
            cases += case.copy(body = body)
        }
        Normalized(statement.copy(cases = cases))
    }
    is NodeWhile -> when (val expanded = expandStatement(statement.body, genericRows)) {
        is Normalized -> Normalized(statement.copy(body = expanded.value))
        is NormalizationError -> expanded
    }
    is NodeDoWhile -> when (val expanded = expandStatement(statement.body, genericRows)) {
        is Normalized -> Normalized(statement.copy(body = expanded.value))
        is NormalizationError -> expanded
    }
    else -> Normalized(statement)
}

private fun substituteStatement(
    statement: NodeStatement,
    valueReplacements: Map<String, String>,
    typeReplacements: Map<String, NodeType>,
): NodeStatement = when (statement) {
    is NodeSymbol -> valueReplacements[statement.name]?.let(::NodeSymbol) ?: statement
    is NodeBlock -> NodeBlock(statement.label, statement.statements.map { substituteStatement(it, valueReplacements, typeReplacements) })
    is NodeUnary -> statement.copy(right = substituteStatement(statement.right, valueReplacements, typeReplacements))
    is NodeBinary -> statement.copy(
        left = substituteStatement(statement.left, valueReplacements, typeReplacements),
        right = substituteStatement(statement.right, valueReplacements, typeReplacements),
    )
    is NodeAssign -> statement.copy(
        left = substituteStatement(statement.left, valueReplacements, typeReplacements),
        right = substituteStatement(statement.right, valueReplacements, typeReplacements),
    )
    is NodeTypeBinding -> statement.copy(type = substituteType(statement.type, typeReplacements))
    is NodeFieldAccess -> statement.copy(target = substituteStatement(statement.target, valueReplacements, typeReplacements))
    is NodeComponentAccess -> statement.copy(target = substituteStatement(statement.target, valueReplacements, typeReplacements))
    is NodeCall -> statement.copy(
        target = statement.target?.let { substituteStatement(it, valueReplacements, typeReplacements) },
        generics = statement.generics?.map { (name, type) -> name to substituteType(type, typeReplacements) },
        parameters = statement.parameters.map { (name, value) -> name to substituteStatement(value, valueReplacements, typeReplacements) },
    )
    is NodeConstruct -> statement.copy(
        type = substituteType(statement.type, typeReplacements),
        parameters = statement.parameters.map { (name, value) -> name to substituteStatement(value, valueReplacements, typeReplacements) },
    )
    is NodeLambda -> statement.copy(
        parameters = statement.parameters.map { (name, type) -> name to type?.let { substituteType(it, typeReplacements) } },
        body = substituteStatement(statement.body, valueReplacements, typeReplacements),
    )
    is NodeLambdaCall -> statement.copy(
        target = statement.target?.let { substituteStatement(it, valueReplacements, typeReplacements) },
        method = substituteStatement(statement.method, valueReplacements, typeReplacements),
        parameters = statement.parameters.map { substituteStatement(it, valueReplacements, typeReplacements) },
    )
    is NodeIf -> statement.copy(
        condition = substituteStatement(statement.condition, valueReplacements, typeReplacements),
        thenBody = substituteStatement(statement.thenBody, valueReplacements, typeReplacements),
        elseBody = statement.elseBody?.let { substituteStatement(it, valueReplacements, typeReplacements) },
    )
    is NodeWhen -> statement.copy(
        value = statement.value?.let { substituteStatement(it, valueReplacements, typeReplacements) },
        cases = statement.cases.map { case ->
            case.copy(
                conditions = case.conditions.map { substituteStatement(it, valueReplacements, typeReplacements) },
                body = substituteStatement(case.body, valueReplacements, typeReplacements),
            )
        },
    )
    is NodeWhile -> statement.copy(
        condition = substituteStatement(statement.condition, valueReplacements, typeReplacements),
        body = substituteStatement(statement.body, valueReplacements, typeReplacements),
    )
    is NodeDoWhile -> statement.copy(
        body = substituteStatement(statement.body, valueReplacements, typeReplacements),
        condition = substituteStatement(statement.condition, valueReplacements, typeReplacements),
    )
    is NodeGenFor -> statement.copy(
        targetType = substituteType(statement.targetType, typeReplacements),
        body = substituteStatement(statement.body, valueReplacements, typeReplacements),
    )
    is NodeYield -> statement.copy(value = substituteStatement(statement.value, valueReplacements, typeReplacements))
    is NodeReturn -> statement.copy(value = statement.value?.let { substituteStatement(it, valueReplacements, typeReplacements) })
    else -> statement
}

private fun substituteType(
    type: NodeType,
    typeReplacements: Map<String, NodeType>,
): NodeType = when (type) {
    is NodeNamedType -> if (type.generics.isEmpty()) typeReplacements[type.name] ?: type
    else type.copy(generics = type.generics.map { (name, argType) -> name to substituteType(argType, typeReplacements) })
    is NodeArrayType -> type.copy(elementType = substituteType(type.elementType, typeReplacements))
    is NodeTupleType -> type.copy(
        items = type.items.map { item ->
            when (item) {
                is NodeTupleTypeItem -> NodeTupleTypeItem(substituteType(item.type, typeReplacements))
                is NodeTupleSpreadItem -> NodeTupleSpreadItem(substituteType(item.type, typeReplacements))
            }
        },
    )
    is NodeNamedProjectionType -> type.copy(baseType = substituteType(type.baseType, typeReplacements))
    is NodeStructType -> type.copy(
        items = type.items.map { item ->
            when (item) {
                is NodeStructFieldItem -> NodeStructFieldItem(item.name, substituteType(item.type, typeReplacements))
                is NodeStructSpreadItem -> NodeStructSpreadItem(substituteType(item.type, typeReplacements))
            }
        },
    )
    is NodeDenamedProjectionType -> type.copy(baseType = substituteType(type.baseType, typeReplacements))
    is NodeEnumType -> type.copy(items = type.items.mapValues { (_, value) -> substituteType(value, typeReplacements) })
    is NodeRefType -> type.copy(referentType = substituteType(type.referentType, typeReplacements))
    is NodeLambdaType -> type.copy(
        thisType = substituteType(type.thisType, typeReplacements),
        parameters = type.parameters.map { substituteType(it, typeReplacements) },
        returnType = substituteType(type.returnType, typeReplacements),
    )
    else -> type
}

private fun foxTypeToNodeType(type: FoxType): NodeType = when (type) {
    is FoxPrimitiveType -> NodePrimitiveType(type)
    is FoxArrayType -> NodeArrayType(foxTypeToNodeType(type.elementType))
    is FoxTupleType -> NodeTupleType(type.componentTypes.map { NodeTupleTypeItem(foxTypeToNodeType(it)) })
    is FoxNamedProjectionType -> NodeNamedProjectionType(foxTypeToNodeType(type.baseType))
    is FoxStructType -> NodeStructType(type.fields.entries.map { (name, fieldType) -> NodeStructFieldItem(name, foxTypeToNodeType(fieldType)) })
    is FoxDenamedProjectionType -> NodeDenamedProjectionType(foxTypeToNodeType(type.baseType))
    is FoxEnumType -> NodeEnumType(type.items.mapValues { (_, value) -> foxTypeToNodeType(value) })
    is FoxRefType -> NodeRefType(foxTypeToNodeType(type.referentType))
    is FoxLambdaType -> NodeLambdaType(
        thisType = foxTypeToNodeType(type.thisType),
        parameters = type.parameters.map(::foxTypeToNodeType),
        returnType = foxTypeToNodeType(type.returnType),
    )
    is FoxGenericType -> NodeNamedType(type.name, emptyList())
    is FoxGenericArrayType -> NodeArrayType(foxTypeToNodeType(type.elementType))
    is FoxGenericTupleType -> NodeTupleType(type.componentTypes.map { NodeTupleTypeItem(foxTypeToNodeType(it)) })
    is FoxTupleTemplateType -> NodeTupleType(
        type.items.map { item ->
            when (item) {
                is FoxTupleTypeTemplateItem -> NodeTupleTypeItem(foxTypeToNodeType(item.type))
                is FoxTupleSpreadTemplateItem -> NodeTupleSpreadItem(foxTypeToNodeType(item.type))
            }
        },
    )
    is FoxGenericStructType -> NodeStructType(
        type.fields.entries.map { (name, fieldType) -> NodeStructFieldItem(name, foxTypeToNodeType(fieldType)) },
    )
    is FoxStructTemplateType -> NodeStructType(
        type.items.map { item ->
            when (item) {
                is FoxStructFieldTemplateItem -> NodeStructFieldItem(item.name, foxTypeToNodeType(item.type))
                is FoxStructSpreadTemplateItem -> NodeStructSpreadItem(foxTypeToNodeType(item.type))
            }
        },
    )
    is FoxGenericEnumType -> NodeEnumType(type.items.mapValues { (_, value) -> foxTypeToNodeType(value) })
    is FoxGenericRefType -> NodeRefType(foxTypeToNodeType(type.referentType))
    is FoxGenericLambdaType -> NodeLambdaType(
        thisType = foxTypeToNodeType(type.thisType),
        parameters = type.parameters.map(::foxTypeToNodeType),
        returnType = foxTypeToNodeType(type.returnType),
    )
}
