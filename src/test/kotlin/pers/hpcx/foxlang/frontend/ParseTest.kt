package pers.hpcx.foxlang.frontend

import pers.hpcx.foxlang.ast.FoxFile
import pers.hpcx.foxlang.ast.FoxGrammar
import pers.hpcx.foxlang.ast.toSource
import pers.hpcx.foxlang.parser.Parser
import pers.hpcx.foxlang.parser.Success
import pers.hpcx.foxlang.parser.node
import kotlin.test.*

class ParseTest {
    
    @Test
    fun test() {
        val grammar = FoxGrammar.check(setOf(node<FoxFile>()))
        assertTrue(grammar.undefinedNonTerminals.isEmpty(), grammar.toString())
        val file = parseFile(source, grammar.toString())
        val printed = file.node.toSource()
        val reparsed = parseFile(printed, "Rendered source should stay parseable:\n$printed\n")
        assertEquals(file.node, reparsed.node)
    }
    
    @Test
    fun testComponentAtType() {
        parseFile("type X = ComponentAt<MyTuple, 4>", "")
    }
    
    @Test
    fun testMethodType() {
        parseFile("type X = Method<MyStruct, left: Int, right: String, Bool>", "")
    }
    
    private fun parseFile(source: String, prefix: String): Success<FoxFile> {
        val parser = Parser(FoxGrammar, node<FoxFile>())
        val report = parser.parse(source)
        val context = prefix + report.stop.toString()
        val file = assertIs<Success<FoxFile>>(report.result, context)
        assertNotNull(file.node)
        assertEquals(report.context.fragments.size, file.interval.end.fragIndex, context)
        return file
    }
}

const val source = """
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
type MyWildcards = Tuple<Any, AnyTuple, AnyStruct, AnyObject, AnyEnum, AnyArray, AnyRef, AnyMethod>

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
type MyMethodThis = ThisOf<MyMethod>
type MyMethodParameters = ParametersOf<MyMethod>
type MyMethodReturn = ReturnOf<MyMethod>

def <T = AnyStruct, E = ItemOf<MyEnum, Success>> MergeFieldsOf<MyStruct, Struct<nick: String>>.describe(
    sample: FieldsOf<MyStruct, name, age>,
    item: E,
    value: T,
): FieldOf<MyStruct, name> {
    return "fox"
}

def Int.collatz(): Ref<List<Int>> {
    result := Ref<ArrayList<Int>>()
    
    i := this
    println((println)("indirect call"))
    result.(forEach)({ println(it) })
    #loop while (i != 1) {
        result += i
        i = if (i % 2 == 0) i / 2 else {
            j := 3 * i + 1
            when {
                j > 1_000_000_000 -> {
                    println(f"Wow, a \"huge\" number: {j}, I can't handle it.")
                    break #loop
                }
                j > 1_000_000 -> println(f"\\Amazing\\ number: {j}")
                j > 10_000 -> println(f"Cool number: {j}")
                else -> {}
            }
            yield j
        }
    }
    result += 1
    
    return result
}

def main(args: Array<String>) {
    println("Hello, I am fox! Let's count numbers!")
    
    i := read<Int>()
    if (i <= 0) panic("Invalid input! Expected a positive number.")
    if (i > 10_000) panic("Invalid input! Expected a number less than 10,000.")
    
    result := i.collatz()
    println("Collatz sequence:")
    result.forEach { println(it) }
    
    if (false) {
        println('a')
        println('3')
        println('"')
        println('\n')
        println('\t')
        println('\r')
        println('\b')
        println('\\')
        println('\'')
        println('\u0000')
        println('\u0abc')
    }
}
"""
