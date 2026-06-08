package pers.hpcx.foxlang.runtime

import pers.hpcx.foxlang.ast.*
import java.util.*

data class FoxMethodIdentifier(
    val name: String,
    val generics: SequencedMap<String, FoxType>,
    val thisType: FoxType,
    val parameters: SequencedMap<String, FoxType>,
)

data class FoxMethodSignature(
    val name: String,
    val generics: SequencedMap<String, FoxGenericConstraint>,
    val thisType: FoxType,
    val parameters: SequencedMap<String, FoxType>,
    val returnType: FoxType,
    val isInline: Boolean,
)

sealed interface FoxMethodImplementation

sealed interface FoxNativeMethodImplementation : FoxMethodImplementation

data class FoxSimpleNativeMethodImplementation(
    val signature: FoxMethodSignature,
    val invoke: (target: FoxEntity, params: SequencedMap<String, FoxEntity>) -> FoxEntity,
) : FoxNativeMethodImplementation

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

data class SlotConst(
    val value: FoxEntity,
) : FoxFetchSlot

data class SlotLocal(
    val name: String,
) : FoxFetchSlot, FoxStoreSlot

data class SlotGlobal(
    val name: String,
) : FoxFetchSlot, FoxStoreSlot

object SlotThis : FoxFetchSlot
object SlotVoid : FoxStoreSlot
object SlotReturnValue : FoxFetchSlot

data class InstLoad(
    val target: FoxStoreSlot,
    val entity: FoxEntity,
) : FoxInst

data class InstCopy(
    val target: FoxStoreSlot,
    val source: FoxFetchSlot,
) : FoxInst

data class InstCall(
    val target: FoxFetchSlot,
    val params: SequencedMap<String, FoxFetchSlot>,
    val method: FoxMethodIdentifier,
) : FoxInst

data class InstLambdaCall(
    val target: FoxFetchSlot,
    val params: List<FoxFetchSlot>,
    val method: FoxFetchSlot,
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
    generics = linkedMapOf(),
    thisType = FoxUnitType,
    parameters = linkedMapOf("args" to FoxArrayType(FoxStringType)),
)

val panicMethodIdentifier = FoxMethodIdentifier(
    name = "panic",
    generics = linkedMapOf(),
    thisType = FoxUnitType,
    parameters = linkedMapOf("message" to FoxStringType),
)

