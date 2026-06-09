package pers.hpcx.foxlang.parser

sealed interface StarterShape
data class ExactToken(val text: String) : StarterShape
data object WordToken : StarterShape
data object CharLiteralToken : StarterShape
data object StringLiteralToken : StarterShape
data object FormattedStringLiteralToken : StarterShape
data object EofToken : StarterShape

private data class ParseStateKey(
    val cursor: Cursor,
    val nonTerminal: NonTerminal<*>,
)

private data class StateEvaluationFrame(
    val state: ParseStateKey,
    val visibleSnapshot: ParseResultSnapshot,
    val candidateStore: ParseResultStore,
)

class ParseContext(
    source: String,
    private val grammar: Grammar,
) {
    val fragments = source.toFragments()
    val parseQueue = ArrayDeque<Pair<Cursor, NonTerminal<*>>>()
    val parseQueueVisited = mutableSetOf<Pair<Cursor, NonTerminal<*>>>()
    val memoization = mutableMapOf<Cursor, MutableMap<NonTerminal<*>, ParseResultStore>>()
    private val discoveredStates = mutableSetOf<ParseStateKey>()
    private val queuedStates = mutableSetOf<ParseStateKey>()
    private val dependentStates = mutableMapOf<ParseStateKey, MutableSet<ParseStateKey>>()
    private var currentState: ParseStateKey? = null
    private var currentEvaluationFrame: StateEvaluationFrame? = null
    
    operator fun get(cursor: Cursor): SourceFragment? {
        return fragments.getOrNull(cursor.fragIndex)
    }
    
    fun memoize(result: ParseResult<*>) {
        val state = ParseStateKey(result.interval.begin, result.nonTerminal)
        val frame = currentEvaluationFrame
        if (frame != null && frame.state == state) {
            frame.candidateStore.add(result)
            return
        }
        val store = memoization.getOrPut(result.interval.begin) { mutableMapOf() }
            .getOrPut(result.nonTerminal) { ParseResultStore() }
        if (store.add(result)) {
            enqueueDependents(state)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N> parse(cursor: Cursor, nonTerminal: NonTerminal<N>): Success<N>? {
        return bestSuccess(cursor, nonTerminal)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N> bestSuccess(cursor: Cursor, nonTerminal: NonTerminal<N>): Success<N>? {
        return parseSuccesses(cursor, nonTerminal).maxWithOrNull { left, right ->
            left.compareTo(right)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N> parseSuccesses(cursor: Cursor, nonTerminal: NonTerminal<N>): List<Success<N>> {
        val next = ParseStateKey(cursor, nonTerminal)
        parseQueueVisited += cursor to nonTerminal
        recordDependency(next)
        if (!starterPredicate(cursor, nonTerminal)) {
            return emptyList()
        }
        currentEvaluationFrame?.takeIf { it.state == next }?.let { frame ->
            return frame.visibleSnapshot.successes as List<Success<N>>
        }
        val raw = memoization[cursor]?.get(nonTerminal)
        if (raw == null) {
            enqueueDiscoveredState(next)
        }
        return raw?.successes() as? List<Success<N>> ?: emptyList()
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N> memoized(cursor: Cursor, nonTerminal: NonTerminal<N>): ParseResult<N>? {
        currentEvaluationFrame
            ?.takeIf { it.state == ParseStateKey(cursor, nonTerminal) }
            ?.let { frame -> return frame.visibleSnapshot.bestResult() as ParseResult<N>? }
        return memoization[cursor]?.get(nonTerminal)?.bestResult() as ParseResult<N>?
    }
    
    fun memoizedAt(cursor: Cursor): Map<NonTerminal<*>, ParseResultSnapshot> {
        return memoization[cursor]
            ?.mapValues { (_, store) -> store.snapshot() }
            ?: emptyMap()
    }
    
    fun seedRoot(root: Pair<Cursor, NonTerminal<*>>) {
        parseQueueVisited += root
        enqueueInitialState(ParseStateKey(root.first, root.second))
    }
    
    fun dequeue(cursor: Cursor, nonTerminal: NonTerminal<*>) {
        queuedStates.remove(ParseStateKey(cursor, nonTerminal))
    }
    
    fun <T> recordStateUpdate(cursor: Cursor, nonTerminal: NonTerminal<*>, block: () -> T): T {
        val previous = currentState
        val previousFrame = currentEvaluationFrame
        currentState = ParseStateKey(cursor, nonTerminal)
        val baseStore = memoization[cursor]?.get(nonTerminal)
        currentEvaluationFrame = StateEvaluationFrame(
            state = currentState!!,
            visibleSnapshot = (baseStore ?: ParseResultStore()).snapshot(),
            candidateStore = (baseStore?.copyStore() ?: ParseResultStore()),
        )
        return try {
            block()
        } finally {
            currentEvaluationFrame?.let(::commitEvaluationFrame)
            currentEvaluationFrame = previousFrame
            currentState = previous
        }
    }
    
    private fun enqueueInitialState(state: ParseStateKey) {
        if (discoveredStates.add(state)) {
            enqueueState(state)
        }
    }
    
    private fun enqueueDiscoveredState(state: ParseStateKey) {
        if (discoveredStates.add(state)) {
            enqueueState(state)
        }
    }
    
    private fun enqueueDependents(state: ParseStateKey) {
        dependentStates[state].orEmpty().forEach { dependent ->
            if (queuedStates.add(dependent)) {
                parseQueue.addLast(dependent.cursor to dependent.nonTerminal)
            }
        }
    }
    
    private fun enqueueState(state: ParseStateKey) {
        if (queuedStates.add(state)) {
            parseQueue.addLast(state.cursor to state.nonTerminal)
        }
    }
    
    private fun recordDependency(child: ParseStateKey) {
        val parent = currentState ?: return
        dependentStates.getOrPut(child) { mutableSetOf() }.add(parent)
    }
    
    private fun commitEvaluationFrame(frame: StateEvaluationFrame) {
        val currentStore = memoization.getOrPut(frame.state.cursor) { mutableMapOf() }
            .getOrPut(frame.state.nonTerminal) { ParseResultStore() }
        var changed = false
        frame.candidateStore.successes().forEach { success ->
            changed = currentStore.add(success) || changed
        }
        frame.candidateStore.bestFailure()?.let { failure ->
            changed = currentStore.add(failure) || changed
        }
        if (changed) {
            enqueueDependents(frame.state)
        }
    }
    
    private fun starterPredicate(cursor: Cursor, nonTerminal: NonTerminal<*>): Boolean {
        val firstSet = grammar.starterFirstSets[nonTerminal]
        if (!firstSet.isNullOrEmpty()) {
            if (!matchesAnyStarterShape(cursor, firstSet)) {
                return false
            }
        }
        return true
    }
    
    private fun matchesAnyStarterShape(cursor: Cursor, shapes: Set<StarterShape>): Boolean {
        return shapes.any { matchesStarterShape(cursor, it) }
    }
    
    private fun matchesStarterShape(cursor: Cursor, shape: StarterShape): Boolean = when (shape) {
        is ExactToken -> exactTokenStartsAt(cursor, shape.text)
        WordToken -> (this[cursor] as? PlainFragment)?.text?.all { it.isWordChar() } == true
        CharLiteralToken -> this[cursor] is CharFragment
        StringLiteralToken -> this[cursor] is StringFragment
        FormattedStringLiteralToken -> this[cursor] is FormattedStringFragment
        EofToken -> this[cursor] == null
    }
    
    private fun exactTokenStartsAt(cursor: Cursor, token: String): Boolean {
        var current = cursor
        var previous: PlainFragment? = null
        var candidate = ""
        while (candidate.length < token.length) {
            val next = this[current]
            if (next !is PlainFragment) break
            if (previous != null) {
                if (next.line != previous.line || next.column != previous.column + previous.text.length) break
            }
            candidate += next.text
            if (!token.startsWith(candidate)) return false
            current += 1
            previous = next
        }
        return candidate == token
    }
}
