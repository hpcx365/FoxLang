package pers.hpcx.foxlang.utils

fun <T> T.link(children: Map<Edge, Tree<T>>) = Tree(this, children)
fun <T> T.link(vararg children: Pair<Edge, Tree<T>>) = Tree(this, children.toMap())

data class Tree<T>(val value: T, val children: Map<Edge, Tree<T>>) {
    
    operator fun get(child: Edge) = children[child]
    operator fun get(vararg path: Edge) = get(path.toList())
    operator fun get(path: List<Edge>): Tree<T>? {
        var current = this
        path.forEach { child ->
            current = current[child] ?: return null
        }
        return current
    }
}

val String.onField get() = FieldEdge(this)
val Int.onIndex get() = IndexEdge(this)
val Any?.onKey get() = KeyEdge(this)

sealed interface Edge
data class FieldEdge(val name: String) : Edge
data class IndexEdge(val index: Int) : Edge
data class KeyEdge(val key: Any?) : Edge
