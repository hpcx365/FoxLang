package pers.hpcx.foxlang.frontend.fox

import pers.hpcx.foxlang.frontend.common.*
import pers.hpcx.foxlang.frontend.fox.FoxAssignOperatorSymbol.PlainAssign
import pers.hpcx.foxlang.frontend.fox.FoxBinaryOperatorCategorySymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxBracketSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxExpressionSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxFormattedStringPartSymbol.FormattedStringEnd
import pers.hpcx.foxlang.frontend.fox.FoxFormattedStringPartSymbol.FormattedStringStart
import pers.hpcx.foxlang.frontend.fox.FoxKeywordSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxLiteralSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxTerminalSymbol.*

val FoxRepairStrategy = RepairStrategy(
    File to { expect(FileElementList) },
    FileElementList to { repairFileElementList() },
    FileElementLine to { repairFileElementLine() },
    FileElement to { repairFileElement() },
    TypeAlias to { repairTypeAlias() },
    MethodDefinition to { repairMethodDefinition() },
    Type to { repairType() },
    
    *FoxDelimitedCommaListSpecs.map { it.repairEntry() }.toTypedArray(),
    *FoxUndelimitedCommaListSpecs.map { it.repairEntry() }.toTypedArray(),
    
    ActualParameterList to { repairActualParameterList() },
    ActualGenericParameterList to { repairDelimitedCommaList(AngleOpen, AngleClose, ActualGenericParameter) },
    AnonymousActualGenericParameterSingleList to {
        repairFixedArityAngleArguments(listOf(AnonymousActualGenericParameter))
    },
    MethodTypeParameterList to { repairMethodTypeParameterList() },
    FormalParameter to { repairColonTypedItem(Identifier) },
    LambdaParameter to { repairOptionalColonTypedItem(Identifier, Type) },
    FormalGenericParameter to { repairOptionalEqualNamedType(TypeName, Type) },
    FormalGenericParameterNoConstraints to { expect(TypeName) },
    ActualGenericParameter to { repairActualGenericParameter() },
    AnonymousActualGenericParameter to { expect(Type) },
    TupleComponentParameter to { expect(Type) },
    StructFieldParameter to { repairColonTypedItem(Identifier) },
    StructFieldName to { expect(Identifier) },
    ObjectMemberParameter to { repairColonTypedItem(Identifier) },
    ObjectMemberName to { expect(Identifier) },
    EnumEntryParameter to { repairEqualTypedItem(TypeName, Type) },
    EnumEntryName to { expect(TypeName) },
    MethodTypeParameter to { repairMethodTypeParameter() },
    ActualParameter to { repairActualParameter() },
    
    Label to { repairLabel() },
    
    StatementBlock to { repairBlock() },
    StatementBlockCore to { repairStatementBlock() },
    StatementLine to { repairStatementLine() },
    Statement to { repairStatement() },
    ParenthesizedExpression to { repairParenthesizedStatement() },
    PrimaryExpression to { repairPrimaryExpression() },
    PostfixExpression to { repairPostfixExpression() },
    UnaryExpression to { repairUnaryExpression() },
    LogicalOrExpression to { repairBinaryExpression(LogicalOrExpression, LogicalAndExpression, LogicalOrOperator) },
    LogicalAndExpression to { repairBinaryExpression(LogicalAndExpression, BitOrExpression, LogicalAndOperator) },
    BitOrExpression to { repairBinaryExpression(BitOrExpression, BitXorExpression, BitOrOperator) },
    BitXorExpression to { repairBinaryExpression(BitXorExpression, BitAndExpression, BitXorOperator) },
    BitAndExpression to { repairBinaryExpression(BitAndExpression, EqualityExpression, BitAndOperator) },
    EqualityExpression to { repairBinaryExpression(EqualityExpression, ComparisonExpression, EqualityOperator) },
    ComparisonExpression to { repairBinaryExpression(ComparisonExpression, ShiftExpression, ComparisonOperator) },
    ShiftExpression to { repairBinaryExpression(ShiftExpression, AdditiveExpression, ShiftOperator) },
    AdditiveExpression to { repairBinaryExpression(AdditiveExpression, MultiplicativeExpression, AdditiveOperator) },
    MultiplicativeExpression to { repairBinaryExpression(MultiplicativeExpression, UnaryExpression, MultiplicativeOperator) },
    LambdaLiteral to { repairLambdaLiteral() },
    ExplicitLambdaLiteral to { repairExplicitLambdaLiteral() },
    InlineImplicitLambdaLiteral to { repairInlineImplicitLambdaLiteral() },
    ImplicitLambdaLiteral to { repairImplicitLambdaLiteral() },
    LambdaBody to { repairLambdaBody() },
    LambdaStatementBlockHead to { repairLambdaStatementBlockHead() },
    IfCore to { repairIfCore() },
    WhileCore to { repairWhileCore() },
    DoWhileCore to { repairDoWhileCore() },
    WhenCore to { repairWhenCore() },
    WhenCaseList to { repairWhenCaseList() },
    WhenCaseLine to { repairWhenCaseLine() },
    WhenCase to { repairWhenCase() },
    WhenCaseConditionList to { repairWhenCaseConditionList() },
)

private const val MaxListFrames = 8

private data class PostfixIndexSpan(
    val open: SourceSpan,
    val close: SourceSpan?,
)

private fun FoxDelimitedCommaListSpec.repairEntry(): Pair<GrammarSymbol<*>, ExpectationStrategy> {
    return list to { repairDelimitedCommaList(open, close, item) }
}

private fun FoxUndelimitedCommaListSpec.repairEntry(): Pair<GrammarSymbol<*>, ExpectationStrategy> {
    return list to { repairUndelimitedCommaList(item) }
}

private fun ExpectationReceiver.repairFileElementList() {
    val lines = matchesInside(FileElementLine).nonOverlapping()
    if (lines.isEmpty()) {
        val candidates = topLevelElementSpans()
        if (candidates.isEmpty()) {
            val span = trimOuterLineBreaks(currentSpan())
            if (span.isNotEmpty()) expect(FileElementLine, span)
        } else {
            candidates.forEach { expect(FileElementLine, it) }
        }
        return
    }
    
    expectFileElementsInGap(trimOuterLineBreaks(SourceSpan(currentSpan().start, lines.first().span.start)))
    lines.zipWithNext().forEach { (left, right) ->
        expectFileElementsInGap(SourceSpan(left.span.end, right.span.start))
    }
    expectFileElementsInGap(SourceSpan(lines.last().span.end, currentSpan().end))
}

private fun ExpectationReceiver.expectFileElementsInGap(gap: SourceSpan) {
    val trimmed = trimOuterLineBreaks(gap)
    if (trimmed.isEmpty()) return
    val candidates = withSpan(trimmed).topLevelElementSpans()
    if (candidates.isEmpty()) {
        expect(FileElementLine, trimmed)
    } else {
        candidates.forEach { expect(FileElementLine, it) }
    }
}

private fun ExpectationReceiver.topLevelElementSpans(): List<SourceSpan> {
    val starts = (matchesInside(KwDef) + matchesInside(KwType))
        .distinctBy { it.span }
        .sortedBy { it.span.start.fragIndex }
    if (starts.isEmpty()) return emptyList()
    return starts.mapIndexed { index, start ->
        val end = starts.getOrNull(index + 1)?.span?.start ?: currentSpan().end
        trimOuterLineBreaks(SourceSpan(start.span.start, end))
    }.filter { it.isNotEmpty() }
}

private fun ExpectationReceiver.repairFileElementLine() {
    val lineBreak = matchesEndingAt(LineBreak, currentSpan().end).firstOrNull()
    if (lineBreak != null) {
        val elementSpan = trimOuterLineBreaks(SourceSpan(currentSpan().start, lineBreak.span.start))
        if (elementSpan.isNotEmpty()) expect(FileElement, elementSpan)
        return
    }
    
    val span = trimOuterLineBreaks(currentSpan())
    if (span.isNotEmpty()) expect(FileElement, span)
    expectAt(LineBreak, currentSpan().end)
}

