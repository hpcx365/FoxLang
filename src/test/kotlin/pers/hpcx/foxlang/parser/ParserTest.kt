package pers.hpcx.foxlang.parser

import pers.hpcx.foxlang.ast.FoxFile
import pers.hpcx.foxlang.ast.FoxFileParser
import pers.hpcx.foxlang.ast.toSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParserTest {

    @Test
    fun testParserAndAstSourcePrinter() {
        val file = assertIs<ExactReduction<FoxFile>>(FoxFileParser.parse(source))
        val printed = file.node.toSource()
        val reparsed = assertIs<ExactReduction<FoxFile>>(FoxFileParser.parse(printed))
        assertEquals(file.node, reparsed.node)
    }
    
    val source = """
// This is a comment

/* This is a block comment, line 1
 * This is a block comment, line 2
 * This is a block comment, line 3
 */
 
type MyTuple = Tuple<Unit, Bool, Byte, Short, Int, Long, Float, Double, Char, String>
type MyPackedTuple = Tuple<Int:3, Float:4, Double>
type MyArray = Array<MyTuple>
type MyEnum = Enum<Success = Int, Failure = String>
type MyStruct = Struct<name: String, age: Int, height: Double>
type MyObject = Object<name: String, age: Int, active: Bool>
type MyMap = Map<String, MyEnum>
type MyMethod = Method<MyStruct, left: Int, right: String, Bool>
type MyMethodNoThis = Method<left: Int, right: String, Bool>
type MyMethodNoReturn = Method<MyStruct, left: Int, right: String>
type MyMethodThisOnly = Method<this: MyStruct>
type MyMethodReturnOnly = Method<return: Bool>
type MyMethodThisAndReturn = Method<MyStruct, Bool>
type MyMethodEmpty = Method<>

type MyTupleComponent = ComponentAt<MyTuple, 4>
type MyTupleLastComponent = LastComponentAt<MyTuple, 0>
type MyTupleFirst = FirstComponentsOf<MyTuple, 3>
type MyTupleLast = LastComponentsOf<MyTuple, 2>
type MyTupleDropFirst = DropFirstComponentsOf<MyTuple, 3>
type MyTupleDropLast = DropLastComponentsOf<MyTuple, 2>
type MyTupleMerge = MergeComponentsOf<MyTuple, Tuple<String, Int>>

type MyStructField = FieldOf<MyStruct, name>
type MyStructFieldAt = FieldAt<MyStruct, 1>
type MyStructLastFieldAt = LastFieldAt<MyStruct, 0>
type MyStructFirstFields = FirstFieldsOf<MyStruct, 2>
type MyStructLastFields = LastFieldsOf<MyStruct, 2>
type MyStructDropFirstFields = DropFirstFieldsOf<MyStruct, 1>
type MyStructDropLastFields = DropLastFieldsOf<MyStruct, 1>
type MyStructFields = FieldsOf<MyStruct, name, age>
type MyStructDropFields = DropFieldsOf<MyStruct, height>
type MyStructMerge = MergeFieldsOf<MyStruct, Struct<nick: String>>

type MyObjectMember = MemberOf<MyObject, name>
type MyObjectMembers = MembersOf<MyObject, name, active>
type MyObjectDropMembers = DropMembersOf<MyObject, age>
type MyObjectMerge = MergeMembersOf<MyObject, Object<nick: String>>

type MyEnumItem = ItemOf<MyEnum, Success>
type MyEnumItems = ItemsOf<MyEnum, Success, Failure>
type MyEnumDropItems = DropItemsOf<MyEnum, Failure>
type MyEnumMerge = MergeItemsOf<MyEnum, Enum<Pending = Unit>>

type MyArrayElement = ElementOf<MyArray>
type MyRefReferent = ReferentOf<Ref<MyTuple>>
type MyMethodOf = MethodOf<MyObject, Struct<name: String, age: Int>, Int>
type MyMethodThis = ThisOf<MyMethod>
type MyMethodParameters = ParametersOf<MyMethod>
type MyMethodReturn = ReturnOf<MyMethod>

def <T = AnyStructOf<String, Int>, E = ItemOf<MyEnum, Success>> MergeFieldsOf<MyStruct, Struct<nick: String>>.describe(
    sample: FieldsOf<MyStruct, name, age>,
    item: E,
    value: AnyOf<AnyTupleOf<T>, MyAnyOf>,
    both: AllOf<AnyTupleOf<T>, MyAllOf>,
    neither: NoneOf<MyNoneOf, AnyStructOf<String, Int>>,
): FieldOf<MyStruct, name> {
    return "fox"
}

//def Int.collatz(): Ref<List<Int>> {
//    result := Ref<ArrayList<Int>>()
//
//    i := this
//    println((println)("indirect call"))
//    result.(forEach)({ println(it) })
//    #loop while (i != 1) {
//        result += i
//        i = if (i % 2 == 0) i / 2 else {
//            j := 3 * i + 1
//            when {
//                j > 1_000_000_000 -> {
//                    println(f"Wow, a \"huge\" number: {j}, I can't handle it.")
//                    break #loop
//                }
//                j > 1_000_000 -> println(f"\\Amazing\\ number: {j}")
//                j > 10_000 -> println(f"Cool number: {j}")
//                else -> {}
//            }
//            yield j
//        }
//    }
//    result += 1
//
//    return result
//}
//
//def main(args: Array<String>) {
//    println("Hello, I am fox! Let's count numbers!")
//
//    i := read<Int>()
//    if (i <= 0) panic("Invalid input! Expected a positive number.")
//    if (i > 10_000) panic("Invalid input! Expected a number less than 10,000.")
//
//    result := i.collatz()
//    println("Collatz sequence:")
//    result.forEach { println(it) }
//
//    if (false) {
//        println('a')
//        println('3')
//        println('"')
//        println('\n')
//        println('\t')
//        println('\r')
//        println('\b')
//        println('\\')
//        println('\'')
//        println('\u0000')
//        println('\u0abc')
//    }
//}
"""
}
