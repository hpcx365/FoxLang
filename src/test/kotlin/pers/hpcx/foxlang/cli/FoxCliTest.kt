package pers.hpcx.foxlang.cli

import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FoxCliTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    @Test
    fun testDiagnoseCommandRendersSyntaxReport() {
        val source = """
def main() {
    @
}
""".trimStart()
        val path = tempDir.resolve("broken.fox")
        Files.write(path, source.toByteArray(StandardCharsets.UTF_8))
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        
        val exitCode = FoxCli.run(arrayOf("diagnose", path.toString()), stdout, stderr)
        
        assertEquals(1, exitCode)
        assertTrue(stderr.isEmpty(), stderr.toString())
        assertTrue(stdout.toString().contains("${path}:2:5: error: Expected expression"), stdout.toString())
        assertTrue(stdout.toString().contains("2 |     @"), stdout.toString())
        assertTrue(stdout.toString().contains("  |     ^"), stdout.toString())
    }
    
    @Test
    fun testDiagnoseCommandReportsOkForExactFile() {
        val source = """
type Good = Int
def main() {
    println("ok")
}
""".trimStart()
        val path = tempDir.resolve("good.fox")
        Files.write(path, source.toByteArray(StandardCharsets.UTF_8))
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        
        val exitCode = FoxCli.run(arrayOf(path.toString()), stdout, stderr)
        
        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty(), stderr.toString())
        assertEquals("${path}: ok\n", stdout.toString())
    }
}
