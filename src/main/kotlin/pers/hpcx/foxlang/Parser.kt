package pers.hpcx.foxlang

class Parser(source: String) {
    
    private val scanner = SourceScanner(source)
    
    fun parseProgram(): NodeProgram {
    
    }
    
    fun parseStatement(): NodeStatement {
        return parseAlsoStatementAfter(parsePrimeStatement())
    }
    
    fun parsePrimeStatement(): NodeStatement {
        tryParseLabel()?.let { label ->
            tryParseBlock(label)?.let { return it }
            tryParseIf(label)?.let { return it }
            tryParseWhile(label)?.let { return it }
            tryParseDoWhile(label)?.let { return it }
            tryParseWhen(label)?.let { return it }
            error("Expected block / if / while / do-while / when")
        }
        tryParseBlock()?.let { return it }
        tryParseIf()?.let { return it }
        tryParseWhile()?.let { return it }
        tryParseDoWhile()?.let { return it }
        tryParseWhen()?.let { return it }
        tryParseBreak()?.let { return it }
        tryParseContinue()?.let { return it }
        tryParseYield()?.let { return it }
        tryParseReturn()?.let { return it }
        tryParseExpressionStatement()?.let { return it }
        error("Expected statement")
    }
    
    fun tryParseLabel(): String? {
        return if (scanner.tryConsume("#")) parseWord() else null
    }
    
    fun tryParseBreak(): NodeBreak? {
        if (!scanner.tryConsume("break")) return null
        val label = tryParseLabel()
        return NodeBreak(label)
    }
    
    fun tryParseContinue(): NodeContinue? {
        if (!scanner.tryConsume("continue")) return null
        val label = tryParseLabel()
        return NodeContinue(label)
    }
    
    fun tryParseYield(): NodeYield? {
        if (!scanner.tryConsume("yield")) return null
        val label = tryParseLabel()
        val value = parseExpression()
        return NodeYield(label, value)
    }
    
    fun tryParseReturn(): NodeReturn? {
        if (!scanner.tryConsume("return")) return null
        val value = parseExpression()
        return NodeReturn(value)
    }
    
    fun tryParseBlock(label: String? = null): NodeBlock? {
        if (!scanner.tryConsume("{")) return null
        val statements = buildList {
            while (!scanner.tryConsume("}")) {
                add(parseStatement())
            }
        }
        return NodeBlock(label, statements)
    }
    
    fun tryParseIf(label: String? = null): NodeIf? {
        if (!scanner.tryConsume("if")) return null
        scanner.consume("(")
        val condition = parseExpression()
        scanner.consume(")")
        val thenBody = parseStatement()
        val elseBody = if (scanner.tryConsume("else")) parseStatement() else null
        return NodeIf(label, condition, thenBody, elseBody)
    }
    
    fun tryParseWhile(label: String? = null): NodeWhile? {
        if (!scanner.tryConsume("while")) return null
        scanner.consume("(")
        val condition = parseExpression()
        scanner.consume(")")
        val body = parseStatement()
        return NodeWhile(label, condition, body)
    }
    
    fun tryParseDoWhile(label: String? = null): NodeDoWhile? {
        if (!scanner.tryConsume("do")) return null
        val body = parseStatement()
        scanner.consume("while")
        scanner.consume("(")
        val condition = parseExpression()
        scanner.consume(")")
        return NodeDoWhile(label, body, condition)
    }
    
    fun tryParseWhen(label: String? = null): NodeWhen? {
        if (!scanner.tryConsume("when")) return null
        
        scanner.consume("(")
        val value = parseExpression()
        scanner.consume(")")
        
        val cases = mutableListOf<NodeCase>()
        var elseBody: NodeStatement? = null
        scanner.consume("{")
        while (true) {
            if (scanner.tryConsume("}")) {
                break
            }
            if (scanner.tryConsume("else")) {
                scanner.consume("->")
                elseBody = parseStatement()
                scanner.consume("}")
                break
            }
            val conditions = mutableListOf<NodeRightExpression>()
            while (true) {
                conditions += parseExpression()
                if (scanner.tryConsume("->")) break
                if (scanner.tryConsume(",")) {
                    if (scanner.tryConsume("->")) break
                } else {
                    error("Expected ',' or '->'")
                }
            }
            val body = parseStatement()
            cases += NodeCase(conditions, body)
        }
        
        return NodeWhen(label, value, cases, elseBody)
    }
    
    fun tryParseExpressionStatement(): NodeStatement? {
        val expression = tryParseExpression() ?: return null
        return if (expression is NodeStatement) expression else error("Expected expression statement")
    }
    
