package pers.hpcx.foxlang.frontend

@JvmInline
value class DiagnosticReport(
    val items: List<DiagnosticReportItem>,
)

data class DiagnosticReportItem(
    val severity: DiagnosticSeverity,
    val message: String,
    val symbol: Symbol<*>,
    val span: SourceSpan,
    val range: DiagnosticRange,
)

enum class DiagnosticSeverity {
    Error,
    Warning,
    Information,
}

data class DiagnosticPosition(
    val line: Int,
    val column: Int,
) : Comparable<DiagnosticPosition> {
    
    override fun compareTo(other: DiagnosticPosition) = when {
        line != other.line -> line.compareTo(other.line)
        else -> column.compareTo(other.column)
    }
}

data class DiagnosticRange(
    val begin: DiagnosticPosition,
    val end: DiagnosticPosition,
) {
    init {
        require(begin <= end) { "begin: $begin > end: $end" }
    }
}

fun DiagnosticResult.report() = DiagnosticReporter.report(this)

object DiagnosticReporter {
    
    fun report(result: DiagnosticResult): DiagnosticReport {
        val rootMatches = result.chart.matches(result.root.symbol, result.root.span)
        val factoryItems = collectRuleFactoryItems(result, rootMatches)
        if (rootMatches.any { it.isExactTree() }) return DiagnosticReport(factoryItems)
        
        val root = bestSyntheticMatch(result.chart, result.root.symbol, result.root.span, emptySet())
            ?: return DiagnosticReport(factoryItems)
        
        return (factoryItems + collectIssues(result.chart, root, mutableSetOf())
            .deduplicate()
            .map { it.toReport(result.chart.source) })
            .sortedWith(diagnosticReportItemComparator)
            .let { DiagnosticReport(it) }
    }
    
    private fun collectIssues(
        chart: ParseChart,
        match: ParseMatch<*>,
        seen: MutableSet<ParseMatch<*>>,
    ): List<DiagnosticIssue> {
        if (!seen.add(match)) return emptyList()
        return try {
            when (val origin = match.origin) {
                MatchOrigin.Exact -> emptyList()
                is MatchOrigin.Expected -> {
                    if (chart.matches(match.symbol, match.span).any { it.isExactTree() }) {
                        return emptyList()
                    }
                    val childIssues = origin.children.flatMap { expectation ->
                        collectIssues(chart, expectation, seen)
                    }
                    childIssues.ifEmpty {
                        listOf(DiagnosticIssue(match.symbol, match.span, origin.reason, origin.cost))
                    }
                }
                is MatchOrigin.Derived -> {
                    if (!origin.fromSynthetic) {
                        emptyList()
                    } else {
                        collectSyntheticChildren(chart, match, seen)
                    }
                }
            }
        } finally {
            seen.remove(match)
        }
    }
    
    private fun collectIssues(
        chart: ParseChart,
        expectation: Expectation,
        seen: MutableSet<ParseMatch<*>>,
    ): List<DiagnosticIssue> {
        val match = bestSyntheticMatch(chart, expectation.symbol, expectation.span, seen)
            ?: return emptyList()
        return collectIssues(chart, match, seen)
    }
    
    private fun collectSyntheticChildren(
        chart: ParseChart,
        match: ParseMatch<*>,
        seen: MutableSet<ParseMatch<*>>,
    ): List<DiagnosticIssue> {
        val rule = match.rule as? NonLeafRule<*> ?: return emptyList()
        return rule.components.zip(match.segments).flatMap { (symbol, span) ->
            val child = bestSyntheticMatch(chart, symbol, span, seen) ?: return@flatMap emptyList()
            collectIssues(chart, child, seen)
        }
    }
    
    private fun bestSyntheticMatch(
        chart: ParseChart,
        symbol: Symbol<*>,
        span: SourceSpan,
        seen: Set<ParseMatch<*>>,
    ): ParseMatch<*>? {
        return chart.matches(symbol, span)
            .asSequence()
            .filter { it.hasSyntheticOrigin() && it !in seen }
            .minWithOrNull(
                compareBy<ParseMatch<*>> { syntheticRank(it) }
                    .thenBy { syntheticCost(chart, it, mutableSetOf()) }
                    .thenBy { it.span.length },
            )
    }
    
