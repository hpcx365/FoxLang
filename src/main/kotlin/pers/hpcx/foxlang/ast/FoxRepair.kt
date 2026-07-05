package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.frontend.*

val FoxRepairStrategy = RepairStrategy(
    mapOf(
        node<FoxFile>() to { expect(FileElementList) },
        FileElementList to { repairFileElementList() },
        FileElementLine to { repairFileElementLine() },
        node<FoxFileElement>() to { repairFileElement() },
        node<FoxTypeAlias>() to { repairTypeAlias() },
        node<FoxMethodDefinition>() to { repairMethodDefinition() },
        MethodHead to { repairMethodHead() },
        ReturnTypeClause to { repairReturnTypeClause() },
        
        FormalParameterList to { repairDelimitedCommaList(ParenOpen, ParenClose, FormalParameter) },
        ActualParameterList to { repairDelimitedCommaList(ParenOpen, ParenClose, ActualParameter) },
        FormalGenericParameterList to { repairDelimitedCommaList(AngleOpen, AngleClose, FormalGenericParameter) },
        FormalGenericParameterNoConstraintsList to {
            repairDelimitedCommaList(AngleOpen, AngleClose, FormalGenericParameterNoConstraints)
        },
        ActualGenericParameterList to { repairDelimitedCommaList(AngleOpen, AngleClose, ActualGenericParameter) },
        NamedActualGenericParameterList to { repairDelimitedCommaList(AngleOpen, AngleClose, NamedActualGenericParameter) },
        AnonymousActualGenericParameterList to { repairDelimitedCommaList(AngleOpen, AngleClose, AnonymousActualGenericParameter) },
        TupleComponentParameterList to { repairDelimitedCommaList(AngleOpen, AngleClose, TupleComponentParameter) },
        StructFieldParameterList to { repairDelimitedCommaList(AngleOpen, AngleClose, StructFieldParameter) },
        ObjectMemberParameterList to { repairDelimitedCommaList(AngleOpen, AngleClose, ObjectMemberParameter) },
        EnumItemParameterList to { repairDelimitedCommaList(AngleOpen, AngleClose, EnumItemParameter) },
        MethodTypeArgumentList to { repairDelimitedCommaList(AngleOpen, AngleClose, MethodTypeArgument) },
        
        FormalParameter to { repairColonTypedItem(IdentifierColon) },
        LambdaParameter to { repairOptionalColonTypedItem(Identifier, node<FoxType>()) },
        FormalGenericParameter to { repairEqualTypedItem(TypeName, node<FoxType>()) },
        ActualGenericParameter to { repairOptionalEqualTypedItem(TypeName, node<FoxType>()) },
        NamedActualGenericParameter to { repairEqualTypedItem(TypeName, node<FoxType>()) },
        TupleComponentParameter to { repairTupleComponentParameter() },
        StructFieldParameter to { repairColonTypedItem(IdentifierColon) },
        ObjectMemberParameter to { repairColonTypedItem(IdentifierColon) },
        EnumItemParameter to { repairEqualTypedItem(TypeName, node<FoxType>()) },
        MethodTypeArgument to { repairMethodTypeArgument() },
        
        IdentifierColon to { repairSymbolThenToken(Identifier, token(":")) },
        TypeNameColon to { repairSymbolThenToken(TypeName, token(":")) },
        IdentifierEqual to { repairSymbolThenToken(Identifier, token("=")) },
        TypeNameEqual to { repairSymbolThenToken(TypeName, token("=")) },
        
        node<FoxBlock>() to { repairBlock() },
        StatementBlock to { repairStatementBlock() },
        StatementLine to { repairStatementLine() },
        node<FoxStatement>() to { repairStatement() },
        ParenthesizedStatement to { repairParenthesizedStatement() },
        ControlBody to { repairControlBody() },
        IfCore to { repairIfCore() },
        WhileCore to { repairWhileCore() },
        DoWhileCore to { repairDoWhileCore() },
        WhenCore to { repairWhenCore() },
        WhenCaseList to { repairWhenCaseList() },
        WhenCaseLine to { repairWhenCaseLine() },
        WhenCase to { repairWhenCase() },
        WhenCaseConditionList to { repairWhenCaseConditionList() },
    ),
)

