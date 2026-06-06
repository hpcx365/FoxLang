package pers.hpcx.foxlang.types

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TypesTest {
    
    @Test
    fun denamedStructResolvesToTuple() {
        val projected = FoxDenamedProjectionType(
            FoxStructType(
                linkedMapOf(
                    "name" to FoxStringType,
                    "age" to FoxIntType,
                ),
            ),
        )
        
        val resolved = projected.resolveStructuralProjections()
        
        assertEquals(
            FoxTupleType(listOf(FoxStringType, FoxIntType)),
            resolved,
        )
    }
    
    @Test
    fun namedTupleNeedsAllocatorToResolveToStruct() {
        val projected = FoxNamedProjectionType(
            FoxTupleType(listOf(FoxStringType, FoxIntType)),
        )
        
        assertEquals(projected, projected.resolveStructuralProjections())
        
        val resolved = projected.resolveStructuralProjections { index -> "__fresh_field_${index + 7}" }
        
        assertIs<FoxStructType>(resolved)
        assertEquals(
            linkedMapOf(
                "__fresh_field_7" to FoxStringType,
                "__fresh_field_8" to FoxIntType,
            ),
            resolved.fields,
        )
    }
    
    @Test
    fun templateSpreadTypesPreserveGenericStructure() {
        val template = FoxTupleTemplateType(
            listOf(
                FoxTupleTypeTemplateItem(FoxIntType),
                FoxTupleSpreadTemplateItem(FoxGenericType("T")),
            ),
        )
        
        assertEquals(setOf("T"), template.collectGenerics())
        
        val replaced = template.replaceGenerics(mapOf("T" to FoxTupleType(listOf(FoxStringType)))) as FoxTupleTemplateType
        
        assertIs<FoxTupleSpreadTemplateItem>(replaced.items[1])
        assertEquals(FoxTupleType(listOf(FoxStringType)), (replaced.items[1] as FoxTupleSpreadTemplateItem).type)
        
        val structTemplate = FoxStructTemplateType(
            listOf(
                FoxStructFieldTemplateItem("name", FoxStringType),
                FoxStructSpreadTemplateItem(FoxGenericType("S")),
            ),
        )
        
        assertTrue("S" in structTemplate.collectGenerics())
    }
    
    @Test
    fun normalizeTupleTemplateSpreadsTupleRows() {
        val template = FoxTupleTemplateType(
            listOf(
                FoxTupleTypeTemplateItem(FoxIntType),
                FoxTupleSpreadTemplateItem(FoxTupleType(listOf(FoxStringType, FoxBoolType))),
                FoxTupleTypeTemplateItem(FoxLongType),
            ),
        )
        
        val normalized = assertIs<Normalized<FoxType>>(template.normalizeType())
        
        assertEquals(
            FoxTupleType(listOf(FoxIntType, FoxStringType, FoxBoolType, FoxLongType)),
            normalized.value,
        )
    }
    
    @Test
    fun normalizeStructTemplateSpreadsStructRowsAndRejectsDuplicates() {
        val okTemplate = FoxStructTemplateType(
            listOf(
                FoxStructFieldTemplateItem("name", FoxStringType),
                FoxStructSpreadTemplateItem(
                    FoxStructType(
                        linkedMapOf(
                            "city" to FoxStringType,
                            "zip" to FoxIntType,
                        ),
                    ),
                ),
            ),
        )
        
        val normalized = assertIs<Normalized<FoxType>>(okTemplate.normalizeType())
        assertEquals(
            FoxStructType(
                linkedMapOf(
                    "name" to FoxStringType,
                    "city" to FoxStringType,
                    "zip" to FoxIntType,
                ),
            ),
            normalized.value,
        )
        
        val badTemplate = FoxStructTemplateType(
            listOf(
                FoxStructFieldTemplateItem("name", FoxStringType),
                FoxStructSpreadTemplateItem(
                    FoxStructType(
                        linkedMapOf(
                            "name" to FoxIntType,
                        ),
                    ),
                ),
            ),
        )
        
        val error = assertIs<NormalizationError>(badTemplate.normalizeType())
        assertEquals("Duplicate struct field 'name' after expansion", error.message)
    }
    
    @Test
    fun namedProjectionRequiresAllocatorDuringNormalization() {
        val projected = FoxNamedProjectionType(
            FoxTupleType(listOf(FoxStringType, FoxIntType)),
        )
        
        val error = assertIs<NormalizationError>(projected.normalizeType())
        assertTrue("fresh field-name allocator" in error.message)
        
        val normalized = assertIs<Normalized<FoxType>>(
            projected.normalizeType { index -> "field$index" },
        )
        assertEquals(
            FoxStructType(
                linkedMapOf(
                    "field0" to FoxStringType,
                    "field1" to FoxIntType,
                ),
            ),
            normalized.value,
        )
    }
}
