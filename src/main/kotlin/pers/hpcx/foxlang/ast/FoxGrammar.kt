package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.parser.*
import pers.hpcx.foxlang.runtime.*
import pers.hpcx.foxlang.type.toFoxTupleType
import pers.hpcx.foxlang.utils.*

// Lexical nodes
private val Identifier = node<String>().name("Identifier")
private val TypeName = node<String>().name("TypeName")
private val IdentifierEqual = node<String>().name("IdentifierEqual")
private val IdentifierColon = node<String>().name("IdentifierColon")
private val TypeNameEqual = node<String>().name("TypeNameEqual")
private val TypeNameColon = node<String>().name("TypeNameColon")
private val Label = node<String>().name("Label")
private val Newline = node<Unit>().name("Newline")
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
private val FormalParameterListHead = node<String>().pair(node<FoxType>()).list().name("FormalParameterListHead")
private val FormalParameterList = node<String>().orderedMap(node<FoxType>()).name("FormalParameterList")

private val ActualParameter = node<String>().optional().pair(node<FoxStatement>()).name("ActualParameter")
private val ActualParameterListHead = node<String>().optional().pair(node<FoxStatement>()).list().name("ActualParameterListHead")
private val ActualParameterList = node<String>().optional().pair(node<FoxStatement>()).list().name("ActualParameterList")

private val FormalGenericParameter = node<String>().pair(node<FoxType>()).name("FormalGenericParameter")
private val FormalGenericParameterListHead = node<String>().pair(node<FoxType>()).list().name("FormalGenericParameterListHead")
private val FormalGenericParameterList = node<String>().orderedMap(node<FoxType>()).name("FormalGenericParameterList")

private val FormalGenericParameterNoConstraints = node<String>().name("FormalGenericParameterNoConstraints")
private val FormalGenericParameterNoConstraintsListHead = node<String>().list().name("FormalGenericParameterNoConstraintsListHead")
private val FormalGenericParameterNoConstraintsList = node<String>().orderedSet().name("FormalGenericParameterNoConstraintsList")

private val ActualGenericParameter = node<String>().optional().pair(node<FoxType>()).name("ActualGenericParameter")
private val ActualGenericParameterListHead = node<String>().optional().pair(node<FoxType>()).list().name("ActualGenericParameterListHead")
private val ActualGenericParameterList = node<String>().optional().pair(node<FoxType>()).list().name("ActualGenericParameterList")

private val NamedActualGenericParameter = node<String>().pair(node<FoxType>()).name("NamedActualGenericParameter")
private val NamedActualGenericParameterListHead = node<String>().pair(node<FoxType>()).list().name("NamedActualGenericParameterListHead")
private val NamedActualGenericParameterList = node<String>().map(node<FoxType>()).name("NamedActualGenericParameterList")

private val AnonymousActualGenericParameter = node<FoxType>().name("AnonymousActualGenericParameter")
private val AnonymousActualGenericParameterListHead = node<FoxType>().list().name("AnonymousActualGenericParameterListHead")
private val AnonymousActualGenericParameterList = node<FoxType>().list().name("AnonymousActualGenericParameterList")

private val TupleComponentParameter = node<FoxType>().pair(node<Int>()).name("TupleComponentParameter")
private val TupleComponentParameterListHead = node<FoxType>().pair(node<Int>()).list().name("TupleComponentParameterListHead")
private val TupleComponentParameterList = node<FoxType>().pair(node<Int>()).list().name("TupleComponentParameterList")

private val StructFieldParameter = node<String>().pair(node<FoxType>()).name("StructFieldParameter")
private val StructFieldParameterListHead = node<String>().pair(node<FoxType>()).list().name("StructFieldParameterListHead")
private val StructFieldParameterList = node<String>().orderedMap(node<FoxType>()).name("StructFieldParameterList")

private val StructFieldName = node<String>().name("StructFieldName")
private val StructFieldNameListHead = node<String>().list().name("StructFieldNameListHead")
private val StructFieldNameList = node<String>().orderedSet().name("StructFieldNameList")

private val ObjectMemberParameter = node<String>().pair(node<FoxType>()).name("ObjectMemberParameter")
private val ObjectMemberParameterListHead = node<String>().pair(node<FoxType>()).list().name("ObjectMemberParameterListHead")
private val ObjectMemberParameterList = node<String>().map(node<FoxType>()).name("ObjectMemberParameterList")

private val ObjectMemberName = node<String>().name("ObjectMemberName")
private val ObjectMemberNameListHead = node<String>().list().name("ObjectMemberNameListHead")
private val ObjectMemberNameList = node<String>().set().name("ObjectMemberNameList")

private val EnumItemParameter = node<String>().pair(node<FoxType>()).name("EnumItemParameter")
private val EnumItemParameterListHead = node<String>().pair(node<FoxType>()).list().name("EnumItemParameterListHead")
private val EnumItemParameterList = node<String>().map(node<FoxType>()).name("EnumItemParameterList")

private val EnumItemName = node<String>().name("EnumItemName")
private val EnumItemNameListHead = node<String>().list().name("EnumItemNameListHead")
private val EnumItemNameList = node<String>().set().name("EnumItemNameList")

private val MethodTypeArgument = node<ParsedMethodTypeArgument>().name("MethodTypeArgument")
private val MethodTypeArgumentListHead = node<ParsedMethodTypeArgument>().list().name("MethodTypeArgumentListHead")
private val MethodTypeArgumentList = node<ParsedMethodTypeArgument>().list().name("MethodTypeArgumentList")