    private fun syntheticCost(
        chart: ParseChart,
        match: ParseMatch<*>,
        seen: MutableSet<ParseMatch<*>>,
    ): Int {
        if (!seen.add(match)) return Int.MAX_VALUE / 4
        return try {
            when (val origin = match.origin) {
                MatchOrigin.Exact -> 0
                is MatchOrigin.Expected -> origin.cost + origin.children.sumOf { child ->
                    val childMatch = bestSyntheticMatch(chart, child.symbol, child.span, seen)
                    childMatch?.let { syntheticCost(chart, it, seen) } ?: 0
                }
                is MatchOrigin.Derived -> {
                    if (!origin.fromSynthetic) {
                        0
                    } else {
                        collectSyntheticChildMatches(chart, match, seen)
                            .sumOf { syntheticCost(chart, it, seen) }
                            .coerceAtLeast(1)
                    }
                }
            }
        } finally {
            seen.remove(match)
        }
    }
    
    private fun collectSyntheticChildMatches(
        chart: ParseChart,
        match: ParseMatch<*>,
        seen: Set<ParseMatch<*>>,
    ): List<ParseMatch<*>> {
        val rule = match.rule as? NonLeafRule<*> ?: return emptyList()
        return rule.components.zip(match.segments).mapNotNull { (symbol, span) ->
            bestSyntheticMatch(chart, symbol, span, seen)
        }
    }
    
    private fun syntheticRank(match: ParseMatch<*>): Int {
        return when (match.origin) {
            is MatchOrigin.Derived -> 0
            is MatchOrigin.Expected -> 1
            MatchOrigin.Exact -> 2
        }
    }
    
    private fun List<DiagnosticIssue>.deduplicate(): List<DiagnosticIssue> {
        return groupBy { DiagnosticIssueKey(it.symbol, it.span, it.message) }
            .values
            .map { issues -> issues.minBy { it.cost } }
            .sortedWith(
                compareBy<DiagnosticIssue> { it.span.begin.fragIndex }
                    .thenBy { it.span.end.fragIndex }
                    .thenBy { it.cost }
                    .thenBy { it.message },
            )
    }
    
    private fun DiagnosticIssue.toReport(source: Source): DiagnosticReportItem {
        val range = span.toDiagnosticRange(source)
        return DiagnosticReportItem(
            severity = DiagnosticSeverity.Error,
            message = message,
            symbol = symbol,
            span = span,
            range = range,
        )
    }
    
    private fun collectRuleFactoryItems(
        result: DiagnosticResult,
        rootMatches: List<ParseMatch<*>>,
    ): List<DiagnosticReportItem> {
        if (result.exactBuildSucceeded) return emptyList()
        if (rootMatches.none { it.isExactTree() }) return emptyList()
        return result.chart.ruleFactoryFailures()
            .distinct()
            .dropContainedFailures()
            .map { it.toReport(result.chart.source) }
            .sortedWith(diagnosticReportItemComparator)
    }
    
    private fun List<RuleFactoryFailure>.dropContainedFailures(): List<RuleFactoryFailure> {
        return filter { failure ->
            none { other ->
                other != failure && failure.span.isStrictlyContainedIn(other.span)
            }
        }
    }
    
    private fun RuleFactoryFailure.toReport(source: Source): DiagnosticReportItem {
        return DiagnosticReportItem(
            severity = DiagnosticSeverity.Error,
            message = message,
            symbol = symbol,
            span = span,
            range = span.toDiagnosticRange(source),
        )
    }

    private data class DiagnosticIssue(
        val symbol: Symbol<*>,
        val span: SourceSpan,
        val message: String,
        val cost: Int,
    )
    
    private data class DiagnosticIssueKey(
        val symbol: Symbol<*>,
        val span: SourceSpan,
        val message: String,
    )
    
    private val diagnosticReportItemComparator = compareBy<DiagnosticReportItem> { it.span.begin.fragIndex }
        .thenBy { it.span.end.fragIndex }
        .thenBy { it.message }
}

private fun SourceSpan.isStrictlyContainedIn(other: SourceSpan): Boolean {
    return begin >= other.begin && end <= other.end && this != other
}

private fun SourceSpan.toDiagnosticRange(source: Source): DiagnosticRange {
    val rangeBegin = begin.toDiagnosticPosition(source)
    val rangeEnd = if (isEmpty()) rangeBegin else end.toDiagnosticPosition(source)
    return DiagnosticRange(rangeBegin, rangeEnd)
}

private fun SourcePosition.toDiagnosticPosition(source: Source): DiagnosticPosition {
    val fragment = source.getOrNull(this)
    if (fragment != null) return DiagnosticPosition(fragment.line, fragment.column)
    val last = source.lastOrNull() ?: return DiagnosticPosition(0, 0)
    return when (last) {
        is LineBreakFragment -> DiagnosticPosition(last.line + 1, 0)
        else -> DiagnosticPosition(last.line, last.column + last.sourceWidth())
    }
}
