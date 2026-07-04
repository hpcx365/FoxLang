package pers.hpcx.foxlang.cli

import pers.hpcx.foxlang.ast.diagnoseFoxFile
import pers.hpcx.foxlang.frontend.SourceFragmentationException
import pers.hpcx.foxlang.frontend.report
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FoxCli {
    
    fun run(
        args: Array<String>,
        stdout: Appendable = System.out,
        stderr: Appendable = System.err,
    ): Int {
        val path = parsePath(args) ?: return usage(stderr)
        val source = try {
            readText(path)
        } catch (e: IOException) {
            stderr.appendLine("${path}: error: ${e.message}")
            return 2
        }
        
        val result = try {
            diagnoseFoxFile(source)
        } catch (e: SourceFragmentationException) {
            stderr.appendLine("${path}: error: ${e.message}")
            return 1
        }
        
        val report = result.report()
        if (report.items.isEmpty()) {
            stdout.appendLine("${path}: ok")
            return 0
        }
        
        stdout.append(renderDiagnosticReport(path.toString(), result.chart.source, report))
        return 1
    }
    
    private fun parsePath(args: Array<String>): Path? {
        return when (args.size) {
            1 if args[0] !in setOf("-h", "--help") -> Paths.get(args[0])
            2 if args[0] == "diagnose" -> Paths.get(args[1])
            else -> null
        }
    }
    
    private fun usage(stderr: Appendable): Int {
        stderr.appendLine("Usage: fox-lang diagnose <file>")
        stderr.appendLine("   or: fox-lang <file>")
        return 2
    }
    
    private fun readText(path: Path): String {
        return String(Files.readAllBytes(path), StandardCharsets.UTF_8)
    }
}
