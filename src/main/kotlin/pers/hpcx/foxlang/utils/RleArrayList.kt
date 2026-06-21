package pers.hpcx.foxlang.utils

class RleArrayList<E>() : AbstractMutableList<E>() {
    
    private val runs = mutableListOf<Run<E>>()
    private var totalSize = 0
    private var mutationCount = 0
    private var hashCodeDirty = true
    private var cachedHashCode = 1
    
    constructor(elements: Iterable<E>) : this() {
        elements.forEach(::append)
    }
    
    companion object {
        fun <E> fromRuns(runs: Iterable<Pair<E, Int>>): RleArrayList<E> {
            val list = RleArrayList<E>()
            runs.forEach { (element, count) ->
                list.appendRun(element, count)
            }
            return list
        }
    }
    
    override val size: Int
        get() = totalSize
    
    override fun get(index: Int): E {
        checkElementIndex(index)
        val (runIndex, _) = locate(index)
        return runs[runIndex].value
    }
    
    override fun add(index: Int, element: E) {
        checkPositionIndex(index)
        if (index == size) {
            append(element)
            return
        }
        
        val (runIndex, offset) = locate(index)
        val run = runs[runIndex]
        when {
            run.value == element -> run.length++
            offset == 0 -> {
                if (runIndex > 0 && runs[runIndex - 1].value == element) {
                    runs[runIndex - 1].length++
                } else {
                    runs.add(runIndex, Run(element, 1))
                }
            }
            else -> {
                val rightLength = run.length - offset
                run.length = offset
                runs.add(runIndex + 1, Run(element, 1))
                runs.add(runIndex + 2, Run(run.value, rightLength))
            }
        }
        totalSize++
        markChanged()
    }
    
    override fun set(index: Int, element: E): E {
        val previous = get(index)
        if (previous == element) return previous
        val previousMutationCount = mutationCount
        removeAt(index)
        add(index, element)
        mutationCount = previousMutationCount + 1
        hashCodeDirty = true
        return previous
    }
    
    override fun removeAt(index: Int): E {
        checkElementIndex(index)
        val (runIndex, _) = locate(index)
        val run = runs[runIndex]
        val previous = run.value
        
        run.length--
        if (run.length == 0) {
            runs.removeAt(runIndex)
            mergeNeighborsAround(runIndex)
        }
        
        totalSize--
        markChanged()
        return previous
    }
    
