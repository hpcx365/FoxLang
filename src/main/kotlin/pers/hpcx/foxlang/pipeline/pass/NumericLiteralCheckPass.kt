package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*

sealed interface NumericLiteralCheckResult
data object NumericLiteralCheckSuccess : NumericLiteralCheckResult
data class NumericLiteralCheckFailure(val errors: List<NumericLiteralCheckError>) : NumericLiteralCheckResult

sealed interface NumericLiteralCheckError
data class NumericIntLiteralOutOfRange(val literal: SyntaxInt) : NumericLiteralCheckError
data class NumericLongLiteralOutOfRange(val literal: SyntaxLong) : NumericLiteralCheckError
data class NumericFloatLiteralOutOfRange(val literal: SyntaxFloat) : NumericLiteralCheckError
data class NumericDoubleLiteralOutOfRange(val literal: SyntaxDouble) : NumericLiteralCheckError

fun runNumericLiteralCheck(file: SyntaxFile) = NumericLiteralCheckContext().run(file)

private class NumericLiteralCheckContext {
    
    private val errors = mutableListOf<NumericLiteralCheckError>()
    
    fun run(file: SyntaxFile): NumericLiteralCheckResult {
        file.elements.forEach { element ->
            when (element) {
                is SyntaxTypeAlias -> visitType(element.alias)
                is SyntaxMethodDefinition -> {
                    element.generics?.node?.forEach { it.node.second?.let(::visitType) }
                    element.thisType?.let(::visitType)
                    element.parameters.node.forEach { visitType(it.node.second) }
                    element.returnType?.let(::visitType)
                    visitStatement(element.body)
                }
            }
        }
        
        if (errors.isNotEmpty()) return NumericLiteralCheckFailure(errors)
        return NumericLiteralCheckSuccess
    }
    
    private fun run(literal: SyntaxInt) {
        val value = literal.text.toIntOrNull(literal.radix)
        if (value == null) errors += NumericIntLiteralOutOfRange(literal)
    }
    
    private fun run(literal: SyntaxLong) {
        val value = literal.text.toLongOrNull(literal.radix)
        if (value == null) errors += NumericLongLiteralOutOfRange(literal)
    }
    
    private fun run(literal: SyntaxFloat) {
        val value = literal.text.toFloatOrNull()
        if (value == null) errors += NumericFloatLiteralOutOfRange(literal)
    }
    
    private fun run(literal: SyntaxDouble) {
        val value = literal.text.toDoubleOrNull()
        if (value == null) errors += NumericDoubleLiteralOutOfRange(literal)
    }
    
    private fun visitType(type: SyntaxType<*>) {
        when (type) {
            is SyntaxPrimitiveType -> {}
            is SyntaxTupleType -> type.components.node.forEach(::visitType)
            is SyntaxStructType -> type.fields.node.forEach { visitType(it.node.second) }
            is SyntaxObjectType -> type.members.node.forEach { visitType(it.node.second) }
            is SyntaxEnumType -> type.entries.node.forEach { visitType(it.node.second) }
            is SyntaxArrayType -> visitType(type.element)
            is SyntaxRefType -> visitType(type.referent)
            is SyntaxMethodType -> {
                type.`this`?.node?.second?.let(::visitType)
                type.parameters?.node?.forEach { visitType(it.node.second) }
                type.`return`?.node?.second?.let(::visitType)
            }
            is SyntaxAnyType -> {}
            is SyntaxAnyOfType -> type.types.forEach(::visitType)
            is SyntaxAllOfType -> type.types.forEach(::visitType)
            is SyntaxNoneOfType -> type.types.forEach(::visitType)
            is SyntaxAnyTupleType -> {}
            is SyntaxAnyTupleOfType -> visitType(type.component)
            is SyntaxAnyStructType -> {}
            is SyntaxAnyStructOfType -> type.fields.node.forEach(::visitType)
            is SyntaxAnyObjectType -> {}
            is SyntaxAnyEnumType -> {}
            is SyntaxTupleGetComponentType -> {
                visitType(type.type)
                run(type.index)
            }
            is SyntaxTupleGetComponentBackType -> {
                visitType(type.type)
                run(type.index)
            }
            is SyntaxTupleGetFirstComponentsType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxTupleGetFirstComponentsExactType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxTupleGetLastComponentsType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxTupleGetLastComponentsExactType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxTupleDropFirstComponentsType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxTupleDropFirstComponentsExactType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxTupleDropLastComponentsType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxTupleDropLastComponentsExactType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxTupleMergeTuplesType -> type.types.node.forEach(::visitType)
            is SyntaxStructGetFieldTypeByNameType -> visitType(type.type)
            is SyntaxStructGetFieldTypeByIndexType -> {
                visitType(type.type)
                run(type.index)
            }
            is SyntaxStructGetFieldTypeByIndexBackType -> {
                visitType(type.type)
                run(type.index)
            }
            is SyntaxStructGetFirstFieldsType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxStructGetFirstFieldsExactType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxStructGetLastFieldsType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxStructGetLastFieldsExactType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxStructDropFirstFieldsType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxStructDropFirstFieldsExactType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxStructDropLastFieldsType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxStructDropLastFieldsExactType -> {
                visitType(type.type)
                run(type.count)
            }
            is SyntaxStructSelectFieldsType -> visitType(type.type)
            is SyntaxStructSelectFieldsExactType -> visitType(type.type)
            is SyntaxStructDropFieldsType -> visitType(type.type)
            is SyntaxStructDropFieldsExactType -> visitType(type.type)
            is SyntaxStructExtractFieldTypesType -> visitType(type.type)
            is SyntaxStructMergeStructsType -> type.types.node.forEach(::visitType)
            is SyntaxObjectGetMemberTypeType -> visitType(type.type)
            is SyntaxObjectSelectMembersType -> visitType(type.type)
            is SyntaxObjectSelectMembersExactType -> visitType(type.type)
            is SyntaxObjectDropMembersType -> visitType(type.type)
            is SyntaxObjectDropMembersExactType -> visitType(type.type)
            is SyntaxObjectMergeObjectsType -> type.types.node.forEach(::visitType)
            is SyntaxEnumGetEntryTypeType -> visitType(type.type)
            is SyntaxEnumSelectEntriesType -> visitType(type.type)
            is SyntaxEnumSelectEntriesExactType -> visitType(type.type)
            is SyntaxEnumDropEntriesType -> visitType(type.type)
            is SyntaxEnumDropEntriesExactType -> visitType(type.type)
            is SyntaxEnumMergeEnumsType -> type.types.node.forEach(::visitType)
            is SyntaxArrayGetElementTypeType -> visitType(type.type)
            is SyntaxRefGetReferentTypeType -> visitType(type.type)
            is SyntaxMethodGetThisTypeType -> visitType(type.type)
            is SyntaxMethodGetParameterStructType -> visitType(type.type)
            is SyntaxMethodGetReturnTypeType -> visitType(type.type)
            is SyntaxMethodOfType -> {
                visitType(type.`this`)
                visitType(type.parameters)
                visitType(type.`return`)
            }
            is SyntaxUnresolvedType -> type.parameters?.node?.forEach(::visitType)
        }
    }
    
