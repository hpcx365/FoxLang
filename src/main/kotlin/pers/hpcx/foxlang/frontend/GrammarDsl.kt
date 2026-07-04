package pers.hpcx.foxlang.frontend

fun buildGrammar(block: GrammarBuilder.() -> Unit): Grammar {
    val builder = GrammarBuilder()
    builder.block()
    return builder.build()
}

class GrammarBuilder {
    
    private val rules = mutableMapOf<Symbol<*>, Set<Rule<*>>>()
    
    fun build() = Grammar(rules)
    
    @Suppress("UNCHECKED_CAST")
    fun <N> rules(symbol: Symbol<N>, block: RuleSetBuilder<N>.() -> Unit) {
        val builder = RuleSetBuilder<N>()
        builder.block()
        rules[symbol] = builder.set
    }
    
    class RuleSetBuilder<N> {
        
        val set = mutableSetOf<Rule<N>>()
        
        fun fixed(string: String, factory: (PlainFragment) -> N) {
            set += FixedRule(string, factory)
        }
        
        fun regex(regex: Regex, factory: (PlainFragment) -> N) {
            set += RegexRule(regex, factory)
        }
        
        fun lineBreak(factory: (LineBreakFragment) -> N) {
            set += LineBreakRule(factory)
        }
        
        fun charLiteral(factory: (CharLiteralFragment) -> N) {
            set += CharLiteralRule(factory)
        }
        
        fun stringLiteral(factory: (StringLiteralFragment) -> N) {
            set += StringLiteralRule(factory)
        }
        
        fun formattedStringStart(factory: (FormattedStringStartFragment) -> N) {
            set += FormattedStringStartRule(factory)
        }
        
        fun formattedStringText(factory: (FormattedStringTextFragment) -> N) {
            set += FormattedStringTextRule(factory)
        }
        
        fun formattedExpressionStart(factory: (FormattedExpressionStartFragment) -> N) {
            set += FormattedExpressionStartRule(factory)
        }
        
        fun formattedExpressionEnd(factory: (FormattedExpressionEndFragment) -> N) {
            set += FormattedExpressionEndRule(factory)
        }
        
        fun formattedStringEnd(factory: (FormattedStringEndFragment) -> N) {
            set += FormattedStringEndRule(factory)
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0> symbols(
            comp0: Symbol<N0>,
            factory: (N0) -> N,
        ) {
            set += NonLeafRule(listOf(comp0)) { list ->
                factory(list[0] as N0)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1> symbols(
            comp0: Symbol<N0>,
            comp1: Symbol<N1>,
            factory: (N0, N1) -> N,
        ) {
            set += NonLeafRule(listOf(comp0, comp1)) { list ->
                factory(list[0] as N0, list[1] as N1)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1, N2> symbols(
            comp0: Symbol<N0>,
            comp1: Symbol<N1>,
            comp2: Symbol<N2>,
            factory: (N0, N1, N2) -> N,
        ) {
            set += NonLeafRule(listOf(comp0, comp1, comp2)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1, N2, N3> symbols(
            comp0: Symbol<N0>,
            comp1: Symbol<N1>,
            comp2: Symbol<N2>,
            comp3: Symbol<N3>,
            factory: (N0, N1, N2, N3) -> N,
        ) {
            set += NonLeafRule(listOf(comp0, comp1, comp2, comp3)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1, N2, N3, N4> symbols(
            comp0: Symbol<N0>,
            comp1: Symbol<N1>,
            comp2: Symbol<N2>,
            comp3: Symbol<N3>,
            comp4: Symbol<N4>,
            factory: (N0, N1, N2, N3, N4) -> N,
        ) {
            set += NonLeafRule(listOf(comp0, comp1, comp2, comp3, comp4)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1, N2, N3, N4, N5> symbols(
            comp0: Symbol<N0>,
            comp1: Symbol<N1>,
            comp2: Symbol<N2>,
            comp3: Symbol<N3>,
            comp4: Symbol<N4>,
            comp5: Symbol<N5>,
            factory: (N0, N1, N2, N3, N4, N5) -> N,
        ) {
            set += NonLeafRule(listOf(comp0, comp1, comp2, comp3, comp4, comp5)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1, N2, N3, N4, N5, N6> symbols(
            comp0: Symbol<N0>,
            comp1: Symbol<N1>,
            comp2: Symbol<N2>,
            comp3: Symbol<N3>,
            comp4: Symbol<N4>,
            comp5: Symbol<N5>,
            comp6: Symbol<N6>,
            factory: (N0, N1, N2, N3, N4, N5, N6) -> N,
        ) {
            set += NonLeafRule(listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5, list[6] as N6)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1, N2, N3, N4, N5, N6, N7> symbols(
            comp0: Symbol<N0>,
            comp1: Symbol<N1>,
            comp2: Symbol<N2>,
            comp3: Symbol<N3>,
            comp4: Symbol<N4>,
            comp5: Symbol<N5>,
            comp6: Symbol<N6>,
            comp7: Symbol<N7>,
            factory: (N0, N1, N2, N3, N4, N5, N6, N7) -> N,
        ) {
            set += NonLeafRule(listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6, comp7)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5, list[6] as N6, list[7] as N7)
            }
        }
    }
}
