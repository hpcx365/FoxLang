package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.space.*
import pers.hpcx.foxlang.utils.emptyOrderedSet
import kotlin.test.*

class TupleTypeSpaceContextTest : TypeSpaceContextTestBase() {
    
    @Test
    fun tupleProductTraverserStartsFromLexicographicallySmallestTupleAtMinimumHeight() {
        val traverser = tupleProduct(
            choice(FoxIntType, FoxStringType),
            choice(FoxBoolType, FoxStringType),
        ).traverser(context)
        
        assertEquals(listOf(FoxIntType, FoxBoolType).toFoxTupleType(), traverser.current())
    }
    
    @Test
    fun tupleProductTraverserMovesToCeilWithinTheSameHeightLayer() {
        val traverser = tupleProduct(
            choice(FoxIntType, FoxStringType),
            choice(FoxBoolType, FoxStringType),
        ).traverser(context)
        
        traverser.seekCeilOf(listOf(FoxIntType, FoxStringType).toFoxTupleType())
        
        assertEquals(listOf(FoxIntType, FoxStringType).toFoxTupleType(), traverser.current())
    }
    
    @Test
    fun tupleProductTraverserMovesNextInLexicographicOrder() {
        val traverser = tupleProduct(
            choice(FoxIntType, FoxStringType),
            choice(FoxBoolType, FoxStringType),
        ).traverser(context)
        
        traverser.seekNext()
        assertEquals(listOf(FoxIntType, FoxStringType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxStringType, FoxBoolType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxStringType, FoxStringType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertNull(traverser.current())
    }
    
    @Test
    fun tupleProductTraverserReportsItsCurrentSize() {
        val traverser = tupleProduct(
            choice(FoxIntType),
            choice(FoxBoolType),
        ).traverser(context)
        
        assertEquals(2, (traverser.current() as FoxTupleType).arity)
    }
    
    @Test
    fun tupleProductTraverserForEmptyProductExhaustsAfterEmptyTuple() {
        val traverser = tupleProduct(emptyList()).traverser(context)
        
        assertEquals(emptyList<FoxType>().toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        
        assertNull(traverser.current())
    }
    
    @Test
    fun tupleProductTraverserCanAdvanceToNextHeightLayer() {
        val traverser = tupleProduct(
            choice(FoxIntType),
            union(
                singleSpace(FoxArrayType(FoxIntType)),
                singleSpace(FoxRefType(FoxIntType)),
            ),
        ).traverser(context)
        
        assertEquals(listOf(FoxIntType, FoxArrayType(FoxIntType)).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        
        assertEquals(listOf(FoxIntType, FoxRefType(FoxIntType)).toFoxTupleType(), traverser.current())
    }
    
    @Test
    fun tupleRepeatTraverserStartsFromEmptyTupleWhenZeroLengthIsAllowed() {
        val traverser = tupleRepeat(
            component = choice(FoxIntType, FoxStringType),
            minArity = 0,
            maxArity = 2,
        ).traverser(context)
        
        assertEquals(emptyList<FoxType>().toFoxTupleType(), traverser.current())
    }
    
    @Test
    fun tupleRepeatTraverserForZeroOnlyRangeExhaustsAfterEmptyTuple() {
        val traverser = tupleRepeat(
            component = choice(FoxIntType, FoxStringType),
            minArity = 0,
            maxArity = 0,
        ).traverser(context)
        
        assertEquals(emptyList<FoxType>().toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        
        assertNull(traverser.current())
    }
    
    @Test
    fun tupleRepeatTraverserOrdersByLengthWithinSameHeight() {
        val traverser = tupleRepeat(
            component = choice(FoxIntType, FoxStringType),
            minArity = 0,
            maxArity = 2,
        ).traverser(context)
        
        traverser.seekNext()
        assertEquals(listOf(FoxIntType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxStringType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxIntType, FoxIntType).toFoxTupleType(), traverser.current())
    }
    
    @Test
    fun tupleRepeatTraverserMovesToCeilAcrossLengths() {
        val traverser = tupleRepeat(
            component = choice(FoxIntType, FoxStringType),
            minArity = 0,
            maxArity = 2,
        ).traverser(context)
        
        traverser.seekCeilOf(listOf(FoxIntType, FoxStringType).toFoxTupleType())
        
        assertEquals(listOf(FoxIntType, FoxStringType).toFoxTupleType(), traverser.current())
    }
    
    @Test
    fun tupleRepeatTraverserReturnsNullWhenLowerBoundIsNonTupleBuiltInType() {
        val traverser = tupleRepeat(
            component = choice(FoxIntType, FoxStringType),
            minArity = 0,
            maxArity = 2,
        ).traverser(context)
        
        traverser.seekCeilOf(FoxArrayType(FoxIntType))
        
        assertNull(traverser.current())
    }
    
    @Test
    fun tupleComponentAtSpaceContainsRequiresMatchingComponentAtIndex() {
        val lang = TupleComponentAtPreimageSpace(1, singleSpace(FoxIntType))
        
        assertTrue(lang.contains(listOf(FoxBoolType, FoxIntType).toFoxTupleType(), context))
        assertTrue(lang.contains(listOf(FoxBoolType, FoxIntType, FoxStringType).toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxBoolType, FoxStringType).toFoxTupleType(), context))
    }
    
    @Test
    fun tupleComponentAtTraverserStartsFromShortestTupleWithConstrainedIndex() {
        val localContext = TypeSpaceContext(TypeBounds(maxHeight = 2, maxTupleArity = 2, maxStructArity = 0, nameDictionary = emptyOrderedSet()))
        val traverser = TupleComponentAtPreimageSpace(
            index = 1,
            component = singleSpace(FoxIntType),
        ).traverser(localContext)
        
        assertEquals(listOf(FoxVoidType, FoxIntType).toFoxTupleType(), traverser.current())
    }
    
    @Test
    fun tupleLastComponentAtSpaceContainsRequiresMatchingComponentFromTail() {
        val lang = TupleLastComponentAtPreimageSpace(1, singleSpace(FoxIntType))
        
        assertTrue(lang.contains(listOf(FoxIntType, FoxBoolType).toFoxTupleType(), context))
        assertTrue(lang.contains(listOf(FoxStringType, FoxIntType, FoxBoolType).toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxBoolType, FoxStringType).toFoxTupleType(), context))
    }
    
    @Test
    fun tupleLastComponentAtTraverserStartsFromShortestTupleWithConstrainedTailIndex() {
        val localContext = TypeSpaceContext(TypeBounds(maxHeight = 2, maxTupleArity = 2, maxStructArity = 0, nameDictionary = emptyOrderedSet()))
        val traverser = TupleLastComponentAtPreimageSpace(
            index = 1,
            component = singleSpace(FoxIntType),
        ).traverser(localContext)
        
        assertEquals(listOf(FoxIntType, FoxVoidType).toFoxTupleType(), traverser.current())
    }
    
    @Test
    fun tuplesAdapterFiltersNonTupleValuesFromMixedTraverser() {
        val traverser = union<FoxType, TypeSpaceContext>(
            singleSpace(FoxIntType),
            singleSpace(emptyList<FoxType>().toFoxTupleType()),
            singleSpace(listOf(FoxStringType).toFoxTupleType()),
            singleSpace(FoxArrayType(FoxIntType)),
        ).tupleTraverser(context)
        
        assertEquals(emptyList<FoxType>().toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxStringType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertNull(traverser.current())
    }
    
    @Test
    fun tuplesOfSizeAdapterKeepsOnlyRequestedTupleArity() {
        val traverser = union<FoxType, TypeSpaceContext>(
            singleSpace(emptyList<FoxType>().toFoxTupleType()),
            singleSpace(listOf(FoxIntType).toFoxTupleType()),
            singleSpace(listOf(FoxIntType, FoxBoolType).toFoxTupleType()),
        ).tupleTraverserOfSize(1, context)
        
        assertEquals(listOf(FoxIntType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertNull(traverser.current())
    }
    
    @Test
    fun tupleConcatTraverserOrdersByTheWholeConcatenatedTuple() {
        val traverser = tupleConcat(
            union(
                singleSpace(listOf(FoxStringType).toFoxTupleType()),
                singleSpace(listOf(FoxIntType, FoxBoolType).toFoxTupleType()),
            ),
            union(
                singleSpace(emptyList<FoxType>().toFoxTupleType()),
                singleSpace(listOf(FoxIntType).toFoxTupleType()),
            ),
        ).traverser(context)
        
        assertEquals(listOf(FoxStringType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxIntType, FoxBoolType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxStringType, FoxIntType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxIntType, FoxBoolType, FoxIntType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertNull(traverser.current())
    }
    
    @Test
    fun tupleConcatTraverserDeduplicatesEqualConcatenatedResults() {
        val empty = singleSpace<FoxType, TypeSpaceContext>(emptyList<FoxType>().toFoxTupleType())
        val singleInt = singleSpace<FoxType, TypeSpaceContext>(listOf(FoxIntType).toFoxTupleType())
        val traverser = tupleConcat(
            union(listOf(empty, singleInt)),
            union(listOf(empty, singleInt)),
        ).traverser(context)
        
        assertEquals(emptyList<FoxType>().toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxIntType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxIntType, FoxIntType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertNull(traverser.current())
    }
    
    @Test
    fun tupleConcatContainsAcceptsVariableLengthPartition() {
        val lang = tupleConcat(
            singleSpace(listOf(FoxIntType).toFoxTupleType()),
            tupleRepeat(singleSpace(FoxIntType), 0, context.bounds.maxTupleArity),
        )
        
        assertTrue(lang.contains(FoxTupleType(listOf(FoxIntType to 3)), context))
    }
    
    @Test
    fun tupleConcatContainsRejectsWhenNoPartitionMatchesAllParts() {
        val lang = tupleConcat(
            singleSpace(listOf(FoxIntType).toFoxTupleType()),
            singleSpace(listOf(FoxStringType).toFoxTupleType()),
        )
        
        assertFalse(lang.contains(FoxTupleType(listOf(FoxIntType to 2)), context))
    }
    
    @Test
    fun tupleConcatContainsAllowsMixedPartsThatCanProduceTuples() {
        val lang = tupleConcat(
            universe(finiteContext.bounds.maxHeight),
            singleSpace(listOf(FoxIntType).toFoxTupleType()),
        )
        
        assertTrue(lang.contains(listOf(FoxIntType).toFoxTupleType(), finiteContext))
        assertTrue(lang.contains(listOf(FoxVoidType, FoxIntType).toFoxTupleType(), finiteContext))
    }
    
    @Test
    fun tupleConcatContainsTreatsEmptyConcatenationAsEmptyTupleOnly() {
        val lang = tupleConcat(emptyList())
        
        assertTrue(lang.contains(emptyList<FoxType>().toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
    }
    
    @Test
    fun compileConcreteTupleTypeToTupleProductLang() {
        val lang = (compileType(listOf(FoxIntType, FoxStringType).toFoxTupleType(), context) as TypeCompileSuccess).typeSpace
        
        assertTrue(lang is TupleProductSpace)
        assertEquals(listOf(FoxIntType, FoxStringType).toFoxTupleType(), lang.traverser(context).current())
    }
    
    @Test
    fun compileAnyTupleTypeToTupleRepeatLang() {
        val lang = (compileType(FoxAnyTupleType, context) as TypeCompileSuccess).typeSpace
        
        assertEquals(tupleRepeat(universe(context.bounds.maxHeight), 0, context.bounds.maxTupleArity), lang)
    }
    
    @Test
    fun compileAnyTupleOfTypeToTupleRepeatLang() {
        val lang = (compileType(FoxAnyTupleOfType(FoxIntType), context) as TypeCompileSuccess).typeSpace
        
        assertEquals(tupleRepeat(singleSpace(FoxIntType), 0, context.bounds.maxTupleArity), lang)
    }
    
    @Test
    fun compileTupleComponentAtTypeToProjectiveLang() {
        val lang = (compileType(FoxTupleComponentAtType(listOf(FoxBoolType, FoxIntType).toFoxTupleType(), 1), context) as TypeCompileSuccess).typeSpace
        
        assertEquals(
            TupleComponentAtProjectiveSpace(
                TupleProductSpace(listOf(singleSpace(FoxBoolType), singleSpace(FoxIntType))),
                1,
            ),
            lang,
        )
        assertTrue(lang.contains(FoxIntType, context))
        assertFalse(lang.contains(FoxStringType, context))
    }
    
    @Test
    fun compileTupleLastComponentAtTypeToProjectiveLang() {
        val lang = (compileType(FoxTupleLastComponentAtType(listOf(FoxIntType, FoxBoolType).toFoxTupleType(), 1), context) as TypeCompileSuccess).typeSpace
        
        assertEquals(
            TupleLastComponentAtProjectiveSpace(
                TupleProductSpace(listOf(singleSpace(FoxIntType), singleSpace(FoxBoolType))),
                1,
            ),
            lang,
        )
        assertTrue(lang.contains(FoxIntType, context))
        assertFalse(lang.contains(FoxStringType, context))
    }
    
    @Test
    fun compileArrayElementOfTypeToProjectiveLang() {
        val lang = (compileType(FoxArrayElementOfType(FoxArrayType(FoxIntType)), context) as TypeCompileSuccess).typeSpace
        
        assertEquals(ArrayElementProjectiveSpace(ArraySpace(singleSpace(FoxIntType))), lang)
        assertTrue(lang.contains(FoxIntType, context))
        assertFalse(lang.contains(FoxStringType, context))
    }
    
    @Test
    fun compileRefReferentOfTypeToProjectiveLang() {
        val lang = (compileType(FoxRefReferentOfType(FoxRefType(FoxIntType)), context) as TypeCompileSuccess).typeSpace
        
        assertEquals(RefReferentProjectiveSpace(RefSpace(singleSpace(FoxIntType))), lang)
        assertTrue(lang.contains(FoxIntType, context))
        assertFalse(lang.contains(FoxStringType, context))
    }
    
    @Test
    fun arrayElementProjectiveContainsUsesPreimageIntersection() {
        val lang = ArrayElementProjectiveSpace(
            union(
                singleSpace(FoxArrayType(FoxIntType)),
                singleSpace(FoxArrayType(FoxStringType)),
            ),
        )
        
        assertTrue(lang.contains(FoxIntType, context))
        assertTrue(lang.contains(FoxStringType, context))
        assertFalse(lang.contains(FoxBoolType, context))
    }
    
    @Test
    fun tupleComponentAtProjectiveContainsUsesPreimageIntersection() {
        val lang = TupleComponentAtProjectiveSpace(
            union(
                singleSpace(listOf(FoxBoolType, FoxIntType).toFoxTupleType()),
                singleSpace(listOf(FoxStringType, FoxStringType).toFoxTupleType()),
            ),
            1,
        )
        
        assertTrue(lang.contains(FoxIntType, context))
        assertTrue(lang.contains(FoxStringType, context))
        assertFalse(lang.contains(FoxBoolType, context))
    }
    
    @Test
    fun tupleLastComponentAtProjectiveContainsUsesPreimageIntersection() {
        val lang = TupleLastComponentAtProjectiveSpace(
            union(
                singleSpace(listOf(FoxIntType, FoxBoolType).toFoxTupleType()),
                singleSpace(listOf(FoxStringType, FoxStringType).toFoxTupleType()),
            ),
            1,
        )
        
        assertTrue(lang.contains(FoxIntType, context))
        assertTrue(lang.contains(FoxStringType, context))
        assertFalse(lang.contains(FoxBoolType, context))
    }
    
    @Test
    fun tupleDropFirstProjectiveContainsUsesPreimageIntersection() {
        val lang = TupleDropFirstProjectiveSpace(
            union(
                singleSpace(listOf(FoxBoolType, FoxIntType).toFoxTupleType()),
                singleSpace(listOf(FoxStringType).toFoxTupleType()),
            ),
            1,
            exact = false,
        )
        
        assertTrue(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
        assertTrue(lang.contains(emptyList<FoxType>().toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxBoolType).toFoxTupleType(), context))
    }
    
    @Test
    fun tupleDropLastProjectiveContainsUsesPreimageIntersection() {
        val lang = TupleDropLastProjectiveSpace(
            union(
                singleSpace(listOf(FoxIntType, FoxBoolType).toFoxTupleType()),
                singleSpace(listOf(FoxStringType).toFoxTupleType()),
            ),
            1,
            exact = false,
        )
        
        assertTrue(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
        assertTrue(lang.contains(emptyList<FoxType>().toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxBoolType).toFoxTupleType(), context))
    }
    
    @Test
    fun tupleDropFirstExactPreimageRequiresExactSourceLength() {
        val lang = TupleDropFirstPreimageSpace(1, emptyList<FoxType>().toFoxTupleType(), exact = true)
        
        assertTrue(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
        assertFalse(lang.contains(emptyList<FoxType>().toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxIntType, FoxBoolType).toFoxTupleType(), context))
    }
    
    @Test
    fun tupleDropLastExactPreimageRequiresExactSourceLength() {
        val lang = TupleDropLastPreimageSpace(1, emptyList<FoxType>().toFoxTupleType(), exact = true)
        
        assertTrue(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
        assertFalse(lang.contains(emptyList<FoxType>().toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxIntType, FoxBoolType).toFoxTupleType(), context))
    }
    
    @Test
    fun tupleFirstProjectiveContainsUsesPreimageIntersection() {
        val lang = TupleFirstProjectiveSpace(
            union(
                singleSpace(listOf(FoxIntType, FoxBoolType).toFoxTupleType()),
                singleSpace(listOf(FoxStringType).toFoxTupleType()),
            ),
            1,
            exact = false,
        )
        
        assertTrue(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
        assertTrue(lang.contains(listOf(FoxStringType).toFoxTupleType(), context))
        assertFalse(lang.contains(emptyList<FoxType>().toFoxTupleType(), context))
    }
    
    @Test
    fun tupleLastProjectiveContainsUsesPreimageIntersection() {
        val lang = TupleLastProjectiveSpace(
            union(
                singleSpace(listOf(FoxIntType, FoxBoolType).toFoxTupleType()),
                singleSpace(listOf(FoxStringType).toFoxTupleType()),
            ),
            1,
            exact = false,
        )
        
        assertTrue(lang.contains(listOf(FoxBoolType).toFoxTupleType(), context))
        assertTrue(lang.contains(listOf(FoxStringType).toFoxTupleType(), context))
        assertFalse(lang.contains(emptyList<FoxType>().toFoxTupleType(), context))
    }
    
    @Test
    fun tupleFirstExactPreimageRequiresResultArityToEqualCount() {
        val lang = TupleFirstPreimageSpace(1, emptyList<FoxType>().toFoxTupleType(), exact = true)
        
        assertFalse(lang.contains(emptyList<FoxType>().toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
    }
    
    @Test
    fun tupleFirstLoosePreimageWithShortResultCollapsesToSingleton() {
        val lang = TupleFirstPreimageSpace(1, emptyList<FoxType>().toFoxTupleType(), exact = false)
        
        assertTrue(lang.contains(emptyList<FoxType>().toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
    }
    
    @Test
    fun tupleLastExactPreimageRequiresResultArityToEqualCount() {
        val lang = TupleLastPreimageSpace(1, emptyList<FoxType>().toFoxTupleType(), exact = true)
        
        assertFalse(lang.contains(emptyList<FoxType>().toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
    }
    
    @Test
    fun tupleLastLoosePreimageWithShortResultCollapsesToSingleton() {
        val lang = TupleLastPreimageSpace(1, emptyList<FoxType>().toFoxTupleType(), exact = false)
        
        assertTrue(lang.contains(emptyList<FoxType>().toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
    }
    
    @Test
    fun refReferentProjectiveContainsUsesPreimageIntersection() {
        val lang = RefReferentProjectiveSpace(
            union(
                singleSpace(FoxRefType(FoxIntType)),
                singleSpace(FoxRefType(FoxStringType)),
            ),
        )
        
        assertTrue(lang.contains(FoxIntType, context))
        assertTrue(lang.contains(FoxStringType, context))
        assertFalse(lang.contains(FoxBoolType, context))
    }
    
    @Test
    fun compileTupleMergeComponentsOfToTupleConcatLang() {
        val lang = (compileType(
            FoxTupleMergeComponentsOfType(
                listOf(
                    FoxTupleType(listOf(FoxIntType to 2)),
                    FoxAnyTupleOfType(FoxIntType),
                ),
            ),
            context,
        ) as TypeCompileSuccess).typeSpace
        
        assertEquals(
            TupleConcatSpace(
                listOf(
                    TupleProductSpace(listOf(singleSpace(FoxIntType), singleSpace(FoxIntType))),
                    tupleRepeat(singleSpace(FoxIntType), 0, context.bounds.maxTupleArity),
                ),
            ),
            lang,
        )
        
        val traverser = (lang as TraversableSpace).traverser(context)
        assertEquals(FoxTupleType(listOf(FoxIntType to 2)), traverser.current())
        
        traverser.seekNext()
        assertEquals(FoxTupleType(listOf(FoxIntType to 3)), traverser.current())
    }
    
    @Test
    fun compileTupleMergeComponentsOfAcceptsMixedLanguagePartsThatCanProduceTuples() {
        val lang = (compileType(
            FoxTupleMergeComponentsOfType(
                listOf(
                    FoxAnyType,
                    FoxTupleType(listOf(FoxIntType to 1)),
                ),
            ),
            finiteContext,
        ) as TypeCompileSuccess).typeSpace
        
        assertEquals(
            TupleConcatSpace(
                listOf(
                    universe(finiteContext.bounds.maxHeight),
                    TupleProductSpace(listOf(singleSpace(FoxIntType))),
                ),
            ),
            lang,
        )
        
        val traverser = (lang as TraversableSpace).traverser(finiteContext)
        assertEquals(FoxTupleType(listOf(FoxIntType to 1)), traverser.current())
    }
    
    @Test
    fun compileAnyOfTypeToUnionLang() {
        val lang = (compileType(FoxAnyOfType(listOf(FoxIntType, FoxStringType)), context) as TypeCompileSuccess).typeSpace
        
        assertEquals(union<FoxType, TypeSpaceContext>(listOf(singleSpace(FoxIntType), singleSpace(FoxStringType))), lang)
    }
    
    @Test
    fun compileAnyOfTupleConcreteAndWildcardContainsBothBranches() {
        val lang = (compileType(
            FoxAnyOfType(
                listOf(
                    listOf(FoxIntType).toFoxTupleType(),
                    FoxAnyTupleOfType(FoxStringType),
                ),
            ),
            context,
        ) as TypeCompileSuccess).typeSpace
        
        assertTrue(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
        assertTrue(lang.contains(listOf(FoxStringType, FoxStringType).toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxIntType, FoxIntType).toFoxTupleType(), context))
    }
    
    @Test
    fun unionTraverserSeekNextOnlyAdvancesBranchesAtCurrentMinimum() {
        val traverser = union<FoxType, TypeSpaceContext>(
            singleSpace(FoxIntType),
            union(listOf(singleSpace(FoxBoolType), singleSpace(FoxStringType))),
        ).traverser(context)
        
        assertEquals(FoxBoolType, traverser.current())
        
        traverser.seekNext()
        
        assertEquals(FoxIntType, traverser.current())
    }
    
    @Test
    fun unionTraverserSeekCeilOfAdvancesEveryBranchBelowTheRequestedLowerBound() {
        val traverser = union<FoxType, TypeSpaceContext>(
            singleSpace(FoxBoolType),
            singleSpace(FoxByteType),
            singleSpace(FoxIntType),
        ).traverser(context)
        
        traverser.seekCeilOf(FoxIntType)
        
        assertEquals(FoxIntType, traverser.current())
    }
    
    @Test
    fun unionTraverserSeekCeilOfFamilyAdvancesEveryBranchBelowTheRequestedFamily() {
        val traverser = union<FoxType, TypeSpaceContext>(
            listOf(
                singleSpace(FoxBoolType),
                singleSpace(FoxByteType),
                singleSpace(FoxIntType),
            ),
        ).traverser(context)
        
        traverser.seekCeilOf(FoxIntType)
        
        assertEquals(FoxIntType, traverser.current())
    }
    
    @Test
    fun unionTraverserDeduplicatesValuesThatAreEqualOnlyUnderTupleOrdering() {
        val normalized = FoxTupleType(listOf(FoxIntType to 2))
        val nonNormalized = FoxTupleType(listOf(FoxIntType to 1, FoxIntType to 1))
        val traverser = union<FoxType, TypeSpaceContext>(
            listOf(
                singleSpace(nonNormalized),
                singleSpace(normalized),
            ),
        ).traverser(context)
        
        assertEquals(0, context.compare(traverser.current()!!, normalized))
        
        traverser.seekNext()
        
        assertNull(traverser.current())
    }
    
    @Test
    fun compileAllOfTypeToIntersectLang() {
        val lang = (compileType(FoxAllOfType(listOf(FoxIntType, FoxStringType)), context) as TypeCompileSuccess).typeSpace
        
        assertEquals(intersect<FoxType, TypeSpaceContext>(listOf(singleSpace(FoxIntType), singleSpace(FoxStringType))), lang)
    }
    
    @Test
    fun compileNoneOfTypeToSubtractLang() {
        val lang = (compileType(FoxNoneOfType(listOf(FoxIntType, FoxStringType)), context) as TypeCompileSuccess).typeSpace
        
        assertEquals(
            TraversableSubtractSpace(
                universe(context.bounds.maxHeight),
                union(singleSpace(FoxIntType), singleSpace(FoxStringType)),
            ),
            lang,
        )
    }
    
    @Test
    fun compileAllOfTupleWildcardAndConcreteNarrowsToConcreteTuple() {
        val lang = (compileType(
            FoxAllOfType(
                listOf(
                    FoxAnyTupleOfType(FoxIntType),
                    FoxTupleType(listOf(FoxIntType to 2)),
                ),
            ),
            context,
        ) as TypeCompileSuccess).typeSpace
        
        val traverser = (lang as TraversableTypeSpace).tupleTraverser(context)
        assertEquals(FoxTupleType(listOf(FoxIntType to 2)), traverser.current())
        assertTrue(lang.contains(FoxTupleType(listOf(FoxIntType to 2)), context))
        assertFalse(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
    }
    
    @Test
    fun compileAllOfConflictingTupleSpacesProducesEmptyIntersection() {
        val lang = (compileType(
            FoxAllOfType(
                listOf(
                    FoxAnyTupleOfType(FoxIntType),
                    listOf(FoxStringType).toFoxTupleType(),
                ),
            ),
            context,
        ) as TypeCompileSuccess).typeSpace
        
        assertEquals(
            intersect(
                tupleRepeat(singleSpace(FoxIntType), 0, context.bounds.maxTupleArity),
                TupleProductSpace(listOf(singleSpace(FoxStringType))),
            ),
            lang,
        )
    }
    
    @Test
    fun tupleRepeatSeekCeilOfStopsWhenLongerTupleAlreadyExceedsTarget() {
        val traverser = tupleRepeat(
            singleSpace(FoxIntType),
            0,
            3,
        ).traverser(context)
        
        traverser.seekCeilOf(listOf(FoxStringType).toFoxTupleType())
        
        assertEquals(listOf(FoxIntType, FoxIntType).toFoxTupleType(), traverser.current())
    }
    
    @Test
    fun subtractTraverserRemovesConcreteTupleFromTupleWildcardSpace() {
        val lang = subtract(
            tupleRepeat(singleSpace(FoxIntType), 0, 2),
            singleSpace(listOf(FoxIntType).toFoxTupleType()),
        )
        val traverser = lang.tupleTraverser(context)
        
        assertEquals(emptyList<FoxType>().toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxIntType, FoxIntType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertNull(traverser.current())
        
        assertTrue(lang.contains(emptyList<FoxType>().toFoxTupleType(), context))
        assertFalse(lang.contains(listOf(FoxIntType).toFoxTupleType(), context))
        assertTrue(lang.contains(listOf(FoxIntType, FoxIntType).toFoxTupleType(), context))
    }
    
    @Test
    fun subtractTraverserRemovesTupleValuesEqualOnlyUnderTupleOrdering() {
        val lang = subtract<FoxType, TypeSpaceContext>(
            union(
                singleSpace(FoxTupleType(listOf(FoxIntType to 1, FoxIntType to 1))),
                singleSpace(FoxTupleType(listOf(FoxIntType to 2))),
            ),
            singleSpace(FoxTupleType(listOf(FoxIntType to 2))),
        )
        
        assertNull(lang.tupleTraverser(context).current())
    }
    
    @Test
    fun universeTraverserStartsFromSmallestConcreteType() {
        val traverser = universe(context.bounds.maxHeight).traverser(finiteContext)
        
        assertEquals(FoxVoidType, traverser.current())
    }
    
    @Test
    fun universeTraverserCanMoveToEmptyTuple() {
        val traverser = universe(context.bounds.maxHeight).traverser(finiteContext)
        
        traverser.seekCeilOf(emptyList<FoxType>().toFoxTupleType())
        
        assertEquals(emptyList<FoxType>().toFoxTupleType(), traverser.current())
    }
    
    @Test
    fun tupleFirstPreimageTraverserStartsFromSingletonWhenResultIsShorterThanCount() {
        val traverser = TupleFirstPreimageSpace(2, listOf(FoxIntType).toFoxTupleType(), exact = false).traverser(context)
        
        assertEquals(listOf(FoxIntType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertNull(traverser.current())
    }
    
    @Test
    fun tupleLastPreimageTraverserStartsFromSingletonWhenResultIsShorterThanCount() {
        val traverser = TupleLastPreimageSpace(2, listOf(FoxIntType).toFoxTupleType(), exact = false).traverser(context)
        
        assertEquals(listOf(FoxIntType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertNull(traverser.current())
    }
    
    @Test
    fun tupleFirstPreimageTraverserStartsFromFixedPrefixWhenResultArityEqualsCount() {
        val localContext = TypeSpaceContext(TypeBounds(maxHeight = 2, maxTupleArity = 2, maxStructArity = 0, nameDictionary = emptyOrderedSet()))
        val traverser = TupleFirstPreimageSpace(1, listOf(FoxIntType).toFoxTupleType(), exact = false).traverser(localContext)
        
        assertEquals(listOf(FoxIntType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxIntType, FoxVoidType).toFoxTupleType(), traverser.current())
    }
    
    @Test
    fun tupleLastPreimageTraverserStartsFromFixedSuffixWhenResultArityEqualsCount() {
        val localContext = TypeSpaceContext(TypeBounds(maxHeight = 2, maxTupleArity = 2, maxStructArity = 0, nameDictionary = emptyOrderedSet()))
        val traverser = TupleLastPreimageSpace(1, listOf(FoxIntType).toFoxTupleType(), exact = false).traverser(localContext)
        
        assertEquals(listOf(FoxIntType).toFoxTupleType(), traverser.current())
        
        traverser.seekNext()
        assertEquals(listOf(FoxVoidType, FoxIntType).toFoxTupleType(), traverser.current())
    }
    
    @Test
    fun tupleConcatTraverserSeekNextSkipsCompareEqualNonNormalizedPrefix() {
        val traverser = TupleConcatSpace(
            listOf(
                singleSpace(FoxTupleType(listOf(FoxIntType to 1, FoxIntType to 1))),
                union(
                    listOf(
                        singleSpace(emptyList<FoxType>().toFoxTupleType()),
                        singleSpace(listOf(FoxIntType).toFoxTupleType()),
                    ),
                ),
            ),
        ).traverser(context)
        
        assertEquals(FoxTupleType(listOf(FoxIntType to 2)), traverser.current())
        
        traverser.seekNext()
        
        assertEquals(FoxTupleType(listOf(FoxIntType to 3)), traverser.current())
    }
}
