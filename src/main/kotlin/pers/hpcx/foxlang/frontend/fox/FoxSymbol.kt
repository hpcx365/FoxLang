package pers.hpcx.foxlang.frontend.fox

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.frontend.common.GrammarSymbol
import pers.hpcx.foxlang.frontend.fox.FoxBinaryOperatorCategorySymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxBracketSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxExpressionSymbol.ParenthesizedExpression
import pers.hpcx.foxlang.frontend.fox.FoxLexicalSymbol.Identifier
import pers.hpcx.foxlang.frontend.fox.FoxLexicalSymbol.TypeName
import pers.hpcx.foxlang.frontend.fox.FoxLiteralSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxSyntheticTypeKeywordSymbol.*

sealed interface FoxGrammarSymbol<N> : GrammarSymbol<N>

enum class FoxTerminalSymbol(val text: String) : FoxGrammarSymbol<ParsedUnit> {
    LParen("("),
    RParen(")"),
    LBracket("["),
    RBracket("]"),
    LBrace("{"),
    RBrace("}"),
    LAngle("<"),
    RAngle(">"),
    Dot("."),
    Colon(":"),
    Semicolon(";"),
    Comma(","),
    Backtick("`"),
    At("@"),
    Hash("#"),
    Dollar("$"),
    Arrow("->"),
    LineBreak("\n"),
}

enum class FoxBracketSymbol : FoxGrammarSymbol<ParsedUnit> {
    ParenOpen,
    ParenClose,
    SquareOpen,
    SquareClose,
    BraceOpen,
    BraceClose,
    AngleOpen,
    AngleClose,
}

enum class FoxFormattedStringPartSymbol : FoxGrammarSymbol<ParsedUnit> {
    FormattedStringStart,
    FormattedStringEnd,
    FormattedExpressionOpen,
    FormattedExpressionClose,
}

data object UnaryOperator : FoxGrammarSymbol<ParsedFoxUnaryOperator>
data object BinaryOperator : FoxGrammarSymbol<ParsedFoxBinaryOperator>
data object AssignOperator : FoxGrammarSymbol<ParsedFoxAssignOperator>

enum class FoxBinaryOperatorCategorySymbol : FoxGrammarSymbol<ParsedFoxBinaryOperator> {
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
}

enum class FoxUnaryOperatorSymbol(val text: String, val operator: FoxUnaryOperator) : FoxGrammarSymbol<ParsedUnit> {
    Not("!", FoxNotOperator),
    Neg("-", FoxNegOperator),
}

enum class FoxBinaryOperatorSymbol(
    val text: String,
    val operator: FoxBinaryOperator,
    val category: FoxBinaryOperatorCategorySymbol,
) : FoxGrammarSymbol<ParsedUnit> {
    Add("+", FoxAddOperator, AdditiveOperator),
    Sub("-", FoxSubOperator, AdditiveOperator),
    Mul("*", FoxMulOperator, MultiplicativeOperator),
    Div("/", FoxDivOperator, MultiplicativeOperator),
    Rem("%", FoxRemOperator, MultiplicativeOperator),
    And("&", FoxAndOperator, BitAndOperator),
    Or("|", FoxOrOperator, BitOrOperator),
    Xor("^", FoxXorOperator, BitXorOperator),
    Shl("<<", FoxShlOperator, ShiftOperator),
    Shr(">>", FoxShrOperator, ShiftOperator),
    Ushr(">>>", FoxUshrOperator, ShiftOperator),
    Eq("==", FoxEqOperator, EqualityOperator),
    Neq("!=", FoxNeqOperator, EqualityOperator),
    Lt("<", FoxLtOperator, ComparisonOperator),
    Gt(">", FoxGtOperator, ComparisonOperator),
    Leq("<=", FoxLeqOperator, ComparisonOperator),
    Geq(">=", FoxGeqOperator, ComparisonOperator),
    AndAnd("&&", FoxAndAndOperator, LogicalAndOperator),
    OrOr("||", FoxOrOrOperator, LogicalOrOperator),
}

