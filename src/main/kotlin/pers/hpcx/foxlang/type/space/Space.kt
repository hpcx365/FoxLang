package pers.hpcx.foxlang.type.space

interface SpaceContext<T> : Comparator<T>

interface Space<T, C : SpaceContext<T>> {
    fun contains(that: T, context: C): Boolean
}

interface TraversableSpace<T, C : SpaceContext<T>> : Space<T, C> {
    fun traverser(context: C): SpaceTraverser<T>
}

interface SpaceTraverser<T> {
    fun current(): T?
    fun seekNext()
    fun seekCeilOf(that: T)
}

interface ProjectiveSpace<T, C : SpaceContext<T>> : Space<T, C> {
    
    val baseSpace: TraversableSpace<T, C>
    
    fun preimageOf(that: T): TraversableSpace<T, C>
    
    override fun contains(that: T, context: C): Boolean {
        val base = baseSpace
        val preimage = preimageOf(that)
        return intersect(base, preimage).traverser(context).current() != null
    }
}
