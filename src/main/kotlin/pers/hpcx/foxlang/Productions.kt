package pers.hpcx.foxlang

import pers.hpcx.foxlang.utils.*
import java.util.*

private val Word = node<String>().name("Word")
private val Identifier = node<String>().name("Identifier")
private val TypeName = node<String>().name("TypeName")
private val IdentifierEqual = node<String>().name("IdentifierEqual")
private val IdentifierColon = node<String>().name("IdentifierColon")
private val TypeNameEqual = node<String>().name("TypeNameEqual")
private val TypeNameColon = node<String>().name("TypeNameColon")
private val Label = node<String>().name("Label")

private val BinInt = node<String>().name("BinInt")
private val DecInt = node<String>().name("DecInt")
private val HexInt = node<String>().name("HexInt")
private val BinLong = node<String>().name("BinLong")
private val DecLong = node<String>().name("DecLong")
private val HexLong = node<String>().name("HexLong")
private val DecFloat = node<String>().name("DecFloat")
private val HexFloat = node<String>().name("HexFloat")
private val DecDouble = node<String>().name("DecDouble")
private val HexDouble = node<String>().name("HexDouble")
private val FormattedStringTemplateLiteral = node<FormattedStringTemplate>().name("FormattedStringTemplateLiteral")

private val FormalParameter = node<String>().pair(node<NodeType>()).name("FormalParameter")
private val RawFormalParameterList = node<String>().pair(node<NodeType>()).list().name("RawFormalParameterList")
private val FormalParameterList = node<String>().seqMap(node<NodeType>()).name("FormalParameterList")

private val ActualParameter = node<Pair<String?, NodeStatement>>().name("ActualParameter")
private val ActualParameterList = ActualParameter.list().name("ActualParameterList")

private val AnonymousActualParameterList = node<NodeStatement>().list().name("AnonymousActualParameterList")

private val FormalGenericParameter = node<String>().pair(node<NodeGenericConstraint>()).name("FormalGenericParameter")
private val RawFormalGenericParameterList = node<String>().pair(node<NodeGenericConstraint>()).list().name("RawFormalGenericParameterList")
private val FormalGenericParameterList = node<String>().seqMap(node<NodeGenericConstraint>()).name("FormalGenericParameterList")

private val RawFormalGenericParameterListWithoutConstraints = node<String>().list().name("RawFormalGenericParameterListWithoutConstraints")
private val FormalGenericParameterListWithoutConstraints = node<String>().seqSet().name("FormalGenericParameterListWithoutConstraints")

private val ActualGenericParameter = node<Pair<String?, NodeType>>().name("ActualGenericParameter")
private val ActualGenericParameterList = ActualGenericParameter.list().name("ActualGenericParameterList")

private val NamedActualGenericParameter = node<String>().pair(node<NodeType>()).name("NamedActualGenericParameter")
private val RawNamedActualGenericParameterList = node<String>().pair(node<NodeType>()).list().name("RawNamedActualGenericParameterList")
private val NamedActualGenericParameterList = node<String>().map(node<NodeType>()).name("NamedActualGenericParameterList")

private val AnonymousActualGenericParameterList = node<NodeType>().list().name("AnonymousActualGenericParameterList")

private val StructFieldTypeParameter = node<String>().pair(node<NodeType>()).name("StructFieldTypeParameter")
private val RawStructFieldTypeParameterList = node<String>().pair(node<NodeType>()).list().name("RawStructFieldTypeParameterList")
private val StructFieldTypeParameterList = node<String>().seqMap(node<NodeType>()).name("StructFieldTypeParameterList")

private val EnumItemTypeParameter = node<String>().pair(node<NodeType>()).name("EnumItemTypeParameter")
private val RawEnumItemTypeParameterList = node<String>().pair(node<NodeType>()).list().name("RawEnumItemTypeParameterList")
private val EnumItemTypeParameterList = node<String>().seqMap(node<NodeType>()).name("EnumItemTypeParameterList")