    override fun clear() {
        if (isEmpty()) return
        runs.clear()
        totalSize = 0
        markChanged()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is List<*>) return false
        if (size != other.size) return false
        if (other is RleArrayList<*>) {
            return runs == other.runs
        }
        return elementsEqual(other)
    }
    
    override fun hashCode(): Int {
        if (!hashCodeDirty) return cachedHashCode
        var result = 1
        for (run in runs) {
            repeat(run.length) {
                result = 31 * result + (run.value?.hashCode() ?: 0)
            }
        }
        cachedHashCode = result
        hashCodeDirty = false
        return result
    }
    
    override fun iterator(): MutableIterator<E> = listIterator()
    
    override fun listIterator(): MutableListIterator<E> = listIterator(0)
    
    override fun listIterator(index: Int): MutableListIterator<E> {
        checkPositionIndex(index)
        return RleListIterator(index)
    }
    
    private fun append(element: E) {
        if (runs.isNotEmpty() && runs.last().value == element) {
            runs.last().length++
        } else {
            runs.add(Run(element, 1))
        }
        totalSize++
        markChanged()
    }
    
    private fun appendRun(element: E, count: Int) {
        require(count > 0) { "Run length must be positive: $count" }
        if (runs.isNotEmpty() && runs.last().value == element) {
            runs.last().length = Math.addExact(runs.last().length, count)
        } else {
            runs.add(Run(element, count))
        }
        totalSize = Math.addExact(totalSize, count)
        markChanged()
    }
    
    private fun locate(index: Int): Position {
        var remaining = index
        for ((runIndex, run) in runs.withIndex()) {
            if (remaining < run.length) return Position(runIndex, remaining)
            remaining -= run.length
        }
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
    }
    
    private fun mergeNeighborsAround(index: Int) {
        if (index <= 0 || index >= runs.size) return
        val left = runs[index - 1]
        val right = runs[index]
        if (left.value != right.value) return
        left.length += right.length
        runs.removeAt(index)
    }
    
    private fun checkElementIndex(index: Int) {
        if (index !in indices) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
    }
    
    private fun checkPositionIndex(index: Int) {
        if (index !in 0..size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
    }
    
    private fun positionAt(index: Int): Position {
        if (index == size) return Position(runs.size, 0)
        return locate(index)
    }
    
    private fun elementsEqual(other: List<*>): Boolean {
        val left = iterator()
        val right = other.iterator()
        while (left.hasNext() && right.hasNext()) {
            if (left.next() != right.next()) return false
        }
        return !left.hasNext() && !right.hasNext()
    }
    
    private fun markChanged() {
        mutationCount++
        hashCodeDirty = true
    }
    
    private data class Run<E>(
        val value: E,
        var length: Int,
    )
    
    private data class Position(
        val runIndex: Int,
        val offset: Int,
    )
    
    private inner class RleListIterator(startIndex: Int) : MutableListIterator<E> {
        private var expectedMutationCount = mutationCount
        private var nextIndex = startIndex
        private var nextPosition = positionAt(startIndex)
        private var lastReturnedIndex = -1
        
        override fun hasNext(): Boolean = nextIndex < size
        
        override fun next(): E {
            checkForConcurrentModification()
            if (!hasNext()) throw NoSuchElementException()
            
            val value = runs[nextPosition.runIndex].value
            lastReturnedIndex = nextIndex
            nextIndex++
            advanceForward()
            return value
        }
        
        override fun hasPrevious(): Boolean = nextIndex > 0
        
        override fun previous(): E {
            checkForConcurrentModification()
            if (!hasPrevious()) throw NoSuchElementException()
            
            retreatBackward()
            nextIndex--
            lastReturnedIndex = nextIndex
            return runs[nextPosition.runIndex].value
        }
        
        override fun nextIndex(): Int = nextIndex
        
        override fun previousIndex(): Int = nextIndex - 1
        
        override fun remove() {
            checkForConcurrentModification()
            check(lastReturnedIndex >= 0) { "Call next() or previous() before remove()" }
            
            this@RleArrayList.removeAt(lastReturnedIndex)
            if (lastReturnedIndex < nextIndex) {
                nextIndex--
            }
            syncAfterMutation()
        }
        
        override fun add(element: E) {
            checkForConcurrentModification()
            
            this@RleArrayList.add(nextIndex, element)
            nextIndex++
            syncAfterMutation()
        }
        
        override fun set(element: E) {
            checkForConcurrentModification()
            check(lastReturnedIndex >= 0) { "Call next() or previous() before set()" }
            
            this@RleArrayList[lastReturnedIndex] = element
            syncAfterMutation()
        }
        
        private fun advanceForward() {
            if (nextIndex == size) {
                nextPosition = Position(runs.size, 0)
                return
            }
            
            val run = runs[nextPosition.runIndex]
            nextPosition = if (nextPosition.offset + 1 < run.length) {
                Position(nextPosition.runIndex, nextPosition.offset + 1)
            } else {
                Position(nextPosition.runIndex + 1, 0)
            }
        }
        
        private fun retreatBackward() {
            if (nextIndex == size) {
                val lastRunIndex = runs.lastIndex
                nextPosition = Position(lastRunIndex, runs[lastRunIndex].length - 1)
                return
            }
            
            nextPosition = if (nextPosition.offset > 0) {
                Position(nextPosition.runIndex, nextPosition.offset - 1)
            } else {
                val previousRunIndex = nextPosition.runIndex - 1
                Position(previousRunIndex, runs[previousRunIndex].length - 1)
            }
        }
        
        private fun syncAfterMutation() {
            expectedMutationCount = mutationCount
            nextPosition = positionAt(nextIndex)
            lastReturnedIndex = -1
        }
        
        private fun checkForConcurrentModification() {
            if (expectedMutationCount != mutationCount) {
                throw ConcurrentModificationException()
            }
        }
    }
}

fun <E> emptyRleArrayList(): RleArrayList<E> = RleArrayList()

fun <E> rleArrayListOf(vararg elements: E): RleArrayList<E> = RleArrayList(elements.asIterable())

fun <E> rleArrayListOfRuns(vararg runs: Pair<E, Int>): RleArrayList<E> = RleArrayList.fromRuns(runs.asIterable())

fun <E> Iterable<E>.toRleArrayList(): RleArrayList<E> = RleArrayList(this)

fun <E> Iterable<Pair<E, Int>>.toRleArrayListFromRuns(): RleArrayList<E> = RleArrayList.fromRuns(this)
