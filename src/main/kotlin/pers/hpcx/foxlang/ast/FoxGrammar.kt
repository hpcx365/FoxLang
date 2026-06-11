package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.parser.*
import pers.hpcx.foxlang.runtime.*
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet
import pers.hpcx.foxlang.utils.mutableOrderedMapOf
import pers.hpcx.foxlang.utils.mutableOrderedSetOf
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

private val FormalParameter = node<String>().pair(node<FoxType>()).name("FormalParameter")
private val RawFormalParameterList = node<String>().pair(node<FoxType>()).list().name("RawFormalParameterList")
private val FormalParameterList = node<String>().orderedMap(node<FoxType>()).name("FormalParameterList")

private val ActualParameter = node<String>().optional().pair(node<FoxStatement>()).name("ActualParameter")
private val ActualParameterList = ActualParameter.list().name("ActualParameterList")

private val AnonymousActualParameterList = node<FoxStatement>().list().name("AnonymousActualParameterList")

private val FormalGenericParameter = node<String>().pair(node<FoxGenericConstraint>()).name("FormalGenericParameter")
private val RawFormalGenericParameterList = node<String>().pair(node<FoxGenericConstraint>()).list().name("RawFormalGenericParameterList")
private val FormalGenericParameterList = node<String>().orderedMap(node<FoxGenericConstraint>()).name("FormalGenericParameterList")
private val FormalGenericConstraintTerm = node<Boolean>().pair(node<FoxType>()).name("FormalGenericConstraintTerm")
private val RawFormalGenericConstraintTermList = node<Boolean>().pair(node<FoxType>()).list().name("RawFormalGenericConstraintTermList")
private val FormalGenericConstraintClause = node<FoxGenericConstraint>().name("FormalGenericConstraintClause")

private val RawFormalGenericParameterListWithoutConstraints = node<String>().list().name("RawFormalGenericParameterListWithoutConstraints")
private val FormalGenericParameterListWithoutConstraints = node<String>().orderedSet().name("FormalGenericParameterListWithoutConstraints")

private val ActualGenericParameter = node<String>().optional().pair(node<FoxType>()).name("ActualGenericParameter")
private val ActualGenericParameterList = ActualGenericParameter.list().name("ActualGenericParameterList")

private val NamedActualGenericParameter = node<String>().pair(node<FoxType>()).name("NamedActualGenericParameter")
private val RawNamedActualGenericParameterList = node<String>().pair(node<FoxType>()).list().name("RawNamedActualGenericParameterList")
private val NamedActualGenericParameterList = node<String>().map(node<FoxType>()).name("NamedActualGenericParameterList")

private val AnonymousActualGenericParameterList = node<FoxType>().list().name("AnonymousActualGenericParameterList")
private val TupleComponentParameter = node<FoxType>().pair(node<Int>()).name("TupleComponentParameter")
private val TupleComponentParameterList = node<FoxType>().pair(node<Int>()).list().name("TupleComponentParameterList")

private val StructFieldParameter = node<String>().pair(node<FoxType>()).name("StructFieldParameter")
private val RawStructFieldParameterList = node<String>().pair(node<FoxType>()).list().name("RawStructFieldParameterList")
private val StructFieldParameterList = node<String>().orderedMap(node<FoxType>()).name("StructFieldParameterList")
private val RawStructFieldNameList = node<String>().list().name("RawStructFieldNameList")
private val StructFieldNameList = node<String>().orderedSet().name("StructFieldNameList")
private val ObjectMemberParameterList = node<String>().map(node<FoxType>()).name("ObjectMemberParameterList")
private val ObjectMemberNameSet = node<String>().set().name("ObjectMemberNameSet")
private val RawMethodTypeParameterList = node<String>().pair(node<FoxType>()).list().name("RawMethodTypeParameterList")
private val MethodTypeParameterList = node<String>().orderedMap(node<FoxType>()).name("MethodTypeParameterList")

private val EnumItemParameter = node<String>().pair(node<FoxType>()).name("EnumItemParameter")
private val RawEnumItemParameterList = node<String>().pair(node<FoxType>()).list().name("RawEnumItemParameterList")
private val EnumItemParameterList = node<String>().map(node<FoxType>()).name("EnumItemParameterList")
private val EnumItemNameList = node<String>().list().name("EnumItemNameList")

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

