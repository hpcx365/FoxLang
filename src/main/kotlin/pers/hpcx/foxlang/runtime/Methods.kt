package pers.hpcx.foxlang.runtime

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.emptyOrderedMap
import pers.hpcx.foxlang.utils.mapValues
import pers.hpcx.foxlang.utils.orderedMapOf

data class FoxMethodIdentifier(
    val name: String,
    val generics: OrderedMap<String, FoxType>,
    val thisType: FoxType,
    val parameters: OrderedMap<String, FoxType>,
)

data class FoxMethodSignature(
    val name: String,
    val generics: OrderedMap<String, FoxType>,
    val thisType: FoxType,
    val parameters: OrderedMap<String, FoxType>,
    val returnType: FoxType,
    val isInline: Boolean,
)

sealed interface FoxMethodImplementation

sealed interface FoxNativeMethodImplementation : FoxMethodImplementation

data class FoxCustomizedMethodImplementation(
    val startBlock: String,
    val blocks: Map<String, FoxInstBlock>,
) : FoxMethodImplementation

data class FoxInstBlock(
    val instructions: List<FoxInst>,
    val jump: FoxJump,
)

sealed interface FoxInst
sealed interface FoxJump

sealed interface FoxSlot
sealed interface FoxFetchSlot : FoxSlot
sealed interface FoxStoreSlot : FoxSlot

object SlotThis : FoxFetchSlot
object SlotIgnore : FoxStoreSlot
object SlotReturn : FoxFetchSlot
data class SlotConst(val value: FoxEntity) : FoxFetchSlot
data class SlotLocal(val name: String) : FoxFetchSlot, FoxStoreSlot
data class SlotParam(val name: String) : FoxFetchSlot
data class SlotGlobal(val name: String) : FoxFetchSlot, FoxStoreSlot

data class InstLoad(
    val target: FoxStoreSlot,
    val source: FoxFetchSlot,
) : FoxInst

data class InstCall(
    val target: FoxFetchSlot,
    val method: FoxMethodIdentifier,
    val params: Map<String, FoxFetchSlot>,
) : FoxInst

data class InstIndirectCall(
    val target: FoxFetchSlot,
    val method: FoxFetchSlot,
    val params: Map<String, FoxFetchSlot>,
) : FoxInst

data class JumpGoto(
    val block: String,
) : FoxJump

data class JumpBranch(
    val condition: FoxFetchSlot,
    val thenBlock: String,
    val elseBlock: String,
) : FoxJump

data class JumpReturn(
    val value: FoxFetchSlot,
) : FoxJump

val mainMethodIdentifier = FoxMethodIdentifier(
    name = "main",
    generics = orderedMapOf(),
    thisType = FoxUnitType,
    parameters = orderedMapOf("args" to FoxArrayType(FoxStringType)),
)

val panicMethodIdentifier = FoxMethodIdentifier(
    name = "panic",
    generics = orderedMapOf(),
    thisType = FoxUnitType,
    parameters = orderedMapOf("message" to FoxStringType),
)

