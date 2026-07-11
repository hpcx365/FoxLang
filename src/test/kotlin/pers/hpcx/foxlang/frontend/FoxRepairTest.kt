package pers.hpcx.foxlang.frontend

import pers.hpcx.foxlang.ast.ParsedFoxFile
import pers.hpcx.foxlang.frontend.common.*
import pers.hpcx.foxlang.frontend.fox.*
import pers.hpcx.foxlang.frontend.fox.FoxBracketSymbol.AngleClose
import pers.hpcx.foxlang.frontend.fox.FoxTerminalSymbol.Comma
import kotlin.test.Test
import kotlin.test.assertTrue

class FoxRepairTest {
    
    @Test
    fun repairFormalParameterListMissingComma() {
        val analysis = "(a: Int b: Int)".analyzeFoxOrThrow()
        
        analysis.repairFormalParameterList()
        
        val bPosition = analysis.source.plainPosition("b")
        val missingCommaSpan = SourceSpan(bPosition, bPosition)
        assertTrue(
            analysis.context.matches(Comma, missingCommaSpan).any { it.matchType == ParseMatchType.Expected },
        )
        assertTrue(
            analysis.context.matches(FormalParameterList, analysis.source.span).any {
                it.matchType == ParseMatchType.Synthetic
            },
        )
    }
    
