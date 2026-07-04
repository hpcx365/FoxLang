package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.frontend.*

fun diagnoseFoxFile(source: String): DiagnosticResult {
    val analysis = FoxFileParser.analyze(source)
    val root = Expectation(
        symbol = node<FoxFile>(),
        span = analysis.source.span,
    )
    return FoxDiagnosticEngine.diagnose(root, analysis)
}

private val typeAfterColon = diagnosticChildren { expect(node<FoxType>()).afterToken(":") }
private val typeAfterEquals = diagnosticChildren { expect(node<FoxType>()).afterToken("=") }
private val typeAtSelf = diagnosticChildren { expect(node<FoxType>()).here() }
private val actualParameterChildren = diagnosticChildren { expect(node<FoxStatement>()).afterTokenOrSelf("=") }

private val fileElementSpans = gapsExcluding(FileElementLine)
    .splitBy(blockAwareLineSpans(setOf("def"), BlockOpen, BlockClose))
private val statementSpans = allGaps()
    .splitBy(blockAwareLineSpans(setOf("if", "while", "do", "when"), BlockOpen, BlockClose))
private val statementBlockSpans = allGaps()
    .splitBy(afterLeading(BlockOpen).then(withoutLeadingLineBreaks()))
    .then(statementSpans)
private val whenCaseSpans = allGaps().splitBy(lineSpans())

private val fileElementLineChildren = diagnosticChildren {
    expect(node<FoxFileElement>()).trimTrailingLineBreak()
}

private val fileElementChildren = diagnosticChildren {
    branch {
        case(firstText("def")) {
            expect(node<FoxMethodDefinition>()).here()
            expect(methodChildren).here()
        }
        case(firstText("type")) {
            expect(node<FoxTypeAlias>()).here()
        }
    }
}

private val methodDefinitionChildren = diagnosticChildren {
    expect(node<FoxMethodDefinition>()).here()
    expect(methodChildren).here()
}

private val methodChildren = diagnosticChildren {
    val headSpan = before(BlockOpen) ?: span
    expect(FormalParameterList).at(delimitedSpan(ParenOpen, ParenClose, headSpan) ?: return@diagnosticChildren)
    expect(node<FoxBlock>()).delimitedBy(BlockOpen, BlockClose)
}

private val statementLineChildren = diagnosticChildren {
    expect(node<FoxStatement>()).trimTrailingLineBreak()
}

private val statementChildren = diagnosticChildren {
    expect(ActualGenericParameterList).delimitedBy(AngleOpen, AngleClose)
    expect(ActualParameterList).delimitedBy(ParenOpen, ParenClose)
}

private val whenCaseLineChildren = diagnosticChildren {
    expect(WhenCase).trimTrailingLineBreak()
}

private val whenCaseListChildren = diagnosticChildren {
    expect(WhenCaseListHead).inside(BlockOpen, BlockClose)
}

private val whenChildren = diagnosticChildren {
    expect(WhenCaseList).delimitedBy(BlockOpen, BlockClose)
    expect(whenCaseListChildren).delimitedBy(BlockOpen, BlockClose)
}

val FoxFileDiagnostics = diagnosticRules {
    diagnose(node<FoxFile>()) {
        recurse(FileElementList)
    }
    
    diagnose(FileElementList) {
        seedMissing(
            symbol = FileElementLine,
            spans = fileElementSpans,
            reason = "Expected file element",
            cost = { 8 + it.length },
            children = fileElementLineChildren,
        )
    }
    
    diagnose(FileElementLine) {
        seedTrimmedTrailingLineBreak(node<FoxFileElement>(), "Expected file element before line break", 6, fileElementChildren)
    }
    
    diagnose(node<FoxFileElement>()) {
        branch {
            case(firstText("def")) {
                seed(node<FoxMethodDefinition>(), span, "Expected method definition", 4, methodDefinitionChildren)
            }
            case(firstText("type")) {
                seed(node<FoxTypeAlias>(), span, "Expected type alias", 4)
            }
        }
    }
}

