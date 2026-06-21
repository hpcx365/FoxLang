package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.runtime.*
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet
import java.io.PrintWriter
import java.io.StringWriter

data class AstSourceOptions(
    val indent: String = "    ",
    val style: AstSourceStyle = AstSourceStyle.DEFAULT,
    val compact: Boolean = false,
)

enum class AstSourceStyle {
    DEFAULT,
    CANONICAL,
}

val DefaultAstSourcePrinter = AstSourcePrinter()

fun FoxFile.toSource() = DefaultAstSourcePrinter.render(this)
fun FoxFileElement.toSource() = DefaultAstSourcePrinter.render(this)
fun FoxType.toSource() = DefaultAstSourcePrinter.render(this)
fun FoxStatement.toSource() = DefaultAstSourcePrinter.render(this)

class AstSourcePrinter(options: AstSourceOptions = AstSourceOptions()) {
    
    private val indentUnit = options.indent
    private val canonical = options.style == AstSourceStyle.CANONICAL
    private val fileSeparator = if (options.compact) "\n" else "\n\n"
    
    // File elements
    fun render(file: FoxFile) = writeToString { render(file, it) }
    
    fun render(file: FoxFile, writer: PrintWriter) {
        file.elements.forEachIndexed { index, element ->
            if (index > 0) writer.print(fileSeparator)
            render(element, writer)
        }
    }
    
    fun render(element: FoxFileElement) = writeToString { render(element, it) }
    
    fun render(element: FoxFileElement, writer: PrintWriter) {
        when (element) {
            is FoxTypeAlias -> renderTypeAlias(element, writer)
            is FoxMethodDefinition -> renderMethodDefinition(element, writer)
        }
    }
    
    // Types
    fun render(type: FoxType) = writeToString { render(type, it) }
    
    fun render(type: FoxType, writer: PrintWriter) {
        when (type) {
            is FoxPrimitiveType -> writer.print(primitiveTypeText(type))
            is FoxWildcardType -> renderWildcardType(type, writer)
            is FoxBuiltInType -> renderBuiltInType(type, writer)
            is FoxTransformType -> renderTransformType(type, writer)
            is FoxUnresolvedType -> renderUnresolvedType(type, writer)
            is FoxPlaceholderType -> error("Placeholder type should not be rendered")
        }
    }
    
    // Statements
    fun render(statement: FoxStatement) = writeToString { render(statement, it) }
    
    fun render(statement: FoxStatement, writer: PrintWriter) {
        render(statement, writer, 0, Standalone)
    }
    
    private fun renderTypeAlias(
        element: FoxTypeAlias,
        writer: PrintWriter,
    ) {
        writer.print("type ")
        writer.print(element.name)
        writeTypeParameterNames(element.generics, writer)
        writer.print(" = ")
        render(element.alias, writer)
    }
    
    private fun renderMethodDefinition(
        element: FoxMethodDefinition,
        writer: PrintWriter,
    ) {
        writer.print("def ")
        if (canonical || element.generics.isNotEmpty()) {
            writeFormalGenerics(element.generics, writer)
            writer.print(' ')
        }
        if (canonical || element.thisType != FoxUnitType) {
            render(element.thisType, writer)
            writer.print('.')
        }
        writer.print(element.name)
        writeFormalParameters(element.parameters, writer)
        if (canonical || element.returnType != FoxUnitType) {
            writer.print(": ")
            render(element.returnType, writer)
        }
        writer.print(' ')
        renderMethodBody(element.body, writer)
    }
    
    private fun primitiveTypeText(type: FoxPrimitiveType) = when (type) {
        FoxVoidType -> "Void"
        FoxUnitType -> "Unit"
        FoxBoolType -> "Bool"
        FoxByteType -> "Byte"
        FoxShortType -> "Short"
        FoxIntType -> "Int"
        FoxLongType -> "Long"
        FoxFloatType -> "Float"
        FoxDoubleType -> "Double"
        FoxCharType -> "Char"
        FoxStringType -> "String"
    }
    
