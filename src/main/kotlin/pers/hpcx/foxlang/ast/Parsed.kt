package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.frontend.common.SourceSpan

interface Parsed<N> {
    val node: N
    val span: SourceSpan
}

data class ParsedUnit(
    override val span: SourceSpan,
) : Parsed<Unit> {
    override val node get() = Unit
}

data class ParsedBoolean(
    override val node: Boolean,
    override val span: SourceSpan,
) : Parsed<Boolean>

data class ParsedInt(
    override val node: Int,
    override val span: SourceSpan,
) : Parsed<Int>

data class ParsedLong(
    override val node: Long,
    override val span: SourceSpan,
) : Parsed<Long>

data class ParsedFloat(
    override val node: Float,
    override val span: SourceSpan,
) : Parsed<Float>

data class ParsedDouble(
    override val node: Double,
    override val span: SourceSpan,
) : Parsed<Double>

data class ParsedChar(
    override val node: Char,
    override val span: SourceSpan,
) : Parsed<Char>

data class ParsedString(
    override val node: String,
    override val span: SourceSpan,
) : Parsed<String>

data class ParsedPair<T, U>(
    override val node: Pair<T, U>,
    override val span: SourceSpan,
) : Parsed<Pair<T, U>>

data class ParsedList<T>(
    override val node: List<T>,
    override val span: SourceSpan,
) : Parsed<List<T>>

operator fun <T> ParsedList<T>.plus(other: Parsed<T>) = ParsedList(node + other.node, span + other.span)
operator fun <T> ParsedList<T>.plus(other: ParsedList<T>) = ParsedList(node + other.node, span + other.span)

fun mergeSpan(vararg spans: Parsed<*>): SourceSpan {
    var result = spans[0].span
    (1..<spans.size).forEach { result += spans[it].span }
    return result
}
