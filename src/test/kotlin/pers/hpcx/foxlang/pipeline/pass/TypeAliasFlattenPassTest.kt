package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.visitTypes
import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.emptyOrderedSet
import pers.hpcx.foxlang.utils.orderedMapOf
import pers.hpcx.foxlang.utils.orderedSetOf
import kotlin.test.*

class TypeAliasFlattenPassTest {
    
    @Test
    fun reducesAliasDependencyChain() {
        val file = FoxFile(
            listOf(
                FoxTypeAlias("Base", emptyOrderedSet(), FoxIntType),
                FoxTypeAlias("Wrapped", emptyOrderedSet(), FoxArrayType(FoxUnresolvedType("Base", null))),
                FoxTypeAlias("Final", emptyOrderedSet(), FoxRefType(FoxUnresolvedType("Wrapped", null))),
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
                            FoxUnresolvedType("Box", listOf(FoxUnresolvedType("A", null))),
                            FoxUnresolvedType("Box", listOf(FoxUnresolvedType("B", null))),
                        ),
                    ),
                ),
                FoxTypeAlias(
                    "Concrete",
                    emptyOrderedSet(),
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
                    FoxArrayType(FoxUnresolvedType("A", null)),
                    FoxArrayType(FoxUnresolvedType("B", null)),
                ),
            ),
            aliases.getValue("PairBox").alias,
        )
        assertTrue(noAliasDependency(aliases.getValue("PairBox").alias, aliases.keys))
        assertEquals(
            FoxTupleType(
                listOf(
                    FoxArrayType(FoxIntType),
                    FoxArrayType(FoxFloatType),
                ),
            ),
            aliases.getValue("Concrete").alias,
        )
        assertTrue(aliases.values.all { noAliasDependency(it.alias, aliases.keys) })
        assertTrue(noAliasDependency(aliases.getValue("Concrete").alias, aliases.keys))
    }
    
    @Test
    fun flattensAliasUsedAsGenericArgument() {
        val file = FoxFile(
            listOf(
                FoxTypeAlias("Base", emptyOrderedSet(), FoxIntType),
                FoxTypeAlias("Box", orderedSetOf("T"), FoxArrayType(FoxUnresolvedType("T", null))),
                FoxTypeAlias("Use", emptyOrderedSet(), FoxUnresolvedType("Box", listOf(FoxUnresolvedType("Base", null)))),
            ),
        )
        
        val result = assertIs<TypeAliasFlattenSuccess>(runTypeAliasFlatten(file))
        val aliases = result.newFile.elements.filterIsInstance<FoxTypeAlias>().associateBy { it.name }
        
        assertEquals(FoxArrayType(FoxIntType), aliases.getValue("Use").alias)
        assertTrue(noAliasDependency(aliases.getValue("Use").alias, aliases.keys))
    }
    
    @Test
    fun keepsMethodDefinitionsUntouched() {
        val method = FoxMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "id",
            parameters = orderedMapOf("value" to FoxUnresolvedType("Wrapped", null)),
            returnType = FoxUnresolvedType("Wrapped", null),
            body = FoxReturn(FoxUnresolvedSymbol("value")),
        )
        val file = FoxFile(
            listOf(
                FoxTypeAlias("Base", emptyOrderedSet(), FoxIntType),
                FoxTypeAlias("Wrapped", emptyOrderedSet(), FoxUnresolvedType("Base", null)),
                method,
            ),
        )
        
        val result = assertIs<TypeAliasFlattenSuccess>(runTypeAliasFlatten(file))
        assertEquals(method, result.newFile.elements.last())
    }
    
    @Test
    fun keepsOriginalFileElementOrder() {
        val method = FoxMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "use",
            parameters = orderedMapOf("value" to FoxIntType),
            returnType = FoxIntType,
            body = FoxReturn(FoxUnresolvedSymbol("value")),
        )
        val file = FoxFile(
            listOf(
                method,
                FoxTypeAlias("Base", emptyOrderedSet(), FoxIntType),
                FoxTypeAlias("Wrapped", emptyOrderedSet(), FoxArrayType(FoxUnresolvedType("Base", null))),
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
    fun detectsAliasLoop() {
        val a = FoxTypeAlias("A", emptyOrderedSet(), FoxUnresolvedType("B", null))
        val b = FoxTypeAlias("B", emptyOrderedSet(), FoxUnresolvedType("C", null))
        val c = FoxTypeAlias("C", emptyOrderedSet(), FoxUnresolvedType("B", null))
        val file = FoxFile(listOf(a, b, c))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val loop = assertIs<TypeAliasLoopDetected>(result.errors.single())
        
        assertEquals(listOf("B", "C"), loop.typeAliases.map { it.name })
        assertSame(b, loop.typeAliases[0])
        assertSame(c, loop.typeAliases[1])
    }
    
    @Test
    fun detectsDuplicatedAlias() {
        val first = FoxTypeAlias("A", emptyOrderedSet(), FoxIntType)
        val duplicate = FoxTypeAlias("A", emptyOrderedSet(), FoxFloatType)
        val file = FoxFile(
            listOf(
                first,
                duplicate,
            ),
        )
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasDuplicated>(result.errors.single())
        assertSame(duplicate, error.typeAlias)
    }
    
    @Test
    fun detectsMissingAliasReference() {
        val alias = FoxTypeAlias("A", emptyOrderedSet(), FoxUnresolvedType("Missing", null))
        val file = FoxFile(listOf(alias))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasNotFound>(result.errors.single())
        
        assertEquals("A", error.referredBy.name)
        assertEquals("Missing", error.typeName)
        assertSame(alias, error.referredBy)
    }
    
    @Test
    fun detectsUnexpectedGenericsOnNonGenericAlias() {
        val alias = FoxTypeAlias("A", emptyOrderedSet(), FoxIntType)
        val reference = FoxUnresolvedType("A", listOf(FoxIntType))
        val referrer = FoxTypeAlias("B", emptyOrderedSet(), reference)
        val file = FoxFile(listOf(alias, referrer))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasGenericCountMismatch>(result.errors.single())
        
        assertEquals("B", error.referredBy.name)
        assertSame(referrer, error.referredBy)
        assertSame(reference, error.type)
    }
    
    @Test
    fun detectsMissingGenericsOnGenericAlias() {
        val alias = FoxTypeAlias("Box", orderedSetOf("T"), FoxArrayType(FoxUnresolvedType("T", null)))
        val reference = FoxUnresolvedType("Box", null)
        val referrer = FoxTypeAlias("Use", emptyOrderedSet(), reference)
        val file = FoxFile(listOf(alias, referrer))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasGenericCountMismatch>(result.errors.single())
        
        assertEquals("Use", error.referredBy.name)
        assertSame(referrer, error.referredBy)
        assertSame(reference, error.type)
    }
    
    @Test
    fun detectsGenericCountMismatch() {
        val alias = FoxTypeAlias(
            "Pair",
            orderedSetOf("A", "B"),
            FoxTupleType(listOf(FoxUnresolvedType("A", null), FoxUnresolvedType("B", null))),
        )
        val reference = FoxUnresolvedType("Pair", listOf(FoxIntType))
        val referrer = FoxTypeAlias("Use", emptyOrderedSet(), reference)
        val file = FoxFile(listOf(alias, referrer))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasGenericCountMismatch>(result.errors.single())
        
        assertEquals("Use", error.referredBy.name)
        assertSame(referrer, error.referredBy)
        assertSame(reference, error.type)
    }
    
    private fun noAliasDependency(type: FoxType, aliasNames: Set<String>): Boolean {
        var result = true
        type.visitTypes<FoxUnresolvedType> { unresolved ->
            result = result && unresolved.name !in aliasNames && unresolved.parameters.orEmpty().all { noAliasDependency(it, aliasNames) }
        }
        return result
    }
}
