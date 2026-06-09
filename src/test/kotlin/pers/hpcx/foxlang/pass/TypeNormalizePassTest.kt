package pers.hpcx.foxlang.pass

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.orderedMapOf
import pers.hpcx.foxlang.utils.orderedSetOf
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeNormalizePassTest {
    @Test
    fun componentAtAndFieldAtSupportPartialEvaluation() {
        assertEquals(
            FoxIntType,
            TypeNormalizePass.normalize(
                FoxTupleComponentAtType(
                    FoxTupleMergeComponentsOfType(listOf(listOf(FoxIntType).toFoxTupleType(), FoxAnyTupleType)),
                    0,
                ),
            ),
        )
        assertEquals(
            FoxTupleComponentAtType(
                FoxTupleMergeComponentsOfType(listOf(listOf(FoxIntType).toFoxTupleType(), FoxAnyTupleType)),
                1,
            ),
            TypeNormalizePass.normalize(
                FoxTupleComponentAtType(
                    FoxTupleMergeComponentsOfType(listOf(listOf(FoxIntType).toFoxTupleType(), FoxAnyTupleType)),
                    1,
                ),
            ),
        )
        assertEquals(
            FoxTupleComponentAtType(listOf(FoxIntType).toFoxTupleType(), 1),
            TypeNormalizePass.normalize(FoxTupleComponentAtType(listOf(FoxIntType).toFoxTupleType(), 1)),
        )
        assertEquals(
            FoxStringType,
            TypeNormalizePass.normalize(
                FoxTupleLastComponentAtType(
                    FoxTupleMergeComponentsOfType(listOf(FoxAnyTupleType, listOf(FoxIntType, FoxStringType).toFoxTupleType())),
                    0,
                ),
            ),
        )
        assertEquals(
            FoxIntType,
            TypeNormalizePass.normalize(
                FoxTupleLastComponentAtType(
                    FoxTupleMergeComponentsOfType(listOf(FoxAnyTupleType, listOf(FoxIntType, FoxStringType).toFoxTupleType())),
                    1,
                ),
            ),
        )
        assertEquals(
            FoxTupleLastComponentAtType(
                FoxTupleMergeComponentsOfType(listOf(FoxAnyTupleType, listOf(FoxIntType, FoxStringType).toFoxTupleType())),
                2,
            ),
            TypeNormalizePass.normalize(
                FoxTupleLastComponentAtType(
                    FoxTupleMergeComponentsOfType(listOf(FoxAnyTupleType, listOf(FoxIntType, FoxStringType).toFoxTupleType())),
                    2,
                ),
            ),
        )
        assertEquals(
            FoxTupleLastComponentAtType(listOf(FoxIntType).toFoxTupleType(), 1),
            TypeNormalizePass.normalize(FoxTupleLastComponentAtType(listOf(FoxIntType).toFoxTupleType(), 1)),
        )
        
        assertEquals(
            FoxIntType,
            TypeNormalizePass.normalize(
                FoxStructFieldAtType(
                    FoxStructMergeFieldsOfType(
                        listOf(FoxStructType(orderedMapOf("id" to FoxIntType)), FoxAnyStructType),
                    ),
                    0,
                ),
            ),
        )
        assertEquals(
            FoxStructFieldAtType(
                FoxStructMergeFieldsOfType(
                    listOf(FoxStructType(orderedMapOf("id" to FoxIntType)), FoxAnyStructType),
                ),
                1,
            ),
            TypeNormalizePass.normalize(
                FoxStructFieldAtType(
                    FoxStructMergeFieldsOfType(
                        listOf(FoxStructType(orderedMapOf("id" to FoxIntType)), FoxAnyStructType),
                    ),
                    1,
                ),
            ),
        )
        assertEquals(
            FoxStructFieldAtType(FoxStructType(orderedMapOf("id" to FoxIntType)), 1),
            TypeNormalizePass.normalize(FoxStructFieldAtType(FoxStructType(orderedMapOf("id" to FoxIntType)), 1)),
        )
        assertEquals(
            FoxStringType,
            TypeNormalizePass.normalize(
                FoxStructLastFieldAtType(
                    FoxStructMergeFieldsOfType(
                        listOf(FoxAnyStructType, FoxStructType(orderedMapOf("id" to FoxIntType, "name" to FoxStringType))),
                    ),
                    0,
                ),
            ),
        )
        assertEquals(
            FoxIntType,
            TypeNormalizePass.normalize(
                FoxStructLastFieldAtType(
                    FoxStructMergeFieldsOfType(
                        listOf(FoxAnyStructType, FoxStructType(orderedMapOf("id" to FoxIntType, "name" to FoxStringType))),
                    ),
                    1,
                ),
            ),
        )
        assertEquals(
            FoxStructLastFieldAtType(
                FoxStructMergeFieldsOfType(
                    listOf(FoxAnyStructType, FoxStructType(orderedMapOf("id" to FoxIntType, "name" to FoxStringType))),
                ),
                2,
            ),
            TypeNormalizePass.normalize(
                FoxStructLastFieldAtType(
                    FoxStructMergeFieldsOfType(
                        listOf(FoxAnyStructType, FoxStructType(orderedMapOf("id" to FoxIntType, "name" to FoxStringType))),
                    ),
                    2,
                ),
            ),
        )
        assertEquals(
            FoxStructLastFieldAtType(FoxStructType(orderedMapOf("id" to FoxIntType)), 1),
            TypeNormalizePass.normalize(FoxStructLastFieldAtType(FoxStructType(orderedMapOf("id" to FoxIntType)), 1)),
        )
    }
    
    @Test
    fun firstLastAndDropSupportDeterministicPartialEvaluation() {
        val openTuple = FoxTupleMergeComponentsOfType(
            listOf(listOf(FoxIntType, FoxStringType).toFoxTupleType(), FoxAnyTupleType),
        )
        assertEquals(
            listOf(FoxIntType).toFoxTupleType(),
            TypeNormalizePass.normalize(FoxTupleFirstComponentsOfType(openTuple, 1)),
        )
        assertEquals(
            FoxTupleFirstComponentsOfType(openTuple, 3),
            TypeNormalizePass.normalize(FoxTupleFirstComponentsOfType(openTuple, 3)),
        )
        assertEquals(
            FoxTupleMergeComponentsOfType(listOf(listOf(FoxStringType).toFoxTupleType(), FoxAnyTupleType)),
            TypeNormalizePass.normalize(FoxTupleDropFirstComponentsOfType(openTuple, 1)),
        )
        assertEquals(FoxAnyTupleType, TypeNormalizePass.normalize(FoxTupleDropFirstComponentsOfType(openTuple, 2)))
        
        val openTupleSuffix = FoxTupleMergeComponentsOfType(
            listOf(FoxAnyTupleType, listOf(FoxIntType, FoxStringType).toFoxTupleType()),
        )
        assertEquals(
            listOf(FoxStringType).toFoxTupleType(),
            TypeNormalizePass.normalize(FoxTupleLastComponentsOfType(openTupleSuffix, 1)),
        )
        assertEquals(
            FoxTupleMergeComponentsOfType(listOf(FoxAnyTupleType, listOf(FoxIntType).toFoxTupleType())),
            TypeNormalizePass.normalize(FoxTupleDropLastComponentsOfType(openTupleSuffix, 1)),
        )
        
        val openStruct = FoxStructMergeFieldsOfType(
            listOf(FoxStructType(orderedMapOf("id" to FoxIntType, "name" to FoxStringType)), FoxAnyStructType),
        )
        assertEquals(
            FoxStructType(orderedMapOf("id" to FoxIntType)),
            TypeNormalizePass.normalize(FoxStructFirstFieldsOfType(openStruct, 1)),
        )
        assertEquals(
            FoxStructMergeFieldsOfType(listOf(FoxStructType(orderedMapOf("name" to FoxStringType)), FoxAnyStructType)),
            TypeNormalizePass.normalize(FoxStructDropFirstFieldsOfType(openStruct, 1)),
        )
        
        val openStructSuffix = FoxStructMergeFieldsOfType(
            listOf(FoxAnyStructType, FoxStructType(orderedMapOf("id" to FoxIntType, "name" to FoxStringType))),
        )
        assertEquals(
            FoxStructType(orderedMapOf("name" to FoxStringType)),
            TypeNormalizePass.normalize(FoxStructLastFieldsOfType(openStructSuffix, 1)),
        )
        assertEquals(
            FoxStructMergeFieldsOfType(listOf(FoxAnyStructType, FoxStructType(orderedMapOf("id" to FoxIntType)))),
            TypeNormalizePass.normalize(FoxStructDropLastFieldsOfType(openStructSuffix, 1)),
        )
    }
    
    @Test
    fun namedProjectionAndDropSupportPartialEvaluation() {
        val openStruct = FoxStructMergeFieldsOfType(
            listOf(
                FoxStructType(orderedMapOf("id" to FoxIntType, "name" to FoxStringType)),
                FoxAnyStructType,
            ),
        )
        assertEquals(FoxIntType, TypeNormalizePass.normalize(FoxStructFieldOfType(openStruct, "id")))
        assertEquals(
            FoxStructType(orderedMapOf("name" to FoxStringType)),
            TypeNormalizePass.normalize(FoxStructFieldsOfType(openStruct, orderedSetOf("name"))),
        )
        assertEquals(
            FoxStructMergeFieldsOfType(listOf(FoxStructType(orderedMapOf("name" to FoxStringType)), FoxAnyStructType)),
            TypeNormalizePass.normalize(FoxStructDropFieldsOfType(openStruct, setOf("id"))),
        )
        assertEquals(
            FoxStructFieldOfType(FoxAnyStructType, "missing"),
            TypeNormalizePass.normalize(FoxStructFieldOfType(FoxAnyStructType, "missing")),
        )
        
        val openObject = FoxObjectMergeMembersOfType(
            listOf(
                FoxObjectType(mapOf("name" to FoxStringType, "id" to FoxIntType)),
                FoxAnyObjectType,
            ),
        )
        assertEquals(FoxStringType, TypeNormalizePass.normalize(FoxObjectMemberOfType(openObject, "name")))
        assertEquals(
            FoxObjectType(mapOf("name" to FoxStringType)),
            TypeNormalizePass.normalize(FoxObjectMembersOfType(openObject, setOf("name"))),
        )
        assertEquals(
            FoxObjectMergeMembersOfType(listOf(FoxObjectType(mapOf("name" to FoxStringType)), FoxAnyObjectType)),
            TypeNormalizePass.normalize(FoxObjectDropMembersOfType(openObject, setOf("id"))),
        )
        assertEquals(
            FoxObjectMemberOfType(FoxAnyObjectType, "missing"),
            TypeNormalizePass.normalize(FoxObjectMemberOfType(FoxAnyObjectType, "missing")),
        )
        
        val openEnum = FoxEnumMergeItemsOfType(
            listOf(
                FoxEnumType(mapOf("Success" to FoxIntType, "Failure" to FoxStringType)),
                FoxAnyEnumType,
            ),
        )
        assertEquals(FoxIntType, TypeNormalizePass.normalize(FoxEnumItemOfType(openEnum, "Success")))
        assertEquals(
            FoxEnumType(mapOf("Failure" to FoxStringType)),
            TypeNormalizePass.normalize(FoxEnumItemsOfType(openEnum, listOf("Failure"))),
        )
        assertEquals(
            FoxEnumMergeItemsOfType(listOf(FoxEnumType(mapOf("Success" to FoxIntType)), FoxAnyEnumType)),
            TypeNormalizePass.normalize(FoxEnumDropItemsOfType(openEnum, listOf("Failure"))),
        )
        assertEquals(
            FoxEnumItemOfType(FoxAnyEnumType, "missing"),
            TypeNormalizePass.normalize(FoxEnumItemOfType(FoxAnyEnumType, "missing")),
        )
    }
    
    @Test
    fun emptyMergeNormalizesToEmptyType() {
        assertEquals(emptyFoxTupleType(), TypeNormalizePass.normalize(FoxTupleMergeComponentsOfType(emptyList())))
        assertEquals(emptyFoxStructType(), TypeNormalizePass.normalize(FoxStructMergeFieldsOfType(emptyList())))
        assertEquals(emptyFoxObjectType(), TypeNormalizePass.normalize(FoxObjectMergeMembersOfType(emptyList())))
        assertEquals(emptyFoxEnumType(), TypeNormalizePass.normalize(FoxEnumMergeItemsOfType(emptyList())))
    }
}
