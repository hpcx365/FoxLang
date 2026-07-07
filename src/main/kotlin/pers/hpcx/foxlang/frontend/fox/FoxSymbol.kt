package pers.hpcx.foxlang.frontend.fox

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.frontend.common.GrammarSymbol
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet

sealed interface FoxGrammarSymbol<N> : GrammarSymbol<N>

// Terminals
data object LineBreak : FoxGrammarSymbol<Unit>
data object FormattedStringStart : FoxGrammarSymbol<Boolean>
data object FormattedStringEnd : FoxGrammarSymbol<Unit>
data object FormattedExpressionOpen : FoxGrammarSymbol<Unit>
data object FormattedExpressionClose : FoxGrammarSymbol<Unit>

// Punctuations
data object LParen : FoxGrammarSymbol<Unit>
data object RParen : FoxGrammarSymbol<Unit>
data object LBracket : FoxGrammarSymbol<Unit>
data object RBracket : FoxGrammarSymbol<Unit>
data object LBrace : FoxGrammarSymbol<Unit>
data object RBrace : FoxGrammarSymbol<Unit>
data object LAngle : FoxGrammarSymbol<Unit>
data object RAngle : FoxGrammarSymbol<Unit>
data object Dot : FoxGrammarSymbol<Unit>
data object Colon : FoxGrammarSymbol<Unit>
data object Semicolon : FoxGrammarSymbol<Unit>
data object Comma : FoxGrammarSymbol<Unit>
data object Backtick : FoxGrammarSymbol<Unit>
data object At : FoxGrammarSymbol<Unit>
data object Hash : FoxGrammarSymbol<Unit>
data object Dollar : FoxGrammarSymbol<Unit>

data object ParenOpen : FoxGrammarSymbol<Unit>
data object ParenClose : FoxGrammarSymbol<Unit>
data object BracketOpen : FoxGrammarSymbol<Unit>
data object BracketClose : FoxGrammarSymbol<Unit>
data object BraceOpen : FoxGrammarSymbol<Unit>
data object BraceClose : FoxGrammarSymbol<Unit>
data object AngleOpen : FoxGrammarSymbol<Unit>
data object AngleClose : FoxGrammarSymbol<Unit>

// Operators
data object Add : FoxGrammarSymbol<Unit>
data object Sub : FoxGrammarSymbol<Unit>
data object Mul : FoxGrammarSymbol<Unit>
data object Div : FoxGrammarSymbol<Unit>
data object Rem : FoxGrammarSymbol<Unit>
data object And : FoxGrammarSymbol<Unit>
data object Or : FoxGrammarSymbol<Unit>
data object Not : FoxGrammarSymbol<Unit>
data object Xor : FoxGrammarSymbol<Unit>
data object Eq : FoxGrammarSymbol<Unit>
data object Lt : FoxGrammarSymbol<Unit>
data object Gt : FoxGrammarSymbol<Unit>
data object Neq : FoxGrammarSymbol<Unit>
data object Leq : FoxGrammarSymbol<Unit>
data object Geq : FoxGrammarSymbol<Unit>
data object AndAnd : FoxGrammarSymbol<Unit>
data object OrOr : FoxGrammarSymbol<Unit>
data object LShift : FoxGrammarSymbol<Unit>
data object RShift : FoxGrammarSymbol<Unit>
data object URShift : FoxGrammarSymbol<Unit>
data object Arrow : FoxGrammarSymbol<Unit>
data object Assign : FoxGrammarSymbol<Unit>
data object DefAssign : FoxGrammarSymbol<Unit>
data object AddAssign : FoxGrammarSymbol<Unit>
data object SubAssign : FoxGrammarSymbol<Unit>
data object MulAssign : FoxGrammarSymbol<Unit>
data object DivAssign : FoxGrammarSymbol<Unit>
data object RemAssign : FoxGrammarSymbol<Unit>
data object AndAssign : FoxGrammarSymbol<Unit>
data object OrAssign : FoxGrammarSymbol<Unit>
data object XorAssign : FoxGrammarSymbol<Unit>
data object LShiftAssign : FoxGrammarSymbol<Unit>
data object RShiftAssign : FoxGrammarSymbol<Unit>
data object URShiftAssign : FoxGrammarSymbol<Unit>
data object AndAndAssign : FoxGrammarSymbol<Unit>
data object OrOrAssign : FoxGrammarSymbol<Unit>

