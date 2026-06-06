package pers.hpcx.foxlang.types

sealed interface NormalizationResult<out T>

data class Normalized<T>(
    val value: T,
) : NormalizationResult<T>

data class NormalizationError(
    val message: String,
) : NormalizationResult<Nothing>

data class NormalizedMethodParameter(
    val name: String,
    val type: FoxType,
)

fun FoxType.normalizeType(
    freshFieldName: ((index: Int) -> String)? = null,
): NormalizationResult<FoxType> = when (this) {
    is FoxConcreteType -> when (this) {
        is FoxArrayType -> when (val normalized = elementType.normalizeType(freshFieldName)) {
            is Normalized -> {
                val value = normalized.value
                if (value is FoxConcreteType) Normalized(FoxArrayType(value))
                else NormalizationError("Array element type '$value' did not normalize to a concrete type")
            }
            is NormalizationError -> normalized
        }
        is FoxTupleType -> Normalized(
            FoxTupleType(
                componentTypes.map { component ->
                    when (val normalized = component.normalizeType(freshFieldName)) {
                        is Normalized -> normalized.value as FoxConcreteType
                        is NormalizationError -> return normalized
                    }
                },
            ),
        )
        is FoxStructType -> {
            val normalizedFields = LinkedHashMap<String, FoxConcreteType>()
            for ((name, type) in fields) {
                when (val normalized = type.normalizeType(freshFieldName)) {
                    is Normalized -> normalizedFields[name] = normalized.value as FoxConcreteType
                    is NormalizationError -> return normalized
                }
            }
            Normalized(FoxStructType(normalizedFields))
        }
        is FoxEnumType -> {
            val normalizedItems = LinkedHashMap<String, FoxConcreteType>()
            for ((name, type) in items) {
                when (val normalized = type.normalizeType(freshFieldName)) {
                    is Normalized -> normalizedItems[name] = normalized.value as FoxConcreteType
                    is NormalizationError -> return normalized
                }
            }
            Normalized(FoxEnumType(normalizedItems))
        }
        is FoxRefType -> when (val normalized = referentType.normalizeType(freshFieldName)) {
            is Normalized -> {
                val value = normalized.value
                if (value is FoxConcreteType) Normalized(FoxRefType(value))
                else NormalizationError("Ref referent type '$value' did not normalize to a concrete type")
            }
            is NormalizationError -> normalized
        }
        is FoxLambdaType -> {
            val normalizedThisType = when (val normalized = thisType.normalizeType(freshFieldName)) {
                is Normalized -> normalized.value as FoxConcreteType
                is NormalizationError -> return normalized
            }
            val normalizedParameters = buildList {
                parameters.forEach { parameter ->
                    when (val normalized = parameter.normalizeType(freshFieldName)) {
                        is Normalized -> add(normalized.value as FoxConcreteType)
                        is NormalizationError -> return normalized
                    }
                }
            }
            val normalizedReturnType = when (val normalized = returnType.normalizeType(freshFieldName)) {
                is Normalized -> normalized.value as FoxConcreteType
                is NormalizationError -> return normalized
            }
            Normalized(FoxLambdaType(normalizedThisType, normalizedParameters, normalizedReturnType))
        }
        else -> Normalized(this)
    }
    is FoxGenericType -> Normalized(this)
    is FoxGenericArrayType -> when (val normalized = elementType.normalizeType(freshFieldName)) {
        is Normalized -> {
            val value = normalized.value
            if (value is FoxConcreteType) Normalized(FoxArrayType(value))
            else Normalized(FoxGenericArrayType(value))
        }
        is NormalizationError -> normalized
    }
    is FoxGenericTupleType -> {
        val normalizedComponents = buildList {
            componentTypes.forEach { component ->
                when (val normalized = component.normalizeType(freshFieldName)) {
                    is Normalized -> add(normalized.value)
                    is NormalizationError -> return normalized
                }
            }
        }
        normalizedTupleFromTypes(normalizedComponents)
    }
    is FoxTupleTemplateType -> normalizeTupleTemplate(freshFieldName)
    is FoxNamedProjectionType -> normalizeNamedProjection(freshFieldName)
    is FoxGenericStructType -> {
        val normalizedFields = LinkedHashMap<String, FoxType>()
        for ((name, type) in fields) {
            when (val normalized = type.normalizeType(freshFieldName)) {
                is Normalized -> normalizedFields[name] = normalized.value
                is NormalizationError -> return normalized
            }
        }
        normalizedStructFromEntries(normalizedFields.entries.map { it.key to it.value })
    }
    is FoxStructTemplateType -> normalizeStructTemplate(freshFieldName)
    is FoxDenamedProjectionType -> normalizeDenamedProjection(freshFieldName)
    is FoxGenericEnumType -> {
        val normalizedItems = LinkedHashMap<String, FoxType>()
        for ((name, type) in items) {
            when (val normalized = type.normalizeType(freshFieldName)) {
                is Normalized -> normalizedItems[name] = normalized.value
                is NormalizationError -> return normalized
            }
        }
        if (normalizedItems.values.all { it is FoxConcreteType }) {
            @Suppress("UNCHECKED_CAST")
            Normalized(FoxEnumType(normalizedItems as Map<String, FoxConcreteType>))
        } else {
            Normalized(FoxGenericEnumType(normalizedItems))
        }
    }
    is FoxGenericRefType -> when (val normalized = referentType.normalizeType(freshFieldName)) {
        is Normalized -> {
            val value = normalized.value
            if (value is FoxConcreteType) Normalized(FoxRefType(value))
            else Normalized(FoxGenericRefType(value))
        }
        is NormalizationError -> normalized
    }
    is FoxGenericLambdaType -> {
        val normalizedThisType = when (val normalized = thisType.normalizeType(freshFieldName)) {
            is Normalized -> normalized.value
            is NormalizationError -> return normalized
        }
        val normalizedParameters = buildList {
            parameters.forEach { parameter ->
                when (val normalized = parameter.normalizeType(freshFieldName)) {
                    is Normalized -> add(normalized.value)
                    is NormalizationError -> return normalized
                }
            }
        }
        val normalizedReturnType = when (val normalized = returnType.normalizeType(freshFieldName)) {
            is Normalized -> normalized.value
            is NormalizationError -> return normalized
        }
        if (normalizedThisType is FoxConcreteType &&
            normalizedReturnType is FoxConcreteType &&
            normalizedParameters.all { it is FoxConcreteType }
        ) {
            @Suppress("UNCHECKED_CAST")
            Normalized(
                FoxLambdaType(
                    normalizedThisType,
                    normalizedParameters as List<FoxConcreteType>,
                    normalizedReturnType,
                ),
            )
        } else {
            Normalized(
                FoxGenericLambdaType(
                    normalizedThisType,
                    normalizedParameters,
                    normalizedReturnType,
                ),
            )
        }
    }
}

