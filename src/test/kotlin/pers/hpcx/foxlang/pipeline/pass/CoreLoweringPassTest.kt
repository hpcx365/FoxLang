package pers.hpcx.foxlang.pipeline.pass

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.runtime.FoxString
import pers.hpcx.foxlang.runtime.FoxUnit
import pers.hpcx.foxlang.utils.emptyOrderedMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CoreLoweringPassTest {
    
    @Test
    fun lowersFieldAccessAndSetterCallsAndCollectsObjectAccessors() {
        val objectType = SurfaceObjectType(mapOf("name" to FoxStringType))
        val program = lowerProgram(
            SurfaceBlock(
                null,
                listOf(
                    SurfaceAssign(
                        SurfaceUnresolvedSymbol("x"),
                        assign(AssignOperatorEnum.Def),
                        SurfaceConstruct(objectType, emptyList()),
                        beforeEvaluation = true,
                    ),
                    SurfaceAssign(
                        SurfaceFieldAccess(SurfaceUnresolvedSymbol("x"), "name"),
                        assign(AssignOperatorEnum.Plain),
                        SurfaceEntityStatement(FoxString("Bob")),
                        beforeEvaluation = true,
                    ),
                    SurfaceCall(
                        target = unit,
                        name = "println",
                        generics = null,
                        parameters = listOf(null to SurfaceFieldAccess(SurfaceUnresolvedSymbol("x"), "name")),
                    ),
                ),
            ),
        )
        
        assertEquals(
            CoreBlock(
                null,
                listOf(
                    CoreLet(CoreTemp(0), CoreConstruct(objectType, emptyList())),
                    CoreDefineSymbol("x", CoreTemp(0)),
                    CoreLet(
                        CoreTemp(1),
                        CoreMethodCall(
                            target = CoreSymbol("x"),
                            name = "name",
                            generics = null,
                            arguments = listOf(null to CoreConst(FoxString("Bob"))),
                        ),
                    ),
                    CoreLet(
                        CoreTemp(2),
                        CoreMethodCall(
                            target = CoreSymbol("x"),
                            name = "name",
                            generics = null,
                            arguments = emptyList(),
                        ),
                    ),
                    CoreLet(
                        CoreTemp(3),
                        CoreMethodCall(
                            target = CoreConst(FoxUnit),
                            name = "println",
                            generics = null,
                            arguments = listOf(null to CoreTemp(2)),
                        ),
                    ),
                    CoreEvaluate(CoreTemp(3)),
                ),
            ),
            program.methods.single().body,
        )
        assertEquals(setOf(CoreObjectAccessorRequest(objectType, "name", FoxStringType)), program.objectAccessors)
    }
    
    @Test
    fun lowersCompoundFieldAssignmentWithoutRepeatingReceiver() {
        val program = lowerProgram(
            SurfaceAssign(
                SurfaceFieldAccess(
                    SurfaceCall(unit, "provider", generics = null, parameters = emptyList()),
                    "name",
                ),
                assign(AssignOperatorEnum.Add),
                SurfaceUnresolvedSymbol("suffix"),
                beforeEvaluation = true,
            ),
        )
        
        assertEquals(
            CoreBlock(
                null,
                listOf(
                    CoreLet(
                        CoreTemp(0),
                        CoreMethodCall(CoreConst(FoxUnit), "provider", null, emptyList()),
                    ),
                    CoreLet(
                        CoreTemp(1),
                        CoreMethodCall(CoreTemp(0), "name", null, emptyList()),
                    ),
                    CoreLet(
                        CoreTemp(2),
                        CoreMethodCall(
                            target = CoreTemp(1),
                            name = "add",
                            generics = null,
                            arguments = listOf(null to CoreSymbol("suffix")),
                        ),
                    ),
                    CoreLet(
                        CoreTemp(3),
                        CoreMethodCall(
                            target = CoreTemp(0),
                            name = "name",
                            generics = null,
                            arguments = listOf(null to CoreTemp(2)),
                        ),
                    ),
                ),
            ),
            program.methods.single().body,
        )
    }
    
    @Test
    fun lowersCompoundIndexAssignmentWithoutRepeatingIndices() {
        val program = lowerProgram(
            SurfaceAssign(
                SurfaceIndexAccess(
                    SurfaceUnresolvedSymbol("x"),
                    listOf(
                        SurfaceCall(unit, "a", generics = null, parameters = emptyList()),
                        SurfaceCall(unit, "b", generics = null, parameters = emptyList()),
                    ),
                ),
                assign(AssignOperatorEnum.Add),
                SurfaceUnresolvedSymbol("y"),
                beforeEvaluation = true,
            ),
        )
        
        assertEquals(
            CoreBlock(
                null,
                listOf(
                    CoreLet(CoreTemp(0), CoreMethodCall(CoreConst(FoxUnit), "a", null, emptyList())),
                    CoreLet(CoreTemp(1), CoreMethodCall(CoreConst(FoxUnit), "b", null, emptyList())),
                    CoreLet(
                        CoreTemp(2),
                        CoreMethodCall(
                            target = CoreSymbol("x"),
                            name = "get",
                            generics = null,
                            arguments = listOf(null to CoreTemp(0), null to CoreTemp(1)),
                        ),
                    ),
                    CoreLet(
                        CoreTemp(3),
                        CoreMethodCall(
                            target = CoreTemp(2),
                            name = "add",
                            generics = null,
                            arguments = listOf(null to CoreSymbol("y")),
                        ),
                    ),
                    CoreLet(
                        CoreTemp(4),
                        CoreMethodCall(
                            target = CoreSymbol("x"),
                            name = "set",
                            generics = null,
                            arguments = listOf(
                                null to CoreTemp(0),
                                null to CoreTemp(1),
                                null to CoreTemp(3),
                            ),
                        ),
                    ),
                ),
            ),
            program.methods.single().body,
        )
    }
    
    @Test
    fun keepsLogicalOperatorsAsShortCircuitExpressions() {
        val program = lowerProgram(
            SurfaceBinary(
                SurfaceCall(unit, "x", generics = null, parameters = emptyList()),
                FoxBinaryOperator(BinaryOperatorEnum.OrOr),
                SurfaceCall(unit, "y", generics = null, parameters = emptyList()),
            ),
        )
        
        assertEquals(
            CoreBlock(
                null,
                listOf(
                    CoreLet(
                        CoreTemp(2),
                        CoreShortCircuit(
                            operator = CoreShortCircuitOperator.Or,
                            left = CoreValueBlock(
                                listOf(CoreLet(CoreTemp(0), CoreMethodCall(CoreConst(FoxUnit), "x", null, emptyList()))),
                                CoreTemp(0),
                            ),
                            right = CoreValueBlock(
                                listOf(CoreLet(CoreTemp(1), CoreMethodCall(CoreConst(FoxUnit), "y", null, emptyList()))),
                                CoreTemp(1),
                            ),
                        ),
                    ),
                    CoreEvaluate(CoreTemp(2)),
                ),
            ),
            program.methods.single().body,
        )
    }
    
    private fun lowerProgram(body: SurfaceStatement): CoreProgram {
        val result = assertIs<CoreLoweringSuccess>(runCoreLowering(SurfaceFile(listOf(method(body)))))
        return result.program
    }
    
    private fun method(body: SurfaceStatement) = SurfaceMethodDefinition(
        generics = emptyOrderedMap(),
        thisType = FoxUnitType,
        name = "test",
        parameters = emptyOrderedMap(),
        returnType = FoxUnitType,
        body = body,
    )
    
    private fun assign(operator: AssignOperatorEnum) = FoxAssignOperator(operator)
    
    private val unit = SurfaceEntityStatement(FoxUnit)
}

private val FoxUnitType = SurfacePrimitiveType(PrimitiveTypeEnum.Unit)
private val FoxStringType = SurfacePrimitiveType(PrimitiveTypeEnum.String)
