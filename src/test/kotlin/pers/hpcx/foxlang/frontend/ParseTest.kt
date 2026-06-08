package pers.hpcx.foxlang.frontend

import pers.hpcx.foxlang.ast.FoxFile
import pers.hpcx.foxlang.ast.FoxGrammar
import pers.hpcx.foxlang.ast.FoxTypeAlias
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
        file.node.elements.filterIsInstance<FoxTypeAlias>().forEach { alias ->
            val reparsedAlias = parseTypeAlias(alias.toSource())
            assertEquals(alias, reparsedAlias.node, alias.toSource())
        }
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
    
    private fun parseTypeAlias(source: String): Success<FoxTypeAlias> {
        val parser = Parser(FoxGrammar, node<FoxTypeAlias>())
        val report = parser.parse(source)
        val context = "Type alias should stay parseable:\n$source\n${report.stop}"
        val alias = assertIs<Success<FoxTypeAlias>>(report.result, context)
        assertEquals(report.context.fragments.size, alias.interval.end.fragIndex, context)
        return alias
    }
}

const val source = """
// This is a comment

/* This is a block comment, line 1
 * This is a block comment, line 2
 * This is a block comment, line 3
 */
 
type MyTuple = Tuple<Unit, Bool, Byte, Short, Int, Long, Float, Double, Char, String>
type MyArray = Array<MyTuple>
type MyEnum = Enum<Success = Int, Failure = String>
type MyStruct = Struct<name: String, age: Int, height: Double>
type MyMap = Map<String, MyEnum>
type MyNamedMap = Map<Key = String, Value = MyEnum>
type MyLambda = Lambda<MyStruct, Int, String, Bool>
type MyWildcards = Tuple<Any, AnyTuple, AnyStruct, AnyEnum, AnyArray, AnyRef, AnyLambda>

type MyTuplePart = PartOf<MyTuple, 4>
type MyTupleFirst = FirstPartsOf<MyTuple, 3>
type MyTupleLast = LastPartsOf<MyTuple, 2>
type MyTupleDropFirst = DropFirstPartsOf<MyTuple, 3>
type MyTupleDropLast = DropLastPartsOf<MyTuple, 2>
type MyTupleMerge = MergePartsOf<MyTuple, Tuple<String, Int>>

type MyStructField = FieldOf<MyStruct, name>
type MyStructFields = FieldsOf<MyStruct, name, age>
type MyStructDropFields = DropFieldsOf<MyStruct, height>
type MyStructMerge = MergeFieldsOf<MyStruct, Struct<nick: String>>

type MyEnumItem = ItemOf<MyEnum, Success>
type MyEnumItems = ItemsOf<MyEnum, Success, Failure>
type MyEnumDropItems = DropItemsOf<MyEnum, Failure>
type MyEnumMerge = MergeItemsOf<MyEnum, Enum<Pending = Unit>>

type MyArrayElement = ElementOf<MyArray>
type MyRefReferent = ReferentOf<Ref<MyTuple>>
type MyLambdaThis = ThisOf<MyLambda>
type MyLambdaParameters = ParametersOf<MyLambda>
type MyLambdaReturn = ReturnOf<MyLambda>

def <T = AnyStruct, E = ItemOf<MyEnum, Success>> MergeFieldsOf<MyStruct, Struct<nick: String>>.describe(
    sample: FieldsOf<MyStruct, name, age>,
    item: E,
    value: T,
): FieldOf<MyStruct, name> {
    return "fox"
}

def collatz(i: Int): Ref<List<Int>> {
    result := Ref<ArrayList<Int>>()
    
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
    
    result := collatz(i)
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
