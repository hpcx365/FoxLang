package pers.hpcx.foxlang.frontend.fox

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.frontend.common.GrammarRuleFactoryResult
import pers.hpcx.foxlang.frontend.common.buildGrammar
import pers.hpcx.foxlang.frontend.common.factoryError
import pers.hpcx.foxlang.frontend.common.factorySuccess
import pers.hpcx.foxlang.runtime.*
import pers.hpcx.foxlang.type.toFoxTupleType
import pers.hpcx.foxlang.utils.*

internal val CamelWordRegex = AutoRegex.pattern("[a-z][a-zA-Z0-9_]*")
internal val PascalWordRegex = AutoRegex.pattern("[A-Z][a-zA-Z0-9_]*")
internal val ReservedKeywordRegex = AutoRegex.literals(Keywords.keys)
internal val IdentifierRegex = CamelWordRegex - ReservedKeywordRegex
internal val TypeNameRegex = PascalWordRegex - ReservedKeywordRegex

internal val BinIntRegex = AutoRegex.pattern(
    "0b[01]+(_[01]+)*",
)
internal val DecIntRegex = AutoRegex.pattern(
    "(0|[1-9][0-9]*(_[0-9]+)*)",
)
internal val HexIntRegex = AutoRegex.pattern(
    "0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*",
)
internal val BinLongRegex = AutoRegex.pattern(
    "0b[01]+(_[01]+)*L",
)
internal val DecLongRegex = AutoRegex.pattern(
    "(0|[1-9][0-9]*(_[0-9]+)*)L",
)
internal val HexLongRegex = AutoRegex.pattern(
    "0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*L",
)
internal val DecFloatRegex = AutoRegex.pattern(
    "(0|[1-9][0-9]*(_[0-9]+)*)(\\.[0-9]+(_[0-9]+)*)?(e[+-]?[0-9]+)?f",
)
internal val HexFloatRegex = AutoRegex.pattern(
    "0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*(\\.[0-9a-fA-F]+(_[0-9a-fA-F]+)*)?p[+-]?[0-9]+f",
)
internal val DecDoubleRegex = AutoRegex.pattern(
    "(0|[1-9][0-9]*(_[0-9]+)*)(\\.[0-9]+(_[0-9]+)*)(e[+-]?[0-9]+)?|(0|[1-9][0-9]*(_[0-9]+)*)e[+-]?[0-9]+",
)
internal val HexDoubleRegex = AutoRegex.pattern(
    "0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*(\\.[0-9a-fA-F]+(_[0-9a-fA-F]+)*)?p[+-]?[0-9]+",
)

