package pers.hpcx.foxlang.frontend.fox

import pers.hpcx.foxlang.ast.FoxFile
import pers.hpcx.foxlang.frontend.common.*

data class FoxSymbolDiagnosticProfile(
    val label: String,
    val insertCost: Long,
    val holeBaseCost: Long,
    val holePerFragmentCost: Long,
    val abstractionPenalty: Long,
    val reportable: Boolean = true,
)

object FoxDiagnosticScoringStrategy : DiagnosticScoringStrategy {
    
    private val delimiterToken = profile("delimiter", 10, 24, 4, 0)
    private val operatorToken = profile("operator", 12, 28, 5, 2)
    private val keywordToken = profile("keyword", 16, 36, 6, 5)
    private val lexical = profile("identifier", 28, 42, 8, 10)
    private val literal = profile("literal", 36, 50, 9, 14)
    private val operator = profile("operator", 24, 40, 8, 12)
    private val typedItem = profile("typed item", 55, 72, 12, 35)
    private val list = profile("list", 75, 94, 16, 50)
    private val expression = profile("expression", 85, 110, 18, 60)
    private val statement = profile("statement", 100, 130, 20, 75)
    private val declaration = profile("declaration", 120, 155, 24, 85)
    private val root = profile("file", 180, 230, 32, 110, reportable = false)
    private val fallback = profile("syntax", 150, 190, 28, 95)
    
    private val profiles: Map<GrammarSymbol<*>, FoxSymbolDiagnosticProfile> = buildMap {
        
        fun register(symbol: GrammarSymbol<*>, profile: FoxSymbolDiagnosticProfile) {
            put(symbol, profile)
        }
        
        fun registerAll(symbols: Iterable<GrammarSymbol<*>>, profile: FoxSymbolDiagnosticProfile) {
            symbols.forEach { register(it, profile) }
        }
        
        Keywords.forEach { (string, symbol) -> register(symbol, keywordToken.copy(label = string)) }
        Punctuations.forEach { (string, symbol) -> register(symbol, delimiterToken.copy(label = string)) }
        Operators.forEach { (string, symbol) -> register(symbol, operatorToken.copy(label = string)) }
        
        registerAll(FoxLexicalSymbols, lexical)
        registerAll(FoxDiagnosticDelimiterSymbols, delimiterToken)
        registerAll(FoxLiteralSymbols, literal)
        registerAll(FoxCommaListItemSymbols, typedItem)
        registerAll(FoxListSymbols, list)
        registerAll(FoxExpressionSymbols, expression)
        registerAll(FoxStatementSymbols, statement)
        registerAll(FoxOperatorSymbols, operator)
        registerAll(FoxDeclarationSymbols, declaration)
        
        register(File, root)
    }
    
    fun profile(symbol: GrammarSymbol<*>): FoxSymbolDiagnosticProfile {
        return profiles[symbol] ?: fallback.copy(label = symbol.toString())
    }
    
    override fun expectedScore(symbol: GrammarSymbol<*>, span: SourceSpan, source: Source<*>): DiagnosticScore {
        val profile = profile(symbol)
        val width = span.length.toLong()
        val repairCost = if (span.isEmpty()) {
            profile.insertCost
        } else {
            profile.holeBaseCost + profile.holePerFragmentCost * width
        }
        return DiagnosticScore(
            repairCost = repairCost,
            maxHoleWidth = width,
            totalHoleWidth2 = width * width,
            abstractionPenalty = profile.abstractionPenalty,
            expectedCount = 1,
            negativeMinDepth = 0,
            negativeDepthSum = 0,
            treeSize = 1,
            leftmost = span.start.fragIndex.toLong(),
        )
    }
    
    private fun profile(
        label: String,
        insertCost: Long,
        holeBaseCost: Long,
        holePerFragmentCost: Long,
        abstractionPenalty: Long,
        reportable: Boolean = true,
    ) = FoxSymbolDiagnosticProfile(
        label = label,
        insertCost = insertCost,
        holeBaseCost = holeBaseCost,
        holePerFragmentCost = holePerFragmentCost,
        abstractionPenalty = abstractionPenalty,
        reportable = reportable,
    )
}

fun ParseAnalysis<FoxFile>.diagnoseFox(): DiagnosticTree? {
    return context.best(start, FoxDiagnosticScoringStrategy)
}
