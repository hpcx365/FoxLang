package pers.hpcx.foxlang.runtime

import pers.hpcx.foxlang.runtime.FoxBuiltInMethodImplementation.*

class Interpreter {
    
    val heap = Heap()
    val stack = mutableListOf<StackFrame>()
    val globals = mutableMapOf<String, FoxEntity>()
    val methods = mutableMapOf<FoxMethodIdentifier, FoxMethodImplementation>()
    var completedReturn: FoxEntity = FoxUnit
    
    fun run(arguments: Array<String>) {
        stack += StackFrame(
            method = methods.getValue(mainMethodIdentifier) as FoxCustomizedMethodImplementation,
            thisEntity = FoxUnit,
            parameters = mapOf("args" to FoxArray(arguments.map { FoxString(it) })),
        )
        runLoop()
    }
    
    fun invoke(
        identifier: FoxMethodIdentifier,
        thisEntity: FoxEntity = FoxUnit,
        parameters: Map<String, FoxEntity>,
    ): FoxEntity {
        completedReturn = FoxUnit
        when (val method = methods.getValue(identifier)) {
            is FoxBuiltInMethodImplementation -> {
                val frame = StackFrame(
                    method = FoxCustomizedMethodImplementation(
                        startBlock = "entry",
                        blocks = mapOf("entry" to FoxInstBlock(emptyList(), JumpReturn(SlotConst(FoxUnit)))),
                    ),
                    thisEntity = thisEntity,
                    parameters = mapOf(),
                )
                return method.invoke(frame, thisEntity, parameters)
            }
            is FoxCustomizedMethodImplementation -> {
                stack += StackFrame(method, thisEntity, parameters)
                runLoop()
                return completedReturn
            }
        }
    }
    
    private fun runLoop() {
        while (true) {
            if (stack.isEmpty()) return
            val frame = stack.last()
            val block = frame.currentBlock
            if (frame.nextInst < block.instructions.size) {
                frame.execute(block.instructions[frame.nextInst++])
            } else {
                frame.execute(block.jump)
            }
        }
    }
    
    inner class StackFrame(
        val method: FoxCustomizedMethodImplementation,
        val thisEntity: FoxEntity,
        parameters: Map<String, FoxEntity>,
    ) {
        val locals: MutableMap<String, FoxEntity> = parameters.toMutableMap()
        var currentBlock: FoxInstBlock = method.blocks.getValue(method.startBlock)
        var nextInst: Int = 0
        lateinit var returnEntity: FoxEntity
        
        fun switchBlock(block: String) {
            currentBlock = method.blocks.getValue(block)
            nextInst = 0
        }
        
        fun FoxFetchSlot.fetch() = when (this) {
            is SlotConst -> value
            SlotThis -> thisEntity
            SlotReturnValue -> returnEntity
            is SlotGlobal -> globals.getValue(name)
            is SlotLocal -> locals.getValue(name)
        }
        
        fun FoxStoreSlot.store(value: FoxEntity) = when (this) {
            SlotVoid -> {}
            is SlotGlobal -> globals[name] = value
            is SlotLocal -> locals[name] = value
        }
        
        fun panic(message: String) {
            execute(
                InstCall(
                    SlotConst(FoxUnit),
                    panicMethodIdentifier,
                    mapOf("message" to SlotConst(FoxString(message))),
                ),
            )
        }
        
        fun execute(inst: FoxInst) {
            when (inst) {
                is InstLoad -> inst.target.store(inst.source.fetch())
                is InstCall -> {
                    val target = inst.target.fetch()
                    val callee = methods.getValue(inst.method)
                    val params = inst.params.mapValues { it.value.fetch() }
                    call(callee, target, params)
                }
                is InstIndirectCall -> {
                    val target = inst.target.fetch()
                    val callee = methods.getValue((inst.method.fetch() as FoxMethod).identifier)
                    val params = inst.params.mapValues { it.value.fetch() }
                    call(callee, target, params)
                }
            }
        }
        
        private fun call(callee: FoxMethodImplementation, target: FoxEntity, parameters: Map<String, FoxEntity>) {
            when (callee) {
                is FoxBuiltInMethodImplementation -> callee.invoke(this, target, parameters)
                is FoxCustomizedMethodImplementation -> {
                    stack += StackFrame(
                        method = callee,
                        thisEntity = target,
                        parameters = parameters,
                    )
                }
            }
        }
        
        fun execute(jump: FoxJump) {
            when (jump) {
                is JumpGoto -> switchBlock(jump.block)
                is JumpBranch -> {
                    val condition = jump.condition.fetch() as FoxBool
                    switchBlock(if (condition.value) jump.thenBlock else jump.elseBlock)
                }
                is JumpReturn -> {
                    val value = jump.value.fetch()
                    completedReturn = value
                    stack.removeLast()
                    val top = stack.lastOrNull() ?: return
                    top.returnEntity = value
                }
            }
        }
    }
    
