package pers.hpcx.foxlang.frontend

import pers.hpcx.foxlang.frontend.fox.SourceFragmentationSuccess
import pers.hpcx.foxlang.frontend.fox.StringLiteralFragment
import pers.hpcx.foxlang.frontend.fox.sourceFox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FoxFragmentTest {
    
    @Test
    fun testStringUnicodeEscapeUsesLiteralRelativeOffset() {
        val sourceText = "value := \"" + "\\u0041" + "\""
        val source = assertIs<SourceFragmentationSuccess>(sourceText.sourceFox()).value
        
        val fragment = source.fragments.filterIsInstance<StringLiteralFragment>().single()
        assertEquals("A", fragment.string)
    }
}
