package pers.hpcx.foxlang

import java.util.*

val CamelCaseParser: Parser<String> = regex("[a-z][a-zA-Z0-9]*").memoized()
val PascalCaseParser: Parser<String> = regex("[A-Z][a-zA-Z0-9]*").memoized()
val CamelCaseAndEqualParser: Parser<String> = composed(CamelCaseParser, fixed("=")) { name, _ -> Success(name) }
val CamelCaseAndColonParser: Parser<String> = composed(CamelCaseParser, fixed(":")) { name, _ -> Success(name) }
val PascalCaseAndEqualParser: Parser<String> = composed(PascalCaseParser, fixed("=")) { name, _ -> Success(name) }
val PascalCaseAndColonParser: Parser<String> = composed(PascalCaseParser, fixed(":")) { name, _ -> Success(name) }

val IntegerParser: Parser<NodeEntity> = parallel(
    regex("0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*L?").map { match ->
        try {
            if (match.endsWith('L')) {
                Success(NodeEntity(FoxLong(match.drop(2).dropLast(1).replace("_", "").toLong(16))))
            } else {
                Success(NodeEntity(FoxInt(match.drop(2).replace("_", "").toInt(16))))
            }
        } catch (e: Exception) {
            Failure { "Invalid number: $match, cause: ${e.message}" }
        }
    },
    regex("0b[01]+(_[01]+)*L?").map { match ->
        try {
            if (match.endsWith('L')) {
                Success(NodeEntity(FoxLong(match.drop(2).dropLast(1).replace("_", "").toLong(2))))
            } else {
                Success(NodeEntity(FoxInt(match.drop(2).replace("_", "").toInt(2))))
            }
        } catch (e: Exception) {
            Failure { "Invalid number: $match, cause: ${e.message}" }
        }
    },
    regex("(0|[1-9][0-9]*(_[0-9]+)*)L?").map { match ->
        try {
            if (match.endsWith('L')) {
                Success(NodeEntity(FoxLong(match.dropLast(1).replace("_", "").toLong())))
            } else {
                Success(NodeEntity(FoxInt(match.replace("_", "").toInt())))
            }
        } catch (e: Exception) {
            Failure { "Invalid number: $match, cause: ${e.message}" }
        }
    },
).memoized()

val NumberParser: Parser<NodeEntity> = parallel(
    regex("0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*(\\.[0-9a-fA-F]+(_[0-9a-fA-F]+)*)?p[+-]?[0-9]+f?").map { match ->
        try {
            Success(match.toFloatingEntity())
        } catch (e: Exception) {
            Failure { "Invalid number: $match, cause: ${e.message}" }
        }
    },
    regex("(0|[1-9][0-9]*(_[0-9]+)*)(\\.[0-9]+(_[0-9]+)*)?(e[+-]?[0-9]+)?f?").map { match ->
        try {
            Success(match.toFloatingEntity())
        } catch (e: Exception) {
            Failure { "Invalid number: $match, cause: ${e.message}" }
        }
    },
    IntegerParser,
).memoized()

val StringParser: Parser<NodeEntity> = regex(""""([^"\\]|\\.)*"""").map { match ->
    val result = StringBuilder()
    var index = 1
    while (index < match.lastIndex) {
        val char = match[index++]
        if (char != '\\') {
            result.append(char)
            continue
        }
        if (index >= match.lastIndex) {
            Failure { "Unclosed escape sequence" }
        }
        when (val escaped = match[index++]) {
            'b' -> result.append('\b')
            't' -> result.append('\t')
            'n' -> result.append('\n')
            'r' -> result.append('\r')
            '"' -> result.append('"')
            '\'' -> result.append('\'')
            '\\' -> result.append('\\')
            else -> Failure { "Unknown escape sequence '\\$escaped'" }
        }
    }
    Success(NodeEntity(FoxString(result.toString())))
}.memoized()

val SymbolParser: Parser<NodeSymbol> = CamelCaseParser.map { Success(NodeSymbol(it)) }

