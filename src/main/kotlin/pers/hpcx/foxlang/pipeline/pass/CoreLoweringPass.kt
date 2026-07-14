package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.runtime.FoxUnit

sealed interface CoreLoweringResult
data class CoreLoweringSuccess(val program: CoreProgram) : CoreLoweringResult

fun runCoreLowering(file: SurfaceFile): CoreLoweringResult = CoreLoweringContext().run(file)

private class CoreLoweringContext {
    
    private val objectAccessors = mutableSetOf<CoreObjectAccessorRequest>()
    
    fun run(file: SurfaceFile): CoreLoweringResult {
        val methods = file.elements.mapNotNull { element ->
            when (element) {
                is SurfaceTypeAlias -> {
                    collectObjectAccessors(element.alias)
                    null
                }
                is SurfaceMethodDefinition -> lowerMethod(element)
            }
        }
        return CoreLoweringSuccess(CoreProgram(methods, objectAccessors))
    }
    
    private fun lowerMethod(method: SurfaceMethodDefinition): CoreMethod {
        method.generics.values.forEach(::collectObjectAccessors)
        collectObjectAccessors(method.thisType)
        method.parameters.values.forEach(::collectObjectAccessors)
        collectObjectAccessors(method.returnType)
        
        return CoreMethod(
            generics = method.generics,
            thisType = method.thisType,
            name = method.name,
            parameters = method.parameters,
            returnType = method.returnType,
            body = CoreMethodLoweringContext(::collectObjectAccessors).lowerStatementBlock(method.body),
        )
    }
    
