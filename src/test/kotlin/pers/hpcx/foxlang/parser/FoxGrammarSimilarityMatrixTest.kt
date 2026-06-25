package pers.hpcx.foxlang.parser

import pers.hpcx.foxlang.ast.FoxGrammar
import pers.hpcx.foxlang.ast.foxGrammarRepresentativeSymbols
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class FoxGrammarSimilarityMatrixTest {
    
    @Test
    fun printRepresentativeFoxGrammarSymbolSimilarityMatrices() {
        val similarities = FoxGrammar.computeSymbolSimilarities(iterations = 6)
        val symbols = foxGrammarRepresentativeSymbols()
        
        symbols.forEach { (_, left) ->
            symbols.forEach { (_, right) ->
                assertEquals(similarities[left, right], similarities[right, left], 1e-12)
            }
        }
        
        println()
        println(formatSimilarityMatrix(similarities, symbols.filterLabels("Identifier", "TypeName", "IntLit", "StringLit", "Entity", "Type", "FormalParams", "ActualParams")))
        println()
        println(formatSimilarityMatrix(similarities, symbols.filterLabels("Entity", "Type", "Postfix", "Unary", "Mul", "Add", "Compare", "AssignExpr", "Statement")))
        println()
        println(formatSimilarityMatrix(similarities, symbols.filterLabels("Statement", "Block", "IfCore", "WhileCore", "WhenCore", "TypeAlias", "MethodDef", "File")))
    }
    
    private fun List<Pair<String, Symbol<*>>>.filterLabels(
        vararg labels: String,
    ): List<Pair<String, Symbol<*>>> {
        val labelSet = labels.toSet()
        return filter { (label, _) -> label in labelSet }
    }
    
    private fun formatSimilarityMatrix(
        similarities: SymbolSimilarity,
        symbols: List<Pair<String, Symbol<*>>>,
    ): String {
        val width = maxOf(10, symbols.maxOf { it.first.length } + 2)
        val builder = StringBuilder()
        builder.append("FoxGrammar symbol similarity matrix\n")
        builder.append(" ".repeat(width))
        symbols.forEach { (label, _) ->
            builder.append(label.take(width - 1).padStart(width))
        }
        symbols.forEach { (leftLabel, leftSymbol) ->
            builder.appendLine()
            builder.append(leftLabel.take(width - 1).padEnd(width))
            symbols.forEach { (_, rightSymbol) ->
                builder.append(String.format(Locale.US, "%${width}.2f", similarities[leftSymbol, rightSymbol]))
            }
        }
        return builder.toString()
    }
}