fun ParseAnalysis<FoxFile>.repairFox(): ParseAnalysis<FoxFile> {
    context.repair(Expectation(start, source.span), FoxRepairStrategy)
    return this
}

private const val MaxListFrames = 8

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
    val starts = (matchesInside(token("def")) + matchesInside(token("type")))
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
        if (elementSpan.isNotEmpty()) expect(node<FoxFileElement>(), elementSpan)
        return
    }
    
    val span = trimOuterLineBreaks(currentSpan())
    if (span.isNotEmpty()) expect(node<FoxFileElement>(), span)
    expectAt(LineBreak, currentSpan().end)
}

private fun ExpectationReceiver.repairFileElement() {
    when (firstPlainText()) {
        "def" -> expect(node<FoxMethodDefinition>())
        "type" -> expect(node<FoxTypeAlias>())
        else -> {
            if (containsBuildable(token("def"))) expect(node<FoxMethodDefinition>())
            if (containsBuildable(token("type"))) expect(node<FoxTypeAlias>())
        }
    }
}

private fun ExpectationReceiver.repairTypeAlias() {
    val span = trimOuterLineBreaks(currentSpan())
    val equals = withSpan(span).lastInside(token("="))
    val typeName = withSpan(span).firstInside(TypeName)
    if (typeName != null) {
        expect(token("type"), trimOuterLineBreaks(SourceSpan(span.start, typeName.span.start)))
    } else {
        expect(token("type"), trimOuterLineBreaks(SourceSpan(span.start, span.start)))
    }
    
    if (equals != null) {
        expect(TypeName, trimOuterLineBreaks(SourceSpan(typeName?.span?.start ?: span.start, equals.span.start)))
        val aliasSpan = trimOuterLineBreaks(SourceSpan(equals.span.end, span.end))
        if (aliasSpan.isNotEmpty()) expect(node<FoxType>(), aliasSpan)
    } else {
        if (typeName != null) expectAt(token("="), typeName.span.end)
        val tail = trimOuterLineBreaks(SourceSpan(typeName?.span?.end ?: span.start, span.end))
        if (tail.isNotEmpty()) expect(node<FoxType>(), tail)
    }
}

private fun ExpectationReceiver.repairMethodDefinition() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val blockSpan = receiver.lastInside(node<FoxBlock>())?.span ?: receiver.lastDelimited(BlockOpen, BlockClose)?.span
    if (blockSpan == null) {
        expect(MethodHead, span)
        expectAt(node<FoxBlock>(), span.end)
        return
    }
    
    expect(node<FoxBlock>(), blockSpan)
    val beforeBlock = trimOuterLineBreaks(SourceSpan(span.start, blockSpan.start))
    if (beforeBlock.isEmpty()) return
    
    val headAndReturn = withSpan(beforeBlock)
    val returnType = headAndReturn.matchesEndingAt(ReturnTypeClause, beforeBlock.end).maxByOrNull { it.span.length }
    if (returnType != null) {
        val headSpan = trimOuterLineBreaks(SourceSpan(beforeBlock.start, returnType.span.start))
        if (headSpan.isNotEmpty()) expect(MethodHead, headSpan)
        return
    }
    
    val parametersEnd = headAndReturn.lastInside(FormalParameterList)?.span?.end
        ?: headAndReturn.lastDelimited(ParenOpen, ParenClose)?.span?.end
    val returnColon = headAndReturn.matchesInside(token(":"))
        .filter { parametersEnd == null || parametersEnd <= it.span.start }
        .maxByOrNull { it.span.start.fragIndex }
    if (returnColon != null) {
        val headSpan = trimOuterLineBreaks(SourceSpan(beforeBlock.start, returnColon.span.start))
        val returnSpan = trimOuterLineBreaks(SourceSpan(returnColon.span.start, beforeBlock.end))
        if (headSpan.isNotEmpty()) expect(MethodHead, headSpan)
        if (returnSpan.isNotEmpty()) expect(ReturnTypeClause, returnSpan)
    } else {
        expect(MethodHead, beforeBlock)
    }
}

