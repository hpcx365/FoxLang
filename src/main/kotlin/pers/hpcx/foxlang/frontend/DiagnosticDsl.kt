package pers.hpcx.foxlang.frontend

fun diagnosticRules(block: DiagnosticDslBuilder.() -> Unit): DiagnosticRules {
    return DiagnosticDslBuilder().apply(block).build()
}

fun diagnosticChildren(block: DiagnosticChildScope.() -> Unit): DiagnosticChildren {
    return DiagnosticChildren { scope, span ->
        DiagnosticChildScope(scope, span).apply(block).children()
    }
}

fun diagnosticList(
    list: Symbol<*>,
    block: DiagnosticListBuilder.() -> Unit,
): DiagnosticDelimitedList {
    return DiagnosticListBuilder(list).apply(block).build()
}

fun lineSpans(): DiagnosticSpanStrategy {
    return DiagnosticSpanStrategy { scope, span -> scope.lines(span) }
}

fun wholeSpan(): DiagnosticSpanStrategy {
    return DiagnosticSpanStrategy { _, span -> listOf(span) }
}

fun blockAwareLineSpans(
    blockStarters: Set<String>,
    open: Symbol<*>,
    close: Symbol<*>,
): DiagnosticSpanStrategy {
    return DiagnosticSpanStrategy { scope, span ->
        val result = mutableListOf<SourceSpan>()
        val lines = scope.lines(span).filter { it.isNotEmpty() }
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            if (scope.firstPlainText(line) !in blockStarters) {
                result += line
                index++
                continue
            }
            
            val blockOpen = scope.firstMatch(open, SourceSpan(line.begin, span.end))
            if (blockOpen == null) {
                result += line
                index++
                continue
            }
            val blockClose = scope.matchingClose(open, close, blockOpen.span.begin, span.end)
            if (blockClose == null) {
                result += SourceSpan(line.begin, span.end)
                break
            }
            val blockEnd = scope.includeFollowingLineBreak(blockClose.span.end, span.end)
            result += SourceSpan(line.begin, blockEnd)
            while (index < lines.size && lines[index].end <= blockEnd) index++
        }
        result
    }
}

data class DiagnosticDelimitedList(
    val list: Symbol<*>,
    val item: Symbol<*>,
    val open: Symbol<*>,
    val close: Symbol<*>,
    val separator: Symbol<*>,
    val name: String,
    val nesting: List<Pair<Symbol<*>, Symbol<*>>> = emptyList(),
    val itemChildren: DiagnosticChildren = DiagnosticChildren.Empty,
)

class DiagnosticListBuilder internal constructor(
    private val list: Symbol<*>,
) {
    
    private var item: Symbol<*>? = null
    private var open: Symbol<*>? = null
    private var close: Symbol<*>? = null
    private var separator: Symbol<*>? = null
    private var name: String? = null
    private val nesting = mutableListOf<Pair<Symbol<*>, Symbol<*>>>()
    private var itemChildren: DiagnosticChildren = DiagnosticChildren.Empty
    
    fun item(
        symbol: Symbol<*>,
        name: String? = null,
    ) {
        item = symbol
        if (name != null) this.name = name
    }
    
    fun named(name: String) {
        this.name = name
    }
    
    fun between(
        open: Symbol<*>,
        close: Symbol<*>,
    ) {
        this.open = open
        this.close = close
    }
    
    fun separatedBy(separator: Symbol<*>) {
        this.separator = separator
    }
    
    fun nestedBy(vararg delimiters: Pair<Symbol<*>, Symbol<*>>) {
        nesting += delimiters
    }
    
    fun nestedBy(delimiters: Iterable<Pair<Symbol<*>, Symbol<*>>>) {
        nesting += delimiters
    }
    
    fun itemChildren(children: DiagnosticChildren) {
        itemChildren = children
    }
    
    fun itemChildren(block: DiagnosticChildScope.() -> Unit) {
        itemChildren(diagnosticChildren(block))
    }
    
    internal fun build(): DiagnosticDelimitedList {
        val item = requireNotNull(item) { "Diagnostic list '$list' must declare an item symbol" }
        val open = requireNotNull(open) { "Diagnostic list '$list' must declare an opening delimiter" }
        val close = requireNotNull(close) { "Diagnostic list '$list' must declare a closing delimiter" }
        val separator = requireNotNull(separator) { "Diagnostic list '$list' must declare a separator" }
        val name = requireNotNull(name) { "Diagnostic list '$list' must declare an item name" }
        return DiagnosticDelimitedList(
            list = list,
            item = item,
            open = open,
            close = close,
            separator = separator,
            name = name,
            nesting = nesting.toList(),
            itemChildren = itemChildren,
        )
    }
}

fun interface DiagnosticChildren {
    
    fun build(scope: DiagnosticScope, span: SourceSpan): List<Expectation>
    
    companion object {
        
        val Empty = DiagnosticChildren { _, _ -> emptyList() }
    }
}

fun DiagnosticChildren.andThen(other: DiagnosticChildren): DiagnosticChildren {
    return DiagnosticChildren { scope, span -> build(scope, span) + other.build(scope, span) }
}

