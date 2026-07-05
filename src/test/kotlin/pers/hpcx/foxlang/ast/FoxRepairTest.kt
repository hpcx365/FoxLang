package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.frontend.*
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FoxRepairTest {
    
    @Test
    fun repairMissingFormalParameterComma() {
        val source = """
def foo(a: Int b: Int) {
    return a
}
""".trimStart()
        val analysis = FoxFileParser.analyze(source)
        assertIs<ParseContextBuildFailure>(analysis.buildResult)
        
        analysis.repairFox()
        
        val bPosition = analysis.source.plainPosition("b")
        val missingCommaSpan = SourceSpan(bPosition, bPosition)
        assertTrue(
            analysis.context.matches(Comma, missingCommaSpan).any { it.matchType == ParseMatchType.Expected },
        )
        
        val parameterListSpan = analysis.source.spanBetweenPlain("(", ")")
        assertTrue(
            analysis.context.matches(FormalParameterList, parameterListSpan).any { it.matchType == ParseMatchType.Synthetic },
        )
        assertTrue(
            analysis.context.matches(node<FoxFile>(), analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
        )
    }
    
    @Test
    fun repairFormalParameterListMissingClose() {
        val analysis = FoxFileParser.analyze("(a: Int, b: Int")
        
        analysis.repairFormalParameterList()
        
        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
        assertTrue(
            analysis.context.matches(ParenClose, closeSpan).any { it.matchType == ParseMatchType.Expected },
        )
        assertTrue(
            analysis.context.matches(FormalParameterList, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
        )
    }
    
    @Test
    fun repairFormalParameterListMissingOpen() {
        val analysis = FoxFileParser.analyze("a: Int, b: Int)")
        
        analysis.repairFormalParameterList()
        
        val openSpan = SourceSpan(analysis.source.span.start, analysis.source.span.start)
        assertTrue(
            analysis.context.matches(ParenOpen, openSpan).any { it.matchType == ParseMatchType.Expected },
        )
        assertTrue(
            analysis.context.matches(FormalParameterList, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
        )
    }
    
    @Test
    fun repairFormalParameterListEmptySegmentBetweenCommas() {
        val analysis = FoxFileParser.analyze("(a: Int,, b: Int)")
        
        analysis.repairFormalParameterList()
        
        val secondComma = analysis.source.plainPositions(",")[1]
        val missingItemSpan = SourceSpan(secondComma, secondComma)
        assertTrue(
            analysis.context.matches(FormalParameter, missingItemSpan).any { it.matchType == ParseMatchType.Expected },
        )
        assertTrue(
            analysis.context.matches(FormalParameterList, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
        )
    }
    
    @Test
    fun repairFormalParameterListMissingCommaAfterNestedGeneric() {
        val analysis = FoxFileParser.analyze("(a: AnyOf<Int, String> b: Int)")
        
        analysis.repairFormalParameterList()
        
        val bPosition = analysis.source.plainPosition("b")
        val missingCommaSpan = SourceSpan(bPosition, bPosition)
        assertTrue(
            analysis.context.matches(Comma, missingCommaSpan).any { it.matchType == ParseMatchType.Expected },
        )
        assertTrue(
            analysis.context.matches(FormalParameterList, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
        )
    }
}

private fun ParseAnalysis<FoxFile>.repairFormalParameterList() {
    context.repair(Expectation(FormalParameterList, source.span), FoxRepairStrategy)
}

private fun Source.plainPosition(text: String): SourcePosition {
    val index = fragments.indexOfFirst { it is PlainFragment && it.text == text }
    require(index >= 0) { "Plain fragment not found: $text" }
    return SourcePosition(index)
}

private fun Source.plainPositions(text: String): List<SourcePosition> {
    return fragments.mapIndexedNotNull { index, fragment ->
        if (fragment is PlainFragment && fragment.text == text) SourcePosition(index) else null
    }
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
