package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.orderedMapOf
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypeCheckPassTest {
    @Test
    fun tupleConstraintPatternsMatchExactAndOpenShapes() {
        val actual = listOf(FoxIntType, FoxStringType, FoxDoubleType, FoxBoolType).toFoxTupleType()
        
        assertTrue(TypeCheckPass.matches(actual, listOf(FoxIntType, FoxAnyType, FoxDoubleType, FoxAnyType).toFoxTupleType()))
        assertTrue(
            TypeCheckPass.matches(
                actual,
                FoxTupleMergeComponentsOfType(listOf(FoxAnyTupleType, listOf(FoxDoubleType, FoxBoolType).toFoxTupleType())),
            ),
        )
        assertTrue(
            TypeCheckPass.matches(
                actual,
                FoxTupleMergeComponentsOfType(
                    listOf(
                        listOf(FoxIntType).toFoxTupleType(),
                        FoxAnyTupleType,
                        listOf(FoxBoolType).toFoxTupleType(),
                    ),
                ),
            ),
        )
        assertFalse(
            TypeCheckPass.matches(
                actual,
                FoxTupleMergeComponentsOfType(listOf(listOf(FoxStringType).toFoxTupleType(), FoxAnyTupleType)),
            ),
        )
    }
    
    @Test
    fun structConstraintPatternsMatchOrderedSegments() {
        val actual = FoxStructType(
            orderedMapOf(
                "id" to FoxIntType,
                "name" to FoxStringType,
                "active" to FoxBoolType,
            ),
        )
        
        assertTrue(
            TypeCheckPass.matches(
                actual,
                FoxStructType(
                    orderedMapOf(
                        "id" to FoxIntType,
                        "name" to FoxAnyType,
                        "active" to FoxBoolType,
                    ),
                ),
            ),
        )
        assertTrue(
            TypeCheckPass.matches(
                actual,
                FoxStructMergeFieldsOfType(
                    listOf(
                        FoxStructType(orderedMapOf("id" to FoxIntType, "name" to FoxStringType)),
                        FoxAnyStructType,
                    ),
                ),
            ),
        )
        assertTrue(
            TypeCheckPass.matches(
                actual,
                FoxStructMergeFieldsOfType(
                    listOf(
                        FoxAnyStructType,
                        FoxStructType(orderedMapOf("active" to FoxBoolType)),
                    ),
                ),
            ),
        )
        assertFalse(
            TypeCheckPass.matches(
                actual,
                FoxStructMergeFieldsOfType(
                    listOf(
                        FoxStructType(orderedMapOf("name" to FoxStringType)),
                        FoxAnyStructType,
                    ),
                ),
            ),
        )
    }
    
    @Test
    fun objectAndEnumConstraintPatternsRequireNamedParts() {
        val actualObject = FoxObjectType(
            mapOf(
                "name" to FoxStringType,
                "active" to FoxBoolType,
                "age" to FoxIntType,
            ),
        )
        val actualEnum = FoxEnumType(
            mapOf(
                "Success" to FoxIntType,
                "Failure" to FoxStringType,
            ),
        )
        
        assertTrue(
            TypeCheckPass.matches(
                actualObject,
                FoxObjectMergeMembersOfType(
                    listOf(
                        FoxObjectType(mapOf("name" to FoxAnyType)),
                        FoxAnyObjectType,
                        FoxObjectType(mapOf("name" to FoxStringType)),
                    ),
                ),
            ),
        )
        assertFalse(
            TypeCheckPass.matches(
                actualObject,
                FoxObjectMergeMembersOfType(listOf(FoxObjectType(mapOf("missing" to FoxStringType)), FoxAnyObjectType)),
            ),
        )
        assertTrue(
            TypeCheckPass.matches(
                actualEnum,
                FoxEnumMergeItemsOfType(
                    listOf(
                        FoxEnumType(mapOf("Success" to FoxAnyType)),
                        FoxAnyEnumType,
                    ),
                ),
            ),
        )
        assertFalse(TypeCheckPass.matches(actualEnum, FoxEnumType(mapOf("Success" to FoxIntType))))
    }
    
    @Test
    fun customizedTypesMustBeNormalizedBeforeConstraintMatching() {
        assertFailsWith<IllegalArgumentException> {
            TypeCheckPass.matches(FoxCustomizedType("Alias", emptyList()), FoxAnyType)
        }
        assertFailsWith<IllegalArgumentException> {
            TypeCheckPass.matches(FoxIntType, FoxCustomizedType("Alias", emptyList()))
        }
    }
    
    @Test
    fun lowAndMediumTransformsNormalizeBeforeMatching() {
        assertTrue(TypeCheckPass.matches(FoxIntType, FoxArrayElementOfType(FoxArrayType(FoxIntType))))
        assertTrue(TypeCheckPass.matches(FoxStringType, FoxRefReferentOfType(FoxRefType(FoxStringType))))
        assertTrue(
            TypeCheckPass.matches(
                listOf(FoxIntType, FoxStringType).toFoxTupleType(),
                FoxMethodParametersOfType(FoxMethodType(FoxBoolType, listOf(FoxIntType, FoxStringType), FoxUnitType)),
            ),
        )
        assertTrue(
            TypeCheckPass.matches(
                FoxStructType(orderedMapOf("name" to FoxStringType)),
                FoxStructFieldsOfType(
                    FoxStructType(orderedMapOf("name" to FoxStringType, "age" to FoxIntType)),
                    pers.hpcx.foxlang.utils.orderedSetOf("name"),
                ),
            ),
        )
        assertTrue(
            TypeCheckPass.matches(
                FoxObjectType(mapOf("name" to FoxStringType)),
                FoxObjectMembersOfType(
                    FoxObjectType(mapOf("name" to FoxStringType, "age" to FoxIntType)),
                    setOf("name"),
                ),
            ),
        )
        assertTrue(
            TypeCheckPass.matches(
                FoxEnumType(mapOf("Success" to FoxIntType)),
                FoxEnumItemsOfType(
                    FoxEnumType(mapOf("Success" to FoxIntType, "Failure" to FoxStringType)),
                    listOf("Success"),
                ),
            ),
        )
    }
}
