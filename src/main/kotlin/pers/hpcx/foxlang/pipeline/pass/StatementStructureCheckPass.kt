package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*

sealed interface StatementStructureCheckResult
data object StatementStructureCheckSuccess : StatementStructureCheckResult
data class StatementStructureCheckFailure(val errors: List<StatementStructureCheckError>) : StatementStructureCheckResult

sealed interface StatementStructureCheckError
data class StatementAssignmentTargetMustBeAssignable(
    val assignment: SyntaxAssign,
    val target: SyntaxStatement<*>,
) : StatementStructureCheckError

data class StatementDefinitionTargetMustBeSymbol(
    val assignment: SyntaxAssign,
    val target: SyntaxStatement<*>,
) : StatementStructureCheckError

data class StatementBreakOutsideLoop(val statement: SyntaxBreak) : StatementStructureCheckError
data class StatementContinueOutsideLoop(val statement: SyntaxContinue) : StatementStructureCheckError

data class StatementLabelNotFound(
    val statement: SyntaxStatement<*>,
    val label: SyntaxString,
) : StatementStructureCheckError

data class StatementContinueTargetMustBeLoop(
    val statement: SyntaxContinue,
    val label: SyntaxString,
) : StatementStructureCheckError

data class StatementMultipleElseCases(
    val statement: SyntaxWhen,
    val case: SyntaxCase,
) : StatementStructureCheckError

data class StatementElseCaseMustBeLast(
    val statement: SyntaxWhen,
    val case: SyntaxCase,
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
    fun withLabel(label: SyntaxString?, kind: LabelTargetKind): StatementContext {
        if (label == null) return this
        return copy(labels = labels + LabelTarget(label.node, kind))
    }
    
    fun enterLoop(label: SyntaxString?): StatementContext {
        return copy(loopDepth = loopDepth + 1).withLabel(label, LabelTargetKind.Loop)
    }
    
    fun findLabel(label: SyntaxString): LabelTarget? {
        return labels.asReversed().firstOrNull { it.name == label.node }
    }
}

fun runStatementStructureCheck(file: SyntaxFile) = StatementStructureCheckContext().run(file)

private class StatementStructureCheckContext {
    
    private val errors = mutableListOf<StatementStructureCheckError>()
    
    fun run(file: SyntaxFile): StatementStructureCheckResult {
        file.elements.forEach { element ->
            if (element is SyntaxMethodDefinition) visit(element.body, StatementContext())
        }
        if (errors.isNotEmpty()) return StatementStructureCheckFailure(errors)
        return StatementStructureCheckSuccess
    }
    
    private fun isAssignableTarget(statement: SyntaxStatement<*>): Boolean = when (statement) {
        is SyntaxUnresolvedSymbol -> true
        is SyntaxFieldAccess -> true
        is SyntaxIndexAccess -> true
        else -> false
    }
    
    private fun visit(statement: SyntaxStatement<*>, context: StatementContext) {
        when (statement) {
            is SyntaxThis -> {}
            is SyntaxUnresolvedSymbol -> {}
            is SyntaxEntityStatement -> {}
            is SyntaxIntStatement -> {}
            is SyntaxLongStatement -> {}
            is SyntaxFloatStatement -> {}
            is SyntaxDoubleStatement -> {}
            is SyntaxBreak -> {
                val label = statement.label
                if (label == null) {
                    if (context.loopDepth == 0) errors += StatementBreakOutsideLoop(statement)
                } else if (context.findLabel(label) == null) {
                    errors += StatementLabelNotFound(statement, label)
                }
            }
            is SyntaxContinue -> {
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
            is SyntaxYield -> {
                statement.label?.let { label ->
                    if (context.findLabel(label) == null) errors += StatementLabelNotFound(statement, label)
                }
                visit(statement.value, context)
            }
            is SyntaxReturn -> statement.value?.let { visit(it, context) }
            is SyntaxUnary -> visit(statement.right, context)
            is SyntaxBinary -> {
                visit(statement.left, context)
                visit(statement.right, context)
            }
            is SyntaxTypeBinding -> {}
            is SyntaxAssign -> {
                if (statement.operator.node.operator == AssignOperatorEnum.Def) {
                    if (statement.left !is SyntaxUnresolvedSymbol) {
                        errors += StatementDefinitionTargetMustBeSymbol(statement, statement.left)
                    }
                } else if (!isAssignableTarget(statement.left)) {
                    errors += StatementAssignmentTargetMustBeAssignable(statement, statement.left)
                }
                visit(statement.left, context)
                visit(statement.right, context)
            }
            is SyntaxFieldAccess -> visit(statement.target, context)
            is SyntaxIndexAccess -> {
                visit(statement.target, context)
                statement.indices.node.forEach { visit(it, context) }
            }
            is SyntaxFormattedString -> statement.parts?.node?.forEach { part ->
                when (part) {
                    is SyntaxFormattedText -> {}
                    is SyntaxFormattedExpression -> visit(part.expression, context)
                }
            }
            is SyntaxConstruct -> statement.parameters.node.forEach { visit(it.node.second, context) }
            is SyntaxCall -> {
                statement.target?.let { visit(it, context) }
                statement.parameters.node.forEach { visit(it.node.second, context) }
            }
            is SyntaxIndirectCall -> {
                statement.target?.let { visit(it, context) }
                visit(statement.method, context)
                statement.parameters.node.forEach { visit(it.node.second, context) }
            }
            is SyntaxBlock -> {
                val bodyContext = context.withLabel(statement.label, LabelTargetKind.Other)
                statement.statements.node.forEach { visit(it, bodyContext) }
            }
            is SyntaxIf -> {
                visit(statement.condition, context)
                val bodyContext = context.withLabel(statement.label, LabelTargetKind.Other)
                visit(statement.thenBody, bodyContext)
                statement.elseBody?.let { visit(it, bodyContext) }
            }
            is SyntaxWhen -> {
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
            is SyntaxWhile -> {
                visit(statement.condition, context)
                visit(statement.body, context.enterLoop(statement.label))
            }
            is SyntaxDoWhile -> {
                val loopContext = context.enterLoop(statement.label)
                visit(statement.body, loopContext)
                visit(statement.condition, loopContext)
            }
            is SyntaxLambda -> visit(statement.body, StatementContext())
        }
    }
}
