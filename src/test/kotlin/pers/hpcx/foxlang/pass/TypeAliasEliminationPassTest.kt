package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.*
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
        val method = FoxMethodDefinition(
            generics = orderedMapOf(
                "T" to FoxUnresolvedType("Box", listOf(FoxUnresolvedType("T", null))),
            ),
            thisType = FoxUnresolvedType("RefInt", null),
            name = "wrap",
            parameters = orderedMapOf(
                "value" to FoxUnresolvedType("Box", listOf(FoxUnresolvedType("T", null))),
                "pair" to FoxUnresolvedType("Pair", listOf(FoxIntType, FoxUnresolvedType("T", null))),
            ),
            returnType = FoxUnresolvedType("Box", listOf(FoxIntType)),
            body = FoxBlock(
                null,
                listOf(
                    FoxTypeBinding("local", FoxUnresolvedType("Box", listOf(FoxUnresolvedType("T", null)))),
                    FoxConstruct(
                        FoxUnresolvedType("Pair", listOf(FoxIntType, FoxUnresolvedType("T", null))),
                        emptyList(),
                    ),
                    FoxCall(
                        target = FoxEntityStatement(FoxUnit),
                        name = "consume",
                        generics = listOf(null to FoxUnresolvedType("Box", listOf(FoxIntType))),
                        parameters = listOf(null to FoxConstruct(FoxUnresolvedType("Box", listOf(FoxIntType)), emptyList())),
                    ),
                    FoxReturn(FoxConstruct(FoxUnresolvedType("Box", listOf(FoxIntType)), emptyList())),
                ),
            ),
        )
        val file = FoxFile(
            listOf(
                FoxTypeAlias("Box", orderedSetOf("T"), FoxArrayType(FoxUnresolvedType("T", null))),
                FoxTypeAlias(
                    "Pair",
                    orderedSetOf("A", "B"),
                    FoxTupleType(listOf(FoxUnresolvedType("A", null) to 1, FoxUnresolvedType("B", null) to 1)),
                ),
                FoxTypeAlias("RefInt", emptyOrderedSet(), FoxRefType(FoxIntType)),
                method,
            ),
        )
        
        val result = assertIs<TypeAliasEliminationSuccess>(runTypeAliasElimination(file))
        val expected = FoxMethodDefinition(
            generics = orderedMapOf(
                "T" to FoxArrayType(FoxUnresolvedType("T", null)),
            ),
            thisType = FoxRefType(FoxIntType),
            name = "wrap",
            parameters = orderedMapOf(
                "value" to FoxArrayType(FoxUnresolvedType("T", null)),
                "pair" to FoxTupleType(listOf(FoxIntType to 1, FoxUnresolvedType("T", null) to 1)),
            ),
            returnType = FoxArrayType(FoxIntType),
            body = FoxBlock(
                null,
                listOf(
                    FoxTypeBinding("local", FoxArrayType(FoxUnresolvedType("T", null))),
                    FoxConstruct(
                        FoxTupleType(listOf(FoxIntType to 1, FoxUnresolvedType("T", null) to 1)),
                        emptyList(),
                    ),
                    FoxCall(
                        target = FoxEntityStatement(FoxUnit),
                        name = "consume",
                        generics = listOf(null to FoxArrayType(FoxIntType)),
                        parameters = listOf(null to FoxConstruct(FoxArrayType(FoxIntType), emptyList())),
                    ),
                    FoxReturn(FoxConstruct(FoxArrayType(FoxIntType), emptyList())),
                ),
            ),
        )
        
        assertEquals(FoxFile(listOf(expected)), result.newFile)
        assertTrue(result.newFile.elements.none { it is FoxTypeAlias })
    }
    
    @Test
    fun removesTypeAliasesAndKeepsMethodOrder() {
        val first = FoxMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "first",
            parameters = orderedMapOf("value" to FoxIntType),
            returnType = FoxIntType,
            body = FoxReturn(FoxSymbol("value")),
        )
        val second = FoxMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "second",
            parameters = orderedMapOf("value" to FoxUnresolvedType("Box", listOf(FoxIntType))),
            returnType = FoxUnresolvedType("Box", listOf(FoxIntType)),
            body = FoxReturn(FoxSymbol("value")),
        )
        val file = FoxFile(
            listOf(
                FoxTypeAlias("Ignored", emptyOrderedSet(), FoxIntType),
                first,
                FoxTypeAlias("Box", orderedSetOf("T"), FoxArrayType(FoxUnresolvedType("T", null))),
                second,
            ),
        )
        
        val result = assertIs<TypeAliasEliminationSuccess>(runTypeAliasElimination(file))
        
        assertEquals(listOf("first", "second"), result.newFile.elements.filterIsInstance<FoxMethodDefinition>().map { it.name })
        assertTrue(result.newFile.elements.none { it is FoxTypeAlias })
        assertEquals(2, result.newFile.elements.size)
    }
    
    @Test
    fun preservesMethodGenericWhenItShadowsAliasName() {
        val method = FoxMethodDefinition(
            generics = orderedMapOf(
                "T" to FoxUnresolvedType("T", null),
            ),
            thisType = FoxUnitType,
            name = "shadow",
            parameters = orderedMapOf("value" to FoxUnresolvedType("Box", listOf(FoxUnresolvedType("T", null)))),
            returnType = FoxUnresolvedType("T", null),
            body = FoxReturn(FoxConstruct(FoxUnresolvedType("Box", listOf(FoxUnresolvedType("T", null))), emptyList())),
        )
        val file = FoxFile(
            listOf(
                FoxTypeAlias("T", emptyOrderedSet(), FoxIntType),
                FoxTypeAlias("Box", orderedSetOf("T"), FoxArrayType(FoxUnresolvedType("T", null))),
                method,
            ),
        )
        
        val result = assertIs<TypeAliasEliminationSuccess>(runTypeAliasElimination(file))
        val newMethod = assertIs<FoxMethodDefinition>(result.newFile.elements.single())
        
        assertEquals(FoxUnresolvedType("T", null), newMethod.returnType)
        assertEquals(FoxArrayType(FoxUnresolvedType("T", null)), newMethod.parameters.getValue("value"))
        assertEquals(
            FoxReturn(FoxConstruct(FoxArrayType(FoxUnresolvedType("T", null)), emptyList())),
            newMethod.body,
        )
    }
    
    @Test
    fun detectsMissingAliasReference() {
        val method = FoxMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "missing",
            parameters = orderedMapOf("value" to FoxUnresolvedType("Missing", null)),
            returnType = FoxUnitType,
            body = FoxReturn(FoxSymbol("value")),
        )
        
        val result = assertIs<TypeAliasEliminationFailure>(runTypeAliasElimination(FoxFile(listOf(method))))
        val error = assertIs<TypeAliasEliminationNotFound>(result.errors.single())
        
        assertEquals("missing", error.referredBy.name)
        assertEquals("Missing", error.typeName)
    }
    
    @Test
    fun detectsUnexpectedGenericsOnNonGenericAlias() {
        val method = FoxMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "unexpectedGenerics",
            parameters = orderedMapOf("value" to FoxUnresolvedType("Base", listOf(FoxIntType))),
            returnType = FoxUnitType,
            body = FoxReturn(FoxSymbol("value")),
        )
        val file = FoxFile(listOf(FoxTypeAlias("Base", emptyOrderedSet(), FoxIntType), method))
        
        val result = assertIs<TypeAliasEliminationFailure>(runTypeAliasElimination(file))
        val error = assertIs<TypeAliasEliminationGenericCountMismatch>(result.errors.single())
        
        assertEquals(FoxUnresolvedType("Base", listOf(FoxIntType)), error.type)
    }
    
    @Test
    fun detectsMissingGenericsOnGenericAlias() {
        val method = FoxMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "missingGenerics",
            parameters = orderedMapOf("value" to FoxUnresolvedType("Box", null)),
            returnType = FoxUnitType,
            body = FoxReturn(FoxSymbol("value")),
        )
        val file = FoxFile(listOf(FoxTypeAlias("Box", orderedSetOf("T"), FoxArrayType(FoxUnresolvedType("T", null))), method))
        
        val result = assertIs<TypeAliasEliminationFailure>(runTypeAliasElimination(file))
        val error = assertIs<TypeAliasEliminationGenericCountMismatch>(result.errors.single())
        
        assertEquals(FoxUnresolvedType("Box", null), error.type)
    }
    
    @Test
    fun detectsGenericCountMismatch() {
        val method = FoxMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "genericCountMismatch",
            parameters = orderedMapOf("value" to FoxUnresolvedType("Pair", listOf(FoxIntType))),
            returnType = FoxUnitType,
            body = FoxReturn(FoxSymbol("value")),
        )
        val file = FoxFile(
            listOf(
                FoxTypeAlias(
                    "Pair",
                    orderedSetOf("A", "B"),
                    FoxTupleType(listOf(FoxUnresolvedType("A", null) to 1, FoxUnresolvedType("B", null) to 1)),
                ),
                method,
            ),
        )
        
        val result = assertIs<TypeAliasEliminationFailure>(runTypeAliasElimination(file))
        val error = assertIs<TypeAliasEliminationGenericCountMismatch>(result.errors.single())
        
        assertEquals(FoxUnresolvedType("Pair", listOf(FoxIntType)), error.type)
    }
}