// Keywords
data object KwConst : FoxGrammarSymbol<Unit>
data object KwType : FoxGrammarSymbol<Unit>
data object KwDef : FoxGrammarSymbol<Unit>
data object KwThis : FoxGrammarSymbol<Unit>
data object KwIf : FoxGrammarSymbol<Unit>
data object KwElse : FoxGrammarSymbol<Unit>
data object KwWhen : FoxGrammarSymbol<Unit>
data object KwNew : FoxGrammarSymbol<Unit>
data object KwYield : FoxGrammarSymbol<Unit>
data object KwReturn : FoxGrammarSymbol<Unit>
data object KwFor : FoxGrammarSymbol<Unit>
data object KwIn : FoxGrammarSymbol<Unit>
data object KwDo : FoxGrammarSymbol<Unit>
data object KwWhile : FoxGrammarSymbol<Unit>
data object KwBreak : FoxGrammarSymbol<Unit>
data object KwContinue : FoxGrammarSymbol<Unit>
data object KwTry : FoxGrammarSymbol<Unit>
data object KwFinally : FoxGrammarSymbol<Unit>
data object KwImport : FoxGrammarSymbol<Unit>
data object KwLowerUnit : FoxGrammarSymbol<Unit>
data object KwTrue : FoxGrammarSymbol<Unit>
data object KwFalse : FoxGrammarSymbol<Unit>
data object KwVoid : FoxGrammarSymbol<Unit>
data object KwUnit : FoxGrammarSymbol<Unit>
data object KwBool : FoxGrammarSymbol<Unit>
data object KwByte : FoxGrammarSymbol<Unit>
data object KwShort : FoxGrammarSymbol<Unit>
data object KwInt : FoxGrammarSymbol<Unit>
data object KwLong : FoxGrammarSymbol<Unit>
data object KwFloat : FoxGrammarSymbol<Unit>
data object KwDouble : FoxGrammarSymbol<Unit>
data object KwChar : FoxGrammarSymbol<Unit>
data object KwString : FoxGrammarSymbol<Unit>
data object KwTuple : FoxGrammarSymbol<Unit>
data object KwStruct : FoxGrammarSymbol<Unit>
data object KwObject : FoxGrammarSymbol<Unit>
data object KwEnum : FoxGrammarSymbol<Unit>
data object KwArray : FoxGrammarSymbol<Unit>
data object KwRef : FoxGrammarSymbol<Unit>
data object KwMethod : FoxGrammarSymbol<Unit>
data object KwAny : FoxGrammarSymbol<Unit>
data object KwAnyOf : FoxGrammarSymbol<Unit>
data object KwAllOf : FoxGrammarSymbol<Unit>
data object KwNoneOf : FoxGrammarSymbol<Unit>
data object KwAnyTuple : FoxGrammarSymbol<Unit>
data object KwAnyTupleOf : FoxGrammarSymbol<Unit>
data object KwAnyStruct : FoxGrammarSymbol<Unit>
data object KwAnyStructOf : FoxGrammarSymbol<Unit>
data object KwAnyObject : FoxGrammarSymbol<Unit>
data object KwAnyEnum : FoxGrammarSymbol<Unit>
data object KwComponentAt : FoxGrammarSymbol<Unit>
data object KwLastComponentAt : FoxGrammarSymbol<Unit>
data object KwFirstComponentsOf : FoxGrammarSymbol<Unit>
data object KwExactFirstComponentsOf : FoxGrammarSymbol<Unit>
data object KwLastComponentsOf : FoxGrammarSymbol<Unit>
data object KwExactLastComponentsOf : FoxGrammarSymbol<Unit>
data object KwDropFirstComponentsOf : FoxGrammarSymbol<Unit>
data object KwExactDropFirstComponentsOf : FoxGrammarSymbol<Unit>
data object KwDropLastComponentsOf : FoxGrammarSymbol<Unit>
data object KwExactDropLastComponentsOf : FoxGrammarSymbol<Unit>
data object KwMergeComponentsOf : FoxGrammarSymbol<Unit>
data object KwFieldOf : FoxGrammarSymbol<Unit>
data object KwFieldAt : FoxGrammarSymbol<Unit>
data object KwLastFieldAt : FoxGrammarSymbol<Unit>
data object KwFirstFieldsOf : FoxGrammarSymbol<Unit>
data object KwExactFirstFieldsOf : FoxGrammarSymbol<Unit>
data object KwLastFieldsOf : FoxGrammarSymbol<Unit>
data object KwExactLastFieldsOf : FoxGrammarSymbol<Unit>
data object KwDropFirstFieldsOf : FoxGrammarSymbol<Unit>
data object KwExactDropFirstFieldsOf : FoxGrammarSymbol<Unit>
data object KwDropLastFieldsOf : FoxGrammarSymbol<Unit>
data object KwExactDropLastFieldsOf : FoxGrammarSymbol<Unit>
data object KwFieldsOf : FoxGrammarSymbol<Unit>
data object KwDropFieldsOf : FoxGrammarSymbol<Unit>
data object KwMergeFieldsOf : FoxGrammarSymbol<Unit>
data object KwMemberOf : FoxGrammarSymbol<Unit>
data object KwMembersOf : FoxGrammarSymbol<Unit>
data object KwDropMembersOf : FoxGrammarSymbol<Unit>
data object KwMergeMembersOf : FoxGrammarSymbol<Unit>
data object KwItemOf : FoxGrammarSymbol<Unit>
data object KwItemsOf : FoxGrammarSymbol<Unit>
data object KwDropItemsOf : FoxGrammarSymbol<Unit>
data object KwMergeItemsOf : FoxGrammarSymbol<Unit>
data object KwElementOf : FoxGrammarSymbol<Unit>
data object KwReferentOf : FoxGrammarSymbol<Unit>
data object KwMethodOf : FoxGrammarSymbol<Unit>
data object KwThisOf : FoxGrammarSymbol<Unit>
data object KwParametersOf : FoxGrammarSymbol<Unit>
data object KwReturnOf : FoxGrammarSymbol<Unit>

