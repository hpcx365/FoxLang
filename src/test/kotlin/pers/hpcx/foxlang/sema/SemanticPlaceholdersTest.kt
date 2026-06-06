package pers.hpcx.foxlang.sema

import pers.hpcx.foxlang.frontend.FoxGrammar
import pers.hpcx.foxlang.frontend.FoxStarters
import pers.hpcx.foxlang.frontend.NodeFile
import pers.hpcx.foxlang.frontend.parser.Cursor
import pers.hpcx.foxlang.frontend.parser.Success
import pers.hpcx.foxlang.frontend.parser.node
import pers.hpcx.foxlang.frontend.parser.render
import pers.hpcx.foxlang.types.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SemanticPlaceholdersTest {
    
    @Test
    fun collectsStructuralSemanticPlaceholders() {
        val file = parse(
            """
            def <T = Struct<*>> forEachPrint(*T) {
                genfor (f: F in T) {
                    print<F>(f)
                }
            }
            """.trimIndent(),
        )
        
        val report = file.collectSemanticPlaceholders()
        
        val method = report.methods.single()
        assertEquals("forEachPrint", method.name)
        assertEquals(FoxStructWildcardConstraint, method.generics.getValue("T"))
        assertIs<FoxSplatMethodParameterPlaceholder>(method.parameters.single())
        assertIs<NormalizationError>(method.normalizedParameters)
        assertEquals(2, method.placeholders.size)
        assertTrue(method.placeholders.any { it is FoxSplatParameterPlaceholder })
        val genFor = assertIs<FoxGenForPlaceholder>(method.placeholders.single { it is FoxGenForPlaceholder })
        assertEquals("f", genFor.valueName)
        assertEquals("F", genFor.typeName)
        assertEquals(FoxGenericType("T"), genFor.targetType)
        assertEquals(2, report.diagnostics.size)
        assertTrue(report.diagnostics.any { "Splat parameter" in it.message })
        assertTrue(report.diagnostics.any { "genfor" in it.message })
    }
    
    @Test
    fun lowersStructWildcardConstraintInAliasesAndMethods() {
        val file = parse(
            """
            type AnyStruct = Struct<*>
            def <T = Struct<*>> foo(arg: T) {}
            """.trimIndent(),
        )
        
        val report = file.collectSemanticPlaceholders()
        
        assertEquals(1, report.typeAliases.size)
        assertEquals("AnyStruct", report.typeAliases.single().name)
        assertTrue(report.diagnostics.any { "Standalone type 'Struct<*>'" in it.message })
        assertEquals(FoxStructWildcardConstraint, report.methods.single().generics.getValue("T"))
        assertIs<Normalized<List<NormalizedMethodParameter>>>(report.methods.single().normalizedParameters)
    }
    
    @Test
    fun preservesTupleAndStructSpreadInSemanticTemplates() {
        val file = parse(
            """
            type ExpandedTuple = Tuple<Int, *Another, Bool>
            type ExpandedStruct = Struct<name: String, *Address, active: Bool>
            """.trimIndent(),
        )
        
        val report = file.collectSemanticPlaceholders()
        
        val tupleAlias = report.typeAliases[0]
        val tupleType = assertIs<FoxTupleTemplateType>(tupleAlias.alias)
        assertEquals(3, tupleType.items.size)
        assertIs<FoxTupleTypeTemplateItem>(tupleType.items[0])
        assertIs<FoxTupleSpreadTemplateItem>(tupleType.items[1])
        assertIs<FoxTupleTypeTemplateItem>(tupleType.items[2])
        
        val structAlias = report.typeAliases[1]
        val structType = assertIs<FoxStructTemplateType>(structAlias.alias)
        assertEquals(3, structType.items.size)
        assertIs<FoxStructFieldTemplateItem>(structType.items[0])
        assertIs<FoxStructSpreadTemplateItem>(structType.items[1])
        assertIs<FoxStructFieldTemplateItem>(structType.items[2])
    }
    
    @Test
    fun collectsOrderedCallSiteSemantics() {
        val file = parse(
            """
            def demo() {
                target(second = 2, first = 1)
                Result(1, 2)
            }
            """.trimIndent(),
        )
        
        val report = file.collectSemanticPlaceholders()
        
        assertEquals(2, report.callSites.size)
        
        val methodCall = report.callSites[0]
        assertEquals(FoxCallSiteKind.METHOD_CALL, methodCall.kind)
        assertEquals("target", methodCall.targetName)
        assertEquals(FoxArgumentShape.NAMED, methodCall.argumentShape)
        assertEquals(listOf("second", "first"), methodCall.arguments.map { it.name })
        assertEquals(listOf("2", "1"), methodCall.arguments.map { it.source })
        
        val constructCall = report.callSites[1]
        assertEquals(FoxCallSiteKind.CONSTRUCT, constructCall.kind)
        assertEquals("Result", constructCall.targetName)
        assertEquals(FoxArgumentShape.POSITIONAL, constructCall.argumentShape)
        assertEquals(listOf(null, null), constructCall.arguments.map { it.name })
        assertEquals(listOf("1", "2"), constructCall.arguments.map { it.source })
    }
    
    @Test
    fun mixedCallArgumentsProduceDiagnostic() {
        val file = parse(
            """
            def demo() {
                target(1, second = 2)
            }
            """.trimIndent(),
        )
        
        val report = file.collectSemanticPlaceholders()
        
        assertEquals(FoxArgumentShape.MIXED, report.callSites.single().argumentShape)
        assertTrue(report.diagnostics.any { "Mixed positional and named arguments" in it.message })
    }
    
    @Test
    fun normalizesExpandedMethodParameterRowsLeftToRight() {
        val normalized = assertIs<Normalized<List<NormalizedMethodParameter>>>(
            normalizeMethodParameters(
                listOf(
                    FoxNamedMethodParameterPlaceholder("a", FoxIntType),
                    FoxSplatMethodParameterPlaceholder(
                        FoxStructType(
                            linkedMapOf(
                                "x" to FoxStringType,
                                "y" to FoxBoolType,
                            ),
                        ),
                    ),
                    FoxSplatMethodParameterPlaceholder(
                        FoxStructType(
                            linkedMapOf(
                                "z" to FoxLongType,
                            ),
                        ),
                    ),
                    FoxNamedMethodParameterPlaceholder("b", FoxIntType),
                ),
            ),
        )
        assertEquals(listOf("a", "x", "y", "z", "b"), normalized.value.map { it.name })
    }
    
    @Test
    fun parameterRowExpansionRejectsDuplicatesAndUnnamedRows() {
        val duplicate = listOf(
            FoxNamedMethodParameterPlaceholder("a", FoxIntType),
            FoxSplatMethodParameterPlaceholder(
                FoxStructType(
                    linkedMapOf(
                        "a" to FoxStringType,
                    ),
                ),
            ),
        ).let(::normalizeMethodParameters)
        
        val duplicateError = assertIs<NormalizationError>(duplicate)
        assertEquals("Duplicate method parameter 'a' after expansion", duplicateError.message)
        
        val unnamed = listOf(
            FoxSplatMethodParameterPlaceholder(
                FoxNamedProjectionType(FoxTupleType(listOf(FoxStringType))),
            ),
        ).let(::normalizeMethodParameters)
        
        val unnamedError = assertIs<NormalizationError>(unnamed)
        assertTrue("does not provide stable parameter names" in unnamedError.message)
        
        val emptyExpanded = listOf(
            FoxNamedMethodParameterPlaceholder("a", FoxIntType),
            FoxSplatMethodParameterPlaceholder(FoxStructType(linkedMapOf())),
            FoxNamedMethodParameterPlaceholder("b", FoxIntType),
        ).let(::normalizeMethodParameters)
        
        val normalizedEmpty = assertIs<Normalized<List<NormalizedMethodParameter>>>(emptyExpanded)
        assertEquals(listOf("a", "b"), normalizedEmpty.value.map { it.name })
    }
    
    @Test
    fun lowersMethodPlaceholderToConcreteSignature() {
        val placeholder = FoxMethodPlaceholder(
            name = "foo",
            generics = linkedMapOf("T" to FoxStructWildcardConstraint),
            thisType = null,
            parameters = listOf(
                FoxNamedMethodParameterPlaceholder("a", FoxIntType),
                FoxSplatMethodParameterPlaceholder(
                    FoxStructType(
                        linkedMapOf(
                            "x" to FoxStringType,
                            "y" to FoxBoolType,
                        ),
                    ),
                ),
                FoxNamedMethodParameterPlaceholder("b", FoxLongType),
            ),
            normalizedParameters = normalizeMethodParameters(
                listOf(
                    FoxNamedMethodParameterPlaceholder("a", FoxIntType),
                    FoxSplatMethodParameterPlaceholder(
                        FoxStructType(
                            linkedMapOf(
                                "x" to FoxStringType,
                                "y" to FoxBoolType,
                            ),
                        ),
                    ),
                    FoxNamedMethodParameterPlaceholder("b", FoxLongType),
                ),
            ),
            returnType = FoxStringType,
            placeholders = emptyList(),
        )
        
        val lowered = assertIs<Normalized<FoxLoweredMethodSignature>>(placeholder.toMethodSignature())
        val signature = lowered.value.signature
        
        assertEquals("foo", signature.name)
        assertEquals(FoxStructWildcardConstraint, signature.generics.getValue("T"))
        assertEquals(listOf("a", "x", "y", "b"), signature.parameters.keys.toList())
        assertEquals(FoxStringType, signature.returnType)
        assertEquals(FoxUnitType, signature.thisType)
    }
    
    @Test
    fun methodSignatureLoweringFailsForUnnamedParameterRows() {
        val placeholder = FoxMethodPlaceholder(
            name = "foo",
            generics = emptyMap(),
            thisType = null,
            parameters = listOf(
                FoxSplatMethodParameterPlaceholder(
                    FoxNamedProjectionType(FoxTupleType(listOf(FoxStringType))),
                ),
            ),
            normalizedParameters = normalizeMethodParameters(
                listOf(
                    FoxSplatMethodParameterPlaceholder(
                        FoxNamedProjectionType(FoxTupleType(listOf(FoxStringType))),
                    ),
                ),
            ),
            returnType = null,
            placeholders = emptyList(),
        )
        
        val error = assertIs<NormalizationError>(placeholder.toMethodSignature())
        assertTrue("stable parameter names" in error.message)
    }
    
    @Test
    fun lowersFileDeclarationsAndCollectsMethodFailures() {
        val file = parse(
            """
            def ok(a: Int, b: Long): String {}
            def bad(*Named<Tuple<String>>) {}
            """.trimIndent(),
        )
        
        val report = file.lowerDeclarations()
        
        assertEquals(1, report.methods.size)
        assertEquals("ok", report.methods.single().signature.name)
        assertEquals(listOf("a", "b"), report.methods.single().signature.parameters.keys.toList())
        assertTrue(report.diagnostics.any { "Method 'bad' could not be lowered" in it.message })
        assertTrue(report.diagnostics.any { "stable parameter names" in it.message })
    }
    
    @Test
    fun lowersInstantiatedSplatMethodDeclaration() {
        val file = parse(
            """
            def <T = Struct<*>> foo(*T, b: Int): String {}
            """.trimIndent(),
        )
        
        val lowered = assertIs<Normalized<FoxLoweredMethodSignature>>(
            file.lowerMethodDeclaration(
                methodName = "foo",
                genericReplacements = linkedMapOf(
                    "T" to FoxStructType(
                        linkedMapOf(
                            "x" to FoxStringType,
                            "y" to FoxLongType,
                        ),
                    ),
                ),
            ),
        )
        
        val signature = lowered.value.signature
        assertEquals(listOf("x", "y", "b"), signature.parameters.keys.toList())
        assertEquals(FoxStringType, signature.parameters.getValue("x"))
        assertEquals(FoxLongType, signature.parameters.getValue("y"))
        assertEquals(FoxIntType, signature.parameters.getValue("b"))
        assertEquals(FoxStringType, signature.returnType)
    }
    
    @Test
    fun instantiatedMethodDeclarationRejectsInvalidGenericArgument() {
        val file = parse(
            """
            def <T = Struct<*>> foo(*T) {}
            """.trimIndent(),
        )
        
        val error = assertIs<NormalizationError>(
            file.lowerMethodDeclaration(
                methodName = "foo",
                genericReplacements = linkedMapOf(
                    "T" to FoxTupleType(listOf(FoxStringType)),
                ),
            ),
        )
        
        assertTrue("does not satisfy constraint" in error.message)
    }
    
    private fun parse(source: String): NodeFile {
        val run = FoxGrammar.parseWithDiagnostics(
            source = source,
            root = node<NodeFile>(),
            cursor = Cursor(0),
            starters = FoxStarters,
        )
        val file = assertIs<Success<NodeFile>>(run.result, run.stop.render())
        assertEquals(run.scanner.fragments.size, file.interval.end.fragIndex, run.stop.render())
        return file.node
    }
}
