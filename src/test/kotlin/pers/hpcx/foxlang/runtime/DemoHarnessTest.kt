package pers.hpcx.foxlang.runtime

import pers.hpcx.foxlang.frontend.FoxGrammar
import pers.hpcx.foxlang.frontend.FoxStarters
import pers.hpcx.foxlang.frontend.NodeFile
import pers.hpcx.foxlang.frontend.NodeNamedFormalParameter
import pers.hpcx.foxlang.frontend.parser.Cursor
import pers.hpcx.foxlang.frontend.parser.Success
import pers.hpcx.foxlang.frontend.parser.node
import pers.hpcx.foxlang.frontend.parser.render
import pers.hpcx.foxlang.types.FoxLongType
import pers.hpcx.foxlang.types.FoxStringType
import pers.hpcx.foxlang.types.FoxStructType
import pers.hpcx.foxlang.types.Normalized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DemoHarnessTest {
    
    @Test
    fun runsInstantiatedExpandedMethodThroughInterpreter() {
        val file = parse(
            """
            def <T = Struct<*>> demo(*T) {
                genfor (f: F in T) {
                    print<F>(f)
                }
            }
            """.trimIndent(),
        )
        
        val result = assertIs<Normalized<DemoRunResult>>(
            runInstantiatedDemoMethod(
                file = file,
                methodName = "demo",
                genericReplacements = linkedMapOf(
                    "T" to FoxStructType(
                        linkedMapOf(
                            "first" to FoxStringType,
                            "second" to FoxLongType,
                        ),
                    ),
                ),
                parameters = linkedMapOf(
                    "first" to FoxString("hello"),
                    "second" to FoxLong(42),
                ),
            ),
        ).value
        
        assertEquals(listOf("first", "second"), result.expanded.parameters.map { (it as NodeNamedFormalParameter).name })
        assertEquals(listOf("first", "second"), result.signature.parameters.keys.toList())
        assertEquals(listOf("hello", "42"), result.output)
        assertEquals(FoxUnit, result.returnValue)
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