// Identifiers
data object CamelWord : FoxGrammarSymbol<String>
data object PascalWord : FoxGrammarSymbol<String>
data object Identifier : FoxGrammarSymbol<String>
data object TypeName : FoxGrammarSymbol<String>
data object IdentifierEqual : FoxGrammarSymbol<String>
data object IdentifierColon : FoxGrammarSymbol<String>
data object TypeNameEqual : FoxGrammarSymbol<String>
data object TypeNameColon : FoxGrammarSymbol<String>
data object Label : FoxGrammarSymbol<String>

// Literals
data object LitUnit : FoxGrammarSymbol<Unit>
data object LitBool : FoxGrammarSymbol<Boolean>
data object LitInt : FoxGrammarSymbol<Int>
data object LitLong : FoxGrammarSymbol<Long>
data object LitFloat : FoxGrammarSymbol<Float>
data object LitDouble : FoxGrammarSymbol<Double>
data object LitChar : FoxGrammarSymbol<Char>
data object LitString : FoxGrammarSymbol<String>

// Operators
data object UnaryOperator : FoxGrammarSymbol<FoxUnaryOperator>
data object BinaryOperator : FoxGrammarSymbol<FoxBinaryOperator>
data object AssignOperator : FoxGrammarSymbol<FoxAssignOperator>

// Parameter and generic nodes
data object Type : FoxGrammarSymbol<FoxType>

data object FormalParameter : FoxGrammarSymbol<Pair<String, FoxType>>
data object FormalParameterListHead : FoxGrammarSymbol<List<Pair<String, FoxType>>>
data object FormalParameterList : FoxGrammarSymbol<OrderedMap<String, FoxType>>

data object ActualParameter : FoxGrammarSymbol<Pair<String?, FoxStatement>>
data object ActualParameterListHead : FoxGrammarSymbol<List<Pair<String?, FoxStatement>>>
data object ActualParameterList : FoxGrammarSymbol<List<Pair<String?, FoxStatement>>>
data object LambdaParameter : FoxGrammarSymbol<Pair<String, FoxType?>>
data object LambdaParameterListHead : FoxGrammarSymbol<List<Pair<String, FoxType?>>>
data object LambdaParameterList : FoxGrammarSymbol<List<Pair<String, FoxType?>>>

data object FormalGenericParameter : FoxGrammarSymbol<Pair<String, FoxType>>
data object FormalGenericParameterListHead : FoxGrammarSymbol<List<Pair<String, FoxType>>>
data object FormalGenericParameterList : FoxGrammarSymbol<OrderedMap<String, FoxType>>

