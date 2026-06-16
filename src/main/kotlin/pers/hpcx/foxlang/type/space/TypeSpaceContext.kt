package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.arity
import pers.hpcx.foxlang.type.componentAt
import pers.hpcx.foxlang.type.family
import pers.hpcx.foxlang.type.toFoxTupleType
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet

typealias TypeSpace = Space<FoxType, TypeSpaceContext>
typealias TraversableTypeSpace = TraversableSpace<FoxType, TypeSpaceContext>

data class TypeBounds(
    val maxHeight: Int,
    val maxTupleArity: Int,
    val maxStructArity: Int,
    val nameDictionary: OrderedSet<String>,
)

class NameSpaceContextView(
    val context: TypeSpaceContext,
) : SpaceContext<String> {
    override fun compare(left: String, right: String) = context.compareNames(left, right)
}

class StructFieldSpaceContextView(
    val context: TypeSpaceContext,
) : SpaceContext<Pair<String, FoxType>> {
    override fun compare(left: Pair<String, FoxType>, right: Pair<String, FoxType>) = context.compareEntries(left, right)
}

class TypeSpaceContext(
    val bounds: TypeBounds,
) : SpaceContext<FoxType> {
    
    val nameSpaceContext = NameSpaceContextView(this)
    val structFieldSpaceContext = StructFieldSpaceContextView(this)
    
    override fun compare(left: FoxType, right: FoxType): Int {
        val familyComparison = left.family().compareTo(right.family())
        if (familyComparison != 0) return familyComparison
        
        return when (left) {
            is FoxPrimitiveType -> 0
            is FoxBuiltInType -> when (left) {
                is FoxTupleType -> compareTupleComponents(left, right as FoxTupleType)
                is FoxStructType -> compareOrderedEntries(left.fields, (right as FoxStructType).fields)
                is FoxObjectType -> compareNamedTypes(left.members, (right as FoxObjectType).members)
                is FoxEnumType -> compareNamedTypes(left.items, (right as FoxEnumType).items)
                is FoxArrayType -> compare(left.element, (right as FoxArrayType).element)
                is FoxRefType -> compare(left.referent, (right as FoxRefType).referent)
                is FoxMethodType -> compareMethods(left, right as FoxMethodType)
            }
            else -> error("Only concrete types can be compared: $left")
        }
    }
    
    fun compareOrderedEntries(left: OrderedMap<String, FoxType>, right: OrderedMap<String, FoxType>): Int {
        val leftSize = left.size
        val rightSize = right.size
        if (leftSize != rightSize) return leftSize.compareTo(rightSize)
        
        (0 until leftSize).forEach { index ->
            val leftEntry = left.entryAt(index).let { it.key to it.value }
            val rightEntry = right.entryAt(index).let { it.key to it.value }
            val entryComparison = compareEntries(leftEntry, rightEntry)
            if (entryComparison != 0) return entryComparison
        }
        
        return 0
    }
    
    fun compareEntries(left: Pair<String, FoxType>, right: Pair<String, FoxType>): Int {
        val nameComparison = compareNames(left.first, right.first)
        if (nameComparison != 0) return nameComparison
        val typeComparison = compare(left.second, right.second)
        if (typeComparison != 0) return typeComparison
        return 0
    }
    
    fun compareNames(left: String, right: String): Int {
        if (left == right) return 0
        val leftIndex = bounds.nameDictionary.indexOf(left)
        val rightIndex = bounds.nameDictionary.indexOf(right)
        check(leftIndex >= 0)
        check(rightIndex >= 0)
        return leftIndex.compareTo(rightIndex)
    }
    
    fun compareTupleComponents(left: FoxTupleType, right: FoxTupleType): Int {
        val leftSize = left.arity
        val rightSize = right.arity
        if (leftSize != rightSize) return leftSize.compareTo(rightSize)
        if (leftSize == 0) return 0
        
        val normalizedLeft = left.components.toFoxTupleType()
        val normalizedRight = right.components.toFoxTupleType()
        if (normalizedLeft == normalizedRight) return 0
        
        var group = 0
        var index = 0
        while (true) {
            val leftComponent = normalizedLeft.components[group]
            val rightComponent = normalizedRight.components[group]
            val typeComparison = compare(leftComponent.first, rightComponent.first)
            if (typeComparison != 0) return typeComparison
            val leftCount = leftComponent.second
            val rightCount = rightComponent.second
            when {
                leftCount > rightCount -> {
                    val different = index + rightCount
                    return compare(normalizedLeft.componentAt(different), normalizedRight.componentAt(different))
                }
                leftCount < rightCount -> {
                    val different = index + leftCount
                    return compare(normalizedLeft.componentAt(different), normalizedRight.componentAt(different))
                }
                else -> {
                    group++
                    index += leftCount
                }
            }
        }
    }
    
    fun compareNamedTypes(left: Map<String, FoxType>, right: Map<String, FoxType>): Int {
        bounds.nameDictionary.elements.asReversed().forEach { name ->
            val leftPresent = left.containsKey(name)
            val rightPresent = right.containsKey(name)
            val presenceComparison = (if (leftPresent) 1 else 0).compareTo(if (rightPresent) 1 else 0)
            if (presenceComparison != 0) return presenceComparison
            if (leftPresent) {
                val typeComparison = compare(left.getValue(name), right.getValue(name))
                if (typeComparison != 0) return typeComparison
            }
        }
        return 0
    }
    
    fun compareMethods(left: FoxMethodType, right: FoxMethodType): Int {
        val thisComparison = compare(left.`this`, right.`this`)
        if (thisComparison != 0) return thisComparison
        val parameterComparison = compareOrderedEntries(left.parameters, right.parameters)
        if (parameterComparison != 0) return parameterComparison
        return compare(left.`return`, right.`return`)
    }
}
