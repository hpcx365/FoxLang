package pers.hpcx.foxlang.frontend.common

import pers.hpcx.foxlang.utils.Opt
import pers.hpcx.foxlang.utils.UniqueQueue
import pers.hpcx.foxlang.utils.none
import pers.hpcx.foxlang.utils.some
import java.util.*

class Parser<N>(
    val grammar: Grammar,
    val start: GrammarSymbol<N>,
) {
    
    fun parse(source: Source<*>): N? {
        return (analyze(source).buildResult as? ParseContextBuildSuccess)?.node
    }
    
    fun analyze(source: Source<*>): ParseAnalysis<N> {
        val context = ParseContext(grammar, source)
        context.init()
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
    val start: GrammarSymbol<N>,
    val source: Source<*>,
    val context: ParseContext,
    val buildResult: ParseContextBuildResult<N>,
)

data class ParseMatch<N>(
    val symbol: GrammarSymbol<N>,
    val matchType: ParseMatchType,
    val segments: List<SourceSpan>,
    val grammarRule: GrammarRule<N>?,
) {
    
    init {
        require(segments.isNotEmpty()) { "Match segments must not be empty" }
        segments.zipWithNext().forEach {
            require(it.first.end == it.second.start) { "Match segments must be contiguous" }
        }
        if (matchType.isBuildable()) {
            requireNotNull(grammarRule) { "Grammar rule must not be null for buildable match" }
        }
        if (matchType == ParseMatchType.Terminal) {
            require(grammarRule is GrammarRule.MatchTerminal<*, *>) { "Grammar rule must be a terminal rule for terminal match" }
        }
        when (grammarRule) {
            null -> {}
            is GrammarRule.MatchTerminal<*, *> -> require(segments.size == 1) {
                "Terminal rule match must have exactly one segment"
            }
            is GrammarRule.MatchSymbols -> require(segments.size == grammarRule.components.size) {
                "Match symbols rule match must have exactly ${grammarRule.components.size} segments"
            }
        }
    }
    
    val span: SourceSpan get() = SourceSpan(segments.first().start, segments.last().end)
}

enum class ParseMatchType {
    
    Terminal,
    Derived,
    Expected,
    Synthetic;
    
    fun isBuildable(): Boolean = when (this) {
        Terminal -> true
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
    val symbol: GrammarSymbol<*>,
    val span: SourceSpan,
    val message: String,
)

class ParseContext(val grammar: Grammar, val source: Source<*>) {
    
    private val updateQueue = UniqueQueue<GrammarSymbol<*>>()
    private val updateMap = mutableMapOf<GrammarSymbol<*>, MutableList<ParseMatch<*>>>()
    private val matches = mutableMapOf<Pair<GrammarSymbol<*>, SourceSpan>, MutableSet<ParseMatch<*>>>()
    private val matchesByStart = mutableMapOf<Pair<GrammarSymbol<*>, SourcePosition>, MutableList<ParseMatch<*>>>()
    private val matchesByEnd = mutableMapOf<Pair<GrammarSymbol<*>, SourcePosition>, MutableList<ParseMatch<*>>>()
    private val terminalNodes = mutableMapOf<Pair<GrammarRule.MatchTerminal<*, *>, SourceSpan>, Any?>()
    
    fun matches(symbol: GrammarSymbol<*>, span: SourceSpan): List<ParseMatch<*>> {
        return matches[symbol to span]?.toList().orEmpty()
    }
    
    fun matchesByStart(symbol: GrammarSymbol<*>, start: SourcePosition): List<ParseMatch<*>> {
        return matchesByStart[symbol to start]?.toList().orEmpty()
    }
    
    fun matchesByEnd(symbol: GrammarSymbol<*>, end: SourcePosition): List<ParseMatch<*>> {
        return matchesByEnd[symbol to end]?.toList().orEmpty()
    }
    
    @Suppress("UNCHECKED_CAST")
    fun init() {
        val source = source as Source<Any?>
        grammar.rules.forEach { (symbol, rules) ->
            rules.asSequence().filterIsInstance<GrammarRule.MatchTerminal<*, *>>().forEach { rule ->
                val matcher = rule.matcher as TerminalMatcher<*, Any?>
                source.positions.forEach { position ->
                    val match = matcher.match(source, position) ?: return@forEach
                    val key = rule to match.span
                    check(key !in terminalNodes)
                    terminalNodes[key] = match.node
                    seed(
                        ParseMatch(
                            symbol = symbol as GrammarSymbol<Any?>,
                            matchType = ParseMatchType.Terminal,
                            segments = listOf(match.span),
                            grammarRule = rule as GrammarRule<Any?>,
                        ),
                    )
                }
            }
        }
    }
    
    private fun seed(match: ParseMatch<*>): Boolean {
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
    
    fun grow() {
        while (true) {
            val symbol = updateQueue.poll() ?: return
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
                                @Suppress("UNCHECKED_CAST")
                                seed(ParseMatch(symbol as GrammarSymbol<Any?>, matchType, segments, rule as GrammarRule<Any?>))
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
    
    @Suppress("UNCHECKED_CAST")
    fun <N> build(symbol: GrammarSymbol<N>, span: SourceSpan = source.span): ParseContextBuildResult<N> {
        val errors = mutableSetOf<GrammarRuleFactoryError>()
        build(symbol, span, errors).ifPresent { node ->
            return ParseContextBuildSuccess(node)
        }
        return ParseContextBuildFailure(errors)
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <N> build(symbol: GrammarSymbol<N>, span: SourceSpan, errors: MutableSet<GrammarRuleFactoryError>): Opt<N> {
        val candidates = (matches[symbol to span] ?: return none())
            .asSequence()
            .map { build(it, errors) }
            .filter { it.isPresent() }
            .map { it.value() as N }
            .toList()
        if (candidates.isEmpty()) return none()
        check(candidates.size == 1) { "Ambiguous grammar" }
        return some(candidates.single())
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <N> build(match: ParseMatch<N>, errors: MutableSet<GrammarRuleFactoryError>): Opt<N> {
        if (!match.matchType.isBuildable()) return none()
        return when (val rule = checkNotNull(match.grammarRule)) {
            is GrammarRule.MatchTerminal<*, *> -> {
                some(terminalNodes.getValue(rule to match.span) as N)
            }
            is GrammarRule.MatchSymbols -> {
                when (
                    val result = rule.factory(
                        rule.components.zip(match.segments).map { (component, segment) ->
                            val node = build(component, segment, errors)
                            if (node.isEmpty()) return none()
                            node.value() as N
                        },
                    )
                ) {
                    is GrammarRuleFactorySuccess -> some(result.node)
                    is GrammarRuleFactoryFailure -> {
                        errors += GrammarRuleFactoryError(
                            symbol = match.symbol,
                            span = match.span,
                            message = result.message,
                        )
                        none()
                    }
                }
            }
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
                val match = ParseMatch(
                    symbol = it.symbol,
                    matchType = ParseMatchType.Expected,
                    segments = listOf(it.span),
                    grammarRule = null,
                )
                if (seed(match)) queue += it
            }
            
            grow()
        }
    }
    
    fun best(symbol: GrammarSymbol<*>, strategy: DiagnosticScoringStrategy) = best(symbol, source.span, strategy)
    
    fun best(symbol: GrammarSymbol<*>, span: SourceSpan, strategy: DiagnosticScoringStrategy): DiagnosticTree? {
        val visiting = mutableSetOf<Expectation>()
        val memo = mutableMapOf<Expectation, DiagnosticTree?>()
        
        fun best(symbol: GrammarSymbol<*>, span: SourceSpan): DiagnosticTree? {
            
            fun best(match: ParseMatch<*>): DiagnosticTree? {
                if (match.matchType == ParseMatchType.Expected) {
                    val score = strategy.expectedScore(match.symbol, match.span, source) ?: return null
                    return DiagnosticTree(match, emptyList(), score)
                }
                
                val rule = match.grammarRule ?: return null
                return when (rule) {
                    is GrammarRule.MatchTerminal<*, *> -> DiagnosticTree(match, emptyList(), DiagnosticScore.Node)
                    is GrammarRule.MatchSymbols -> {
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