    private fun wildcardTypeText(type: FoxWildcardType) = when (type) {
        FoxAnyType -> "Any"
        is FoxAnyOfType -> error("Parameterized wildcard type should not be rendered as plain text")
        is FoxAllOfType -> error("Parameterized wildcard type should not be rendered as plain text")
        is FoxNoneOfType -> error("Parameterized wildcard type should not be rendered as plain text")
        FoxAnyTupleType -> "AnyTuple"
        is FoxAnyTupleOfType -> error("Parameterized wildcard type should not be rendered as plain text")
        FoxAnyStructType -> "AnyStruct"
        is FoxAnyStructOfType -> error("Parameterized wildcard type should not be rendered as plain text")
        FoxAnyObjectType -> "AnyObject"
        FoxAnyEnumType -> "AnyEnum"
    }
    
    private fun renderWildcardType(
        type: FoxWildcardType,
        writer: PrintWriter,
    ) {
        when (type) {
            is FoxAnyOfType -> renderTypeListArgument("AnyOf", type.types, writer)
            is FoxAllOfType -> renderTypeListArgument("AllOf", type.types, writer)
            is FoxNoneOfType -> renderTypeListArgument("NoneOf", type.types, writer)
            is FoxAnyTupleOfType -> renderSingleTypeArgument("AnyTupleOf", type.component, writer)
            is FoxAnyStructOfType -> renderTypeListArgument("AnyStructOf", type.fields, writer)
            else -> writer.print(wildcardTypeText(type))
        }
    }
    
    private fun renderBuiltInType(
        type: FoxBuiltInType,
        writer: PrintWriter,
    ) {
        when (type) {
            is FoxTupleType -> renderTupleType(type, writer)
            is FoxStructType -> renderStructType(type, writer)
            is FoxObjectType -> renderObjectType(type, writer)
            is FoxEnumType -> renderEnumType(type, writer)
            is FoxArrayType -> renderSingleTypeArgument("Array", type.element, writer)
            is FoxRefType -> renderSingleTypeArgument("Ref", type.referent, writer)
            is FoxMethodType -> renderMethodType(type, writer)
        }
    }
    
