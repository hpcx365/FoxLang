@file:Suppress("NOTHING_TO_INLINE")

package pers.hpcx.foxlang.frontend

import pers.hpcx.foxlang.utils.UniqueQueue

class Parser<N>(
    val grammar: Grammar,
    val start: Symbol<N>,
) {
    
    fun parse(src: String): N? {
        return analyze(src).value
    }
    
    fun analyze(src: String): ParseAnalysis<N> {
        val source = Source(src)
        val chart = ParseChart(grammar, source)
        return ParseAnalysis(
            grammar = grammar,
            start = start,
            source = source,
            exactChart = chart,
            value = chart.build(start),
        )
    }
}

data class ParseAnalysis<N>(
    val grammar: Grammar,
    val start: Symbol<N>,
    val source: Source,
    val exactChart: ParseChart,
    val value: N?,
)

data class RuleFactoryFailure(
    val symbol: Symbol<*>,
    val span: SourceSpan,
    val message: String,
)

data class ParseMatch<N>(
    val symbol: Symbol<N>,
    val rule: Rule<*>?,
    val segments: List<SourceSpan>,
    val origin: MatchOrigin = MatchOrigin.Exact,
) {
    
    init {
        require(segments.isNotEmpty()) { "Match segments must not be empty" }
    }
    
    val span: SourceSpan get() = SourceSpan(segments.first().begin, segments.last().end)
    
    fun isExactTree(): Boolean = when (origin) {
        MatchOrigin.Exact -> true
        is MatchOrigin.Expected -> false
        is MatchOrigin.Derived -> !origin.fromSynthetic
    }
    
    fun hasSyntheticOrigin(): Boolean = when (origin) {
        MatchOrigin.Exact -> false
        is MatchOrigin.Expected -> true
        is MatchOrigin.Derived -> origin.fromSynthetic
    }
}

sealed interface MatchOrigin {
    
    data object Exact : MatchOrigin
    
    data class Expected(
        val reason: String,
        val cost: Int,
        val children: List<Expectation> = emptyList(),
    ) : MatchOrigin
    
    data class Derived(
        val fromSynthetic: Boolean,
    ) : MatchOrigin
}

