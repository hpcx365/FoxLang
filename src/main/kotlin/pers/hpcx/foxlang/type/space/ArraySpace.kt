package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxArrayType
import pers.hpcx.foxlang.ast.FoxType

fun arraySpace(element: Space<FoxType>) = ArraySpace(element)

data class ArraySpace(val element: Space<FoxType>) : Space<FoxType> {
    
    override fun contains(that: FoxType) = that is FoxArrayType && element.contains(that.element)
}

fun arrayElementOf(generic: String) = ArrayElementProjectionSpace(generic)

data class ArrayElementProjectionSpace(val generic: String) : PlaceHolderSpace<FoxType>