fun interface DiagnosticCondition {
    
    fun matches(scope: DiagnosticScope, span: SourceSpan): Boolean
}

fun always(): DiagnosticCondition {
    return DiagnosticCondition { _, _ -> true }
}

fun never(): DiagnosticCondition {
    return DiagnosticCondition { _, _ -> false }
}

operator fun DiagnosticCondition.not(): DiagnosticCondition {
    return DiagnosticCondition { scope, span -> !matches(scope, span) }
}

infix fun DiagnosticCondition.and(other: DiagnosticCondition): DiagnosticCondition {
    return DiagnosticCondition { scope, span -> matches(scope, span) && other.matches(scope, span) }
}

infix fun DiagnosticCondition.or(other: DiagnosticCondition): DiagnosticCondition {
    return DiagnosticCondition { scope, span -> matches(scope, span) || other.matches(scope, span) }
}

fun firstText(vararg texts: String): DiagnosticCondition {
    val accepted = texts.toSet()
    return DiagnosticCondition { scope, span -> scope.firstPlainText(span) in accepted }
}

fun containsText(vararg texts: String): DiagnosticCondition {
    val accepted = texts.toSet()
    return DiagnosticCondition { scope, span -> accepted.any { scope.containsToken(it, span) } }
}

fun containsMatch(symbol: Symbol<*>): DiagnosticCondition {
    return DiagnosticCondition { scope, span -> scope.firstMatch(symbol, span) != null }
}

fun exactMatch(symbol: Symbol<*>): DiagnosticCondition {
    return DiagnosticCondition { scope, span -> scope.hasMatch(symbol, span) }
}

fun startsWith(symbol: Symbol<*>): DiagnosticCondition {
    return DiagnosticCondition { scope, span ->
        scope.matchesByBegin(symbol, span.begin).any { it.span.end <= span.end }
    }
}

fun endsWith(symbol: Symbol<*>): DiagnosticCondition {
    return DiagnosticCondition { scope, span ->
        scope.matchesByEnd(symbol, span.end).any { it.span.begin >= span.begin }
    }
}

fun interface DiagnosticSpanStrategy {
    
    fun spans(scope: DiagnosticScope, span: SourceSpan): List<SourceSpan>
}

fun interface DiagnosticGapStrategy {
    
    fun gaps(scope: DiagnosticScope, span: SourceSpan): List<SourceSpan>
}

typealias SpanStrategy = DiagnosticSpanStrategy
typealias GapStrategy = DiagnosticGapStrategy

fun DiagnosticSpanStrategy.then(next: DiagnosticSpanStrategy): DiagnosticSpanStrategy {
    return DiagnosticSpanStrategy { scope, span ->
        spans(scope, span).flatMap { next.spans(scope, it) }
    }
}

fun DiagnosticSpanStrategy.filter(condition: DiagnosticCondition): DiagnosticSpanStrategy {
    return DiagnosticSpanStrategy { scope, span ->
        spans(scope, span).filter { condition.matches(scope, it) }
    }
}

fun DiagnosticSpanStrategy.nonEmpty(): DiagnosticSpanStrategy {
    return DiagnosticSpanStrategy { scope, span ->
        spans(scope, span).filter { it.isNotEmpty() }
    }
}

fun DiagnosticGapStrategy.then(next: DiagnosticSpanStrategy): DiagnosticSpanStrategy {
    return DiagnosticSpanStrategy { scope, span ->
        gaps(scope, span).flatMap { next.spans(scope, it) }
    }
}

fun DiagnosticGapStrategy.splitBy(strategy: DiagnosticSpanStrategy): DiagnosticSpanStrategy {
    return then(strategy)
}

fun DiagnosticGapStrategy.asSpans(): DiagnosticSpanStrategy {
    return then(wholeSpan())
}

fun DiagnosticGapStrategy.filter(condition: DiagnosticCondition): DiagnosticGapStrategy {
    return DiagnosticGapStrategy { scope, span ->
        gaps(scope, span).filter { condition.matches(scope, it) }
    }
}

fun allGaps(): DiagnosticGapStrategy {
    return DiagnosticGapStrategy { _, span -> listOf(span).filter { it.isNotEmpty() } }
}

fun gapsExcluding(vararg symbols: Symbol<*>): DiagnosticGapStrategy {
    return DiagnosticGapStrategy { scope, span ->
        val existing = symbols.asSequence()
            .flatMap { symbol -> scope.matchesInside(symbol, span).asSequence() }
            .map { it.span }
            .sortedWith(compareBy<SourceSpan> { it.begin.fragIndex }.thenBy { it.end.fragIndex })
            .toList()
        if (existing.isEmpty()) return@DiagnosticGapStrategy listOf(span).filter { it.isNotEmpty() }
        
        val gaps = mutableListOf<SourceSpan>()
        var cursor = span.begin
        existing.forEach { matchSpan ->
            if (cursor < matchSpan.begin) {
                gaps += SourceSpan(cursor, matchSpan.begin)
            }
            if (cursor < matchSpan.end) {
                cursor = matchSpan.end
            }
        }
        if (cursor < span.end) {
            gaps += SourceSpan(cursor, span.end)
        }
        gaps.filter { it.isNotEmpty() }
    }
}

