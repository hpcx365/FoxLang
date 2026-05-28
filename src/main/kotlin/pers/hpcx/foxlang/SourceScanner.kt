package pers.hpcx.foxlang

val REGEX_BLANKS = Regex("\\s+")

data class Cursor(val row: Int, val col: Int) {
    init {
        check(row >= 0)
        check(col >= 0)
    }
    
    override fun toString(): String {
        return "line $row, column $col"
    }
}

class SourceScanner(source: String) {
    val lines = source.lines()
    
    var cursor = Cursor(0, 0)
        private set
    
    private fun internalMove(numChars: Int) {
        check(numChars >= 0)
        check(cursor.row < lines.size)
        val line = lines[cursor.row]
        val col = cursor.col + numChars
        check(col <= line.length)
        cursor = (if (col < line.length) Cursor(cursor.row, col) else Cursor(cursor.row + 1, 0))
    }
    
    private fun internalMatch(string: String): Boolean {
        return cursor.row < lines.size && lines[cursor.row].startsWith(string, cursor.col)
    }
    
    private fun internalMatchRegex(regex: Regex): String? {
        if (cursor.row >= lines.size) return null
        val line = lines[cursor.row]
        val match = regex.matchAt(line, cursor.col)
        return match?.value
    }
    
    private fun skipBlanksAndComments() {
        while (cursor.row < lines.size) {
            if (cursor.col >= lines[cursor.row].length) {
                internalMove(0)
                continue
            }
            internalMatchRegex(REGEX_BLANKS)?.let {
                internalMove(it.length)
                continue
            }
            if (internalMatch("//")) {
                internalMove(lines[cursor.row].length - cursor.col)
                continue
            }
            if (internalMatch("/*")) {
                internalMove(2)
                while (cursor.row < lines.size) {
                    val index = lines[cursor.row].indexOf("*/", cursor.col)
                    if (index >= 0) {
                        internalMove(index - cursor.col + 2)
                        break
                    }
                    internalMove(lines[cursor.row].length - cursor.col)
                }
                continue
            }
            return
        }
    }
    
    fun isEnded(): Boolean {
        skipBlanksAndComments()
        return cursor.row >= lines.size
    }
    
    fun tryConsume(string: String): Boolean {
        skipBlanksAndComments()
        if (internalMatch(string)) {
            internalMove(string.length)
            return true
        }
        return false
    }
    
    fun tryConsumeRegex(regex: Regex): String? {
        skipBlanksAndComments()
        val match = internalMatchRegex(regex)
        if (match != null) {
            internalMove(match.length)
            return match
        }
        return null
    }
    
    private val cursorStack = mutableListOf<Cursor>()
    
    fun saveCursor() {
        skipBlanksAndComments()
        cursorStack.add(cursor)
    }
    
    fun restoreCursor() {
        cursor = cursorStack.removeLast()
    }
    
    fun discardCursor() {
        cursorStack.removeLast()
    }
    
    inline fun <N> attempt(factory: () -> ParseResult<N>): ParseResult<N> {
        saveCursor()
        val result = factory()
        when (result) {
            is Success -> discardCursor()
            is Failure -> restoreCursor()
        }
        return result
    }
    
    private val memoization = mutableMapOf<Pair<Parser<*>, Cursor>, ParseResult<*>?>()
    
    @Suppress("UNCHECKED_CAST")
    fun <N> memoize(parser: Parser<N>): ParseResult<N> {
        val from = cursor
        memoization[parser to from]?.let { return it as ParseResult<N> }
        val result = parser.parse(this)
        memoization[parser to from] = result
        return result
    }
}
