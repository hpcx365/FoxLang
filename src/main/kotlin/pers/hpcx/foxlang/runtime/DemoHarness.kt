package pers.hpcx.foxlang.runtime

import pers.hpcx.foxlang.frontend.*
import pers.hpcx.foxlang.lowering.*
import pers.hpcx.foxlang.types.*
import java.util.*

data class DemoRunResult(
    val expanded: NodeMethodDefinition,
    val signature: FoxMethodSignature,
    val returnValue: FoxEntity,
    val output: List<String>,
)

fun runInstantiatedDemoMethod(
    file: NodeFile,
    methodName: String,
    genericReplacements: SequencedMap<String, FoxConcreteType>,
    parameters: SequencedMap<String, FoxEntity>,
): NormalizationResult<DemoRunResult> {
    val lowered = when (val result = file.instantiateMethodDeclaration(methodName, genericReplacements)) {
        is Normalized -> result.value
        is NormalizationError -> return result
    }
    
    val interpreter = Interpreter()
    val output = mutableListOf<String>()
    collectPrintTypes(lowered.definition.body).forEach { type ->
        registerPrintImplementation(interpreter, type, output)
    }
    
    val (methodId, implementation) = ExpandedMethodDefinition(lowered.definition).lowerDemoMethod(
        generics = genericReplacements.mapValuesTo(LinkedHashMap()) { (_, value) -> value },
    )
    interpreter.methods[methodId] = implementation
    
    val returnValue = interpreter.invoke(methodId, FoxUnit, parameters)
    return Normalized(
        DemoRunResult(
            expanded = lowered.definition,
            signature = lowered.signature,
            returnValue = returnValue,
            output = output,
        ),
    )
}

private fun collectPrintTypes(statement: NodeStatement, result: MutableSet<FoxType> = linkedSetOf()): Set<FoxType> {
    when (statement) {
        is NodeBlock -> statement.statements.forEach { collectPrintTypes(it, result) }
        is NodeCall -> {
            if (statement.name == "print") {
                statement.generics?.singleOrNull()?.second?.let { result += nodeTypeToFoxType(it) }
            }
            statement.target?.let { collectPrintTypes(it, result) }
            statement.parameters.forEach { (_, parameter) -> collectPrintTypes(parameter, result) }
        }
        is NodeConstruct -> {
            statement.parameters.forEach { (_, parameter) -> collectPrintTypes(parameter, result) }
        }
        is NodeUnary -> collectPrintTypes(statement.right, result)
        is NodeBinary -> {
            collectPrintTypes(statement.left, result)
            collectPrintTypes(statement.right, result)
        }
        is NodeAssign -> {
            collectPrintTypes(statement.left, result)
            collectPrintTypes(statement.right, result)
        }
        is NodeFieldAccess -> collectPrintTypes(statement.target, result)
        is NodeComponentAccess -> collectPrintTypes(statement.target, result)
        is NodeLambda -> collectPrintTypes(statement.body, result)
        is NodeLambdaCall -> {
            statement.target?.let { collectPrintTypes(it, result) }
            collectPrintTypes(statement.method, result)
            statement.parameters.forEach { collectPrintTypes(it, result) }
        }
        is NodeIf -> {
            collectPrintTypes(statement.condition, result)
            collectPrintTypes(statement.thenBody, result)
            statement.elseBody?.let { collectPrintTypes(it, result) }
        }
        is NodeWhen -> {
            statement.value?.let { collectPrintTypes(it, result) }
            statement.cases.forEach { case ->
                case.conditions.forEach { collectPrintTypes(it, result) }
                collectPrintTypes(case.body, result)
            }
        }
        is NodeWhile -> {
            collectPrintTypes(statement.condition, result)
            collectPrintTypes(statement.body, result)
        }
        is NodeDoWhile -> {
            collectPrintTypes(statement.body, result)
            collectPrintTypes(statement.condition, result)
        }
        is NodeGenFor -> collectPrintTypes(statement.body, result)
        is NodeYield -> collectPrintTypes(statement.value, result)
        is NodeReturn -> statement.value?.let { collectPrintTypes(it, result) }
        else -> {}
    }
    return result
}

