package pers.hpcx.foxlang.ast

sealed interface FoxUnaryOperator
object FoxNotOperator : FoxUnaryOperator
object FoxNegOperator : FoxUnaryOperator

sealed interface FoxBinaryOperator
object FoxAddOperator : FoxBinaryOperator
object FoxSubOperator : FoxBinaryOperator
object FoxMulOperator : FoxBinaryOperator
object FoxDivOperator : FoxBinaryOperator
object FoxRemOperator : FoxBinaryOperator
object FoxAndOperator : FoxBinaryOperator
object FoxOrOperator : FoxBinaryOperator
object FoxXorOperator : FoxBinaryOperator
object FoxShlOperator : FoxBinaryOperator
object FoxShrOperator : FoxBinaryOperator
object FoxUshrOperator : FoxBinaryOperator
object FoxEqOperator : FoxBinaryOperator
object FoxNeqOperator : FoxBinaryOperator
object FoxLtOperator : FoxBinaryOperator
object FoxGtOperator : FoxBinaryOperator
object FoxLeOperator : FoxBinaryOperator
object FoxGeOperator : FoxBinaryOperator
object FoxAndAndOperator : FoxBinaryOperator
object FoxOrOrOperator : FoxBinaryOperator

sealed interface FoxAssignOperator
object FoxPlainAssignOperator : FoxAssignOperator
object FoxTypeBindingAssignOperator : FoxAssignOperator
object FoxAddAssignOperator : FoxAssignOperator
object FoxSubAssignOperator : FoxAssignOperator
object FoxMulAssignOperator : FoxAssignOperator
object FoxDivAssignOperator : FoxAssignOperator
object FoxRemAssignOperator : FoxAssignOperator
object FoxAndAssignOperator : FoxAssignOperator
object FoxOrAssignOperator : FoxAssignOperator
object FoxXorAssignOperator : FoxAssignOperator
object FoxShlAssignOperator : FoxAssignOperator
object FoxShrAssignOperator : FoxAssignOperator
object FoxUshrAssignOperator : FoxAssignOperator
object FoxAndAndAssignOperator : FoxAssignOperator
object FoxOrOrAssignOperator : FoxAssignOperator
