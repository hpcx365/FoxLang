package pers.hpcx.foxlang.frontend

import dk.brics.automaton.RegExp
import dk.brics.automaton.RunAutomaton
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet
import kotlin.reflect.KClass

data class Grammar(
    val rules: Map<Symbol<*>, Set<Rule<*>>>,
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
data class FixedRule<N>(val string: String, val factory: (PlainFragment) -> N) : LeafRule<N>
data class RegexRule<N>(val regex: Regex, val factory: (PlainFragment) -> N) : LeafRule<N> {
    val automaton by lazy { RunAutomaton(RegExp(regex.pattern).toAutomaton(true), true) }
}

data class LineBreakRule<N>(val factory: (LineBreakFragment) -> N) : LeafRule<N>
data class CharLiteralRule<N>(val factory: (CharLiteralFragment) -> N) : LeafRule<N>
data class StringLiteralRule<N>(val factory: (StringLiteralFragment) -> N) : LeafRule<N>
data class FormattedStringStartRule<N>(val factory: (FormattedStringStartFragment) -> N) : LeafRule<N>
data class FormattedStringTextRule<N>(val factory: (FormattedStringTextFragment) -> N) : LeafRule<N>
data class FormattedExpressionStartRule<N>(val factory: (FormattedExpressionStartFragment) -> N) : LeafRule<N>
data class FormattedExpressionEndRule<N>(val factory: (FormattedExpressionEndFragment) -> N) : LeafRule<N>
data class FormattedStringEndRule<N>(val factory: (FormattedStringEndFragment) -> N) : LeafRule<N>

data class NonLeafRule<N>(
    val components: List<Symbol<*>>,
    val factory: (List<*>) -> N,
) : Rule<N> {
    init {
        require(components.isNotEmpty())
    }
}
