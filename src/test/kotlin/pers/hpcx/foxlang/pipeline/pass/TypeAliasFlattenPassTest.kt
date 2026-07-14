package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.type.visitTypes
import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.emptyOrderedSet
import pers.hpcx.foxlang.utils.orderedMapOf
import pers.hpcx.foxlang.utils.orderedSetOf
import kotlin.test.*

class TypeAliasFlattenPassTest {
    
    @Test
    fun reducesAliasDependencyChain() {
        val file = SurfaceFile(
            listOf(
                SurfaceTypeAlias("Base", emptyOrderedSet(), FoxIntType),
                SurfaceTypeAlias("Wrapped", emptyOrderedSet(), SurfaceArrayType(SurfaceUnresolvedType("Base", null))),
                SurfaceTypeAlias("Final", emptyOrderedSet(), SurfaceRefType(SurfaceUnresolvedType("Wrapped", null))),
            ),
        )
        
        val result = assertIs<TypeAliasFlattenSuccess>(runTypeAliasFlatten(file))
        val aliases = result.newFile.elements.filterIsInstance<SurfaceTypeAlias>().associateBy { it.name }
        
        assertEquals(FoxIntType, aliases.getValue("Base").alias)
        assertEquals(SurfaceArrayType(FoxIntType), aliases.getValue("Wrapped").alias)
        assertEquals(SurfaceRefType(SurfaceArrayType(FoxIntType)), aliases.getValue("Final").alias)
        assertTrue(aliases.values.all { noAliasDependency(it.alias, aliases.keys) })
        assertTrue(noAliasDependency(aliases.getValue("Final").alias, aliases.keys))
    }
    
    @Test
    fun reducesGenericAliasDependencyChain() {
        val file = SurfaceFile(
            listOf(
                SurfaceTypeAlias("Box", orderedSetOf("T"), SurfaceArrayType(SurfaceUnresolvedType("T", null))),
                SurfaceTypeAlias(
                    "PairBox",
                    orderedSetOf("A", "B"),
                    SurfaceTupleType(
                        listOf(
                            SurfaceUnresolvedType("Box", listOf(SurfaceUnresolvedType("A", null))),
                            SurfaceUnresolvedType("Box", listOf(SurfaceUnresolvedType("B", null))),
                        ),
                    ),
                ),
                SurfaceTypeAlias(
                    "Concrete",
                    emptyOrderedSet(),
                    SurfaceUnresolvedType(
                        "PairBox",
                        listOf(FoxIntType, FoxFloatType),
                    ),
                ),
            ),
        )
        
        val result = assertIs<TypeAliasFlattenSuccess>(runTypeAliasFlatten(file))
        val aliases = result.newFile.elements.filterIsInstance<SurfaceTypeAlias>().associateBy { it.name }
        
        assertEquals(SurfaceArrayType(SurfaceUnresolvedType("T", null)), aliases.getValue("Box").alias)
        assertTrue(noAliasDependency(aliases.getValue("Box").alias, aliases.keys))
        assertEquals(
            SurfaceTupleType(
                listOf(
                    SurfaceArrayType(SurfaceUnresolvedType("A", null)),
                    SurfaceArrayType(SurfaceUnresolvedType("B", null)),
                ),
            ),
            aliases.getValue("PairBox").alias,
        )
        assertTrue(noAliasDependency(aliases.getValue("PairBox").alias, aliases.keys))
        assertEquals(
            SurfaceTupleType(
                listOf(
                    SurfaceArrayType(FoxIntType),
                    SurfaceArrayType(FoxFloatType),
                ),
            ),
            aliases.getValue("Concrete").alias,
        )
        assertTrue(aliases.values.all { noAliasDependency(it.alias, aliases.keys) })
        assertTrue(noAliasDependency(aliases.getValue("Concrete").alias, aliases.keys))
    }
    
    @Test
    fun flattensAliasUsedAsGenericArgument() {
        val file = SurfaceFile(
            listOf(
                SurfaceTypeAlias("Base", emptyOrderedSet(), FoxIntType),
                SurfaceTypeAlias("Box", orderedSetOf("T"), SurfaceArrayType(SurfaceUnresolvedType("T", null))),
                SurfaceTypeAlias("Use", emptyOrderedSet(), SurfaceUnresolvedType("Box", listOf(SurfaceUnresolvedType("Base", null)))),
            ),
        )
        
        val result = assertIs<TypeAliasFlattenSuccess>(runTypeAliasFlatten(file))
        val aliases = result.newFile.elements.filterIsInstance<SurfaceTypeAlias>().associateBy { it.name }
        
        assertEquals(SurfaceArrayType(FoxIntType), aliases.getValue("Use").alias)
        assertTrue(noAliasDependency(aliases.getValue("Use").alias, aliases.keys))
    }
    
    @Test
    fun keepsMethodDefinitionsUntouched() {
        val method = SurfaceMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "id",
            parameters = orderedMapOf("value" to SurfaceUnresolvedType("Wrapped", null)),
            returnType = SurfaceUnresolvedType("Wrapped", null),
            body = SurfaceReturn(SurfaceUnresolvedSymbol("value")),
        )
        val file = SurfaceFile(
            listOf(
                SurfaceTypeAlias("Base", emptyOrderedSet(), FoxIntType),
                SurfaceTypeAlias("Wrapped", emptyOrderedSet(), SurfaceUnresolvedType("Base", null)),
                method,
            ),
        )
        
