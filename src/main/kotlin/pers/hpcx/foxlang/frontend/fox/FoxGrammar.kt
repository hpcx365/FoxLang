package pers.hpcx.foxlang.frontend.fox

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.frontend.common.buildGrammar
import pers.hpcx.foxlang.frontend.fox.FoxAssignOperatorSymbol.PlainAssign
import pers.hpcx.foxlang.frontend.fox.FoxBinaryOperatorCategorySymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxBracketSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxExpressionSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxFormattedStringPartSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxKeywordSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxLexicalSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxLiteralSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxSyntheticTypeKeywordSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxTerminalSymbol.*
import pers.hpcx.foxlang.runtime.FoxBool
import pers.hpcx.foxlang.runtime.FoxChar
import pers.hpcx.foxlang.runtime.FoxString
import pers.hpcx.foxlang.runtime.FoxUnit
import pers.hpcx.foxlang.utils.AutoRegex.Companion.literals
import pers.hpcx.foxlang.utils.AutoRegex.Companion.pattern

internal val KeywordRegex = literals(FoxKeywordsByText.keys)
internal val IdentifierRegex = pattern("[a-z][a-zA-Z0-9_]*") - KeywordRegex
internal val TypeNameRegex = pattern("[A-Z][a-zA-Z0-9_]*") - KeywordRegex

internal val BinIntRegex = pattern("0b[01]+(_[01]+)*")
internal val DecIntRegex = pattern("(0|[1-9][0-9]*(_[0-9]+)*)")
internal val HexIntRegex = pattern("0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*")
internal val BinLongRegex = pattern("0b[01]+(_[01]+)*L")
internal val DecLongRegex = DecIntRegex / pattern("L")
internal val HexLongRegex = pattern("0x[0-9a-fA-F]+(_[0-9a-fA-F]+)*L")
internal val DecFloatRegex = DecIntRegex / pattern("(\\.[0-9]+(_[0-9]+)*)?(e[+-]?[0-9]+)?f")
internal val DecDoubleRegex = DecIntRegex / pattern("(\\.[0-9]+(_[0-9]+)*)?(e[+-]?[0-9]+)?") - DecIntRegex

private fun checkRegexDisjoint() {
    val regexList = listOf(
        KeywordRegex,
        IdentifierRegex,
        TypeNameRegex,
        BinIntRegex,
        DecIntRegex,
        HexIntRegex,
        BinLongRegex,
        DecLongRegex,
        HexLongRegex,
        DecFloatRegex,
        DecDoubleRegex,
    )
    regexList.forEachIndexed { index, regex ->
        (index + 1..<regexList.size).map { regexList[it] }.forEach { other ->
            val intersection = regex * other
            if (intersection.isNotEmpty()) {
                error("Regex $regex and $other can match the same string '${intersection.shortestExample()}'")
            }
        }
    }
}

