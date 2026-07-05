@file:Suppress("NOTHING_TO_INLINE")

package pers.hpcx.foxlang.frontend

import pers.hpcx.foxlang.utils.UniqueQueue
import java.util.*

class Parser<N>(val grammar: Grammar, val start: Symbol<N>) {
    
    fun parse(src: String): N? {
        return (analyze(src).buildResult as? ParseContextBuildSuccess)?.node
    }
    
    fun analyze(src: String): ParseAnalysis<N> {
        val source = Source(src)
        val context = ParseContext(grammar, source)
        context.seedLeafRules()
        context.grow()
        return ParseAnalysis(
            grammar = grammar,
            start = start,
            source = source,
            context = context,
            buildResult = context.build(start),
        )
    }
}

data class ParseAnalysis<N>(
    val grammar: Grammar,
    val start: Symbol<N>,
    val source: Source,
    val context: ParseContext,
    val buildResult: ParseContextBuildResult<N>,
)

data class ParseMatch<N>(
    val symbol: Symbol<N>,
    val matchType: ParseMatchType,
    val segments: List<SourceSpan>,
    val grammarRule: GrammarRule<*>?,
) {
    
    init {
        require(segments.isNotEmpty()) { "Match segments must not be empty" }
        segments.zipWithNext().forEach {
            require(it.first.end == it.second.start) { "Match segments must be contiguous" }
        }
        if (matchType.isBuildable()) {
            requireNotNull(grammarRule) { "Grammar rule must not be null for buildable match type" }
        }
        when (grammarRule) {
            null -> {}
            is GrammarRule.LeafRule -> require(segments.size == 1) {
                "Leaf rule match must have exactly one segment"
            }
            is GrammarRule.MatchSymbols -> require(segments.size == grammarRule.components.size) {
                "Match symbols rule match must have exactly ${grammarRule.components.size} segments"
            }
        }
    }
    
    val span: SourceSpan get() = SourceSpan(segments.first().start, segments.last().end)
}

enum class ParseMatchType {
    
    Exact,
    Derived,
    Expected,
    Synthetic;
    
    fun isBuildable(): Boolean = when (this) {
        Exact -> true
        Derived -> true
        Expected -> false
        Synthetic -> false
    }
}

sealed interface ParseContextBuildResult<out N>

data class ParseContextBuildSuccess<N>(
    val node: N,
) : ParseContextBuildResult<N>

data class ParseContextBuildFailure(
    val errors: Set<GrammarRuleFactoryError>,
) : ParseContextBuildResult<Nothing>

data class GrammarRuleFactoryError(
    val symbol: Symbol<*>,
    val span: SourceSpan,
    val message: String,
)

class ParseContext(val grammar: Grammar, val source: Source) {
    
    private val updateQueue = UniqueQueue<Symbol<*>>()
    private val updateMap = mutableMapOf<Symbol<*>, MutableList<ParseMatch<*>>>()
    private val matches = mutableMapOf<Pair<Symbol<*>, SourceSpan>, MutableSet<ParseMatch<*>>>()
    private val matchesByStart = mutableMapOf<Pair<Symbol<*>, SourcePosition>, MutableList<ParseMatch<*>>>()
    private val matchesByEnd = mutableMapOf<Pair<Symbol<*>, SourcePosition>, MutableList<ParseMatch<*>>>()
    
    fun matches(symbol: Symbol<*>, span: SourceSpan): List<ParseMatch<*>> {
        return matches[symbol to span]?.toList().orEmpty()
    }
    
    fun matchesByStart(symbol: Symbol<*>, start: SourcePosition): List<ParseMatch<*>> {
        return matchesByStart[symbol to start]?.toList().orEmpty()
    }
    
    fun matchesByEnd(symbol: Symbol<*>, end: SourcePosition): List<ParseMatch<*>> {
        return matchesByEnd[symbol to end]?.toList().orEmpty()
    }
    
    fun seedLeafRules() {
        grammar.rules.forEach { (symbol, rules) ->
            rules.asSequence().filterIsInstance<GrammarRule.LeafRule<*>>().forEach { rule ->
                rule.seed(symbol)
            }
        }
    }
    
    fun seed(match: ParseMatch<*>): Boolean {
        val span = match.span
        val symbol = match.symbol
        if (!matches.getOrPut(symbol to span) { mutableSetOf() }.add(match)) return false
        matchesByStart.getOrPut(symbol to span.start) { mutableListOf() } += match
        matchesByEnd.getOrPut(symbol to span.end) { mutableListOf() } += match
        grammar.dependencyGraph[symbol]?.forEach { parent ->
            updateQueue += parent
            updateMap.getOrPut(parent) { mutableListOf() } += match
        }
        return true
    }
    
