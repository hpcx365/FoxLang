package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.parser.*
import pers.hpcx.foxlang.runtime.*
import pers.hpcx.foxlang.type.toFoxTupleType
import pers.hpcx.foxlang.utils.*

// Lexical nodes
private val Word = node<String>().name("Word")
private val Identifier = node<String>().name("Identifier")
private val TypeName = node<String>().name("TypeName")
private val IdentifierEqual = node<String>().name("IdentifierEqual")
private val IdentifierColon = node<String>().name("IdentifierColon")
private val TypeNameEqual = node<String>().name("TypeNameEqual")
private val TypeNameColon = node<String>().name("TypeNameColon")
private val Label = node<String>().name("Label")
private val LineSeparator = node<Unit>().name("LineSeparator")
private val LineBreaks = node<Unit>().name("LineBreaks")

// Literal nodes
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

// Parameter and generic nodes
private val FormalParameter = node<String>().pair(node<FoxType>()).name("FormalParameter")
private val FormalParameterList = node<String>().orderedMap(node<FoxType>()).name("FormalParameterList")
private val ActualParameter = node<String>().optional().pair(node<FoxStatement>()).name("ActualParameter")
private val ActualParameterList = node<String>().optional().pair(node<FoxStatement>()).list().name("ActualParameterList")
private val AnonymousActualParameterList = node<FoxStatement>().list().name("AnonymousActualParameterList")
private val FormalGenericParameter = node<String>().pair(node<FoxType>()).name("FormalGenericParameter")
private val FormalGenericParameterList = node<String>().orderedMap(node<FoxType>()).name("FormalGenericParameterList")
private val FormalGenericParameterListWithoutConstraints = node<String>().orderedSet().name("FormalGenericParameterListWithoutConstraints")
private val ActualGenericParameter = node<String>().optional().pair(node<FoxType>()).name("ActualGenericParameter")
private val ActualGenericParameterList = node<String>().optional().pair(node<FoxType>()).list().name("ActualGenericParameterList")
private val NamedActualGenericParameter = node<String>().pair(node<FoxType>()).name("NamedActualGenericParameter")
private val NamedActualGenericParameterList = node<String>().map(node<FoxType>()).name("NamedActualGenericParameterList")
private val AnonymousActualGenericParameterList = node<FoxType>().list().name("AnonymousActualGenericParameterList")
private val TupleComponentParameter = node<FoxType>().pair(node<Int>()).name("TupleComponentParameter")
private val TupleComponentParameterList = node<FoxType>().pair(node<Int>()).list().name("TupleComponentParameterList")
private val StructFieldParameter = node<String>().pair(node<FoxType>()).name("StructFieldParameter")
private val StructFieldParameterList = node<String>().orderedMap(node<FoxType>()).name("StructFieldParameterList")
private val StructFieldNameList = node<String>().orderedSet().name("StructFieldNameList")
private val ObjectMemberParameterList = node<String>().map(node<FoxType>()).name("ObjectMemberParameterList")
private val ObjectMemberNameSet = node<String>().set().name("ObjectMemberNameSet")
private val EnumItemParameter = node<String>().pair(node<FoxType>()).name("EnumItemParameter")
private val EnumItemParameterList = node<String>().map(node<FoxType>()).name("EnumItemParameterList")
private val EnumItemNameList = node<String>().list().name("EnumItemNameList")
private val MethodTypeArgument = node<ParsedMethodTypeArgument>().name("MethodTypeArgument")
private val MethodTypeArgumentList = node<ParsedMethodTypeArgument>().list().name("MethodTypeArgumentList")

// Expression nodes
private val StatementBlock = node<FoxStatement>().list().name("StatementBlock")
private val ParenthesizedStatement = node<FoxStatement>().name("ParenthesizedStatement")
private val PrimaryExpression = node<FoxStatement>().name("PrimaryExpression")
private val PostfixExpression = node<FoxStatement>().name("PostfixExpression")
private val UnaryExpression = node<FoxStatement>().name("UnaryExpression")
private val MultiplicativeExpression = node<FoxStatement>().name("MultiplicativeExpression")
private val AdditiveExpression = node<FoxStatement>().name("AdditiveExpression")
private val ShiftExpression = node<FoxStatement>().name("ShiftExpression")
private val ComparisonExpression = node<FoxStatement>().name("ComparisonExpression")
private val EqualityExpression = node<FoxStatement>().name("EqualityExpression")
private val BitAndExpression = node<FoxStatement>().name("BitAndExpression")
private val BitXorExpression = node<FoxStatement>().name("BitXorExpression")
private val BitOrExpression = node<FoxStatement>().name("BitOrExpression")
private val LogicalAndExpression = node<FoxStatement>().name("LogicalAndExpression")
private val LogicalOrExpression = node<FoxStatement>().name("LogicalOrExpression")
private val AssignableExpression = node<FoxStatement>().name("AssignableExpression")
private val AssignmentExpression = node<FoxStatement>().name("AssignmentExpression")

// Operator nodes
private val MultiplicativeOperator = node<FoxBinaryOperator>().name("MultiplicativeOperator")
private val AdditiveOperator = node<FoxBinaryOperator>().name("AdditiveOperator")
private val ShiftOperator = node<FoxBinaryOperator>().name("ShiftOperator")
private val ComparisonOperator = node<FoxBinaryOperator>().name("ComparisonOperator")
private val EqualityOperator = node<FoxBinaryOperator>().name("EqualityOperator")
private val BitAndOperator = node<FoxBinaryOperator>().name("BitAndOperator")
private val BitXorOperator = node<FoxBinaryOperator>().name("BitXorOperator")
private val BitOrOperator = node<FoxBinaryOperator>().name("BitOrOperator")
private val LogicalAndOperator = node<FoxBinaryOperator>().name("LogicalAndOperator")
private val LogicalOrOperator = node<FoxBinaryOperator>().name("LogicalOrOperator")

