package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.FoxFile
import pers.hpcx.foxlang.ast.FoxType

data class TypePassContext(
    val file: FoxFile? = null,
)

interface TypePassIssueCode {
    val key: String
}

enum class TypePassIssueSeverity {
    INFO,
    WARNING,
    ERROR,
}

data class TypePassIssue(
    val code: TypePassIssueCode,
    val severity: TypePassIssueSeverity,
    val message: String,
    val type: FoxType? = null,
)

data class TypePassResult<T>(
    val value: T,
    val issues: List<TypePassIssue> = emptyList(),
) {
    val isSuccess: Boolean
        get() = issues.none { it.severity == TypePassIssueSeverity.ERROR }
}

interface TypePass<in I, O> {
    fun run(input: I, context: TypePassContext = TypePassContext()): TypePassResult<O>
}

interface FoxTypePass : TypePass<FoxType, FoxType>
