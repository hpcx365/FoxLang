package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet

data class FoxFile(val elements: List<FoxFileElement>)

sealed interface FoxFileElement

data class FoxTypeAlias(
    val name: String,
    val generics: OrderedSet<String>,
    val alias: FoxType,
) : FoxFileElement

data class FoxMethodDefinition(
    val generics: OrderedMap<String, FoxType>,
    val thisType: FoxType,
    val name: String,
    val parameters: OrderedMap<String, FoxType>,
    val returnType: FoxType,
    val body: FoxStatement,
) : FoxFileElement
