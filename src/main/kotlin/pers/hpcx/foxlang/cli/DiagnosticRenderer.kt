package pers.hpcx.foxlang.cli

import pers.hpcx.foxlang.frontend.*

fun renderDiagnosticReport(
    sourceName: String,
    source: Source,
    report: DiagnosticReport,
): String {
    if (report.items.isEmpty()) return ""
    
    val lines = source.text.lines()
    return buildString {
        report.items.forEachIndexed { index, item ->
            if (index > 0) appendLine()
            appendDiagnosticItem(sourceName, lines, item)
        }
    }
}

private fun StringBuilder.appendDiagnosticItem(
    sourceName: String,
    lines: List<String>,
    item: DiagnosticReportItem,
) {
    val position = item.range.begin
    val lineNumber = position.line + 1
    val columnNumber = position.column + 1
    append(sourceName)
    append(':')
    append(lineNumber)
    append(':')
    append(columnNumber)
    append(": ")
    append(item.severity.renderName())
    append(": ")
    appendLine(item.message)
    
    val sourceLine = lines.getOrNull(position.line).orEmpty()
    val linePrefix = lineNumber.toString()
    append(" ".repeat(linePrefix.length))
    appendLine(" |")
    append(linePrefix)
    append(" | ")
    appendLine(sourceLine)
    append(" ".repeat(linePrefix.length))
    append(" | ")
    appendLine(sourceLine.markerFor(item.range))
}

private fun String.markerFor(range: DiagnosticRange): String {
    val begin = range.begin.column.coerceIn(0, length)
    val end = if (range.end.line == range.begin.line) {
        range.end.column.coerceIn(begin, length)
    } else {
        length
    }
    val width = (end - begin).coerceAtLeast(1)
    return buildString {
        repeatVisualSpace(this@markerFor.take(begin))
        append("^".repeat(width))
    }
}

private fun StringBuilder.repeatVisualSpace(text: String) {
    text.forEach { char ->
        append(if (char == '\t') '\t' else ' ')
    }
}

private fun DiagnosticSeverity.renderName(): String {
    return when (this) {
        DiagnosticSeverity.Error -> "error"
        DiagnosticSeverity.Warning -> "warning"
        DiagnosticSeverity.Information -> "info"
    }
}
