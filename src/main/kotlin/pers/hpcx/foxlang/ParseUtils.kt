package pers.hpcx.foxlang

fun epsilon() = EpsilonParser

fun fixed(string: String) = FixedParser(string)

fun regex(pattern: String) = RegexParser(Regex(pattern))

fun <N> optional(parser: Parser<N>) = OptionalParser(parser)

fun <N0, N> Parser<N0>.map(factory: (N0) -> ParseResult<N>) = MappedParser(this, factory)

fun <N> lazy(factory: () -> Parser<N>) = Parser { factory().parse(it) }

fun <N> parallel(vararg parsers: Parser<N>) = ParallelParser(*parsers)

fun <N, T : N> Parser<N>.tail(tailParser: Parser<(N) -> ParseResult<T>>) = TailParser(this, tailParser)

fun <N0, N1, N> composed(
    parser0: Parser<N0>,
    parser1: Parser<N1>,
    factory: (N0, N1) -> ParseResult<N>,
) = ComposedParser2(parser0, parser1, factory)

fun <N0, N1, N2, N> composed(
    parser0: Parser<N0>,
    parser1: Parser<N1>,
    parser2: Parser<N2>,
    factory: (N0, N1, N2) -> ParseResult<N>,
) = ComposedParser3(parser0, parser1, parser2, factory)

fun <N0, N1, N2, N3, N> composed(
    parser0: Parser<N0>,
    parser1: Parser<N1>,
    parser2: Parser<N2>,
    parser3: Parser<N3>,
    factory: (N0, N1, N2, N3) -> ParseResult<N>,
) = ComposedParser4(parser0, parser1, parser2, parser3, factory)

fun <N0, N1, N2, N3, N4, N> composed(
    parser0: Parser<N0>,
    parser1: Parser<N1>,
    parser2: Parser<N2>,
    parser3: Parser<N3>,
    parser4: Parser<N4>,
    factory: (N0, N1, N2, N3, N4) -> ParseResult<N>,
) = ComposedParser5(parser0, parser1, parser2, parser3, parser4, factory)

fun <N0, N1, N2, N3, N4, N5, N> composed(
    parser0: Parser<N0>,
    parser1: Parser<N1>,
    parser2: Parser<N2>,
    parser3: Parser<N3>,
    parser4: Parser<N4>,
    parser5: Parser<N5>,
    factory: (N0, N1, N2, N3, N4, N5) -> ParseResult<N>,
) = ComposedParser6(parser0, parser1, parser2, parser3, parser4, parser5, factory)

fun <N0, N1, N2, N3, N4, N5, N6, N> composed(
    parser0: Parser<N0>,
    parser1: Parser<N1>,
    parser2: Parser<N2>,
    parser3: Parser<N3>,
    parser4: Parser<N4>,
    parser5: Parser<N5>,
    parser6: Parser<N6>,
    factory: (N0, N1, N2, N3, N4, N5, N6) -> ParseResult<N>,
) = ComposedParser7(parser0, parser1, parser2, parser3, parser4, parser5, parser6, factory)

fun <N0, N1, N2, N3, N4, N5, N6, N7, N> composed(
    parser0: Parser<N0>,
    parser1: Parser<N1>,
    parser2: Parser<N2>,
    parser3: Parser<N3>,
    parser4: Parser<N4>,
    parser5: Parser<N5>,
    parser6: Parser<N6>,
    parser7: Parser<N7>,
    factory: (N0, N1, N2, N3, N4, N5, N6, N7) -> ParseResult<N>,
) = ComposedParser8(parser0, parser1, parser2, parser3, parser4, parser5, parser6, parser7, factory)

fun <N> listLike(
    beginParser: Parser<*>,
    elementParser: Parser<N>,
    separatorParser: Parser<*>,
    endParser: Parser<*>,
) = ListParser(beginParser, elementParser, separatorParser, endParser)

fun <N> Parser<N>.memoized() = MemoizedParser(this)

sealed interface ParseResult<out N>
class Success<out N>(val node: N) : ParseResult<N>
class Failure(val message: () -> String) : ParseResult<Nothing>

inline fun <N0, N1> ParseResult<N0>.ifSuccess(block: (N0) -> ParseResult<N1>): ParseResult<N1> {
    return when (this) {
        is Success -> block(node)
        is Failure -> this
    }
}

inline fun <N> ParseResult<N>.ifFailure(block: (Failure) -> Nothing): Success<N> {
    return when (this) {
        is Success -> this
        is Failure -> block(this)
    }
}

fun interface Parser<out N> {
    fun parse(scanner: SourceScanner): ParseResult<N>
}