    private fun visitStatement(statement: SyntaxStatement<*>) {
        when (statement) {
            is SyntaxThis -> {}
            is SyntaxUnresolvedSymbol -> {}
            is SyntaxEntityStatement -> {}
            is SyntaxIntStatement -> run(statement.value)
            is SyntaxLongStatement -> run(statement.value)
            is SyntaxFloatStatement -> run(statement.value)
            is SyntaxDoubleStatement -> run(statement.value)
            is SyntaxBreak -> {}
            is SyntaxContinue -> {}
            is SyntaxYield -> visitStatement(statement.value)
            is SyntaxReturn -> statement.value?.let(::visitStatement)
            is SyntaxUnary -> visitStatement(statement.right)
            is SyntaxBinary -> {
                visitStatement(statement.left)
                visitStatement(statement.right)
            }
            is SyntaxTypeBinding -> visitType(statement.type)
            is SyntaxAssign -> {
                visitStatement(statement.left)
                visitStatement(statement.right)
            }
            is SyntaxFieldAccess -> visitStatement(statement.target)
            is SyntaxIndexAccess -> {
                visitStatement(statement.target)
                statement.indices.node.forEach(::visitStatement)
            }
            is SyntaxFormattedString -> statement.parts?.node?.forEach { part ->
                when (part) {
                    is SyntaxFormattedText -> {}
                    is SyntaxFormattedExpression -> visitStatement(part.expression)
                }
            }
            is SyntaxConstruct -> {
                visitType(statement.type)
                statement.parameters.node.forEach { visitStatement(it.node.second) }
            }
            is SyntaxCall -> {
                statement.target?.let(::visitStatement)
                statement.generics?.node?.forEach { visitType(it.node.second) }
                statement.parameters.node.forEach { visitStatement(it.node.second) }
            }
            is SyntaxIndirectCall -> {
                statement.target?.let(::visitStatement)
                visitStatement(statement.method)
                statement.parameters.node.forEach { visitStatement(it.node.second) }
            }
            is SyntaxBlock -> statement.statements.node.forEach(::visitStatement)
            is SyntaxIf -> {
                visitStatement(statement.condition)
                visitStatement(statement.thenBody)
                statement.elseBody?.let(::visitStatement)
            }
            is SyntaxWhen -> {
                statement.value?.let(::visitStatement)
                statement.cases.node.forEach { case ->
                    case.conditions?.node?.forEach(::visitStatement)
                    visitStatement(case.body)
                }
            }
            is SyntaxWhile -> {
                visitStatement(statement.condition)
                visitStatement(statement.body)
            }
            is SyntaxDoWhile -> {
                visitStatement(statement.body)
                visitStatement(statement.condition)
            }
            is SyntaxLambda -> {
                statement.parameters?.node?.forEach { it.node.second?.let(::visitType) }
                visitStatement(statement.body)
            }
        }
    }
}
