package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ast.*

sealed interface NumericLiteralCheckResult
data object NumericLiteralCheckSuccess : NumericLiteralCheckResult
data class NumericLiteralCheckFailure(val errors: List<NumericLiteralCheckError>) : NumericLiteralCheckResult

sealed interface NumericLiteralCheckError
data class NumericIntLiteralOutOfRange(val literal: ParsedInt) : NumericLiteralCheckError
data class NumericLongLiteralOutOfRange(val literal: ParsedLong) : NumericLiteralCheckError
data class NumericFloatLiteralOutOfRange(val literal: ParsedFloat) : NumericLiteralCheckError
data class NumericDoubleLiteralOutOfRange(val literal: ParsedDouble) : NumericLiteralCheckError

fun runNumericLiteralCheck(file: ParsedFoxFile): NumericLiteralCheckResult {
    val errors = mutableListOf<NumericLiteralCheckError>()
    
    fun check(literal: ParsedInt) {
        val value = literal.text.toIntOrNull(literal.radix)
        if (value == null) errors += NumericIntLiteralOutOfRange(literal)
    }
    
    fun check(literal: ParsedLong) {
        val value = literal.text.toLongOrNull(literal.radix)
        if (value == null) errors += NumericLongLiteralOutOfRange(literal)
    }
    
    fun check(literal: ParsedFloat) {
        val value = literal.text.toFloatOrNull()
        if (value == null) errors += NumericFloatLiteralOutOfRange(literal)
    }
    
    fun check(literal: ParsedDouble) {
        val value = literal.text.toDoubleOrNull()
        if (value == null) errors += NumericDoubleLiteralOutOfRange(literal)
    }
    
    fun visitType(type: ParsedFoxType<*>) {
        when (type) {
            is ParsedFoxPrimitiveType -> {}
            is ParsedFoxTupleType -> type.components.node.forEach(::visitType)
            is ParsedFoxStructType -> type.fields.node.forEach { visitType(it.node.second) }
            is ParsedFoxObjectType -> type.members.node.forEach { visitType(it.node.second) }
            is ParsedFoxEnumType -> type.entries.node.forEach { visitType(it.node.second) }
            is ParsedFoxArrayType -> visitType(type.element)
            is ParsedFoxRefType -> visitType(type.referent)
            is ParsedFoxMethodType -> {
                type.`this`?.node?.second?.let(::visitType)
                type.parameters?.node?.forEach { visitType(it.node.second) }
                type.`return`?.node?.second?.let(::visitType)
            }
            is ParsedFoxAnyType -> {}
            is ParsedFoxAnyOfType -> type.types.forEach(::visitType)
            is ParsedFoxAllOfType -> type.types.forEach(::visitType)
            is ParsedFoxNoneOfType -> type.types.forEach(::visitType)
            is ParsedFoxAnyTupleType -> {}
            is ParsedFoxAnyTupleOfType -> visitType(type.component)
            is ParsedFoxAnyStructType -> {}
            is ParsedFoxAnyStructOfType -> type.fields.node.forEach(::visitType)
            is ParsedFoxAnyObjectType -> {}
            is ParsedFoxAnyEnumType -> {}
            is ParsedFoxTupleGetComponentType -> {
                visitType(type.type)
                check(type.index)
            }
            is ParsedFoxTupleGetComponentBackType -> {
                visitType(type.type)
                check(type.index)
            }
            is ParsedFoxTupleGetFirstComponentsType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxTupleGetFirstComponentsExactType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxTupleGetLastComponentsType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxTupleGetLastComponentsExactType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxTupleDropFirstComponentsType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxTupleDropFirstComponentsExactType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxTupleDropLastComponentsType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxTupleDropLastComponentsExactType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxTupleMergeTuplesType -> type.types.node.forEach(::visitType)
            is ParsedFoxStructGetFieldTypeByNameType -> visitType(type.type)
            is ParsedFoxStructGetFieldTypeByIndexType -> {
                visitType(type.type)
                check(type.index)
            }
            is ParsedFoxStructGetFieldTypeByIndexBackType -> {
                visitType(type.type)
                check(type.index)
            }
            is ParsedFoxStructGetFirstFieldsType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxStructGetFirstFieldsExactType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxStructGetLastFieldsType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxStructGetLastFieldsExactType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxStructDropFirstFieldsType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxStructDropFirstFieldsExactType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxStructDropLastFieldsType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxStructDropLastFieldsExactType -> {
                visitType(type.type)
                check(type.count)
            }
            is ParsedFoxStructSelectFieldsType -> visitType(type.type)
            is ParsedFoxStructSelectFieldsExactType -> visitType(type.type)
            is ParsedFoxStructDropFieldsType -> visitType(type.type)
            is ParsedFoxStructDropFieldsExactType -> visitType(type.type)
            is ParsedFoxStructExtractFieldTypesType -> visitType(type.type)
            is ParsedFoxStructMergeStructsType -> type.types.node.forEach(::visitType)
            is ParsedFoxObjectGetMemberTypeType -> visitType(type.type)
            is ParsedFoxObjectSelectMembersType -> visitType(type.type)
            is ParsedFoxObjectSelectMembersExactType -> visitType(type.type)
            is ParsedFoxObjectDropMembersType -> visitType(type.type)
            is ParsedFoxObjectDropMembersExactType -> visitType(type.type)
            is ParsedFoxObjectMergeObjectsType -> type.types.node.forEach(::visitType)
            is ParsedFoxEnumGetEntryTypeType -> visitType(type.type)
            is ParsedFoxEnumSelectEntriesType -> visitType(type.type)
            is ParsedFoxEnumSelectEntriesExactType -> visitType(type.type)
            is ParsedFoxEnumDropEntriesType -> visitType(type.type)
            is ParsedFoxEnumDropEntriesExactType -> visitType(type.type)
            is ParsedFoxEnumMergeEnumsType -> type.types.node.forEach(::visitType)
            is ParsedFoxArrayGetElementTypeType -> visitType(type.type)
            is ParsedFoxRefGetReferentTypeType -> visitType(type.type)
            is ParsedFoxMethodGetThisTypeType -> visitType(type.type)
            is ParsedFoxMethodGetParameterStructType -> visitType(type.type)
            is ParsedFoxMethodGetReturnTypeType -> visitType(type.type)
            is ParsedFoxMethodOfType -> {
                visitType(type.`this`)
                visitType(type.parameters)
                visitType(type.`return`)
            }
            is ParsedFoxUnresolvedType -> type.parameters?.node?.forEach(::visitType)
        }
    }
    
    fun visitStatement(statement: ParsedFoxStatement<*>) {
        when (statement) {
            is ParsedFoxThis -> {}
            is ParsedFoxSymbol -> {}
            is ParsedFoxEntityStatement -> {}
            is ParsedFoxIntStatement -> check(statement.value)
            is ParsedFoxLongStatement -> check(statement.value)
            is ParsedFoxFloatStatement -> check(statement.value)
            is ParsedFoxDoubleStatement -> check(statement.value)
            is ParsedFoxBreak -> {}
            is ParsedFoxContinue -> {}
            is ParsedFoxYield -> visitStatement(statement.value)
            is ParsedFoxReturn -> statement.value?.let(::visitStatement)
            is ParsedFoxUnary -> visitStatement(statement.right)
            is ParsedFoxBinary -> {
                visitStatement(statement.left)
                visitStatement(statement.right)
            }
            is ParsedFoxTypeBinding -> visitType(statement.type)
            is ParsedFoxAssign -> {
                visitStatement(statement.left)
                visitStatement(statement.right)
            }
            is ParsedFoxFieldAccess -> visitStatement(statement.target)
            is ParsedFoxIndexAccess -> {
                visitStatement(statement.target)
                statement.indices.node.forEach(::visitStatement)
            }
            is ParsedFoxFormattedString -> statement.parts?.node?.forEach { part ->
                when (part) {
                    is ParsedFoxFormattedText -> {}
                    is ParsedFoxFormattedExpression -> visitStatement(part.expression)
                }
            }
            is ParsedFoxConstruct -> {
                visitType(statement.type)
                statement.parameters.node.forEach { visitStatement(it.node.second) }
            }
            is ParsedFoxCall -> {
                statement.target?.let(::visitStatement)
                statement.generics?.node?.forEach { visitType(it.node.second) }
                statement.parameters.node.forEach { visitStatement(it.node.second) }
            }
            is ParsedFoxIndirectCall -> {
                statement.target?.let(::visitStatement)
                visitStatement(statement.method)
                statement.parameters.node.forEach { visitStatement(it.node.second) }
            }
            is ParsedFoxBlock -> statement.statements.node.forEach(::visitStatement)
            is ParsedFoxIf -> {
                visitStatement(statement.condition)
                visitStatement(statement.thenBody)
                statement.elseBody?.let(::visitStatement)
            }
            is ParsedFoxWhen -> {
                statement.value?.let(::visitStatement)
                statement.cases.node.forEach { case ->
                    case.conditions?.node?.forEach(::visitStatement)
                    visitStatement(case.body)
                }
            }
            is ParsedFoxWhile -> {
                visitStatement(statement.condition)
                visitStatement(statement.body)
            }
            is ParsedFoxDoWhile -> {
                visitStatement(statement.body)
                visitStatement(statement.condition)
            }
            is ParsedFoxLambda -> {
                statement.parameters?.node?.forEach { it.node.second?.let(::visitType) }
                visitStatement(statement.body)
            }
        }
    }
    
    file.elements.forEach { element ->
        when (element) {
            is ParsedFoxTypeAlias -> visitType(element.alias)
            is ParsedFoxMethodDefinition -> {
                element.generics?.node?.forEach { it.node.second?.let(::visitType) }
                element.thisType?.let(::visitType)
                element.parameters.node.forEach { visitType(it.node.second) }
                element.returnType?.let(::visitType)
                visitStatement(element.body)
            }
        }
    }
    
    return if (errors.isEmpty()) NumericLiteralCheckSuccess else NumericLiteralCheckFailure(errors)
}