class ParseChart(
    val grammar: Grammar,
    val source: Source,
    seedLeaves: Boolean = true,
) {
    
    private val updateQueue = UniqueQueue<Symbol<*>>()
    private val updateMap = mutableMapOf<Symbol<*>, MutableList<ParseMatch<*>>>()
    private val matches = mutableMapOf<Pair<Symbol<*>, SourceSpan>, MutableSet<ParseMatch<*>>>()
    private val matchesByBegin = mutableMapOf<Pair<Symbol<*>, SourcePosition>, MutableList<ParseMatch<*>>>()
    private val matchesByEnd = mutableMapOf<Pair<Symbol<*>, SourcePosition>, MutableList<ParseMatch<*>>>()
    private val ruleFactoryFailures = mutableSetOf<RuleFactoryFailure>()
    
    init {
        if (seedLeaves) seedLeafRules()
    }
    
    fun forkForDiagnostics(): ParseChart {
        val result = ParseChart(grammar, source, seedLeaves = false)
        allMatches().forEach { result.seed(it, grow = false) }
        result.ruleFactoryFailures += ruleFactoryFailures
        result.grow()
        return result
    }
    
    fun allMatches(): List<ParseMatch<*>> {
        return matches.values.flatten()
    }
    
    fun matches(symbol: Symbol<*>, span: SourceSpan): List<ParseMatch<*>> {
        return matches[symbol to span]?.toList().orEmpty()
    }
    
    fun matchesByBegin(symbol: Symbol<*>, begin: SourcePosition): List<ParseMatch<*>> {
        return matchesByBegin[symbol to begin]?.toList().orEmpty()
    }
    
    fun matchesByEnd(symbol: Symbol<*>, end: SourcePosition): List<ParseMatch<*>> {
        return matchesByEnd[symbol to end]?.toList().orEmpty()
    }
    
    fun ruleFactoryFailures(): List<RuleFactoryFailure> {
        return ruleFactoryFailures.toList()
    }

    fun seed(match: ParseMatch<*>, grow: Boolean = true): Boolean {
        val span = match.span
        val symbol = match.symbol
        if (!matches.getOrPut(symbol to span) { mutableSetOf() }.add(match)) return false
        matchesByBegin.getOrPut(symbol to span.begin) { mutableListOf() } += match
        matchesByEnd.getOrPut(symbol to span.end) { mutableListOf() } += match
        grammar.dependencyGraph[symbol]?.forEach { parent ->
            updateQueue += parent
            updateMap.getOrPut(parent) { mutableListOf() } += match
        }
        if (grow) grow()
        return true
    }
    
    private fun seedLeafRules() {
        grammar.rules.forEach { (target, rules) ->
            rules.asSequence().filterIsInstance<LeafRule<*>>().forEach { rule ->
                when (rule) {
                    is FixedRule<*> -> seedFixed(target, rule)
                    is RegexRule<*> -> seedRegex(target, rule)
                    is LineBreakRule<*> -> seed<LineBreakFragment>(target, rule)
                    is CharLiteralRule<*> -> seed<CharLiteralFragment>(target, rule)
                    is StringLiteralRule<*> -> seed<StringLiteralFragment>(target, rule)
                    is FormattedStringStartRule<*> -> seed<FormattedStringStartFragment>(target, rule)
                    is FormattedStringTextRule<*> -> seed<FormattedStringTextFragment>(target, rule)
                    is FormattedExpressionStartRule<*> -> seed<FormattedExpressionStartFragment>(target, rule)
                    is FormattedExpressionEndRule<*> -> seed<FormattedExpressionEndFragment>(target, rule)
                    is FormattedStringEndRule<*> -> seed<FormattedStringEndFragment>(target, rule)
                }
            }
        }
        grow()
    }
    
    fun grow() {
        while (true) {
            val symbol = updateQueue.poll() ?: break
            updateMap.remove(symbol)?.forEach { match ->
                grammar.rules[symbol]?.forEach { rule ->
                    if (rule !is NonLeafRule) return@forEach
                    
                    fun collectLeft(index: Int, end: SourcePosition): List<List<SourceSpan>> {
                        if (index < 0) return listOf(emptyList())
                        val component = rule.components[index]
                        val matches = matchesByEnd[component to end] ?: return emptyList()
                        val results = mutableListOf<List<SourceSpan>>()
                        matches.forEach { leftMatch ->
                            collectLeft(index - 1, leftMatch.span.begin).forEach { left ->
                                results += left + listOf(leftMatch.span)
                            }
                        }
                        return results
                    }
                    
                    fun collectRight(index: Int, begin: SourcePosition): List<List<SourceSpan>> {
                        if (index >= rule.components.size) return listOf(emptyList())
                        val component = rule.components[index]
                        val matches = matchesByBegin[component to begin] ?: return emptyList()
                        val results = mutableListOf<List<SourceSpan>>()
                        matches.forEach { rightMatch ->
                            collectRight(index + 1, rightMatch.span.end).forEach { right ->
                                results += listOf(rightMatch.span) + right
                            }
                        }
                        return results
                    }
                    
                    fun collect(index: Int) {
                        val lefts = collectLeft(index - 1, match.span.begin)
                        val rights = collectRight(index + 1, match.span.end)
                        lefts.forEach { left ->
                            rights.forEach { right ->
                                val segments = left + listOf(match.span) + right
                                val origin = MatchOrigin.Derived(
                                    segments.withIndex().any { (segmentIndex, segment) ->
                                        matches(rule.components[segmentIndex], segment).any { it.hasSyntheticOrigin() }
                                    },
                                )
                                seed(ParseMatch(symbol, rule, segments, origin), grow = false)
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
    fun <N> build(symbol: Symbol<N>): N? {
        return build(symbol, source.span) as N?
    }
    
    private fun build(symbol: Symbol<*>, span: SourceSpan): Any? {
        val candidates = matches[symbol to span] ?: return null
        val results = candidates
            .filter { it.isExactTree() }
            .mapNotNull { build(it) }
        if (results.isEmpty()) return null
        check(results.size == 1) { "Ambiguous grammar" }
        return results.single()
    }
    
    private fun build(match: ParseMatch<*>): Any? {
        if (match.origin is MatchOrigin.Expected) return null
        val first = source[match.segments.first().begin]
        return try {
            when (val rule = match.rule) {
                null -> null
                is FixedRule<*> -> rule.factory(
                    PlainFragment(
                        first.line,
                        first.column,
                        match.segments.first().asSequence()
                            .map { (source[it] as PlainFragment).text }
                            .joinToString(separator = "") { it },
                    ),
                )
                is RegexRule<*> -> rule.factory(
                    PlainFragment(
                        first.line,
                        first.column,
                        match.segments.first().asSequence()
                            .map { (source[it] as PlainFragment).text }
                            .joinToString(separator = "") { it },
                    ),
                )
                is LineBreakRule<*> -> rule.factory(first as LineBreakFragment)
                is CharLiteralRule<*> -> rule.factory(first as CharLiteralFragment)
                is StringLiteralRule<*> -> rule.factory(first as StringLiteralFragment)
                is FormattedStringStartRule<*> -> rule.factory(first as FormattedStringStartFragment)
                is FormattedStringTextRule<*> -> rule.factory(first as FormattedStringTextFragment)
                is FormattedExpressionStartRule<*> -> rule.factory(first as FormattedExpressionStartFragment)
                is FormattedExpressionEndRule<*> -> rule.factory(first as FormattedExpressionEndFragment)
                is FormattedStringEndRule<*> -> rule.factory(first as FormattedStringEndFragment)
                is NonLeafRule<*> -> rule.factory(
                    rule.components.zip(match.segments).map { (component, segment) ->
                        build(component, segment) ?: return null
                    },
                )
            }
        } catch (e: RuleFactoryException) {
            ruleFactoryFailures += RuleFactoryFailure(
                symbol = match.symbol,
                span = match.span,
                message = e.message ?: "Rule factory rejected match",
            )
            null
        }
    }
    
    private inline fun seedFixed(target: Symbol<*>, rule: FixedRule<*>) {
        source.positions.forEach { position ->
            val span = matchFixed(rule, position) ?: return@forEach
            seed(ParseMatch(target, rule, listOf(span)), grow = false)
        }
    }
    
    private inline fun seedRegex(target: Symbol<*>, rule: RegexRule<*>) {
        source.positions.forEach { position ->
            val span = matchRegex(rule, position) ?: return@forEach
            seed(ParseMatch(target, rule, listOf(span)), grow = false)
        }
    }
    
    private inline fun <reified F> seed(target: Symbol<*>, rule: LeafRule<*>) {
        source.forEachIndexed { index, fragment ->
            if (fragment !is F) return@forEachIndexed
            val span = SourceSpan(SourcePosition(index), SourcePosition(index) + 1)
            seed(ParseMatch(target, rule, listOf(span)), grow = false)
        }
    }
    
    private fun matchFixed(rule: FixedRule<*>, begin: SourcePosition): SourceSpan? {
        val string = rule.string
        var stringIndex = 0
        val seen = mutableListOf<PlainFragment>()
        
        loop@ while (true) {
            val currFrag = source.getOrNull(begin + seen.size) ?: break
            if (currFrag !is PlainFragment) break
            seen.lastOrNull()?.let { prevFrag ->
                if (prevFrag.line != currFrag.line || prevFrag.column + prevFrag.text.length != currFrag.column) {
                    break
                }
            }
            currFrag.text.forEach { char ->
                if (stringIndex >= string.length || string[stringIndex] != char) break@loop
                stringIndex++
            }
            seen += currFrag
        }
        if (seen.isEmpty() || stringIndex < string.length) return null
        
        val result = buildString { seen.forEach { append(it.text) } }
        check(result == string)
        return SourceSpan(begin, begin + seen.size)
    }
    
    private fun matchRegex(rule: RegexRule<*>, begin: SourcePosition): SourceSpan? {
        val automaton = rule.automaton
        var automatonState = automaton.initialState
        val seen = mutableListOf<PlainFragment>()
        var longestMatch = 0
        
        loop@ while (true) {
            val currFrag = source.getOrNull(begin + seen.size) ?: break
            if (currFrag !is PlainFragment) break
            seen.lastOrNull()?.let { prevFrag ->
                if (prevFrag.line != currFrag.line || prevFrag.column + prevFrag.text.length != currFrag.column) {
                    break
                }
            }
            currFrag.text.forEach { char ->
                val nextState = automaton.step(automatonState, char)
                if (nextState < 0) break@loop
                automatonState = nextState
            }
            seen += currFrag
            if (automaton.isAccept(automatonState)) longestMatch = seen.size
        }
        if (seen.isEmpty() || longestMatch == 0) return null
        
        val result = buildString { seen.subList(0, longestMatch).forEach { append(it.text) } }
        check(rule.regex.matches(result))
        return SourceSpan(begin, begin + longestMatch)
    }
}