val UnaryParser: Parser<NodeUnary> = parallel(
    composed(fixed("-"), lazy { UnaryExpressionParser }) { _, right -> Success(NodeUnary(NodeNegOperator, right)) },
    composed(fixed("!"), lazy { UnaryExpressionParser }) { _, right -> Success(NodeUnary(NodeNotOperator, right)) },
).memoized()

val ParenthesizedExpressionParser: Parser<NodeRightExpression> = composed(
    fixed("("),
    lazy { ExpressionParser },
    fixed(")"),
) { _, node, _ -> Success(node) }.memoized()

val UnaryExpressionParser: Parser<NodeRightExpression> = parallel(
    NumberParser,
    StringParser,
    SymbolParser,
    UnaryParser,
    ParenthesizedExpressionParser,
    lazy { BlockParser },
    lazy { IfParser },
    lazy { WhenParser },
    lazy { UnitCallParser },
    lazy { ConstructParser },
).memoized().tail(lazy { ExpressionTailParser })

val BinaryParserP9: Parser<NodeBinary> = parallel(
    composed(UnaryExpressionParser, fixed("*"), UnaryExpressionParser) { left, _, right -> Success(NodeBinary(left, NodeMulOperator, right)) },
    composed(UnaryExpressionParser, fixed("/"), UnaryExpressionParser) { left, _, right -> Success(NodeBinary(left, NodeDivOperator, right)) },
    composed(UnaryExpressionParser, fixed("%"), UnaryExpressionParser) { left, _, right -> Success(NodeBinary(left, NodeRemOperator, right)) },
).memoized()

val BinaryParserP8: Parser<NodeBinary> = parallel(
    BinaryParserP9,
    composed(BinaryParserP9, fixed("+"), BinaryParserP9) { left, _, right -> Success(NodeBinary(left, NodeAddOperator, right)) },
    composed(BinaryParserP9, fixed("-"), BinaryParserP9) { left, _, right -> Success(NodeBinary(left, NodeSubOperator, right)) },
).memoized()

val BinaryParserP7: Parser<NodeBinary> = parallel(
    BinaryParserP8,
    composed(BinaryParserP8, fixed(">>>"), BinaryParserP8) { left, _, right -> Success(NodeBinary(left, NodeUshrOperator, right)) },
    composed(BinaryParserP8, fixed("<<"), BinaryParserP8) { left, _, right -> Success(NodeBinary(left, NodeShlOperator, right)) },
    composed(BinaryParserP8, fixed(">>"), BinaryParserP8) { left, _, right -> Success(NodeBinary(left, NodeShrOperator, right)) },
).memoized()

val BinaryParserP6: Parser<NodeBinary> = parallel(
    BinaryParserP7,
    composed(BinaryParserP7, fixed("<="), BinaryParserP7) { left, _, right -> Success(NodeBinary(left, NodeLeOperator, right)) },
    composed(BinaryParserP7, fixed(">="), BinaryParserP7) { left, _, right -> Success(NodeBinary(left, NodeGeOperator, right)) },
    composed(BinaryParserP7, fixed("<"), BinaryParserP7) { left, _, right -> Success(NodeBinary(left, NodeLtOperator, right)) },
    composed(BinaryParserP7, fixed(">"), BinaryParserP7) { left, _, right -> Success(NodeBinary(left, NodeGtOperator, right)) },
).memoized()

val BinaryParserP5: Parser<NodeBinary> = parallel(
    BinaryParserP6,
    composed(BinaryParserP6, fixed("=="), BinaryParserP6) { left, _, right -> Success(NodeBinary(left, NodeEqOperator, right)) },
    composed(BinaryParserP6, fixed("!="), BinaryParserP6) { left, _, right -> Success(NodeBinary(left, NodeNeqOperator, right)) },
).memoized()

val BinaryParserP4: Parser<NodeBinary> = parallel(
    BinaryParserP5,
    composed(BinaryParserP5, fixed("&"), BinaryParserP5) { left, _, right -> Success(NodeBinary(left, NodeAndOperator, right)) },
).memoized()

val BinaryParserP3: Parser<NodeBinary> = parallel(
    BinaryParserP4,
    composed(BinaryParserP4, fixed("^"), BinaryParserP4) { left, _, right -> Success(NodeBinary(left, NodeXorOperator, right)) },
).memoized()

