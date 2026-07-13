package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ast.*

sealed interface DuplicateItemCheckResult
data object DuplicateItemCheckSuccess : DuplicateItemCheckResult
data class DuplicateItemCheckFailure(val errors: List<DuplicateItemCheckError>) : DuplicateItemCheckResult

sealed interface DuplicateItemCheckError
data class DuplicateTypeAliasGeneric(val typeAlias: ParsedFoxTypeAlias, val generic: ParsedString) : DuplicateItemCheckError
data class DuplicateMethodGeneric(val method: ParsedFoxMethodDefinition, val generic: ParsedString) : DuplicateItemCheckError
data class DuplicateMethodParameter(val method: ParsedFoxMethodDefinition, val parameter: ParsedString) : DuplicateItemCheckError
data class DuplicateLambdaParameter(val lambda: ParsedFoxLambda, val parameter: ParsedString) : DuplicateItemCheckError
data class DuplicateStructField(val type: ParsedFoxStructType, val field: ParsedString) : DuplicateItemCheckError
data class DuplicateObjectMember(val type: ParsedFoxObjectType, val member: ParsedString) : DuplicateItemCheckError
data class DuplicateEnumEntry(val type: ParsedFoxEnumType, val entry: ParsedString) : DuplicateItemCheckError
data class DuplicateMethodTypeParameter(val type: ParsedFoxMethodType, val parameter: ParsedString) : DuplicateItemCheckError
data class DuplicateStructFieldSelection(val type: ParsedFoxStructSelectFieldsType, val field: ParsedString) : DuplicateItemCheckError
data class DuplicateStructFieldExactSelection(val type: ParsedFoxStructSelectFieldsExactType, val field: ParsedString) : DuplicateItemCheckError
data class DuplicateStructFieldDrop(val type: ParsedFoxStructDropFieldsType, val field: ParsedString) : DuplicateItemCheckError
data class DuplicateStructFieldExactDrop(val type: ParsedFoxStructDropFieldsExactType, val field: ParsedString) : DuplicateItemCheckError
data class DuplicateObjectMemberSelection(val type: ParsedFoxObjectSelectMembersType, val member: ParsedString) : DuplicateItemCheckError
data class DuplicateObjectMemberExactSelection(val type: ParsedFoxObjectSelectMembersExactType, val member: ParsedString) : DuplicateItemCheckError
data class DuplicateObjectMemberDrop(val type: ParsedFoxObjectDropMembersType, val member: ParsedString) : DuplicateItemCheckError
data class DuplicateObjectMemberExactDrop(val type: ParsedFoxObjectDropMembersExactType, val member: ParsedString) : DuplicateItemCheckError
data class DuplicateEnumEntrySelection(val type: ParsedFoxEnumSelectEntriesType, val entry: ParsedString) : DuplicateItemCheckError
data class DuplicateEnumEntryExactSelection(val type: ParsedFoxEnumSelectEntriesExactType, val entry: ParsedString) : DuplicateItemCheckError
data class DuplicateEnumEntryDrop(val type: ParsedFoxEnumDropEntriesType, val entry: ParsedString) : DuplicateItemCheckError
data class DuplicateEnumEntryExactDrop(val type: ParsedFoxEnumDropEntriesExactType, val entry: ParsedString) : DuplicateItemCheckError
data class DuplicateConstructParameter(val construct: ParsedFoxConstruct, val parameter: ParsedString) : DuplicateItemCheckError
data class DuplicateCallGenericArgument(val call: ParsedFoxCall, val argument: ParsedString) : DuplicateItemCheckError
data class DuplicateCallParameter(val call: ParsedFoxCall, val parameter: ParsedString) : DuplicateItemCheckError
data class DuplicateIndirectCallParameter(val call: ParsedFoxIndirectCall, val parameter: ParsedString) : DuplicateItemCheckError

fun runDuplicateItemCheck(file: ParsedFoxFile) = DuplicateItemCheckContext().run(file)

private class DuplicateItemCheckContext {
    
    private val errors = mutableListOf<DuplicateItemCheckError>()
    
