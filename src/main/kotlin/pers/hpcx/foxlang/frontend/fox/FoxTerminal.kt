package pers.hpcx.foxlang.frontend.fox

import pers.hpcx.foxlang.frontend.common.*
import pers.hpcx.foxlang.utils.AutoRegex

internal sealed interface FoxTerminal<out F : FoxFragment> {
    data class Fixed(val string: String) : FoxTerminal<PlainFragment>
    data class Regex(val regex: AutoRegex) : FoxTerminal<PlainFragment>
    data object LineBreak : FoxTerminal<LineBreakFragment>
    data object CharLiteral : FoxTerminal<CharLiteralFragment>
    data object StringLiteral : FoxTerminal<StringLiteralFragment>
    data object FormattedStringStart : FoxTerminal<FormattedStringStartFragment>
    data object FormattedStringText : FoxTerminal<FormattedStringTextFragment>
    data object FormattedExpressionStart : FoxTerminal<FormattedExpressionStartFragment>
    data object FormattedExpressionEnd : FoxTerminal<FormattedExpressionEndFragment>
    data object FormattedStringEnd : FoxTerminal<FormattedStringEndFragment>
}

typealias FoxRuleBuilder<N> = GrammarRuleSetBuilder<N>

internal fun <N> FoxRuleBuilder<N>.fixed(string: String, factory: () -> N) {
    terminal(FoxTerminal.Fixed(string).matcher { _ -> factory() })
}

internal fun <N> FoxRuleBuilder<N>.regex(regex: AutoRegex, factory: (String) -> N) {
    terminal(FoxTerminal.Regex(regex).matcher { frag -> factory(frag.text) })
}

internal fun <N> FoxRuleBuilder<N>.lineBreak(factory: () -> N) {
    terminal(FoxTerminal.LineBreak.matcher { _ -> factory() })
}

internal fun <N> FoxRuleBuilder<N>.charLiteral(factory: (Char) -> N) {
    terminal(FoxTerminal.CharLiteral.matcher { frag -> factory(frag.char) })
}

internal fun <N> FoxRuleBuilder<N>.stringLiteral(factory: (String) -> N) {
    terminal(FoxTerminal.StringLiteral.matcher { frag -> factory(frag.string) })
}

internal fun <N> FoxRuleBuilder<N>.formattedStringStart(factory: (Boolean) -> N) {
    terminal(FoxTerminal.FormattedStringStart.matcher { frag -> factory(frag.isRaw) })
}

internal fun <N> FoxRuleBuilder<N>.formattedStringText(factory: (String) -> N) {
    terminal(FoxTerminal.FormattedStringText.matcher { frag -> factory(frag.text) })
}

internal fun <N> FoxRuleBuilder<N>.formattedExpressionStart(factory: () -> N) {
    terminal(FoxTerminal.FormattedExpressionStart.matcher { _ -> factory() })
}

internal fun <N> FoxRuleBuilder<N>.formattedExpressionEnd(factory: () -> N) {
    terminal(FoxTerminal.FormattedExpressionEnd.matcher { _ -> factory() })
}

internal fun <N> FoxRuleBuilder<N>.formattedStringEnd(factory: () -> N) {
    terminal(FoxTerminal.FormattedStringEnd.matcher { _ -> factory() })
}

private fun <F : FoxFragment, N> FoxTerminal<F>.matcher(factory: (F) -> N) = TerminalMatcher { source, start ->
    val match = match(source, start) ?: return@TerminalMatcher null
    TerminalMatch(match.span, factory(match.fragment))
}

@Suppress("UNCHECKED_CAST")
private fun <F : FoxFragment> FoxTerminal<F>.match(
    source: Source<FoxFragment>,
    start: SourcePosition,
) = when (this) {
    is FoxTerminal.Fixed -> matchFixed(source, start, string)
    is FoxTerminal.Regex -> matchRegex(source, start, this)
    FoxTerminal.LineBreak -> source.matchSingle<LineBreakFragment>(start)
    FoxTerminal.CharLiteral -> source.matchSingle<CharLiteralFragment>(start)
    FoxTerminal.StringLiteral -> source.matchSingle<StringLiteralFragment>(start)
    FoxTerminal.FormattedStringStart -> source.matchSingle<FormattedStringStartFragment>(start)
    FoxTerminal.FormattedStringText -> source.matchSingle<FormattedStringTextFragment>(start)
    FoxTerminal.FormattedExpressionStart -> source.matchSingle<FormattedExpressionStartFragment>(start)
    FoxTerminal.FormattedExpressionEnd -> source.matchSingle<FormattedExpressionEndFragment>(start)
    FoxTerminal.FormattedStringEnd -> source.matchSingle<FormattedStringEndFragment>(start)
} as FoxTerminalMatch<F>?

private inline fun <reified F : FoxFragment> Source<FoxFragment>.matchSingle(
    start: SourcePosition,
): FoxTerminalMatch<F>? {
    val fragment = getOrNull(start) as? F ?: return null
    return FoxTerminalMatch(SourceSpan(start, start + 1), fragment)
}

private fun matchFixed(
    source: Source<FoxFragment>,
    start: SourcePosition,
    string: String,
): FoxTerminalMatch<PlainFragment>? {
    var stringIndex = 0
    val seen = mutableListOf<PlainFragment>()
    
    loop@ while (true) {
        val curr = source.getOrNull(start + seen.size) ?: break
        if (curr !is PlainFragment) break
        seen.lastOrNull()?.let { prev ->
            if (prev.line != curr.line || prev.column + prev.text.length != curr.column) {
                break
            }
        }
        curr.text.forEach { char ->
            if (stringIndex >= string.length || string[stringIndex] != char) break@loop
            stringIndex++
        }
        seen += curr
    }
    if (seen.isEmpty() || stringIndex < string.length) return null
    
    val result = buildString { seen.forEach { append(it.text) } }
    check(result == string)
    return FoxTerminalMatch(
        span = SourceSpan(start, start + seen.size),
        fragment = PlainFragment(seen.first().line, seen.first().column, result),
    )
}

private fun matchRegex(
    source: Source<FoxFragment>,
    start: SourcePosition,
    terminal: FoxTerminal.Regex,
): FoxTerminalMatch<PlainFragment>? {
    val automaton = terminal.regex.runner
    var automatonState = automaton.initialState
    val seen = mutableListOf<PlainFragment>()
    var longestMatch = 0
    
    loop@ while (true) {
        val curr = source.getOrNull(start + seen.size) ?: break
        if (curr !is PlainFragment) break
        seen.lastOrNull()?.let { prev ->
            if (prev.line != curr.line || prev.column + prev.text.length != curr.column) {
                break
            }
        }
        curr.text.forEach { char ->
            val nextState = automaton.step(automatonState, char)
            if (nextState < 0) break@loop
            automatonState = nextState
        }
        seen += curr
        if (automaton.isAccept(automatonState)) longestMatch = seen.size
    }
    if (seen.isEmpty() || longestMatch == 0) return null
    
    val result = buildString { seen.subList(0, longestMatch).forEach { append(it.text) } }
    check(result in terminal.regex)
    return FoxTerminalMatch(
        span = SourceSpan(start, start + longestMatch),
        fragment = PlainFragment(seen.first().line, seen.first().column, result),
    )
}

private data class FoxTerminalMatch<F : FoxFragment>(
    val span: SourceSpan,
    val fragment: F,
)
