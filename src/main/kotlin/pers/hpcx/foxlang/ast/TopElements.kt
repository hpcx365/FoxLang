package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.frontend.common.SourceSpan
import pers.hpcx.foxlang.utils.*

data class FoxFile(val elements: List<FoxFileElement>)
data class ParsedFoxFile(
    val elements: List<ParsedFoxFileElement<*>>,
    override val span: SourceSpan,
) : Parsed<FoxFile> {
    override val node get() = FoxFile(elements.map { it.node })
}

sealed interface FoxFileElement
sealed interface ParsedFoxFileElement<N : FoxFileElement> : Parsed<N>

data class FoxTypeAlias(
    val name: String,
    val generics: OrderedSet<String>,
    val alias: FoxType,
) : FoxFileElement

data class ParsedFoxTypeAlias(
    val name: ParsedString,
    val generics: ParsedList<ParsedString>?,
    val alias: ParsedFoxType<*>,
    override val span: SourceSpan,
) : ParsedFoxFileElement<FoxTypeAlias> {
    override val node
        get() = FoxTypeAlias(
            name.node,
            generics?.node?.map { it.node }?.toOrderedSet() ?: emptyOrderedSet(),
            alias.node,
        )
}

data class FoxMethodDefinition(
    val generics: OrderedMap<String, FoxType>,
    val thisType: FoxType,
    val name: String,
    val parameters: OrderedMap<String, FoxType>,
    val returnType: FoxType,
    val body: FoxStatement,
) : FoxFileElement

data class ParsedFoxMethodDefinition(
    val generics: ParsedList<ParsedPair<ParsedString, ParsedFoxType<*>?>>?,
    val thisType: ParsedFoxType<*>?,
    val name: ParsedString,
    val parameters: ParsedList<ParsedPair<ParsedString, ParsedFoxType<*>>>,
    val returnType: ParsedFoxType<*>?,
    val body: ParsedFoxStatement<*>,
    override val span: SourceSpan,
) : ParsedFoxFileElement<FoxMethodDefinition> {
    override val node
        get() = FoxMethodDefinition(
            generics?.node?.map { it.node.first.node to (it.node.second?.node ?: FoxAnyType) }?.toOrderedMap() ?: emptyOrderedMap(),
            thisType?.node ?: FoxUnitType,
            name.node,
            parameters.node.map { it.node.first.node to it.node.second.node }.toOrderedMap(),
            returnType?.node ?: FoxUnitType,
            body.node,
        )
}