    private fun GrammarRule.LeafRule<*>.seed(symbol: Symbol<*>) = when (this) {
        is GrammarRule.MatchFixed<*> -> seedFixed(symbol, this)
        is GrammarRule.MatchRegex<*> -> seedRegex(symbol, this)
        is GrammarRule.MatchLineBreak<*> -> seed<LineBreakFragment>(symbol, this)
        is GrammarRule.MatchCharLiteral<*> -> seed<CharLiteralFragment>(symbol, this)
        is GrammarRule.MatchStringLiteral<*> -> seed<StringLiteralFragment>(symbol, this)
        is GrammarRule.MatchFormattedStringStart<*> -> seed<FormattedStringStartFragment>(symbol, this)
        is GrammarRule.MatchFormattedStringText<*> -> seed<FormattedStringTextFragment>(symbol, this)
        is GrammarRule.MatchFormattedExpressionStart<*> -> seed<FormattedExpressionStartFragment>(symbol, this)
        is GrammarRule.MatchFormattedExpressionEnd<*> -> seed<FormattedExpressionEndFragment>(symbol, this)
        is GrammarRule.MatchFormattedStringEnd<*> -> seed<FormattedStringEndFragment>(symbol, this)
    }
    
    private inline fun seedFixed(target: Symbol<*>, rule: GrammarRule.MatchFixed<*>) {
        source.positions.forEach { position ->
            val span = matchFixed(rule, position) ?: return@forEach
            seed(ParseMatch(target, ParseMatchType.Exact, listOf(span), rule))
        }
    }
    
    private inline fun seedRegex(target: Symbol<*>, rule: GrammarRule.MatchRegex<*>) {
        source.positions.forEach { position ->
            val span = matchRegex(rule, position) ?: return@forEach
            seed(ParseMatch(target, ParseMatchType.Exact, listOf(span), rule))
        }
    }
    
    private inline fun <reified F> seed(target: Symbol<*>, rule: GrammarRule.LeafRule<*>) {
        source.forEachIndexed { index, fragment ->
            if (fragment !is F) return@forEachIndexed
            val span = SourceSpan(SourcePosition(index), SourcePosition(index) + 1)
            seed(ParseMatch(target, ParseMatchType.Exact, listOf(span), rule))
        }
    }
    
    private fun matchFixed(rule: GrammarRule.MatchFixed<*>, start: SourcePosition): SourceSpan? {
        val string = rule.string
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
        return SourceSpan(start, start + seen.size)
    }
    
    private fun matchRegex(rule: GrammarRule.MatchRegex<*>, start: SourcePosition): SourceSpan? {
        val automaton = rule.automaton
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
        check(rule.regex.matches(result))
        return SourceSpan(start, start + longestMatch)
    }
    
    fun grow() {
        while (true) {
            val symbol = updateQueue.poll() ?: break
            updateMap.remove(symbol)?.forEach { match ->
                grammar.rules[symbol]?.forEach { rule ->
                    if (rule !is GrammarRule.MatchSymbols) return@forEach
                    
                    fun collectLeft(index: Int, end: SourcePosition): List<List<SourceSpan>> {
                        if (index < 0) return listOf(emptyList())
                        val component = rule.components[index]
                        val matches = matchesByEnd[component to end] ?: return emptyList()
                        val results = mutableListOf<List<SourceSpan>>()
                        matches.forEach { leftMatch ->
                            collectLeft(index - 1, leftMatch.span.start).forEach { left ->
                                results += left + listOf(leftMatch.span)
                            }
                        }
                        return results
                    }
                    
                    fun collectRight(index: Int, start: SourcePosition): List<List<SourceSpan>> {
                        if (index >= rule.components.size) return listOf(emptyList())
                        val component = rule.components[index]
                        val matches = matchesByStart[component to start] ?: return emptyList()
                        val results = mutableListOf<List<SourceSpan>>()
                        matches.forEach { rightMatch ->
                            collectRight(index + 1, rightMatch.span.end).forEach { right ->
                                results += listOf(rightMatch.span) + right
                            }
                        }
                        return results
                    }
                    
                    fun collect(index: Int) {
                        val lefts = collectLeft(index - 1, match.span.start)
                        val rights = collectRight(index + 1, match.span.end)
                        lefts.forEach { left ->
                            rights.forEach { right ->
                                val segments = left + listOf(match.span) + right
                                val buildable = segments.withIndex().all { (segment, span) ->
                                    matches(rule.components[segment], span).any { it.matchType.isBuildable() }
                                }
                                val matchType = if (buildable) ParseMatchType.Derived else ParseMatchType.Synthetic
                                seed(ParseMatch(symbol, matchType, segments, rule))
                            }
                        }
                    }
                    
                    rule.components.forEachIndexed { index, component ->
                        if (component == match.symbol) collect(index)
                    }
                }
            }
        }
    }
    
    fun <N> build(symbol: Symbol<N>): ParseContextBuildResult<N> = build(symbol, source.span)
    
    @Suppress("UNCHECKED_CAST")
    fun <N> build(symbol: Symbol<N>, span: SourceSpan): ParseContextBuildResult<N> {
        val errors = mutableSetOf<GrammarRuleFactoryError>()
        build(symbol, span, errors)?.let { node ->
            return ParseContextBuildSuccess(node as N)
        }
        return ParseContextBuildFailure(errors)
    }
    
