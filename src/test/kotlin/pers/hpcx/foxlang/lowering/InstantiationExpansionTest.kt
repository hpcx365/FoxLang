package pers.hpcx.foxlang.lowering

import pers.hpcx.foxlang.frontend.*
import pers.hpcx.foxlang.types.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InstantiationExpansionTest {
    
    @Test
    fun expandsSplatParametersAndGenForBody() {
        val method = NodeMethodDefinition(
            generics = linkedMapOf("T" to NodeGenericConstraint(NodeStructWildcardType())),
            thisType = null,
            name = "foo",
            parameters = listOf(
                NodeSplatFormalParameter(NodeNamedType("T", emptyList())),
                NodeNamedFormalParameter("b", NodePrimitiveType(FoxIntType)),
            ),
            returnType = NodePrimitiveType(FoxUnitType),
            body = NodeBlock(
                null,
                listOf(
                    NodeGenFor(
                        valueName = "f",
                        typeName = "F",
                        targetType = NodeNamedType("T", emptyList()),
                        body = NodeCall(
                            target = null,
                            name = "print",
                            generics = listOf(null to NodeNamedType("F", emptyList())),
                            parameters = listOf(null to NodeSymbol("f")),
                        ),
                    ),
                ),
            ),
        )
        
        val expanded = assertIs<Normalized<ExpandedMethodDefinition>>(
            method.expandInstantiation(
                linkedMapOf(
                    "T" to FoxStructType(
                        linkedMapOf(
                            "x" to FoxStringType,
                            "y" to FoxLongType,
                        ),
                    ),
                ),
            ),
        ).value.definition
        
        assertEquals(listOf("x", "y", "b"), expanded.parameters.map { (it as NodeNamedFormalParameter).name })
        
        val body = assertIs<NodeBlock>(expanded.body)
        assertEquals(2, body.statements.size)
        
        val firstCall = assertIs<NodeCall>(body.statements[0])
        assertEquals("print", firstCall.name)
        assertEquals("x", assertIs<NodeSymbol>(firstCall.parameters.single().second).name)
        assertEquals("String", firstCall.generics!!.single().second.toSource())
        
        val secondCall = assertIs<NodeCall>(body.statements[1])
        assertEquals("y", assertIs<NodeSymbol>(secondCall.parameters.single().second).name)
        assertEquals("Long", secondCall.generics!!.single().second.toSource())
    }
    
    @Test
    fun chainsExpansionAndSignatureLowering() {
        val file = NodeFile(
            listOf(
                NodeMethodDefinition(
                    generics = linkedMapOf("T" to NodeGenericConstraint(NodeStructWildcardType())),
                    thisType = null,
                    name = "foo",
                    parameters = listOf(
                        NodeNamedFormalParameter("a", NodePrimitiveType(FoxIntType)),
                        NodeSplatFormalParameter(NodeNamedType("T", emptyList())),
                    ),
                    returnType = NodePrimitiveType(FoxStringType),
                    body = NodeBlock(
                        null,
                        listOf(
                            NodeGenFor(
                                valueName = "f",
                                typeName = "F",
                                targetType = NodeNamedType("T", emptyList()),
                                body = NodeCall(
                                    target = null,
                                    name = "print",
                                    generics = listOf(null to NodeNamedType("F", emptyList())),
                                    parameters = listOf(null to NodeSymbol("f")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        
        val lowered = assertIs<Normalized<InstantiatedMethodLowering>>(
            file.instantiateMethodDeclaration(
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
        ).value
        
        assertEquals(listOf("a", "x", "y"), lowered.definition.parameters.map { (it as NodeNamedFormalParameter).name })
        assertEquals(listOf("a", "x", "y"), lowered.signature.parameters.keys.toList())
        assertEquals(FoxStringType, lowered.signature.returnType)
        assertEquals(2, assertIs<NodeBlock>(lowered.definition.body).statements.size)
    }
}