private fun ExpectationReceiver.repairMethodHead() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val parametersSpan = receiver.lastInside(FormalParameterList)?.span ?: receiver.lastDelimited(ParenOpen, ParenClose)?.span
    if (parametersSpan != null) {
        expect(FormalParameterList, parametersSpan)
        repairMethodHeadPrefix(trimOuterLineBreaks(SourceSpan(span.start, parametersSpan.start)))
    } else {
        repairMethodHeadPrefix(span)
        expectAt(FormalParameterList, span.end)
    }
}

private fun ExpectationReceiver.repairMethodHeadPrefix(prefix: SourceSpan) {
    if (prefix.isEmpty()) return
    val receiver = withSpan(prefix)
    val def = receiver.firstInside(token("def"))
    if (def == null) {
        expectAt(token("def"), prefix.start)
    }
    
    val afterDef = trimOuterLineBreaks(SourceSpan(def?.span?.end ?: prefix.start, prefix.end))
    if (afterDef.isEmpty()) return
    val tail = withSpan(afterDef)
    tail.firstDelimited(AngleOpen, AngleClose)?.let { expect(FormalGenericParameterList, it.span) }
    
    val dot = tail.lastInside(Dot)
    val name = tail.lastInside(Identifier)
    if (dot != null) {
        val thisTypeSpan = trimOuterLineBreaks(SourceSpan(afterDef.start, dot.span.end))
        if (thisTypeSpan.isNotEmpty()) expect(ThisTypeQualifier, thisTypeSpan)
        val nameSpan = trimOuterLineBreaks(SourceSpan(dot.span.end, afterDef.end))
        if (nameSpan.isNotEmpty()) expect(Identifier, nameSpan)
    } else if (name == null) {
        expect(Identifier, afterDef)
    }
}

private fun ExpectationReceiver.repairReturnTypeClause() {
    val colon = firstInside(token(":"))
    if (colon != null) {
        val typeSpan = trimOuterLineBreaks(SourceSpan(colon.span.end, currentSpan().end))
        if (typeSpan.isNotEmpty()) expect(node<FoxType>(), typeSpan)
    } else {
        expectAt(token(":"), currentSpan().start)
        val typeSpan = trimOuterLineBreaks(currentSpan())
        if (typeSpan.isNotEmpty()) expect(node<FoxType>(), typeSpan)
    }
}

private fun ExpectationReceiver.repairDelimitedCommaList(
    open: Symbol<*>,
    close: Symbol<*>,
    item: Symbol<*>,
) {
    candidateListFrames(open, close).forEach { frame ->
        frame.missingOpenAt?.let { expectAt(open, it) }
        frame.missingCloseAt?.let { expectAt(close, it) }
        repairListContent(frame.content, item)
    }
}

private fun ExpectationReceiver.candidateListFrames(
    open: Symbol<*>,
    close: Symbol<*>,
): List<ListFrame> {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val opens = receiver.matchesInside(open).distinctBy { it.span }
        .sortedWith(compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenBy { it.span.end.fragIndex })
    val closes = receiver.matchesInside(close).distinctBy { it.span }
        .sortedWith(compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenBy { it.span.end.fragIndex })
    val frames = mutableListOf<ListFrame>()
    
    fun addFrame(
        contentStart: SourcePosition,
        contentEnd: SourcePosition,
        missingOpenAt: SourcePosition?,
        missingCloseAt: SourcePosition?,
    ) {
        if (contentStart > contentEnd) return
        frames += ListFrame(
            content = trimOuterLineBreaks(SourceSpan(contentStart, contentEnd)),
            missingOpenAt = missingOpenAt,
            missingCloseAt = missingCloseAt,
        )
    }
    
    opens.forEach { openMatch ->
        closes.filter { closeMatch -> openMatch.span.end <= closeMatch.span.start }.forEach { closeMatch ->
            addFrame(openMatch.span.end, closeMatch.span.start, null, null)
        }
    }
    
    opens.filter { openMatch -> closes.none { closeMatch -> openMatch.span.end <= closeMatch.span.start } }
        .forEach { openMatch ->
            addFrame(openMatch.span.end, span.end, null, span.end)
        }
    
    closes.filter { closeMatch -> opens.none { openMatch -> openMatch.span.end <= closeMatch.span.start } }
        .forEach { closeMatch ->
            addFrame(span.start, closeMatch.span.start, span.start, null)
        }
    
    if (opens.isEmpty() && closes.isEmpty()) {
        addFrame(span.start, span.end, span.start, span.end)
    }
    
    return frames.distinct().take(MaxListFrames)
}

