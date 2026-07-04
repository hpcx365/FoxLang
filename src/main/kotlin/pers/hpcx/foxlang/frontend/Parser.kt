package pers.hpcx.foxlang.frontend

import pers.hpcx.foxlang.utils.UniqueQueue

class Parser<N>(
    val grammar: Grammar,
    val start: Symbol<N>,
) {
    
    fun parse(source: String): N? {
        val context = ParseContext(grammar, source.toFragments())
        context.init()
        return context.build(start)
    }
}

private data class Match<N>(
    val symbol: Symbol<N>,
    val rule: Rule<*>,
    val segments: List<SourceSpan>,
) {
    
    val span: SourceSpan get() = SourceSpan(segments.first().begin, segments.last().end)
}

private class ParseContext(
    private val grammar: Grammar,
    private val fragments: List<SourceFragment>,
) {
    
    private val updateQueue = UniqueQueue<Symbol<*>>()
    private val updateMap = mutableMapOf<Symbol<*>, MutableList<Match<*>>>()
    private val matches = mutableMapOf<Pair<Symbol<*>, SourceSpan>, MutableSet<Match<*>>>()
    private val matchesByBegin = mutableMapOf<Pair<Symbol<*>, SourcePosition>, MutableList<Match<*>>>()
    private val matchesByEnd = mutableMapOf<Pair<Symbol<*>, SourcePosition>, MutableList<Match<*>>>()
    
    @Suppress("UNCHECKED_CAST")
    fun <N> build(symbol: Symbol<N>): N? {
        return build(symbol, SourceSpan(SourcePosition(0), SourcePosition(fragments.size))) as N?
    }
    
    private fun build(symbol: Symbol<*>, span: SourceSpan): Any? {
        val candidates = matches[symbol to span] ?: return null
        val results = candidates.mapNotNull { build(it) }
        if (results.isEmpty()) return null
        check(results.size == 1) { "Ambiguous grammar" }
        return results.single()
    }
    
    private fun build(match: Match<*>): Any? {
        val first = fragments[match.segments.first().begin.fragIndex]
        return try {
            when (val rule = match.rule) {
                is FixedRule<*> -> rule.factory(
                    PlainFragment(
                        first.line,
                        first.column,
                        match.segments.first().asSequence()
                            .map { (fragments[it.fragIndex] as PlainFragment).text }
                            .joinToString(separator = "") { it },
                    ),
                )
                is RegexRule<*> -> rule.factory(
                    PlainFragment(
                        first.line,
                        first.column,
                        match.segments.first().asSequence()
                            .map { (fragments[it.fragIndex] as PlainFragment).text }
                            .joinToString(separator = "") { it },
                    ),
                )
                is LineBreakRule<*> -> rule.factory(first as LineBreakFragment)
                is CharLiteralRule<*> -> rule.factory(first as CharLiteralFragment)
                is StringLiteralRule<*> -> rule.factory(first as StringLiteralFragment)
                is NonLeafRule<*> -> rule.factory(
                    rule.components.zip(match.segments).map { (component, segment) ->
                        build(component, segment) ?: return null
                    },
                )
            }
        } catch (_: RuleFactoryException) {
            null
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun init() {
        grammar.rules.forEach { (target, rules) ->
            rules.asSequence().filterIsInstance<LeafRule<*>>().forEach { rule ->
                when (rule) {
                    is FixedRule<*> -> seedFixed(target, rule)
                    is RegexRule<*> -> seedRegex(target, rule)
                    is LineBreakRule<*> -> seedLineBreak(target, rule)
                    is CharLiteralRule<*> -> seedCharLiteral(target, rule)
                    is StringLiteralRule<*> -> seedStringLiteral(target, rule)
                }
            }
        }
        
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
                        matches.forEach { match ->
                            collectLeft(index - 1, match.span.begin).forEach { left ->
                                results += left + listOf(match.span)
                            }
                        }
                        return results
                    }
                    
                    fun collectRight(index: Int, begin: SourcePosition): List<List<SourceSpan>> {
                        if (index >= rule.components.size) return listOf(emptyList())
                        val component = rule.components[index]
                        val matches = matchesByBegin[component to begin] ?: return emptyList()
                        val results = mutableListOf<List<SourceSpan>>()
                        matches.forEach { match ->
                            collectRight(index + 1, match.span.end).forEach { right ->
                                results += listOf(match.span) + right
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
                                seedMatch(Match(symbol, rule, segments))
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
    
    private fun seedFixed(target: Symbol<*>, rule: FixedRule<*>) {
        fragments.indices.forEach { index ->
            val span = matchFixed(rule, SourcePosition(index)) ?: return@forEach
            seedMatch(Match(target, rule, listOf(span)))
        }
    }
    
    private fun seedRegex(target: Symbol<*>, rule: RegexRule<*>) {
        fragments.indices.forEach { index ->
            val span = matchRegex(rule, SourcePosition(index)) ?: return@forEach
            seedMatch(Match(target, rule, listOf(span)))
        }
    }
    
    private fun seedLineBreak(target: Symbol<*>, rule: LineBreakRule<*>) {
        fragments.forEachIndexed { index, fragment ->
            if (fragment !is LineBreakFragment) return@forEachIndexed
            val span = SourceSpan(SourcePosition(index), SourcePosition(index) + 1)
            seedMatch(Match(target, rule, listOf(span)))
        }
    }
    
    private fun seedCharLiteral(target: Symbol<*>, rule: CharLiteralRule<*>) {
        fragments.forEachIndexed { index, fragment ->
            if (fragment !is CharLiteralFragment) return@forEachIndexed
            val span = SourceSpan(SourcePosition(index), SourcePosition(index) + 1)
            seedMatch(Match(target, rule, listOf(span)))
        }
    }
    
    private fun seedStringLiteral(target: Symbol<*>, rule: StringLiteralRule<*>) {
        fragments.forEachIndexed { index, fragment ->
            if (fragment !is StringLiteralFragment) return@forEachIndexed
            val span = SourceSpan(SourcePosition(index), SourcePosition(index) + 1)
            seedMatch(Match(target, rule, listOf(span)))
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun seedMatch(match: Match<*>) {
        val span = match.span
        val symbol = match.symbol
        if (!matches.getOrPut(symbol to span) { mutableSetOf() }.add(match)) return
        matchesByBegin.getOrPut(symbol to span.begin) { mutableListOf() } += match
        matchesByEnd.getOrPut(symbol to span.end) { mutableListOf() } += match
        grammar.dependencyGraph[symbol]?.forEach { parent ->
            updateQueue += parent
            updateMap.getOrPut(parent) { mutableListOf() } += match
        }
    }
    
    private fun matchFixed(rule: FixedRule<*>, begin: SourcePosition): SourceSpan? {
        val string = rule.string
        var stringIndex = 0
        val seen = mutableListOf<PlainFragment>()
        
        loop@ while (true) {
            val currFrag = fragments.getOrNull((begin + seen.size).fragIndex) ?: break
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
            val currFrag = fragments.getOrNull((begin + seen.size).fragIndex) ?: break
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