enum class FoxAssignOperatorSymbol(val text: String, val operator: FoxAssignOperator) : FoxGrammarSymbol<ParsedUnit> {
    PlainAssign("=", FoxPlainAssignOperator),
    DefAssign(":=", FoxDefAssignOperator),
    AddAssign("+=", FoxAddAssignOperator),
    SubAssign("-=", FoxSubAssignOperator),
    MulAssign("*=", FoxMulAssignOperator),
    DivAssign("/=", FoxDivAssignOperator),
    RemAssign("%=", FoxRemAssignOperator),
    AndAssign("&=", FoxAndAssignOperator),
    OrAssign("|=", FoxOrAssignOperator),
    XorAssign("^=", FoxXorAssignOperator),
    ShlAssign("<<=", FoxShlAssignOperator),
    ShrAssign(">>=", FoxShrAssignOperator),
    UshrAssign(">>>=", FoxUshrAssignOperator),
    AndAndAssign("&&=", FoxAndAndAssignOperator),
    OrOrAssign("||=", FoxOrOrAssignOperator),
}

enum class FoxKeywordSymbol(val text: String) : FoxGrammarSymbol<ParsedUnit> {
    KwConst("const"),
    KwType("type"),
    KwDef("def"),
    KwThis("this"),
    KwIf("if"),
    KwElse("else"),
    KwWhen("when"),
    KwNew("new"),
    KwYield("yield"),
    KwReturn("return"),
    KwFor("for"),
    KwIn("in"),
    KwDo("do"),
    KwWhile("while"),
    KwBreak("break"),
    KwContinue("continue"),
    KwTry("try"),
    KwFinally("finally"),
    KwImport("import"),
    KwLowerUnit("unit"),
    KwTrue("true"),
    KwFalse("false"),
}

enum class FoxPrimitiveTypeKeywordSymbol(val text: String, val type: FoxPrimitiveType) : FoxGrammarSymbol<ParsedUnit> {
    KwVoid("Void", FoxVoidType),
    KwUnit("Unit", FoxUnitType),
    KwBool("Bool", FoxBoolType),
    KwByte("Byte", FoxByteType),
    KwShort("Short", FoxShortType),
    KwInt("Int", FoxIntType),
    KwLong("Long", FoxLongType),
    KwFloat("Float", FoxFloatType),
    KwDouble("Double", FoxDoubleType),
    KwChar("Char", FoxCharType),
    KwString("String", FoxStringType),
}

enum class FoxSyntheticTypeKeywordSymbol(val text: String) : FoxGrammarSymbol<ParsedUnit> {
    KwTuple("Tuple"),
    KwStruct("Struct"),
    KwObject("Object"),
    KwEnum("Enum"),
    KwArray("Array"),
    KwRef("Ref"),
    KwMethod("Method"),
    KwAny("Any"),
    KwAnyOf("AnyOf"),
    KwAllOf("AllOf"),
    KwNoneOf("NoneOf"),
    KwAnyTuple("AnyTuple"),
    KwAnyTupleOf("AnyTupleOf"),
    KwAnyStruct("AnyStruct"),
    KwAnyStructOf("AnyStructOf"),
    KwAnyEnum("AnyEnum"),
    KwAnyObject("AnyObject"),
    KwGetComponent("GetComponent"),
    KwGetComponentBack("GetComponentBack"),
    KwGetFirstComponents("GetFirstComponents"),
    KwGetFirstComponentsExact("GetFirstComponentsExact"),
    KwGetLastComponents("GetLastComponents"),
    KwGetLastComponentsExact("GetLastComponentsExact"),
    KwDropFirstComponents("DropFirstComponents"),
    KwDropFirstComponentsExact("DropFirstComponentsExact"),
    KwDropLastComponents("DropLastComponents"),
    KwDropLastComponentsExact("DropLastComponentsExact"),
    KwMergeTuples("MergeTuples"),
    KwGetFieldTypeByName("GetFieldTypeByName"),
    KwGetFieldTypeByIndex("GetFieldTypeByIndex"),
    KwGetFieldTypeByIndexBack("GetFieldTypeByIndexBack"),
    KwGetFirstFields("GetFirstFields"),
    KwGetFirstFieldsExact("GetFirstFieldsExact"),
    KwGetLastFields("GetLastFields"),
    KwGetLastFieldsExact("GetLastFieldsExact"),
    KwDropFirstFields("DropFirstFields"),
    KwDropFirstFieldsExact("DropFirstFieldsExact"),
    KwDropLastFields("DropLastFields"),
    KwDropLastFieldsExact("DropLastFieldsExact"),
    KwSelectFields("SelectFields"),
    KwSelectFieldsExact("SelectFieldsExact"),
    KwDropFields("DropFields"),
    KwDropFieldsExact("DropFieldsExact"),
    KwExtractFieldTypes("ExtractFieldTypes"),
    KwMergeStructs("MergeStructs"),
    KwGetMemberType("GetMemberType"),
    KwSelectMembers("SelectMembers"),
    KwSelectMembersExact("SelectMembersExact"),
    KwDropMembers("DropMembers"),
    KwDropMembersExact("DropMembersExact"),
    KwMergeObjects("MergeObjects"),
    KwGetEntryType("GetEntryType"),
    KwSelectEntries("SelectEntries"),
    KwSelectEntriesExact("SelectEntriesExact"),
    KwDropEntries("DropEntries"),
    KwDropEntriesExact("DropEntriesExact"),
    KwMergeEnums("MergeEnums"),
    KwGetElementType("GetElementType"),
    KwGetReferentType("GetReferentType"),
    KwGetThisType("GetThisType"),
    KwGetParameterStruct("GetParameterStruct"),
    KwGetReturnType("GetReturnType"),
    KwMethodOf("MethodOf"),
}