fun FoxGenericConstraint.normalizeConstraint(): NormalizationResult<FoxGenericConstraint> = when (this) {
    is FoxAnyConstraint -> Normalized(this)
    is FoxStructWildcardConstraint -> Normalized(this)
    is FoxExactMatchConstraint -> when (val normalized = type.normalizeType()) {
        is Normalized -> {
            val value = normalized.value
            if (value is FoxConcreteType) Normalized(FoxExactMatchConstraint(value))
            else NormalizationError("Exact-match constraint '$type' did not normalize to a concrete type")
        }
        is NormalizationError -> normalized
    }
}

fun FoxType.asTupleRow(
    freshFieldName: ((index: Int) -> String)? = null,
): NormalizationResult<List<FoxType>> = when (val normalized = normalizeType(freshFieldName)) {
    is Normalized -> when (val value = normalized.value) {
        is FoxTupleType -> Normalized(value.componentTypes)
        is FoxGenericTupleType -> Normalized(value.componentTypes)
        else -> NormalizationError("Type '$value' cannot be expanded as tuple row")
    }
    is NormalizationError -> normalized
}

fun FoxType.asStructRow(
    freshFieldName: ((index: Int) -> String)? = null,
): NormalizationResult<List<Pair<String, FoxType>>> = when (val normalized = normalizeType(freshFieldName)) {
    is Normalized -> when (val value = normalized.value) {
        is FoxStructType -> Normalized(value.fields.entries.map { it.key to it.value })
        is FoxGenericStructType -> Normalized(value.fields.entries.map { it.key to it.value })
        else -> NormalizationError("Type '$value' cannot be expanded as struct row")
    }
    is NormalizationError -> normalized
}

