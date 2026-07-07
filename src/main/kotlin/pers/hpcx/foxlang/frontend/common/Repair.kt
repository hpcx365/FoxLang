package pers.hpcx.foxlang.frontend.common

data class Expectation(
    val symbol: GrammarSymbol<*>,
    val span: SourceSpan,
)

class RepairStrategy internal constructor(
    val expectations: Map<GrammarSymbol<*>, ExpectationStrategy>,
) {
    constructor(vararg expectations: Pair<GrammarSymbol<*>, ExpectationStrategy>) : this(expectations.toMap())
    
    fun childExpectations(context: ParseContext, expectation: Expectation): Set<Expectation> {
        val receiver = ExpectationReceiver(context, expectation.span)
        expectations[expectation.symbol]?.let { receiver.apply(it) }
        return receiver.build()
    }
}

typealias ExpectationStrategy = ExpectationReceiver.() -> Unit

class ExpectationReceiver internal constructor(
    private val context: ParseContext,
    private val currentSpan: SourceSpan,
    private val expectations: MutableSet<Expectation> = mutableSetOf(),
) {
    
    fun source() = context.source
    
    fun currentSpan() = currentSpan
    
    fun build() = expectations.toSet()
    
    fun expectAt(symbol: GrammarSymbol<*>, position: SourcePosition) {
        expect(symbol, SourceSpan(position, position))
    }
    
    fun expect(symbol: GrammarSymbol<*>, span: SourceSpan = currentSpan) {
        expectations += Expectation(symbol, span)
    }
    
    fun withStart(start: SourcePosition) = withSpan(SourceSpan(start, currentSpan.end))
    fun withEnd(end: SourcePosition) = withSpan(SourceSpan(currentSpan.start, end))
    fun withSpan(start: SourcePosition, end: SourcePosition) = withSpan(SourceSpan(start, end))
    fun withSpan(span: SourceSpan) = ExpectationReceiver(context, span, expectations)
    
    fun matchesInside(symbol: GrammarSymbol<*>, buildableOnly: Boolean = true): List<ParseMatch<*>> {
        val result = mutableListOf<ParseMatch<*>>()
        var cursor = currentSpan.start
        while (cursor <= currentSpan.end) {
            context.matchesByStart(symbol, cursor).forEach { match ->
                if (match.span.end <= currentSpan.end && (!buildableOnly || match.matchType.isBuildable())) {
                    result += match
                }
            }
            if (cursor == currentSpan.end) break
            cursor += 1
        }
        return result
    }
    
    fun matchesEndingAt(
        symbol: GrammarSymbol<*>,
        end: SourcePosition,
        buildableOnly: Boolean = true,
    ): List<ParseMatch<*>> {
        return context.matchesByEnd(symbol, end).filter { match ->
            currentSpan.start <= match.span.start && match.span.end <= currentSpan.end &&
                (!buildableOnly || match.matchType.isBuildable())
        }
    }
}

fun ExpectationReceiver.containsBuildable(symbol: GrammarSymbol<*>): Boolean {
    return matchesInside(symbol).isNotEmpty()
}

fun ExpectationReceiver.firstInside(symbol: GrammarSymbol<*>, buildableOnly: Boolean = true): ParseMatch<*>? {
    return matchesInside(symbol, buildableOnly).minWithOrNull(
        compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenBy { it.span.end.fragIndex },
    )
}

fun ExpectationReceiver.lastInside(symbol: GrammarSymbol<*>, buildableOnly: Boolean = true): ParseMatch<*>? {
    return matchesInside(symbol, buildableOnly).maxWithOrNull(
        compareBy<ParseMatch<*>> { it.span.end.fragIndex }.thenBy { it.span.length },
    )
}

fun ExpectationReceiver.repairDelimitedSeparatedList(
    open: GrammarSymbol<*>,
    close: GrammarSymbol<*>,
    separator: GrammarSymbol<*>,
    item: GrammarSymbol<*>,
    maxFrames: Int = 8,
    trim: ExpectationReceiver.(SourceSpan) -> SourceSpan = { it },
    ignoredFragment: (Any?) -> Boolean = { false },
) {
    candidateListFrames(open, close, maxFrames, trim).forEach { frame ->
        frame.missingOpenAt?.let { expectAt(open, it) }
        frame.missingCloseAt?.let { expectAt(close, it) }
        repairListContent(frame.content, separator, item, trim, ignoredFragment)
    }
}

