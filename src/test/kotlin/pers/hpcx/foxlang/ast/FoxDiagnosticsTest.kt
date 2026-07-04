package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.frontend.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FoxDiagnosticsTest {
    
    @Test
    fun testFileDiagnosticRecoversMalformedFileElementLine() {
        val source = """
type Good = Int
@
type AlsoGood = String
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertExpected(FileElementLine)
    }
    
    @Test
    fun testFileDiagnosticRecoversMalformedStatementLine() {
        val source = """
def main() {
    println("ok")
    @
    println("still ok")
}
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertExpected(StatementLine)
    }
    
    @Test
    fun testFileDiagnosticRecoversMultipleIndependentGaps() {
        val source = """
type Good = Int
@
def main() {
    println("ok")
    @
    println("still ok")
}
type AlsoGood = String
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertExpected(FileElementLine)
        result.assertExpected(StatementLine)
    }
    
    @Test
    fun testMethodDiagnosticRecoversMalformedFormalParameter() {
        val source = """
def main(ok: Int, @, name: String) {
    println("ok")
}
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertMatched(FormalParameterList)
        result.assertExpected(FormalParameter)
    }
    
    @Test
    fun testMethodDiagnosticRecoversEmptyFormalParameterSlot() {
        val source = """
def main(ok: Int, , name: String) {
    println("ok")
}
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertExpected(FormalParameter)
    }
    
    @Test
    fun testMethodDiagnosticRecoversMissingFormalParameterList() {
        val source = """
def main {
    println("ok")
}
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertExpected(FormalParameterList)
    }
    
    @Test
    fun testMethodDiagnosticRecoversMissingBody() {
        val source = """
def main()
type StillParsed = Int
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertExpected(node<FoxBlock>())
    }
    
    @Test
    fun testMethodDiagnosticPrefersDelimitedBodyOverFallback() {
        val source = """
def main() {
    @
}
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertExpected(StatementLine)
        result.assertNoExpected(node<FoxBlock>(), "Expected method body") { it.isEmpty() }
    }
    
    @Test
    fun testStatementDiagnosticRecoversMalformedActualParameter() {
        val source = """
def main() {
    println("ok", @, "still ok")
}
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertExpected(StatementLine)
        result.assertExpected(ActualParameter)
    }
    
    @Test
    fun testStatementDiagnosticRecoversEmptyActualParameterSlot() {
        val source = """
def main() {
    println("ok", , "still ok")
}
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertExpected(ActualParameter)
    }
    
    @Test
    fun testTypeDiagnosticRecoversMalformedStructField() {
        val source = """
def main(value: Struct<name: String, @, age: Int>) {
    println("ok")
}
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertMatched(StructFieldParameterList)
        result.assertExpected(StructFieldParameter)
    }
    
    @Test
    fun testWhenDiagnosticRecoversMalformedCaseBody() {
        val source = """
def main() {
    when {
        true -> @
        else -> {}
    }
}
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertExpected(WhenCaseLine)
        result.assertExpected(ControlBody)
    }
    
    @Test
    fun testDiagnosticReportsNothingForExactFile() {
        val source = """
type Good = Int
def main() {
    println("ok")
}
""".trimStart()
        
        val result = diagnoseFoxFile(source)
        
        assertTrue(FoxFileParser.parse(source) != null)
        assertTrue(result.report().items.isEmpty(), result.reportSummary())
    }
    
    @Test
    fun testDiagnosticReportsMissingFormalParameterListAsInsertion() {
        val source = """
def main {
    println("ok")
}
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        val report = result.assertReport(FormalParameterList, "Expected formal parameter list")
        assertEquals(DiagnosticSeverity.Error, report.severity)
        assertTrue(report.span.isEmpty(), result.reportSummary())
        assertEquals(report.range.begin, report.range.end)
    }
    
    @Test
    fun testDiagnosticReportsMalformedFormalParameterAsLeafIssue() {
        val source = """
def main(ok: Int, @, name: String) {
    println("ok")
}
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertReport(FormalParameter, "Malformed formal parameter")
        result.assertNoReport(FormalParameterList, "Expected formal parameter list")
    }
    
    @Test
    fun testDiagnosticReportsMultipleIndependentIssues() {
        val source = """
type Good = Int
@
def main() {
    @
}
""".trimStart()
        
        val result = diagnoseMalformed(source)
        
        result.assertReport(FileElementLine, "Expected file element")
        val expressionReport = result.assertReport(AssignmentExpression, "Expected expression")
        assertEquals(2, result.report().items.size, result.reportSummary())
        assertEquals(DiagnosticPosition(3, 4), expressionReport.range.begin)
    }
    
    @Test
    fun testDiagnosticReportsRuleFactoryFailure() {
        val source = """
type Bad = Struct<name: String, name: Int>
""".trimStart()
        
        assertNull(FoxFileParser.parse(source))
        val result = diagnoseFoxFile(source)
        
        val report = result.assertReport(StructFieldParameterList, "Duplicate struct field parameter name 'name'")
        assertEquals(DiagnosticSeverity.Error, report.severity)
        assertTrue(report.span.isNotEmpty(), result.reportSummary())
        assertEquals(listOf("Duplicate struct field parameter name 'name'"), result.report().items.map { it.message })
        assertTrue(result.expectedSymbols().isEmpty(), result.expectedSymbols().joinToString())
    }
    
    @Test
    fun testDiagnosticReportsMethodTypeRuleFactoryFailureWithoutCandidateNoise() {
        val source = """
type Bad = Method<value: Int, this: String, return: Bool>
""".trimStart()
        
        assertNull(FoxFileParser.parse(source))
        val result = diagnoseFoxFile(source)
        
        assertEquals(listOf("Method type 'this' must be the first item"), result.report().items.map { it.message })
    }

    private fun diagnoseMalformed(source: String): DiagnosticResult {
        assertNull(FoxFileParser.parse(source))
        
        val result = diagnoseFoxFile(source)
        assertTrue(
            result.recovered,
            result.expectedSymbols().joinToString(),
        )
        return result
    }
    
    private fun DiagnosticResult.assertExpected(symbol: Symbol<*>) {
        assertTrue(
            chart.allMatches().any { it.origin is MatchOrigin.Expected && it.symbol == symbol },
            expectedSymbols().joinToString(),
        )
    }
    
    private fun DiagnosticResult.assertNoExpected(
        symbol: Symbol<*>,
        reason: String,
        span: (SourceSpan) -> Boolean = { true },
    ) {
        assertTrue(
            chart.allMatches().none {
                val origin = it.origin as? MatchOrigin.Expected ?: return@none false
                it.symbol == symbol && origin.reason == reason && span(it.span)
            },
            expectedSymbols().joinToString(),
        )
    }
    
    private fun DiagnosticResult.assertMatched(symbol: Symbol<*>) {
        assertTrue(
            chart.allMatches().any { it.symbol == symbol },
            expectedSymbols().joinToString(),
        )
    }
    
    private fun DiagnosticResult.assertReport(
        symbol: Symbol<*>,
        message: String,
    ): DiagnosticReportItem {
        return report().items.firstOrNull { it.symbol == symbol && it.message == message }
            ?: error(reportSummary())
    }
    
    private fun DiagnosticResult.assertNoReport(
        symbol: Symbol<*>,
        message: String,
    ) {
        assertTrue(
            report().items.none { it.symbol == symbol && it.message == message },
            reportSummary(),
        )
    }
    
    private fun DiagnosticResult.expectedSymbols(): List<String> {
        return chart.allMatches()
            .mapNotNull {
                val origin = it.origin as? MatchOrigin.Expected ?: return@mapNotNull null
                "${it.symbol}@${it.span}: ${origin.reason}"
            }
            .sorted()
    }
    
    private fun DiagnosticResult.reportSummary(): String {
        return report().items
            .joinToString(separator = "\n") { "${it.symbol}@${it.span}: ${it.message}" }
    }
}