private val StatementBlock = node<NodeStatement>().list().name("StatementBlock")
private val ParenthesizedStatement = node<NodeStatement>().name("ParenthesizedStatement")
private val PrimaryExpression = node<NodeStatement>().name("PrimaryExpression")
private val PostfixExpression = node<NodeStatement>().name("PostfixExpression")
private val UnaryExpression = node<NodeStatement>().name("UnaryExpression")
private val MultiplicativeExpression = node<NodeStatement>().name("MultiplicativeExpression")
private val AdditiveExpression = node<NodeStatement>().name("AdditiveExpression")
private val ShiftExpression = node<NodeStatement>().name("ShiftExpression")
private val ComparisonExpression = node<NodeStatement>().name("ComparisonExpression")
private val EqualityExpression = node<NodeStatement>().name("EqualityExpression")
private val BitAndExpression = node<NodeStatement>().name("BitAndExpression")
private val BitXorExpression = node<NodeStatement>().name("BitXorExpression")
private val BitOrExpression = node<NodeStatement>().name("BitOrExpression")
private val LogicalAndExpression = node<NodeStatement>().name("LogicalAndExpression")
private val LogicalOrExpression = node<NodeStatement>().name("LogicalOrExpression")
private val AssignableExpression = node<NodeStatement>().name("AssignableExpression")
private val AssignmentExpression = node<NodeStatement>().name("AssignmentExpression")

private val MultiplicativeOperator = node<NodeBinaryOperator>().name("MultiplicativeOperator")
private val AdditiveOperator = node<NodeBinaryOperator>().name("AdditiveOperator")
private val ShiftOperator = node<NodeBinaryOperator>().name("ShiftOperator")
private val ComparisonOperator = node<NodeBinaryOperator>().name("ComparisonOperator")
private val EqualityOperator = node<NodeBinaryOperator>().name("EqualityOperator")
private val BitAndOperator = node<NodeBinaryOperator>().name("BitAndOperator")
private val BitXorOperator = node<NodeBinaryOperator>().name("BitXorOperator")
private val BitOrOperator = node<NodeBinaryOperator>().name("BitOrOperator")
private val LogicalAndOperator = node<NodeBinaryOperator>().name("LogicalAndOperator")
private val LogicalOrOperator = node<NodeBinaryOperator>().name("LogicalOrOperator")
private val PrimitiveTypeExpression = node<NodeType>().name("PrimitiveTypeExpression")
private val BuiltInTypeExpression = node<NodeType>().name("BuiltInTypeExpression")
private val NamedTypeExpression = node<NodeType>().name("NamedTypeExpression")

private val WhenCaseConditionList = node<NodeStatement>().list().name("WhenCaseConditionList")
private val WhenCase = node<NodeCase>().name("WhenCase")
private val WhenCaseList = node<NodeCase>().list().name("WhenCaseList")
private val IfCore = node<ParsedIfCore>().name("IfCore")
private val WhileCore = node<ParsedWhileCore>().name("WhileCore")
private val DoWhileCore = node<ParsedDoWhileCore>().name("DoWhileCore")
private val WhenCore = node<ParsedWhenCore>().name("WhenCore")

private val ThisTypeQualifier = node<NodeType>().name("ThisTypeQualifier")
private val ReturnTypeClause = node<NodeType>().name("ReturnTypeClause")
private val MethodHead = node<ParsedMethodHead>().name("MethodHead")
private val FileElementList = node<NodeFileElement>().list().name("FileElementList")

private val ReservedKeywords = setOf(
    "const", "type", "def", "if", "else", "when", "new", "yield", "return", "for",
    "do", "while", "break", "continue", "try", "finally", "import", "unit", "true", "false",
    
    "Void", "Unit", "Bool", "Byte", "Short", "Int", "Long", "Float", "Double", "Char",
    "String", "Array", "Tuple", "Struct", "Enum", "Ref", "Lambda",
)

