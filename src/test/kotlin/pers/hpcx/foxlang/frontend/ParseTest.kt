package pers.hpcx.foxlang.frontend

import pers.hpcx.foxlang.frontend.parser.Cursor
import pers.hpcx.foxlang.frontend.parser.Success
import pers.hpcx.foxlang.frontend.parser.node
import pers.hpcx.foxlang.frontend.parser.render
import kotlin.test.*

class ParseTest {
    
    @Test
    fun test() {
        val grammar = FoxGrammar.check(setOf(node<NodeFile>()))
        assertTrue(grammar.undefinedNonTerminals.isEmpty(), grammar.render())
        assertTrue(grammar.duplicateProductions.isEmpty(), grammar.render())
        val run = FoxGrammar.parseWithDiagnostics(
            source = source,
            root = node<NodeFile>(),
            cursor = Cursor(0),
            starters = FoxStarters,
        )
        val context = grammar.render() + run.stop.render()
        val file = assertIs<Success<NodeFile>>(run.result, context)
        assertNotNull(file.node)
        assertEquals(run.scanner.fragments.size, file.interval.end.fragIndex, context)
    }
    
    @Test
    fun structuralProjectionTypesParseAsBuiltIns() {
        val file = parse(
            """
            type UserFields = Denamed<Struct<name: String, age: Int>>
            type UserTuple = Named<Tuple<String, Int>>
            """.trimIndent(),
        )
        val denamed = assertIs<NodeTypeAlias>(file.elements[0]).alias
        val named = assertIs<NodeTypeAlias>(file.elements[1]).alias
        assertIs<NodeDenamedProjectionType>(denamed)
        assertIs<NodeNamedProjectionType>(named)
        assertEquals("Denamed<Struct<name: String, age: Int>>", denamed.toSource())
        assertEquals("Named<Tuple<String, Int>>", named.toSource())
    }
    
    @Test
    fun tupleAndStructSpreadTypesParse() {
        val file = parse(
            """
            type ExpandedTuple = Tuple<Int, *Another, Bool>
            type ExpandedStruct = Struct<name: String, *Address, active: Bool>
            """.trimIndent(),
        )
        val tupleAlias = assertIs<NodeTypeAlias>(file.elements[0]).alias
        val structAlias = assertIs<NodeTypeAlias>(file.elements[1]).alias
        
        val tuple = assertIs<NodeTupleType>(tupleAlias)
        assertEquals(3, tuple.items.size)
        assertIs<NodeTupleTypeItem>(tuple.items[0])
        assertIs<NodeTupleSpreadItem>(tuple.items[1])
        assertIs<NodeTupleTypeItem>(tuple.items[2])
        assertEquals("Tuple<Int, *Another, Bool>", tuple.toSource())
        
        val struct = assertIs<NodeStructType>(structAlias)
        assertEquals(3, struct.items.size)
        assertIs<NodeStructFieldItem>(struct.items[0])
        assertIs<NodeStructSpreadItem>(struct.items[1])
        assertIs<NodeStructFieldItem>(struct.items[2])
        assertEquals("Struct<name: String, *Address, active: Bool>", struct.toSource())
    }
    
    @Test
    fun wildcardStructSplatAndGenForParse() {
        val file = parse(
            """
            def <T = Struct<*>> forEachPrint(*T) {
                genfor (f: F in T) {
                    print<F>(f)
                }
            }
            """.trimIndent(),
        )
        val method = assertIs<NodeMethodDefinition>(file.elements.single())
        assertEquals("forEachPrint", method.name)
        assertIs<NodeSplatFormalParameter>(method.parameters.single())
        val genericConstraint = method.generics!!.getValue("T")
        assertIs<NodeStructWildcardType>(genericConstraint.match)
        val body = assertIs<NodeBlock>(method.body)
        val genFor = assertIs<NodeGenFor>(body.statements.single())
        assertEquals("f", genFor.valueName)
        assertEquals("F", genFor.typeName)
        assertEquals("T", assertIs<NodeNamedType>(genFor.targetType).name)
        assertEquals(
            "def <T = Struct<*>> forEachPrint(*T) {\n    genfor (f: F in T) {\n        print<F>(f)\n    }\n}",
            method.toSource(),
        )
    }
    
    private fun parse(source: String): NodeFile {
        val run = FoxGrammar.parseWithDiagnostics(
            source = source,
            root = node<NodeFile>(),
            cursor = Cursor(0),
            starters = FoxStarters,
        )
        val file = assertIs<Success<NodeFile>>(run.result, run.stop.render())
        assertEquals(run.scanner.fragments.size, file.interval.end.fragIndex, run.stop.render())
        return file.node
    }
}

const val source = """
// This is a comment

/* This is a block comment, line 1
 * This is a block comment, line 2
 * This is a block comment, line 3
 */
 
type MyTuple = Tuple<Unit, Bool, Byte, Short, Int, Long, Float, Double, Char, String>
type ExpandedTuple = Tuple<Int, *Another, Bool>
type MyArray = Array<MyTuple>
type MyEnum = Enum<Success = Int, Failure = String>
type MyStruct = Struct<name: String, age: Int, height: Double>
type ExpandedStruct = Struct<name: String, *Address, active: Bool>
type AnyStruct = Struct<*>
type MyUnnamedTuple = Denamed<Struct<left: Int, right: String>>
type MyNamedStruct = Named<Tuple<Int, String>>
type MyMap = Map<String, MyEnum>

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

def <T = Struct<*>> forEachPrint(*T) {
    genfor (f: F in T) {
        print<F>(f)
    }
}
"""