private fun ExpectationReceiver.repairFileElement() {
    when (firstPlainText()) {
        "def" -> expect(MethodDefinition)
        "type" -> expect(TypeAlias)
        else -> {
            if (containsBuildable(KwDef)) expect(MethodDefinition)
            if (containsBuildable(KwType)) expect(TypeAlias)
        }
    }
}

private fun ExpectationReceiver.repairTypeAlias() {
    val span = trimOuterLineBreaks(currentSpan())
    val equals = withSpan(span).lastInside(PlainAssign)
    val typeName = withSpan(span).firstInside(TypeName)
    if (typeName != null) {
        expect(KwType, trimOuterLineBreaks(SourceSpan(span.start, typeName.span.start)))
    } else {
        expect(KwType, trimOuterLineBreaks(SourceSpan(span.start, span.start)))
    }
    
    if (equals != null) {
        expect(TypeName, trimOuterLineBreaks(SourceSpan(typeName?.span?.start ?: span.start, equals.span.start)))
        val aliasSpan = trimOuterLineBreaks(SourceSpan(equals.span.end, span.end))
        if (aliasSpan.isNotEmpty()) expect(Type, aliasSpan)
    } else {
        if (typeName != null) expectAt(PlainAssign, typeName.span.end)
        val tail = trimOuterLineBreaks(SourceSpan(typeName?.span?.end ?: span.start, span.end))
        if (tail.isNotEmpty()) expect(Type, tail)
    }
}

private fun ExpectationReceiver.repairMethodDefinition() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val blockSpan = receiver.lastInside(StatementBlock)?.span ?: receiver.lastDelimited(BraceOpen, BraceClose)
    if (blockSpan != null) {
        expect(StatementBlock, blockSpan)
    } else {
        expectAt(StatementBlock, span.end)
    }
    
    val beforeBlock = trimOuterLineBreaks(SourceSpan(span.start, blockSpan?.start ?: span.end))
    if (beforeBlock.isEmpty()) return
    
    val headAndReturn = withSpan(beforeBlock)
    val parametersEnd = headAndReturn.lastInside(FormalParameterList)?.span?.end
        ?: headAndReturn.lastDelimited(ParenOpen, ParenClose)?.end
    val returnColon = headAndReturn.matchesInside(Colon)
        .filter { parametersEnd == null || parametersEnd <= it.span.start }
        .maxByOrNull { it.span.start.fragIndex }
    if (returnColon != null) {
        val headSpan = trimOuterLineBreaks(SourceSpan(beforeBlock.start, returnColon.span.start))
        if (headSpan.isNotEmpty()) repairMethodDefinitionHead(headSpan)
        expect(Colon, returnColon.span)
        val returnType = trimOuterLineBreaks(SourceSpan(returnColon.span.end, beforeBlock.end))
        if (returnType.isNotEmpty()) {
            expect(Type, returnType)
        } else {
            expectAt(Type, returnColon.span.end)
        }
    } else {
        repairMethodDefinitionHead(beforeBlock)
    }
}

private fun ExpectationReceiver.repairMethodDefinitionHead(prefix: SourceSpan) {
    if (prefix.isEmpty()) return
    
    val receiver = withSpan(prefix)
    val def = receiver.firstInside(KwDef)
    if (def == null) expectAt(KwDef, prefix.start)
    
    val afterDef = trimOuterLineBreaks(SourceSpan(def?.span?.end ?: prefix.start, prefix.end))
    if (afterDef.isEmpty()) return
    
    val tail = withSpan(afterDef)
    val generics = tail.firstDelimited(AngleOpen, AngleClose)
    if (generics != null) expect(FormalGenericParameterList, generics)
    
    val afterGenerics = trimOuterLineBreaks(SourceSpan(generics?.end ?: afterDef.start, afterDef.end))
    val parameterReceiver = withSpan(afterGenerics)
    val parametersSpan = parameterReceiver.lastInside(FormalParameterList)?.span
        ?: parameterReceiver.lastDelimited(ParenOpen, ParenClose)
    
    if (parametersSpan != null) {
        expect(FormalParameterList, parametersSpan)
    } else {
        expectAt(FormalParameterList, afterDef.end)
    }
    
    val namePrefixEnd = parametersSpan?.start ?: afterGenerics.end
    repairMethodNamePrefix(trimOuterLineBreaks(SourceSpan(afterGenerics.start, namePrefixEnd)))
}

private fun ExpectationReceiver.repairMethodNamePrefix(prefix: SourceSpan) {
    if (prefix.isEmpty()) {
        expectAt(Identifier, prefix.start)
        return
    }
    
    val receiver = withSpan(prefix)
    val dot = receiver.lastInside(Dot)
    if (dot != null) {
        val thisTypeSpan = trimOuterLineBreaks(SourceSpan(prefix.start, dot.span.start))
        if (thisTypeSpan.isNotEmpty()) {
            expect(Type, thisTypeSpan)
        } else {
            expectAt(Type, dot.span.start)
        }
        expect(Dot, dot.span)
        val nameSpan = trimOuterLineBreaks(SourceSpan(dot.span.end, prefix.end))
        if (nameSpan.isNotEmpty()) {
            expect(Identifier, nameSpan)
        } else {
            expectAt(Identifier, dot.span.end)
        }
        return
    }
    
    val name = receiver.lastInside(Identifier)
    if (name != null) {
        expect(Identifier, name.span)
    } else {
        expect(Identifier, prefix)
    }
}

private fun ExpectationReceiver.repairType() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    if (span.isEmpty()) {
        expect(TypeName, span)
        return
    }
    
    val firstKeyword = FoxKeywordsByText[receiver.firstPlainText()]
    if (firstKeyword != null) {
        FoxTypeArgumentLists[firstKeyword]?.let {
            receiver.repairTypeArgumentList(firstKeyword, it)
            return
        }
        FoxFixedArityTypeArguments[firstKeyword]?.let {
            receiver.repairFixedArityType(firstKeyword, it)
            return
        }
    }
    
    val typeName = receiver.firstInside(TypeName)
    if (typeName != null) {
        expect(TypeName, typeName.span)
        val genericSpan = trimOuterLineBreaks(SourceSpan(typeName.span.end, span.end))
        if (genericSpan.isNotEmpty()) expect(AnonymousActualGenericParameterList, genericSpan)
        return
    }
    
    expect(TypeName, span)
}

private fun ExpectationReceiver.repairTypeArgumentList(
    keyword: FoxGrammarSymbol<*>,
    arguments: GrammarSymbol<*>,
) {
    val span = trimOuterLineBreaks(currentSpan())
    val keywordMatch = withSpan(span).firstInside(keyword)
    if (keywordMatch == null) {
        expectAt(keyword, span.start)
        expect(arguments, span)
        return
    }
    
    val argumentSpan = trimOuterLineBreaks(SourceSpan(keywordMatch.span.end, span.end))
    expect(arguments, argumentSpan)
}

private fun ExpectationReceiver.repairFixedArityType(
    keyword: FoxGrammarSymbol<*>,
    arguments: List<GrammarSymbol<*>>,
) {
    val span = trimOuterLineBreaks(currentSpan())
    val keywordMatch = withSpan(span).firstInside(keyword)
    if (keywordMatch == null) {
        expectAt(keyword, span.start)
        withSpan(span).repairFixedArityAngleArguments(arguments)
        return
    }
    
    val argumentSpan = trimOuterLineBreaks(SourceSpan(keywordMatch.span.end, span.end))
    withSpan(argumentSpan).repairFixedArityAngleArguments(arguments)
}

private fun ExpectationReceiver.repairFixedArityAngleArguments(arguments: List<GrammarSymbol<*>>) {
    repairFixedArityDelimitedArguments(
        open = AngleOpen,
        close = AngleClose,
        separator = Comma,
        arguments = arguments,
        maxFrames = MaxListFrames,
        trim = { trimOuterLineBreaks(it) },
    )
}

private fun ExpectationReceiver.repairDelimitedCommaList(
    open: GrammarSymbol<*>,
    close: GrammarSymbol<*>,
    item: GrammarSymbol<*>,
) {
    repairDelimitedSeparatedList(
        open = open,
        close = close,
        separator = Comma,
        item = item,
        maxFrames = MaxListFrames,
        trim = { trimOuterLineBreaks(it) },
        ignoredFragment = { it is LineBreakFragment },
    )
}