fun FoxType.asParameterRow(): NormalizationResult<List<NormalizedMethodParameter>> = when (val normalized = normalizeType()) {
    is Normalized -> when (val value = normalized.value) {
        is FoxNamedProjectionType -> NormalizationError(
            "Type '$value' cannot be expanded as parameter row because Named<Tuple<...>> does not provide stable parameter names",
        )
        is FoxStructType -> Normalized(value.fields.entries.map { (name, type) -> NormalizedMethodParameter(name, type) })
        is FoxGenericStructType -> Normalized(value.fields.entries.map { (name, type) -> NormalizedMethodParameter(name, type) })
        else -> NormalizationError("Type '$value' cannot be expanded as parameter row")
    }
    is NormalizationError -> when (this) {
        is FoxNamedProjectionType -> NormalizationError(
            "Type '$this' cannot be expanded as parameter row because Named<Tuple<...>> does not provide stable parameter names",
        )
        else -> normalized
    }
}

private fun FoxTupleTemplateType.normalizeTupleTemplate(
    freshFieldName: ((index: Int) -> String)?,
): NormalizationResult<FoxType> {
    val elements = mutableListOf<FoxType>()
    for (item in items) {
        when (item) {
            is FoxTupleTypeTemplateItem -> {
                when (val normalized = item.type.normalizeType(freshFieldName)) {
                    is Normalized -> elements += normalized.value
                    is NormalizationError -> return normalized
                }
            }
            is FoxTupleSpreadTemplateItem -> {
                when (val row = item.type.asTupleRow(freshFieldName)) {
                    is Normalized -> elements += row.value
                    is NormalizationError -> return row
                }
            }
        }
    }
    return normalizedTupleFromTypes(elements)
}

private fun FoxStructTemplateType.normalizeStructTemplate(
    freshFieldName: ((index: Int) -> String)?,
): NormalizationResult<FoxType> {
    val entries = mutableListOf<Pair<String, FoxType>>()
    for (item in items) {
        when (item) {
            is FoxStructFieldTemplateItem -> {
                when (val normalized = item.type.normalizeType(freshFieldName)) {
                    is Normalized -> entries += item.name to normalized.value
                    is NormalizationError -> return normalized
                }
            }
            is FoxStructSpreadTemplateItem -> {
                when (val row = item.type.asStructRow(freshFieldName)) {
                    is Normalized -> entries += row.value
                    is NormalizationError -> return row
                }
            }
        }
    }
    return normalizedStructFromEntries(entries)
}

private fun FoxNamedProjectionType.normalizeNamedProjection(
    freshFieldName: ((index: Int) -> String)?,
): NormalizationResult<FoxType> {
    val allocator = freshFieldName ?: return NormalizationError(
        "Named projection '$this' requires a fresh field-name allocator",
    )
    return when (val row = baseType.asTupleRow(freshFieldName)) {
        is Normalized -> normalizedStructFromEntries(
            row.value.mapIndexed { index, type -> allocator(index) to type },
        )
        is NormalizationError -> row
    }
}

private fun FoxDenamedProjectionType.normalizeDenamedProjection(
    freshFieldName: ((index: Int) -> String)?,
): NormalizationResult<FoxType> = when (val row = baseType.asStructRow(freshFieldName)) {
    is Normalized -> normalizedTupleFromTypes(row.value.map { it.second })
    is NormalizationError -> row
}

private fun normalizedTupleFromTypes(types: List<FoxType>): NormalizationResult<FoxType> {
    return if (types.all { it is FoxConcreteType }) {
        @Suppress("UNCHECKED_CAST")
        Normalized(FoxTupleType(types as List<FoxConcreteType>))
    } else {
        Normalized(FoxGenericTupleType(types))
    }
}

private fun normalizedStructFromEntries(entries: List<Pair<String, FoxType>>): NormalizationResult<FoxType> {
    val names = LinkedHashSet<String>()
    for ((name, _) in entries) {
        if (!names.add(name)) return NormalizationError("Duplicate struct field '$name' after expansion")
    }
    val fields = LinkedHashMap<String, FoxType>()
    entries.forEach { (name, type) -> fields[name] = type }
    return if (fields.values.all { it is FoxConcreteType }) {
        @Suppress("UNCHECKED_CAST")
        Normalized(FoxStructType(fields as Map<String, FoxConcreteType>))
    } else {
        Normalized(FoxGenericStructType(fields))
    }
}