    fun run(file: ParsedFoxFile): DuplicateItemCheckResult {
        file.elements.forEach { element ->
            when (element) {
                is ParsedFoxTypeAlias -> {
                    checkDuplicates(element.generics?.node.orEmpty()) { DuplicateTypeAliasGeneric(element, it) }
                    visitType(element.alias)
                }
                is ParsedFoxMethodDefinition -> {
                    checkDuplicates(element.generics?.node?.map { it.node.first }.orEmpty()) {
                        DuplicateMethodGeneric(element, it)
                    }
                    checkDuplicates(element.parameters.node.map { it.node.first }) {
                        DuplicateMethodParameter(element, it)
                    }
                    element.generics?.node?.forEach { it.node.second?.let(::visitType) }
                    element.thisType?.let(::visitType)
                    element.parameters.node.forEach { visitType(it.node.second) }
                    element.returnType?.let(::visitType)
                    visitStatement(element.body)
                }
            }
        }
        
        if (errors.isNotEmpty()) return DuplicateItemCheckFailure(errors)
        return DuplicateItemCheckSuccess
    }
    
    private fun checkDuplicates(items: Iterable<ParsedString>, error: (ParsedString) -> DuplicateItemCheckError) {
        val seen = mutableSetOf<String>()
        items.forEach { item ->
            if (!seen.add(item.node)) errors += error(item)
        }
    }
    
    private fun checkNamedDuplicates(
        items: Iterable<ParsedPair<ParsedString?, *>>,
        error: (ParsedString) -> DuplicateItemCheckError,
    ) {
        checkDuplicates(items.mapNotNull { it.node.first }, error)
    }
    