val BinaryParserP2: Parser<NodeBinary> = parallel(
    BinaryParserP3,
    composed(BinaryParserP3, fixed("|"), BinaryParserP3) { left, _, right -> Success(NodeBinary(left, NodeOrOperator, right)) },
).memoized()

val BinaryParserP1: Parser<NodeBinary> = parallel(
    BinaryParserP2,
    composed(BinaryParserP2, fixed("&&"), BinaryParserP2) { left, _, right -> Success(NodeBinary(left, NodeAndAndOperator, right)) },
).memoized()

val BinaryParserP0: Parser<NodeBinary> = parallel(
    BinaryParserP1,
    composed(BinaryParserP1, fixed("||"), BinaryParserP1) { left, _, right -> Success(NodeBinary(left, NodeOrOrOperator, right)) },
).memoized()

val ExpressionParser: Parser<NodeRightExpression> = parallel(
    UnaryExpressionParser,
    BinaryParserP0,
).memoized()

val LabelParser: Parser<String> = composed(
    fixed("#"),
    CamelCaseParser,
) { _, label -> Success(label) }.memoized()

val BreakParser: Parser<NodeBreak> = composed(
    fixed("break"),
    optional(LabelParser),
) { _, label -> Success(NodeBreak(label)) }.memoized()

val ContinueParser: Parser<NodeContinue> = composed(
    fixed("continue"),
    optional(LabelParser),
) { _, label -> Success(NodeContinue(label)) }.memoized()

val YieldParser: Parser<NodeYield> = composed(
    fixed("yield"),
    optional(LabelParser),
    ExpressionParser,
) { _, label, value -> Success(NodeYield(label, value)) }.memoized()

val ReturnParser: Parser<NodeReturn> = composed(
    fixed("return"),
    ExpressionParser,
) { _, value -> Success(NodeReturn(value)) }.memoized()

val ElseBodyParser: Parser<NodeStatement> = composed(
    fixed("else"),
    lazy { StatementParser },
) { _, body -> Success(body) }.memoized()

val BlockParser: Parser<NodeBlock> = composed(
    optional(LabelParser),
    listLike(
        fixed("{"),
        lazy { StatementParser },
        epsilon(),
        fixed("}"),
    ),
) { label, statements -> Success(NodeBlock(label, statements)) }.memoized()

val IfParser: Parser<NodeIf> = composed(
    optional(LabelParser),
    fixed("if"),
    ParenthesizedExpressionParser,
    lazy { StatementParser },
    optional(ElseBodyParser),
) { label, _, condition, thenBody, elseBody -> Success(NodeIf(label, condition, thenBody, elseBody)) }.memoized()

val WhileParser: Parser<NodeWhile> = composed(
    optional(LabelParser),
    fixed("while"),
    ParenthesizedExpressionParser,
    lazy { StatementParser },
) { label, _, condition, body -> Success(NodeWhile(label, condition, body)) }.memoized()

val DoWhileParser: Parser<NodeDoWhile> = composed(
    optional(LabelParser),
    fixed("do"),
    lazy { StatementParser },
    fixed("while"),
    ParenthesizedExpressionParser,
) { label, _, body, _, condition -> Success(NodeDoWhile(label, body, condition)) }.memoized()

val WhenCaseParser: Parser<NodeCase> = parallel(
    composed(
        listLike(
            epsilon(),
            ExpressionParser,
            fixed(","),
            fixed("->"),
        ),
        lazy { StatementParser },
    ) { conditions, body -> Success(NodeCase(conditions, body)) },
    composed(
        fixed("else"),
        fixed("->"),
        lazy { StatementParser },
    ) { _, _, body -> Success(NodeCase(emptyList(), body)) },
).memoized()

val WhenParser: Parser<NodeWhen> = composed(
    optional(LabelParser),
    fixed("when"),
    ParenthesizedExpressionParser,
    listLike(
        fixed("{"),
        WhenCaseParser,
        epsilon(),
        fixed("}"),
    ),
) { label, _, expression, cases -> Success(NodeWhen(label, expression, cases)) }.memoized()