fun afterLeading(symbol: Symbol<*>): DiagnosticSpanStrategy {
    return DiagnosticSpanStrategy { scope, span ->
        val leading = scope.matchesByBegin(symbol, span.begin).firstOrNull()
            ?: return@DiagnosticSpanStrategy emptyList()
        listOf(SourceSpan(leading.span.end, span.end))
    }
}

fun withoutLeadingLineBreaks(): DiagnosticSpanStrategy {
    return DiagnosticSpanStrategy { scope, span ->
        var result = span
        while (true) {
            val next = scope.trimLeadingLineBreak(result)
            if (next == result) break
            result = next
        }
        listOf(result).filter { it.isNotEmpty() }
    }
}

class DiagnosticDslBuilder {
    
    private val rules = mutableListOf<DiagnosticRule>()
    
    fun diagnose(
        symbol: Symbol<*>,
        block: DiagnosticScope.() -> Unit,
    ) {
        rules += DiagnosticRule(symbol, block)
    }
    
    fun delimitedList(list: DiagnosticDelimitedList) {
        diagnose(list.list) {
            val openMatch = matchesByBegin(list.open, span.begin).firstOrNull()
            val closeMatch = matchesByEnd(list.close, span.end).firstOrNull()
            
            if (openMatch == null) {
                seed(list.open, SourceSpan(span.begin, span.begin), "Expected '${list.open}' before ${list.name} list", 6)
                return@diagnose
            }
            if (closeMatch == null) {
                seed(list.close, SourceSpan(span.end, span.end), "Expected '${list.close}' after ${list.name} list", 6)
            }
            
            val contentEnd = closeMatch?.span?.begin ?: span.end
            val contentSpan = SourceSpan(openMatch.span.end, contentEnd)
            for (slot in delimitedSlots(list, contentSpan)) {
                val children = list.itemChildren.build(this, slot)
                if (slot.isEmpty()) {
                    seed(list.item, slot, "Expected ${list.name}", 2, children = children)
                } else if (!hasMatch(list.item, slot)) {
                    seed(list.item, slot, "Malformed ${list.name}", 4 + slot.length, children = children)
                }
            }
        }
    }
    
    fun build(): DiagnosticRules {
        return DiagnosticRules(rules.groupBy({ it.symbol }, { it.block }))
    }
}