private fun ExpectationReceiver.repairListContent(
    content: SourceSpan,
    item: Symbol<*>,
) {
    if (content.isEmpty()) return

    val contentReceiver = withSpan(content)
    val itemMatches = contentReceiver.matchesInside(item)
    val separators = contentReceiver.topLevelListSeparators(content, itemMatches)
    
    repairListSegments(content, item, separators, itemMatches)
    
    if (itemMatches.isEmpty()) {
        expect(item, content)
        return
    }
    
    itemMatches.candidateItemLanes().forEach { items ->
        repairListItemLane(content, item, separators, items)
    }
}

private fun ExpectationReceiver.repairListSegments(
    content: SourceSpan,
    item: Symbol<*>,
    separators: List<ParseMatch<*>>,
    itemMatches: List<ParseMatch<*>>,
) {
    val starts = listOf(content.start) + separators.map { it.span.end }
    val ends = separators.map { it.span.start } + listOf(content.end)
    
    starts.zip(ends).forEachIndexed { index, (start, end) ->
        if (start > end) return@forEachIndexed
        
        val segment = trimOuterLineBreaks(SourceSpan(start, end))
        if (segment.isEmpty()) {
            val isTrailing = index == starts.lastIndex
            if (!isTrailing) expectAt(item, start)
            return@forEachIndexed
        }
        
        val hasItem = itemMatches.any { segment.contains(it.span) }
        if (!hasItem) expect(item, segment)
    }
}

private fun ExpectationReceiver.repairListItemLane(
    content: SourceSpan,
    item: Symbol<*>,
    separators: List<ParseMatch<*>>,
    items: List<ParseMatch<*>>,
) {
    if (items.isEmpty()) return

    val leading = trimOuterLineBreaks(SourceSpan(content.start, items.first().span.start))
    if (leading.isNotEmpty() && !withSpan(leading).hasOnlyCommas()) expect(item, leading)

    items.zipWithNext().forEach { (left, right) ->
        val gap = SourceSpan(left.span.end, right.span.start)
        val hasComma = separators.any { gap.contains(it.span) }
        if (!hasComma) expect(Comma, gap)
        val trimmed = trimOuterLineBreaks(gap)
        if (trimmed.isNotEmpty() && !withSpan(trimmed).hasOnlyCommas()) {
            val hasNestedItem = withSpan(trimmed).matchesInside(item).isNotEmpty()
            if (!hasNestedItem) expect(item, trimmed)
        }
    }
    
    val trailing = trimOuterLineBreaks(SourceSpan(items.last().span.end, content.end))
    if (trailing.isNotEmpty() && !withSpan(trailing).hasOnlyCommas()) expect(item, trailing)
}

private fun ExpectationReceiver.repairColonTypedItem(nameColon: Symbol<*>) {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val colon = receiver.firstInside(token(":"))
    if (colon != null) {
        val head = trimOuterLineBreaks(SourceSpan(span.start, colon.span.end))
        val type = trimOuterLineBreaks(SourceSpan(colon.span.end, span.end))
        if (head.isNotEmpty()) expect(nameColon, head)
        if (type.isNotEmpty()) expect(node<FoxType>(), type)
        return
    }
    
    val identifier = receiver.firstInside(Identifier) ?: receiver.firstInside(TypeName)
    if (identifier != null) {
        expect(nameColon, trimOuterLineBreaks(SourceSpan(identifier.span.start, identifier.span.end)))
        val type = trimOuterLineBreaks(SourceSpan(identifier.span.end, span.end))
        if (type.isNotEmpty()) expect(node<FoxType>(), type)
    } else {
        expect(nameColon, span)
    }
}