val FoxGrammar = buildGrammar {
    sequence {
        yieldAll(Punctuations.entries)
        yieldAll(Operators.entries)
        yieldAll(Keywords.entries)
    }.forEach { (token, symbol) ->
        rules(symbol) {
            fixed(token) { }
        }
    }
    rules(LParen) { fixed("(") { } }
    rules(RParen) { fixed(")") { } }
    rules(LBracket) { fixed("[") { } }
    rules(RBracket) { fixed("]") { } }
    rules(LBrace) { fixed("{") { } }
    rules(RBrace) { fixed("}") { } }
    rules(LAngle) { fixed("<") { } }
    rules(RAngle) { fixed(">") { } }
    
    rules(LineBreak) { lineBreak { } }
    rules(FormattedStringStart) { formattedStringStart { it } }
    rules(FormattedStringEnd) { formattedStringEnd { } }
    rules(FormattedExpressionOpen) { formattedExpressionStart { } }
    rules(FormattedExpressionClose) { formattedExpressionEnd { } }
    
    rules(Dot) { symbols(LineBreak, Dot) { _, _ -> } }
    rules(Comma) { symbols(Comma, LineBreak) { _, _ -> } }
    rules(ParenOpen) { symbols(ParenOpen, LineBreak) { _, _ -> } }
    rules(ParenClose) { symbols(LineBreak, ParenClose) { _, _ -> } }
    rules(BracketOpen) { symbols(BracketOpen, LineBreak) { _, _ -> } }
    rules(BracketClose) { symbols(LineBreak, BracketClose) { _, _ -> } }
    rules(BraceOpen) { symbols(BraceOpen, LineBreak) { _, _ -> } }
    rules(BraceClose) { symbols(LineBreak, BraceClose) { _, _ -> } }
    rules(AngleOpen) { symbols(AngleOpen, LineBreak) { _, _ -> } }
    rules(AngleClose) { symbols(LineBreak, AngleClose) { _, _ -> } }
    
    rules(CamelWord) { regex(CamelWordRegex) { it } }
    rules(PascalWord) { regex(PascalWordRegex) { it } }
    rules(Identifier) { regex(IdentifierRegex) { it } }
    rules(TypeName) { regex(TypeNameRegex) { it } }
    
    rules(IdentifierEqual) { symbols(Identifier, Assign) { it, _ -> it } }
    rules(IdentifierColon) { symbols(Identifier, Colon) { it, _ -> it } }
    rules(TypeNameEqual) { symbols(TypeName, Assign) { it, _ -> it } }
    rules(TypeNameColon) { symbols(TypeName, Colon) { it, _ -> it } }
    
    rules(LitUnit) { symbols(KwLowerUnit) {} }
    rules(LitBool) {
        symbols(KwTrue) { true }
        symbols(KwFalse) { false }
    }
    rules(LitInt) {
        regex(BinIntRegex) { it.drop(2).replace("_", "").toInt(2) }
        regex(DecIntRegex) { it.replace("_", "").toInt() }
        regex(HexIntRegex) { it.drop(2).replace("_", "").toInt(16) }
    }
    rules(LitLong) {
        regex(BinLongRegex) { it.drop(2).dropLast(1).replace("_", "").toLong(2) }
        regex(DecLongRegex) { it.dropLast(1).replace("_", "").toLong() }
        regex(HexLongRegex) { it.drop(2).dropLast(1).replace("_", "").toLong(16) }
    }
    rules(LitFloat) {
        regex(DecFloatRegex) { it.dropLast(1).replace("_", "").toFloat() }
        regex(HexFloatRegex) { it.dropLast(1).replace("_", "").toFloat() }
    }
    rules(LitDouble) {
        regex(DecDoubleRegex) { it.replace("_", "").toDouble() }
        regex(HexDoubleRegex) { it.replace("_", "").toDouble() }
    }
    rules(LitChar) { charLiteral { it } }
    rules(LitString) { stringLiteral { it } }
    
    rules(UnaryOperator) {
        symbols(Not) { FoxNotOperator }
        symbols(Sub) { FoxNegOperator }
    }
    
    rules(AssignOperator) {
        symbols(Assign) { FoxPlainAssignOperator }
        symbols(DefAssign) { FoxTypeBindingAssignOperator }
        symbols(AddAssign) { FoxAddAssignOperator }
        symbols(SubAssign) { FoxSubAssignOperator }
        symbols(MulAssign) { FoxMulAssignOperator }
        symbols(DivAssign) { FoxDivAssignOperator }
        symbols(RemAssign) { FoxRemAssignOperator }
        symbols(AndAssign) { FoxAndAssignOperator }
        symbols(OrAssign) { FoxOrAssignOperator }
        symbols(XorAssign) { FoxXorAssignOperator }
        symbols(LShiftAssign) { FoxShlAssignOperator }
        symbols(RShiftAssign) { FoxShrAssignOperator }
        symbols(URShiftAssign) { FoxUshrAssignOperator }
        symbols(AndAndAssign) { FoxAndAndAssignOperator }
        symbols(OrOrAssign) { FoxOrOrAssignOperator }
    }
    
    rules(MultiplicativeOperator) {
        symbols(Mul) { FoxMulOperator }
        symbols(Div) { FoxDivOperator }
        symbols(Rem) { FoxRemOperator }
    }
    rules(AdditiveOperator) {
        symbols(Add) { FoxAddOperator }
        symbols(Sub) { FoxSubOperator }
    }
    rules(ShiftOperator) {
        symbols(LShift) { FoxShlOperator }
        symbols(RShift) { FoxShrOperator }
        symbols(URShift) { FoxUshrOperator }
    }
    rules(ComparisonOperator) {
        symbols(Lt) { FoxLtOperator }
        symbols(Gt) { FoxGtOperator }
        symbols(Leq) { FoxLeOperator }
        symbols(Geq) { FoxGeOperator }
    }
    rules(EqualityOperator) {
        symbols(Eq) { FoxEqOperator }
        symbols(Neq) { FoxNeqOperator }
    }
    rules(BitAndOperator) { symbols(And) { FoxAndOperator } }
    rules(BitXorOperator) { symbols(Xor) { FoxXorOperator } }
    rules(BitOrOperator) { symbols(Or) { FoxOrOperator } }
    rules(LogicalAndOperator) { symbols(AndAnd) { FoxAndAndOperator } }
    rules(LogicalOrOperator) { symbols(OrOr) { FoxOrOrOperator } }
    
    rules(BinaryOperator) {
        symbols(MultiplicativeOperator) { it }
        symbols(AdditiveOperator) { it }
        symbols(ShiftOperator) { it }
        symbols(ComparisonOperator) { it }
        symbols(EqualityOperator) { it }
        symbols(BitAndOperator) { it }
        symbols(BitXorOperator) { it }
        symbols(BitOrOperator) { it }
        symbols(LogicalAndOperator) { it }
        symbols(LogicalOrOperator) { it }
    }
    
    rules(Type) {
        symbols(KwVoid) { FoxVoidType }
        symbols(KwUnit) { FoxUnitType }
        symbols(KwBool) { FoxBoolType }
        symbols(KwByte) { FoxByteType }
        symbols(KwShort) { FoxShortType }
        symbols(KwInt) { FoxIntType }
        symbols(KwLong) { FoxLongType }
        symbols(KwFloat) { FoxFloatType }
        symbols(KwDouble) { FoxDoubleType }
        symbols(KwChar) { FoxCharType }
        symbols(KwString) { FoxStringType }
        symbols(KwAny) { FoxAnyType }
        symbols(KwAnyTuple) { FoxAnyTupleType }
        symbols(KwAnyStruct) { FoxAnyStructType }
        symbols(KwAnyObject) { FoxAnyObjectType }
        symbols(KwAnyEnum) { FoxAnyEnumType }
        symbols(KwAnyOf, AnonymousActualGenericParameterList) { _, it ->
            FoxAnyOfType(it)
        }
        symbols(KwAllOf, AnonymousActualGenericParameterList) { _, it ->
            FoxAllOfType(it)
        }
        symbols(KwNoneOf, AnonymousActualGenericParameterList) { _, it ->
            FoxNoneOfType(it)
        }
        symbols(KwTuple, TupleComponentParameterList) { _, it ->
            it.toFoxTupleType()
        }
        symbolsResult(KwAnyTupleOf, AnonymousActualGenericParameterList) { _, it ->
            if (it.size != 1) factoryError("AnyTupleOf type must have exactly one generic parameter")
            else factorySuccess(FoxAnyTupleOfType(it.first()))
        }
        symbols(KwStruct, StructFieldParameterList) { _, it ->
            FoxStructType(it)
        }
        symbolsResult(KwAnyStructOf, AnonymousActualGenericParameterList) { _, it ->
            if (it.isEmpty()) factoryError("AnyStructOf type must have at least one generic parameter")
            else factorySuccess(FoxAnyStructOfType(it))
        }
        symbols(KwObject, ObjectMemberParameterList) { _, it ->
            FoxObjectType(it)
        }
        symbols(KwEnum, EnumItemParameterList) { _, it ->
            FoxEnumType(it)
        }
        symbolsResult(KwArray, AnonymousActualGenericParameterList) { _, it ->
            if (it.size != 1) factoryError("Array type must have exactly one generic parameter")
            else factorySuccess(FoxArrayType(it.first()))
        }
        symbolsResult(KwRef, AnonymousActualGenericParameterList) { _, it ->
            if (it.size != 1) factoryError("Ref type must have exactly one generic parameter")
            else factorySuccess(FoxRefType(it.first()))
        }
        symbolsResult(KwMethod, MethodTypeArgumentList) { _, items ->
            items.toFoxMethodType()
        }
        symbols(KwComponentAt, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, index, _ ->
            FoxTupleComponentAtType(type, index)
        }
        symbols(KwLastComponentAt, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, index, _ ->
            FoxTupleLastComponentAtType(type, index)
        }
        symbols(KwFirstComponentsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxTupleFirstComponentsOfType(type, count)
        }
        symbols(KwExactFirstComponentsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxTupleExactFirstComponentsOfType(type, count)
        }
        symbols(KwLastComponentsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxTupleLastComponentsOfType(type, count)
        }
        symbols(KwExactLastComponentsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxTupleExactLastComponentsOfType(type, count)
        }
        symbols(KwDropFirstComponentsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxTupleDropFirstComponentsOfType(type, count)
        }
        symbols(KwExactDropFirstComponentsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxTupleExactDropFirstComponentsOfType(type, count)
        }
        symbols(KwDropLastComponentsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxTupleDropLastComponentsOfType(type, count)
        }
        symbols(KwExactDropLastComponentsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxTupleExactDropLastComponentsOfType(type, count)
        }
        symbols(KwMergeComponentsOf, AnonymousActualGenericParameterList) { _, it ->
            FoxTupleMergeComponentsOfType(it)
        }
        symbols(KwFieldOf, AngleOpen, Type, Comma, Identifier, AngleClose) { _, _, type, _, name, _ ->
            FoxStructFieldOfType(type, name)
        }
        symbols(KwFieldAt, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, index, _ ->
            FoxStructFieldAtType(type, index)
        }
        symbols(KwLastFieldAt, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, index, _ ->
            FoxStructLastFieldAtType(type, index)
        }
        symbols(KwFirstFieldsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxStructFirstFieldsOfType(type, count)
        }
        symbols(KwExactFirstFieldsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxStructExactFirstFieldsOfType(type, count)
        }
        symbols(KwLastFieldsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxStructLastFieldsOfType(type, count)
        }
        symbols(KwExactLastFieldsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxStructExactLastFieldsOfType(type, count)
        }
        symbols(KwDropFirstFieldsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxStructDropFirstFieldsOfType(type, count)
        }
        symbols(KwExactDropFirstFieldsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxStructExactDropFirstFieldsOfType(type, count)
        }
        symbols(KwDropLastFieldsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxStructDropLastFieldsOfType(type, count)
        }
        symbols(KwExactDropLastFieldsOf, AngleOpen, Type, Comma, LitInt, AngleClose) { _, _, type, _, count, _ ->
            FoxStructExactDropLastFieldsOfType(type, count)
        }
        symbols(KwFieldsOf, AngleOpen, Type, Comma, StructFieldNameList, AngleClose) { _, _, type, _, names, _ ->
            FoxStructFieldsOfType(type, names)
        }
        symbols(KwDropFieldsOf, AngleOpen, Type, Comma, StructFieldNameList, AngleClose) { _, _, type, _, names, _ ->
            FoxStructDropFieldsOfType(type, names.toSet())
        }
        symbols(KwMergeFieldsOf, AnonymousActualGenericParameterList) { _, it ->
            FoxStructMergeFieldsOfType(it)
        }
        symbols(KwMemberOf, AngleOpen, Type, Comma, Identifier, AngleClose) { _, _, type, _, name, _ ->
            FoxObjectMemberOfType(type, name)
        }
        symbols(KwMembersOf, AngleOpen, Type, Comma, ObjectMemberNameList, AngleClose) { _, _, type, _, names, _ ->
            FoxObjectMembersOfType(type, names)
        }
        symbols(KwDropMembersOf, AngleOpen, Type, Comma, ObjectMemberNameList, AngleClose) { _, _, type, _, names, _ ->
            FoxObjectDropMembersOfType(type, names)
        }
        symbols(KwMergeMembersOf, AnonymousActualGenericParameterList) { _, it ->
            FoxObjectMergeMembersOfType(it)
        }
        symbols(KwItemOf, AngleOpen, Type, Comma, TypeName, AngleClose) { _, _, type, _, name, _ ->
            FoxEnumItemOfType(type, name)
        }
        symbols(KwItemsOf, AngleOpen, Type, Comma, EnumItemNameList, AngleClose) { _, _, type, _, names, _ ->
            FoxEnumItemsOfType(type, names)
        }
        symbols(KwDropItemsOf, AngleOpen, Type, Comma, EnumItemNameList, AngleClose) { _, _, type, _, names, _ ->
            FoxEnumDropItemsOfType(type, names)
        }
        symbols(KwMergeItemsOf, AnonymousActualGenericParameterList) { _, it ->
            FoxEnumMergeItemsOfType(it)
        }
        symbols(KwElementOf, AngleOpen, Type, AngleClose) { _, _, type, _ ->
            FoxArrayElementOfType(type)
        }
        symbols(KwReferentOf, AngleOpen, Type, AngleClose) { _, _, type, _ ->
            FoxRefReferentOfType(type)
        }
        symbols(KwMethodOf, AngleOpen, Type, Comma, Type, Comma, Type, AngleClose) { _, _, `this`, _, parameters, _, `return`, _ ->
            FoxMethodOfType(`this`, parameters, `return`)
        }
        symbols(KwThisOf, AngleOpen, Type, AngleClose) { _, _, type, _ ->
            FoxMethodThisOfType(type)
        }
        symbols(KwParametersOf, AngleOpen, Type, AngleClose) { _, _, type, _ ->
            FoxMethodParametersOfType(type)
        }
        symbols(KwReturnOf, AngleOpen, Type, AngleClose) { _, _, type, _ ->
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
        symbols(IdentifierColon, Type) { name, type -> name to type }
    }
    rules(FormalParameterListHead) {
        symbols(ParenOpen, FormalParameter) { _, it -> listOf(it) }
        symbols(FormalParameterListHead, Comma, FormalParameter) { head, _, it -> head + it }
    }
    rules(FormalParameterList) {
        symbols(LParen, ParenClose) { _, _ -> emptyOrderedMap() }
        symbolsResult(FormalParameterListHead, ParenClose) { head, _ -> head.toOrderedMap("formal parameter") }
        symbolsResult(FormalParameterListHead, Comma, RParen) { head, _, _ -> head.toOrderedMap("formal parameter") }
    }
    
    rules(ActualParameter) {
        symbols(Statement) { null to it }
        symbols(ImplicitLambdaLiteral) { null to it }
        symbols(IdentifierEqual, Statement) { name, value -> name to value }
        symbols(IdentifierEqual, ImplicitLambdaLiteral) { name, value -> name to value }
    }
    rules(ActualParameterListHead) {
        symbols(ParenOpen, ActualParameter) { _, it -> listOf(it) }
        symbols(ActualParameterListHead, Comma, ActualParameter) { head, _, it -> head + it }
    }
    rules(ActualParameterList) {
        symbols(LParen, ParenClose) { _, _ -> emptyList() }
        symbols(ActualParameterListHead, ParenClose) { head, _ -> head }
        symbols(ActualParameterListHead, Comma, RParen) { head, _, _ -> head }
    }
    
    rules(LambdaParameter) {
        symbols(Identifier) { name -> name to null }
        symbols(IdentifierColon, Type) { name, type -> name to type }
    }
    rules(LambdaParameterListHead) {
        symbols(LambdaParameter) { listOf(it) }
        symbols(LineBreak, LambdaParameter) { _, it -> listOf(it) }
        symbols(LambdaParameterListHead, Comma, LambdaParameter) { head, _, it -> head + it }
    }
    rules(LambdaParameterList) {
        symbols(LambdaParameterListHead) { it }
        symbols(LambdaParameterListHead, Comma) { head, _ -> head }
    }
    
    rules(FormalGenericParameter) {
        symbols(TypeName) { it to FoxAnyType }
        symbols(TypeName, Assign, Type) { name, _, constraint -> name to constraint }
    }
    rules(FormalGenericParameterListHead) {
        symbols(AngleOpen, FormalGenericParameter) { _, it -> listOf(it) }
        symbols(FormalGenericParameterListHead, Comma, FormalGenericParameter) { head, _, it -> head + it }
    }
    rules(FormalGenericParameterList) {
        symbols(LAngle, AngleClose) { _, _ -> emptyOrderedMap() }
        symbolsResult(FormalGenericParameterListHead, AngleClose) { head, _ -> head.toOrderedMap("formal generic parameter") }
        symbolsResult(FormalGenericParameterListHead, Comma, RAngle) { head, _, _ -> head.toOrderedMap("formal generic parameter") }
    }
    
    rules(FormalGenericParameterNoConstraints) {
        symbols(TypeName) { it }
    }
    rules(FormalGenericParameterNoConstraintsListHead) {
        symbols(AngleOpen, FormalGenericParameterNoConstraints) { _, it -> listOf(it) }
        symbols(FormalGenericParameterNoConstraintsListHead, Comma, FormalGenericParameterNoConstraints) { head, _, it -> head + it }
    }
    rules(FormalGenericParameterNoConstraintsList) {
        symbols(LAngle, AngleClose) { _, _ -> emptyOrderedSet() }
        symbolsResult(FormalGenericParameterNoConstraintsListHead, AngleClose) { head, _ -> head.toOrderedSet("formal generic parameter") }
        symbolsResult(FormalGenericParameterNoConstraintsListHead, Comma, RAngle) { head, _, _ -> head.toOrderedSet("formal generic parameter") }
    }
    
    rules(ActualGenericParameter) {
        symbols(Type) { null to it }
        symbols(TypeNameEqual, Type) { name, type -> name to type }
    }
    rules(ActualGenericParameterListHead) {
        symbols(AngleOpen, ActualGenericParameter) { _, it -> listOf(it) }
        symbols(ActualGenericParameterListHead, Comma, ActualGenericParameter) { head, _, it -> head + it }
    }
    rules(ActualGenericParameterList) {
        symbols(LAngle, AngleClose) { _, _ -> emptyList() }
        symbols(ActualGenericParameterListHead, AngleClose) { head, _ -> head }
        symbols(ActualGenericParameterListHead, Comma, RAngle) { head, _, _ -> head }
    }
    
    rules(NamedActualGenericParameter) {
        symbols(TypeNameEqual, Type) { name, type -> name to type }
    }
    rules(NamedActualGenericParameterListHead) {
        symbols(AngleOpen, NamedActualGenericParameter) { _, it -> listOf(it) }
        symbols(NamedActualGenericParameterListHead, Comma, NamedActualGenericParameter) { head, _, it -> head + it }
    }
    rules(NamedActualGenericParameterList) {
        symbols(LAngle, AngleClose) { _, _ -> emptyMap() }
        symbolsResult(NamedActualGenericParameterListHead, AngleClose) { head, _ -> head.toMap("named actual generic parameter") }
        symbolsResult(NamedActualGenericParameterListHead, Comma, RAngle) { head, _, _ -> head.toMap("named actual generic parameter") }
    }
    
    rules(AnonymousActualGenericParameter) {
        symbols(Type) { it }
    }
    rules(AnonymousActualGenericParameterListHead) {
        symbols(AngleOpen, AnonymousActualGenericParameter) { _, it -> listOf(it) }
        symbols(AnonymousActualGenericParameterListHead, Comma, AnonymousActualGenericParameter) { head, _, it -> head + it }
    }
    rules(AnonymousActualGenericParameterList) {
        symbols(LAngle, AngleClose) { _, _ -> emptyList() }
        symbols(AnonymousActualGenericParameterListHead, AngleClose) { head, _ -> head }
        symbols(AnonymousActualGenericParameterListHead, Comma, RAngle) { head, _, _ -> head }
    }
    
    rules(TupleComponentParameter) {
        symbols(Type) { it to 1 }
        symbolsResult(Type, Colon, LitInt) { type, _, count ->
            if (count <= 0) factoryError("Tuple component count must be positive")
            else factorySuccess(type to count)
        }
    }
    rules(TupleComponentParameterListHead) {
        symbols(AngleOpen, TupleComponentParameter) { _, it -> listOf(it) }
        symbols(TupleComponentParameterListHead, Comma, TupleComponentParameter) { head, _, it -> head + it }
    }
    rules(TupleComponentParameterList) {
        symbols(LAngle, AngleClose) { _, _ -> emptyList() }
        symbols(TupleComponentParameterListHead, AngleClose) { head, _ -> head }
        symbols(TupleComponentParameterListHead, Comma, RAngle) { head, _, _ -> head }
    }
    
    rules(StructFieldParameter) {
        symbols(IdentifierColon, Type) { name, type -> name to type }
    }
    rules(StructFieldParameterListHead) {
        symbols(AngleOpen, StructFieldParameter) { _, it -> listOf(it) }
        symbols(StructFieldParameterListHead, Comma, StructFieldParameter) { head, _, it -> head + it }
    }
    rules(StructFieldParameterList) {
        symbols(LAngle, AngleClose) { _, _ -> emptyOrderedMap() }
        symbolsResult(StructFieldParameterListHead, AngleClose) { head, _ -> head.toOrderedMap("struct field parameter") }
        symbolsResult(StructFieldParameterListHead, Comma, RAngle) { head, _, _ -> head.toOrderedMap("struct field parameter") }
    }
    
    rules(StructFieldName) {
        symbols(Identifier) { it }
    }
    rules(StructFieldNameListHead) {
        symbols(StructFieldName) { listOf(it) }
        symbols(StructFieldNameListHead, Comma, StructFieldName) { head, _, it -> head + it }
    }
    rules(StructFieldNameList) {
        symbolsResult(StructFieldNameListHead) { it.toOrderedSet("struct field name") }
    }
    
    rules(ObjectMemberParameter) {
        symbols(IdentifierColon, Type) { name, type -> name to type }
    }
    rules(ObjectMemberParameterListHead) {
        symbols(AngleOpen, ObjectMemberParameter) { _, it -> listOf(it) }
        symbols(ObjectMemberParameterListHead, Comma, ObjectMemberParameter) { head, _, it -> head + it }
    }
    rules(ObjectMemberParameterList) {
        symbols(LAngle, AngleClose) { _, _ -> emptyMap() }
        symbolsResult(ObjectMemberParameterListHead, AngleClose) { head, _ -> head.toMap("object member parameter") }
        symbolsResult(ObjectMemberParameterListHead, Comma, RAngle) { head, _, _ -> head.toMap("object member parameter") }
    }
    
    rules(ObjectMemberName) {
        symbols(Identifier) { it }
    }
    rules(ObjectMemberNameListHead) {
        symbols(ObjectMemberName) { listOf(it) }
        symbols(ObjectMemberNameListHead, Comma, ObjectMemberName) { head, _, it -> head + it }
    }
    rules(ObjectMemberNameList) {
        symbolsResult(ObjectMemberNameListHead) { it.toSet("object member name") }
    }
    
    rules(EnumItemParameter) {
        symbols(TypeNameEqual, Type) { name, type -> name to type }
    }
    rules(EnumItemParameterListHead) {
        symbols(AngleOpen, EnumItemParameter) { _, it -> listOf(it) }
        symbols(EnumItemParameterListHead, Comma, EnumItemParameter) { head, _, it -> head + it }
    }
    rules(EnumItemParameterList) {
        symbols(LAngle, AngleClose) { _, _ -> emptyMap() }
        symbolsResult(EnumItemParameterListHead, AngleClose) { head, _ -> head.toMap("enum item parameter") }
        symbolsResult(EnumItemParameterListHead, Comma, RAngle) { head, _, _ -> head.toMap("enum item parameter") }
    }
    
    rules(EnumItemName) {
        symbols(TypeName) { it }
    }
    rules(EnumItemNameListHead) {
        symbols(EnumItemName) { listOf(it) }
        symbols(EnumItemNameListHead, Comma, EnumItemName) { head, _, it -> head + it }
    }
    rules(EnumItemNameList) {
        symbolsResult(EnumItemNameListHead) { it.toSet("enum item name") }
    }
    
    rules(MethodTypeArgument) {
        symbols(KwThis, Colon, Type) { _, _, type ->
            ParsedMethodTypeArgument.This(type)
        }
        symbols(KwReturn, Colon, Type) { _, _, type ->
            ParsedMethodTypeArgument.Return(type)
        }
        symbols(FormalParameter) { (name, type) ->
            ParsedMethodTypeArgument.Parameter(name, type)
        }
        symbols(Type) { type ->
            ParsedMethodTypeArgument.AnonymousType(type)
        }
    }
    rules(MethodTypeArgumentListHead) {
        symbols(AngleOpen, MethodTypeArgument) { _, it -> listOf(it) }
        symbols(MethodTypeArgumentListHead, Comma, MethodTypeArgument) { head, _, it -> head + it }
    }
    rules(MethodTypeArgumentList) {
        symbols(LAngle, AngleClose) { _, _ -> emptyList() }
        symbols(MethodTypeArgumentListHead, AngleClose) { head, _ -> head }
        symbols(MethodTypeArgumentListHead, Comma, RAngle) { head, _, _ -> head }
    }
    
    rules(Label) { symbols(Hash, Identifier) { _, it -> it } }
    rules(ParenthesizedStatement) { symbols(ParenOpen, Statement, ParenClose) { _, node, _ -> node } }
    
    rules(FormattedStringPart) {
        formattedStringText { FoxFormattedText(it) }
        symbols(FormattedExpressionOpen, AssignmentExpression, FormattedExpressionClose) { _, expression, _ ->
            FoxFormattedExpression(expression)
        }
    }
    
    rules(FormattedStringPartListHead) {
        symbols(FormattedStringPart) { listOf(it) }
        symbols(FormattedStringPartListHead, FormattedStringPart) { head, part -> head + part }
    }
    
    rules(LambdaStatementBlockHead) {
        symbols(LineBreak, StatementLine) { _, it -> listOf(it) }
        symbols(LambdaStatementBlockHead, StatementLine) { head, it -> head + it }
    }
    
    rules(LambdaBody) {
        symbols(Statement) { it }
        symbols(LineBreak, Statement) { _, it -> it }
        symbols(LambdaStatementBlockHead) { FoxBlock(null, it) }
    }
    
    rules(ExplicitLambdaLiteral) {
        symbols(BraceOpen, Arrow, BraceClose) { _, _, _ ->
            FoxLambda(emptyList(), FoxBlock(null, emptyList()))
        }
        symbols(BraceOpen, Arrow, LambdaBody, RBrace) { _, _, body, _ ->
            FoxLambda(emptyList(), body)
        }
        symbols(BraceOpen, LambdaParameterList, Arrow, BraceClose) { _, parameters, _, _ ->
            FoxLambda(parameters, FoxBlock(null, emptyList()))
        }
        symbols(BraceOpen, LambdaParameterList, Arrow, LambdaBody, RBrace) { _, parameters, _, body, _ ->
            FoxLambda(parameters, body)
        }
    }
    
    rules(InlineImplicitLambdaLiteral) {
        symbols(LBrace, Statement, RBrace) { _, body, _ ->
            FoxLambda(null, body)
        }
    }
    
    rules(ImplicitLambdaLiteral) {
        symbols(LBrace, BraceClose) { _, _ ->
            FoxLambda(null, FoxBlock(null, emptyList()))
        }
        symbols(InlineImplicitLambdaLiteral) { it }
        symbols(BraceOpen, LambdaStatementBlockHead, BraceClose) { _, statements, _ ->
            FoxLambda(null, FoxBlock(null, statements))
        }
    }
    
    rules(LambdaLiteral) {
        symbols(ExplicitLambdaLiteral) { it }
        symbols(ImplicitLambdaLiteral) { it }
    }
    
    rules(SingleLineStatementBlock) {
        symbols(LBrace, Statement, RBrace) { _, statement, _ ->
            FoxBlock(null, listOf(statement))
        }
        symbols(Label, LBrace, Statement, RBrace) { label, _, statement, _ ->
            FoxBlock(label, listOf(statement))
        }
    }
    
    rules(PrimaryExpression) {
        symbols(LitUnit) { FoxEntityStatement(FoxUnit) }
        symbols(LitBool) { FoxEntityStatement(FoxBool(it)) }
        symbols(LitInt) { FoxEntityStatement(FoxInt(it)) }
        symbols(LitLong) { FoxEntityStatement(FoxLong(it)) }
        symbols(LitFloat) { FoxEntityStatement(FoxFloat(it)) }
        symbols(LitDouble) { FoxEntityStatement(FoxDouble(it)) }
        symbols(LitChar) { FoxEntityStatement(FoxChar(it)) }
        symbols(LitString) { FoxEntityStatement(FoxString(it)) }
        symbols(KwThis) { FoxThis }
        symbols(Identifier) { FoxSymbol(it) }
        symbols(ParenthesizedStatement) { it }
        symbols(ExplicitLambdaLiteral) { it }
        symbols(FormattedStringStart, FormattedStringEnd) { isRaw, _ ->
            FoxFormattedString(emptyList(), isRaw)
        }
        symbols(FormattedStringStart, FormattedStringPartListHead, FormattedStringEnd) { isRaw, parts, _ ->
            FoxFormattedString(parts, isRaw)
        }
    }
    
    rules(PostfixExpression) {
        symbols(PrimaryExpression) { it }
        symbols(PostfixExpression, Dot, Identifier) { target, _, name ->
            FoxFieldAccess(target, name)
        }
        symbols(PostfixExpression, Dot, LitInt) { target, _, index ->
            FoxComponentAccess(target, index)
        }
        symbols(Identifier, ActualGenericParameterList, ActualParameterList) { name, generics, parameters ->
            FoxCall(FoxEntityStatement(FoxUnit), name, generics, parameters)
        }
        symbols(Identifier, ActualGenericParameterList, ActualParameterList, LambdaLiteral) { name, generics, parameters, lambda ->
            FoxCall(FoxEntityStatement(FoxUnit), name, generics, parameters + listOf(null to lambda))
        }
        symbols(Identifier, ActualGenericParameterList, LambdaLiteral) { name, generics, lambda ->
            FoxCall(FoxEntityStatement(FoxUnit), name, generics, listOf(null to lambda))
        }
        symbols(Identifier, ActualParameterList) { name, parameters ->
            FoxCall(FoxEntityStatement(FoxUnit), name, null, parameters)
        }
        symbols(Identifier, ActualParameterList, LambdaLiteral) { name, parameters, lambda ->
            FoxCall(FoxEntityStatement(FoxUnit), name, null, parameters + listOf(null to lambda))
        }
        symbols(Identifier, LambdaLiteral) { name, lambda ->
            FoxCall(FoxEntityStatement(FoxUnit), name, null, listOf(null to lambda))
        }
        symbols(PostfixExpression, Dot, Identifier, ActualGenericParameterList, ActualParameterList) { target, _, name, generics, parameters ->
            FoxCall(target, name, generics, parameters)
        }
        symbols(PostfixExpression, Dot, Identifier, ActualGenericParameterList, ActualParameterList, LambdaLiteral) { target, _, name, generics, parameters, lambda ->
            FoxCall(target, name, generics, parameters + listOf(null to lambda))
        }
        symbols(PostfixExpression, Dot, Identifier, ActualGenericParameterList, LambdaLiteral) { target, _, name, generics, lambda ->
            FoxCall(target, name, generics, listOf(null to lambda))
        }
        symbols(PostfixExpression, Dot, Identifier, ActualParameterList) { target, _, name, parameters ->
            FoxCall(target, name, null, parameters)
        }
        symbols(PostfixExpression, Dot, Identifier, ActualParameterList, LambdaLiteral) { target, _, name, parameters, lambda ->
            FoxCall(target, name, null, parameters + listOf(null to lambda))
        }
        symbols(PostfixExpression, Dot, Identifier, LambdaLiteral) { target, _, name, lambda ->
            FoxCall(target, name, null, listOf(null to lambda))
        }
        symbols(Type, ActualParameterList) { type, parameters ->
            FoxConstruct(type, parameters)
        }
        symbols(Type, ActualParameterList, LambdaLiteral) { type, parameters, lambda ->
            FoxConstruct(type, parameters + listOf(null to lambda))
        }
        symbols(Type, LambdaLiteral) { type, lambda ->
            FoxConstruct(type, listOf(null to lambda))
        }
        symbols(ParenthesizedStatement, ActualParameterList) { method, parameters ->
            FoxIndirectCall(FoxEntityStatement(FoxUnit), method, parameters)
        }
        symbols(ParenthesizedStatement, ActualParameterList, LambdaLiteral) { method, parameters, lambda ->
            FoxIndirectCall(FoxEntityStatement(FoxUnit), method, parameters + listOf(null to lambda))
        }
        symbols(ParenthesizedStatement, LambdaLiteral) { method, lambda ->
            FoxIndirectCall(FoxEntityStatement(FoxUnit), method, listOf(null to lambda))
        }
        symbols(PostfixExpression, Dot, ParenthesizedStatement, ActualParameterList) { target, _, method, parameters ->
            FoxIndirectCall(target, method, parameters)
        }
        symbols(PostfixExpression, Dot, ParenthesizedStatement, ActualParameterList, LambdaLiteral) { target, _, method, parameters, lambda ->
            FoxIndirectCall(target, method, parameters + listOf(null to lambda))
        }
        symbols(PostfixExpression, Dot, ParenthesizedStatement, LambdaLiteral) { target, _, method, lambda ->
            FoxIndirectCall(target, method, listOf(null to lambda))
        }
    }
    
    rules(UnaryExpression) {
        symbols(PostfixExpression) { it }
        symbols(UnaryOperator, UnaryExpression) { operator, node ->
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
        symbols(AssignableExpression, AssignOperator, Statement) { left, operator, right ->
            FoxAssign(left, operator, right, beforeEvaluation = true)
        }
    }
    
    rules(StatementLine) {
        symbols(Statement, LineBreak) { statement, _ -> statement }
        symbols(SingleLineStatementBlock, LineBreak) { statement, _ -> statement }
    }
    
    rules(StatementBlockHead) {
        symbols(BraceOpen, StatementLine) { _, it -> listOf(it) }
        symbols(StatementBlockHead, StatementLine) { head, it -> head + it }
    }
    rules(StatementBlockCore) {
        symbols(LBrace, BraceClose) { _, _ -> emptyList() }
        symbols(StatementBlockHead, BraceClose) { head, _ -> head }
    }
    
    rules(Statement) {
        symbols(AssignmentExpression) { it }
        symbols(Identifier, Colon, Type) { name, _, type -> FoxTypeBinding(name, type) }
        symbols(KwBreak) { FoxBreak(null) }
        symbols(KwBreak, Label) { _, label -> FoxBreak(label) }
        symbols(KwContinue) { FoxContinue(null) }
        symbols(KwContinue, Label) { _, label -> FoxContinue(label) }
        symbols(KwReturn) { FoxReturn(null) }
        symbols(KwReturn, Statement) { _, value -> FoxReturn(value) }
        symbols(KwYield, Statement) { _, value -> FoxYield(null, value) }
        symbols(KwYield, Label, Statement) { _, label, value -> FoxYield(label, value) }
        symbols(StatementBlock) { it }
        symbols(IfCore) { core -> FoxIf(null, core.condition, core.thenBody, core.elseBody) }
        symbols(WhileCore) { core -> FoxWhile(null, core.condition, core.body) }
        symbols(DoWhileCore) { core -> FoxDoWhile(null, core.body, core.condition) }
        symbols(WhenCore) { core -> FoxWhen(null, core.value, core.cases) }
        symbols(Label, IfCore) { label, core -> FoxIf(label, core.condition, core.thenBody, core.elseBody) }
        symbols(Label, WhileCore) { label, core -> FoxWhile(label, core.condition, core.body) }
        symbols(Label, DoWhileCore) { label, core -> FoxDoWhile(label, core.body, core.condition) }
        symbols(Label, WhenCore) { label, core -> FoxWhen(label, core.value, core.cases) }
    }
    
    rules(StatementBlock) {
        symbols(StatementBlockCore) { FoxBlock(null, it) }
        symbols(Label, StatementBlockCore) { label, it -> FoxBlock(label, it) }
    }
    
    rules(ControlBody) {
        symbols(Statement) { it }
        symbols(LineBreak, Statement) { _, it -> it }
    }
    
    rules(IfCore) {
        symbols(
            KwIf,
            ParenthesizedStatement,
            ControlBody,
        ) { _, condition, body -> FoxIf(null, condition, body, null) }
        symbols(
            KwIf,
            ParenthesizedStatement,
            ControlBody,
            KwElse,
            ControlBody,
        ) { _, condition, thenBody, _, elseBody -> FoxIf(null, condition, thenBody, elseBody) }
    }
    
    rules(WhileCore) {
        symbols(
            KwWhile,
            ParenthesizedStatement,
            ControlBody,
        ) { _, condition, body -> FoxWhile(null, condition, body) }
    }
    
    rules(DoWhileCore) {
        symbols(
            KwDo,
            ControlBody,
            KwWhile,
            ParenthesizedStatement,
        ) { _, body, _, condition -> FoxDoWhile(null, body, condition) }
    }
    
    rules(WhenCaseConditionListHead) {
        symbols(Statement) { listOf(it) }
        symbols(WhenCaseConditionListHead, Comma, Statement) { head, _, it -> head + it }
    }
    rules(WhenCaseConditionList) {
        symbols(WhenCaseConditionListHead, Arrow) { it, _ -> it }
    }
    
    rules(WhenCase) {
        symbols(
            WhenCaseConditionList,
            ControlBody,
        ) { conditions, body -> FoxCase(conditions, body) }
        symbols(
            KwElse,
            Arrow,
            ControlBody,
        ) { _, _, body -> FoxCase(emptyList(), body) }
    }
    
    rules(WhenCaseLine) {
        symbols(WhenCase, LineBreak) { case, _ -> case }
    }
    
    rules(WhenCaseListHead) {
        symbols(WhenCaseLine) { listOf(it) }
        symbols(WhenCaseListHead, WhenCaseLine) { head, it -> head + it }
    }
    rules(WhenCaseList) {
        symbols(LBrace, BraceClose) { _, _ -> emptyList() }
        symbols(BraceOpen, WhenCaseListHead, BraceClose) { _, it, _ -> it }
    }
    
    rules(WhenCore) {
        symbols(
            KwWhen,
            WhenCaseList,
        ) { _, cases -> FoxWhen(null, null, cases) }
        symbols(
            KwWhen,
            ParenthesizedStatement,
            WhenCaseList,
        ) { _, value, cases -> FoxWhen(null, value, cases) }
    }
    
    rules(TypeAlias) {
        symbols(
            KwType,
            TypeName,
            FormalGenericParameterNoConstraintsList,
            Assign,
            Type,
        ) { _, name, generics, _, type -> FoxTypeAlias(name, generics, type) }
        symbols(
            KwType,
            TypeName,
            Assign,
            Type,
        ) { _, name, _, type -> FoxTypeAlias(name, emptyOrderedSet(), type) }
    }
    
    rules(ThisTypeQualifier) { symbols(Type, Dot) { type, _ -> type } }
    rules(ReturnTypeClause) { symbols(Colon, Type) { _, type -> type } }
    
    rules(MethodHead) {
        symbols(
            KwDef,
            FormalGenericParameterList,
            ThisTypeQualifier,
            Identifier,
            FormalParameterList,
        ) { _, generics, thisType, name, parameters ->
            FoxMethodDefinition(generics, thisType, name, parameters, FoxUnitType, FoxEntityStatement(FoxUnit))
        }
        symbols(
            KwDef,
            FormalGenericParameterList,
            Identifier,
            FormalParameterList,
        ) { _, generics, name, parameters ->
            FoxMethodDefinition(generics, FoxUnitType, name, parameters, FoxUnitType, FoxEntityStatement(FoxUnit))
        }
        symbols(
            KwDef,
            ThisTypeQualifier,
            Identifier,
            FormalParameterList,
        ) { _, thisType, name, parameters ->
            FoxMethodDefinition(emptyOrderedMap(), thisType, name, parameters, FoxUnitType, FoxEntityStatement(FoxUnit))
        }
        symbols(
            KwDef,
            Identifier,
            FormalParameterList,
        ) { _, name, parameters ->
            FoxMethodDefinition(emptyOrderedMap(), FoxUnitType, name, parameters, FoxUnitType, FoxEntityStatement(FoxUnit))
        }
    }
    
    rules(MethodDefinition) {
        symbols(
            MethodHead,
            ReturnTypeClause,
            StatementBlock,
        ) { head, returnType, body -> FoxMethodDefinition(head.generics, head.thisType, head.name, head.parameters, returnType, body) }
        symbols(
            MethodHead,
            StatementBlock,
        ) { head, body -> FoxMethodDefinition(head.generics, head.thisType, head.name, head.parameters, FoxUnitType, body) }
    }
    
    rules(FileElement) {
        symbols(TypeAlias) { it }
        symbols(MethodDefinition) { it }
    }
    
    rules(FileElementLine) {
        symbols(FileElement, LineBreak) { element, _ -> element }
    }
    
    rules(FileElementList) {
        symbols(FileElementLine) { listOf(it) }
        symbols(FileElementList, FileElementLine) { head, it -> head + it }
    }
    rules(File) {
        symbols(FileElementList) { FoxFile(it) }
    }
}

internal sealed interface ParsedMethodTypeArgument {
    data class This(val type: FoxType) : ParsedMethodTypeArgument
    data class Return(val type: FoxType) : ParsedMethodTypeArgument
    data class Parameter(val name: String, val type: FoxType) : ParsedMethodTypeArgument
    data class AnonymousType(val type: FoxType) : ParsedMethodTypeArgument
}

private fun List<String>.toSet(itemName: String): GrammarRuleFactoryResult<Set<String>> {
    val result = LinkedHashSet<String>()
    forEach { name ->
        if (name in result) return factoryError("Duplicate $itemName name '$name'")
        result += name
    }
    return factorySuccess(result)
}

private fun <V : Any> List<Pair<String, V>>.toMap(itemName: String): GrammarRuleFactoryResult<Map<String, V>> {
    val result = LinkedHashMap<String, V>()
    forEach { (name, value) ->
        if (name in result) return factoryError("Duplicate $itemName name '$name'")
        result[name] = value
    }
    return factorySuccess(result)
}

private fun List<String>.toOrderedSet(itemName: String): GrammarRuleFactoryResult<OrderedSet<String>> {
    val result = mutableOrderedSetOf<String>()
    forEach { name ->
        if (name in result) return factoryError("Duplicate $itemName name '$name'")
        result += name
    }
    return factorySuccess(result)
}

private fun <V : Any> List<Pair<String, V>>.toOrderedMap(itemName: String): GrammarRuleFactoryResult<OrderedMap<String, V>> {
    val result = mutableOrderedMapOf<String, V>()
    forEach { (name, value) ->
        if (name in result) return factoryError("Duplicate $itemName name '$name'")
        result[name] = value
    }
    return factorySuccess(result)
}

private fun List<ParsedMethodTypeArgument>.toFoxMethodType(): GrammarRuleFactoryResult<FoxMethodType> {
    if (count { it is ParsedMethodTypeArgument.This } > 1) {
        return factoryError("Method type cannot declare more than one 'this' type")
    }
    if (count { it is ParsedMethodTypeArgument.Return } > 1) {
        return factoryError("Method type cannot declare more than one 'return' type")
    }
    forEachIndexed { index, item ->
        if (item is ParsedMethodTypeArgument.This && index != 0) {
            return factoryError("Method type 'this' must be the first item")
        }
        if (item is ParsedMethodTypeArgument.Return && index != lastIndex) {
            return factoryError("Method type 'return' must be the last item")
        }
    }
    if (size == 1 && firstOrNull() is ParsedMethodTypeArgument.AnonymousType) {
        return factoryError("Method<T> is ambiguous; use 'this: T' or 'return: T' explicitly")
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
                    return factoryError("Duplicate method type parameter name '${item.name}'")
                }
                parameters[item.name] = item.type
            }
            is ParsedMethodTypeArgument.This ->
                return factoryError("Method type 'this' must be the first item")
            is ParsedMethodTypeArgument.Return ->
                return factoryError("Method type 'return' must be the last item")
            is ParsedMethodTypeArgument.AnonymousType ->
                return factoryError("Anonymous method type items may only appear as leading 'this' or trailing 'return'")
        }
    }
    return factorySuccess(FoxMethodType(thisType, parameters, returnType))
}