val FoxProductions = buildList {
    addAll(fixedTokens(*ReservedKeywords.toTypedArray()))
    
    addAll(
        fixedTokens(
            "(", ")", "[", "]", "{", "}", ".", ":", ";", ",",
            "+", "-", "*", "/", "%", "`", "~", "?", "!", "@", "#", "$", "&", "|", "^",
            "<", ">", "==", "!=", "<=", ">=", "&&", "||", "<<", ">>", ">>>", "->",
            "=", ":=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=", "&&=", "||=",
        ),
    )
    
    addAll(
        tokenValues(
            node<NodeUnaryOperator>(),
            "!" to NodeNotOperator,
            "-" to NodeNegOperator,
        ),
    )
    
    addAll(
        tokenValues(
            node<NodeBinaryOperator>(),
            "+" to NodeAddOperator,
            "-" to NodeSubOperator,
            "*" to NodeMulOperator,
            "/" to NodeDivOperator,
            "%" to NodeRemOperator,
            "&" to NodeAndOperator,
            "|" to NodeOrOperator,
            "^" to NodeXorOperator,
            "<<" to NodeShlOperator,
            ">>" to NodeShrOperator,
            ">>>" to NodeUshrOperator,
            "==" to NodeEqOperator,
            "!=" to NodeNeqOperator,
            "<" to NodeLtOperator,
            ">" to NodeGtOperator,
            "<=" to NodeLeOperator,
            ">=" to NodeGeOperator,
            "&&" to NodeAndAndOperator,
            "||" to NodeOrOrOperator,
        ),
    )
    
    addAll(tokenValues(MultiplicativeOperator, "*" to NodeMulOperator, "/" to NodeDivOperator, "%" to NodeRemOperator))
    addAll(tokenValues(AdditiveOperator, "+" to NodeAddOperator, "-" to NodeSubOperator))
    addAll(tokenValues(ShiftOperator, "<<" to NodeShlOperator, ">>" to NodeShrOperator, ">>>" to NodeUshrOperator))
    addAll(tokenValues(ComparisonOperator, "<" to NodeLtOperator, ">" to NodeGtOperator, "<=" to NodeLeOperator, ">=" to NodeGeOperator))
    addAll(tokenValues(EqualityOperator, "==" to NodeEqOperator, "!=" to NodeNeqOperator))
    addAll(tokenValues(BitAndOperator, "&" to NodeAndOperator))
    addAll(tokenValues(BitXorOperator, "^" to NodeXorOperator))
    addAll(tokenValues(BitOrOperator, "|" to NodeOrOperator))
    addAll(tokenValues(LogicalAndOperator, "&&" to NodeAndAndOperator))
    addAll(tokenValues(LogicalOrOperator, "||" to NodeOrOrOperator))
    
    addAll(
        tokenValues(
            node<NodeAssignOperator>(),
            "=" to NodePlainAssignOperator,
            ":=" to NodeTypeBindingAssignOperator,
            "+=" to NodeAddAssignOperator,
            "-=" to NodeSubAssignOperator,
            "*=" to NodeMulAssignOperator,
            "/=" to NodeDivAssignOperator,
            "%=" to NodeRemAssignOperator,
            "&=" to NodeAndAssignOperator,
            "|=" to NodeOrAssignOperator,
            "^=" to NodeXorAssignOperator,
            "<<=" to NodeShlAssignOperator,
            ">>=" to NodeShrAssignOperator,
            ">>>=" to NodeUshrAssignOperator,
            "&&=" to NodeAndAndAssignOperator,
            "||=" to NodeOrOrAssignOperator,
        ),
    )
    
    add(regex(Word, Regex("[a-zA-Z0-9_]+"), "word"))
    add(
        serial(Identifier, Word) {
            if (it in ReservedKeywords) throw ParseException("'$it' is a reserved keyword")
            if (it[0] !in 'a'..'z') throw ParseException("'${it[0]}' is not a valid first character of an identifier")
            it
        },
    )
    add(
        serial(TypeName, Word) {
            if (it in ReservedKeywords) throw ParseException("'$it' is a reserved keyword")
            if (it[0] !in 'A'..'Z') throw ParseException("'${it[0]}' is not a valid first character of a type name")
            it
        },
    )
    add(serial(IdentifierEqual, Identifier, token("=")) { it, _ -> it })
    add(serial(IdentifierColon, Identifier, token(":")) { it, _ -> it })
    add(serial(TypeNameEqual, TypeName, token("=")) { it, _ -> it })
    add(serial(TypeNameColon, TypeName, token(":")) { it, _ -> it })
    
    add(serial(node<Unit>(), token("unit")) { })
    add(serial(node<Boolean>(), token("true")) { true })
    add(serial(node<Boolean>(), token("false")) { false })
    add(charLiteral(node<Char>()))
    add(stringLiteral(node<String>()))
    add(formattedStringLiteral(FormattedStringTemplateLiteral))
    
    addAll(regexValue(BinInt, "0b[01]+(_[01]+)*", "integer literal", node<Int>()) { it.drop(2).replace("_", "").toInt(2) })
    addAll(regexValue(DecInt, "(0|[1-9][0-9]*(_[0-9]+)*)", "integer literal", node<Int>()) { it.replace("_", "").toInt() })
    addAll(regexValue(HexInt, "0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*", "integer literal", node<Int>()) { it.drop(2).replace("_", "").toInt(16) })
    addAll(regexValue(BinLong, "0b[01]+(_[01]+)*L", "long literal", node<Long>()) { it.drop(2).dropLast(1).replace("_", "").toLong(2) })
    addAll(regexValue(DecLong, "(0|[1-9][0-9]*(_[0-9]+)*)L", "long literal", node<Long>()) { it.dropLast(1).replace("_", "").toLong() })
    addAll(regexValue(HexLong, "0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*L", "long literal", node<Long>()) { it.drop(2).dropLast(1).replace("_", "").toLong(16) })
    addAll(regexValue(DecFloat, "(0|[1-9][0-9]*(_[0-9]+)*)(\\.[0-9]+(_[0-9]+)*)?(e[+-]?[0-9]+)?f", "number literal", node<Float>()) { it.dropLast(1).replace("_", "").toFloat() })
    addAll(regexValue(HexFloat, "0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*(\\.[0-9a-fA-F]+(_[0-9a-fA-F]+)*)?p[+-]?[0-9]+f", "number literal", node<Float>()) { it.dropLast(1).replace("_", "").toFloat() })
    addAll(regexValue(DecDouble, "(0|[1-9][0-9]*(_[0-9]+)*)(\\.[0-9]+(_[0-9]+)*)?(e[+-]?[0-9]+)?", "number literal", node<Double>()) { it.replace("_", "").toDouble() })
    addAll(regexValue(HexDouble, "0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*(\\.[0-9a-fA-F]+(_[0-9a-fA-F]+)*)?p[+-]?[0-9]+", "number literal", node<Double>()) { it.replace("_", "").toDouble() })
    
    add(serial(node<NodeEntity>(), node<Unit>()) { NodeEntity(FoxUnit) })
    add(serial(node<NodeEntity>(), node<Boolean>()) { NodeEntity(FoxBool(it)) })
    add(serial(node<NodeEntity>(), node<Int>()) { NodeEntity(FoxInt(it)) })
    add(serial(node<NodeEntity>(), node<Long>()) { NodeEntity(FoxLong(it)) })
    add(serial(node<NodeEntity>(), node<Float>()) { NodeEntity(FoxFloat(it)) })
    add(serial(node<NodeEntity>(), node<Double>()) { NodeEntity(FoxDouble(it)) })
    add(serial(node<NodeEntity>(), node<Char>()) { NodeEntity(FoxChar(it)) })
    add(serial(node<NodeEntity>(), node<String>()) { NodeEntity(FoxString(it)) })
    add(serial(PrimaryExpression, node<NodeEntity>()) { it })
    add(
        serial(PrimaryExpression, FormattedStringTemplateLiteral) { template ->
            NodeFormattedString(
                parts = template.parts.map { part ->
                    when (part) {
                        is FormattedTextPart -> NodeFormattedText(part.text)
                        is FormattedExpressionPart -> NodeFormattedExpression(parseFormattedExpression(part.source))
                    }
                },
                isRaw = template.isRaw,
            )
        },
    )
    
    addAll(
        tokenValues(
            PrimitiveTypeExpression,
            "Void" to NodePrimitiveType(FoxVoidType),
            "Unit" to NodePrimitiveType(FoxUnitType),
            "Bool" to NodePrimitiveType(FoxBoolType),
            "Byte" to NodePrimitiveType(FoxByteType),
            "Short" to NodePrimitiveType(FoxShortType),
            "Int" to NodePrimitiveType(FoxIntType),
            "Long" to NodePrimitiveType(FoxLongType),
            "Float" to NodePrimitiveType(FoxFloatType),
            "Double" to NodePrimitiveType(FoxDoubleType),
            "Char" to NodePrimitiveType(FoxCharType),
            "String" to NodePrimitiveType(FoxStringType),
        ),
    )
    add(serial(node<NodeType>(), PrimitiveTypeExpression) { it })
    addAll(
        listOf(
            serial(BuiltInTypeExpression, token("Array"), AnonymousActualGenericParameterList) { _, it ->
                if (it.size != 1) throw ParseException("Array type must have exactly one generic parameter")
                NodeArrayType(it.first())
            },
            serial(BuiltInTypeExpression, token("Tuple"), AnonymousActualGenericParameterList) { _, it ->
                NodeTupleType(it)
            },
            serial(BuiltInTypeExpression, token("Struct"), StructFieldTypeParameterList) { _, it ->
                NodeStructType(it)
            },
            serial(BuiltInTypeExpression, token("Enum"), EnumItemTypeParameterList) { _, it ->
                NodeEnumType(it)
            },
            serial(BuiltInTypeExpression, token("Ref"), AnonymousActualGenericParameterList) { _, it ->
                if (it.size != 1) throw ParseException("Ref type must have exactly one generic parameter")
                NodeRefType(it.first())
            },
            serial(BuiltInTypeExpression, token("Lambda"), AnonymousActualGenericParameterList) { _, it ->
                if (it.size < 2) throw ParseException("Lambda type must have at least two generic parameters")
                NodeLambdaType(it.first(), it.drop(1).dropLast(1), it.last())
            },
            serial(node<NodeType>(), BuiltInTypeExpression) { it },
            serial(NamedTypeExpression, TypeName) { name ->
                NodeNamedType(name, emptyList())
            },
            serial(NamedTypeExpression, TypeName, ActualGenericParameterList) { name, generics ->
                NodeNamedType(name, generics)
            },
            serial(node<NodeType>(), NamedTypeExpression) { it },
            
            serial(FormalParameter, IdentifierColon, node<NodeType>()) { name, type -> name to type },
            listLike(RawFormalParameterList, token("("), FormalParameter, token(","), token(")")),
            serial(FormalParameterList, RawFormalParameterList) {
                linkedSequencedMap(it, "parameter")
            },
            
            serial(ActualParameter, node<NodeStatement>()) { null to it },
            serial(ActualParameter, IdentifierEqual, node<NodeStatement>()) { name, value -> name to value },
            listLike(ActualParameterList, token("("), ActualParameter, token(","), token(")")),
            
            listLike(AnonymousActualParameterList, token("("), node<NodeStatement>(), token(","), token(")")),
            
            serial(FormalGenericParameter, TypeName) { it to NodeGenericConstraint(match = null) },
            serial(FormalGenericParameter, TypeNameEqual, node<NodeType>()) { name, type -> name to NodeGenericConstraint(type) },
            listLike(RawFormalGenericParameterList, token("<"), FormalGenericParameter, token(","), token(">")),
            serial(FormalGenericParameterList, RawFormalGenericParameterList) { linkedSequencedMap(it, "formal generic parameter") },
            
            listLike(RawFormalGenericParameterListWithoutConstraints, token("<"), TypeName, token(","), token(">")),
            serial(FormalGenericParameterListWithoutConstraints, RawFormalGenericParameterListWithoutConstraints) { linkedSequencedSet(it, "formal generic parameter") },
            
            serial(ActualGenericParameter, node<NodeType>()) { null to it },
            serial(ActualGenericParameter, TypeNameEqual, node<NodeType>()) { name, type -> name to type },
            listLike(ActualGenericParameterList, token("<"), ActualGenericParameter, token(","), token(">")),
            
            serial(NamedActualGenericParameter, TypeNameEqual, node<NodeType>()) { name, type -> name to type },
            listLike(RawNamedActualGenericParameterList, token("<"), NamedActualGenericParameter, token(","), token(">")),
            serial(NamedActualGenericParameterList, RawNamedActualGenericParameterList) { linkedSequencedMap(it, "actual generic parameter") },
            
            listLike(AnonymousActualGenericParameterList, token("<"), node<NodeType>(), token(","), token(">")),
            
            serial(StructFieldTypeParameter, IdentifierColon, node<NodeType>()) { name, type -> name to type },
            listLike(RawStructFieldTypeParameterList, token("<"), StructFieldTypeParameter, token(","), token(">")),
            serial(StructFieldTypeParameterList, RawStructFieldTypeParameterList) { linkedSequencedMap(it, "struct field type parameter") },
            
            serial(EnumItemTypeParameter, TypeNameEqual, node<NodeType>()) { name, type -> name to type },
            listLike(RawEnumItemTypeParameterList, token("<"), EnumItemTypeParameter, token(","), token(">")),
            serial(EnumItemTypeParameterList, RawEnumItemTypeParameterList) { linkedSequencedMap(it, "enum item type parameter") },
            
            serial(Label, token("#"), Identifier) { _, it -> it },
            serial(ParenthesizedStatement, token("("), node<NodeStatement>(), token(")")) { _, node, _ -> node },
            
            serial(PrimaryExpression, Identifier) { NodeSymbol(it) },
            serial(PrimaryExpression, ParenthesizedStatement) { it },
            
            serial(PostfixExpression, PrimaryExpression) { it },
            serial(PostfixExpression, PostfixExpression, token("."), Identifier) { target, _, name ->
                NodeFieldAccess(target, name)
            },
            serial(PostfixExpression, PostfixExpression, token("."), node<Int>()) { target, _, index ->
                NodeComponentAccess(target, index)
            },
            serial(PostfixExpression, Identifier, ActualGenericParameterList, ActualParameterList) { name, generics, parameters ->
                NodeCall(null, name, generics, parameters)
            },
            serial(PostfixExpression, Identifier, ActualParameterList) { name, parameters ->
                NodeCall(null, name, null, parameters)
            },
            serial(PostfixExpression, PostfixExpression, token("."), Identifier, ActualGenericParameterList, ActualParameterList) { target, _, name, generics, parameters ->
                NodeCall(target, name, generics, parameters)
            },
            serial(PostfixExpression, PostfixExpression, token("."), Identifier, ActualParameterList) { target, _, name, parameters ->
                NodeCall(target, name, null, parameters)
            },
            serial(PostfixExpression, node<NodeType>(), ActualParameterList) { type, parameters ->
                NodeConstruct(type, parameters)
            },
            
            serial(UnaryExpression, PostfixExpression) { it },
            serial(UnaryExpression, node<NodeUnaryOperator>(), UnaryExpression) { operator, node ->
                NodeUnary(operator, node)
            },
            
            serial(MultiplicativeExpression, UnaryExpression) { it },
            serial(MultiplicativeExpression, MultiplicativeExpression, MultiplicativeOperator, UnaryExpression) { left, operator, right ->
                NodeBinary(left, operator, right)
            },
            
            serial(AdditiveExpression, MultiplicativeExpression) { it },
            serial(AdditiveExpression, AdditiveExpression, AdditiveOperator, MultiplicativeExpression) { left, operator, right ->
                NodeBinary(left, operator, right)
            },
            
            serial(ShiftExpression, AdditiveExpression) { it },
            serial(ShiftExpression, ShiftExpression, ShiftOperator, AdditiveExpression) { left, operator, right ->
                NodeBinary(left, operator, right)
            },
            
            serial(ComparisonExpression, ShiftExpression) { it },
            serial(ComparisonExpression, ComparisonExpression, ComparisonOperator, ShiftExpression) { left, operator, right ->
                NodeBinary(left, operator, right)
            },
            
            serial(EqualityExpression, ComparisonExpression) { it },
            serial(EqualityExpression, EqualityExpression, EqualityOperator, ComparisonExpression) { left, operator, right ->
                NodeBinary(left, operator, right)
            },
            
            serial(BitAndExpression, EqualityExpression) { it },
            serial(BitAndExpression, BitAndExpression, BitAndOperator, EqualityExpression) { left, operator, right ->
                NodeBinary(left, operator, right)
            },
            
            serial(BitXorExpression, BitAndExpression) { it },
            serial(BitXorExpression, BitXorExpression, BitXorOperator, BitAndExpression) { left, operator, right ->
                NodeBinary(left, operator, right)
            },
            
            serial(BitOrExpression, BitXorExpression) { it },
            serial(BitOrExpression, BitOrExpression, BitOrOperator, BitXorExpression) { left, operator, right ->
                NodeBinary(left, operator, right)
            },
            
            serial(LogicalAndExpression, BitOrExpression) { it },
            serial(LogicalAndExpression, LogicalAndExpression, LogicalAndOperator, BitOrExpression) { left, operator, right ->
                NodeBinary(left, operator, right)
            },
            
            serial(LogicalOrExpression, LogicalAndExpression) { it },
            serial(LogicalOrExpression, LogicalOrExpression, LogicalOrOperator, LogicalAndExpression) { left, operator, right ->
                NodeBinary(left, operator, right)
            },
            
            serial(AssignableExpression, PostfixExpression) { it },
            
            serial(AssignmentExpression, LogicalOrExpression) { it },
            serial(AssignmentExpression, AssignableExpression, node<NodeAssignOperator>(), node<NodeStatement>()) { left, operator, right ->
                NodeAssign(left, operator, right, beforeEvaluation = true)
            },
            
            serial(node<NodeStatement>(), AssignmentExpression) { it },
            serial(node<NodeStatement>(), Identifier, token(":"), node<NodeType>()) { name, _, type ->
                NodeTypeBinding(name, type)
            },
            
            serial(node<NodeStatement>(), token("break")) { NodeBreak(null) },
            serial(node<NodeStatement>(), token("break"), Label) { _, label -> NodeBreak(label) },
            serial(node<NodeStatement>(), token("continue")) { NodeContinue(null) },
            serial(node<NodeStatement>(), token("continue"), Label) { _, label -> NodeContinue(label) },
            serial(node<NodeStatement>(), token("return")) { NodeReturn(null) },
            serial(node<NodeStatement>(), token("return"), node<NodeStatement>()) { _, value -> NodeReturn(value) },
            serial(node<NodeStatement>(), token("yield"), node<NodeStatement>()) { _, value -> NodeYield(null, value) },
            serial(node<NodeStatement>(), token("yield"), Label, node<NodeStatement>()) { _, label, value -> NodeYield(label, value) },
            
            listLike(StatementBlock, token("{"), node<NodeStatement>(), null, token("}")),
            serial(node<NodeStatement>(), StatementBlock) { NodeBlock(null, it) },
            serial(node<NodeStatement>(), Label, StatementBlock) { label, it -> NodeBlock(label, it) },
            
            serial(
                IfCore,
                token("if"),
                ParenthesizedStatement,
                node<NodeStatement>(),
            ) { _, condition, body -> ParsedIfCore(condition, body, null) },
            
            serial(
                IfCore,
                token("if"),
                ParenthesizedStatement,
                node<NodeStatement>(),
                token("else"),
                node<NodeStatement>(),
            ) { _, condition, thenBody, _, elseBody -> ParsedIfCore(condition, thenBody, elseBody) },
            
            serial(node<NodeStatement>(), IfCore) { core ->
                NodeIf(null, core.condition, core.thenBody, core.elseBody)
            },
            
            serial(node<NodeStatement>(), Label, IfCore) { label, core ->
                NodeIf(label, core.condition, core.thenBody, core.elseBody)
            },
            
            serial(
                WhileCore,
                token("while"),
                ParenthesizedStatement,
                node<NodeStatement>(),
            ) { _, condition, body -> ParsedWhileCore(condition, body) },
            
            serial(node<NodeStatement>(), WhileCore) { core ->
                NodeWhile(null, core.condition, core.body)
            },
            
            serial(node<NodeStatement>(), Label, WhileCore) { label, core ->
                NodeWhile(label, core.condition, core.body)
            },
            
            serial(
                DoWhileCore,
                token("do"),
                node<NodeStatement>(),
                token("while"),
                ParenthesizedStatement,
            ) { _, body, _, condition -> ParsedDoWhileCore(body, condition) },
            
            serial(node<NodeStatement>(), DoWhileCore) { core ->
                NodeDoWhile(null, core.body, core.condition)
            },
            
            serial(node<NodeStatement>(), Label, DoWhileCore) { label, core ->
                NodeDoWhile(label, core.body, core.condition)
            },
            
            listLike(
                WhenCaseConditionList,
                null,
                node<NodeStatement>(),
                token(","),
                token("->"),
            ),
            
            serial(
                WhenCase,
                WhenCaseConditionList,
                node<NodeStatement>(),
            ) { conditions, body -> NodeCase(conditions, body) },
            
            serial(
                WhenCase,
                token("else"),
                token("->"),
                node<NodeStatement>(),
            ) { _, _, body -> NodeCase(emptyList(), body) },
            
            listLike(
                WhenCaseList,
                token("{"),
                WhenCase,
                null,
                token("}"),
            ),
            
            serial(
                WhenCore,
                token("when"),
                WhenCaseList,
            ) { _, cases -> ParsedWhenCore(null, cases) },
            
            serial(
                WhenCore,
                token("when"),
                ParenthesizedStatement,
                WhenCaseList,
            ) { _, value, cases -> ParsedWhenCore(value, cases) },
            
            serial(node<NodeStatement>(), WhenCore) { core ->
                NodeWhen(null, core.value, core.cases)
            },
            
            serial(node<NodeStatement>(), Label, WhenCore) { label, core ->
                NodeWhen(label, core.value, core.cases)
            },
            
            serial(ThisTypeQualifier, node<NodeType>(), token(".")) { type, _ -> type },
            serial(ReturnTypeClause, token(":"), node<NodeType>()) { _, type -> type },
            
            serial(
                node<NodeTypeAlias>(),
                token("type"),
                TypeName,
                FormalGenericParameterListWithoutConstraints,
                token("="),
                node<NodeType>(),
            ) { _, name, generics, _, type -> NodeTypeAlias(name, generics, type) },
            serial(
                node<NodeTypeAlias>(),
                token("type"),
                TypeName,
                token("="),
                node<NodeType>(),
            ) { _, name, _, type -> NodeTypeAlias(name, null, type) },
            
            serial(
                MethodHead,
                token("def"),
                FormalGenericParameterList,
                ThisTypeQualifier,
                Identifier,
                FormalParameterList,
            ) { _, generics, thisType, name, parameters ->
                ParsedMethodHead(generics, thisType, name, parameters)
            },
            serial(
                MethodHead,
                token("def"),
                FormalGenericParameterList,
                Identifier,
                FormalParameterList,
            ) { _, generics, name, parameters ->
                ParsedMethodHead(generics, null, name, parameters)
            },
            serial(
                MethodHead,
                token("def"),
                ThisTypeQualifier,
                Identifier,
                FormalParameterList,
            ) { _, thisType, name, parameters ->
                ParsedMethodHead(null, thisType, name, parameters)
            },
            serial(
                MethodHead,
                token("def"),
                Identifier,
                FormalParameterList,
            ) { _, name, parameters ->
                ParsedMethodHead(null, null, name, parameters)
            },
            serial(
                node<NodeMethodDefinition>(),
                MethodHead,
                ReturnTypeClause,
                node<NodeStatement>(),
            ) { head, returnType, body ->
                NodeMethodDefinition(head.generics, head.thisType, head.name, head.parameters, returnType, body)
            },
            serial(
                node<NodeMethodDefinition>(),
                MethodHead,
                node<NodeStatement>(),
            ) { head, body ->
                NodeMethodDefinition(head.generics, head.thisType, head.name, head.parameters, null, body)
            },
            
            serial(node<NodeFileElement>(), node<NodeTypeAlias>()) { it },
            serial(node<NodeFileElement>(), node<NodeMethodDefinition>()) { it },
            listLike(FileElementList, null, node<NodeFileElement>(), null, null),
            serial(node<NodeFile>(), FileElementList) { NodeFile(it) },
        ),
    )
}

