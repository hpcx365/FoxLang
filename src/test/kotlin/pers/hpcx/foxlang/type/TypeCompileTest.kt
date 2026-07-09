package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.space.*
import pers.hpcx.foxlang.utils.orderedMapOf
import pers.hpcx.foxlang.utils.orderedSetOf
import kotlin.test.*

class TypeCompileTest {
    @Test
    fun compilesStructNameBasedProjectionSpaces() {
        val base = FoxUnresolvedType("S", null)
        
        assertEquals(
            StructFieldProjectionSpace("S", "name"),
            compileType(FoxStructFieldOfType(base, "name")),
        )
        assertEquals(
            StructFieldsProjectionSpace("S", listOf("height", "name")),
            compileType(FoxStructFieldsOfType(base, orderedSetOf("height", "name"))),
        )
        assertEquals(
            StructDropFieldsProjectionSpace("S", setOf("age")),
            compileType(FoxStructDropFieldsOfType(base, setOf("age"))),
        )
    }
    
    @Test
    fun compilesStructMergeIntoConcatSpace() {
        val left = FoxStructType(
            orderedMapOf(
                "name" to FoxStringType,
                "age" to FoxIntType,
            ),
        )
        val right = FoxStructType(
            orderedMapOf(
                "height" to FoxDoubleType,
            ),
        )
        val compiled = compileType(FoxStructMergeFieldsOfType(listOf(left, right)))
        val concat = assertIs<StructConcatSpace>(compiled)
        
        assertEquals(2, concat.parts.size)
        assertTrue(
            compiled.contains(
                FoxStructType(
                    orderedMapOf(
                        "name" to FoxStringType,
                        "age" to FoxIntType,
                        "height" to FoxDoubleType,
                    ),
                ),
            ),
        )
        assertFalse(
            compiled.contains(
                FoxStructType(
                    orderedMapOf(
                        "height" to FoxDoubleType,
                        "name" to FoxStringType,
                        "age" to FoxIntType,
                    ),
                ),
            ),
        )
        assertFalse(
            compiled.contains(
                FoxStructType(
                    orderedMapOf(
                        "name" to FoxStringType,
                        "height" to FoxDoubleType,
                    ),
                ),
            ),
        )
    }
    
    @Test
    fun structMergeWithDuplicateNamesMatchesNoConcreteStruct() {
        val compiled = compileType(
            FoxStructMergeFieldsOfType(
                listOf(
                    FoxStructType(orderedMapOf("name" to FoxStringType)),
                    FoxStructType(orderedMapOf("name" to FoxIntType)),
                ),
            ),
        )
        
        assertFalse(compiled.contains(FoxStructType(orderedMapOf("name" to FoxStringType))))
        assertFalse(
            compiled.contains(
                FoxStructType(
                    orderedMapOf(
                        "name" to FoxStringType,
                        "age" to FoxIntType,
                    ),
                ),
            ),
        )
    }
    
    @Test
    fun compilesObjectPatternAndWildcardSpaces() {
        val objectType = FoxObjectType(
            linkedMapOf(
                "name" to FoxStringType,
                "age" to FoxIntType,
            ),
        )
        val pattern = assertIs<ObjectPatternSpace>(compileType(objectType))
        val wildcard = assertIs<ObjectRepeatSpace>(compileType(FoxAnyObjectType))
        
        assertTrue(pattern.contains(FoxObjectType(mapOf("age" to FoxIntType, "name" to FoxStringType))))
        assertFalse(pattern.contains(FoxObjectType(mapOf("name" to FoxStringType))))
        assertTrue(wildcard.contains(FoxObjectType(mapOf("x" to FoxStringType, "y" to FoxIntType))))
    }
    
    @Test
    fun compilesObjectNameBasedProjectionSpaces() {
        val base = FoxUnresolvedType("O", null)
        
        assertEquals(
            ObjectMemberProjectionSpace("O", "name"),
            compileType(FoxObjectMemberOfType(base, "name")),
        )
        assertEquals(
            ObjectMembersProjectionSpace("O", setOf("height", "name")),
            compileType(FoxObjectMembersOfType(base, setOf("height", "name"))),
        )
        assertEquals(
            ObjectDropMembersProjectionSpace("O", setOf("age")),
            compileType(FoxObjectDropMembersOfType(base, setOf("age"))),
        )
    }
    
    @Test
    fun compilesObjectMergeIgnoringConcreteMemberOrder() {
        val left = FoxObjectType(
            linkedMapOf(
                "name" to FoxStringType,
                "age" to FoxIntType,
            ),
        )
        val right = FoxObjectType(
            linkedMapOf(
                "height" to FoxDoubleType,
            ),
        )
        val compiled = assertIs<ObjectMergeSpace>(compileType(FoxObjectMergeMembersOfType(listOf(left, right))))
        
        assertTrue(
            compiled.contains(
                FoxObjectType(
                    linkedMapOf(
                        "height" to FoxDoubleType,
                        "name" to FoxStringType,
                        "age" to FoxIntType,
                    ),
                ),
            ),
        )
        assertFalse(
            compiled.contains(
                FoxObjectType(
                    linkedMapOf(
                        "name" to FoxStringType,
                        "age" to FoxIntType,
                    ),
                ),
            ),
        )
    }
    
    @Test
    fun objectMergeWithDuplicateNamesMatchesNoConcreteObject() {
        val compiled = compileType(
            FoxObjectMergeMembersOfType(
                listOf(
                    FoxObjectType(mapOf("name" to FoxStringType)),
                    FoxObjectType(mapOf("name" to FoxIntType)),
                ),
            ),
        )
        
        assertFalse(compiled.contains(FoxObjectType(mapOf("name" to FoxStringType))))
        assertFalse(compiled.contains(FoxObjectType(mapOf("name" to FoxStringType, "age" to FoxIntType))))
    }
    
