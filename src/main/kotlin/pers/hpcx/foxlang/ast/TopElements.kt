package pers.hpcx.foxlang.ast

data class FoxFile(
    val elements: List<FoxFileElement>,
)

sealed interface FoxFileElement

data class FoxTypeAlias(
    val name: String,
    val generics: Set<String>?,
    val alias: FoxType,
) : FoxFileElement

data class FoxMethodDefinition(
    val generics: Map<String, FoxGenericConstraint>?,
    val thisType: FoxType?,
    val name: String,
    val parameters: Map<String, FoxType>,
    val returnType: FoxType?,
    val body: FoxStatement,
) : FoxFileElement

data class FoxGenericConstraint(
    val match: FoxType?,
)
