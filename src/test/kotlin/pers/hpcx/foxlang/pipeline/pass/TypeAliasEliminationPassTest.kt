package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.runtime.FoxUnit
import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.emptyOrderedSet
import pers.hpcx.foxlang.utils.orderedMapOf
import pers.hpcx.foxlang.utils.orderedSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TypeAliasEliminationPassTest {
    
    @Test
    fun eliminatesAliasesFromMethodTypes() {
        val method = SurfaceMethodDefinition(
            generics = orderedMapOf(
                "T" to SurfaceUnresolvedType("Box", listOf(SurfaceUnresolvedType("T", null))),
            ),
            thisType = SurfaceUnresolvedType("RefInt", null),
            name = "wrap",
            parameters = orderedMapOf(
                "value" to SurfaceUnresolvedType("Box", listOf(SurfaceUnresolvedType("T", null))),
                "pair" to SurfaceUnresolvedType("Pair", listOf(FoxIntType, SurfaceUnresolvedType("T", null))),
            ),
            returnType = SurfaceUnresolvedType("Box", listOf(FoxIntType)),
            body = SurfaceBlock(
                null,
                listOf(
                    SurfaceTypeBinding("local", SurfaceUnresolvedType("Box", listOf(SurfaceUnresolvedType("T", null)))),
                    SurfaceConstruct(
                        SurfaceUnresolvedType("Pair", listOf(FoxIntType, SurfaceUnresolvedType("T", null))),
                        emptyList(),
                    ),
                    SurfaceCall(
                        target = SurfaceEntityStatement(FoxUnit),
                        name = "consume",
                        generics = listOf(null to SurfaceUnresolvedType("Box", listOf(FoxIntType))),
                        parameters = listOf(null to SurfaceConstruct(SurfaceUnresolvedType("Box", listOf(FoxIntType)), emptyList())),
                    ),
                    SurfaceReturn(SurfaceConstruct(SurfaceUnresolvedType("Box", listOf(FoxIntType)), emptyList())),
                ),
            ),
        )
        val file = SurfaceFile(
            listOf(
                SurfaceTypeAlias("Box", orderedSetOf("T"), SurfaceArrayType(SurfaceUnresolvedType("T", null))),
                SurfaceTypeAlias(
                    "Pair",
                    orderedSetOf("A", "B"),
                    SurfaceTupleType(listOf(SurfaceUnresolvedType("A", null), SurfaceUnresolvedType("B", null))),
                ),
                SurfaceTypeAlias("RefInt", emptyOrderedSet(), SurfaceRefType(FoxIntType)),
                method,
            ),
        )
        
        val result = assertIs<TypeAliasEliminationSuccess>(runTypeAliasElimination(file))
        val expected = SurfaceMethodDefinition(
            generics = orderedMapOf(
                "T" to SurfaceArrayType(SurfaceUnresolvedType("T", null)),
            ),
            thisType = SurfaceRefType(FoxIntType),
            name = "wrap",
            parameters = orderedMapOf(
                "value" to SurfaceArrayType(SurfaceUnresolvedType("T", null)),
                "pair" to SurfaceTupleType(listOf(FoxIntType, SurfaceUnresolvedType("T", null))),
            ),
            returnType = SurfaceArrayType(FoxIntType),
            body = SurfaceBlock(
                null,
                listOf(
                    SurfaceTypeBinding("local", SurfaceArrayType(SurfaceUnresolvedType("T", null))),
                    SurfaceConstruct(
                        SurfaceTupleType(listOf(FoxIntType, SurfaceUnresolvedType("T", null))),
                        emptyList(),
                    ),
                    SurfaceCall(
                        target = SurfaceEntityStatement(FoxUnit),
                        name = "consume",
                        generics = listOf(null to SurfaceArrayType(FoxIntType)),
                        parameters = listOf(null to SurfaceConstruct(SurfaceArrayType(FoxIntType), emptyList())),
                    ),
                    SurfaceReturn(SurfaceConstruct(SurfaceArrayType(FoxIntType), emptyList())),
                ),
            ),
        )
        
        assertEquals(SurfaceFile(listOf(expected)), result.newFile)
        assertTrue(result.newFile.elements.none { it is SurfaceTypeAlias })
    }
    
    @Test
    fun removesTypeAliasesAndKeepsMethodOrder() {
        val first = SurfaceMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "first",
            parameters = orderedMapOf("value" to FoxIntType),
            returnType = FoxIntType,
            body = SurfaceReturn(SurfaceUnresolvedSymbol("value")),
        )
        val second = SurfaceMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "second",
            parameters = orderedMapOf("value" to SurfaceUnresolvedType("Box", listOf(FoxIntType))),
            returnType = SurfaceUnresolvedType("Box", listOf(FoxIntType)),
            body = SurfaceReturn(SurfaceUnresolvedSymbol("value")),
        )
        val file = SurfaceFile(
            listOf(
                SurfaceTypeAlias("Ignored", emptyOrderedSet(), FoxIntType),
                first,
                SurfaceTypeAlias("Box", orderedSetOf("T"), SurfaceArrayType(SurfaceUnresolvedType("T", null))),
                second,
            ),
        )
        
        val result = assertIs<TypeAliasEliminationSuccess>(runTypeAliasElimination(file))
        
        assertEquals(listOf("first", "second"), result.newFile.elements.filterIsInstance<SurfaceMethodDefinition>().map { it.name })
        assertTrue(result.newFile.elements.none { it is SurfaceTypeAlias })
        assertEquals(2, result.newFile.elements.size)
    }
    
    @Test
    fun preservesMethodGenericWhenItShadowsAliasName() {
        val method = SurfaceMethodDefinition(
            generics = orderedMapOf(
                "T" to SurfaceUnresolvedType("T", null),
            ),
            thisType = FoxUnitType,
            name = "shadow",
            parameters = orderedMapOf("value" to SurfaceUnresolvedType("Box", listOf(SurfaceUnresolvedType("T", null)))),
            returnType = SurfaceUnresolvedType("T", null),
            body = SurfaceReturn(SurfaceConstruct(SurfaceUnresolvedType("Box", listOf(SurfaceUnresolvedType("T", null))), emptyList())),
        )
        val file = SurfaceFile(
            listOf(
                SurfaceTypeAlias("T", emptyOrderedSet(), FoxIntType),
                SurfaceTypeAlias("Box", orderedSetOf("T"), SurfaceArrayType(SurfaceUnresolvedType("T", null))),
                method,
            ),
        )
        
        val result = assertIs<TypeAliasEliminationSuccess>(runTypeAliasElimination(file))
        val newMethod = assertIs<SurfaceMethodDefinition>(result.newFile.elements.single())
        
        assertEquals(SurfaceUnresolvedType("T", null), newMethod.returnType)
        assertEquals(SurfaceArrayType(SurfaceUnresolvedType("T", null)), newMethod.parameters.getValue("value"))
        assertEquals(
            SurfaceReturn(SurfaceConstruct(SurfaceArrayType(SurfaceUnresolvedType("T", null)), emptyList())),
            newMethod.body,
        )
    }
    
    @Test
    fun detectsMissingAliasReference() {
        val method = SurfaceMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "missing",
            parameters = orderedMapOf("value" to SurfaceUnresolvedType("Missing", null)),
            returnType = FoxUnitType,
            body = SurfaceReturn(SurfaceUnresolvedSymbol("value")),
        )
        
        val result = assertIs<TypeAliasEliminationFailure>(runTypeAliasElimination(SurfaceFile(listOf(method))))
        val error = assertIs<TypeAliasEliminationNotFound>(result.errors.single())
        
        assertEquals("missing", error.referredBy.name)
        assertEquals("Missing", error.typeName)
    }
    
    @Test
    fun detectsUnexpectedGenericsOnNonGenericAlias() {
        val method = SurfaceMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "unexpectedGenerics",
            parameters = orderedMapOf("value" to SurfaceUnresolvedType("Base", listOf(FoxIntType))),
            returnType = FoxUnitType,
            body = SurfaceReturn(SurfaceUnresolvedSymbol("value")),
        )
        val file = SurfaceFile(listOf(SurfaceTypeAlias("Base", emptyOrderedSet(), FoxIntType), method))
        
        val result = assertIs<TypeAliasEliminationFailure>(runTypeAliasElimination(file))
        val error = assertIs<TypeAliasEliminationGenericCountMismatch>(result.errors.single())
        
        assertEquals(SurfaceUnresolvedType("Base", listOf(FoxIntType)), error.type)
    }
    
    @Test
    fun detectsMissingGenericsOnGenericAlias() {
        val method = SurfaceMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "missingGenerics",
            parameters = orderedMapOf("value" to SurfaceUnresolvedType("Box", null)),
            returnType = FoxUnitType,
            body = SurfaceReturn(SurfaceUnresolvedSymbol("value")),
        )
        val file = SurfaceFile(listOf(SurfaceTypeAlias("Box", orderedSetOf("T"), SurfaceArrayType(SurfaceUnresolvedType("T", null))), method))
        
        val result = assertIs<TypeAliasEliminationFailure>(runTypeAliasElimination(file))
        val error = assertIs<TypeAliasEliminationGenericCountMismatch>(result.errors.single())
        
        assertEquals(SurfaceUnresolvedType("Box", null), error.type)
    }
    
    @Test
    fun detectsGenericCountMismatch() {
        val method = SurfaceMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "genericCountMismatch",
            parameters = orderedMapOf("value" to SurfaceUnresolvedType("Pair", listOf(FoxIntType))),
            returnType = FoxUnitType,
            body = SurfaceReturn(SurfaceUnresolvedSymbol("value")),
        )
        val file = SurfaceFile(
            listOf(
                SurfaceTypeAlias(
                    "Pair",
                    orderedSetOf("A", "B"),
                    SurfaceTupleType(listOf(SurfaceUnresolvedType("A", null), SurfaceUnresolvedType("B", null))),
                ),
                method,
            ),
        )
        
        val result = assertIs<TypeAliasEliminationFailure>(runTypeAliasElimination(file))
        val error = assertIs<TypeAliasEliminationGenericCountMismatch>(result.errors.single())
        
        assertEquals(SurfaceUnresolvedType("Pair", listOf(FoxIntType)), error.type)
    }
}

private val FoxUnitType = SurfacePrimitiveType(PrimitiveTypeEnum.Unit)
private val FoxIntType = SurfacePrimitiveType(PrimitiveTypeEnum.Int)
