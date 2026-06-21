package pers.hpcx.foxlang.type.space

sealed interface Space<T> {
    operator fun contains(that: T): Boolean
}

sealed interface PlaceHolderSpace<T> : Space<T> {
    override fun contains(that: T): Boolean {
        error("Placeholder space cannot be used")
    }
}

fun <T> emptySpace() = @Suppress("UNCHECKED_CAST") (EmptySpace as Space<T>)

data object EmptySpace : Space<Any?> {
    
    override fun contains(that: Any?) = false
}

fun <T> singleSpace(single: T): Space<T> = SingleSpace(single)

data class SingleSpace<T>(val single: T) : Space<T> {
    
    override fun contains(that: T) = single == that
}

fun <T> universalSpace() = @Suppress("UNCHECKED_CAST") (UniversalSpace as Space<T>)

data object UniversalSpace : Space<Any?> {
    
    override fun contains(that: Any?) = true
}

fun <T> intersect(vararg spaces: Space<T>) = intersect(spaces.toList())

fun <T> intersect(spaces: Iterable<Space<T>>) = IntersectSpace(spaces)

data class IntersectSpace<T>(val parts: Iterable<Space<T>>) : Space<T> {
    
    override fun contains(that: T) = parts.all { it.contains(that) }
}

fun <T> union(vararg spaces: Space<T>) = union(spaces.toList())

fun <T> union(spaces: Iterable<Space<T>>) = UnionSpace(spaces)

data class UnionSpace<T>(val parts: Iterable<Space<T>>) : Space<T> {
    
    override fun contains(that: T) = parts.any { it.contains(that) }
}

fun <T> subtract(base: Space<T>, removed: Space<T>) = SubtractSpace(base, removed)

data class SubtractSpace<T>(val base: Space<T>, val removed: Space<T>) : Space<T> {
    
    override fun contains(that: T) = base.contains(that) && !removed.contains(that)
}