    fun parseAlsoStatementAfter(body: NodeStatement): NodeStatement {
        if (scanner.tryConsume("also")) {
            val also = parseStatement()
            return parseAlsoStatementAfter(NodeAlso(body, also))
        }
        return body
    }
    
    fun parseExpression(): NodeRightExpression {
        return tryParseExpression() ?: error("Expected expression")
    }
    
    fun tryParseExpression(): NodeRightExpression? {
        val first = tryParseUnaryExpression() ?: return null
        
        tryParseAssignOperator()?.let { operator ->
            if (first !is NodeLeftExpression) {
                error("Left side of assignment must be left expression")
            }
            val right = parseExpression()
            return NodeAssign(first, operator, right, beforeEvaluation = true)
        }
        
        val operands = mutableListOf(first)
        val operators = mutableListOf<NodeBinaryOperator>()
        
        while (true) {
            val operator = tryParseBinaryOperator() ?: break
            val right = parseUnaryExpression()
            
            while (
                operators.isNotEmpty() &&
                operators.last().priority <= operator.priority
            ) {
                val rhs = operands.removeAt(operands.lastIndex)
                val lhs = operands.removeAt(operands.lastIndex)
                val top = operators.removeAt(operators.lastIndex)
                operands += NodeBinary(lhs, top, rhs)
            }
            
            operators += operator
            operands += right
        }
        
        while (operators.isNotEmpty()) {
            val rhs = operands.removeAt(operands.lastIndex)
            val lhs = operands.removeAt(operands.lastIndex)
            val top = operators.removeAt(operators.lastIndex)
            operands += NodeBinary(lhs, top, rhs)
        }
        
        return operands.single()
    }
    
    fun parseUnaryExpression(): NodeRightExpression {
        return tryParseUnaryExpression() ?: error("Expected unary expression")
    }
    
    fun tryParseUnaryExpression(): NodeRightExpression? {
        tryParseNumber()?.let {
            return parseChainAfter(it)
        }
        
        tryParseString()?.let {
            return parseChainAfter(it)
        }
        
        if (scanner.tryConsume("(")) {
            val inner = parseExpression()
            scanner.consume(")")
            return parseChainAfter(inner)
        }
        
        if (scanner.tryConsume("++")) {
            val right = parseUnaryExpression()
            if (right !is NodeLeftExpression) {
                error("Expected left expression")
            }
            return NodeAssign(right, NodeAddAssignOperator, NodeEntity(FoxInt(1)), beforeEvaluation = true)
        }
        
        if (scanner.tryConsume("--")) {
            val right = parseUnaryExpression()
            if (right !is NodeLeftExpression) {
                error("Expected left expression")
            }
            return NodeAssign(right, NodeSubAssignOperator, NodeEntity(FoxInt(1)), beforeEvaluation = true)
        }
        
        tryParseUnaryOperator()?.let { operator ->
            val right = parseUnaryExpression()
            return NodeUnary(operator, right)
        }
        
        tryParseWord()?.let { name ->
            tryParseActualParameterList()?.let { parameters ->
                return parseChainAfter(
                    NodeCall(
                        target = NodeEntity(FoxUnit),
                        name = name,
                        generics = emptyList(),
                        parameters = parameters,
                    ),
                )
            }
            
            tryParseActualGenericParameterList()?.let { generics ->
                val parameters = parseActualParameterList()
                return parseChainAfter(
                    NodeCall(
                        target = NodeEntity(FoxUnit),
                        name = name,
                        generics = generics,
                        parameters = parameters,
                    ),
                )
            }
            
            return parseChainAfter(NodeSymbol(name))
        }
        
        return null
    }
    
    fun parseChainAfter(left: NodeRightExpression): NodeRightExpression {
        if (!scanner.tryConsume(".")) {
            if (scanner.tryConsume("++")) {
                if (left !is NodeLeftExpression) {
                    error("Left side of assignment must be left expression")
                }
                return NodeAssign(left, NodeAddAssignOperator, NodeEntity(FoxInt(1)), beforeEvaluation = false)
            }
            
            if (scanner.tryConsume("--")) {
                if (left !is NodeLeftExpression) {
                    error("Left side of assignment must be left expression")
                }
                return NodeAssign(left, NodeSubAssignOperator, NodeEntity(FoxInt(1)), beforeEvaluation = false)
            }
            
            return left
        }
        
        tryParseInteger()?.let { index ->
            return parseChainAfter(
                NodeComponentAccess(
                    target = left,
                    index = index,
                ),
            )
        }
        
        val right = parseWord()
        
        tryParseActualParameterList()?.let { parameters ->
            return parseChainAfter(
                NodeCall(
                    target = left,
                    name = right,
                    generics = emptyList(),
                    parameters = parameters,
                ),
            )
        }
        
        tryParseActualGenericParameterList()?.let { generics ->
            val parameters = parseActualParameterList()
            return parseChainAfter(
                NodeCall(
                    target = left,
                    name = right,
                    generics = generics,
                    parameters = parameters,
                ),
            )
        }
        
        return parseChainAfter(
            NodeFieldAccess(
                target = left,
                name = right,
            ),
        )
    }
    
