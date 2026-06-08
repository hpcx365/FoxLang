package pers.hpcx.foxlang.parser

import java.util.*
import kotlin.reflect.KClass

inline fun <reified T : Any> node() = ClassNonTerminal(T::class)
fun token(token: String) = node<String>().name(token)
fun <F, S> NonTerminal<F>.pair(second: NonTerminal<S>): NonTerminal<Pair<F, S>> = PairNonTerminal(this, second)
fun <N> NonTerminal<N>.list(): NonTerminal<List<N>> = ListNonTerminal(this)
fun <N> NonTerminal<N>.set(): NonTerminal<Set<N>> = SetNonTerminal(this)
fun <N> NonTerminal<N>.seqSet(): NonTerminal<SequencedSet<N>> = SeqSetNonTerminal(this)
fun <K, V> NonTerminal<K>.map(value: NonTerminal<V>): NonTerminal<Map<K, V>> = MapNonTerminal(this, value)
fun <K, V> NonTerminal<K>.seqMap(value: NonTerminal<V>): NonTerminal<SequencedMap<K, V>> = SeqMapNonTerminal(this, value)
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

data class SeqSetNonTerminal<N>(val type: NonTerminal<N>) : NonTerminal<SequencedSet<N>> {
    override fun toString() = "SeqSet<${type}>"
}

data class MapNonTerminal<K, V>(val key: NonTerminal<K>, val value: NonTerminal<V>) : NonTerminal<Map<K, V>> {
    override fun toString() = "Map<${key}, ${value}>"
}

data class SeqMapNonTerminal<K, V>(val key: NonTerminal<K>, val value: NonTerminal<V>) : NonTerminal<SequencedMap<K, V>> {
    override fun toString() = "SeqMap<${key}, ${value}>"
}

data class NamedNonTerminal<N>(val type: NonTerminal<N>, val name: String) : NonTerminal<N> {
    override fun toString() = "$name($type)"
}