private val WhenCaseConditionList = node<FoxStatement>().list().name("WhenCaseConditionList")
private val WhenCase = node<FoxCase>().name("WhenCase")
private val WhenCaseList = node<FoxCase>().list().name("WhenCaseList")
private val IfCore = node<ParsedIfCore>().name("IfCore")
private val WhileCore = node<ParsedWhileCore>().name("WhileCore")
private val DoWhileCore = node<ParsedDoWhileCore>().name("DoWhileCore")
private val WhenCore = node<ParsedWhenCore>().name("WhenCore")

private val ThisTypeQualifier = node<FoxType>().name("ThisTypeQualifier")
private val ReturnTypeClause = node<FoxType>().name("ReturnTypeClause")
private val MethodHead = node<ParsedMethodHead>().name("MethodHead")
private val FileElementList = node<FoxFileElement>().list().name("FileElementList")

private val ReservedKeywords = setOf(
    "const", "type", "def", "if", "else", "when", "new", "yield", "return", "for", "in",
    "do", "while", "break", "continue", "try", "finally", "import", "unit", "true", "false",
    
    "Void", "Unit", "Bool", "Byte", "Short", "Int", "Long", "Float", "Double", "Char",
    "String", "Tuple", "Struct", "Object", "Enum", "Array", "Ref", "Method",
    "Any", "AnyTuple", "AnyStruct", "AnyObject", "AnyEnum", "AnyArray", "AnyRef", "AnyMethod",
    "ComponentAt", "LastComponentAt", "FirstComponentsOf", "LastComponentsOf", "DropFirstComponentsOf", "DropLastComponentsOf", "MergeComponentsOf",
    "FieldOf", "FieldAt", "LastFieldAt", "FirstFieldsOf", "LastFieldsOf", "DropFirstFieldsOf", "DropLastFieldsOf", "FieldsOf", "DropFieldsOf", "MergeFieldsOf",
    "MemberOf", "MembersOf", "DropMembersOf", "MergeMembersOf",
    "ItemOf", "ItemsOf", "DropItemsOf", "MergeItemsOf",
    "ElementOf", "ReferentOf", "ThisOf", "ParametersOf", "ReturnOf",
)

