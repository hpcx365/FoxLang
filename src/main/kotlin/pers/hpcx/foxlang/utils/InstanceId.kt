package pers.hpcx.foxlang.utils

import java.util.concurrent.atomic.AtomicLong

@JvmInline
value class InstanceId internal constructor(val value: Long) : Comparable<InstanceId> {
    override fun compareTo(other: InstanceId) = value.compareTo(other.value)
}

private object InstanceIdAllocator {
    private var id = AtomicLong()
    fun newId() = InstanceId(id.getAndIncrement())
}

open class WithInstanceId {
    val instanceId = InstanceIdAllocator.newId()
}
