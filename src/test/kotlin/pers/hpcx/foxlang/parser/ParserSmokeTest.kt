package pers.hpcx.foxlang.parser

import kotlin.test.*

class ParserSmokeTest {
    
    @Test
    fun testSingleComponentSequenceProduction() {
        val start = node<Atom>()
        val grammar = buildGrammar {
            target(token("a")) { fixed("a") { it.text } }
            target(start) {
                sequence(token("a")) { Atom(it) }
            }
        }
        
        val reduction = assertIs<ExactReduction<Atom>>(Parser(grammar, start).parse("a"))
        assertEquals(Atom("a"), reduction.node)
    }
    
    @Test
    fun testLeftRecursiveSequenceProduction() {
        val value = node<Atom>().name("Value")
        val expression = node<Expr>().name("Expression")
        val grammar = buildGrammar {
            target(token("a")) { fixed("a") { it.text } }
            target(token("+")) { fixed("+") { it.text } }
            target(value) {
                sequence(token("a")) { Atom(it) }
            }
            target(expression) {
                sequence(value) { Expr.AtomExpr(it) }
                sequence(expression, token("+"), value) { left, _, right ->
                    Expr.Add(left, right)
                }
            }
        }
        
        val reduction = assertIs<ExactReduction<Expr>>(Parser(grammar, expression).parse("a+a+a"))
        assertEquals(
            Expr.Add(
                Expr.Add(Expr.AtomExpr(Atom("a")), Atom("a")),
                Atom("a"),
            ),
            reduction.node,
        )
    }
    
    @Test
    fun testListLikeWithDelimitersAllowsEmptyAndTrailingSeparator() {
        val start = node<Items>()
        val grammar = buildGrammar {
            target(token("(")) { fixed("(") { it.text } }
            target(token(")")) { fixed(")") { it.text } }
            target(token(",")) { fixed(",") { it.text } }
            target(token("a")) { fixed("a") { it.text } }
            target(start) {
                listLike(token("("), token("a"), token(","), token(")")) { Items(it) }
            }
        }
        
        val empty = assertIs<ExactReduction<Items>>(Parser(grammar, start).parse("()"))
        assertEquals(Items(emptyList()), empty.node)
        
        val trailingSeparator = assertIs<ExactReduction<Items>>(Parser(grammar, start).parse("(a,a,)"))
        assertEquals(Items(listOf("a", "a")), trailingSeparator.node)
    }
    
    @Test
    fun testListLikeWithoutDelimitersParsesSeparatedSequence() {
        val start = node<Items>()
        val grammar = buildGrammar {
            target(token(",")) { fixed(",") { it.text } }
            target(token("a")) { fixed("a") { it.text } }
            target(start) {
                listLike(null, token("a"), token(","), null) { Items(it) }
            }
        }
        
        val reduction = assertIs<ExactReduction<Items>>(Parser(grammar, start).parse("a,a,a"))
        assertEquals(Items(listOf("a", "a", "a")), reduction.node)
    }
    
    @Test
    fun testAmbiguousParseFails() {
        val start = node<Choice>()
        val grammar = buildGrammar {
            target(token("a")) { fixed("a") { it.text } }
            target(start) {
                sequence(token("a")) { Choice.Left(it) }
                sequence(token("a")) { Choice.Right(it) }
            }
        }
        
        val error = assertFailsWith<ParseException> {
            Parser(grammar, start).parse("a")
        }
        assertEquals("Ambiguous parse for ${start}: 2 complete parses", error.message)
    }
    
    @Test
    fun testRegexLeafMatchesAcrossAdjacentPlainFragments() {
        val start = node<WordValue>()
        val grammar = buildGrammar {
            target(start) {
                regex(Regex("[a-z]+\\.[a-z]+")) { WordValue(it.text) }
            }
        }
        
        val reduction = assertIs<ExactReduction<WordValue>>(Parser(grammar, start).parse("foo.bar"))
        assertEquals(WordValue("foo.bar"), reduction.node)
    }
    
    @Test
    fun testFixedLeafMatchesAcrossAdjacentPlainFragments() {
        val start = node<WordValue>()
        val grammar = buildGrammar {
            target(start) {
                fixed("a+b") { WordValue(it.text) }
            }
        }
        
        val reduction = assertIs<ExactReduction<WordValue>>(Parser(grammar, start).parse("a+b"))
        assertEquals(WordValue("a+b"), reduction.node)
    }
    
    @Test
    fun testRegexLeafPrefersLongestMatch() {
        val word = node<WordValue>().name("Word")
        val start = node<Sentence>()
        val grammar = buildGrammar {
            target(word) {
                regex(Regex("[a-z]+")) { WordValue(it.text) }
            }
            target(start) {
                sequence(word) { Sentence.Single(it) }
            }
        }
        
        val reduction = assertIs<ExactReduction<Sentence>>(Parser(grammar, start).parse("abc"))
        assertEquals(Sentence.Single(WordValue("abc")), reduction.node)
    }
    
    @Test
    fun testCharLiteralLeafProduction() {
        val start = node<CharBox>()
        val grammar = buildGrammar {
            target(start) {
                charLiteral { CharBox(it.char) }
            }
        }
        
        val reduction = assertIs<ExactReduction<CharBox>>(Parser(grammar, start).parse("'x'"))
        assertEquals(CharBox('x'), reduction.node)
    }
    
