package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.orderedMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MethodTypeNormalizationPassTest {
    
    @Test
    fun allowsWildcardTypeInGenericConstraint() {
        val method = method(generics = orderedMapOf("T" to FoxAnyType))
        val result = assertIs<MethodTypeNormalizationSuccess>(runMethodTypeNormalization(SurfaceFile(listOf(method))))
        val newMethod = assertIs<SurfaceMethodDefinition>(result.newFile.elements.single())
        
        assertEquals(FoxAnyType, newMethod.generics.getValue("T"))
    }
    
    @Test
    fun allowsTransformTypeInGenericConstraint() {
        val transform = SurfaceTupleGetComponentType(SurfaceTupleType(listOf(FoxIntType)), 0)
        val method = method(generics = orderedMapOf("T" to transform))
        val result = assertIs<MethodTypeNormalizationSuccess>(runMethodTypeNormalization(SurfaceFile(listOf(method))))
        val newMethod = assertIs<SurfaceMethodDefinition>(result.newFile.elements.single())
        
        assertEquals(transform, newMethod.generics.getValue("T"))
    }
    
    @Test
    fun allowsDeclaredGenericReferencesInMethodTypesAndStatements() {
        val generic = SurfaceUnresolvedType("T", null)
        val method = method(
            generics = orderedMapOf(
                "T" to FoxAnyType,
                "R" to generic,
            ),
            thisType = generic,
            parameters = orderedMapOf("value" to SurfaceArrayType(generic)),
            returnType = SurfaceTupleType(listOf(generic)),
            body = SurfaceBlock(
                null,
                listOf(
                    SurfaceTypeBinding("local", generic),
                    SurfaceConstruct(generic, emptyList()),
                    SurfaceCall(SurfaceUnresolvedSymbol("target"), "use", listOf(null to generic), emptyList()),
                ),
            ),
        )
        
        assertIs<MethodTypeNormalizationSuccess>(runMethodTypeNormalization(SurfaceFile(listOf(method))))
    }
    
    @Test
    fun detectsUnknownGenericReferencesInMethodTypesAndStatements() {
        val method = method(
            generics = orderedMapOf("T" to FoxAnyType),
            thisType = SurfaceUnresolvedType("MissingThis", null),
            parameters = orderedMapOf("value" to SurfaceUnresolvedType("MissingParameter", null)),
            returnType = SurfaceUnresolvedType("MissingReturn", null),
            body = SurfaceBlock(
                null,
                listOf(
                    SurfaceTypeBinding("local", SurfaceUnresolvedType("MissingLocal", null)),
                    SurfaceConstruct(SurfaceUnresolvedType("MissingConstruct", null), emptyList()),
                    SurfaceCall(
                        SurfaceUnresolvedSymbol("target"),
                        "use",
                        listOf(null to SurfaceUnresolvedType("MissingCallGeneric", null)),
                        emptyList(),
                    ),
                ),
            ),
        )
        
        val result = assertIs<MethodTypeNormalizationFailure>(runMethodTypeNormalization(SurfaceFile(listOf(method))))
        val names = result.errors.filterIsInstance<MethodTypeNormalizationUnknownGenericReference>().map { it.type.name }
        
        assertEquals(
            listOf(
                "MissingThis",
                "MissingParameter",
                "MissingReturn",
                "MissingLocal",
                "MissingConstruct",
                "MissingCallGeneric",
            ),
            names,
        )
    }
    
    @Test
    fun normalizesTransformTypeInMethodTypesAndStatements() {
        val thisTransform = SurfaceRefGetReferentTypeType(SurfaceRefType(FoxIntType))
        val parameterTransform = SurfaceArrayGetElementTypeType(SurfaceArrayType(FoxStringType))
        val returnTransform = SurfaceTupleGetComponentType(SurfaceTupleType(listOf(FoxBoolType)), 0)
        val localTransform = SurfaceStructGetFieldTypeByNameType(SurfaceStructType(orderedMapOf("value" to FoxLongType)), "value")
        val constructTransform = SurfaceMethodGetReturnTypeType(SurfaceMethodType(FoxUnitType, emptyOrderedMap(), FoxDoubleType))
        val callGenericTransform = SurfaceTupleGetLastComponentsType(SurfaceTupleType(listOf(FoxIntType, FoxFloatType)), 1)
        val method = method(
            thisType = thisTransform,
            parameters = orderedMapOf("value" to parameterTransform),
            returnType = returnTransform,
            body = SurfaceBlock(
                null,
                listOf(
                    SurfaceTypeBinding("local", localTransform),
                    SurfaceConstruct(constructTransform, emptyList()),
                    SurfaceCall(
                        SurfaceUnresolvedSymbol("target"),
                        "use",
                        listOf(null to callGenericTransform),
                        emptyList(),
                    ),
                ),
            ),
        )
        
        val result = assertIs<MethodTypeNormalizationSuccess>(runMethodTypeNormalization(SurfaceFile(listOf(method))))
        val newMethod = assertIs<SurfaceMethodDefinition>(result.newFile.elements.single())
        val block = assertIs<SurfaceBlock>(newMethod.body)
        
        assertEquals(FoxIntType, newMethod.thisType)
        assertEquals(orderedMapOf("value" to FoxStringType), newMethod.parameters)
        assertEquals(FoxBoolType, newMethod.returnType)
        assertEquals(SurfaceTypeBinding("local", FoxLongType), block.statements[0])
        assertEquals(SurfaceConstruct(FoxDoubleType, emptyList()), block.statements[1])
        assertEquals(
            SurfaceCall(
                SurfaceUnresolvedSymbol("target"),
                "use",
                listOf(null to SurfaceTupleType(listOf(FoxFloatType))),
                emptyList(),
            ),
            block.statements[2],
        )
    }
    
    @Test
    fun detectsUnnormalizedTransformTypeInMethodTypesAndStatements() {
        val refGeneric = SurfaceUnresolvedType("R", null)
        val arrayGeneric = SurfaceUnresolvedType("A", null)
        val tupleGeneric = SurfaceUnresolvedType("T", null)
        val structGeneric = SurfaceUnresolvedType("S", null)
        val methodGeneric = SurfaceUnresolvedType("M", null)
        val thisTransform = SurfaceRefGetReferentTypeType(refGeneric)
        val parameterTransform = SurfaceArrayGetElementTypeType(arrayGeneric)
        val returnTransform = SurfaceTupleGetComponentType(tupleGeneric, 0)
        val localTransform = SurfaceStructGetFieldTypeByNameType(structGeneric, "value")
        val constructTransform = SurfaceMethodGetReturnTypeType(methodGeneric)
        val callGenericTransform = SurfaceTupleGetLastComponentsType(tupleGeneric, 1)
        val method = method(
            generics = orderedMapOf(
                "R" to FoxAnyType,
                "A" to FoxAnyType,
                "T" to FoxAnyTupleType,
                "S" to FoxAnyStructType,
                "M" to FoxAnyType,
            ),
            thisType = thisTransform,
            parameters = orderedMapOf("value" to parameterTransform),
            returnType = returnTransform,
            body = SurfaceBlock(
                null,
                listOf(
                    SurfaceTypeBinding("local", localTransform),
                    SurfaceConstruct(constructTransform, emptyList()),
                    SurfaceCall(
                        SurfaceUnresolvedSymbol("target"),
                        "use",
                        listOf(null to callGenericTransform),
                        emptyList(),
                    ),
                ),
            ),
        )
        
        val result = assertIs<MethodTypeNormalizationFailure>(runMethodTypeNormalization(SurfaceFile(listOf(method))))
        val types = result.errors.filterIsInstance<MethodTypeNormalizationTransformNotAllowed>().map { it.type }
        
        assertEquals(
            listOf(
                thisTransform,
                parameterTransform,
                returnTransform,
                localTransform,
                constructTransform,
                callGenericTransform,
            ),
            types,
        )
    }
    
    @Test
    fun detectsWildcardTypeInParameter() {
        val method = method(parameters = orderedMapOf("value" to FoxAnyType))
        val result = assertIs<MethodTypeNormalizationFailure>(runMethodTypeNormalization(SurfaceFile(listOf(method))))
        val error = assertIs<MethodTypeNormalizationWildcardNotAllowed>(result.errors.single())
        
        assertEquals(FoxAnyType, error.type)
    }
    
    @Test
    fun detectsWildcardTypeInReturnType() {
        val method = method(returnType = SurfaceTupleType(listOf(FoxAnyTupleType)))
        val result = assertIs<MethodTypeNormalizationFailure>(runMethodTypeNormalization(SurfaceFile(listOf(method))))
        val error = assertIs<MethodTypeNormalizationWildcardNotAllowed>(result.errors.single())
        
        assertEquals(FoxAnyTupleType, error.type)
    }
    
    @Test
    fun detectsWildcardTypeInStatementType() {
        val method = method(body = SurfaceBlock(null, listOf(SurfaceTypeBinding("value", SurfaceAnyOfType(listOf(FoxIntType))))))
        val result = assertIs<MethodTypeNormalizationFailure>(runMethodTypeNormalization(SurfaceFile(listOf(method))))
        val error = assertIs<MethodTypeNormalizationWildcardNotAllowed>(result.errors.single())
        
        assertEquals(SurfaceAnyOfType(listOf(FoxIntType)), error.type)
    }
    
    private fun method(
        generics: OrderedMap<String, SurfaceType> = emptyOrderedMap(),
        thisType: SurfaceType = FoxUnitType,
        parameters: OrderedMap<String, SurfaceType> = emptyOrderedMap(),
        returnType: SurfaceType = FoxUnitType,
        body: SurfaceStatement = SurfaceBlock(null, emptyList()),
    ) = SurfaceMethodDefinition(
        generics = generics,
        thisType = thisType,
        name = "test",
        parameters = parameters,
        returnType = returnType,
        body = body,
    )
}

private val FoxUnitType = SurfacePrimitiveType(PrimitiveTypeEnum.Unit)
private val FoxBoolType = SurfacePrimitiveType(PrimitiveTypeEnum.Bool)
private val FoxIntType = SurfacePrimitiveType(PrimitiveTypeEnum.Int)
private val FoxLongType = SurfacePrimitiveType(PrimitiveTypeEnum.Long)
private val FoxFloatType = SurfacePrimitiveType(PrimitiveTypeEnum.Float)
private val FoxDoubleType = SurfacePrimitiveType(PrimitiveTypeEnum.Double)
private val FoxStringType = SurfacePrimitiveType(PrimitiveTypeEnum.String)
private val FoxAnyType = SurfaceAnyType()
private val FoxAnyTupleType = SurfaceAnyTupleType()
private val FoxAnyStructType = SurfaceAnyStructType()
