package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ast.*

sealed interface StatementStructureCheckResult
data object StatementStructureCheckSuccess : StatementStructureCheckResult
data class StatementStructureCheckFailure(val errors: List<StatementStructureCheckError>) : StatementStructureCheckResult

sealed interface StatementStructureCheckError
data class StatementAssignmentTargetMustBeAssignable(
    val assignment: ParsedFoxAssign,
    val target: ParsedFoxStatement<*>,
) : StatementStructureCheckError

data class StatementDefinitionTargetMustBeSymbol(
    val assignment: ParsedFoxAssign,
    val target: ParsedFoxStatement<*>,
) : StatementStructureCheckError

data class StatementBreakOutsideLoop(val statement: ParsedFoxBreak) : StatementStructureCheckError
data class StatementContinueOutsideLoop(val statement: ParsedFoxContinue) : StatementStructureCheckError

data class StatementLabelNotFound(
    val statement: ParsedFoxStatement<*>,
    val label: ParsedString,
) : StatementStructureCheckError

data class StatementContinueTargetMustBeLoop(
    val statement: ParsedFoxContinue,
    val label: ParsedString,
) : StatementStructureCheckError

data class StatementMultipleElseCases(
    val statement: ParsedFoxWhen,
    val case: ParsedFoxCase,
) : StatementStructureCheckError

data class StatementElseCaseMustBeLast(
    val statement: ParsedFoxWhen,
    val case: ParsedFoxCase,
) : StatementStructureCheckError

private enum class LabelTargetKind {
    Loop,
    Other,
}

private data class LabelTarget(val name: String, val kind: LabelTargetKind)

private data class StatementContext(
    val loopDepth: Int = 0,
    val labels: List<LabelTarget> = emptyList(),
) {
    fun withLabel(label: ParsedString?, kind: LabelTargetKind): StatementContext {
        if (label == null) return this
        return copy(labels = labels + LabelTarget(label.node, kind))
    }
    
    fun enterLoop(label: ParsedString?): StatementContext {
        return copy(loopDepth = loopDepth + 1).withLabel(label, LabelTargetKind.Loop)
    }
    
    fun findLabel(label: ParsedString): LabelTarget? {
        return labels.asReversed().firstOrNull { it.name == label.node }
    }
}

fun runStatementStructureCheck(file: ParsedFoxFile) = StatementStructureCheckContext().run(file)

private class StatementStructureCheckContext {
    
    private val errors = mutableListOf<StatementStructureCheckError>()
    
    fun run(file: ParsedFoxFile): StatementStructureCheckResult {
        file.elements.forEach { element ->
            if (element is ParsedFoxMethodDefinition) visit(element.body, StatementContext())
        }
        if (errors.isNotEmpty()) return StatementStructureCheckFailure(errors)
        return StatementStructureCheckSuccess
    }
    
    private fun isAssignableTarget(statement: ParsedFoxStatement<*>): Boolean = when (statement) {
        is ParsedFoxUnresolvedSymbol -> true
        is ParsedFoxFieldAccess -> true
        is ParsedFoxIndexAccess -> true
        else -> false
    }
    
