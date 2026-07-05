package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.frontend.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FoxDiagnosticTreeTest {
    
    @Test
    fun selectsMissingCommaAsBestFoxDiagnosticTree() {
        val source = """
def foo(a: Int b: Int) {
    return a
}
""".trimStart()
        val analysis = source.analyzeFox().repairFox()
        
        val tree = assertNotNull(analysis.diagnoseFox())
        val leaf = tree.expectedLeaves().single().match
        val bPosition = analysis.source.plainPosition("b")
        
        assertEquals(node<FoxFile>(), tree.match.symbol)
        assertEquals(ParseMatchType.Synthetic, tree.match.matchType)
        assertEquals(Comma, leaf.symbol)
        assertEquals(SourceSpan(bPosition, bPosition), leaf.span)
        assertTrue(tree.score.negativeMinDepth < 0)
    }
    
    @Test
    fun prefersPreciseChildRepairOverBroadExpectedList() {
        val analysis = "(a: Int b: Int)".analyzeFox()
        val parameterListSpan = analysis.source.spanBetweenPlain("(", ")")
        
        analysis.context.repair(Expectation(FormalParameterList, parameterListSpan), FoxRepairStrategy)
        analysis.context.seed(Expectation(FormalParameterList, parameterListSpan).toParseMatch())
        analysis.context.grow()
        
        val tree = assertNotNull(
            analysis.context.best(FormalParameterList, parameterListSpan, FoxDiagnosticScoringStrategy),
        )
        val leaf = tree.expectedLeaves().single().match
        val bPosition = analysis.source.plainPosition("b")
        
        assertEquals(ParseMatchType.Synthetic, tree.match.matchType)
        assertEquals(Comma, leaf.symbol)
        assertEquals(SourceSpan(bPosition, bPosition), leaf.span)
        assertTrue(
            tree.score < FoxDiagnosticScoringStrategy.expectedScore(FormalParameterList, parameterListSpan, analysis.source),
        )
    }
    
    @Test
    fun selectsMissingCloseParenForFormalParameterList() {
        val analysis = "(a: Int, b: Int".analyzeFox()
        
        analysis.context.repair(Expectation(FormalParameterList, analysis.source.span), FoxRepairStrategy)
        
        val tree = assertNotNull(
            analysis.context.best(FormalParameterList, FoxDiagnosticScoringStrategy),
        )
        val leaf = tree.expectedLeaves().single().match
        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
        
        assertEquals(ParseMatchType.Synthetic, tree.match.matchType)
        assertEquals(ParenClose, leaf.symbol)
        assertEquals(closeSpan, leaf.span)
    }
}

private fun Source.plainPosition(text: String): SourcePosition {
    val index = fragments.indexOfFirst { it is PlainFragment && it.text == text }
    require(index >= 0) { "Plain fragment not found: $text" }
    return SourcePosition(index)
}

private fun Source.spanBetweenPlain(open: String, close: String): SourceSpan {
    val openIndex = fragments.indexOfFirst { it is PlainFragment && it.text == open }
    val closeIndex = fragments.indices.firstOrNull { index ->
        index > openIndex && fragments[index] is PlainFragment && (fragments[index] as PlainFragment).text == close
    } ?: -1
    require(openIndex >= 0) { "Open fragment not found: $open" }
    require(closeIndex >= 0) { "Close fragment not found: $close" }
    return SourceSpan(SourcePosition(openIndex), SourcePosition(closeIndex + 1))
}