    private fun renderTransformType(
        type: FoxTransformType,
        writer: PrintWriter,
    ) {
        when (type) {
            is FoxTupleComponentAtType -> renderTypeAndIntArgument("ComponentAt", type.type, type.index, writer)
            is FoxTupleLastComponentAtType -> renderTypeAndIntArgument("LastComponentAt", type.type, type.index, writer)
            is FoxTupleFirstComponentsOfType -> renderTypeAndIntArgument("FirstComponentsOf", type.type, type.count, writer)
            is FoxTupleExactFirstComponentsOfType -> renderTypeAndIntArgument("ExactFirstComponentsOf", type.type, type.count, writer)
            is FoxTupleLastComponentsOfType -> renderTypeAndIntArgument("LastComponentsOf", type.type, type.count, writer)
            is FoxTupleExactLastComponentsOfType -> renderTypeAndIntArgument("ExactLastComponentsOf", type.type, type.count, writer)
            is FoxTupleDropFirstComponentsOfType -> renderTypeAndIntArgument("DropFirstComponentsOf", type.type, type.count, writer)
            is FoxTupleExactDropFirstComponentsOfType -> renderTypeAndIntArgument("ExactDropFirstComponentsOf", type.type, type.count, writer)
            is FoxTupleDropLastComponentsOfType -> renderTypeAndIntArgument("DropLastComponentsOf", type.type, type.count, writer)
            is FoxTupleExactDropLastComponentsOfType -> renderTypeAndIntArgument("ExactDropLastComponentsOf", type.type, type.count, writer)
            is FoxTupleMergeComponentsOfType -> renderTypeListArgument("MergeComponentsOf", type.types, writer)
            is FoxStructFieldOfType -> renderTypeAndNameArgument("FieldOf", type.type, type.name, writer)
            is FoxStructFieldAtType -> renderTypeAndIntArgument("FieldAt", type.type, type.index, writer)
            is FoxStructLastFieldAtType -> renderTypeAndIntArgument("LastFieldAt", type.type, type.index, writer)
            is FoxStructFirstFieldsOfType -> renderTypeAndIntArgument("FirstFieldsOf", type.type, type.count, writer)
            is FoxStructExactFirstFieldsOfType -> renderTypeAndIntArgument("ExactFirstFieldsOf", type.type, type.count, writer)
            is FoxStructLastFieldsOfType -> renderTypeAndIntArgument("LastFieldsOf", type.type, type.count, writer)
            is FoxStructExactLastFieldsOfType -> renderTypeAndIntArgument("ExactLastFieldsOf", type.type, type.count, writer)
            is FoxStructDropFirstFieldsOfType -> renderTypeAndIntArgument("DropFirstFieldsOf", type.type, type.count, writer)
            is FoxStructExactDropFirstFieldsOfType -> renderTypeAndIntArgument("ExactDropFirstFieldsOf", type.type, type.count, writer)
            is FoxStructDropLastFieldsOfType -> renderTypeAndIntArgument("DropLastFieldsOf", type.type, type.count, writer)
            is FoxStructExactDropLastFieldsOfType -> renderTypeAndIntArgument("ExactDropLastFieldsOf", type.type, type.count, writer)
            is FoxStructFieldsOfType -> renderTypeAndNamesArgument("FieldsOf", type.type, type.names, writer)
            is FoxStructDropFieldsOfType -> renderTypeAndNamesArgument("DropFieldsOf", type.type, type.names, writer)
            is FoxStructMergeFieldsOfType -> renderTypeListArgument("MergeFieldsOf", type.types, writer)
            is FoxObjectMemberOfType -> renderTypeAndNameArgument("MemberOf", type.type, type.name, writer)
            is FoxObjectMembersOfType -> renderTypeAndNamesArgument("MembersOf", type.type, type.names, writer)
            is FoxObjectDropMembersOfType -> renderTypeAndNamesArgument("DropMembersOf", type.type, type.names, writer)
            is FoxObjectMergeMembersOfType -> renderTypeListArgument("MergeMembersOf", type.types, writer)
            is FoxEnumItemOfType -> renderTypeAndNameArgument("ItemOf", type.type, type.name, writer)
            is FoxEnumItemsOfType -> renderTypeAndNamesArgument("ItemsOf", type.type, type.names, writer)
            is FoxEnumDropItemsOfType -> renderTypeAndNamesArgument("DropItemsOf", type.type, type.names, writer)
            is FoxEnumMergeItemsOfType -> renderTypeListArgument("MergeItemsOf", type.types, writer)
            is FoxArrayElementOfType -> renderSingleTypeArgument("ElementOf", type.type, writer)
            is FoxRefReferentOfType -> renderSingleTypeArgument("ReferentOf", type.type, writer)
            is FoxMethodOfType -> renderTypeListArgument("MethodOf", listOf(type.`this`, type.parameters, type.`return`), writer)
            is FoxMethodThisOfType -> renderSingleTypeArgument("ThisOf", type.type, writer)
            is FoxMethodParametersOfType -> renderSingleTypeArgument("ParametersOf", type.type, writer)
            is FoxMethodReturnOfType -> renderSingleTypeArgument("ReturnOf", type.type, writer)
        }
    }
    
    private fun renderTupleType(
        type: FoxTupleType,
        writer: PrintWriter,
    ) {
        writer.print("Tuple<")
        var first = true
        var current: FoxType? = null
        var count = 0
        fun flushCurrent(out: PrintWriter) {
            val value = current ?: return
            if (!first) out.print(", ")
            first = false
            render(value, out)
            if (count > 1) {
                out.print(':')
                out.print(count)
            }
        }
        type.components.forEach { component ->
            if (current == component) {
                count++
            } else {
                flushCurrent(writer)
                current = component
                count = 1
            }
        }
        flushCurrent(writer)
        writer.print('>')
    }
    
    private fun renderStructType(
        type: FoxStructType,
        writer: PrintWriter,
    ) {
        writer.print("Struct<")
        writeCommaSeparated(type.fields.entries, writer) { field, out ->
            out.print(field.key)
            out.print(": ")
            render(field.value, out)
        }
        writer.print('>')
    }
    
    private fun renderObjectType(
        type: FoxObjectType,
        writer: PrintWriter,
    ) {
        writer.print("Object<")
        writeCommaSeparated(type.members.entries, writer) { member, out ->
            out.print(member.key)
            out.print(": ")
            render(member.value, out)
        }
        writer.print('>')
    }
    
