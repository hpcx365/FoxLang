package pers.hpcx.foxlang.runtime

import pers.hpcx.foxlang.types.FoxIntType
import pers.hpcx.foxlang.types.FoxStringType
import pers.hpcx.foxlang.types.FoxUnitType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MethodSequencingTest {
    
    @Test
    fun methodIdentifierPreservesGenericAndParameterOrder() {
        val identifier = FoxMethodIdentifier(
            name = "demo",
            generics = linkedMapOf(
                "B" to FoxStringType,
                "A" to FoxIntType,
            ),
            thisType = FoxUnitType,
            parameters = linkedMapOf(
                "second" to FoxStringType,
                "first" to FoxIntType,
            ),
        )
        
        assertEquals(listOf("B", "A"), identifier.generics.keys.toList())
        assertEquals(listOf("second", "first"), identifier.parameters.keys.toList())
    }
    
    @Test
    fun instCallPreservesParameterOrder() {
        val inst = InstCall(
            target = SlotConst(FoxUnit),
            params = linkedMapOf(
                "z" to SlotConst(FoxInt(1)),
                "a" to SlotConst(FoxInt(2)),
            ),
            method = panicMethodIdentifier,
        )
        
        assertEquals(listOf("z", "a"), inst.params.keys.toList())
        assertIs<java.util.SequencedMap<String, FoxFetchSlot>>(inst.params)
    }
}