    @Test
    fun repairSingleGenericTypeMissingClose() {
        val analysis = "Array<Int".analyzeFoxOrThrow()
        
        analysis.repairType()
        
        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
        assertTrue(
            analysis.context.matches(AngleClose, closeSpan).any { it.matchType == ParseMatchType.Expected },
        )
        assertTrue(
            analysis.context.matches(Type, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
        )
    }
    
    @Test
    fun repairIfMissingThenBody() {
        val analysis = "if (true)".analyzeFoxOrThrow()
        
        analysis.repairStatement()
        
        val bodySpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
        assertTrue(
            analysis.context.matches(Statement, bodySpan).any { it.matchType == ParseMatchType.Expected },
        )
        assertTrue(
            analysis.context.matches(IfCore, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
        )
    }

//    @Test
//    fun repairMissingFormalParameterComma() {
//        val source = """
//def foo(a: Int b: Int) {
//    return a
//}
//""".trimStart()
//        val analysis = source.analyzeFoxOrThrow()
//        assertTrue(assertIs<ParseContextBuildFailure>(analysis.buildResult).errors.isEmpty())
//
//        analysis.repairFox()
//
//        val bPosition = analysis.source.plainPosition("b")
//        val missingCommaSpan = SourceSpan(bPosition, bPosition)
//        assertTrue(
//            analysis.context.matches(Comma, missingCommaSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//
//        val parameterListSpan = analysis.source.spanBetweenPlain("(", ")")
//        assertTrue(
//            analysis.context.matches(FormalParameterList, parameterListSpan).any { it.matchType == ParseMatchType.Synthetic },
//        )
//        assertTrue(
//            analysis.context.matches(File, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairFormalParameterListMissingClose() {
//        val analysis = "(a: Int, b: Int".analyzeFoxOrThrow()
//
//        analysis.repairFormalParameterList()
//
//        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(ParenClose, closeSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(FormalParameterList, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairFormalParameterListMissingOpen() {
//        val analysis = "a: Int, b: Int)".analyzeFoxOrThrow()
//
//        analysis.repairFormalParameterList()
//
//        val openSpan = SourceSpan(analysis.source.span.start, analysis.source.span.start)
//        assertTrue(
//            analysis.context.matches(ParenOpen, openSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(FormalParameterList, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairFormalParameterListEmptySegmentBetweenCommas() {
//        val analysis = "(a: Int,, b: Int)".analyzeFoxOrThrow()
//
//        analysis.repairFormalParameterList()
//
//        val secondComma = analysis.source.plainPositions(",")[1]
//        val missingItemSpan = SourceSpan(secondComma, secondComma)
//        assertTrue(
//            analysis.context.matches(FormalParameter, missingItemSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(FormalParameterList, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairFormalParameterListMissingCommaAfterNestedGeneric() {
//        val analysis = "(a: AnyOf<Int, String> b: Int)".analyzeFoxOrThrow()
//
//        analysis.repairFormalParameterList()
//
//        val bPosition = analysis.source.plainPosition("b")
//        val missingCommaSpan = SourceSpan(bPosition, bPosition)
//        assertTrue(
//            analysis.context.matches(Comma, missingCommaSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(FormalParameterList, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairTypeArgumentListMissingClose() {
//        val analysis = "Array<Int".analyzeFoxOrThrow()
//
//        analysis.repairType()
//
//        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(AngleClose, closeSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(Type, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairFixedArityTypeMissingComma() {
//        val analysis = "GetComponent<Int 0>".analyzeFoxOrThrow()
//
//        analysis.repairType()
//
//        val zeroPosition = analysis.source.plainPosition("0")
//        val missingCommaSpan = SourceSpan(zeroPosition, zeroPosition)
//        assertTrue(
//            analysis.context.matches(Comma, missingCommaSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(Type, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairUnresolvedGenericTypeMissingClose() {
//        val analysis = "Box<Int".analyzeFoxOrThrow()
//
//        analysis.repairType()
//
//        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(AngleClose, closeSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(Type, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairStructFieldNameListMissingComma() {
//        val analysis = "a b".analyzeFoxOrThrow()
//
//        analysis.repairStructFieldNameList()
//
//        val bPosition = analysis.source.plainPosition("b")
//        val missingCommaSpan = SourceSpan(bPosition, bPosition)
//        assertTrue(
//            analysis.context.matches(Comma, missingCommaSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(StructFieldNameList, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairTypeStructFieldNameListMissingComma() {
//        val analysis = "FieldsOf<Int, a b>".analyzeFoxOrThrow()
//
//        analysis.repairType()
//
//        val bPosition = analysis.source.plainPosition("b")
//        val missingCommaSpan = SourceSpan(bPosition, bPosition)
//        assertTrue(
//            analysis.context.matches(Comma, missingCommaSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(Type, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairExplicitLambdaParameterListMissingComma() {
//        val analysis = "{ x y -> x }".analyzeFoxOrThrow()
//
//        analysis.repairLambdaLiteral()
//
//        val yPosition = analysis.source.plainPosition("y")
//        val missingCommaSpan = SourceSpan(yPosition, yPosition)
//        assertTrue(
//            analysis.context.matches(Comma, missingCommaSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(LambdaLiteral, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairExplicitLambdaMissingClose() {
//        val analysis = "{ x -> x".analyzeFoxOrThrow()
//
//        analysis.repairLambdaLiteral()
//
//        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(RBrace, closeSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(LambdaLiteral, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairImplicitLambdaMissingClose() {
//        val analysis = "{ x".analyzeFoxOrThrow()
//
//        analysis.repairLambdaLiteral()
//
//        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(RBrace, closeSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(LambdaLiteral, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairTypeBindingMissingColon() {
//        val analysis = "value Int".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val typePosition = analysis.source.plainPosition("Int")
//        val missingColonSpan = SourceSpan(typePosition, typePosition)
//        assertTrue(
//            analysis.context.matches(Colon, missingColonSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairTypeBindingMissingType() {
//        val analysis = "value:".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val typeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(Type, typeSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairAdditiveExpressionMissingRightOperand() {
//        val analysis = "a +".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val missingOperandSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(MultiplicativeExpression, missingOperandSpan).any {
//                it.matchType == ParseMatchType.Expected
//            },
//        )
//        assertTrue(
//            analysis.context.matches(AdditiveExpression, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairMultiplicativeExpressionMissingLeftOperand() {
//        val analysis = "* b".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val operatorPosition = analysis.source.plainPosition("*")
//        val missingOperandSpan = SourceSpan(operatorPosition, operatorPosition)
//        assertTrue(
//            analysis.context.matches(UnaryExpression, missingOperandSpan).any {
//                it.matchType == ParseMatchType.Expected
//            },
//        )
//        assertTrue(
//            analysis.context.matches(MultiplicativeExpression, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairNestedBinaryExpressionMissingOperand() {
//        val analysis = "a + * b".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val operatorPosition = analysis.source.plainPosition("*")
//        val missingOperandSpan = SourceSpan(operatorPosition, operatorPosition)
//        assertTrue(
//            analysis.context.matches(UnaryExpression, missingOperandSpan).any {
//                it.matchType == ParseMatchType.Expected
//            },
//        )
//        assertTrue(
//            analysis.context.matches(MultiplicativeExpression, analysis.source.spanBetweenPlain("*", "b")).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//        assertTrue(
//            analysis.context.matches(AdditiveExpression, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//    }
//
//    @Test
//    fun repairPostfixExpressionMissingMemberAfterDot() {
//        val analysis = "obj.".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val missingMemberSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(Identifier, missingMemberSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(PostfixExpression, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairPostfixExpressionMissingIndexClose() {
//        val analysis = "tuple[0".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(SquareClose, closeSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(PostfixExpression, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//    }
//
//    @Test
//    fun repairPostfixExpressionMissingIndexValue() {
//        val analysis = "tuple[]".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val closePosition = analysis.source.plainPosition("]")
//        val missingIndexSpan = SourceSpan(closePosition, closePosition)
//        assertTrue(
//            analysis.context.matches(Statement, missingIndexSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(PostfixExpression, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//    }
//
//    @Test
//    fun repairCallMissingCloseParen() {
//        val analysis = "foo(".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(ParenClose, closeSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(ActualParameterList, analysis.source.spanFromPlain("(")).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//        assertTrue(
//            analysis.context.matches(PostfixExpression, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//    }
//
//    @Test
//    fun repairCallMissingParameterComma() {
//        val analysis = "foo(a b)".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val bPosition = analysis.source.plainPosition("b")
//        val missingCommaSpan = SourceSpan(bPosition, bPosition)
//        assertTrue(
//            analysis.context.matches(Comma, missingCommaSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(PostfixExpression, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//    }
//
//    @Test
//    fun repairConstructMissingCloseParen() {
//        val analysis = "Int(".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val closeSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(ParenClose, closeSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(Type, analysis.source.spanUntilPlain("(")).any { it.matchType.isBuildable() },
//        )
//        assertTrue(
//            analysis.context.matches(PostfixExpression, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//    }
//
//    @Test
//    fun repairNamedActualParameterMissingValue() {
//        val analysis = "foo(name=)".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val closePosition = analysis.source.plainPosition(")")
//        val missingValueSpan = SourceSpan(closePosition, closePosition)
//        assertTrue(
//            analysis.context.matches(LogicalOrExpression, missingValueSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(ActualParameterList, analysis.source.spanBetweenPlain("(", ")")).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//    }
//
//    @Test
//    fun repairLabelledStatementMissingLabelName() {
//        val analysis = "# if (true) {}".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val hashPosition = analysis.source.plainPosition("#")
//        val missingNameSpan = SourceSpan(hashPosition + 1, hashPosition + 1)
//        assertTrue(
//            analysis.context.matches(Identifier, missingNameSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairBreakMissingLabelName() {
//        val analysis = "break #".analyzeFoxOrThrow()
//
//        analysis.repairStatement()
//
//        val missingNameSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(Identifier, missingNameSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
//
//    @Test
//    fun repairIfMissingThenBody() {
//        val analysis = "if (true)".analyzeFoxOrThrow()
//        assertTrue(assertIs<ParseContextBuildFailure>(analysis.buildResult).errors.isEmpty())
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).none { it.matchType.isBuildable() },
//        )
//
//        analysis.repairStatement()
//
//        val bodySpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(ControlBody, bodySpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(Statement, bodySpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(IfCore, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//    }
//
//    @Test
//    fun repairIfMissingElseBody() {
//        val analysis = "if (true) {} else".analyzeFoxOrThrow()
//        assertTrue(assertIs<ParseContextBuildFailure>(analysis.buildResult).errors.isEmpty())
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).none { it.matchType.isBuildable() },
//        )
//
//        analysis.repairStatement()
//
//        val bodySpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(ControlBody, bodySpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(IfCore, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//    }
//
//    @Test
//    fun repairWhileMissingBody() {
//        val analysis = "while (true)".analyzeFoxOrThrow()
//        assertTrue(assertIs<ParseContextBuildFailure>(analysis.buildResult).errors.isEmpty())
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).none { it.matchType.isBuildable() },
//        )
//
//        analysis.repairStatement()
//
//        val bodySpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(ControlBody, bodySpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(WhileCore, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//    }
//
//    @Test
//    fun repairDoWhileMissingWhileCondition() {
//        val analysis = "do {}".analyzeFoxOrThrow()
//        assertTrue(assertIs<ParseContextBuildFailure>(analysis.buildResult).errors.isEmpty())
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).none { it.matchType.isBuildable() },
//        )
//
//        analysis.repairStatement()
//
//        val tailSpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(KwWhile, tailSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(ParenthesizedExpression, tailSpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(DoWhileCore, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//        assertTrue(
//            analysis.context.matches(Statement, analysis.source.span).any {
//                it.matchType == ParseMatchType.Synthetic
//            },
//        )
//    }
//
//    @Test
//    fun repairWhenCaseMissingBody() {
//        val analysis = "else ->".analyzeFoxOrThrow()
//
//        analysis.repairWhenCase()
//
//        val bodySpan = SourceSpan(analysis.source.span.end, analysis.source.span.end)
//        assertTrue(
//            analysis.context.matches(ControlBody, bodySpan).any { it.matchType == ParseMatchType.Expected },
//        )
//        assertTrue(
//            analysis.context.matches(WhenCase, analysis.source.span).any { it.matchType == ParseMatchType.Synthetic },
//        )
//    }
}

