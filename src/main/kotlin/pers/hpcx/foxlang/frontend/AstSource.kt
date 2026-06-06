package pers.hpcx.foxlang.frontend

import pers.hpcx.foxlang.runtime.*
import pers.hpcx.foxlang.types.*
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

data class AstSourceOptions(
    val indent: String = "    ",
    val style: AstSourceStyle = AstSourceStyle.DEFAULT,
    val compact: Boolean = false,
)

enum class AstSourceStyle {
    DEFAULT,
    CANONICAL,
}

fun NodeFile.toSource(options: AstSourceOptions = AstSourceOptions()): String = AstSourcePrinter(options).renderFile(this)

fun NodeFile.toSource(indent: String): String = toSource(AstSourceOptions(indent = indent))

fun NodeFile.render(
    writer: PrintWriter,
    options: AstSourceOptions = AstSourceOptions(),
) {
    AstSourcePrinter(options).renderFile(this, writer)
}

fun NodeFileElement.toSource(options: AstSourceOptions = AstSourceOptions()): String =
    AstSourcePrinter(options).renderFileElement(this)

fun NodeFileElement.toSource(indent: String): String = toSource(AstSourceOptions(indent = indent))

fun NodeFileElement.render(
    writer: PrintWriter,
    options: AstSourceOptions = AstSourceOptions(),
) {
    AstSourcePrinter(options).renderFileElement(this, writer)
}

fun NodeType.toSource(options: AstSourceOptions = AstSourceOptions()): String = AstSourcePrinter(options).renderType(this)

fun NodeType.render(
    writer: PrintWriter,
    options: AstSourceOptions = AstSourceOptions(),
) {
    AstSourcePrinter(options).renderType(this, writer)
}

fun NodeStatement.toSource(options: AstSourceOptions = AstSourceOptions()): String =
    AstSourcePrinter(options).renderStatement(this)

fun NodeStatement.toSource(indent: String): String = toSource(AstSourceOptions(indent = indent))

fun NodeStatement.render(
    writer: PrintWriter,
    options: AstSourceOptions = AstSourceOptions(),
) {
    AstSourcePrinter(options).renderStatement(this, writer)
}

