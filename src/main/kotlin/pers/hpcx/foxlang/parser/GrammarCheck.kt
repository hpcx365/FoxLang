package pers.hpcx.foxlang.parser

data class GrammarCheckReport(
    val undefinedNonTerminals: Set<NonTerminal<*>>,
    val unreachableNonTerminals: Set<NonTerminal<*>>,
    val emptyResults: Set<NonTerminal<*>>,
) {
    
    fun isClean() = undefinedNonTerminals.isEmpty() && unreachableNonTerminals.isEmpty() && emptyResults.isEmpty()
    
    override fun toString() = buildString {
        appendLine("undefined: ${undefinedNonTerminals.joinToString()}")
        appendLine("unreachable: ${unreachableNonTerminals.joinToString()}")
        appendLine("empty: ${emptyResults.joinToString()}")
    }
}

fun Grammar.check(roots: Set<NonTerminal<*>>): GrammarCheckReport {
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
