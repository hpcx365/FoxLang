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
    val span: SourceSpan,
    val node: N,
)

fun <N> factorySuccess(node: N): GrammarRuleFactoryResult<N> = GrammarRuleFactorySuccess(node)
fun factoryError(message: String): GrammarRuleFactoryResult<Nothing> = GrammarRuleFactoryFailure(message)

sealed interface GrammarRuleFactoryResult<out N>
data class GrammarRuleFactorySuccess<N>(val node: N) : GrammarRuleFactoryResult<N>
data class GrammarRuleFactoryFailure(val message: String) : GrammarRuleFactoryResult<Nothing>

sealed interface GrammarRule<N> {
    
    data class MatchTerminal<N, U>(
        val matcher: TerminalMatcher<N, U>,
    ) : GrammarRule<N>
    
    data class MatchSymbols<N>(
        val components: List<GrammarSymbol<*>>,
        val factory: (List<*>) -> GrammarRuleFactoryResult<N>,
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
        symbolsResult(comp0) { item0 ->
            factorySuccess(factory(item0))
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0> symbolsResult(
        comp0: GrammarSymbol<N0>,
        factory: (N0) -> GrammarRuleFactoryResult<N>,
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
        symbolsResult(comp0, comp1) { item0, item1 ->
            factorySuccess(factory(item0, item1))
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1> symbolsResult(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        factory: (N0, N1) -> GrammarRuleFactoryResult<N>,
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
        symbolsResult(comp0, comp1, comp2) { item0, item1, item2 ->
            factorySuccess(factory(item0, item1, item2))
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2> symbolsResult(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        factory: (N0, N1, N2) -> GrammarRuleFactoryResult<N>,
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
        symbolsResult(comp0, comp1, comp2, comp3) { item0, item1, item2, item3 ->
            factorySuccess(factory(item0, item1, item2, item3))
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2, N3> symbolsResult(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        comp3: GrammarSymbol<N3>,
        factory: (N0, N1, N2, N3) -> GrammarRuleFactoryResult<N>,
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
        symbolsResult(comp0, comp1, comp2, comp3, comp4) { item0, item1, item2, item3, item4 ->
            factorySuccess(factory(item0, item1, item2, item3, item4))
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2, N3, N4> symbolsResult(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        comp3: GrammarSymbol<N3>,
        comp4: GrammarSymbol<N4>,
        factory: (N0, N1, N2, N3, N4) -> GrammarRuleFactoryResult<N>,
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
        symbolsResult(comp0, comp1, comp2, comp3, comp4, comp5) { item0, item1, item2, item3, item4, item5 ->
            factorySuccess(factory(item0, item1, item2, item3, item4, item5))
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2, N3, N4, N5> symbolsResult(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        comp3: GrammarSymbol<N3>,
        comp4: GrammarSymbol<N4>,
        comp5: GrammarSymbol<N5>,
        factory: (N0, N1, N2, N3, N4, N5) -> GrammarRuleFactoryResult<N>,
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
        symbolsResult(comp0, comp1, comp2, comp3, comp4, comp5, comp6) { item0, item1, item2, item3, item4, item5, item6 ->
            factorySuccess(factory(item0, item1, item2, item3, item4, item5, item6))
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2, N3, N4, N5, N6> symbolsResult(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        comp3: GrammarSymbol<N3>,
        comp4: GrammarSymbol<N4>,
        comp5: GrammarSymbol<N5>,
        comp6: GrammarSymbol<N6>,
        factory: (N0, N1, N2, N3, N4, N5, N6) -> GrammarRuleFactoryResult<N>,
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
        symbolsResult(comp0, comp1, comp2, comp3, comp4, comp5, comp6, comp7) { item0, item1, item2, item3, item4, item5, item6, item7 ->
            factorySuccess(factory(item0, item1, item2, item3, item4, item5, item6, item7))
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N0, N1, N2, N3, N4, N5, N6, N7> symbolsResult(
        comp0: GrammarSymbol<N0>,
        comp1: GrammarSymbol<N1>,
        comp2: GrammarSymbol<N2>,
        comp3: GrammarSymbol<N3>,
        comp4: GrammarSymbol<N4>,
        comp5: GrammarSymbol<N5>,
        comp6: GrammarSymbol<N6>,
        comp7: GrammarSymbol<N7>,
        factory: (N0, N1, N2, N3, N4, N5, N6, N7) -> GrammarRuleFactoryResult<N>,
    ) {
        set += GrammarRule.MatchSymbols(listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6, comp7)) { list ->
            factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5, list[6] as N6, list[7] as N7)
        }
    }
}