class DiagnosticScope internal constructor(
    private val ctx: DiagnosticContext,
    private val expectation: Expectation,
) {
    
    internal val actions = mutableListOf<SeedAction>()
    
    val span: SourceSpan get() = expectation.span
    
    fun seed(
        symbol: Symbol<*>,
        span: SourceSpan = expectation.span,
        reason: String,
        cost: Int = 4,
        children: List<Expectation> = emptyList(),
        block: SeedScope.() -> Unit = {},
    ) {
        val seedScope = SeedScope()
        seedScope.block()
        actions += SeedAction.Seed(
            symbol = symbol,
            span = span,
            reason = reason,
            cost = cost,
            children = children + seedScope.children,
        )
    }
    
    fun seed(
        symbol: Symbol<*>,
        span: SourceSpan = expectation.span,
        reason: String,
        cost: Int = 4,
        children: DiagnosticChildren,
        block: SeedScope.() -> Unit = {},
    ) {
        seed(symbol, span, reason, cost, children.build(this, span), block)
    }
    
    fun seedAtEnd(
        symbol: Symbol<*>,
        reason: String,
        cost: Int = 4,
        children: List<Expectation> = emptyList(),
        block: SeedScope.() -> Unit = {},
    ) {
        seed(symbol, SourceSpan(span.end, span.end), reason, cost, children, block)
    }
    
    fun recurse(
        symbol: Symbol<*>,
        span: SourceSpan = expectation.span,
        costLimit: Int = expectation.costLimit,
    ) {
        actions += SeedAction.Recurse(Expectation(symbol, span, costLimit))
    }
    
    fun seedAndRecurse(
        symbol: Symbol<*>,
        span: SourceSpan = expectation.span,
        reason: String,
        cost: Int = 4,
        costLimit: Int = expectation.costLimit,
        children: List<Expectation> = emptyList(),
        block: SeedScope.() -> Unit = {},
    ) {
        seed(symbol, span, reason, cost, children, block)
        recurse(symbol, span, costLimit)
    }
    
    fun seedAndRecurse(
        symbol: Symbol<*>,
        span: SourceSpan = expectation.span,
        reason: String,
        cost: Int = 4,
        costLimit: Int = expectation.costLimit,
        children: DiagnosticChildren,
        block: SeedScope.() -> Unit = {},
    ) {
        seedAndRecurse(symbol, span, reason, cost, costLimit, children.build(this, span), block)
    }
    
    fun seedMissing(
        symbol: Symbol<*>,
        spans: DiagnosticSpanStrategy,
        reason: String,
        cost: (SourceSpan) -> Int = { 4 },
        children: DiagnosticChildren = DiagnosticChildren.Empty,
        block: SeedScope.(SourceSpan) -> Unit = {},
    ) {
        forEachMissing(symbol, spans) { candidate ->
            seed(symbol, candidate, reason, cost(candidate), children.build(this, candidate)) {
                block(candidate)
            }
        }
    }
    
    fun seedNonEmpty(
        symbol: Symbol<*>,
        span: SourceSpan,
        reason: String,
        cost: Int = 4,
        children: DiagnosticChildren = DiagnosticChildren.Empty,
        block: SeedScope.() -> Unit = {},
    ) {
        if (span.isNotEmpty()) {
            seed(symbol, span, reason, cost, children.build(this, span), block)
        }
    }
    
    fun seedTrimmedTrailingLineBreak(
        symbol: Symbol<*>,
        reason: String,
        cost: Int = 4,
        children: DiagnosticChildren = DiagnosticChildren.Empty,
        block: SeedScope.() -> Unit = {},
    ) {
        seedNonEmpty(symbol, trimTrailingLineBreak(), reason, cost, children, block)
    }
    
    fun seedTrimmedLeadingLineBreak(
        symbol: Symbol<*>,
        reason: String,
        cost: Int = 4,
        children: DiagnosticChildren = DiagnosticChildren.Empty,
        block: SeedScope.() -> Unit = {},
    ) {
        seedNonEmpty(symbol, trimLeadingLineBreak(), reason, cost, children, block)
    }
    
    fun seedAfter(
        symbol: Symbol<*>,
        afterSymbol: Symbol<*>,
        reason: String,
        cost: Int = 4,
        children: List<Expectation> = emptyList(),
        block: SeedScope.(SourceSpan) -> Unit = {},
    ) {
        val seedSpan = after(afterSymbol) ?: return
        seed(symbol, seedSpan, reason, cost, children) {
            block(seedSpan)
        }
    }
    
    fun seedAfterToken(
        symbol: Symbol<*>,
        text: String,
        reason: String,
        cost: Int = 4,
        children: List<Expectation> = emptyList(),
        block: SeedScope.(SourceSpan) -> Unit = {},
    ) {
        seedAfter(symbol, token(text), reason, cost, children, block)
    }
    
    fun seedDelimited(
        symbol: Symbol<*>,
        open: Symbol<*>,
        close: Symbol<*>,
        reason: String,
        cost: Int = 4,
        span: SourceSpan = expectation.span,
        children: List<Expectation> = emptyList(),
        block: SeedScope.(SourceSpan) -> Unit = {},
    ) {
        val seedSpan = delimitedSpan(open, close, span) ?: return
        seed(symbol, seedSpan, reason, cost, children) {
            block(seedSpan)
        }
    }
    
    fun seedDelimited(
        symbol: Symbol<*>,
        open: Symbol<*>,
        close: Symbol<*>,
        reason: String,
        cost: Int = 4,
        span: SourceSpan = expectation.span,
        children: DiagnosticChildren,
        block: SeedScope.(SourceSpan) -> Unit = {},
    ) {
        val seedSpan = delimitedSpan(open, close, span) ?: return
        seed(symbol, seedSpan, reason, cost, children.build(this, seedSpan)) {
            block(seedSpan)
        }
    }
    
    fun seedDelimitedList(
        list: DiagnosticDelimitedList,
        span: SourceSpan,
        reason: String,
        cost: Int = 4,
    ) {
        seedAndRecurse(
            symbol = list.list,
            span = span,
            reason = reason,
            cost = cost,
            children = list.itemExpectations(this, span),
        )
    }
    
    fun seedDelimitedList(
        list: DiagnosticDelimitedList,
        reason: String,
        cost: Int = 4,
    ) {
        val listSpan = delimitedSpan(list.open, list.close) ?: return
        seedDelimitedList(list, listSpan, reason, cost)
    }
    
    fun recurseInsideDelimiters(
        symbol: Symbol<*>,
        open: Symbol<*>,
        close: Symbol<*>,
        costLimit: Int = expectation.costLimit,
    ) {
        val innerSpan = insideDelimiters(open, close) ?: return
        recurse(symbol, innerSpan, costLimit)
    }
    
    fun recurseThroughDelimitedContent(
        symbol: Symbol<*>,
        open: Symbol<*>,
        close: Symbol<*>,
        costLimit: Int = expectation.costLimit,
    ) {
        val headSpan = throughDelimitedContent(open, close) ?: return
        recurse(symbol, headSpan, costLimit)
    }
    
    fun firstPlainText(span: SourceSpan = expectation.span): String? {
        return ctx.firstPlainText(span)
    }
    
    fun satisfies(
        condition: DiagnosticCondition,
        span: SourceSpan = expectation.span,
    ): Boolean {
        return condition.matches(this, span)
    }
    
    fun on(
        condition: DiagnosticCondition,
        span: SourceSpan = expectation.span,
        block: DiagnosticScope.() -> Unit,
    ) {
        if (satisfies(condition, span)) block()
    }
    
    fun unless(
        condition: DiagnosticCondition,
        span: SourceSpan = expectation.span,
        block: DiagnosticScope.() -> Unit,
    ) {
        if (!satisfies(condition, span)) block()
    }
    
    fun branch(
        span: SourceSpan = expectation.span,
        block: DiagnosticConditionSwitch<DiagnosticScope>.() -> Unit,
    ) {
        DiagnosticConditionSwitch(this, this, span).apply(block).runDefault()
    }
    
    fun choice(block: DiagnosticChoiceScope.() -> Unit) {
        DiagnosticChoiceScope(this).apply(block).run()
    }
    
    internal fun matches(
        symbol: Symbol<*>,
        span: SourceSpan = expectation.span,
    ): List<ParseMatch<*>> {
        return ctx.chart.matches(symbol, span)
    }
    
    internal fun matchesByBegin(
        symbol: Symbol<*>,
        begin: SourcePosition = span.begin,
    ): List<ParseMatch<*>> {
        return ctx.chart.matchesByBegin(symbol, begin)
    }
    
    internal fun matchesByEnd(
        symbol: Symbol<*>,
        end: SourcePosition = span.end,
    ): List<ParseMatch<*>> {
        return ctx.chart.matchesByEnd(symbol, end)
    }
    
    internal fun hasMatch(
        symbol: Symbol<*>,
        span: SourceSpan = expectation.span,
    ): Boolean {
        return ctx.chart.matches(symbol, span).isNotEmpty()
    }
    
    internal fun firstMatch(
        symbol: Symbol<*>,
        span: SourceSpan = expectation.span,
    ): ParseMatch<*>? {
        return ctx.firstMatch(symbol, span)
    }
    
    internal fun containsToken(
        text: String,
        span: SourceSpan = expectation.span,
    ): Boolean {
        return ctx.containsToken(span, text)
    }
    
    internal fun delimitedSpan(
        open: Symbol<*>,
        close: Symbol<*>,
        span: SourceSpan = expectation.span,
    ): SourceSpan? {
        return ctx.findDelimitedSpan(span, open, close)
    }
    
    internal fun insideDelimiters(
        open: Symbol<*>,
        close: Symbol<*>,
        span: SourceSpan = expectation.span,
    ): SourceSpan? {
        return span.dropDelimiters(ctx, open, close)
    }
    
    internal fun throughDelimitedContent(
        open: Symbol<*>,
        close: Symbol<*>,
        span: SourceSpan = expectation.span,
    ): SourceSpan? {
        val innerSpan = insideDelimiters(open, close, span) ?: return null
        return SourceSpan(span.begin, innerSpan.end)
    }
    
    internal fun after(
        symbol: Symbol<*>,
        span: SourceSpan = expectation.span,
    ): SourceSpan? {
        val match = firstMatch(symbol, span) ?: return null
        return SourceSpan(match.span.end, span.end)
    }
    
    internal fun before(
        symbol: Symbol<*>,
        span: SourceSpan = expectation.span,
    ): SourceSpan? {
        val match = firstMatch(symbol, span) ?: return null
        return SourceSpan(span.begin, match.span.begin)
    }

    internal fun afterToken(
        text: String,
        span: SourceSpan = expectation.span,
    ): SourceSpan? {
        return after(token(text), span)
    }
    
    fun afterFirstFragment(span: SourceSpan = expectation.span): SourceSpan {
        return span.dropFirstFragment()
    }
    
    fun trimTrailingLineBreak(span: SourceSpan = expectation.span): SourceSpan {
        return span.trimTrailingLineBreak(ctx)
    }
    
    fun trimLeadingLineBreak(span: SourceSpan = expectation.span): SourceSpan {
        return span.trimLeadingLineBreak(ctx)
    }
    
    internal fun matchesInside(
        symbol: Symbol<*>,
        span: SourceSpan = expectation.span,
    ): List<ParseMatch<*>> {
        return ctx.chart.allMatches()
            .filter { it.symbol == symbol && it.span.begin >= span.begin && it.span.end <= span.end }
    }
    
    internal fun matchingClose(
        open: Symbol<*>,
        close: Symbol<*>,
        openBegin: SourcePosition,
        limit: SourcePosition,
    ): ParseMatch<*>? {
        var cursor = openBegin
        var depth = 0
        while (cursor < limit) {
            val openMatch = matchesByBegin(open, cursor).firstOrNull()
            if (openMatch != null) {
                depth++
                cursor = openMatch.span.end
                continue
            }
            val closeMatch = matchesByBegin(close, cursor).firstOrNull()
            if (closeMatch != null) {
                depth--
                if (depth == 0) return closeMatch
                cursor = closeMatch.span.end
                continue
            }
            cursor += 1
        }
        return null
    }
    
    internal fun includeFollowingLineBreak(
        position: SourcePosition,
        limit: SourcePosition,
    ): SourcePosition {
        return if (position < limit && ctx.source[position] is LineBreakFragment) position + 1 else position
    }
    
    internal fun delimitedSlots(
        list: DiagnosticDelimitedList,
        contentSpan: SourceSpan,
    ): List<SourceSpan> {
        return ctx.splitDiagnosticSlots(contentSpan, list.separator, list.close, list.nesting)
    }
    
    internal fun delimitedContentSpan(
        list: DiagnosticDelimitedList,
        span: SourceSpan = expectation.span,
    ): SourceSpan? {
        val openMatch = matchesByBegin(list.open, span.begin).firstOrNull() ?: return null
        val closeMatch = matchesByEnd(list.close, span.end).firstOrNull()
        val contentEnd = closeMatch?.span?.begin ?: span.end
        return SourceSpan(openMatch.span.end, contentEnd)
    }
    
    internal fun delimitedItemExpectations(
        list: DiagnosticDelimitedList,
        span: SourceSpan = expectation.span,
    ): List<Expectation> {
        val contentSpan = delimitedContentSpan(list, span) ?: return emptyList()
        return delimitedSlots(list, contentSpan)
            .filter { it.isNotEmpty() && !hasMatch(list.item, it) }
            .map { Expectation(list.item, it) }
    }
    
    internal fun lines(span: SourceSpan = expectation.span): List<SourceSpan> {
        return ctx.splitDiagnosticLines(span)
    }
    
    internal fun forEachSpan(
        spans: Iterable<SourceSpan>,
        skipEmpty: Boolean = true,
        block: DiagnosticScope.(SourceSpan) -> Unit,
    ) {
        spans.forEach { span ->
            if (!skipEmpty || span.isNotEmpty()) block(span)
        }
    }
    
    internal fun forEachSpan(
        spans: DiagnosticSpanStrategy,
        skipEmpty: Boolean = true,
        block: DiagnosticScope.(SourceSpan) -> Unit,
    ) {
        forEachSpan(spans.spans(this, span), skipEmpty, block)
    }
    
    internal fun forEachLine(
        span: SourceSpan = expectation.span,
        skipEmpty: Boolean = true,
        block: DiagnosticScope.(SourceSpan) -> Unit,
    ) {
        forEachSpan(lines(span), skipEmpty, block)
    }
    
    internal fun forEachMissing(
        symbol: Symbol<*>,
        spans: Iterable<SourceSpan>,
        skipEmpty: Boolean = true,
        block: DiagnosticScope.(SourceSpan) -> Unit,
    ) {
        forEachSpan(spans, skipEmpty) { span ->
            if (!hasMatch(symbol, span)) block(span)
        }
    }
    
    internal fun forEachMissing(
        symbol: Symbol<*>,
        spans: DiagnosticSpanStrategy,
        skipEmpty: Boolean = true,
        block: DiagnosticScope.(SourceSpan) -> Unit,
    ) {
        forEachMissing(symbol, spans.spans(this, span), skipEmpty, block)
    }
}

