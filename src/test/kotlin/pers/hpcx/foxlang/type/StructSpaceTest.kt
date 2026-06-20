package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.space.*
import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.orderedMapOf
import kotlin.test.*

class StructSpaceTest : TypeSpaceTestBase() {
    
    @Test
    fun structFieldAtPreimageContainsRequiresMatchingFieldTypeAtIndex() {
        val lang = StructFieldAtPreimageSpace(1, singleSpace(FoxIntType))
        
        assertTrue(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxBoolType,
                        "y" to FoxIntType,
                    ),
                ),
                context,
            ),
        )
        assertTrue(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxBoolType,
                        "y" to FoxIntType,
                        "z" to FoxStringType,
                    ),
                ),
                context,
            ),
        )
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                    ),
                ),
                context,
            ),
        )
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxBoolType,
                        "y" to FoxStringType,
                    ),
                ),
                context,
            ),
        )
    }
    
    @Test
    fun structFieldAtPreimageTraverserStartsFromShortestStructWithConstrainedIndex() {
        val traverser = StructFieldAtPreimageSpace(
            index = 1,
            fieldType = singleSpace(FoxIntType),
        ).traverser(context)
        
        assertEquals(
            FoxStructType(
                orderedMapOf(
                    "x" to FoxVoidType,
                    "y" to FoxIntType,
                ),
            ),
            traverser.current(),
        )
    }
    
    @Test
    fun compileStructFieldAtTypeToProjectiveLang() {
        val lang = compileType(
            FoxStructFieldAtType(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxBoolType,
                        "y" to FoxIntType,
                    ),
                ),
                1,
            ),
            context,
        )
        
        assertEquals(
            StructFieldAtProjectionSpace(
                StructPatternSpace(
                    listOf(
                        structFieldSpace(singleSpace("x"), singleSpace(FoxBoolType)),
                        structFieldSpace(singleSpace("y"), singleSpace(FoxIntType)),
                    ),
                ),
                1,
            ),
            lang,
        )
        assertTrue(lang.contains(FoxIntType, context))
        assertFalse(lang.contains(FoxStringType, context))
    }
    
    @Test
    fun structFieldAtProjectiveContainsUsesPreimageIntersection() {
        val lang = StructFieldAtProjectionSpace(
            union(
                singleSpace(
                    FoxStructType(
                        orderedMapOf(
                            "x" to FoxBoolType,
                            "y" to FoxIntType,
                        ),
                    ),
                ),
                singleSpace(
                    FoxStructType(
                        orderedMapOf(
                            "x" to FoxStringType,
                            "y" to FoxStringType,
                        ),
                    ),
                ),
            ),
            1,
        )
        
        assertTrue(lang.contains(FoxIntType, context))
        assertTrue(lang.contains(FoxStringType, context))
        assertFalse(lang.contains(FoxBoolType, context))
    }
    
    @Test
    fun structLastFieldAtPreimageContainsRequiresMatchingFieldTypeFromTail() {
        val lang = StructLastFieldAtPreimageSpace(1, singleSpace(FoxIntType))
        
        assertTrue(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                context,
            ),
        )
        assertTrue(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxStringType,
                        "y" to FoxIntType,
                        "z" to FoxBoolType,
                    ),
                ),
                context,
            ),
        )
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                    ),
                ),
                context,
            ),
        )
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxBoolType,
                        "y" to FoxStringType,
                    ),
                ),
                context,
            ),
        )
    }
    
    @Test
    fun structLastFieldAtPreimageTraverserStartsFromShortestStructWithConstrainedTailIndex() {
        val traverser = StructLastFieldAtPreimageSpace(
            index = 1,
            fieldType = singleSpace(FoxIntType),
        ).traverser(context)
        
        assertEquals(
            FoxStructType(
                orderedMapOf(
                    "x" to FoxIntType,
                    "y" to FoxVoidType,
                ),
            ),
            traverser.current(),
        )
    }
    
    @Test
    fun compileStructLastFieldAtTypeToProjectiveLang() {
        val lang = compileType(
            FoxStructLastFieldAtType(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                1,
            ),
            context,
        )
        
        assertEquals(
            StructLastFieldAtProjectionSpace(
                StructPatternSpace(
                    listOf(
                        structFieldSpace(singleSpace("x"), singleSpace(FoxIntType)),
                        structFieldSpace(singleSpace("y"), singleSpace(FoxBoolType)),
                    ),
                ),
                1,
            ),
            lang,
        )
        assertTrue(lang.contains(FoxIntType, context))
        assertFalse(lang.contains(FoxStringType, context))
    }
    
    @Test
    fun structLastFieldAtProjectiveContainsUsesPreimageIntersection() {
        val lang = StructLastFieldAtProjectionSpace(
            union(
                singleSpace(
                    FoxStructType(
                        orderedMapOf(
                            "x" to FoxIntType,
                            "y" to FoxBoolType,
                        ),
                    ),
                ),
                singleSpace(
                    FoxStructType(
                        orderedMapOf(
                            "x" to FoxStringType,
                            "y" to FoxStringType,
                        ),
                    ),
                ),
            ),
            1,
        )
        
        assertTrue(lang.contains(FoxIntType, context))
        assertTrue(lang.contains(FoxStringType, context))
        assertFalse(lang.contains(FoxBoolType, context))
    }
    
    @Test
    fun structFirstPreimageContainsRequiresMatchingPrefix() {
        val lang = StructFirstPreimageSpace(
            2,
            FoxStructType(
                orderedMapOf(
                    "x" to FoxIntType,
                    "y" to FoxBoolType,
                ),
            ),
            exact = false,
        )
        
        assertTrue(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                context,
            ),
        )
        assertTrue(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                        "z" to FoxStringType,
                    ),
                ),
                context,
            ),
        )
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxStringType,
                    ),
                ),
                context,
            ),
        )
    }
    
    @Test
    fun structFirstPreimageLooseWithShortResultCollapsesToSingleton() {
        val prefix = FoxStructType(orderedMapOf("x" to FoxIntType))
        val lang = StructFirstPreimageSpace(2, prefix, exact = false)
        
        assertTrue(lang.contains(prefix, context))
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                context,
            ),
        )
        
        val traverser = lang.traverser(context)
        assertEquals(prefix, traverser.current())
        traverser.seekNext()
        assertNull(traverser.current())
    }
    
    @Test
    fun structFirstPreimageExactRequiresResultArityToEqualCount() {
        val lang = StructFirstPreimageSpace(
            2,
            FoxStructType(orderedMapOf("x" to FoxIntType)),
            exact = true,
        )
        
        assertFalse(
            lang.contains(
                FoxStructType(orderedMapOf("x" to FoxIntType)),
                context,
            ),
        )
        assertNull(lang.traverser(context).current())
    }
    
    @Test
    fun structFirstPreimageTraverserStartsFromFixedPrefixWhenResultArityEqualsCount() {
        val traverser = StructFirstPreimageSpace(
            1,
            FoxStructType(orderedMapOf("x" to FoxIntType)),
            exact = false,
        ).traverser(context)
        
        assertEquals(
            FoxStructType(orderedMapOf("x" to FoxIntType)),
            traverser.current(),
        )
        
        traverser.seekNext()
        assertEquals(
            FoxStructType(
                orderedMapOf(
                    "x" to FoxIntType,
                    "y" to FoxVoidType,
                ),
            ),
            traverser.current(),
        )
    }
    
    @Test
    fun compileStructFirstFieldsOfTypeToProjectiveLang() {
        val lang = compileType(
            FoxStructFirstFieldsOfType(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                1,
            ),
            context,
        )
        
        assertEquals(
            StructFirstProjectionSpace(
                StructPatternSpace(
                    listOf(
                        structFieldSpace(singleSpace("x"), singleSpace(FoxIntType)),
                        structFieldSpace(singleSpace("y"), singleSpace(FoxBoolType)),
                    ),
                ),
                1,
                exact = false,
            ),
            lang,
        )
        assertTrue(lang.contains(FoxStructType(orderedMapOf("x" to FoxIntType)), context))
        assertFalse(lang.contains(FoxStructType(orderedMapOf("x" to FoxBoolType)), context))
    }
    
    @Test
    fun compileStructExactFirstFieldsOfTypeToProjectiveLang() {
        val lang = compileType(
            FoxStructExactFirstFieldsOfType(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                1,
            ),
            context,
        )
        
        assertEquals(
            StructFirstProjectionSpace(
                StructPatternSpace(
                    listOf(
                        structFieldSpace(singleSpace("x"), singleSpace(FoxIntType)),
                        structFieldSpace(singleSpace("y"), singleSpace(FoxBoolType)),
                    ),
                ),
                1,
                exact = true,
            ),
            lang,
        )
        assertTrue(lang.contains(FoxStructType(orderedMapOf("x" to FoxIntType)), context))
        assertFalse(lang.contains(FoxStructType(emptyOrderedMap()), context))
    }
    
    @Test
    fun structFirstProjectiveContainsUsesPreimageIntersection() {
        val lang = StructFirstProjectionSpace(
            union(
                singleSpace(
                    FoxStructType(
                        orderedMapOf(
                            "x" to FoxIntType,
                            "y" to FoxBoolType,
                        ),
                    ),
                ),
                singleSpace(
                    FoxStructType(
                        orderedMapOf(
                            "x" to FoxStringType,
                        ),
                    ),
                ),
            ),
            1,
            exact = false,
        )
        
        assertTrue(lang.contains(FoxStructType(orderedMapOf("x" to FoxIntType)), context))
        assertTrue(lang.contains(FoxStructType(orderedMapOf("x" to FoxStringType)), context))
        assertFalse(lang.contains(FoxStructType(emptyOrderedMap()), context))
    }
    
    @Test
    fun structLastPreimageContainsRequiresMatchingSuffix() {
        val lang = StructLastPreimageSpace(
            2,
            FoxStructType(
                orderedMapOf(
                    "y" to FoxBoolType,
                    "z" to FoxStringType,
                ),
            ),
            exact = false,
        )
        
        assertTrue(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                        "z" to FoxStringType,
                    ),
                ),
                context,
            ),
        )
        assertTrue(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "y" to FoxBoolType,
                        "z" to FoxStringType,
                    ),
                ),
                context,
            ),
        )
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxStringType,
                        "z" to FoxStringType,
                    ),
                ),
                context,
            ),
        )
    }
    
    @Test
    fun structLastPreimageLooseWithShortResultCollapsesToSingleton() {
        val suffix = FoxStructType(orderedMapOf("z" to FoxStringType))
        val lang = StructLastPreimageSpace(2, suffix, exact = false)
        
        assertTrue(lang.contains(suffix, context))
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "y" to FoxBoolType,
                        "z" to FoxStringType,
                    ),
                ),
                context,
            ),
        )
        
        val traverser = lang.traverser(context)
        assertEquals(suffix, traverser.current())
        traverser.seekNext()
        assertNull(traverser.current())
    }
    
    @Test
    fun structLastPreimageExactRequiresResultArityToEqualCount() {
        val lang = StructLastPreimageSpace(
            2,
            FoxStructType(orderedMapOf("z" to FoxStringType)),
            exact = true,
        )
        
        assertFalse(
            lang.contains(
                FoxStructType(orderedMapOf("z" to FoxStringType)),
                context,
            ),
        )
        assertNull(lang.traverser(context).current())
    }
    
    @Test
    fun structLastPreimageTraverserStartsFromFixedSuffixWhenResultArityEqualsCount() {
        val traverser = StructLastPreimageSpace(
            1,
            FoxStructType(orderedMapOf("z" to FoxStringType)),
            exact = false,
        ).traverser(context)
        
        assertEquals(
            FoxStructType(orderedMapOf("z" to FoxStringType)),
            traverser.current(),
        )
        
        traverser.seekNext()
        assertEquals(
            FoxStructType(
                orderedMapOf(
                    "x" to FoxVoidType,
                    "z" to FoxStringType,
                ),
            ),
            traverser.current(),
        )
    }
    
    @Test
    fun compileStructLastFieldsOfTypeToProjectiveLang() {
        val lang = compileType(
            FoxStructLastFieldsOfType(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "z" to FoxStringType,
                    ),
                ),
                1,
            ),
            context,
        )
        
        assertEquals(
            StructLastProjectionSpace(
                StructPatternSpace(
                    listOf(
                        structFieldSpace(singleSpace("x"), singleSpace(FoxIntType)),
                        structFieldSpace(singleSpace("z"), singleSpace(FoxStringType)),
                    ),
                ),
                1,
                exact = false,
            ),
            lang,
        )
        assertTrue(lang.contains(FoxStructType(orderedMapOf("z" to FoxStringType)), context))
        assertFalse(lang.contains(FoxStructType(orderedMapOf("z" to FoxIntType)), context))
    }
    
    @Test
    fun compileStructExactLastFieldsOfTypeToProjectiveLang() {
        val lang = compileType(
            FoxStructExactLastFieldsOfType(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "z" to FoxStringType,
                    ),
                ),
                1,
            ),
            context,
        )
        
        assertEquals(
            StructLastProjectionSpace(
                StructPatternSpace(
                    listOf(
                        structFieldSpace(singleSpace("x"), singleSpace(FoxIntType)),
                        structFieldSpace(singleSpace("z"), singleSpace(FoxStringType)),
                    ),
                ),
                1,
                exact = true,
            ),
            lang,
        )
        assertTrue(lang.contains(FoxStructType(orderedMapOf("z" to FoxStringType)), context))
        assertFalse(lang.contains(FoxStructType(emptyOrderedMap()), context))
    }
    
    @Test
    fun structLastProjectiveContainsUsesPreimageIntersection() {
        val lang = StructLastProjectionSpace(
            union(
                singleSpace(
                    FoxStructType(
                        orderedMapOf(
                            "x" to FoxIntType,
                            "z" to FoxStringType,
                        ),
                    ),
                ),
                singleSpace(
                    FoxStructType(
                        orderedMapOf(
                            "z" to FoxBoolType,
                        ),
                    ),
                ),
            ),
            1,
            exact = false,
        )
        
        assertTrue(lang.contains(FoxStructType(orderedMapOf("z" to FoxStringType)), context))
        assertTrue(lang.contains(FoxStructType(orderedMapOf("z" to FoxBoolType)), context))
        assertFalse(lang.contains(FoxStructType(emptyOrderedMap()), context))
    }
    
    @Test
    fun structDropFirstPreimageContainsRequiresMatchingSuffixAfterDropping() {
        val lang = StructDropFirstPreimageSpace(
            1,
            FoxStructType(orderedMapOf("y" to FoxBoolType)),
            exact = false,
        )
        
        assertTrue(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                context,
            ),
        )
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxStringType,
                    ),
                ),
                context,
            ),
        )
    }
    
    @Test
    fun structDropFirstPreimageLooseWithEmptyResultAllowsUpToCountFields() {
        val lang = StructDropFirstPreimageSpace(2, FoxStructType(emptyOrderedMap()), exact = false)
        
        assertTrue(lang.contains(FoxStructType(emptyOrderedMap()), context))
        assertTrue(lang.contains(FoxStructType(orderedMapOf("x" to FoxIntType)), context))
        assertTrue(lang.contains(FoxStructType(orderedMapOf("x" to FoxIntType, "y" to FoxBoolType)), context))
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                        "z" to FoxStringType,
                    ),
                ),
                context,
            ),
        )
    }
    
    @Test
    fun structDropFirstExactPreimageRequiresExactSourceLength() {
        val lang = StructDropFirstPreimageSpace(1, FoxStructType(emptyOrderedMap()), exact = true)
        
        assertTrue(lang.contains(FoxStructType(orderedMapOf("x" to FoxIntType)), context))
        assertFalse(lang.contains(FoxStructType(emptyOrderedMap()), context))
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                context,
            ),
        )
    }
    
    @Test
    fun compileStructDropFirstFieldsOfTypeToProjectiveLang() {
        val lang = compileType(
            FoxStructDropFirstFieldsOfType(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                1,
            ),
            context,
        )
        
        assertEquals(
            StructDropFirstProjectionSpace(
                StructPatternSpace(
                    listOf(
                        structFieldSpace(singleSpace("x"), singleSpace(FoxIntType)),
                        structFieldSpace(singleSpace("y"), singleSpace(FoxBoolType)),
                    ),
                ),
                1,
                exact = false,
            ),
            lang,
        )
        assertTrue(lang.contains(FoxStructType(orderedMapOf("y" to FoxBoolType)), context))
        assertFalse(lang.contains(FoxStructType(orderedMapOf("y" to FoxIntType)), context))
    }
    
    @Test
    fun compileStructExactDropFirstFieldsOfTypeToProjectiveLang() {
        val lang = compileType(
            FoxStructExactDropFirstFieldsOfType(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                1,
            ),
            context,
        )
        
        assertEquals(
            StructDropFirstProjectionSpace(
                StructPatternSpace(
                    listOf(
                        structFieldSpace(singleSpace("x"), singleSpace(FoxIntType)),
                        structFieldSpace(singleSpace("y"), singleSpace(FoxBoolType)),
                    ),
                ),
                1,
                exact = true,
            ),
            lang,
        )
        assertTrue(lang.contains(FoxStructType(orderedMapOf("y" to FoxBoolType)), context))
        assertFalse(lang.contains(FoxStructType(emptyOrderedMap()), context))
    }
    
    @Test
    fun structDropLastPreimageContainsRequiresMatchingPrefixAfterDropping() {
        val lang = StructDropLastPreimageSpace(
            1,
            FoxStructType(orderedMapOf("x" to FoxIntType)),
            exact = false,
        )
        
        assertTrue(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                context,
            ),
        )
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxBoolType,
                        "y" to FoxIntType,
                    ),
                ),
                context,
            ),
        )
    }
    
    @Test
    fun structDropLastPreimageLooseWithEmptyResultAllowsUpToCountFields() {
        val lang = StructDropLastPreimageSpace(2, FoxStructType(emptyOrderedMap()), exact = false)
        
        assertTrue(lang.contains(FoxStructType(emptyOrderedMap()), context))
        assertTrue(lang.contains(FoxStructType(orderedMapOf("x" to FoxIntType)), context))
        assertTrue(lang.contains(FoxStructType(orderedMapOf("x" to FoxIntType, "y" to FoxBoolType)), context))
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                        "z" to FoxStringType,
                    ),
                ),
                context,
            ),
        )
    }
    
    @Test
    fun structDropLastExactPreimageRequiresExactSourceLength() {
        val lang = StructDropLastPreimageSpace(1, FoxStructType(emptyOrderedMap()), exact = true)
        
        assertTrue(lang.contains(FoxStructType(orderedMapOf("x" to FoxIntType)), context))
        assertFalse(lang.contains(FoxStructType(emptyOrderedMap()), context))
        assertFalse(
            lang.contains(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                context,
            ),
        )
    }
    
    @Test
    fun compileStructDropLastFieldsOfTypeToProjectiveLang() {
        val lang = compileType(
            FoxStructDropLastFieldsOfType(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                1,
            ),
            context,
        )
        
        assertEquals(
            StructDropLastProjectionSpace(
                StructPatternSpace(
                    listOf(
                        structFieldSpace(singleSpace("x"), singleSpace(FoxIntType)),
                        structFieldSpace(singleSpace("y"), singleSpace(FoxBoolType)),
                    ),
                ),
                1,
                exact = false,
            ),
            lang,
        )
        assertTrue(lang.contains(FoxStructType(orderedMapOf("x" to FoxIntType)), context))
        assertFalse(lang.contains(FoxStructType(orderedMapOf("x" to FoxBoolType)), context))
    }
    
    @Test
    fun compileStructExactDropLastFieldsOfTypeToProjectiveLang() {
        val lang = compileType(
            FoxStructExactDropLastFieldsOfType(
                FoxStructType(
                    orderedMapOf(
                        "x" to FoxIntType,
                        "y" to FoxBoolType,
                    ),
                ),
                1,
            ),
            context,
        )
        
        assertEquals(
            StructDropLastProjectionSpace(
                StructPatternSpace(
                    listOf(
                        structFieldSpace(singleSpace("x"), singleSpace(FoxIntType)),
                        structFieldSpace(singleSpace("y"), singleSpace(FoxBoolType)),
                    ),
                ),
                1,
                exact = true,
            ),
            lang,
        )
        assertTrue(lang.contains(FoxStructType(orderedMapOf("x" to FoxIntType)), context))
        assertFalse(lang.contains(FoxStructType(emptyOrderedMap()), context))
    }
}
