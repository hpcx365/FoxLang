package pers.hpcx.foxlang.utils

import dk.brics.automaton.Automaton
import dk.brics.automaton.RegExp
import dk.brics.automaton.RunAutomaton

class AutoRegex private constructor(private val automaton: Automaton) {
    
    init {
        automaton.determinize()
        automaton.minimize()
    }
    
    val runner: RunAutomaton by lazy { RunAutomaton(automaton, true) }
    
    fun isEmpty() = automaton.isEmpty
    fun isTotal() = automaton.isTotal
    fun isNotEmpty() = !isEmpty()
    fun isNotTotal() = !isTotal()
    
    operator fun contains(text: String) = runner.run(text)
    operator fun unaryMinus() = AutoRegex(automaton.complement())
    operator fun plus(other: AutoRegex) = AutoRegex(automaton.union(other.automaton))
    operator fun minus(other: AutoRegex) = AutoRegex(automaton.minus(other.automaton))
    operator fun times(other: AutoRegex) = AutoRegex(automaton.intersection(other.automaton))
    operator fun div(other: AutoRegex) = AutoRegex(automaton.concatenate(other.automaton))
    
    fun shortestExample(): String? = automaton.getShortestExample(true)
    fun shortestRejectedExample(): String? = automaton.getShortestExample(false)
    
    override fun toString() = automaton.toString()
    override fun hashCode() = automaton.hashCode()
    override fun equals(other: Any?) = other is AutoRegex && automaton == other.automaton
    
    companion object {
        
        private val Empty = AutoRegex(Automaton.makeEmpty())
        private val Total = AutoRegex(Automaton.makeAnyString())
        
        fun empty() = Empty
        fun total() = Total
        
        fun literal(literal: String) = AutoRegex(Automaton.makeString(literal))
        fun pattern(pattern: String) = AutoRegex(RegExp(pattern).toAutomaton())
        
        fun literals(literals: Iterable<String>) = literals.map { literal(it) }.reduce { a, b -> a + b }
        fun patterns(patterns: Iterable<String>) = patterns.map { pattern(it) }.reduce { a, b -> a + b }
    }
}
