package pers.hpcx.foxlang.parser

class Parser<N>(
    val grammar: Grammar,
    val root: NonTerminal<N>,
) {
    
    fun parse(source: String): ParseReport<N> {
        val context = ParseContext(source, grammar)
        setOf(Cursor(0) to root).forEach<Pair<Cursor, NonTerminal<*>>> { seed ->
            context.seedRoot(seed)
        }
        while (context.parseQueue.isNotEmpty()) {
            val (currentCursor, nonTerminal) = context.parseQueue.removeFirst()
            context.dequeue(currentCursor, nonTerminal)
            context.recordStateUpdate(currentCursor, nonTerminal) {
                grammar.productionsFor(nonTerminal).forEach { production ->
                    production.update(context, currentCursor)
                }
            }
        }
        val result = context.memoized(Cursor(0), root)
        val stop = if (result is Success<N> && result.interval.end.fragIndex == context.fragments.size) {
            ParseStopReport(
                farthestCursor = result.interval.end,
                fragment = context[result.interval.end],
                requested = emptyList(),
                successes = emptyList(),
                failures = emptyList(),
                diagnoses = emptyList(),
            )
        } else {
            diagnoseParseFailure(context)
        }
        return ParseReport(
            context = context,
            result = result,
            stop = stop,
        )
    }
    
    fun diagnoseParseFailure(
        context: ParseContext,
        maxItems: Int = 8,
    ): ParseStopReport = enrichStopReport(
        context = context,
        report = parseStopReport(context, maxItemsPerSection = maxItems),
        maxItems = maxItems,
    )
    
    fun collectDiagnoses(
        context: ParseContext,
        cursor: Cursor,
        maxItems: Int = 8,
    ): List<Diagnosis> {
        val requested = context.parseQueueVisited
            .asSequence()
            .filter { (requestedCursor, _) -> requestedCursor == cursor }
            .map { it.second }
            .toSet()
        if (requested.isEmpty()) return emptyList()
        val diagnoses = requested.asSequence()
            .flatMap { grammar.productionsFor(it).asSequence() }
            .mapNotNull { it.diagnose(context, cursor) }
            .toList()
        val filtered = filterDiagnoses(diagnoses, maxItems)
        if (filtered.isNotEmpty()) return filtered
        return requested.asSequence()
            .map {
                Diagnosis(
                    nonTerminal = it,
                    interval = Interval(cursor, cursor),
                    message = "$it was requested here but never resolved",
                )
            }
            .take(maxItems)
            .toList()
    }
    
    fun enrichStopReport(
        context: ParseContext,
        report: ParseStopReport,
        maxItems: Int = 8,
    ): ParseStopReport {
        val diagnoses = collectDiagnoses(context, report.farthestCursor, maxItems)
            .map(::describeDiagnosis)
        return report.copy(diagnoses = diagnoses)
    }
}

fun parseStopReport(context: ParseContext, maxItemsPerSection: Int = 8): ParseStopReport {
    val unresolvedByCursor = context.parseQueueVisited
        .groupBy({ it.first }, { it.second })
        .mapValues { (cursor, requested) ->
            requested.filter { it !in context.memoizedAt(cursor).keys }.toSet()
        }
        .filterValues { it.isNotEmpty() }
    val farthestCursor = unresolvedByCursor.keys.maxOrNull()
        ?: context.memoization
            .values
            .flatMap { it.values }
            .filterIsInstance<Success<*>>()
            .maxByOrNull { it.interval.end.fragIndex }
            ?.interval
            ?.end
        ?: Cursor(0)
    val results = context.memoizedAt(farthestCursor)
    val requested = unresolvedByCursor[farthestCursor]
        .orEmpty()
        .map { it.toString() }
        .sorted()
        .take(maxItemsPerSection)
    val successes = results.values
        .filterIsInstance<Success<*>>()
        .sortedByDescending { it.interval.end.fragIndex }
        .take(maxItemsPerSection)
        .map { describeResult(it) }
    val failures = results.values
        .filterIsInstance<Failure<*>>()
        .sortedBy { it.nonTerminal.toString() }
        .take(maxItemsPerSection)
        .map { describeResult(it) }
    return ParseStopReport(
        farthestCursor = farthestCursor,
        fragment = context[farthestCursor],
        requested = requested,
        successes = successes,
        failures = failures,
        diagnoses = emptyList(),
    )
}

private fun describeResult(result: ParseResult<*>): String {
    val nonTerminal = result.nonTerminal
    val interval = "${result.interval.begin.fragIndex}..${result.interval.end.fragIndex}"
    return when (result) {
        is Success<*> -> "$nonTerminal success @$interval"
        is Failure<*> -> "$nonTerminal failure @$interval: ${result.message}"
    }
}

private fun describeDiagnosis(diagnosis: Diagnosis): String {
    val interval = "${diagnosis.interval.begin.fragIndex}..${diagnosis.interval.end.fragIndex}"
    val suffix = if (diagnosis.details.isEmpty()) {
        ""
    } else {
        diagnosis.details.joinToString(
            prefix = " [",
            postfix = "]",
            separator = "; ",
        )
    }
    return "${diagnosis.nonTerminal} maybe @$interval: ${diagnosis.message}$suffix"
}

private fun filterDiagnoses(
    diagnoses: List<Diagnosis>,
    maxItems: Int,
): List<Diagnosis> {
    if (diagnoses.isEmpty()) return emptyList()
    val deduplicated = diagnoses
        .groupBy { it.nonTerminal }
        .map { (_, candidates) ->
            candidates.maxWithOrNull(
                compareBy(
                    ::diagnosisTier,
                    Diagnosis::matchedParts,
                    Diagnosis::confidence,
                    { it.interval.end.fragIndex - it.interval.begin.fragIndex },
                ),
            )!!
        }
    val hasStructured = deduplicated.any { diagnosisTier(it) >= 2 }
    val filtered = if (hasStructured) {
        deduplicated.filterNot(::isLowSignalLeaf)
    } else {
        deduplicated
    }
    return filtered
        .sortedWith(
            compareByDescending<Diagnosis> { diagnosisTier(it) }
                .thenByDescending { it.matchedParts }
                .thenByDescending { it.confidence }
                .thenByDescending { it.interval.end.fragIndex - it.interval.begin.fragIndex }
                .thenBy { it.nonTerminal.toString() },
        )
        .take(maxItems)
}

private fun diagnosisTier(diagnosis: Diagnosis): Int = when {
    diagnosis.details.isNotEmpty() && diagnosis.matchedParts > 0 -> 3
    diagnosis.matchedParts > 0 -> 2
    diagnosis.details.isNotEmpty() -> 1
    else -> 0
}

private fun isLowSignalLeaf(diagnosis: Diagnosis): Boolean {
    if (diagnosisTier(diagnosis) > 0) return false
    val name = diagnosis.nonTerminal.toString()
    if (name in setOf("Boolean", "Int", "Long", "Float", "Double", "Char", "String", "CallTarget", "Label")) {
        return true
    }
    return name.all { !it.isLetterOrDigit() }
}