val FoxMethodDiagnostics = diagnosticRules {
    diagnose(node<FoxMethodDefinition>()) {
        seedAndRecurse(MethodHead, span, "Expected method head", 6)
        choice {
            prefer {
                seedDelimited(node<FoxBlock>(), BlockOpen, BlockClose, "Expected method body", 4) {
                    expect(StatementBlock).at(it)
                }
            }
            fallback {
                seedAtEnd(node<FoxBlock>(), "Expected method body", 10)
            }
        }
    }
    
    diagnose(MethodHead) {
        val headSpan = before(BlockOpen) ?: span
        choice {
            prefer {
                seedDelimited(FormalParameterList, ParenOpen, ParenClose, "Expected formal parameter list", 4, span = headSpan)
            }
            fallback {
                if (containsToken("def", headSpan)) {
                    seed(FormalParameterList, SourceSpan(headSpan.end, headSpan.end), "Expected formal parameter list", 8)
                }
            }
        }
    }
    
    diagnose(ReturnTypeClause) {
        seedAfterToken(node<FoxType>(), ":", "Expected return type", 4)
    }
    
    diagnose(node<FoxBlock>()) {
        recurse(StatementBlock)
    }
    
    diagnose(StatementBlock) {
        recurseThroughDelimitedContent(StatementBlockHead, BlockOpen, BlockClose)
    }
    
    diagnose(StatementBlockHead) {
        seedMissing(
            symbol = StatementLine,
            spans = statementBlockSpans,
            reason = "Expected statement",
            cost = { 8 + it.length },
            children = statementLineChildren,
        )
    }
}

val FoxStatementDiagnostics = diagnosticRules {
    diagnose(StatementLine) {
        seedTrimmedTrailingLineBreak(node<FoxStatement>(), "Expected statement before line break", 4, statementChildren)
    }
    
    diagnose(node<FoxStatement>()) {
        branch {
            case(firstText("if")) {
                seed(IfCore, span, "Expected if statement", 4)
            }
            case(firstText("while")) {
                seed(WhileCore, span, "Expected while statement", 4)
            }
            case(firstText("do")) {
                seed(DoWhileCore, span, "Expected do-while statement", 4)
            }
            case(firstText("when")) {
                seed(WhenCore, span, "Expected when statement", 4, whenChildren)
            }
            case(firstText("return")) {
                seed(AssignmentExpression, afterFirstFragment(), "Expected return value", 6, statementChildren)
            }
            case(firstText("yield")) {
                seed(AssignmentExpression, afterFirstFragment(), "Expected yielded value", 6, statementChildren)
            }
            case(firstText("break", "continue")) {
            }
            otherwise {
                seed(AssignmentExpression, span, "Expected expression", 6 + span.length, statementChildren)
            }
        }
    }
    
    diagnose(WhenCore) {
        seedDelimited(WhenCaseList, BlockOpen, BlockClose, "Expected when case list", 4, children = whenCaseListChildren)
    }
    
    diagnose(WhenCaseList) {
        recurseInsideDelimiters(WhenCaseListHead, BlockOpen, BlockClose)
    }
    
    diagnose(ControlBody) {
        seedTrimmedLeadingLineBreak(node<FoxStatement>(), "Expected control-flow body", 4)
    }
    
    diagnose(WhenCaseListHead) {
        seedMissing(
            symbol = WhenCaseLine,
            spans = whenCaseSpans,
            reason = "Expected when case",
            cost = { 8 + it.length },
            children = whenCaseLineChildren,
        )
    }
    
    diagnose(WhenCaseLine) {
        seedTrimmedTrailingLineBreak(WhenCase, "Expected when case before line break", 4)
    }
    
    diagnose(WhenCase) {
        seedAfter(ControlBody, Arrow, "Expected when case body", 4)
    }
}

private val foxListNesting = listOf(
    ParenOpen to ParenClose,
    AngleOpen to AngleClose,
    BlockOpen to BlockClose,
)

private val formalParameterList = diagnosticList(FormalParameterList) {
    item(FormalParameter, "formal parameter")
    between(ParenOpen, ParenClose)
    separatedBy(Comma)
    nestedBy(foxListNesting)
    itemChildren(typeAfterColon)
}

private val actualParameterList = diagnosticList(ActualParameterList) {
    item(ActualParameter, "actual parameter")
    between(ParenOpen, ParenClose)
    separatedBy(Comma)
    nestedBy(foxListNesting)
    itemChildren(actualParameterChildren)
}

private val lambdaParameterList = diagnosticList(LambdaParameterList) {
    item(LambdaParameter, "lambda parameter")
    between(BlockOpen, Arrow)
    separatedBy(Comma)
    nestedBy(foxListNesting)
}