private fun ParseAnalysis<ParsedFoxFile>.repairFormalParameterList() {
    context.repair(Expectation(FormalParameterList, source.span), FoxRepairStrategy)
}

private fun ParseAnalysis<ParsedFoxFile>.repairType() {
    context.repair(Expectation(Type, source.span), FoxRepairStrategy)
}

private fun ParseAnalysis<ParsedFoxFile>.repairStructFieldNameList() {
    context.repair(Expectation(StructFieldNameList, source.span), FoxRepairStrategy)
}

private fun ParseAnalysis<ParsedFoxFile>.repairLambdaLiteral() {
    context.repair(Expectation(LambdaLiteral, source.span), FoxRepairStrategy)
}

private fun ParseAnalysis<ParsedFoxFile>.repairStatement() {
    context.repair(Expectation(Statement, source.span), FoxRepairStrategy)
}

private fun ParseAnalysis<ParsedFoxFile>.repairWhenCase() {
    context.repair(Expectation(WhenCase, source.span), FoxRepairStrategy)
}

private fun String.analyzeFoxOrThrow(): ParseAnalysis<ParsedFoxFile> {
    return when (val result = sourceFox()) {
        is SourceFragmentationSuccess -> (result.value).analyzeFox()
        is SourceFragmentationFailure -> error(result.errors.joinToString { it })
    }
}