private val FoxProductions = buildList {
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
            node<FoxUnaryOperator>(),
            "!" to FoxNotOperator,
            "-" to FoxNegOperator,
        ),
    )
    
    addAll(
        tokenValues(
            node<FoxBinaryOperator>(),
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
        ),
    )
    
    addAll(tokenValues(MultiplicativeOperator, "*" to FoxMulOperator, "/" to FoxDivOperator, "%" to FoxRemOperator))
    addAll(tokenValues(AdditiveOperator, "+" to FoxAddOperator, "-" to FoxSubOperator))
    addAll(tokenValues(ShiftOperator, "<<" to FoxShlOperator, ">>" to FoxShrOperator, ">>>" to FoxUshrOperator))
    addAll(tokenValues(ComparisonOperator, "<" to FoxLtOperator, ">" to FoxGtOperator, "<=" to FoxLeOperator, ">=" to FoxGeOperator))
    addAll(tokenValues(EqualityOperator, "==" to FoxEqOperator, "!=" to FoxNeqOperator))
    addAll(tokenValues(BitAndOperator, "&" to FoxAndOperator))
    addAll(tokenValues(BitXorOperator, "^" to FoxXorOperator))
    addAll(tokenValues(BitOrOperator, "|" to FoxOrOperator))
    addAll(tokenValues(LogicalAndOperator, "&&" to FoxAndAndOperator))
    addAll(tokenValues(LogicalOrOperator, "||" to FoxOrOrOperator))
    
    addAll(
        tokenValues(
            node<FoxAssignOperator>(),
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
    
    add(serial(node<FoxEntityStatement>(), node<Unit>()) { FoxEntityStatement(FoxUnit) })
    add(serial(node<FoxEntityStatement>(), node<Boolean>()) { FoxEntityStatement(FoxBool(it)) })
    add(serial(node<FoxEntityStatement>(), node<Int>()) { FoxEntityStatement(FoxInt(it)) })
    add(serial(node<FoxEntityStatement>(), node<Long>()) { FoxEntityStatement(FoxLong(it)) })
    add(serial(node<FoxEntityStatement>(), node<Float>()) { FoxEntityStatement(FoxFloat(it)) })
    add(serial(node<FoxEntityStatement>(), node<Double>()) { FoxEntityStatement(FoxDouble(it)) })
    add(serial(node<FoxEntityStatement>(), node<Char>()) { FoxEntityStatement(FoxChar(it)) })
    add(serial(node<FoxEntityStatement>(), node<String>()) { FoxEntityStatement(FoxString(it)) })
    add(serial(PrimaryExpression, node<FoxEntityStatement>()) { it })
    add(
        serial(PrimaryExpression, FormattedStringTemplateLiteral) { template ->
            FoxFormattedString(
                parts = template.parts.map { part ->
                    when (part) {
                        is FormattedTextPart -> FoxFormattedText(part.text)
                        is FormattedExpressionPart -> FoxFormattedExpression(parseFormattedExpression(part.source))
                    }
                },
                isRaw = template.isRaw,
            )
        },
    )
    
    addAll(
        tokenValues(
            node<FoxType>(),
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
            "AnyArray" to FoxAnyArrayType,
            "AnyRef" to FoxAnyRefType,
            "AnyMethod" to FoxAnyMethodType,
        ),
    )
    addAll(
        listOf(
            serial(node<FoxType>(), token("Tuple"), TupleComponentParameterList) { _, it ->
                it.toFoxTupleType()
            },
            serial(node<FoxType>(), token("Struct"), StructFieldParameterList) { _, it ->
                FoxStructType(it)
            },
            serial(node<FoxType>(), token("Object"), ObjectMemberParameterList) { _, it ->
                FoxObjectType(it)
            },
            serial(node<FoxType>(), token("Enum"), EnumItemParameterList) { _, it ->
                FoxEnumType(it)
            },
            serial(node<FoxType>(), token("Array"), AnonymousActualGenericParameterList) { _, it ->
                if (it.size != 1) throw ParseException("Array type must have exactly one generic parameter")
                FoxArrayType(it.first())
            },
            serial(node<FoxType>(), token("Ref"), AnonymousActualGenericParameterList) { _, it ->
                if (it.size != 1) throw ParseException("Ref type must have exactly one generic parameter")
                FoxRefType(it.first())
            },
            serial(
                node<FoxType>(),
                token("Method"),
                token("<"),
                node<FoxType>(),
                token(","),
                MethodTypeParameterList,
                token(","),
                node<FoxType>(),
                token(">"),
            ) { _, _, `this`, _, params, _, `return`, _ -> FoxMethodType(`this`, params, `return`) },
            
            serial(node<FoxType>(), token("ComponentAt"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, index, _ ->
                FoxTupleComponentAtType(type, index)
            },
            serial(node<FoxType>(), token("LastComponentAt"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, index, _ ->
                FoxTupleLastComponentAtType(type, index)
            },
            serial(node<FoxType>(), token("FirstComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
                FoxTupleFirstComponentsOfType(type, count)
            },
            serial(node<FoxType>(), token("LastComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
                FoxTupleLastComponentsOfType(type, count)
            },
            serial(node<FoxType>(), token("DropFirstComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
                FoxTupleDropFirstComponentsOfType(type, count)
            },
            serial(node<FoxType>(), token("DropLastComponentsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
                FoxTupleDropLastComponentsOfType(type, count)
            },
            serial(node<FoxType>(), token("MergeComponentsOf"), AnonymousActualGenericParameterList) { _, it ->
                FoxTupleMergeComponentsOfType(it)
            },
            serial(node<FoxType>(), token("FieldOf"), token("<"), node<FoxType>(), token(","), Identifier, token(">")) { _, _, type, _, name, _ ->
                FoxStructFieldOfType(type, name)
            },
            serial(node<FoxType>(), token("FieldAt"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, index, _ ->
                FoxStructFieldAtType(type, index)
            },
            serial(node<FoxType>(), token("LastFieldAt"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, index, _ ->
                FoxStructLastFieldAtType(type, index)
            },
            serial(node<FoxType>(), token("FirstFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
                FoxStructFirstFieldsOfType(type, count)
            },
            serial(node<FoxType>(), token("LastFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
                FoxStructLastFieldsOfType(type, count)
            },
            serial(node<FoxType>(), token("DropFirstFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
                FoxStructDropFirstFieldsOfType(type, count)
            },
            serial(node<FoxType>(), token("DropLastFieldsOf"), token("<"), node<FoxType>(), token(","), node<Int>(), token(">")) { _, _, type, _, count, _ ->
                FoxStructDropLastFieldsOfType(type, count)
            },
            serial(node<FoxType>(), token("FieldsOf"), token("<"), node<FoxType>(), token(","), StructFieldNameList, token(">")) { _, _, type, _, names, _ ->
                FoxStructFieldsOfType(type, names)
            },
            serial(node<FoxType>(), token("DropFieldsOf"), token("<"), node<FoxType>(), token(","), StructFieldNameList, token(">")) { _, _, type, _, names, _ ->
                FoxStructDropFieldsOfType(type, names.toSet())
            },
            serial(node<FoxType>(), token("MergeFieldsOf"), AnonymousActualGenericParameterList) { _, it ->
                FoxStructMergeFieldsOfType(it)
            },
            serial(node<FoxType>(), token("MemberOf"), token("<"), node<FoxType>(), token(","), Identifier, token(">")) { _, _, type, _, name, _ ->
                FoxObjectMemberOfType(type, name)
            },
            serial(node<FoxType>(), token("MembersOf"), token("<"), node<FoxType>(), token(","), ObjectMemberNameSet, token(">")) { _, _, type, _, names, _ ->
                FoxObjectMembersOfType(type, names)
            },
            serial(node<FoxType>(), token("DropMembersOf"), token("<"), node<FoxType>(), token(","), ObjectMemberNameSet, token(">")) { _, _, type, _, names, _ ->
                FoxObjectDropMembersOfType(type, names)
            },
            serial(node<FoxType>(), token("MergeMembersOf"), AnonymousActualGenericParameterList) { _, it ->
                FoxObjectMergeMembersOfType(it)
            },
            serial(node<FoxType>(), token("ItemOf"), token("<"), node<FoxType>(), token(","), TypeName, token(">")) { _, _, type, _, name, _ ->
                FoxEnumItemOfType(type, name)
            },
            serial(node<FoxType>(), token("ItemsOf"), token("<"), node<FoxType>(), token(","), EnumItemNameList, token(">")) { _, _, type, _, names, _ ->
                FoxEnumItemsOfType(type, names)
            },
            serial(node<FoxType>(), token("DropItemsOf"), token("<"), node<FoxType>(), token(","), EnumItemNameList, token(">")) { _, _, type, _, names, _ ->
                FoxEnumDropItemsOfType(type, names)
            },
            serial(node<FoxType>(), token("MergeItemsOf"), AnonymousActualGenericParameterList) { _, it ->
                FoxEnumMergeItemsOfType(it)
            },
            serial(node<FoxType>(), token("ElementOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
                FoxArrayElementOfType(type)
            },
            serial(node<FoxType>(), token("ReferentOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
                FoxRefReferentOfType(type)
            },
            serial(node<FoxType>(), token("ThisOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
                FoxMethodThisOfType(type)
            },
            serial(node<FoxType>(), token("ParametersOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
                FoxMethodParametersOfType(type)
            },
            serial(node<FoxType>(), token("ReturnOf"), token("<"), node<FoxType>(), token(">")) { _, _, type, _ ->
                FoxMethodReturnOfType(type)
            },
            serial(node<FoxType>(), TypeName) {
                FoxUnresolvedType(it, null)
            },
            serial(node<FoxType>(), TypeName, AnonymousActualGenericParameterList) { name, it ->
                FoxUnresolvedType(name, it)
            },
            
            serial(FormalParameter, IdentifierColon, node<FoxType>()) { name, type -> name to type },
            listLike(RawFormalParameterList, token("("), FormalParameter, token(","), token(")")),
            serial(FormalParameterList, RawFormalParameterList) { it.toOrderedMap("formal parameter") },
            
            serial(ActualParameter, node<FoxStatement>()) { null to it },
            serial(ActualParameter, IdentifierEqual, node<FoxStatement>()) { name, value -> name to value },
            listLike(ActualParameterList, token("("), ActualParameter, token(","), token(")")),
            
            listLike(AnonymousActualParameterList, token("("), node<FoxStatement>(), token(","), token(")")),
            
            serial(FormalGenericConstraintTerm, token("+"), node<FoxType>()) { _, type -> true to type },
            serial(FormalGenericConstraintTerm, token("-"), node<FoxType>()) { _, type -> false to type },
            listLike(RawFormalGenericConstraintTermList, null, FormalGenericConstraintTerm, null, null),
            serial(FormalGenericConstraintClause, node<FoxType>()) {
                FoxGenericConstraint(listOf(it), emptyList())
            },
            serial(FormalGenericConstraintClause, FormalGenericConstraintTerm) { (positive, type) ->
                if (positive) FoxGenericConstraint(listOf(type), emptyList())
                else FoxGenericConstraint(emptyList(), listOf(type))
            },
            serial(FormalGenericConstraintClause, node<FoxType>(), RawFormalGenericConstraintTermList) { type, rest ->
                FoxGenericConstraint(
                    positivePatterns = listOf(type) + rest.filter { it.first }.map { it.second },
                    negativePatterns = rest.filter { !it.first }.map { it.second },
                )
            },
            serial(FormalGenericConstraintClause, FormalGenericConstraintTerm, RawFormalGenericConstraintTermList) { first, rest ->
                val constraints = listOf(first) + rest
                FoxGenericConstraint(
                    positivePatterns = constraints.filter { it.first }.map { it.second },
                    negativePatterns = constraints.filter { !it.first }.map { it.second },
                )
            },
            serial(FormalGenericParameter, TypeName) { it to FoxGenericConstraint(emptyList(), emptyList()) },
            serial(FormalGenericParameter, TypeName, token("="), FormalGenericConstraintClause) { name, _, constraint ->
                name to constraint
            },
            listLike(RawFormalGenericParameterList, token("<"), FormalGenericParameter, token(","), token(">")),
            serial(FormalGenericParameterList, RawFormalGenericParameterList) { it.toOrderedMap("formal generic parameter") },
            
            listLike(RawFormalGenericParameterListWithoutConstraints, token("<"), TypeName, token(","), token(">")),
            serial(FormalGenericParameterListWithoutConstraints, RawFormalGenericParameterListWithoutConstraints) { it.toOrderedSet("formal generic parameter") },
            
            serial(ActualGenericParameter, node<FoxType>()) { null to it },
            serial(ActualGenericParameter, TypeNameEqual, node<FoxType>()) { name, type -> name to type },
            listLike(ActualGenericParameterList, token("<"), ActualGenericParameter, token(","), token(">")),
            
            serial(NamedActualGenericParameter, TypeNameEqual, node<FoxType>()) { name, type -> name to type },
            listLike(RawNamedActualGenericParameterList, token("<"), NamedActualGenericParameter, token(","), token(">")),
            serial(NamedActualGenericParameterList, RawNamedActualGenericParameterList) { it.toMap("actual generic parameter") },
            
            listLike(AnonymousActualGenericParameterList, token("<"), node<FoxType>(), token(","), token(">")),
            serial(TupleComponentParameter, node<FoxType>()) { it to 1 },
            serial(TupleComponentParameter, node<FoxType>(), token(":"), node<Int>()) { type, _, count ->
                if (count <= 0) throw ParseException("Tuple component count must be positive")
                type to count
            },
            listLike(TupleComponentParameterList, token("<"), TupleComponentParameter, token(","), token(">")),
            
            serial(StructFieldParameter, IdentifierColon, node<FoxType>()) { name, type -> name to type },
            listLike(RawStructFieldParameterList, token("<"), StructFieldParameter, token(","), token(">")),
            serial(StructFieldParameterList, RawStructFieldParameterList) { it.toOrderedMap("struct field parameter") },
            serial(ObjectMemberParameterList, RawStructFieldParameterList) { it.toMap("object member parameter") },
            
            serial(EnumItemParameter, TypeNameEqual, node<FoxType>()) { name, type -> name to type },
            listLike(RawEnumItemParameterList, token("<"), EnumItemParameter, token(","), token(">")),
            serial(EnumItemParameterList, RawEnumItemParameterList) { it.toMap("enum item type parameter") },
            listLike(RawStructFieldNameList, null, Identifier, token(","), null),
            serial(StructFieldNameList, RawStructFieldNameList) { it.toOrderedSet("struct field name") },
            serial(ObjectMemberNameSet, RawStructFieldNameList) { it.toSet("object member name") },
            listLike(EnumItemNameList, null, TypeName, token(","), null),
            
            listLike(RawMethodTypeParameterList, null, FormalParameter, token(","), null),
            serial(MethodTypeParameterList, RawMethodTypeParameterList) { it.toOrderedMap("method type parameter") },
            
            serial(Label, token("#"), Identifier) { _, it -> it },
            serial(ParenthesizedStatement, token("("), node<FoxStatement>(), token(")")) { _, node, _ -> node },
            
            serial(PrimaryExpression, Identifier) { FoxSymbol(it) },
            serial(PrimaryExpression, ParenthesizedStatement) { it },
            
            serial(PostfixExpression, PrimaryExpression) { it },
            serial(PostfixExpression, PostfixExpression, token("."), Identifier) { target, _, name ->
                FoxFieldAccess(target, name)
            },
            serial(PostfixExpression, PostfixExpression, token("."), node<Int>()) { target, _, index ->
                FoxComponentAccess(target, index)
            },
            serial(PostfixExpression, Identifier, ActualGenericParameterList, ActualParameterList) { name, generics, parameters ->
                FoxCall(null, name, generics, parameters)
            },
            serial(PostfixExpression, Identifier, ActualParameterList) { name, parameters ->
                FoxCall(null, name, null, parameters)
            },
            serial(PostfixExpression, PostfixExpression, token("."), Identifier, ActualGenericParameterList, ActualParameterList) { target, _, name, generics, parameters ->
                FoxCall(target, name, generics, parameters)
            },
            serial(PostfixExpression, PostfixExpression, token("."), Identifier, ActualParameterList) { target, _, name, parameters ->
                FoxCall(target, name, null, parameters)
            },
            serial(PostfixExpression, node<FoxType>(), ActualParameterList) { type, parameters ->
                FoxConstruct(type, parameters)
            },
            serial(PostfixExpression, ParenthesizedStatement, AnonymousActualParameterList) { method, parameters ->
                FoxIndirectCall(null, method, parameters)
            },
            serial(PostfixExpression, PostfixExpression, token("."), ParenthesizedStatement, AnonymousActualParameterList) { target, _, method, parameters ->
                FoxIndirectCall(target, method, parameters)
            },
            
            serial(UnaryExpression, PostfixExpression) { it },
            serial(UnaryExpression, node<FoxUnaryOperator>(), UnaryExpression) { operator, node ->
                FoxUnary(operator, node)
            },
            
            serial(MultiplicativeExpression, UnaryExpression) { it },
            serial(MultiplicativeExpression, MultiplicativeExpression, MultiplicativeOperator, UnaryExpression) { left, operator, right ->
                FoxBinary(left, operator, right)
            },
            
            serial(AdditiveExpression, MultiplicativeExpression) { it },
            serial(AdditiveExpression, AdditiveExpression, AdditiveOperator, MultiplicativeExpression) { left, operator, right ->
                FoxBinary(left, operator, right)
            },
            
            serial(ShiftExpression, AdditiveExpression) { it },
            serial(ShiftExpression, ShiftExpression, ShiftOperator, AdditiveExpression) { left, operator, right ->
                FoxBinary(left, operator, right)
            },
            
            serial(ComparisonExpression, ShiftExpression) { it },
            serial(ComparisonExpression, ComparisonExpression, ComparisonOperator, ShiftExpression) { left, operator, right ->
                FoxBinary(left, operator, right)
            },
            
            serial(EqualityExpression, ComparisonExpression) { it },
            serial(EqualityExpression, EqualityExpression, EqualityOperator, ComparisonExpression) { left, operator, right ->
                FoxBinary(left, operator, right)
            },
            
            serial(BitAndExpression, EqualityExpression) { it },
            serial(BitAndExpression, BitAndExpression, BitAndOperator, EqualityExpression) { left, operator, right ->
                FoxBinary(left, operator, right)
            },
            
            serial(BitXorExpression, BitAndExpression) { it },
            serial(BitXorExpression, BitXorExpression, BitXorOperator, BitAndExpression) { left, operator, right ->
                FoxBinary(left, operator, right)
            },
            
            serial(BitOrExpression, BitXorExpression) { it },
            serial(BitOrExpression, BitOrExpression, BitOrOperator, BitXorExpression) { left, operator, right ->
                FoxBinary(left, operator, right)
            },
            
            serial(LogicalAndExpression, BitOrExpression) { it },
            serial(LogicalAndExpression, LogicalAndExpression, LogicalAndOperator, BitOrExpression) { left, operator, right ->
                FoxBinary(left, operator, right)
            },
            
            serial(LogicalOrExpression, LogicalAndExpression) { it },
            serial(LogicalOrExpression, LogicalOrExpression, LogicalOrOperator, LogicalAndExpression) { left, operator, right ->
                FoxBinary(left, operator, right)
            },
            
            serial(AssignableExpression, PostfixExpression) { it },
            
            serial(AssignmentExpression, LogicalOrExpression) { it },
            serial(AssignmentExpression, AssignableExpression, node<FoxAssignOperator>(), node<FoxStatement>()) { left, operator, right ->
                FoxAssign(left, operator, right, beforeEvaluation = true)
            },
            
            serial(node<FoxStatement>(), AssignmentExpression) { it },
            serial(node<FoxStatement>(), Identifier, token(":"), node<FoxType>()) { name, _, type ->
                FoxTypeBinding(name, type)
            },
            
            serial(node<FoxStatement>(), token("break")) { FoxBreak(null) },
            serial(node<FoxStatement>(), token("break"), Label) { _, label -> FoxBreak(label) },
            serial(node<FoxStatement>(), token("continue")) { FoxContinue(null) },
            serial(node<FoxStatement>(), token("continue"), Label) { _, label -> FoxContinue(label) },
            serial(node<FoxStatement>(), token("return")) { FoxReturn(null) },
            serial(node<FoxStatement>(), token("return"), node<FoxStatement>()) { _, value -> FoxReturn(value) },
            serial(node<FoxStatement>(), token("yield"), node<FoxStatement>()) { _, value -> FoxYield(null, value) },
            serial(node<FoxStatement>(), token("yield"), Label, node<FoxStatement>()) { _, label, value -> FoxYield(label, value) },
            
            listLike(StatementBlock, token("{"), node<FoxStatement>(), null, token("}")),
            serial(node<FoxStatement>(), StatementBlock) { FoxBlock(null, it) },
            serial(node<FoxStatement>(), Label, StatementBlock) { label, it -> FoxBlock(label, it) },
            
            serial(
                IfCore,
                token("if"),
                ParenthesizedStatement,
                node<FoxStatement>(),
            ) { _, condition, body -> ParsedIfCore(condition, body, null) },
            
            serial(
                IfCore,
                token("if"),
                ParenthesizedStatement,
                node<FoxStatement>(),
                token("else"),
                node<FoxStatement>(),
            ) { _, condition, thenBody, _, elseBody -> ParsedIfCore(condition, thenBody, elseBody) },
            
            serial(node<FoxStatement>(), IfCore) { core ->
                FoxIf(null, core.condition, core.thenBody, core.elseBody)
            },
            
            serial(node<FoxStatement>(), Label, IfCore) { label, core ->
                FoxIf(label, core.condition, core.thenBody, core.elseBody)
            },
            
            serial(
                WhileCore,
                token("while"),
                ParenthesizedStatement,
                node<FoxStatement>(),
            ) { _, condition, body -> ParsedWhileCore(condition, body) },
            
            serial(node<FoxStatement>(), WhileCore) { core ->
                FoxWhile(null, core.condition, core.body)
            },
            
            serial(node<FoxStatement>(), Label, WhileCore) { label, core ->
                FoxWhile(label, core.condition, core.body)
            },
            
            serial(
                DoWhileCore,
                token("do"),
                node<FoxStatement>(),
                token("while"),
                ParenthesizedStatement,
            ) { _, body, _, condition -> ParsedDoWhileCore(body, condition) },
            
            serial(node<FoxStatement>(), DoWhileCore) { core ->
                FoxDoWhile(null, core.body, core.condition)
            },
            
            serial(node<FoxStatement>(), Label, DoWhileCore) { label, core ->
                FoxDoWhile(label, core.body, core.condition)
            },
            
            listLike(
                WhenCaseConditionList,
                null,
                node<FoxStatement>(),
                token(","),
                token("->"),
            ),
            
            serial(
                WhenCase,
                WhenCaseConditionList,
                node<FoxStatement>(),
            ) { conditions, body -> FoxCase(conditions, body) },
            
            serial(
                WhenCase,
                token("else"),
                token("->"),
                node<FoxStatement>(),
            ) { _, _, body -> FoxCase(emptyList(), body) },
            
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
            
            serial(node<FoxStatement>(), WhenCore) { core ->
                FoxWhen(null, core.value, core.cases)
            },
            
            serial(node<FoxStatement>(), Label, WhenCore) { label, core ->
                FoxWhen(label, core.value, core.cases)
            },
            
            serial(ThisTypeQualifier, node<FoxType>(), token(".")) { type, _ -> type },
            serial(ReturnTypeClause, token(":"), node<FoxType>()) { _, type -> type },
            
            serial(
                node<FoxTypeAlias>(),
                token("type"),
                TypeName,
                FormalGenericParameterListWithoutConstraints,
                token("="),
                node<FoxType>(),
            ) { _, name, generics, _, type -> FoxTypeAlias(name, generics, type) },
            serial(
                node<FoxTypeAlias>(),
                token("type"),
                TypeName,
                token("="),
                node<FoxType>(),
            ) { _, name, _, type -> FoxTypeAlias(name, null, type) },
            
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
                node<FoxMethodDefinition>(),
                MethodHead,
                ReturnTypeClause,
                node<FoxStatement>(),
            ) { head, returnType, body ->
                FoxMethodDefinition(head.generics, head.thisType, head.name, head.parameters, returnType, body)
            },
            serial(
                node<FoxMethodDefinition>(),
                MethodHead,
                node<FoxStatement>(),
            ) { head, body ->
                FoxMethodDefinition(head.generics, head.thisType, head.name, head.parameters, null, body)
            },
            
            serial(node<FoxFileElement>(), node<FoxTypeAlias>()) { it },
            serial(node<FoxFileElement>(), node<FoxMethodDefinition>()) { it },
            listLike(FileElementList, null, node<FoxFileElement>(), null, null),
            serial(node<FoxFile>(), FileElementList) { FoxFile(it) },
        ),
    )
}

val FoxGrammar = Grammar(FoxProductions)

private data class ParsedIfCore(
    val condition: FoxStatement,
    val thenBody: FoxStatement,
    val elseBody: FoxStatement?,
)

private data class ParsedWhileCore(
    val condition: FoxStatement,
    val body: FoxStatement,
)

private data class ParsedDoWhileCore(
    val body: FoxStatement,
    val condition: FoxStatement,
)

private data class ParsedWhenCore(
    val value: FoxStatement?,
    val cases: List<FoxCase>,
)

private data class ParsedMethodHead(
    val generics: OrderedMap<String, FoxGenericConstraint>?,
    val thisType: FoxType?,
    val name: String,
    val parameters: OrderedMap<String, FoxType>,
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

private val formattedExpressionCache = Collections.synchronizedMap(HashMap<String, FoxStatement>())

private fun parseFormattedExpression(source: String): FoxStatement {
    formattedExpressionCache[source]?.let { return it }
    val parser = Parser(FoxGrammar, node<FoxStatement>())
    val report = parser.parse(source)
    val success = report.result as? Success<FoxStatement>
    if (success != null && success.interval.end.fragIndex == report.context.fragments.size) {
        formattedExpressionCache[source] = success.node
        return success.node
    }
    throw ParseException("Invalid formatted string expression '$source': ${report.stop}")
}