private data class ParsedIfCore(
    val condition: NodeStatement,
    val thenBody: NodeStatement,
    val elseBody: NodeStatement?,
)

private data class ParsedWhileCore(
    val condition: NodeStatement,
    val body: NodeStatement,
)

private data class ParsedDoWhileCore(
    val body: NodeStatement,
    val condition: NodeStatement,
)

private data class ParsedWhenCore(
    val value: NodeStatement?,
    val cases: List<NodeCase>,
)

private data class ParsedMethodHead(
    val generics: SequencedMap<String, NodeGenericConstraint>?,
    val thisType: NodeType?,
    val name: String,
    val parameters: SequencedMap<String, NodeType>,
)

private fun fixedTokens(vararg texts: String): List<Production<*>> = texts.map { fixed(token(it), it) }

private fun <N> tokenValues(
    result: NonTerminal<N>,
    vararg mappings: Pair<String, N>,
): List<Production<*>> = mappings.map { (text, value) ->
    serial(result, token(text)) { value }
}

private fun <N> regexValue(
    source: NonTerminal<String>,
    pattern: String,
    expectation: String,
    result: NonTerminal<N>,
    parser: (String) -> N,
): List<Production<*>> = listOf(
    regex(source, Regex(pattern), expectation),
    serial(result, source) {
        try {
            parser(it)
        } catch (e: Exception) {
            throw ParseException("Invalid number: $it, cause: ${e.message}")
        }
    },
)

