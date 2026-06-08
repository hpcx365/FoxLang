package pers.hpcx.foxlang.parser

data class ParseReport<N>(
    val context: ParseContext,
    val result: ParseResult<N>?,
    val stop: ParseStopReport,
)

data class ParseStopReport(
    val farthestCursor: Cursor,
    val fragment: SourceFragment?,
    val requested: List<String>,
    val successes: List<String>,
    val failures: List<String>,
    val diagnoses: List<String>,
) {
    override fun toString() = buildString {
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
}

class ParseException(override val message: String) : Exception(message)

sealed interface ParseResult<N> : Comparable<ParseResult<*>> {
    val interval: Interval
    val nonTerminal: NonTerminal<N>
}

class Success<N>(
    val node: N,
    override val interval: Interval,
    override val nonTerminal: NonTerminal<N>,
) : ParseResult<N> {
    
    init {
        require(interval.isNotEmpty()) { "Empty interval" }
    }
    
    override fun compareTo(other: ParseResult<*>): Int {
        check(nonTerminal == other.nonTerminal)
        when (other) {
            is Success -> {}
            is Failure -> return 1
        }
        return interval.compareTo(other.interval)
    }
}

class Failure<N>(
    val message: String,
    override val interval: Interval,
    override val nonTerminal: NonTerminal<N>,
) : ParseResult<N> {
    
    override fun compareTo(other: ParseResult<*>): Int {
        check(nonTerminal == other.nonTerminal)
        when (other) {
            is Success -> return -1
            is Failure -> {}
        }
        return interval.compareTo(other.interval)
    }
}

data class Diagnosis(
    val nonTerminal: NonTerminal<*>,
    val interval: Interval,
    val message: String,
    val matchedParts: Int = 0,
    val confidence: Int = 0,
    val details: List<String> = emptyList(),
) : Comparable<Diagnosis> {
    override fun compareTo(other: Diagnosis): Int {
        return compareValuesBy(this, other, Diagnosis::interval, Diagnosis::matchedParts, Diagnosis::confidence)
    }
}
