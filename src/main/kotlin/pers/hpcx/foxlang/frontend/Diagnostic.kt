package pers.hpcx.foxlang.frontend

import java.util.*

data class Expectation(
    val symbol: Symbol<*>,
    val span: SourceSpan,
    val costLimit: Int = Int.MAX_VALUE,
)

data class DiagnosticContext(
    val grammar: Grammar,
    val source: Source,
    val chart: ParseChart,
)

sealed interface SeedAction {
    
    data class Seed(
        val symbol: Symbol<*>,
        val span: SourceSpan,
        val reason: String,
        val cost: Int,
        val children: List<Expectation> = emptyList(),
    ) : SeedAction {
        
        fun toParseMatch(): ParseMatch<*> {
            return ParseMatch(
                symbol = symbol,
                rule = null,
                segments = listOf(span),
                origin = MatchOrigin.Expected(reason, cost, children),
            )
        }
    }
    
    data class Recurse(val expectation: Expectation) : SeedAction
}

data class DiagnosticResult(
    val root: Expectation,
    val chart: ParseChart,
    val exactBuildSucceeded: Boolean,
) {
    
    val recovered: Boolean get() = chart.matches(root.symbol, root.span).isNotEmpty()
}

class DiagnosticEngine(
    private val rules: List<DiagnosticRules>,
    private val maxActionsPerExpectation: Int = 16,
) {
    
    fun diagnose(root: Expectation, analysis: ParseAnalysis<*>): DiagnosticResult {
        return diagnose(root, analysis.exactChart, exactBuildSucceeded = analysis.value != null)
    }
    
    fun diagnose(root: Expectation, exactChart: ParseChart): DiagnosticResult {
        return diagnose(root, exactChart, exactBuildSucceeded = exactChart.build(root.symbol) != null)
    }
    
    private fun diagnose(
        root: Expectation,
        exactChart: ParseChart,
        exactBuildSucceeded: Boolean,
    ): DiagnosticResult {
        val chart = exactChart.forkForDiagnostics()
        val ctx = DiagnosticContext(chart.grammar, chart.source, chart)
        val queue = PriorityQueue(compareBy<QueuedExpectation> { it.cost }.thenBy { it.order })
        val processed = mutableSetOf<Expectation>()
        var order = 0L
        
        fun enqueue(expectation: Expectation, cost: Int = 0) {
            if (cost <= expectation.costLimit) {
                queue += QueuedExpectation(expectation, cost, order++)
            }
        }
        
        enqueue(root)
        while (queue.isNotEmpty()) {
            val current = queue.poll()
            val expectation = current.expectation
            if (!processed.add(expectation)) continue
            val existingMatches = chart.matches(expectation.symbol, expectation.span)
            if (existingMatches.isNotEmpty()) {
                var hasSynthetic = false
                existingMatches.forEach { match ->
                    val origin = match.origin
                    if (origin is MatchOrigin.Expected) {
                        hasSynthetic = true
                        origin.children.forEach { child -> enqueue(child, current.cost + origin.cost) }
                    } else if (origin is MatchOrigin.Derived && origin.fromSynthetic) {
                        hasSynthetic = true
                    }
                }
                if (!hasSynthetic) continue
            }
            
            rules.asSequence()
                .filter { it.supports(expectation.symbol) }
                .flatMap { it.propose(ctx, expectation) }
                .take(maxActionsPerExpectation)
                .forEach { action ->
                    when (action) {
                        is SeedAction.Seed -> {
                            if (action.cost <= expectation.costLimit && chart.seed(action.toParseMatch())) {
                                action.children.forEach { child ->
                                    enqueue(child, current.cost + action.cost)
                                }
                            }
                        }
                        is SeedAction.Recurse -> enqueue(action.expectation, current.cost + 1)
                    }
                }
            chart.grow()
        }
        
        return DiagnosticResult(root, chart, exactBuildSucceeded)
    }
    
    private data class QueuedExpectation(
        val expectation: Expectation,
        val cost: Int,
        val order: Long,
    )
}