    private fun collectObjectAccessors(type: SurfaceType) {
        when (type) {
            is SurfacePrimitiveType -> {}
            is SurfaceWildcardType -> when (type) {
                is SurfaceAnyType -> {}
                is SurfaceAnyTupleType -> {}
                is SurfaceAnyStructType -> {}
                is SurfaceAnyObjectType -> {}
                is SurfaceAnyEnumType -> {}
                is SurfaceAnyOfType -> type.types.forEach(::collectObjectAccessors)
                is SurfaceAllOfType -> type.types.forEach(::collectObjectAccessors)
                is SurfaceNoneOfType -> type.types.forEach(::collectObjectAccessors)
                is SurfaceAnyTupleOfType -> collectObjectAccessors(type.component)
                is SurfaceAnyStructOfType -> type.fields.forEach(::collectObjectAccessors)
            }
            is SurfaceBuiltInType -> when (type) {
                is SurfaceTupleType -> type.components.forEach(::collectObjectAccessors)
                is SurfaceStructType -> type.fields.values.forEach(::collectObjectAccessors)
                is SurfaceObjectType -> type.members.forEach { (name, memberType) ->
                    objectAccessors += CoreObjectAccessorRequest(type, name, memberType)
                    collectObjectAccessors(memberType)
                }
                is SurfaceEnumType -> type.entries.values.forEach(::collectObjectAccessors)
                is SurfaceArrayType -> collectObjectAccessors(type.element)
                is SurfaceRefType -> collectObjectAccessors(type.referent)
                is SurfaceMethodType -> {
                    collectObjectAccessors(type.`this`)
                    type.parameters.values.forEach(::collectObjectAccessors)
                    collectObjectAccessors(type.`return`)
                }
            }
            is SurfaceTransformType -> when (type) {
                is SurfaceTupleGetComponentType -> collectObjectAccessors(type.type)
                is SurfaceTupleGetComponentBackType -> collectObjectAccessors(type.type)
                is SurfaceTupleGetFirstComponentsType -> collectObjectAccessors(type.type)
                is SurfaceTupleGetFirstComponentsExactType -> collectObjectAccessors(type.type)
                is SurfaceTupleGetLastComponentsType -> collectObjectAccessors(type.type)
                is SurfaceTupleGetLastComponentsExactType -> collectObjectAccessors(type.type)
                is SurfaceTupleDropFirstComponentsType -> collectObjectAccessors(type.type)
                is SurfaceTupleDropFirstComponentsExactType -> collectObjectAccessors(type.type)
                is SurfaceTupleDropLastComponentsType -> collectObjectAccessors(type.type)
                is SurfaceTupleDropLastComponentsExactType -> collectObjectAccessors(type.type)
                is SurfaceTupleMergeTuplesType -> type.types.forEach(::collectObjectAccessors)
                is SurfaceStructGetFieldTypeByNameType -> collectObjectAccessors(type.type)
                is SurfaceStructGetFieldTypeByIndexType -> collectObjectAccessors(type.type)
                is SurfaceStructGetFieldTypeByIndexBackType -> collectObjectAccessors(type.type)
                is SurfaceStructGetFirstFieldsType -> collectObjectAccessors(type.type)
                is SurfaceStructGetFirstFieldsExactType -> collectObjectAccessors(type.type)
                is SurfaceStructGetLastFieldsType -> collectObjectAccessors(type.type)
                is SurfaceStructGetLastFieldsExactType -> collectObjectAccessors(type.type)
                is SurfaceStructDropFirstFieldsType -> collectObjectAccessors(type.type)
                is SurfaceStructDropFirstFieldsExactType -> collectObjectAccessors(type.type)
                is SurfaceStructDropLastFieldsType -> collectObjectAccessors(type.type)
                is SurfaceStructDropLastFieldsExactType -> collectObjectAccessors(type.type)
                is SurfaceStructSelectFieldsType -> collectObjectAccessors(type.type)
                is SurfaceStructSelectFieldsExactType -> collectObjectAccessors(type.type)
                is SurfaceStructDropFieldsType -> collectObjectAccessors(type.type)
                is SurfaceStructDropFieldsExactType -> collectObjectAccessors(type.type)
                is SurfaceStructExtractFieldTypesType -> collectObjectAccessors(type.type)
                is SurfaceStructMergeStructsType -> type.types.forEach(::collectObjectAccessors)
                is SurfaceObjectGetMemberTypeType -> collectObjectAccessors(type.type)
                is SurfaceObjectSelectMembersType -> collectObjectAccessors(type.type)
                is SurfaceObjectSelectMembersExactType -> collectObjectAccessors(type.type)
                is SurfaceObjectDropMembersType -> collectObjectAccessors(type.type)
                is SurfaceObjectDropMembersExactType -> collectObjectAccessors(type.type)
                is SurfaceObjectMergeObjectsType -> type.types.forEach(::collectObjectAccessors)
                is SurfaceEnumGetEntryTypeType -> collectObjectAccessors(type.type)
                is SurfaceEnumSelectEntriesType -> collectObjectAccessors(type.type)
                is SurfaceEnumSelectEntriesExactType -> collectObjectAccessors(type.type)
                is SurfaceEnumDropEntriesType -> collectObjectAccessors(type.type)
                is SurfaceEnumDropEntriesExactType -> collectObjectAccessors(type.type)
                is SurfaceEnumMergeEnumsType -> type.types.forEach(::collectObjectAccessors)
                is SurfaceArrayGetElementTypeType -> collectObjectAccessors(type.type)
                is SurfaceRefGetReferentTypeType -> collectObjectAccessors(type.type)
                is SurfaceMethodGetThisTypeType -> collectObjectAccessors(type.type)
                is SurfaceMethodGetParameterStructType -> collectObjectAccessors(type.type)
                is SurfaceMethodGetReturnTypeType -> collectObjectAccessors(type.type)
                is SurfaceMethodOfType -> {
                    collectObjectAccessors(type.`this`)
                    collectObjectAccessors(type.parameters)
                    collectObjectAccessors(type.`return`)
                }
            }
            is SurfaceUnresolvedType -> type.parameters?.forEach(::collectObjectAccessors)
            is SurfacePlaceholderType -> error("Placeholder type cannot be lowered")
        }
    }
}