data object FormalGenericParameterNoConstraints : FoxGrammarSymbol<String>
data object FormalGenericParameterNoConstraintsListHead : FoxGrammarSymbol<List<String>>
data object FormalGenericParameterNoConstraintsList : FoxGrammarSymbol<OrderedSet<String>>

data object ActualGenericParameter : FoxGrammarSymbol<Pair<String?, FoxType>>
data object ActualGenericParameterListHead : FoxGrammarSymbol<List<Pair<String?, FoxType>>>
data object ActualGenericParameterList : FoxGrammarSymbol<List<Pair<String?, FoxType>>>

data object NamedActualGenericParameter : FoxGrammarSymbol<Pair<String, FoxType>>
data object NamedActualGenericParameterListHead : FoxGrammarSymbol<List<Pair<String, FoxType>>>
data object NamedActualGenericParameterList : FoxGrammarSymbol<Map<String, FoxType>>

data object AnonymousActualGenericParameter : FoxGrammarSymbol<FoxType>
data object AnonymousActualGenericParameterListHead : FoxGrammarSymbol<List<FoxType>>
data object AnonymousActualGenericParameterList : FoxGrammarSymbol<List<FoxType>>

data object TupleComponentParameter : FoxGrammarSymbol<Pair<FoxType, Int>>
data object TupleComponentParameterListHead : FoxGrammarSymbol<List<Pair<FoxType, Int>>>
data object TupleComponentParameterList : FoxGrammarSymbol<List<Pair<FoxType, Int>>>

data object StructFieldParameter : FoxGrammarSymbol<Pair<String, FoxType>>
data object StructFieldParameterListHead : FoxGrammarSymbol<List<Pair<String, FoxType>>>
data object StructFieldParameterList : FoxGrammarSymbol<OrderedMap<String, FoxType>>

data object StructFieldName : FoxGrammarSymbol<String>
data object StructFieldNameListHead : FoxGrammarSymbol<List<String>>
data object StructFieldNameList : FoxGrammarSymbol<OrderedSet<String>>

data object ObjectMemberParameter : FoxGrammarSymbol<Pair<String, FoxType>>
data object ObjectMemberParameterListHead : FoxGrammarSymbol<List<Pair<String, FoxType>>>
data object ObjectMemberParameterList : FoxGrammarSymbol<Map<String, FoxType>>

data object ObjectMemberName : FoxGrammarSymbol<String>
data object ObjectMemberNameListHead : FoxGrammarSymbol<List<String>>
data object ObjectMemberNameList : FoxGrammarSymbol<Set<String>>

data object EnumItemParameter : FoxGrammarSymbol<Pair<String, FoxType>>
data object EnumItemParameterListHead : FoxGrammarSymbol<List<Pair<String, FoxType>>>
data object EnumItemParameterList : FoxGrammarSymbol<Map<String, FoxType>>

data object EnumItemName : FoxGrammarSymbol<String>
data object EnumItemNameListHead : FoxGrammarSymbol<List<String>>
data object EnumItemNameList : FoxGrammarSymbol<Set<String>>

data object MethodTypeArgument : FoxGrammarSymbol<ParsedMethodTypeArgument>
data object MethodTypeArgumentListHead : FoxGrammarSymbol<List<ParsedMethodTypeArgument>>
data object MethodTypeArgumentList : FoxGrammarSymbol<List<ParsedMethodTypeArgument>>