private fun ExpectationReceiver.repairUndelimitedCommaList(item: GrammarSymbol<*>) {
    repairUndelimitedSeparatedList(
        separator = Comma,
        item = item,
        trim = { trimOuterLineBreaks(it) },
        ignoredFragment = { it is LineBreakFragment },
    )
}

private fun ExpectationReceiver.repairActualParameterList() {
    repairDelimitedCommaList(ParenOpen, ParenClose, ActualParameter)
    
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val open = receiver.firstInside(ParenOpen) ?: return
    val close = receiver.lastPlainTextSpan(")") ?: receiver.lastInside(RParen)?.span ?: receiver.lastInside(ParenClose)?.span
    val content = SourceSpan(open.span.end, close?.start ?: span.end)
    val contentReceiver = withSpan(content)
    val separators = contentReceiver.matchesInside(Comma)
        .filter { contentReceiver.isTopLevelSourcePosition(it.span.start) }
        .distinctBy { it.span }
        .sortedBy { it.span.start.fragIndex }
    val starts = listOf(content.start) + separators.map { it.span.end }
    val ends = separators.map { it.span.start } + listOf(content.end)
    
    starts.zip(ends).forEach { (start, end) ->
        if (start > end) return@forEach
        val segment = trimOuterLineBreaks(SourceSpan(start, end))
        if (segment.isNotEmpty() && withSpan(segment).firstTopLevelPlainTextSpan("=") != null) {
            expect(ActualParameter, segment)
        }
    }
}

private fun ExpectationReceiver.repairColonTypedItem(name: GrammarSymbol<*>) {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val colon = receiver.firstInside(Colon)
    if (colon != null) {
        val head = trimOuterLineBreaks(SourceSpan(span.start, colon.span.start))
        val type = trimOuterLineBreaks(SourceSpan(colon.span.end, span.end))
        if (head.isNotEmpty()) {
            expect(name, head)
        } else {
            expectAt(name, colon.span.start)
        }
        expect(Colon, colon.span)
        if (type.isNotEmpty()) {
            expect(Type, type)
        } else {
            expectAt(Type, colon.span.end)
        }
        return
    }
    
    val identifier = receiver.firstInside(name) ?: receiver.firstInside(Identifier) ?: receiver.firstInside(TypeName)
    if (identifier != null) {
        expect(name, trimOuterLineBreaks(SourceSpan(identifier.span.start, identifier.span.end)))
        expectAt(Colon, identifier.span.end)
        val type = trimOuterLineBreaks(SourceSpan(identifier.span.end, span.end))
        if (type.isNotEmpty()) {
            expect(Type, type)
        } else {
            expectAt(Type, identifier.span.end)
        }
    } else {
        expect(name, span)
        expectAt(Colon, span.end)
        expectAt(Type, span.end)
    }
}

private fun ExpectationReceiver.repairOptionalColonTypedItem(name: GrammarSymbol<*>, type: GrammarSymbol<*>) {
    val span = trimOuterLineBreaks(currentSpan())
    val colon = withSpan(span).firstInside(Colon)
    if (colon == null) {
        expect(name, span)
    } else {
        val nameSpan = trimOuterLineBreaks(SourceSpan(span.start, colon.span.start))
        val typeSpan = trimOuterLineBreaks(SourceSpan(colon.span.end, span.end))
        if (nameSpan.isNotEmpty()) {
            expect(name, nameSpan)
        } else {
            expectAt(name, colon.span.start)
        }
        expect(Colon, colon.span)
        if (typeSpan.isNotEmpty()) {
            expect(type, typeSpan)
        } else {
            expectAt(type, colon.span.end)
        }
    }
}

private fun ExpectationReceiver.repairEqualTypedItem(name: GrammarSymbol<*>, type: GrammarSymbol<*>) {
    val span = trimOuterLineBreaks(currentSpan())
    val equals = withSpan(span).firstInside(PlainAssign)
    if (equals == null) {
        val nameMatch = withSpan(span).firstInside(name)
        if (nameMatch != null) {
            expect(name, nameMatch.span)
            expectAt(PlainAssign, nameMatch.span.end)
            val typeSpan = trimOuterLineBreaks(SourceSpan(nameMatch.span.end, span.end))
            if (typeSpan.isNotEmpty()) expect(type, typeSpan)
        } else {
            expect(name, span)
        }
    } else {
        val nameSpan = trimOuterLineBreaks(SourceSpan(span.start, equals.span.start))
        val typeSpan = trimOuterLineBreaks(SourceSpan(equals.span.end, span.end))
        if (nameSpan.isNotEmpty()) expect(name, nameSpan) else expectAt(name, equals.span.start)
        expect(PlainAssign, equals.span)
        if (typeSpan.isNotEmpty()) expect(type, typeSpan) else expectAt(type, equals.span.end)
    }
}

private fun ExpectationReceiver.repairOptionalEqualNamedType(name: GrammarSymbol<*>, type: GrammarSymbol<*>) {
    if (withSpan(trimOuterLineBreaks(currentSpan())).containsBuildable(PlainAssign)) {
        repairEqualTypedItem(name, type)
    } else {
        val span = trimOuterLineBreaks(currentSpan())
        if (span.isNotEmpty()) expect(name, span) else expectAt(name, span.start)
    }
}

private fun ExpectationReceiver.repairActualGenericParameter() {
    val span = trimOuterLineBreaks(currentSpan())
    val equals = withSpan(span).firstInside(PlainAssign)
    if (equals == null) {
        if (span.isNotEmpty()) expect(Type, span) else expectAt(Type, span.start)
        return
    }
    
    val name = trimOuterLineBreaks(SourceSpan(span.start, equals.span.start))
    val type = trimOuterLineBreaks(SourceSpan(equals.span.end, span.end))
    if (name.isNotEmpty()) expect(TypeName, name) else expectAt(TypeName, equals.span.start)
    expect(PlainAssign, equals.span)
    if (type.isNotEmpty()) expect(Type, type) else expectAt(Type, equals.span.end)
}

private fun ExpectationReceiver.repairActualParameter() {
    val span = trimOuterLineBreaks(currentSpan())
    if (span.isEmpty()) {
        expectAt(LogicalOrExpression, span.start)
        return
    }
    
    val receiver = withSpan(span)
    val assign = receiver.firstInside(PlainAssign)
        ?.takeIf { receiver.isTopLevelSourcePosition(it.span.start) }
        ?.span
        ?: receiver.firstTopLevelPlainTextSpan("=")
    if (assign == null) {
        receiver.repairActualParameterValue(span)
        return
    }
    
    val name = trimOuterLineBreaks(SourceSpan(span.start, assign.end))
    val value = trimOuterLineBreaks(SourceSpan(assign.end, span.end))
    if (name.isNotEmpty()) {
        expect(Identifier, trimOuterLineBreaks(SourceSpan(span.start, assign.start)))
    } else {
        expectAt(Identifier, assign.start)
    }
    expect(PlainAssign, assign)
    if (value.isNotEmpty()) {
        withSpan(value).repairActualParameterValue(value)
    } else {
        expectAt(LogicalOrExpression, assign.end)
    }
}

private fun ExpectationReceiver.repairActualParameterValue(span: SourceSpan) {
    when {
        startsWithArgumentClose() -> expectAt(LogicalOrExpression, span.start)
        startsWithBraceOpen() -> expect(ImplicitLambdaLiteral, span)
        else -> expect(LogicalOrExpression, span)
    }
}

private fun ExpectationReceiver.repairMethodTypeParameter() {
    repairColonTypedItem(Identifier)
}