val UnitCallParser: Parser<NodeRightExpression> = composed(
    CamelCaseParser,
    optional(lazy { ActualGenericParameterListParser }),
    lazy { ActualParameterListParser },
) { name, generics, parameters -> Success(NodeCall(null, name, generics, parameters)) }.memoized()

val ConstructParser: Parser<NodeRightExpression> = composed(
    lazy { TypeParser },
    lazy { ActualParameterListParser },
) { type, parameters -> Success(NodeConstruct(type, parameters)) }.memoized()

val IncrementParser: Parser<(NodeRightExpression) -> ParseResult<NodeAssign>> = fixed("++").map { _ ->
    Success { left: NodeRightExpression ->
        if (left !is NodeLeftExpression) Failure { "Cannot increment a non-left expression" }
        else Success(NodeAssign(left, NodeAddAssignOperator, NodeEntity(FoxInt(1)), beforeEvaluation = false))
    }
}.memoized()

val DecrementParser: Parser<(NodeRightExpression) -> ParseResult<NodeAssign>> = fixed("--").map { _ ->
    Success { left: NodeRightExpression ->
        if (left !is NodeLeftExpression) Failure { "Cannot decrement a non-left expression" }
        else Success(NodeAssign(left, NodeSubAssignOperator, NodeEntity(FoxInt(1)), beforeEvaluation = false))
    }
}.memoized()

val AssignParser: Parser<(NodeRightExpression) -> ParseResult<NodeAssign>> = composed(
    parallel(
        fixed("=").map { Success(NodePlainAssignOperator) },
        fixed("+=").map { Success(NodeAddAssignOperator) },
        fixed("-=").map { Success(NodeSubAssignOperator) },
        fixed("*=").map { Success(NodeMulAssignOperator) },
        fixed("/=").map { Success(NodeDivAssignOperator) },
        fixed("%=").map { Success(NodeRemAssignOperator) },
        fixed("&=").map { Success(NodeAndAssignOperator) },
        fixed("|=").map { Success(NodeOrAssignOperator) },
        fixed("^=").map { Success(NodeXorAssignOperator) },
        fixed("&&=").map { Success(NodeAndAndAssignOperator) },
        fixed("||=").map { Success(NodeOrOrAssignOperator) },
        fixed("<<=").map { Success(NodeShlAssignOperator) },
        fixed(">>=").map { Success(NodeShrAssignOperator) },
        fixed(">>>=").map { Success(NodeUshrAssignOperator) },
    ),
    ExpressionParser,
) { operator, right ->
    Success { left: NodeRightExpression ->
        if (left !is NodeLeftExpression) Failure { "Cannot assign to a non-left expression" }
        else Success(NodeAssign(left, operator, right, beforeEvaluation = true))
    }
}.memoized()

val ChainedCallParser: Parser<(NodeRightExpression) -> ParseResult<NodeCall>> = composed(
    fixed("."),
    CamelCaseParser,
    optional(lazy { ActualGenericParameterListParser }),
    lazy { ActualParameterListParser },
) { _, name, generics, parameters ->
    Success { left: NodeRightExpression ->
        Success(NodeCall(left, name, generics, parameters))
    }
}.memoized()

val FieldAccessParser: Parser<(NodeRightExpression) -> ParseResult<NodeFieldAccess>> = composed(
    fixed("."),
    CamelCaseParser,
) { _, name ->
    Success { left: NodeRightExpression ->
        Success(NodeFieldAccess(left, name))
    }
}.memoized()

val ExpressionTailParser: Parser<(NodeRightExpression) -> ParseResult<NodeRightExpression>> = parallel(
    IncrementParser,
    DecrementParser,
    AssignParser,
    ChainedCallParser,
    FieldAccessParser,
).memoized()

val ExpressionStatementParser: Parser<NodeStatement> = ExpressionParser.map {
    if (it is NodeStatement) Success(it) else Failure { "Expected expression statement" }
}

