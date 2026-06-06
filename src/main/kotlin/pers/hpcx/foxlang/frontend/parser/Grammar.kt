package pers.hpcx.foxlang.frontend.parser

data class ParseReport<N>(
    val scanner: SourceScanner,
    val result: ParseResult<N>?,
)

data class GrammarCheckReport(
    val undefinedNonTerminals: Set<NonTerminal<*>>,
    val unreachableNonTerminals: Set<NonTerminal<*>>,
    val emptyResults: Set<NonTerminal<*>>,
    val duplicateProductions: List<DuplicateProduction>,
)

data class DuplicateProduction(
    val description: String,
    val count: Int,
)

data class ParseStopReport(
    val farthestCursor: Cursor,
    val fragment: SourceFragment?,
    val requested: List<String>,
    val successes: List<String>,
    val failures: List<String>,
    val diagnoses: List<String>,
)

data class ParseRunReport<N>(
    val scanner: SourceScanner,
    val result: ParseResult<N>?,
    val stop: ParseStopReport,
)

class Grammar(
    val productions: List<Production<*>>,
) {
    val groupedProductions by lazy {
        productions.groupBy { it.result }
    }
    
    val starterFirstSets by lazy {
        val first = mutableMapOf<NonTerminal<*>, MutableSet<StarterShape>>()
        val dependents = mutableMapOf<NonTerminal<*>, MutableSet<NonTerminal<*>>>()
        val queue = ArrayDeque(groupedProductions.keys)
        val queued = groupedProductions.keys.toMutableSet()
        while (queue.isNotEmpty()) {
            val nonTerminal = queue.removeFirst()
            queued.remove(nonTerminal)
            val target = first.getOrPut(nonTerminal) { mutableSetOf() }
            var changed = false
            productionsFor(nonTerminal).forEach { production ->
                referencedNonTerminals(production).forEach { child ->
                    dependents.getOrPut(child) { mutableSetOf() }.add(nonTerminal)
                }
                starterShapesFor(production, first).forEach { shape ->
                    if (target.add(shape)) {
                        changed = true
                    }
                }
            }
            if (changed) {
                dependents[nonTerminal].orEmpty().forEach { dependent ->
                    if (queued.add(dependent)) {
                        queue.addLast(dependent)
                    }
                }
            }
        }
        first.mapValues { it.value.toSet() }
    }
    
    fun <N> parse(
        source: String,
        root: NonTerminal<N>,
        cursor: Cursor = Cursor(0),
        starters: StarterSpec? = null,
    ): ParseReport<N> {
        val scanner = SourceScanner(source, starters)
        setOf<Pair<Cursor, NonTerminal<N>>>(cursor to root).forEach<Pair<Cursor, NonTerminal<*>>> { seed ->
            scanner.seedRoot(seed)
        }
        while (scanner.parseQueue.isNotEmpty()) {
            val (currentCursor, nonTerminal) = scanner.parseQueue.removeFirst()
            scanner.dequeue(currentCursor, nonTerminal)
            scanner.recordStateUpdate(currentCursor, nonTerminal) {
                productionsFor(nonTerminal).forEach { production ->
                    production.update(scanner, currentCursor)
                }
            }
        }
        return ParseReport(
            scanner = scanner,
            result = scanner.memoized(cursor, root),
        )
    }
    
    fun <N> parseWithDiagnostics(
        source: String,
        root: NonTerminal<N>,
        cursor: Cursor = Cursor(0),
        starters: StarterSpec? = null,
    ): ParseRunReport<N> {
        val report = parse(source, root, cursor, starters)
        val scanner = report.scanner
        val result = report.result
        val stop = if (result is Success<N> && result.interval.end.fragIndex == scanner.fragments.size) {
            ParseStopReport(
                farthestCursor = result.interval.end,
                fragment = scanner[result.interval.end],
                requested = emptyList(),
                successes = emptyList(),
                failures = emptyList(),
                diagnoses = emptyList(),
            )
        } else {
            diagnoseParseFailure(scanner)
        }
        return ParseRunReport(
            scanner = scanner,
            result = result,
            stop = stop,
        )
    }
    
    fun diagnoseParseFailure(
        scanner: SourceScanner,
        maxItems: Int = 8,
    ): ParseStopReport = enrichStopReport(
        scanner = scanner,
        report = parseStopReport(scanner, maxItemsPerSection = maxItems),
        maxItems = maxItems,
    )
    
    fun collectDiagnoses(
        scanner: SourceScanner,
        cursor: Cursor,
        maxItems: Int = 8,
    ): List<Diagnosis> {
        val requested = scanner.parseQueueVisited
            .asSequence()
            .filter { (requestedCursor, _) -> requestedCursor == cursor }
            .map { it.second }
            .toSet()
        if (requested.isEmpty()) return emptyList()
        val diagnoses = requested.asSequence()
            .flatMap { productionsFor(it).asSequence() }
            .mapNotNull { it.diagnose(scanner, cursor) }
            .toList()
        val filtered = filterDiagnoses(diagnoses, maxItems)
        if (filtered.isNotEmpty()) return filtered
        return requested.asSequence()
            .map {
                Diagnosis(
                    nonTerminal = it,
                    interval = Interval(cursor, cursor),
                    message = "${it.displayName()} was requested here but never resolved",
                )
            }
            .take(maxItems)
            .toList()
    }
    
    fun enrichStopReport(
        scanner: SourceScanner,
        report: ParseStopReport,
        maxItems: Int = 8,
    ): ParseStopReport {
        val diagnoses = collectDiagnoses(scanner, report.farthestCursor, maxItems)
            .map(::describeDiagnosis)
        return report.copy(diagnoses = diagnoses)
    }
    
    fun check(
        roots: Set<NonTerminal<*>>,
    ): GrammarCheckReport {
        val defined = productions.map { it.result }.toSet()
        val referenced = productions.flatMap(::referencedNonTerminals).toSet()
        val reachable = reachableNonTerminals(roots)
        return GrammarCheckReport(
            undefinedNonTerminals = referenced - defined,
            unreachableNonTerminals = defined - reachable,
            emptyResults = emptySet(),
            duplicateProductions = findDuplicateProductions(),
        )
    }
    
    internal fun productionsFor(nonTerminal: NonTerminal<*>): List<Production<*>> =
        groupedProductions[nonTerminal].orEmpty()
    
    private fun reachableNonTerminals(
        roots: Set<NonTerminal<*>>,
    ): Set<NonTerminal<*>> {
        val visited = mutableSetOf<NonTerminal<*>>()
        val queue = ArrayDeque<NonTerminal<*>>()
        roots.forEach {
            if (visited.add(it)) queue += it
        }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            productionsFor(current)
                .flatMap(::referencedNonTerminals)
                .forEach {
                    if (visited.add(it)) queue += it
                }
        }
        return visited
    }
    
    private fun findDuplicateProductions(): List<DuplicateProduction> = productions
        .groupingBy(::describeProduction)
        .eachCount()
        .filterValues { it > 1 }
        .entries
        .sortedBy { it.key }
        .map { (description, count) -> DuplicateProduction(description, count) }
}

