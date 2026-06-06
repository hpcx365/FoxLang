package pers.hpcx.foxlang.frontend.parser

import java.util.*
import kotlin.reflect.KClass

inline fun <reified T : Any> node() = ClassNonTerminal(T::class)
fun token(token: String) = node<String>().name(token)
fun <F, S> NonTerminal<F>.pair(second: NonTerminal<S>): NonTerminal<Pair<F, S>> = PairNonTerminal(this, second)
fun <N> NonTerminal<N>.list(): NonTerminal<List<N>> = ListNonTerminal(this)
fun <N> NonTerminal<N>.set(): NonTerminal<Set<N>> = SetNonTerminal(this)
fun <N> NonTerminal<N>.seqSet(): NonTerminal<SequencedSet<N>> = SeqSetNonTerminal(this)
fun <K, V> NonTerminal<K>.map(value: NonTerminal<V>): NonTerminal<Map<K, V>> = MapNonTerminal(this, value)
fun <K, V> NonTerminal<K>.seqMap(value: NonTerminal<V>): NonTerminal<SequencedMap<K, V>> = SeqMapNonTerminal(this, value)
fun <N> NonTerminal<N>.name(name: String): NonTerminal<N> = NamedNonTerminal(this, name)

fun fixed(
    result: NonTerminal<String>,
    string: String,
) = FixedProduction(result, string)

fun regex(
    result: NonTerminal<String>,
    regex: Regex,
    expectation: String,
) = RegexProduction(result, regex, expectation)

fun charLiteral(
    result: NonTerminal<Char>,
) = CharLiteralProduction(result)

fun stringLiteral(
    result: NonTerminal<String>,
) = StringLiteralProduction(result)

fun formattedStringLiteral(
    result: NonTerminal<FormattedStringTemplate>,
) = FormattedStringLiteralProduction(result)

@Suppress("UNCHECKED_CAST")
fun <N0, N> serial(
    result: NonTerminal<N>,
    comp0: NonTerminal<N0>,
    factory: (N0) -> N,
) = SerialProduction(result, listOf(comp0)) { list ->
    factory(list[0] as N0)
}

@Suppress("UNCHECKED_CAST")
fun <N0, N1, N> serial(
    result: NonTerminal<N>,
    comp0: NonTerminal<N0>,
    comp1: NonTerminal<N1>,
    factory: (N0, N1) -> N,
) = SerialProduction(result, listOf(comp0, comp1)) { list ->
    factory(list[0] as N0, list[1] as N1)
}

@Suppress("UNCHECKED_CAST")
fun <N0, N1, N2, N> serial(
    result: NonTerminal<N>,
    comp0: NonTerminal<N0>,
    comp1: NonTerminal<N1>,
    comp2: NonTerminal<N2>,
    factory: (N0, N1, N2) -> N,
) = SerialProduction(result, listOf(comp0, comp1, comp2)) { list ->
    factory(list[0] as N0, list[1] as N1, list[2] as N2)
}

@Suppress("UNCHECKED_CAST")
fun <N0, N1, N2, N3, N> serial(
    result: NonTerminal<N>,
    comp0: NonTerminal<N0>,
    comp1: NonTerminal<N1>,
    comp2: NonTerminal<N2>,
    comp3: NonTerminal<N3>,
    factory: (N0, N1, N2, N3) -> N,
) = SerialProduction(result, listOf(comp0, comp1, comp2, comp3)) { list ->
    factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3)
}

@Suppress("UNCHECKED_CAST")
fun <N0, N1, N2, N3, N4, N> serial(
    result: NonTerminal<N>,
    comp0: NonTerminal<N0>,
    comp1: NonTerminal<N1>,
    comp2: NonTerminal<N2>,
    comp3: NonTerminal<N3>,
    comp4: NonTerminal<N4>,
    factory: (N0, N1, N2, N3, N4) -> N,
) = SerialProduction(result, listOf(comp0, comp1, comp2, comp3, comp4)) { list ->
    factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4)
}

@Suppress("UNCHECKED_CAST")
fun <N0, N1, N2, N3, N4, N5, N> serial(
    result: NonTerminal<N>,
    comp0: NonTerminal<N0>,
    comp1: NonTerminal<N1>,
    comp2: NonTerminal<N2>,
    comp3: NonTerminal<N3>,
    comp4: NonTerminal<N4>,
    comp5: NonTerminal<N5>,
    factory: (N0, N1, N2, N3, N4, N5) -> N,
) = SerialProduction(result, listOf(comp0, comp1, comp2, comp3, comp4, comp5)) { list ->
    factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5)
}