    fun parseActualParameterList(): List<Pair<String?, NodeRightExpression>> {
        return tryParseActualParameterList() ?: error("Expected actual parameter list")
    }
    
    fun tryParseActualParameterList(): List<Pair<String?, NodeRightExpression>>? {
        if (!scanner.tryConsume("(")) return null
        if (scanner.tryConsume(")")) return emptyList()
        val result = mutableListOf<Pair<String?, NodeRightExpression>>()
        while (true) {
            result += tryParseWordAndEqual() to parseExpression()
            if (scanner.tryConsume(")")) break
            if (scanner.tryConsume(",")) {
                if (scanner.tryConsume(")")) break
            } else {
                error("Expected ',' or ')'")
            }
        }
        return result
    }
    
    fun parseActualGenericParameterList(): List<Pair<String?, NodeType>> {
        return tryParseActualGenericParameterList() ?: error("Expected actual generic parameter list")
    }
    
    fun tryParseActualGenericParameterList(): List<Pair<String?, NodeType>>? {
        if (!scanner.tryConsume("<")) return null
        if (scanner.tryConsume(">")) return emptyList()
        val result = mutableListOf<Pair<String?, NodeType>>()
        while (true) {
            result += tryParseWordAndEqual() to parseType()
            if (scanner.tryConsume(">")) break
            if (scanner.tryConsume(",")) {
                if (scanner.tryConsume(">")) break
            } else {
                error("Expected ',' or '>'")
            }
        }
        return result
    }
    
    fun parseType(): NodeType {
        return when (val prefix = parseWord()) {
            "Unit" -> NodePrimitiveType(FoxUnitType)
            "Bool" -> NodePrimitiveType(FoxBoolType)
            "Byte" -> NodePrimitiveType(FoxByteType)
            "Short" -> NodePrimitiveType(FoxShortType)
            "Int" -> NodePrimitiveType(FoxIntType)
            "Long" -> NodePrimitiveType(FoxLongType)
            "Float" -> NodePrimitiveType(FoxFloatType)
            "Double" -> NodePrimitiveType(FoxDoubleType)
            "Char" -> NodePrimitiveType(FoxCharType)
            "String" -> NodePrimitiveType(FoxStringType)
            "Array" -> {
                val list = parseActualGenericParameterList()
                if (list.size != 1) error("Expected one generic parameter")
                val (name, elementType) = list.first()
                if (name != null) error("Expected anonymous generic")
                NodeArrayType(elementType)
            }
            "Tuple" -> NodeTupleType(
                parseActualGenericParameterList().map { (name, type) ->
                    if (name != null) error("Expected anonymous generic")
                    type
                },
            )
            "Struct" -> NodeStructType(
                buildMap {
                    parseActualGenericParameterList().forEach { (name, type) ->
                        if (name == null) error("Expected named generic")
                        put(name, type)
                    }
                },
            )
            "Enum" -> NodeEnumType(
                buildMap {
                    parseActualGenericParameterList().forEach { (name, type) ->
                        if (name == null) error("Expected named generic")
                        put(name, type)
                    }
                },
            )
            "Ref" -> {
                val list = parseActualGenericParameterList()
                if (list.size != 1) error("Expected one generic parameter")
                val (name, elementType) = list.first()
                if (name != null) error("Expected anonymous generic")
                NodeRefType(elementType)
            }
            else -> NodeNamedType(prefix, tryParseActualGenericParameterList() ?: emptyList())
        }
    }
    
    fun tryParseUnaryOperator(): NodeUnaryOperator? {
        return when {
            scanner.tryConsume("-") -> NodeNegOperator
            scanner.tryConsume("!") -> NodeNotOperator
            else -> null
        }
    }
    