val StatementParser: Parser<NodeStatement> = parallel(
    BreakParser,
    ContinueParser,
    YieldParser,
    ReturnParser,
    WhileParser,
    DoWhileParser,
    ExpressionStatementParser,
).memoized()

val TypeParser: Parser<NodeType> = parallel(
    PascalCaseParser.map {
        when (it) {
            "Unit" -> Success(NodePrimitiveType(FoxUnitType))
            "Bool" -> Success(NodePrimitiveType(FoxBoolType))
            "Byte" -> Success(NodePrimitiveType(FoxByteType))
            "Short" -> Success(NodePrimitiveType(FoxShortType))
            "Int" -> Success(NodePrimitiveType(FoxIntType))
            "Long" -> Success(NodePrimitiveType(FoxLongType))
            "Float" -> Success(NodePrimitiveType(FoxFloatType))
            "Double" -> Success(NodePrimitiveType(FoxDoubleType))
            "Char" -> Success(NodePrimitiveType(FoxCharType))
            "String" -> Success(NodePrimitiveType(FoxStringType))
            else -> Failure { "Expected primitive type" }
        }
    },
    composed(
        fixed("Array"),
        lazy { AnonymousActualGenericParameterListParser },
    ) { _, list ->
        if (list.size != 1) Failure { "Expected one generic parameter" }
        else Success(NodeArrayType(list.first()))
    },
    composed(
        fixed("Tuple"),
        lazy { AnonymousActualGenericParameterListParser },
    ) { _, list -> Success(NodeTupleType(list)) },
    composed(
        fixed("Struct"),
        lazy { NamedActualGenericParameterListParser },
    ) { _, list -> Success(NodeStructType(list)) },
    composed(
        fixed("Enum"),
        lazy { NamedActualGenericParameterListParser },
    ) { _, list -> Success(NodeEnumType(list)) },
    composed(
        fixed("Ref"),
        lazy { AnonymousActualGenericParameterListParser },
    ) { _, list ->
        if (list.size != 1) Failure { "Expected one generic parameter" }
        else Success(NodeRefType(list.first()))
    },
    composed(
        fixed("Lambda"),
        lazy { AnonymousActualGenericParameterListParser },
    ) { _, list ->
        if (list.size < 2) Failure { "Expected at least two generic parameters" }
        else Success(
            NodeLambdaType(
                thisType = list.first(),
                parameters = list.subList(1, list.size - 1),
                returnType = list.last(),
            ),
        )
    },
    composed(
        PascalCaseParser,
        optional(lazy { ActualGenericParameterListParser }),
    ) { name, list -> Success(NodeNamedType(name, list ?: emptyList())) },
).memoized()

val FormalParameterParser: Parser<Pair<String, NodeType>> = composed(
    CamelCaseAndColonParser,
    TypeParser,
) { name, type -> Success(name to type) }.memoized()

val FormalParameterListParser: Parser<SequencedMap<String, NodeType>> = listLike(
    fixed("("),
    FormalParameterParser,
    fixed(","),
    fixed(")"),
).map { parameters -> linkedSequencedMap(parameters, "parameter") }.memoized()

val ActualParameterParser: Parser<Pair<String?, NodeRightExpression>> = composed(
    optional(CamelCaseAndEqualParser),
    ExpressionParser,
) { name, expression -> Success(name to expression) }.memoized()

val ActualParameterListParser: Parser<List<Pair<String?, NodeRightExpression>>> = listLike(
    fixed("("),
    ActualParameterParser,
    fixed(","),
    fixed(")"),
).memoized()

val AnonymousActualParameterListParser: Parser<List<NodeRightExpression>> = listLike(
    fixed("("),
    ExpressionParser,
    fixed(","),
    fixed(")"),
).memoized()

val FormalGenericParameterConstraintParser: Parser<NodeGenericConstraint> = optional(
    composed(
        fixed("="),
        TypeParser,
    ) { _, type -> Success(type) },
).map { constraint -> Success(NodeGenericConstraint(constraint)) }.memoized()

val FormalGenericParameterParser: Parser<Pair<String, NodeGenericConstraint>> = composed(
    PascalCaseAndColonParser,
    FormalGenericParameterConstraintParser,
) { name, constraint -> Success(name to constraint) }.memoized()