@Suppress("UNCHECKED_CAST")
fun <N0, N1, N2, N3, N4, N5, N6, N> serial(
    result: NonTerminal<N>,
    comp0: NonTerminal<N0>,
    comp1: NonTerminal<N1>,
    comp2: NonTerminal<N2>,
    comp3: NonTerminal<N3>,
    comp4: NonTerminal<N4>,
    comp5: NonTerminal<N5>,
    comp6: NonTerminal<N6>,
    factory: (N0, N1, N2, N3, N4, N5, N6) -> N,
) = SerialProduction(result, listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6)) { list ->
    factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5, list[6] as N6)
}

@Suppress("UNCHECKED_CAST")
fun <N0, N1, N2, N3, N4, N5, N6, N7, N> serial(
    result: NonTerminal<N>,
    comp0: NonTerminal<N0>,
    comp1: NonTerminal<N1>,
    comp2: NonTerminal<N2>,
    comp3: NonTerminal<N3>,
    comp4: NonTerminal<N4>,
    comp5: NonTerminal<N5>,
    comp6: NonTerminal<N6>,
    comp7: NonTerminal<N7>,
    factory: (N0, N1, N2, N3, N4, N5, N6, N7) -> N,
) = SerialProduction(result, listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6, comp7)) { list ->
    factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5, list[6] as N6, list[7] as N7)
}

fun <N> listLike(
    result: NonTerminal<List<N>>,
    begin: NonTerminal<*>?,
    element: NonTerminal<N>,
    separator: NonTerminal<*>?,
    end: NonTerminal<*>?,
) = ListProduction(result, begin, element, separator, end)

sealed interface StarterShape
data class ExactToken(val text: String) : StarterShape
data object WordToken : StarterShape
data object CharLiteralToken : StarterShape
data object StringLiteralToken : StarterShape
data object FormattedStringLiteralToken : StarterShape
data object EofToken : StarterShape

typealias StarterRefinement = SourceScanner.(Cursor, NonTerminal<*>) -> Boolean

data class StarterSpec(
    val firstSets: Map<NonTerminal<*>, Set<StarterShape>>,
    val refinements: Map<NonTerminal<*>, StarterRefinement> = emptyMap(),
)

sealed interface NonTerminal<N>
data class ClassNonTerminal<N : Any>(val clazz: KClass<N>) : NonTerminal<N>
data class PairNonTerminal<F, S>(val first: NonTerminal<F>, val second: NonTerminal<S>) : NonTerminal<Pair<F, S>>
data class ListNonTerminal<N>(val type: NonTerminal<N>) : NonTerminal<List<N>>
data class SetNonTerminal<N>(val type: NonTerminal<N>) : NonTerminal<Set<N>>
data class SeqSetNonTerminal<N>(val type: NonTerminal<N>) : NonTerminal<SequencedSet<N>>
data class MapNonTerminal<K, V>(val key: NonTerminal<K>, val value: NonTerminal<V>) : NonTerminal<Map<K, V>>
data class SeqMapNonTerminal<K, V>(val key: NonTerminal<K>, val value: NonTerminal<V>) : NonTerminal<SequencedMap<K, V>>
data class NamedNonTerminal<N>(val type: NonTerminal<N>, val name: String) : NonTerminal<N>