    fun tryParseBinaryOperator(): NodeBinaryOperator? {
        return when {
            scanner.tryConsume(">>>") -> NodeShrUnsignedOperator
            scanner.tryConsume("&&") -> NodeShortCircuitAndOperator
            scanner.tryConsume("||") -> NodeShortCircuitOrOperator
            scanner.tryConsume("<<") -> NodeShlOperator
            scanner.tryConsume(">>") -> NodeShrOperator
            scanner.tryConsume("==") -> NodeEqualOperator
            scanner.tryConsume("!=") -> NodeNotEqualOperator
            scanner.tryConsume("<=") -> NodeLessOrEqualOperator
            scanner.tryConsume(">=") -> NodeGreaterOrEqualOperator
            scanner.tryConsume("+") -> NodeAddOperator
            scanner.tryConsume("-") -> NodeSubOperator
            scanner.tryConsume("*") -> NodeMulOperator
            scanner.tryConsume("/") -> NodeDivOperator
            scanner.tryConsume("%") -> NodeRemOperator
            scanner.tryConsume("&") -> NodeAndOperator
            scanner.tryConsume("|") -> NodeOrOperator
            scanner.tryConsume("^") -> NodeXorOperator
            scanner.tryConsume("<") -> NodeLessOperator
            scanner.tryConsume(">") -> NodeGreaterOperator
            else -> null
        }
    }
    
    fun tryParseAssignOperator(): NodeAssignOperator? {
        return when {
            scanner.tryConsume(">>>=") -> NodeShrUnsignedAssignOperator
            scanner.tryConsume("&&=") -> NodeShortCircuitAndAssignOperator
            scanner.tryConsume("||=") -> NodeShortCircuitOrAssignOperator
            scanner.tryConsume("<<=") -> NodeShlAssignOperator
            scanner.tryConsume(">>=") -> NodeShrAssignOperator
            scanner.tryConsume("+=") -> NodeAddAssignOperator
            scanner.tryConsume("-=") -> NodeSubAssignOperator
            scanner.tryConsume("*=") -> NodeMulAssignOperator
            scanner.tryConsume("/=") -> NodeDivAssignOperator
            scanner.tryConsume("%=") -> NodeRemAssignOperator
            scanner.tryConsume("&=") -> NodeAndAssignOperator
            scanner.tryConsume("|=") -> NodeOrAssignOperator
            scanner.tryConsume("^=") -> NodeXorAssignOperator
            scanner.tryConsume("=") -> NodePlainAssignOperator
            else -> null
        }
    }
    
    fun parseWord(): String {
        return scanner.consumeRegex("[a-zA-Z_][a-zA-Z0-9_]*")
    }
    
    fun tryParseWord(): String? {
        return scanner.tryConsumeRegex("[a-zA-Z_][a-zA-Z0-9_]*")
    }
    
    fun tryParseWordAndEqual(): String? {
        return scanner.tryConsumeRegex("[a-zA-Z_][a-zA-Z0-9_]*", "=")?.first()
    }
    
    fun tryParseNumber(): NodeEntity? {
        scanner.tryConsumeRegex("0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*(\\.[0-9a-fA-F]+(_[0-9a-fA-F]+)*)?p[+-]?[0-9]+f?")?.let { match ->
            return if (match.endsWith('f')) {
                NodeEntity(FoxFloat(match.dropLast(1).replace("_", "").toFloat()))
            } else {
                NodeEntity(FoxDouble(match.replace("_", "").toDouble()))
            }
        }
        
        scanner.tryConsumeRegex("(0|[1-9][0-9]*(_[0-9]+)*)(\\.[0-9]+(_[0-9]+)*)?(e[+-]?[0-9]+)?f?")?.let { match ->
            return if (match.endsWith('f')) {
                NodeEntity(FoxFloat(match.dropLast(1).replace("_", "").toFloat()))
            } else {
                NodeEntity(FoxDouble(match.replace("_", "").toDouble()))
            }
        }
        
        return tryParseInteger()
    }
    
    fun tryParseInteger(): NodeEntity? {
        scanner.tryConsumeRegex("0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*L?")?.let { match ->
            return if (match.endsWith('L')) {
                NodeEntity(FoxLong(match.drop(2).dropLast(1).replace("_", "").toLong(16)))
            } else {
                NodeEntity(FoxInt(match.drop(2).replace("_", "").toInt(16)))
            }
        }
        
        scanner.tryConsumeRegex("0b[01]+(_[01]+)*L?")?.let { match ->
            return if (match.endsWith('L')) {
                NodeEntity(FoxLong(match.drop(2).dropLast(1).replace("_", "").toLong(2)))
            } else {
                NodeEntity(FoxInt(match.drop(2).replace("_", "").toInt(2)))
            }
        }
        
        scanner.tryConsumeRegex("(0|[1-9][0-9]*(_[0-9]+)*)L?")?.let { match ->
            return if (match.endsWith('L')) {
                NodeEntity(FoxLong(match.dropLast(1).replace("_", "").toLong()))
            } else {
                NodeEntity(FoxInt(match.replace("_", "").toInt()))
            }
        }
        
        return null
    }
    
