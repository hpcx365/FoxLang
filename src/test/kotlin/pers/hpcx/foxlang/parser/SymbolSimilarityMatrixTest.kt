package pers.hpcx.foxlang.parser

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SymbolSimilarityMatrixTest {
    
    @Test
    fun printSmallGrammarSymbolSimilarityMatrix() {
        printSmallGrammarSymbolSimilarityMatrix(specialSymbolWeight = 2)
        printSmallGrammarSymbolSimilarityMatrix(specialSymbolWeight = 6)
    }
    
    private fun printSmallGrammarSymbolSimilarityMatrix(specialSymbolWeight: Int) {
        val identifier = node<String>().name("Identifier")
        val number = node<String>().name("Number")
        val value = node<String>().name("Value")
        val add = node<String>().name("Add")
        val sub = node<String>().name("Sub")
        val mul = node<String>().name("Mul")
        val call = node<String>().name("Call")
        val group = node<String>().name("Group")
        
        val plus = token("+")
        val minus = token("-")
        val star = token("*")
        val leftParen = token("(")
        val rightParen = token(")")
        
        val grammar = buildGrammar {
            weight(identifier, 8)
            weight(number, 3)
            weight(value, 10)
            weight(add, 21)
            weight(sub, 21)
            weight(mul, 21)
            weight(call, 20)
            weight(group, 12)
            weight(plus, specialSymbolWeight)
            weight(minus, specialSymbolWeight)
            weight(star, specialSymbolWeight)
            weight(leftParen, specialSymbolWeight)
            weight(rightParen, specialSymbolWeight)
            
            rules(identifier) { fixed("id") { it.text } }
            rules(number) { fixed("num") { it.text } }
            rules(plus) { fixed("+") { it.text } }
            rules(minus) { fixed("-") { it.text } }
            rules(star) { fixed("*") { it.text } }
            rules(leftParen) { fixed("(") { it.text } }
            rules(rightParen) { fixed(")") { it.text } }
            
            rules(value) {
                symbols(identifier) { it }
                symbols(number) { it }
            }
            rules(add) {
                symbols(value, plus, value) { left, _, right -> "$left+$right" }
            }
            rules(sub) {
                symbols(value, minus, value) { left, _, right -> "$left-$right" }
            }
            rules(mul) {
                symbols(value, star, value) { left, _, right -> "$left*$right" }
            }
            rules(call) {
                symbols(identifier, leftParen, value, rightParen) { name, _, argument, _ -> "$name($argument)" }
            }
            rules(group) {
                symbols(leftParen, value, rightParen) { _, inner, _ -> "($inner)" }
            }
        }
        
        val similarities = grammar.computeSymbolSimilarities(iterations = 4)
        val symbols = listOf(
            "Id" to identifier,
            "Num" to number,
            "Val" to value,
            "Add" to add,
            "Sub" to sub,
            "Mul" to mul,
            "Call" to call,
            "Grp" to group,
        )
        symbols.forEach { (_, left) ->
            symbols.forEach { (_, right) ->
                assertEquals(similarities[left, right], similarities[right, left], 1e-12)
            }
        }
        
        println()
        println("Special symbol weight: $specialSymbolWeight")
        println(formatSimilarityMatrix(similarities, symbols))
    }
    
    private fun formatSimilarityMatrix(
        similarities: SymbolSimilarity,
        symbols: List<Pair<String, Symbol<*>>>,
    ): String {
        val builder = StringBuilder()
        builder.append("Symbol similarity matrix\n")
        builder.append("      ")
        symbols.forEach { (label, _) ->
            builder.append(label.padStart(6))
        }
        symbols.forEach { (leftLabel, leftSymbol) ->
            builder.appendLine()
            builder.append(leftLabel.padEnd(6))
            symbols.forEach { (_, rightSymbol) ->
                builder.append(String.format(Locale.US, "%6.2f", similarities[leftSymbol, rightSymbol]))
            }
        }
        return builder.toString()
    }
}
