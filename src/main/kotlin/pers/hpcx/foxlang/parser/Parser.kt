package pers.hpcx.foxlang.parser

import java.util.*

class Parser<N>(val grammar: Grammar, val start: NonTerminal<N>) {
    
    fun parse(source: String): Reduction<N> {
        val context = ParseContext(grammar, source.toFragments())
        context.run()
        
        val span = SourceSpan(SourcePosition(0), SourcePosition(context.fragments.size))
        val results = context.getExactReductions(start, span)
        return when (results.size) {
            0 -> throw ParseException("Failed to parse input as $start")
            1 -> @Suppress("UNCHECKED_CAST") (results.single() as ExactReduction<N>)
            else -> throw ParseException("Ambiguous parse for $start: ${results.size} complete parses")
        }
    }
}

private class ParseContext(private val grammar: Grammar, val fragments: List<SourceFragment>) {
    
    private val sequenceAdvancers: List<SequenceAdvancer>
    private val listLikeAdvancers: List<ListLikeAdvancer>
    
    private val completeStore = mutableMapOf<NonTerminal<*>, MutableMap<SourcePosition, MutableSet<ExactReduction<*>>>>()
    private val agendaComplete = ArrayDeque<ExactReduction<*>>()
    private val agendaTask = ArrayDeque<AdvanceTask>()
    
    private val seenTasks = mutableSetOf<AdvanceTask>()
    private val waitingStates = mutableMapOf<ComponentKey, MutableList<AdvanceTask>>()
    
    init {
        val sequenceAdvancers = mutableListOf<SequenceAdvancer>()
        val listLikeAdvancers = mutableListOf<ListLikeAdvancer>()
        
        grammar.productions.forEach { (target, productions) ->
            productions.filterIsInstance<NonLeafProduction<*>>().forEach { production ->
                when (production) {
                    is SequenceProduction<*> -> sequenceAdvancers += SequenceAdvancer(target, production)
                    is ListLikeProduction<*, *> -> listLikeAdvancers += ListLikeAdvancer(target, production)
                }
            }
        }
        
        this.sequenceAdvancers = sequenceAdvancers
        this.listLikeAdvancers = listLikeAdvancers
    }
    
    fun getExactReductions(nonTerminal: NonTerminal<*>, span: SourceSpan): Set<ExactReduction<*>> {
        return completeStore[nonTerminal]
            ?.get(span.begin)
            ?.filter { it.span.end == span.end }
            ?.toSet()
            ?: emptySet()
    }
    
    @Suppress("UNCHECKED_CAST")
    fun run() {
        seedLeafReductions()
        while (agendaComplete.isNotEmpty() || agendaTask.isNotEmpty()) {
            while (agendaComplete.isNotEmpty()) {
                processComplete(agendaComplete.removeFirst())
            }
            while (agendaTask.isNotEmpty()) {
                processTask(agendaTask.removeFirst())
            }
        }
    }
    
    private fun seedLeafReductions() {
        grammar.productions.forEach { (target, productions) ->
            productions.filterIsInstance<LeafProduction<*>>().forEach { production ->
                when (production) {
                    is FixedProduction<*> -> seedFixed(target, production)
                    is RegexProduction<*> -> seedRegex(target, production)
                    is LineSeparatorProduction<*> -> seedLineSeparator(target, production)
                    is CharLiteralProduction<*> -> seedCharLiteral(target, production)
                    is StringLiteralProduction<*> -> seedStringLiteral(target, production)
                }
            }
        }
    }
    
    private fun seedFixed(target: NonTerminal<*>, production: FixedProduction<*>) {
        fragments.indices.forEach { index ->
            val match = matchFixed(production, SourcePosition(index)) ?: return@forEach
            addLeafReduction(target, match.first) { production.factory(match.second) }
        }
    }
    
    private fun seedRegex(target: NonTerminal<*>, production: RegexProduction<*>) {
        fragments.indices.forEach { index ->
            val match = matchRegex(production, SourcePosition(index)) ?: return@forEach
            addLeafReduction(target, match.first) { production.factory(match.second) }
        }
    }
    
    private fun seedLineSeparator(target: NonTerminal<*>, production: LineSeparatorProduction<*>) {
        fragments.forEachIndexed { index, fragment ->
            val lineSeparatorFragment = fragment as? LineSeparatorFragment ?: return@forEachIndexed
            val span = SourceSpan(SourcePosition(index), SourcePosition(index) + 1)
            addLeafReduction(target, span) { production.factory(lineSeparatorFragment) }
        }
    }
    
    private fun seedCharLiteral(target: NonTerminal<*>, production: CharLiteralProduction<*>) {
        fragments.forEachIndexed { index, fragment ->
            val charFragment = fragment as? CharFragment ?: return@forEachIndexed
            val span = SourceSpan(SourcePosition(index), SourcePosition(index) + 1)
            addLeafReduction(target, span) { production.factory(charFragment) }
        }
    }
    
