package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.orderedMapOf
import pers.hpcx.foxlang.utils.orderedSetOf
import kotlin.test.*

class TypeAliasFlattenPassTest {
    
    @Test
    fun reducesAliasDependencyChain() {
        val file = FoxFile(
            listOf(
                FoxTypeAlias("Base", null, FoxIntType),
                FoxTypeAlias("Wrapped", null, FoxArrayType(FoxUnresolvedType("Base", null))),
                FoxTypeAlias("Final", null, FoxRefType(FoxUnresolvedType("Wrapped", null))),
            ),
        )
        
        val result = assertIs<TypeAliasFlattenSuccess>(runTypeAliasFlatten(file))
        val aliases = result.newFile.elements.filterIsInstance<FoxTypeAlias>().associateBy { it.name }
        
        assertEquals(FoxIntType, aliases.getValue("Base").alias)
        assertEquals(FoxArrayType(FoxIntType), aliases.getValue("Wrapped").alias)
        assertEquals(FoxRefType(FoxArrayType(FoxIntType)), aliases.getValue("Final").alias)
        assertTrue(aliases.values.all { noAliasDependency(it.alias, aliases.keys) })
        assertTrue(noAliasDependency(aliases.getValue("Final").alias, aliases.keys))
    }
    
    @Test
    fun reducesGenericAliasDependencyChain() {
        val file = FoxFile(
            listOf(
                FoxTypeAlias("Box", orderedSetOf("T"), FoxArrayType(FoxUnresolvedType("T", null))),
                FoxTypeAlias(
                    "PairBox",
                    orderedSetOf("A", "B"),
                    FoxTupleType(
                        listOf(
                            FoxUnresolvedType("Box", listOf(FoxUnresolvedType("A", null))) to 1,
                            FoxUnresolvedType("Box", listOf(FoxUnresolvedType("B", null))) to 1,
                        ),
                    ),
                ),
                FoxTypeAlias(
                    "Concrete",
                    null,
                    FoxUnresolvedType(
                        "PairBox",
                        listOf(FoxIntType, FoxFloatType),
                    ),
                ),
            ),
        )
        
        val result = assertIs<TypeAliasFlattenSuccess>(runTypeAliasFlatten(file))
        val aliases = result.newFile.elements.filterIsInstance<FoxTypeAlias>().associateBy { it.name }
        
        assertEquals(FoxArrayType(FoxUnresolvedType("T", null)), aliases.getValue("Box").alias)
        assertTrue(noAliasDependency(aliases.getValue("Box").alias, aliases.keys))
        assertEquals(
            FoxTupleType(
                listOf(
                    FoxArrayType(FoxUnresolvedType("A", null)) to 1,
                    FoxArrayType(FoxUnresolvedType("B", null)) to 1,
                ),
            ),
            aliases.getValue("PairBox").alias,
        )
        assertTrue(noAliasDependency(aliases.getValue("PairBox").alias, aliases.keys))
        assertEquals(
            FoxTupleType(
                listOf(
                    FoxArrayType(FoxIntType) to 1,
                    FoxArrayType(FoxFloatType) to 1,
                ),
            ),
            aliases.getValue("Concrete").alias,
        )
        assertTrue(aliases.values.all { noAliasDependency(it.alias, aliases.keys) })
        assertTrue(noAliasDependency(aliases.getValue("Concrete").alias, aliases.keys))
    }
    
    @Test
    fun keepsMethodDefinitionsUntouched() {
        val method = FoxMethodDefinition(
            generics = null,
            thisType = null,
            name = "id",
            parameters = orderedMapOf("value" to FoxUnresolvedType("Wrapped", null)),
            returnType = FoxUnresolvedType("Wrapped", null),
            body = FoxReturn(FoxSymbol("value")),
        )
        val file = FoxFile(
            listOf(
                FoxTypeAlias("Base", null, FoxIntType),
                FoxTypeAlias("Wrapped", null, FoxUnresolvedType("Base", null)),
                method,
            ),
        )
        
        val result = assertIs<TypeAliasFlattenSuccess>(runTypeAliasFlatten(file))
        assertEquals(method, result.newFile.elements.last())
    }
    
