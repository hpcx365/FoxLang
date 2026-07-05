package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.frontend.*

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
    
    private val profiles: Map<Symbol<*>, FoxSymbolDiagnosticProfile> = buildMap {
        fun register(symbol: Symbol<*>, profile: FoxSymbolDiagnosticProfile) {
            put(symbol, profile.copy(label = symbolLabel(symbol, profile.label)))
        }
        
        ReservedKeywords.forEach { register(token(it), keywordToken.copy(label = it)) }
        listOf(
            "(", ")", "[", "]", "{", "}", ".", ":", ";", ",",
            "<", ">", "->",
        ).forEach { register(token(it), delimiterToken.copy(label = it)) }
        listOf(
            "+", "-", "*", "/", "%", "`", "~", "?", "!", "@", "#", "$", "&", "|", "^",
            "==", "!=", "<=", ">=", "&&", "||", "<<", ">>", ">>>",
            "=", ":=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=", "&&=", "||=",
        ).forEach { register(token(it), operatorToken.copy(label = it)) }
        
        listOf(
            Identifier,
            TypeName,
            IdentifierEqual,
            IdentifierColon,
            TypeNameEqual,
            TypeNameColon,
            Label,
        ).forEach { register(it, lexical) }
        
        listOf(
            LineBreak,
            FormattedStringStart,
            FormattedStringEnd,
            FormattedExpressionOpen,
            FormattedExpressionClose,
            Dot,
            BlockOpen,
            BlockClose,
            ParenOpen,
            ParenClose,
            AngleOpen,
            AngleClose,
            Comma,
            Arrow,
            ElseKeyword,
            DoWhileKeyword,
        ).forEach { register(it, delimiterToken) }
        
        listOf(
            BinInt,
            DecInt,
            HexInt,
            BinLong,
            DecLong,
            HexLong,
            DecFloat,
            HexFloat,
            DecDouble,
            HexDouble,
            node<Unit>(),
            node<Boolean>(),
            node<Char>(),
            node<String>(),
            node<Int>(),
            node<Long>(),
            node<Float>(),
            node<Double>(),
        ).forEach { register(it, literal) }
        
        listOf(
            FormalParameter,
            ActualParameter,
            LambdaParameter,
            FormalGenericParameter,
            FormalGenericParameterNoConstraints,
            ActualGenericParameter,
            NamedActualGenericParameter,
            AnonymousActualGenericParameter,
            TupleComponentParameter,
            StructFieldParameter,
            StructFieldName,
            ObjectMemberParameter,
            ObjectMemberName,
            EnumItemParameter,
            EnumItemName,
            MethodTypeArgument,
        ).forEach { register(it, typedItem) }
        
        listOf(
            FormalParameterListHead,
            FormalParameterList,
            ActualParameterListHead,
            ActualParameterList,
            LambdaParameterListHead,
            LambdaParameterList,
            FormalGenericParameterListHead,
            FormalGenericParameterList,
            FormalGenericParameterNoConstraintsListHead,
            FormalGenericParameterNoConstraintsList,
            ActualGenericParameterListHead,
            ActualGenericParameterList,
            NamedActualGenericParameterListHead,
            NamedActualGenericParameterList,
            AnonymousActualGenericParameterListHead,
            AnonymousActualGenericParameterList,
            TupleComponentParameterListHead,
            TupleComponentParameterList,
            StructFieldParameterListHead,
            StructFieldParameterList,
            StructFieldNameListHead,
            StructFieldNameList,
            ObjectMemberParameterListHead,
            ObjectMemberParameterList,
            ObjectMemberNameListHead,
            ObjectMemberNameList,
            EnumItemParameterListHead,
            EnumItemParameterList,
            EnumItemNameListHead,
            EnumItemNameList,
            MethodTypeArgumentListHead,
            MethodTypeArgumentList,
            FormattedStringPartListHead,
            LambdaStatementBlockHead,
            WhenCaseConditionListHead,
            WhenCaseConditionList,
            WhenCaseListHead,
            WhenCaseList,
            FileElementList,
        ).forEach { register(it, list) }
        
        listOf(
            PrimaryExpression,
            PostfixExpression,
            UnaryExpression,
            MultiplicativeExpression,
            AdditiveExpression,
            ShiftExpression,
            ComparisonExpression,
            EqualityExpression,
            BitAndExpression,
            BitXorExpression,
            BitOrExpression,
            LogicalAndExpression,
            LogicalOrExpression,
            AssignableExpression,
            AssignmentExpression,
            ParenthesizedStatement,
            ControlBody,
            FormattedStringPart,
            LambdaBody,
            ExplicitLambdaLiteral,
            InlineImplicitLambdaLiteral,
            ImplicitLambdaLiteral,
            LambdaLiteral,
            SingleLineStatementBlock,
            node<FoxEntityStatement>(),
            node<FoxStatement>(),
            node<FoxType>(),
        ).forEach { register(it, expression) }
        
        listOf(
            StatementLine,
            StatementBlockHead,
            StatementBlock,
            node<FoxBlock>(),
            WhenCase,
            WhenCaseLine,
            IfCore,
            WhileCore,
            DoWhileCore,
            WhenCore,
        ).forEach { register(it, statement) }
        
        listOf(
            MultiplicativeOperator,
            AdditiveOperator,
            ShiftOperator,
            ComparisonOperator,
            EqualityOperator,
            BitAndOperator,
            BitXorOperator,
            BitOrOperator,
            LogicalAndOperator,
            LogicalOrOperator,
            node<FoxUnaryOperator>(),
            node<FoxBinaryOperator>(),
            node<FoxAssignOperator>(),
        ).forEach { register(it, operator) }
        
        listOf(
            FileElementLine,
            ThisTypeQualifier,
            ReturnTypeClause,
            MethodHead,
            node<FoxFileElement>(),
            node<FoxTypeAlias>(),
            node<FoxMethodDefinition>(),
        ).forEach { register(it, declaration) }
        
        register(node<FoxFile>(), root)
    }
    
    fun profile(symbol: Symbol<*>): FoxSymbolDiagnosticProfile {
        return profiles[symbol] ?: fallback.copy(label = symbol.toString())
    }
    
    override fun expectedScore(
        symbol: Symbol<*>,
        span: SourceSpan,
        source: Source,
    ): DiagnosticScore {
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
    
    private fun symbolLabel(symbol: Symbol<*>, fallback: String): String {
        return when (symbol) {
            is NamedSymbol<*> -> symbol.name
            else -> fallback
        }
    }
}

fun ParseAnalysis<FoxFile>.diagnoseFox(): DiagnosticTree? {
    return context.best(start, FoxDiagnosticScoringStrategy)
}