    fun tryParseString(): NodeEntity? {
        val match = scanner.tryConsumeRegex(""""([^"\\]|\\.)*"""") ?: return null
        val result = StringBuilder()
        var index = 1
        while (index < match.lastIndex) {
            val char = match[index++]
            if (char != '\\') {
                result.append(char)
                continue
            }
            
            if (index >= match.lastIndex) {
                error("Unclosed escape sequence")
            }
            
            when (val escaped = match[index++]) {
                'b' -> result.append('\b')
                't' -> result.append('\t')
                'n' -> result.append('\n')
                'r' -> result.append('\r')
                '"' -> result.append('"')
                '\'' -> result.append('\'')
                '\\' -> result.append('\\')
                else -> error("Unknown escape sequence '\\$escaped'")
            }
        }
        return NodeEntity(FoxString(result.toString()))
    }
    
    private fun error(message: String): Nothing {
        throw ParserException(scanner.pos, message)
    }
}

class ParserException(val pos: SourceScanner.Position, message: String) : Exception(message)

class SourceScanner(source: String) {
    val lines = source.lines()
    
    data class Position(val row: Int, val col: Int) {
        init {
            check(row >= 0)
            check(col >= 0)
        }
    }
    
    var pos = Position(0, 0)
        private set
    
    private val regexCache = mutableMapOf<String, Regex>()
    
    private fun internalMove(numChars: Int) {
        check(numChars >= 0)
        check(pos.row < lines.size)
        val line = lines[pos.row]
        val col = pos.col + numChars
        check(col <= line.length)
        pos = (if (col < line.length) Position(pos.row, col) else Position(pos.row + 1, 0))
    }
    
    private fun internalMatch(string: String): Boolean {
        return pos.row < lines.size && lines[pos.row].startsWith(string, pos.col)
    }
    
    private fun internalMatchRegex(regexString: String): String? {
        if (pos.row >= lines.size) return null
        val line = lines[pos.row]
        val regex = regexCache.getOrPut(regexString) { Regex(regexString) }
        val match = regex.matchAt(line, pos.col)
        return match?.value
    }
    
    private fun skipBlanksAndComments() {
        while (pos.row < lines.size) {
            if (pos.col >= lines[pos.row].length) {
                internalMove(0)
                continue
            }
            internalMatchRegex("\\s+")?.let {
                internalMove(it.length)
                continue
            }
            if (internalMatch("//")) {
                internalMove(lines[pos.row].length - pos.col)
                continue
            }
            if (internalMatch("/*")) {
                val commentStart = pos
                internalMove(2)
                while (true) {
                    if (pos.row >= lines.size) {
                        throw ParserException(commentStart, "Unclosed block comment")
                    }
                    val index = lines[pos.row].indexOf("*/", pos.col)
                    if (index >= 0) {
                        internalMove(index - pos.col + 2)
                        break
                    }
                    internalMove(lines[pos.row].length - pos.col)
                }
                continue
            }
            return
        }
    }
    
    fun tryConsume(string: String): Boolean {
        skipBlanksAndComments()
        if (internalMatch(string)) {
            internalMove(string.length)
            return true
        }
        return false
    }
    
    fun tryConsumeRegex(regexString: String): String? {
        skipBlanksAndComments()
        val match = internalMatchRegex(regexString)
        if (match != null) {
            internalMove(match.length)
            return match
        }
        return null
    }
    
    fun tryConsume(vararg strings: String): Boolean {
        skipBlanksAndComments()
        val startPos = pos
        val result = strings.all { tryConsume(it) }
        if (!result) pos = startPos
        return result
    }
    
    fun tryConsumeRegex(vararg regexStrings: String): List<String>? {
        skipBlanksAndComments()
        val startPos = pos
        val result = buildList {
            regexStrings.forEach {
                val match = tryConsumeRegex(it)
                if (match != null) add(match)
                else {
                    pos = startPos
                    return null
                }
            }
        }
        return result
    }
    
    fun consume(string: String) {
        if (!tryConsume(string)) throw ParserException(pos, "Expected '$string'")
    }
    
    fun consumeRegex(regexString: String): String {
        return tryConsumeRegex(regexString) ?: throw ParserException(pos, "Expected match to '$regexString'")
    }
}