private fun ExpectationReceiver.repairMethodTypeParameterList() {
    repairDelimitedCommaList(AngleOpen, AngleClose, MethodTypeParameter)
    
    val span = trimOuterLineBreaks(currentSpan())
    val open = withSpan(span).firstInside(AngleOpen)
    val close = withSpan(span).lastInside(AngleClose) ?: withSpan(span).lastInside(RAngle)
    val content = trimOuterLineBreaks(SourceSpan(open?.span?.end ?: span.start, close?.span?.start ?: span.end))
    if (content.isEmpty()) return
    
    val receiver = withSpan(content)
    val separators = receiver.matchesInside(Comma)
        .filter { receiver.isTopLevelSourcePosition(it.span.start) }
        .distinctBy { it.span }
        .sortedBy { it.span.start.fragIndex }
    val starts = listOf(content.start) + separators.map { it.span.end }
    val ends = separators.map { it.span.start } + listOf(content.end)
    starts.zip(ends).forEach { (start, end) ->
        val segment = trimOuterLineBreaks(SourceSpan(start, end))
        when (FoxKeywordsByText[withSpan(segment).firstPlainText()]) {
            KwThis -> withSpan(segment).repairKeywordTypedItem(KwThis)
            KwReturn -> withSpan(segment).repairKeywordTypedItem(KwReturn)
            else -> {}
        }
    }
}

private fun ExpectationReceiver.repairKeywordTypedItem(keyword: FoxGrammarSymbol<*>) {
    val span = trimOuterLineBreaks(currentSpan())
    val keywordMatch = withSpan(span).firstInside(keyword)
    val colon = withSpan(span).firstInside(Colon)
    if (keywordMatch != null) {
        expect(keyword, keywordMatch.span)
    } else {
        expectAt(keyword, span.start)
    }
    if (colon != null) {
        expect(Colon, colon.span)
        val type = trimOuterLineBreaks(SourceSpan(colon.span.end, span.end))
        if (type.isNotEmpty()) expect(Type, type) else expectAt(Type, colon.span.end)
    } else {
        val colonPosition = keywordMatch?.span?.end ?: span.start
        expectAt(Colon, colonPosition)
        val type = trimOuterLineBreaks(SourceSpan(colonPosition, span.end))
        if (type.isNotEmpty()) expect(Type, type) else expectAt(Type, colonPosition)
    }
}

private fun ExpectationReceiver.repairLabel() {
    val span = trimOuterLineBreaks(currentSpan())
    val hash = withSpan(span).firstInside(Hash)
    if (hash == null) {
        expectAt(Hash, span.start)
        if (span.isNotEmpty()) {
            expect(Identifier, span)
        } else {
            expectAt(Identifier, span.start)
        }
        return
    }
    
    val label = trimOuterLineBreaks(SourceSpan(hash.span.end, span.end))
    if (label.isNotEmpty()) {
        expect(Identifier, label)
    } else {
        expectAt(Identifier, hash.span.end)
    }
}

private fun ExpectationReceiver.repairLambdaLiteral() {
    if (containsBuildable(Arrow)) {
        expect(ExplicitLambdaLiteral)
    } else {
        expect(ImplicitLambdaLiteral)
    }
}

private fun ExpectationReceiver.repairExplicitLambdaLiteral() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val open = receiver.firstInside(BraceOpen)
    val arrow = receiver.firstInside(Arrow)
    val close = receiver.lastLambdaClose()
    
    if (open == null) expectAt(BraceOpen, span.start)
    
    val contentStart = open?.span?.end ?: span.start
    val contentEnd = close?.span?.start ?: span.end
    
    if (arrow == null || arrow.span.start < contentStart || contentEnd < arrow.span.end) {
        val content = trimOuterLineBreaks(SourceSpan(contentStart, contentEnd))
        val parameters = withSpan(content).matchesInside(LambdaParameter).candidateItemLanes().firstOrNull()
        if (!parameters.isNullOrEmpty()) {
            val parameterSpan = SourceSpan(parameters.first().span.start, parameters.last().span.end)
            expect(LambdaParameterList, parameterSpan)
            expectAt(Arrow, parameterSpan.end)
            val body = trimOuterLineBreaks(SourceSpan(parameterSpan.end, content.end))
            if (body.isNotEmpty()) expect(LambdaBody, body)
        } else {
            expectAt(Arrow, contentStart)
            if (content.isNotEmpty()) expect(LambdaBody, content)
        }
        if (close == null) expectAt(RBrace, span.end)
        return
    }
    
    val parameters = trimOuterLineBreaks(SourceSpan(contentStart, arrow.span.start))
    if (parameters.isNotEmpty()) expect(LambdaParameterList, parameters)
    
    val body = trimOuterLineBreaks(SourceSpan(arrow.span.end, contentEnd))
    if (body.isNotEmpty()) {
        expect(LambdaBody, body)
        if (close == null) expectAt(RBrace, span.end)
    } else if (close == null) {
        expectAt(BraceClose, span.end)
    }
}

private fun ExpectationReceiver.repairImplicitLambdaLiteral() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val open = receiver.firstInside(BraceOpen)
    val close = receiver.lastLambdaClose()
    
    if (open == null) expectAt(BraceOpen, span.start)
    
    val contentStart = open?.span?.end ?: span.start
    val contentEnd = close?.span?.start ?: span.end
    val content = SourceSpan(contentStart, contentEnd)
    val trimmedContent = trimOuterLineBreaks(content)
    
    if (trimmedContent.isEmpty()) {
        if (close == null) expectAt(BraceClose, span.end)
        return
    }
    
    if (withSpan(content).startsWithLineBreak()) {
        expect(LambdaBody, content)
    } else {
        expect(InlineImplicitLambdaLiteral, span)
    }
    
    if (close == null) {
        expectAt(if (withSpan(content).startsWithLineBreak()) BraceClose else RBrace, span.end)
    }
}

private fun ExpectationReceiver.repairInlineImplicitLambdaLiteral() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val open = receiver.firstInside(LBrace)
    val close = receiver.lastLambdaClose()
    
    if (open == null) expectAt(LBrace, span.start)
    if (close == null) expectAt(RBrace, span.end)
    
    val body = trimOuterLineBreaks(SourceSpan(open?.span?.end ?: span.start, close?.span?.start ?: span.end))
    if (body.isNotEmpty()) expect(Statement, body)
}

private fun ExpectationReceiver.repairLambdaBody() {
    val span = currentSpan()
    if (span.isEmpty()) {
        expect(Statement, span)
        return
    }
    
    if (startsWithLineBreak()) {
        val afterLineBreak = trimOuterLineBreaks(SourceSpan(span.start + 1, span.end))
        if (afterLineBreak.isNotEmpty() && withSpan(afterLineBreak).containsBuildable(LineBreak)) {
            expect(LambdaStatementBlockHead, span)
        } else if (afterLineBreak.isNotEmpty()) {
            expect(Statement, afterLineBreak)
        } else {
            expect(LambdaStatementBlockHead, span)
        }
    } else {
        val trimmed = trimOuterLineBreaks(span)
        if (trimmed.isNotEmpty()) expect(Statement, trimmed)
    }
}

private fun ExpectationReceiver.repairLambdaStatementBlockHead() {
    val span = currentSpan()
    if (!startsWithLineBreak()) expectAt(LineBreak, span.start)
    
    val contentStart = if (startsWithLineBreak()) span.start + 1 else span.start
    withSpan(contentStart, span.end).repairLineItems(StatementLine)
}

private fun ExpectationReceiver.repairBlock() {
    val label = firstInside(Label)
    val hash = if (label == null) firstInside(Hash) else null
    val block = firstDelimited(BraceOpen, BraceClose)
    if (label != null || hash != null) {
        val labelSpan = label?.span ?: checkNotNull(hash).span
        expect(Label, labelSpan)
        if (block != null) {
            expect(StatementBlockCore, block)
        } else {
            expectAt(StatementBlockCore, labelSpan.end)
        }
        return
    }
    
    if (block != null) {
        expect(StatementBlockCore, block)
    } else {
        expect(StatementBlockCore)
    }
}

private fun ExpectationReceiver.repairStatementBlock() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val open = receiver.firstInsideLongest(BraceOpen)
    val close = receiver.lastInside(RBrace)
    if (open == null) expectAt(BraceOpen, span.start)
    if (close == null) expectAt(BraceClose, span.end)
    
    val body = SourceSpan(open?.span?.end ?: span.start, close?.span?.start ?: span.end)
    withSpan(body).repairLineItems(StatementLine)
}

