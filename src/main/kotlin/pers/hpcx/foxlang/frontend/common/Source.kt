package pers.hpcx.foxlang.frontend.common

data class Source<U>(
    val text: String,
    val fragments: List<U>,
) : Iterable<U> {
    
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