enum class FoxBuiltInMethodImplementation(
    name: String,
    generics: OrderedMap<String, FoxType>,
    thisType: FoxType,
    parameters: OrderedMap<String, FoxType>,
    returnType: FoxType,
    val signature: FoxMethodSignature = FoxMethodSignature(
        name = name,
        generics = generics.mapValues { it.value },
        thisType = thisType,
        parameters = parameters,
        returnType = returnType,
        isInline = true,
    ),
) : FoxMethodImplementation {
    
    ByteToByte("to", orderedMapOf("T" to FoxByteType), FoxByteType, emptyOrderedMap(), FoxByteType),
    ShortToByte("to", orderedMapOf("T" to FoxByteType), FoxShortType, emptyOrderedMap(), FoxByteType),
    IntToByte("to", orderedMapOf("T" to FoxByteType), FoxIntType, emptyOrderedMap(), FoxByteType),
    LongToByte("to", orderedMapOf("T" to FoxByteType), FoxLongType, emptyOrderedMap(), FoxByteType),
    FloatToByte("to", orderedMapOf("T" to FoxByteType), FoxFloatType, emptyOrderedMap(), FoxByteType),
    DoubleToByte("to", orderedMapOf("T" to FoxByteType), FoxDoubleType, emptyOrderedMap(), FoxByteType),
    
    ByteToShort("to", orderedMapOf("T" to FoxShortType), FoxByteType, emptyOrderedMap(), FoxShortType),
    ShortToShort("to", orderedMapOf("T" to FoxShortType), FoxShortType, emptyOrderedMap(), FoxShortType),
    IntToShort("to", orderedMapOf("T" to FoxShortType), FoxIntType, emptyOrderedMap(), FoxShortType),
    LongToShort("to", orderedMapOf("T" to FoxShortType), FoxLongType, emptyOrderedMap(), FoxShortType),
    FloatToShort("to", orderedMapOf("T" to FoxShortType), FoxFloatType, emptyOrderedMap(), FoxShortType),
    DoubleToShort("to", orderedMapOf("T" to FoxShortType), FoxDoubleType, emptyOrderedMap(), FoxShortType),
    CharToShort("to", orderedMapOf("T" to FoxShortType), FoxCharType, emptyOrderedMap(), FoxShortType),
    
    ByteToInt("to", orderedMapOf("T" to FoxIntType), FoxByteType, emptyOrderedMap(), FoxIntType),
    ShortToInt("to", orderedMapOf("T" to FoxIntType), FoxShortType, emptyOrderedMap(), FoxIntType),
    IntToInt("to", orderedMapOf("T" to FoxIntType), FoxIntType, emptyOrderedMap(), FoxIntType),
    LongToInt("to", orderedMapOf("T" to FoxIntType), FoxLongType, emptyOrderedMap(), FoxIntType),
    FloatToInt("to", orderedMapOf("T" to FoxIntType), FoxFloatType, emptyOrderedMap(), FoxIntType),
    DoubleToInt("to", orderedMapOf("T" to FoxIntType), FoxDoubleType, emptyOrderedMap(), FoxIntType),
    CharToInt("to", orderedMapOf("T" to FoxIntType), FoxCharType, emptyOrderedMap(), FoxIntType),
    
    ByteToLong("to", orderedMapOf("T" to FoxLongType), FoxByteType, emptyOrderedMap(), FoxLongType),
    ShortToLong("to", orderedMapOf("T" to FoxLongType), FoxShortType, emptyOrderedMap(), FoxLongType),
    IntToLong("to", orderedMapOf("T" to FoxLongType), FoxIntType, emptyOrderedMap(), FoxLongType),
    LongToLong("to", orderedMapOf("T" to FoxLongType), FoxLongType, emptyOrderedMap(), FoxLongType),
    FloatToLong("to", orderedMapOf("T" to FoxLongType), FoxFloatType, emptyOrderedMap(), FoxLongType),
    DoubleToLong("to", orderedMapOf("T" to FoxLongType), FoxDoubleType, emptyOrderedMap(), FoxLongType),
    
    ByteToFloat("to", orderedMapOf("T" to FoxFloatType), FoxByteType, emptyOrderedMap(), FoxFloatType),
    ShortToFloat("to", orderedMapOf("T" to FoxFloatType), FoxShortType, emptyOrderedMap(), FoxFloatType),
    IntToFloat("to", orderedMapOf("T" to FoxFloatType), FoxIntType, emptyOrderedMap(), FoxFloatType),
    LongToFloat("to", orderedMapOf("T" to FoxFloatType), FoxLongType, emptyOrderedMap(), FoxFloatType),
    FloatToFloat("to", orderedMapOf("T" to FoxFloatType), FoxFloatType, emptyOrderedMap(), FoxFloatType),
    DoubleToFloat("to", orderedMapOf("T" to FoxFloatType), FoxDoubleType, emptyOrderedMap(), FoxFloatType),
    
    ByteToDouble("to", orderedMapOf("T" to FoxDoubleType), FoxByteType, emptyOrderedMap(), FoxDoubleType),
    ShortToDouble("to", orderedMapOf("T" to FoxDoubleType), FoxShortType, emptyOrderedMap(), FoxDoubleType),
    IntToDouble("to", orderedMapOf("T" to FoxDoubleType), FoxIntType, emptyOrderedMap(), FoxDoubleType),
    LongToDouble("to", orderedMapOf("T" to FoxDoubleType), FoxLongType, emptyOrderedMap(), FoxDoubleType),
    FloatToDouble("to", orderedMapOf("T" to FoxDoubleType), FoxFloatType, emptyOrderedMap(), FoxDoubleType),
    DoubleToDouble("to", orderedMapOf("T" to FoxDoubleType), FoxDoubleType, emptyOrderedMap(), FoxDoubleType),
    
    ShortToChar("to", orderedMapOf("T" to FoxCharType), FoxShortType, emptyOrderedMap(), FoxCharType),
    IntToChar("to", orderedMapOf("T" to FoxCharType), FoxIntType, emptyOrderedMap(), FoxCharType),
    
    UnitToString("to", orderedMapOf("T" to FoxStringType), FoxUnitType, emptyOrderedMap(), FoxStringType),
    BoolToString("to", orderedMapOf("T" to FoxStringType), FoxBoolType, emptyOrderedMap(), FoxStringType),
    ByteToString("to", orderedMapOf("T" to FoxStringType), FoxByteType, emptyOrderedMap(), FoxStringType),
    ShortToString("to", orderedMapOf("T" to FoxStringType), FoxShortType, emptyOrderedMap(), FoxStringType),
    IntToString("to", orderedMapOf("T" to FoxStringType), FoxIntType, emptyOrderedMap(), FoxStringType),
    LongToString("to", orderedMapOf("T" to FoxStringType), FoxLongType, emptyOrderedMap(), FoxStringType),
    FloatToString("to", orderedMapOf("T" to FoxStringType), FoxFloatType, emptyOrderedMap(), FoxStringType),
    DoubleToString("to", orderedMapOf("T" to FoxStringType), FoxDoubleType, emptyOrderedMap(), FoxStringType),
    CharToString("to", orderedMapOf("T" to FoxStringType), FoxCharType, emptyOrderedMap(), FoxStringType),
    StringToString("to", orderedMapOf("T" to FoxStringType), FoxStringType, emptyOrderedMap(), FoxStringType),
    
    FloatAsInt("intBits", emptyOrderedMap(), FoxFloatType, emptyOrderedMap(), FoxIntType),
    DoubleAsLong("longBits", emptyOrderedMap(), FoxDoubleType, emptyOrderedMap(), FoxLongType),
    
    BoolNot("not", emptyOrderedMap(), FoxBoolType, emptyOrderedMap(), FoxBoolType),
    ByteNot("not", emptyOrderedMap(), FoxByteType, emptyOrderedMap(), FoxByteType),
    ShortNot("not", emptyOrderedMap(), FoxShortType, emptyOrderedMap(), FoxShortType),
    IntNot("not", emptyOrderedMap(), FoxIntType, emptyOrderedMap(), FoxIntType),
    LongNot("not", emptyOrderedMap(), FoxLongType, emptyOrderedMap(), FoxLongType),
    
    ByteAnd("and", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxByteType),
    ShortAnd("and", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxShortType),
    IntAnd("and", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxIntType),
    LongAnd("and", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxLongType),
    
    ByteOr("or", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxByteType),
    ShortOr("or", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxShortType),
    IntOr("or", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxIntType),
    LongOr("or", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxLongType),
    
    ByteXor("xor", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxByteType),
    ShortXor("xor", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxShortType),
    IntXor("xor", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxIntType),
    LongXor("xor", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxLongType),
    
    ByteShl("shl", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxIntType), FoxByteType),
    ShortShl("shl", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxIntType), FoxShortType),
    IntShl("shl", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxIntType),
    LongShl("shl", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxIntType), FoxLongType),
    
    ByteShr("shr", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxIntType), FoxByteType),
    ShortShr("shr", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxIntType), FoxShortType),
    IntShr("shr", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxIntType),
    LongShr("shr", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxIntType), FoxLongType),
    
    ByteUshr("ushr", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxIntType), FoxByteType),
    ShortUshr("ushr", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxIntType), FoxShortType),
    IntUshr("ushr", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxIntType),
    LongUshr("ushr", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxIntType), FoxLongType),
    
    ByteEq("eq", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxBoolType),
    ShortEq("eq", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxBoolType),
    IntEq("eq", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxBoolType),
    LongEq("eq", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxBoolType),
    FloatEq("eq", emptyOrderedMap(), FoxFloatType, orderedMapOf("that" to FoxFloatType), FoxBoolType),
    DoubleEq("eq", emptyOrderedMap(), FoxDoubleType, orderedMapOf("that" to FoxDoubleType), FoxBoolType),
    StringEq("eq", emptyOrderedMap(), FoxStringType, orderedMapOf("that" to FoxStringType), FoxBoolType),
    
    ByteNeq("neq", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxBoolType),
    ShortNeq("neq", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxBoolType),
    IntNeq("neq", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxBoolType),
    LongNeq("neq", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxBoolType),
    FloatNeq("neq", emptyOrderedMap(), FoxFloatType, orderedMapOf("that" to FoxFloatType), FoxBoolType),
    DoubleNeq("neq", emptyOrderedMap(), FoxDoubleType, orderedMapOf("that" to FoxDoubleType), FoxBoolType),
    StringNeq("neq", emptyOrderedMap(), FoxStringType, orderedMapOf("that" to FoxStringType), FoxBoolType),
    
    ByteGt("gt", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxBoolType),
    ShortGt("gt", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxBoolType),
    IntGt("gt", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxBoolType),
    LongGt("gt", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxBoolType),
    FloatGt("gt", emptyOrderedMap(), FoxFloatType, orderedMapOf("that" to FoxFloatType), FoxBoolType),
    DoubleGt("gt", emptyOrderedMap(), FoxDoubleType, orderedMapOf("that" to FoxDoubleType), FoxBoolType),
    
    ByteGte("gte", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxBoolType),
    ShortGte("gte", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxBoolType),
    IntGte("gte", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxBoolType),
    LongGte("gte", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxBoolType),
    FloatGte("gte", emptyOrderedMap(), FoxFloatType, orderedMapOf("that" to FoxFloatType), FoxBoolType),
    DoubleGte("gte", emptyOrderedMap(), FoxDoubleType, orderedMapOf("that" to FoxDoubleType), FoxBoolType),
    
    ByteLt("lt", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxBoolType),
    ShortLt("lt", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxBoolType),
    IntLt("lt", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxBoolType),
    LongLt("lt", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxBoolType),
    FloatLt("lt", emptyOrderedMap(), FoxFloatType, orderedMapOf("that" to FoxFloatType), FoxBoolType),
    DoubleLt("lt", emptyOrderedMap(), FoxDoubleType, orderedMapOf("that" to FoxDoubleType), FoxBoolType),
    
    ByteLte("lte", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxBoolType),
    ShortLte("lte", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxBoolType),
    IntLte("lte", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxBoolType),
    LongLte("lte", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxBoolType),
    FloatLte("lte", emptyOrderedMap(), FoxFloatType, orderedMapOf("that" to FoxFloatType), FoxBoolType),
    DoubleLte("lte", emptyOrderedMap(), FoxDoubleType, orderedMapOf("that" to FoxDoubleType), FoxBoolType),
    
    ByteCompareTo("cmp", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxIntType),
    ShortCompareTo("cmp", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxIntType),
    IntCompareTo("cmp", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxIntType),
    LongCompareTo("cmp", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxIntType),
    FloatCompareTo("cmp", emptyOrderedMap(), FoxFloatType, orderedMapOf("that" to FoxFloatType), FoxIntType),
    DoubleCompareTo("cmp", emptyOrderedMap(), FoxDoubleType, orderedMapOf("that" to FoxDoubleType), FoxIntType),
    StringCompareTo("cmp", emptyOrderedMap(), FoxStringType, orderedMapOf("that" to FoxStringType), FoxIntType),
    
    ByteAdd("add", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxByteType),
    ShortAdd("add", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxShortType),
    IntAdd("add", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxIntType),
    LongAdd("add", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxLongType),
    FloatAdd("add", emptyOrderedMap(), FoxFloatType, orderedMapOf("that" to FoxFloatType), FoxFloatType),
    DoubleAdd("add", emptyOrderedMap(), FoxDoubleType, orderedMapOf("that" to FoxDoubleType), FoxDoubleType),
    StringAdd("add", emptyOrderedMap(), FoxStringType, orderedMapOf("that" to FoxStringType), FoxStringType),
    
    ByteSub("sub", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxByteType),
    ShortSub("sub", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxShortType),
    IntSub("sub", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxIntType),
    LongSub("sub", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxLongType),
    FloatSub("sub", emptyOrderedMap(), FoxFloatType, orderedMapOf("that" to FoxFloatType), FoxFloatType),
    DoubleSub("sub", emptyOrderedMap(), FoxDoubleType, orderedMapOf("that" to FoxDoubleType), FoxDoubleType),
    
    ByteMul("mul", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxByteType),
    ShortMul("mul", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxShortType),
    IntMul("mul", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxIntType),
    LongMul("mul", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxLongType),
    FloatMul("mul", emptyOrderedMap(), FoxFloatType, orderedMapOf("that" to FoxFloatType), FoxFloatType),
    DoubleMul("mul", emptyOrderedMap(), FoxDoubleType, orderedMapOf("that" to FoxDoubleType), FoxDoubleType),
    
    ByteDiv("div", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxByteType),
    ShortDiv("div", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxShortType),
    IntDiv("div", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxIntType),
    LongDiv("div", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxLongType),
    FloatDiv("div", emptyOrderedMap(), FoxFloatType, orderedMapOf("that" to FoxFloatType), FoxFloatType),
    DoubleDiv("div", emptyOrderedMap(), FoxDoubleType, orderedMapOf("that" to FoxDoubleType), FoxDoubleType),
    
    ByteRem("rem", emptyOrderedMap(), FoxByteType, orderedMapOf("that" to FoxByteType), FoxByteType),
    ShortRem("rem", emptyOrderedMap(), FoxShortType, orderedMapOf("that" to FoxShortType), FoxShortType),
    IntRem("rem", emptyOrderedMap(), FoxIntType, orderedMapOf("that" to FoxIntType), FoxIntType),
    LongRem("rem", emptyOrderedMap(), FoxLongType, orderedMapOf("that" to FoxLongType), FoxLongType),
    FloatRem("rem", emptyOrderedMap(), FoxFloatType, orderedMapOf("that" to FoxFloatType), FoxFloatType),
    DoubleRem("rem", emptyOrderedMap(), FoxDoubleType, orderedMapOf("that" to FoxDoubleType), FoxDoubleType),
}