    private fun seedStringLiteral(target: NonTerminal<*>, production: StringLiteralProduction<*>) {
        fragments.forEachIndexed { index, fragment ->
            val stringFragment = fragment as? StringFragment ?: return@forEachIndexed
            val span = SourceSpan(SourcePosition(index), SourcePosition(index) + 1)
            addLeafReduction(target, span) { production.factory(stringFragment) }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun addLeafReduction(target: NonTerminal<*>, span: SourceSpan, factory: () -> Any?) {
        val node = try {
            factory()
        } catch (_: ParseException) {
            return
        }
        addExactReduction(ExactReduction(node, span, target as NonTerminal<Any?>))
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun processComplete(reduction: ExactReduction<*>) {
        sequence {
            yieldAll(sequenceAdvancers.asSequence())
            yieldAll(listLikeAdvancers.asSequence())
        }
            .forEach { advancer ->
                val state = advancer.start(reduction) ?: return@forEach
                addOrReduceState(state, advancer as StateAdvancer<ParseState>)
            }
        
        val key = ComponentKey(reduction.nonTerminal, reduction.span.begin)
        waitingStates[key]?.forEach { task -> advance(task, reduction) }
    }
    
    private fun processTask(task: AdvanceTask) {
        task.advancer.nextNonTerminals(task.state).forEach { next ->
            completeStore[next]?.get(task.state.position).orEmpty().forEach { reduction ->
                advance(task, reduction)
            }
        }
    }
    
    private fun advance(task: AdvanceTask, reduction: ExactReduction<*>) {
        val advanced = task.advancer.advance(task.state, reduction)
        checkNotNull(advanced)
        addOrReduceState(advanced, task.advancer)
    }
    
    private fun addOrReduceState(state: ParseState, advancer: StateAdvancer<ParseState>) {
        advancer.reduce(state)?.let { reduction ->
            addExactReduction(reduction)
        }
        if (advancer.nextNonTerminals(state).isNotEmpty()) {
            addState(AdvanceTask(state, advancer))
        }
    }
    
    private fun addState(task: AdvanceTask) {
        if (!seenTasks.add(task)) return
        task.advancer.nextNonTerminals(task.state).forEach { awaited ->
            waitingStates.getOrPut(ComponentKey(awaited, task.state.position)) { mutableListOf() } += task
        }
        agendaTask += task
    }
    
    private fun addExactReduction(reduction: ExactReduction<*>) {
        val store = completeStore
            .getOrPut(reduction.nonTerminal) { mutableMapOf() }
            .getOrPut(reduction.span.begin) { mutableSetOf() }
        if (store.add(reduction)) {
            agendaComplete += reduction
        }
    }
    
    private fun matchFixed(production: FixedProduction<*>, begin: SourcePosition): Pair<SourceSpan, PlainFragment>? {
        val string = production.string
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
        
        val first = seen.first()
        val result = buildString { seen.forEach { append(it.text) } }
        check(result == string)
        return SourceSpan(begin, begin + seen.size) to PlainFragment(first.line, first.column, result)
    }
    
    private fun matchRegex(production: RegexProduction<*>, begin: SourcePosition): Pair<SourceSpan, PlainFragment>? {
        val automaton = production.automaton
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
        
        val first = seen.first()
        val result = buildString { seen.subList(0, longestMatch).forEach { append(it.text) } }
        check(production.regex.matches(result))
        return SourceSpan(begin, begin + longestMatch) to PlainFragment(first.line, first.column, result)
    }
}

private data class ComponentKey(
    val nonTerminal: NonTerminal<*>,
    val position: SourcePosition,
)

private data class AdvanceTask(
    val state: ParseState,
    val advancer: StateAdvancer<ParseState>,
)

private sealed interface ParseState {
    val start: SourcePosition
    val position: SourcePosition
}

private data class SequenceState(
    override val start: SourcePosition,
    override val position: SourcePosition,
    val matchedComponents: List<ExactReduction<*>>,
    val nextComponentIndex: Int,
) : ParseState

private data class ListLikeState(
    override val start: SourcePosition,
    override val position: SourcePosition,
    val matchedElements: List<ExactReduction<*>>,
    val lastMatched: NonTerminal<*>,
) : ParseState

private sealed interface StateAdvancer<S : ParseState> {
    val nonTerminal: NonTerminal<*>
    val production: NonLeafProduction<*>
    fun start(reduction: ExactReduction<*>): ParseState?
    fun nextNonTerminals(state: S): List<NonTerminal<*>>
    fun advance(state: S, reduction: ExactReduction<*>): S?
    fun reduce(state: S): ExactReduction<*>?
}

private data class SequenceAdvancer(
    override val nonTerminal: NonTerminal<*>,
    override val production: SequenceProduction<*>,
) : StateAdvancer<SequenceState> {
    
    override fun start(reduction: ExactReduction<*>): SequenceState? {
        if (reduction.nonTerminal != production.components.first()) return null
        return SequenceState(
            start = reduction.span.begin,
            position = reduction.span.end,
            matchedComponents = listOf(reduction),
            nextComponentIndex = 1,
        )
    }
    
    override fun nextNonTerminals(state: SequenceState): List<NonTerminal<*>> {
        if (state.nextComponentIndex >= production.components.size) return listOf()
        return listOf(production.components[state.nextComponentIndex])
    }
    
    override fun advance(state: SequenceState, reduction: ExactReduction<*>): SequenceState? {
        if (state.nextComponentIndex >= production.components.size) return null
        if (reduction.nonTerminal != production.components[state.nextComponentIndex]) return null
        return SequenceState(
            start = state.start,
            position = reduction.span.end,
            matchedComponents = state.matchedComponents + reduction,
            nextComponentIndex = state.nextComponentIndex + 1,
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun reduce(state: SequenceState): ExactReduction<*>? {
        if (state.nextComponentIndex < production.components.size) return null
        val node = try {
            production.factory(state.matchedComponents.map { it.node })
        } catch (_: ParseException) {
            return null
        }
        return ExactReduction(
            node = node,
            span = SourceSpan(state.start, state.position),
            nonTerminal = nonTerminal as NonTerminal<Any?>,
        )
    }
}

private data class ListLikeAdvancer(
    override val nonTerminal: NonTerminal<*>,
    override val production: ListLikeProduction<*, *>,
) : StateAdvancer<ListLikeState> {
    
    override fun start(reduction: ExactReduction<*>): ListLikeState? {
        production.begin?.let { begin ->
            if (reduction.nonTerminal != begin) return null
            return ListLikeState(
                start = reduction.span.begin,
                position = reduction.span.end,
                matchedElements = listOf(),
                lastMatched = reduction.nonTerminal,
            )
        }
        if (reduction.nonTerminal == production.element) {
            return ListLikeState(
                start = reduction.span.begin,
                position = reduction.span.end,
                matchedElements = listOf(reduction),
                lastMatched = reduction.nonTerminal,
            )
        }
        production.end?.let { end ->
            if (reduction.nonTerminal != end) return null
            return ListLikeState(
                start = reduction.span.begin,
                position = reduction.span.end,
                matchedElements = listOf(),
                lastMatched = reduction.nonTerminal,
            )
        }
        return null
    }
    
    override fun nextNonTerminals(state: ListLikeState): List<NonTerminal<*>> {
        return when (state.lastMatched) {
            production.begin, production.separator -> listOfNotNull(production.element, production.end)
            production.element -> if (production.separator == null) {
                listOfNotNull(production.element, production.end)
            } else {
                listOfNotNull(production.separator, production.end)
            }
            else -> listOf()
        }
    }
    
    override fun advance(state: ListLikeState, reduction: ExactReduction<*>): ListLikeState? {
        return when (state.lastMatched) {
            production.begin, production.separator -> when (reduction.nonTerminal) {
                production.element -> ListLikeState(
                    start = state.start,
                    position = reduction.span.end,
                    matchedElements = state.matchedElements + reduction,
                    lastMatched = reduction.nonTerminal,
                )
                production.end -> ListLikeState(
                    start = state.start,
                    position = reduction.span.end,
                    matchedElements = state.matchedElements,
                    lastMatched = reduction.nonTerminal,
                )
                else -> null
            }
            production.element -> if (production.separator == null) {
                when (reduction.nonTerminal) {
                    production.element -> ListLikeState(
                        start = state.start,
                        position = reduction.span.end,
                        matchedElements = state.matchedElements + reduction,
                        lastMatched = reduction.nonTerminal,
                    )
                    production.end -> ListLikeState(
                        start = state.start,
                        position = reduction.span.end,
                        matchedElements = state.matchedElements,
                        lastMatched = reduction.nonTerminal,
                    )
                    else -> null
                }
            } else {
                when (reduction.nonTerminal) {
                    production.separator, production.end -> ListLikeState(
                        start = state.start,
                        position = reduction.span.end,
                        matchedElements = state.matchedElements,
                        lastMatched = reduction.nonTerminal,
                    )
                    else -> null
                }
            }
            else -> null
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun reduce(state: ListLikeState): ExactReduction<*>? {
        production.end?.let { end ->
            if (state.lastMatched != end) return null
        }
        val node = try {
            (production as ListLikeProduction<Any?, Any?>).factory(state.matchedElements.map { it.node })
        } catch (_: ParseException) {
            return null
        }
        return ExactReduction(
            node = node,
            span = SourceSpan(state.start, state.position),
            nonTerminal = nonTerminal as NonTerminal<Any?>,
        )
    }
}
