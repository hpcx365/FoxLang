package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.FoxMethodType
import pers.hpcx.foxlang.ast.FoxStructType
import pers.hpcx.foxlang.ast.FoxType

fun methodSpace(`this`: Space<FoxType>, parameters: Space<FoxType>, `return`: Space<FoxType>) =
    MethodSpace(`this`, parameters, `return`)

data class MethodSpace(
    val `this`: Space<FoxType>,
    val parameters: Space<FoxType>,
    val `return`: Space<FoxType>,
) : Space<FoxType> {
    
    override fun contains(that: FoxType): Boolean {
        if (that !is FoxMethodType) return false
        return `this`.contains(that.`this`)
            && parameters.contains(FoxStructType(that.parameters))
            && `return`.contains(that.`return`)
    }
}

fun methodThisOf(generic: String) = MethodThisProjectionSpace(generic)

data class MethodThisProjectionSpace(val generic: String) : PlaceHolderSpace<FoxType>

fun methodParametersOf(generic: String) = MethodParametersProjectionSpace(generic)

data class MethodParametersProjectionSpace(val generic: String) : PlaceHolderSpace<FoxType>

fun methodReturnOf(generic: String) = MethodReturnProjectionSpace(generic)

data class MethodReturnProjectionSpace(val generic: String) : PlaceHolderSpace<FoxType>
