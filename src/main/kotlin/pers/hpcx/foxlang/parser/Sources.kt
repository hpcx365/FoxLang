package pers.hpcx.foxlang.parser

@JvmInline
value class SourcePosition(val fragIndex: Int) : Comparable<SourcePosition> {
    
    init {
        require(fragIndex >= 0) { "Fragment index must be non-negative: $fragIndex" }
    }
    
    operator fun plus(other: Int) = SourcePosition(fragIndex + other)
    override fun compareTo(other: SourcePosition) = fragIndex.compareTo(other.fragIndex)
    override fun toString() = fragIndex.toString()
}

data class SourceSpan(val begin: SourcePosition, val end: SourcePosition) : Comparable<SourceSpan>, Iterable<SourcePosition> {
    
    init {
        require(begin <= end) { "Begin must be less than or equal to end: $begin, $end" }
    }
    
    fun isEmpty() = begin == end
    fun isNotEmpty() = begin < end
    
    override fun compareTo(other: SourceSpan): Int {
        check(begin == other.begin) { "Begin must be equal: $begin, ${other.begin}" }
        return end.compareTo(other.end)
    }
    
    override fun iterator() = (begin.fragIndex..<end.fragIndex).asSequence().map { SourcePosition(it) }.iterator()
    override fun toString() = "[$begin, $end)"
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
        require(line >= 0) { "Line number must be non-negative: $line" }
        require(column >= 0) { "Column must be non-negative: $column" }
        require(text.isNotEmpty()) { "Text must not be empty: $text" }
        require(text.none { it.isWhitespace() }) { "Text must not contain whitespace: $text" }
    }
    
    override fun toString() = "line ${line + 1}, column ${column + 1}: <plain-text> $text"
}

data class NewlineFragment(
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
    
    init {
        require(line >= 0) { "Line number must be non-negative: $line" }
        require(column >= 0) { "Column must be non-negative: $column" }
    }
    
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
    
    init {
        require(line >= 0) { "Line number must be non-negative: $line" }
        require(column >= 0) { "Column must be non-negative: $column" }
    }
    
    override fun toString() = "line ${line + 1}, column ${column + 1}: <string-literal> $string"
}

class SourceFragmentationException(message: String) : Exception(message)

fun String.toFragments(): List<SourceFragment> {
    val lines = lines()
    var line = 0
    var column = 0
    
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
            line++
            column = 0
        }
    }
    
    fun fragmentPlain(line: Int, begin: Int, end: Int): PlainFragment {
        val lineText = lines[line]
        val text = lineText.substring(begin, end)
        return PlainFragment(line, begin, text)
    }
    
    fun fragmentNewline(line: Int, begin: Int): NewlineFragment {
        return NewlineFragment(line, begin)
    }
    
    fun fragmentCharLiteral(line: Int, begin: Int, end: Int): CharLiteralFragment {
        val text = lines[line].substring(begin + 1, end - 1)
        when (text.length) {
            0 -> throw SourceFragmentationException("Empty character literal")
            1 -> return CharLiteralFragment(line, begin, text[0])
            2 -> if (text[0] == '\\') {
                return when (text[1]) {
                    't' -> CharLiteralFragment(line, begin, '\t')
                    'b' -> CharLiteralFragment(line, begin, '\b')
                    'n' -> CharLiteralFragment(line, begin, '\n')
                    'r' -> CharLiteralFragment(line, begin, '\r')
                    '\'' -> CharLiteralFragment(line, begin, '\'')
                    '\\' -> CharLiteralFragment(line, begin, '\\')
                    else -> throw SourceFragmentationException("Invalid escape sequence: $text")
                }
            }
            6 -> if (text[0] == '\\' && text[1] == 'u') {
                try {
                    return CharLiteralFragment(line, begin, text.substring(2, 6).toInt(16).toChar())
                } catch (_: NumberFormatException) {
                    throw SourceFragmentationException("Invalid unicode escape sequence: $text")
                }
            }
        }
        throw SourceFragmentationException("Invalid character literal: $text")
    }
    
    fun fragmentStringLiteral(line: Int, begin: Int, end: Int): StringLiteralFragment {
        var i = 0
        val builder = StringBuilder()
        val text = lines[line].substring(begin + 1, end - 1)
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
        return StringLiteralFragment(line, begin, builder.toString())
    }
    
    fun fragmentRawStringLiteral(line: Int, begin: Int, end: Int): StringLiteralFragment {
        var i = 0
        val builder = StringBuilder()
        val text = lines[line].substring(begin + 2, end - 1)
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
        return StringLiteralFragment(line, begin, builder.toString())
    }
    
    val result = mutableListOf<SourceFragment>()
    
    while (line < lines.size) {
        if (column >= lines[line].length) {
            result += fragmentNewline(line, lines[line].length)
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
        
        if (lines[line].startsWith("r\"", column)) {
            val begin = column
            val lineText = lines[line]
            val index = findClosingQuote(lineText, begin + 2, '"')
                ?: throw SourceFragmentationException("Unterminated raw string at line ${line + 1}, column ${begin + 1}")
            result += fragmentRawStringLiteral(line, begin, index + 1)
            move(index - column + 1)
            continue
        }
        
        if (lines[line][column] == '\'') {
            val begin = column
            val lineText = lines[line]
            val index = findClosingQuote(lineText, begin + 1, '\'')
                ?: throw SourceFragmentationException("Unterminated char at line ${line + 1}, column ${begin + 1}")
            result += fragmentCharLiteral(line, begin, index + 1)
            move(index - column + 1)
            continue
        }
        
        if (lines[line][column] == '"') {
            val begin = column
            val lineText = lines[line]
            val index = findClosingQuote(lineText, begin + 1, '"')
                ?: throw SourceFragmentationException("Unterminated string at line ${line + 1}, column ${begin + 1}")
            result += fragmentStringLiteral(line, begin, index + 1)
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
