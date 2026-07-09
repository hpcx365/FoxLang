package pers.hpcx.foxlang.frontend.common

data class Grammar(
    val rules: Map<GrammarSymbol<*>, Set<GrammarRule<*>>>,
) {
    
    val dependencyGraph: Map<GrammarSymbol<*>, Set<GrammarSymbol<*>>> =
        mutableMapOf<GrammarSymbol<*>, MutableSet<GrammarSymbol<*>>>().apply {
            rules.forEach { (symbol, set) ->
                set.forEach { rule ->
                    when (rule) {
                        is GrammarRule.MatchTerminal<*, *> -> {}
                        is GrammarRule.MatchSymbols -> {
                            rule.components.forEach { component ->
                                getOrPut(component) { mutableSetOf() } += symbol
                            }
                        }
                    }
                }
            }
        }
}

interface GrammarSymbol<N>

fun interface TerminalMatcher<N, U> {
    fun match(source: Source<U>, start: SourcePosition): TerminalMatch<N>?
}

data class TerminalMatch<N>(
    val node: N,
    val span: SourceSpan,
)

sealed interface GrammarRule<N> {
    
    data class MatchTerminal<N, U>(
        val matcher: TerminalMatcher<N, U>,
    ) : GrammarRule<N>
    
    data class MatchSymbols<N>(
        val components: List<GrammarSymbol<*>>,
        val factory: (List<*>) -> N,
    ) : GrammarRule<N> {
        init {
            require(components.isNotEmpty())
        }
    }
}

fun buildGrammar(block: GrammarBuilder.() -> Unit): Grammar {
    val builder = GrammarBuilder()
    builder.block()
    return builder.build()
}

class GrammarBuilder {
    
    private val rules = mutableMapOf<GrammarSymbol<*>, MutableSet<GrammarRule<*>>>()
    
    fun build() = Grammar(rules.toMap())
    
    @Suppress("UNCHECKED_CAST")
    fun <N> rules(symbol: GrammarSymbol<N>, block: GrammarRuleSetBuilder<N>.() -> Unit) {
        val builder = GrammarRuleSetBuilder<N>()
        builder.block()
        rules.getOrPut(symbol) { mutableSetOf() } += builder.build()
    }
}

class GrammarRuleSetBuilder<N> {
    
    private val set = mutableSetOf<GrammarRule<N>>()
    
    fun build() = set.toSet()
    
    fun <U> terminal(matcher: TerminalMatcher<N, U>) {
        set += GrammarRule.MatchTerminal(matcher)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0> symbols(
        comp0: GrammarSymbol<N0>,
        factory: (N0) -> N,
    ) {
        set += GrammarRule.MatchSymbols(listOf(comp0)) { list ->
            factory(list[0] as N0)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1> symbols(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        factory: (N0, N1) -> N,
    ) {
        set += GrammarRule.MatchSymbols(listOf(comp0, comp1)) { list ->
            factory(list[0] as N0, list[1] as N1)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2> symbols(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        factory: (N0, N1, N2) -> N,
    ) {
        set += GrammarRule.MatchSymbols(listOf(comp0, comp1, comp2)) { list ->
            factory(list[0] as N0, list[1] as N1, list[2] as N2)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2, N3> symbols(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        comp3: GrammarSymbol<N3>,
        factory: (N0, N1, N2, N3) -> N,
    ) {
        set += GrammarRule.MatchSymbols(listOf(comp0, comp1, comp2, comp3)) { list ->
            factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2, N3, N4> symbols(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        comp3: GrammarSymbol<N3>,
        comp4: GrammarSymbol<N4>,
        factory: (N0, N1, N2, N3, N4) -> N,
    ) {
        set += GrammarRule.MatchSymbols(listOf(comp0, comp1, comp2, comp3, comp4)) { list ->
            factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2, N3, N4, N5> symbols(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        comp3: GrammarSymbol<N3>,
        comp4: GrammarSymbol<N4>,
        comp5: GrammarSymbol<N5>,
        factory: (N0, N1, N2, N3, N4, N5) -> N,
    ) {
        set += GrammarRule.MatchSymbols(listOf(comp0, comp1, comp2, comp3, comp4, comp5)) { list ->
            factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2, N3, N4, N5, N6> symbols(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        comp3: GrammarSymbol<N3>,
        comp4: GrammarSymbol<N4>,
        comp5: GrammarSymbol<N5>,
        comp6: GrammarSymbol<N6>,
        factory: (N0, N1, N2, N3, N4, N5, N6) -> N,
    ) {
        set += GrammarRule.MatchSymbols(listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6)) { list ->
            factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5, list[6] as N6)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2, N3, N4, N5, N6, N7> symbols(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        comp3: GrammarSymbol<N3>,
        comp4: GrammarSymbol<N4>,
        comp5: GrammarSymbol<N5>,
        comp6: GrammarSymbol<N6>,
        comp7: GrammarSymbol<N7>,
        factory: (N0, N1, N2, N3, N4, N5, N6, N7) -> N,
    ) {
        set += GrammarRule.MatchSymbols(listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6, comp7)) { list ->
            factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5, list[6] as N6, list[7] as N7)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2, N3, N4, N5, N6, N7, N8> symbols(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        comp3: GrammarSymbol<N3>,
        comp4: GrammarSymbol<N4>,
        comp5: GrammarSymbol<N5>,
        comp6: GrammarSymbol<N6>,
        comp7: GrammarSymbol<N7>,
        comp8: GrammarSymbol<N8>,
        factory: (N0, N1, N2, N3, N4, N5, N6, N7, N8) -> N,
    ) {
        set += GrammarRule.MatchSymbols(listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6, comp7, comp8)) { list ->
            factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5, list[6] as N6, list[7] as N7, list[8] as N8)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2, N3, N4, N5, N6, N7, N8, N9> symbols(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        comp3: GrammarSymbol<N3>,
        comp4: GrammarSymbol<N4>,
        comp5: GrammarSymbol<N5>,
        comp6: GrammarSymbol<N6>,
        comp7: GrammarSymbol<N7>,
        comp8: GrammarSymbol<N8>,
        comp9: GrammarSymbol<N9>,
        factory: (N0, N1, N2, N3, N4, N5, N6, N7, N8, N9) -> N,
    ) {
        set += GrammarRule.MatchSymbols(listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6, comp7, comp8, comp9)) { list ->
            factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5, list[6] as N6, list[7] as N7, list[8] as N8, list[9] as N9)
        }
    }
}