private fun ExpectationReceiver.repairStatementLine() {
    val lineBreak = matchesEndingAt(LineBreak, currentSpan().end).firstOrNull()
    if (lineBreak != null) {
        val statement = trimOuterLineBreaks(SourceSpan(currentSpan().start, lineBreak.span.start))
        if (statement.isNotEmpty()) expect(Statement, statement)
    } else {
        val statement = trimOuterLineBreaks(currentSpan())
        if (statement.isNotEmpty()) expect(Statement, statement)
        expectAt(LineBreak, currentSpan().end)
    }
}

private fun ExpectationReceiver.repairStatement() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    if (receiver.firstInside(Hash)?.span?.start == span.start) {
        receiver.repairLabelledStatement()
        return
    }
    when (FoxKeywordsByText[receiver.firstPlainText()]) {
        KwIf -> expect(IfCore)
        KwWhile -> expect(WhileCore)
        KwDo -> expect(DoWhileCore)
        KwWhen -> expect(WhenCore)
        KwReturn -> repairKeywordValueStatement(KwReturn)
        KwYield -> repairYieldStatement()
        KwBreak -> repairOptionalLabelStatement(KwBreak)
        KwContinue -> repairOptionalLabelStatement(KwContinue)
        else -> {
            if (receiver.containsBuildable(Label)) {
                receiver.repairLabelledStatement()
            } else if (receiver.looksLikeTypeBindingStatement()) {
                receiver.repairTypeBindingStatement()
            } else {
                val operator = receiver.firstAssignmentOperator()
                if (operator != null) {
                    val left = trimOuterLineBreaks(SourceSpan(span.start, operator.span.start))
                    val right = trimOuterLineBreaks(SourceSpan(operator.span.end, span.end))
                    if (left.isNotEmpty()) expect(PostfixExpression, left) else expectAt(PostfixExpression, operator.span.start)
                    if (right.isNotEmpty()) expect(Statement, right) else expectAt(Statement, operator.span.end)
                } else if (receiver.startsWithParenOpen() && receiver.containsBuildable(ParenClose)) {
                    receiver.firstDelimited(ParenOpen, ParenClose)?.let { expect(ParenthesizedExpression, it) }
                } else {
                    repairAssignmentExpression()
                }
            }
        }
    }
}

private fun ExpectationReceiver.repairPrimaryExpression() {
    val span = trimOuterLineBreaks(currentSpan())
    if (span.isEmpty()) {
        expectAt(Identifier, span.start)
        return
    }
    
    val receiver = withSpan(span)
    when (FoxKeywordsByText[receiver.firstPlainText()]) {
        KwThis -> expect(KwThis, span)
        KwLowerUnit -> expect(LitUnit, span)
        KwTrue, KwFalse -> expect(LitBool, span)
        else -> when {
            receiver.startsWithBraceOpen() -> expect(ExplicitLambdaLiteral, span)
            receiver.startsWithFormattedString() -> {
                if (receiver.lastInside(FormattedStringEnd) == null) expectAt(FormattedStringEnd, span.end)
            }
            receiver.containsBuildable(ParenOpen) || receiver.containsBuildable(ParenClose) -> {
                expect(ParenthesizedExpression, span)
            }
            receiver.containsBuildable(LitInt) -> expect(LitInt, span)
            receiver.containsBuildable(LitLong) -> expect(LitLong, span)
            receiver.containsBuildable(LitFloat) -> expect(LitFloat, span)
            receiver.containsBuildable(LitDouble) -> expect(LitDouble, span)
            receiver.containsBuildable(LitChar) -> expect(LitChar, span)
            receiver.containsBuildable(LitString) -> expect(LitString, span)
            receiver.containsBuildable(Identifier) -> expect(Identifier, span)
            else -> expectAt(Identifier, span.start)
        }
    }
}

private fun ExpectationReceiver.repairPostfixExpression() {
    val span = trimOuterLineBreaks(currentSpan())
    if (span.isEmpty()) {
        expectAt(PrimaryExpression, span.start)
        return
    }
    
    val receiver = withSpan(span)
    val dot = receiver.topLevelMatches(Dot).maxByOrNull { it.span.start.fragIndex }
    val index = receiver.postfixIndexAccessSpan()
    if (index != null && (dot == null || dot.span.start < index.open.start)) {
        receiver.repairIndexedPostfixExpression(index)
        return
    }
    
    if (dot != null) {
        receiver.repairDottedPostfixExpression(dot)
        return
    }
    
    val parameters = receiver.postfixActualParameterListSpan()
    if (parameters != null) {
        val callee = trimOuterLineBreaks(SourceSpan(span.start, parameters.start))
        if (callee.isNotEmpty()) {
            withSpan(callee).repairCallablePrefix()
        } else {
            expectAt(Identifier, parameters.start)
        }
        expect(ActualParameterList, parameters)
        receiver.trailingLambdaSpan(SourceSpan(parameters.end, span.end))?.let { expect(LambdaLiteral, it) }
        return
    }
    
    val lambda = receiver.trailingLambdaSpan(span)
    if (lambda != null && span.start < lambda.start) {
        val callee = trimOuterLineBreaks(SourceSpan(span.start, lambda.start))
        if (callee.isNotEmpty()) withSpan(callee).repairCallablePrefix()
        expect(LambdaLiteral, lambda)
        return
    }
    
    val genericArguments = receiver.postfixActualGenericParameterListSpan()
    if (genericArguments != null) {
        val name = trimOuterLineBreaks(SourceSpan(span.start, genericArguments.start))
        if (name.isNotEmpty()) expect(Identifier, name) else expectAt(Identifier, genericArguments.start)
        expect(ActualGenericParameterList, genericArguments)
        expectAt(ActualParameterList, genericArguments.end)
        return
    }
    
    expect(PrimaryExpression, span)
}

private fun ExpectationReceiver.repairIndexedPostfixExpression(index: PostfixIndexSpan) {
    val span = trimOuterLineBreaks(currentSpan())
    val target = trimOuterLineBreaks(SourceSpan(span.start, index.open.start))
    if (target.isNotEmpty()) {
        expect(PostfixExpression, target)
    } else {
        expectAt(PostfixExpression, index.open.start)
    }
    
    val indexEnd = index.close?.end ?: span.end
    expect(IndexArgumentList, SourceSpan(index.open.start, indexEnd))
}

private fun ExpectationReceiver.repairDottedPostfixExpression(dot: ParseMatch<*>) {
    val span = trimOuterLineBreaks(currentSpan())
    val left = trimOuterLineBreaks(SourceSpan(span.start, dot.span.start))
    val suffix = trimOuterLineBreaks(SourceSpan(dot.span.end, span.end))
    
    if (left.isNotEmpty()) {
        expect(PostfixExpression, left)
    } else {
        expectAt(PostfixExpression, dot.span.start)
    }
    
    if (suffix.isEmpty()) {
        expectAt(Identifier, dot.span.end)
        return
    }
    
    val receiver = withSpan(suffix)
    val parameters = receiver.postfixActualParameterListSpan()
    if (parameters != null) {
        val member = trimOuterLineBreaks(SourceSpan(suffix.start, parameters.start))
        receiver.repairPostfixMember(member)
        expect(ActualParameterList, parameters)
        receiver.trailingLambdaSpan(SourceSpan(parameters.end, suffix.end))?.let { expect(LambdaLiteral, it) }
        return
    }
    
    val lambda = receiver.trailingLambdaSpan(suffix)
    if (lambda != null && suffix.start < lambda.start) {
        val member = trimOuterLineBreaks(SourceSpan(suffix.start, lambda.start))
        receiver.repairPostfixMember(member)
        expect(LambdaLiteral, lambda)
        return
    }
    
    receiver.repairPostfixMember(suffix)
}

private fun ExpectationReceiver.repairPostfixMember(member: SourceSpan) {
    val span = trimOuterLineBreaks(member)
    if (span.isEmpty()) {
        expectAt(Identifier, span.start)
        return
    }
    
    val receiver = withSpan(span)
    if (receiver.startsWithParenOpen()) {
        expect(ParenthesizedExpression, span)
        return
    }
    
    val genericArguments = receiver.postfixActualGenericParameterListSpan()
    if (genericArguments != null) {
        val name = trimOuterLineBreaks(SourceSpan(span.start, genericArguments.start))
        if (name.isNotEmpty()) expect(Identifier, name) else expectAt(Identifier, genericArguments.start)
        expect(ActualGenericParameterList, genericArguments)
        return
    }
    
    if (receiver.containsBuildable(LitInt) && !receiver.containsBuildable(Identifier)) {
        expect(LitInt, span)
    } else {
        expect(Identifier, span)
    }
}