private fun DiagnosticDelimitedList.itemExpectations(
    scope: DiagnosticScope,
    span: SourceSpan,
): List<Expectation> {
    return scope.delimitedItemExpectations(this, span)
}

class DiagnosticConditionSwitch<T : Any> internal constructor(
    private val target: T,
    private val scope: DiagnosticScope,
    private val span: SourceSpan,
) {
    
    private var matched = false
    private var default: (T.() -> Unit)? = null
    
    fun case(
        condition: DiagnosticCondition,
        block: T.() -> Unit,
    ) {
        if (!matched && condition.matches(scope, span)) {
            matched = true
            block(target)
        }
    }
    
    fun otherwise(block: T.() -> Unit) {
        default = block
    }
    
    internal fun runDefault() {
        if (!matched) default?.invoke(target)
    }
}

class DiagnosticChoiceScope internal constructor(
    private val scope: DiagnosticScope,
) {
    
    private val branches = mutableListOf<DiagnosticChoiceBranch>()
    
    fun prefer(
        condition: DiagnosticCondition = always(),
        block: DiagnosticScope.() -> Unit,
    ) {
        branches += DiagnosticChoiceBranch(condition, block)
    }
    
    fun fallback(
        condition: DiagnosticCondition = always(),
        block: DiagnosticScope.() -> Unit,
    ) {
        branches += DiagnosticChoiceBranch(condition, block)
    }
    
    internal fun run() {
        for (branch in branches) {
            if (!branch.condition.matches(scope, scope.span)) continue
            
            val actionCount = scope.actions.size
            branch.block(scope)
            if (scope.actions.size > actionCount) return
        }
    }
    
    private data class DiagnosticChoiceBranch(
        val condition: DiagnosticCondition,
        val block: DiagnosticScope.() -> Unit,
    )
}

