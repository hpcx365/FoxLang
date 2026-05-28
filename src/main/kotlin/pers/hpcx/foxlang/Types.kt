package pers.hpcx.foxlang

sealed interface FoxType
sealed interface FoxConcreteType : FoxType
sealed interface FoxPrimitiveType : FoxConcreteType

object FoxUnitType : FoxPrimitiveType
object FoxBoolType : FoxPrimitiveType
object FoxByteType : FoxPrimitiveType
object FoxShortType : FoxPrimitiveType
object FoxIntType : FoxPrimitiveType
object FoxLongType : FoxPrimitiveType
object FoxFloatType : FoxPrimitiveType
object FoxDoubleType : FoxPrimitiveType
object FoxCharType : FoxPrimitiveType
object FoxStringType : FoxPrimitiveType

data class FoxArrayType(val elementType: FoxConcreteType) : FoxConcreteType
data class FoxTupleType(val componentTypes: List<FoxConcreteType>) : FoxConcreteType
data class FoxStructType(val fields: Map<String, FoxConcreteType>) : FoxConcreteType
data class FoxEnumType(val items: Map<String, FoxConcreteType>) : FoxConcreteType
data class FoxRefType(val referentType: FoxConcreteType) : FoxConcreteType
data class FoxLambdaType(
    val thisType: FoxConcreteType,
    val parameters: List<FoxConcreteType>,
    val returnType: FoxConcreteType,
) : FoxConcreteType

data class FoxGenericType(val name: String) : FoxType
data class FoxGenericArrayType(val elementType: FoxType) : FoxType
data class FoxGenericTupleType(val componentTypes: List<FoxType>) : FoxType
data class FoxGenericStructType(val fields: Map<String, FoxType>) : FoxType
data class FoxGenericEnumType(val items: Map<String, FoxType>) : FoxType
data class FoxGenericRefType(val referentType: FoxType) : FoxType
data class FoxGenericLambdaType(
    val thisType: FoxType,
    val parameters: List<FoxType>,
    val returnType: FoxType,
) : FoxType

sealed interface FoxGenericConstraint
object FoxAnyConstraint : FoxGenericConstraint
data class FoxExactMatchConstraint(val type: FoxConcreteType) : FoxGenericConstraint

fun FoxType.collectGenerics(result: MutableSet<String> = mutableSetOf()): Set<String> {
    when (this) {
        is FoxConcreteType -> {}
        is FoxGenericType -> result.add(name)
        is FoxGenericArrayType -> elementType.collectGenerics(result)
        is FoxGenericTupleType -> componentTypes.forEach { it.collectGenerics(result) }
        is FoxGenericStructType -> fields.values.forEach { it.collectGenerics(result) }
        is FoxGenericEnumType -> items.values.forEach { it.collectGenerics(result) }
        is FoxGenericRefType -> referentType.collectGenerics(result)
        is FoxGenericLambdaType -> {
            thisType.collectGenerics(result)
            parameters.forEach { it.collectGenerics(result) }
            returnType.collectGenerics(result)
        }
    }
    return result
}

fun FoxType.replaceGenerics(replacements: Map<String, FoxConcreteType>): FoxType {
    when (this) {
        is FoxConcreteType -> return this
        is FoxGenericType -> return replacements[name] ?: this
        is FoxGenericArrayType -> {
            val newElementType = elementType.replaceGenerics(replacements)
            if (newElementType is FoxConcreteType) return FoxArrayType(newElementType)
            return FoxGenericArrayType(newElementType)
        }
        is FoxGenericTupleType -> {
            val newElementTypes = componentTypes.map { it.replaceGenerics(replacements) }
            val newElementTypes2 = buildList {
                newElementTypes.forEach {
                    if (it is FoxConcreteType) add(it)
                    else return FoxGenericTupleType(newElementTypes)
                }
            }
            return FoxTupleType(newElementTypes2)
        }
        is FoxGenericStructType -> {
            val newFields = fields.mapValues { it.value.replaceGenerics(replacements) }
            val newFields2 = buildMap {
                newFields.forEach {
                    val value = it.value
                    if (value is FoxConcreteType) put(it.key, value)
                    else return FoxGenericStructType(newFields)
                }
            }
            return FoxStructType(newFields2)
        }
        is FoxGenericEnumType -> {
            val newFields = items.mapValues { it.value.replaceGenerics(replacements) }
            val newFields2 = buildMap {
                newFields.forEach {
                    val value = it.value
                    if (value is FoxConcreteType) put(it.key, value)
                    else return FoxGenericEnumType(newFields)
                }
            }
            return FoxEnumType(newFields2)
        }
        is FoxGenericRefType -> {
            val newReferentType = referentType.replaceGenerics(replacements)
            if (newReferentType is FoxConcreteType) return FoxRefType(newReferentType)
            return FoxGenericRefType(newReferentType)
        }
        is FoxGenericLambdaType -> {
            val newThisType = thisType.replaceGenerics(replacements)
            val newParameters = parameters.map { it.replaceGenerics(replacements) }
            val newReturnType = returnType.replaceGenerics(replacements)
            val newParameters2 = buildList {
                newParameters.forEach {
                    if (it is FoxConcreteType) add(it)
                    else return FoxGenericLambdaType(newThisType, newParameters, newReturnType)
                }
            }
            if (newThisType is FoxConcreteType && newReturnType is FoxConcreteType) {
                return FoxLambdaType(newThisType, newParameters2, newReturnType)
            }
            return FoxGenericLambdaType(newThisType, newParameters2, newReturnType)
        }
    }
}