private fun ExpectationReceiver.repairCallablePrefix() {
    val span = trimOuterLineBreaks(currentSpan())
    if (span.isEmpty()) {
        expectAt(Identifier, span.start)
        return
    }
    
    val receiver = withSpan(span)
    val genericArguments = receiver.postfixActualGenericParameterListSpan()
    if (genericArguments != null && receiver.containsBuildable(Identifier)) {
        val name = trimOuterLineBreaks(SourceSpan(span.start, genericArguments.start))
        if (name.isNotEmpty()) expect(Identifier, name) else expectAt(Identifier, genericArguments.start)
        expect(ActualGenericParameterList, genericArguments)
        return
    }
    
    when {
        receiver.startsWithParenOpen() -> expect(ParenthesizedExpression, span)
        receiver.containsBuildable(TypeName) || receiver.startsWithTypeKeyword() -> expect(Type, span)
        receiver.containsBuildable(Identifier) -> expect(Identifier, span)
        else -> expect(PrimaryExpression, span)
    }
}

private fun ExpectationReceiver.repairUnaryExpression() {
    val span = trimOuterLineBreaks(currentSpan())
    val operator = withSpan(span).firstInside(UnaryOperator)
    if (operator != null && operator.span.start == span.start) {
        expect(UnaryOperator, operator.span)
        val operand = trimOuterLineBreaks(SourceSpan(operator.span.end, span.end))
        if (operand.isNotEmpty()) {
            expect(UnaryExpression, operand)
        } else {
            expectAt(UnaryExpression, operator.span.end)
        }
        return
    }
    
    if (span.isNotEmpty()) {
        expect(PostfixExpression, span)
    } else {
        expectAt(PostfixExpression, span.start)
    }
}

private fun ExpectationReceiver.repairAssignmentExpression() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val operator = receiver.firstAssignmentOperator()
    if (operator == null) {
        if (span.isNotEmpty()) {
            expect(LogicalOrExpression, span)
        } else {
            expectAt(LogicalOrExpression, span.start)
        }
        return
    }
    
    val left = trimOuterLineBreaks(SourceSpan(span.start, operator.span.start))
    val right = trimOuterLineBreaks(SourceSpan(operator.span.end, span.end))
    if (left.isNotEmpty()) {
        expect(PostfixExpression, left)
    } else {
        expectAt(PostfixExpression, operator.span.start)
    }
    if (right.isNotEmpty()) {
        expect(Statement, right)
    } else {
        expectAt(Statement, operator.span.end)
    }
}

private fun ExpectationReceiver.repairBinaryExpression(
    self: GrammarSymbol<*>,
    operand: GrammarSymbol<*>,
    operator: GrammarSymbol<*>,
) {
    val span = trimOuterLineBreaks(currentSpan())
    if (span.isEmpty()) {
        expectAt(operand, span.start)
        return
    }
    
    val receiver = withSpan(span)
    val operators = receiver.topLevelBinaryOperators(operator, operand)
    if (operators.isEmpty()) {
        expect(operand, span)
        return
    }
    
    operators.forEach { operatorMatch ->
        val left = trimOuterLineBreaks(SourceSpan(span.start, operatorMatch.span.start))
        val right = trimOuterLineBreaks(SourceSpan(operatorMatch.span.end, span.end))
        if (left.isNotEmpty()) {
            expect(self, left)
        } else {
            expectAt(operand, operatorMatch.span.start)
        }
        if (right.isNotEmpty()) {
            expect(operand, right)
        } else {
            expectAt(operand, operatorMatch.span.end)
        }
    }
}

private fun ExpectationReceiver.looksLikeTypeBindingStatement(): Boolean {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val identifier = receiver.firstInside(Identifier)
    val colon = receiver.firstInside(Colon)
    
    if (colon != null) {
        return identifier == null || identifier.span.end <= colon.span.start
    }
    
    if (identifier == null) return false
    return receiver.firstTypeAfter(identifier.span.end) != null
}

private fun ExpectationReceiver.repairTypeBindingStatement() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val identifier = receiver.firstInside(Identifier)
    val colon = receiver.firstInside(Colon)
    
    if (identifier != null && (colon == null || identifier.span.end <= colon.span.start)) {
        expect(Identifier, identifier.span)
    } else {
        expectAt(Identifier, colon?.span?.start ?: span.start)
    }
    
    if (colon != null) {
        val type = trimOuterLineBreaks(SourceSpan(colon.span.end, span.end))
        if (type.isNotEmpty()) {
            expect(Type, type)
        } else {
            expectAt(Type, colon.span.end)
        }
        return
    }
    
    val type = identifier?.let { receiver.firstTypeAfter(it.span.end) }
    val colonPosition = type?.span?.start ?: identifier?.span?.end ?: span.start
    expectAt(Colon, colonPosition)
    
    val typeSpan = if (type != null) {
        trimOuterLineBreaks(SourceSpan(type.span.start, span.end))
    } else {
        trimOuterLineBreaks(SourceSpan(identifier?.span?.end ?: span.start, span.end))
    }
    if (typeSpan.isNotEmpty()) {
        expect(Type, typeSpan)
    } else {
        expectAt(Type, colonPosition)
    }
}

private fun ExpectationReceiver.repairLabelledStatement() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val labelCandidate = receiver.firstInside(Label)
    val hash = receiver.firstInside(Hash)
    val targetAfterHash = hash?.let { trimOuterLineBreaks(SourceSpan(it.span.end, span.end)) }
    val missingLabelBeforeTarget = targetAfterHash != null && isLabelledStatementTarget(targetAfterHash)
    val label = if (missingLabelBeforeTarget) null else labelCandidate
    
    val labelSpan = label?.span ?: hash?.span ?: SourceSpan(span.start, span.start)
    expect(Label, labelSpan)
    
    val targetStart = label?.span?.end ?: hash?.span?.end ?: span.start
    val target = trimOuterLineBreaks(SourceSpan(targetStart, span.end))
    if (target.isEmpty()) {
        expectAt(IfCore, span.end)
        return
    }
    
    when (withSpan(target).firstPlainText()) {
        "if" -> expect(IfCore, target)
        "while" -> expect(WhileCore, target)
        "do" -> expect(DoWhileCore, target)
        "when" -> expect(WhenCore, target)
        "{" -> expect(StatementBlock, span)
        else -> expect(StatementBlock, span)
    }
}

private fun ExpectationReceiver.repairKeywordValueStatement(keyword: FoxGrammarSymbol<*>) {
    val match = firstInside(keyword)
    val value = trimOuterLineBreaks(SourceSpan(match?.span?.end ?: currentSpan().start, currentSpan().end))
    if (value.isNotEmpty()) expect(Statement, value)
}

private fun ExpectationReceiver.repairYieldStatement() {
    val span = trimOuterLineBreaks(currentSpan())
    val match = withSpan(span).firstInside(KwYield)
    val tail = trimOuterLineBreaks(SourceSpan(match?.span?.end ?: span.start, span.end))
    if (tail.isEmpty()) return
    
    val label = withSpan(tail).firstInside(Label)
    val hash = withSpan(tail).firstInside(Hash)
    if (label != null || hash != null) {
        val labelSpan = label?.span ?: hash?.span ?: tail
        expect(Label, labelSpan)
        val value = trimOuterLineBreaks(SourceSpan(labelSpan.end, tail.end))
        if (value.isNotEmpty()) expect(Statement, value)
    } else {
        expect(Statement, tail)
    }
}

private fun ExpectationReceiver.repairOptionalLabelStatement(keyword: FoxGrammarSymbol<*>) {
    val match = firstInside(keyword)
    val label = trimOuterLineBreaks(SourceSpan(match?.span?.end ?: currentSpan().start, currentSpan().end))
    if (label.isNotEmpty()) expect(Label, label)
}