private fun ExpectationReceiver.repairOptionalColonTypedItem(name: Symbol<*>, type: Symbol<*>) {
    val span = trimOuterLineBreaks(currentSpan())
    val colon = withSpan(span).firstInside(token(":"))
    if (colon == null) {
        expect(name, span)
    } else {
        val nameSpan = trimOuterLineBreaks(SourceSpan(span.start, colon.span.start))
        val typeSpan = trimOuterLineBreaks(SourceSpan(colon.span.end, span.end))
        if (nameSpan.isNotEmpty()) expect(name, nameSpan)
        if (typeSpan.isNotEmpty()) expect(type, typeSpan)
    }
}

private fun ExpectationReceiver.repairEqualTypedItem(name: Symbol<*>, type: Symbol<*>) {
    val span = trimOuterLineBreaks(currentSpan())
    val equals = withSpan(span).firstInside(token("="))
    if (equals == null) {
        val nameMatch = withSpan(span).firstInside(name)
        if (nameMatch != null) {
            expect(name, nameMatch.span)
            expectAt(token("="), nameMatch.span.end)
            val typeSpan = trimOuterLineBreaks(SourceSpan(nameMatch.span.end, span.end))
            if (typeSpan.isNotEmpty()) expect(type, typeSpan)
        } else {
            expect(name, span)
        }
    } else {
        val nameSpan = trimOuterLineBreaks(SourceSpan(span.start, equals.span.start))
        val typeSpan = trimOuterLineBreaks(SourceSpan(equals.span.end, span.end))
        if (nameSpan.isNotEmpty()) expect(name, nameSpan)
        if (typeSpan.isNotEmpty()) expect(type, typeSpan)
    }
}

private fun ExpectationReceiver.repairOptionalEqualTypedItem(name: Symbol<*>, type: Symbol<*>) {
    if (withSpan(trimOuterLineBreaks(currentSpan())).containsBuildable(token("="))) {
        repairEqualTypedItem(name, type)
    } else {
        val span = trimOuterLineBreaks(currentSpan())
        if (span.isNotEmpty()) expect(type, span)
    }
}

private fun ExpectationReceiver.repairTupleComponentParameter() {
    val colon = firstInside(token(":"))
    if (colon == null) {
        expect(node<FoxType>())
    } else {
        val type = trimOuterLineBreaks(SourceSpan(currentSpan().start, colon.span.start))
        val count = trimOuterLineBreaks(SourceSpan(colon.span.end, currentSpan().end))
        if (type.isNotEmpty()) expect(node<FoxType>(), type)
        if (count.isNotEmpty()) expect(node<Int>(), count)
    }
}

private fun ExpectationReceiver.repairMethodTypeArgument() {
    when (firstPlainText()) {
        "this", "return" -> {
            val colon = firstInside(token(":"))
            if (colon == null) {
                expectAt(token(":"), firstInside(token(firstPlainText() ?: ""))?.span?.end ?: currentSpan().start)
            }
            val type = trimOuterLineBreaks(SourceSpan(colon?.span?.end ?: currentSpan().start, currentSpan().end))
            if (type.isNotEmpty()) expect(node<FoxType>(), type)
        }
        else -> {
            repairColonTypedItem(IdentifierColon)
            expect(node<FoxType>())
        }
    }
}

private fun ExpectationReceiver.repairSymbolThenToken(symbol: Symbol<*>, trailing: Symbol<*>) {
    val span = trimOuterLineBreaks(currentSpan())
    val head = withSpan(span).firstInside(symbol)
    if (head == null) {
        expect(symbol, span)
        expectAt(trailing, span.end)
    } else {
        expect(symbol, head.span)
        expectAt(trailing, head.span.end)
    }
}

private fun ExpectationReceiver.repairBlock() {
    val block = firstDelimited(BlockOpen, BlockClose)
    if (block != null) {
        expect(StatementBlock, block.span)
    } else {
        expect(StatementBlock)
    }
}

