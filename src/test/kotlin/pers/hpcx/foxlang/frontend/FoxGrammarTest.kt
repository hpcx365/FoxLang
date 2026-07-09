package pers.hpcx.foxlang.frontend

import pers.hpcx.foxlang.frontend.fox.SourceFragmentationSuccess
import pers.hpcx.foxlang.frontend.fox.parseFox
import pers.hpcx.foxlang.frontend.fox.sourceFox
import pers.hpcx.foxlang.frontend.fox.toSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FoxGrammarTest {
    
    @Test
    fun testRoundTrip() {
        val file = assertIs<SourceFragmentationSuccess>(source.sourceFox()).value.parseFox().value().node
        val printed = file.toSource()
        val reparsed = assertIs<SourceFragmentationSuccess>(printed.sourceFox()).value.parseFox().value().node
        assertEquals(file, reparsed)
    }
    
    val source = """
// Round-trip positive grammar fixture.
// The examples are parse-positive only; type and runtime semantics are intentionally irrelevant.

/* Block comments may span lines.
 * Whitespace and comments should not change the parsed file.
 */

// Types: primitive, wildcard, built-in, unresolved, and list sugar.
type VoidAlias = Void
type PrimitiveTypes = Tuple<Unit, Bool, Byte, Short, Int, Long, Float, Double, Char, String>
type WildcardTypes = Tuple<Any, AnyTuple, AnyStruct, AnyObject, AnyEnum>
type CompoundWildcards = Tuple<AnyOf<Int, String, Bool>, AllOf<Any, AnyObject>, NoneOf<Void, Unit>>
type TupleWildcard = AnyTupleOf<Int>
type StructWildcard = AnyStructOf<String, Int, Bool>
type EmptyTuple = Tuple<>
type EmptyStruct = Struct<>
type EmptyObject = Object<>
type EmptyEnum = Enum<>
type VectorTuple = Tuple<Float:3>
type MultilineTuple = Tuple<
    Int:3,
    Long,
    String:1,
>
type PersonStruct = Struct<name: String, age: Int, active: Bool>
type ConfigObject = Object<path: String, retries: Int, enabled: Bool>
type ResultEnum = Enum<Ok = Int, Err = String, Pending = Unit>
type MultilineStruct = Struct<
    first: Int,
    second: String,
    third: Bool,
>
type MultilineObject = Object<
    first: Int,
    second: String,
    third: Bool,
>
type MultilineEnum = Enum<
    Ok = Int,
    Err = String,
    Pending = Unit,
>
type GenericPair<First, Second> = Struct<first: First, second: Second>
type EmptyGeneric<> = Unit
type GenericPairUse = GenericPair<Int, String>
type NestedContainers = Tuple<Array<PersonStruct>, Ref<ConfigObject>, Map<String, ResultEnum>>
type CallbackBox<T> = Object<callback: T>

// Types: method forms.
type MethodEmpty = Method<>
type MethodThisOnly = Method<this: PersonStruct>
type MethodReturnOnly = Method<return: Bool>
type MethodParametersOnly = Method<left: Int, right: String>
type MethodExplicitThisAndReturn = Method<this: PersonStruct, return: Bool>
type MethodExplicitFull = Method<this: PersonStruct, left: Int, right: String, return: Bool>
type MethodMultiline = Method<
    this: PersonStruct,
    left: Int,
    right: String,
    return: Bool,
>

// Types: tuple transforms.
type TupleComponent = ComponentAt<PackedTuple, 1>
type TupleLastComponent = LastComponentAt<PackedTuple, 0>
type TupleFirst = FirstComponentsOf<PackedTuple, 2>
type TupleExactFirst = ExactFirstComponentsOf<PackedTuple, 2>
type TupleLast = LastComponentsOf<PackedTuple, 2>
type TupleExactLast = ExactLastComponentsOf<PackedTuple, 2>
type TupleDropFirst = DropFirstComponentsOf<PackedTuple, 1>
type TupleExactDropFirst = ExactDropFirstComponentsOf<PackedTuple, 1>
type TupleDropLast = DropLastComponentsOf<PackedTuple, 1>
type TupleExactDropLast = ExactDropLastComponentsOf<PackedTuple, 1>
type TupleMerge = MergeComponentsOf<PackedTuple, Tuple<String, Int>>

// Types: struct transforms.
type StructField = FieldOf<PersonStruct, name>
type StructFieldAt = FieldAt<PersonStruct, 1>
type StructLastFieldAt = LastFieldAt<PersonStruct, 0>
type StructFirstFields = FirstFieldsOf<PersonStruct, 2>
type StructExactFirstFields = ExactFirstFieldsOf<PersonStruct, 2>
type StructLastFields = LastFieldsOf<PersonStruct, 2>
type StructExactLastFields = ExactLastFieldsOf<PersonStruct, 2>
type StructDropFirstFields = DropFirstFieldsOf<PersonStruct, 1>
type StructExactDropFirstFields = ExactDropFirstFieldsOf<PersonStruct, 1>
type StructDropLastFields = DropLastFieldsOf<PersonStruct, 1>
type StructExactDropLastFields = ExactDropLastFieldsOf<PersonStruct, 1>
type StructFields = FieldsOf<PersonStruct, name, age>
type StructDropFields = DropFieldsOf<PersonStruct, active>
type StructMerge = MergeFieldsOf<PersonStruct, Struct<nick: String>>

// Types: object, enum, array, ref, and method transforms.
type ObjectMember = MemberOf<ConfigObject, path>
type ObjectMembers = MembersOf<ConfigObject, path, enabled>
type ObjectDropMembers = DropMembersOf<ConfigObject, retries>
type ObjectMerge = MergeMembersOf<ConfigObject, Object<owner: String>>
type EnumEntry = EntryOf<ResultEnum, Ok>
type EnumEntries = EntriesOf<ResultEnum, Ok, Err>
type EnumDropEntries = DropEntriesOf<ResultEnum, Pending>
type EnumMerge = MergeEntriesOf<ResultEnum, Enum<Unknown = Unit>>
type ArrayElement = ElementOf<Array<PersonStruct>>
type RefReferent = ReferentOf<Ref<ConfigObject>>
type MethodOfType = MethodOf<PersonStruct, Struct<value: Int>, Bool>
type MethodThis = ThisOf<MethodFull>
type MethodParameters = ParametersOf<MethodFull>
type MethodReturn = ReturnOf<MethodFull>

// Method heads: optional generics, receiver type, return type, and trailing commas.
def emptyBody() {}

def unitReturn(): Unit {
    return
}

def <T = Any, Row = AnyStructOf<String, Int>> PersonStruct.describe(
    sample: T,
    row: Row,
    callback: Method<this: PersonStruct, value: Int, return: String>,
): FieldOf<PersonStruct, name> {
    return "header"
}

// Statements and expressions: literals, operators, calls, blocks, and control flow.
def syntaxPositive(
    flag: Bool,
    other: Bool,
    tuple: PackedTuple,
    object: ConfigObject,
    callback: Any,
    handler: Any,
) {
    name: String
    unitValue := unit
    boolTrue := true
    boolFalse := false
    binaryInt := 0b1010_0101
    decimalInt := 1_234_567
    hexInt := 0x2a_FF
    binaryLong := 0b1010_0101L
    decimalLong := 123_456L
    hexLong := 0x2aL
    decimalFloat := 12.5f
    exponentFloat := 1e3f
    charLetter := 'a'
    charDigit := '3'
    charQuote := '"'
    charNewline := '\n'
    charTab := '\t'
    charReturn := '\r'
    charBackspace := '\b'
    charBackslash := '\\'
    charSingleQuote := '\''
    charUnicodeZero := '\u0000'
    charUnicodeAbc := '\u0abc'
    normalString := "tab\tbackspace\bnewline\nreturn\rslash\\quote\""
    rawString := r"c:\fox\raw\path"
    rawQuoteString := r"raw \" quote"
    emptyFormatted := f""
    formattedText := f"value {decimalInt + 1}, braces \{open\}, close \}, quote \""
    formattedUnicode := f"unicode \u0041"
    rawFormatted := rf"raw \{brace\} {hexInt}"
    nestedFormatted := f"outer {f"inner {decimalInt}"}"
    parenthesized := (decimalInt)
    parenthesizedAcrossLines := (
        decimalInt + hexInt
    )
    negative := -decimalInt
    inverted := !flag
    multiplicative := decimalInt * 2 / 3 % 4
    additive := decimalInt + 1 - 2
    shifts := decimalInt << 1 >> 2 >>> 3
    comparisons := decimalInt < 10 <= 20 > 5 >= 1
    equalities := decimalInt == hexInt != binaryInt
    bits := binaryInt & 1 ^ 2 | 3
    logical := flag && other || false
    precedence := (decimalInt + 1) * (hexInt - 2) / 3 % 4
    fieldAccess := object.path
    indexAccess := tuple[0]
    multiIndexAccess := tensor[row, column + 1, depth]
    multiIndexTrailingComma := tensor[
        row,
        column,
        depth,
    ]
    dottedAcrossLine := object
        .path
    constructedStruct := PersonStruct("fox", decimalInt, flag)
    constructedGeneric := GenericPair<Int, String>(1, "two")
    constructedWithTrailingLambda := CallbackBox<Any> { it }
    multilineConstruct := PersonStruct(
        name = "fox",
        decimalInt,
        flag,
    )
    noArgCall()
    positionalCall(decimalInt, "fox",)
    expressionCall(decimalInt + 1, text = "fox" + rawString)
    repeatedCall(decimalInt, "fox")
    genericCall<Int, T = String>(decimalInt)
    genericCall<
        Int,
        T = String,
    >(
        decimalInt,
    )
    trailingImplicit { it }
    trailingExplicit { value: Int, text: String -> value + decimalInt }
    trailingEmpty {}
    callWithParametersAndLambda(decimalInt, "fox") { it }
    object
        .update<String>(
            rawString,
        ) { it }
    (callback)(decimalInt)
    (callback) { it }
    handler.(callback)(decimalInt, "fox")
    handler.(callback) { it }
    explicitLambda := { value: Int, text: String -> value }
    explicitLambdaTrailingComma := { value: Int, -> value }
    emptyExplicitLambda := { -> }
    multilineLambda := { value: Int ->
        temp := value + 1
        yield temp
    }
    implicitBlockLambdaCall {
        first := it
        yield first
    }
    decimalInt = decimalInt + 1
    decimalInt = decimalInt = decimalInt + 1
    decimalInt += 1
    decimalInt -= 1
    decimalInt *= 2
    decimalInt /= 3
    decimalInt %= 2
    binaryInt &= 3
    binaryInt |= 4
    binaryInt ^= 1
    binaryInt <<= 1
    binaryInt >>= 1
    binaryInt >>>= 1
    flag &&= other
    flag ||= other
    branch := if (flag) 1 else if (other) 2 else 3
    if (flag) return decimalInt
    if (flag) #thenBlock {
        yield decimalInt
    } else #elseBlock {
        yield hexInt
    }
    #standaloneBlock {
        yield unit
    }
    selected := when (decimalInt) {
        0, 1 -> "small"
        2 -> #caseBlock {
            yield "two"
        }
        else -> "large"
    }
    selectedWithoutValue := when {
        flag -> "flag"
        other -> "other"
        else -> "none"
    }
    #loop while (decimalInt != 0) #loopBody {
        decimalInt -= 1
        if (decimalInt == 3) continue #loop
        if (decimalInt == 2) break #loop
        continue
    }
    while (false) break
    #retry do #retryBody {
        decimalInt += 1
        continue #retry
    } while (decimalInt < 3)
    #yieldBlock {
        yield #yieldBlock decimalInt
    }
    return
}
"""
}