    private fun renderEnumType(
        type: FoxEnumType,
        writer: PrintWriter,
    ) {
        writer.print("Enum<")
        writeCommaSeparated(type.items.entries, writer) { item, out ->
            out.print(item.key)
            out.print(" = ")
            render(item.value, out)
        }
        writer.print('>')
    }
    
    private fun renderMethodType(
        type: FoxMethodType,
        writer: PrintWriter,
    ) {
        writer.print("Method<")
        render(type.`this`, writer)
        writer.print(", ")
        type.parameters.forEach {
            writer.print(it.key)
            writer.print(": ")
            render(it.value, writer)
            writer.print(", ")
        }
        render(type.`return`, writer)
        writer.print('>')
    }
    
    private fun renderUnresolvedType(
        type: FoxUnresolvedType,
        writer: PrintWriter,
    ) {
        writer.print(type.name)
        type.parameters?.let { parameters ->
            writer.print('<')
            writeCommaSeparated(parameters, writer) { parameter, out ->
                render(parameter, out)
            }
            writer.print('>')
        }
    }
    
    private fun renderSingleTypeArgument(
        keyword: String,
        type: FoxType,
        writer: PrintWriter,
    ) {
        writer.print(keyword)
        writer.print('<')
        render(type, writer)
        writer.print('>')
    }
    
    private fun renderTypeAndIntArgument(
        keyword: String,
        type: FoxType,
        value: Int,
        writer: PrintWriter,
    ) {
        writer.print(keyword)
        writer.print('<')
        render(type, writer)
        writer.print(", ")
        writer.print(value)
        writer.print('>')
    }
    
    private fun renderTypeAndNameArgument(
        keyword: String,
        type: FoxType,
        name: String,
        writer: PrintWriter,
    ) {
        writer.print(keyword)
        writer.print('<')
        render(type, writer)
        writer.print(", ")
        writer.print(name)
        writer.print('>')
    }
    
    private fun renderTypeAndNamesArgument(
        keyword: String,
        type: FoxType,
        names: Iterable<String>,
        writer: PrintWriter,
    ) {
        writer.print(keyword)
        writer.print('<')
        render(type, writer)
        writer.print(", ")
        writeCommaSeparated(names, writer) { name, out ->
            out.print(name)
        }
        writer.print('>')
    }
    
    private fun renderTypeListArgument(
        keyword: String,
        types: Iterable<FoxType>,
        writer: PrintWriter,
    ) {
        writer.print(keyword)
        writer.print('<')
        writeCommaSeparated(types, writer) { type, out ->
            render(type, out)
        }
        writer.print('>')
    }
    
    private fun render(
        statement: FoxStatement,
        writer: PrintWriter,
        indentLevel: Int,
        position: StatementPosition,
    ) {
        val precedence = precedenceOf(statement)
        if (needsParentheses(statement, precedence, position)) writer.print('(')
        renderRawStatement(statement, writer, indentLevel)
        if (needsParentheses(statement, precedence, position)) writer.print(')')
    }
    
    private fun renderRawStatement(
        statement: FoxStatement,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        when (statement) {
            FoxThis -> writer.print("this")
            is FoxSymbol -> writer.print(statement.name)
            is FoxEntityStatement -> renderEntity(statement.value, writer)
            is FoxFormattedString -> renderFormattedString(statement, writer)
            is FoxBlock -> renderBlock(statement, writer, indentLevel)
            is FoxUnary -> renderUnary(statement, writer, indentLevel)
            is FoxBinary -> renderBinary(statement, writer, indentLevel)
            is FoxAssign -> renderAssign(statement, writer, indentLevel)
            is FoxTypeBinding -> {
                writer.print(statement.name)
                writer.print(": ")
                render(statement.type, writer)
            }
            is FoxFieldAccess -> {
                render(statement.target, writer, indentLevel, PostfixTarget)
                writer.print('.')
                writer.print(statement.name)
            }
            is FoxComponentAccess -> {
                render(statement.target, writer, indentLevel, PostfixTarget)
                writer.print('.')
                writer.print(statement.index)
            }
            is FoxCall -> renderCall(statement, writer, indentLevel)
            is FoxConstruct -> {
                render(statement.type, writer)
                writeActualParameters(statement.parameters, writer, indentLevel)
            }
            is FoxIndirectCall -> renderIndirectCall(statement, writer, indentLevel)
            is FoxIf -> renderIf(statement, writer, indentLevel)
            is FoxWhen -> renderWhen(statement, writer, indentLevel)
            is FoxWhile -> renderWhile(statement, writer, indentLevel)
            is FoxDoWhile -> renderDoWhile(statement, writer, indentLevel)
            is FoxBreak -> {
                writer.print("break")
                statement.label?.let {
                    writer.print(" #")
                    writer.print(it)
                }
            }
            is FoxContinue -> {
                writer.print("continue")
                statement.label?.let {
                    writer.print(" #")
                    writer.print(it)
                }
            }
            is FoxYield -> {
                writer.print("yield")
                statement.label?.let {
                    writer.print(" #")
                    writer.print(it)
                }
                writer.print(' ')
                render(statement.value, writer, indentLevel, Standalone)
            }
            is FoxReturn -> {
                writer.print("return")
                statement.value?.let {
                    writer.print(' ')
                    render(it, writer, indentLevel, Standalone)
                }
            }
        }
    }
    
