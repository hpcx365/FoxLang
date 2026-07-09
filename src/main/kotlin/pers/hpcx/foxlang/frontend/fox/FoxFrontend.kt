package pers.hpcx.foxlang.frontend.fox

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.frontend.common.*
import java.io.PrintWriter
import java.io.StringWriter

val FoxFileParser = Parser(FoxGrammar, File)
val DefaultAstSourceContext = AstSourceContext()

fun String.sourceFox() = toFoxSource()

fun Source<FoxFragment>.parseFox() = FoxFileParser.parse(this)
fun Source<FoxFragment>.analyzeFox() = FoxFileParser.analyze(this)

fun ParseAnalysis<ParsedFoxFile>.repairFox() = also {
    context.repair(Expectation(start, source.span), FoxRepairStrategy)
}

fun ParseAnalysis<ParsedFoxFile>.diagnoseFox(): DiagnosticTree? {
    return context.best(start, FoxDiagnosticScoringStrategy)
}

fun FoxFile.toSource() = with(DefaultAstSourceContext) { printToString { printFile(this@toSource) } }
fun FoxFileElement.toSource() = with(DefaultAstSourceContext) { printToString { printFileElement(this@toSource) } }
fun FoxType.toSource() = with(DefaultAstSourceContext) { printToString { printType(this@toSource) } }
fun FoxStatement.toSource() = with(DefaultAstSourceContext) { printToString { printStatement(this@toSource) } }

private fun printToString(block: PrintWriter.() -> Unit): String {
    val result = StringWriter()
    PrintWriter(result).block()
    return result.toString()
}
