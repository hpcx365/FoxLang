package pers.hpcx.foxlang.frontend.fox

import pers.hpcx.foxlang.frontend.common.*
import pers.hpcx.foxlang.ir.*
import java.io.PrintWriter
import java.io.StringWriter

val FoxFileParser = Parser(FoxGrammar, File)
val DefaultAstSourceContext = AstSourceContext()

fun String.sourceFox() = toFoxFragments()

fun Source<FoxFragment>.parseFox() = FoxFileParser.parse(this)
fun Source<FoxFragment>.analyzeFox() = FoxFileParser.analyze(this)

fun ParseAnalysis<SyntaxFile>.repairFox() = also {
    context.repair(Expectation(start, source.span), FoxRepairStrategy)
}

fun ParseAnalysis<SyntaxFile>.diagnoseFox(): DiagnosticTree? {
    return context.best(start, FoxDiagnosticScoringStrategy)
}

fun SurfaceFile.toSource() = with(DefaultAstSourceContext) { printToString { printFile(this@toSource) } }
fun SurfaceFileElement.toSource() = with(DefaultAstSourceContext) { printToString { printFileElement(this@toSource) } }
fun SurfaceType.toSource() = with(DefaultAstSourceContext) { printToString { printType(this@toSource) } }
fun SurfaceStatement.toSource() = with(DefaultAstSourceContext) { printToString { printStatement(this@toSource) } }

private fun printToString(block: PrintWriter.() -> Unit): String {
    val result = StringWriter()
    PrintWriter(result).block()
    return result.toString()
}
