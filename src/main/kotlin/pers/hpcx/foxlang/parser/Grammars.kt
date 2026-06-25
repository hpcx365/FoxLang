package pers.hpcx.foxlang.parser

import dk.brics.automaton.RegExp
import dk.brics.automaton.RunAutomaton
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet
import kotlin.reflect.KClass

data class Grammar(
    val rules: Map<Symbol<*>, Set<Rule<*>>>,
    val weights: Map<Symbol<*>, Int> = emptyMap(),
) {
    
    val dependencyGraph: Map<Symbol<*>, Set<Symbol<*>>> =
        mutableMapOf<Symbol<*>, MutableSet<Symbol<*>>>().apply {
            rules.forEach { (symbol, rules2) ->
                rules2.forEach { rule ->
                    when (rule) {
                        is LeafRule<*> -> {}
                        is NonLeafRule<*> -> {
                            rule.components.forEach { component ->
                                getOrPut(component) { mutableSetOf() } += symbol
                            }
                        }
                    }
                }
            }
        }
}

fun buildGrammar(block: GrammarBuilder.() -> Unit): Grammar {
    val builder = GrammarBuilder()
    builder.block()
    return builder.build()
}

class GrammarBuilder {
    
    private val rules = mutableMapOf<Symbol<*>, MutableSet<Rule<*>>>()
    private val weights = mutableMapOf<Symbol<*>, Int>()
    
    fun build() = Grammar(rules, weights)
    
    fun <N> weight(symbol: Symbol<N>, weight: Int) {
        require(weight > 0) { "weight must be positive: $weight" }
        weights[symbol] = weight
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N> rules(symbol: Symbol<N>, block: RuleListBuilder<N>.() -> Unit) {
        val set = rules.getOrPut(symbol) { mutableSetOf() }
        RuleListBuilder(set as MutableSet<Rule<N>>).block()
    }
    
    class RuleListBuilder<N>(internal val list: MutableSet<Rule<N>>) {
        
        fun fixed(string: String, factory: (PlainFragment) -> N) {
            list += FixedRule(string, factory)
        }
        
        fun regex(regex: Regex, factory: (PlainFragment) -> N) {
            list += RegexRule(regex, factory)
        }
        
        fun newline(factory: (NewlineFragment) -> N) {
            list += NewlineRule(factory)
        }
        
        fun charLiteral(factory: (CharLiteralFragment) -> N) {
            list += CharLiteralRule(factory)
        }
        
        fun stringLiteral(factory: (StringLiteralFragment) -> N) {
            list += StringLiteralRule(factory)
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0> symbols(
            comp0: Symbol<N0>,
            factory: (N0) -> N,
        ) {
            list += NonLeafRule(listOf(comp0)) { list ->
                factory(list[0] as N0)
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <N0, N1> symbols(
            comp0: Symbol<N0>,
            comp1: Symbol<N1>,
            factory: (N0, N1) -> N,
        ) {
            list += NonLeafRule(listOf(comp0, comp1)) { list ->
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
            list += NonLeafRule(listOf(comp0, comp1, comp2)) { list ->
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
            list += NonLeafRule(listOf(comp0, comp1, comp2, comp3)) { list ->
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
            list += NonLeafRule(listOf(comp0, comp1, comp2, comp3, comp4)) { list ->
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
            list += NonLeafRule(listOf(comp0, comp1, comp2, comp3, comp4, comp5)) { list ->
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
            list += NonLeafRule(listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6)) { list ->
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
            list += NonLeafRule(listOf(comp0, comp1, comp2, comp3, comp4, comp5, comp6, comp7)) { list ->
                factory(list[0] as N0, list[1] as N1, list[2] as N2, list[3] as N3, list[4] as N4, list[5] as N5, list[6] as N6, list[7] as N7)
            }
        }
    }
}

inline fun <reified T : Any> node() = ClassSymbol(T::class)
fun token(token: String) = node<String>().name(token)
fun <N> Symbol<N>.optional(): Symbol<N?> = OptionalSymbol(this)
fun <F, S> Symbol<F>.pair(second: Symbol<S>): Symbol<Pair<F, S>> = PairSymbol(this, second)
fun <N> Symbol<N>.list(): Symbol<List<N>> = ListSymbol(this)
fun <N> Symbol<N>.set(): Symbol<Set<N>> = SetSymbol(this)
fun <K, V> Symbol<K>.map(value: Symbol<V>): Symbol<Map<K, V>> = MapSymbol(this, value)
fun <N> Symbol<N>.orderedSet(): Symbol<OrderedSet<N>> = OrderedSetSymbol(this)
fun <K, V> Symbol<K>.orderedMap(value: Symbol<V>): Symbol<OrderedMap<K, V>> = OrderedMapSymbol(this, value)
fun <N> Symbol<N>.name(name: String): Symbol<N> = NamedSymbol(this, name)

sealed interface Symbol<N> {
    override fun toString(): String
}

data class ClassSymbol<N : Any>(val clazz: KClass<N>) : Symbol<N> {
    override fun toString() = clazz.simpleName ?: clazz.toString()
}

data class OptionalSymbol<N>(val type: Symbol<N>) : Symbol<N?> {
    override fun toString() = "Optional<${type}>"
}

data class PairSymbol<F, S>(val first: Symbol<F>, val second: Symbol<S>) : Symbol<Pair<F, S>> {
    override fun toString() = "Pair<${first}, ${second}>"
}

data class ListSymbol<N>(val type: Symbol<N>) : Symbol<List<N>> {
    override fun toString() = "List<${type}>"
}

data class SetSymbol<N>(val type: Symbol<N>) : Symbol<Set<N>> {
    override fun toString() = "Set<${type}>"
}

data class MapSymbol<K, V>(val key: Symbol<K>, val value: Symbol<V>) : Symbol<Map<K, V>> {
    override fun toString() = "Map<${key}, ${value}>"
}

data class OrderedSetSymbol<N>(val type: Symbol<N>) : Symbol<OrderedSet<N>> {
    override fun toString() = "OrderedSet<${type}>"
}

data class OrderedMapSymbol<K, V>(val key: Symbol<K>, val value: Symbol<V>) : Symbol<OrderedMap<K, V>> {
    override fun toString() = "OrderedMap<${key}, ${value}>"
}

data class NamedSymbol<N>(val type: Symbol<N>, val name: String) : Symbol<N> {
    override fun toString() = "$name($type)"
}

class RuleFactoryException(message: String) : Exception(message)

sealed interface Rule<N>

sealed interface LeafRule<N> : Rule<N>
class FixedRule<N>(val string: String, val factory: (PlainFragment) -> N) : LeafRule<N>
class RegexRule<N>(val regex: Regex, val factory: (PlainFragment) -> N) : LeafRule<N> {
    val automaton by lazy { RunAutomaton(RegExp(regex.pattern).toAutomaton(true), true) }
}

class NewlineRule<N>(val factory: (NewlineFragment) -> N) : LeafRule<N>
class CharLiteralRule<N>(val factory: (CharLiteralFragment) -> N) : LeafRule<N>
class StringLiteralRule<N>(val factory: (StringLiteralFragment) -> N) : LeafRule<N>

class NonLeafRule<N>(
    val components: List<Symbol<*>>,
    val factory: (List<*>) -> N,
) : Rule<N> {
    init {
        require(components.isNotEmpty())
    }
}
