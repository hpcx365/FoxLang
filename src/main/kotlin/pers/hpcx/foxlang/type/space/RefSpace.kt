package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxRefType
import pers.hpcx.foxlang.ast.FoxType

fun refSpace(referent: Space<FoxType>) = RefSpace(referent)

data class RefSpace(val referent: Space<FoxType>) : Space<FoxType> {
    
    override fun contains(that: FoxType) = that is FoxRefType && referent.contains(that.referent)
}

fun refReferentOf(generic: String) = RefReferentProjectionSpace(generic)

data class RefReferentProjectionSpace(val generic: String) : PlaceHolderSpace<FoxType>