// Control-flow nodes
private val WhenCaseConditionList = node<FoxStatement>().list().name("WhenCaseConditionList")
private val WhenCase = node<FoxCase>().name("WhenCase")
private val WhenCaseList = node<FoxCase>().list().name("WhenCaseList")
private val IfCore = node<FoxIf>().name("IfCore")
private val WhileCore = node<FoxWhile>().name("WhileCore")
private val DoWhileCore = node<FoxDoWhile>().name("DoWhileCore")
private val WhenCore = node<FoxWhen>().name("WhenCore")

// Top-level nodes
private val ThisTypeQualifier = node<FoxType>().name("ThisTypeQualifier")
private val ReturnTypeClause = node<FoxType>().name("ReturnTypeClause")
private val MethodHead = node<FoxMethodDefinition>().name("MethodHead")

private val ReservedKeywords = setOf(
    "const", "type", "def", "this", "if", "else", "when", "new", "yield", "return", "for", "in",
    "do", "while", "break", "continue", "try", "finally", "import", "unit", "true", "false",
    
    "Void", "Unit", "Bool", "Byte", "Short", "Int", "Long", "Float", "Double", "Char",
    "String", "Tuple", "Struct", "Object", "Enum", "Array", "Ref", "Method",
    "Any", "AnyOf", "AllOf", "NoneOf", "AnyTuple", "AnyTupleOf", "AnyStruct", "AnyStructOf", "AnyObject", "AnyEnum",
    "ComponentAt", "LastComponentAt", "FirstComponentsOf", "ExactFirstComponentsOf", "LastComponentsOf", "ExactLastComponentsOf",
    "DropFirstComponentsOf", "ExactDropFirstComponentsOf", "DropLastComponentsOf", "ExactDropLastComponentsOf", "MergeComponentsOf",
    "FieldOf", "FieldAt", "LastFieldAt", "FirstFieldsOf", "ExactFirstFieldsOf", "LastFieldsOf", "ExactLastFieldsOf",
    "DropFirstFieldsOf", "ExactDropFirstFieldsOf", "DropLastFieldsOf", "ExactDropLastFieldsOf", "FieldsOf", "DropFieldsOf", "MergeFieldsOf",
    "MemberOf", "MembersOf", "DropMembersOf", "MergeMembersOf",
    "ItemOf", "ItemsOf", "DropItemsOf", "MergeItemsOf",
    "ElementOf", "ReferentOf", "MethodOf", "ThisOf", "ParametersOf", "ReturnOf",
)