    private fun renderBlock(
        block: FoxBlock,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        writeLabelPrefix(block.label, writer)
        if (block.statements.isEmpty()) {
            writer.print("{}")
            return
        }
        writer.print("{\n")
        block.statements.forEachIndexed { index, statement ->
            writer.print(indent(indentLevel + 1))
            render(statement, writer, indentLevel + 1, Standalone)
            if (index != block.statements.lastIndex) writer.print('\n')
        }
        writer.print('\n')
        writer.print(indent(indentLevel))
        writer.print('}')
    }
    
    private fun renderUnary(
        statement: FoxUnary,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        writer.print(unaryOperatorText(statement.operator))
        render(statement.right, writer, indentLevel, UnaryOperand)
    }
    
    private fun renderBinary(
        statement: FoxBinary,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        val precedence = binaryPrecedence(statement.operator)
        render(statement.left, writer, indentLevel, BinaryLeft(precedence))
        writer.print(' ')
        writer.print(binaryOperatorText(statement.operator))
        writer.print(' ')
        render(statement.right, writer, indentLevel, BinaryRight(precedence))
    }
    
    private fun renderAssign(
        statement: FoxAssign,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        render(statement.left, writer, indentLevel, AssignLeft)
        writer.print(' ')
        writer.print(assignOperatorText(statement.operator))
        writer.print(' ')
        render(statement.right, writer, indentLevel, AssignRight)
    }
    
    private fun renderCall(
        statement: FoxCall,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        if (canonical || statement.target != FoxUnit) {
            render(statement.target, writer, indentLevel, PostfixTarget)
            writer.print('.')
        }
        writer.print(statement.name)
        statement.generics?.let { writeActualTypeArguments(it, writer) }
        writeActualParameters(statement.parameters, writer, indentLevel)
    }
    
    private fun renderIndirectCall(
        statement: FoxIndirectCall,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        if (canonical || statement.target != FoxUnit) {
            render(statement.target, writer, indentLevel, PostfixTarget)
            writer.print('.')
        }
        writer.print('(')
        render(statement.method, writer, indentLevel, Standalone)
        writer.print(')')
        writer.print('(')
        writeCommaSeparated(statement.parameters, writer) { parameter, out ->
            render(parameter, out, indentLevel, Standalone)
        }
        writer.print(')')
    }
    
    private fun renderIf(
        statement: FoxIf,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        writeLabelPrefix(statement.label, writer)
        writer.print("if (")
        render(statement.condition, writer, indentLevel, Standalone)
        writer.print(") ")
        renderControlBody(statement.thenBody, writer, indentLevel)
        statement.elseBody?.let {
            writer.print(" else ")
            renderControlBody(it, writer, indentLevel, allowElseIfChain = true)
        }
    }
    
