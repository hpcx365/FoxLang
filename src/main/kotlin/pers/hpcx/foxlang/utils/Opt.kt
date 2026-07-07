package pers.hpcx.foxlang.utils

fun <T> some(value: T) = Opt.of(value)
fun <T> none(): Opt<T> = Opt.empty()

@Suppress("UNCHECKED_CAST")
class Opt<T> private constructor(
    private val value: T?,
    private val present: Boolean,
) {
    
    fun isEmpty() = !present
    fun isPresent() = present
    
    fun value(): T = if (present) value as T else throw NoSuchElementException("No value present")
    fun valueOrNull(): T? = if (present) value as T else null
    
    fun fallback(default: T): T = if (present) value as T else default
    fun fallback(lazyDefault: () -> T): T = if (present) value as T else lazyDefault()
    
    inline fun <R> ifEmpty(block: () -> R): Opt<R> = if (isEmpty()) some(block()) else none()
    inline fun <R> ifPresent(block: (T) -> R): Opt<R> = if (isPresent()) some(block(value())) else none()
    
    companion object {
        
        private val EMPTY = Opt(null, false)
        
        fun <T> empty(): Opt<T> = EMPTY as Opt<T>
        fun <T> of(value: T): Opt<T> = Opt(value, true)
    }
    
    override fun toString(): String = if (present) "Some($value)" else "None"
    
    override fun hashCode() = if (present) value.hashCode() else 0
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Opt<*>) return false
        if (present) return other.present && value == other.value
        return !other.present
    }
}
