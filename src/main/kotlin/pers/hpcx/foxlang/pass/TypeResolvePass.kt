package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.FoxType

object TypeResolvePass : FoxTypePass {
    override fun run(input: FoxType, context: TypePassContext): TypePassResult<FoxType> {
        // Alias expansion and generic-name resolution will move here incrementally.
        return TypePassResult(input)
    }
}
