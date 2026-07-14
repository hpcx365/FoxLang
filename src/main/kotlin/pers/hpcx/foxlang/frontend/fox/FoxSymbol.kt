package pers.hpcx.foxlang.frontend.fox

import pers.hpcx.foxlang.frontend.common.GrammarSymbol
import pers.hpcx.foxlang.frontend.fox.FoxBinaryOperatorCategorySymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxBracketSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxExpressionSymbol.ParenthesizedExpression
import pers.hpcx.foxlang.frontend.fox.FoxLiteralSymbol.*
import pers.hpcx.foxlang.frontend.fox.FoxSyntheticTypeKeywordSymbol.*
import pers.hpcx.foxlang.ir.*

sealed interface FoxGrammarSymbol<N> : GrammarSymbol<N>

enum class FoxTerminalSymbol(val text: String) : FoxGrammarSymbol<SyntaxUnit> {
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

enum class FoxBracketSymbol : FoxGrammarSymbol<SyntaxUnit> {
    ParenOpen,
    ParenClose,
    SquareOpen,
    SquareClose,
    BraceOpen,
    BraceClose,
    AngleOpen,
    AngleClose,
}

enum class FoxFormattedStringPartSymbol : FoxGrammarSymbol<SyntaxUnit> {
    FormattedStringStart,
    FormattedStringEnd,
    FormattedExpressionOpen,
    FormattedExpressionClose,
}

data object UnaryOperator : FoxGrammarSymbol<SyntaxUnaryOperator>
data object BinaryOperator : FoxGrammarSymbol<SyntaxBinaryOperator>
data object AssignOperator : FoxGrammarSymbol<SyntaxAssignOperator>

enum class FoxBinaryOperatorCategorySymbol : FoxGrammarSymbol<SyntaxBinaryOperator> {
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

enum class FoxUnaryOperatorSymbol(val text: String, val operator: UnaryOperatorEnum) : FoxGrammarSymbol<SyntaxUnit> {
    Not("!", UnaryOperatorEnum.Not),
    Neg("-", UnaryOperatorEnum.Neg),
}

enum class FoxBinaryOperatorSymbol(
    val text: String,
    val operator: BinaryOperatorEnum,
    val category: FoxBinaryOperatorCategorySymbol,
) : FoxGrammarSymbol<SyntaxUnit> {
    Add("+", BinaryOperatorEnum.Add, AdditiveOperator),
    Sub("-", BinaryOperatorEnum.Sub, AdditiveOperator),
    Mul("*", BinaryOperatorEnum.Mul, MultiplicativeOperator),
    Div("/", BinaryOperatorEnum.Div, MultiplicativeOperator),
    Rem("%", BinaryOperatorEnum.Rem, MultiplicativeOperator),
    And("&", BinaryOperatorEnum.And, BitAndOperator),
    Or("|", BinaryOperatorEnum.Or, BitOrOperator),
    Xor("^", BinaryOperatorEnum.Xor, BitXorOperator),
    Shl("<<", BinaryOperatorEnum.Shl, ShiftOperator),
    Shr(">>", BinaryOperatorEnum.Shr, ShiftOperator),
    Ushr(">>>", BinaryOperatorEnum.Ushr, ShiftOperator),
    Eq("==", BinaryOperatorEnum.Eq, EqualityOperator),
    Neq("!=", BinaryOperatorEnum.Neq, EqualityOperator),
    Lt("<", BinaryOperatorEnum.Lt, ComparisonOperator),
    Gt(">", BinaryOperatorEnum.Gt, ComparisonOperator),
    Leq("<=", BinaryOperatorEnum.Leq, ComparisonOperator),
    Geq(">=", BinaryOperatorEnum.Geq, ComparisonOperator),
    AndAnd("&&", BinaryOperatorEnum.AndAnd, LogicalAndOperator),
    OrOr("||", BinaryOperatorEnum.OrOr, LogicalOrOperator),
}

enum class FoxAssignOperatorSymbol(val text: String, val operator: AssignOperatorEnum) : FoxGrammarSymbol<SyntaxUnit> {
    PlainAssign("=", AssignOperatorEnum.Plain),
    DefAssign(":=", AssignOperatorEnum.Def),
    AddAssign("+=", AssignOperatorEnum.Add),
    SubAssign("-=", AssignOperatorEnum.Sub),
    MulAssign("*=", AssignOperatorEnum.Mul),
    DivAssign("/=", AssignOperatorEnum.Div),
    RemAssign("%=", AssignOperatorEnum.Rem),
    AndAssign("&=", AssignOperatorEnum.And),
    OrAssign("|=", AssignOperatorEnum.Or),
    XorAssign("^=", AssignOperatorEnum.Xor),
    ShlAssign("<<=", AssignOperatorEnum.Shl),
    ShrAssign(">>=", AssignOperatorEnum.Shr),
    UshrAssign(">>>=", AssignOperatorEnum.Ushr),
    AndAndAssign("&&=", AssignOperatorEnum.AndAnd),
    OrOrAssign("||=", AssignOperatorEnum.OrOr),
}

enum class FoxKeywordSymbol(val text: String) : FoxGrammarSymbol<SyntaxUnit> {
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

enum class FoxPrimitiveTypeKeywordSymbol(val text: String, val type: PrimitiveTypeEnum) : FoxGrammarSymbol<SyntaxUnit> {
    KwVoid("Void", PrimitiveTypeEnum.Void),
    KwUnit("Unit", PrimitiveTypeEnum.Unit),
    KwBool("Bool", PrimitiveTypeEnum.Bool),
    KwByte("Byte", PrimitiveTypeEnum.Byte),
    KwShort("Short", PrimitiveTypeEnum.Short),
    KwInt("Int", PrimitiveTypeEnum.Int),
    KwLong("Long", PrimitiveTypeEnum.Long),
    KwFloat("Float", PrimitiveTypeEnum.Float),
    KwDouble("Double", PrimitiveTypeEnum.Double),
    KwChar("Char", PrimitiveTypeEnum.Char),
    KwString("String", PrimitiveTypeEnum.String),
}

enum class FoxSyntheticTypeKeywordSymbol(val text: String) : FoxGrammarSymbol<SyntaxUnit> {
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
    data object LitUnit : FoxLiteralSymbol<SyntaxUnit>
    data object LitBool : FoxLiteralSymbol<SyntaxBoolean>
    data object LitInt : FoxLiteralSymbol<SyntaxInt>
    data object LitLong : FoxLiteralSymbol<SyntaxLong>
    data object LitFloat : FoxLiteralSymbol<SyntaxFloat>
    data object LitDouble : FoxLiteralSymbol<SyntaxDouble>
    data object LitChar : FoxLiteralSymbol<SyntaxChar>
    data object LitString : FoxLiteralSymbol<SyntaxString>
}

data object TypeName : FoxGrammarSymbol<SyntaxString>
data object Identifier : FoxGrammarSymbol<SyntaxString>

typealias SyntaxStringTypePair = SyntaxPair<SyntaxString, SyntaxType<*>>
typealias SyntaxOptionalStringTypePair = SyntaxPair<SyntaxString?, SyntaxType<*>>
typealias SyntaxStringOptionalTypePair = SyntaxPair<SyntaxString, SyntaxType<*>?>

data object Type : FoxGrammarSymbol<SyntaxType<*>>

data object TupleComponentParameter : FoxGrammarSymbol<SyntaxType<*>>
data object TupleComponentParameterListHead : FoxGrammarSymbol<SyntaxList<SyntaxType<*>>>
data object TupleComponentParameterList : FoxGrammarSymbol<SyntaxList<SyntaxType<*>>>

data object StructFieldParameter : FoxGrammarSymbol<SyntaxStringTypePair>
data object StructFieldParameterListHead : FoxGrammarSymbol<SyntaxList<SyntaxStringTypePair>>
data object StructFieldParameterList : FoxGrammarSymbol<SyntaxList<SyntaxStringTypePair>>

data object StructFieldName : FoxGrammarSymbol<SyntaxString>
data object StructFieldNameList : FoxGrammarSymbol<SyntaxList<SyntaxString>>

data object ObjectMemberParameter : FoxGrammarSymbol<SyntaxStringTypePair>
data object ObjectMemberParameterListHead : FoxGrammarSymbol<SyntaxList<SyntaxStringTypePair>>
data object ObjectMemberParameterList : FoxGrammarSymbol<SyntaxList<SyntaxStringTypePair>>

data object ObjectMemberName : FoxGrammarSymbol<SyntaxString>
data object ObjectMemberNameList : FoxGrammarSymbol<SyntaxList<SyntaxString>>

data object EnumEntryParameter : FoxGrammarSymbol<SyntaxStringTypePair>
data object EnumEntryParameterListHead : FoxGrammarSymbol<SyntaxList<SyntaxStringTypePair>>
data object EnumEntryParameterList : FoxGrammarSymbol<SyntaxList<SyntaxStringTypePair>>

data object EnumEntryName : FoxGrammarSymbol<SyntaxString>
data object EnumEntryNameList : FoxGrammarSymbol<SyntaxList<SyntaxString>>

data object MethodTypeParameter : FoxGrammarSymbol<SyntaxStringTypePair>
data object MethodTypeParameterListHead : FoxGrammarSymbol<SyntaxMethodTypeHead>
data object MethodTypeParameterList : FoxGrammarSymbol<SyntaxMethodType>

data object FormalParameter : FoxGrammarSymbol<SyntaxStringTypePair>
data object FormalParameterListHead : FoxGrammarSymbol<SyntaxList<SyntaxStringTypePair>>
data object FormalParameterList : FoxGrammarSymbol<SyntaxList<SyntaxStringTypePair>>

data object ActualParameter : FoxGrammarSymbol<SyntaxPair<SyntaxString?, SyntaxStatement<*>>>
data object ActualParameterListHead : FoxGrammarSymbol<SyntaxList<SyntaxPair<SyntaxString?, SyntaxStatement<*>>>>
data object ActualParameterList : FoxGrammarSymbol<SyntaxList<SyntaxPair<SyntaxString?, SyntaxStatement<*>>>>

data object FormalGenericParameter : FoxGrammarSymbol<SyntaxStringOptionalTypePair>
data object FormalGenericParameterListHead : FoxGrammarSymbol<SyntaxList<SyntaxStringOptionalTypePair>>
data object FormalGenericParameterList : FoxGrammarSymbol<SyntaxList<SyntaxStringOptionalTypePair>>

data object FormalGenericParameterNoConstraints : FoxGrammarSymbol<SyntaxString>
data object FormalGenericParameterNoConstraintsListHead : FoxGrammarSymbol<SyntaxList<SyntaxString>>
data object FormalGenericParameterNoConstraintsList : FoxGrammarSymbol<SyntaxList<SyntaxString>>

data object ActualGenericParameter : FoxGrammarSymbol<SyntaxOptionalStringTypePair>
data object ActualGenericParameterListHead : FoxGrammarSymbol<SyntaxList<SyntaxOptionalStringTypePair>>
data object ActualGenericParameterList : FoxGrammarSymbol<SyntaxList<SyntaxOptionalStringTypePair>>

data object AnonymousActualGenericParameter : FoxGrammarSymbol<SyntaxType<*>>
data object AnonymousActualGenericParameterListHead : FoxGrammarSymbol<SyntaxList<SyntaxType<*>>>
data object AnonymousActualGenericParameterList : FoxGrammarSymbol<SyntaxList<SyntaxType<*>>>
data object AnonymousActualGenericParameterSingleList : FoxGrammarSymbol<SyntaxList<SyntaxType<*>>>

data object ActualGenericIntParameterList : FoxGrammarSymbol<SyntaxPair<SyntaxType<*>, SyntaxInt>>
data object IndexArgumentListHead : FoxGrammarSymbol<SyntaxList<SyntaxStatement<*>>>
data object IndexArgumentList : FoxGrammarSymbol<SyntaxList<SyntaxStatement<*>>>

enum class FoxExpressionSymbol : FoxGrammarSymbol<SyntaxStatement<*>> {
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

data object FormattedStringPart : FoxGrammarSymbol<SyntaxFormattedStringPart>
data object FormattedStringPartList : FoxGrammarSymbol<SyntaxList<SyntaxFormattedStringPart>>

data object LambdaParameter : FoxGrammarSymbol<SyntaxStringOptionalTypePair>
data object LambdaParameterListHead : FoxGrammarSymbol<SyntaxList<SyntaxStringOptionalTypePair>>
data object LambdaParameterList : FoxGrammarSymbol<SyntaxList<SyntaxStringOptionalTypePair>>
data object LambdaStatementBlockHead : FoxGrammarSymbol<SyntaxList<SyntaxStatement<*>>>
data object LambdaBody : FoxGrammarSymbol<SyntaxStatement<*>>
data object ExplicitLambdaLiteral : FoxGrammarSymbol<SyntaxLambda>
data object InlineImplicitLambdaLiteral : FoxGrammarSymbol<SyntaxLambda>
data object ImplicitLambdaLiteral : FoxGrammarSymbol<SyntaxLambda>
data object LambdaLiteral : FoxGrammarSymbol<SyntaxLambda>

data object Label : FoxGrammarSymbol<SyntaxString>

data object Statement : FoxGrammarSymbol<SyntaxStatement<*>>
data object StatementLine : FoxGrammarSymbol<SyntaxStatement<*>>
data object StatementBlockHead : FoxGrammarSymbol<SyntaxList<SyntaxStatement<*>>>
data object StatementBlockCore : FoxGrammarSymbol<SyntaxList<SyntaxStatement<*>>>
data object StatementBlock : FoxGrammarSymbol<SyntaxBlock>

data object WhenCaseConditionList : FoxGrammarSymbol<SyntaxList<SyntaxStatement<*>>>
data object WhenCase : FoxGrammarSymbol<SyntaxCase>
data object WhenCaseLine : FoxGrammarSymbol<SyntaxCase>
data object WhenCaseListHead : FoxGrammarSymbol<SyntaxList<SyntaxCase>>
data object WhenCaseList : FoxGrammarSymbol<SyntaxList<SyntaxCase>>
data object IfCore : FoxGrammarSymbol<SyntaxIf>
data object WhileCore : FoxGrammarSymbol<SyntaxWhile>
data object DoWhileCore : FoxGrammarSymbol<SyntaxDoWhile>
data object WhenCore : FoxGrammarSymbol<SyntaxWhen>
data object FileElementList : FoxGrammarSymbol<SyntaxList<SyntaxFileElement<*>>>

data object File : FoxGrammarSymbol<SyntaxFile>
data object FileElement : FoxGrammarSymbol<SyntaxFileElement<*>>
data object FileElementLine : FoxGrammarSymbol<SyntaxFileElement<*>>
data object TypeAlias : FoxGrammarSymbol<SyntaxTypeAlias>
data object MethodDefinition : FoxGrammarSymbol<SyntaxMethodDefinition>

internal val FoxKeywordsByText: Map<String, FoxGrammarSymbol<SyntaxUnit>> = buildMap {
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

internal val FoxLexicalSymbols = listOf(
    TypeName,
    Identifier,
)

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
