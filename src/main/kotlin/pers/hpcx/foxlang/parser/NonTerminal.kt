package pers.hpcx.foxlang.parser

import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet
import kotlin.reflect.KClass

inline fun <reified T : Any> node() = ClassNonTerminal(T::class)
fun token(token: String) = node<String>().name(token)
fun <F, S> NonTerminal<F>.pair(second: NonTerminal<S>): NonTerminal<Pair<F, S>> = PairNonTerminal(this, second)
fun <N> NonTerminal<N>.list(): NonTerminal<List<N>> = ListNonTerminal(this)
fun <N> NonTerminal<N>.set(): NonTerminal<Set<N>> = SetNonTerminal(this)
fun <K, V> NonTerminal<K>.map(value: NonTerminal<V>): NonTerminal<Map<K, V>> = MapNonTerminal(this, value)
fun <N> NonTerminal<N>.orderedSet(): NonTerminal<OrderedSet<N>> = OrderedSetNonTerminal(this)
fun <K, V> NonTerminal<K>.orderedMap(value: NonTerminal<V>): NonTerminal<OrderedMap<K, V>> = OrderedMapNonTerminal(this, value)
fun <N> NonTerminal<N>.name(name: String): NonTerminal<N> = NamedNonTerminal(this, name)

sealed interface NonTerminal<N>

data class ClassNonTerminal<N : Any>(val clazz: KClass<N>) : NonTerminal<N> {
    override fun toString() = clazz.simpleName ?: clazz.toString()
}

data class PairNonTerminal<F, S>(val first: NonTerminal<F>, val second: NonTerminal<S>) : NonTerminal<Pair<F, S>> {
    override fun toString() = "Pair<${first}, ${second}>"
}

data class ListNonTerminal<N>(val type: NonTerminal<N>) : NonTerminal<List<N>> {
    override fun toString() = "List<${type}>"
}

data class SetNonTerminal<N>(val type: NonTerminal<N>) : NonTerminal<Set<N>> {
    override fun toString() = "Set<${type}>"
}

data class MapNonTerminal<K, V>(val key: NonTerminal<K>, val value: NonTerminal<V>) : NonTerminal<Map<K, V>> {
    override fun toString() = "Map<${key}, ${value}>"
}

data class OrderedSetNonTerminal<N>(val type: NonTerminal<N>) : NonTerminal<OrderedSet<N>> {
    override fun toString() = "OrderedSet<${type}>"
}

data class OrderedMapNonTerminal<K, V>(val key: NonTerminal<K>, val value: NonTerminal<V>) : NonTerminal<OrderedMap<K, V>> {
    override fun toString() = "OrderedMap<${key}, ${value}>"
}

data class NamedNonTerminal<N>(val type: NonTerminal<N>, val name: String) : NonTerminal<N> {
    override fun toString() = "$name($type)"
}
