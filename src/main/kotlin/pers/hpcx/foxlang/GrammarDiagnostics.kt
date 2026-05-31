package pers.hpcx.foxlang

import pers.hpcx.foxlang.utils.*
import java.util.*

data class GrammarCheckReport(
    val undefinedNonTerminals: Set<NonTerminal<*>>,
    val unreachableNonTerminals: Set<NonTerminal<*>>,
    val emptyResults: Set<NonTerminal<*>>,
    val duplicateProductions: List<DuplicateProduction>,
) {
    fun isClean(): Boolean {
        return undefinedNonTerminals.isEmpty() &&
            unreachableNonTerminals.isEmpty() &&
            emptyResults.isEmpty() &&
            duplicateProductions.isEmpty()
    }
}

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

sealed interface BaseParseReport<N> {
    val scanner: SourceScanner
    val result: ParseResult<N>?
}

data class ParseRunReport<N>(
    override val scanner: SourceScanner,
    override val result: ParseResult<N>?,
    val stop: ParseStopReport,
) : BaseParseReport<N>

data class ParseReport<N>(
    override val scanner: SourceScanner,
    override val result: ParseResult<N>?,
) : BaseParseReport<N>

fun checkGrammar(
    productions: List<Production<*>>,
    roots: Set<NonTerminal<*>>,
): GrammarCheckReport {
    val defined = productions.map { it.result }.toSet()
    val referenced = productions.flatMap { referencedNonTerminals(it) }.toSet()
    val reachable = reachableNonTerminals(productions, roots)
    return GrammarCheckReport(
        undefinedNonTerminals = referenced - defined,
        unreachableNonTerminals = defined - reachable,
        emptyResults = emptySet(),
        duplicateProductions = findDuplicateProductions(productions),
    )
}

fun runToFixpoint(
    scanner: SourceScanner,
    productions: List<Production<*>>,
    roots: Set<Pair<Cursor, NonTerminal<*>>>,
) {
    runToFixpoint(scanner, groupedProductions(productions), roots)
}

fun <N> parse(
    source: String,
    productions: List<Production<*>>,
    root: NonTerminal<N>,
    cursor: Cursor = Cursor(0),
): ParseReport<N> {
    val scanner = SourceScanner(source)
    runToFixpoint(scanner, productions, setOf(cursor to root))
    return ParseReport(
        scanner = scanner,
        result = scanner.memoized(cursor, root),
    )
}

fun <N> parseFully(
    source: String,
    productions: List<Production<*>>,
    root: NonTerminal<N>,
    cursor: Cursor = Cursor(0),
): Success<N>? {
    val report = parse(source, productions, root, cursor)
    val scanner = report.scanner
    val result = report.result
    val success = result as? Success<N> ?: return null
    if (success.interval.end.fragIndex != scanner.fragments.size) return null
    return success
}

private fun runToFixpoint(
    scanner: SourceScanner,
    grouped: Map<NonTerminal<*>, List<Production<*>>>,
    roots: Set<Pair<Cursor, NonTerminal<*>>>,
) {
    while (true) {
        roots.forEach { root ->
            scanner.parseQueue += root
            scanner.parseQueueVisited += root
        }
        while (scanner.parseQueue.isNotEmpty()) {
            val (cursor, nonTerminal) = scanner.parseQueue.removeFirst()
            grouped[nonTerminal].orEmpty().forEach { production ->
                production.update(scanner, cursor)
            }
        }
        if (!scanner.changed) break
        scanner.changed = false
        scanner.parseQueue.clear()
        scanner.parseQueueVisited.clear()
    }
}

fun <N> parseWithDiagnostics(
    source: String,
    productions: List<Production<*>>,
    root: NonTerminal<N>,
    cursor: Cursor = Cursor(0),
): ParseRunReport<N> {
    val report = parse(source, productions, root, cursor)
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
        diagnoseParseFailure(scanner, productions)
    }
    return ParseRunReport(
        scanner = scanner,
        result = result,
        stop = stop,
    )
}