private fun ExpectationReceiver.repairParenthesizedStatement() {
    val grouped = firstDelimited(ParenOpen, ParenClose)
    if (grouped != null) {
        val open = withSpan(grouped).firstInside(ParenOpen) ?: return
        val close = withSpan(grouped).lastInside(ParenClose) ?: return
        val inner = trimOuterLineBreaks(SourceSpan(open.span.end, close.span.start))
        if (inner.isNotEmpty()) expect(Statement, inner)
    } else {
        expectAt(ParenOpen, currentSpan().start)
        expectAt(ParenClose, currentSpan().end)
        val inner = trimOuterLineBreaks(currentSpan())
        if (inner.isNotEmpty()) expect(Statement, inner)
    }
}

private fun ExpectationReceiver.repairIfCore() {
    val span = trimOuterLineBreaks(currentSpan())
    val keywordMatch = withSpan(span).firstInside(KwIf)
    val condition = withSpan(span).firstDelimited(ParenOpen, ParenClose)
    val elseMatch = withSpan(span).lastInside(KwElse)
    
    if (condition == null) {
        val conditionSpan = trimOuterLineBreaks(SourceSpan(keywordMatch?.span?.end ?: span.start, elseMatch?.span?.start ?: span.end))
        if (conditionSpan.isNotEmpty()) {
            expect(ParenthesizedExpression, conditionSpan)
        } else {
            expectAt(ParenthesizedExpression, keywordMatch?.span?.end ?: span.start)
        }
    } else {
        expect(ParenthesizedExpression, condition)
        val thenEnd = elseMatch?.span?.start ?: span.end
        val thenBody = trimOuterLineBreaks(SourceSpan(condition.end, thenEnd))
        if (thenBody.isNotEmpty()) {
            expect(Statement, thenBody)
        } else {
            expectAt(Statement, condition.end)
        }
    }
    
    if (elseMatch != null) {
        val elseBody = trimOuterLineBreaks(SourceSpan(elseMatch.span.end, span.end))
        if (elseBody.isNotEmpty()) {
            expect(Statement, elseBody)
        } else {
            expectAt(Statement, elseMatch.span.end)
        }
    }
}

private fun ExpectationReceiver.repairWhileCore() {
    repairKeywordConditionBody(KwWhile)
}

private fun ExpectationReceiver.repairDoWhileCore() {
    val span = trimOuterLineBreaks(currentSpan())
    val doMatch = withSpan(span).firstInside(KwDo)
    val whileMatch = withSpan(span).lastInside(KwWhile)
    if (whileMatch != null) {
        val body = trimOuterLineBreaks(SourceSpan(doMatch?.span?.end ?: span.start, whileMatch.span.start))
        val condition = trimOuterLineBreaks(SourceSpan(whileMatch.span.end, span.end))
        if (body.isNotEmpty()) {
            expect(Statement, body)
        } else {
            expectAt(Statement, doMatch?.span?.end ?: span.start)
        }
        if (condition.isNotEmpty()) {
            expect(ParenthesizedExpression, condition)
        } else {
            expectAt(ParenthesizedExpression, whileMatch.span.end)
        }
        return
    }
    
    val body = trimOuterLineBreaks(SourceSpan(doMatch?.span?.end ?: span.start, span.end))
    if (body.isNotEmpty()) {
        expect(Statement, body)
    } else {
        expectAt(Statement, doMatch?.span?.end ?: span.start)
    }
    expectAt(KwWhile, span.end)
    expectAt(ParenthesizedExpression, span.end)
}

private fun ExpectationReceiver.repairWhenCore() {
    val cases = lastDelimited(BraceOpen, BraceClose)
    if (cases != null) {
        expect(WhenCaseList, cases)
        val beforeCases = trimOuterLineBreaks(SourceSpan(currentSpan().start, cases.start))
        withSpan(beforeCases).firstDelimited(ParenOpen, ParenClose)?.let { expect(ParenthesizedExpression, it) }
    } else {
        expectAt(WhenCaseList, currentSpan().end)
    }
}

private fun ExpectationReceiver.repairWhenCaseList() {
    val block = firstDelimited(BraceOpen, BraceClose)
    if (block == null) {
        expectAt(BraceOpen, currentSpan().start)
        expectAt(BraceClose, currentSpan().end)
        return
    }
    val open = withSpan(block).firstInsideLongest(BraceOpen) ?: return
    val close = withSpan(block).lastInside(RBrace) ?: return
    val body = SourceSpan(open.span.end, close.span.start)
    withSpan(body).repairLineItems(WhenCaseLine)
}

private fun ExpectationReceiver.repairWhenCaseLine() {
    val lineBreak = matchesEndingAt(LineBreak, currentSpan().end).firstOrNull()
    val caseSpan = trimOuterLineBreaks(SourceSpan(currentSpan().start, lineBreak?.span?.start ?: currentSpan().end))
    if (caseSpan.isNotEmpty()) expect(WhenCase, caseSpan)
    if (lineBreak == null) expectAt(LineBreak, currentSpan().end)
}

private fun ExpectationReceiver.repairWhenCase() {
    val arrow = firstInside(Arrow)
    if (arrow != null) {
        val conditions = trimOuterLineBreaks(SourceSpan(currentSpan().start, arrow.span.end))
        val body = trimOuterLineBreaks(SourceSpan(arrow.span.end, currentSpan().end))
        if (conditions.isNotEmpty()) expect(WhenCaseConditionList, conditions)
        if (body.isNotEmpty()) {
            expect(Statement, body)
        } else {
            expectAt(Statement, arrow.span.end)
        }
    } else if (firstPlainText() == "else") {
        expectAt(Arrow, firstInside(KwElse)?.span?.end ?: currentSpan().start)
    } else {
        expectAt(Arrow, currentSpan().end)
        expect(WhenCaseConditionList)
    }
}

private fun ExpectationReceiver.repairWhenCaseConditionList() {
    val arrow = lastInside(Arrow)
    val conditions = trimOuterLineBreaks(SourceSpan(currentSpan().start, arrow?.span?.start ?: currentSpan().end))
    if (conditions.isEmpty()) return
    val statements = withSpan(conditions).matchesInside(Statement).nonOverlapping()
    if (statements.isEmpty()) {
        expect(Statement, conditions)
        return
    }
    statements.zipWithNext().forEach { (left, right) ->
        val gap = trimOuterLineBreaks(SourceSpan(left.span.end, right.span.start))
        if (withSpan(gap).matchesInside(Comma).isEmpty()) expect(Comma, gap)
    }
}

private fun ExpectationReceiver.repairKeywordConditionBody(keyword: FoxGrammarSymbol<*>) {
    val keywordMatch = firstInside(keyword)
    val condition = firstDelimited(ParenOpen, ParenClose)
    if (condition != null) {
        expect(ParenthesizedExpression, condition)
        val body = trimOuterLineBreaks(SourceSpan(condition.end, currentSpan().end))
        if (body.isNotEmpty()) {
            expect(Statement, body)
        } else {
            expectAt(Statement, condition.end)
        }
    } else {
        val conditionSpan = trimOuterLineBreaks(SourceSpan(keywordMatch?.span?.end ?: currentSpan().start, currentSpan().end))
        if (conditionSpan.isNotEmpty()) {
            expect(ParenthesizedExpression, conditionSpan)
        } else {
            expectAt(ParenthesizedExpression, keywordMatch?.span?.end ?: currentSpan().start)
        }
    }
}

private fun ExpectationReceiver.firstAssignmentOperator(): ParseMatch<*>? {
    return matchesInside(AssignOperator)
        .filter { isTopLevelSourcePosition(it.span.start) }
        .minWithOrNull(
            compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenByDescending { it.span.length },
        )
}

private fun ExpectationReceiver.topLevelBinaryOperators(
    operator: GrammarSymbol<*>,
    operand: GrammarSymbol<*>,
): List<ParseMatch<*>> {
    val operands = matchesInside(operand)
    return matchesInside(operator)
        .filter { candidate -> operands.none { operandMatch -> operandMatch.span.contains(candidate.span) } }
        .nonOverlapping()
}