private val formalGenericParameterList = diagnosticList(FormalGenericParameterList) {
    item(FormalGenericParameter, "formal generic parameter")
    between(AngleOpen, AngleClose)
    separatedBy(Comma)
    nestedBy(foxListNesting)
    itemChildren(typeAfterEquals)
}

private val formalGenericParameterNoConstraintsList = diagnosticList(FormalGenericParameterNoConstraintsList) {
    item(FormalGenericParameterNoConstraints, "formal generic parameter")
    between(AngleOpen, AngleClose)
    separatedBy(Comma)
    nestedBy(foxListNesting)
}

private val actualGenericParameterList = diagnosticList(ActualGenericParameterList) {
    item(ActualGenericParameter, "actual generic parameter")
    between(AngleOpen, AngleClose)
    separatedBy(Comma)
    nestedBy(foxListNesting)
    itemChildren(typeAfterEquals)
}

private val anonymousActualGenericParameterList = diagnosticList(AnonymousActualGenericParameterList) {
    item(AnonymousActualGenericParameter, "generic parameter")
    between(AngleOpen, AngleClose)
    separatedBy(Comma)
    nestedBy(foxListNesting)
    itemChildren(typeAtSelf)
}

private val tupleComponentParameterList = diagnosticList(TupleComponentParameterList) {
    item(TupleComponentParameter, "tuple component")
    between(AngleOpen, AngleClose)
    separatedBy(Comma)
    nestedBy(foxListNesting)
    itemChildren(typeAtSelf)
}

private val structFieldParameterList = diagnosticList(StructFieldParameterList) {
    item(StructFieldParameter, "struct field")
    between(AngleOpen, AngleClose)
    separatedBy(Comma)
    nestedBy(foxListNesting)
    itemChildren(typeAfterColon)
}

private val objectMemberParameterList = diagnosticList(ObjectMemberParameterList) {
    item(ObjectMemberParameter, "object member")
    between(AngleOpen, AngleClose)
    separatedBy(Comma)
    nestedBy(foxListNesting)
    itemChildren(typeAfterColon)
}

private val enumItemParameterList = diagnosticList(EnumItemParameterList) {
    item(EnumItemParameter, "enum item")
    between(AngleOpen, AngleClose)
    separatedBy(Comma)
    nestedBy(foxListNesting)
    itemChildren(typeAfterEquals)
}

private val methodTypeArgumentList = diagnosticList(MethodTypeArgumentList) {
    item(MethodTypeArgument, "method type argument")
    between(AngleOpen, AngleClose)
    separatedBy(Comma)
    nestedBy(foxListNesting)
    itemChildren(typeAfterColon)
}

val FoxTypeDiagnostics = diagnosticRules {
    diagnose(node<FoxType>()) {
        branch {
            case(firstText("Tuple")) {
                seedDelimitedList(tupleComponentParameterList, "Expected type argument list", 4)
            }
            case(firstText("Struct")) {
                seedDelimitedList(structFieldParameterList, "Expected type argument list", 4)
            }
            case(firstText("Object")) {
                seedDelimitedList(objectMemberParameterList, "Expected type argument list", 4)
            }
            case(firstText("Enum")) {
                seedDelimitedList(enumItemParameterList, "Expected type argument list", 4)
            }
            case(firstText("Method")) {
                seedDelimitedList(methodTypeArgumentList, "Expected type argument list", 4)
            }
            otherwise {
                seedDelimitedList(anonymousActualGenericParameterList, "Expected type argument list", 4)
            }
        }
    }
}

val FoxDelimitedListDiagnostics = diagnosticRules {
    listOf(
        formalParameterList,
        actualParameterList,
        lambdaParameterList,
        formalGenericParameterList,
        formalGenericParameterNoConstraintsList,
        actualGenericParameterList,
        anonymousActualGenericParameterList,
        tupleComponentParameterList,
        structFieldParameterList,
        objectMemberParameterList,
        enumItemParameterList,
        methodTypeArgumentList,
    ).forEach { delimitedList(it) }
}

val FoxDiagnosticEngine = DiagnosticEngine(
    listOf(
        FoxFileDiagnostics,
        FoxMethodDiagnostics,
        FoxStatementDiagnostics,
        FoxTypeDiagnostics,
        FoxDelimitedListDiagnostics,
    ),
)
