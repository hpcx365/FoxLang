package pers.hpcx.foxlang.utils

@JvmInline
value class Cursor(val fragIndex: Int) : Comparable<Cursor> {
    init {
        require(fragIndex >= 0) { "Fragment index must be non-negative: $fragIndex" }
    }
    
    operator fun plus(other: Int) = Cursor(fragIndex + other)
    override fun compareTo(other: Cursor) = fragIndex.compareTo(other.fragIndex)
}

data class Interval(val begin: Cursor, val end: Cursor) : Comparable<Interval> {
    init {
        require(begin <= end) { "Begin must be less than or equal to end: $begin, $end" }
    }
    
    fun isEmpty() = begin == end
    
    fun isNotEmpty() = begin < end
    
    override fun compareTo(other: Interval): Int {
        check(begin == other.begin) { "Begin must be equal: $begin, ${other.begin}" }
        return end.compareTo(other.end)
    }
}

sealed interface SourceFragment

data class PlainFragment(val line: Int, val column: Int, val text: String) : SourceFragment {
    init {
        require(line >= 0) { "Line number must be non-negative: $line" }
        require(column >= 0) { "Column must be non-negative: $column" }
        require(text.isNotEmpty()) { "Text must not be empty: $text" }
        require(text.none { it.isWhitespace() }) { "Text must not contain whitespace: $text" }
    }
    
    override fun toString(): String {
        return "line ${line + 1}, column ${column + 1}: $text"
    }
}

data class CharFragment(val line: Int, val column: Int, val char: Char) : SourceFragment {
    init {
        require(line >= 0) { "Line number must be non-negative: $line" }
        require(column >= 0) { "Column must be non-negative: $column" }
    }
    
    override fun toString(): String {
        return "line ${line + 1}, column ${column + 1}: '${
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
}

data class StringFragment(val line: Int, val column: Int, val string: String) : SourceFragment {
    init {
        require(line >= 0) { "Line number must be non-negative: $line" }
        require(column >= 0) { "Column must be non-negative: $column" }
    }
    
    override fun toString(): String {
        return "line ${line + 1}, column ${column + 1}: \"$string\""
    }
}

data class FormattedStringFragment(
    val line: Int,
    val column: Int,
    val isRaw: Boolean,
    val parts: List<FormattedStringPart>,
) : SourceFragment {
    init {
        require(line >= 0) { "Line number must be non-negative: $line" }
        require(column >= 0) { "Column must be non-negative: $column" }
    }
    
    override fun toString(): String {
        return "line ${line + 1}, column ${column + 1}: formatted string"
    }
}

sealed interface FormattedStringPart

data class FormattedTextPart(
    val text: String,
) : FormattedStringPart

data class FormattedExpressionPart(
    val source: String,
) : FormattedStringPart

data class FormattedStringTemplate(
    val isRaw: Boolean,
    val parts: List<FormattedStringPart>,
)

class SourceScanner(source: String) {
    
    var changed = false
    val fragments = compact(source)
    val parseQueue = ArrayDeque<Pair<Cursor, NonTerminal<*>>>()
    val parseQueueVisited = mutableSetOf<Pair<Cursor, NonTerminal<*>>>()
    val memoization = mutableMapOf<Cursor, MutableMap<NonTerminal<*>, ParseResult<*>>>()
    
    operator fun get(cursor: Cursor): SourceFragment? {
        return fragments.getOrNull(cursor.fragIndex)
    }
    