private class CoreMethodLoweringContext(
    private val collectObjectAccessors: (SurfaceType) -> Unit,
) {
    
    private var nextTempId = 0L
    
    fun lowerStatementBlock(statement: SurfaceStatement): CoreBlock {
        if (statement is SurfaceBlock) return lowerBlock(statement)
        return CoreBlock(null, lowerStatement(statement))
    }
    
    private fun lowerBlock(block: SurfaceBlock): CoreBlock {
        return CoreBlock(block.label, block.statements.flatMap(::lowerStatement))
    }
    
    private fun lowerStatement(statement: SurfaceStatement): List<CoreStatement> = when (statement) {
        SurfaceThis,
        is SurfaceUnresolvedSymbol,
        is SurfaceEntityStatement,
        is SurfaceUnary,
        is SurfaceBinary,
        is SurfaceFieldAccess,
        is SurfaceIndexAccess,
        is SurfaceFormattedString,
        is SurfaceConstruct,
        is SurfaceCall,
        is SurfaceIndirectCall,
        is SurfaceLambda,
            -> lowerValue(statement).let { it.statements + CoreEvaluate(it.value) }
        
        is SurfaceTypeBinding -> {
            collectObjectAccessors(statement.type)
            listOf(CoreTypeBinding(statement.name, statement.type))
        }
        is SurfaceAssign -> lowerAssign(statement).statements
        is SurfaceBreak -> listOf(CoreBreak(statement.label))
        is SurfaceContinue -> listOf(CoreContinue(statement.label))
        is SurfaceYield -> lowerValue(statement.value).let { it.statements + CoreYield(statement.label, it.value) }
        is SurfaceReturn -> statement.value?.let(::lowerValue)
            ?.let { it.statements + CoreReturn(it.value) }
            ?: listOf(CoreReturn(null))
        
        is SurfaceBlock -> listOf(CoreBlockStatement(lowerBlock(statement)))
        is SurfaceIf -> listOf(lowerIfStatement(statement))
        is SurfaceWhen -> listOf(lowerWhenStatement(statement))
        is SurfaceWhile -> listOf(
            CoreWhileStatement(
                label = statement.label,
                condition = lowerValue(statement.condition),
                body = lowerStatementBlock(statement.body),
            ),
        )
        is SurfaceDoWhile -> listOf(
            CoreDoWhileStatement(
                label = statement.label,
                body = lowerStatementBlock(statement.body),
                condition = lowerValue(statement.condition),
            ),
        )
    }
    
    private fun lowerValue(statement: SurfaceStatement): CoreValueBlock = when (statement) {
        SurfaceThis -> CoreValueBlock(emptyList(), CoreThis)
        is SurfaceUnresolvedSymbol -> CoreValueBlock(emptyList(), CoreSymbol(statement.name))
        is SurfaceEntityStatement -> CoreValueBlock(emptyList(), CoreConst(statement.value))
        is SurfaceUnary -> lowerUnary(statement)
        is SurfaceBinary -> lowerBinary(statement)
        is SurfaceTypeBinding -> {
            collectObjectAccessors(statement.type)
            CoreValueBlock(listOf(CoreTypeBinding(statement.name, statement.type)), unitValue)
        }
        is SurfaceFieldAccess -> lowerFieldGetter(statement)
        is SurfaceIndexAccess -> lowerIndexGetter(statement)
        is SurfaceAssign -> lowerAssign(statement)
        is SurfaceFormattedString -> lowerFormattedString(statement)
        is SurfaceConstruct -> lowerConstruct(statement)
        is SurfaceCall -> lowerCall(statement)
        is SurfaceIndirectCall -> lowerIndirectCall(statement)
        is SurfaceLambda -> lowerLambda(statement)
        is SurfaceBlock -> lowerToTemp(CoreBlockExpression(lowerBlock(statement)))
        is SurfaceIf -> lowerIfExpression(statement)
        is SurfaceWhen -> lowerWhenExpression(statement)
        is SurfaceWhile -> {
            val loop = CoreWhileStatement(statement.label, lowerValue(statement.condition), lowerStatementBlock(statement.body))
            CoreValueBlock(listOf(loop), unitValue)
        }
        is SurfaceDoWhile -> {
            val loop = CoreDoWhileStatement(statement.label, lowerStatementBlock(statement.body), lowerValue(statement.condition))
            CoreValueBlock(listOf(loop), unitValue)
        }
        is SurfaceBreak -> CoreValueBlock(listOf(CoreBreak(statement.label)), unitValue)
        is SurfaceContinue -> CoreValueBlock(listOf(CoreContinue(statement.label)), unitValue)
        is SurfaceYield -> lowerValue(statement.value).let { it.copy(statements = it.statements + CoreYield(statement.label, it.value), value = unitValue) }
        is SurfaceReturn -> statement.value?.let(::lowerValue)
            ?.let { it.copy(statements = it.statements + CoreReturn(it.value), value = unitValue) }
            ?: CoreValueBlock(listOf(CoreReturn(null)), unitValue)
    }
    
    private fun lowerUnary(statement: SurfaceUnary): CoreValueBlock {
        val right = lowerValue(statement.right)
        return lowerToTemp(
            CoreMethodCall(
                target = right.value,
                name = statement.operator.operator.methodName(),
                generics = null,
                arguments = emptyList(),
            ),
            right.statements,
        )
    }
    
    private fun lowerBinary(statement: SurfaceBinary): CoreValueBlock {
        return when (statement.operator.operator) {
            BinaryOperatorEnum.AndAnd -> lowerShortCircuit(CoreShortCircuitOperator.And, statement.left, statement.right)
            BinaryOperatorEnum.OrOr -> lowerShortCircuit(CoreShortCircuitOperator.Or, statement.left, statement.right)
            else -> {
                val left = lowerValue(statement.left)
                val right = lowerValue(statement.right)
                lowerToTemp(
                    CoreMethodCall(
                        target = left.value,
                        name = statement.operator.operator.methodName(),
                        generics = null,
                        arguments = listOf(null to right.value),
                    ),
                    left.statements + right.statements,
                )
            }
        }
    }
    
    private fun lowerShortCircuit(
        operator: CoreShortCircuitOperator,
        left: SurfaceStatement,
        right: SurfaceStatement,
    ): CoreValueBlock {
        return lowerToTemp(CoreShortCircuit(operator, lowerValue(left), lowerValue(right)))
    }
    
    private fun lowerFieldGetter(statement: SurfaceFieldAccess): CoreValueBlock {
        val target = lowerValue(statement.target)
        return lowerToTemp(
            CoreMethodCall(
                target = target.value,
                name = statement.name,
                generics = null,
                arguments = emptyList(),
            ),
            target.statements,
        )
    }
    
    private fun lowerIndexGetter(statement: SurfaceIndexAccess): CoreValueBlock {
        val target = lowerValue(statement.target)
        val indices = lowerArguments(statement.indices.map { null to it })
        return lowerToTemp(
            CoreMethodCall(
                target = target.value,
                name = "get",
                generics = null,
                arguments = indices.values,
            ),
            target.statements + indices.statements,
        )
    }
    
    private fun lowerAssign(statement: SurfaceAssign): CoreValueBlock = when (statement.operator.operator) {
        AssignOperatorEnum.Plain -> lowerPlainAssign(statement.left, statement.right)
        AssignOperatorEnum.Def -> lowerDefinition(statement.left, statement.right)
        else -> lowerCompoundAssign(statement.left, statement.operator.operator, statement.right)
    }
    
    private fun lowerDefinition(left: SurfaceStatement, right: SurfaceStatement): CoreValueBlock {
        val name = (left as SurfaceUnresolvedSymbol).name
        val value = lowerValue(right)
        return CoreValueBlock(value.statements + CoreDefineSymbol(name, value.value), unitValue)
    }
    
    private fun lowerPlainAssign(left: SurfaceStatement, right: SurfaceStatement): CoreValueBlock = when (left) {
        is SurfaceUnresolvedSymbol -> {
            val value = lowerValue(right)
            CoreValueBlock(value.statements + CoreAssignSymbol(left.name, value.value), unitValue)
        }
        is SurfaceFieldAccess -> lowerFieldSetter(left, right)
        is SurfaceIndexAccess -> lowerIndexSetter(left, right)
        else -> error("Unsupported assignment target after statement structure check: $left")
    }
    
    private fun lowerCompoundAssign(
        left: SurfaceStatement,
        operator: AssignOperatorEnum,
        right: SurfaceStatement,
    ): CoreValueBlock = when (left) {
        is SurfaceUnresolvedSymbol -> {
            val current = CoreValueBlock(emptyList(), CoreSymbol(left.name))
            val newValue = lowerCompoundValue(current, operator, right)
            CoreValueBlock(newValue.statements + CoreAssignSymbol(left.name, newValue.value), unitValue)
        }
        is SurfaceFieldAccess -> {
            val target = lowerValue(left.target)
            val current = lowerToTemp(
                CoreMethodCall(target.value, left.name, null, emptyList()),
                target.statements,
            )
            val newValue = lowerCompoundValue(current, operator, right)
            lowerToTemp(
                CoreMethodCall(
                    target = target.value,
                    name = left.name,
                    generics = null,
                    arguments = listOf(null to newValue.value),
                ),
                newValue.statements,
            )
        }
        is SurfaceIndexAccess -> {
            val target = lowerValue(left.target)
            val indices = lowerArguments(left.indices.map { null to it })
            val current = lowerToTemp(
                CoreMethodCall(target.value, "get", null, indices.values),
                target.statements + indices.statements,
            )
            val newValue = lowerCompoundValue(current, operator, right)
            lowerToTemp(
                CoreMethodCall(
                    target = target.value,
                    name = "set",
                    generics = null,
                    arguments = indices.values + (null to newValue.value),
                ),
                newValue.statements,
            )
        }
        else -> error("Unsupported assignment target after statement structure check: $left")
    }
    
    private fun lowerCompoundValue(
        current: CoreValueBlock,
        operator: AssignOperatorEnum,
        right: SurfaceStatement,
    ): CoreValueBlock = when (operator) {
        AssignOperatorEnum.AndAnd -> {
            lowerToTemp(
                CoreShortCircuit(CoreShortCircuitOperator.And, CoreValueBlock(emptyList(), current.value), lowerValue(right)),
                current.statements,
            )
        }
        AssignOperatorEnum.OrOr -> {
            lowerToTemp(
                CoreShortCircuit(CoreShortCircuitOperator.Or, CoreValueBlock(emptyList(), current.value), lowerValue(right)),
                current.statements,
            )
        }
        else -> {
            val rightValue = lowerValue(right)
            lowerToTemp(
                CoreMethodCall(
                    target = current.value,
                    name = operator.methodName(),
                    generics = null,
                    arguments = listOf(null to rightValue.value),
                ),
                current.statements + rightValue.statements,
            )
        }
    }
    
    private fun lowerFieldSetter(left: SurfaceFieldAccess, right: SurfaceStatement): CoreValueBlock {
        val target = lowerValue(left.target)
        val value = lowerValue(right)
        return lowerToTemp(
            CoreMethodCall(
                target = target.value,
                name = left.name,
                generics = null,
                arguments = listOf(null to value.value),
            ),
            target.statements + value.statements,
        )
    }
    
    private fun lowerIndexSetter(left: SurfaceIndexAccess, right: SurfaceStatement): CoreValueBlock {
        val target = lowerValue(left.target)
        val indices = lowerArguments(left.indices.map { null to it })
        val value = lowerValue(right)
        return lowerToTemp(
            CoreMethodCall(
                target = target.value,
                name = "set",
                generics = null,
                arguments = indices.values + (null to value.value),
            ),
            target.statements + indices.statements + value.statements,
        )
    }
    
    private fun lowerFormattedString(statement: SurfaceFormattedString): CoreValueBlock {
        val statements = mutableListOf<CoreStatement>()
        val parts = statement.parts.map { part ->
            when (part) {
                is FoxFormattedText -> CoreFormattedText(part.text)
                is FoxFormattedExpression -> {
                    val value = lowerValue(part.expression)
                    statements += value.statements
                    CoreFormattedExpression(value.value)
                }
            }
        }
        return lowerToTemp(CoreFormattedString(parts), statements)
    }
    
    private fun lowerConstruct(statement: SurfaceConstruct): CoreValueBlock {
        collectObjectAccessors(statement.type)
        val arguments = lowerArguments(statement.parameters)
        return lowerToTemp(CoreConstruct(statement.type, arguments.values), arguments.statements)
    }
    
    private fun lowerCall(statement: SurfaceCall): CoreValueBlock {
        statement.generics?.forEach { collectObjectAccessors(it.second) }
        val target = lowerValue(statement.target)
        val arguments = lowerArguments(statement.parameters)
        return lowerToTemp(
            CoreMethodCall(
                target = target.value,
                name = statement.name,
                generics = statement.generics,
                arguments = arguments.values,
            ),
            target.statements + arguments.statements,
        )
    }
    
    private fun lowerIndirectCall(statement: SurfaceIndirectCall): CoreValueBlock {
        val target = lowerValue(statement.target)
        val method = lowerValue(statement.method)
        val arguments = lowerArguments(statement.parameters)
        return lowerToTemp(
            CoreIndirectCall(
                target = target.value,
                method = method.value,
                arguments = arguments.values,
            ),
            target.statements + method.statements + arguments.statements,
        )
    }
    
    private fun lowerLambda(statement: SurfaceLambda): CoreValueBlock {
        statement.parameters?.forEach { it.second?.let(collectObjectAccessors) }
        return lowerToTemp(CoreLambda(statement.parameters, lowerStatementBlock(statement.body)))
    }
    
    private fun lowerIfStatement(statement: SurfaceIf): CoreIfStatement {
        return CoreIfStatement(
            label = statement.label,
            condition = lowerValue(statement.condition),
            thenBranch = lowerStatementBlock(statement.thenBody),
            elseBranch = statement.elseBody?.let(::lowerStatementBlock),
        )
    }
    
    private fun lowerIfExpression(statement: SurfaceIf): CoreValueBlock {
        val elseBranch = statement.elseBody?.let(::lowerValue) ?: CoreValueBlock(emptyList(), unitValue)
        return lowerToTemp(
            CoreIfExpression(
                label = statement.label,
                condition = lowerValue(statement.condition),
                thenBranch = lowerValue(statement.thenBody),
                elseBranch = elseBranch,
            ),
        )
    }
    
    private fun lowerWhenStatement(statement: SurfaceWhen): CoreWhenStatement {
        return CoreWhenStatement(
            label = statement.label,
            value = statement.value?.let(::lowerValue),
            cases = statement.cases.map { case ->
                CoreWhenStatementCase(
                    conditions = case.conditions?.map(::lowerValue),
                    body = lowerStatementBlock(case.body),
                )
            },
        )
    }
    
    private fun lowerWhenExpression(statement: SurfaceWhen): CoreValueBlock {
        return lowerToTemp(
            CoreWhenExpression(
                label = statement.label,
                value = statement.value?.let(::lowerValue),
                cases = statement.cases.map { case ->
                    CoreWhenValueCase(
                        conditions = case.conditions?.map(::lowerValue),
                        body = lowerValue(case.body),
                    )
                },
            ),
        )
    }
    
    private fun lowerArguments(arguments: List<Pair<String?, SurfaceStatement>>): CoreArguments {
        val statements = mutableListOf<CoreStatement>()
        val values = arguments.map { (name, argument) ->
            val value = lowerValue(argument)
            statements += value.statements
            name to value.value
        }
        return CoreArguments(statements, values)
    }
    
    private fun lowerToTemp(
        expression: CoreExpression,
        statements: List<CoreStatement> = emptyList(),
    ): CoreValueBlock {
        val temp = newTemp()
        return CoreValueBlock(statements + CoreLet(temp, expression), temp)
    }
    
    private fun newTemp() = CoreTemp(nextTempId++)
    
    private val unitValue get() = CoreConst(FoxUnit)
}