    private fun build(symbol: Symbol<*>, span: SourceSpan, errors: MutableSet<GrammarRuleFactoryError>): Any? {
        val results = matches[symbol to span].orEmpty().mapNotNull { build(it, errors) }
        if (results.isEmpty()) return null
        check(results.size == 1) { "Ambiguous grammar" }
        return results.single()
    }
    
    private fun build(match: ParseMatch<*>, errors: MutableSet<GrammarRuleFactoryError>): Any? {
        if (!match.matchType.isBuildable()) return null
        val rule = checkNotNull(match.grammarRule)
        val first = source[match.segments.first().start]
        return try {
            when (rule) {
                is GrammarRule.MatchFixed<*> -> {
                    rule.factory(
                        PlainFragment(
                            first.line,
                            first.column,
                            match.segments.first().asSequence()
                                .map { (source[it] as PlainFragment).text }
                                .joinToString(separator = "") { it },
                        ),
                    )
                }
                is GrammarRule.MatchRegex<*> -> {
                    rule.factory(
                        PlainFragment(
                            first.line,
                            first.column,
                            match.segments.first().asSequence()
                                .map { (source[it] as PlainFragment).text }
                                .joinToString(separator = "") { it },
                        ),
                    )
                }
                is GrammarRule.MatchLineBreak<*> -> {
                    rule.factory(first as LineBreakFragment)
                }
                is GrammarRule.MatchCharLiteral<*> -> {
                    rule.factory(first as CharLiteralFragment)
                }
                is GrammarRule.MatchStringLiteral<*> -> {
                    rule.factory(first as StringLiteralFragment)
                }
                is GrammarRule.MatchFormattedStringStart<*> -> {
                    rule.factory(first as FormattedStringStartFragment)
                }
                is GrammarRule.MatchFormattedStringText<*> -> {
                    rule.factory(first as FormattedStringTextFragment)
                }
                is GrammarRule.MatchFormattedExpressionStart<*> -> {
                    rule.factory(first as FormattedExpressionStartFragment)
                }
                is GrammarRule.MatchFormattedExpressionEnd<*> -> {
                    rule.factory(first as FormattedExpressionEndFragment)
                }
                is GrammarRule.MatchFormattedStringEnd<*> -> {
                    rule.factory(first as FormattedStringEndFragment)
                }
                is GrammarRule.MatchSymbols<*> -> {
                    rule.factory(
                        rule.components.zip(match.segments).map { (component, segment) ->
                            build(component, segment, errors) ?: return null
                        },
                    )
                }
            }
        } catch (e: GrammarRuleFactoryException) {
            errors += GrammarRuleFactoryError(
                symbol = match.symbol,
                span = match.span,
                message = e.message,
            )
            null
        }
    }
    
    fun repair(root: Expectation, strategy: RepairStrategy) {
        val queue = ArrayDeque<Expectation>()
        val seen = mutableSetOf<Expectation>()
        
        queue += root
        while (queue.isNotEmpty()) {
            val expectation = queue.poll()
            if (!seen.add(expectation)) continue
            
            val existing = matches[expectation.symbol to expectation.span].orEmpty()
            if (existing.any { it.matchType.isBuildable() }) continue
            
            strategy.childExpectations(this, expectation).forEach {
                if (seed(it.toParseMatch())) {
                    queue += it
                }
            }
            
            grow()
        }
    }
    
    fun best(symbol: Symbol<*>, strategy: DiagnosticScoringStrategy) = best(symbol, source.span, strategy)
    
    fun best(symbol: Symbol<*>, span: SourceSpan, strategy: DiagnosticScoringStrategy): DiagnosticTree? {
        val memo = mutableMapOf<Expectation, DiagnosticTree?>()
        val visiting = mutableSetOf<Expectation>()
        
        fun best(symbol: Symbol<*>, span: SourceSpan): DiagnosticTree? {
            
            fun best(match: ParseMatch<*>): DiagnosticTree? {
                if (match.matchType == ParseMatchType.Expected) {
                    val score = strategy.expectedScore(match.symbol, match.span, source) ?: return null
                    return DiagnosticTree(match, emptyList(), score)
                }
                
                val rule = match.grammarRule ?: return null
                return when (rule) {
                    is GrammarRule.LeafRule<*> -> DiagnosticTree(match, emptyList(), DiagnosticScore.Node)
                    is GrammarRule.MatchSymbols<*> -> {
                        val children = rule.components.zip(match.segments).map { (component, segment) ->
                            best(component, segment) ?: return null
                        }
                        val score = children.fold(DiagnosticScore.Node + strategy.rulePenalty(match, rule, source)) { acc, child ->
                            acc + child.score.nested()
                        }
                        DiagnosticTree(match, children, score)
                    }
                }
            }
            
            val key = Expectation(symbol, span)
            if (key in memo) return memo[key]
            if (!visiting.add(key)) return null
            
            val result = matches[symbol to span].orEmpty().mapNotNull { best(it) }.minOrNull()
            visiting -= key
            memo[key] = result
            return result
        }
        
        return best(symbol, span)
    }
}
