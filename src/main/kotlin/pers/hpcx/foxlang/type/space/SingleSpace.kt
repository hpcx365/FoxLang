package pers.hpcx.foxlang.type.space

fun <T, C : SpaceContext<T>> singleSpace(single: T): TraversableSpace<T, C> = SingleSpace(single)

data class SingleSpace<T, C : SpaceContext<T>>(val single: T) : TraversableSpace<T, C> {
    
    override fun contains(that: T, context: C) = context.compare(single, that) == 0
    
    override fun traverser(context: C) = object : SpaceTraverser<T> {
        
        private var current: T? = single
        
        override fun current() = current
        
        override fun seekNext() {
            current = null
        }
        
        override fun seekCeilOf(that: T) {
            current?.let {
                if (context.compare(it, that) < 0) {
                    current = null
                }
            }
        }
    }
}