sealed interface FoxLiteralSymbol<N> : FoxGrammarSymbol<N> {
    data object LitUnit : FoxLiteralSymbol<ParsedUnit>
    data object LitBool : FoxLiteralSymbol<ParsedBoolean>
    data object LitInt : FoxLiteralSymbol<ParsedInt>
    data object LitLong : FoxLiteralSymbol<ParsedLong>
    data object LitFloat : FoxLiteralSymbol<ParsedFloat>
    data object LitDouble : FoxLiteralSymbol<ParsedDouble>
    data object LitChar : FoxLiteralSymbol<ParsedChar>
    data object LitString : FoxLiteralSymbol<ParsedString>
}

enum class FoxLexicalSymbol : FoxGrammarSymbol<ParsedString> {
    Identifier,
    TypeName,
    Label,
}

typealias ParsedStringTypePair = ParsedPair<ParsedString, ParsedFoxType<*>>
typealias ParsedOptionalStringTypePair = ParsedPair<ParsedString?, ParsedFoxType<*>>
typealias ParsedStringOptionalTypePair = ParsedPair<ParsedString, ParsedFoxType<*>?>

data object Type : FoxGrammarSymbol<ParsedFoxType<*>>

data object TupleComponentParameter : FoxGrammarSymbol<ParsedFoxType<*>>
data object TupleComponentParameterListHead : FoxGrammarSymbol<ParsedList<ParsedFoxType<*>>>
data object TupleComponentParameterList : FoxGrammarSymbol<ParsedList<ParsedFoxType<*>>>

data object StructFieldParameter : FoxGrammarSymbol<ParsedStringTypePair>
data object StructFieldParameterListHead : FoxGrammarSymbol<ParsedList<ParsedStringTypePair>>
data object StructFieldParameterList : FoxGrammarSymbol<ParsedList<ParsedStringTypePair>>

data object StructFieldName : FoxGrammarSymbol<ParsedString>
data object StructFieldNameList : FoxGrammarSymbol<ParsedList<ParsedString>>

data object ObjectMemberParameter : FoxGrammarSymbol<ParsedStringTypePair>
data object ObjectMemberParameterListHead : FoxGrammarSymbol<ParsedList<ParsedStringTypePair>>
data object ObjectMemberParameterList : FoxGrammarSymbol<ParsedList<ParsedStringTypePair>>

data object ObjectMemberName : FoxGrammarSymbol<ParsedString>
data object ObjectMemberNameList : FoxGrammarSymbol<ParsedList<ParsedString>>

data object EnumEntryParameter : FoxGrammarSymbol<ParsedStringTypePair>
data object EnumEntryParameterListHead : FoxGrammarSymbol<ParsedList<ParsedStringTypePair>>
data object EnumEntryParameterList : FoxGrammarSymbol<ParsedList<ParsedStringTypePair>>