class SeedScope {
    
    internal val children = mutableListOf<Expectation>()
    
    fun expect(
        symbol: Symbol<*>,
        costLimit: Int = Int.MAX_VALUE,
    ): SeedChildExpectation {
        return SeedChildExpectation(this, symbol, costLimit)
    }
    
    internal fun addChild(
        symbol: Symbol<*>,
        span: SourceSpan,
        costLimit: Int = Int.MAX_VALUE,
    ) {
        children += Expectation(symbol, span, costLimit)
    }
}

class SeedChildExpectation internal constructor(
    private val scope: SeedScope,
    private val symbol: Symbol<*>,
    private val costLimit: Int,
) {
    
    fun at(span: SourceSpan) {
        scope.addChild(symbol, span, costLimit)
    }
}

class DiagnosticChildScope internal constructor(
    private val scope: DiagnosticScope,
    val span: SourceSpan,
) {
    
    private val children = mutableListOf<Expectation>()
    
    internal fun children(): List<Expectation> {
        return children
    }
    
    fun expect(
        symbol: Symbol<*>,
        costLimit: Int = Int.MAX_VALUE,
    ): DiagnosticChildExpectation {
        return DiagnosticChildExpectation(this, symbol, costLimit)
    }
    
    fun expect(children: DiagnosticChildren): DiagnosticChildSetExpectation {
        return DiagnosticChildSetExpectation(this, children)
    }
    
    internal fun addChild(
        symbol: Symbol<*>,
        span: SourceSpan,
        costLimit: Int = Int.MAX_VALUE,
    ) {
        children += Expectation(symbol, span, costLimit)
    }
    
    internal fun addChildren(
        children: DiagnosticChildren,
        span: SourceSpan = this.span,
    ) {
        this.children += children.build(scope, span)
    }
    
    internal fun trimTrailingLineBreak(span: SourceSpan = this.span): SourceSpan {
        return scope.trimTrailingLineBreak(span)
    }
    
    internal fun trimLeadingLineBreak(span: SourceSpan = this.span): SourceSpan {
        return scope.trimLeadingLineBreak(span)
    }
    
    internal fun afterFirstFragment(span: SourceSpan = this.span): SourceSpan {
        return scope.afterFirstFragment(span)
    }
    
    internal fun delimitedSpan(
        open: Symbol<*>,
        close: Symbol<*>,
        span: SourceSpan = this.span,
    ): SourceSpan? {
        return scope.delimitedSpan(open, close, span)
    }
    
    internal fun insideDelimiters(
        open: Symbol<*>,
        close: Symbol<*>,
        span: SourceSpan = this.span,
    ): SourceSpan? {
        return scope.insideDelimiters(open, close, span)
    }
    
    internal fun after(
        symbol: Symbol<*>,
        span: SourceSpan = this.span,
    ): SourceSpan? {
        return scope.after(symbol, span)
    }
    
    internal fun before(
        symbol: Symbol<*>,
        span: SourceSpan = this.span,
    ): SourceSpan? {
        return scope.before(symbol, span)
    }

    internal fun afterToken(
        text: String,
        span: SourceSpan = this.span,
    ): SourceSpan? {
        return scope.afterToken(text, span)
    }
    
    fun satisfies(
        condition: DiagnosticCondition,
        span: SourceSpan = this.span,
    ): Boolean {
        return condition.matches(scope, span)
    }
    
    fun on(
        condition: DiagnosticCondition,
        span: SourceSpan = this.span,
        block: DiagnosticChildScope.() -> Unit,
    ) {
        if (satisfies(condition, span)) block()
    }
    
    fun unless(
        condition: DiagnosticCondition,
        span: SourceSpan = this.span,
        block: DiagnosticChildScope.() -> Unit,
    ) {
        if (!satisfies(condition, span)) block()
    }
    
    fun branch(
        span: SourceSpan = this.span,
        block: DiagnosticConditionSwitch<DiagnosticChildScope>.() -> Unit,
    ) {
        DiagnosticConditionSwitch(this, scope, span).apply(block).runDefault()
    }
}