val FormalGenericParameterListParser: Parser<SequencedMap<String, NodeGenericConstraint>> = listLike(
    fixed("<"),
    FormalGenericParameterParser,
    fixed(","),
    fixed(">"),
).map { parameters -> linkedSequencedMap(parameters, "generic parameter") }.memoized()

val FormalGenericParameterListWithoutConstraintsParser: Parser<SequencedSet<String>> = listLike(
    fixed("<"),
    PascalCaseParser,
    fixed(","),
    fixed(">"),
).map { names -> linkedSequencedSet(names, "generic parameter") }.memoized()

val ActualGenericParameterParser: Parser<Pair<String?, NodeType>> = composed(
    optional(PascalCaseAndEqualParser),
    TypeParser,
) { name, type -> Success(name to type) }.memoized()

val NamedActualGenericParameterParser: Parser<Pair<String, NodeType>> = composed(
    PascalCaseAndEqualParser,
    TypeParser,
) { name, type -> Success(name to type) }.memoized()

val ActualGenericParameterListParser: Parser<List<Pair<String?, NodeType>>> = listLike(
    fixed("<"),
    ActualGenericParameterParser,
    fixed(","),
    fixed(">"),
).memoized()

val AnonymousActualGenericParameterListParser: Parser<List<NodeType>> = listLike(
    fixed("<"),
    TypeParser,
    fixed(","),
    fixed(">"),
).memoized()

val NamedActualGenericParameterListParser: Parser<SequencedMap<String, NodeType>> = listLike(
    fixed("<"),
    NamedActualGenericParameterParser,
    fixed(","),
    fixed(">"),
).map { parameters -> linkedSequencedMap(parameters, "generic parameter") }.memoized()

val TypeAliasParser: Parser<NodeTypeAlias> = composed(
    fixed("type"),
    PascalCaseParser,
    optional(FormalGenericParameterListWithoutConstraintsParser),
    fixed("="),
    TypeParser,
) { _, name, generics, _, type -> Success(NodeTypeAlias(name, generics, type)) }.memoized()

val MethodDefinitionParser: Parser<NodeMethodDefinition> = composed(
    fixed("def"),
    optional(FormalGenericParameterListParser),
    optional(
        composed(
            TypeParser,
            fixed("."),
        ) { type, _ -> Success(type) },
    ),
    CamelCaseParser,
    FormalParameterListParser,
    optional(
        composed(
            fixed(":"),
            TypeParser,
        ) { _, type -> Success(type) },
    ),
    StatementParser,
) { _, generics, thisType, name, parameters, returnType, body ->
    Success(NodeMethodDefinition(generics, thisType, name, parameters, returnType, body))
}.memoized()

val FileElementParser: Parser<NodeFileElement> = parallel(
    TypeAliasParser,
    MethodDefinitionParser,
).memoized()

val FileParser: Parser<NodeFile> = listLike(
    epsilon(),
    FileElementParser,
    epsilon(),
    EofParser,
).map { elements -> Success(NodeFile(elements)) }.memoized()

private fun String.toFloatingEntity(): NodeEntity {
    return if (endsWith('f')) {
        NodeEntity(FoxFloat(dropLast(1).replace("_", "").toFloat()))
    } else {
        NodeEntity(FoxDouble(replace("_", "").toDouble()))
    }
}

private fun <V : Any> linkedSequencedMap(
    entries: List<Pair<String, V>>,
    itemName: String,
): ParseResult<SequencedMap<String, V>> {
    val result = LinkedHashMap<String, V>()
    entries.forEach { (name, value) ->
        if (name in result) return Failure { "Duplicate $itemName name '$name'" }
        result[name] = value
    }
    return Success(result)
}

private fun linkedSequencedSet(names: List<String>, itemName: String): ParseResult<SequencedSet<String>> {
    val result = LinkedHashSet<String>()
    names.forEach { name ->
        if (name in result) return Failure { "Duplicate $itemName name '$name'" }
        result += name
    }
    return Success(result)
}