private fun ExpectationReceiver.repairStatementBlock() {
    val span = trimOuterLineBreaks(currentSpan())
    val receiver = withSpan(span)
    val open = receiver.firstInside(BlockOpen)
    val close = receiver.lastInside(BlockClose)
    if (open == null) expectAt(BlockOpen, span.start)
    if (close == null) expectAt(BlockClose, span.end)
    
    val body = trimOuterLineBreaks(SourceSpan(open?.span?.end ?: span.start, close?.span?.start ?: span.end))
    withSpan(body).lines().forEach { line ->
        val lineSpan = trimOuterLineBreaks(line.currentSpan())
        if (lineSpan.isNotEmpty()) expect(StatementLine, line.currentSpan())
    }
}

private fun ExpectationReceiver.repairStatementLine() {
    val lineBreak = matchesEndingAt(LineBreak, currentSpan().end).firstOrNull()
    if (lineBreak != null) {
        val statement = trimOuterLineBreaks(SourceSpan(currentSpan().start, lineBreak.span.start))
        if (statement.isNotEmpty()) expect(node<FoxStatement>(), statement)
    } else {
        val statement = trimOuterLineBreaks(currentSpan())
        if (statement.isNotEmpty()) expect(node<FoxStatement>(), statement)
        expectAt(LineBreak, currentSpan().end)
    }
}

private fun ExpectationReceiver.repairStatement() {
    when (firstPlainText()) {
        "if" -> expect(IfCore)
        "while" -> expect(WhileCore)
        "do" -> expect(DoWhileCore)
        "when" -> expect(WhenCore)
        "return" -> repairKeywordValueStatement("return")
        "yield" -> repairKeywordValueStatement("yield")
        "break" -> repairOptionalLabelStatement("break")
        "continue" -> repairOptionalLabelStatement("continue")
        else -> {
            if (containsBuildable(token("=")) || containsBuildable(token(":="))) {
                val operator = firstAssignmentOperator()
                if (operator != null) {
                    val left = trimOuterLineBreaks(SourceSpan(currentSpan().start, operator.span.start))
                    val right = trimOuterLineBreaks(SourceSpan(operator.span.end, currentSpan().end))
                    if (left.isNotEmpty()) expect(AssignableExpression, left)
                    if (right.isNotEmpty()) expect(node<FoxStatement>(), right)
                }
            } else if (containsBuildable(ParenOpen) && containsBuildable(ParenClose)) {
                firstDelimited(ParenOpen, ParenClose)?.let { expect(ParenthesizedStatement, it.span) }
            } else {
                expect(AssignmentExpression)
            }
        }
    }
}

private fun ExpectationReceiver.repairKeywordValueStatement(keyword: String) {
    val match = firstInside(token(keyword))
    val value = trimOuterLineBreaks(SourceSpan(match?.span?.end ?: currentSpan().start, currentSpan().end))
    if (value.isNotEmpty()) expect(node<FoxStatement>(), value)
}

private fun ExpectationReceiver.repairOptionalLabelStatement(keyword: String) {
    val match = firstInside(token(keyword))
    val label = trimOuterLineBreaks(SourceSpan(match?.span?.end ?: currentSpan().start, currentSpan().end))
    if (label.isNotEmpty()) expect(Label, label)
}

private fun ExpectationReceiver.repairParenthesizedStatement() {
    val grouped = firstDelimited(ParenOpen, ParenClose)
    if (grouped != null) {
        val open = withSpan(grouped.span).firstInside(ParenOpen) ?: return
        val close = withSpan(grouped.span).lastInside(ParenClose) ?: return
        val inner = trimOuterLineBreaks(SourceSpan(open.span.end, close.span.start))
        if (inner.isNotEmpty()) expect(node<FoxStatement>(), inner)
    } else {
        expectAt(ParenOpen, currentSpan().start)
        expectAt(ParenClose, currentSpan().end)
        val inner = trimOuterLineBreaks(currentSpan())
        if (inner.isNotEmpty()) expect(node<FoxStatement>(), inner)
    }
}

private fun ExpectationReceiver.repairControlBody() {
    val span = trimOuterLineBreaks(currentSpan())
    if (span.isNotEmpty()) expect(node<FoxStatement>(), span)
}

