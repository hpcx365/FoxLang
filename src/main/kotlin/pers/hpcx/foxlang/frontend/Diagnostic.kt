package pers.hpcx.foxlang.frontend

data class DiagnosticScore(
    val repairCost: Long,
    val maxHoleWidth: Long,
    val totalHoleWidth2: Long,
    val abstractionPenalty: Long,
    val expectedCount: Long,
    val negativeMinDepth: Long,
    val negativeDepthSum: Long,
    val treeSize: Long,
    val leftmost: Long,
) : Comparable<DiagnosticScore> {
    
    override fun compareTo(other: DiagnosticScore) = comparator.compare(this, other)
    
    operator fun plus(other: DiagnosticScore): DiagnosticScore {
        val combinedExpectedCount = expectedCount + other.expectedCount
        val combinedNegativeMinDepth = when {
            expectedCount == 0L -> other.negativeMinDepth
            other.expectedCount == 0L -> negativeMinDepth
            else -> minOf(negativeMinDepth, other.negativeMinDepth)
        }
        return DiagnosticScore(
            repairCost = repairCost + other.repairCost,
            maxHoleWidth = maxOf(maxHoleWidth, other.maxHoleWidth),
            totalHoleWidth2 = totalHoleWidth2 + other.totalHoleWidth2,
            abstractionPenalty = abstractionPenalty + other.abstractionPenalty,
            expectedCount = combinedExpectedCount,
            negativeMinDepth = if (combinedExpectedCount == 0L) 0 else combinedNegativeMinDepth,
            negativeDepthSum = negativeDepthSum + other.negativeDepthSum,
            treeSize = treeSize + other.treeSize,
            leftmost = minOf(leftmost, other.leftmost),
        )
    }
    
    fun nested(): DiagnosticScore {
        if (expectedCount == 0L) return this
        return copy(
            negativeMinDepth = negativeMinDepth - 1,
            negativeDepthSum = negativeDepthSum - expectedCount,
        )
    }
    
    companion object {
        val Zero = DiagnosticScore(
            repairCost = 0,
            maxHoleWidth = 0,
            totalHoleWidth2 = 0,
            abstractionPenalty = 0,
            expectedCount = 0,
            negativeMinDepth = 0,
            negativeDepthSum = 0,
            treeSize = 0,
            leftmost = Long.MAX_VALUE,
        )
        
        val Node = Zero.copy(treeSize = 1)
        
        private val comparator = compareBy<DiagnosticScore> { it.repairCost }
            .thenBy { it.maxHoleWidth }
            .thenBy { it.totalHoleWidth2 }
            .thenBy { it.abstractionPenalty }
            .thenBy { it.expectedCount }
            .thenBy { it.negativeMinDepth }
            .thenBy { it.negativeDepthSum }
            .thenBy { it.treeSize }
            .thenBy { it.leftmost }
    }
}

interface DiagnosticScoringStrategy {
    
    fun expectedScore(
        symbol: Symbol<*>,
        span: SourceSpan,
        source: Source,
    ): DiagnosticScore?
    
    fun rulePenalty(
        match: ParseMatch<*>,
        rule: GrammarRule.MatchSymbols<*>,
        source: Source,
    ): DiagnosticScore = DiagnosticScore.Zero
}

data class DiagnosticTree(
    val match: ParseMatch<*>,
    val children: List<DiagnosticTree>,
    val score: DiagnosticScore,
) : Comparable<DiagnosticTree> {
    
    fun expectedLeaves(): List<DiagnosticTree> {
        if (match.matchType == ParseMatchType.Expected) return listOf(this)
        return children.flatMap { it.expectedLeaves() }
    }
    
    override fun compareTo(other: DiagnosticTree) = comparator.compare(this, other)
    
    private companion object {
        val comparator = compareBy<DiagnosticTree> { it.score }
            .thenBy { it.match.span.start.fragIndex }
            .thenBy { it.match.span.end.fragIndex }
            .thenBy { it.match.symbol.toString() }
    }
}