    private fun visitType(type: ParsedFoxType<*>) {
        when (type) {
            is ParsedFoxPrimitiveType -> {}
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
            is ParsedFoxTupleType -> type.components.node.forEach(::visitType)
            is ParsedFoxStructType -> {
                checkDuplicates(type.fields.node.map { it.node.first }) { DuplicateStructField(type, it) }
                type.fields.node.forEach { visitType(it.node.second) }
            }
            is ParsedFoxObjectType -> {
                checkDuplicates(type.members.node.map { it.node.first }) { DuplicateObjectMember(type, it) }
                type.members.node.forEach { visitType(it.node.second) }
            }
            is ParsedFoxEnumType -> {
                checkDuplicates(type.entries.node.map { it.node.first }) { DuplicateEnumEntry(type, it) }
                type.entries.node.forEach { visitType(it.node.second) }
            }
            is ParsedFoxArrayType -> visitType(type.element)
            is ParsedFoxRefType -> visitType(type.referent)
            is ParsedFoxMethodType -> {
                checkDuplicates(type.parameters?.node?.map { it.node.first }.orEmpty()) {
                    DuplicateMethodTypeParameter(type, it)
                }
                type.`this`?.node?.second?.let(::visitType)
                type.parameters?.node?.forEach { visitType(it.node.second) }
                type.`return`?.node?.second?.let(::visitType)
            }
            is ParsedFoxTupleGetComponentType -> visitType(type.type)
            is ParsedFoxTupleGetComponentBackType -> visitType(type.type)
            is ParsedFoxTupleGetFirstComponentsType -> visitType(type.type)
            is ParsedFoxTupleGetFirstComponentsExactType -> visitType(type.type)
            is ParsedFoxTupleGetLastComponentsType -> visitType(type.type)
            is ParsedFoxTupleGetLastComponentsExactType -> visitType(type.type)
            is ParsedFoxTupleDropFirstComponentsType -> visitType(type.type)
            is ParsedFoxTupleDropFirstComponentsExactType -> visitType(type.type)
            is ParsedFoxTupleDropLastComponentsType -> visitType(type.type)
            is ParsedFoxTupleDropLastComponentsExactType -> visitType(type.type)
            is ParsedFoxTupleMergeTuplesType -> type.types.node.forEach(::visitType)
            is ParsedFoxStructGetFieldTypeByNameType -> visitType(type.type)
            is ParsedFoxStructGetFieldTypeByIndexType -> visitType(type.type)
            is ParsedFoxStructGetFieldTypeByIndexBackType -> visitType(type.type)
            is ParsedFoxStructGetFirstFieldsType -> visitType(type.type)
            is ParsedFoxStructGetFirstFieldsExactType -> visitType(type.type)
            is ParsedFoxStructGetLastFieldsType -> visitType(type.type)
            is ParsedFoxStructGetLastFieldsExactType -> visitType(type.type)
            is ParsedFoxStructDropFirstFieldsType -> visitType(type.type)
            is ParsedFoxStructDropFirstFieldsExactType -> visitType(type.type)
            is ParsedFoxStructDropLastFieldsType -> visitType(type.type)
            is ParsedFoxStructDropLastFieldsExactType -> visitType(type.type)
            is ParsedFoxStructExtractFieldTypesType -> visitType(type.type)
            is ParsedFoxStructSelectFieldsType -> {
                checkDuplicates(type.names.node) { DuplicateStructFieldSelection(type, it) }
                visitType(type.type)
            }
            is ParsedFoxStructSelectFieldsExactType -> {
                checkDuplicates(type.names.node) { DuplicateStructFieldExactSelection(type, it) }
                visitType(type.type)
            }
            is ParsedFoxStructDropFieldsType -> {
                checkDuplicates(type.names.node) { DuplicateStructFieldDrop(type, it) }
                visitType(type.type)
            }
            is ParsedFoxStructDropFieldsExactType -> {
                checkDuplicates(type.names.node) { DuplicateStructFieldExactDrop(type, it) }
                visitType(type.type)
            }
            is ParsedFoxStructMergeStructsType -> type.types.node.forEach(::visitType)
            is ParsedFoxObjectGetMemberTypeType -> visitType(type.type)
            is ParsedFoxObjectSelectMembersType -> {
                checkDuplicates(type.names.node) { DuplicateObjectMemberSelection(type, it) }
                visitType(type.type)
            }
            is ParsedFoxObjectSelectMembersExactType -> {
                checkDuplicates(type.names.node) { DuplicateObjectMemberExactSelection(type, it) }
                visitType(type.type)
            }
            is ParsedFoxObjectDropMembersType -> {
                checkDuplicates(type.names.node) { DuplicateObjectMemberDrop(type, it) }
                visitType(type.type)
            }
            is ParsedFoxObjectDropMembersExactType -> {
                checkDuplicates(type.names.node) { DuplicateObjectMemberExactDrop(type, it) }
                visitType(type.type)
            }
            is ParsedFoxObjectMergeObjectsType -> type.types.node.forEach(::visitType)
            is ParsedFoxEnumGetEntryTypeType -> visitType(type.type)
            is ParsedFoxEnumSelectEntriesType -> {
                checkDuplicates(type.names.node) { DuplicateEnumEntrySelection(type, it) }
                visitType(type.type)
            }
            is ParsedFoxEnumSelectEntriesExactType -> {
                checkDuplicates(type.names.node) { DuplicateEnumEntryExactSelection(type, it) }
                visitType(type.type)
            }
            is ParsedFoxEnumDropEntriesType -> {
                checkDuplicates(type.names.node) { DuplicateEnumEntryDrop(type, it) }
                visitType(type.type)
            }
            is ParsedFoxEnumDropEntriesExactType -> {
                checkDuplicates(type.names.node) { DuplicateEnumEntryExactDrop(type, it) }
                visitType(type.type)
            }
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
    
    private fun visitStatement(statement: ParsedFoxStatement<*>) {
        when (statement) {
            is ParsedFoxThis -> {}
            is ParsedFoxUnresolvedSymbol -> {}
            is ParsedFoxEntityStatement -> {}
            is ParsedFoxIntStatement -> {}
            is ParsedFoxLongStatement -> {}
            is ParsedFoxFloatStatement -> {}
            is ParsedFoxDoubleStatement -> {}
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
                checkNamedDuplicates(statement.parameters.node) { DuplicateConstructParameter(statement, it) }
                visitType(statement.type)
                statement.parameters.node.forEach { visitStatement(it.node.second) }
            }
            is ParsedFoxCall -> {
                checkNamedDuplicates(statement.generics?.node.orEmpty()) { DuplicateCallGenericArgument(statement, it) }
                checkNamedDuplicates(statement.parameters.node) { DuplicateCallParameter(statement, it) }
                statement.target?.let(::visitStatement)
                statement.generics?.node?.forEach { visitType(it.node.second) }
                statement.parameters.node.forEach { visitStatement(it.node.second) }
            }
            is ParsedFoxIndirectCall -> {
                checkNamedDuplicates(statement.parameters.node) { DuplicateIndirectCallParameter(statement, it) }
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
                checkDuplicates(statement.parameters?.node?.map { it.node.first }.orEmpty()) {
                    DuplicateLambdaParameter(statement, it)
                }
                statement.parameters?.node?.forEach { it.node.second?.let(::visitType) }
                visitStatement(statement.body)
            }
        }
    }
}
