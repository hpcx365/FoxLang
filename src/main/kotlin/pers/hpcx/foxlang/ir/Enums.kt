package pers.hpcx.foxlang.ir

enum class PrimitiveTypeEnum {
    Void, Unit, Bool, Byte, Short, Int, Long, Float, Double, Char, String,
}

enum class UnaryOperatorEnum {
    Not, Neg,
}

enum class BinaryOperatorEnum {
    Add, Sub, Mul, Div, Rem, And, Or, Xor, Shl, Shr, Ushr, Eq, Neq, Lt, Gt, Leq, Geq, AndAnd, OrOr,
}

enum class AssignOperatorEnum {
    Plain, Def, Add, Sub, Mul, Div, Rem, And, Or, Xor, Shl, Shr, Ushr, AndAnd, OrOr,
}