// Expression nodes
private val StatementBlockHead = node<FoxStatement>().list().name("StatementBlockHead")
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
private val WhenCaseConditionListHead = node<FoxStatement>().list().name("WhenCaseConditionListHead")
private val WhenCaseConditionList = node<FoxStatement>().list().name("WhenCaseConditionList")
private val WhenCase = node<FoxCase>().name("WhenCase")
private val WhenCaseListHead = node<FoxCase>().list().name("WhenCaseListHead")
private val WhenCaseList = node<FoxCase>().list().name("WhenCaseList")
private val IfCore = node<FoxIf>().name("IfCore")
private val WhileCore = node<FoxWhile>().name("WhileCore")
private val DoWhileCore = node<FoxDoWhile>().name("DoWhileCore")
private val WhenCore = node<FoxWhen>().name("WhenCore")
private val FileElementList = node<FoxFileElement>().list().name("FileElementList")

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
        tokens.forEach { token -> rules(token(token)) { fixed(token) { it.text } } }
    }
    
    fun tokenWeights(weight: Int, vararg tokens: String) {
        tokens.forEach { token -> weight(token(token), weight) }
    }
    
    fun symbolWeights(weight: Int, vararg symbols: Symbol<*>) {
        symbols.forEach { symbol -> weight(symbol, weight) }
    }
    
    fun <N> GrammarBuilder.RuleListBuilder<N>.tokenValues(vararg mappings: Pair<String, N>) {
        mappings.forEach { (text, value) ->
            symbols(token(text)) { value }
        }
    }
    
    fun <N> regexValue(
        source: Symbol<String>,
        pattern: String,
        result: Symbol<N>,
        parser: (String) -> N,
    ) {
        rules(source) { regex(Regex(pattern)) { it.text } }
        rules(result) {
            symbols(source) {
                try {
                    parser(it)
                } catch (e: Exception) {
                    throw RuleFactoryException("Invalid token: $it, cause: ${e.message}")
                }
            }
        }
    }
    
    symbolWeights(
        3,
        Newline,
        LineBreaks,
    )
    symbolWeights(
        5,
        Identifier,
        TypeName,
        BinInt,
        DecInt,
        HexInt,
        BinLong,
        DecLong,
        HexLong,
        DecFloat,
        HexFloat,
        DecDouble,
        HexDouble,
        node<Unit>(),
        node<Boolean>(),
        node<Char>(),
        node<String>(),
        node<Int>(),
        node<Long>(),
        node<Float>(),
        node<Double>(),
    )
    symbolWeights(
        7,
        IdentifierEqual,
        IdentifierColon,
        TypeNameEqual,
        TypeNameColon,
        Label,
        node<FoxUnaryOperator>(),
        node<FoxBinaryOperator>(),
        MultiplicativeOperator,
        AdditiveOperator,
        ShiftOperator,
        ComparisonOperator,
        EqualityOperator,
        BitAndOperator,
        BitXorOperator,
        BitOrOperator,
        LogicalAndOperator,
        LogicalOrOperator,
    )
    symbolWeights(
        9,
        node<FoxAssignOperator>(),
        FormalParameter,
        ActualParameter,
        FormalGenericParameter,
        FormalGenericParameterNoConstraints,
        ActualGenericParameter,
        NamedActualGenericParameter,
        AnonymousActualGenericParameter,
        TupleComponentParameter,
        StructFieldParameter,
        StructFieldName,
        ObjectMemberParameter,
        ObjectMemberName,
        EnumItemParameter,
        EnumItemName,
        MethodTypeArgument,
        ThisTypeQualifier,
        ReturnTypeClause,
    )
    symbolWeights(
        12,
        node<FoxEntityStatement>(),
        ParenthesizedStatement,
        PrimaryExpression,
        FormalParameterListHead,
        ActualParameterListHead,
        FormalGenericParameterListHead,
        FormalGenericParameterNoConstraintsListHead,
        ActualGenericParameterListHead,
        NamedActualGenericParameterListHead,
        AnonymousActualGenericParameterListHead,
        TupleComponentParameterListHead,
        StructFieldParameterListHead,
        StructFieldNameListHead,
        ObjectMemberParameterListHead,
        ObjectMemberNameListHead,
        EnumItemParameterListHead,
        EnumItemNameListHead,
        MethodTypeArgumentListHead,
    )
    symbolWeights(
        15,
        node<FoxType>(),
        PostfixExpression,
        UnaryExpression,
        FormalParameterList,
        ActualParameterList,
        FormalGenericParameterList,
        FormalGenericParameterNoConstraintsList,
        ActualGenericParameterList,
        NamedActualGenericParameterList,
        AnonymousActualGenericParameterList,
        TupleComponentParameterList,
        StructFieldParameterList,
        StructFieldNameList,
        ObjectMemberParameterList,
        ObjectMemberNameList,
        EnumItemParameterList,
        EnumItemNameList,
        MethodTypeArgumentList,
    )
    symbolWeights(
        18,
        MultiplicativeExpression,
        AdditiveExpression,
        ShiftExpression,
        ComparisonExpression,
        EqualityExpression,
        BitAndExpression,
        BitXorExpression,
        BitOrExpression,
        LogicalAndExpression,
        LogicalOrExpression,
        AssignableExpression,
        WhenCaseConditionListHead,
        WhenCaseConditionList,
        WhenCase,
        WhenCaseListHead,
        IfCore,
        WhileCore,
        DoWhileCore,
        WhenCore,
    )
    symbolWeights(
        24,
        AssignmentExpression,
        node<FoxStatement>(),
        StatementBlockHead,
        StatementBlock,
        node<FoxBlock>(),
        WhenCaseList,
    )
    symbolWeights(
        30,
        MethodHead,
        node<FoxTypeAlias>(),
        node<FoxMethodDefinition>(),
        node<FoxFileElement>(),
        FileElementList,
    )
    symbolWeights(36, node<FoxFile>())
    
    tokenWeights(
        5,
        ".", ":", ";", ",", "`", "~", "?", "@", "#", "$",
    )
    tokenWeights(
        6,
        "(", ")", "[", "]", "{", "}", "<", ">", "->",
    )
    tokenWeights(
        8,
        "+", "-", "*", "/", "%", "&", "|", "^", "!", "==", "!=", "<=", ">=", "&&", "||", "<<", ">>", ">>>",
    )
    tokenWeights(
        10,
        "=", ":=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=", "&&=", "||=",
    )
    tokenWeights(
        11,
        "unit", "true", "false", "this", "new",
    )
    tokenWeights(
        12,
        "type", "def", "if", "else", "when", "do", "while", "break", "continue", "yield", "return",
    )
    tokenWeights(
        13,
        "Void", "Unit", "Bool", "Byte", "Short", "Int", "Long", "Float", "Double", "Char", "String",
        "Any", "AnyTuple", "AnyStruct", "AnyObject", "AnyEnum",
    )
    tokenWeights(
        15,
        "Tuple", "Struct", "Object", "Enum", "Array", "Ref", "Method",
        "AnyOf", "AllOf", "NoneOf", "AnyTupleOf", "AnyStructOf",
        "ComponentAt", "LastComponentAt", "FirstComponentsOf", "ExactFirstComponentsOf",
        "LastComponentsOf", "ExactLastComponentsOf", "DropFirstComponentsOf", "ExactDropFirstComponentsOf",
        "DropLastComponentsOf", "ExactDropLastComponentsOf", "MergeComponentsOf",
        "FieldOf", "FieldAt", "LastFieldAt", "FirstFieldsOf", "ExactFirstFieldsOf",
        "LastFieldsOf", "ExactLastFieldsOf", "DropFirstFieldsOf", "ExactDropFirstFieldsOf",
        "DropLastFieldsOf", "ExactDropLastFieldsOf", "FieldsOf", "DropFieldsOf", "MergeFieldsOf",
        "MemberOf", "MembersOf", "DropMembersOf", "MergeMembersOf",
        "ItemOf", "ItemsOf", "DropItemsOf", "MergeItemsOf",
        "ElementOf", "ReferentOf", "MethodOf", "ThisOf", "ParametersOf", "ReturnOf",
    )
    
    fixedTokens(*ReservedKeywords.toTypedArray())
    fixedTokens(
        "(", ")", "[", "]", "{", "}", ".", ":", ";", ",",
        "+", "-", "*", "/", "%", "`", "~", "?", "!", "@", "#", "$", "&", "|", "^",
        "<", ">", "==", "!=", "<=", ">=", "&&", "||", "<<", ">>", ">>>", "->",
        "=", ":=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=", "&&=", "||=",
    )
    
    rules(node<FoxUnaryOperator>()) {
        tokenValues(
            "!" to FoxNotOperator,
            "-" to FoxNegOperator,
        )
    }
    
    rules(node<FoxBinaryOperator>()) {
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
    
    rules(node<FoxAssignOperator>()) {
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
    
    rules(MultiplicativeOperator) {
        tokenValues(
            "*" to FoxMulOperator,
            "/" to FoxDivOperator,
            "%" to FoxRemOperator,
        )
    }
    rules(AdditiveOperator) {
        tokenValues(
            "+" to FoxAddOperator,
            "-" to FoxSubOperator,
        )
    }
    rules(ShiftOperator) {
        tokenValues(
            "<<" to FoxShlOperator,
            ">>" to FoxShrOperator,
            ">>>" to FoxUshrOperator,
        )
    }
    rules(ComparisonOperator) {
        tokenValues(
            "<" to FoxLtOperator,
            ">" to FoxGtOperator,
            "<=" to FoxLeOperator,
            ">=" to FoxGeOperator,
        )
    }
    rules(EqualityOperator) {
        tokenValues(
            "==" to FoxEqOperator,
            "!=" to FoxNeqOperator,
        )
    }
    rules(BitAndOperator) {
        tokenValues("&" to FoxAndOperator)
    }
    rules(BitXorOperator) {
        tokenValues("^" to FoxXorOperator)
    }
    rules(BitOrOperator) {
        tokenValues("|" to FoxOrOperator)
    }
    rules(LogicalAndOperator) {
        tokenValues("&&" to FoxAndAndOperator)
    }
    rules(LogicalOrOperator) {
        tokenValues("||" to FoxOrOrOperator)
    }
    
    rules(Newline) { newline { } }
    rules(LineBreaks) {
        symbols(Newline) {}
        symbols(LineBreaks, Newline) { _, _ -> }
    }
    rules(Identifier) {
        regex(Regex("[a-z][a-zA-Z0-9_]*")) {
            if (it.text in ReservedKeywords) throw RuleFactoryException("'${it.text}' is a reserved keyword")
            it.text
        }
    }
    rules(TypeName) {
        regex(Regex("[A-Z][a-zA-Z0-9_]*")) {
            if (it.text in ReservedKeywords) throw RuleFactoryException("'${it.text}' is a reserved keyword")
            it.text
        }
    }
    rules(IdentifierEqual) { symbols(Identifier, token("=")) { it, _ -> it } }
    rules(IdentifierColon) { symbols(Identifier, token(":")) { it, _ -> it } }
    rules(TypeNameEqual) { symbols(TypeName, token("=")) { it, _ -> it } }
    rules(TypeNameColon) { symbols(TypeName, token(":")) { it, _ -> it } }
    
    rules(node<FoxType>()) {
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
    
    rules(node<Unit>()) { symbols(token("unit")) { } }
    rules(node<Boolean>()) { symbols(token("true")) { true } }
    rules(node<Boolean>()) { symbols(token("false")) { false } }
    rules(node<Char>()) { charLiteral { it.char } }
    rules(node<String>()) { stringLiteral { it.string } }
    
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
    
    rules(node<FoxEntityStatement>()) {
        symbols(node<Unit>()) { FoxEntityStatement(FoxUnit) }
        symbols(node<Boolean>()) { FoxEntityStatement(FoxBool(it)) }
        symbols(node<Int>()) { FoxEntityStatement(FoxInt(it)) }
        symbols(node<Long>()) { FoxEntityStatement(FoxLong(it)) }
        symbols(node<Float>()) { FoxEntityStatement(FoxFloat(it)) }
        symbols(node<Double>()) { FoxEntityStatement(FoxDouble(it)) }
        symbols(node<Char>()) { FoxEntityStatement(FoxChar(it)) }
        symbols(node<String>()) { FoxEntityStatement(FoxString(it)) }
    }
    
    rules(node<FoxType>()) {
        symbols(token("AnyOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxAnyOfType(it)
        }
        symbols(token("AllOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxAllOfType(it)
        }
        symbols(token("NoneOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxNoneOfType(it)
        }
        symbols(token("Tuple"), TupleComponentParameterList) { _, it ->
            it.toFoxTupleType()
        }
        symbols(token("AnyTupleOf"), AnonymousActualGenericParameterList) { _, it ->
            if (it.size != 1) throw RuleFactoryException("AnyTupleOf type must have exactly one generic parameter")
            FoxAnyTupleOfType(it.first())
        }
        symbols(token("Struct"), StructFieldParameterList) { _, it ->
            FoxStructType(it)
        }
        symbols(token("AnyStructOf"), AnonymousActualGenericParameterList) { _, it ->
            if (it.isEmpty()) throw RuleFactoryException("AnyStructOf type must have at least one generic parameter")
            FoxAnyStructOfType(it)
        }
        symbols(token("Object"), ObjectMemberParameterList) { _, it ->
            FoxObjectType(it)
        }
        symbols(token("Enum"), EnumItemParameterList) { _, it ->
            FoxEnumType(it)
        }
        symbols(token("Array"), AnonymousActualGenericParameterList) { _, it ->
            if (it.size != 1) throw RuleFactoryException("Array type must have exactly one generic parameter")
            FoxArrayType(it.first())
        }
        symbols(token("Ref"), AnonymousActualGenericParameterList) { _, it ->
            if (it.size != 1) throw RuleFactoryException("Ref type must have exactly one generic parameter")
            FoxRefType(it.first())
        }
        symbols(token("Method"), MethodTypeArgumentList) { _, items ->
            items.toFoxMethodType()
        }
        symbols(token("ComponentAt"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, index, _ ->
            FoxTupleComponentAtType(type, index)
        }
        symbols(token("LastComponentAt"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, index, _ ->
            FoxTupleLastComponentAtType(type, index)
        }
        symbols(token("FirstComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleFirstComponentsOfType(type, count)
        }
        symbols(token("ExactFirstComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleExactFirstComponentsOfType(type, count)
        }
        symbols(token("LastComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleLastComponentsOfType(type, count)
        }
        symbols(token("ExactLastComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleExactLastComponentsOfType(type, count)
        }
        symbols(token("DropFirstComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleDropFirstComponentsOfType(type, count)
        }
        symbols(token("ExactDropFirstComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleExactDropFirstComponentsOfType(type, count)
        }
        symbols(token("DropLastComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleDropLastComponentsOfType(type, count)
        }
        symbols(token("ExactDropLastComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxTupleExactDropLastComponentsOfType(type, count)
        }
        symbols(token("MergeComponentsOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxTupleMergeComponentsOfType(it)
        }
        symbols(token("FieldOf"), token("<"), node<FoxType>(), token(","), Identifier, token(">")) { _, _, type, _, name, _ ->
            FoxStructFieldOfType(type, name)
        }
        symbols(token("FieldAt"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, index, _ ->
            FoxStructFieldAtType(type, index)
        }
        symbols(token("LastFieldAt"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, index, _ ->
            FoxStructLastFieldAtType(type, index)
        }
        symbols(token("FirstFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructFirstFieldsOfType(type, count)
        }
        symbols(token("ExactFirstFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructExactFirstFieldsOfType(type, count)
        }
        symbols(token("LastFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructLastFieldsOfType(type, count)
        }
        symbols(token("ExactLastFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructExactLastFieldsOfType(type, count)
        }
        symbols(token("DropFirstFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructDropFirstFieldsOfType(type, count)
        }
        symbols(token("ExactDropFirstFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructExactDropFirstFieldsOfType(type, count)
        }
        symbols(token("DropLastFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructDropLastFieldsOfType(type, count)
        }
        symbols(token("ExactDropLastFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
            FoxStructExactDropLastFieldsOfType(type, count)
        }
        symbols(token("FieldsOf"), token("<"), node<FoxType>(), token(","), StructFieldNameList, token(">")) { _, _, type, _, names, _ ->
            FoxStructFieldsOfType(type, names)
        }
        symbols(token("DropFieldsOf"), token("<"), node<FoxType>(), token(","), StructFieldNameList, token(">")) { _, _, type, _, names, _ ->
            FoxStructDropFieldsOfType(type, names.toSet())
        }
        symbols(token("MergeFieldsOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxStructMergeFieldsOfType(it)
        }
        symbols(token("MemberOf"), token("<"), node<FoxType>(), token(","), Identifier, token(">")) { _, _, type, _, name, _ ->
            FoxObjectMemberOfType(type, name)
        }
        symbols(token("MembersOf"), token("<"), node<FoxType>(), token(","), ObjectMemberNameList, token(">")) { _, _, type, _, names, _ ->
            FoxObjectMembersOfType(type, names)
        }
        symbols(token("DropMembersOf"), token("<"), node<FoxType>(), token(","), ObjectMemberNameList, token(">")) { _, _, type, _, names, _ ->
            FoxObjectDropMembersOfType(type, names)
        }
        symbols(token("MergeMembersOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxObjectMergeMembersOfType(it)
        }
        symbols(token("ItemOf"), token("<"), node<FoxType>(), token(","), TypeName, token(">")) { _, _, type, _, name, _ ->
            FoxEnumItemOfType(type, name)
        }
        symbols(token("ItemsOf"), token("<"), node<FoxType>(), token(","), EnumItemNameList, token(">")) { _, _, type, _, names, _ ->
            FoxEnumItemsOfType(type, names)
        }
        symbols(token("DropItemsOf"), token("<"), node<FoxType>(), token(","), EnumItemNameList, token(">")) { _, _, type, _, names, _ ->
            FoxEnumDropItemsOfType(type, names)
        }
        symbols(token("MergeItemsOf"), AnonymousActualGenericParameterList) { _, it ->
            FoxEnumMergeItemsOfType(it)
        }
        symbols(token("ElementOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
            FoxArrayElementOfType(type)
        }
        symbols(token("ReferentOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
            FoxRefReferentOfType(type)
        }
        symbols(
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
        symbols(token("ThisOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
            FoxMethodThisOfType(type)
        }
        symbols(token("ParametersOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
            FoxMethodParametersOfType(type)
        }
        symbols(token("ReturnOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
            FoxMethodReturnOfType(type)
        }
        symbols(TypeName) {
            FoxUnresolvedType(it, null)
        }
        symbols(TypeName, AnonymousActualGenericParameterList) { name, it ->
            FoxUnresolvedType(name, it)
        }
    }
    
    rules(FormalParameter) {
        symbols(IdentifierColon, node<FoxType>()) { name, type -> name to type }
    }
    rules(FormalParameterListHead) {
        symbols(token("("), FormalParameter) { _, it -> listOf(it) }
        symbols(FormalParameterListHead, token(","), FormalParameter) { head, _, it -> head + it }
    }
    rules(FormalParameterList) {
        symbols(token("("), token(")")) { _, _ -> emptyOrderedMap() }
        symbols(FormalParameterListHead, token(")")) { head, _ -> head.toOrderedMap("formal parameter") }
        symbols(FormalParameterListHead, token(","), token(")")) { head, _, _ -> head.toOrderedMap("formal parameter") }
    }
    
    rules(ActualParameter) {
        symbols(node<FoxStatement>()) { null to it }
        symbols(IdentifierEqual, node<FoxStatement>()) { name, value -> name to value }
    }
    rules(ActualParameterListHead) {
        symbols(token("("), ActualParameter) { _, it -> listOf(it) }
        symbols(ActualParameterListHead, token(","), ActualParameter) { head, _, it -> head + it }
    }
    rules(ActualParameterList) {
        symbols(token("("), token(")")) { _, _ -> emptyList() }
        symbols(ActualParameterListHead, token(")")) { head, _ -> head }
        symbols(ActualParameterListHead, token(","), token(")")) { head, _, _ -> head }
    }
    
    rules(FormalGenericParameter) {
        symbols(TypeName) { it to FoxAnyType }
        symbols(TypeName, token("="), node<FoxType>()) { name, _, constraint -> name to constraint }
    }
    rules(FormalGenericParameterListHead) {
        symbols(token("<"), FormalGenericParameter) { _, it -> listOf(it) }
        symbols(FormalGenericParameterListHead, token(","), FormalGenericParameter) { head, _, it -> head + it }
    }
    rules(FormalGenericParameterList) {
        symbols(token("<"), token(">")) { _, _ -> emptyOrderedMap() }
        symbols(FormalGenericParameterListHead, token(">")) { head, _ -> head.toOrderedMap("formal generic parameter") }
        symbols(FormalGenericParameterListHead, token(","), token(">")) { head, _, _ -> head.toOrderedMap("formal generic parameter") }
    }
    
    rules(FormalGenericParameterNoConstraints) {
        symbols(TypeName) { it }
    }
    rules(FormalGenericParameterNoConstraintsListHead) {
        symbols(token("<"), FormalGenericParameterNoConstraints) { _, it -> listOf(it) }
        symbols(FormalGenericParameterNoConstraintsListHead, token(","), FormalGenericParameterNoConstraints) { head, _, it -> head + it }
    }
    rules(FormalGenericParameterNoConstraintsList) {
        symbols(token("<"), token(">")) { _, _ -> emptyOrderedSet() }
        symbols(FormalGenericParameterNoConstraintsListHead, token(">")) { head, _ -> head.toOrderedSet("formal generic parameter") }
        symbols(FormalGenericParameterNoConstraintsListHead, token(","), token(">")) { head, _, _ -> head.toOrderedSet("formal generic parameter") }
    }
    
    rules(ActualGenericParameter) {
        symbols(node<FoxType>()) { null to it }
        symbols(TypeNameEqual, node<FoxType>()) { name, type -> name to type }
    }
    rules(ActualGenericParameterListHead) {
        symbols(token("<"), ActualGenericParameter) { _, it -> listOf(it) }
        symbols(ActualGenericParameterListHead, token(","), ActualGenericParameter) { head, _, it -> head + it }
    }
    rules(ActualGenericParameterList) {
        symbols(token("<"), token(">")) { _, _ -> emptyList() }
        symbols(ActualGenericParameterListHead, token(">")) { head, _ -> head }
        symbols(ActualGenericParameterListHead, token(","), token(">")) { head, _, _ -> head }
    }
    
    rules(NamedActualGenericParameter) {
        symbols(TypeNameEqual, node<FoxType>()) { name, type -> name to type }
    }
    rules(NamedActualGenericParameterListHead) {
        symbols(token("<"), NamedActualGenericParameter) { _, it -> listOf(it) }
        symbols(NamedActualGenericParameterListHead, token(","), NamedActualGenericParameter) { head, _, it -> head + it }
    }
    rules(NamedActualGenericParameterList) {
        symbols(token("<"), token(">")) { _, _ -> emptyMap() }
        symbols(NamedActualGenericParameterListHead, token(">")) { head, _ -> head.toMap("named actual generic parameter") }
        symbols(NamedActualGenericParameterListHead, token(","), token(">")) { head, _, _ -> head.toMap("named actual generic parameter") }
    }
    
    rules(AnonymousActualGenericParameter) {
        symbols(node<FoxType>()) { it }
    }
    rules(AnonymousActualGenericParameterListHead) {
        symbols(token("<"), AnonymousActualGenericParameter) { _, it -> listOf(it) }
        symbols(AnonymousActualGenericParameterListHead, token(","), AnonymousActualGenericParameter) { head, _, it -> head + it }
    }
    rules(AnonymousActualGenericParameterList) {
        symbols(token("<"), token(">")) { _, _ -> emptyList() }
        symbols(AnonymousActualGenericParameterListHead, token(">")) { head, _ -> head }
        symbols(AnonymousActualGenericParameterListHead, token(","), token(">")) { head, _, _ -> head }
    }
    
    rules(TupleComponentParameter) {
        symbols(node<FoxType>()) { it to 1 }
        symbols(node<FoxType>(), token(":"), node<Int>()) { type, _, count ->
            if (count <= 0) throw RuleFactoryException("Tuple component count must be positive")
            type to count
        }
    }
    rules(TupleComponentParameterListHead) {
        symbols(token("<"), TupleComponentParameter) { _, it -> listOf(it) }
        symbols(TupleComponentParameterListHead, token(","), TupleComponentParameter) { head, _, it -> head + it }
    }
    rules(TupleComponentParameterList) {
        symbols(token("<"), token(">")) { _, _ -> emptyList() }
        symbols(TupleComponentParameterListHead, token(">")) { head, _ -> head }
        symbols(TupleComponentParameterListHead, token(","), token(">")) { head, _, _ -> head }
    }
    
    rules(StructFieldParameter) {
        symbols(IdentifierColon, node<FoxType>()) { name, type -> name to type }
    }
    rules(StructFieldParameterListHead) {
        symbols(token("<"), StructFieldParameter) { _, it -> listOf(it) }
        symbols(StructFieldParameterListHead, token(","), StructFieldParameter) { head, _, it -> head + it }
    }
    rules(StructFieldParameterList) {
        symbols(token("<"), token(">")) { _, _ -> emptyOrderedMap() }
        symbols(StructFieldParameterListHead, token(">")) { head, _ -> head.toOrderedMap("struct field parameter") }
        symbols(StructFieldParameterListHead, token(","), token(">")) { head, _, _ -> head.toOrderedMap("struct field parameter") }
    }
    
    rules(StructFieldName) {
        symbols(Identifier) { it }
    }
    rules(StructFieldNameListHead) {
        symbols(StructFieldName) { it -> listOf(it) }
        symbols(StructFieldNameListHead, token(","), StructFieldName) { head, _, it -> head + it }
    }
    rules(StructFieldNameList) {
        symbols(StructFieldNameListHead) { it.toOrderedSet("struct field name") }
    }
    
    rules(ObjectMemberParameter) {
        symbols(IdentifierColon, node<FoxType>()) { name, type -> name to type }
    }
    rules(ObjectMemberParameterListHead) {
        symbols(token("<"), ObjectMemberParameter) { _, it -> listOf(it) }
        symbols(ObjectMemberParameterListHead, token(","), ObjectMemberParameter) { head, _, it -> head + it }
    }
    rules(ObjectMemberParameterList) {
        symbols(token("<"), token(">")) { _, _ -> emptyMap() }
        symbols(ObjectMemberParameterListHead, token(">")) { head, _ -> head.toMap("object member parameter") }
        symbols(ObjectMemberParameterListHead, token(","), token(">")) { head, _, _ -> head.toMap("object member parameter") }
    }
    
    rules(ObjectMemberName) {
        symbols(Identifier) { it }
    }
    rules(ObjectMemberNameListHead) {
        symbols(ObjectMemberName) { it -> listOf(it) }
        symbols(ObjectMemberNameListHead, token(","), ObjectMemberName) { head, _, it -> head + it }
    }
    rules(ObjectMemberNameList) {
        symbols(ObjectMemberNameListHead) { it.toSet("object member name") }
    }
    
    rules(EnumItemParameter) {
        symbols(TypeNameEqual, node<FoxType>()) { name, type -> name to type }
    }
    rules(EnumItemParameterListHead) {
        symbols(token("<"), EnumItemParameter) { _, it -> listOf(it) }
        symbols(EnumItemParameterListHead, token(","), EnumItemParameter) { head, _, it -> head + it }
    }
    rules(EnumItemParameterList) {
        symbols(token("<"), token(">")) { _, _ -> emptyMap() }
        symbols(EnumItemParameterListHead, token(">")) { head, _ -> head.toMap("enum item parameter") }
        symbols(EnumItemParameterListHead, token(","), token(">")) { head, _, _ -> head.toMap("enum item parameter") }
    }
    
    rules(EnumItemName) {
        symbols(TypeName) { it }
    }
    rules(EnumItemNameListHead) {
        symbols(EnumItemName) { it -> listOf(it) }
        symbols(EnumItemNameListHead, token(","), EnumItemName) { head, _, it -> head + it }
    }
    rules(EnumItemNameList) {
        symbols(EnumItemNameListHead) { it.toSet("enum item name") }
    }
    
    rules(MethodTypeArgument) {
        symbols(token("this"), token(":"), node<FoxType>()) { _, _, type ->
            ParsedMethodTypeArgument.This(type)
        }
        symbols(token("return"), token(":"), node<FoxType>()) { _, _, type ->
            ParsedMethodTypeArgument.Return(type)
        }
        symbols(FormalParameter) { (name, type) ->
            ParsedMethodTypeArgument.Parameter(name, type)
        }
        symbols(node<FoxType>()) { type ->
            ParsedMethodTypeArgument.AnonymousType(type)
        }
    }
    rules(MethodTypeArgumentListHead) {
        symbols(token("<"), MethodTypeArgument) { _, it -> listOf(it) }
        symbols(MethodTypeArgumentListHead, token(","), MethodTypeArgument) { head, _, it -> head + it }
    }
    rules(MethodTypeArgumentList) {
        symbols(token("<"), token(">")) { _, _ -> emptyList() }
        symbols(MethodTypeArgumentListHead, token(">")) { head, _ -> head }
        symbols(MethodTypeArgumentListHead, token(","), token(">")) { head, _, _ -> head }
    }
    
    rules(Label) { symbols(token("#"), Identifier) { _, it -> it } }
    rules(ParenthesizedStatement) { symbols(token("("), node<FoxStatement>(), token(")")) { _, node, _ -> node } }
    
    rules(PrimaryExpression) {
        symbols(token("this")) { FoxThis }
        symbols(Identifier) { FoxSymbol(it) }
        symbols(ParenthesizedStatement) { it }
        symbols(node<FoxEntityStatement>()) { it }
    }
    
    rules(PostfixExpression) {
        symbols(PrimaryExpression) { it }
        symbols(PostfixExpression, token("."), Identifier) { target, _, name ->
            FoxFieldAccess(target, name)
        }
        symbols(PostfixExpression, token("."), node<Int>()) { target, _, index ->
            FoxComponentAccess(target, index)
        }
        symbols(Identifier, ActualGenericParameterList, ActualParameterList) { name, generics, parameters ->
            FoxCall(FoxEntityStatement(FoxUnit), name, generics, parameters)
        }
        symbols(Identifier, ActualParameterList) { name, parameters ->
            FoxCall(FoxEntityStatement(FoxUnit), name, null, parameters)
        }
        symbols(PostfixExpression, token("."), Identifier, ActualGenericParameterList, ActualParameterList) { target, _, name, generics, parameters ->
            FoxCall(target, name, generics, parameters)
        }
        symbols(PostfixExpression, token("."), Identifier, ActualParameterList) { target, _, name, parameters ->
            FoxCall(target, name, null, parameters)
        }
        symbols(node<FoxType>(), ActualParameterList) { type, parameters ->
            FoxConstruct(type, parameters)
        }
        symbols(ParenthesizedStatement, ActualParameterList) { method, parameters ->
            FoxIndirectCall(FoxEntityStatement(FoxUnit), method, parameters)
        }
        symbols(PostfixExpression, token("."), ParenthesizedStatement, ActualParameterList) { target, _, method, parameters ->
            FoxIndirectCall(target, method, parameters)
        }
    }
    
    rules(UnaryExpression) {
        symbols(PostfixExpression) { it }
        symbols(node<FoxUnaryOperator>(), UnaryExpression) { operator, node ->
            FoxUnary(operator, node)
        }
    }
    
    rules(MultiplicativeExpression) {
        symbols(UnaryExpression) { it }
        symbols(MultiplicativeExpression, MultiplicativeOperator, UnaryExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    rules(AdditiveExpression) {
        symbols(MultiplicativeExpression) { it }
        symbols(AdditiveExpression, AdditiveOperator, MultiplicativeExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    rules(ShiftExpression) {
        symbols(AdditiveExpression) { it }
        symbols(ShiftExpression, ShiftOperator, AdditiveExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    rules(ComparisonExpression) {
        symbols(ShiftExpression) { it }
        symbols(ComparisonExpression, ComparisonOperator, ShiftExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    rules(EqualityExpression) {
        symbols(ComparisonExpression) { it }
        symbols(EqualityExpression, EqualityOperator, ComparisonExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    rules(BitAndExpression) {
        symbols(EqualityExpression) { it }
        symbols(BitAndExpression, BitAndOperator, EqualityExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    rules(BitXorExpression) {
        symbols(BitAndExpression) { it }
        symbols(BitXorExpression, BitXorOperator, BitAndExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    rules(BitOrExpression) {
        symbols(BitXorExpression) { it }
        symbols(BitOrExpression, BitOrOperator, BitXorExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    rules(LogicalAndExpression) {
        symbols(BitOrExpression) { it }
        symbols(LogicalAndExpression, LogicalAndOperator, BitOrExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    rules(LogicalOrExpression) {
        symbols(LogicalAndExpression) { it }
        symbols(LogicalOrExpression, LogicalOrOperator, LogicalAndExpression) { left, operator, right ->
            FoxBinary(left, operator, right)
        }
    }
    
    rules(AssignableExpression) {
        symbols(PostfixExpression) { it }
    }
    
    rules(AssignmentExpression) {
        symbols(LogicalOrExpression) { it }
        symbols(AssignableExpression, node<FoxAssignOperator>(), node<FoxStatement>()) { left, operator, right ->
            FoxAssign(left, operator, right, beforeEvaluation = true)
        }
    }
    
    rules(StatementBlockHead) {
        symbols(token("{")) { emptyList() }
        symbols(StatementBlockHead, node<FoxStatement>()) { head, it -> head + it }
    }
    rules(StatementBlock) {
        symbols(StatementBlockHead, token("}")) { head, _ -> head }
    }
    
    rules(node<FoxStatement>()) {
        symbols(AssignmentExpression) { it }
        symbols(Identifier, token(":"), node<FoxType>()) { name, _, type -> FoxTypeBinding(name, type) }
        symbols(token("break")) { FoxBreak(null) }
        symbols(token("break"), Label) { _, label -> FoxBreak(label) }
        symbols(token("continue")) { FoxContinue(null) }
        symbols(token("continue"), Label) { _, label -> FoxContinue(label) }
        symbols(token("return")) { FoxReturn(null) }
        symbols(token("return"), node<FoxStatement>()) { _, value -> FoxReturn(value) }
        symbols(token("yield"), node<FoxStatement>()) { _, value -> FoxYield(null, value) }
        symbols(token("yield"), Label, node<FoxStatement>()) { _, label, value -> FoxYield(label, value) }
        symbols(node<FoxBlock>()) { it }
        symbols(IfCore) { core -> FoxIf(null, core.condition, core.thenBody, core.elseBody) }
        symbols(WhileCore) { core -> FoxWhile(null, core.condition, core.body) }
        symbols(DoWhileCore) { core -> FoxDoWhile(null, core.body, core.condition) }
        symbols(WhenCore) { core -> FoxWhen(null, core.value, core.cases) }
        symbols(Label, IfCore) { label, core -> FoxIf(label, core.condition, core.thenBody, core.elseBody) }
        symbols(Label, WhileCore) { label, core -> FoxWhile(label, core.condition, core.body) }
        symbols(Label, DoWhileCore) { label, core -> FoxDoWhile(label, core.body, core.condition) }
        symbols(Label, WhenCore) { label, core -> FoxWhen(label, core.value, core.cases) }
    }
    
    rules(node<FoxBlock>()) {
        symbols(StatementBlock) { FoxBlock(null, it) }
        symbols(Label, StatementBlock) { label, it -> FoxBlock(label, it) }
    }
    
    rules(IfCore) {
        symbols(
            token("if"),
            ParenthesizedStatement,
            node<FoxStatement>(),
        ) { _, condition, body -> FoxIf(null, condition, body, null) }
        symbols(
            token("if"),
            ParenthesizedStatement,
            node<FoxStatement>(),
            token("else"),
            node<FoxStatement>(),
        ) { _, condition, thenBody, _, elseBody -> FoxIf(null, condition, thenBody, elseBody) }
    }
    
    rules(WhileCore) {
        symbols(
            token("while"),
            ParenthesizedStatement,
            node<FoxStatement>(),
        ) { _, condition, body -> FoxWhile(null, condition, body) }
    }
    
    rules(DoWhileCore) {
        symbols(
            token("do"),
            node<FoxStatement>(),
            token("while"),
            ParenthesizedStatement,
        ) { _, body, _, condition -> FoxDoWhile(null, body, condition) }
    }
    
    rules(WhenCaseConditionListHead) {
        symbols(node<FoxStatement>()) { listOf(it) }
        symbols(WhenCaseConditionListHead, token(","), node<FoxStatement>()) { head, _, it -> head + it }
    }
    rules(WhenCaseConditionList) {
        symbols(WhenCaseConditionListHead, token("->")) { it, _ -> it }
    }
    
    rules(WhenCase) {
        symbols(
            WhenCaseConditionList,
            node<FoxStatement>(),
        ) { conditions, body -> FoxCase(conditions, body) }
        symbols(
            token("else"),
            token("->"),
            node<FoxStatement>(),
        ) { _, _, body -> FoxCase(emptyList(), body) }
    }
    
    rules(WhenCaseListHead) {
        symbols(WhenCase) { listOf(it) }
        symbols(WhenCaseListHead, WhenCase) { head, it -> head + it }
    }
    rules(WhenCaseList) {
        symbols(token("{"), WhenCaseListHead, token("}")) { _, it, _ -> it }
    }
    
    rules(WhenCore) {
        symbols(
            token("when"),
            WhenCaseList,
        ) { _, cases -> FoxWhen(null, null, cases) }
        symbols(
            token("when"),
            ParenthesizedStatement,
            WhenCaseList,
        ) { _, value, cases -> FoxWhen(null, value, cases) }
    }
    
    rules(node<FoxTypeAlias>()) {
        symbols(
            token("type"),
            TypeName,
            FormalGenericParameterNoConstraintsList,
            token("="),
            node<FoxType>(),
        ) { _, name, generics, _, type -> FoxTypeAlias(name, generics, type) }
        symbols(
            token("type"),
            TypeName,
            token("="),
            node<FoxType>(),
        ) { _, name, _, type -> FoxTypeAlias(name, emptyOrderedSet(), type) }
    }
    
    rules(ThisTypeQualifier) { symbols(node<FoxType>(), token(".")) { type, _ -> type } }
    rules(ReturnTypeClause) { symbols(token(":"), node<FoxType>()) { _, type -> type } }
    
    rules(MethodHead) {
        symbols(
            token("def"),
            FormalGenericParameterList,
            ThisTypeQualifier,
            Identifier,
            FormalParameterList,
        ) { _, generics, thisType, name, parameters ->
            FoxMethodDefinition(generics, thisType, name, parameters, FoxUnitType, FoxEntityStatement(FoxUnit))
        }
        symbols(
            token("def"),
            FormalGenericParameterList,
            Identifier,
            FormalParameterList,
        ) { _, generics, name, parameters ->
            FoxMethodDefinition(generics, FoxUnitType, name, parameters, FoxUnitType, FoxEntityStatement(FoxUnit))
        }
        symbols(
            token("def"),
            ThisTypeQualifier,
            Identifier,
            FormalParameterList,
        ) { _, thisType, name, parameters ->
            FoxMethodDefinition(emptyOrderedMap(), thisType, name, parameters, FoxUnitType, FoxEntityStatement(FoxUnit))
        }
        symbols(
            token("def"),
            Identifier,
            FormalParameterList,
        ) { _, name, parameters ->
            FoxMethodDefinition(emptyOrderedMap(), FoxUnitType, name, parameters, FoxUnitType, FoxEntityStatement(FoxUnit))
        }
    }
    
    rules(node<FoxMethodDefinition>()) {
        symbols(
            MethodHead,
            ReturnTypeClause,
            node<FoxBlock>(),
        ) { head, returnType, body -> FoxMethodDefinition(head.generics, head.thisType, head.name, head.parameters, returnType, body) }
        symbols(
            MethodHead,
            node<FoxBlock>(),
        ) { head, body -> FoxMethodDefinition(head.generics, head.thisType, head.name, head.parameters, FoxUnitType, body) }
    }
    
    rules(node<FoxFileElement>()) {
        symbols(node<FoxTypeAlias>()) { it }
        symbols(node<FoxMethodDefinition>()) { it }
    }
    
    rules(FileElementList) {
        symbols(node<FoxFileElement>()) { listOf(it) }
        symbols(FileElementList, node<FoxFileElement>()) { head, it -> head + it }
    }
    rules(node<FoxFile>()) {
        symbols(FileElementList) { FoxFile(it) }
    }
}

val FoxFileParser = Parser(FoxGrammar, node<FoxFile>())

fun foxGrammarRepresentativeSymbols(): List<Pair<String, Symbol<*>>> = listOf(
    "Identifier" to Identifier,
    "TypeName" to TypeName,
    "IntLit" to node<Int>(),
    "StringLit" to node<String>(),
    "Entity" to node<FoxEntityStatement>(),
    "Type" to node<FoxType>(),
    "FormalParams" to FormalParameterList,
    "ActualParams" to ActualParameterList,
    "Postfix" to PostfixExpression,
    "Unary" to UnaryExpression,
    "Mul" to MultiplicativeExpression,
    "Add" to AdditiveExpression,
    "Compare" to ComparisonExpression,
    "AssignExpr" to AssignmentExpression,
    "Statement" to node<FoxStatement>(),
    "Block" to node<FoxBlock>(),
    "IfCore" to IfCore,
    "WhileCore" to WhileCore,
    "WhenCore" to WhenCore,
    "TypeAlias" to node<FoxTypeAlias>(),
    "MethodDef" to node<FoxMethodDefinition>(),
    "File" to node<FoxFile>(),
)

private sealed interface ParsedMethodTypeArgument {
    data class This(val type: FoxType) : ParsedMethodTypeArgument
    data class Return(val type: FoxType) : ParsedMethodTypeArgument
    data class Parameter(val name: String, val type: FoxType) : ParsedMethodTypeArgument
    data class AnonymousType(val type: FoxType) : ParsedMethodTypeArgument
}

private fun List<String>.toSet(itemName: String): Set<String> {
    val result = LinkedHashSet<String>()
    forEach { name ->
        if (name in result) throw RuleFactoryException("Duplicate $itemName name '$name'")
        result += name
    }
    return result
}

private fun <V : Any> List<Pair<String, V>>.toMap(itemName: String): Map<String, V> {
    val result = LinkedHashMap<String, V>()
    forEach { (name, value) ->
        if (name in result) throw RuleFactoryException("Duplicate $itemName name '$name'")
        result[name] = value
    }
    return result
}

private fun List<String>.toOrderedSet(itemName: String): OrderedSet<String> {
    val result = mutableOrderedSetOf<String>()
    forEach { name ->
        if (name in result) throw RuleFactoryException("Duplicate $itemName name '$name'")
        result += name
    }
    return result
}

private fun <V : Any> List<Pair<String, V>>.toOrderedMap(itemName: String): OrderedMap<String, V> {
    val result = mutableOrderedMapOf<String, V>()
    forEach { (name, value) ->
        if (name in result) throw RuleFactoryException("Duplicate $itemName name '$name'")
        result[name] = value
    }
    return result
}

private fun List<ParsedMethodTypeArgument>.toFoxMethodType(): FoxMethodType {
    if (count { it is ParsedMethodTypeArgument.This } > 1) {
        throw RuleFactoryException("Method type cannot declare more than one 'this' type")
    }
    if (count { it is ParsedMethodTypeArgument.Return } > 1) {
        throw RuleFactoryException("Method type cannot declare more than one 'return' type")
    }
    forEachIndexed { index, item ->
        if (item is ParsedMethodTypeArgument.This && index != 0) {
            throw RuleFactoryException("Method type 'this' must be the first item")
        }
        if (item is ParsedMethodTypeArgument.Return && index != lastIndex) {
            throw RuleFactoryException("Method type 'return' must be the last item")
        }
    }
    if (size == 1 && firstOrNull() is ParsedMethodTypeArgument.AnonymousType) {
        throw RuleFactoryException("Method<T> is ambiguous; use 'this: T' or 'return: T' explicitly")
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
                    throw RuleFactoryException("Duplicate method type parameter name '${item.name}'")
                }
                parameters[item.name] = item.type
            }
            is ParsedMethodTypeArgument.This ->
                throw RuleFactoryException("Method type 'this' must be the first item")
            is ParsedMethodTypeArgument.Return ->
                throw RuleFactoryException("Method type 'return' must be the last item")
            is ParsedMethodTypeArgument.AnonymousType ->
                throw RuleFactoryException("Anonymous method type items may only appear as leading 'this' or trailing 'return'")
        }
    }
    return FoxMethodType(thisType, parameters, returnType)
}
