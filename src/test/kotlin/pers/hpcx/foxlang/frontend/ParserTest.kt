package pers.hpcx.foxlang.frontend

import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class ParserTest {
    
    @Test
    fun testSingleComponentSequenceRule() {
        val start = node<Atom>()
        val grammar = buildGrammar {
            rules(token("a")) { fixed("a") { it.text } }
            rules(start) {
                symbols(token("a")) { Atom(it) }
            }
        }
        
        val result = assertIs<Atom>(Parser(grammar, start).parse("a"))
        assertEquals(Atom("a"), result)
    }
    
    @Test
    fun testLeftRecursiveSequenceRule() {
        val value = node<Atom>().name("Value")
        val expression = node<Expr>().name("Expression")
        val grammar = buildGrammar {
            rules(token("a")) { fixed("a") { it.text } }
            rules(token("+")) { fixed("+") { it.text } }
            rules(value) {
                symbols(token("a")) { Atom(it) }
            }
            rules(expression) {
                symbols(value) { Expr.AtomExpr(it) }
                symbols(expression, token("+"), value) { left, _, right ->
                    Expr.Add(left, right)
                }
            }
        }
        
        val result = assertIs<Expr>(Parser(grammar, expression).parse("a+a+a"))
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
        val start = node<Choice>()
        val grammar = buildGrammar {
            rules(token("a")) { fixed("a") { it.text } }
            rules(start) {
                symbols(token("a")) { Choice.Left(it) }
                symbols(token("a")) { Choice.Right(it) }
            }
        }
        
        val exception = assertThrows<IllegalStateException> { Parser(grammar, start).parse("a") }
        assertEquals("Ambiguous grammar", exception.message)
    }
    
    @Test
    fun testRegexLeafMatchesAcrossAdjacentPlainFragments() {
        val start = node<WordValue>()
        val grammar = buildGrammar {
            rules(start) {
                regex(Regex("[a-z]+\\.[a-z]+")) { WordValue(it.text) }
            }
        }
        
        val result = assertIs<WordValue>(Parser(grammar, start).parse("foo.bar"))
        assertEquals(WordValue("foo.bar"), result)
    }
    
    @Test
    fun testFixedLeafMatchesAcrossAdjacentPlainFragments() {
        val start = node<WordValue>()
        val grammar = buildGrammar {
            rules(start) {
                fixed("a+b") { WordValue(it.text) }
            }
        }
        
        val result = assertIs<WordValue>(Parser(grammar, start).parse("a+b"))
        assertEquals(WordValue("a+b"), result)
    }
    
    @Test
    fun testRegexLeafPrefersLongestMatch() {
        val word = node<WordValue>().name("Word")
        val start = node<Sentence>()
        val grammar = buildGrammar {
            rules(word) {
                regex(Regex("[a-z]+")) { WordValue(it.text) }
            }
            rules(start) {
                symbols(word) { Sentence.Single(it) }
            }
        }
        
        val result = assertIs<Sentence>(Parser(grammar, start).parse("abc"))
        assertEquals(Sentence.Single(WordValue("abc")), result)
    }
    
    @Test
    fun testCharLiteralLeafRule() {
        val start = node<CharBox>()
        val grammar = buildGrammar {
            rules(start) {
                charLiteral { CharBox(it.char) }
            }
        }
        
        val result = assertIs<CharBox>(Parser(grammar, start).parse("'x'"))
        assertEquals(CharBox('x'), result)
    }
    
    @Test
    fun testStringLiteralLeafRule() {
        val start = node<StringBox>()
        val grammar = buildGrammar {
            rules(start) {
                stringLiteral { StringBox(it.string) }
            }
        }
        
        val result = assertIs<StringBox>(Parser(grammar, start).parse("\"fox\""))
        assertEquals(StringBox("fox"), result)
    }
    
    @Test
    fun testFormattedStringLeafRules() {
        val start = node<FormattedParts>()
        val isRaw = node<Boolean>().name("FormattedStart")
        val text = node<String>().name("FormattedText")
        val expressionOpen = node<Unit>().name("FormattedExpressionOpen")
        val expressionClose = node<Unit>().name("FormattedExpressionClose")
        val stringEnd = node<Unit>().name("FormattedStringEnd")
        val identifier = node<String>().name("Identifier")
        val grammar = buildGrammar {
            rules(isRaw) { formattedStringStart { it.isRaw } }
            rules(text) { formattedStringText { it.text } }
            rules(expressionOpen) { formattedExpressionStart { } }
            rules(expressionClose) { formattedExpressionEnd { } }
            rules(stringEnd) { formattedStringEnd { } }
            rules(identifier) { regex(Regex("[a-z]+")) { it.text } }
            rules(start) {
                symbols(isRaw, text, expressionOpen, identifier, expressionClose, stringEnd) { raw, literal, _, name, _, _ ->
                    FormattedParts(raw, literal, name)
                }
            }
        }
        
        val result = assertIs<FormattedParts>(Parser(grammar, start).parse("""f"hello {name}""""))
        assertEquals(FormattedParts(false, "hello ", "name"), result)
    }
    
    @Test
    fun testParseFailsWhenInputHasUnconsumedSuffix() {
        val start = node<Atom>()
        val grammar = buildGrammar {
            rules(token("a")) { fixed("a") { it.text } }
            rules(start) {
                symbols(token("a")) { Atom(it) }
            }
        }
        
        assertNull(Parser(grammar, start).parse("a+a"))
    }
    
    @Test
    fun testFactoryParseExceptionSuppressesCandidate() {
        val start = node<IntBox>()
        val grammar = buildGrammar {
            rules(start) {
                regex(Regex("[0-9]+")) {
                    val value = it.text.toInt()
                    if (value > 9) throw RuleFactoryException("Too large")
                    IntBox(value)
                }
            }
        }
        val parser = Parser(grammar, start)
        
        val result = assertIs<IntBox>(parser.parse("7"))
        assertEquals(IntBox(7), result)
        
        val analysis = parser.analyze("12")
        assertNull(analysis.value)
        val failure = analysis.exactChart.ruleFactoryFailures().single()
        assertEquals(start, failure.symbol)
        assertEquals(SourceSpan(SourcePosition(0), SourcePosition(1)), failure.span)
        assertEquals("Too large", failure.message)
    }
    
    @Test
    fun testFactoryParseExceptionDoesNotReportWhenAnotherCandidateBuilds() {
        val start = node<IntBox>()
        val grammar = buildGrammar {
            rules(start) {
                regex(Regex("[0-9]+")) {
                    throw RuleFactoryException("Rejected candidate")
                }
                regex(Regex("[0-9]+")) {
                    IntBox(it.text.toInt())
                }
            }
        }
        
        val analysis = Parser(grammar, start).analyze("7")
        val result = DiagnosticResult(
            root = Expectation(start, analysis.source.span),
            chart = analysis.exactChart,
            exactBuildSucceeded = analysis.value != null,
        )
        
        assertEquals(IntBox(7), analysis.value)
        assertEquals("Rejected candidate", analysis.exactChart.ruleFactoryFailures().single().message)
        assertTrue(result.report().items.isEmpty(), result.report().items.joinToString())
    }
    
    @Test
    fun testAmbiguityCanComeFromDifferentParseShapes() {
        val start = node<WrappedShape>()
        val group = node<Int>().name("Group")
        val atom = node<Int>().name("Atom")
        val grammar = buildGrammar {
            rules(token("(")) { fixed("(") { it.text } }
            rules(token(")")) { fixed(")") { it.text } }
            rules(token("a")) { fixed("a") { it.text } }
            rules(atom) {
                symbols(token("a")) { 1 }
            }
            rules(group) {
                symbols(atom) { it }
                symbols(token("("), group, token(")")) { _, value, _ -> value }
            }
            rules(start) {
                symbols(group) { WrappedShape.FromGroup(it) }
                symbols(token("("), start, token(")")) { _, value, _ -> WrappedShape.FromParen(value) }
            }
        }
        
        val exception = assertThrows<IllegalStateException> { Parser(grammar, start).parse("(a)") }
        assertEquals("Ambiguous grammar", exception.message)
    }
}

private data class Atom(val text: String)
private data class WordValue(val text: String)
private data class CharBox(val value: Char)
private data class StringBox(val value: String)
private data class FormattedParts(val isRaw: Boolean, val text: String, val expression: String)
private data class IntBox(val value: Int)

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

private sealed interface WrappedShape {
    data class FromGroup(val value: Int) : WrappedShape
    data class FromParen(val inner: WrappedShape) : WrappedShape
}
