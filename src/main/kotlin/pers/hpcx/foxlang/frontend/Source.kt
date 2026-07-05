package pers.hpcx.foxlang.frontend

data class Source(
    val text: String,
    val fragments: List<SourceFragment>,
) : Iterable<SourceFragment> {
    
    constructor(text: String) : this(text, text.toFragments())
    
    override fun iterator() = fragments.iterator()
    operator fun get(pos: SourcePosition) = fragments[pos.fragIndex]
    fun getOrNull(pos: SourcePosition) = fragments.getOrNull(pos.fragIndex)
    val span get() = SourceSpan(SourcePosition(0), SourcePosition(fragments.size))
    val positions get() = fragments.indices.map { SourcePosition(it) }
}

@JvmInline
value class SourcePosition(val fragIndex: Int) : Comparable<SourcePosition> {
    
    init {
        require(fragIndex >= 0) { "Fragment index must be non-negative: $fragIndex" }
    }
    
    operator fun plus(other: Int) = SourcePosition(fragIndex + other)
    operator fun minus(other: Int) = SourcePosition(fragIndex - other)
    override fun compareTo(other: SourcePosition) = fragIndex.compareTo(other.fragIndex)
    override fun toString() = fragIndex.toString()
}

data class SourceSpan(val start: SourcePosition, val end: SourcePosition) : Comparable<SourceSpan>, Iterable<SourcePosition> {
    
    init {
        require(start <= end) { "Start must be less than or equal to end: $start, $end" }
    }
    
    fun isEmpty() = start == end
    fun isNotEmpty() = start < end
    val length get() = end.fragIndex - start.fragIndex
    
    override fun compareTo(other: SourceSpan): Int {
        check(start == other.start) { "Start must be equal: $start, ${other.start}" }
        return end.compareTo(other.end)
    }
    
    override fun iterator() = (start.fragIndex..<end.fragIndex).asSequence().map { SourcePosition(it) }.iterator()
    override fun toString() = "[$start, $end)"
}

sealed interface SourceFragment {
    val line: Int
    val column: Int
}

data class PlainFragment(
    override val line: Int,
    override val column: Int,
    val text: String,
) : SourceFragment {
    
    init {
        require(text.isNotEmpty()) { "Text must not be empty: $text" }
        require(text.none { it.isWhitespace() }) { "Text must not contain whitespace: $text" }
    }
    
    override fun toString() = "line ${line + 1}, column ${column + 1}: <plain-text> $text"
}

data class LineBreakFragment(
    override val line: Int,
    override val column: Int,
) : SourceFragment {
    
    override fun toString() = "line ${line + 1}, column ${column + 1}: <line-separator>"
}

data class CharLiteralFragment(
    override val line: Int,
    override val column: Int,
    val char: Char,
) : SourceFragment {
    
    override fun toString() = "line ${line + 1}, column ${column + 1}: <char-literal> '${
        when (char) {
            '\b' -> "\\b"
            '\t' -> "\\t"
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\\' -> "\\\\"
            '\'' -> "\\'"
            '\"' -> "\\\""
            else -> char
        }
    }'"
}

data class StringLiteralFragment(
    override val line: Int,
    override val column: Int,
    val string: String,
) : SourceFragment {
    
    override fun toString() = "line ${line + 1}, column ${column + 1}: <string-literal> $string"
}

data class FormattedStringStartFragment(
    override val line: Int,
    override val column: Int,
    val isRaw: Boolean,
) : SourceFragment {
    
    override fun toString() = "line ${line + 1}, column ${column + 1}: <formatted-string-start> ${if (isRaw) "raw" else "regular"}"
}

data class FormattedStringTextFragment(
    override val line: Int,
    override val column: Int,
    val text: String,
) : SourceFragment {
    
    init {
        require(text.isNotEmpty()) { "Text must not be empty: $text" }
    }
    
    override fun toString() = "line ${line + 1}, column ${column + 1}: <formatted-string-text> $text"
}