private class AstSourcePrinter(
    private val options: AstSourceOptions = AstSourceOptions(),
) {
    private val indentUnit = options.indent
    private val canonical = options.style == AstSourceStyle.CANONICAL
    private val fileSeparator = if (options.compact) "\n" else "\n\n"
    
    fun renderFile(file: NodeFile): String = writeToString { renderFile(file, it) }
    
    fun renderFile(
        file: NodeFile,
        writer: PrintWriter,
    ) {
        file.elements.forEachIndexed { index, element ->
            if (index > 0) writer.print(fileSeparator)
            renderFileElement(element, writer)
        }
    }
    
    fun renderFileElement(element: NodeFileElement): String = writeToString { renderFileElement(element, it) }
    
    fun renderFileElement(
        element: NodeFileElement,
        writer: PrintWriter,
    ) {
        when (element) {
            is NodeTypeAlias -> {
                writer.print("type ")
                writer.print(element.name)
                writeTypeParameterNames(element.generics, writer)
                writer.print(" = ")
                renderType(element.alias, writer)
            }
            
            is NodeMethodDefinition -> {
                writer.print("def ")
                element.generics?.takeIf { it.isNotEmpty() }?.let {
                    writeFormalGenerics(it, writer)
                    writer.print(' ')
                }
                element.thisType?.let {
                    renderType(it, writer)
                    writer.print('.')
                }
                writer.print(element.name)
                writeFormalParameters(element.parameters, writer)
                element.returnType?.let {
                    writer.print(": ")
                    renderType(it, writer)
                }
                writer.print(' ')
                renderMethodBody(element.body, writer)
            }
        }
    }
    
    fun renderType(type: NodeType): String = writeToString { renderType(type, it) }
    
    fun renderType(
        type: NodeType,
        writer: PrintWriter,
    ) {
        when (type) {
            is NodePrimitiveType -> writer.print(renderPrimitiveType(type.type))
            is NodeNamedType -> {
                writer.print(type.name)
                writeActualTypeArguments(type.generics, writer)
            }
            is NodeArrayType -> {
                writer.print("Array<")
                renderType(type.elementType, writer)
                writer.print('>')
            }
            is NodeTupleType -> {
                writer.print("Tuple<")
                writeCommaSeparated(type.items, writer) { item, out ->
                    when (item) {
                        is NodeTupleTypeItem -> renderType(item.type, out)
                        is NodeTupleSpreadItem -> {
                            out.print('*')
                            renderType(item.type, out)
                        }
                    }
                }
                writer.print('>')
            }
            is NodeNamedProjectionType -> {
                writer.print("Named<")
                renderType(type.baseType, writer)
                writer.print('>')
            }
            is NodeStructWildcardType -> {
                writer.print("Struct<")
                writer.print(type.wildcardToken)
                writer.print('>')
            }
            is NodeStructType -> {
                writer.print("Struct<")
                writeCommaSeparated(type.items, writer) { item, out ->
                    when (item) {
                        is NodeStructFieldItem -> {
                            out.print(item.name)
                            out.print(": ")
                            renderType(item.type, out)
                        }
                        is NodeStructSpreadItem -> {
                            out.print('*')
                            renderType(item.type, out)
                        }
                    }
                }
                writer.print('>')
            }
            is NodeDenamedProjectionType -> {
                writer.print("Denamed<")
                renderType(type.baseType, writer)
                writer.print('>')
            }
            is NodeEnumType -> {
                writer.print("Enum<")
                writeCommaSeparated(type.items.entries, writer) { entry, out ->
                    out.print(entry.key)
                    out.print(" = ")
                    renderType(entry.value, out)
                }
                writer.print('>')
            }
            is NodeRefType -> {
                writer.print("Ref<")
                renderType(type.referentType, writer)
                writer.print('>')
            }
            is NodeLambdaType -> {
                writer.print("Lambda<")
                var first = true
                fun printTypeArgument(argument: NodeType) {
                    if (!first) writer.print(", ")
                    first = false
                    renderType(argument, writer)
                }
                printTypeArgument(type.thisType)
                type.parameters.forEach(::printTypeArgument)
                printTypeArgument(type.returnType)
                writer.print('>')
            }
        }
    }
    
    fun renderStatement(statement: NodeStatement): String = writeToString { renderStatement(statement, it) }
    
    fun renderStatement(
        statement: NodeStatement,
        writer: PrintWriter,
    ) {
        renderStatement(statement, writer, 0, StatementPosition.STANDALONE)
    }
    
    private fun renderStatement(
        statement: NodeStatement,
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
        statement: NodeStatement,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        when (statement) {
            is NodeEntity -> renderEntity(statement.value, writer)
            is NodeFormattedString -> renderFormattedString(statement, writer)
            is NodeSymbol -> writer.print(statement.name)
            is NodeBlock -> renderBlock(statement, writer, indentLevel)
            is NodeUnary -> renderUnary(statement, writer, indentLevel)
            is NodeBinary -> renderBinary(statement, writer, indentLevel)
            is NodeAssign -> renderAssign(statement, writer, indentLevel)
            is NodeTypeBinding -> {
                writer.print(statement.name)
                writer.print(": ")
                renderType(statement.type, writer)
            }
            is NodeFieldAccess -> {
                renderStatement(statement.target, writer, indentLevel, StatementPosition.POSTFIX_TARGET)
                writer.print('.')
                writer.print(statement.name)
            }
            is NodeComponentAccess -> {
                renderStatement(statement.target, writer, indentLevel, StatementPosition.POSTFIX_TARGET)
                writer.print('.')
                writer.print(statement.index)
            }
            is NodeCall -> renderCall(statement, writer, indentLevel)
            is NodeConstruct -> {
                renderType(statement.type, writer)
                writeActualParameters(statement.parameters, writer, indentLevel)
            }
            is NodeLambda -> renderLambda(statement, writer, indentLevel)
            is NodeLambdaCall -> renderLambdaCall(statement, writer, indentLevel)
            is NodeIf -> renderIf(statement, writer, indentLevel)
            is NodeWhen -> renderWhen(statement, writer, indentLevel)
            is NodeWhile -> renderWhile(statement, writer, indentLevel)
            is NodeDoWhile -> renderDoWhile(statement, writer, indentLevel)
            is NodeGenFor -> renderGenFor(statement, writer, indentLevel)
            is NodeBreak -> {
                writer.print("break")
                statement.label?.let {
                    writer.print(" #")
                    writer.print(it)
                }
            }
            is NodeContinue -> {
                writer.print("continue")
                statement.label?.let {
                    writer.print(" #")
                    writer.print(it)
                }
            }
            is NodeYield -> {
                writer.print("yield")
                statement.label?.let {
                    writer.print(" #")
                    writer.print(it)
                }
                writer.print(' ')
                renderStatement(statement.value, writer, indentLevel, StatementPosition.STANDALONE)
            }
            is NodeReturn -> {
                writer.print("return")
                statement.value?.let {
                    writer.print(' ')
                    renderStatement(it, writer, indentLevel, StatementPosition.STANDALONE)
                }
            }
        }
    }
    
    private fun renderBlock(
        block: NodeBlock,
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
            renderStatement(statement, writer, indentLevel + 1, StatementPosition.STANDALONE)
            if (index != block.statements.lastIndex) writer.print('\n')
        }
        writer.print('\n')
        writer.print(indent(indentLevel))
        writer.print('}')
    }
    
    private fun renderUnary(
        statement: NodeUnary,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        writer.print(unaryOperatorText(statement.operator))
        renderStatement(statement.right, writer, indentLevel, StatementPosition.UNARY_OPERAND)
    }
    
    private fun renderBinary(
        statement: NodeBinary,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        val precedence = binaryPrecedence(statement.operator)
        renderStatement(statement.left, writer, indentLevel, StatementPosition.BINARY_LEFT(precedence))
        writer.print(' ')
        writer.print(binaryOperatorText(statement.operator))
        writer.print(' ')
        renderStatement(statement.right, writer, indentLevel, StatementPosition.BINARY_RIGHT(precedence))
    }
    
    private fun renderAssign(
        statement: NodeAssign,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        renderStatement(statement.left, writer, indentLevel, StatementPosition.ASSIGN_LEFT)
        writer.print(' ')
        writer.print(assignOperatorText(statement.operator))
        writer.print(' ')
        renderStatement(statement.right, writer, indentLevel, StatementPosition.ASSIGN_RIGHT)
    }
    
    private fun renderCall(
        statement: NodeCall,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        statement.target?.let {
            renderStatement(it, writer, indentLevel, StatementPosition.POSTFIX_TARGET)
            writer.print('.')
        }
        writer.print(statement.name)
        statement.generics?.let { writeActualTypeArguments(it, writer) }
        writeActualParameters(statement.parameters, writer, indentLevel)
    }
    
    private fun renderLambda(
        statement: NodeLambda,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        writer.print("{ ")
        writeCommaSeparated(statement.parameters, writer) { (name, type), out ->
            out.print(name)
            type?.let {
                out.print(": ")
                renderType(it, out)
            }
        }
        writer.print(" -> ")
        renderStatement(statement.body, writer, indentLevel + 1, StatementPosition.STANDALONE)
        writer.print(" }")
    }
    
    private fun renderLambdaCall(
        statement: NodeLambdaCall,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        statement.target?.let {
            renderStatement(it, writer, indentLevel, StatementPosition.POSTFIX_TARGET)
            writer.print(".(")
            renderStatement(statement.method, writer, indentLevel, StatementPosition.STANDALONE)
            writer.print(')')
        } ?: run {
            writer.print('(')
            renderStatement(statement.method, writer, indentLevel, StatementPosition.STANDALONE)
            writer.print(')')
        }
        writer.print('(')
        writeCommaSeparated(statement.parameters, writer) { parameter, out ->
            renderStatement(parameter, out, indentLevel, StatementPosition.STANDALONE)
        }
        writer.print(')')
    }
    
    private fun renderIf(
        statement: NodeIf,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        statement.label?.let {
            writer.print('#')
            writer.print(it)
            writer.print(' ')
        }
        writer.print("if (")
        renderStatement(statement.condition, writer, indentLevel, StatementPosition.STANDALONE)
        writer.print(") ")
        renderControlBody(statement.thenBody, writer, indentLevel)
        statement.elseBody?.let {
            writer.print(" else ")
            renderControlBody(it, writer, indentLevel, allowElseIfChain = true)
        }
    }
    
    private fun renderWhen(
        statement: NodeWhen,
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
            renderStatement(it, writer, indentLevel, StatementPosition.STANDALONE)
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
                    renderStatement(condition, out, indentLevel + 1, StatementPosition.STANDALONE)
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
        statement: NodeWhile,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        statement.label?.let {
            writer.print('#')
            writer.print(it)
            writer.print(' ')
        }
        writer.print("while (")
        renderStatement(statement.condition, writer, indentLevel, StatementPosition.STANDALONE)
        writer.print(") ")
        renderControlBody(statement.body, writer, indentLevel)
    }
    
    private fun renderDoWhile(
        statement: NodeDoWhile,
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
        renderStatement(statement.condition, writer, indentLevel, StatementPosition.STANDALONE)
        writer.print(')')
    }
    
    private fun renderGenFor(
        statement: NodeGenFor,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        writer.print("genfor (")
        writer.print(statement.valueName)
        writer.print(": ")
        writer.print(statement.typeName)
        writer.print(" in ")
        renderType(statement.targetType, writer)
        writer.print(") ")
        renderControlBody(statement.body, writer, indentLevel)
    }
    
    private fun renderMethodBody(
        statement: NodeStatement,
        writer: PrintWriter,
    ) {
        if (canonical) renderBlockBody(statement, writer, 0)
        else renderStatement(statement, writer, 0, StatementPosition.STANDALONE)
    }
    
    private fun renderControlBody(
        statement: NodeStatement,
        writer: PrintWriter,
        indentLevel: Int,
        allowElseIfChain: Boolean = false,
    ) {
        when {
            canonical && statement is NodeIf && allowElseIfChain ->
                renderStatement(statement, writer, indentLevel, StatementPosition.STANDALONE)
            
            canonical -> renderBlockBody(statement, writer, indentLevel)
            else -> renderStatement(statement, writer, indentLevel, StatementPosition.STANDALONE)
        }
    }
    
    private fun renderBlockBody(
        statement: NodeStatement,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        if (statement is NodeBlock) {
            renderBlock(statement, writer, indentLevel)
            return
        }
        writer.print("{\n")
        writer.print(indent(indentLevel + 1))
        renderStatement(statement, writer, indentLevel + 1, StatementPosition.STANDALONE)
        writer.print('\n')
        writer.print(indent(indentLevel))
        writer.print('}')
    }
    
    private fun writeTypeParameterNames(
        generics: SequencedSet<String>?,
        writer: PrintWriter,
    ) {
        val values = generics?.takeIf { it.isNotEmpty() } ?: return
        writer.print('<')
        writeCommaSeparated(values, writer) { name, out -> out.print(name) }
        writer.print('>')
    }
    
    private fun writeFormalGenerics(
        generics: SequencedMap<String, NodeGenericConstraint>,
        writer: PrintWriter,
    ) {
        writer.print('<')
        writeCommaSeparated(generics.entries, writer) { (name, constraint), out ->
            out.print(name)
            constraint.match?.let {
                out.print(" = ")
                renderType(it, out)
            }
        }
        writer.print('>')
    }
    
    private fun writeFormalParameters(
        parameters: List<NodeFormalParameter>,
        writer: PrintWriter,
    ) {
        writer.print('(')
        writeCommaSeparated(parameters, writer) { parameter, out ->
            when (parameter) {
                is NodeNamedFormalParameter -> {
                    out.print(parameter.name)
                    out.print(": ")
                    renderType(parameter.type, out)
                }
                is NodeSplatFormalParameter -> {
                    out.print('*')
                    renderType(parameter.type, out)
                }
            }
        }
        writer.print(')')
    }
    
    private fun writeActualParameters(
        parameters: List<Pair<String?, NodeStatement>>,
        writer: PrintWriter,
        indentLevel: Int,
    ) {
        writer.print('(')
        writeCommaSeparated(parameters, writer) { (name, value), out ->
            if (name != null) {
                out.print(name)
                out.print(" = ")
            }
            renderStatement(value, out, indentLevel, StatementPosition.STANDALONE)
        }
        writer.print(')')
    }
    
    private fun writeActualTypeArguments(
        generics: List<Pair<String?, NodeType>>,
        writer: PrintWriter,
    ) {
        if (generics.isEmpty()) return
        writer.print('<')
        writeCommaSeparated(generics, writer) { (name, type), out ->
            if (name != null) {
                out.print(name)
                out.print(" = ")
            }
            renderType(type, out)
        }
        writer.print('>')
    }
    
    private fun renderPrimitiveType(type: FoxPrimitiveType): String = when (type) {
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
    
    private fun renderFormattedString(
        statement: NodeFormattedString,
        writer: PrintWriter,
    ) {
        writer.print(if (statement.isRaw) "rf\"" else "f\"")
        statement.parts.forEach { part ->
            when (part) {
                is NodeFormattedText ->
                    writer.print(if (statement.isRaw) escapeRawFormattedText(part.text) else escapeFormattedText(part.text))
                
                is NodeFormattedExpression -> {
                    writer.print('{')
                    renderStatement(part.expression, writer, 0, StatementPosition.STANDALONE)
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
            is FoxArray -> {
                writer.print('[')
                writeCommaSeparated(entity.elements, writer) { item, out -> renderEntity(item, out) }
                writer.print(']')
            }
            is FoxTuple -> {
                writer.print('(')
                writeCommaSeparated(entity.components, writer) { item, out -> renderEntity(item, out) }
                writer.print(')')
            }
            is FoxStruct -> {
                writer.print("Struct(")
                writeCommaSeparated(entity.fields.entries, writer) { (name, value), out ->
                    out.print(name)
                    out.print(" = ")
                    renderEntity(value, out)
                }
                writer.print(')')
            }
            is FoxEnum -> {
                writer.print(entity.name)
                writer.print('(')
                renderEntity(entity.value, writer)
                writer.print(')')
            }
            is FoxRef -> {
                writer.print("ref(")
                writer.print(entity.referent)
                writer.print(')')
            }
            is FoxLambda -> {
                writer.print("lambda(captured = ")
                renderEntity(entity.captured, writer)
                writer.print(", implementation = ")
                writer.print(entity.implementation)
                writer.print(')')
            }
        }
    }
    
    private fun unaryOperatorText(operator: NodeUnaryOperator): String = when (operator) {
        NodeNotOperator -> "!"
        NodeNegOperator -> "-"
    }
    
    private fun binaryOperatorText(operator: NodeBinaryOperator): String = when (operator) {
        NodeAddOperator -> "+"
        NodeSubOperator -> "-"
        NodeMulOperator -> "*"
        NodeDivOperator -> "/"
        NodeRemOperator -> "%"
        NodeAndOperator -> "&"
        NodeOrOperator -> "|"
        NodeXorOperator -> "^"
        NodeShlOperator -> "<<"
        NodeShrOperator -> ">>"
        NodeUshrOperator -> ">>>"
        NodeEqOperator -> "=="
        NodeNeqOperator -> "!="
        NodeLtOperator -> "<"
        NodeGtOperator -> ">"
        NodeLeOperator -> "<="
        NodeGeOperator -> ">="
        NodeAndAndOperator -> "&&"
        NodeOrOrOperator -> "||"
    }
    
    private fun assignOperatorText(operator: NodeAssignOperator): String = when (operator) {
        NodePlainAssignOperator -> "="
        NodeTypeBindingAssignOperator -> ":="
        NodeAddAssignOperator -> "+="
        NodeSubAssignOperator -> "-="
        NodeMulAssignOperator -> "*="
        NodeDivAssignOperator -> "/="
        NodeRemAssignOperator -> "%="
        NodeAndAssignOperator -> "&="
        NodeOrAssignOperator -> "|="
        NodeXorAssignOperator -> "^="
        NodeAndAndAssignOperator -> "&&="
        NodeOrOrAssignOperator -> "||="
        NodeShlAssignOperator -> "<<="
        NodeShrAssignOperator -> ">>="
        NodeUshrAssignOperator -> ">>>="
    }
    
    private fun precedenceOf(statement: NodeStatement): Int = when (statement) {
        is NodeIf,
        is NodeWhen,
        is NodeWhile,
        is NodeDoWhile,
        is NodeGenFor,
        is NodeBreak,
        is NodeContinue,
        is NodeYield,
        is NodeReturn,
        is NodeBlock,
        is NodeTypeBinding,
        is NodeLambda,
            -> 0
        
        is NodeAssign -> 10
        is NodeBinary -> binaryPrecedence(statement.operator)
        is NodeUnary -> 120
        is NodeFieldAccess,
        is NodeComponentAccess,
        is NodeCall,
        is NodeConstruct,
        is NodeLambdaCall,
            -> 130
        
        is NodeFormattedString,
        is NodeEntity,
        is NodeSymbol,
            -> 140
    }
    
    private fun binaryPrecedence(operator: NodeBinaryOperator): Int = when (operator) {
        NodeOrOrOperator -> 20
        NodeAndAndOperator -> 30
        NodeOrOperator -> 40
        NodeXorOperator -> 50
        NodeAndOperator -> 60
        NodeEqOperator, NodeNeqOperator -> 70
        NodeLtOperator, NodeGtOperator, NodeLeOperator, NodeGeOperator -> 80
        NodeShlOperator, NodeShrOperator, NodeUshrOperator -> 90
        NodeAddOperator, NodeSubOperator -> 100
        NodeMulOperator, NodeDivOperator, NodeRemOperator -> 110
    }
    
    private fun needsParentheses(
        statement: NodeStatement,
        precedence: Int,
        position: StatementPosition,
    ): Boolean {
        val parentPrecedence = position.parentPrecedence
        if (precedence < parentPrecedence) return true
        if (statement is NodeBinary && position is StatementPosition.BinaryRight && precedence == parentPrecedence) return true
        return false
    }
    
    private fun escapeChar(char: Char): String = when (char) {
        '\b' -> "\\b"
        '\t' -> "\\t"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\\' -> "\\\\"
        '\'' -> "\\'"
        else -> if (char.isISOControl()) "\\u%04x".format(char.code) else char.toString()
    }
    
    private fun escapeString(string: String): String = buildString {
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
    
    private fun escapeFormattedText(string: String): String = buildString {
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
    
    private fun escapeRawFormattedText(string: String): String = buildString {
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
    
    private fun indent(level: Int): String = indentUnit.repeat(level)
    
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

private sealed class StatementPosition(
    val parentPrecedence: Int,
) {
    data object Standalone : StatementPosition(0)
    data object UnaryOperand : StatementPosition(120)
    data object PostfixTarget : StatementPosition(130)
    data object AssignLeft : StatementPosition(11)
    data object AssignRight : StatementPosition(10)
    data class BinaryLeft(val precedence: Int) : StatementPosition(precedence)
    data class BinaryRight(val precedence: Int) : StatementPosition(precedence)
    
    companion object {
        val STANDALONE = Standalone
        val UNARY_OPERAND = UnaryOperand
        val POSTFIX_TARGET = PostfixTarget
        val ASSIGN_LEFT = AssignLeft
        val ASSIGN_RIGHT = AssignRight
        
        fun BINARY_LEFT(precedence: Int): StatementPosition = BinaryLeft(precedence)
        
        fun BINARY_RIGHT(precedence: Int): StatementPosition = BinaryRight(precedence)
    }
}