val FoxGrammar = buildGrammar {
    fun fixedTokens(vararg tokens: String) {
        tokens.forEach { token -> target(token(token)) { fixed(token) { it.text } } }
    }
    
    fun <N> GrammarBuilder.ProductionListBuilder<N>.tokenValues(vararg mappings: Pair<String, N>) {
        mappings.forEach { (text, value) ->
            sequence(token(text)) { value }
        }
    }
    
    fun <N> regexValue(
        source: NonTerminal<String>,
        pattern: String,
        result: NonTerminal<N>,
        parser: (String) -> N,
    ) {
        target(source) { regex(Regex(pattern)) { it.text } }
        target(result) {
            sequence(source) {
                try {
                    parser(it)
                } catch (e: Exception) {
                    throw ParseException("Invalid token: $it, cause: ${e.message}")
                }
            }
        }
    }
    
    fixedTokens(*ReservedKeywords.toTypedArray())
    fixedTokens(
        "(", ")", "[", "]", "{", "}", ".", ":", ";", ",",
        "+", "-", "*", "/", "%", "`", "~", "?", "!", "@", "#", "$", "&", "|", "^",
        "<", ">", "==", "!=", "<=", ">=", "&&", "||", "<<", ">>", ">>>", "->",
        "=", ":=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=", "&&=", "||=",
    )
    
    target(node<FoxUnaryOperator>()) {
        tokenValues(
            "!" to FoxNotOperator,
            "-" to FoxNegOperator,
        )
    }
    
    target(node<FoxBinaryOperator>()) {
        tokenValues(
            "+" to FoxAddOperator,
            "-" to FoxSubOperator,
            "*" to FoxMulOperator,
            "/" to FoxDivOperator,
            "%" to FoxRemOperator,
            "&" to FoxAndOperator,
            "|" to FoxOrOperator,
            "^" to FoxXorOperator,
            "<<" to FoxShlOperator,
            ">>" to FoxShrOperator,
            ">>>" to FoxUshrOperator,
            "==" to FoxEqOperator,
            "!=" to FoxNeqOperator,
            "<" to FoxLtOperator,
            ">" to FoxGtOperator,
            "<=" to FoxLeOperator,
            ">=" to FoxGeOperator,
            "&&" to FoxAndAndOperator,
            "||" to FoxOrOrOperator,
        )
    }
    
    target(node<FoxAssignOperator>()) {
        tokenValues(
            "=" to FoxPlainAssignOperator,
            ":=" to FoxTypeBindingAssignOperator,
            "+=" to FoxAddAssignOperator,
            "-=" to FoxSubAssignOperator,
            "*=" to FoxMulAssignOperator,
            "/=" to FoxDivAssignOperator,
            "%=" to FoxRemAssignOperator,
            "&=" to FoxAndAssignOperator,
            "|=" to FoxOrAssignOperator,
            "^=" to FoxXorAssignOperator,
            "<<=" to FoxShlAssignOperator,
            ">>=" to FoxShrAssignOperator,
            ">>>=" to FoxUshrAssignOperator,
            "&&=" to FoxAndAndAssignOperator,
            "||=" to FoxOrOrAssignOperator,
        )
    }
    
    target(MultiplicativeOperator) {
        tokenValues(
            "*" to FoxMulOperator,
            "/" to FoxDivOperator,
            "%" to FoxRemOperator,
        )
    }
    target(AdditiveOperator) {
        tokenValues(
            "+" to FoxAddOperator,
            "-" to FoxSubOperator,
        )
    }
    target(ShiftOperator) {
        tokenValues(
            "<<" to FoxShlOperator,
            ">>" to FoxShrOperator,
            ">>>" to FoxUshrOperator,
        )
    }
    target(ComparisonOperator) {
        tokenValues(
            "<" to FoxLtOperator,
            ">" to FoxGtOperator,
            "<=" to FoxLeOperator,
            ">=" to FoxGeOperator,
        )
    }
    target(EqualityOperator) {
        tokenValues(
            "==" to FoxEqOperator,
            "!=" to FoxNeqOperator,
        )
    }
    target(BitAndOperator) {
        tokenValues("&" to FoxAndOperator)
    }
    target(BitXorOperator) {
        tokenValues("^" to FoxXorOperator)
    }
    target(BitOrOperator) {
        tokenValues("|" to FoxOrOperator)
    }
    target(LogicalAndOperator) {
        tokenValues("&&" to FoxAndAndOperator)
    }
    target(LogicalOrOperator) {
        tokenValues("||" to FoxOrOrOperator)
    }
    
    target(Word) { regex(Regex("[a-zA-Z0-9_]+")) { it.text } }
    target(LineSeparator) { lineSeparator { } }
    target(LineBreaks) { listLike(LineSeparator, LineSeparator, null, null) { } }
    target(Identifier) {
        sequence(Word) {
            if (it in ReservedKeywords) throw ParseException("'$it' is a reserved keyword")
            if (it[0] !in 'a'..'z') throw ParseException("'${it[0]}' is not a valid first character of an identifier")
            it
        }
    }
    target(TypeName) {
        sequence(Word) {
            if (it in ReservedKeywords) throw ParseException("'$it' is a reserved keyword")
            if (it[0] !in 'A'..'Z') throw ParseException("'${it[0]}' is not a valid first character of a type name")
            it
        }
    }
    target(IdentifierEqual) { sequence(Identifier, token("=")) { it, _ -> it } }
    target(IdentifierColon) { sequence(Identifier, token(":")) { it, _ -> it } }
    target(TypeNameEqual) { sequence(TypeName, token("=")) { it, _ -> it } }
    target(TypeNameColon) { sequence(TypeName, token(":")) { it, _ -> it } }
    
    target(node<FoxType>()) {
        tokenValues(
            "Void" to FoxVoidType,
            "Unit" to FoxUnitType,
            "Bool" to FoxBoolType,
            "Byte" to FoxByteType,
            "Short" to FoxShortType,
            "Int" to FoxIntType,
            "Long" to FoxLongType,
            "Float" to FoxFloatType,
            "Double" to FoxDoubleType,
            "Char" to FoxCharType,
            "String" to FoxStringType,
            "Any" to FoxAnyType,
            "AnyTuple" to FoxAnyTupleType,
            "AnyStruct" to FoxAnyStructType,
            "AnyObject" to FoxAnyObjectType,
            "AnyEnum" to FoxAnyEnumType,
        )
    }
    
    target(node<Unit>()) { sequence(token("unit")) { } }
    target(node<Boolean>()) { sequence(token("true")) { true } }
    target(node<Boolean>()) { sequence(token("false")) { false } }
    target(node<Char>()) { charLiteral { it.char } }
    target(node<String>()) { stringLiteral { it.string } }
    
    regexValue(BinInt, "0b[01]+(_[01]+)*", node<Int>()) { it.drop(2).replace("_", "").toInt(2) }
    regexValue(DecInt, "(0|[1-9][0-9]*(_[0-9]+)*)", node<Int>()) { it.replace("_", "").toInt() }
    regexValue(HexInt, "0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*", node<Int>()) { it.drop(2).replace("_", "").toInt(16) }
    regexValue(BinLong, "0b[01]+(_[01]+)*L", node<Long>()) { it.drop(2).dropLast(1).replace("_", "").toLong(2) }
    regexValue(DecLong, "(0|[1-9][0-9]*(_[0-9]+)*)L", node<Long>()) { it.dropLast(1).replace("_", "").toLong() }
    regexValue(HexLong, "0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*L", node<Long>()) { it.drop(2).dropLast(1).replace("_", "").toLong(16) }
    regexValue(DecFloat, "(0|[1-9][0-9]*(_[0-9]+)*)(\\.[0-9]+(_[0-9]+)*)?(e[+-]?[0-9]+)?f", node<Float>()) { it.dropLast(1).replace("_", "").toFloat() }
    regexValue(HexFloat, "0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*(\\.[0-9a-fA-F]+(_[0-9a-fA-F]+)*)?p[+-]?[0-9]+f", node<Float>()) { it.dropLast(1).replace("_", "").toFloat() }
    regexValue(DecDouble, "(0|[1-9][0-9]*(_[0-9]+)*)(\\.[0-9]+(_[0-9]+)*)?(e[+-]?[0-9]+)?", node<Double>()) { it.replace("_", "").toDouble() }
    regexValue(HexDouble, "0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*(\\.[0-9a-fA-F]+(_[0-9a-fA-F]+)*)?p[+-]?[0-9]+", node<Double>()) { it.replace("_", "").toDouble() }
    
    target(node<FoxEntityStatement>()) {
        sequence(node<Unit>()) { FoxEntityStatement(FoxUnit) }
        sequence(node<Boolean>()) { FoxEntityStatement(FoxBool(it)) }
        sequence(node<Int>()) { FoxEntityStatement(FoxInt(it)) }
        sequence(node<Long>()) { FoxEntityStatement(FoxLong(it)) }
        sequence(node<Float>()) { FoxEntityStatement(FoxFloat(it)) }
        sequence(node<Double>()) { FoxEntityStatement(FoxDouble(it)) }
        sequence(node<Char>()) { FoxEntityStatement(FoxChar(it)) }
        sequence(node<String>()) { FoxEntityStatement(FoxString(it)) }
    }
    
    target(node<FoxType>()) {
        sequence(token("AnyOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxAnyOfType(it)
        }
        sequence(token("AllOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxAllOfType(it)
        }
        sequence(token("NoneOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxNoneOfType(it)
        }
        sequence(token("Tuple"), TupleComponentParameterList) { _, it ->
            it.toFoxTupleType()
        }
        sequence(token("AnyTupleOf"), AnonymousActualGenericParameterList) { _, it ->
            if (it.size != 1) throw ParseException("AnyTupleOf type must have exactly one generic parameter")
            FoxAnyTupleOfType(it.first())
        }
        sequence(token("Struct"), StructFieldParameterList) { _, it ->
            FoxStructType(it)
        }
        sequence(token("AnyStructOf"), AnonymousActualGenericParameterList) { _, it ->
            if (it.isEmpty()) throw ParseException("AnyStructOf type must have at least one generic parameter")
            FoxAnyStructOfType(it)
        }
        sequence(token("Object"), ObjectMemberParameterList) { _, it ->
            FoxObjectType(it)
        }
        sequence(token("Enum"), EnumItemParameterList) { _, it ->
            FoxEnumType(it)
        }
        sequence(token("Array"), AnonymousActualGenericParameterList) { _, it ->
            if (it.size != 1) throw ParseException("Array type must have exactly one generic parameter")
            FoxArrayType(it.first())
        }
        sequence(token("Ref"), AnonymousActualGenericParameterList) { _, it ->
            if (it.size != 1) throw ParseException("Ref type must have exactly one generic parameter")
            FoxRefType(it.first())
        }
        sequence(token("Method"), MethodTypeArgumentList) { _, items ->
            items.toFoxMethodType()
        }
        sequence(token("ComponentAt"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, index, _ ->
            FoxTupleComponentAtType(type, index)
        }
        sequence(token("LastComponentAt"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, index, _ ->
            FoxTupleLastComponentAtType(type, index)
        }
        sequence(token("FirstComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleFirstComponentsOfType(type, count)
        }
        sequence(token("ExactFirstComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleExactFirstComponentsOfType(type, count)
        }
        sequence(token("LastComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleLastComponentsOfType(type, count)
        }
        sequence(token("ExactLastComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleExactLastComponentsOfType(type, count)
        }
        sequence(token("DropFirstComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleDropFirstComponentsOfType(type, count)
        }
        sequence(token("ExactDropFirstComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleExactDropFirstComponentsOfType(type, count)
        }
        sequence(token("DropLastComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleDropLastComponentsOfType(type, count)
        }
        sequence(token("ExactDropLastComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleExactDropLastComponentsOfType(type, count)
        }
        sequence(token("MergeComponentsOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxTupleMergeComponentsOfType(it)
        }
        sequence(token("FieldOf"), token("<"), node<FoxType>(), token(","), Identifier, token(">")) { _, _, type, _, name, _ ->
            FoxStructFieldOfType(type, name)
        }
        sequence(token("FieldAt"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, index, _ ->
            FoxStructFieldAtType(type, index)
        }
        sequence(token("LastFieldAt"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, index, _ ->
            FoxStructLastFieldAtType(type, index)
        }
        sequence(token("FirstFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructFirstFieldsOfType(type, count)
        }
        sequence(token("ExactFirstFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructExactFirstFieldsOfType(type, count)
        }
        sequence(token("LastFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructLastFieldsOfType(type, count)
        }
        sequence(token("ExactLastFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructExactLastFieldsOfType(type, count)
        }
        sequence(token("DropFirstFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructDropFirstFieldsOfType(type, count)
        }
        sequence(token("ExactDropFirstFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructExactDropFirstFieldsOfType(type, count)
        }
        sequence(token("DropLastFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructDropLastFieldsOfType(type, count)
        }
        sequence(token("ExactDropLastFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructExactDropLastFieldsOfType(type, count)
        }
        sequence(token("FieldsOf"), token("<"), node<FoxType>(), token(","), StructFieldNameList, token(">")) { _, _, type, _, names, _ ->
            FoxStructFieldsOfType(type, names)
        }
        sequence(token("DropFieldsOf"), token("<"), node<FoxType>(), token(","), StructFieldNameList, token(">")) { _, _, type, _, names, _ ->
            FoxStructDropFieldsOfType(type, names.toSet())
        }
        sequence(token("MergeFieldsOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxStructMergeFieldsOfType(it)
        }
        sequence(token("MemberOf"), token("<"), node<FoxType>(), token(","), Identifier, token(">")) { _, _, type, _, name, _ ->
            FoxObjectMemberOfType(type, name)
        }
        sequence(token("MembersOf"), token("<"), node<FoxType>(), token(","), ObjectMemberNameSet, token(">")) { _, _, type, _, names, _ ->
            FoxObjectMembersOfType(type, names)
        }
        sequence(token("DropMembersOf"), token("<"), node<FoxType>(), token(","), ObjectMemberNameSet, token(">")) { _, _, type, _, names, _ ->
            FoxObjectDropMembersOfType(type, names)
        }
        sequence(token("MergeMembersOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxObjectMergeMembersOfType(it)
        }
        sequence(token("ItemOf"), token("<"), node<FoxType>(), token(","), TypeName, token(">")) { _, _, type, _, name, _ ->
            FoxEnumItemOfType(type, name)
        }
        sequence(token("ItemsOf"), token("<"), node<FoxType>(), token(","), EnumItemNameList, token(">")) { _, _, type, _, names, _ ->
            FoxEnumItemsOfType(type, names)
        }
        sequence(token("DropItemsOf"), token("<"), node<FoxType>(), token(","), EnumItemNameList, token(">")) { _, _, type, _, names, _ ->
            FoxEnumDropItemsOfType(type, names)
        }
        sequence(token("MergeItemsOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxEnumMergeItemsOfType(it)
        }
        sequence(token("ElementOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
            FoxArrayElementOfType(type)
        }
        sequence(token("ReferentOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
            FoxRefReferentOfType(type)
        }
        sequence(
            token("MethodOf"),
            token("<"),
            node<FoxType>(),
            token(","),
            node<FoxType>(),
            token(","),
            node<FoxType>(),
            token(">"),
        ) { _, _, `this`, _, parameters, _, `return`, _ ->
            FoxMethodOfType(`this`, parameters, `return`)
        }
        sequence(token("ThisOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
            FoxMethodThisOfType(type)
        }
        sequence(token("ParametersOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
            FoxMethodParametersOfType(type)
        }
        sequence(token("ReturnOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
            FoxMethodReturnOfType(type)
        }
        sequence(TypeName) {
            FoxUnresolvedType(it, null)
        }
        sequence(TypeName, AnonymousActualGenericParameterList) { name, it ->
            FoxUnresolvedType(name, it)
        }
    }
    
    target(FormalParameter) {
        sequence(IdentifierColon, node<FoxType>()) { name, type -> name to type }
    }
    target(FormalParameterList) {
        listLike(token("("), FormalParameter, token(","), token(")")) { it.toOrderedMap("formal parameter") }
    }
    target(ActualParameter) {
        sequence(node<FoxStatement>()) { null to it }
    }
    target(ActualParameter) {
        sequence(IdentifierEqual, node<FoxStatement>()) { name, value -> name to value }
    }
    target(ActualParameterList) {
        listLike(token("("), ActualParameter, token(","), token(")")) { it }
    }
    target(AnonymousActualParameterList) {
        listLike(token("("), node<FoxStatement>(), token(","), token(")")) { it }
    }
    target(FormalGenericParameter) {
        sequence(TypeName) { it to FoxAnyType }
    }
    target(FormalGenericParameter) {
        sequence(TypeName, token("="), node<FoxType>()) { name, _, constraint -> name to constraint }
    }
    target(FormalGenericParameterList) {
        listLike(token("<"), FormalGenericParameter, token(","), token(">")) { it.toOrderedMap("formal generic parameter") }
    }
    target(FormalGenericParameterListWithoutConstraints) {
        listLike(token("<"), TypeName, token(","), token(">")) { it.toOrderedSet("formal generic parameter") }
    }
    target(ActualGenericParameter) {
        sequence(node<FoxType>()) { null to it }
    }
    target(ActualGenericParameter) {
        sequence(TypeNameEqual, node<FoxType>()) { name, type -> name to type }
    }
    target(ActualGenericParameterList) {
        listLike(token("<"), ActualGenericParameter, token(","), token(">")) { it }
    }
    target(NamedActualGenericParameter) {
        sequence(TypeNameEqual, node<FoxType>()) { name, type -> name to type }
    }
    target(NamedActualGenericParameterList) {
        listLike(token("<"), NamedActualGenericParameter, token(","), token(">")) { it.toMap("actual generic parameter") }
    }
    target(AnonymousActualGenericParameterList) {
        listLike(token("<"), node<FoxType>(), token(","), token(">")) { it }
    }
    target(TupleComponentParameter) {
        sequence(node<FoxType>()) { it to 1 }
    }
    target(TupleComponentParameter) {
        sequence(node<FoxType>(), token(":"), node<Int>()) { type, _, count ->
            if (count <= 0) throw ParseException("Tuple component count must be positive")
            type to count
        }
    }
    target(TupleComponentParameterList) {
        listLike(token("<"), TupleComponentParameter, token(","), token(">")) { it }
    }
    target(StructFieldParameter) {
        sequence(IdentifierColon, node<FoxType>()) { name, type -> name to type }
    }
    target(StructFieldParameterList) {
        listLike(token("<"), StructFieldParameter, token(","), token(">")) { it.toOrderedMap("struct field parameter") }
    }
    target(ObjectMemberParameterList) {
        listLike(token("<"), StructFieldParameter, token(","), token(">")) { it.toMap("object member parameter") }
    }
    target(EnumItemParameter) {
        sequence(TypeNameEqual, node<FoxType>()) { name, type -> name to type }
    }
    target(EnumItemParameterList) {
        listLike(token("<"), EnumItemParameter, token(","), token(">")) { it.toMap("enum item type parameter") }
    }
    target(StructFieldNameList) {
        listLike(null, Identifier, token(","), null) { it.toOrderedSet("struct field name") }
    }
    target(ObjectMemberNameSet) {
        listLike(null, Identifier, token(","), null) { it.toSet("object member name") }
    }
    target(EnumItemNameList) {
        listLike(null, TypeName, token(","), null) { it }
    }
    target(MethodTypeArgument) {
        sequence(token("this"), token(":"), node<FoxType>()) { _, _, type ->
            ParsedMethodTypeArgument.This(type)
        }
    }
    target(MethodTypeArgument) {
        sequence(token("return"), token(":"), node<FoxType>()) { _, _, type ->
            ParsedMethodTypeArgument.Return(type)
        }
    }
    target(MethodTypeArgument) {
        sequence(FormalParameter) { (name, type) ->
            ParsedMethodTypeArgument.Parameter(name, type)
        }
    }
    target(MethodTypeArgument) {
        sequence(node<FoxType>()) { type ->
            ParsedMethodTypeArgument.AnonymousType(type)
        }
    }
    target(MethodTypeArgumentList) { listLike(token("<"), MethodTypeArgument, token(","), token(">")) { it } }
    
    target(Label) { sequence(token("#"), Identifier) { _, it -> it } }
    target(ParenthesizedStatement) { sequence(token("("), node<FoxStatement>(), token(")")) { _, node, _ -> node } }
    
    target(PrimaryExpression) {
        sequence(token("this")) { FoxThis }
        sequence(Identifier) { FoxSymbol(it) }
        sequence(ParenthesizedStatement) { it }
        sequence(node<FoxEntityStatement>()) { it }
    }
    
    target(PostfixExpression) {
        sequence(PrimaryExpression) { it }
        sequence(PostfixExpression, token("."), Identifier) { target, _, name ->
            FoxFieldAccess(target, name)
        }
        sequence(PostfixExpression, token("."), node<Int>()) { target, _, index ->
            FoxComponentAccess(target, index)
        }
        sequence(Identifier, ActualGenericParameterList, ActualParameterList) { name, generics, parameters ->
            FoxCall(FoxEntityStatement(FoxUnit), name, generics, parameters)
        }
        sequence(Identifier, ActualParameterList) { name, parameters ->
            FoxCall(FoxEntityStatement(FoxUnit), name, null, parameters)
        }
        sequence(PostfixExpression, token("."), Identifier, ActualGenericParameterList, ActualParameterList) { target, _, name, generics, parameters ->
            FoxCall(target, name, generics, parameters)
        }
        sequence(PostfixExpression, token("."), Identifier, ActualParameterList) { target, _, name, parameters ->
            FoxCall(target, name, null, parameters)
        }
        sequence(node<FoxType>(), ActualParameterList) { type, parameters ->
            FoxConstruct(type, parameters)
        }
        sequence(ParenthesizedStatement, AnonymousActualParameterList) { method, parameters ->
            FoxIndirectCall(FoxEntityStatement(FoxUnit), method, parameters)
        }
        sequence(PostfixExpression, token("."), ParenthesizedStatement, AnonymousActualParameterList) { target, _, method, parameters ->
            FoxIndirectCall(target, method, parameters)
        }
    }
    
    target(UnaryExpression) {
        sequence(PostfixExpression) { it }
        sequence(node<FoxUnaryOperator>(), UnaryExpression) { operator, node ->
            FoxUnary(operator, node)
        }
    }
    
    target(MultiplicativeExpression) {
        sequence(UnaryExpression) { it }
        sequence(MultiplicativeExpression, MultiplicativeOperator, UnaryExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    target(AdditiveExpression) {
        sequence(MultiplicativeExpression) { it }
        sequence(AdditiveExpression, AdditiveOperator, MultiplicativeExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    target(ShiftExpression) {
        sequence(AdditiveExpression) { it }
        sequence(ShiftExpression, ShiftOperator, AdditiveExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    target(ComparisonExpression) {
        sequence(ShiftExpression) { it }
        sequence(ComparisonExpression, ComparisonOperator, ShiftExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    target(EqualityExpression) {
        sequence(ComparisonExpression) { it }
        sequence(EqualityExpression, EqualityOperator, ComparisonExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    target(BitAndExpression) {
        sequence(EqualityExpression) { it }
        sequence(BitAndExpression, BitAndOperator, EqualityExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    target(BitXorExpression) {
        sequence(BitAndExpression) { it }
        sequence(BitXorExpression, BitXorOperator, BitAndExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    target(BitOrExpression) {
        sequence(BitXorExpression) { it }
        sequence(BitOrExpression, BitOrOperator, BitXorExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    target(LogicalAndExpression) {
        sequence(BitOrExpression) { it }
        sequence(LogicalAndExpression, LogicalAndOperator, BitOrExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    target(LogicalOrExpression) {
        sequence(LogicalAndExpression) { it }
        sequence(LogicalOrExpression, LogicalOrOperator, LogicalAndExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    target(AssignableExpression) {
        sequence(PostfixExpression) { it }
    }
    
    target(AssignmentExpression) {
        sequence(LogicalOrExpression) { it }
        sequence(AssignableExpression, node<FoxAssignOperator>(), node<FoxStatement>()) { left, operator, right ->
            FoxAssign(left, operator, right, beforeEvaluation = true)
        }
    }
    
    target(StatementBlock) {
        listLike(token("{"), node<FoxStatement>(), null, token("}")) { it }
    }
    
    target(node<FoxStatement>()) {
        sequence(AssignmentExpression) { it }
        sequence(Identifier, token(":"), node<FoxType>()) { name, _, type -> FoxTypeBinding(name, type) }
        sequence(token("break")) { FoxBreak(null) }
        sequence(token("break"), Label) { _, label -> FoxBreak(label) }
        sequence(token("continue")) { FoxContinue(null) }
        sequence(token("continue"), Label) { _, label -> FoxContinue(label) }
        sequence(token("return")) { FoxReturn(null) }
        sequence(token("return"), node<FoxStatement>()) { _, value -> FoxReturn(value) }
        sequence(token("yield"), node<FoxStatement>()) { _, value -> FoxYield(null, value) }
        sequence(token("yield"), Label, node<FoxStatement>()) { _, label, value -> FoxYield(label, value) }
        sequence(node<FoxBlock>()) { it }
        sequence(IfCore) { core -> FoxIf(null, core.condition, core.thenBody, core.elseBody) }
        sequence(WhileCore) { core -> FoxWhile(null, core.condition, core.body) }
        sequence(DoWhileCore) { core -> FoxDoWhile(null, core.body, core.condition) }
        sequence(WhenCore) { core -> FoxWhen(null, core.value, core.cases) }
        sequence(Label, IfCore) { label, core -> FoxIf(label, core.condition, core.thenBody, core.elseBody) }
        sequence(Label, WhileCore) { label, core -> FoxWhile(label, core.condition, core.body) }
        sequence(Label, DoWhileCore) { label, core -> FoxDoWhile(label, core.body, core.condition) }
        sequence(Label, WhenCore) { label, core -> FoxWhen(label, core.value, core.cases) }
    }
    
    target(node<FoxBlock>()) {
        sequence(StatementBlock) { FoxBlock(null, it) }
        sequence(Label, StatementBlock) { label, it -> FoxBlock(label, it) }
    }
    
    target(IfCore) {
        sequence(
            token("if"),
            ParenthesizedStatement,
            node<FoxStatement>(),
        ) { _, condition, body -> FoxIf(null, condition, body, null) }
        sequence(
            token("if"),
            ParenthesizedStatement,
            node<FoxStatement>(),
            token("else"),
            node<FoxStatement>(),
        ) { _, condition, thenBody, _, elseBody -> FoxIf(null, condition, thenBody, elseBody) }
    }
    
    target(WhileCore) {
        sequence(
            token("while"),
            ParenthesizedStatement,
            node<FoxStatement>(),
        ) { _, condition, body -> FoxWhile(null, condition, body) }
    }
    
    target(DoWhileCore) {
        sequence(
            token("do"),
            node<FoxStatement>(),
            token("while"),
            ParenthesizedStatement,
        ) { _, body, _, condition -> FoxDoWhile(null, body, condition) }
    }
    
    target(WhenCaseConditionList) {
        listLike(
            null,
            node<FoxStatement>(),
            token(","),
            token("->"),
        ) { it }
    }
    
    target(WhenCase) {
        sequence(
            WhenCaseConditionList,
            node<FoxStatement>(),
        ) { conditions, body -> FoxCase(conditions, body) }
        sequence(
            token("else"),
            token("->"),
            node<FoxStatement>(),
        ) { _, _, body -> FoxCase(emptyList(), body) }
    }
    
    target(WhenCaseList) {
        listLike(
            token("{"),
            WhenCase,
            null,
            token("}"),
        ) { it }
    }
    
    target(WhenCore) {
        sequence(
            token("when"),
            WhenCaseList,
        ) { _, cases -> FoxWhen(null, null, cases) }
        sequence(
            token("when"),
            ParenthesizedStatement,
            WhenCaseList,
        ) { _, value, cases -> FoxWhen(null, value, cases) }
    }
    
    target(node<FoxTypeAlias>()) {
        sequence(
            token("type"),
            TypeName,
            FormalGenericParameterListWithoutConstraints,
            token("="),
            node<FoxType>(),
        ) { _, name, generics, _, type -> FoxTypeAlias(name, generics, type) }
        sequence(
            token("type"),
            TypeName,
            token("="),
            node<FoxType>(),
        ) { _, name, _, type -> FoxTypeAlias(name, emptyOrderedSet(), type) }
    }
    
    target(ThisTypeQualifier) { sequence(node<FoxType>(), token(".")) { type, _ -> type } }
    target(ReturnTypeClause) { sequence(token(":"), node<FoxType>()) { _, type -> type } }
    
    target(MethodHead) {
        sequence(
            token("def"),
            FormalGenericParameterList,
            ThisTypeQualifier,
            Identifier,
            FormalParameterList,
        ) { _, generics, thisType, name, parameters ->
            FoxMethodDefinition(generics, thisType, name, parameters, FoxUnitType, FoxEntityStatement(FoxUnit))
        }
        sequence(
            token("def"),
            FormalGenericParameterList,
            Identifier,
            FormalParameterList,
        ) { _, generics, name, parameters ->
            FoxMethodDefinition(generics, FoxUnitType, name, parameters, FoxUnitType, FoxEntityStatement(FoxUnit))
        }
        sequence(
            token("def"),
            ThisTypeQualifier,
            Identifier,
            FormalParameterList,
        ) { _, thisType, name, parameters ->
            FoxMethodDefinition(emptyOrderedMap(), thisType, name, parameters, FoxUnitType, FoxEntityStatement(FoxUnit))
        }
        sequence(
            token("def"),
            Identifier,
            FormalParameterList,
        ) { _, name, parameters ->
            FoxMethodDefinition(emptyOrderedMap(), FoxUnitType, name, parameters, FoxUnitType, FoxEntityStatement(FoxUnit))
        }
    }
    
    target(node<FoxMethodDefinition>()) {
        sequence(
            MethodHead,
            ReturnTypeClause,
            node<FoxBlock>(),
        ) { head, returnType, body -> FoxMethodDefinition(head.generics, head.thisType, head.name, head.parameters, returnType, body) }
        sequence(
            MethodHead,
            node<FoxBlock>(),
        ) { head, body -> FoxMethodDefinition(head.generics, head.thisType, head.name, head.parameters, FoxUnitType, body) }
    }
    
    target(node<FoxFileElement>()) {
        sequence(node<FoxTypeAlias>()) { it }
        sequence(node<FoxMethodDefinition>()) { it }
    }
    
    target(node<FoxFile>()) {
        listLike(null, node<FoxFileElement>(), null, null) { FoxFile(it) }
    }
}

val FoxFileParser = Parser(FoxGrammar, node<FoxFile>())

private sealed interface ParsedMethodTypeArgument {
    data class This(val type: FoxType) : ParsedMethodTypeArgument
    data class Return(val type: FoxType) : ParsedMethodTypeArgument
    data class Parameter(val name: String, val type: FoxType) : ParsedMethodTypeArgument
    data class AnonymousType(val type: FoxType) : ParsedMethodTypeArgument
}

private fun List<String>.toSet(itemName: String): Set<String> {
    val result = LinkedHashSet<String>()
    forEach { name ->
        if (name in result) throw ParseException("Duplicate $itemName name '$name'")
        result += name
    }
    return result
}

private fun <V : Any> List<Pair<String, V>>.toMap(itemName: String): Map<String, V> {
    val result = LinkedHashMap<String, V>()
    forEach { (name, value) ->
        if (name in result) throw ParseException("Duplicate $itemName name '$name'")
        result[name] = value
    }
    return result
}

private fun List<String>.toOrderedSet(itemName: String): OrderedSet<String> {
    val result = mutableOrderedSetOf<String>()
    forEach { name ->
        if (name in result) throw ParseException("Duplicate $itemName name '$name'")
        result += name
    }
    return result
}

private fun <V : Any> List<Pair<String, V>>.toOrderedMap(itemName: String): OrderedMap<String, V> {
    val result = mutableOrderedMapOf<String, V>()
    forEach { (name, value) ->
        if (name in result) throw ParseException("Duplicate $itemName name '$name'")
        result[name] = value
    }
    return result
}

private fun List<ParsedMethodTypeArgument>.toFoxMethodType(): FoxMethodType {
    if (count { it is ParsedMethodTypeArgument.This } > 1) {
        throw ParseException("Method type cannot declare more than one 'this' type")
    }
    if (count { it is ParsedMethodTypeArgument.Return } > 1) {
        throw ParseException("Method type cannot declare more than one 'return' type")
    }
    forEachIndexed { index, item ->
        if (item is ParsedMethodTypeArgument.This && index != 0) {
            throw ParseException("Method type 'this' must be the first item")
        }
        if (item is ParsedMethodTypeArgument.Return && index != lastIndex) {
            throw ParseException("Method type 'return' must be the last item")
        }
    }
    if (size == 1 && firstOrNull() is ParsedMethodTypeArgument.AnonymousType) {
        throw ParseException("Method<T> is ambiguous; use 'this: T' or 'return: T' explicitly")
    }
    
    var start = 0
    val thisType = when (val first = firstOrNull()) {
        is ParsedMethodTypeArgument.This -> {
            start = 1
            first.type
        }
        is ParsedMethodTypeArgument.AnonymousType -> {
            start = 1
            first.type
        }
        else -> FoxUnitType
    }
    
    var endExclusive = size
    val returnType = when (val last = lastOrNull()) {
        is ParsedMethodTypeArgument.Return -> {
            endExclusive -= 1
            last.type
        }
        is ParsedMethodTypeArgument.AnonymousType -> {
            endExclusive -= 1
            last.type
        }
        else -> FoxUnitType
    }
    
    val parameters = mutableOrderedMapOf<String, FoxType>()
    subList(start, endExclusive).forEach { item ->
        when (item) {
            is ParsedMethodTypeArgument.Parameter -> {
                if (item.name in parameters) {
                    throw ParseException("Duplicate method type parameter name '${item.name}'")
                }
                parameters[item.name] = item.type
            }
            is ParsedMethodTypeArgument.This ->
                throw ParseException("Method type 'this' must be the first item")
            is ParsedMethodTypeArgument.Return ->
                throw ParseException("Method type 'return' must be the last item")
            is ParsedMethodTypeArgument.AnonymousType ->
                throw ParseException("Anonymous method type items may only appear as leading 'this' or trailing 'return'")
        }
    }
    return FoxMethodType(thisType, parameters, returnType)
}
