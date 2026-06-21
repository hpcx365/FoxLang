package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxType

fun generic(generic: String) = GenericSpace(generic)

data class GenericSpace(val generic: String) : PlaceHolderSpace<FoxType>
