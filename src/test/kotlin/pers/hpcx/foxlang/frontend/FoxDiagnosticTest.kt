package pers.hpcx.foxlang.frontend

import pers.hpcx.foxlang.frontend.common.*
import pers.hpcx.foxlang.frontend.fox.*
import pers.hpcx.foxlang.frontend.fox.FoxBracketSymbol.ParenClose
import pers.hpcx.foxlang.frontend.fox.FoxBracketSymbol.SquareClose
import pers.hpcx.foxlang.frontend.fox.FoxTerminalSymbol.Comma
import pers.hpcx.foxlang.ir.SyntaxFile
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
        val analysis = source.analyzeFoxOrThrow().repairFox()
        
        val tree = assertNotNull(analysis.diagnoseFox())
        val terminal = tree.expectedLeaves().single().match
        val bPosition = analysis.source.plainPosition("b")
        
        assertEquals(File, tree.match.symbol)
        assertEquals(ParseMatchType.Synthetic, tree.match.matchType)
        assertEquals(Comma, terminal.symbol)
        assertEquals(SourceSpan(bPosition, bPosition), terminal.span)
        assertTrue(tree.score.negativeMinDepth < 0)
    }
    
    @Test
    fun selectsMissingCloseParenForFormalParameterList() {
        val analysis = "(a: Int, b: Int".analyzeFoxOrThrow()
        
        analysis.context.repair(Expectation(FormalParameterList, analysis.source.span), FoxRepairStrategy)
        
        val tree = assertNotNull(
            analysis.context.best(FormalParameterList, FoxDiagnosticScoringStrategy),
        )
        val terminal = tree.expectedLeaves().single().match
        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
        
        assertEquals(ParseMatchType.Synthetic, tree.match.matchType)
        assertEquals(ParenClose, terminal.symbol)
        assertEquals(closeSpan, terminal.span)
    }
    
    @Test
    fun selectsMissingIndexCloseForPostfixExpression() {
        val analysis = "tuple[0".analyzeFoxOrThrow()
        
        analysis.context.repair(Expectation(Statement, analysis.source.span), FoxRepairStrategy)
        
        val tree = assertNotNull(
            analysis.context.best(Statement, FoxDiagnosticScoringStrategy),
        )
        val terminal = tree.expectedLeaves().single().match
        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
        
        assertEquals(ParseMatchType.Synthetic, tree.match.matchType)
        assertEquals(SquareClose, terminal.symbol)
        assertEquals(closeSpan, terminal.span)
    }
}

private fun Source<*>.plainPosition(text: String): SourcePosition {
    val index = fragments.indexOfFirst { it is PlainFragment && it.text == text }
    require(index >= 0) { "Plain fragment not found: $text" }
    return SourcePosition(index)
}

private fun String.analyzeFoxOrThrow(): ParseAnalysis<SyntaxFile> {
    return when (val result = sourceFox()) {
        is SourceFragmentationSuccess -> (result.value).analyzeFox()
        is SourceFragmentationFailure -> error(result.errors.joinToString { it })
    }
}