    class Heap {
        
        var pointer = 0
        val values = mutableMapOf<Int, FoxEntity>()
        
        fun reset() {
            pointer = 0
            values.clear()
        }
        
        fun allocate(value: FoxEntity): FoxRef {
            values[pointer] = value
            return FoxRef(pointer++)
        }
        
        fun get(ref: FoxRef): FoxEntity {
            return values[ref.referent] ?: error("Invalid reference")
        }
        
        fun gc(stackFrames: List<StackFrame>) {
            val reachable = mutableSetOf<Int>()
            for (frame in stackFrames) {
                for (value in frame.locals.values) {
                    reachable.collectReferences(value)
                }
            }
            val unreachable = values.keys.filter { it !in reachable }
            unreachable.forEach { values.remove(it) }
        }
        
        private fun MutableSet<Int>.collectReferences(value: FoxEntity) {
            when (value) {
                is FoxPrimitive -> {}
                is FoxArray -> value.elements.forEach { collectReferences(it) }
                is FoxTuple -> value.components.forEach { collectReferences(it) }
                is FoxStruct -> value.fields.values.forEach { collectReferences(it) }
                is FoxObject -> value.members.values.forEach { collectReferences(it) }
                is FoxEnum -> collectReferences(value.value)
                is FoxRef -> if (add(value.referent)) collectReferences(values[value.referent] ?: error("Invalid reference"))
                is FoxMethod -> {}
            }
        }
    }
    