    private fun visit(statement: ParsedFoxStatement<*>, context: StatementContext) {
        when (statement) {
            is ParsedFoxThis -> {}
            is ParsedFoxUnresolvedSymbol -> {}
            is ParsedFoxEntityStatement -> {}
            is ParsedFoxIntStatement -> {}
            is ParsedFoxLongStatement -> {}
            is ParsedFoxFloatStatement -> {}
            is ParsedFoxDoubleStatement -> {}
            is ParsedFoxBreak -> {
                val label = statement.label
                if (label == null) {
                    if (context.loopDepth == 0) errors += StatementBreakOutsideLoop(statement)
                } else if (context.findLabel(label) == null) {
                    errors += StatementLabelNotFound(statement, label)
                }
            }
            is ParsedFoxContinue -> {
                val label = statement.label
                if (label == null) {
                    if (context.loopDepth == 0) errors += StatementContinueOutsideLoop(statement)
                } else {
                    val target = context.findLabel(label)
                    when {
                        target == null -> errors += StatementLabelNotFound(statement, label)
                        target.kind != LabelTargetKind.Loop -> errors += StatementContinueTargetMustBeLoop(statement, label)
                    }
                }
            }
            is ParsedFoxYield -> {
                statement.label?.let { label ->
                    if (context.findLabel(label) == null) errors += StatementLabelNotFound(statement, label)
                }
                visit(statement.value, context)
            }
            is ParsedFoxReturn -> statement.value?.let { visit(it, context) }
            is ParsedFoxUnary -> visit(statement.right, context)
            is ParsedFoxBinary -> {
                visit(statement.left, context)
                visit(statement.right, context)
            }
            is ParsedFoxTypeBinding -> {}
            is ParsedFoxAssign -> {
                if (statement.operator.node == FoxDefAssignOperator) {
                    if (statement.left !is ParsedFoxUnresolvedSymbol) {
                        errors += StatementDefinitionTargetMustBeSymbol(statement, statement.left)
                    }
                } else if (!isAssignableTarget(statement.left)) {
                    errors += StatementAssignmentTargetMustBeAssignable(statement, statement.left)
                }
                visit(statement.left, context)
                visit(statement.right, context)
            }
            is ParsedFoxFieldAccess -> visit(statement.target, context)
            is ParsedFoxIndexAccess -> {
                visit(statement.target, context)
                statement.indices.node.forEach { visit(it, context) }
            }
            is ParsedFoxFormattedString -> statement.parts?.node?.forEach { part ->
                when (part) {
                    is ParsedFoxFormattedText -> {}
                    is ParsedFoxFormattedExpression -> visit(part.expression, context)
                }
            }
            is ParsedFoxConstruct -> statement.parameters.node.forEach { visit(it.node.second, context) }
            is ParsedFoxCall -> {
                statement.target?.let { visit(it, context) }
                statement.parameters.node.forEach { visit(it.node.second, context) }
            }
            is ParsedFoxIndirectCall -> {
                statement.target?.let { visit(it, context) }
                visit(statement.method, context)
                statement.parameters.node.forEach { visit(it.node.second, context) }
            }
            is ParsedFoxBlock -> {
                val bodyContext = context.withLabel(statement.label, LabelTargetKind.Other)
                statement.statements.node.forEach { visit(it, bodyContext) }
            }
            is ParsedFoxIf -> {
                visit(statement.condition, context)
                val bodyContext = context.withLabel(statement.label, LabelTargetKind.Other)
                visit(statement.thenBody, bodyContext)
                statement.elseBody?.let { visit(it, bodyContext) }
            }
            is ParsedFoxWhen -> {
                statement.value?.let { visit(it, context) }
                val cases = statement.cases.node
                var seenElse = false
                cases.forEachIndexed { index, case ->
                    if (case.conditions == null) {
                        if (seenElse) errors += StatementMultipleElseCases(statement, case)
                        if (index != cases.lastIndex) errors += StatementElseCaseMustBeLast(statement, case)
                        seenElse = true
                    }
                }
                
                val bodyContext = context.withLabel(statement.label, LabelTargetKind.Other)
                cases.forEach { case ->
                    case.conditions?.node?.forEach { visit(it, context) }
                    visit(case.body, bodyContext)
                }
            }
            is ParsedFoxWhile -> {
                visit(statement.condition, context)
                visit(statement.body, context.enterLoop(statement.label))
            }
            is ParsedFoxDoWhile -> {
                val loopContext = context.enterLoop(statement.label)
                visit(statement.body, loopContext)
                visit(statement.condition, loopContext)
            }
            is ParsedFoxLambda -> visit(statement.body, StatementContext())
        }
    }
}