fun parseStopReport(scanner: SourceScanner, maxItemsPerSection: Int = 8): ParseStopReport {
    val unresolvedByCursor = scanner.parseQueueVisited
        .groupBy({ it.first }, { it.second })
        .mapValues { (cursor, requested) ->
            requested.filter { it !in scanner.memoizedAt(cursor).keys }.toSet()
        }
        .filterValues { it.isNotEmpty() }
    val farthestCursor = unresolvedByCursor.keys.maxOrNull()
        ?: scanner.memoization
            .values
            .flatMap { it.values }
            .filterIsInstance<Success<*>>()
            .maxByOrNull { it.interval.end.fragIndex }
            ?.interval
            ?.end
        ?: Cursor(0)
    val results = scanner.memoizedAt(farthestCursor)
    val requested = unresolvedByCursor[farthestCursor]
        .orEmpty()
        .map { it.displayName() }
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
        fragment = scanner[farthestCursor],
        requested = requested,
        successes = successes,
        failures = failures,
        diagnoses = emptyList(),
    )
}

fun GrammarCheckReport.render(): String = buildString {
    appendLine("undefined: ${undefinedNonTerminals.joinToString { it.displayName() }}")
    appendLine("unreachable: ${unreachableNonTerminals.joinToString { it.displayName() }}")
    appendLine("empty: ${emptyResults.joinToString { it.displayName() }}")
    appendLine("duplicate productions: ${duplicateProductions.joinToString { "${it.description} x${it.count}" }}")
}