data class FormattedExpressionStartFragment(
    override val line: Int,
    override val column: Int,
) : SourceFragment {
    
    override fun toString() = "line ${line + 1}, column ${column + 1}: <formatted-expression-start>"
}

data class FormattedExpressionEndFragment(
    override val line: Int,
    override val column: Int,
) : SourceFragment {
    
    override fun toString() = "line ${line + 1}, column ${column + 1}: <formatted-expression-end>"
}

data class FormattedStringEndFragment(
    override val line: Int,
    override val column: Int,
) : SourceFragment {
    
    override fun toString() = "line ${line + 1}, column ${column + 1}: <formatted-string-end>"
}

fun SourceFragment.sourceWidth(): Int = when (this) {
    is PlainFragment -> text.length
    is LineBreakFragment -> 1
    is CharLiteralFragment -> 2 + when (char) {
        '\b', '\t', '\n', '\r', '\\', '\'' -> 2
        else -> 1
    }
    is StringLiteralFragment -> 2 + string.length
    is FormattedStringStartFragment -> if (isRaw) 3 else 2
    is FormattedStringTextFragment -> text.length
    is FormattedExpressionStartFragment -> 1
    is FormattedExpressionEndFragment -> 1
    is FormattedStringEndFragment -> 1
}

class SourceFragmentationException(message: String) : Exception(message)