    private fun renderWhen(
        statement: FoxWhen,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        writeLabelPrefix(statement.label, writer)
        writer.print("when")
        statement.value?.let {
            writer.print(" (")
            render(it, writer, indentLevel, Standalone)
            writer.print(')')
        }
        if (statement.cases.isEmpty()) {
            writer.print(" {}")
            return
        }
        writer.print(" {\n")
        statement.cases.forEachIndexed { index, case ->
            writer.print(indent(indentLevel + 1))
            if (case.conditions.isEmpty()) {
                writer.print("else")
            } else {
                writeCommaSeparated(case.conditions, writer) { condition, out ->
                    render(condition, out, indentLevel + 1, Standalone)
                }
            }
            writer.print(" -> ")
            renderControlBody(case.body, writer, indentLevel + 1)
            if (index != statement.cases.lastIndex) writer.print('\n')
        }
        writer.print('\n')
        writer.print(indent(indentLevel))
        writer.print('}')
    }
    
    private fun renderWhile(
        statement: FoxWhile,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        writeLabelPrefix(statement.label, writer)
        writer.print("while (")
        render(statement.condition, writer, indentLevel, Standalone)
        writer.print(") ")
        renderControlBody(statement.body, writer, indentLevel)
    }
    
    private fun renderDoWhile(
        statement: FoxDoWhile,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        writeLabelPrefix(statement.label, writer)
        writer.print("do ")
        renderControlBody(statement.body, writer, indentLevel)
        writer.print(" while (")
        render(statement.condition, writer, indentLevel, Standalone)
        writer.print(')')
    }
    
    private fun renderMethodBody(
        statement: FoxStatement,
        writer: PrintWriter,
    ) {
        if (canonical) renderBlockBody(statement, writer, 0)
        else render(statement, writer, 0, Standalone)
    }
    
    private fun renderControlBody(
        statement: FoxStatement,
        writer: PrintWriter,
        indentLevel: Int,
        allowElseIfChain: Boolean = false,
    ) {
        when {
            canonical && statement is FoxIf && allowElseIfChain ->
                render(statement, writer, indentLevel, Standalone)
            
            canonical -> renderBlockBody(statement, writer, indentLevel)
            else -> render(statement, writer, indentLevel, Standalone)
        }
    }
    
    private fun renderBlockBody(
        statement: FoxStatement,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        if (statement is FoxBlock) {
            renderBlock(statement, writer, indentLevel)
            return
        }
        writer.print("{\n")
        writer.print(indent(indentLevel + 1))
        render(statement, writer, indentLevel + 1, Standalone)
        writer.print('\n')
        writer.print(indent(indentLevel))
        writer.print('}')
    }
    
    private fun writeLabelPrefix(
        label: String?,
        writer: PrintWriter,
    ) {
        label?.let {
            writer.print('#')
            writer.print(it)
            writer.print(' ')
        }
    }
    
    private fun writeTypeParameterNames(
        generics: OrderedSet<String>,
        writer: PrintWriter,
    ) {
        if (canonical || generics.isNotEmpty()) {
            writer.print('<')
            writeCommaSeparated(generics, writer) { name, out -> out.print(name) }
            writer.print('>')
        }
    }
    
    private fun writeFormalGenerics(
        generics: OrderedMap<String, FoxType>,
        writer: PrintWriter,
    ) {
        writer.print('<')
        writeCommaSeparated(generics.entries, writer) { (name, pattern), out ->
            out.print(name)
            out.print(" = ")
            render(pattern, out)
        }
        writer.print('>')
    }
    
    private fun writeFormalParameters(
        parameters: OrderedMap<String, FoxType>,
        writer: PrintWriter,
    ) {
        writer.print('(')
        writeCommaSeparated(parameters.entries, writer) { parameter, out ->
            out.print(parameter.key)
            out.print(": ")
            render(parameter.value, out)
        }
        writer.print(')')
    }
    
    private fun writeActualParameters(
        parameters: List<Pair<String?, FoxStatement>>,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        writer.print('(')
        writeCommaSeparated(parameters, writer) { (name, value), out ->
            if (name != null) {
                out.print(name)
                out.print(" = ")
            }
            render(value, out, indentLevel, Standalone)
        }
        writer.print(')')
    }
    
    private fun writeActualTypeArguments(
        generics: List<Pair<String?, FoxType>>,
        writer: PrintWriter,
    ) {
        if (generics.isEmpty()) return
        writer.print('<')
        writeCommaSeparated(generics, writer) { (name, type), out ->
            if (name != null) {
                out.print(name)
                out.print(" = ")
            }
            render(type, out)
        }
        writer.print('>')
    }
    