data object EnumEntryName : FoxGrammarSymbol<ParsedString>
data object EnumEntryNameList : FoxGrammarSymbol<ParsedList<ParsedString>>

data object MethodTypeParameter : FoxGrammarSymbol<ParsedStringTypePair>
data object MethodTypeParameterListHead : FoxGrammarSymbol<ParsedFoxMethodTypeHead>
data object MethodTypeParameterList : FoxGrammarSymbol<ParsedFoxMethodType>

data object FormalParameter : FoxGrammarSymbol<ParsedStringTypePair>
data object FormalParameterListHead : FoxGrammarSymbol<ParsedList<ParsedStringTypePair>>
data object FormalParameterList : FoxGrammarSymbol<ParsedList<ParsedStringTypePair>>

data object ActualParameter : FoxGrammarSymbol<ParsedPair<ParsedString?, ParsedFoxStatement<*>>>
data object ActualParameterListHead : FoxGrammarSymbol<ParsedList<ParsedPair<ParsedString?, ParsedFoxStatement<*>>>>
data object ActualParameterList : FoxGrammarSymbol<ParsedList<ParsedPair<ParsedString?, ParsedFoxStatement<*>>>>

data object FormalGenericParameter : FoxGrammarSymbol<ParsedStringOptionalTypePair>
data object FormalGenericParameterListHead : FoxGrammarSymbol<ParsedList<ParsedStringOptionalTypePair>>
data object FormalGenericParameterList : FoxGrammarSymbol<ParsedList<ParsedStringOptionalTypePair>>

data object FormalGenericParameterNoConstraints : FoxGrammarSymbol<ParsedString>
data object FormalGenericParameterNoConstraintsListHead : FoxGrammarSymbol<ParsedList<ParsedString>>
data object FormalGenericParameterNoConstraintsList : FoxGrammarSymbol<ParsedList<ParsedString>>

data object ActualGenericParameter : FoxGrammarSymbol<ParsedOptionalStringTypePair>
data object ActualGenericParameterListHead : FoxGrammarSymbol<ParsedList<ParsedOptionalStringTypePair>>
data object ActualGenericParameterList : FoxGrammarSymbol<ParsedList<ParsedOptionalStringTypePair>>

data object AnonymousActualGenericParameter : FoxGrammarSymbol<ParsedFoxType<*>>
data object AnonymousActualGenericParameterListHead : FoxGrammarSymbol<ParsedList<ParsedFoxType<*>>>
data object AnonymousActualGenericParameterList : FoxGrammarSymbol<ParsedList<ParsedFoxType<*>>>
data object AnonymousActualGenericParameterSingleList : FoxGrammarSymbol<ParsedList<ParsedFoxType<*>>>

data object ActualGenericIntParameterList : FoxGrammarSymbol<ParsedPair<ParsedFoxType<*>, ParsedInt>>
data object IndexArgumentListHead : FoxGrammarSymbol<ParsedList<ParsedFoxStatement<*>>>
data object IndexArgumentList : FoxGrammarSymbol<ParsedList<ParsedFoxStatement<*>>>

enum class FoxExpressionSymbol : FoxGrammarSymbol<ParsedFoxStatement<*>> {
    ParenthesizedExpression,
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
}

data object FormattedStringPart : FoxGrammarSymbol<ParsedFoxFormattedStringPart>
data object FormattedStringPartList : FoxGrammarSymbol<ParsedList<ParsedFoxFormattedStringPart>>

data object LambdaParameter : FoxGrammarSymbol<ParsedStringOptionalTypePair>
data object LambdaParameterListHead : FoxGrammarSymbol<ParsedList<ParsedStringOptionalTypePair>>
data object LambdaParameterList : FoxGrammarSymbol<ParsedList<ParsedStringOptionalTypePair>>
data object LambdaStatementBlockHead : FoxGrammarSymbol<ParsedList<ParsedFoxStatement<*>>>
data object LambdaBody : FoxGrammarSymbol<ParsedFoxStatement<*>>
data object ExplicitLambdaLiteral : FoxGrammarSymbol<ParsedFoxLambda>
data object InlineImplicitLambdaLiteral : FoxGrammarSymbol<ParsedFoxLambda>
data object ImplicitLambdaLiteral : FoxGrammarSymbol<ParsedFoxLambda>
data object LambdaLiteral : FoxGrammarSymbol<ParsedFoxLambda>