fun ExpectationReceiver.repairUndelimitedSeparatedList(
    separator: GrammarSymbol<*>,
    item: GrammarSymbol<*>,
    trim: ExpectationReceiver.(SourceSpan) -> SourceSpan = { it },
    ignoredFragment: (Any?) -> Boolean = { false },
) {
    repairListContent(trim(currentSpan()), separator, item, trim, ignoredFragment)
}

fun ExpectationReceiver.repairFixedArityDelimitedArguments(
    open: GrammarSymbol<*>,
    close: GrammarSymbol<*>,
    separator: GrammarSymbol<*>,
    arguments: List<GrammarSymbol<*>>,
    maxFrames: Int = 8,
    trim: ExpectationReceiver.(SourceSpan) -> SourceSpan = { it },
) {
    candidateListFrames(open, close, maxFrames, trim).forEach { frame ->
        frame.missingOpenAt?.let { expectAt(open, it) }
        frame.missingCloseAt?.let { expectAt(close, it) }
        repairFixedArityArgumentContent(frame.content, separator, arguments, trim)
    }
}

fun ExpectationReceiver.repairLineSeparatedItems(
    item: GrammarSymbol<*>,
    isLineBreak: (Any?) -> Boolean,
    continuationSymbols: Iterable<GrammarSymbol<*>> = emptyList(),
    trim: ExpectationReceiver.(SourceSpan) -> SourceSpan = { it },
) {
    val span = trim(currentSpan())
    if (span.isEmpty()) return
    
    val existing = matchesInside(item).longestNonOverlapping()
    if (existing.isEmpty()) {
        expectLineItemsInGap(span, item, isLineBreak, continuationSymbols, trim)
        return
    }
    
    expectLineItemsInGap(SourceSpan(span.start, existing.first().span.start), item, isLineBreak, continuationSymbols, trim)
    existing.zipWithNext().forEach { (left, right) ->
        expectLineItemsInGap(SourceSpan(left.span.end, right.span.start), item, isLineBreak, continuationSymbols, trim)
    }
    expectLineItemsInGap(SourceSpan(existing.last().span.end, span.end), item, isLineBreak, continuationSymbols, trim)
}

fun ExpectationReceiver.firstDelimited(open: GrammarSymbol<*>, close: GrammarSymbol<*>): SourceSpan? {
    val openMatch = firstInside(open) ?: return null
    val closeMatch = withStart(openMatch.span.end).firstInside(close) ?: return null
    return SourceSpan(openMatch.span.start, closeMatch.span.end)
}

fun ExpectationReceiver.lastDelimited(open: GrammarSymbol<*>, close: GrammarSymbol<*>): SourceSpan? {
    val closeMatch = lastInside(close) ?: return null
    val openMatch = withEnd(closeMatch.span.start).lastInside(open) ?: return null
    return SourceSpan(openMatch.span.start, closeMatch.span.end)
}

fun ExpectationReceiver.firstInsideLongest(symbol: GrammarSymbol<*>): ParseMatch<*>? {
    return matchesInside(symbol).minWithOrNull(
        compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenByDescending { it.span.end.fragIndex },
    )
}

fun ExpectationReceiver.trimOuterFragments(
    span: SourceSpan,
    ignoredFragment: (Any?) -> Boolean,
): SourceSpan {
    var start = span.start
    var end = span.end
    while (start < end && ignoredFragment(source()[start])) start += 1
    while (start < end && ignoredFragment(source()[end - 1])) end -= 1
    return SourceSpan(start, end)
}

fun ExpectationReceiver.startsWithFragment(predicate: (Any?) -> Boolean): Boolean {
    val span = currentSpan()
    return span.isNotEmpty() && predicate(source()[span.start])
}

