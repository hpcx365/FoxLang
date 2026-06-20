package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.FoxType
import pers.hpcx.foxlang.type.space.*
import pers.hpcx.foxlang.utils.orderedSetOf

open class TypeSpaceTestBase {
    
    protected val context = TypeSpaceContext(
        structFieldNames = orderedSetOf("x", "y", "z", "w"),
        objectMemberNames = orderedSetOf("name", "phone", "email", "address"),
        enumItemNames = orderedSetOf("Associate", "Bachelor", "Master", "Doctor"),
    )
    
    protected fun choice(vararg types: FoxType): TraversableTypeSpace {
        return when (types.size) {
            0 -> emptySpace()
            1 -> singleSpace(types[0])
            else -> union(types.map { singleSpace(it) })
        }
    }
}
