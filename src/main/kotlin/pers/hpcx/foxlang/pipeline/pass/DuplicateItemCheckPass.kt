package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*

sealed interface DuplicateItemCheckResult
data object DuplicateItemCheckSuccess : DuplicateItemCheckResult
data class DuplicateItemCheckFailure(val errors: List<DuplicateItemCheckError>) : DuplicateItemCheckResult

sealed interface DuplicateItemCheckError
data class DuplicateTypeAliasGeneric(val typeAlias: SyntaxTypeAlias, val generic: SyntaxString) : DuplicateItemCheckError
data class DuplicateMethodGeneric(val method: SyntaxMethodDefinition, val generic: SyntaxString) : DuplicateItemCheckError
data class DuplicateMethodParameter(val method: SyntaxMethodDefinition, val parameter: SyntaxString) : DuplicateItemCheckError
data class DuplicateLambdaParameter(val lambda: SyntaxLambda, val parameter: SyntaxString) : DuplicateItemCheckError
data class DuplicateStructField(val type: SyntaxStructType, val field: SyntaxString) : DuplicateItemCheckError
data class DuplicateObjectMember(val type: SyntaxObjectType, val member: SyntaxString) : DuplicateItemCheckError
data class DuplicateEnumEntry(val type: SyntaxEnumType, val entry: SyntaxString) : DuplicateItemCheckError
data class DuplicateMethodTypeParameter(val type: SyntaxMethodType, val parameter: SyntaxString) : DuplicateItemCheckError
data class DuplicateStructFieldSelection(val type: SyntaxStructSelectFieldsType, val field: SyntaxString) : DuplicateItemCheckError
data class DuplicateStructFieldExactSelection(val type: SyntaxStructSelectFieldsExactType, val field: SyntaxString) : DuplicateItemCheckError
data class DuplicateStructFieldDrop(val type: SyntaxStructDropFieldsType, val field: SyntaxString) : DuplicateItemCheckError
data class DuplicateStructFieldExactDrop(val type: SyntaxStructDropFieldsExactType, val field: SyntaxString) : DuplicateItemCheckError
data class DuplicateObjectMemberSelection(val type: SyntaxObjectSelectMembersType, val member: SyntaxString) : DuplicateItemCheckError
data class DuplicateObjectMemberExactSelection(val type: SyntaxObjectSelectMembersExactType, val member: SyntaxString) : DuplicateItemCheckError
data class DuplicateObjectMemberDrop(val type: SyntaxObjectDropMembersType, val member: SyntaxString) : DuplicateItemCheckError
data class DuplicateObjectMemberExactDrop(val type: SyntaxObjectDropMembersExactType, val member: SyntaxString) : DuplicateItemCheckError
data class DuplicateEnumEntrySelection(val type: SyntaxEnumSelectEntriesType, val entry: SyntaxString) : DuplicateItemCheckError
data class DuplicateEnumEntryExactSelection(val type: SyntaxEnumSelectEntriesExactType, val entry: SyntaxString) : DuplicateItemCheckError
data class DuplicateEnumEntryDrop(val type: SyntaxEnumDropEntriesType, val entry: SyntaxString) : DuplicateItemCheckError
data class DuplicateEnumEntryExactDrop(val type: SyntaxEnumDropEntriesExactType, val entry: SyntaxString) : DuplicateItemCheckError
data class DuplicateConstructParameter(val construct: SyntaxConstruct, val parameter: SyntaxString) : DuplicateItemCheckError
data class DuplicateCallGenericArgument(val call: SyntaxCall, val argument: SyntaxString) : DuplicateItemCheckError
data class DuplicateCallParameter(val call: SyntaxCall, val parameter: SyntaxString) : DuplicateItemCheckError
data class DuplicateIndirectCallParameter(val call: SyntaxIndirectCall, val parameter: SyntaxString) : DuplicateItemCheckError

fun runDuplicateItemCheck(file: SyntaxFile) = DuplicateItemCheckContext().run(file)