    private fun renderFormattedString(
        statement: FoxFormattedString,
        writer: PrintWriter,
    ) {
        writer.print(if (statement.isRaw) "rf\"" else "f\"")
        statement.parts.forEach { part ->
            when (part) {
                is FoxFormattedText ->
                    writer.print(if (statement.isRaw) escapeRawFormattedText(part.text) else escapeFormattedText(part.text))
                
                is FoxFormattedExpression -> {
                    writer.print('{')
                    render(part.expression, writer, 0, Standalone)
                    writer.print('}')
                }
            }
        }
        writer.print('"')
    }
    
    private fun renderEntity(
        entity: FoxEntity,
        writer: PrintWriter,
    ) {
        when (entity) {
            FoxUnit -> writer.print("unit")
            is FoxBool -> writer.print(entity.value)
            is FoxByte -> writer.print(entity.value)
            is FoxShort -> writer.print(entity.value)
            is FoxInt -> writer.print(entity.value)
            is FoxLong -> {
                writer.print(entity.value)
                writer.print('L')
            }
            is FoxFloat -> {
                writer.print(entity.value)
                writer.print('f')
            }
            is FoxDouble -> writer.print(entity.value)
            is FoxChar -> {
                writer.print('\'')
                writer.print(escapeChar(entity.value))
                writer.print('\'')
            }
            is FoxString -> {
                writer.print('"')
                writer.print(escapeString(entity.value))
                writer.print('"')
            }
            is FoxArray -> TODO()
            is FoxTuple -> TODO()
            is FoxStruct -> TODO()
            is FoxObject -> TODO()
            is FoxEnum -> TODO()
            is FoxRef -> TODO()
            is FoxMethod -> TODO()
        }
    }
    
    private fun unaryOperatorText(operator: FoxUnaryOperator) = when (operator) {
        FoxNotOperator -> "!"
        FoxNegOperator -> "-"
    }
    
    private fun binaryOperatorText(operator: FoxBinaryOperator) = when (operator) {
        FoxAddOperator -> "+"
        FoxSubOperator -> "-"
        FoxMulOperator -> "*"
        FoxDivOperator -> "/"
        FoxRemOperator -> "%"
        FoxAndOperator -> "&"
        FoxOrOperator -> "|"
        FoxXorOperator -> "^"
        FoxShlOperator -> "<<"
        FoxShrOperator -> ">>"
        FoxUshrOperator -> ">>>"
        FoxEqOperator -> "=="
        FoxNeqOperator -> "!="
        FoxLtOperator -> "<"
        FoxGtOperator -> ">"
        FoxLeOperator -> "<="
        FoxGeOperator -> ">="
        FoxAndAndOperator -> "&&"
        FoxOrOrOperator -> "||"
    }
    
    private fun assignOperatorText(operator: FoxAssignOperator) = when (operator) {
        FoxPlainAssignOperator -> "="
        FoxTypeBindingAssignOperator -> ":="
        FoxAddAssignOperator -> "+="
        FoxSubAssignOperator -> "-="
        FoxMulAssignOperator -> "*="
        FoxDivAssignOperator -> "/="
        FoxRemAssignOperator -> "%="
        FoxAndAssignOperator -> "&="
        FoxOrAssignOperator -> "|="
        FoxXorAssignOperator -> "^="
        FoxShlAssignOperator -> "<<="
        FoxShrAssignOperator -> ">>="
        FoxUshrAssignOperator -> ">>>="
        FoxAndAndAssignOperator -> "&&="
        FoxOrOrAssignOperator -> "||="
    }
    
    private fun precedenceOf(statement: FoxStatement): Int = when (statement) {
        is FoxIf,
        is FoxWhen,
        is FoxWhile,
        is FoxDoWhile,
        is FoxBreak,
        is FoxContinue,
        is FoxYield,
        is FoxReturn,
        is FoxBlock,
        is FoxTypeBinding,
            -> 0
        
        is FoxAssign -> 10
        is FoxBinary -> binaryPrecedence(statement.operator)
        is FoxUnary -> 120
        is FoxFieldAccess,
        is FoxComponentAccess,
        is FoxCall,
        is FoxConstruct,
        is FoxIndirectCall,
            -> 130
        
        FoxThis,
        is FoxSymbol,
        is FoxFormattedString,
        is FoxEntityStatement,
            -> 140
    }
    