        val result = assertIs<TypeAliasFlattenSuccess>(runTypeAliasFlatten(file))
        assertEquals(method, result.newFile.elements.last())
    }
    
    @Test
    fun keepsOriginalFileElementOrder() {
        val method = SurfaceMethodDefinition(
            generics = emptyOrderedMap(),
            thisType = FoxUnitType,
            name = "use",
            parameters = orderedMapOf("value" to FoxIntType),
            returnType = FoxIntType,
            body = SurfaceReturn(SurfaceUnresolvedSymbol("value")),
        )
        val file = SurfaceFile(
            listOf(
                method,
                SurfaceTypeAlias("Base", emptyOrderedSet(), FoxIntType),
                SurfaceTypeAlias("Wrapped", emptyOrderedSet(), SurfaceArrayType(SurfaceUnresolvedType("Base", null))),
            ),
        )
        
        val result = assertIs<TypeAliasFlattenSuccess>(runTypeAliasFlatten(file))
        assertSame(method, result.newFile.elements[0])
        assertEquals(
            listOf("use", "Base", "Wrapped"),
            result.newFile.elements.map {
                when (it) {
                    is SurfaceMethodDefinition -> it.name
                    is SurfaceTypeAlias -> it.name
                }
            },
        )
    }
    
    @Test
    fun detectsAliasLoop() {
        val a = SurfaceTypeAlias("A", emptyOrderedSet(), SurfaceUnresolvedType("B", null))
        val b = SurfaceTypeAlias("B", emptyOrderedSet(), SurfaceUnresolvedType("C", null))
        val c = SurfaceTypeAlias("C", emptyOrderedSet(), SurfaceUnresolvedType("B", null))
        val file = SurfaceFile(listOf(a, b, c))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val loop = assertIs<TypeAliasLoopDetected>(result.errors.single())
        
        assertEquals(listOf("B", "C"), loop.typeAliases.map { it.name })
        assertSame(b, loop.typeAliases[0])
        assertSame(c, loop.typeAliases[1])
    }
    
    @Test
    fun detectsDuplicatedAlias() {
        val first = SurfaceTypeAlias("A", emptyOrderedSet(), FoxIntType)
        val duplicate = SurfaceTypeAlias("A", emptyOrderedSet(), FoxFloatType)
        val file = SurfaceFile(
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
        val alias = SurfaceTypeAlias("A", emptyOrderedSet(), SurfaceUnresolvedType("Missing", null))
        val file = SurfaceFile(listOf(alias))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasNotFound>(result.errors.single())
        
        assertEquals("A", error.referredBy.name)
        assertEquals("Missing", error.typeName)
        assertSame(alias, error.referredBy)
    }
    
    @Test
    fun detectsUnexpectedGenericsOnNonGenericAlias() {
        val alias = SurfaceTypeAlias("A", emptyOrderedSet(), FoxIntType)
        val reference = SurfaceUnresolvedType("A", listOf(FoxIntType))
        val referrer = SurfaceTypeAlias("B", emptyOrderedSet(), reference)
        val file = SurfaceFile(listOf(alias, referrer))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasGenericCountMismatch>(result.errors.single())
        
        assertEquals("B", error.referredBy.name)
        assertSame(referrer, error.referredBy)
        assertSame(reference, error.type)
    }
    
    @Test
    fun detectsMissingGenericsOnGenericAlias() {
        val alias = SurfaceTypeAlias("Box", orderedSetOf("T"), SurfaceArrayType(SurfaceUnresolvedType("T", null)))
        val reference = SurfaceUnresolvedType("Box", null)
        val referrer = SurfaceTypeAlias("Use", emptyOrderedSet(), reference)
        val file = SurfaceFile(listOf(alias, referrer))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasGenericCountMismatch>(result.errors.single())
        
        assertEquals("Use", error.referredBy.name)
        assertSame(referrer, error.referredBy)
        assertSame(reference, error.type)
    }
    
    @Test
    fun detectsGenericCountMismatch() {
        val alias = SurfaceTypeAlias(
            "Pair",
            orderedSetOf("A", "B"),
            SurfaceTupleType(listOf(SurfaceUnresolvedType("A", null), SurfaceUnresolvedType("B", null))),
        )
        val reference = SurfaceUnresolvedType("Pair", listOf(FoxIntType))
        val referrer = SurfaceTypeAlias("Use", emptyOrderedSet(), reference)
        val file = SurfaceFile(listOf(alias, referrer))
        
        val result = assertIs<TypeAliasFlattenFailure>(runTypeAliasFlatten(file))
        val error = assertIs<TypeAliasGenericCountMismatch>(result.errors.single())
        
        assertEquals("Use", error.referredBy.name)
        assertSame(referrer, error.referredBy)
        assertSame(reference, error.type)
    }
    
    private fun noAliasDependency(type: SurfaceType, aliasNames: Set<String>): Boolean {
        var result = true
        type.visitTypes<SurfaceUnresolvedType> { unresolved ->
            result = result && unresolved.name !in aliasNames && unresolved.parameters.orEmpty().all { noAliasDependency(it, aliasNames) }
        }
        return result
    }
}

private val FoxUnitType = SurfacePrimitiveType(PrimitiveTypeEnum.Unit)
private val FoxIntType = SurfacePrimitiveType(PrimitiveTypeEnum.Int)
private val FoxFloatType = SurfacePrimitiveType(PrimitiveTypeEnum.Float)
