package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ast.*
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
        val result = assertIs<MethodTypeNormalizationSuccess>(runMethodTypeNormalization(FoxFile(listOf(method))))
        val newMethod = assertIs<FoxMethodDefinition>(result.newFile.elements.single())
        
        assertEquals(FoxAnyType, newMethod.generics.getValue("T"))
    }
    
    @Test
    fun allowsTransformTypeInGenericConstraint() {
        val transform = FoxTupleGetComponentType(FoxTupleType(listOf(FoxIntType)), 0)
        val method = method(generics = orderedMapOf("T" to transform))
        val result = assertIs<MethodTypeNormalizationSuccess>(runMethodTypeNormalization(FoxFile(listOf(method))))
        val newMethod = assertIs<FoxMethodDefinition>(result.newFile.elements.single())
        
        assertEquals(transform, newMethod.generics.getValue("T"))
    }
    
    @Test
    fun allowsDeclaredGenericReferencesInMethodTypesAndStatements() {
        val generic = FoxUnresolvedType("T", null)
        val method = method(
            generics = orderedMapOf(
                "T" to FoxAnyType,
                "R" to generic,
            ),
            thisType = generic,
            parameters = orderedMapOf("value" to FoxArrayType(generic)),
            returnType = FoxTupleType(listOf(generic)),
            body = FoxBlock(
                null,
                listOf(
                    FoxTypeBinding("local", generic),
                    FoxConstruct(generic, emptyList()),
                    FoxCall(FoxUnresolvedSymbol("target"), "use", listOf(null to generic), emptyList()),
                ),
            ),
        )
        
        assertIs<MethodTypeNormalizationSuccess>(runMethodTypeNormalization(FoxFile(listOf(method))))
    }
    
    @Test
    fun detectsUnknownGenericReferencesInMethodTypesAndStatements() {
        val method = method(
            generics = orderedMapOf("T" to FoxAnyType),
            thisType = FoxUnresolvedType("MissingThis", null),
            parameters = orderedMapOf("value" to FoxUnresolvedType("MissingParameter", null)),
            returnType = FoxUnresolvedType("MissingReturn", null),
            body = FoxBlock(
                null,
                listOf(
                    FoxTypeBinding("local", FoxUnresolvedType("MissingLocal", null)),
                    FoxConstruct(FoxUnresolvedType("MissingConstruct", null), emptyList()),
                    FoxCall(
                        FoxUnresolvedSymbol("target"),
                        "use",
                        listOf(null to FoxUnresolvedType("MissingCallGeneric", null)),
                        emptyList(),
                    ),
                ),
            ),
        )
        
        val result = assertIs<MethodTypeNormalizationFailure>(runMethodTypeNormalization(FoxFile(listOf(method))))
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
        val thisTransform = FoxRefGetReferentTypeType(FoxRefType(FoxIntType))
        val parameterTransform = FoxArrayGetElementTypeType(FoxArrayType(FoxStringType))
        val returnTransform = FoxTupleGetComponentType(FoxTupleType(listOf(FoxBoolType)), 0)
        val localTransform = FoxStructGetFieldTypeByNameType(FoxStructType(orderedMapOf("value" to FoxLongType)), "value")
        val constructTransform = FoxMethodGetReturnTypeType(FoxMethodType(FoxUnitType, emptyOrderedMap(), FoxDoubleType))
        val callGenericTransform = FoxTupleGetLastComponentsType(FoxTupleType(listOf(FoxIntType, FoxFloatType)), 1)
        val method = method(
            thisType = thisTransform,
            parameters = orderedMapOf("value" to parameterTransform),
            returnType = returnTransform,
            body = FoxBlock(
                null,
                listOf(
                    FoxTypeBinding("local", localTransform),
                    FoxConstruct(constructTransform, emptyList()),
                    FoxCall(
                        FoxUnresolvedSymbol("target"),
                        "use",
                        listOf(null to callGenericTransform),
                        emptyList(),
                    ),
                ),
            ),
        )
        
        val result = assertIs<MethodTypeNormalizationSuccess>(runMethodTypeNormalization(FoxFile(listOf(method))))
        val newMethod = assertIs<FoxMethodDefinition>(result.newFile.elements.single())
        val block = assertIs<FoxBlock>(newMethod.body)
        
        assertEquals(FoxIntType, newMethod.thisType)
        assertEquals(orderedMapOf("value" to FoxStringType), newMethod.parameters)
        assertEquals(FoxBoolType, newMethod.returnType)
        assertEquals(FoxTypeBinding("local", FoxLongType), block.statements[0])
        assertEquals(FoxConstruct(FoxDoubleType, emptyList()), block.statements[1])
        assertEquals(
            FoxCall(
                FoxUnresolvedSymbol("target"),
                "use",
                listOf(null to FoxTupleType(listOf(FoxFloatType))),
                emptyList(),
            ),
            block.statements[2],
        )
    }
    
    @Test
    fun detectsUnnormalizedTransformTypeInMethodTypesAndStatements() {
        val refGeneric = FoxUnresolvedType("R", null)
        val arrayGeneric = FoxUnresolvedType("A", null)
        val tupleGeneric = FoxUnresolvedType("T", null)
        val structGeneric = FoxUnresolvedType("S", null)
        val methodGeneric = FoxUnresolvedType("M", null)
        val thisTransform = FoxRefGetReferentTypeType(refGeneric)
        val parameterTransform = FoxArrayGetElementTypeType(arrayGeneric)
        val returnTransform = FoxTupleGetComponentType(tupleGeneric, 0)
        val localTransform = FoxStructGetFieldTypeByNameType(structGeneric, "value")
        val constructTransform = FoxMethodGetReturnTypeType(methodGeneric)
        val callGenericTransform = FoxTupleGetLastComponentsType(tupleGeneric, 1)
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
            body = FoxBlock(
                null,
                listOf(
                    FoxTypeBinding("local", localTransform),
                    FoxConstruct(constructTransform, emptyList()),
                    FoxCall(
                        FoxUnresolvedSymbol("target"),
                        "use",
                        listOf(null to callGenericTransform),
                        emptyList(),
                    ),
                ),
            ),
        )
        
        val result = assertIs<MethodTypeNormalizationFailure>(runMethodTypeNormalization(FoxFile(listOf(method))))
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
        val result = assertIs<MethodTypeNormalizationFailure>(runMethodTypeNormalization(FoxFile(listOf(method))))
        val error = assertIs<MethodTypeNormalizationWildcardNotAllowed>(result.errors.single())
        
        assertEquals(FoxAnyType, error.type)
    }
    
    @Test
    fun detectsWildcardTypeInReturnType() {
        val method = method(returnType = FoxTupleType(listOf(FoxAnyTupleType)))
        val result = assertIs<MethodTypeNormalizationFailure>(runMethodTypeNormalization(FoxFile(listOf(method))))
        val error = assertIs<MethodTypeNormalizationWildcardNotAllowed>(result.errors.single())
        
        assertEquals(FoxAnyTupleType, error.type)
    }
    
    @Test
    fun detectsWildcardTypeInStatementType() {
        val method = method(body = FoxBlock(null, listOf(FoxTypeBinding("value", FoxAnyOfType(listOf(FoxIntType))))))
        val result = assertIs<MethodTypeNormalizationFailure>(runMethodTypeNormalization(FoxFile(listOf(method))))
        val error = assertIs<MethodTypeNormalizationWildcardNotAllowed>(result.errors.single())
        
        assertEquals(FoxAnyOfType(listOf(FoxIntType)), error.type)
    }
    
    private fun method(
        generics: OrderedMap<String, FoxType> = emptyOrderedMap(),
        thisType: FoxType = FoxUnitType,
        parameters: OrderedMap<String, FoxType> = emptyOrderedMap(),
        returnType: FoxType = FoxUnitType,
        body: FoxStatement = FoxBlock(null, emptyList()),
    ) = FoxMethodDefinition(
        generics = generics,
        thisType = thisType,
        name = "test",
        parameters = parameters,
        returnType = returnType,
        body = body,
    )
}