data object Statement : FoxGrammarSymbol<ParsedFoxStatement<*>>
data object StatementLine : FoxGrammarSymbol<ParsedFoxStatement<*>>
data object StatementBlockHead : FoxGrammarSymbol<ParsedList<ParsedFoxStatement<*>>>
data object StatementBlockCore : FoxGrammarSymbol<ParsedList<ParsedFoxStatement<*>>>
data object StatementBlock : FoxGrammarSymbol<ParsedFoxBlock>

data object WhenCaseConditionList : FoxGrammarSymbol<ParsedList<ParsedFoxStatement<*>>>
data object WhenCase : FoxGrammarSymbol<ParsedFoxCase>
data object WhenCaseLine : FoxGrammarSymbol<ParsedFoxCase>
data object WhenCaseListHead : FoxGrammarSymbol<ParsedList<ParsedFoxCase>>
data object WhenCaseList : FoxGrammarSymbol<ParsedList<ParsedFoxCase>>
data object IfCore : FoxGrammarSymbol<ParsedFoxIf>
data object WhileCore : FoxGrammarSymbol<ParsedFoxWhile>
data object DoWhileCore : FoxGrammarSymbol<ParsedFoxDoWhile>
data object WhenCore : FoxGrammarSymbol<ParsedFoxWhen>
data object FileElementList : FoxGrammarSymbol<ParsedList<ParsedFoxFileElement<*>>>

data object File : FoxGrammarSymbol<ParsedFoxFile>
data object FileElement : FoxGrammarSymbol<ParsedFoxFileElement<*>>
data object FileElementLine : FoxGrammarSymbol<ParsedFoxFileElement<*>>
data object TypeAlias : FoxGrammarSymbol<ParsedFoxTypeAlias>
data object MethodDefinition : FoxGrammarSymbol<ParsedFoxMethodDefinition>

internal val FoxKeywordsByText: Map<String, FoxGrammarSymbol<ParsedUnit>> = buildMap {
    FoxKeywordSymbol.entries.forEach { put(it.text, it) }
    FoxPrimitiveTypeKeywordSymbol.entries.forEach { put(it.text, it) }
    FoxSyntheticTypeKeywordSymbol.entries.forEach { put(it.text, it) }
}

internal val FoxPunctuationSymbolsByText: Map<String, FoxGrammarSymbol<*>> =
    FoxTerminalSymbol.entries.associateBy { it.text }

internal val FoxOperatorTokenSymbolsByText: Map<String, FoxGrammarSymbol<*>> = buildMap {
    FoxUnaryOperatorSymbol.entries.forEach { put(it.text, it) }
    FoxBinaryOperatorSymbol.entries.forEach { put(it.text, it) }
    FoxAssignOperatorSymbol.entries.forEach { put(it.text, it) }
}

internal val FoxDelimiterSymbols: List<GrammarSymbol<*>> = buildList {
    addAll(FoxTerminalSymbol.entries)
    addAll(FoxBracketSymbol.entries)
    addAll(FoxFormattedStringPartSymbol.entries)
}

internal val FoxLineContinuationSymbols = listOf(
    ParenthesizedExpression,
    ActualParameterList,
    IndexArgumentList,
    ActualGenericParameterList,
    FormalParameterList,
    FormalGenericParameterList,
    FormalGenericParameterNoConstraintsList,
    AnonymousActualGenericParameterList,
    AnonymousActualGenericParameterSingleList,
    TupleComponentParameterList,
    StructFieldParameterList,
    ObjectMemberParameterList,
    EnumEntryParameterList,
    MethodTypeParameterList,
    StatementBlock,
    LambdaLiteral,
    Type,
)

internal sealed interface FoxCommaListSpec {
    val head: GrammarSymbol<*>
    val list: GrammarSymbol<*>
    val item: GrammarSymbol<*>
}

internal data class FoxDelimitedCommaListSpec(
    override val head: GrammarSymbol<*>,
    override val list: GrammarSymbol<*>,
    override val item: GrammarSymbol<*>,
    val open: GrammarSymbol<*>,
    val close: GrammarSymbol<*>,
) : FoxCommaListSpec