    @Test
    fun keepsOriginalFileElementOrder() {
        val method = FoxMethodDefinition(
            generics = null,
            thisType = null,
            name = "use",
            parameters = orderedMapOf("value" to FoxIntType),
            returnType = FoxIntType,
            body = FoxReturn(FoxSymbol("value")),
        )
        val file = FoxFile(
            listOf(
                method,
                FoxTypeAlias("Base", null, FoxIntType),
                FoxTypeAlias("Wrapped", null, FoxArrayType(FoxUnresolvedType("Base", null))),
            ),
        )
        
        val result = assertIs<TypeAliasFlattenSuccess>(runTypeAliasFlatten(file))
        assertSame(method, result.newFile.elements[0])
        assertEquals(
            listOf("use", "Base", "Wrapped"),
            result.newFile.elements.map {
                when (it) {
                    is FoxMethodDefinition -> it.name
                    is FoxTypeAlias -> it.name
                }
            },
        )
    }
    
    @Test
    fun rejectsWildcardInAlias() {
        val file = FoxFile(
            listOf(
                FoxTypeAlias("WildcardAlias", null, FoxArrayType(FoxAnyType)),
            ),
        )
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        assertIs<TypeAliasUnexpectedWildcard>(result.errors.single())
    }
    
    @Test
    fun detectsAliasLoop() {
        val a = FoxTypeAlias("A", null, FoxUnresolvedType("B", null))
        val b = FoxTypeAlias("B", null, FoxUnresolvedType("C", null))
        val c = FoxTypeAlias("C", null, FoxUnresolvedType("B", null))
        val file = FoxFile(listOf(a, b, c))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val loop = assertIs<TypeAliasLoopDetected>(result.errors.single())
        
        assertEquals(listOf("B", "C"), loop.typeAliases.map { it.name })
    }
    
    @Test
    fun detectsDuplicatedAlias() {
        val file = FoxFile(
            listOf(
                FoxTypeAlias("A", null, FoxIntType),
                FoxTypeAlias("A", null, FoxFloatType),
            ),
        )
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        assertIs<TypeAliasDuplicated>(result.errors.single())
    }
    
    @Test
    fun detectsMissingAliasReference() {
        val alias = FoxTypeAlias("A", null, FoxUnresolvedType("Missing", null))
        val file = FoxFile(listOf(alias))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasNotFound>(result.errors.single())
        
        assertEquals("A", error.referredBy.name)
        assertEquals("Missing", error.typeName)
    }
    
    @Test
    fun detectsUnexpectedGenericsOnNonGenericAlias() {
        val alias = FoxTypeAlias("A", null, FoxIntType)
        val referrer = FoxTypeAlias("B", null, FoxUnresolvedType("A", listOf(FoxIntType)))
        val file = FoxFile(listOf(alias, referrer))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasUnexpectedGenerics>(result.errors.single())
        
        assertEquals("B", error.referredBy.name)
    }
    
    @Test
    fun detectsMissingGenericsOnGenericAlias() {
        val alias = FoxTypeAlias("Box", orderedSetOf("T"), FoxArrayType(FoxUnresolvedType("T", null)))
        val referrer = FoxTypeAlias("Use", null, FoxUnresolvedType("Box", null))
        val file = FoxFile(listOf(alias, referrer))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasMissingGenerics>(result.errors.single())
        
        assertEquals("Use", error.referredBy.name)
    }
    
    @Test
    fun detectsGenericCountMismatch() {
        val alias = FoxTypeAlias(
            "Pair",
            orderedSetOf("A", "B"),
            FoxTupleType(listOf(FoxUnresolvedType("A", null) to 1, FoxUnresolvedType("B", null) to 1)),
        )
        val referrer = FoxTypeAlias("Use", null, FoxUnresolvedType("Pair", listOf(FoxIntType)))
        val file = FoxFile(listOf(alias, referrer))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasGenericCountMismatch>(result.errors.single())
        
        assertEquals("Use", error.referredBy.name)
    }
    
    private fun noAliasDependency(type: FoxType, aliasNames: Set<String>): Boolean = when (type) {
        is FoxPrimitiveType -> true
        is FoxBuiltInType -> type.nestedTypes().all { noAliasDependency(it, aliasNames) }
        is FoxTransformType -> type.nestedTypes().all { noAliasDependency(it, aliasNames) }
        is FoxUnresolvedType -> type.name !in aliasNames && type.parameters.orEmpty().all { noAliasDependency(it, aliasNames) }
        is FoxWildcardType -> false
        is FoxPlaceholderType -> false
    }
}
