package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.FoxType

data class TypeInferenceRequest(
    val actual: FoxType,
    val expected: FoxType,
)

data class TypeInferenceResult(
    val actual: FoxType,
    val expected: FoxType,
    val substitutions: Map<String, FoxType> = emptyMap(),
)

object TypeInferPass : TypePass<TypeInferenceRequest, TypeInferenceResult> {
    override fun run(input: TypeInferenceRequest, context: TypePassContext): TypePassResult<TypeInferenceResult> {
        // Constraint collection / solving will move here incrementally.
        return TypePassResult(TypeInferenceResult(input.actual, input.expected))
    }
}
