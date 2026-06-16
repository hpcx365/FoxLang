package pers.hpcx.foxlang.type

import pers.hpcx.foxlang.ast.*

enum class ConcreteTypeFamily {
    VOID, UNIT, BOOL, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR, STRING,
    TUPLE, STRUCT, OBJECT, ENUM, ARRAY, REF, METHOD,
}

fun FoxPrimitiveType.family(): ConcreteTypeFamily = when (this) {
    FoxVoidType -> ConcreteTypeFamily.VOID
    FoxUnitType -> ConcreteTypeFamily.UNIT
    FoxBoolType -> ConcreteTypeFamily.BOOL
    FoxByteType -> ConcreteTypeFamily.BYTE
    FoxShortType -> ConcreteTypeFamily.SHORT
    FoxIntType -> ConcreteTypeFamily.INT
    FoxLongType -> ConcreteTypeFamily.LONG
    FoxFloatType -> ConcreteTypeFamily.FLOAT
    FoxDoubleType -> ConcreteTypeFamily.DOUBLE
    FoxCharType -> ConcreteTypeFamily.CHAR
    FoxStringType -> ConcreteTypeFamily.STRING
}

fun FoxBuiltInType.family(): ConcreteTypeFamily = when (this) {
    is FoxTupleType -> ConcreteTypeFamily.TUPLE
    is FoxStructType -> ConcreteTypeFamily.STRUCT
    is FoxObjectType -> ConcreteTypeFamily.OBJECT
    is FoxEnumType -> ConcreteTypeFamily.ENUM
    is FoxArrayType -> ConcreteTypeFamily.ARRAY
    is FoxRefType -> ConcreteTypeFamily.REF
    is FoxMethodType -> ConcreteTypeFamily.METHOD
}

fun FoxType.family(): ConcreteTypeFamily = when (this) {
    is FoxPrimitiveType -> family()
    is FoxBuiltInType -> family()
    else -> error("Only concrete types have a family: $this")
}
