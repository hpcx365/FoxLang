package pers.hpcx.foxlang.type.space

import pers.hpcx.foxlang.ast.*
import pers.hpcx.foxlang.type.arity
import pers.hpcx.foxlang.type.componentAt
import pers.hpcx.foxlang.type.family
import pers.hpcx.foxlang.type.toFoxTupleType
import pers.hpcx.foxlang.utils.OrderedSet

typealias NameSpace = TraversableSpace<String, NameSpaceContextView>
typealias StructField = Pair<String, FoxType>
typealias TypeSpace = Space<FoxType, TypeSpaceContext>
typealias TraversableTypeSpace = TraversableSpace<FoxType, TypeSpaceContext>
typealias ProjectionTypeSpace = ProjectionSpace<FoxType, TypeSpaceContext>

enum class NameDictionary {
    StructFieldNames,
    ObjectMemberNames,
    EnumItemNames,
}

class NameSpaceContextView(
    val context: TypeSpaceContext,
    val dictionary: NameDictionary,
) : SpaceContext<String> {
    override fun compare(left: String, right: String): Int {
        return context.compareNames(left, right, dictionary)
    }
}

class StructFieldSpaceContextView(
    val context: TypeSpaceContext,
) : SpaceContext<StructField> {
    override fun compare(left: StructField, right: StructField): Int {
        return context.compareStructField(left, right)
    }
}

class TypeSpaceContext(
    val structFieldNames: OrderedSet<String>,
    val objectMemberNames: OrderedSet<String>,
    val enumItemNames: OrderedSet<String>,
) : SpaceContext<FoxType> {
    
    val structFieldNameSpaceContext = NameSpaceContextView(this, NameDictionary.StructFieldNames)
    val objectMemberNameSpaceContext = NameSpaceContextView(this, NameDictionary.ObjectMemberNames)
    val enumItemNameSpaceContext = NameSpaceContextView(this, NameDictionary.EnumItemNames)
    
    val structFieldSpaceContext = StructFieldSpaceContextView(this)
    
    override fun compare(left: FoxType, right: FoxType): Int {
        val familyComparison = left.family().compareTo(right.family())
        if (familyComparison != 0) return familyComparison
        
        return when (left) {
            is FoxPrimitiveType -> 0
            is FoxBuiltInType -> when (left) {
                is FoxTupleType -> compareTupleComponents(left, right as FoxTupleType)
                is FoxStructType -> compareStruct(left, right as FoxStructType)
                is FoxObjectType -> compareNamedTypes(left.members, (right as FoxObjectType).members, NameDictionary.ObjectMemberNames)
                is FoxEnumType -> compareNamedTypes(left.items, (right as FoxEnumType).items, NameDictionary.EnumItemNames)
                is FoxArrayType -> compare(left.element, (right as FoxArrayType).element)
                is FoxRefType -> compare(left.referent, (right as FoxRefType).referent)
                is FoxMethodType -> compareMethods(left, right as FoxMethodType)
            }
            else -> error("Only concrete types can be compared: $left")
        }
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
    
    fun compareStruct(left: FoxStructType, right: FoxStructType): Int {
        val leftSize = left.arity
        val rightSize = right.arity
        if (leftSize != rightSize) return leftSize.compareTo(rightSize)
        
        (0 until leftSize).forEach { index ->
            val leftEntry = left.fields.entryAt(index).let { it.key to it.value }
            val rightEntry = right.fields.entryAt(index).let { it.key to it.value }
            val entryComparison = compareStructField(leftEntry, rightEntry)
            if (entryComparison != 0) return entryComparison
        }
        
        return 0
    }
    
    fun compareStructField(left: StructField, right: StructField): Int {
        val nameComparison = compareNames(left.first, right.first, NameDictionary.StructFieldNames)
        if (nameComparison != 0) return nameComparison
        val typeComparison = compare(left.second, right.second)
        if (typeComparison != 0) return typeComparison
        return 0
    }
    
    fun compareNames(left: String, right: String, dictionary: NameDictionary): Int {
        val names = namesOf(dictionary)
        val leftIndex = names.indexOf(left).also { check(it >= 0) { "Invalid name: $left" } }
        val rightIndex = names.indexOf(right).also { check(it >= 0) { "Invalid name: $right" } }
        return leftIndex.compareTo(rightIndex)
    }
    
    fun compareNamedTypes(left: Map<String, FoxType>, right: Map<String, FoxType>, dictionary: NameDictionary): Int {
        namesOf(dictionary).elements.asReversed().forEach { name ->
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
        val parameterComparison = compareStruct(FoxStructType(left.parameters), FoxStructType(right.parameters))
        if (parameterComparison != 0) return parameterComparison
        return compare(left.`return`, right.`return`)
    }
    
    fun namesOf(dictionary: NameDictionary) = when (dictionary) {
        NameDictionary.StructFieldNames -> structFieldNames
        NameDictionary.ObjectMemberNames -> objectMemberNames
        NameDictionary.EnumItemNames -> enumItemNames
    }
}