// Expression nodes
data object StatementLine : FoxGrammarSymbol<FoxStatement>
data object StatementBlockHead : FoxGrammarSymbol<List<FoxStatement>>
data object StatementBlockCore : FoxGrammarSymbol<List<FoxStatement>>
data object StatementBlock : FoxGrammarSymbol<FoxBlock>
data object Statement : FoxGrammarSymbol<FoxStatement>
data object ParenthesizedStatement : FoxGrammarSymbol<FoxStatement>
data object PrimaryExpression : FoxGrammarSymbol<FoxStatement>
data object PostfixExpression : FoxGrammarSymbol<FoxStatement>
data object UnaryExpression : FoxGrammarSymbol<FoxStatement>
data object MultiplicativeExpression : FoxGrammarSymbol<FoxStatement>
data object AdditiveExpression : FoxGrammarSymbol<FoxStatement>
data object ShiftExpression : FoxGrammarSymbol<FoxStatement>
data object ComparisonExpression : FoxGrammarSymbol<FoxStatement>
data object EqualityExpression : FoxGrammarSymbol<FoxStatement>
data object BitAndExpression : FoxGrammarSymbol<FoxStatement>
data object BitXorExpression : FoxGrammarSymbol<FoxStatement>
data object BitOrExpression : FoxGrammarSymbol<FoxStatement>
data object LogicalAndExpression : FoxGrammarSymbol<FoxStatement>
data object LogicalOrExpression : FoxGrammarSymbol<FoxStatement>
data object AssignableExpression : FoxGrammarSymbol<FoxStatement>
data object AssignmentExpression : FoxGrammarSymbol<FoxStatement>
data object ControlBody : FoxGrammarSymbol<FoxStatement>
data object FormattedStringPart : FoxGrammarSymbol<FoxFormattedStringPart>
data object FormattedStringPartListHead : FoxGrammarSymbol<List<FoxFormattedStringPart>>
data object LambdaStatementBlockHead : FoxGrammarSymbol<List<FoxStatement>>
data object LambdaBody : FoxGrammarSymbol<FoxStatement>
data object ExplicitLambdaLiteral : FoxGrammarSymbol<FoxLambda>
data object InlineImplicitLambdaLiteral : FoxGrammarSymbol<FoxLambda>
data object ImplicitLambdaLiteral : FoxGrammarSymbol<FoxLambda>
data object LambdaLiteral : FoxGrammarSymbol<FoxLambda>
data object SingleLineStatementBlock : FoxGrammarSymbol<FoxBlock>

// Operator nodes
data object MultiplicativeOperator : FoxGrammarSymbol<FoxBinaryOperator>
data object AdditiveOperator : FoxGrammarSymbol<FoxBinaryOperator>
data object ShiftOperator : FoxGrammarSymbol<FoxBinaryOperator>
data object ComparisonOperator : FoxGrammarSymbol<FoxBinaryOperator>
data object EqualityOperator : FoxGrammarSymbol<FoxBinaryOperator>
data object BitAndOperator : FoxGrammarSymbol<FoxBinaryOperator>
data object BitXorOperator : FoxGrammarSymbol<FoxBinaryOperator>
data object BitOrOperator : FoxGrammarSymbol<FoxBinaryOperator>
data object LogicalAndOperator : FoxGrammarSymbol<FoxBinaryOperator>
data object LogicalOrOperator : FoxGrammarSymbol<FoxBinaryOperator>

// Control-flow nodes
data object WhenCaseConditionListHead : FoxGrammarSymbol<List<FoxStatement>>
data object WhenCaseConditionList : FoxGrammarSymbol<List<FoxStatement>>
data object WhenCase : FoxGrammarSymbol<FoxCase>
data object WhenCaseLine : FoxGrammarSymbol<FoxCase>
data object WhenCaseListHead : FoxGrammarSymbol<List<FoxCase>>
data object WhenCaseList : FoxGrammarSymbol<List<FoxCase>>
data object IfCore : FoxGrammarSymbol<FoxIf>
data object WhileCore : FoxGrammarSymbol<FoxWhile>
data object DoWhileCore : FoxGrammarSymbol<FoxDoWhile>
data object WhenCore : FoxGrammarSymbol<FoxWhen>
data object FileElementList : FoxGrammarSymbol<List<FoxFileElement>>

// Top-level nodes
data object File : FoxGrammarSymbol<FoxFile>
data object FileElement : FoxGrammarSymbol<FoxFileElement>
data object FileElementLine : FoxGrammarSymbol<FoxFileElement>
data object TypeAlias : FoxGrammarSymbol<FoxTypeAlias>
data object MethodDefinition : FoxGrammarSymbol<FoxMethodDefinition>
data object ThisTypeQualifier : FoxGrammarSymbol<FoxType>
data object ReturnTypeClause : FoxGrammarSymbol<FoxType>
data object MethodHead : FoxGrammarSymbol<FoxMethodDefinition>

internal val Punctuations = mapOf(
    "(" to ParenOpen,
    ")" to ParenClose,
    "[" to BracketOpen,
    "]" to BracketClose,
    "{" to BraceOpen,
    "}" to BraceClose,
    "<" to AngleOpen,
    ">" to AngleClose,
    "." to Dot,
    ":" to Colon,
    ";" to Semicolon,
    "," to Comma,
    "`" to Backtick,
    "@" to At,
    "#" to Hash,
    "$" to Dollar,
)

