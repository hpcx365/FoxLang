package pers.hpcx.foxlang.parser

import dk.brics.automaton.Automaton
import dk.brics.automaton.RegExp
import dk.brics.automaton.Transition
import java.util.*

fun stringStringSimilarity(string1: String, string2: String): Double {
    val maxLen = maxOf(string1.length, string2.length)
    return if (maxLen == 0) 1.0 else 1.0 - editDistance(string1, string2).toDouble() / maxLen
}

private fun editDistance(string1: String, string2: String): Int {
    if (string1.length < string2.length) {
        return editDistance(string2, string1)
    }
    
    val len1 = string1.length
    val len2 = string2.length
    
    var curr = IntArray(len2 + 1)
    var next = IntArray(len2 + 1)
    
    for (j in 0..len2) {
        curr[j] = j
    }
    
    for (i in 1..len1) {
        next[0] = i
        
        for (j in 1..len2) {
            next[j] = minOf(
                curr[j] + 1,
                next[j - 1] + 1,
                curr[j - 1] + if (string1[i - 1] == string2[j - 1]) 0 else 1,
            )
        }
        
        curr = next.also { next = curr }
    }
    
    return curr[len2]
}

fun stringRegexSimilarity(string: String, regex: String): Double {
    val nearest = nearestStringInRegexLanguage(string, regex)
    val maxLen = maxOf(string.length, nearest.string.length)
    return if (maxLen == 0) 1.0 else 1.0 - nearest.distance.toDouble() / maxLen
}

private data class RegexNearestString(
    val string: String,
    val distance: Int,
)

private data class IndexedTransition(
    val dest: Int,
    val min: Char,
    val max: Char,
) {
    fun accepts(char: Char): Boolean = char in min..max
}

private fun relax(
    from: Int,
    to: Int,
    weight: Int,
    writesChar: Boolean,
    char: Char,
    dist: IntArray,
    emittedLength: IntArray,
    prev: IntArray,
    prevChar: CharArray,
    prevWritesChar: BooleanArray,
    deque: ArrayDeque<Int>,
) {
    val nextDistance = dist[from] + weight
    val nextLength = emittedLength[from] + if (writesChar) 1 else 0
    if (nextDistance > dist[to] || nextDistance == dist[to] && nextLength <= emittedLength[to]) {
        return
    }
    
    dist[to] = nextDistance
    emittedLength[to] = nextLength
    prev[to] = from
    prevChar[to] = char
    prevWritesChar[to] = writesChar
    if (weight == 0) {
        deque.addFirst(to)
    } else {
        deque.addLast(to)
    }
}