private fun ExpectationReceiver.topLevelMatches(symbol: GrammarSymbol<*>): List<ParseMatch<*>> {
    val nested = (FoxExpressionSymbol.entries + listOf(
        Type,
        ActualParameterList,
        ActualGenericParameterList,
        LambdaLiteral,
    )).flatMap { matchesInside(it) }
    return matchesInside(symbol)
        .filter { candidate -> nested.none { match -> match.span != candidate.span && match.span.contains(candidate.span) } }
        .nonOverlapping()
}

private fun ExpectationReceiver.postfixActualParameterListSpan(): SourceSpan? {
    sourceDelimitedSpan("(", ")")?.let { return it }
    return lastDelimited(ParenOpen, ParenClose)
        ?: firstCallOpenWithoutClose(ParenOpen)
        ?: matchesInside(ActualParameterList).maxByOrNull { it.span.end.fragIndex }?.span
}

private fun ExpectationReceiver.postfixActualGenericParameterListSpan(): SourceSpan? {
    matchesInside(ActualGenericParameterList).maxByOrNull { it.span.end.fragIndex }?.let { return it.span }
    return lastDelimited(AngleOpen, AngleClose) ?: firstCallOpenWithoutClose(AngleOpen)
}

private fun ExpectationReceiver.postfixIndexAccessSpan(): PostfixIndexSpan? {
    val open = matchesInside(SquareOpen)
        .filter { it.span.start > currentSpan().start && isTopLevelSourcePosition(it.span.start) }
        .maxByOrNull { it.span.start.fragIndex }
        ?.span
        ?: return null
    
    var depth = 0
    var cursor = open.start
    while (cursor < currentSpan().end) {
        when ((source()[cursor] as? PlainFragment)?.text) {
            "[" -> depth += 1
            "]" -> {
                depth -= 1
                if (depth == 0) return PostfixIndexSpan(open, SourceSpan(cursor, cursor + 1))
            }
        }
        cursor += 1
    }
    return PostfixIndexSpan(open, null)
}

private fun ExpectationReceiver.sourceDelimitedSpan(open: String, close: String): SourceSpan? {
    val openSpan = firstPlainTextSpanAfterStart(open) ?: return null
    var depth = 0
    var cursor = openSpan.start
    while (cursor < currentSpan().end) {
        when ((source()[cursor] as? PlainFragment)?.text) {
            open -> depth += 1
            close -> {
                depth -= 1
                if (depth == 0) return SourceSpan(openSpan.start, cursor + 1)
            }
        }
        cursor += 1
    }
    return SourceSpan(openSpan.start, currentSpan().end)
}

private fun ExpectationReceiver.firstCallOpenWithoutClose(open: GrammarSymbol<*>): SourceSpan? {
    val openMatch = matchesInside(open)
        .filter { candidate -> candidate.span.start > currentSpan().start }
        .minWithOrNull(compareByDescending<ParseMatch<*>> { it.span.start.fragIndex }.thenBy { it.span.length })
        ?: return null
    return SourceSpan(openMatch.span.start, currentSpan().end)
}

private fun ExpectationReceiver.trailingLambdaSpan(span: SourceSpan): SourceSpan? {
    val trimmed = trimOuterLineBreaks(span)
    if (trimmed.isEmpty()) return null
    val receiver = withSpan(trimmed)
    return receiver.matchesInside(LambdaLiteral).maxByOrNull { it.span.end.fragIndex }?.span
        ?: receiver.firstDelimited(BraceOpen, BraceClose)
        ?: if (receiver.startsWithBraceOpen()) trimmed else null
}

private fun ExpectationReceiver.firstTypeAfter(start: SourcePosition): ParseMatch<*>? {
    return (matchesInside(Type) + matchesInside(TypeName))
        .filter { start <= it.span.start }
        .minWithOrNull(compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenByDescending { it.span.length })
}

private fun ExpectationReceiver.isLabelledStatementTarget(span: SourceSpan): Boolean {
    if (span.isEmpty()) return false
    return when (withSpan(span).firstPlainText()) {
        "if", "while", "do", "when", "{" -> true
        else -> false
    }
}

private fun ExpectationReceiver.isTopLevelSourcePosition(position: SourcePosition): Boolean {
    var depth = 0
    var cursor = currentSpan().start
    while (cursor < position && cursor < currentSpan().end) {
        when ((source()[cursor] as? PlainFragment)?.text) {
            "(", "[", "{", "<" -> depth += 1
            ")", "]", "}", ">" -> if (depth > 0) depth -= 1
        }
        cursor += 1
    }
    return depth == 0
}

private fun ExpectationReceiver.trimOuterLineBreaks(span: SourceSpan): SourceSpan {
    return trimOuterFragments(span) { it is LineBreakFragment }
}

private fun ExpectationReceiver.firstPlainText(): String? {
    var cursor = currentSpan().start
    while (cursor < currentSpan().end) {
        val fragment = source()[cursor]
        if (fragment is PlainFragment) return fragment.text
        cursor += 1
    }
    return null
}

private fun ExpectationReceiver.firstPlainTextSpanAfterStart(text: String): SourceSpan? {
    var cursor = currentSpan().start + 1
    while (cursor < currentSpan().end) {
        val fragment = source()[cursor]
        if (fragment is PlainFragment && fragment.text == text) return SourceSpan(cursor, cursor + 1)
        cursor += 1
    }
    return null
}

private fun ExpectationReceiver.firstTopLevelPlainTextSpan(text: String): SourceSpan? {
    var cursor = currentSpan().start
    while (cursor < currentSpan().end) {
        val fragment = source()[cursor]
        if (fragment is PlainFragment && fragment.text == text && isTopLevelSourcePosition(cursor)) {
            return SourceSpan(cursor, cursor + 1)
        }
        cursor += 1
    }
    return null
}

private fun ExpectationReceiver.lastPlainTextSpan(text: String): SourceSpan? {
    var cursor = currentSpan().end
    while (currentSpan().start < cursor) {
        cursor -= 1
        val fragment = source()[cursor]
        if (fragment is PlainFragment && fragment.text == text) return SourceSpan(cursor, cursor + 1)
    }
    return null
}

private fun ExpectationReceiver.lastLambdaClose(): ParseMatch<*>? {
    val close = lastInside(BraceClose)
    val tokenClose = lastInside(RBrace)
    return listOfNotNull(close, tokenClose).maxByOrNull { it.span.end.fragIndex }
}

private fun ExpectationReceiver.startsWithLineBreak(): Boolean {
    return startsWithFragment { it is LineBreakFragment }
}

private fun ExpectationReceiver.startsWithParenOpen(): Boolean {
    val span = trimOuterLineBreaks(currentSpan())
    return withSpan(span).firstInside(ParenOpen)?.span?.start == span.start
}

private fun ExpectationReceiver.startsWithBraceOpen(): Boolean {
    val span = trimOuterLineBreaks(currentSpan())
    return withSpan(span).firstInside(BraceOpen)?.span?.start == span.start
}

private fun ExpectationReceiver.startsWithFormattedString(): Boolean {
    val span = trimOuterLineBreaks(currentSpan())
    return withSpan(span).firstInside(FormattedStringStart)?.span?.start == span.start
}

private fun ExpectationReceiver.startsWithArgumentClose(): Boolean {
    val span = trimOuterLineBreaks(currentSpan())
    return ((source().getOrNull(span.start) as? PlainFragment)?.text == ")") ||
        withSpan(span).firstInside(ParenClose)?.span?.start == span.start ||
        withSpan(span).firstInside(RParen)?.span?.start == span.start
}

private fun ExpectationReceiver.startsWithTypeKeyword(): Boolean {
    val keyword = FoxKeywordsByText[firstPlainText()] ?: return false
    return keyword in FoxTypeArgumentLists.keys
        || keyword in FoxFixedArityTypeArguments.keys
        || keyword is FoxPrimitiveTypeKeywordSymbol
        || keyword is FoxSyntheticTypeKeywordSymbol
}

private fun ExpectationReceiver.repairLineItems(item: GrammarSymbol<*>) {
    repairLineSeparatedItems(
        item = item,
        isLineBreak = { it is LineBreakFragment },
        continuationSymbols = FoxLineContinuationSymbols,
        trim = { trimOuterLineBreaks(it) },
    )
}