private fun ExpectationReceiver.repairIfCore() {
    repairKeywordConditionBody("if")
    val elseMatch = lastInside(ElseKeyword)
    if (elseMatch != null) {
        val elseBody = trimOuterLineBreaks(SourceSpan(elseMatch.span.end, currentSpan().end))
        if (elseBody.isNotEmpty()) expect(ControlBody, elseBody)
    }
}

private fun ExpectationReceiver.repairWhileCore() {
    repairKeywordConditionBody("while")
}

private fun ExpectationReceiver.repairDoWhileCore() {
    val doMatch = firstInside(token("do"))
    val whileMatch = lastInside(DoWhileKeyword)
    if (whileMatch != null) {
        val body = trimOuterLineBreaks(SourceSpan(doMatch?.span?.end ?: currentSpan().start, whileMatch.span.start))
        val condition = trimOuterLineBreaks(SourceSpan(whileMatch.span.end, currentSpan().end))
        if (body.isNotEmpty()) expect(ControlBody, body)
        if (condition.isNotEmpty()) expect(ParenthesizedStatement, condition)
    }
}

private fun ExpectationReceiver.repairWhenCore() {
    val cases = lastDelimited(BlockOpen, BlockClose)
    if (cases != null) {
        expect(WhenCaseList, cases.span)
        val beforeCases = trimOuterLineBreaks(SourceSpan(currentSpan().start, cases.span.start))
        withSpan(beforeCases).firstDelimited(ParenOpen, ParenClose)?.let { expect(ParenthesizedStatement, it.span) }
    } else {
        expectAt(WhenCaseList, currentSpan().end)
    }
}

private fun ExpectationReceiver.repairWhenCaseList() {
    val block = firstDelimited(BlockOpen, BlockClose)
    if (block == null) {
        expectAt(BlockOpen, currentSpan().start)
        expectAt(BlockClose, currentSpan().end)
        return
    }
    val open = withSpan(block.span).firstInside(BlockOpen) ?: return
    val close = withSpan(block.span).lastInside(BlockClose) ?: return
    val body = trimOuterLineBreaks(SourceSpan(open.span.end, close.span.start))
    withSpan(body).lines().forEach { line ->
        val lineSpan = trimOuterLineBreaks(line.currentSpan())
        if (lineSpan.isNotEmpty()) expect(WhenCaseLine, line.currentSpan())
    }
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
        if (body.isNotEmpty()) expect(ControlBody, body)
    } else if (firstPlainText() == "else") {
        expectAt(Arrow, firstInside(token("else"))?.span?.end ?: currentSpan().start)
    } else {
        expectAt(Arrow, currentSpan().end)
        expect(WhenCaseConditionList)
    }
}

private fun ExpectationReceiver.repairWhenCaseConditionList() {
    val arrow = lastInside(Arrow)
    val conditions = trimOuterLineBreaks(SourceSpan(currentSpan().start, arrow?.span?.start ?: currentSpan().end))
    if (conditions.isEmpty()) return
    val statements = withSpan(conditions).matchesInside(node<FoxStatement>()).nonOverlapping()
    if (statements.isEmpty()) {
        expect(node<FoxStatement>(), conditions)
        return
    }
    statements.zipWithNext().forEach { (left, right) ->
        val gap = trimOuterLineBreaks(SourceSpan(left.span.end, right.span.start))
        if (withSpan(gap).matchesInside(Comma).isEmpty()) expect(Comma, gap)
    }
}

private fun ExpectationReceiver.repairKeywordConditionBody(keyword: String) {
    val keywordMatch = firstInside(token(keyword))
    val condition = firstDelimited(ParenOpen, ParenClose)
    if (condition != null) {
        expect(ParenthesizedStatement, condition.span)
        val body = trimOuterLineBreaks(SourceSpan(condition.span.end, currentSpan().end))
        if (body.isNotEmpty()) expect(ControlBody, body)
    } else {
        val conditionSpan = trimOuterLineBreaks(SourceSpan(keywordMatch?.span?.end ?: currentSpan().start, currentSpan().end))
        if (conditionSpan.isNotEmpty()) expect(ParenthesizedStatement, conditionSpan)
    }
}

private fun ExpectationReceiver.firstAssignmentOperator(): ParseMatch<*>? {
    return listOf("=", ":=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=", "&&=", "||=")
        .asSequence()
        .flatMap { matchesInside(token(it)).asSequence() }
        .minByOrNull { it.span.start.fragIndex }
}

