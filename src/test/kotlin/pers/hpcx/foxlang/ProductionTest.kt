package pers.hpcx.foxlang

import pers.hpcx.foxlang.utils.Cursor
import pers.hpcx.foxlang.utils.Success
import pers.hpcx.foxlang.utils.node
import kotlin.test.*

class ProductionTest {
    
    @Test
    fun test() {
        val grammar = checkGrammar(FoxProductions, setOf(node<NodeFile>()))
        assertTrue(grammar.undefinedNonTerminals.isEmpty(), grammar.render())
        assertTrue(grammar.duplicateProductions.isEmpty(), grammar.render())
        val run = parseWithDiagnostics(
            source = source,
            productions = FoxProductions,
            root = node<NodeFile>(),
            cursor = Cursor(0),
        )
        val context = grammar.render() + run.stop.render()
        val file = assertIs<Success<NodeFile>>(run.result, context)
        assertNotNull(file.node)
        assertEquals(run.scanner.fragments.size, file.interval.end.fragIndex, context)
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