    fun memoize(result: ParseResult<*>) {
        memoization.getOrPut(result.interval.begin) { mutableMapOf() }.compute(result.nonTerminal) { _, oldResult ->
            if (oldResult == null || result > oldResult) {
                result.also { changed = true }
            } else {
                oldResult
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N> parse(cursor: Cursor, nonTerminal: NonTerminal<N>): Success<N>? {
        val result = memoization[cursor]?.get(nonTerminal) as? Success<N>?
        val next = cursor to nonTerminal
        if (parseQueueVisited.add(next)) {
            parseQueue.addLast(next)
        }
        return result
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <N> memoized(cursor: Cursor, nonTerminal: NonTerminal<N>): ParseResult<N>? {
        return memoization[cursor]?.get(nonTerminal) as ParseResult<N>?
    }
    
    fun memoizedAt(cursor: Cursor): Map<NonTerminal<*>, ParseResult<*>> {
        return memoization[cursor]?.toMap() ?: emptyMap()
    }
}

fun compact(source: String): List<SourceFragment> {
    val lines = source.lines()
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
    
    fun fragmentChar(line: Int, begin: Int, end: Int): CharFragment {
        val text = lines[line].substring(begin + 1, end - 1)
        when (text.length) {
            0 -> throw ParseException("Empty character literal")
            1 -> return CharFragment(line, begin, text[0])
            2 -> if (text[0] == '\\') {
                return when (text[1]) {
                    't' -> CharFragment(line, begin, '\t')
                    'b' -> CharFragment(line, begin, '\b')
                    'n' -> CharFragment(line, begin, '\n')
                    'r' -> CharFragment(line, begin, '\r')
                    '\'' -> CharFragment(line, begin, '\'')
                    '\\' -> CharFragment(line, begin, '\\')
                    else -> throw ParseException("Invalid escape sequence: $text")
                }
            }
            6 -> if (text[0] == '\\' && text[1] == 'u') {
                try {
                    return CharFragment(line, begin, text.substring(2, 6).toInt(16).toChar())
                } catch (_: NumberFormatException) {
                    throw ParseException("Invalid unicode escape sequence: $text")
                }
            }
        }
        throw ParseException("Invalid character literal: $text")
    }
    
    fun fragmentString(line: Int, begin: Int, end: Int): StringFragment {
        var i = 0
        val builder = StringBuilder()
        val text = lines[line].substring(begin + 1, end - 1)
        while (i < text.length) {
            val char = text[i++]
            if (char == '\\') {
                if (i >= text.length) {
                    throw ParseException("Unterminated escape sequence in string literal")
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
                            throw ParseException("Unterminated unicode escape sequence in string literal")
                        }
                        builder.append(lines[line].substring(i, i + 4).toInt(16).toChar())
                        i += 4
                    }
                    else -> throw ParseException("Invalid escape sequence in string literal")
                }
            } else {
                builder.append(char)
            }
        }
        return StringFragment(line, begin, builder.toString())
    }
    
    fun fragmentRawString(line: Int, begin: Int, end: Int): StringFragment {
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
        return StringFragment(line, begin, builder.toString())
    }
    
    fun fragmentFormattedString(
        line: Int,
        begin: Int,
        end: Int,
        prefixLength: Int,
        isRaw: Boolean,
    ): FormattedStringFragment {
        val text = lines[line].substring(begin + prefixLength, end - 1)
        val parts = mutableListOf<FormattedStringPart>()
        val builder = StringBuilder()
        var i = 0
        var literalBraceDepth = 0
        
        fun flushText() {
            if (builder.isNotEmpty()) {
                parts += FormattedTextPart(builder.toString())
                builder.setLength(0)
            }
        }
        
        while (i < text.length) {
            when (val char = text[i++]) {
                '\\' -> {
                    if (isRaw) {
                        if (i >= text.length) {
                            builder.append('\\')
                            continue
                        }
                        when (text[i]) {
                            '"' -> {
                                builder.append('"')
                                i++
                            }
                            '\\' -> {
                                if (i + 1 == text.length) {
                                    builder.append('\\')
                                    i++
                                } else {
                                    builder.append('\\')
                                }
                            }
                            '{' -> {
                                builder.append('{')
                                literalBraceDepth++
                                i++
                            }
                            '}' -> {
                                builder.append('}')
                                i++
                            }
                            else -> builder.append('\\')
                        }
                    } else {
                        if (i >= text.length) throw ParseException("Unterminated escape sequence in formatted string literal")
                        val nextChar = text[i++]
                        when (nextChar) {
                            't' -> builder.append('\t')
                            'b' -> builder.append('\b')
                            'n' -> builder.append('\n')
                            'r' -> builder.append('\r')
                            '\\' -> builder.append('\\')
                            '"' -> builder.append('"')
                            '{' -> {
                                builder.append('{')
                                literalBraceDepth++
                            }
                            '}' -> builder.append('}')
                            'u' -> {
                                if (i + 3 >= text.length) {
                                    throw ParseException("Unterminated unicode escape sequence in formatted string literal")
                                }
                                builder.append(text.substring(i, i + 4).toInt(16).toChar())
                                i += 4
                            }
                            else -> throw ParseException("Invalid escape sequence in formatted string literal")
                        }
                    }
                }
                
                '{' -> {
                    if (literalBraceDepth > 0) {
                        builder.append('{')
                        literalBraceDepth++
                        continue
                    }
                    flushText()
                    val endIndex = findFormattedExpressionEnd(text, i)
                    val expressionSource = text.substring(i, endIndex).trim()
                    if (expressionSource.isEmpty()) {
                        throw ParseException("Empty expression in formatted string literal")
                    }
                    parts += FormattedExpressionPart(expressionSource)
                    i = endIndex + 1
                }
                
                '}' -> {
                    if (literalBraceDepth > 0) {
                        builder.append('}')
                        literalBraceDepth--
                    } else {
                        throw ParseException("Unexpected '}' in formatted string literal")
                    }
                }
                else -> builder.append(char)
            }
        }
        
        if (literalBraceDepth != 0) {
            throw ParseException("Unterminated escaped brace section in formatted string literal")
        }
        flushText()
        return FormattedStringFragment(line, begin, isRaw, parts)
    }
    
    val result = mutableListOf<SourceFragment>()
    
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
                    throw ParseException("Unterminated comment starting at line ${startLine + 1}, column ${startColumn + 1}")
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
        
        if (lines[line].startsWith("rf\"", column) || lines[line].startsWith("fr\"", column)) {
            val begin = column
            val lineText = lines[line]
            val index = findClosingQuote(lineText, begin + 3)
                ?: throw ParseException("Unterminated raw formatted string at line ${line + 1}, column ${begin + 1}")
            result += fragmentFormattedString(line, begin, index + 1, prefixLength = 3, isRaw = true)
            move(index - column + 1)
            continue
        }

        if (lines[line].startsWith("r\"", column)) {
            val begin = column
            val lineText = lines[line]
            val index = findClosingQuote(lineText, begin + 2)
                ?: throw ParseException("Unterminated raw string at line ${line + 1}, column ${begin + 1}")
            result += fragmentRawString(line, begin, index + 1)
            move(index - column + 1)
            continue
        }
        
        if (lines[line].startsWith("f\"", column)) {
            val begin = column
            val lineText = lines[line]
            val index = findClosingQuote(lineText, begin + 2)
                ?: throw ParseException("Unterminated formatted string at line ${line + 1}, column ${begin + 1}")
            result += fragmentFormattedString(line, begin, index + 1, prefixLength = 2, isRaw = false)
            move(index - column + 1)
            continue
        }

        if (lines[line][column] == '\'') {
            val begin = column
            val lineText = lines[line]
            val index = findClosingQuote(lineText, begin + 1, '\'')
                ?: throw ParseException("Unterminated char at line ${line + 1}, column ${begin + 1}")
            result += fragmentChar(line, begin, index + 1)
            move(index - column + 1)
            continue
        }
        
        if (lines[line][column] == '"') {
            val begin = column
            val lineText = lines[line]
            val index = findClosingQuote(lineText, begin + 1)
                ?: throw ParseException("Unterminated string at line ${line + 1}, column ${begin + 1}")
            result += fragmentString(line, begin, index + 1)
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

private fun Char.isWordChar(): Boolean {
    return this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this == '_'
}

private fun findFormattedExpressionEnd(
    text: String,
    start: Int,
): Int {
    var index = start
    var braceDepth = 0
    var parenDepth = 0
    var bracketDepth = 0
    while (index < text.length) {
        when (text[index]) {
            '\'' -> index = skipQuoted(text, index, '\'')
            '"' -> index = skipQuoted(text, index, '"')
            '(' -> parenDepth++
            ')' -> if (parenDepth > 0) parenDepth--
            '[' -> bracketDepth++
            ']' -> if (bracketDepth > 0) bracketDepth--
            '{' -> braceDepth++
            '}' -> {
                if (braceDepth == 0 && parenDepth == 0 && bracketDepth == 0) return index
                if (braceDepth > 0) braceDepth--
            }
        }
        index++
    }
    throw ParseException("Unterminated expression in formatted string literal")
}

private fun skipQuoted(
    text: String,
    begin: Int,
    quote: Char,
): Int {
    var index = begin + 1
    while (index < text.length) {
        val char = text[index]
        if (char == '\\') {
            index += 2
            continue
        }
        if (char == quote) return index
        index++
    }
    throw ParseException("Unterminated quoted literal in formatted string expression")
}

private fun findClosingQuote(
    text: String,
    start: Int,
    quote: Char = '"',
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