    @Test
    fun testStringLiteralLeafProduction() {
        val start = node<StringBox>()
        val grammar = buildGrammar {
            target(start) {
                stringLiteral { StringBox(it.string) }
            }
        }
        
        val reduction = assertIs<ExactReduction<StringBox>>(Parser(grammar, start).parse("\"fox\""))
        assertEquals(StringBox("fox"), reduction.node)
    }
    
    @Test
    fun testParseFailsWhenInputHasUnconsumedSuffix() {
        val start = node<Atom>()
        val grammar = buildGrammar {
            target(token("a")) { fixed("a") { it.text } }
            target(start) {
                sequence(token("a")) { Atom(it) }
            }
        }
        
        val error = assertFailsWith<ParseException> {
            Parser(grammar, start).parse("a+a")
        }
        assertEquals("Failed to parse input as $start", error.message)
    }
    
    @Test
    fun testFactoryParseExceptionSuppressesCandidate() {
        val start = node<IntBox>()
        val grammar = buildGrammar {
            target(start) {
                regex(Regex("[0-9]+")) {
                    val value = it.text.toInt()
                    if (value > 9) throw ParseException("Too large")
                    IntBox(value)
                }
            }
        }
        
        val reduction = assertIs<ExactReduction<IntBox>>(Parser(grammar, start).parse("7"))
        assertEquals(IntBox(7), reduction.node)
        assertFailsWith<ParseException> {
            Parser(grammar, start).parse("12")
        }
    }
    
    @Test
    fun testListLikeWithoutSeparatorParsesJuxtaposedElements() {
        val charValue = node<Char>().name("CharValue")
        val start = node<CharItems>()
        val grammar = buildGrammar {
            target(charValue) {
                charLiteral { it.char }
            }
            target(start) {
                listLike(null, charValue, null, null) { CharItems(it) }
            }
        }
        
        val reduction = assertIs<ExactReduction<CharItems>>(Parser(grammar, start).parse("'a''b''c'"))
        assertEquals(CharItems(listOf('a', 'b', 'c')), reduction.node)
    }
    
    @Test
    fun testListLikeWithOnlyDelimitersParsesEmpty() {
        val start = node<Items>()
        val grammar = buildGrammar {
            target(token("<")) { fixed("<") { it.text } }
            target(token(">")) { fixed(">") { it.text } }
            target(token("a")) { fixed("a") { it.text } }
            target(start) {
                listLike(token("<"), token("a"), null, token(">")) { Items(it) }
            }
        }
        
        val reduction = assertIs<ExactReduction<Items>>(Parser(grammar, start).parse("<>"))
        assertEquals(Items(emptyList()), reduction.node)
    }
    
    @Test
    fun testNestedSequenceAndListLike() {
        val item = node<Item>().name("Item")
        val list = node<Items>().name("Items")
        val start = node<WrappedItems>()
        val grammar = buildGrammar {
            target(token("[")) { fixed("[") { it.text } }
            target(token("]")) { fixed("]") { it.text } }
            target(token(",")) { fixed(",") { it.text } }
            target(token("a")) { fixed("a") { it.text } }
            target(item) {
                sequence(token("a")) { Item(it) }
            }
            target(list) {
                listLike(token("["), item, token(","), token("]")) { reductions ->
                    Items(reductions.map { it.value })
                }
            }
            target(start) {
                sequence(list) { WrappedItems(it) }
            }
        }
        
        val reduction = assertIs<ExactReduction<WrappedItems>>(Parser(grammar, start).parse("[a,a]"))
        assertEquals(WrappedItems(Items(listOf("a", "a"))), reduction.node)
    }
    
    @Test
    fun testAmbiguityCanComeFromDifferentParseShapes() {
        val start = node<WrappedShape>()
        val group = node<Int>().name("Group")
        val atom = node<Int>().name("Atom")
        val grammar = buildGrammar {
            target(token("(")) { fixed("(") { it.text } }
            target(token(")")) { fixed(")") { it.text } }
            target(token("a")) { fixed("a") { it.text } }
            target(atom) {
                sequence(token("a")) { 1 }
            }
            target(group) {
                sequence(atom) { it }
                sequence(token("("), group, token(")")) { _, value, _ -> value }
            }
            target(start) {
                sequence(group) { WrappedShape.FromGroup(it) }
                sequence(token("("), start, token(")")) { _, value, _ -> WrappedShape.FromParen(value) }
            }
        }
        
        val error = assertFailsWith<ParseException> {
            Parser(grammar, start).parse("(a)")
        }
        assertTrue(error.message.startsWith("Ambiguous parse for"))
    }
}

private data class Atom(val text: String)
private data class WordValue(val text: String)
private data class CharBox(val value: Char)
private data class StringBox(val value: String)
private data class IntBox(val value: Int)
private data class Item(val value: String)
private data class WrappedItems(val items: Items)
private data class CharItems(val values: List<Char>)

private sealed interface Expr {
    data class AtomExpr(val atom: Atom) : Expr
    data class Add(val left: Expr, val right: Atom) : Expr
}

private data class Items(val values: List<String>)

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