    private fun FoxBuiltInMethodImplementation.invoke(frame: StackFrame, target: FoxEntity, params: Map<String, FoxEntity>): FoxEntity = when (this) {
        ByteToByte -> target
        ShortToByte -> FoxByte((target as FoxShort).value.toByte())
        IntToByte -> FoxByte((target as FoxInt).value.toByte())
        LongToByte -> FoxByte((target as FoxLong).value.toByte())
        FloatToByte -> FoxByte((target as FoxFloat).value.toInt().coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte())
        DoubleToByte -> FoxByte((target as FoxDouble).value.toInt().coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte())
        
        ByteToShort -> FoxShort((target as FoxByte).value.toShort())
        ShortToShort -> target
        IntToShort -> FoxShort((target as FoxInt).value.toShort())
        LongToShort -> FoxShort((target as FoxLong).value.toShort())
        FloatToShort -> FoxShort((target as FoxFloat).value.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        DoubleToShort -> FoxShort((target as FoxDouble).value.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        CharToShort -> FoxShort((target as FoxChar).value.code.toShort())
        
        ByteToInt -> FoxInt((target as FoxByte).value.toInt())
        ShortToInt -> FoxInt((target as FoxShort).value.toInt())
        IntToInt -> target
        LongToInt -> FoxInt((target as FoxLong).value.toInt())
        FloatToInt -> FoxInt((target as FoxFloat).value.toInt())
        DoubleToInt -> FoxInt((target as FoxDouble).value.toInt())
        CharToInt -> FoxInt((target as FoxChar).value.code)
        
        ByteToLong -> FoxLong((target as FoxByte).value.toLong())
        ShortToLong -> FoxLong((target as FoxShort).value.toLong())
        IntToLong -> FoxLong((target as FoxInt).value.toLong())
        LongToLong -> target
        FloatToLong -> FoxLong((target as FoxFloat).value.toLong())
        DoubleToLong -> FoxLong((target as FoxDouble).value.toLong())
        
        ByteToFloat -> FoxFloat((target as FoxByte).value.toFloat())
        ShortToFloat -> FoxFloat((target as FoxShort).value.toFloat())
        IntToFloat -> FoxFloat((target as FoxInt).value.toFloat())
        LongToFloat -> FoxFloat((target as FoxLong).value.toFloat())
        FloatToFloat -> target
        DoubleToFloat -> FoxFloat((target as FoxDouble).value.toFloat())
        
        ByteToDouble -> FoxDouble((target as FoxByte).value.toDouble())
        ShortToDouble -> FoxDouble((target as FoxShort).value.toDouble())
        IntToDouble -> FoxDouble((target as FoxInt).value.toDouble())
        LongToDouble -> FoxDouble((target as FoxLong).value.toDouble())
        FloatToDouble -> FoxDouble((target as FoxFloat).value.toDouble())
        DoubleToDouble -> target
        
        FloatAsInt -> FoxInt((target as FoxFloat).value.toRawBits())
        DoubleAsLong -> FoxLong((target as FoxDouble).value.toRawBits())
        
        ShortToChar -> FoxChar((target as FoxShort).value.toInt().toChar())
        IntToChar -> FoxChar((target as FoxInt).value.toChar())
        
        UnitToString -> FoxString("unit")
        BoolToString -> FoxString((target as FoxBool).value.toString())
        ByteToString -> FoxString((target as FoxByte).value.toString())
        ShortToString -> FoxString((target as FoxShort).value.toString())
        IntToString -> FoxString((target as FoxInt).value.toString())
        LongToString -> FoxString((target as FoxLong).value.toString())
        FloatToString -> FoxString((target as FoxFloat).value.toString())
        DoubleToString -> FoxString((target as FoxDouble).value.toString())
        CharToString -> FoxString((target as FoxChar).value.toString())
        StringToString -> target
        
        BoolNot -> FoxBool((target as FoxBool).value.not())
        ByteNot -> FoxByte((target as FoxByte).value.toInt().inv().toByte())
        ShortNot -> FoxShort((target as FoxShort).value.toInt().inv().toShort())
        IntNot -> FoxInt((target as FoxInt).value.inv())
        LongNot -> FoxLong((target as FoxLong).value.inv())
        
        ByteAnd -> FoxByte(((target as FoxByte).value.toInt() and (params["that"] as FoxByte).value.toInt()).toByte())
        ShortAnd -> FoxShort(((target as FoxShort).value.toInt() and (params["that"] as FoxShort).value.toInt()).toShort())
        IntAnd -> FoxInt((target as FoxInt).value and (params["that"] as FoxInt).value)
        LongAnd -> FoxLong((target as FoxLong).value and (params["that"] as FoxLong).value)
        
        ByteOr -> FoxByte(((target as FoxByte).value.toInt() or (params["that"] as FoxByte).value.toInt()).toByte())
        ShortOr -> FoxShort(((target as FoxShort).value.toInt() or (params["that"] as FoxShort).value.toInt()).toShort())
        IntOr -> FoxInt((target as FoxInt).value or (params["that"] as FoxInt).value)
        LongOr -> FoxLong((target as FoxLong).value or (params["that"] as FoxLong).value)
        
        ByteXor -> FoxByte(((target as FoxByte).value.toInt() xor (params["that"] as FoxByte).value.toInt()).toByte())
        ShortXor -> FoxShort(((target as FoxShort).value.toInt() xor (params["that"] as FoxShort).value.toInt()).toShort())
        IntXor -> FoxInt((target as FoxInt).value xor (params["that"] as FoxInt).value)
        LongXor -> FoxLong((target as FoxLong).value xor (params["that"] as FoxLong).value)
        
        ByteShl -> FoxByte(((target as FoxByte).value.toInt() shl (params["that"] as FoxInt).value).toByte())
        ShortShl -> FoxShort(((target as FoxShort).value.toInt() shl (params["that"] as FoxInt).value).toShort())
        IntShl -> FoxInt((target as FoxInt).value shl (params["that"] as FoxInt).value)
        LongShl -> FoxLong((target as FoxLong).value shl (params["that"] as FoxInt).value)
        
        ByteShr -> FoxByte(((target as FoxByte).value.toInt() shr (params["that"] as FoxInt).value).toByte())
        ShortShr -> FoxShort(((target as FoxShort).value.toInt() shr (params["that"] as FoxInt).value).toShort())
        IntShr -> FoxInt((target as FoxInt).value shr (params["that"] as FoxInt).value)
        LongShr -> FoxLong((target as FoxLong).value shr (params["that"] as FoxInt).value)
        
        ByteUshr -> FoxByte(((target as FoxByte).value.toInt() ushr (params["that"] as FoxInt).value).toByte())
        ShortUshr -> FoxShort(((target as FoxShort).value.toInt() ushr (params["that"] as FoxInt).value).toShort())
        IntUshr -> FoxInt((target as FoxInt).value ushr (params["that"] as FoxInt).value)
        LongUshr -> FoxLong((target as FoxLong).value ushr (params["that"] as FoxInt).value)
        
        ByteEq -> FoxBool((target as FoxByte).value == (params["that"] as FoxByte).value)
        ShortEq -> FoxBool((target as FoxShort).value == (params["that"] as FoxShort).value)
        IntEq -> FoxBool((target as FoxInt).value == (params["that"] as FoxInt).value)
        LongEq -> FoxBool((target as FoxLong).value == (params["that"] as FoxLong).value)
        FloatEq -> FoxBool((target as FoxFloat).value == (params["that"] as FoxFloat).value)
        DoubleEq -> FoxBool((target as FoxDouble).value == (params["that"] as FoxDouble).value)
        StringEq -> FoxBool((target as FoxString).value == (params["that"] as FoxString).value)
        
        ByteNeq -> FoxBool((target as FoxByte).value != (params["that"] as FoxByte).value)
        ShortNeq -> FoxBool((target as FoxShort).value != (params["that"] as FoxShort).value)
        IntNeq -> FoxBool((target as FoxInt).value != (params["that"] as FoxInt).value)
        LongNeq -> FoxBool((target as FoxLong).value != (params["that"] as FoxLong).value)
        FloatNeq -> FoxBool((target as FoxFloat).value != (params["that"] as FoxFloat).value)
        DoubleNeq -> FoxBool((target as FoxDouble).value != (params["that"] as FoxDouble).value)
        StringNeq -> FoxBool((target as FoxString).value != (params["that"] as FoxString).value)
        
        ByteGt -> FoxBool((target as FoxByte).value > (params["that"] as FoxByte).value)
        ShortGt -> FoxBool((target as FoxShort).value > (params["that"] as FoxShort).value)
        IntGt -> FoxBool((target as FoxInt).value > (params["that"] as FoxInt).value)
        LongGt -> FoxBool((target as FoxLong).value > (params["that"] as FoxLong).value)
        FloatGt -> FoxBool((target as FoxFloat).value > (params["that"] as FoxFloat).value)
        DoubleGt -> FoxBool((target as FoxDouble).value > (params["that"] as FoxDouble).value)
        
        ByteGte -> FoxBool((target as FoxByte).value >= (params["that"] as FoxByte).value)
        ShortGte -> FoxBool((target as FoxShort).value >= (params["that"] as FoxShort).value)
        IntGte -> FoxBool((target as FoxInt).value >= (params["that"] as FoxInt).value)
        LongGte -> FoxBool((target as FoxLong).value >= (params["that"] as FoxLong).value)
        FloatGte -> FoxBool((target as FoxFloat).value >= (params["that"] as FoxFloat).value)
        DoubleGte -> FoxBool((target as FoxDouble).value >= (params["that"] as FoxDouble).value)
        
        ByteLt -> FoxBool((target as FoxByte).value < (params["that"] as FoxByte).value)
        ShortLt -> FoxBool((target as FoxShort).value < (params["that"] as FoxShort).value)
        IntLt -> FoxBool((target as FoxInt).value < (params["that"] as FoxInt).value)
        LongLt -> FoxBool((target as FoxLong).value < (params["that"] as FoxLong).value)
        FloatLt -> FoxBool((target as FoxFloat).value < (params["that"] as FoxFloat).value)
        DoubleLt -> FoxBool((target as FoxDouble).value < (params["that"] as FoxDouble).value)
        
        ByteLte -> FoxBool((target as FoxByte).value <= (params["that"] as FoxByte).value)
        ShortLte -> FoxBool((target as FoxShort).value <= (params["that"] as FoxShort).value)
        IntLte -> FoxBool((target as FoxInt).value <= (params["that"] as FoxInt).value)
        LongLte -> FoxBool((target as FoxLong).value <= (params["that"] as FoxLong).value)
        FloatLte -> FoxBool((target as FoxFloat).value <= (params["that"] as FoxFloat).value)
        DoubleLte -> FoxBool((target as FoxDouble).value <= (params["that"] as FoxDouble).value)
        
        ByteAdd -> FoxByte(((target as FoxByte).value + (params["that"] as FoxByte).value).toByte())
        ShortAdd -> FoxShort(((target as FoxShort).value + (params["that"] as FoxShort).value).toShort())
        IntAdd -> FoxInt((target as FoxInt).value + (params["that"] as FoxInt).value)
        LongAdd -> FoxLong((target as FoxLong).value + (params["that"] as FoxLong).value)
        FloatAdd -> FoxFloat((target as FoxFloat).value + (params["that"] as FoxFloat).value)
        DoubleAdd -> FoxDouble((target as FoxDouble).value + (params["that"] as FoxDouble).value)
        StringAdd -> FoxString((target as FoxString).value + (params["that"] as FoxString).value)
        
        ByteSub -> FoxByte(((target as FoxByte).value - (params["that"] as FoxByte).value).toByte())
        ShortSub -> FoxShort(((target as FoxShort).value - (params["that"] as FoxShort).value).toShort())
        IntSub -> FoxInt((target as FoxInt).value - (params["that"] as FoxInt).value)
        LongSub -> FoxLong((target as FoxLong).value - (params["that"] as FoxLong).value)
        FloatSub -> FoxFloat((target as FoxFloat).value - (params["that"] as FoxFloat).value)
        DoubleSub -> FoxDouble((target as FoxDouble).value - (params["that"] as FoxDouble).value)
        
        ByteMul -> FoxByte(((target as FoxByte).value * (params["that"] as FoxByte).value).toByte())
        ShortMul -> FoxShort(((target as FoxShort).value * (params["that"] as FoxShort).value).toShort())
        IntMul -> FoxInt((target as FoxInt).value * (params["that"] as FoxInt).value)
        LongMul -> FoxLong((target as FoxLong).value * (params["that"] as FoxLong).value)
        FloatMul -> FoxFloat((target as FoxFloat).value * (params["that"] as FoxFloat).value)
        DoubleMul -> FoxDouble((target as FoxDouble).value * (params["that"] as FoxDouble).value)
        
        ByteDiv -> {
            val divisor = (params["that"] as FoxByte).value
            if (divisor == 0.toByte()) {
                frame.panic("Division by zero")
                FoxUnit
            } else FoxEnum("Result", FoxByte(((target as FoxByte).value / divisor).toByte()))
        }
        ShortDiv -> {
            val divisor = (params["that"] as FoxShort).value
            if (divisor == 0.toShort()) {
                frame.panic("Division by zero")
                FoxUnit
            } else FoxEnum("Result", FoxShort(((target as FoxShort).value / divisor).toShort()))
        }
        IntDiv -> {
            val divisor = (params["that"] as FoxInt).value
            if (divisor == 0) {
                frame.panic("Division by zero")
                FoxUnit
            } else FoxEnum("Result", FoxInt((target as FoxInt).value / divisor))
        }
        LongDiv -> {
            val divisor = (params["that"] as FoxLong).value
            if (divisor == 0L) {
                frame.panic("Division by zero")
                FoxUnit
            } else FoxEnum("Result", FoxLong((target as FoxLong).value / divisor))
        }
        FloatDiv -> FoxFloat((target as FoxFloat).value / (params["that"] as FoxFloat).value)
        DoubleDiv -> FoxDouble((target as FoxDouble).value / (params["that"] as FoxDouble).value)
        
        ByteRem -> {
            val divisor = (params["that"] as FoxByte).value
            if (divisor == 0.toByte()) {
                frame.panic("Division by zero")
                FoxUnit
            } else FoxEnum("Result", FoxByte(((target as FoxByte).value % divisor).toByte()))
        }
        ShortRem -> {
            val divisor = (params["that"] as FoxShort).value
            if (divisor == 0.toShort()) {
                frame.panic("Division by zero")
                FoxUnit
            } else FoxEnum("Result", FoxShort(((target as FoxShort).value % divisor).toShort()))
        }
        IntRem -> {
            val divisor = (params["that"] as FoxInt).value
            if (divisor == 0) {
                frame.panic("Division by zero")
                FoxUnit
            } else FoxEnum("Result", FoxInt((target as FoxInt).value % divisor))
        }
        LongRem -> {
            val divisor = (params["that"] as FoxLong).value
            if (divisor == 0L) {
                frame.panic("Division by zero")
                FoxUnit
            } else FoxEnum("Result", FoxLong((target as FoxLong).value % divisor))
        }
        FloatRem -> FoxFloat((target as FoxFloat).value % (params["that"] as FoxFloat).value)
        DoubleRem -> FoxDouble((target as FoxDouble).value % (params["that"] as FoxDouble).value)
        
        ByteCompareTo -> FoxInt((target as FoxByte).value.compareTo((params["that"] as FoxByte).value))
        ShortCompareTo -> FoxInt((target as FoxShort).value.compareTo((params["that"] as FoxShort).value))
        IntCompareTo -> FoxInt((target as FoxInt).value.compareTo((params["that"] as FoxInt).value))
        LongCompareTo -> FoxInt((target as FoxLong).value.compareTo((params["that"] as FoxLong).value))
        FloatCompareTo -> FoxInt((target as FoxFloat).value.compareTo((params["that"] as FoxFloat).value))
        DoubleCompareTo -> FoxInt((target as FoxDouble).value.compareTo((params["that"] as FoxDouble).value))
        StringCompareTo -> FoxInt((target as FoxString).value.compareTo((params["that"] as FoxString).value))
    }
}