abstract class DiagnosticChildTarget internal constructor(
    protected val scope: DiagnosticChildScope,
) {
    
    abstract fun at(span: SourceSpan)
    
    fun here() {
        at(scope.span)
    }
    
    fun trimTrailingLineBreak() {
        at(scope.trimTrailingLineBreak())
    }
    
    fun trimLeadingLineBreak() {
        at(scope.trimLeadingLineBreak())
    }
    
    fun afterFirstFragment() {
        at(scope.afterFirstFragment())
    }
    
    fun delimitedBy(
        open: Symbol<*>,
        close: Symbol<*>,
    ) {
        at(scope.delimitedSpan(open, close) ?: return)
    }
    
    fun inside(
        open: Symbol<*>,
        close: Symbol<*>,
    ) {
        at(scope.insideDelimiters(open, close) ?: return)
    }
    
    fun after(symbol: Symbol<*>) {
        at(scope.after(symbol) ?: return)
    }
    
    fun afterToken(text: String) {
        after(token(text))
    }
    
    fun afterOrSelf(symbol: Symbol<*>) {
        at(scope.after(symbol) ?: scope.span)
    }
    
    fun afterTokenOrSelf(text: String) {
        afterOrSelf(token(text))
    }
}

class DiagnosticChildExpectation internal constructor(
    scope: DiagnosticChildScope,
    private val symbol: Symbol<*>,
    private val costLimit: Int,
) : DiagnosticChildTarget(scope) {
    
    override fun at(span: SourceSpan) {
        scope.addChild(symbol, span, costLimit)
    }
}

class DiagnosticChildSetExpectation internal constructor(
    scope: DiagnosticChildScope,
    private val children: DiagnosticChildren,
) : DiagnosticChildTarget(scope) {
    
    override fun at(span: SourceSpan) {
        scope.addChildren(children, span)
    }
}

private data class DiagnosticRule(
    val symbol: Symbol<*>,
    val block: DiagnosticScope.() -> Unit,
)