    @Test
    fun compilesEnumPatternAndWildcardSpaces() {
        val enumType = FoxEnumType(
            linkedMapOf(
                "Ok" to FoxStringType,
                "Err" to FoxIntType,
            ),
        )
        val pattern = assertIs<EnumPatternSpace>(compileType(enumType))
        val wildcard = assertIs<EnumRepeatSpace>(compileType(FoxAnyEnumType))
        
        assertTrue(pattern.contains(FoxEnumType(mapOf("Err" to FoxIntType, "Ok" to FoxStringType))))
        assertFalse(pattern.contains(FoxEnumType(mapOf("Ok" to FoxStringType))))
        assertTrue(wildcard.contains(FoxEnumType(mapOf("X" to FoxStringType, "Y" to FoxIntType))))
    }
    
    @Test
    fun compilesEnumNameBasedProjectionSpaces() {
        val base = FoxUnresolvedType("E", null)
        
        assertEquals(
            EnumEntryProjectionSpace("E", "Ok"),
            compileType(FoxEnumEntryOfType(base, "Ok")),
        )
        assertEquals(
            EnumEntriesProjectionSpace("E", setOf("Err", "Ok")),
            compileType(FoxEnumEntriesOfType(base, setOf("Err", "Ok"))),
        )
        assertEquals(
            EnumDropItemsProjectionSpace("E", setOf("Err")),
            compileType(FoxEnumDropEntriesOfType(base, setOf("Err"))),
        )
    }
    
    @Test
    fun compilesEnumMergeIgnoringConcreteItemOrder() {
        val left = FoxEnumType(
            linkedMapOf(
                "Ok" to FoxStringType,
                "Err" to FoxIntType,
            ),
        )
        val right = FoxEnumType(
            linkedMapOf(
                "Pending" to FoxDoubleType,
            ),
        )
        val compiled = assertIs<EnumMergeSpace>(compileType(FoxEnumMergeEntriesOfType(listOf(left, right))))
        
        assertTrue(
            compiled.contains(
                FoxEnumType(
                    linkedMapOf(
                        "Pending" to FoxDoubleType,
                        "Err" to FoxIntType,
                        "Ok" to FoxStringType,
                    ),
                ),
            ),
        )
        assertFalse(
            compiled.contains(
                FoxEnumType(
                    linkedMapOf(
                        "Ok" to FoxStringType,
                        "Err" to FoxIntType,
                    ),
                ),
            ),
        )
    }
    
    @Test
    fun enumMergeWithDuplicateNamesMatchesNoConcreteEnum() {
        val compiled = compileType(
            FoxEnumMergeEntriesOfType(
                listOf(
                    FoxEnumType(mapOf("Ok" to FoxStringType)),
                    FoxEnumType(mapOf("Ok" to FoxIntType)),
                ),
            ),
        )
        
        assertFalse(compiled.contains(FoxEnumType(mapOf("Ok" to FoxStringType))))
        assertFalse(compiled.contains(FoxEnumType(mapOf("Ok" to FoxStringType, "Err" to FoxIntType))))
    }
    
    @Test
    fun compilesMethodPatternUsingStructCompatibleParameters() {
        val method = FoxMethodType(
            FoxStringType,
            orderedMapOf(
                "left" to FoxIntType,
                "right" to FoxDoubleType,
            ),
            FoxBoolType,
        )
        val compiled = assertIs<MethodSpace>(compileType(method))
        
        assertTrue(compiled.contains(method))
        assertTrue(
            compiled.contains(
                FoxMethodType(
                    FoxStringType,
                    orderedMapOf(
                        "left" to FoxIntType,
                        "right" to FoxDoubleType,
                    ),
                    FoxBoolType,
                ),
            ),
        )
        assertFalse(
            compiled.contains(
                FoxMethodType(
                    FoxStringType,
                    orderedMapOf(
                        "right" to FoxDoubleType,
                        "left" to FoxIntType,
                    ),
                    FoxBoolType,
                ),
            ),
        )
    }
    
    @Test
    fun compilesMethodTransformAndProjectionSpaces() {
        val base = FoxUnresolvedType("M", null)
        
        assertEquals(
            MethodThisProjectionSpace("M"),
            compileType(FoxMethodThisOfType(base)),
        )
        assertEquals(
            MethodParametersProjectionSpace("M"),
            compileType(FoxMethodParametersOfType(base)),
        )
        assertEquals(
            MethodReturnProjectionSpace("M"),
            compileType(FoxMethodReturnOfType(base)),
        )
        
        val compiled = assertIs<MethodSpace>(
            compileType(
                FoxMethodOfType(
                    FoxStringType,
                    FoxStructType(
                        orderedMapOf(
                            "left" to FoxIntType,
                            "right" to FoxDoubleType,
                        ),
                    ),
                    FoxBoolType,
                ),
            ),
        )
        
        assertTrue(
            compiled.contains(
                FoxMethodType(
                    FoxStringType,
                    orderedMapOf(
                        "left" to FoxIntType,
                        "right" to FoxDoubleType,
                    ),
                    FoxBoolType,
                ),
            ),
        )
        assertFalse(
            compiled.contains(
                FoxMethodType(
                    FoxStringType,
                    orderedMapOf("left" to FoxIntType),
                    FoxBoolType,
                ),
            ),
        )
    }
}