enum class FoxBuiltInMethodImplementation(
    name: String,
    generics: Map<String, FoxType>,
    thisType: FoxType,
    parameters: Map<String, FoxType>,
    returnType: FoxType,
    val signature: FoxMethodSignature = FoxMethodSignature(
        name = name,
        generics = generics.mapValuesTo(LinkedHashMap()) { (_, value) -> FoxGenericConstraint(value) },
        thisType = thisType,
        parameters = LinkedHashMap(parameters),
        returnType = returnType,
        isInline = true,
    ),
) : FoxMethodImplementation {
    
    ByteToByte("to", mapOf("T" to FoxByteType), FoxByteType, emptyMap(), FoxByteType),
    ShortToByte("to", mapOf("T" to FoxByteType), FoxShortType, emptyMap(), FoxByteType),
    IntToByte("to", mapOf("T" to FoxByteType), FoxIntType, emptyMap(), FoxByteType),
    LongToByte("to", mapOf("T" to FoxByteType), FoxLongType, emptyMap(), FoxByteType),
    FloatToByte("to", mapOf("T" to FoxByteType), FoxFloatType, emptyMap(), FoxByteType),
    DoubleToByte("to", mapOf("T" to FoxByteType), FoxDoubleType, emptyMap(), FoxByteType),
    
    ByteToShort("to", mapOf("T" to FoxShortType), FoxByteType, emptyMap(), FoxShortType),
    ShortToShort("to", mapOf("T" to FoxShortType), FoxShortType, emptyMap(), FoxShortType),
    IntToShort("to", mapOf("T" to FoxShortType), FoxIntType, emptyMap(), FoxShortType),
    LongToShort("to", mapOf("T" to FoxShortType), FoxLongType, emptyMap(), FoxShortType),
    FloatToShort("to", mapOf("T" to FoxShortType), FoxFloatType, emptyMap(), FoxShortType),
    DoubleToShort("to", mapOf("T" to FoxShortType), FoxDoubleType, emptyMap(), FoxShortType),
    CharToShort("to", mapOf("T" to FoxShortType), FoxCharType, emptyMap(), FoxShortType),
    
    ByteToInt("to", mapOf("T" to FoxIntType), FoxByteType, emptyMap(), FoxIntType),
    ShortToInt("to", mapOf("T" to FoxIntType), FoxShortType, emptyMap(), FoxIntType),
    IntToInt("to", mapOf("T" to FoxIntType), FoxIntType, emptyMap(), FoxIntType),
    LongToInt("to", mapOf("T" to FoxIntType), FoxLongType, emptyMap(), FoxIntType),
    FloatToInt("to", mapOf("T" to FoxIntType), FoxFloatType, emptyMap(), FoxIntType),
    DoubleToInt("to", mapOf("T" to FoxIntType), FoxDoubleType, emptyMap(), FoxIntType),
    CharToInt("to", mapOf("T" to FoxIntType), FoxCharType, emptyMap(), FoxIntType),
    
    ByteToLong("to", mapOf("T" to FoxLongType), FoxByteType, emptyMap(), FoxLongType),
    ShortToLong("to", mapOf("T" to FoxLongType), FoxShortType, emptyMap(), FoxLongType),
    IntToLong("to", mapOf("T" to FoxLongType), FoxIntType, emptyMap(), FoxLongType),
    LongToLong("to", mapOf("T" to FoxLongType), FoxLongType, emptyMap(), FoxLongType),
    FloatToLong("to", mapOf("T" to FoxLongType), FoxFloatType, emptyMap(), FoxLongType),
    DoubleToLong("to", mapOf("T" to FoxLongType), FoxDoubleType, emptyMap(), FoxLongType),
    
    ByteToFloat("to", mapOf("T" to FoxFloatType), FoxByteType, emptyMap(), FoxFloatType),
    ShortToFloat("to", mapOf("T" to FoxFloatType), FoxShortType, emptyMap(), FoxFloatType),
    IntToFloat("to", mapOf("T" to FoxFloatType), FoxIntType, emptyMap(), FoxFloatType),
    LongToFloat("to", mapOf("T" to FoxFloatType), FoxLongType, emptyMap(), FoxFloatType),
    FloatToFloat("to", mapOf("T" to FoxFloatType), FoxFloatType, emptyMap(), FoxFloatType),
    DoubleToFloat("to", mapOf("T" to FoxFloatType), FoxDoubleType, emptyMap(), FoxFloatType),
    
    ByteToDouble("to", mapOf("T" to FoxDoubleType), FoxByteType, emptyMap(), FoxDoubleType),
    ShortToDouble("to", mapOf("T" to FoxDoubleType), FoxShortType, emptyMap(), FoxDoubleType),
    IntToDouble("to", mapOf("T" to FoxDoubleType), FoxIntType, emptyMap(), FoxDoubleType),
    LongToDouble("to", mapOf("T" to FoxDoubleType), FoxLongType, emptyMap(), FoxDoubleType),
    FloatToDouble("to", mapOf("T" to FoxDoubleType), FoxFloatType, emptyMap(), FoxDoubleType),
    DoubleToDouble("to", mapOf("T" to FoxDoubleType), FoxDoubleType, emptyMap(), FoxDoubleType),
    
    ShortToChar("to", mapOf("T" to FoxCharType), FoxShortType, emptyMap(), FoxCharType),
    IntToChar("to", mapOf("T" to FoxCharType), FoxIntType, emptyMap(), FoxCharType),
    
    UnitToString("to", mapOf("T" to FoxStringType), FoxUnitType, emptyMap(), FoxStringType),
    BoolToString("to", mapOf("T" to FoxStringType), FoxBoolType, emptyMap(), FoxStringType),
    ByteToString("to", mapOf("T" to FoxStringType), FoxByteType, emptyMap(), FoxStringType),
    ShortToString("to", mapOf("T" to FoxStringType), FoxShortType, emptyMap(), FoxStringType),
    IntToString("to", mapOf("T" to FoxStringType), FoxIntType, emptyMap(), FoxStringType),
    LongToString("to", mapOf("T" to FoxStringType), FoxLongType, emptyMap(), FoxStringType),
    FloatToString("to", mapOf("T" to FoxStringType), FoxFloatType, emptyMap(), FoxStringType),
    DoubleToString("to", mapOf("T" to FoxStringType), FoxDoubleType, emptyMap(), FoxStringType),
    CharToString("to", mapOf("T" to FoxStringType), FoxCharType, emptyMap(), FoxStringType),
    StringToString("to", mapOf("T" to FoxStringType), FoxStringType, emptyMap(), FoxStringType),
    
    FloatAsInt("intBits", emptyMap(), FoxFloatType, emptyMap(), FoxIntType),
    DoubleAsLong("longBits", emptyMap(), FoxDoubleType, emptyMap(), FoxLongType),
    
    BoolNot("not", emptyMap(), FoxBoolType, emptyMap(), FoxBoolType),
    ByteNot("not", emptyMap(), FoxByteType, emptyMap(), FoxByteType),
    ShortNot("not", emptyMap(), FoxShortType, emptyMap(), FoxShortType),
    IntNot("not", emptyMap(), FoxIntType, emptyMap(), FoxIntType),
    LongNot("not", emptyMap(), FoxLongType, emptyMap(), FoxLongType),
    
    ByteAnd("and", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxByteType),
    ShortAnd("and", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxShortType),
    IntAnd("and", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxIntType),
    LongAnd("and", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxLongType),
    
    ByteOr("or", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxByteType),
    ShortOr("or", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxShortType),
    IntOr("or", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxIntType),
    LongOr("or", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxLongType),
    
    ByteXor("xor", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxByteType),
    ShortXor("xor", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxShortType),
    IntXor("xor", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxIntType),
    LongXor("xor", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxLongType),
    
    ByteShl("shl", emptyMap(), FoxByteType, mapOf("that" to FoxIntType), FoxByteType),
    ShortShl("shl", emptyMap(), FoxShortType, mapOf("that" to FoxIntType), FoxShortType),
    IntShl("shl", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxIntType),
    LongShl("shl", emptyMap(), FoxLongType, mapOf("that" to FoxIntType), FoxLongType),
    
    ByteShr("shr", emptyMap(), FoxByteType, mapOf("that" to FoxIntType), FoxByteType),
    ShortShr("shr", emptyMap(), FoxShortType, mapOf("that" to FoxIntType), FoxShortType),
    IntShr("shr", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxIntType),
    LongShr("shr", emptyMap(), FoxLongType, mapOf("that" to FoxIntType), FoxLongType),
    
    ByteUshr("ushr", emptyMap(), FoxByteType, mapOf("that" to FoxIntType), FoxByteType),
    ShortUshr("ushr", emptyMap(), FoxShortType, mapOf("that" to FoxIntType), FoxShortType),
    IntUshr("ushr", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxIntType),
    LongUshr("ushr", emptyMap(), FoxLongType, mapOf("that" to FoxIntType), FoxLongType),
    
    ByteEq("eq", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxBoolType),
    ShortEq("eq", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxBoolType),
    IntEq("eq", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxBoolType),
    LongEq("eq", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxBoolType),
    FloatEq("eq", emptyMap(), FoxFloatType, mapOf("that" to FoxFloatType), FoxBoolType),
    DoubleEq("eq", emptyMap(), FoxDoubleType, mapOf("that" to FoxDoubleType), FoxBoolType),
    StringEq("eq", emptyMap(), FoxStringType, mapOf("that" to FoxStringType), FoxBoolType),
    
    ByteNeq("neq", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxBoolType),
    ShortNeq("neq", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxBoolType),
    IntNeq("neq", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxBoolType),
    LongNeq("neq", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxBoolType),
    FloatNeq("neq", emptyMap(), FoxFloatType, mapOf("that" to FoxFloatType), FoxBoolType),
    DoubleNeq("neq", emptyMap(), FoxDoubleType, mapOf("that" to FoxDoubleType), FoxBoolType),
    StringNeq("neq", emptyMap(), FoxStringType, mapOf("that" to FoxStringType), FoxBoolType),
    
    ByteGt("gt", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxBoolType),
    ShortGt("gt", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxBoolType),
    IntGt("gt", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxBoolType),
    LongGt("gt", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxBoolType),
    FloatGt("gt", emptyMap(), FoxFloatType, mapOf("that" to FoxFloatType), FoxBoolType),
    DoubleGt("gt", emptyMap(), FoxDoubleType, mapOf("that" to FoxDoubleType), FoxBoolType),
    
    ByteGte("gte", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxBoolType),
    ShortGte("gte", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxBoolType),
    IntGte("gte", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxBoolType),
    LongGte("gte", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxBoolType),
    FloatGte("gte", emptyMap(), FoxFloatType, mapOf("that" to FoxFloatType), FoxBoolType),
    DoubleGte("gte", emptyMap(), FoxDoubleType, mapOf("that" to FoxDoubleType), FoxBoolType),
    
    ByteLt("lt", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxBoolType),
    ShortLt("lt", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxBoolType),
    IntLt("lt", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxBoolType),
    LongLt("lt", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxBoolType),
    FloatLt("lt", emptyMap(), FoxFloatType, mapOf("that" to FoxFloatType), FoxBoolType),
    DoubleLt("lt", emptyMap(), FoxDoubleType, mapOf("that" to FoxDoubleType), FoxBoolType),
    
    ByteLte("lte", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxBoolType),
    ShortLte("lte", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxBoolType),
    IntLte("lte", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxBoolType),
    LongLte("lte", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxBoolType),
    FloatLte("lte", emptyMap(), FoxFloatType, mapOf("that" to FoxFloatType), FoxBoolType),
    DoubleLte("lte", emptyMap(), FoxDoubleType, mapOf("that" to FoxDoubleType), FoxBoolType),
    
    ByteCompareTo("cmp", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxIntType),
    ShortCompareTo("cmp", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxIntType),
    IntCompareTo("cmp", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxIntType),
    LongCompareTo("cmp", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxIntType),
    FloatCompareTo("cmp", emptyMap(), FoxFloatType, mapOf("that" to FoxFloatType), FoxIntType),
    DoubleCompareTo("cmp", emptyMap(), FoxDoubleType, mapOf("that" to FoxDoubleType), FoxIntType),
    StringCompareTo("cmp", emptyMap(), FoxStringType, mapOf("that" to FoxStringType), FoxIntType),
    
    ByteAdd("add", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxByteType),
    ShortAdd("add", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxShortType),
    IntAdd("add", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxIntType),
    LongAdd("add", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxLongType),
    FloatAdd("add", emptyMap(), FoxFloatType, mapOf("that" to FoxFloatType), FoxFloatType),
    DoubleAdd("add", emptyMap(), FoxDoubleType, mapOf("that" to FoxDoubleType), FoxDoubleType),
    StringAdd("add", emptyMap(), FoxStringType, mapOf("that" to FoxStringType), FoxStringType),
    
    ByteSub("sub", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxByteType),
    ShortSub("sub", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxShortType),
    IntSub("sub", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxIntType),
    LongSub("sub", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxLongType),
    FloatSub("sub", emptyMap(), FoxFloatType, mapOf("that" to FoxFloatType), FoxFloatType),
    DoubleSub("sub", emptyMap(), FoxDoubleType, mapOf("that" to FoxDoubleType), FoxDoubleType),
    
    ByteMul("mul", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxByteType),
    ShortMul("mul", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxShortType),
    IntMul("mul", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxIntType),
    LongMul("mul", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxLongType),
    FloatMul("mul", emptyMap(), FoxFloatType, mapOf("that" to FoxFloatType), FoxFloatType),
    DoubleMul("mul", emptyMap(), FoxDoubleType, mapOf("that" to FoxDoubleType), FoxDoubleType),
    
    ByteDiv("div", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxByteType),
    ShortDiv("div", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxShortType),
    IntDiv("div", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxIntType),
    LongDiv("div", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxLongType),
    FloatDiv("div", emptyMap(), FoxFloatType, mapOf("that" to FoxFloatType), FoxFloatType),
    DoubleDiv("div", emptyMap(), FoxDoubleType, mapOf("that" to FoxDoubleType), FoxDoubleType),
    
    ByteRem("rem", emptyMap(), FoxByteType, mapOf("that" to FoxByteType), FoxByteType),
    ShortRem("rem", emptyMap(), FoxShortType, mapOf("that" to FoxShortType), FoxShortType),
    IntRem("rem", emptyMap(), FoxIntType, mapOf("that" to FoxIntType), FoxIntType),
    LongRem("rem", emptyMap(), FoxLongType, mapOf("that" to FoxLongType), FoxLongType),
    FloatRem("rem", emptyMap(), FoxFloatType, mapOf("that" to FoxFloatType), FoxFloatType),
    DoubleRem("rem", emptyMap(), FoxDoubleType, mapOf("that" to FoxDoubleType), FoxDoubleType),
}