internal val Operators = mapOf(
    "+" to Add,
    "-" to Sub,
    "*" to Mul,
    "/" to Div,
    "%" to Rem,
    "&" to And,
    "|" to Or,
    "!" to Not,
    "^" to Xor,
    "<" to Lt,
    ">" to Gt,
    "==" to Eq,
    "!=" to Neq,
    "<=" to Leq,
    ">=" to Geq,
    "&&" to AndAnd,
    "||" to OrOr,
    "<<" to LShift,
    ">>" to RShift,
    ">>>" to URShift,
    "->" to Arrow,
    "=" to Assign,
    ":=" to DefAssign,
    "+=" to AddAssign,
    "-=" to SubAssign,
    "*=" to MulAssign,
    "/=" to DivAssign,
    "%=" to RemAssign,
    "&=" to AndAssign,
    "|=" to OrAssign,
    "^=" to XorAssign,
    "<<=" to LShiftAssign,
    ">>=" to RShiftAssign,
    ">>>=" to URShiftAssign,
    "&&=" to AndAndAssign,
    "||=" to OrOrAssign,
)

internal val Keywords = mapOf(
    "const" to KwConst,
    "type" to KwType,
    "def" to KwDef,
    "this" to KwThis,
    "if" to KwIf,
    "else" to KwElse,
    "when" to KwWhen,
    "new" to KwNew,
    "yield" to KwYield,
    "return" to KwReturn,
    "for" to KwFor,
    "in" to KwIn,
    "do" to KwDo,
    "while" to KwWhile,
    "break" to KwBreak,
    "continue" to KwContinue,
    "try" to KwTry,
    "finally" to KwFinally,
    "import" to KwImport,
    "unit" to KwLowerUnit,
    "true" to KwTrue,
    "false" to KwFalse,
    "Void" to KwVoid,
    "Unit" to KwUnit,
    "Bool" to KwBool,
    "Byte" to KwByte,
    "Short" to KwShort,
    "Int" to KwInt,
    "Long" to KwLong,
    "Float" to KwFloat,
    "Double" to KwDouble,
    "Char" to KwChar,
    "String" to KwString,
    "Tuple" to KwTuple,
    "Struct" to KwStruct,
    "Object" to KwObject,
    "Enum" to KwEnum,
    "Array" to KwArray,
    "Ref" to KwRef,
    "Method" to KwMethod,
    "Any" to KwAny,
    "AnyOf" to KwAnyOf,
    "AllOf" to KwAllOf,
    "NoneOf" to KwNoneOf,
    "AnyTuple" to KwAnyTuple,
    "AnyTupleOf" to KwAnyTupleOf,
    "AnyStruct" to KwAnyStruct,
    "AnyStructOf" to KwAnyStructOf,
    "AnyObject" to KwAnyObject,
    "AnyEnum" to KwAnyEnum,
    "ComponentAt" to KwComponentAt,
    "LastComponentAt" to KwLastComponentAt,
    "FirstComponentsOf" to KwFirstComponentsOf,
    "ExactFirstComponentsOf" to KwExactFirstComponentsOf,
    "LastComponentsOf" to KwLastComponentsOf,
    "ExactLastComponentsOf" to KwExactLastComponentsOf,
    "DropFirstComponentsOf" to KwDropFirstComponentsOf,
    "ExactDropFirstComponentsOf" to KwExactDropFirstComponentsOf,
    "DropLastComponentsOf" to KwDropLastComponentsOf,
    "ExactDropLastComponentsOf" to KwExactDropLastComponentsOf,
    "MergeComponentsOf" to KwMergeComponentsOf,
    "FieldOf" to KwFieldOf,
    "FieldAt" to KwFieldAt,
    "LastFieldAt" to KwLastFieldAt,
    "FirstFieldsOf" to KwFirstFieldsOf,
    "ExactFirstFieldsOf" to KwExactFirstFieldsOf,
    "LastFieldsOf" to KwLastFieldsOf,
    "ExactLastFieldsOf" to KwExactLastFieldsOf,
    "DropFirstFieldsOf" to KwDropFirstFieldsOf,
    "ExactDropFirstFieldsOf" to KwExactDropFirstFieldsOf,
    "DropLastFieldsOf" to KwDropLastFieldsOf,
    "ExactDropLastFieldsOf" to KwExactDropLastFieldsOf,
    "FieldsOf" to KwFieldsOf,
    "DropFieldsOf" to KwDropFieldsOf,
    "MergeFieldsOf" to KwMergeFieldsOf,
    "MemberOf" to KwMemberOf,
    "MembersOf" to KwMembersOf,
    "DropMembersOf" to KwDropMembersOf,
    "MergeMembersOf" to KwMergeMembersOf,
    "ItemOf" to KwItemOf,
    "ItemsOf" to KwItemsOf,
    "DropItemsOf" to KwDropItemsOf,
    "MergeItemsOf" to KwMergeItemsOf,
    "ElementOf" to KwElementOf,
    "ReferentOf" to KwReferentOf,
    "MethodOf" to KwMethodOf,
    "ThisOf" to KwThisOf,
    "ParametersOf" to KwParametersOf,
    "ReturnOf" to KwReturnOf,
)