fun ParseStopReport.render(): String = buildString {
    appendLine("stop cursor: ${farthestCursor.fragIndex}")
    appendLine("fragment: ${fragment ?: "<eof>"}")
    appendLine("requested:")
    requested.forEach { appendLine("  $it") }
    appendLine("successes:")
    successes.forEach { appendLine("  $it") }
    appendLine("failures:")
    failures.forEach { appendLine("  $it") }
    appendLine("diagnoses:")
    diagnoses.forEach { appendLine("  $it") }
}

private fun describeResult(result: ParseResult<*>): String {
    val nonTerminal = result.nonTerminal.displayName()
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
    return "${diagnosis.nonTerminal.displayName()} maybe @$interval: ${diagnosis.message}$suffix"
}

private fun filterDiagnoses(
    diagnoses: List<Diagnosis>,
    maxItems: Int,
): List<Diagnosis> {
    if (diagnoses.isEmpty()) return emptyList()
    val deduplicated = diagnoses
        .groupBy { it.nonTerminal.displayName() }
        .map { (_, candidates) ->
            candidates.maxWithOrNull(
                compareBy<Diagnosis>(
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
                .thenBy { it.nonTerminal.displayName() },
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
    val name = diagnosis.nonTerminal.displayName()
    if (name in setOf("Boolean", "Int", "Long", "Float", "Double", "Char", "String", "CallTarget", "Label")) {
        return true
    }
    return name.all { !it.isLetterOrDigit() }
}

private fun describeProduction(production: Production<*>): String = when (production) {
    is FixedProduction -> "${production.result.displayName()} ::= '${production.string}'"
    is RegexProduction -> "${production.result.displayName()} ::= /${production.regex.pattern}/"
    is CharLiteralProduction -> "${production.result.displayName()} ::= <char literal>"
    is StringLiteralProduction -> "${production.result.displayName()} ::= <string literal>"
    is FormattedStringLiteralProduction -> "${production.result.displayName()} ::= <formatted string literal>"
    is SerialProduction<*> -> "${production.result.displayName()} ::= ${production.components.joinToString(" ") { it.displayName() }}"
    is ListProduction<*> -> buildString {
        append(production.result.displayName())
        append(" ::= list(")
        append("begin=")
        append(production.begin?.displayName() ?: "<none>")
        append(", element=")
        append(production.element.displayName())
        append(", separator=")
        append(production.separator?.displayName() ?: "<none>")
        append(", end=")
        append(production.end?.displayName() ?: "<none>")
        append(")")
    }
}

private fun starterShapesFor(
    production: Production<*>,
    known: Map<NonTerminal<*>, Set<StarterShape>>,
): Set<StarterShape> = when (production) {
    is FixedProduction -> setOf(ExactToken(production.string))
    is RegexProduction -> setOf(WordToken)
    is CharLiteralProduction -> setOf(CharLiteralToken)
    is StringLiteralProduction -> setOf(StringLiteralToken)
    is FormattedStringLiteralProduction -> setOf(FormattedStringLiteralToken)
    is SerialProduction<*> -> production.components.firstOrNull()?.let { known[it].orEmpty() } ?: emptySet()
    is ListProduction<*> -> when {
        production.begin != null -> known[production.begin].orEmpty()
        else -> known[production.element].orEmpty()
    }
}

private fun referencedNonTerminals(production: Production<*>): List<NonTerminal<*>> = when (production) {
    is FixedProduction -> emptyList()
    is RegexProduction -> emptyList()
    is CharLiteralProduction -> emptyList()
    is StringLiteralProduction -> emptyList()
    is FormattedStringLiteralProduction -> emptyList()
    is SerialProduction<*> -> production.components
    is ListProduction<*> -> listOfNotNull(production.begin, production.element, production.separator, production.end)
}

