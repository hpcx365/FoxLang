package pers.hpcx.foxlang

import java.util.*

fun NodeFile.toSource(indent: String = "    "): String = AstSourcePrinter(indent).renderFile(this)

fun NodeFileElement.toSource(indent: String = "    "): String = AstSourcePrinter(indent).renderFileElement(this)

fun NodeType.toSource(): String = AstSourcePrinter().renderType(this)

fun NodeStatement.toSource(indent: String = "    "): String = AstSourcePrinter(indent).renderStatement(this)

private class AstSourcePrinter(
    private val indentUnit: String = "    ",
) {
    fun renderFile(file: NodeFile): String = file.elements.joinToString("\n\n") { renderFileElement(it) }
    
    fun renderFileElement(element: NodeFileElement): String = when (element) {
        is NodeTypeAlias -> buildString {
            append("type ")
            append(element.name)
            append(renderTypeParameterNames(element.generics))
            append(" = ")
            append(renderType(element.alias))
        }
        
        is NodeMethodDefinition -> buildString {
            append("def ")
            element.generics?.takeIf { it.isNotEmpty() }?.let {
                append(renderFormalGenerics(it))
                append(' ')
            }
            element.thisType?.let {
                append(renderType(it))
                append('.')
            }
            append(element.name)
            append(renderFormalParameters(element.parameters))
            element.returnType?.let {
                append(": ")
                append(renderType(it))
            }
            append(' ')
            append(renderStatement(element.body))
        }
    }
    
    fun renderType(type: NodeType): String = when (type) {
        is NodePrimitiveType -> renderPrimitiveType(type.type)
        is NodeNamedType -> type.name + renderActualTypeArguments(type.generics)
        is NodeArrayType -> "Array<${renderType(type.elementType)}>"
        is NodeTupleType -> "Tuple<${type.componentTypes.joinToString(", ") { renderType(it) }}>"
        is NodeStructType -> "Struct<${type.fields.entries.joinToString(", ") { "${it.key}: ${renderType(it.value)}" }}>"
        is NodeEnumType -> "Enum<${type.items.entries.joinToString(", ") { "${it.key} = ${renderType(it.value)}" }}>"
        is NodeRefType -> "Ref<${renderType(type.referentType)}>"
        is NodeLambdaType -> {
            val arguments = buildList {
                add(type.thisType)
                addAll(type.parameters)
                add(type.returnType)
            }
            "Lambda<${arguments.joinToString(", ") { renderType(it) }}>"
        }
    }
    
    fun renderStatement(statement: NodeStatement): String = renderStatement(statement, 0, StatementPosition.STANDALONE)
    
    private fun renderStatement(
        statement: NodeStatement,
        indentLevel: Int,
        position: StatementPosition,
    ): String {
        val precedence = precedenceOf(statement)
        val raw = when (statement) {
            is NodeEntity -> renderEntity(statement.value)
            is NodeFormattedString -> renderFormattedString(statement)
            is NodeSymbol -> statement.name
            is NodeBlock -> renderBlock(statement, indentLevel)
            is NodeUnary -> renderUnary(statement, indentLevel)
            is NodeBinary -> renderBinary(statement, indentLevel)
            is NodeAssign -> renderAssign(statement, indentLevel)
            is NodeTypeBinding -> "${statement.name}: ${renderType(statement.type)}"
            is NodeFieldAccess -> "${renderStatement(statement.target, indentLevel, StatementPosition.POSTFIX_TARGET)}.${statement.name}"
            is NodeComponentAccess -> "${renderStatement(statement.target, indentLevel, StatementPosition.POSTFIX_TARGET)}.${statement.index}"
            is NodeCall -> renderCall(statement, indentLevel)
            is NodeConstruct -> renderType(statement.type) + renderActualParameters(statement.parameters, indentLevel)
            is NodeLambda -> renderLambda(statement, indentLevel)
            is NodeLambdaCall -> renderLambdaCall(statement, indentLevel)
            is NodeIf -> renderIf(statement, indentLevel)
            is NodeWhen -> renderWhen(statement, indentLevel)
            is NodeWhile -> renderWhile(statement, indentLevel)
            is NodeDoWhile -> renderDoWhile(statement, indentLevel)
            is NodeBreak -> "break" + statement.label?.let { " #$it" }.orEmpty()
            is NodeContinue -> "continue" + statement.label?.let { " #$it" }.orEmpty()
            is NodeYield -> "yield" + statement.label?.let { " #$it" }.orEmpty() + " " + renderStatement(statement.value, indentLevel, StatementPosition.STANDALONE)
            is NodeReturn -> "return" + statement.value?.let { " ${renderStatement(it, indentLevel, StatementPosition.STANDALONE)}" }.orEmpty()
        }
        return if (needsParentheses(statement, precedence, position)) "($raw)" else raw
    }
    
    private fun renderBlock(
        block: NodeBlock,
        indentLevel: Int,
    ): String {
        val prefix = block.label?.let { "#$it " }.orEmpty()
        if (block.statements.isEmpty()) return "${prefix}{}"
        val inner = block.statements.joinToString("\n") {
            indent(indentLevel + 1) + renderStatement(it, indentLevel + 1, StatementPosition.STANDALONE)
        }
        return buildString {
            append(prefix)
            appendLine("{")
            appendLine(inner)
            append(indent(indentLevel))
            append("}")
        }
    }
    
    private fun renderUnary(
        statement: NodeUnary,
        indentLevel: Int,
    ): String = unaryOperatorText(statement.operator) +
        renderStatement(statement.right, indentLevel, StatementPosition.UNARY_OPERAND)
    
    private fun renderBinary(
        statement: NodeBinary,
        indentLevel: Int,
    ): String = buildString {
        append(renderStatement(statement.left, indentLevel, StatementPosition.BINARY_LEFT(binaryPrecedence(statement.operator))))
        append(' ')
        append(binaryOperatorText(statement.operator))
        append(' ')
        append(renderStatement(statement.right, indentLevel, StatementPosition.BINARY_RIGHT(binaryPrecedence(statement.operator))))
    }
    
    private fun renderAssign(
        statement: NodeAssign,
        indentLevel: Int,
    ): String = buildString {
        append(renderStatement(statement.left, indentLevel, StatementPosition.ASSIGN_LEFT))
        append(' ')
        append(assignOperatorText(statement.operator))
        append(' ')
        append(renderStatement(statement.right, indentLevel, StatementPosition.ASSIGN_RIGHT))
    }
    
    private fun renderCall(
        statement: NodeCall,
        indentLevel: Int,
    ): String = buildString {
        statement.target?.let {
            append(renderStatement(it, indentLevel, StatementPosition.POSTFIX_TARGET))
            append('.')
        }
        append(statement.name)
        statement.generics?.let { append(renderActualTypeArguments(it)) }
        append(renderActualParameters(statement.parameters, indentLevel))
    }
    
    private fun renderLambda(
        statement: NodeLambda,
        indentLevel: Int,
    ): String = buildString {
        append("{ ")
        append(
            statement.parameters.joinToString(", ") { (name, type) ->
                if (type == null) name else "$name: ${renderType(type)}"
            },
        )
        append(" -> ")
        append(renderStatement(statement.body, indentLevel + 1, StatementPosition.STANDALONE))
        append(" }")
    }
    
    private fun renderLambdaCall(
        statement: NodeLambdaCall,
        indentLevel: Int,
    ): String = buildString {
        statement.target?.let {
            append(renderStatement(it, indentLevel, StatementPosition.POSTFIX_TARGET))
            append(".(")
            append(renderStatement(statement.method, indentLevel, StatementPosition.STANDALONE))
            append(')')
        } ?: run {
            append('(')
            append(renderStatement(statement.method, indentLevel, StatementPosition.STANDALONE))
            append(')')
        }
        append('(')
        append(statement.parameters.joinToString(", ") { renderStatement(it, indentLevel, StatementPosition.STANDALONE) })
        append(')')
    }
    
    private fun renderIf(
        statement: NodeIf,
        indentLevel: Int,
    ): String = buildString {
        statement.label?.let { append("#$it ") }
        append("if (")
        append(renderStatement(statement.condition, indentLevel, StatementPosition.STANDALONE))
        append(") ")
        append(renderControlBody(statement.thenBody, indentLevel))
        statement.elseBody?.let {
            append(" else ")
            append(renderControlBody(it, indentLevel))
        }
    }
    
    private fun renderWhen(
        statement: NodeWhen,
        indentLevel: Int,
    ): String = buildString {
        statement.label?.let { append("#$it ") }
        append("when")
        statement.value?.let {
            append(" (")
            append(renderStatement(it, indentLevel, StatementPosition.STANDALONE))
            append(')')
        }
        if (statement.cases.isEmpty()) {
            append(" {}")
            return@buildString
        }
        appendLine(" {")
        statement.cases.forEachIndexed { index, case ->
            append(indent(indentLevel + 1))
            append(
                if (case.conditions.isEmpty()) {
                    "else"
                } else {
                    case.conditions.joinToString(", ") {
                        renderStatement(it, indentLevel + 1, StatementPosition.STANDALONE)
                    }
                },
            )
            append(" -> ")
            append(renderControlBody(case.body, indentLevel + 1))
            if (index != statement.cases.lastIndex) appendLine()
        }
        appendLine()
        append(indent(indentLevel))
        append("}")
    }
    
    private fun renderWhile(
        statement: NodeWhile,
        indentLevel: Int,
    ): String = buildString {
        statement.label?.let { append("#$it ") }
        append("while (")
        append(renderStatement(statement.condition, indentLevel, StatementPosition.STANDALONE))
        append(") ")
        append(renderControlBody(statement.body, indentLevel))
    }
    
    private fun renderDoWhile(
        statement: NodeDoWhile,
        indentLevel: Int,
    ): String = buildString {
        statement.label?.let { append("#$it ") }
        append("do ")
        append(renderControlBody(statement.body, indentLevel))
        append(" while (")
        append(renderStatement(statement.condition, indentLevel, StatementPosition.STANDALONE))
        append(")")
    }
    
    private fun renderControlBody(
        statement: NodeStatement,
        indentLevel: Int,
    ): String = when (statement) {
        is NodeIf,
        is NodeWhen,
        is NodeWhile,
        is NodeDoWhile,
        is NodeBlock,
            -> renderStatement(statement, indentLevel, StatementPosition.STANDALONE)
        
        else -> renderStatement(statement, indentLevel, StatementPosition.STANDALONE)
    }
    
    private fun renderTypeParameterNames(generics: SequencedSet<String>?): String =
        generics?.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") ?: ""
    
    private fun renderFormalGenerics(generics: SequencedMap<String, NodeGenericConstraint>): String =
        generics.entries.joinToString(prefix = "<", postfix = ">") { (name, constraint) ->
            constraint.match?.let { "$name = ${renderType(it)}" } ?: name
        }
    
    private fun renderFormalParameters(parameters: SequencedMap<String, NodeType>): String =
        parameters.entries.joinToString(prefix = "(", postfix = ")") { (name, type) ->
            "$name: ${renderType(type)}"
        }
    
    private fun renderActualParameters(
        parameters: List<Pair<String?, NodeStatement>>,
        indentLevel: Int,
    ): String = parameters.joinToString(prefix = "(", postfix = ")") { (name, value) ->
        val rendered = renderStatement(value, indentLevel, StatementPosition.STANDALONE)
        if (name == null) rendered else "$name = $rendered"
    }
    
    private fun renderActualTypeArguments(generics: List<Pair<String?, NodeType>>): String =
        generics.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") { (name, type) ->
            val rendered = renderType(type)
            if (name == null) rendered else "$name = $rendered"
        } ?: ""
    
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
    
    private fun renderFormattedString(statement: NodeFormattedString): String = buildString {
        append(if (statement.isRaw) "rf\"" else "f\"")
        statement.parts.forEach { part ->
            when (part) {
                is NodeFormattedText -> append(
                    if (statement.isRaw) escapeRawFormattedText(part.text) else escapeFormattedText(part.text),
                )
                is NodeFormattedExpression -> {
                    append('{')
                    append(renderStatement(part.expression, 0, StatementPosition.STANDALONE))
                    append('}')
                }
            }
        }
        append('"')
    }
    
    private fun renderEntity(entity: FoxEntity): String = when (entity) {
        FoxUnit -> "unit"
        is FoxBool -> entity.value.toString()
        is FoxByte -> entity.value.toString()
        is FoxShort -> entity.value.toString()
        is FoxInt -> entity.value.toString()
        is FoxLong -> "${entity.value}L"
        is FoxFloat -> "${entity.value}f"
        is FoxDouble -> entity.value.toString()
        is FoxChar -> "'${escapeChar(entity.value)}'"
        is FoxString -> "\"${escapeString(entity.value)}\""
        is FoxArray -> "[${entity.elements.joinToString(", ") { renderEntity(it) }}]"
        is FoxTuple -> "(${entity.components.joinToString(", ") { renderEntity(it) }})"
        is FoxStruct -> "Struct(${entity.fields.entries.joinToString(", ") { "${it.key} = ${renderEntity(it.value)}" }})"
        is FoxEnum -> "${entity.name}(${renderEntity(entity.value)})"
        is FoxRef -> "ref(${entity.referent})"
        is FoxLambda -> "lambda(captured = ${renderEntity(entity.captured)}, implementation = ${entity.implementation})"
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