class DiagnosticRules internal constructor(
    private val rulesBySymbol: Map<Symbol<*>, List<DiagnosticScope.() -> Unit>>,
) {
    fun supports(symbol: Symbol<*>): Boolean {
        return symbol in rulesBySymbol
    }
    
    fun propose(
        ctx: DiagnosticContext,
        expectation: Expectation,
    ): Sequence<SeedAction> = sequence {
        for (rule in rulesBySymbol[expectation.symbol].orEmpty()) {
            val scope = DiagnosticScope(ctx, expectation)
            rule(scope)
            yieldAll(scope.actions)
        }
    }
}

private fun DiagnosticContext.splitDiagnosticSlots(
    span: SourceSpan,
    separator: Symbol<*>,
    close: Symbol<*>,
    nesting: List<Pair<Symbol<*>, Symbol<*>>> = emptyList(),
): List<SourceSpan> {
    val result = mutableListOf<SourceSpan>()
    val depths = IntArray(nesting.size)
    var begin = span.begin
    var cursor = span.begin
    
    fun isNested(): Boolean = depths.any { it > 0 }
    
    while (cursor < span.end) {
        if (chart.matchesByBegin(close, cursor).isNotEmpty() && !isNested()) break
        
        val separatorMatch = chart.matchesByBegin(separator, cursor).firstOrNull()
        if (separatorMatch != null && !isNested()) {
            result += SourceSpan(begin, cursor)
            begin = separatorMatch.span.end
            cursor = separatorMatch.span.end
            continue
        }
        
        nesting.forEachIndexed { index, (open, nestedClose) ->
            when {
                chart.matchesByBegin(open, cursor).isNotEmpty() -> depths[index]++
                chart.matchesByBegin(nestedClose, cursor).isNotEmpty() && depths[index] > 0 -> depths[index]--
            }
        }
        cursor += 1
    }
    result += SourceSpan(begin, span.end)
    return result
}

private fun DiagnosticContext.splitDiagnosticLines(span: SourceSpan): List<SourceSpan> {
    val result = mutableListOf<SourceSpan>()
    var begin = span.begin
    var cursor = span.begin
    while (cursor < span.end) {
        val next = cursor + 1
        if (source[cursor] is LineBreakFragment) {
            result += SourceSpan(begin, next)
            begin = next
        }
        cursor = next
    }
    if (begin < span.end) result += SourceSpan(begin, span.end)
    return result
}

private fun DiagnosticContext.firstPlainText(span: SourceSpan): String? {
    var cursor = span.begin
    while (cursor < span.end) {
        val fragment = source[cursor]
        if (fragment is PlainFragment) return fragment.text
        cursor += 1
    }
    return null
}

private fun DiagnosticContext.containsToken(span: SourceSpan, text: String): Boolean {
    return firstMatch(token(text), span) != null
}

private fun DiagnosticContext.firstMatch(symbol: Symbol<*>, span: SourceSpan): ParseMatch<*>? {
    var cursor = span.begin
    while (cursor < span.end) {
        val match = chart.matchesByBegin(symbol, cursor).firstOrNull()
        if (match != null && match.span.end <= span.end) return match
        cursor += 1
    }
    return null
}

private fun DiagnosticContext.findDelimitedSpan(
    span: SourceSpan,
    open: Symbol<*>,
    close: Symbol<*>,
): SourceSpan? {
    val openMatch = firstMatch(open, span) ?: return null
    val closeMatch = chart.matchesByEnd(close, span.end).firstOrNull()
        ?: firstMatch(close, SourceSpan(openMatch.span.end, span.end))
        ?: return null
    return SourceSpan(openMatch.span.begin, closeMatch.span.end)
}

private fun SourceSpan.dropFirstFragment(): SourceSpan {
    return if (isEmpty()) this else SourceSpan(begin + 1, end)
}

private fun SourceSpan.trimTrailingLineBreak(ctx: DiagnosticContext): SourceSpan {
    val previous = if (end.fragIndex == 0) null else SourcePosition(end.fragIndex - 1)
    return if (previous != null && previous >= begin && ctx.source[previous] is LineBreakFragment) {
        SourceSpan(begin, previous)
    } else {
        this
    }
}

private fun SourceSpan.trimLeadingLineBreak(ctx: DiagnosticContext): SourceSpan {
    return if (begin < end && ctx.source[begin] is LineBreakFragment) {
        SourceSpan(begin + 1, end)
    } else {
        this
    }
}

private fun SourceSpan.dropLeadingToken(ctx: DiagnosticContext, text: String): SourceSpan? {
    val match = ctx.chart.matchesByBegin(token(text), begin).firstOrNull() ?: return null
    return SourceSpan(match.span.end, end)
}

private fun SourceSpan.dropDelimiters(
    ctx: DiagnosticContext,
    open: Symbol<*>,
    close: Symbol<*>,
): SourceSpan? {
    val openMatch = ctx.chart.matchesByBegin(open, begin).firstOrNull() ?: return null
    val closeMatch = ctx.chart.matchesByEnd(close, end).firstOrNull() ?: return null
    return SourceSpan(openMatch.span.end, closeMatch.span.begin)
}