val FoxGrammar = buildGrammar {
    checkRegexDisjoint()
    
    sequence {
        yieldAll(FoxTerminalSymbol.entries.map { it.text to it })
        yieldAll(FoxUnaryOperatorSymbol.entries.map { it.text to it })
        yieldAll(FoxBinaryOperatorSymbol.entries.map { it.text to it })
        yieldAll(FoxAssignOperatorSymbol.entries.map { it.text to it })
        yieldAll(FoxKeywordsByText.entries.map { it.key to it.value })
    }.forEach { (token, symbol) ->
        rules(symbol) {
            fixed(token) { span -> ParsedUnit(span) }
        }
    }
    
    rules(LineBreak) { lineBreak { span -> ParsedUnit(span) } }
    rules(FormattedStringStart) { formattedStringStart { span -> ParsedUnit(span) } }
    rules(FormattedStringEnd) { formattedStringEnd { span -> ParsedUnit(span) } }
    rules(FormattedExpressionOpen) { formattedExpressionStart { span -> ParsedUnit(span) } }
    rules(FormattedExpressionClose) { formattedExpressionEnd { span -> ParsedUnit(span) } }
    
    rules(Dot) {
        symbols(LineBreak, Dot) { _, it -> it }
    }
    rules(Comma) {
        symbols(Comma, LineBreak) { it, _ -> it }
    }
    rules(ParenOpen) {
        symbols(LParen) { it }
        symbols(LParen, LineBreak) { it, _ -> it }
    }
    rules(ParenClose) {
        symbols(RParen) { it }
        symbols(LineBreak, RParen) { _, it -> it }
    }
    rules(SquareOpen) {
        symbols(LBracket) { it }
        symbols(LBracket, LineBreak) { it, _ -> it }
    }
    rules(SquareClose) {
        symbols(RBracket) { it }
        symbols(LineBreak, RBracket) { _, it -> it }
    }
    rules(BraceOpen) {
        symbols(LBrace) { it }
        symbols(LBrace, LineBreak) { it, _ -> it }
    }
    rules(BraceClose) {
        symbols(RBrace) { it }
        symbols(LineBreak, RBrace) { _, it -> it }
    }
    rules(AngleOpen) {
        symbols(LAngle) { it }
        symbols(LAngle, LineBreak) { it, _ -> it }
    }
    rules(AngleClose) {
        symbols(RAngle) { it }
        symbols(LineBreak, RAngle) { _, it -> it }
    }
    
    rules(UnaryOperator) {
        FoxUnaryOperatorSymbol.entries.forEach { symbol ->
            symbols(symbol) { ParsedFoxUnaryOperator(symbol.operator, it.span) }
        }
    }
    
    FoxBinaryOperatorSymbol.entries.forEach { symbol ->
        rules(symbol.category) {
            symbols(symbol) { ParsedFoxBinaryOperator(symbol.operator, it.span) }
        }
    }
    rules(BinaryOperator) {
        FoxBinaryOperatorCategorySymbol.entries.forEach { symbol ->
            symbols(symbol) { it }
        }
    }
    
    rules(AssignOperator) {
        FoxAssignOperatorSymbol.entries.forEach { symbol ->
            symbols(symbol) { ParsedFoxAssignOperator(symbol.operator, it.span) }
        }
    }
    
    rules(Identifier) { regex(IdentifierRegex) { it, span -> ParsedString(it, span) } }
    rules(TypeName) { regex(TypeNameRegex) { it, span -> ParsedString(it, span) } }
    
    rules(LitUnit) {
        symbols(KwLowerUnit) { it }
    }
    rules(LitBool) {
        symbols(KwTrue) { ParsedBoolean(true, it.span) }
        symbols(KwFalse) { ParsedBoolean(false, it.span) }
    }
    rules(LitInt) {
        regex(BinIntRegex) { it, span -> ParsedInt(2, it.drop(2).replace("_", ""), span) }
        regex(DecIntRegex) { it, span -> ParsedInt(10, it.replace("_", ""), span) }
        regex(HexIntRegex) { it, span -> ParsedInt(16, it.drop(2).replace("_", ""), span) }
    }
    rules(LitLong) {
        regex(BinLongRegex) { it, span -> ParsedLong(2, it.drop(2).dropLast(1).replace("_", ""), span) }
        regex(DecLongRegex) { it, span -> ParsedLong(10, it.dropLast(1).replace("_", ""), span) }
        regex(HexLongRegex) { it, span -> ParsedLong(16, it.drop(2).dropLast(1).replace("_", ""), span) }
    }
    rules(LitFloat) {
        regex(DecFloatRegex) { it, span -> ParsedFloat(it.dropLast(1).replace("_", ""), span) }
    }
    rules(LitDouble) {
        regex(DecDoubleRegex) { it, span -> ParsedDouble(it.replace("_", ""), span) }
    }
    rules(LitChar) { charLiteral { it, span -> ParsedChar(it, span) } }
    rules(LitString) { stringLiteral { it, span -> ParsedString(it, span) } }
    
    rules(Type) {
        FoxPrimitiveTypeKeywordSymbol.entries.forEach { symbol ->
            symbols(symbol) { ParsedFoxPrimitiveType(symbol.type, it.span) }
        }
        
        symbols(KwTuple, TupleComponentParameterList) { kw, components ->
            ParsedFoxTupleType(components, mergeSpan(kw, components))
        }
        symbols(KwStruct, StructFieldParameterList) { kw, fields ->
            ParsedFoxStructType(fields, mergeSpan(kw, fields))
        }
        symbols(KwObject, ObjectMemberParameterList) { kw, members ->
            ParsedFoxObjectType(members, mergeSpan(kw, members))
        }
        symbols(KwEnum, EnumEntryParameterList) { kw, entries ->
            ParsedFoxEnumType(entries, mergeSpan(kw, entries))
        }
        symbols(KwArray, AnonymousActualGenericParameterSingleList) { kw, type ->
            ParsedFoxArrayType(type.node.single(), mergeSpan(kw, type))
        }
        symbols(KwRef, AnonymousActualGenericParameterSingleList) { kw, type ->
            ParsedFoxRefType(type.node.single(), mergeSpan(kw, type))
        }
        symbols(KwMethod, MethodTypeParameterList) { kw, types ->
            types.copy(span = mergeSpan(kw, types))
        }
        
        symbols(KwAny) {
            ParsedFoxAnyType(it.span)
        }
        symbols(KwAnyOf, AnonymousActualGenericParameterList) { kw, types ->
            ParsedFoxAnyOfType(types.node, mergeSpan(kw, types))
        }
        symbols(KwAllOf, AnonymousActualGenericParameterList) { kw, types ->
            ParsedFoxAllOfType(types.node, mergeSpan(kw, types))
        }
        symbols(KwNoneOf, AnonymousActualGenericParameterList) { kw, types ->
            ParsedFoxNoneOfType(types.node, mergeSpan(kw, types))
        }
        symbols(KwAnyTuple) { kw ->
            ParsedFoxAnyTupleType(kw.span)
        }
        symbols(KwAnyTupleOf, AnonymousActualGenericParameterSingleList) { kw, type ->
            ParsedFoxAnyTupleOfType(type.node.single(), mergeSpan(kw, type))
        }
        symbols(KwAnyStruct) { kw ->
            ParsedFoxAnyStructType(kw.span)
        }
        symbols(KwAnyStructOf, AnonymousActualGenericParameterList) { kw, types ->
            ParsedFoxAnyStructOfType(types, mergeSpan(kw, types))
        }
        symbols(KwAnyObject) { kw ->
            ParsedFoxAnyObjectType(kw.span)
        }
        symbols(KwAnyEnum) { kw ->
            ParsedFoxAnyEnumType(kw.span)
        }
        
        symbols(KwGetComponent, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxTupleGetComponentType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwGetComponentBack, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxTupleGetComponentBackType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwGetFirstComponents, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxTupleGetFirstComponentsType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwGetFirstComponentsExact, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxTupleGetFirstComponentsExactType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwGetLastComponents, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxTupleGetLastComponentsType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwGetLastComponentsExact, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxTupleGetLastComponentsExactType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwDropFirstComponents, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxTupleDropFirstComponentsType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwDropFirstComponentsExact, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxTupleDropFirstComponentsExactType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwDropLastComponents, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxTupleDropLastComponentsType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwDropLastComponentsExact, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxTupleDropLastComponentsExactType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwMergeTuples, AnonymousActualGenericParameterList) { kw, types ->
            ParsedFoxTupleMergeTuplesType(types, mergeSpan(kw, types))
        }
        
        symbols(
            KwGetFieldTypeByName, AngleOpen, Type, Comma, Identifier, AngleClose,
        ) { kw, open, type, comma, name, close ->
            ParsedFoxStructGetFieldTypeByNameType(type, name, mergeSpan(kw, open, type, comma, name, close))
        }
        symbols(KwGetFieldTypeByIndex, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxStructGetFieldTypeByIndexType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwGetFieldTypeByIndexBack, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxStructGetFieldTypeByIndexBackType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwGetFirstFields, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxStructGetFirstFieldsType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwGetFirstFieldsExact, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxStructGetFirstFieldsExactType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwGetLastFields, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxStructGetLastFieldsType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwGetLastFieldsExact, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxStructGetLastFieldsExactType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwDropFirstFields, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxStructDropFirstFieldsType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwDropFirstFieldsExact, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxStructDropFirstFieldsExactType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwDropLastFields, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxStructDropLastFieldsType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(KwDropLastFieldsExact, ActualGenericIntParameterList) { kw, it ->
            ParsedFoxStructDropLastFieldsExactType(it.node.first, it.node.second, mergeSpan(kw, it))
        }
        symbols(
            KwSelectFields, AngleOpen, Type, Comma, StructFieldNameList, AngleClose,
        ) { kw, open, type, comma, names, close ->
            ParsedFoxStructSelectFieldsType(type, names, mergeSpan(kw, open, type, comma, names, close))
        }
        symbols(
            KwSelectFieldsExact, AngleOpen, Type, Comma, StructFieldNameList, AngleClose,
        ) { kw, open, type, comma, names, close ->
            ParsedFoxStructSelectFieldsExactType(type, names, mergeSpan(kw, open, type, comma, names, close))
        }
        symbols(
            KwDropFields, AngleOpen, Type, Comma, StructFieldNameList, AngleClose,
        ) { kw, open, type, comma, names, close ->
            ParsedFoxStructDropFieldsType(type, names, mergeSpan(kw, open, type, comma, names, close))
        }
        symbols(
            KwDropFieldsExact, AngleOpen, Type, Comma, StructFieldNameList, AngleClose,
        ) { kw, open, type, comma, names, close ->
            ParsedFoxStructDropFieldsExactType(type, names, mergeSpan(kw, open, type, comma, names, close))
        }
        symbols(
            KwExtractFieldTypes, AngleOpen, Type, AngleClose,
        ) { kw, open, type, close ->
            ParsedFoxStructExtractFieldTypesType(type, mergeSpan(kw, open, type, close))
        }
        symbols(KwMergeStructs, AnonymousActualGenericParameterList) { kw, types ->
            ParsedFoxStructMergeStructsType(types, mergeSpan(kw, types))
        }
        
        symbols(
            KwGetMemberType, AngleOpen, Type, Comma, Identifier, AngleClose,
        ) { kw, open, type, comma, name, close ->
            ParsedFoxObjectGetMemberTypeType(type, name, mergeSpan(kw, open, type, comma, name, close))
        }
        symbols(
            KwSelectMembers, AngleOpen, Type, Comma, ObjectMemberNameList, AngleClose,
        ) { kw, open, type, comma, names, close ->
            ParsedFoxObjectSelectMembersType(type, names, mergeSpan(kw, open, type, comma, names, close))
        }
        symbols(
            KwSelectMembersExact, AngleOpen, Type, Comma, ObjectMemberNameList, AngleClose,
        ) { kw, open, type, comma, names, close ->
            ParsedFoxObjectSelectMembersExactType(type, names, mergeSpan(kw, open, type, comma, names, close))
        }
        symbols(
            KwDropMembers, AngleOpen, Type, Comma, ObjectMemberNameList, AngleClose,
        ) { kw, open, type, comma, names, close ->
            ParsedFoxObjectDropMembersType(type, names, mergeSpan(kw, open, type, comma, names, close))
        }
        symbols(
            KwDropMembersExact, AngleOpen, Type, Comma, ObjectMemberNameList, AngleClose,
        ) { kw, open, type, comma, names, close ->
            ParsedFoxObjectDropMembersExactType(type, names, mergeSpan(kw, open, type, comma, names, close))
        }
        symbols(KwMergeObjects, AnonymousActualGenericParameterList) { kw, types ->
            ParsedFoxObjectMergeObjectsType(types, mergeSpan(kw, types))
        }
        
        symbols(
            KwGetEntryType, AngleOpen, Type, Comma, TypeName, AngleClose,
        ) { kw, open, type, comma, name, close ->
            ParsedFoxEnumGetEntryTypeType(type, name, mergeSpan(kw, open, type, comma, name, close))
        }
        symbols(
            KwSelectEntries, AngleOpen, Type, Comma, EnumEntryNameList, AngleClose,
        ) { kw, open, type, comma, names, close ->
            ParsedFoxEnumSelectEntriesType(type, names, mergeSpan(kw, open, type, comma, names, close))
        }
        symbols(
            KwSelectEntriesExact, AngleOpen, Type, Comma, EnumEntryNameList, AngleClose,
        ) { kw, open, type, comma, names, close ->
            ParsedFoxEnumSelectEntriesExactType(type, names, mergeSpan(kw, open, type, comma, names, close))
        }
        symbols(
            KwDropEntries, AngleOpen, Type, Comma, EnumEntryNameList, AngleClose,
        ) { kw, open, type, comma, names, close ->
            ParsedFoxEnumDropEntriesType(type, names, mergeSpan(kw, open, type, comma, names, close))
        }
        symbols(
            KwDropEntriesExact, AngleOpen, Type, Comma, EnumEntryNameList, AngleClose,
        ) { kw, open, type, comma, names, close ->
            ParsedFoxEnumDropEntriesExactType(type, names, mergeSpan(kw, open, type, comma, names, close))
        }
        symbols(KwMergeEnums, AnonymousActualGenericParameterList) { kw, it ->
            ParsedFoxEnumMergeEnumsType(it, mergeSpan(kw, it))
        }
        
        symbols(
            KwGetElementType, AngleOpen, Type, AngleClose,
        ) { kw, open, type, close ->
            ParsedFoxArrayGetElementTypeType(type, mergeSpan(kw, open, type, close))
        }
        
        symbols(
            KwGetReferentType, AngleOpen, Type, AngleClose,
        ) { kw, open, type, close ->
            ParsedFoxRefGetReferentTypeType(type, mergeSpan(kw, open, type, close))
        }
        
        symbols(
            KwGetThisType, AngleOpen, Type, AngleClose,
        ) { kw, open, type, close ->
            ParsedFoxMethodGetThisTypeType(type, mergeSpan(kw, open, type, close))
        }
        symbols(
            KwGetParameterStruct, AngleOpen, Type, AngleClose,
        ) { kw, open, type, close ->
            ParsedFoxMethodGetParameterStructType(type, mergeSpan(kw, open, type, close))
        }
        symbols(
            KwGetReturnType, AngleOpen, Type, AngleClose,
        ) { kw, open, type, close ->
            ParsedFoxMethodGetReturnTypeType(type, mergeSpan(kw, open, type, close))
        }
        symbols(
            KwMethodOf, AngleOpen, Type, Comma, Type, Comma, Type, AngleClose,
        ) { kw, open, `this`, comma0, parameters, comma1, `return`, close ->
            ParsedFoxMethodOfType(
                `this`, parameters, `return`,
                mergeSpan(kw, open, `this`, comma0, parameters, comma1, `return`, close),
            )
        }
        
        symbols(TypeName) {
            ParsedFoxUnresolvedType(it, null, it.span)
        }
        symbols(TypeName, AnonymousActualGenericParameterList) { name, types ->
            ParsedFoxUnresolvedType(name, types, mergeSpan(name, types))
        }
    }
    
    rules(FormalParameter) {
        symbols(
            Identifier, Colon, Type,
        ) { name, colon, type ->
            ParsedPair(name to type, mergeSpan(name, colon, type))
        }
    }
    rules(FormalParameterListHead) {
        symbols(ParenOpen, FormalParameter) { open, it ->
            ParsedList(listOf(it), mergeSpan(open, it))
        }
        symbols(
            FormalParameterListHead, Comma, FormalParameter,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(FormalParameterList) {
        symbols(LParen, ParenClose) { open, close ->
            ParsedList(emptyList(), mergeSpan(open, close))
        }
        symbols(FormalParameterListHead, ParenClose) { head, close ->
            ParsedList(head.node, mergeSpan(head, close))
        }
        symbols(
            FormalParameterListHead, Comma, RParen,
        ) { head, comma, close ->
            ParsedList(head.node, mergeSpan(head, comma, close))
        }
    }
    
    rules(ActualParameter) {
        symbols(LogicalOrExpression) {
            ParsedPair(null to it, it.span)
        }
        symbols(ImplicitLambdaLiteral) {
            ParsedPair(null to it, it.span)
        }
        symbols(
            Identifier, PlainAssign, LogicalOrExpression,
        ) { name, assign, expr ->
            ParsedPair(name to expr, mergeSpan(name, assign, expr))
        }
        symbols(
            Identifier, PlainAssign, ImplicitLambdaLiteral,
        ) { name, assign, expr ->
            ParsedPair(name to expr, mergeSpan(name, assign, expr))
        }
    }
    rules(ActualParameterListHead) {
        symbols(ParenOpen, ActualParameter) { open, it ->
            ParsedList(listOf(it), mergeSpan(open, it))
        }
        symbols(
            ActualParameterListHead, Comma, ActualParameter,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(ActualParameterList) {
        symbols(LParen, ParenClose) { open, close ->
            ParsedList(emptyList(), mergeSpan(open, close))
        }
        symbols(ActualParameterListHead, ParenClose) { head, close ->
            ParsedList(head.node, mergeSpan(head, close))
        }
        symbols(
            ActualParameterListHead, Comma, RParen,
        ) { head, comma, close ->
            ParsedList(head.node, mergeSpan(head, comma, close))
        }
    }
    
    rules(FormalGenericParameter) {
        symbols(TypeName) {
            ParsedPair(it to null, it.span)
        }
        symbols(
            TypeName, PlainAssign, Type,
        ) { name, assign, type ->
            ParsedPair(name to type, mergeSpan(name, assign, type))
        }
    }
    rules(FormalGenericParameterListHead) {
        symbols(AngleOpen, FormalGenericParameter) { open, it ->
            ParsedList(listOf(it), mergeSpan(open, it))
        }
        symbols(
            FormalGenericParameterListHead, Comma, FormalGenericParameter,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(FormalGenericParameterList) {
        symbols(LAngle, AngleClose) { open, close ->
            ParsedList(emptyList(), mergeSpan(open, close))
        }
        symbols(FormalGenericParameterListHead, AngleClose) { head, close ->
            ParsedList(head.node, mergeSpan(head, close))
        }
        symbols(
            FormalGenericParameterListHead, Comma, RAngle,
        ) { head, comma, close ->
            ParsedList(head.node, mergeSpan(head, comma, close))
        }
    }
    
    rules(FormalGenericParameterNoConstraints) {
        symbols(TypeName) { it }
    }
    rules(FormalGenericParameterNoConstraintsListHead) {
        symbols(AngleOpen, FormalGenericParameterNoConstraints) { open, it ->
            ParsedList(listOf(it), mergeSpan(open, it))
        }
        symbols(
            FormalGenericParameterNoConstraintsListHead, Comma, FormalGenericParameterNoConstraints,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(FormalGenericParameterNoConstraintsList) {
        symbols(LAngle, AngleClose) { open, close ->
            ParsedList(emptyList(), mergeSpan(open, close))
        }
        symbols(FormalGenericParameterNoConstraintsListHead, AngleClose) { head, close ->
            ParsedList(head.node, mergeSpan(head, close))
        }
        symbols(
            FormalGenericParameterNoConstraintsListHead, Comma, RAngle,
        ) { head, comma, close ->
            ParsedList(head.node, mergeSpan(head, comma, close))
        }
    }
    
    rules(ActualGenericParameter) {
        symbols(Type) {
            ParsedPair(null to it, it.span)
        }
        symbols(
            TypeName, PlainAssign, Type,
        ) { name, assign, type ->
            ParsedPair(name to type, mergeSpan(name, assign, type))
        }
    }
    rules(ActualGenericParameterListHead) {
        symbols(AngleOpen, ActualGenericParameter) { open, it ->
            ParsedList(listOf(it), mergeSpan(open, it))
        }
        symbols(
            ActualGenericParameterListHead, Comma, ActualGenericParameter,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(ActualGenericParameterList) {
        symbols(LAngle, AngleClose) { open, close ->
            ParsedList(emptyList(), mergeSpan(open, close))
        }
        symbols(ActualGenericParameterListHead, AngleClose) { head, close ->
            ParsedList(head.node, mergeSpan(head, close))
        }
        symbols(
            ActualGenericParameterListHead, Comma, RAngle,
        ) { head, comma, close ->
            ParsedList(head.node, mergeSpan(head, comma, close))
        }
    }
    
    rules(AnonymousActualGenericParameter) {
        symbols(Type) { it }
    }
    rules(AnonymousActualGenericParameterListHead) {
        symbols(AngleOpen, AnonymousActualGenericParameter) { open, it ->
            ParsedList(listOf(it), mergeSpan(open, it))
        }
        symbols(
            AnonymousActualGenericParameterListHead, Comma, AnonymousActualGenericParameter,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(AnonymousActualGenericParameterList) {
        symbols(LAngle, AngleClose) { open, close ->
            ParsedList(emptyList(), mergeSpan(open, close))
        }
        symbols(AnonymousActualGenericParameterListHead, AngleClose) { head, close ->
            ParsedList(head.node, mergeSpan(head, close))
        }
        symbols(
            AnonymousActualGenericParameterListHead, Comma, RAngle,
        ) { head, comma, close ->
            ParsedList(head.node, mergeSpan(head, comma, close))
        }
    }
    rules(AnonymousActualGenericParameterSingleList) {
        symbols(
            AngleOpen, AnonymousActualGenericParameter, AngleClose,
        ) { open, it, close ->
            ParsedList(listOf(it), mergeSpan(open, it, close))
        }
    }
    
    rules(ActualGenericIntParameterList) {
        symbols(
            AngleOpen, AnonymousActualGenericParameter, Comma, LitInt, AngleClose,
        ) { open, it, comma, count, close ->
            ParsedPair(it to count, mergeSpan(open, it, comma, count, close))
        }
    }
    
    rules(LambdaParameter) {
        symbols(Identifier) {
            ParsedPair(it to null, it.span)
        }
        symbols(
            Identifier, Colon, Type,
        ) { name, colon, type ->
            ParsedPair(name to type, mergeSpan(name, colon, type))
        }
    }
    rules(LambdaParameterListHead) {
        symbols(LambdaParameter) {
            ParsedList(listOf(it), it.span)
        }
        symbols(LineBreak, LambdaParameter) { _, it ->
            ParsedList(listOf(it), it.span)
        }
        symbols(
            LambdaParameterListHead, Comma, LambdaParameter,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(LambdaParameterList) {
        symbols(LambdaParameterListHead) { it }
        symbols(LambdaParameterListHead, Comma) { head, comma ->
            ParsedList(head.node, mergeSpan(head, comma))
        }
    }
    
    rules(TupleComponentParameter) {
        symbols(Type) { it }
    }
    rules(TupleComponentParameterListHead) {
        symbols(AngleOpen, TupleComponentParameter) { open, it ->
            ParsedList(listOf(it), mergeSpan(open, it))
        }
        symbols(
            TupleComponentParameterListHead, Comma, TupleComponentParameter,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(TupleComponentParameterList) {
        symbols(LAngle, AngleClose) { open, close ->
            ParsedList(emptyList(), mergeSpan(open, close))
        }
        symbols(TupleComponentParameterListHead, AngleClose) { head, close ->
            ParsedList(head.node, mergeSpan(head, close))
        }
        symbols(
            TupleComponentParameterListHead, Comma, RAngle,
        ) { head, comma, close ->
            ParsedList(head.node, mergeSpan(head, comma, close))
        }
    }
    
    rules(StructFieldParameter) {
        symbols(
            Identifier, Colon, Type,
        ) { name, colon, type ->
            ParsedPair(name to type, mergeSpan(name, colon, type))
        }
    }
    rules(StructFieldParameterListHead) {
        symbols(AngleOpen, StructFieldParameter) { open, it ->
            ParsedList(listOf(it), mergeSpan(open, it))
        }
        symbols(
            StructFieldParameterListHead, Comma, StructFieldParameter,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(StructFieldParameterList) {
        symbols(LAngle, AngleClose) { open, close ->
            ParsedList(emptyList(), mergeSpan(open, close))
        }
        symbols(StructFieldParameterListHead, AngleClose) { head, close ->
            ParsedList(head.node, mergeSpan(head, close))
        }
        symbols(
            StructFieldParameterListHead, Comma, RAngle,
        ) { head, comma, close ->
            ParsedList(head.node, mergeSpan(head, comma, close))
        }
    }
    
    rules(StructFieldName) {
        symbols(Identifier) { it }
    }
    rules(StructFieldNameList) {
        symbols(StructFieldName) {
            ParsedList(listOf(it), it.span)
        }
        symbols(
            StructFieldNameList, Comma, StructFieldName,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    
    rules(ObjectMemberParameter) {
        symbols(
            Identifier, Colon, Type,
        ) { name, colon, type ->
            ParsedPair(name to type, mergeSpan(name, colon, type))
        }
    }
    rules(ObjectMemberParameterListHead) {
        symbols(AngleOpen, ObjectMemberParameter) { open, it ->
            ParsedList(listOf(it), mergeSpan(open, it))
        }
        symbols(
            ObjectMemberParameterListHead, Comma, ObjectMemberParameter,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(ObjectMemberParameterList) {
        symbols(LAngle, AngleClose) { open, close ->
            ParsedList(emptyList(), mergeSpan(open, close))
        }
        symbols(ObjectMemberParameterListHead, AngleClose) { head, close ->
            ParsedList(head.node, mergeSpan(head, close))
        }
        symbols(
            ObjectMemberParameterListHead, Comma, RAngle,
        ) { head, comma, close ->
            ParsedList(head.node, mergeSpan(head, comma, close))
        }
    }
    
    rules(ObjectMemberName) {
        symbols(Identifier) { it }
    }
    rules(ObjectMemberNameList) {
        symbols(ObjectMemberName) {
            ParsedList(listOf(it), it.span)
        }
        symbols(
            ObjectMemberNameList, Comma, ObjectMemberName,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    
    rules(EnumEntryParameter) {
        symbols(
            TypeName, PlainAssign, Type,
        ) { name, assign, type ->
            ParsedPair(name to type, mergeSpan(name, assign, type))
        }
    }
    rules(EnumEntryParameterListHead) {
        symbols(AngleOpen, EnumEntryParameter) { open, it ->
            ParsedList(listOf(it), mergeSpan(open, it))
        }
        symbols(
            EnumEntryParameterListHead, Comma, EnumEntryParameter,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(EnumEntryParameterList) {
        symbols(LAngle, AngleClose) { open, close ->
            ParsedList(emptyList(), mergeSpan(open, close))
        }
        symbols(EnumEntryParameterListHead, AngleClose) { head, close ->
            ParsedList(head.node, mergeSpan(head, close))
        }
        symbols(
            EnumEntryParameterListHead, Comma, RAngle,
        ) { head, comma, close ->
            ParsedList(head.node, mergeSpan(head, comma, close))
        }
    }
    
    rules(EnumEntryName) {
        symbols(TypeName) { it }
    }
    rules(EnumEntryNameList) {
        symbols(EnumEntryName) {
            ParsedList(listOf(it), it.span)
        }
        symbols(
            EnumEntryNameList, Comma, EnumEntryName,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    
    rules(MethodTypeParameter) {
        symbols(
            Identifier, Colon, Type,
        ) { name, colon, type ->
            ParsedPair(name to type, mergeSpan(name, colon, type))
        }
    }
    rules(MethodTypeParameterListHead) {
        symbols(AngleOpen, MethodTypeParameter) { open, it ->
            ParsedFoxMethodTypeHead(null, ParsedList(listOf(it), it.span), mergeSpan(open, it))
        }
        symbols(
            AngleOpen, KwThis, Colon, Type, Comma, MethodTypeParameter,
        ) { open, kw, colon, type, comma, it ->
            ParsedFoxMethodTypeHead(
                ParsedPair(kw to type, mergeSpan(kw, colon, type)),
                ParsedList(listOf(it), it.span),
                mergeSpan(open, kw, colon, type, comma, it),
            )
        }
        symbols(
            MethodTypeParameterListHead, Comma, MethodTypeParameter,
        ) { head, comma, it ->
            ParsedFoxMethodTypeHead(
                head.`this`,
                ParsedList(head.parameters.node + it, mergeSpan(head.parameters, comma, it)),
                mergeSpan(head, comma, it),
            )
        }
    }
    rules(MethodTypeParameterList) {
        symbols(
            LAngle, AngleClose,
        ) { open, close ->
            ParsedFoxMethodType(null, null, null, mergeSpan(open, close))
        }
        symbols(
            AngleOpen, KwReturn, Colon, Type, AngleClose,
        ) { open, kw, colon, type, close ->
            ParsedFoxMethodType(
                null,
                null,
                ParsedPair(kw to type, mergeSpan(kw, colon, type)),
                mergeSpan(open, kw, colon, type, close),
            )
        }
        symbols(
            AngleOpen, KwReturn, Colon, Type, Comma, RAngle,
        ) { open, kw, colon, type, comma, close ->
            ParsedFoxMethodType(
                null,
                null,
                ParsedPair(kw to type, mergeSpan(kw, colon, type)),
                mergeSpan(open, kw, colon, type, comma, close),
            )
        }
        symbols(
            AngleOpen, KwThis, Colon, Type, AngleClose,
        ) { open, kw, colon, type, close ->
            ParsedFoxMethodType(
                ParsedPair(kw to type, mergeSpan(kw, colon, type)),
                null,
                null,
                mergeSpan(open, kw, colon, type, close),
            )
        }
        symbols(
            AngleOpen, KwThis, Colon, Type, Comma, RAngle,
        ) { open, kw, colon, type, comma, close ->
            ParsedFoxMethodType(
                ParsedPair(kw to type, mergeSpan(kw, colon, type)),
                null,
                null,
                mergeSpan(open, kw, colon, type, comma, close),
            )
        }
        symbols(
            AngleOpen, KwThis, Colon, Type, Comma, KwReturn, Colon, Type, AngleClose,
        ) { open, kw0, colon0, type0, comma, kw1, colon1, type1, close ->
            ParsedFoxMethodType(
                ParsedPair(kw0 to type0, mergeSpan(kw0, colon0, type0)),
                null,
                ParsedPair(kw1 to type1, mergeSpan(kw1, colon1, type1)),
                mergeSpan(open, kw0, colon0, type0, comma, kw1, colon1, type1, close),
            )
        }
        symbols(
            AngleOpen, KwThis, Colon, Type, Comma, KwReturn, Colon, Type, Comma, RAngle,
        ) { open, kw0, colon0, type0, comma0, kw1, colon1, type1, comma1, close ->
            ParsedFoxMethodType(
                ParsedPair(kw0 to type0, mergeSpan(kw0, colon0, type0)),
                null,
                ParsedPair(kw1 to type1, mergeSpan(kw1, colon1, type1)),
                mergeSpan(open, kw0, colon0, type0, comma0, kw1, colon1, type1, comma1, close),
            )
        }
        symbols(
            MethodTypeParameterListHead, AngleClose,
        ) { head, close ->
            ParsedFoxMethodType(
                head.`this`,
                head.parameters,
                null,
                mergeSpan(head, close),
            )
        }
        symbols(
            MethodTypeParameterListHead, Comma, RAngle,
        ) { head, comma, close ->
            ParsedFoxMethodType(
                head.`this`,
                head.parameters,
                null,
                mergeSpan(head, comma, close),
            )
        }
        symbols(
            MethodTypeParameterListHead, Comma, KwReturn, Colon, Type, AngleClose,
        ) { head, comma, kw, colon, type, close ->
            ParsedFoxMethodType(
                head.`this`,
                head.parameters,
                ParsedPair(kw to type, mergeSpan(kw, colon, type)),
                mergeSpan(head, comma, kw, colon, type, close),
            )
        }
        symbols(
            MethodTypeParameterListHead, Comma, KwReturn, Colon, Type, Comma, RAngle,
        ) { head, comma0, kw, colon, type, comma1, close ->
            ParsedFoxMethodType(
                head.`this`,
                head.parameters,
                ParsedPair(kw to type, mergeSpan(kw, colon, type)),
                mergeSpan(head, comma0, kw, colon, type, comma1, close),
            )
        }
    }
    
    rules(ParenthesizedExpression) {
        symbols(
            ParenOpen, Statement, ParenClose,
        ) { _, it, _ ->
            it
        }
    }
    
    rules(IndexArgumentListHead) {
        symbols(SquareOpen, Statement) { open, it ->
            ParsedList(listOf(it), mergeSpan(open, it))
        }
        symbols(
            IndexArgumentListHead, Comma, Statement,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(IndexArgumentList) {
        symbols(IndexArgumentListHead, SquareClose) { head, close ->
            ParsedList(head.node, mergeSpan(head, close))
        }
        symbols(
            IndexArgumentListHead, Comma, RBracket,
        ) { head, comma, close ->
            ParsedList(head.node, mergeSpan(head, comma, close))
        }
    }
    
    rules(PrimaryExpression) {
        symbols(ParenthesizedExpression) { it }
        
        symbols(LitUnit) { ParsedFoxEntityStatement(FoxUnit, it.span) }
        symbols(LitBool) { ParsedFoxEntityStatement(FoxBool(it.node), it.span) }
        symbols(LitInt) { ParsedFoxIntStatement(it, it.span) }
        symbols(LitLong) { ParsedFoxLongStatement(it, it.span) }
        symbols(LitFloat) { ParsedFoxFloatStatement(it, it.span) }
        symbols(LitDouble) { ParsedFoxDoubleStatement(it, it.span) }
        symbols(LitChar) { ParsedFoxEntityStatement(FoxChar(it.node), it.span) }
        symbols(LitString) { ParsedFoxEntityStatement(FoxString(it.node), it.span) }
        
        symbols(KwThis) { ParsedFoxThis(it.span) }
        
        symbols(Identifier) { ParsedFoxSymbol(it, it.span) }
        
        symbols(FormattedStringStart, FormattedStringEnd) { st, ed ->
            ParsedFoxFormattedString(null, mergeSpan(st, ed))
        }
        symbols(
            FormattedStringStart, FormattedStringPartList, FormattedStringEnd,
        ) { st, parts, ed ->
            ParsedFoxFormattedString(parts, mergeSpan(st, ed))
        }
        
        symbols(ExplicitLambdaLiteral) { it }
    }
    
    rules(PostfixExpression) {
        symbols(PrimaryExpression) { it }
        
        symbols(
            PostfixExpression, Dot, Identifier,
        ) { target, dot, name ->
            ParsedFoxFieldAccess(target, name, mergeSpan(target, dot, name))
        }
        symbols(
            PostfixExpression, IndexArgumentList,
        ) { target, indices ->
            ParsedFoxIndexAccess(target, indices, mergeSpan(target, indices))
        }
        
        symbols(Type, ActualParameterList) { type, parameters ->
            ParsedFoxConstruct(type, parameters, mergeSpan(type, parameters))
        }
        symbols(Type, LambdaLiteral) { type, lambda ->
            ParsedFoxConstruct(
                type,
                ParsedList(
                    listOf(ParsedPair(null to lambda, lambda.span)),
                    lambda.span,
                ),
                mergeSpan(type, lambda),
            )
        }
        symbols(
            Type, ActualParameterList, LambdaLiteral,
        ) { type, parameters, lambda ->
            ParsedFoxConstruct(
                type,
                ParsedList(
                    parameters.node + ParsedPair(null to lambda, lambda.span),
                    mergeSpan(parameters, lambda),
                ),
                mergeSpan(type, parameters, lambda),
            )
        }
        
        symbols(Identifier, ActualParameterList) { name, parameters ->
            ParsedFoxCall(null, name, null, parameters, mergeSpan(name, parameters))
        }
        symbols(Identifier, LambdaLiteral) { name, lambda ->
            ParsedFoxCall(
                null, name, null,
                ParsedList(
                    listOf(ParsedPair(null to lambda, lambda.span)),
                    lambda.span,
                ),
                mergeSpan(name, lambda),
            )
        }
        symbols(
            Identifier, ActualParameterList, LambdaLiteral,
        ) { name, parameters, lambda ->
            ParsedFoxCall(
                null, name, null,
                ParsedList(
                    parameters.node + ParsedPair(null to lambda, lambda.span),
                    mergeSpan(parameters, lambda),
                ),
                mergeSpan(name, parameters, lambda),
            )
        }
        symbols(
            Identifier, ActualGenericParameterList, ActualParameterList,
        ) { name, generics, parameters ->
            ParsedFoxCall(null, name, generics, parameters, mergeSpan(name, generics, parameters))
        }
        symbols(
            Identifier, ActualGenericParameterList, LambdaLiteral,
        ) { name, generics, lambda ->
            ParsedFoxCall(
                null, name, generics,
                ParsedList(
                    listOf(ParsedPair(null to lambda, lambda.span)),
                    lambda.span,
                ),
                mergeSpan(name, generics, lambda),
            )
        }
        symbols(
            Identifier, ActualGenericParameterList, ActualParameterList, LambdaLiteral,
        ) { name, generics, parameters, lambda ->
            ParsedFoxCall(
                null, name, generics,
                ParsedList(
                    parameters.node + ParsedPair(null to lambda, lambda.span),
                    mergeSpan(parameters, lambda),
                ),
                mergeSpan(name, generics, parameters, lambda),
            )
        }
        symbols(
            PostfixExpression, Dot, Identifier, ActualParameterList,
        ) { target, dot, name, parameters ->
            ParsedFoxCall(target, name, null, parameters, mergeSpan(target, dot, name, parameters))
        }
        symbols(
            PostfixExpression, Dot, Identifier, LambdaLiteral,
        ) { target, dot, name, lambda ->
            ParsedFoxCall(
                target, name, null,
                ParsedList(
                    listOf(ParsedPair(null to lambda, lambda.span)),
                    lambda.span,
                ),
                mergeSpan(target, dot, name, lambda),
            )
        }
        symbols(
            PostfixExpression, Dot, Identifier, ActualParameterList, LambdaLiteral,
        ) { target, dot, name, parameters, lambda ->
            ParsedFoxCall(
                target, name, null,
                ParsedList(
                    parameters.node + ParsedPair(null to lambda, lambda.span),
                    mergeSpan(parameters, lambda),
                ),
                mergeSpan(target, dot, name, parameters, lambda),
            )
        }
        symbols(
            PostfixExpression, Dot, Identifier, ActualGenericParameterList, ActualParameterList,
        ) { target, dot, name, generics, parameters ->
            ParsedFoxCall(target, name, generics, parameters, mergeSpan(target, dot, name, generics, parameters))
        }
        symbols(
            PostfixExpression, Dot, Identifier, ActualGenericParameterList, LambdaLiteral,
        ) { target, dot, name, generics, lambda ->
            ParsedFoxCall(
                target, name, generics,
                ParsedList(
                    listOf(ParsedPair(null to lambda, lambda.span)),
                    lambda.span,
                ),
                mergeSpan(target, dot, name, generics, lambda),
            )
        }
        symbols(
            PostfixExpression, Dot, Identifier, ActualGenericParameterList, ActualParameterList, LambdaLiteral,
        ) { target, dot, name, generics, parameters, lambda ->
            ParsedFoxCall(
                target, name, generics,
                ParsedList(
                    parameters.node + ParsedPair(null to lambda, lambda.span),
                    mergeSpan(parameters, lambda),
                ),
                mergeSpan(target, dot, name, generics, parameters, lambda),
            )
        }
        
        symbols(ParenthesizedExpression, ActualParameterList) { method, parameters ->
            ParsedFoxIndirectCall(null, method, parameters, mergeSpan(method, parameters))
        }
        symbols(ParenthesizedExpression, LambdaLiteral) { method, lambda ->
            ParsedFoxIndirectCall(
                null, method,
                ParsedList(listOf(ParsedPair(null to lambda, lambda.span)), lambda.span),
                mergeSpan(method, lambda),
            )
        }
        symbols(
            ParenthesizedExpression, ActualParameterList, LambdaLiteral,
        ) { method, parameters, lambda ->
            ParsedFoxIndirectCall(
                null, method,
                ParsedList(
                    parameters.node + ParsedPair(null to lambda, lambda.span),
                    mergeSpan(parameters, lambda),
                ),
                mergeSpan(method, parameters, lambda),
            )
        }
        symbols(
            PostfixExpression, Dot, ParenthesizedExpression, ActualParameterList,
        ) { target, dot, method, parameters ->
            ParsedFoxIndirectCall(target, method, parameters, mergeSpan(target, dot, method, parameters))
        }
        symbols(
            PostfixExpression, Dot, ParenthesizedExpression, LambdaLiteral,
        ) { target, dot, method, lambda ->
            ParsedFoxIndirectCall(
                target, method,
                ParsedList(listOf(ParsedPair(null to lambda, lambda.span)), lambda.span),
                mergeSpan(target, dot, method, lambda),
            )
        }
        symbols(
            PostfixExpression, Dot, ParenthesizedExpression, ActualParameterList, LambdaLiteral,
        ) { target, dot, method, parameters, lambda ->
            ParsedFoxIndirectCall(
                target, method,
                ParsedList(
                    parameters.node + ParsedPair(null to lambda, lambda.span),
                    mergeSpan(parameters, lambda),
                ),
                mergeSpan(target, dot, method, parameters, lambda),
            )
        }
    }
    
    rules(UnaryExpression) {
        symbols(PostfixExpression) { it }
        symbols(UnaryOperator, UnaryExpression) { operator, node ->
            ParsedFoxUnary(operator, node, mergeSpan(operator, node))
        }
    }
    
    rules(MultiplicativeExpression) {
        symbols(UnaryExpression) { it }
        symbols(
            MultiplicativeExpression, MultiplicativeOperator, UnaryExpression,
        ) { left, operator, right ->
            ParsedFoxBinary(left, operator, right, mergeSpan(left, operator, right))
        }
    }
    
    rules(AdditiveExpression) {
        symbols(MultiplicativeExpression) { it }
        symbols(
            AdditiveExpression, AdditiveOperator, MultiplicativeExpression,
        ) { left, operator, right ->
            ParsedFoxBinary(left, operator, right, mergeSpan(left, operator, right))
        }
    }
    
    rules(ShiftExpression) {
        symbols(AdditiveExpression) { it }
        symbols(
            ShiftExpression, ShiftOperator, AdditiveExpression,
        ) { left, operator, right ->
            ParsedFoxBinary(left, operator, right, mergeSpan(left, operator, right))
        }
    }
    
    rules(ComparisonExpression) {
        symbols(ShiftExpression) { it }
        symbols(
            ComparisonExpression, ComparisonOperator, ShiftExpression,
        ) { left, operator, right ->
            ParsedFoxBinary(left, operator, right, mergeSpan(left, operator, right))
        }
    }
    
    rules(EqualityExpression) {
        symbols(ComparisonExpression) { it }
        symbols(
            EqualityExpression, EqualityOperator, ComparisonExpression,
        ) { left, operator, right ->
            ParsedFoxBinary(left, operator, right, mergeSpan(left, operator, right))
        }
    }
    
    rules(BitAndExpression) {
        symbols(EqualityExpression) { it }
        symbols(
            BitAndExpression, BitAndOperator, EqualityExpression,
        ) { left, operator, right ->
            ParsedFoxBinary(left, operator, right, mergeSpan(left, operator, right))
        }
    }
    
    rules(BitXorExpression) {
        symbols(BitAndExpression) { it }
        symbols(
            BitXorExpression, BitXorOperator, BitAndExpression,
        ) { left, operator, right ->
            ParsedFoxBinary(left, operator, right, mergeSpan(left, operator, right))
        }
    }
    
    rules(BitOrExpression) {
        symbols(BitXorExpression) { it }
        symbols(
            BitOrExpression, BitOrOperator, BitXorExpression,
        ) { left, operator, right ->
            ParsedFoxBinary(left, operator, right, mergeSpan(left, operator, right))
        }
    }
    
    rules(LogicalAndExpression) {
        symbols(BitOrExpression) { it }
        symbols(
            LogicalAndExpression, LogicalAndOperator, BitOrExpression,
        ) { left, operator, right ->
            ParsedFoxBinary(left, operator, right, mergeSpan(left, operator, right))
        }
    }
    
    rules(LogicalOrExpression) {
        symbols(LogicalAndExpression) { it }
        symbols(
            LogicalOrExpression, LogicalOrOperator, LogicalAndExpression,
        ) { left, operator, right ->
            ParsedFoxBinary(left, operator, right, mergeSpan(left, operator, right))
        }
    }
    
    rules(FormattedStringPart) {
        formattedStringText { text, span -> ParsedFoxFormattedText(text, span) }
        symbols(
            FormattedExpressionOpen, Statement, FormattedExpressionClose,
        ) { open, expr, close ->
            ParsedFoxFormattedExpression(expr, mergeSpan(open, expr, close))
        }
    }
    rules(FormattedStringPartList) {
        symbols(FormattedStringPart) {
            ParsedList(listOf(it), it.span)
        }
        symbols(FormattedStringPartList, FormattedStringPart) { head, part ->
            ParsedList(head.node + part, mergeSpan(head, part))
        }
    }
    
    rules(LambdaStatementBlockHead) {
        symbols(LineBreak, StatementLine) { _, stmt ->
            ParsedList(listOf(stmt), stmt.span)
        }
        symbols(LambdaStatementBlockHead, StatementLine) { head, stmt ->
            ParsedList(head.node + stmt, mergeSpan(head, stmt))
        }
    }
    rules(LambdaBody) {
        symbols(Statement) { it }
        symbols(LineBreak, Statement) { _, it -> it }
        symbols(LambdaStatementBlockHead) { ParsedFoxBlock(null, it, it.span) }
    }
    rules(ExplicitLambdaLiteral) {
        symbols(
            BraceOpen, Arrow, BraceClose,
        ) { open, arrow, close ->
            val span = mergeSpan(open, arrow, close)
            ParsedFoxLambda(null, ParsedFoxBlock(null, ParsedList(emptyList(), span), span), span)
        }
        symbols(
            BraceOpen, Arrow, LambdaBody, RBrace,
        ) { open, arrow, body, close ->
            val span = mergeSpan(open, arrow, body, close)
            ParsedFoxLambda(null, body, span)
        }
        symbols(
            BraceOpen, LambdaParameterList, Arrow, BraceClose,
        ) { open, params, arrow, close ->
            val span = mergeSpan(open, params, arrow, close)
            ParsedFoxLambda(params, ParsedFoxBlock(null, ParsedList(emptyList(), span), span), span)
        }
        symbols(
            BraceOpen, LambdaParameterList, Arrow, LambdaBody, RBrace,
        ) { open, params, arrow, body, close ->
            val span = mergeSpan(open, params, arrow, body, close)
            ParsedFoxLambda(params, body, span)
        }
    }
    rules(InlineImplicitLambdaLiteral) {
        symbols(
            LBrace, Statement, RBrace,
        ) { open, body, close ->
            ParsedFoxLambda(null, body, mergeSpan(open, body, close))
        }
    }
    rules(ImplicitLambdaLiteral) {
        symbols(InlineImplicitLambdaLiteral) { it }
        symbols(LBrace, BraceClose) { open, close ->
            val span = mergeSpan(open, close)
            ParsedFoxLambda(null, ParsedFoxBlock(null, ParsedList(emptyList(), span), span), span)
        }
        symbols(
            BraceOpen, LambdaStatementBlockHead, BraceClose,
        ) { open, stmts, close ->
            val span = mergeSpan(open, stmts, close)
            ParsedFoxLambda(null, ParsedFoxBlock(null, stmts, span), span)
        }
    }
    rules(LambdaLiteral) {
        symbols(ExplicitLambdaLiteral) { it }
        symbols(ImplicitLambdaLiteral) { it }
    }
    
    rules(Label) { symbols(Hash, Identifier) { _, it -> it } }
    
    rules(StatementBlockHead) {
        symbols(BraceOpen, StatementLine) { open, it ->
            ParsedList(listOf(it), mergeSpan(open, it))
        }
        symbols(StatementBlockHead, StatementLine) { head, it ->
            ParsedList(head.node + it, mergeSpan(head, it))
        }
    }
    rules(StatementBlockCore) {
        symbols(LBrace, BraceClose) { open, close ->
            ParsedList(emptyList(), mergeSpan(open, close))
        }
        symbols(StatementBlockHead, BraceClose) { head, close ->
            ParsedList(head.node, mergeSpan(head, close))
        }
    }
    rules(StatementBlock) {
        symbols(StatementBlockCore) {
            ParsedFoxBlock(null, it, it.span)
        }
        symbols(Label, StatementBlockCore) { label, core ->
            ParsedFoxBlock(label, core, mergeSpan(label, core))
        }
    }
    
    rules(Statement) {
        symbols(LogicalOrExpression) { it }
        
        symbols(
            PostfixExpression, AssignOperator, Statement,
        ) { left, operator, right ->
            ParsedFoxAssign(left, operator, right, beforeEvaluation = true, mergeSpan(left, operator, right))
        }
        
        symbols(StatementBlock) { it }
        
        symbols(
            Identifier, Colon, Type,
        ) { name, colon, type ->
            ParsedFoxTypeBinding(name, type, mergeSpan(name, colon, type))
        }
        
        symbols(KwBreak) {
            ParsedFoxBreak(null, it.span)
        }
        symbols(KwBreak, Label) { kw, label ->
            ParsedFoxBreak(label, mergeSpan(kw, label))
        }
        symbols(KwContinue) {
            ParsedFoxContinue(null, it.span)
        }
        symbols(KwContinue, Label) { kw, label ->
            ParsedFoxContinue(label, mergeSpan(kw, label))
        }
        symbols(KwReturn) {
            ParsedFoxReturn(null, it.span)
        }
        symbols(KwReturn, LogicalOrExpression) { kw, value ->
            ParsedFoxReturn(value, mergeSpan(kw, value))
        }
        symbols(KwYield, LogicalOrExpression) { kw, value ->
            ParsedFoxYield(null, value, mergeSpan(kw, value))
        }
        symbols(
            KwYield, Label, LogicalOrExpression,
        ) { kw, label, value ->
            ParsedFoxYield(label, value, mergeSpan(kw, label, value))
        }
        
        symbols(IfCore) { core ->
            ParsedFoxIf(null, core.condition, core.thenBody, core.elseBody, core.span)
        }
        symbols(WhileCore) { core ->
            ParsedFoxWhile(null, core.condition, core.body, core.span)
        }
        symbols(DoWhileCore) { core ->
            ParsedFoxDoWhile(null, core.body, core.condition, core.span)
        }
        symbols(WhenCore) { core ->
            ParsedFoxWhen(null, core.value, core.cases, core.span)
        }
        symbols(Label, IfCore) { label, core ->
            ParsedFoxIf(label, core.condition, core.thenBody, core.elseBody, mergeSpan(label, core))
        }
        symbols(Label, WhileCore) { label, core ->
            ParsedFoxWhile(label, core.condition, core.body, mergeSpan(label, core))
        }
        symbols(Label, DoWhileCore) { label, core ->
            ParsedFoxDoWhile(label, core.body, core.condition, mergeSpan(label, core))
        }
        symbols(Label, WhenCore) { label, core ->
            ParsedFoxWhen(label, core.value, core.cases, mergeSpan(label, core))
        }
    }
    rules(StatementLine) {
        symbols(Statement, LineBreak) { stmt, _ -> stmt }
    }
    
    rules(IfCore) {
        symbols(
            KwIf, ParenthesizedExpression, Statement,
        ) { kw, condition, body ->
            ParsedFoxIf(null, condition, body, null, mergeSpan(kw, condition, body))
        }
        symbols(
            KwIf, ParenthesizedExpression, Statement, KwElse, Statement,
        ) { kw0, condition, thenBody, kw1, elseBody ->
            ParsedFoxIf(null, condition, thenBody, elseBody, mergeSpan(kw0, condition, thenBody, kw1, elseBody))
        }
    }
    
    rules(WhileCore) {
        symbols(
            KwWhile, ParenthesizedExpression, Statement,
        ) { kw, condition, body ->
            ParsedFoxWhile(null, condition, body, mergeSpan(kw, condition, body))
        }
    }
    
    rules(DoWhileCore) {
        symbols(
            KwDo, Statement, KwWhile, ParenthesizedExpression,
        ) { kw0, body, kw1, condition ->
            ParsedFoxDoWhile(null, body, condition, mergeSpan(kw0, body, kw1, condition))
        }
    }
    
    rules(WhenCaseConditionList) {
        symbols(LogicalOrExpression) {
            ParsedList(listOf(it), it.span)
        }
        symbols(
            WhenCaseConditionList, Comma, LogicalOrExpression,
        ) { head, comma, it ->
            ParsedList(head.node + it, mergeSpan(head, comma, it))
        }
    }
    rules(WhenCase) {
        symbols(
            WhenCaseConditionList, Arrow, Statement,
        ) { conditions, arrow, body ->
            ParsedFoxCase(conditions, body, mergeSpan(conditions, arrow, body))
        }
        symbols(
            KwElse, Arrow, Statement,
        ) { kw, arrow, body ->
            ParsedFoxCase(null, body, mergeSpan(kw, arrow, body))
        }
    }
    rules(WhenCaseLine) {
        symbols(WhenCase, LineBreak) { case, _ -> case }
    }
    rules(WhenCaseListHead) {
        symbols(WhenCaseLine) {
            ParsedList(listOf(it), it.span)
        }
        symbols(WhenCaseListHead, WhenCaseLine) { head, it ->
            ParsedList(head.node + it, mergeSpan(head, it))
        }
    }
    rules(WhenCaseList) {
        symbols(
            BraceOpen, WhenCaseListHead, BraceClose,
        ) { open, it, close ->
            ParsedList(it.node, mergeSpan(open, it, close))
        }
    }
    
    rules(WhenCore) {
        symbols(KwWhen, WhenCaseList) { kw, cases ->
            ParsedFoxWhen(null, null, cases, mergeSpan(kw, cases))
        }
        symbols(
            KwWhen, ParenthesizedExpression, WhenCaseList,
        ) { kw, value, cases ->
            ParsedFoxWhen(null, value, cases, mergeSpan(kw, value, cases))
        }
    }
    
    rules(TypeAlias) {
        symbols(
            KwType, TypeName, PlainAssign, Type,
        ) { kw, name, assign, type ->
            ParsedFoxTypeAlias(name, null, type, mergeSpan(kw, name, assign, type))
        }
        symbols(
            KwType, TypeName, FormalGenericParameterNoConstraintsList, PlainAssign, Type,
        ) { kw, name, generics, assign, type ->
            ParsedFoxTypeAlias(name, generics, type, mergeSpan(kw, name, generics, assign, type))
        }
    }
    
    rules(MethodDefinition) {
        symbols(
            KwDef, Identifier, FormalParameterList, StatementBlock,
        ) { kw, name, parameters, body ->
            val span = mergeSpan(kw, name, parameters, body)
            ParsedFoxMethodDefinition(null, null, name, parameters, null, body, span)
        }
        symbols(
            KwDef, Type, Dot, Identifier, FormalParameterList, StatementBlock,
        ) { kw, thisType, dot, name, parameters, body ->
            val span = mergeSpan(kw, thisType, dot, name, parameters, body)
            ParsedFoxMethodDefinition(null, thisType, name, parameters, null, body, span)
        }
        symbols(
            KwDef, FormalGenericParameterList, Identifier, FormalParameterList, StatementBlock,
        ) { kw, generics, name, parameters, body ->
            val span = mergeSpan(kw, generics, name, parameters, body)
            ParsedFoxMethodDefinition(generics, null, name, parameters, null, body, span)
        }
        symbols(
            KwDef, FormalGenericParameterList, Type, Dot, Identifier, FormalParameterList, StatementBlock,
        ) { kw, generics, thisType, dot, name, parameters, body ->
            val span = mergeSpan(kw, generics, thisType, dot, name, parameters, body)
            ParsedFoxMethodDefinition(generics, thisType, name, parameters, null, body, span)
        }
        symbols(
            KwDef, Identifier, FormalParameterList, Colon, Type, StatementBlock,
        ) { kw, name, parameters, colon, returnType, body ->
            val span = mergeSpan(kw, name, parameters, colon, returnType, body)
            ParsedFoxMethodDefinition(null, null, name, parameters, returnType, body, span)
        }
        symbols(
            KwDef, Type, Dot, Identifier, FormalParameterList, Colon, Type, StatementBlock,
        ) { kw, thisType, dot, name, parameters, colon, returnType, body ->
            val span = mergeSpan(kw, thisType, dot, name, parameters, colon, returnType, body)
            ParsedFoxMethodDefinition(null, thisType, name, parameters, returnType, body, span)
        }
        symbols(
            KwDef, FormalGenericParameterList, Identifier, FormalParameterList, Colon, Type, StatementBlock,
        ) { kw, generics, name, parameters, colon, returnType, body ->
            val span = mergeSpan(kw, generics, name, parameters, colon, returnType, body)
            ParsedFoxMethodDefinition(generics, null, name, parameters, returnType, body, span)
        }
        symbols(
            KwDef, FormalGenericParameterList, Type, Dot, Identifier, FormalParameterList, Colon, Type, StatementBlock,
        ) { kw, generics, thisType, dot, name, parameters, colon, returnType, body ->
            val span = mergeSpan(kw, generics, thisType, dot, name, parameters, colon, returnType, body)
            ParsedFoxMethodDefinition(generics, thisType, name, parameters, returnType, body, span)
        }
    }
    
    rules(FileElement) {
        symbols(TypeAlias) { it }
        symbols(MethodDefinition) { it }
    }
    rules(FileElementLine) {
        symbols(FileElement, LineBreak) { element, _ -> element }
    }
    rules(FileElementList) {
        symbols(FileElementLine) {
            ParsedList(listOf(it), it.span)
        }
        symbols(FileElementList, FileElementLine) { head, it ->
            ParsedList(head.node + it, mergeSpan(head, it))
        }
    }
    rules(File) {
        symbols(FileElementList) {
            ParsedFoxFile(it.node, it.span)
        }
    }
}
