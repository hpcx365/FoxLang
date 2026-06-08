package pers.hpcx.foxlang.parser

data class GrammarCheckReport(
    val undefinedNonTerminals: Set<NonTerminal<*>>,
    val unreachableNonTerminals: Set<NonTerminal<*>>,
    val emptyResults: Set<NonTerminal<*>>,
) {
    override fun toString() = buildString {
        appendLine("undefined: ${undefinedNonTerminals.joinToString()}")
        appendLine("unreachable: ${unreachableNonTerminals.joinToString()}")
        appendLine("empty: ${emptyResults.joinToString()}")
    }
}

class Grammar(
    val productions: List<Production<*>>,
) {
    val groupedProductions by lazy {
        productions.groupBy { it.result }
    }
    
    fun productionsFor(nonTerminal: NonTerminal<*>) = groupedProductions[nonTerminal].orEmpty()
    
    val starterFirstSets by lazy {
        val first = mutableMapOf<NonTerminal<*>, MutableSet<StarterShape>>()
        val dependents = mutableMapOf<NonTerminal<*>, MutableSet<NonTerminal<*>>>()
        val queue = ArrayDeque(groupedProductions.keys)
        val queued = groupedProductions.keys.toMutableSet()
        
        fun starterShapesFor(production: Production<*>): Set<StarterShape> = when (production) {
            is FixedProduction -> setOf(ExactToken(production.string))
            is RegexProduction -> setOf(WordToken)
            is CharLiteralProduction -> setOf(CharLiteralToken)
            is StringLiteralProduction -> setOf(StringLiteralToken)
            is FormattedStringLiteralProduction -> setOf(FormattedStringLiteralToken)
            is SerialProduction<*> -> production.components.firstOrNull()?.let { first[it].orEmpty() } ?: emptySet()
            is ListProduction<*> -> when {
                production.begin != null -> first[production.begin].orEmpty()
                else -> first[production.element].orEmpty()
            }
        }
        
        while (queue.isNotEmpty()) {
            val nonTerminal = queue.removeFirst()
            queued.remove(nonTerminal)
            val target = first.getOrPut(nonTerminal) { mutableSetOf() }
            var changed = false
            productionsFor(nonTerminal).forEach { production ->
                production.referencedNonTerminals().forEach { child ->
                    dependents.getOrPut(child) { mutableSetOf() }.add(nonTerminal)
                }
                starterShapesFor(production).forEach { shape ->
                    if (target.add(shape)) {
                        changed = true
                    }
                }
            }
            if (changed) {
                dependents[nonTerminal].orEmpty().forEach { dependent ->
                    if (queued.add(dependent)) {
                        queue.addLast(dependent)
                    }
                }
            }
        }
        first.mapValues { it.value.toSet() }
    }
    
    fun check(roots: Set<NonTerminal<*>>): GrammarCheckReport {
        val defined = productions.map { it.result }.toSet()
        val referenced = productions.flatMap { it.referencedNonTerminals() }.toSet()
        val visited = mutableSetOf<NonTerminal<*>>()
        val queue = ArrayDeque<NonTerminal<*>>()
        roots.forEach {
            if (visited.add(it)) queue += it
        }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            productionsFor(current)
                .flatMap { it.referencedNonTerminals() }
                .forEach {
                    if (visited.add(it)) queue += it
                }
        }
        return GrammarCheckReport(
            undefinedNonTerminals = referenced - defined,
            unreachableNonTerminals = defined - visited,
            emptyResults = emptySet(),
        )
    }
}