fun diagnoseParseFailure(
    scanner: SourceScanner,
    productions: List<Production<*>>,
    maxItems: Int = 8,
): ParseStopReport = enrichStopReport(
    scanner = scanner,
    productions = productions,
    report = parseStopReport(scanner, maxItemsPerSection = maxItems),
    maxItems = maxItems,
)

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

fun collectDiagnoses(
    scanner: SourceScanner,
    productions: List<Production<*>>,
    cursor: Cursor,
    maxItems: Int = 8,
): List<Diagnosis> {
    val requested = scanner.parseQueueVisited
        .asSequence()
        .filter { (requestedCursor, _) -> requestedCursor == cursor }
        .map { it.second }
        .toSet()
    if (requested.isEmpty()) return emptyList()
    val diagnoses = productions.asSequence()
        .filter { it.result in requested }
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
    productions: List<Production<*>>,
    report: ParseStopReport,
    maxItems: Int = 8,
): ParseStopReport {
    val diagnoses = collectDiagnoses(scanner, productions, report.farthestCursor, maxItems)
        .map(::describeDiagnosis)
    return report.copy(diagnoses = diagnoses)
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

private fun reachableNonTerminals(
    productions: List<Production<*>>,
    roots: Set<NonTerminal<*>>,
): Set<NonTerminal<*>> {
    val grouped = productions.groupBy { it.result }
    val visited = mutableSetOf<NonTerminal<*>>()
    val queue = ArrayDeque<NonTerminal<*>>()
    roots.forEach {
        if (visited.add(it)) queue += it
    }
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        grouped[current].orEmpty()
            .flatMap(::referencedNonTerminals)
            .forEach {
                if (visited.add(it)) queue += it
            }
    }
    return visited
}

private fun referencedNonTerminals(production: Production<*>): List<NonTerminal<*>> = when (production) {
    is SerialProduction<*> -> production.comps
    is ListProduction<*> -> listOfNotNull(production.begin, production.element, production.separator, production.end)
    else -> emptyList()
}

private val groupedProductionsCache =
    Collections.synchronizedMap(
        IdentityHashMap<List<Production<*>>, Map<NonTerminal<*>, List<Production<*>>>>(),
    )

private fun groupedProductions(
    productions: List<Production<*>>,
): Map<NonTerminal<*>, List<Production<*>>> = groupedProductionsCache.getOrPut(productions) {
    productions.groupBy { it.result }
}

private fun findDuplicateProductions(
    productions: List<Production<*>>,
): List<DuplicateProduction> = productions
    .groupingBy(::describeProduction)
    .eachCount()
    .filterValues { it > 1 }
    .entries
    .sortedBy { it.key }
    .map { (description, count) ->
        DuplicateProduction(description, count)
    }

private fun describeProduction(production: Production<*>): String = when (production) {
    is FixedProduction -> "${production.result.displayName()} ::= '${production.string}'"
    is RegexProduction -> "${production.result.displayName()} ::= /${production.regex.pattern}/"
    is CharLiteralProduction -> "${production.result.displayName()} ::= <char literal>"
    is StringLiteralProduction -> "${production.result.displayName()} ::= <string literal>"
    is FormattedStringLiteralProduction -> "${production.result.displayName()} ::= <formatted string literal>"
    is SerialProduction<*> -> "${production.result.displayName()} ::= ${production.comps.joinToString(" ") { it.displayName() }}"
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

private fun NonTerminal<*>.displayName(): String = when (this) {
    is NamedNonTerminal<*> -> name
    is ClassNonTerminal<*> -> clazz.simpleName ?: clazz.toString()
    is PairNonTerminal<*, *> -> "Pair<${first.displayName()}, ${second.displayName()}>"
    is ListNonTerminal<*> -> "List<${type.displayName()}>"
    is SetNonTerminal<*> -> "Set<${type.displayName()}>"
    is SeqSetNonTerminal<*> -> "SeqSet<${type.displayName()}>"
    is MapNonTerminal<*, *> -> "Map<${key.displayName()}, ${value.displayName()}>"
    is SeqMapNonTerminal<*, *> -> "SeqMap<${key.displayName()}, ${value.displayName()}>"
}
