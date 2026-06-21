package pers.hpcx.foxlang.parser

import dk.brics.automaton.RegExp
import dk.brics.automaton.RunAutomaton

data class Grammar(val productions: Map<NonTerminal<*>, List<Production<*>>>)

fun buildGrammar(block: GrammarBuilder.() -> Unit): Grammar {
    val builder = GrammarBuilder()
    builder.block()
    return builder.build()
}

class GrammarBuilder {
    
    private val productions = mutableMapOf<NonTerminal<*>, MutableList<Production<*>>>()
    
    fun build() = Grammar(productions)
    
    @Suppress("UNCHECKED_CAST")
    fun <N> target(result: NonTerminal<N>, block: ProductionListBuilder<N>.() -> Unit) {
        val list = productions.getOrPut(result) { mutableListOf() }
        ProductionListBuilder(list as MutableList<Production<N>>).block()
    }
    
    class ProductionListBuilder<N>(internal val list: MutableList<Production<N>>) {
        
        fun fixed(string: String, factory: (PlainFragment) -> N) {
            list += FixedProduction(string, factory)
        }
        
        fun regex(regex: Regex, factory: (PlainFragment) -> N) {
            list += RegexProduction(regex, factory)
        }
        
        fun lineSeparator(factory: (LineSeparatorFragment) -> N) {
            list += LineSeparatorProduction(factory)
        }
        
        fun charLiteral(factory: (CharFragment) -> N) {
            list += CharLiteralProduction(factory)
        }
        
        fun stringLiteral(factory: (StringFragment) -> N) {
            list += StringLiteralProduction(factory)
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0> sequence(
            comp0: NonTerminal<N0>,
            factory: (N0) -> N,
        ) {
            list += SequenceProduction(listOf(comp0)) { list ->
                factory(list[0] as N0)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1> sequence(
            comp0: NonTerminal<N0>,
            comp1: NonTerminal<N1>,
            factory: (N0, N1) -> N,
        ) {
            list += SequenceProduction(listOf(comp0, comp1)) { list ->
                factory(list[0] as N0, list[1] as N1)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1, N2> sequence(
            comp0: NonTerminal<N0>,
            comp1: NonTerminal<N1>,
            comp2: NonTerminal<N2>,
            factory: (N0, N1, N2) -> N,
        ) {
            list += SequenceProduction(listOf(comp0, comp1, comp2)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1, N2, N3> sequence(
            comp0: NonTerminal<N0>,
            comp1: NonTerminal<N1>,
            comp2: NonTerminal<N2>,
            comp3: NonTerminal<N3>,
            factory: (N0, N1, N2, N3) -> N,
        ) {
            list += SequenceProduction(listOf(comp0, comp1, comp2, comp3)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1, N2, N3, N4> sequence(
            comp0: NonTerminal<N0>,
            comp1: NonTerminal<N1>,
            comp2: NonTerminal<N2>,
            comp3: NonTerminal<N3>,
            comp4: NonTerminal<N4>,
            factory: (N0, N1, N2, N3, N4) -> N,
        ) {
            list += SequenceProduction(listOf(comp0, comp1, comp2, comp3, comp4)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1, N2, N3, N4, N5> sequence(
            comp0: NonTerminal<N0>,
            comp1: NonTerminal<N1>,
            comp2: NonTerminal<N2>,
            comp3: NonTerminal<N3>,
            comp4: NonTerminal<N4>,
            comp5: NonTerminal<N5>,
            factory: (N0, N1, N2, N3, N4, N5) -> N,
        ) {
            list += SequenceProduction(listOf(comp0, comp1, comp2, comp3, comp4, comp5)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1, N2, N3, N4, N5, N6> sequence(
            comp0: NonTerminal<N0>,
            comp1: NonTerminal<N1>,
            comp2: NonTerminal<N2>,
            comp3: NonTerminal<N3>,
            comp4: NonTerminal<N4>,
            comp5: NonTerminal<N5>,
            comp6: NonTerminal<N6>,
            factory: (N0, N1, N2, N3, N4, N5, N6) -> N,
        ) {
            list += SequenceProduction(listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5, list[6] as N6)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1, N2, N3, N4, N5, N6, N7> sequence(
            comp0: NonTerminal<N0>,
            comp1: NonTerminal<N1>,
            comp2: NonTerminal<N2>,
            comp3: NonTerminal<N3>,
            comp4: NonTerminal<N4>,
            comp5: NonTerminal<N5>,
            comp6: NonTerminal<N6>,
            comp7: NonTerminal<N7>,
            factory: (N0, N1, N2, N3, N4, N5, N6, N7) -> N,
        ) {
            list += SequenceProduction(listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6, comp7)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5, list[6] as N6, list[7] as N7)
            }
        }
        
        fun <E> listLike(
            begin: NonTerminal<*>?,
            element: NonTerminal<E>,
            separator: NonTerminal<*>?,
            end: NonTerminal<*>?,
            factory: (List<E>) -> N,
        ) {
            list += ListLikeProduction(begin, element, separator, end, factory)
        }
    }
}

sealed interface Production<N>
sealed interface LeafProduction<N> : Production<N>
sealed interface NonLeafProduction<N> : Production<N>

class FixedProduction<N>(val string: String, val factory: (PlainFragment) -> N) : LeafProduction<N>
class RegexProduction<N>(val regex: Regex, val factory: (PlainFragment) -> N) : LeafProduction<N> {
    val automaton by lazy { RunAutomaton(RegExp(regex.pattern).toAutomaton(true), true) }
}
class LineSeparatorProduction<N>(val factory: (LineSeparatorFragment) -> N) : LeafProduction<N>
class CharLiteralProduction<N>(val factory: (CharFragment) -> N) : LeafProduction<N>
class StringLiteralProduction<N>(val factory: (StringFragment) -> N) : LeafProduction<N>

class SequenceProduction<N>(
    val components: List<NonTerminal<*>>,
    val factory: (List<*>) -> N,
) : NonLeafProduction<N> {
    init {
        require(components.isNotEmpty())
    }
}

class ListLikeProduction<E, N>(
    val begin: NonTerminal<*>?,
    val element: NonTerminal<E>,
    val separator: NonTerminal<*>?,
    val end: NonTerminal<*>?,
    val factory: (List<E>) -> N,
) : NonLeafProduction<N>
