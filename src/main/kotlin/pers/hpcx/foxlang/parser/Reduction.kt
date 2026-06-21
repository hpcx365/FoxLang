package pers.hpcx.foxlang.parser

class ParseException(override val message: String) : Exception(message)

sealed interface Reduction<N> {
    val span: SourceSpan
    val nonTerminal: NonTerminal<N>
}

data class ExactReduction<N>(
    val node: N,
    override val span: SourceSpan,
    override val nonTerminal: NonTerminal<N>,
) : Reduction<N> {
}

data class FuzzyReduction<N>(
    val confidence: Double,
    override val span: SourceSpan,
    override val nonTerminal: NonTerminal<N>,
) : Reduction<N>