    private fun binaryPrecedence(operator: FoxBinaryOperator): Int = when (operator) {
        FoxOrOrOperator -> 20
        FoxAndAndOperator -> 30
        FoxOrOperator -> 40
        FoxXorOperator -> 50
        FoxAndOperator -> 60
        FoxEqOperator, FoxNeqOperator -> 70
        FoxLtOperator, FoxGtOperator, FoxLeOperator, FoxGeOperator -> 80
        FoxShlOperator, FoxShrOperator, FoxUshrOperator -> 90
        FoxAddOperator, FoxSubOperator -> 100
        FoxMulOperator, FoxDivOperator, FoxRemOperator -> 110
    }
    
    private fun needsParentheses(
        statement: FoxStatement,
        precedence: Int,
        position: StatementPosition,
    ): Boolean {
        val parentPrecedence = position.parentPrecedence
        if (precedence < parentPrecedence) return true
        if (statement is FoxBinary && position is BinaryRight && precedence == parentPrecedence) return true
        return false
    }
    
    private fun escapeChar(char: Char) = when (char) {
        '\b' -> "\\b"
        '\t' -> "\\t"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\\' -> "\\\\"
        '\'' -> "\\'"
        else -> if (char.isISOControl()) "\\u%04x".format(char.code) else char.toString()
    }
    
    private fun escapeString(string: String) = buildString {
        string.forEach { char ->
            append(
                when (char) {
                    '\b' -> "\\b"
                    '\t' -> "\\t"
                    '\n' -> "\\n"
                    '\r' -> "\\r"
                    '\\' -> "\\\\"
                    '"' -> "\\\""
                    else -> if (char.isISOControl()) "\\u%04x".format(char.code) else char.toString()
                },
            )
        }
    }
    
    private fun escapeFormattedText(string: String) = buildString {
        var escapedBraceDepth = 0
        string.forEach { char ->
            when (char) {
                '\b' -> append("\\b")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '{' -> {
                    append("\\{")
                    escapedBraceDepth++
                }
                '}' -> {
                    if (escapedBraceDepth > 0) {
                        append('}')
                        escapedBraceDepth--
                    } else {
                        append("\\}")
                    }
                }
                else -> append(if (char.isISOControl()) "\\u%04x".format(char.code) else char.toString())
            }
        }
    }
    
    private fun escapeRawFormattedText(string: String) = buildString {
        var escapedBraceDepth = 0
        string.forEachIndexed { index, char ->
            when (char) {
                '"' -> append("\\\"")
                '{' -> {
                    append("\\{")
                    escapedBraceDepth++
                }
                '}' -> {
                    if (escapedBraceDepth > 0) {
                        append('}')
                        escapedBraceDepth--
                    } else {
                        append("\\}")
                    }
                }
                '\\' -> {
                    if (index == string.lastIndex) append("\\\\")
                    else append('\\')
                }
                else -> append(char)
            }
        }
    }
    
    private fun indent(level: Int) = indentUnit.repeat(level)
    
    private fun <T> writeCommaSeparated(
        values: Iterable<T>,
        writer: PrintWriter,
        block: (T, PrintWriter) -> Unit,
    ) {
        var first = true
        values.forEach { value ->
            if (!first) writer.print(", ")
            first = false
            block(value, writer)
        }
    }
    
    private fun writeToString(block: (PrintWriter) -> Unit): String {
        val target = StringWriter()
        PrintWriter(target).use(block)
        return target.toString()
    }
}

private sealed class StatementPosition(val parentPrecedence: Int)
private data object Standalone : StatementPosition(0)
private data object UnaryOperand : StatementPosition(120)
private data object PostfixTarget : StatementPosition(130)
private data object AssignLeft : StatementPosition(11)
private data object AssignRight : StatementPosition(10)
private data class BinaryLeft(val precedence: Int) : StatementPosition(precedence)
private data class BinaryRight(val precedence: Int) : StatementPosition(precedence)
