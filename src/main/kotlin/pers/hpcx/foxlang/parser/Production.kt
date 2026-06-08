package pers.hpcx.foxlang.parser

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

sealed interface Production<N> {
    val result: NonTerminal<N>
    override fun toString(): String
    fun referencedNonTerminals(): List<NonTerminal<*>>
    fun update(context: ParseContext, cursor: Cursor)
    fun diagnose(context: ParseContext, cursor: Cursor): Diagnosis? = null
}

class FixedProduction(
    override val result: NonTerminal<String>,
    val string: String,
) : Production<String> {
    
    override fun toString() = "$result ::= '${string}'"
    
    override fun referencedNonTerminals(): List<NonTerminal<*>> = emptyList()
    
    override fun update(context: ParseContext, cursor: Cursor) {
        context.memoize(
            exactMatch(context, cursor)?.let { end ->
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
    
    override fun diagnose(context: ParseContext, cursor: Cursor): Diagnosis? {
        if (exactMatch(context, cursor) != null) return null
        val fragment = context[cursor]
        val end = if (fragment == null) cursor else cursor + 1
        return Diagnosis(
            nonTerminal = result,
            interval = Interval(cursor, end),
            message = "Expected '$string'",
            confidence = 1,
        )
    }
    
    private fun exactMatch(context: ParseContext, cursor: Cursor): Cursor? {
        var current = cursor
        var previous: PlainFragment? = null
        var candidate = ""
        while (candidate.length < string.length) {
            val next = context[current]
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
    
    override fun toString() = "$result ::= /${regex.pattern}/"
    
    override fun referencedNonTerminals(): List<NonTerminal<*>> = emptyList()
    
    override fun update(context: ParseContext, cursor: Cursor) {
        context.memoize(
            longestMatch(context, cursor)?.let { (match, end) ->
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
    
    override fun diagnose(context: ParseContext, cursor: Cursor): Diagnosis? {
        if (longestMatch(context, cursor) != null) return null
        val fragment = context[cursor]
        val end = if (fragment == null) cursor else cursor + 1
        return Diagnosis(
            nonTerminal = result,
            interval = Interval(cursor, end),
            message = "Expected $expectation matching /${regex.pattern}/",
            confidence = 1,
        )
    }
    
    private fun longestMatch(context: ParseContext, cursor: Cursor): Pair<String, Cursor>? {
        var current = cursor
        var previous: PlainFragment? = null
        var candidate = ""
        var best: Pair<String, Cursor>? = null
        while (true) {
            val next = context[current]
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
    
    override fun toString() = "$result ::= <char literal>"
    
    override fun referencedNonTerminals(): List<NonTerminal<*>> = emptyList()
    
    override fun update(context: ParseContext, cursor: Cursor) {
        val fragment = context[cursor]
        context.memoize(
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
    
    override fun diagnose(context: ParseContext, cursor: Cursor): Diagnosis? {
        val fragment = context[cursor]
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
    
    override fun toString() = "$result ::= <string literal>"
    
    override fun referencedNonTerminals(): List<NonTerminal<*>> = emptyList()
    
    override fun update(context: ParseContext, cursor: Cursor) {
        val fragment = context[cursor]
        context.memoize(
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
    
    override fun diagnose(context: ParseContext, cursor: Cursor): Diagnosis? {
        val fragment = context[cursor]
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
    
    override fun toString() = "$result ::= <formatted string literal>"
    
    override fun referencedNonTerminals(): List<NonTerminal<*>> = emptyList()
    
    override fun update(context: ParseContext, cursor: Cursor) {
        val fragment = context[cursor]
        context.memoize(
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
    
    override fun diagnose(context: ParseContext, cursor: Cursor): Diagnosis? {
        val fragment = context[cursor]
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
    
    override fun toString() = "$result ::= ${components.joinToString(" ")}"
    
    override fun referencedNonTerminals(): List<NonTerminal<*>> = components
    
    override fun update(context: ParseContext, cursor: Cursor) {
        var current = cursor
        val results = mutableListOf<Any?>()
        components.forEach { comp ->
            val result = context.parse(current, comp) ?: return
            results += result.node
            current = result.interval.end
        }
        val node = try {
            factory(results)
        } catch (e: ParseException) {
            context.memoize(
                Failure(
                    message = e.message,
                    interval = Interval(cursor, current),
                    nonTerminal = result,
                ),
            )
            return
        }
        context.memoize(
            Success(
                node = node,
                interval = Interval(cursor, current),
                nonTerminal = result,
            ),
        )
    }
    
    override fun diagnose(context: ParseContext, cursor: Cursor): Diagnosis? {
        var current = cursor
        val details = mutableListOf<String>()
        components.forEachIndexed { index, comp ->
            val begin = current
            when (val result = context.memoized(current, comp)) {
                is Success<*> -> {
                    current = result.interval.end
                    details += "matched #${index + 1} $comp @${begin.fragIndex}..${current.fragIndex}"
                }
                is Failure<*> -> {
                    return Diagnosis(
                        nonTerminal = this.result,
                        interval = Interval(cursor, maxOf(current, result.interval.end)),
                        message = "failed at component #${index + 1} ${comp}: ${result.message}",
                        matchedParts = index,
                        confidence = index + 1,
                        details = details + "failed #${index + 1} $comp @${begin.fragIndex}..${result.interval.end.fragIndex}",
                    )
                }
                null -> {
                    return Diagnosis(
                        nonTerminal = this.result,
                        interval = Interval(cursor, current),
                        message = "stalled at component #${index + 1} $comp",
                        matchedParts = index,
                        confidence = index,
                        details = details + "stalled #${index + 1} $comp @${begin.fragIndex}",
                    )
                }
            }
        }
        return when (val result = context.memoized(cursor, this.result)) {
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
    
    override fun toString() = buildString {
        append(result)
        append(" ::= list(")
        append("begin=")
        append(begin ?: "<none>")
        append(", element=")
        append(element)
        append(", separator=")
        append(separator ?: "<none>")
        append(", end=")
        append(end ?: "<none>")
        append(")")
    }
    
    override fun referencedNonTerminals(): List<NonTerminal<*>> = listOfNotNull(begin, element, separator, end)
    
    override fun update(context: ParseContext, cursor: Cursor) {
        var current = cursor
        
        begin?.let {
            context.parse(cursor, begin) ?: return
        }?.let {
            current = it.interval.end
        }
        
        val elements = mutableListOf<N>()
        while (true) {
            val element = context.parse(current, element) ?: break
            elements += element.node
            current = element.interval.end
            separator?.let {
                context.parse(current, separator) ?: break
            }?.let {
                current = it.interval.end
            }
        }
        
        end?.let {
            context.parse(current, end) ?: return
        }?.let {
            current = it.interval.end
        }
        
        if (current > cursor) {
            context.memoize(
                Success(
                    node = elements,
                    interval = Interval(cursor, current),
                    nonTerminal = result,
                ),
            )
        }
    }
    
    override fun diagnose(context: ParseContext, cursor: Cursor): Diagnosis? {
        var current = cursor
        begin?.let { begin ->
            when (val result = context.memoized(cursor, begin)) {
                is Success<*> -> current = result.interval.end
                is Failure<*> -> {
                    return Diagnosis(
                        nonTerminal = this.result,
                        interval = result.interval,
                        message = "Expected $begin to start ${this.result}",
                        confidence = 1,
                    )
                }
                null -> {
                    return Diagnosis(
                        nonTerminal = this.result,
                        interval = Interval(cursor, cursor),
                        message = "Could not determine whether $begin starts ${this.result}",
                    )
                }
            }
        }
        var count = 0
        while (true) {
            when (val elementResult = context.memoized(current, element)) {
                is Success<*> -> {
                    count++
                    current = elementResult.interval.end
                }
                is Failure<*> -> break
                null -> break
            }
            separator?.let { separator ->
                when (val separatorResult = context.memoized(current, separator)) {
                    is Success<*> -> current = separatorResult.interval.end
                    is Failure<*> -> return diagnoseListEnd(context, cursor, current, count)
                    null -> return diagnoseListEnd(context, cursor, current, count)
                }
            } ?: break
        }
        return diagnoseListEnd(context, cursor, current, count)
    }
    
    private fun diagnoseListEnd(context: ParseContext, cursor: Cursor, current: Cursor, count: Int): Diagnosis? {
        end ?: return null
        return when (val endResult = context.memoized(current, end)) {
            is Success<*> -> null
            is Failure<*> -> Diagnosis(
                nonTerminal = result,
                interval = Interval(cursor, maxOf(current, endResult.interval.end)),
                message = "Looks like $result with $count item(s) but missing $end",
                matchedParts = count,
                confidence = count,
            )
            null -> Diagnosis(
                nonTerminal = result,
                interval = Interval(cursor, current),
                message = "Looks like $result with $count item(s) but $end was not resolved",
                matchedParts = count,
                confidence = count,
            )
        }
    }
}