sealed interface ParseResult<N> : Comparable<ParseResult<*>> {
    val interval: Interval
    val nonTerminal: NonTerminal<N>
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

class ParseException(override val message: String) : Exception(message)

sealed interface Production<N> {
    val result: NonTerminal<N>
    fun update(scanner: SourceScanner, cursor: Cursor)
    fun diagnose(scanner: SourceScanner, cursor: Cursor): Diagnosis? = null
}

class FixedProduction(
    override val result: NonTerminal<String>,
    val string: String,
) : Production<String> {
    override fun update(scanner: SourceScanner, cursor: Cursor) {
        scanner.memoize(
            exactMatch(scanner, cursor)?.let { end ->
                Success(
                    node = string,
                    interval = Interval(cursor, end),
                    nonTerminal = result,
                )
            } ?: Failure(
                message = "Expected '${string}'",
                interval = Interval(cursor, cursor),
                nonTerminal = result,
            ),
        )
    }
    
    override fun diagnose(scanner: SourceScanner, cursor: Cursor): Diagnosis? {
        if (exactMatch(scanner, cursor) != null) return null
        val fragment = scanner[cursor]
        val end = if (fragment == null) cursor else cursor + 1
        return Diagnosis(
            nonTerminal = result,
            interval = Interval(cursor, end),
            message = "Expected '$string'",
            confidence = 1,
        )
    }
    
    private fun exactMatch(scanner: SourceScanner, cursor: Cursor): Cursor? {
        var current = cursor
        var previous: PlainFragment? = null
        var candidate = ""
        while (candidate.length < string.length) {
            val next = scanner[current]
            if (next !is PlainFragment) break
            if (previous != null) {
                if (next.line != previous.line || next.column != previous.column + previous.text.length) break
            }
            candidate += next.text
            if (!string.startsWith(candidate)) return null
            current += 1
            previous = next
        }
        return current.takeIf { candidate == string }
    }
}

class RegexProduction(
    override val result: NonTerminal<String>,
    val regex: Regex,
    val expectation: String,
) : Production<String> {
    override fun update(scanner: SourceScanner, cursor: Cursor) {
        scanner.memoize(
            longestMatch(scanner, cursor)?.let { (match, end) ->
                Success(
                    node = match,
                    interval = Interval(cursor, end),
                    nonTerminal = result,
                )
            } ?: Failure(
                message = "Expected '${expectation}', pattern '${regex.pattern}'",
                interval = Interval(cursor, cursor),
                nonTerminal = result,
            ),
        )
    }
    
    override fun diagnose(scanner: SourceScanner, cursor: Cursor): Diagnosis? {
        if (longestMatch(scanner, cursor) != null) return null
        val fragment = scanner[cursor]
        val end = if (fragment == null) cursor else cursor + 1
        return Diagnosis(
            nonTerminal = result,
            interval = Interval(cursor, end),
            message = "Expected $expectation matching /${regex.pattern}/",
            confidence = 1,
        )
    }
    
    private fun longestMatch(scanner: SourceScanner, cursor: Cursor): Pair<String, Cursor>? {
        var current = cursor
        var previous: PlainFragment? = null
        var candidate = ""
        var best: Pair<String, Cursor>? = null
        while (true) {
            val next = scanner[current]
            if (next !is PlainFragment) break
            if (previous != null) {
                if (next.line != previous.line || next.column != previous.column + previous.text.length) break
            }
            candidate += next.text
            current += 1
            if (regex.matches(candidate)) {
                best = candidate to current
            }
            previous = next
        }
        return best
    }
}

class CharLiteralProduction(
    override val result: NonTerminal<Char>,
) : Production<Char> {
    override fun update(scanner: SourceScanner, cursor: Cursor) {
        val fragment = scanner[cursor]
        scanner.memoize(
            if (fragment is CharFragment) {
                Success(
                    node = fragment.char,
                    interval = Interval(cursor, cursor + 1),
                    nonTerminal = result,
                )
            } else {
                Failure(
                    message = "Expected char literal",
                    interval = Interval(cursor, cursor),
                    nonTerminal = result,
                )
            },
        )
    }
    
    override fun diagnose(scanner: SourceScanner, cursor: Cursor): Diagnosis? {
        val fragment = scanner[cursor]
        if (fragment is CharFragment) return null
        val end = if (fragment == null) cursor else cursor + 1
        return Diagnosis(
            nonTerminal = result,
            interval = Interval(cursor, end),
            message = "Expected char literal",
            confidence = 1,
        )
    }
}

class StringLiteralProduction(
    override val result: NonTerminal<String>,
) : Production<String> {
    override fun update(scanner: SourceScanner, cursor: Cursor) {
        val fragment = scanner[cursor]
        scanner.memoize(
            if (fragment is StringFragment) {
                Success(
                    node = fragment.string,
                    interval = Interval(cursor, cursor + 1),
                    nonTerminal = result,
                )
            } else {
                Failure(
                    message = "Expected string literal",
                    interval = Interval(cursor, cursor),
                    nonTerminal = result,
                )
            },
        )
    }
    
    override fun diagnose(scanner: SourceScanner, cursor: Cursor): Diagnosis? {
        val fragment = scanner[cursor]
        if (fragment is StringFragment) return null
        val end = if (fragment == null) cursor else cursor + 1
        return Diagnosis(
            nonTerminal = result,
            interval = Interval(cursor, end),
            message = "Expected string literal",
            confidence = 1,
        )
    }
}

class FormattedStringLiteralProduction(
    override val result: NonTerminal<FormattedStringTemplate>,
) : Production<FormattedStringTemplate> {
    override fun update(scanner: SourceScanner, cursor: Cursor) {
        val fragment = scanner[cursor]
        scanner.memoize(
            if (fragment is FormattedStringFragment) {
                Success(
                    node = FormattedStringTemplate(fragment.isRaw, fragment.parts),
                    interval = Interval(cursor, cursor + 1),
                    nonTerminal = result,
                )
            } else {
                Failure(
                    message = "Expected formatted string literal",
                    interval = Interval(cursor, cursor),
                    nonTerminal = result,
                )
            },
        )
    }
    
    override fun diagnose(scanner: SourceScanner, cursor: Cursor): Diagnosis? {
        val fragment = scanner[cursor]
        if (fragment is FormattedStringFragment) return null
        val end = if (fragment == null) cursor else cursor + 1
        return Diagnosis(
            nonTerminal = result,
            interval = Interval(cursor, end),
            message = "Expected formatted string literal",
            confidence = 1,
        )
    }
}

class SerialProduction<N>(
    override val result: NonTerminal<N>,
    val components: List<NonTerminal<*>>,
    val factory: (List<*>) -> N,
) : Production<N> {
    override fun update(scanner: SourceScanner, cursor: Cursor) {
        var current = cursor
        val results = mutableListOf<Any?>()
        components.forEach { comp ->
            val result = scanner.parse(current, comp) ?: return
            results += result.node
            current = result.interval.end
        }
        val node = try {
            factory(results)
        } catch (e: ParseException) {
            scanner.memoize(
                Failure(
                    message = e.message,
                    interval = Interval(cursor, current),
                    nonTerminal = result,
                ),
            )
            return
        }
        scanner.memoize(
            Success(
                node = node,
                interval = Interval(cursor, current),
                nonTerminal = result,
            ),
        )
    }
    
    override fun diagnose(scanner: SourceScanner, cursor: Cursor): Diagnosis? {
        var current = cursor
        val details = mutableListOf<String>()
        components.forEachIndexed { index, comp ->
            val begin = current
            when (val result = scanner.memoized(current, comp)) {
                is Success<*> -> {
                    current = result.interval.end
                    details += "matched #${index + 1} ${comp.displayName()} @${begin.fragIndex}..${current.fragIndex}"
                }
                is Failure<*> -> {
                    return Diagnosis(
                        nonTerminal = this.result,
                        interval = Interval(cursor, maxOf(current, result.interval.end)),
                        message = "failed at component #${index + 1} ${comp.displayName()}: ${result.message}",
                        matchedParts = index,
                        confidence = index + 1,
                        details = details + "failed #${index + 1} ${comp.displayName()} @${begin.fragIndex}..${result.interval.end.fragIndex}",
                    )
                }
                null -> {
                    return Diagnosis(
                        nonTerminal = this.result,
                        interval = Interval(cursor, current),
                        message = "stalled at component #${index + 1} ${comp.displayName()}",
                        matchedParts = index,
                        confidence = index,
                        details = details + "stalled #${index + 1} ${comp.displayName()} @${begin.fragIndex}",
                    )
                }
            }
        }
        return when (val result = scanner.memoized(cursor, this.result)) {
            is Failure<*> -> Diagnosis(
                nonTerminal = this.result,
                interval = result.interval,
                message = "all components matched but factory/result failed: ${result.message}",
                matchedParts = components.size,
                confidence = components.size,
                details = details,
            )
            else -> null
        }
    }
}

class ListProduction<N>(
    override val result: NonTerminal<List<N>>,
    val begin: NonTerminal<*>?,
    val element: NonTerminal<N>,
    val separator: NonTerminal<*>?,
    val end: NonTerminal<*>?,
) : Production<List<N>> {
    override fun update(scanner: SourceScanner, cursor: Cursor) {
        var current = cursor
        
        begin?.let {
            scanner.parse(cursor, begin) ?: return
        }?.let {
            current = it.interval.end
        }
        
        val elements = mutableListOf<N>()
        while (true) {
            val element = scanner.parse(current, element) ?: break
            elements += element.node
            current = element.interval.end
            separator?.let {
                scanner.parse(current, separator) ?: break
            }?.let {
                current = it.interval.end
            }
        }
        
        end?.let {
            scanner.parse(current, end) ?: return
        }?.let {
            current = it.interval.end
        }
        
        if (current > cursor) {
            scanner.memoize(
                Success(
                    node = elements,
                    interval = Interval(cursor, current),
                    nonTerminal = result,
                ),
            )
        }
    }
    
    override fun diagnose(scanner: SourceScanner, cursor: Cursor): Diagnosis? {
        var current = cursor
        begin?.let { begin ->
            when (val result = scanner.memoized(cursor, begin)) {
                is Success<*> -> current = result.interval.end
                is Failure<*> -> {
                    return Diagnosis(
                        nonTerminal = this.result,
                        interval = result.interval,
                        message = "Expected ${begin.displayName()} to start ${this.result.displayName()}",
                        confidence = 1,
                    )
                }
                null -> {
                    return Diagnosis(
                        nonTerminal = this.result,
                        interval = Interval(cursor, cursor),
                        message = "Could not determine whether ${begin.displayName()} starts ${this.result.displayName()}",
                    )
                }
            }
        }
        var count = 0
        while (true) {
            when (val elementResult = scanner.memoized(current, element)) {
                is Success<*> -> {
                    count++
                    current = elementResult.interval.end
                }
                is Failure<*> -> break
                null -> break
            }
            separator?.let { separator ->
                when (val separatorResult = scanner.memoized(current, separator)) {
                    is Success<*> -> current = separatorResult.interval.end
                    is Failure<*> -> return diagnoseListEnd(scanner, cursor, current, count)
                    null -> return diagnoseListEnd(scanner, cursor, current, count)
                }
            } ?: break
        }
        return diagnoseListEnd(scanner, cursor, current, count)
    }
    
    private fun diagnoseListEnd(scanner: SourceScanner, cursor: Cursor, current: Cursor, count: Int): Diagnosis? {
        end ?: return null
        return when (val endResult = scanner.memoized(current, end)) {
            is Success<*> -> null
            is Failure<*> -> Diagnosis(
                nonTerminal = result,
                interval = Interval(cursor, maxOf(current, endResult.interval.end)),
                message = "Looks like ${result.displayName()} with $count item(s) but missing ${end.displayName()}",
                matchedParts = count,
                confidence = count,
            )
            null -> Diagnosis(
                nonTerminal = result,
                interval = Interval(cursor, current),
                message = "Looks like ${result.displayName()} with $count item(s) but ${end.displayName()} was not resolved",
                matchedParts = count,
                confidence = count,
            )
        }
    }
}

fun NonTerminal<*>.displayName(): String = when (this) {
    is NamedNonTerminal<*> -> name
    is ClassNonTerminal<*> -> clazz.simpleName ?: clazz.toString()
    is PairNonTerminal<*, *> -> "Pair<${first.displayName()}, ${second.displayName()}>"
    is ListNonTerminal<*> -> "List<${type.displayName()}>"
    is SetNonTerminal<*> -> "Set<${type.displayName()}>"
    is SeqSetNonTerminal<*> -> "SeqSet<${type.displayName()}>"
    is MapNonTerminal<*, *> -> "Map<${key.displayName()}, ${value.displayName()}>"
    is SeqMapNonTerminal<*, *> -> "SeqMap<${key.displayName()}, ${value.displayName()}>"
}

fun Production<*>.displayName(): String = when (this) {
    is FixedProduction -> "${result.displayName()} ::= '$string'"
    is RegexProduction -> "${result.displayName()} ::= /${regex.pattern}/"
    is CharLiteralProduction -> "${result.displayName()} ::= <char>"
    is StringLiteralProduction -> "${result.displayName()} ::= <string>"
    is FormattedStringLiteralProduction -> "${result.displayName()} ::= <formatted-string>"
    is SerialProduction<*> -> "${result.displayName()} ::= ${components.joinToString(" ") { it.displayName() }}"
    is ListProduction<*> -> buildString {
        append(result.displayName())
        append(" ::= list(")
        append(begin?.displayName() ?: "null")
        append(", ")
        append(element.displayName())
        append(", ")
        append(separator?.displayName() ?: "null")
        append(", ")
        append(end?.displayName() ?: "null")
        append(")")
    }
}

