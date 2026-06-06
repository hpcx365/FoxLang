package pers.hpcx.foxlang.types

sealed interface FoxType
sealed interface FoxConcreteType : FoxType
sealed interface FoxPrimitiveType : FoxConcreteType

object FoxVoidType : FoxPrimitiveType
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
sealed interface FoxTupleTemplateItem
data class FoxTupleTypeTemplateItem(val type: FoxType) : FoxTupleTemplateItem
data class FoxTupleSpreadTemplateItem(val type: FoxType) : FoxTupleTemplateItem
data class FoxTupleTemplateType(val items: List<FoxTupleTemplateItem>) : FoxType
data class FoxNamedProjectionType(val baseType: FoxType) : FoxType
data class FoxStructType(val fields: Map<String, FoxConcreteType>) : FoxConcreteType
sealed interface FoxStructTemplateItem
data class FoxStructFieldTemplateItem(val name: String, val type: FoxType) : FoxStructTemplateItem
data class FoxStructSpreadTemplateItem(val type: FoxType) : FoxStructTemplateItem
data class FoxStructTemplateType(val items: List<FoxStructTemplateItem>) : FoxType
data class FoxDenamedProjectionType(val baseType: FoxType) : FoxType
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
object FoxStructWildcardConstraint : FoxGenericConstraint

sealed interface FoxSemanticPlaceholder

data class FoxSplatParameterPlaceholder(
    val type: FoxType,
) : FoxSemanticPlaceholder

data class FoxGenForPlaceholder(
    val valueName: String,
    val typeName: String,
    val targetType: FoxType,
) : FoxSemanticPlaceholder

fun FoxType.collectGenerics(result: MutableSet<String> = mutableSetOf()): Set<String> {
    when (this) {
        is FoxConcreteType -> {}
        is FoxGenericType -> result.add(name)
        is FoxGenericArrayType -> elementType.collectGenerics(result)
        is FoxGenericTupleType -> componentTypes.forEach { it.collectGenerics(result) }
        is FoxTupleTemplateType -> items.forEach { item ->
            when (item) {
                is FoxTupleTypeTemplateItem -> item.type.collectGenerics(result)
                is FoxTupleSpreadTemplateItem -> item.type.collectGenerics(result)
            }
        }
        is FoxNamedProjectionType -> baseType.collectGenerics(result)
        is FoxGenericStructType -> fields.values.forEach { it.collectGenerics(result) }
        is FoxStructTemplateType -> items.forEach { item ->
            when (item) {
                is FoxStructFieldTemplateItem -> item.type.collectGenerics(result)
                is FoxStructSpreadTemplateItem -> item.type.collectGenerics(result)
            }
        }
        is FoxDenamedProjectionType -> baseType.collectGenerics(result)
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
        is FoxTupleTemplateType -> {
            return FoxTupleTemplateType(
                items.map { item ->
                    when (item) {
                        is FoxTupleTypeTemplateItem -> FoxTupleTypeTemplateItem(item.type.replaceGenerics(replacements))
                        is FoxTupleSpreadTemplateItem -> FoxTupleSpreadTemplateItem(item.type.replaceGenerics(replacements))
                    }
                },
            )
        }
        is FoxNamedProjectionType -> {
            return FoxNamedProjectionType(baseType.replaceGenerics(replacements))
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
        is FoxStructTemplateType -> {
            return FoxStructTemplateType(
                items.map { item ->
                    when (item) {
                        is FoxStructFieldTemplateItem -> FoxStructFieldTemplateItem(item.name, item.type.replaceGenerics(replacements))
                        is FoxStructSpreadTemplateItem -> FoxStructSpreadTemplateItem(item.type.replaceGenerics(replacements))
                    }
                },
            )
        }
        is FoxDenamedProjectionType -> {
            return FoxDenamedProjectionType(baseType.replaceGenerics(replacements))
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

fun FoxType.resolveStructuralProjections(
    allocateNamedField: ((index: Int) -> String)? = null,
): FoxType = when (val normalized = normalizeType(allocateNamedField)) {
    is Normalized -> normalized.value
    is NormalizationError -> this
}
