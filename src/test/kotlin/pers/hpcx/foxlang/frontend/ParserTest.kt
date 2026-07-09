package pers.hpcx.foxlang.frontend

import org.junit.jupiter.api.assertThrows
import pers.hpcx.foxlang.frontend.common.*
import pers.hpcx.foxlang.utils.Opt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParserTest {
    
    @Test
    fun testSingleComponentSequenceRule() {
        val start = AtomSymbol
        val grammar = buildGrammar {
            rules(KwA) { fixed("a") { it.text } }
            rules(start) {
                symbols(KwA) { Atom(it) }
            }
        }
        
        val result = Parser(grammar, start).parseText("a").value()
        assertEquals(Atom("a"), result)
    }
    
    @Test
    fun testLeftRecursiveSequenceRule() {
        val value = AtomSymbol
        val expression = ExprSymbol
        val grammar = buildGrammar {
            rules(KwA) { fixed("a") { it.text } }
            rules(KwPlus) { fixed("+") { it.text } }
            rules(value) {
                symbols(KwA) { Atom(it) }
            }
            rules(expression) {
                symbols(value) { Expr.AtomExpr(it) }
                symbols(expression, KwPlus, value) { left, _, right ->
                    Expr.Add(left, right)
                }
            }
        }
        
        val result = Parser(grammar, expression).parseText("a+a+a").value()
        assertEquals(
            Expr.Add(
                Expr.Add(Expr.AtomExpr(Atom("a")), Atom("a")),
                Atom("a"),
            ),
            result,
        )
    }
    
    @Test
    fun testAmbiguousParseFails() {
        val start = ChoiceSymbol
        val grammar = buildGrammar {
            rules(KwA) { fixed("a") { it.text } }
            rules(start) {
                symbols(KwA) { Choice.Left(it) }
                symbols(KwA) { Choice.Right(it) }
            }
        }
        
        val exception = assertThrows<IllegalStateException> { Parser(grammar, start).parseText("a") }
        assertEquals("Ambiguous grammar", exception.message)
    }
    
    @Test
    fun testRegexTerminalMatchesAcrossAdjacentTextFragments() {
        val start = WordValueSymbol
        val grammar = buildGrammar {
            rules(start) {
                regex(Regex("[a-z]+\\.[a-z]+")) { WordValue(it.text) }
            }
        }
        
        val result = Parser(grammar, start).parseText("foo.bar").value()
        assertEquals(WordValue("foo.bar"), result)
    }
    
    @Test
    fun testFixedTerminalMatchesAcrossAdjacentTextFragments() {
        val start = WordValueSymbol
        val grammar = buildGrammar {
            rules(start) {
                fixed("a+b") { WordValue(it.text) }
            }
        }
        
        val result = Parser(grammar, start).parseText("a+b").value()
        assertEquals(WordValue("a+b"), result)
    }
    
    @Test
    fun testRegexTerminalPrefersLongestMatch() {
        val word = WordValueSymbol
        val start = SentenceSymbol
        val grammar = buildGrammar {
            rules(word) {
                regex(Regex("[a-z]+")) { WordValue(it.text) }
            }
            rules(start) {
                symbols(word) { Sentence.Single(it) }
            }
        }
        
        val result = Parser(grammar, start).parseText("abc").value()
        assertEquals(Sentence.Single(WordValue("abc")), result)
    }
    
    @Test
    fun testParseFailsWhenInputHasUnconsumedSuffix() {
        val start = AtomSymbol
        val grammar = buildGrammar {
            rules(KwA) { fixed("a") { it.text } }
            rules(start) {
                symbols(KwA) { Atom(it) }
            }
        }
        
        assertTrue { Parser(grammar, start).parseText("a+a").isEmpty() }
    }
}

private fun testSource(text: String): Source<Text> {
    return Source(text, text.mapIndexed { index, char -> Text(index, char.toString()) })
}

private fun <N> Parser<N>.parseText(text: String): Opt<N> {
    return parse(testSource(text))
}

private fun <N> GrammarRuleSetBuilder<N>.fixed(
    string: String,
    factory: (Text) -> N,
) {
    terminal<Text>(
        TerminalMatcher { source, start ->
            var cursor = start
            val builder = StringBuilder()
            val first = source.getOrNull(start) ?: return@TerminalMatcher null
            
            while (builder.length < string.length) {
                val fragment = source.getOrNull(cursor) ?: return@TerminalMatcher null
                if (fragment.offset != first.offset + builder.length) return@TerminalMatcher null
                builder.append(fragment.text)
                cursor += 1
            }
            
            if (builder.toString() != string) return@TerminalMatcher null
            TerminalMatch(factory(Text(first.offset, builder.toString())), SourceSpan(start, cursor))
        },
    )
}

private fun <N> GrammarRuleSetBuilder<N>.regex(
    regex: Regex,
    factory: (Text) -> N,
) {
    terminal<Text>(
        TerminalMatcher { source, start ->
            var cursor = start
            val builder = StringBuilder()
            val first = source.getOrNull(start) ?: return@TerminalMatcher null
            var longest: SourcePosition? = null
            var longestText = ""
            
            while (true) {
                val fragment = source.getOrNull(cursor) ?: break
                if (fragment.offset != first.offset + builder.length) break
                builder.append(fragment.text)
                cursor += 1
                val text = builder.toString()
                if (regex.matches(text)) {
                    longest = cursor
                    longestText = text
                }
            }
            
            val end = longest ?: return@TerminalMatcher null
            TerminalMatch(factory(Text(first.offset, longestText)), SourceSpan(start, end))
        },
    )
}

private sealed interface TestSymbol<N> : GrammarSymbol<N>
private data object KwA : TestSymbol<String>
private data object KwPlus : TestSymbol<String>
private data object AtomSymbol : TestSymbol<Atom>
private data object WordValueSymbol : TestSymbol<WordValue>
private data object ExprSymbol : TestSymbol<Expr>
private data object SentenceSymbol : TestSymbol<Sentence>
private data object ChoiceSymbol : TestSymbol<Choice>

data class Text(val offset: Int, val text: String)

private data class Atom(val text: String)
private data class WordValue(val text: String)

private sealed interface Expr {
    data class AtomExpr(val atom: Atom) : Expr
    data class Add(val left: Expr, val right: Atom) : Expr
}

private sealed interface Sentence {
    data class Single(val word: WordValue) : Sentence
}

private sealed interface Choice {
    data class Left(val token: String) : Choice
    data class Right(val token: String) : Choice
}
