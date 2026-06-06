package pers.hpcx.foxlang.runtime

sealed interface FoxEntity
sealed interface FoxPrimitive : FoxEntity

object FoxUnit : FoxPrimitive
data class FoxBool(val value: Boolean) : FoxPrimitive
data class FoxByte(val value: Byte) : FoxPrimitive
data class FoxShort(val value: Short) : FoxPrimitive
data class FoxInt(val value: Int) : FoxPrimitive
data class FoxLong(val value: Long) : FoxPrimitive
data class FoxFloat(val value: Float) : FoxPrimitive
data class FoxDouble(val value: Double) : FoxPrimitive
data class FoxChar(val value: Char) : FoxPrimitive
data class FoxString(val value: String) : FoxPrimitive

data class FoxArray(val elements: List<FoxEntity>) : FoxEntity
data class FoxTuple(val components: List<FoxEntity>) : FoxEntity
data class FoxStruct(val fields: Map<String, FoxEntity>) : FoxEntity
data class FoxEnum(val name: String, val value: FoxEntity) : FoxEntity
data class FoxRef(val referent: Int) : FoxEntity
data class FoxLambda(val captured: FoxTuple, val implementation: FoxMethodIdentifier) : FoxEntity