fun List<ParseMatch<*>>.candidateItemLanes(): List<List<ParseMatch<*>>> {
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

fun List<ParseMatch<*>>.nonOverlapping(): List<ParseMatch<*>> {
    val result = mutableListOf<ParseMatch<*>>()
    sortedWith(compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenByDescending { it.span.length })
        .forEach { match ->
            if (result.none { it.span.overlaps(match.span) }) result += match
        }
    return result.sortedBy { it.span.start.fragIndex }
}

fun List<ParseMatch<*>>.longestNonOverlapping(): List<ParseMatch<*>> {
    val result = mutableListOf<ParseMatch<*>>()
    sortedWith(compareByDescending<ParseMatch<*>> { it.span.length }.thenBy { it.span.start.fragIndex })
        .forEach { match ->
            if (result.none { it.span.overlaps(match.span) }) result += match
        }
    return result.sortedBy { it.span.start.fragIndex }
}

fun SourceSpan.contains(other: SourceSpan): Boolean {
    return start <= other.start && other.end <= end
}

fun SourceSpan.overlaps(other: SourceSpan): Boolean {
    return start < other.end && other.start < end
}

private fun ExpectationReceiver.candidateListFrames(
    open: GrammarSymbol<*>,
    close: GrammarSymbol<*>,
    maxFrames: Int,
    trim: ExpectationReceiver.(SourceSpan) -> SourceSpan,
): List<ListFrame> {
    val span = trim(currentSpan())
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
            content = trim(SourceSpan(contentStart, contentEnd)),
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
    
    return frames.distinct().take(maxFrames)
}

private fun ExpectationReceiver.repairFixedArityArgumentContent(
    content: SourceSpan,
    separator: GrammarSymbol<*>,
    arguments: List<GrammarSymbol<*>>,
    trim: ExpectationReceiver.(SourceSpan) -> SourceSpan,
) {
    if (arguments.isEmpty()) return
    
    val receiver = withSpan(content)
    val argumentMatches = arguments.flatMap { receiver.matchesInside(it) }.distinctBy { it.span }
    val separators = receiver.topLevelListSeparators(separator, content, argumentMatches)
    val starts = listOf(content.start) + separators.map { it.span.end }
    val ends = separators.map { it.span.start } + listOf(content.end)
    
    starts.zip(ends).forEachIndexed { index, (start, end) ->
        if (index >= arguments.size || start > end) return@forEachIndexed
        val segment = trim(SourceSpan(start, end))
        if (segment.isEmpty()) {
            expectAt(arguments[index], start)
        } else {
            expect(arguments[index], segment)
        }
    }
    
    val lane = fixedArityArgumentLane(content, arguments)
    if (lane.size < arguments.size && starts.size < arguments.size) {
        (starts.size..<arguments.size).forEach { index ->
            if (index > 0) expectAt(separator, content.end)
            expectAt(arguments[index], content.end)
        }
    }
    
    if (lane.isEmpty()) {
        if (content.isNotEmpty()) expect(arguments.first(), content)
        return
    }
    
    lane.zipWithNext().forEach { (left, right) ->
        val gap = SourceSpan(left.span.end, right.span.start)
        val hasSeparator = separators.any { gap.contains(it.span) }
        if (!hasSeparator) expect(separator, gap)
    }
}

private fun ExpectationReceiver.fixedArityArgumentLane(
    content: SourceSpan,
    arguments: List<GrammarSymbol<*>>,
): List<ParseMatch<*>> {
    val result = mutableListOf<ParseMatch<*>>()
    var cursor = content.start
    
    arguments.forEach { argument ->
        val match = withSpan(cursor, content.end).matchesInside(argument).minWithOrNull(
            compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenByDescending { it.span.length },
        ) ?: return@forEach
        result += match
        cursor = match.span.end
    }
    
    return result
}

private fun ExpectationReceiver.repairListContent(
    content: SourceSpan,
    separator: GrammarSymbol<*>,
    item: GrammarSymbol<*>,
    trim: ExpectationReceiver.(SourceSpan) -> SourceSpan,
    ignoredFragment: (Any?) -> Boolean,
) {
    if (content.isEmpty()) return
    
    val contentReceiver = withSpan(content)
    val itemMatches = contentReceiver.matchesInside(item)
    val separators = contentReceiver.topLevelListSeparators(separator, content, itemMatches)
    
    repairListSegments(content, item, separators, itemMatches, trim)
    
    if (itemMatches.isEmpty()) {
        expect(item, content)
        return
    }
    
    itemMatches.candidateItemLanes().forEach { items ->
        repairListItemLane(content, separator, item, separators, items, trim, ignoredFragment)
    }
}

private fun ExpectationReceiver.repairListSegments(
    content: SourceSpan,
    item: GrammarSymbol<*>,
    separators: List<ParseMatch<*>>,
    itemMatches: List<ParseMatch<*>>,
    trim: ExpectationReceiver.(SourceSpan) -> SourceSpan,
) {
    val starts = listOf(content.start) + separators.map { it.span.end }
    val ends = separators.map { it.span.start } + listOf(content.end)
    
    starts.zip(ends).forEachIndexed { index, (start, end) ->
        if (start > end) return@forEachIndexed
        
        val segment = trim(SourceSpan(start, end))
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
    separator: GrammarSymbol<*>,
    item: GrammarSymbol<*>,
    separators: List<ParseMatch<*>>,
    items: List<ParseMatch<*>>,
    trim: ExpectationReceiver.(SourceSpan) -> SourceSpan,
    ignoredFragment: (Any?) -> Boolean,
) {
    if (items.isEmpty()) return
    
    val leading = trim(SourceSpan(content.start, items.first().span.start))
    if (leading.isNotEmpty() && !withSpan(leading).hasOnlySeparators(separator, trim, ignoredFragment)) {
        expect(item, leading)
    }
    
    items.zipWithNext().forEach { (left, right) ->
        val gap = SourceSpan(left.span.end, right.span.start)
        val hasSeparator = separators.any { gap.contains(it.span) }
        if (!hasSeparator) expect(separator, gap)
        val trimmed = trim(gap)
        if (trimmed.isNotEmpty() && !withSpan(trimmed).hasOnlySeparators(separator, trim, ignoredFragment)) {
            val hasNestedItem = withSpan(trimmed).matchesInside(item).isNotEmpty()
            if (!hasNestedItem) expect(item, trimmed)
        }
    }
    
    val trailing = trim(SourceSpan(items.last().span.end, content.end))
    if (trailing.isNotEmpty() && !withSpan(trailing).hasOnlySeparators(separator, trim, ignoredFragment)) {
        expect(item, trailing)
    }
}

private fun ExpectationReceiver.hasOnlySeparators(
    separator: GrammarSymbol<*>,
    trim: ExpectationReceiver.(SourceSpan) -> SourceSpan,
    ignoredFragment: (Any?) -> Boolean,
): Boolean {
    val span = trim(currentSpan())
    if (span.isEmpty()) return true
    val separators = matchesInside(separator)
        .filter { span.contains(it.span) }
        .distinctBy { it.span }
        .sortedWith(compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenByDescending { it.span.length })
    var cursor = span.start
    
    while (cursor < span.end) {
        while (cursor < span.end && ignoredFragment(source()[cursor])) cursor += 1
        if (cursor == span.end) break
        val match = separators.firstOrNull { it.span.start == cursor } ?: return false
        cursor = match.span.end
    }
    
    return true
}

private fun ExpectationReceiver.topLevelListSeparators(
    separator: GrammarSymbol<*>,
    content: SourceSpan,
    itemMatches: List<ParseMatch<*>>,
): List<ParseMatch<*>> {
    val candidates = matchesInside(separator)
        .filter { candidate -> content.contains(candidate.span) }
        .filter { candidate -> itemMatches.none { item -> item.span.contains(candidate.span) } }
        .distinctBy { it.span }
        .sortedWith(compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenByDescending { it.span.length })
    val result = mutableListOf<ParseMatch<*>>()
    candidates.forEach { candidate ->
        if (result.none { it.span.overlaps(candidate.span) }) result += candidate
    }
    return result.sortedBy { it.span.start.fragIndex }
}

private fun ExpectationReceiver.expectLineItemsInGap(
    gap: SourceSpan,
    item: GrammarSymbol<*>,
    isLineBreak: (Any?) -> Boolean,
    continuationSymbols: Iterable<GrammarSymbol<*>>,
    trim: ExpectationReceiver.(SourceSpan) -> SourceSpan,
) {
    val span = trim(gap)
    if (span.isEmpty()) return
    
    logicalLineSpans(span, isLineBreak, continuationSymbols).forEach { line ->
        if (trim(line).isNotEmpty()) expect(item, line)
    }
}

private fun ExpectationReceiver.logicalLineSpans(
    span: SourceSpan,
    isLineBreak: (Any?) -> Boolean,
    continuationSymbols: Iterable<GrammarSymbol<*>>,
): List<SourceSpan> {
    val result = mutableListOf<SourceSpan>()
    var start = span.start
    var cursor = span.start
    while (cursor < span.end) {
        val next = cursor + 1
        if (isLineBreak(source()[cursor]) && !isContinuationLineBreak(cursor, continuationSymbols)) {
            result += SourceSpan(start, next)
            start = next
        }
        cursor = next
    }
    if (start < span.end) result += SourceSpan(start, span.end)
    return result
}

private fun ExpectationReceiver.isContinuationLineBreak(
    position: SourcePosition,
    continuationSymbols: Iterable<GrammarSymbol<*>>,
): Boolean {
    val lineBreakSpan = SourceSpan(position, position + 1)
    return continuationSymbols.any { symbol ->
        matchesInside(symbol).any { match ->
            match.span.length > 1 && match.span.contains(lineBreakSpan)
        }
    }
}

private data class ListFrame(
    val content: SourceSpan,
    val missingOpenAt: SourcePosition?,
    val missingCloseAt: SourcePosition?,
)