private class DuplicateItemCheckContext {
    
    private val errors = mutableListOf<DuplicateItemCheckError>()
    
    fun run(file: SyntaxFile): DuplicateItemCheckResult {
        file.elements.forEach { element ->
            when (element) {
                is SyntaxTypeAlias -> {
                    checkDuplicates(element.generics?.node.orEmpty()) { DuplicateTypeAliasGeneric(element, it) }
                    visitType(element.alias)
                }
                is SyntaxMethodDefinition -> {
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
    
    private fun checkDuplicates(items: Iterable<SyntaxString>, error: (SyntaxString) -> DuplicateItemCheckError) {
        val seen = mutableSetOf<String>()
        items.forEach { item ->
            if (!seen.add(item.node)) errors += error(item)
        }
    }
    
    private fun checkNamedDuplicates(
        items: Iterable<SyntaxPair<SyntaxString?, *>>,
        error: (SyntaxString) -> DuplicateItemCheckError,
    ) {
        checkDuplicates(items.mapNotNull { it.node.first }, error)
    }
    
    private fun visitType(type: SyntaxType<*>) {
        when (type) {
            is SyntaxPrimitiveType -> {}
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
            is SyntaxTupleType -> type.components.node.forEach(::visitType)
            is SyntaxStructType -> {
                checkDuplicates(type.fields.node.map { it.node.first }) { DuplicateStructField(type, it) }
                type.fields.node.forEach { visitType(it.node.second) }
            }
            is SyntaxObjectType -> {
                checkDuplicates(type.members.node.map { it.node.first }) { DuplicateObjectMember(type, it) }
                type.members.node.forEach { visitType(it.node.second) }
            }
            is SyntaxEnumType -> {
                checkDuplicates(type.entries.node.map { it.node.first }) { DuplicateEnumEntry(type, it) }
                type.entries.node.forEach { visitType(it.node.second) }
            }
            is SyntaxArrayType -> visitType(type.element)
            is SyntaxRefType -> visitType(type.referent)
            is SyntaxMethodType -> {
                checkDuplicates(type.parameters?.node?.map { it.node.first }.orEmpty()) {
                    DuplicateMethodTypeParameter(type, it)
                }
                type.`this`?.node?.second?.let(::visitType)
                type.parameters?.node?.forEach { visitType(it.node.second) }
                type.`return`?.node?.second?.let(::visitType)
            }
            is SyntaxTupleGetComponentType -> visitType(type.type)
            is SyntaxTupleGetComponentBackType -> visitType(type.type)
            is SyntaxTupleGetFirstComponentsType -> visitType(type.type)
            is SyntaxTupleGetFirstComponentsExactType -> visitType(type.type)
            is SyntaxTupleGetLastComponentsType -> visitType(type.type)
            is SyntaxTupleGetLastComponentsExactType -> visitType(type.type)
            is SyntaxTupleDropFirstComponentsType -> visitType(type.type)
            is SyntaxTupleDropFirstComponentsExactType -> visitType(type.type)
            is SyntaxTupleDropLastComponentsType -> visitType(type.type)
            is SyntaxTupleDropLastComponentsExactType -> visitType(type.type)
            is SyntaxTupleMergeTuplesType -> type.types.node.forEach(::visitType)
            is SyntaxStructGetFieldTypeByNameType -> visitType(type.type)
            is SyntaxStructGetFieldTypeByIndexType -> visitType(type.type)
            is SyntaxStructGetFieldTypeByIndexBackType -> visitType(type.type)
            is SyntaxStructGetFirstFieldsType -> visitType(type.type)
            is SyntaxStructGetFirstFieldsExactType -> visitType(type.type)
            is SyntaxStructGetLastFieldsType -> visitType(type.type)
            is SyntaxStructGetLastFieldsExactType -> visitType(type.type)
            is SyntaxStructDropFirstFieldsType -> visitType(type.type)
            is SyntaxStructDropFirstFieldsExactType -> visitType(type.type)
            is SyntaxStructDropLastFieldsType -> visitType(type.type)
            is SyntaxStructDropLastFieldsExactType -> visitType(type.type)
            is SyntaxStructExtractFieldTypesType -> visitType(type.type)
            is SyntaxStructSelectFieldsType -> {
                checkDuplicates(type.names.node) { DuplicateStructFieldSelection(type, it) }
                visitType(type.type)
            }
            is SyntaxStructSelectFieldsExactType -> {
                checkDuplicates(type.names.node) { DuplicateStructFieldExactSelection(type, it) }
                visitType(type.type)
            }
            is SyntaxStructDropFieldsType -> {
                checkDuplicates(type.names.node) { DuplicateStructFieldDrop(type, it) }
                visitType(type.type)
            }
            is SyntaxStructDropFieldsExactType -> {
                checkDuplicates(type.names.node) { DuplicateStructFieldExactDrop(type, it) }
                visitType(type.type)
            }
            is SyntaxStructMergeStructsType -> type.types.node.forEach(::visitType)
            is SyntaxObjectGetMemberTypeType -> visitType(type.type)
            is SyntaxObjectSelectMembersType -> {
                checkDuplicates(type.names.node) { DuplicateObjectMemberSelection(type, it) }
                visitType(type.type)
            }
            is SyntaxObjectSelectMembersExactType -> {
                checkDuplicates(type.names.node) { DuplicateObjectMemberExactSelection(type, it) }
                visitType(type.type)
            }
            is SyntaxObjectDropMembersType -> {
                checkDuplicates(type.names.node) { DuplicateObjectMemberDrop(type, it) }
                visitType(type.type)
            }
            is SyntaxObjectDropMembersExactType -> {
                checkDuplicates(type.names.node) { DuplicateObjectMemberExactDrop(type, it) }
                visitType(type.type)
            }
            is SyntaxObjectMergeObjectsType -> type.types.node.forEach(::visitType)
            is SyntaxEnumGetEntryTypeType -> visitType(type.type)
            is SyntaxEnumSelectEntriesType -> {
                checkDuplicates(type.names.node) { DuplicateEnumEntrySelection(type, it) }
                visitType(type.type)
            }
            is SyntaxEnumSelectEntriesExactType -> {
                checkDuplicates(type.names.node) { DuplicateEnumEntryExactSelection(type, it) }
                visitType(type.type)
            }
            is SyntaxEnumDropEntriesType -> {
                checkDuplicates(type.names.node) { DuplicateEnumEntryDrop(type, it) }
                visitType(type.type)
            }
            is SyntaxEnumDropEntriesExactType -> {
                checkDuplicates(type.names.node) { DuplicateEnumEntryExactDrop(type, it) }
                visitType(type.type)
            }
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
            is SyntaxIntStatement -> {}
            is SyntaxLongStatement -> {}
            is SyntaxFloatStatement -> {}
            is SyntaxDoubleStatement -> {}
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
                checkNamedDuplicates(statement.parameters.node) { DuplicateConstructParameter(statement, it) }
                visitType(statement.type)
                statement.parameters.node.forEach { visitStatement(it.node.second) }
            }
            is SyntaxCall -> {
                checkNamedDuplicates(statement.generics?.node.orEmpty()) { DuplicateCallGenericArgument(statement, it) }
                checkNamedDuplicates(statement.parameters.node) { DuplicateCallParameter(statement, it) }
                statement.target?.let(::visitStatement)
                statement.generics?.node?.forEach { visitType(it.node.second) }
                statement.parameters.node.forEach { visitStatement(it.node.second) }
            }
            is SyntaxIndirectCall -> {
                checkNamedDuplicates(statement.parameters.node) { DuplicateIndirectCallParameter(statement, it) }
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
                checkDuplicates(statement.parameters?.node?.map { it.node.first }.orEmpty()) {
                    DuplicateLambdaParameter(statement, it)
                }
                statement.parameters?.node?.forEach { it.node.second?.let(::visitType) }
                visitStatement(statement.body)
            }
        }
    }
}
