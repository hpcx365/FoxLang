package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.FoxType
import pers.hpcx.foxlang.type.space.*
import pers.hpcx.foxlang.utils.emptyOrderedSet

open class TypeSpaceContextTestBase {
    
    protected val context = TypeSpaceContext(
        TypeBounds(
            Int.MAX_VALUE,
            Int.MAX_VALUE,
            Int.MAX_VALUE,
            emptyOrderedSet(),
        ),
    )
    protected val finiteContext = TypeSpaceContext(
        TypeBounds(
            maxHeight = 8,
            maxTupleArity = 8,
            maxStructArity = 8,
            nameDictionary = emptyOrderedSet(),
        ),
    )
    
    protected fun choice(vararg types: FoxType): TraversableTypeSpace {
        return when (types.size) {
            0 -> emptySpace()
            1 -> singleSpace(types[0])
            else -> union(types.map { singleSpace(it) })
        }
    }
}