private fun nearestStringInRegexLanguage(string: String, regex: String): RegexNearestString {
    val automaton = RegExp(regex).toAutomaton()
    automaton.determinize()
    automaton.minimize()
    require(!automaton.isEmpty) { "Regex language is empty." }
    
    val states = automaton.states.sorted()
    val index = states.withIndex().associate { it.value to it.index }
    val outgoing = states.map { state ->
        state.transitions
            .sortedWith(compareBy<Transition>({ it.min }, { it.max }, { index.getValue(it.dest) }))
            .map { transition ->
                IndexedTransition(
                    dest = index.getValue(transition.dest),
                    min = transition.min,
                    max = transition.max,
                )
            }
    }
    
    val nodeCount = (string.length + 1) * states.size
    val dist = IntArray(nodeCount) { Int.MAX_VALUE }
    val emittedLength = IntArray(nodeCount) { -1 }
    val prev = IntArray(nodeCount) { -1 }
    val prevChar = CharArray(nodeCount)
    val prevWritesChar = BooleanArray(nodeCount)
    val deque = ArrayDeque<Int>()
    val startNode = index.getValue(automaton.initialState)
    
    dist[startNode] = 0
    emittedLength[startNode] = 0
    deque.addFirst(startNode)
    
    while (deque.isNotEmpty()) {
        val node = deque.removeFirst()
        val position = node / states.size
        val stateIndex = node % states.size
        
        if (position < string.length) {
            relax(
                from = node,
                to = node + states.size,
                weight = 1,
                writesChar = false,
                char = '\u0000',
                dist = dist,
                emittedLength = emittedLength,
                prev = prev,
                prevChar = prevChar,
                prevWritesChar = prevWritesChar,
                deque = deque,
            )
        }
        
        for (transition in outgoing[stateIndex]) {
            val emitted = if (position < string.length && transition.accepts(string[position])) {
                string[position]
            } else {
                transition.min
            }
            
            if (position < string.length) {
                relax(
                    from = node,
                    to = (position + 1) * states.size + transition.dest,
                    weight = if (transition.accepts(string[position])) 0 else 1,
                    writesChar = true,
                    char = emitted,
                    dist = dist,
                    emittedLength = emittedLength,
                    prev = prev,
                    prevChar = prevChar,
                    prevWritesChar = prevWritesChar,
                    deque = deque,
                )
            }
            
            relax(
                from = node,
                to = position * states.size + transition.dest,
                weight = 1,
                writesChar = true,
                char = transition.min,
                dist = dist,
                emittedLength = emittedLength,
                prev = prev,
                prevChar = prevChar,
                prevWritesChar = prevWritesChar,
                deque = deque,
            )
        }
    }
    
    var bestNode = -1
    var bestDistance = Int.MAX_VALUE
    val endPosition = string.length
    for ((stateIndex, state) in states.withIndex()) {
        if (!state.isAccept) continue
        val node = endPosition * states.size + stateIndex
        val candidateLength = emittedLength[node]
        val bestLength = if (bestNode >= 0) emittedLength[bestNode] else -1
        if (dist[node] < bestDistance || dist[node] == bestDistance && candidateLength > bestLength) {
            bestDistance = dist[node]
            bestNode = node
        }
    }
    
    check(bestNode >= 0) { "Failed to reach an accept state in a non-empty deterministic automaton." }
    
    val reversed = StringBuilder()
    var node = bestNode
    while (node != startNode) {
        if (prevWritesChar[node]) {
            reversed.append(prevChar[node])
        }
        node = prev[node]
    }
    
    return RegexNearestString(
        string = reversed.reverse().toString(),
        distance = bestDistance,
    )
}

fun regexRegexSimilarity(regex1: String, regex2: String, maxLen: Int): Double {
    require(maxLen >= 0)
    
    val a1 = RegExp(regex1).toAutomaton()
    val a2 = RegExp(regex2).toAutomaton()
    a1.determinize()
    a2.determinize()
    a1.minimize()
    a2.minimize()
    
    val intersection = a1.intersection(a2)
    val union = a1.union(a2)
    
    val interCount = countAcceptedUpToLength(intersection, maxLen)
    val unionCount = countAcceptedUpToLength(union, maxLen)
    
    return if (unionCount == 0L) 1.0 else interCount.toDouble() / unionCount.toDouble()
}

private fun countAcceptedUpToLength(automaton: Automaton, maxLen: Int): Long {
    require(maxLen >= 0)
    
    val a = automaton.clone()
    a.determinize()
    a.minimize()
    
    val states = a.states.toList()
    val index = states.withIndex().associate { it.value to it.index }
    
    var curr = LongArray(states.size)
    var next = LongArray(states.size)
    curr[index.getValue(a.initialState)] = 1L
    
    var total = 0L
    for (len in 0..maxLen) {
        for (i in states.indices) {
            if (states[i].isAccept) total = Math.addExact(total, curr[i])
        }
        if (len == maxLen) break
        
        next.fill(0L)
        for ((i, state) in states.withIndex()) {
            val ways = curr[i]
            if (ways == 0L) continue
            
            for (t in state.transitions) {
                val width = t.max.code - t.min.code + 1L
                val j = index.getValue(t.dest)
                next[j] = Math.addExact(next[j], ways * width)
            }
        }
        
        curr = next.also { next = curr }
    }
    
    return total
}