private fun Source<*>.plainPosition(text: String): SourcePosition {
    val index = fragments.indexOfFirst { it is PlainFragment && it.text == text }
    require(index >= 0) { "Plain fragment not found: $text" }
    return SourcePosition(index)
}

private fun Source<*>.plainPositions(text: String): List<SourcePosition> {
    return fragments.mapIndexedNotNull { index, fragment ->
        if (fragment is PlainFragment && fragment.text == text) SourcePosition(index) else null
    }
}

private fun Source<*>.spanBetweenPlain(open: String, close: String): SourceSpan {
    val openIndex = fragments.indexOfFirst { it is PlainFragment && it.text == open }
    val closeIndex = fragments.indices.firstOrNull { index ->
        index > openIndex && fragments[index] is PlainFragment && (fragments[index] as PlainFragment).text == close
    } ?: -1
    require(openIndex >= 0) { "Open fragment not found: $open" }
    require(closeIndex >= 0) { "Close fragment not found: $close" }
    return SourceSpan(SourcePosition(openIndex), SourcePosition(closeIndex + 1))
}

private fun Source<*>.spanFromPlain(open: String): SourceSpan {
    val openIndex = fragments.indexOfFirst { it is PlainFragment && it.text == open }
    require(openIndex >= 0) { "Open fragment not found: $open" }
    return SourceSpan(SourcePosition(openIndex), span.end)
}

private fun Source<*>.spanUntilPlain(close: String): SourceSpan {
    val closeIndex = fragments.indexOfFirst { it is PlainFragment && it.text == close }
    require(closeIndex >= 0) { "Close fragment not found: $close" }
    return SourceSpan(span.start, SourcePosition(closeIndex))
}
