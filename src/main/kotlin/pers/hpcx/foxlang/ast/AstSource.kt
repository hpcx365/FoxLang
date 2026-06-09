package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.runtime.*
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
    
    val indentUnit = options.indent
    val canonical = options.style == AstSourceStyle.CANONICAL
    val fileSeparator = if (options.compact) "\n" else "\n\n"
    
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
            is FoxTypeAlias -> {
                writer.print("type ")
                writer.print(element.name)
                writeTypeParameterNames(element.generics, writer)
                writer.print(" = ")
                render(element.alias, writer)
            }
            
            is FoxMethodDefinition -> {
                writer.print("def ")
                element.generics?.takeIf { it.isNotEmpty() }?.let {
                    writeFormalGenerics(it, writer)
                    writer.print(' ')
                }
                element.thisType?.let {
                    render(it, writer)
                    writer.print('.')
                }
                writer.print(element.name)
                writeFormalParameters(element.parameters, writer)
                element.returnType?.let {
                    writer.print(": ")
                    render(it, writer)
                }
                writer.print(' ')
                renderMethodBody(element.body, writer)
            }
        }
    }
    
    fun render(type: FoxType) = writeToString { render(type, it) }
    
    fun render(type: FoxType, writer: PrintWriter) {
        when (type) {
            is FoxPrimitiveType -> writer.print(
                when (type) {
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
                },
            )
            is FoxWildcardType -> writer.print(
                when (type) {
                    FoxAnyType -> "Any"
                    FoxAnyTupleType -> "AnyTuple"
                    FoxAnyStructType -> "AnyStruct"
                    FoxAnyObjectType -> "AnyObject"
                    FoxAnyEnumType -> "AnyEnum"
                    FoxAnyArrayType -> "AnyArray"
                    FoxAnyRefType -> "AnyRef"
                    FoxAnyMethodType -> "AnyMethod"
                },
            )
            is FoxBuiltInType -> when (type) {
                is FoxTupleType -> {
                    writer.print("Tuple<")
                    writeCommaSeparated(type.components, writer) { component, out ->
                        render(component.first, out)
                        if (component.second > 1) {
                            out.print(':')
                            out.print(component.second)
                        }
                    }
                    writer.print('>')
                }
                is FoxStructType -> {
                    writer.print("Struct<")
                    writeCommaSeparated(type.fields.entries, writer) { field, out ->
                        out.print(field.key)
                        out.print(": ")
                        render(field.value, out)
                    }
                    writer.print('>')
                }
                is FoxObjectType -> {
                    writer.print("Object<")
                    writeCommaSeparated(type.members.entries, writer) { member, out ->
                        out.print(member.key)
                        out.print(": ")
                        render(member.value, out)
                    }
                    writer.print('>')
                }
                is FoxEnumType -> {
                    writer.print("Enum<")
                    writeCommaSeparated(type.items.entries, writer) { item, out ->
                        out.print(item.key)
                        out.print(" = ")
                        render(item.value, out)
                    }
                    writer.print('>')
                }
                is FoxArrayType -> {
                    writer.print("Array<")
                    render(type.element, writer)
                    writer.print('>')
                }
                is FoxRefType -> {
                    writer.print("Ref<")
                    render(type.referent, writer)
                    writer.print('>')
                }
                is FoxMethodType -> {
                    writer.print("Method<")
                    writeCommaSeparated(listOf(type.`this`, *type.parameters.toTypedArray(), type.`return`), writer) { type, out ->
                        render(type, out)
                    }
                    writer.print('>')
                }
            }
            is FoxTransformType -> when (type) {
                is FoxTupleComponentAtType -> {
                    writer.print("ComponentAt<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.index)
                    writer.print('>')
                }
                is FoxTupleLastComponentAtType -> {
                    writer.print("LastComponentAt<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.index)
                    writer.print('>')
                }
                is FoxTupleFirstComponentsOfType -> {
                    writer.print("FirstComponentsOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.count)
                    writer.print('>')
                }
                is FoxTupleLastComponentsOfType -> {
                    writer.print("LastComponentsOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.count)
                    writer.print('>')
                }
                is FoxTupleDropFirstComponentsOfType -> {
                    writer.print("DropFirstComponentsOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.count)
                    writer.print('>')
                }
                is FoxTupleDropLastComponentsOfType -> {
                    writer.print("DropLastComponentsOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.count)
                    writer.print('>')
                }
                is FoxTupleMergeComponentsOfType -> {
                    writer.print("MergeComponentsOf<")
                    writeCommaSeparated(type.types, writer) { type, out ->
                        render(type, out)
                    }
                    writer.print('>')
                }
                is FoxStructFieldOfType -> {
                    writer.print("FieldOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.name)
                    writer.print('>')
                }
                is FoxStructFieldAtType -> {
                    writer.print("FieldAt<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.index)
                    writer.print('>')
                }
                is FoxStructLastFieldAtType -> {
                    writer.print("LastFieldAt<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.index)
                    writer.print('>')
                }
                is FoxStructFirstFieldsOfType -> {
                    writer.print("FirstFieldsOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.count)
                    writer.print('>')
                }
                is FoxStructLastFieldsOfType -> {
                    writer.print("LastFieldsOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.count)
                    writer.print('>')
                }
                is FoxStructDropFirstFieldsOfType -> {
                    writer.print("DropFirstFieldsOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.count)
                    writer.print('>')
                }
                is FoxStructDropLastFieldsOfType -> {
                    writer.print("DropLastFieldsOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.count)
                    writer.print('>')
                }
                is FoxStructFieldsOfType -> {
                    writer.print("FieldsOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writeCommaSeparated(type.names, writer) { name, out ->
                        out.print(name)
                    }
                    writer.print('>')
                }
                is FoxStructDropFieldsOfType -> {
                    writer.print("DropFieldsOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writeCommaSeparated(type.names, writer) { name, out ->
                        out.print(name)
                    }
                    writer.print('>')
                }
                is FoxStructMergeFieldsOfType -> {
                    writer.print("MergeFieldsOf<")
                    writeCommaSeparated(type.types, writer) { type, out ->
                        render(type, out)
                    }
                    writer.print('>')
                }
                is FoxObjectMemberOfType -> {
                    writer.print("MemberOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.name)
                    writer.print('>')
                }
                is FoxObjectMembersOfType -> {
                    writer.print("MembersOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writeCommaSeparated(type.names, writer) { name, out ->
                        out.print(name)
                    }
                    writer.print('>')
                }
                is FoxObjectDropMembersOfType -> {
                    writer.print("DropMembersOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writeCommaSeparated(type.names, writer) { name, out ->
                        out.print(name)
                    }
                    writer.print('>')
                }
                is FoxObjectMergeMembersOfType -> {
                    writer.print("MergeMembersOf<")
                    writeCommaSeparated(type.types, writer) { type, out ->
                        render(type, out)
                    }
                    writer.print('>')
                }
                is FoxEnumItemOfType -> {
                    writer.print("ItemOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writer.print(type.name)
                    writer.print('>')
                }
                is FoxEnumItemsOfType -> {
                    writer.print("ItemsOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writeCommaSeparated(type.names, writer) { name, out ->
                        out.print(name)
                    }
                    writer.print('>')
                }
                is FoxEnumDropItemsOfType -> {
                    writer.print("DropItemsOf<")
                    render(type.type, writer)
                    writer.print(", ")
                    writeCommaSeparated(type.names, writer) { name, out ->
                        out.print(name)
                    }
                    writer.print('>')
                }
                is FoxEnumMergeItemsOfType -> {
                    writer.print("MergeItemsOf<")
                    writeCommaSeparated(type.types, writer) { type, out ->
                        render(type, out)
                    }
                    writer.print('>')
                }
                is FoxArrayElementOfType -> {
                    writer.print("ElementOf<")
                    render(type.type, writer)
                    writer.print('>')
                }
                is FoxRefReferentOfType -> {
                    writer.print("ReferentOf<")
                    render(type.type, writer)
                    writer.print('>')
                }
                is FoxMethodThisOfType -> {
                    writer.print("ThisOf<")
                    render(type.type, writer)
                    writer.print('>')
                }
                is FoxMethodParametersOfType -> {
                    writer.print("ParametersOf<")
                    render(type.type, writer)
                    writer.print('>')
                }
                is FoxMethodReturnOfType -> {
                    writer.print("ReturnOf<")
                    render(type.type, writer)
                    writer.print('>')
                }
            }
            is FoxCustomizedType -> {
                writer.print(type.name)
                writeActualTypeArguments(type.parameters, writer)
            }
        }
    }
    
    fun render(statement: FoxStatement) = writeToString { render(statement, it) }
    
    fun render(statement: FoxStatement, writer: PrintWriter) {
        render(statement, writer, 0, Standalone)
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
            is FoxEntityStatement -> renderEntity(statement.value, writer)
            is FoxFormattedString -> renderFormattedString(statement, writer)
            is FoxSymbol -> writer.print(statement.name)
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
        block.label?.let {
            writer.print('#')
            writer.print(it)
            writer.print(' ')
        }
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
        statement.target?.let {
            render(it, writer, indentLevel, PostfixTarget)
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
        statement.target?.let {
            render(it, writer, indentLevel, PostfixTarget)
            writer.print(".(")
            render(statement.method, writer, indentLevel, Standalone)
            writer.print(')')
        } ?: run {
            writer.print('(')
            render(statement.method, writer, indentLevel, Standalone)
            writer.print(')')
        }
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
        statement.label?.let {
            writer.print('#')
            writer.print(it)
            writer.print(' ')
        }
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
        statement.label?.let {
            writer.print('#')
            writer.print(it)
            writer.print(' ')
        }
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
        statement.label?.let {
            writer.print('#')
            writer.print(it)
            writer.print(' ')
        }
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
        statement.label?.let {
            writer.print('#')
            writer.print(it)
            writer.print(' ')
        }
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
    
    private fun writeTypeParameterNames(
        generics: Set<String>?,
        writer: PrintWriter,
    ) {
        val values = generics?.takeIf { it.isNotEmpty() } ?: return
        writer.print('<')
        writeCommaSeparated(values, writer) { name, out -> out.print(name) }
        writer.print('>')
    }
    
    private fun writeFormalGenerics(
        generics: Map<String, FoxGenericConstraint>,
        writer: PrintWriter,
    ) {
        writer.print('<')
        writeCommaSeparated(generics.entries, writer) { (name, constraint), out ->
            out.print(name)
            constraint.match?.let {
                out.print(" = ")
                render(it, out)
            }
        }
        writer.print('>')
    }
    
    private fun writeFormalParameters(
        parameters: Map<String, FoxType>,
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
        
        is FoxFormattedString,
        is FoxEntityStatement,
        is FoxSymbol,
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
