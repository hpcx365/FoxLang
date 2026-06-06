package pers.hpcx.foxlang.lowering

import pers.hpcx.foxlang.frontend.*
import pers.hpcx.foxlang.runtime.*
import pers.hpcx.foxlang.types.*
import java.util.*

val printMethodSignature = FoxMethodSignature(
    name = "print",
    generics = linkedMapOf("T" to FoxAnyConstraint),
    thisType = FoxUnitType,
    parameters = linkedMapOf("value" to FoxGenericType("T")),
    returnType = FoxUnitType,
    isInline = false,
)

fun createPrintMethodImplementation(output: MutableList<String>) = FoxSimpleNativeMethodImplementation(
    signature = printMethodSignature,
) { _, params ->
    output += params.getValue("value").toDisplayString()
    FoxUnit
}

fun printMethodIdentifier(type: FoxType) = FoxMethodIdentifier(
    name = "print",
    generics = linkedMapOf("T" to type),
    thisType = FoxUnitType,
    parameters = linkedMapOf("value" to type),
)

fun registerPrintImplementation(
    interpreter: Interpreter,
    type: FoxType,
    output: MutableList<String>,
) {
    interpreter.methods[printMethodIdentifier(type)] = createPrintMethodImplementation(output)
}

fun ExpandedMethodDefinition.lowerDemoMethod(
    generics: SequencedMap<String, FoxType>,
): Pair<FoxMethodIdentifier, FoxCustomizedMethodImplementation> {
    val definition = definition
    val parameterTypes = linkedMapOf<String, FoxType>()
    definition.parameters.forEach { parameter ->
        val named = parameter as NodeNamedFormalParameter
        parameterTypes[named.name] = nodeTypeToFoxType(named.type)
    }
    val methodId = FoxMethodIdentifier(
        name = definition.name,
        generics = generics,
        thisType = FoxUnitType,
        parameters = parameterTypes,
    )
    val instructions = mutableListOf<FoxInst>()
    definition.body.collectDemoInstructions(instructions)
    val implementation = FoxCustomizedMethodImplementation(
        startBlock = "entry",
        blocks = linkedMapOf(
            "entry" to FoxInstBlock(
                instructions = instructions,
                jump = JumpReturn(SlotConst(FoxUnit)),
            ),
        ),
    )
    return methodId to implementation
}

private fun NodeStatement.collectDemoInstructions(
    target: MutableList<FoxInst>,
) {
    when (this) {
        is NodeBlock -> statements.forEach { it.collectDemoInstructions(target) }
        is NodeCall -> {
            if (this.target != null) error("Demo lowering supports only free function calls")
            val parameterType = generics?.singleOrNull()?.second?.let(::nodeTypeToFoxType) ?: FoxStringType
            val method = when (name) {
                "print" -> printMethodIdentifier(parameterType)
                else -> FoxMethodIdentifier(
                    name = name,
                    generics = linkedMapOf<String, FoxType>().apply {
                        generics?.forEach { (genericName, type) ->
                            put(genericName ?: error("Demo lowering requires named generics"), nodeTypeToFoxType(type))
                        }
                    },
                    thisType = FoxUnitType,
                    parameters = linkedMapOf("value" to parameterType),
                )
            }
            val params = linkedMapOf<String, FoxFetchSlot>()
            val value = parameters.singleOrNull()
                ?: error("Demo lowering supports only one-argument calls")
            params["value"] = when (val statement = value.second) {
                is NodeSymbol -> SlotLocal(statement.name)
                is NodeEntity -> SlotConst(statement.value)
                else -> error("Demo lowering supports only symbol or constant call arguments")
            }
            target += InstCall(
                target = SlotConst(FoxUnit),
                params = params,
                method = method,
            )
        }
        is NodeReturn -> {
            target += InstCopy(SlotLocal("__return__"), statementToFetchSlot(this.value))
        }
        else -> error("Demo lowering does not support statement '${this::class.simpleName}' yet")
    }
}

private fun statementToFetchSlot(statement: NodeStatement?): FoxFetchSlot = when (statement) {
    null -> SlotConst(FoxUnit)
    is NodeSymbol -> SlotLocal(statement.name)
    is NodeEntity -> SlotConst(statement.value)
    else -> error("Demo lowering supports only symbol or constant return values")
}

internal fun nodeTypeToFoxType(type: NodeType): FoxType = when (type) {
    is NodePrimitiveType -> type.type
    is NodeNamedType -> FoxGenericType(type.name)
    is NodeArrayType -> FoxGenericArrayType(nodeTypeToFoxType(type.elementType))
    is NodeTupleType -> FoxTupleTemplateType(
        type.items.map { item ->
            when (item) {
                is NodeTupleTypeItem -> FoxTupleTypeTemplateItem(nodeTypeToFoxType(item.type))
                is NodeTupleSpreadItem -> FoxTupleSpreadTemplateItem(nodeTypeToFoxType(item.type))
            }
        },
    )
    is NodeNamedProjectionType -> FoxNamedProjectionType(nodeTypeToFoxType(type.baseType))
    is NodeStructWildcardType -> FoxGenericStructType(emptyMap())
    is NodeStructType -> FoxStructTemplateType(
        type.items.map { item ->
            when (item) {
                is NodeStructFieldItem -> FoxStructFieldTemplateItem(item.name, nodeTypeToFoxType(item.type))
                is NodeStructSpreadItem -> FoxStructSpreadTemplateItem(nodeTypeToFoxType(item.type))
            }
        },
    )
    is NodeDenamedProjectionType -> FoxDenamedProjectionType(nodeTypeToFoxType(type.baseType))
    is NodeEnumType -> FoxGenericEnumType(type.items.mapValues { (_, value) -> nodeTypeToFoxType(value) })
    is NodeRefType -> FoxGenericRefType(nodeTypeToFoxType(type.referentType))
    is NodeLambdaType -> FoxGenericLambdaType(
        thisType = nodeTypeToFoxType(type.thisType),
        parameters = type.parameters.map(::nodeTypeToFoxType),
        returnType = nodeTypeToFoxType(type.returnType),
    )
}

private fun FoxEntity.toDisplayString(): String = when (this) {
    FoxUnit -> "unit"
    is FoxBool -> value.toString()
    is FoxByte -> value.toString()
    is FoxShort -> value.toString()
    is FoxInt -> value.toString()
    is FoxLong -> value.toString()
    is FoxFloat -> value.toString()
    is FoxDouble -> value.toString()
    is FoxChar -> value.toString()
    is FoxString -> value
    is FoxArray -> elements.joinToString(prefix = "[", postfix = "]") { it.toDisplayString() }
    is FoxTuple -> components.joinToString(prefix = "(", postfix = ")") { it.toDisplayString() }
    is FoxStruct -> fields.entries.joinToString(prefix = "Struct(", postfix = ")") { "${it.key}=${it.value.toDisplayString()}" }
    is FoxEnum -> "$name(${value.toDisplayString()})"
    is FoxRef -> "ref($referent)"
    is FoxLambda -> "lambda"
}

