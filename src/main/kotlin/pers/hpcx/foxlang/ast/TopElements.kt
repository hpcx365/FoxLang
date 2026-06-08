package pers.hpcx.foxlang.ast

import java.util.*

data class FoxFile(
    val elements: List<FoxFileElement>,
)

sealed interface FoxFileElement

data class FoxTypeAlias(
    val name: String,
    val generics: SequencedSet<String>?,
    val alias: FoxType,
) : FoxFileElement

data class FoxMethodDefinition(
    val generics: SequencedMap<String, FoxGenericConstraint>?,
    val thisType: FoxType?,
    val name: String,
    val parameters: SequencedMap<String, FoxType>,
    val returnType: FoxType?,
    val body: FoxStatement,
) : FoxFileElement

data class FoxGenericConstraint(
    val match: FoxType?,
)