private fun ExpectationReceiver.firstDelimited(open: Symbol<*>, close: Symbol<*>): SpanMatch? {
    val openMatch = firstInside(open) ?: return null
    val closeMatch = withStart(openMatch.span.end).firstInside(close) ?: return null
    return SpanMatch(SourceSpan(openMatch.span.start, closeMatch.span.end))
}

private fun ExpectationReceiver.lastDelimited(open: Symbol<*>, close: Symbol<*>): SpanMatch? {
    val closeMatch = lastInside(close) ?: return null
    val openMatch = withEnd(closeMatch.span.start).lastInside(open) ?: return null
    return SpanMatch(SourceSpan(openMatch.span.start, closeMatch.span.end))
}

private fun ExpectationReceiver.hasOnlyCommas(): Boolean {
    val span = trimOuterLineBreaks(currentSpan())
    if (span.isEmpty()) return true
    val commas = (matchesInside(Comma) + matchesInside(token(",")))
        .filter { span.contains(it.span) }
        .distinctBy { it.span }
        .sortedWith(compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenByDescending { it.span.length })
    var cursor = span.start
    
    while (cursor < span.end) {
        while (cursor < span.end && source()[cursor] is LineBreakFragment) cursor += 1
        if (cursor == span.end) break
        val comma = commas.firstOrNull { it.span.start == cursor } ?: return false
        cursor = comma.span.end
    }
    
    return true
}

private fun ExpectationReceiver.topLevelListSeparators(
    content: SourceSpan,
    itemMatches: List<ParseMatch<*>>,
): List<ParseMatch<*>> {
    val candidates = (matchesInside(Comma) + matchesInside(token(",")))
        .filter { separator -> content.contains(separator.span) }
        .filter { separator -> itemMatches.none { item -> item.span.contains(separator.span) } }
        .distinctBy { it.span }
        .sortedWith(compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenByDescending { it.span.length })
    val result = mutableListOf<ParseMatch<*>>()
    candidates.forEach { separator ->
        if (result.none { it.span.overlaps(separator.span) }) result += separator
    }
    return result.sortedBy { it.span.start.fragIndex }
}

private fun List<ParseMatch<*>>.candidateItemLanes(): List<List<ParseMatch<*>>> {
    fun lane(comparator: Comparator<ParseMatch<*>>): List<ParseMatch<*>> {
        val result = mutableListOf<ParseMatch<*>>()
        sortedWith(comparator).forEach { match ->
            if (result.none { it.span.overlaps(match.span) }) result += match
        }
        return result.sortedBy { it.span.start.fragIndex }
    }
    
    return listOf(
        lane(compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenByDescending { it.span.length }),
        lane(compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenBy { it.span.length }),
        lane(compareByDescending<ParseMatch<*>> { it.span.length }.thenBy { it.span.start.fragIndex }),
    ).distinct()
}

private fun List<ParseMatch<*>>.nonOverlapping(): List<ParseMatch<*>> {
    val result = mutableListOf<ParseMatch<*>>()
    sortedWith(compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenByDescending { it.span.length })
        .forEach { match ->
            if (result.none { it.span.overlaps(match.span) }) result += match
        }
    return result.sortedBy { it.span.start.fragIndex }
}

private fun SourceSpan.contains(other: SourceSpan): Boolean {
    return start <= other.start && other.end <= end
}

private fun SourceSpan.overlaps(other: SourceSpan): Boolean {
    return start < other.end && other.start < end
}

private fun ExpectationReceiver.trimOuterLineBreaks(span: SourceSpan): SourceSpan {
    var start = span.start
    var end = span.end
    while (start < end && source()[start] is LineBreakFragment) start += 1
    while (start < end && source()[end - 1] is LineBreakFragment) end -= 1
    return SourceSpan(start, end)
}

private data class SpanMatch(val span: SourceSpan)

private data class ListFrame(
    val content: SourceSpan,
    val missingOpenAt: SourcePosition?,
    val missingCloseAt: SourcePosition?,
)