internal val FoxRawOpenDelimiters = listOf<GrammarSymbol<*>>(LParen, LBracket, LBrace, LAngle)
internal val FoxRawCloseDelimiters = listOf<GrammarSymbol<*>>(RParen, RBracket, RBrace, RAngle)
internal val FoxSemanticOpenDelimiters = listOf<GrammarSymbol<*>>(ParenOpen, BracketOpen, BraceOpen, AngleOpen)
internal val FoxSemanticCloseDelimiters = listOf<GrammarSymbol<*>>(ParenClose, BracketClose, BraceClose, AngleClose)

internal val FoxLineContinuationSymbols =
    listOf<GrammarSymbol<*>>(Dot, Comma) + FoxSemanticOpenDelimiters + FoxSemanticCloseDelimiters

internal val FoxDiagnosticDelimiterSymbols =
    listOf<GrammarSymbol<*>>(
        LineBreak,
        FormattedStringStart,
        FormattedStringEnd,
        FormattedExpressionOpen,
        FormattedExpressionClose,
        Dot,
        Comma,
        Arrow,
        KwElse,
        KwWhile,
    ) + FoxSemanticOpenDelimiters + FoxSemanticCloseDelimiters + FoxRawOpenDelimiters + FoxRawCloseDelimiters

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
    FoxDelimitedCommaListSpec(FormalParameterListHead, FormalParameterList, FormalParameter, ParenOpen, ParenClose),
    FoxDelimitedCommaListSpec(ActualParameterListHead, ActualParameterList, ActualParameter, ParenOpen, ParenClose),
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
        NamedActualGenericParameterListHead,
        NamedActualGenericParameterList,
        NamedActualGenericParameter,
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
        EnumItemParameterListHead,
        EnumItemParameterList,
        EnumItemParameter,
        AngleOpen,
        AngleClose,
    ),
    FoxDelimitedCommaListSpec(
        MethodTypeArgumentListHead,
        MethodTypeArgumentList,
        MethodTypeArgument,
        AngleOpen,
        AngleClose,
    ),
)

internal val FoxUndelimitedCommaListSpecs = listOf(
    FoxUndelimitedCommaListSpec(LambdaParameterListHead, LambdaParameterList, LambdaParameter),
    FoxUndelimitedCommaListSpec(StructFieldNameListHead, StructFieldNameList, StructFieldName),
    FoxUndelimitedCommaListSpec(ObjectMemberNameListHead, ObjectMemberNameList, ObjectMemberName),
    FoxUndelimitedCommaListSpec(EnumItemNameListHead, EnumItemNameList, EnumItemName),
)

internal val FoxCommaListSpecs: List<FoxCommaListSpec> = FoxDelimitedCommaListSpecs + FoxUndelimitedCommaListSpecs

internal val FoxCommaListItemSymbols: List<GrammarSymbol<*>> = FoxCommaListSpecs
    .map { it.item }
    .distinct()

internal val FoxCommaListSymbols: List<GrammarSymbol<*>> = FoxCommaListSpecs
    .flatMap { listOf(it.head, it.list) }
    .distinct()

internal val FoxLexicalSymbols = listOf<GrammarSymbol<*>>(
    Identifier,
    TypeName,
    IdentifierEqual,
    IdentifierColon,
    TypeNameEqual,
    TypeNameColon,
    Label,
)