internal data class FoxUndelimitedCommaListSpec(
    override val head: GrammarSymbol<*>,
    override val list: GrammarSymbol<*>,
    override val item: GrammarSymbol<*>,
) : FoxCommaListSpec

internal val FoxDelimitedCommaListSpecs = listOf(
    FoxDelimitedCommaListSpec(
        FormalParameterListHead,
        FormalParameterList,
        FormalParameter,
        ParenOpen,
        ParenClose,
    ),
    FoxDelimitedCommaListSpec(
        ActualParameterListHead,
        ActualParameterList,
        ActualParameter,
        ParenOpen,
        ParenClose,
    ),
    FoxDelimitedCommaListSpec(
        IndexArgumentListHead,
        IndexArgumentList,
        Statement,
        SquareOpen,
        SquareClose,
    ),
    FoxDelimitedCommaListSpec(
        FormalGenericParameterListHead,
        FormalGenericParameterList,
        FormalGenericParameter,
        AngleOpen,
        AngleClose,
    ),
    FoxDelimitedCommaListSpec(
        FormalGenericParameterNoConstraintsListHead,
        FormalGenericParameterNoConstraintsList,
        FormalGenericParameterNoConstraints,
        AngleOpen,
        AngleClose,
    ),
    FoxDelimitedCommaListSpec(
        ActualGenericParameterListHead,
        ActualGenericParameterList,
        ActualGenericParameter,
        AngleOpen,
        AngleClose,
    ),
    FoxDelimitedCommaListSpec(
        AnonymousActualGenericParameterListHead,
        AnonymousActualGenericParameterList,
        AnonymousActualGenericParameter,
        AngleOpen,
        AngleClose,
    ),
    FoxDelimitedCommaListSpec(
        TupleComponentParameterListHead,
        TupleComponentParameterList,
        TupleComponentParameter,
        AngleOpen,
        AngleClose,
    ),
    FoxDelimitedCommaListSpec(
        StructFieldParameterListHead,
        StructFieldParameterList,
        StructFieldParameter,
        AngleOpen,
        AngleClose,
    ),
    FoxDelimitedCommaListSpec(
        ObjectMemberParameterListHead,
        ObjectMemberParameterList,
        ObjectMemberParameter,
        AngleOpen,
        AngleClose,
    ),
    FoxDelimitedCommaListSpec(
        EnumEntryParameterListHead,
        EnumEntryParameterList,
        EnumEntryParameter,
        AngleOpen,
        AngleClose,
    ),
    FoxDelimitedCommaListSpec(
        MethodTypeParameterListHead,
        MethodTypeParameterList,
        MethodTypeParameter,
        AngleOpen,
        AngleClose,
    ),
)

internal val FoxUndelimitedCommaListSpecs = listOf(
    FoxUndelimitedCommaListSpec(LambdaParameterListHead, LambdaParameterList, LambdaParameter),
    FoxUndelimitedCommaListSpec(StructFieldNameList, StructFieldNameList, StructFieldName),
    FoxUndelimitedCommaListSpec(ObjectMemberNameList, ObjectMemberNameList, ObjectMemberName),
    FoxUndelimitedCommaListSpec(EnumEntryNameList, EnumEntryNameList, EnumEntryName),
)

internal val FoxCommaListSpecs = FoxDelimitedCommaListSpecs + FoxUndelimitedCommaListSpecs

internal val FoxCommaListItemSymbols = FoxCommaListSpecs.map { it.item }.distinct()

internal val FoxCommaListSymbols = FoxCommaListSpecs.flatMap { listOf(it.head, it.list) }.distinct()

internal val FoxLexicalSymbols = FoxLexicalSymbol.entries

internal val FoxLiteralSymbols = listOf(
    LitUnit,
    LitBool,
    LitInt,
    LitLong,
    LitFloat,
    LitDouble,
    LitChar,
    LitString,
)

internal val FoxListSymbols = FoxCommaListSymbols + listOf(
    FormattedStringPartList,
    LambdaStatementBlockHead,
    WhenCaseConditionList,
    WhenCaseListHead,
    WhenCaseList,
    FileElementList,
)

