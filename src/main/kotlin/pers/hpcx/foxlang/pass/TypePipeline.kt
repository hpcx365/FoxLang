package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.FoxType

object TypePipeline {
    fun resolve(type: FoxType, context: TypePassContext = TypePassContext()): TypePassResult<FoxType> {
        return TypeResolvePass.run(type, context)
    }
    
    fun normalize(type: FoxType, context: TypePassContext = TypePassContext()): TypePassResult<FoxType> {
        val resolved = resolve(type, context)
        val normalized = TypeNormalizePass.run(resolved.value, context)
        return TypePassResult(normalized.value, resolved.issues + normalized.issues)
    }
    
    fun checkPattern(type: FoxType, context: TypePassContext = TypePassContext()): TypePassResult<FoxType> {
        val resolved = resolve(type, context)
        val checked = TypePatternCheckPass.run(resolved.value, context)
        val normalized = TypeNormalizePass.run(checked.value, context)
        return TypePassResult(normalized.value, resolved.issues + checked.issues + normalized.issues)
    }
}