object EpsilonParser : Parser<Unit> {
    override fun parse(scanner: SourceScanner): ParseResult<Unit> {
        return Success(Unit)
    }
}

object EofParser : Parser<Unit> {
    override fun parse(scanner: SourceScanner): ParseResult<Unit> {
        return if (scanner.isEnded()) Success(Unit) else Failure { "Expected end of file" }
    }
}

class FixedParser(val string: String) : Parser<Unit> {
    override fun parse(scanner: SourceScanner): ParseResult<Unit> {
        if (scanner.tryConsume(string)) {
            return Success(Unit)
        }
        return Failure { "Expected '$string'" }
    }
}

class RegexParser(val regex: Regex) : Parser<String> {
    override fun parse(scanner: SourceScanner): ParseResult<String> {
        val match = scanner.tryConsumeRegex(regex)
        if (match != null) {
            return Success(match)
        }
        return Failure { "Expected match for pattern '$regex'" }
    }
}

class OptionalParser<out N>(val parser: Parser<N>) : Parser<N?> {
    
    override fun parse(scanner: SourceScanner): ParseResult<N?> {
        return when (val result = parser.parse(scanner)) {
            is Success -> result
            is Failure -> Success(null)
        }
    }
}

class ParallelParser<out N>(vararg val parsers: Parser<N>) : Parser<N> {
    
    override fun parse(scanner: SourceScanner): ParseResult<N> {
        val failureMessages = mutableListOf<() -> String>()
        parsers.forEach { parser ->
            when (val result = parser.parse(scanner)) {
                is Success -> return result
                is Failure -> failureMessages += result.message
            }
        }
        return Failure { failureMessages.joinToString(System.lineSeparator()) { it() } }
    }
}

class MappedParser<N0, out N>(
    val parser: Parser<N0>,
    val factory: (N0) -> ParseResult<N>,
) : Parser<N> {
    
    override fun parse(scanner: SourceScanner): ParseResult<N> {
        return scanner.attempt {
            parser.parse(scanner).ifSuccess { factory(it) }
        }
    }
}

class ComposedParser2<N0, N1, out N>(
    val parser0: Parser<N0>,
    val parser1: Parser<N1>,
    val factory: (N0, N1) -> ParseResult<N>,
) : Parser<N> {
    
    override fun parse(scanner: SourceScanner): ParseResult<N> {
        scanner.saveCursor()
        val n0 = parser0.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n1 = parser1.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val result = factory(n0, n1).ifFailure { scanner.restoreCursor(); return it }
        scanner.discardCursor()
        return result
    }
}

class ComposedParser3<N0, N1, N2, out N>(
    val parser0: Parser<N0>,
    val parser1: Parser<N1>,
    val parser2: Parser<N2>,
    val factory: (N0, N1, N2) -> ParseResult<N>,
) : Parser<N> {
    
    override fun parse(scanner: SourceScanner): ParseResult<N> {
        scanner.saveCursor()
        val n0 = parser0.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n1 = parser1.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n2 = parser2.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val result = factory(n0, n1, n2).ifFailure { scanner.restoreCursor(); return it }
        scanner.discardCursor()
        return result
    }
}

class ComposedParser4<N0, N1, N2, N3, out N>(
    val parser0: Parser<N0>,
    val parser1: Parser<N1>,
    val parser2: Parser<N2>,
    val parser3: Parser<N3>,
    val factory: (N0, N1, N2, N3) -> ParseResult<N>,
) : Parser<N> {
    
    override fun parse(scanner: SourceScanner): ParseResult<N> {
        scanner.saveCursor()
        val n0 = parser0.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n1 = parser1.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n2 = parser2.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n3 = parser3.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val result = factory(n0, n1, n2, n3).ifFailure { scanner.restoreCursor(); return it }
        scanner.discardCursor()
        return result
    }
}

class ComposedParser5<N0, N1, N2, N3, N4, out N>(
    val parser0: Parser<N0>,
    val parser1: Parser<N1>,
    val parser2: Parser<N2>,
    val parser3: Parser<N3>,
    val parser4: Parser<N4>,
    val factory: (N0, N1, N2, N3, N4) -> ParseResult<N>,
) : Parser<N> {
    
    override fun parse(scanner: SourceScanner): ParseResult<N> {
        scanner.saveCursor()
        val n0 = parser0.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n1 = parser1.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n2 = parser2.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n3 = parser3.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n4 = parser4.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val result = factory(n0, n1, n2, n3, n4).ifFailure { scanner.restoreCursor(); return it }
        scanner.discardCursor()
        return result
    }
}

