package pers.hpcx.foxlang

class Parser(source: String) {
    
    private val scanner = SourceScanner(source)
    
    fun parseProgram(): NodeProgram {
    
    }
    
    private fun parseExpression(): NodeRightExpression {
        val first = parseUnaryExpression()
        
        parseOptionalAssignOperator()?.let { operator ->
            if (first !is NodeLeftExpression) {
                error("Left side of assignment must be left expression")
            }
            val right = parseExpression()
            return NodeAssign(first, operator, right, beforeEvaluation = true)
        }
        
        val operands = mutableListOf(first)
        val operators = mutableListOf<NodeBinaryOperator>()
        
        while (true) {
            val operator = parseOptionalBinaryOperator() ?: break
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
        parseOptionalNumber()?.let {
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
        
        parseOptionalUnaryOperator()?.let { operator ->
            val right = parseUnaryExpression()
            return NodeUnary(operator, right)
        }
        
        parseOptionalWord()?.let { name ->
            if (scanner.match("(")) {
                val parameters = parseActualParameterList()
                return parseChainAfter(
                    NodeCall(
                        target = NodeEntity(FoxUnit),
                        name = name,
                        generics = emptyList(),
                        parameters = parameters,
                    ),
                )
            }
            
            if (scanner.match("<")) {
                val generics = parseActualGenericParameterList()
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
        
        error("Expected unary expression")
    }
    
    fun parseChainAfter(left: NodeRightExpression): NodeRightExpression {
        if (!scanner.tryConsume(".")) {
            if (left !is NodeLeftExpression) {
                return left
            }
            if (scanner.tryConsume("++")) {
                return NodeAssign(left, NodeAddAssignOperator, NodeEntity(FoxInt(1)), beforeEvaluation = false)
            }
            if (scanner.tryConsume("--")) {
                return NodeAssign(left, NodeSubAssignOperator, NodeEntity(FoxInt(1)), beforeEvaluation = false)
            }
            return left
        }
        
        val right = parseWord()
        
        if (scanner.match("(")) {
            val parameters = parseActualParameterList()
            return parseChainAfter(
                NodeCall(
                    target = left,
                    name = right,
                    generics = emptyList(),
                    parameters = parameters,
                ),
            )
        }
        
        if (scanner.match("<")) {
            val generics = parseActualGenericParameterList()
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
        scanner.consume("(")
        if (scanner.tryConsume(")")) return emptyList()
        val result = mutableListOf<Pair<String?, NodeRightExpression>>()
        while (true) {
            result += parseOptionalWordAndEqual() to parseExpression()
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
        if (!scanner.tryConsume("<")) return emptyList()
        if (scanner.tryConsume(">")) return emptyList()
        val result = mutableListOf<Pair<String?, NodeType>>()
        while (true) {
            result += parseOptionalWordAndEqual() to parseType()
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
            else -> NodeNamedType(prefix, parseActualGenericParameterList())
        }
    }
    
    fun parseOptionalUnaryOperator(): NodeUnaryOperator? {
        return when {
            scanner.tryConsume("-") -> NodeNegOperator
            scanner.tryConsume("!") -> NodeNotOperator
            else -> null
        }
    }
    
    fun parseOptionalBinaryOperator(): NodeBinaryOperator? {
        return when {
            scanner.tryConsume("+") -> NodeAddOperator
            scanner.tryConsume("-") -> NodeSubOperator
            scanner.tryConsume("*") -> NodeMulOperator
            scanner.tryConsume("/") -> NodeDivOperator
            scanner.tryConsume("%") -> NodeRemOperator
            scanner.tryConsume("&") -> NodeAndOperator
            scanner.tryConsume("|") -> NodeOrOperator
            scanner.tryConsume("^") -> NodeXorOperator
            scanner.tryConsume("&&") -> NodeShortCircuitAndOperator
            scanner.tryConsume("||") -> NodeShortCircuitOrOperator
            scanner.tryConsume("<<") -> NodeShlOperator
            scanner.tryConsume(">>") -> NodeShrOperator
            scanner.tryConsume(">>>") -> NodeShrUnsignedOperator
            scanner.tryConsume("==") -> NodeEqualOperator
            scanner.tryConsume("!=") -> NodeNotEqualOperator
            scanner.tryConsume("<") -> NodeLessOperator
            scanner.tryConsume(">") -> NodeGreaterOperator
            scanner.tryConsume("<=") -> NodeLessOrEqualOperator
            scanner.tryConsume(">=") -> NodeGreaterOrEqualOperator
            else -> null
        }
    }
    
    fun parseOptionalAssignOperator(): NodeAssignOperator? {
        return when {
            scanner.tryConsume("=") -> NodePlainAssignOperator
            scanner.tryConsume("+=") -> NodeAddAssignOperator
            scanner.tryConsume("-=") -> NodeSubAssignOperator
            scanner.tryConsume("*=") -> NodeMulAssignOperator
            scanner.tryConsume("/=") -> NodeDivAssignOperator
            scanner.tryConsume("%=") -> NodeRemAssignOperator
            scanner.tryConsume("&=") -> NodeAndAssignOperator
            scanner.tryConsume("|=") -> NodeOrAssignOperator
            scanner.tryConsume("^=") -> NodeXorAssignOperator
            scanner.tryConsume("&&=") -> NodeShortCircuitAndAssignOperator
            scanner.tryConsume("||=") -> NodeShortCircuitOrAssignOperator
            scanner.tryConsume("<<=") -> NodeShlAssignOperator
            scanner.tryConsume(">>=") -> NodeShrAssignOperator
            scanner.tryConsume(">>>=") -> NodeShrUnsignedAssignOperator
            else -> null
        }
    }
    
    fun parseSymbol(): NodeSymbol {
        return NodeSymbol(parseWord())
    }
    
    fun parseOptionalSymbol(): NodeSymbol? {
        return parseOptionalWord()?.let { NodeSymbol(it) }
    }
    
    fun parseWord(): String {
        return scanner.consumeRegex("[a-zA-Z_][a-zA-Z0-9_]*")
    }
    
    fun parseOptionalWord(): String? {
        return scanner.tryConsumeRegex("[a-zA-Z_][a-zA-Z0-9_]*")
    }
    
    fun parseOptionalWordAndEqual(): String? {
        return scanner.tryConsumeRegex("[a-zA-Z_][a-zA-Z0-9_]*", "=")?.first()
    }
    
    fun parseOptionalNumber(): NodeEntity? {
        val hexMatch = scanner.tryConsumeRegex("0x([0-9a-fA-F]+|[0-9a-fA-F]+\\.[0-9a-fA-F]+)(p[+-]?[0-9]+)?f?")
        if (hexMatch != null) {
            val hasFloatSuffix = hexMatch.endsWith("f")
            val hasExponent = hexMatch.contains('p')
            val hasDot = hexMatch.contains('.')
            
            if (hasFloatSuffix || hasExponent || hasDot) {
                val numberStr = if (hasFloatSuffix) hexMatch.dropLast(1) else hexMatch
                return NodeEntity(FoxDouble(numberStr.toDouble()))
            } else {
                return NodeEntity(FoxLong(hexMatch.toLong(16)))
            }
        }
        
        val binMatch = scanner.tryConsumeRegex("0b[01]+L?")
        if (binMatch != null) {
            val hasLongSuffix = binMatch.endsWith("L")
            val numberStr = if (hasLongSuffix) binMatch.dropLast(1) else binMatch
            val value = numberStr.drop(2).toLong(2)
            return if (hasLongSuffix) NodeEntity(FoxLong(value)) else NodeEntity(FoxInt(value.toInt()))
        }
        
        val decMatch = scanner.tryConsumeRegex("([0-9]+|[0-9]+\\.[0-9]+)(e[+-]?[0-9]+)?[fL]?")
        if (decMatch != null) {
            val hasLongSuffix = decMatch.endsWith("L")
            val hasFloatSuffix = decMatch.endsWith("f")
            val hasExponent = decMatch.contains('e')
            val hasDot = decMatch.contains('.')
            
            if (hasFloatSuffix || hasExponent || hasDot) {
                val numberStr = if (hasFloatSuffix) decMatch.dropLast(1) else decMatch
                return NodeEntity(FoxDouble(numberStr.toDouble()))
            } else {
                val numberStr = if (hasLongSuffix) decMatch.dropLast(1) else decMatch
                val value = numberStr.toLong()
                return if (hasLongSuffix) NodeEntity(FoxLong(value)) else NodeEntity(FoxInt(value.toInt()))
            }
        }
        
        return null
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
        check(numChars > 0)
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
        while (true) {
            if (internalMatch(" ") || internalMatch("\t")) {
                internalMove(1)
                continue
            }
            if (internalMatch("//")) {
                internalMove(lines[pos.row].length - pos.col)
                continue
            }
            if (internalMatch("/*")) {
                internalMove(2)
                while (true) {
                    if (pos.row >= lines.size) {
                        throw ParserException(pos, "Unclosed block comment")
                    }
                    if (internalMatch("*/")) {
                        internalMove(2)
                        break
                    }
                    internalMove(1)
                }
                continue
            }
            return
        }
    }
    
    fun match(string: String): Boolean {
        skipBlanksAndComments()
        return internalMatch(string)
    }
    
    fun matchRegex(regexString: String): String? {
        skipBlanksAndComments()
        return internalMatchRegex(regexString)
    }
    
    fun match(vararg strings: String): Boolean {
        skipBlanksAndComments()
        val startPos = pos
        val result = strings.all { tryConsume(it) }
        pos = startPos
        return result
    }
    
    fun matchRegex(vararg regexStrings: String): List<String>? {
        skipBlanksAndComments()
        val startPos = pos
        try {
            val result = buildList {
                regexStrings.forEach {
                    val match = tryConsumeRegex(it)
                    if (match != null) add(match)
                    else return null
                }
            }
            return result
        } finally {
            pos = startPos
        }
    }
    
    fun tryConsume(string: String): Boolean {
        if (match(string)) {
            internalMove(string.length)
            return true
        }
        return false
    }
    
    fun tryConsume(vararg strings: String): Boolean {
        skipBlanksAndComments()
        val startPos = pos
        val result = strings.all { tryConsume(it) }
        if (!result) pos = startPos
        return result
    }
    
    fun consume(string: String) {
        if (!tryConsume(string)) throw ParserException(pos, "Expected '$string'")
    }
    
    fun consume(vararg strings: String) {
        strings.forEach { consume(it) }
    }
    
    fun tryConsumeRegex(regexString: String): String? {
        return matchRegex(regexString)?.also { internalMove(it.length) }
    }
    
    fun tryConsumeRegex(vararg regexString: String): List<String>? {
        skipBlanksAndComments()
        val startPos = pos
        val result = buildList {
            regexString.forEach {
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
    
    fun consumeRegex(regexString: String): String {
        return tryConsumeRegex(regexString) ?: throw ParserException(pos, "Expected match to '$regexString'")
    }
}