private fun <V : Any> linkedSequencedMap(
    entries: List<Pair<String, V>>,
    itemName: String,
): SequencedMap<String, V> {
    val result = LinkedHashMap<String, V>()
    entries.forEach { (name, value) ->
        if (name in result) throw ParseException("Duplicate $itemName name '$name'")
        result[name] = value
    }
    return result
}

private fun linkedSequencedSet(names: List<String>, itemName: String): SequencedSet<String> {
    val result = LinkedHashSet<String>()
    names.forEach { name ->
        if (name in result) throw ParseException("Duplicate $itemName name '$name'")
        result += name
    }
    return result
}

private val formattedExpressionCache = Collections.synchronizedMap(HashMap<String, NodeStatement>())

private fun parseFormattedExpression(source: String): NodeStatement {
    formattedExpressionCache[source]?.let { return it }
    parseFully(
        source = source,
        productions = FoxProductions,
        root = AssignmentExpression,
        cursor = Cursor(0),
    )?.let {
        formattedExpressionCache[source] = it.node
        return it.node
    }
    val run = parseWithDiagnostics(
        source = source,
        productions = FoxProductions,
        root = AssignmentExpression,
        cursor = Cursor(0),
    )
    throw ParseException("Invalid formatted string expression '$source': ${run.stop.render().trim()}")
}
