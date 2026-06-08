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

class ParseContext(
    source: String,
    private val grammar: Grammar,
) {
    val fragments = source.toFragments()
    val parseQueue = ArrayDeque<Pair<Cursor, NonTerminal<*>>>()
    val parseQueueVisited = mutableSetOf<Pair<Cursor, NonTerminal<*>>>()
    val memoization = mutableMapOf<Cursor, MutableMap<NonTerminal<*>, ParseResult<*>>>()
    private val discoveredStates = mutableSetOf<ParseStateKey>()
    private val queuedStates = mutableSetOf<ParseStateKey>()
    private val dependentStates = mutableMapOf<ParseStateKey, MutableSet<ParseStateKey>>()
    private var currentState: ParseStateKey? = null
    
    operator fun get(cursor: Cursor): SourceFragment? {
        return fragments.getOrNull(cursor.fragIndex)
    }
    
    fun memoize(result: ParseResult<*>) {
        memoization.getOrPut(result.interval.begin) { mutableMapOf() }.compute(result.nonTerminal) { _, oldResult ->
            if (oldResult == null || result > oldResult) {
                result.also {
                    enqueueDependents(ParseStateKey(result.interval.begin, result.nonTerminal))
                }
            } else {
                oldResult
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N> parse(cursor: Cursor, nonTerminal: NonTerminal<N>): Success<N>? {
        val next = ParseStateKey(cursor, nonTerminal)
        parseQueueVisited += cursor to nonTerminal
        recordDependency(next)
        if (!starterPredicate(cursor, nonTerminal)) {
            return null
        }
        val raw = memoization[cursor]?.get(nonTerminal)
        if (raw == null) {
            enqueueDiscoveredState(next)
        }
        return raw as? Success<N>?
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N> memoized(cursor: Cursor, nonTerminal: NonTerminal<N>): ParseResult<N>? {
        return memoization[cursor]?.get(nonTerminal) as ParseResult<N>?
    }
    
    fun memoizedAt(cursor: Cursor): Map<NonTerminal<*>, ParseResult<*>> {
        return memoization[cursor]?.toMap() ?: emptyMap()
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
        currentState = ParseStateKey(cursor, nonTerminal)
        return try {
            block()
        } finally {
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