class ComposedParser6<N0, N1, N2, N3, N4, N5, out N>(
    val parser0: Parser<N0>,
    val parser1: Parser<N1>,
    val parser2: Parser<N2>,
    val parser3: Parser<N3>,
    val parser4: Parser<N4>,
    val parser5: Parser<N5>,
    val factory: (N0, N1, N2, N3, N4, N5) -> ParseResult<N>,
) : Parser<N> {
    
    override fun parse(scanner: SourceScanner): ParseResult<N> {
        scanner.saveCursor()
        val n0 = parser0.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n1 = parser1.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n2 = parser2.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n3 = parser3.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n4 = parser4.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n5 = parser5.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val result = factory(n0, n1, n2, n3, n4, n5).ifFailure { scanner.restoreCursor(); return it }
        scanner.discardCursor()
        return result
    }
}

class ComposedParser7<N0, N1, N2, N3, N4, N5, N6, out N>(
    val parser0: Parser<N0>,
    val parser1: Parser<N1>,
    val parser2: Parser<N2>,
    val parser3: Parser<N3>,
    val parser4: Parser<N4>,
    val parser5: Parser<N5>,
    val parser6: Parser<N6>,
    val factory: (N0, N1, N2, N3, N4, N5, N6) -> ParseResult<N>,
) : Parser<N> {
    
    override fun parse(scanner: SourceScanner): ParseResult<N> {
        scanner.saveCursor()
        val n0 = parser0.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n1 = parser1.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n2 = parser2.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n3 = parser3.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n4 = parser4.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n5 = parser5.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n6 = parser6.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val result = factory(n0, n1, n2, n3, n4, n5, n6).ifFailure { scanner.restoreCursor(); return it }
        scanner.discardCursor()
        return result
    }
}

class ComposedParser8<N0, N1, N2, N3, N4, N5, N6, N7, out N>(
    val parser0: Parser<N0>,
    val parser1: Parser<N1>,
    val parser2: Parser<N2>,
    val parser3: Parser<N3>,
    val parser4: Parser<N4>,
    val parser5: Parser<N5>,
    val parser6: Parser<N6>,
    val parser7: Parser<N7>,
    val factory: (N0, N1, N2, N3, N4, N5, N6, N7) -> ParseResult<N>,
) : Parser<N> {
    
    override fun parse(scanner: SourceScanner): ParseResult<N> {
        scanner.saveCursor()
        val n0 = parser0.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n1 = parser1.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n2 = parser2.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n3 = parser3.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n4 = parser4.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n5 = parser5.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n6 = parser6.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val n7 = parser7.parse(scanner).ifFailure { scanner.restoreCursor(); return it }.node
        val result = factory(n0, n1, n2, n3, n4, n5, n6, n7).ifFailure { scanner.restoreCursor(); return it }
        scanner.discardCursor()
        return result
    }
}

class ListParser<N>(
    val beginParser: Parser<*>,
    val elementParser: Parser<N>,
    val separatorParser: Parser<*>,
    val endParser: Parser<*>,
) : Parser<List<N>> {
    
    override fun parse(scanner: SourceScanner): ParseResult<List<N>> {
        return scanner.attempt {
            beginParser.parse(scanner).ifSuccess {
                val elements = mutableListOf<N>()
                while (true) {
                    when (val result = elementParser.parse(scanner)) {
                        is Success -> elements += result.node
                        is Failure -> break
                    }
                    when (separatorParser.parse(scanner)) {
                        is Success -> {}
                        is Failure -> break
                    }
                }
                endParser.parse(scanner).ifSuccess {
                    Success(elements)
                }
            }
        }
    }
}

class TailParser<N, out T : N>(
    val headParser: Parser<N>,
    val tailParser: Parser<(N) -> ParseResult<T>>,
) : Parser<N> {
    override fun parse(scanner: SourceScanner): ParseResult<N> {
        return headParser.parse(scanner).ifSuccess { head ->
            var result = head
            while (true) {
                scanner.saveCursor()
                when (val tail = tailParser.parse(scanner)) {
                    is Success -> result = when (val concatResult = tail.node(result)) {
                        is Success -> {
                            scanner.discardCursor()
                            concatResult.node
                        }
                        is Failure -> {
                            scanner.restoreCursor()
                            break
                        }
                    }
                    is Failure -> {
                        scanner.restoreCursor()
                        break
                    }
                }
            }
            Success(result)
        }
    }
}

class MemoizedParser<out N>(val parser: Parser<N>) : Parser<N> {
    override fun parse(scanner: SourceScanner): ParseResult<N> {
        return scanner.memoize(parser)
    }
}