fun String.toFragments(): List<SourceFragment> {
    val lines = lines()
    
    fun fragmentPlain(line: Int, start: Int, end: Int): PlainFragment {
        val lineText = lines[line]
        val text = lineText.substring(start, end)
        return PlainFragment(line, start, text)
    }
    
    fun fragmentLineBreak(line: Int, start: Int): LineBreakFragment {
        return LineBreakFragment(line, start)
    }
    
    fun fragmentCharLiteral(line: Int, start: Int, end: Int): CharLiteralFragment {
        val text = lines[line].substring(start + 1, end - 1)
        when (text.length) {
            0 -> throw SourceFragmentationException("Empty character literal")
            1 -> return CharLiteralFragment(line, start, text[0])
            2 -> if (text[0] == '\\') {
                return when (text[1]) {
                    't' -> CharLiteralFragment(line, start, '\t')
                    'b' -> CharLiteralFragment(line, start, '\b')
                    'n' -> CharLiteralFragment(line, start, '\n')
                    'r' -> CharLiteralFragment(line, start, '\r')
                    '\'' -> CharLiteralFragment(line, start, '\'')
                    '\\' -> CharLiteralFragment(line, start, '\\')
                    else -> throw SourceFragmentationException("Invalid escape sequence: $text")
                }
            }
            6 -> if (text[0] == '\\' && text[1] == 'u') {
                try {
                    return CharLiteralFragment(line, start, text.substring(2, 6).toInt(16).toChar())
                } catch (_: NumberFormatException) {
                    throw SourceFragmentationException("Invalid unicode escape sequence: $text")
                }
            }
        }
        throw SourceFragmentationException("Invalid character literal: $text")
    }
    
    fun fragmentStringLiteral(line: Int, start: Int, end: Int): StringLiteralFragment {
        var i = 0
        val builder = StringBuilder()
        val text = lines[line].substring(start + 1, end - 1)
        while (i < text.length) {
            val char = text[i++]
            if (char == '\\') {
                if (i >= text.length) {
                    throw SourceFragmentationException("Unterminated escape sequence in string literal")
                }
                val nextChar = text[i++]
                when (nextChar) {
                    't' -> builder.append('\t')
                    'b' -> builder.append('\b')
                    'n' -> builder.append('\n')
                    'r' -> builder.append('\r')
                    '\\' -> builder.append('\\')
                    '"' -> builder.append('"')
                    'u' -> {
                        if (i + 3 >= text.length) {
                            throw SourceFragmentationException("Unterminated unicode escape sequence in string literal")
                        }
                        builder.append(lines[line].substring(i, i + 4).toInt(16).toChar())
                        i += 4
                    }
                    else -> throw SourceFragmentationException("Invalid escape sequence in string literal")
                }
            } else {
                builder.append(char)
            }
        }
        return StringLiteralFragment(line, start, builder.toString())
    }
    
    fun fragmentRawStringLiteral(line: Int, start: Int, end: Int): StringLiteralFragment {
        var i = 0
        val builder = StringBuilder()
        val text = lines[line].substring(start + 2, end - 1)
        while (i < text.length) {
            val char = text[i++]
            if (char == '\\') {
                if (i < text.length && text[i] == '"') {
                    builder.append('"')
                    i++
                } else if (i == text.length && builder.isNotEmpty()) {
                    builder.append('\\')
                } else if (i < text.length && text[i] == '\\' && i + 1 == text.length) {
                    builder.append('\\')
                    i++
                } else {
                    builder.append('\\')
                }
            } else {
                builder.append(char)
            }
        }
        return StringLiteralFragment(line, start, builder.toString())
    }
    
    fun parseUnicodeEscape(lineText: String, line: Int, column: Int): Char {
        if (column + 5 >= lineText.length) {
            throw SourceFragmentationException("Unterminated unicode escape sequence at line ${line + 1}, column ${column + 1}")
        }
        val text = lineText.substring(column + 2, column + 6)
        return try {
            text.toInt(16).toChar()
        } catch (_: NumberFormatException) {
            throw SourceFragmentationException("Invalid unicode escape sequence: $text")
        }
    }
    
    val result = mutableListOf<SourceFragment>()
    var line = 0
    var column = 0
    
    val formattedScanner = object {
        
        fun scanFormattedExpression(line: Int, start: Int): Int {
            val lineText = lines[line]
            var index = start
            var braceDepth = 0
            
            while (index < lineText.length) {
                if (lineText[index].isWhitespace()) {
                    index++
                    continue
                }
                
                if (lineText.startsWith("//", index)) {
                    throw SourceFragmentationException("Unterminated formatted expression at line ${line + 1}, column ${start + 1}")
                }
                
                if (lineText.startsWith("/*", index)) {
                    val end = lineText.indexOf("*/", index + 2)
                        .takeIf { it >= 0 }
                        ?: throw SourceFragmentationException("Unterminated comment in formatted expression at line ${line + 1}, column ${index + 1}")
                    index = end + 2
                    continue
                }
                
                if (lineText.startsWith("rf\"", index)) {
                    index = scanFormattedString(line, index, isRaw = true)
                    continue
                }
                
                if (lineText.startsWith("f\"", index)) {
                    index = scanFormattedString(line, index, isRaw = false)
                    continue
                }
                
                if (lineText.startsWith("r\"", index)) {
                    val end = findClosingQuote(lineText, index + 2, '"')
                        ?: throw SourceFragmentationException("Unterminated raw string at line ${line + 1}, column ${index + 1}")
                    result += fragmentRawStringLiteral(line, index, end + 1)
                    index = end + 1
                    continue
                }
                
                if (lineText[index] == '\'') {
                    val end = findClosingQuote(lineText, index + 1, '\'')
                        ?: throw SourceFragmentationException("Unterminated char at line ${line + 1}, column ${index + 1}")
                    result += fragmentCharLiteral(line, index, end + 1)
                    index = end + 1
                    continue
                }
                
                if (lineText[index] == '"') {
                    val end = findClosingQuote(lineText, index + 1, '"')
                        ?: throw SourceFragmentationException("Unterminated string at line ${line + 1}, column ${index + 1}")
                    result += fragmentStringLiteral(line, index, end + 1)
                    index = end + 1
                    continue
                }
                
                if (lineText[index] == '{') {
                    result += PlainFragment(line, index, "{")
                    braceDepth++
                    index++
                    continue
                }
                
                if (lineText[index] == '}') {
                    if (braceDepth == 0) {
                        result += FormattedExpressionEndFragment(line, index)
                        return index + 1
                    }
                    result += PlainFragment(line, index, "}")
                    braceDepth--
                    index++
                    continue
                }
                
                var end = index
                while (end < lineText.length && lineText[end].isWordChar()) {
                    end++
                }
                
                if (index < end) {
                    result += fragmentPlain(line, index, end)
                    index = end
                    continue
                }
                
                result += fragmentPlain(line, index, index + 1)
                index++
            }
            
            throw SourceFragmentationException("Unterminated formatted expression at line ${line + 1}, column ${start + 1}")
        }
        
        fun scanFormattedString(line: Int, start: Int, isRaw: Boolean): Int {
            val lineText = lines[line]
            var index = start + if (isRaw) 3 else 2
            var textColumn = index
            var escapedBraceDepth = 0
            val builder = StringBuilder()
            
            fun appendEscapedClosingBrace() {
                builder.append('}')
                if (escapedBraceDepth > 0) escapedBraceDepth--
            }
            
            fun flushText(end: Int) {
                if (builder.isNotEmpty()) {
                    result += FormattedStringTextFragment(line, textColumn, builder.toString())
                    builder.clear()
                }
                textColumn = end
            }
            
            result += FormattedStringStartFragment(line, start, isRaw)
            
            while (index < lineText.length) {
                when (val char = lineText[index]) {
                    '"' -> {
                        flushText(index)
                        result += FormattedStringEndFragment(line, index)
                        return index + 1
                    }
                    
                    '{' -> {
                        flushText(index)
                        result += FormattedExpressionStartFragment(line, index)
                        index = scanFormattedExpression(line, index + 1)
                        textColumn = index
                    }
                    
                    '}' -> {
                        if (escapedBraceDepth <= 0) {
                            throw SourceFragmentationException("Unescaped } in formatted string at line ${line + 1}, column ${index + 1}")
                        }
                        builder.append(char)
                        escapedBraceDepth--
                        index++
                    }
                    
                    '\\' -> {
                        if (isRaw) {
                            if (index + 1 >= lineText.length) {
                                builder.append('\\')
                                index++
                                continue
                            }
                            when (lineText[index + 1]) {
                                '"' -> {
                                    builder.append('"')
                                    index += 2
                                }
                                '{' -> {
                                    builder.append('{')
                                    escapedBraceDepth++
                                    index += 2
                                }
                                '}' -> {
                                    appendEscapedClosingBrace()
                                    index += 2
                                }
                                '\\' -> {
                                    if (index + 2 < lineText.length && lineText[index + 2] == '"') {
                                        builder.append('\\')
                                        index += 2
                                    } else {
                                        builder.append('\\')
                                        index++
                                    }
                                }
                                else -> {
                                    builder.append('\\')
                                    index++
                                }
                            }
                        } else {
                            if (index + 1 >= lineText.length) {
                                throw SourceFragmentationException("Unterminated escape sequence in formatted string")
                            }
                            when (lineText[index + 1]) {
                                't' -> builder.append('\t')
                                'b' -> builder.append('\b')
                                'n' -> builder.append('\n')
                                'r' -> builder.append('\r')
                                '\\' -> builder.append('\\')
                                '"' -> builder.append('"')
                                '{' -> {
                                    builder.append('{')
                                    escapedBraceDepth++
                                }
                                '}' -> appendEscapedClosingBrace()
                                'u' -> builder.append(parseUnicodeEscape(lineText, line, index))
                                else -> throw SourceFragmentationException("Invalid escape sequence in formatted string")
                            }
                            index += if (lineText[index + 1] == 'u') 6 else 2
                        }
                    }
                    
                    else -> {
                        builder.append(char)
                        index++
                    }
                }
            }
            
            throw SourceFragmentationException("Unterminated formatted string at line ${line + 1}, column ${start + 1}")
        }
    }
    
    fun move(numChars: Int) {
        require(numChars >= 0) { "Cannot move negative characters: $numChars" }
        require(line < lines.size) { "Line index out of bounds: $line >= ${lines.size}" }
        
        val lineText = lines[line]
        val newColumn = column + numChars
        
        require(newColumn <= lineText.length) {
            "Cannot move beyond line end: column=$column, move=$numChars, line length=${lineText.length}"
        }
        
        if (newColumn < lineText.length) {
            column = newColumn
        } else {
            if (line + 1 < lines.size && result.isNotEmpty() && result.last() !is LineBreakFragment) {
                result += fragmentLineBreak(line, lineText.length)
            }
            line++
            column = 0
        }
    }
    
    while (line < lines.size) {
        if (column >= lines[line].length) {
            move(0)
            continue
        }
        
        if (lines[line][column].isWhitespace()) {
            move(1)
            continue
        }
        
        if (lines[line].startsWith("//", column)) {
            move(lines[line].length - column)
            continue
        }
        
        if (lines[line].startsWith("/*", column)) {
            move(2)
            val startLine = line
            val startColumn = column
            
            while (true) {
                if (line >= lines.size) {
                    throw SourceFragmentationException("Unterminated comment starting at line ${startLine + 1}, column ${startColumn + 1}")
                }
                val index = lines[line].indexOf("*/", column)
                if (index >= 0) {
                    move(index - column + 2)
                    break
                }
                move(lines[line].length - column)
            }
            continue
        }
        
        if (lines[line].startsWith("rf\"", column)) {
            val end = formattedScanner.scanFormattedString(line, column, isRaw = true)
            move(end - column)
            continue
        }
        
        if (lines[line].startsWith("f\"", column)) {
            val end = formattedScanner.scanFormattedString(line, column, isRaw = false)
            move(end - column)
            continue
        }
        
        if (lines[line].startsWith("r\"", column)) {
            val start = column
            val lineText = lines[line]
            val index = findClosingQuote(lineText, start + 2, '"')
                ?: throw SourceFragmentationException("Unterminated raw string at line ${line + 1}, column ${start + 1}")
            result += fragmentRawStringLiteral(line, start, index + 1)
            move(index - column + 1)
            continue
        }
        
        if (lines[line][column] == '\'') {
            val start = column
            val lineText = lines[line]
            val index = findClosingQuote(lineText, start + 1, '\'')
                ?: throw SourceFragmentationException("Unterminated char at line ${line + 1}, column ${start + 1}")
            result += fragmentCharLiteral(line, start, index + 1)
            move(index - column + 1)
            continue
        }
        
        if (lines[line][column] == '"') {
            val start = column
            val lineText = lines[line]
            val index = findClosingQuote(lineText, start + 1, '"')
                ?: throw SourceFragmentationException("Unterminated string at line ${line + 1}, column ${start + 1}")
            result += fragmentStringLiteral(line, start, index + 1)
            move(index - column + 1)
            continue
        }
        
        var end = column
        while (end < lines[line].length && lines[line][end].isWordChar()) {
            end++
        }
        
        if (column < end) {
            result += fragmentPlain(line, column, end)
            move(end - column)
            continue
        }
        
        result += fragmentPlain(line, column, column + 1)
        move(1)
    }
    
    return result
}

fun Char.isWordChar(): Boolean {
    return this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this == '_'
}

private fun findClosingQuote(
    text: String,
    start: Int,
    quote: Char,
): Int? {
    var index = start
    while (index < text.length) {
        if (text[index] == quote) {
            var backslashCount = 0
            var cursor = index - 1
            while (cursor >= start - 1 && text[cursor] == '\\') {
                backslashCount++
                cursor--
            }
            if (backslashCount % 2 == 0) return index
        }
        index++
    }
    return null
}