internal val FoxLiteralSymbols = listOf<GrammarSymbol<*>>(
    LitUnit,
    LitBool,
    LitInt,
    LitLong,
    LitFloat,
    LitDouble,
    LitChar,
    LitString,
)

internal val FoxListSymbols = FoxCommaListSymbols + listOf<GrammarSymbol<*>>(
    FormattedStringPartListHead,
    LambdaStatementBlockHead,
    WhenCaseConditionListHead,
    WhenCaseConditionList,
    WhenCaseListHead,
    WhenCaseList,
    FileElementList,
)

internal val FoxExpressionSymbols = listOf<GrammarSymbol<*>>(
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
    Statement,
    Type,
)

internal val FoxStatementSymbols = listOf<GrammarSymbol<*>>(
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

internal val FoxOperatorSymbols = listOf<GrammarSymbol<*>>(
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
    UnaryOperator,
    BinaryOperator,
    AssignOperator,
)

internal val FoxDeclarationSymbols = listOf<GrammarSymbol<*>>(
    FileElementLine,
    ThisTypeQualifier,
    ReturnTypeClause,
    MethodHead,
    FileElement,
    TypeAlias,
    MethodDefinition,
)

internal val FoxTypeArgumentLists: Map<FoxGrammarSymbol<*>, GrammarSymbol<*>> = mapOf(
    *typeArgumentListEntries(
        AnonymousActualGenericParameterList,
        KwAnyOf,
        KwAllOf,
        KwNoneOf,
        KwAnyTupleOf,
        KwAnyStructOf,
        KwArray,
        KwRef,
        KwMergeComponentsOf,
        KwMergeFieldsOf,
        KwMergeMembersOf,
        KwMergeItemsOf,
    ).toTypedArray(),
    KwTuple to TupleComponentParameterList,
    KwStruct to StructFieldParameterList,
    KwObject to ObjectMemberParameterList,
    KwEnum to EnumItemParameterList,
    KwMethod to MethodTypeArgumentList,
)

internal val FoxFixedArityTypeArguments: Map<FoxGrammarSymbol<*>, List<GrammarSymbol<*>>> = mapOf(
    *fixedArityTypeEntries(
        listOf(Type, LitInt),
        KwComponentAt,
        KwLastComponentAt,
        KwFirstComponentsOf,
        KwExactFirstComponentsOf,
        KwLastComponentsOf,
        KwExactLastComponentsOf,
        KwDropFirstComponentsOf,
        KwExactDropFirstComponentsOf,
        KwDropLastComponentsOf,
        KwExactDropLastComponentsOf,
        KwFieldAt,
        KwLastFieldAt,
        KwFirstFieldsOf,
        KwExactFirstFieldsOf,
        KwLastFieldsOf,
        KwExactLastFieldsOf,
        KwDropFirstFieldsOf,
        KwExactDropFirstFieldsOf,
        KwDropLastFieldsOf,
        KwExactDropLastFieldsOf,
    ).toTypedArray(),
    *fixedArityTypeEntries(listOf(Type, Identifier), KwFieldOf, KwMemberOf).toTypedArray(),
    *fixedArityTypeEntries(listOf(Type, StructFieldNameList), KwFieldsOf, KwDropFieldsOf).toTypedArray(),
    *fixedArityTypeEntries(listOf(Type, ObjectMemberNameList), KwMembersOf, KwDropMembersOf).toTypedArray(),
    KwItemOf to listOf(Type, TypeName),
    *fixedArityTypeEntries(listOf(Type, EnumItemNameList), KwItemsOf, KwDropItemsOf).toTypedArray(),
    *fixedArityTypeEntries(listOf(Type), KwElementOf, KwReferentOf, KwThisOf, KwParametersOf, KwReturnOf)
        .toTypedArray(),
    KwMethodOf to listOf(Type, Type, Type),
)

private fun typeArgumentListEntries(
    arguments: GrammarSymbol<*>,
    vararg keywords: FoxGrammarSymbol<*>,
): List<Pair<FoxGrammarSymbol<*>, GrammarSymbol<*>>> {
    return keywords.map { it to arguments }
}

private fun fixedArityTypeEntries(
    arguments: List<GrammarSymbol<*>>,
    vararg keywords: FoxGrammarSymbol<*>,
): List<Pair<FoxGrammarSymbol<*>, List<GrammarSymbol<*>>>> {
    return keywords.map { it to arguments }
}
