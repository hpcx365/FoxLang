package pers.hpcx.foxlang.frontend

data class Expectation(
    val symbol: Symbol<*>,
    val span: SourceSpan,
) {
    
    fun toParseMatch() = ParseMatch(
        symbol = symbol,
        matchType = ParseMatchType.Expected,
        segments = listOf(span),
        grammarRule = null,
    )
}

class RepairStrategy internal constructor(
    val strategiesBySymbol: Map<Symbol<*>, ExpectationStrategy>,
) {
    fun childExpectations(context: ParseContext, expectation: Expectation): Set<Expectation> {
        val receiver = ExpectationReceiver(context, expectation.span)
        strategiesBySymbol[expectation.symbol]?.let { receiver.apply(it) }
        return receiver.build()
    }
}

typealias ExpectationStrategy = ExpectationReceiver.() -> Unit

class ExpectationReceiver internal constructor(
    private val context: ParseContext,
    private val currentSpan: SourceSpan,
    private val expectations: MutableSet<Expectation> = mutableSetOf(),
) {
    
    fun build() = expectations.toSet()
    
    fun expect(symbol: Symbol<*>) {
        expectations += Expectation(symbol, currentSpan)
    }
    
    fun expect(symbol: Symbol<*>, span: SourceSpan) {
        expectations += Expectation(symbol, span)
    }
    
    fun expectAt(symbol: Symbol<*>, position: SourcePosition) {
        expect(symbol, SourceSpan(position, position))
    }
    
    fun currentSpan() = currentSpan
    
    fun source() = context.source
    
    fun containsBuildable(symbol: Symbol<*>): Boolean {
        return matchesInside(symbol).isNotEmpty()
    }
    
    fun matchesInside(symbol: Symbol<*>, buildableOnly: Boolean = true): List<ParseMatch<*>> {
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
        symbol: Symbol<*>,
        end: SourcePosition,
        buildableOnly: Boolean = true,
    ): List<ParseMatch<*>> {
        return context.matchesByEnd(symbol, end).filter { match ->
            currentSpan.start <= match.span.start && match.span.end <= currentSpan.end &&
                (!buildableOnly || match.matchType.isBuildable())
        }
    }
    
    fun firstInside(symbol: Symbol<*>, buildableOnly: Boolean = true): ParseMatch<*>? {
        return matchesInside(symbol, buildableOnly).minWithOrNull(
            compareBy<ParseMatch<*>> { it.span.start.fragIndex }.thenBy { it.span.end.fragIndex },
        )
    }
    
    fun firstPlainText(): String? {
        var cursor = currentSpan.start
        while (cursor < currentSpan.end) {
            val fragment = context.source[cursor]
            if (fragment is PlainFragment) return fragment.text
            cursor += 1
        }
        return null
    }
    
    fun lastInside(symbol: Symbol<*>, buildableOnly: Boolean = true): ParseMatch<*>? {
        return matchesInside(symbol, buildableOnly).maxWithOrNull(
            compareBy<ParseMatch<*>> { it.span.end.fragIndex }.thenBy { it.span.length },
        )
    }
    
    fun withStart(start: SourcePosition) = withSpan(SourceSpan(start, currentSpan.end))
    fun withEnd(end: SourcePosition) = withSpan(SourceSpan(currentSpan.start, end))
    fun withSpan(start: SourcePosition, end: SourcePosition) = withSpan(SourceSpan(start, end))
    fun withSpan(span: SourceSpan) = ExpectationReceiver(context, span, expectations)
    
    fun lines(): List<ExpectationReceiver> {
        val result = mutableListOf<ExpectationReceiver>()
        var start = currentSpan.start
        var end = currentSpan.start
        while (end < currentSpan.end) {
            val next = end + 1
            if (context.source[end] is LineBreakFragment) {
                result += withSpan(start, next)
                start = next
            }
            end = next
        }
        if (start < currentSpan.end) result += withSpan(start, currentSpan.end)
        return result
    }
}
