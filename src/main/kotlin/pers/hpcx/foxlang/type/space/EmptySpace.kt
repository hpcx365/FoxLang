package pers.hpcx.foxlang.type.space

fun <T, C : SpaceContext<T>> emptySpace(): TraversableSpace<T, C> = @Suppress("UNCHECKED_CAST") (EmptySpace as TraversableSpace<T, C>)

data object EmptySpace : TraversableSpace<Any, SpaceContext<Any>> {
    
    override fun contains(that: Any, context: SpaceContext<Any>) = false
    
    override fun traverser(context: SpaceContext<Any>) = object : SpaceTraverser<Any> {
        
        override fun current(): Any? = null
        
        override fun seekNext() {}
        
        override fun seekCeilOf(that: Any) {}
    }
}