private data class CoreArguments(
    val statements: List<CoreStatement>,
    val values: List<Pair<String?, CoreValue>>,
)

private fun UnaryOperatorEnum.methodName() = when (this) {
    UnaryOperatorEnum.Not -> "not"
    UnaryOperatorEnum.Neg -> "neg"
}

private fun BinaryOperatorEnum.methodName() = when (this) {
    BinaryOperatorEnum.Add -> "add"
    BinaryOperatorEnum.Sub -> "sub"
    BinaryOperatorEnum.Mul -> "mul"
    BinaryOperatorEnum.Div -> "div"
    BinaryOperatorEnum.Rem -> "rem"
    BinaryOperatorEnum.And -> "and"
    BinaryOperatorEnum.Or -> "or"
    BinaryOperatorEnum.Xor -> "xor"
    BinaryOperatorEnum.Shl -> "shl"
    BinaryOperatorEnum.Shr -> "shr"
    BinaryOperatorEnum.Ushr -> "ushr"
    BinaryOperatorEnum.Eq -> "eq"
    BinaryOperatorEnum.Neq -> "neq"
    BinaryOperatorEnum.Lt -> "lt"
    BinaryOperatorEnum.Gt -> "gt"
    BinaryOperatorEnum.Leq -> "lte"
    BinaryOperatorEnum.Geq -> "gte"
    BinaryOperatorEnum.AndAnd,
    BinaryOperatorEnum.OrOr,
        -> error("Short-circuit operators do not lower to method names")
}

private fun AssignOperatorEnum.methodName() = when (this) {
    AssignOperatorEnum.Add -> "add"
    AssignOperatorEnum.Sub -> "sub"
    AssignOperatorEnum.Mul -> "mul"
    AssignOperatorEnum.Div -> "div"
    AssignOperatorEnum.Rem -> "rem"
    AssignOperatorEnum.And -> "and"
    AssignOperatorEnum.Or -> "or"
    AssignOperatorEnum.Xor -> "xor"
    AssignOperatorEnum.Shl -> "shl"
    AssignOperatorEnum.Shr -> "shr"
    AssignOperatorEnum.Ushr -> "ushr"
    AssignOperatorEnum.Plain,
    AssignOperatorEnum.Def,
    AssignOperatorEnum.AndAnd,
    AssignOperatorEnum.OrOr,
        -> error("Assignment operator $this does not lower to a method name")
}