internal val FoxExpressionSymbols = FoxExpressionSymbol.entries + listOf(
    FormattedStringPart,
    LambdaBody,
    ExplicitLambdaLiteral,
    InlineImplicitLambdaLiteral,
    ImplicitLambdaLiteral,
    LambdaLiteral,
    Statement,
)

internal val FoxStatementSymbols = listOf(
    StatementLine,
    StatementBlockHead,
    StatementBlockCore,
    StatementBlock,
    WhenCase,
    WhenCaseLine,
    IfCore,
    WhileCore,
    DoWhileCore,
    WhenCore,
)

internal val FoxOperatorSymbols = buildList {
    addAll(FoxUnaryOperatorSymbol.entries)
    addAll(FoxBinaryOperatorSymbol.entries)
    addAll(FoxBinaryOperatorCategorySymbol.entries)
    addAll(FoxAssignOperatorSymbol.entries)
    add(UnaryOperator)
    add(BinaryOperator)
    add(AssignOperator)
}

internal val FoxDeclarationSymbols = listOf(
    FileElementLine,
    MethodDefinition,
    FileElement,
    TypeAlias,
)

internal val FoxTypeArgumentLists = mapOf(
    *arrayOf(
        KwAnyOf,
        KwAllOf,
        KwNoneOf,
        KwAnyStructOf,
        KwMergeTuples,
        KwMergeStructs,
        KwMergeObjects,
        KwMergeEnums,
    ).map { it to AnonymousActualGenericParameterList }.toTypedArray(),
    *arrayOf(
        KwAnyTupleOf,
        KwArray,
        KwRef,
    ).map { it to AnonymousActualGenericParameterSingleList }.toTypedArray(),
    KwTuple to TupleComponentParameterList,
    KwStruct to StructFieldParameterList,
    KwObject to ObjectMemberParameterList,
    KwEnum to EnumEntryParameterList,
    KwMethod to MethodTypeParameterList,
)

internal val FoxFixedArityTypeArguments = mapOf(
    *fixedArityTypeEntries(
        listOf(Type, LitInt),
        KwGetComponent,
        KwGetComponentBack,
        KwGetFirstComponents,
        KwGetFirstComponentsExact,
        KwGetLastComponents,
        KwGetLastComponentsExact,
        KwDropFirstComponents,
        KwDropFirstComponentsExact,
        KwDropLastComponents,
        KwDropLastComponentsExact,
        KwGetFieldTypeByIndex,
        KwGetFieldTypeByIndexBack,
        KwGetFirstFields,
        KwGetFirstFieldsExact,
        KwGetLastFields,
        KwGetLastFieldsExact,
        KwDropFirstFields,
        KwDropFirstFieldsExact,
        KwDropLastFields,
        KwDropLastFieldsExact,
    ).toTypedArray(),
    *fixedArityTypeEntries(listOf(Type, Identifier), KwGetFieldTypeByName, KwGetMemberType).toTypedArray(),
    *fixedArityTypeEntries(
        listOf(Type, StructFieldNameList),
        KwSelectFields,
        KwSelectFieldsExact,
        KwDropFields,
        KwDropFieldsExact,
        KwExtractFieldTypes,
    ).toTypedArray(),
    *fixedArityTypeEntries(
        listOf(Type, ObjectMemberNameList),
        KwSelectMembers,
        KwSelectMembersExact,
        KwDropMembers,
        KwDropMembersExact,
    ).toTypedArray(),
    KwGetEntryType to listOf(Type, TypeName),
    *fixedArityTypeEntries(
        listOf(Type, EnumEntryNameList),
        KwSelectEntries,
        KwSelectEntriesExact,
        KwDropEntries,
        KwDropEntriesExact,
    ).toTypedArray(),
    *fixedArityTypeEntries(listOf(Type), KwGetElementType, KwGetReferentType, KwGetThisType, KwGetParameterStruct, KwGetReturnType)
        .toTypedArray(),
    KwMethodOf to listOf(Type, Type, Type),
)

private fun fixedArityTypeEntries(
    arguments: List<GrammarSymbol<*>>,
    vararg keywords: FoxGrammarSymbol<*>,
): List<Pair<FoxGrammarSymbol<*>, List<GrammarSymbol<*>>>> {
    return keywords.map { it to arguments }
}
